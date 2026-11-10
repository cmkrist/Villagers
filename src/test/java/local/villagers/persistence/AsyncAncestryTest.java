package local.villagers.persistence;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import local.villagers.mind.AgentMind;
import local.villagers.mind.AgentStore;
import local.villagers.mind.Ancestry;
import local.villagers.mind.Home;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.helpers.NOPLogger;
import static org.junit.jupiter.api.Assertions.*;

class AsyncAncestryTest {
    @TempDir Path root;
    private static AgentMind mind(long id, long age, int x) {
        AgentMind m = new AgentMind(new UUID(0, id));
        m.home(new Home("minecraft:overworld", x, 64, 0)); m.advanceDrives(age, 0); return m;
    }
    private static Ancestry.Neighborhood query(UUID id) {
        return new Ancestry.Neighborhood("minecraft:overworld", .5, 64.5, .5, 64, id);
    }

    @Test void aBusyIoWorkerDoesNotBlockCallerAndQueuedSnapshotOverridesDisk() throws Exception {
        AgentStore store = AgentStore.worldScoped(root);
        AgentMind fallback = mind(1, 1, 0), moved = mind(2, 1000, 0), current = mind(3, 100, 1);
        assertTrue(store.save(moved)); assertTrue(store.save(current));
        moved.home(new Home("minecraft:overworld", 1000, 64, 0));
        var executor = Executors.newSingleThreadScheduledExecutor();
        var release = new CountDownLatch(1);
        executor.execute(() -> { try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
        MindPersistence persistence = new MindPersistence(store, NOPLogger.NOP_LOGGER, Duration.ofHours(1), executor);
        try {
            persistence.queue(moved);
            var future = persistence.ancestor(query(fallback.id()), fallback.snapshot(), Set.of(fallback.id()));
            assertFalse(future.isDone(), "caller returned while the sole IO worker is deliberately occupied");
            moved.home(new Home("minecraft:overworld", 0, 64, 0));
            persistence.queue(moved); // A later state cannot mutate this request's detached overlay.
            release.countDown();
            assertEquals(current.id(), future.get(5, TimeUnit.SECONDS).id(), "queued changed home must hide stale disk home");
        } finally {
            release.countDown(); assertTrue(persistence.close(Duration.ofSeconds(5)));
        }
    }

    @Test void latestLiveCandidateWinsOverOlderQueuedVersionAndShutdownNeverFallsBackToCallerIo() throws Exception {
        AgentStore store = AgentStore.worldScoped(root);
        MindPersistence persistence = new MindPersistence(store, NOPLogger.NOP_LOGGER, Duration.ofHours(1));
        AgentMind live = mind(1, 10, 0), competitor = mind(2, 20, 0);
        persistence.queue(live); persistence.queue(competitor);
        live.advanceDrives(100, 0);
        try {
            assertEquals(live.id(), persistence.ancestor(query(live.id()), live.snapshot(), Set.of(live.id())).get(5, TimeUnit.SECONDS).id());
        } finally { assertTrue(persistence.close(Duration.ofSeconds(5))); }
        assertTrue(persistence.ancestor(query(live.id()), live.snapshot(), Set.of(live.id())).isCompletedExceptionally());
    }
}
