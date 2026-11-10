package local.villagers.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import local.villagers.mind.AgentMind;
import local.villagers.mind.AgentStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.helpers.NOPLogger;
import static org.junit.jupiter.api.Assertions.*;

class MindPersistenceTest {
    @TempDir Path root;
    private MindPersistence persistence;

    @BeforeEach void open() {
        // A long interval keeps the scheduled flush out of the way; tests flush explicitly.
        persistence = new MindPersistence(AgentStore.worldScoped(root), NOPLogger.NOP_LOGGER, Duration.ofHours(1));
    }

    @AfterEach void close() { persistence.close(Duration.ofSeconds(5)); }

    @Test void flushWritesQueuedSnapshotsAndEmptiesQueue() {
        AgentMind mind = new AgentMind(UUID.randomUUID());
        mind.remember("queued before flush");
        persistence.queue(mind);
        assertEquals(1, persistence.pendingCount());
        persistence.flush();
        assertEquals(0, persistence.pendingCount());
        assertEquals(mind.memories(), AgentStore.worldScoped(root).load(mind.id()).memories());
    }

    @Test void queuedSnapshotIsNewerThanDiskAndIsUsedWithoutWaiting() {
        AgentMind mind = new AgentMind(UUID.randomUUID());
        mind.remember("only in memory");
        persistence.queue(mind);
        AgentMind reacquired = persistence.tryAcquire(mind.id());
        assertNotNull(reacquired);
        assertEquals(mind.memories(), reacquired.memories());
    }

    @Test void tryAcquireEventuallyReturnsTheDiskStateWithoutBlocking() throws Exception {
        AgentMind saved = new AgentMind(UUID.randomUUID());
        saved.remember("from disk");
        assertTrue(AgentStore.worldScoped(root).save(saved));
        AgentMind loaded = null;
        for (int attempt = 0; attempt < 200 && loaded == null; attempt++) {
            loaded = persistence.tryAcquire(saved.id());
            if (loaded == null) Thread.sleep(5);
        }
        assertNotNull(loaded, "background load never completed");
        assertEquals(saved.memories(), loaded.memories());
    }

    @Test void protectedMindsAreDroppedInsteadOfRetriedForever() throws Exception {
        UUID id = UUID.randomUUID();
        Path file = Files.createDirectories(root.resolve(id.toString())).resolve("mind.json");
        String future = "{\"schemaVersion\":999}";
        Files.writeString(file, future);
        persistence.queue(new AgentMind(id));
        persistence.flush();
        assertEquals(0, persistence.pendingCount(), "a save that can never succeed must not stay queued");
        persistence.queue(new AgentMind(id));
        assertEquals(0, persistence.pendingCount(), "later saves for a protected mind are skipped");
        assertEquals(future, Files.readString(file));
    }

    @Test void closeWritesWhatIsStillQueued() {
        AgentMind mind = new AgentMind(UUID.randomUUID());
        persistence.queue(mind);
        assertTrue(persistence.close(Duration.ofSeconds(5)));
        assertTrue(Files.exists(root.resolve(mind.id().toString()).resolve("mind.json")));
    }
}
