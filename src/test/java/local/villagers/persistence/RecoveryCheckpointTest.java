package local.villagers.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import local.villagers.mind.AgentMind;
import local.villagers.mind.AgentStore;
import local.villagers.mind.Ancestry;
import local.villagers.mind.Home;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.helpers.NOPLogger;
import static org.junit.jupiter.api.Assertions.*;

class RecoveryCheckpointTest {
    @TempDir Path root;
    private RecoveryCheckpoint.Ticket ticket(int id) {
        return new RecoveryCheckpoint.Ticket("minecraft:overworld", new UUID(0, id), id, 64, 0, 0, 64, 0);
    }
    private AgentMind dead(int id) {
        AgentMind mind = new AgentMind(new UUID(0, id));
        mind.home(new Home("minecraft:overworld", 0, 64, 0));
        mind.die(123, "starve");
        return mind;
    }

    @Test void sixtyFourCompactRequestsRoundTripAndOverCapacityNeverReplacesThem() throws Exception {
        Path file = root.resolve("recoveries.json");
        var store = new RecoveryCheckpoint(file);
        List<RecoveryCheckpoint.Ticket> requests = IntStream.rangeClosed(1, 64).mapToObj(this::ticket).toList();
        assertTrue(store.save(requests));
        byte[] before = Files.readAllBytes(file);
        assertEquals(requests, new RecoveryCheckpoint(file).load());
        assertFalse(store.save(IntStream.rangeClosed(1, 65).mapToObj(this::ticket).toList()));
        assertArrayEquals(before, Files.readAllBytes(file));
        assertTrue(store.save(List.of()), "resolved work becomes an explicit empty checkpoint");
        assertEquals(List.of(), new RecoveryCheckpoint(file).load());
    }

    @Test void futureCorruptAndOversizedDocumentsStayUntouchedEvenBeforeLoad() throws Exception {
        for (String contents : List.of("{\"schemaVersion\":2,\"future\":true}", "{broken", "x".repeat(131_073))) {
            Path file = root.resolve(UUID.randomUUID() + ".json");
            Files.writeString(file, contents);
            assertFalse(new RecoveryCheckpoint(file).save(List.of(ticket(1))));
            assertEquals(contents, Files.readString(file));
            assertThrows(java.io.IOException.class, () -> new RecoveryCheckpoint(file).load());
            assertEquals(contents, Files.readString(file));
        }
    }

    @Test void missingFieldsDuplicateIdentitiesNonfinitePositionsAndLinksAreRejected() throws Exception {
        Path file = root.resolve("recoveries.json");
        var store = new RecoveryCheckpoint(file);
        assertTrue(store.save(List.of(ticket(1))));
        String valid = Files.readString(file);
        assertFalse(store.save(List.of(ticket(1), ticket(1))));
        assertEquals(valid, Files.readString(file));
        var missing = com.google.gson.JsonParser.parseString(valid).getAsJsonObject();
        missing.getAsJsonArray("requests").get(0).getAsJsonObject().remove("x");
        Files.writeString(file, missing.toString());
        assertThrows(java.io.IOException.class, () -> new RecoveryCheckpoint(file).load());
        assertThrows(IllegalArgumentException.class, () -> new RecoveryCheckpoint.Ticket("minecraft:overworld",
                UUID.randomUUID(), Double.NaN, 64, 0, 0, 64, 0));
        Path link = root.resolve("link.json"); Files.createSymbolicLink(link, file);
        String before = Files.readString(file);
        assertFalse(new RecoveryCheckpoint(link).save(List.of()));
        assertTrue(Files.isSymbolicLink(link)); assertEquals(before, Files.readString(file));
    }

    @Test void queuedChangesDoNoCallerIoAndCleanShutdownRestoresNewestTicketsAndDeathHistory() throws Exception {
        var executor = Executors.newSingleThreadScheduledExecutor();
        var release = new CountDownLatch(1);
        executor.execute(() -> { try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
        var persistence = new MindPersistence(AgentStore.worldScoped(root), NOPLogger.NOP_LOGGER, Duration.ofHours(1), executor);
        try {
            persistence.queue(dead(1)); persistence.queue(dead(2));
            persistence.queueRecoveries(List.of(ticket(1)));
            persistence.queueRecoveries(List.of(ticket(2)));
            assertFalse(Files.exists(root.resolve("recoveries.json")), "server caller does not write through blocked worker");
            release.countDown();
            assertTrue(persistence.close(Duration.ofSeconds(5)));
        } finally { release.countDown(); persistence.close(Duration.ofSeconds(5)); }
        var reopened = new MindPersistence(AgentStore.worldScoped(root), NOPLogger.NOP_LOGGER, Duration.ofHours(1));
        try {
            var restored = reopened.restoreRecoveries().get(5, TimeUnit.SECONDS);
            assertEquals(1, restored.size()); assertEquals(ticket(2), restored.getFirst().ticket());
            assertEquals(new UUID(0, 2), restored.getFirst().deceased().id());
            assertEquals("starve", restored.getFirst().deceased().death().cause());
        } finally { assertTrue(reopened.close(Duration.ofSeconds(5))); }
    }

    @Test void unreadableDeathRecordsDoNotBecomeFoundersOrGetQuarantined() throws Exception {
        var ticket = ticket(3);
        Path mind = Files.createDirectories(root.resolve(ticket.deceased().toString())).resolve("mind.json");
        String future = "{\"schemaVersion\":999,\"preserve\":true}";
        Files.writeString(mind, future);
        assertTrue(new RecoveryCheckpoint(root.resolve("recoveries.json")).save(List.of(ticket)));
        var persistence = new MindPersistence(AgentStore.worldScoped(root), NOPLogger.NOP_LOGGER, Duration.ofHours(1));
        try {
            var restored = persistence.restoreRecoveries().get(5, TimeUnit.SECONDS);
            assertEquals(ticket, restored.getFirst().ticket()); assertNull(restored.getFirst().deceased());
            assertEquals(future, Files.readString(mind));
            try (var files = Files.list(mind.getParent())) { assertEquals(1, files.count()); }
        } finally { assertTrue(persistence.close(Duration.ofSeconds(5))); }
    }

    @Test void failedCheckpointMakesShutdownUnsuccessfulAndPreservesTheFile() throws Exception {
        Path file = root.resolve("recoveries.json");
        String future = "{\"schemaVersion\":999}"; Files.writeString(file, future);
        var persistence = new MindPersistence(AgentStore.worldScoped(root), NOPLogger.NOP_LOGGER, Duration.ofHours(1));
        persistence.queueRecoveries(List.of(ticket(1)));
        assertFalse(persistence.close(Duration.ofSeconds(5)));
        assertFalse(persistence.close(Duration.ofSeconds(5)), "later close cannot turn retained failed writes into success");
        assertEquals(future, Files.readString(file));
    }

    @Test void failedMindWriteAlsoCannotBeReportedAsSuccessfulShutdown() throws Exception {
        AgentMind mind = dead(6);
        Path obstruction = root.resolve(mind.id().toString());
        Files.writeString(obstruction, "preserve existing non-directory data");
        var persistence = new MindPersistence(AgentStore.worldScoped(root), NOPLogger.NOP_LOGGER, Duration.ofHours(1));
        persistence.queue(mind);
        assertFalse(persistence.close(Duration.ofSeconds(5)));
        assertEquals(1, persistence.pendingCount());
        assertEquals("preserve existing non-directory data", Files.readString(obstruction));
    }

    @Test void ancestryFlushesAdmittedCheckpointBeforeItsResultAndStoppedRestoreDoesNoCallerIo() throws Exception {
        var persistence = new MindPersistence(AgentStore.worldScoped(root), NOPLogger.NOP_LOGGER, Duration.ofHours(1));
        AgentMind dead = dead(1); persistence.queue(dead); persistence.queueRecoveries(List.of(ticket(1)));
        try {
            var query = new Ancestry.Neighborhood("minecraft:overworld", 0, 64, 0, 64, dead.id());
            assertEquals(dead.id(), persistence.ancestor(query, dead.snapshot(), Set.of(dead.id())).get(5, TimeUnit.SECONDS).id());
            assertEquals(List.of(ticket(1)), new RecoveryCheckpoint(root.resolve("recoveries.json")).load());
        } finally { assertTrue(persistence.close(Duration.ofSeconds(5))); }
        assertTrue(persistence.restoreRecoveries().isCompletedExceptionally());
    }
}
