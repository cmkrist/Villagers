package local.villagers.persistence;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import local.villagers.mind.AgentMind;
import local.villagers.mind.AgentStore;
import local.villagers.mind.Ancestry;
import org.slf4j.Logger;

/**
 * Moves mind file IO onto a single background thread. The game thread queues immutable snapshots
 * and requests loads without waiting on disk. One pending snapshot is kept per identity, so the newest
 * state wins even when an entity unloads and reloads before its save is written.
 */
public final class MindPersistence {
    private final AgentStore store;
    private final Logger log;
    private final ScheduledExecutorService writer;
    private final ConcurrentMap<UUID, AgentMind.Snapshot> pending = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, CompletableFuture<AgentMind>> loading = new ConcurrentHashMap<>();
    private final Set<UUID> reportedProtected = ConcurrentHashMap.newKeySet();
    private final FailureThrottle<UUID> failures = new FailureThrottle<>(Duration.ofMinutes(1));
    private final RecoveryCheckpoint recoveryCheckpoint;
    private final AtomicReference<List<RecoveryCheckpoint.Ticket>> pendingRecoveries = new AtomicReference<>();
    private final FailureThrottle<String> recoveryFailures = new FailureThrottle<>(Duration.ofMinutes(1));
    public record RestoredRecovery(RecoveryCheckpoint.Ticket ticket, AgentMind.Snapshot deceased) {}

    public MindPersistence(AgentStore store, Logger log, Duration flushInterval) {
        this(store, log, flushInterval, newWriter());
    }

    MindPersistence(AgentStore store, Logger log, Duration flushInterval, ScheduledExecutorService writer) {
        this.store = Objects.requireNonNull(store);
        this.log = Objects.requireNonNull(log);
        this.writer = Objects.requireNonNull(writer);
        this.recoveryCheckpoint = new RecoveryCheckpoint(store.root().resolve("recoveries.json"));
        long millis = flushInterval.toMillis();
        writer.scheduleWithFixedDelay(this::flush, millis, millis, TimeUnit.MILLISECONDS);
    }

    private static ScheduledExecutorService newWriter() {
        return Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "villagers-save");
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Immutable request-time overlay wins over older disk state; caller never waits on IO. */
    public CompletableFuture<AgentMind.Snapshot> ancestor(Ancestry.Neighborhood query, AgentMind.Snapshot liveBest,
                                                         Set<UUID> liveIds) {
        Map<UUID, AgentMind.Snapshot> queued = Map.copyOf(pending);
        Set<UUID> live = Set.copyOf(liveIds);
        try {
            return CompletableFuture.supplyAsync(() -> {
                // Checkpoint admitted work before a potentially long historical scan occupies this worker.
                flush();
                AgentMind.Snapshot best = liveBest;
                var overridden = new java.util.HashSet<>(live);
                overridden.addAll(queued.keySet());
                for (var entry : queued.entrySet())
                    if (!live.contains(entry.getKey()) && query.includes(entry.getValue()))
                        best = Ancestry.better(best, entry.getValue());
                return store.bestAncestor(query, best, overridden);
            }, writer);
        } catch (RejectedExecutionException stopped) {
            return CompletableFuture.failedFuture(stopped);
        }
    }

    public AgentStore store() { return store; }

    public CompletableFuture<List<RestoredRecovery>> restoreRecoveries() {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return recoveryCheckpoint.load().stream()
                            .map(ticket -> new RestoredRecovery(ticket, store.readExisting(ticket.deceased()))).toList();
                } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
            }, writer);
        } catch (RejectedExecutionException stopped) { return CompletableFuture.failedFuture(stopped); }
    }

    /** Coalesce latest immutable state; does not enqueue a task per change or touch disk on the caller. */
    public void queueRecoveries(List<RecoveryCheckpoint.Ticket> tickets) {
        if (writer.isShutdown()) throw new IllegalStateException("Recovery persistence is stopping");
        pendingRecoveries.set(RecoveryCheckpoint.checked(tickets));
    }

    /** Queue the newest state of a mind. Protected identities are skipped, since every save would be refused. */
    public void queue(AgentMind mind) {
        if (writer.isShutdown()) return;
        UUID id = mind.id();
        if (store.isProtected(id)) {
            reportProtected(id);
            return;
        }
        pending.put(id, mind.snapshot());
    }

    /**
     * Non-blocking load for the tick path. Returns null while the disk read is still running;
     * call again on a later tick. A queued snapshot is newer than disk and is used directly.
     */
    public AgentMind tryAcquire(UUID id) {
        AgentMind.Snapshot queued = pending.get(id);
        if (queued != null) {
            loading.remove(id);
            return AgentMind.fromSnapshot(queued);
        }
        CompletableFuture<AgentMind> future;
        try {
            future = loading.computeIfAbsent(id, key -> CompletableFuture.supplyAsync(() -> store.load(key), writer));
        } catch (RejectedExecutionException shuttingDown) {
            return store.load(id);
        }
        if (!future.isDone()) return null;
        loading.remove(id, future);
        return future.join();
    }

    /** Blocking load for rare paths such as commands and player interactions. */
    public AgentMind acquire(UUID id) {
        AgentMind.Snapshot queued = pending.get(id);
        if (queued != null) {
            loading.remove(id);
            return AgentMind.fromSnapshot(queued);
        }
        CompletableFuture<AgentMind> inFlight = loading.remove(id);
        return inFlight != null ? inFlight.join() : store.load(id);
    }

    /**
     * Every known mind, living or dead: stored files, with queued snapshots taking precedence because they are
     * newer. Blocking diagnostic/export API; extinction recovery uses ancestor() instead.
     */
    public List<AgentMind> census() {
        Map<UUID, AgentMind> minds = new LinkedHashMap<>();
        for (AgentMind stored : store.readAll()) minds.put(stored.id(), stored);
        pending.forEach((id, snapshot) -> minds.put(id, AgentMind.fromSnapshot(snapshot)));
        return List.copyOf(minds.values());
    }

    /** Drop an unconsumed background load, e.g. when its entity unloads before it was ever ticked. */
    public void cancelLoad(UUID id) { loading.remove(id); }

    /** Write every queued snapshot. Runs on the writer thread; tests may call it directly. */
    public void flush() {
        pending.forEach((id, snapshot) -> {
            if (store.save(snapshot)) {
                pending.remove(id, snapshot);
                failures.recovered(id);
            } else if (store.isProtected(id)) {
                // Retrying can never succeed; keep the file untouched and stop retrying.
                pending.remove(id, snapshot);
                reportProtected(id);
            } else if (failures.shouldReport(id)) {
                log.error("Villager persistence failed (retrying, further errors for this villager suppressed for 1 minute): {}",
                        store.lastError());
            }
        });
        List<RecoveryCheckpoint.Ticket> recoveries = pendingRecoveries.get();
        if (recoveries != null) {
            if (recoveryCheckpoint.save(recoveries)) {
                pendingRecoveries.compareAndSet(recoveries, null);
                recoveryFailures.recovered("checkpoint");
            } else if (recoveryFailures.shouldReport("checkpoint")) {
                log.error("Divine recovery checkpoint failed (retained for retry): {}", recoveryCheckpoint.lastError());
            }
        }
    }

    public int pendingCount() { return pending.size(); }

    /** Write remaining snapshots/checkpoint and stop. False also reports retained failed writes. */
    public boolean close(Duration timeout) {
        if (writer.isShutdown()) return writer.isTerminated() && pending.isEmpty() && pendingRecoveries.get() == null;
        try {
            writer.execute(this::flush);
        } catch (RejectedExecutionException alreadyStopping) { /* Shutdown already requested; wait for it below. */ }
        writer.shutdown();
        loading.clear();
        try {
            return writer.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    && pending.isEmpty() && pendingRecoveries.get() == null;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void reportProtected(UUID id) {
        if (reportedProtected.add(id))
            log.warn("Not saving villager {}: its mind file has an unsupported schema or could not be quarantined, "
                    + "so it is left untouched. Learned changes this session stay in memory only.", id);
    }
}
