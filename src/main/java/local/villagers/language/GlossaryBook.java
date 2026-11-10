package local.villagers.language;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import local.villagers.persistence.FailureThrottle;

/**
 * The glossaries of players on this server. Loaded when a player joins (a small file), held in memory, and written
 * on a background thread when they leave, periodically, and at shutdown. Game-thread callers never wait for writes.
 */
public final class GlossaryBook {
    private final GlossaryStore store;
    private final Consumer<String> problems;
    private final Map<UUID, Glossary> glossaries = new ConcurrentHashMap<>();
    private final Map<UUID, Glossary> dirty = new ConcurrentHashMap<>();
    private final AtomicBoolean flushQueued = new AtomicBoolean();
    private final FailureThrottle<UUID> failures = new FailureThrottle<>(Duration.ofMinutes(1));
    private final ExecutorService writer;

    private static ExecutorService newWriter() { return Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "villagers-glossary-save");
        thread.setDaemon(true);
        return thread;
    }); }

    public GlossaryBook(GlossaryStore store, Consumer<String> problems) {
        this(store, problems, newWriter());
    }

    GlossaryBook(GlossaryStore store, Consumer<String> problems, ExecutorService writer) {
        this.writer = Objects.requireNonNull(writer);
        this.store = Objects.requireNonNull(store);
        this.problems = Objects.requireNonNull(problems);
    }

    /** Load a joining player's glossary. */
    public void join(UUID player) {
        StringBuilder problem = new StringBuilder();
        Glossary loaded = dirty.get(player);
        if (loaded == null) loaded = store.load(player, problem);
        if (!problem.isEmpty()) problems.accept(problem.toString());
        glossaries.put(player, loaded);
    }

    public Glossary get(UUID player) { return glossaries.computeIfAbsent(player, Glossary::empty); }

    public void update(UUID player, UnaryOperator<Glossary> change) {
        Glossary next = change.apply(get(player));
        glossaries.put(player, next);
        dirty.put(player, next);
    }

    /** Save and forget a leaving player's glossary. */
    public void leave(UUID player) {
        flush();
        glossaries.remove(player);
    }

    /** Queue writes for every changed glossary. */
    public void flush() {
        if (dirty.isEmpty() || !flushQueued.compareAndSet(false, true)) return;
        try {
            writer.execute(() -> {
                try { writePending(); }
                finally { flushQueued.set(false); }
            });
        } catch (RejectedExecutionException closed) {
            flushQueued.set(false);
        }
    }

    private void writePending() {
        dirty.forEach((player, snapshot) -> {
            if (store.save(snapshot)) {
                // A new lesson received during this write must remain pending.
                dirty.remove(player, snapshot);
                failures.recovered(player);
            } else if (failures.shouldReport(player)) {
                problems.accept("Could not save glossary for " + player + "; changes retained for retry");
            }
        });
    }

    /** Write everything still pending and stop. Returns false if writes time out or any changes remain unsaved. */
    public boolean close(long timeoutMillis) {
        if (!writer.isShutdown()) {
            try { writer.execute(this::writePending); }
            catch (RejectedExecutionException closed) { /* Another close already stopped submission. */ }
            writer.shutdown();
        }
        try {
            return writer.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS) && dirty.isEmpty();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
