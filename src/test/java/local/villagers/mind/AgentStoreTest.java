package local.villagers.mind;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AgentStoreTest {
    @TempDir Path root;

    @Test void retainsIdentityHomeDrivesKnowledgeAndMemoriesAcrossReload() throws Exception {
        UUID id = UUID.randomUUID();
        AgentMind mind = new AgentMind(id);
        mind.home(new Home("minecraft:overworld", 12, 70, -30));
        mind.advanceDrives(300);
        Observation before = new Observation(0.4, 0.6, 0.1, 1, false, false, true, true);
        Observation after = new Observation(0, 0.6, 0.1, 1, false, false, true, true);
        for (int i = 0; i < 20; i++) mind.learn(before, Action.FORAGE, after);
        mind.remember("Ate bread after reaching it");
        AgentStore store = AgentStore.worldScoped(root);
        assertTrue(store.save(mind.snapshot()), store.lastError());
        AgentMind restored = AgentStore.worldScoped(root).load(id);
        assertEquals(id, restored.id());
        assertEquals(mind.name(), restored.name());
        assertEquals(mind.personality(), restored.personality());
        assertEquals(mind.home(), restored.home());
        assertEquals(mind.hunger(), restored.hunger());
        assertEquals(mind.fatigue(), restored.fatigue());
        assertEquals(mind.loneliness(), restored.loneliness());
        assertEquals(mind.learningUpdates(), restored.learningUpdates());
        assertEquals(mind.memories(), restored.memories());
        assertEquals(mind.choose(before, new Random(1)), restored.choose(before, new Random(1)));
        assertTrue(Files.readString(root.resolve(id.toString()).resolve("mind.json")).contains("Ate bread"));
    }

    @Test void unsupportedSchemasAreProtectedEvenWithoutLoading() throws Exception {
        UUID id = UUID.randomUUID();
        Path directory = Files.createDirectories(root.resolve(id.toString()));
        Path file = directory.resolve("mind.json");
        String future = "{\"schemaVersion\":999,\"precious\":\"future format\"}";
        Files.writeString(file, future);
        AgentStore store = AgentStore.worldScoped(root);
        assertEquals(id, store.load(id).id());
        assertFalse(store.save(new AgentMind(id)));
        assertFalse(AgentStore.worldScoped(root).save(new AgentMind(id)));
        assertEquals(future, Files.readString(file));
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }

    @Test void malformedFilesAreQuarantinedBeforeReplacement() throws Exception {
        UUID id = UUID.randomUUID();
        Path directory = Files.createDirectories(root.resolve(id.toString()));
        Path file = directory.resolve("mind.json");
        Files.writeString(file, "broken json");
        AgentStore store = AgentStore.worldScoped(root);
        AgentMind recovered = store.load(id);
        assertNotNull(store.lastError());
        assertFalse(Files.exists(file));
        try (var files = Files.list(directory)) {
            Path quarantine = files.findFirst().orElseThrow();
            assertTrue(quarantine.getFileName().toString().startsWith("mind.corrupt."));
            assertEquals("broken json", Files.readString(quarantine));
        }
        assertTrue(store.save(recovered));
    }

    @Test void worldsDoNotShareAnIdentityFile() {
        UUID id = UUID.randomUUID();
        AgentMind first = new AgentMind(id);
        first.home(new Home("minecraft:overworld", 1, 2, 3));
        assertTrue(AgentStore.worldScoped(root.resolve("world-one")).save(first));
        assertNull(AgentStore.worldScoped(root.resolve("world-two")).load(id).home());
    }

    @Test void unwritablePathFailsSafely() throws Exception {
        Path occupied = root.resolve("not-a-directory");
        Files.writeString(occupied, "keep");
        AgentStore store = AgentStore.worldScoped(occupied);
        assertFalse(store.save(new AgentMind(UUID.randomUUID())));
        assertNotNull(store.lastError());
        assertEquals("keep", Files.readString(occupied));
    }
}
