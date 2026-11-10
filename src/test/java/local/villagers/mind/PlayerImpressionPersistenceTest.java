package local.villagers.mind;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import static local.villagers.mind.PlayerImpression.Evidence.*;
import static org.junit.jupiter.api.Assertions.*;

class PlayerImpressionPersistenceTest {
    @TempDir Path root;

    @Test void socialHistoryAndCooldownsSurviveDiskReload() {
        UUID id = UUID.randomUUID(), player = UUID.randomUUID();
        AgentMind original = new AgentMind(id);
        original.observePlayer(player, USEFUL_GIFT, 100);
        original.observePlayer(player, PLAYER_HURT, 101);
        AgentStore store = AgentStore.worldScoped(root);
        assertTrue(store.save(original), store.lastError());
        AgentMind restored = AgentStore.worldScoped(root).load(id);
        assertEquals(original.impressions(), restored.impressions());
        assertEquals(original.memories(), restored.memories());
        assertFalse(restored.observePlayer(player, USEFUL_GIFT, 200));
        assertEquals(2, restored.playerImpression(player).evidenceCount());
    }

    @Test void schemaOneJsonMigratesWithoutLosingIdentityHomeOrActionLearning() throws Exception {
        AgentMind original = trainedMind();
        AgentMind.Snapshot before = original.snapshot();
        JsonObject legacy = new Gson().toJsonTree(before).getAsJsonObject();
        legacy.addProperty("schemaVersion", 1);
        legacy.remove("impressions");
        Path directory = Files.createDirectories(root.resolve(original.id().toString()));
        Path file = directory.resolve("mind.json");
        Files.writeString(file, legacy.toString());
        AgentStore store = AgentStore.worldScoped(root);
        AgentMind migrated = store.load(original.id());
        assertNull(store.lastError(), "Legitimate schema one must not be quarantined");
        AgentMind.Snapshot after = migrated.snapshot();
        assertEquals(AgentMind.SCHEMA_VERSION, after.schemaVersion());
        assertEquals(before.id(), after.id());
        assertEquals(before.name(), after.name());
        assertEquals(before.personality(), after.personality());
        assertEquals(before.home(), after.home());
        assertEquals(before.hunger(), after.hunger());
        assertEquals(before.fatigue(), after.fatigue());
        assertEquals(before.loneliness(), after.loneliness());
        assertEquals(before.learningUpdates(), after.learningUpdates());
        assertEquals(before.memories(), after.memories());
        for (int c = 0; c < before.values().length; c++) {
            assertArrayEquals(before.values()[c], after.values()[c]);
            assertArrayEquals(before.visits()[c], after.visits()[c]);
        }
        assertTrue(after.impressions().isEmpty());
        assertTrue(store.save(migrated), store.lastError());
        assertEquals(AgentMind.SCHEMA_VERSION, new Gson().fromJson(Files.readString(file), JsonObject.class).get("schemaVersion").getAsInt());
        try (var files = Files.list(directory)) {
            assertFalse(files.anyMatch(p -> p.getFileName().toString().contains("corrupt")));
        }
    }

    @Test void oldSnapshotConstructorIsAcceptedAndSavesAsCurrentSchema() throws Exception {
        AgentMind.Snapshot s = trainedMind().snapshot();
        AgentMind.Snapshot legacy = new AgentMind.Snapshot(1, s.id(), s.name(), s.personality(),
                s.hunger(), s.fatigue(), s.loneliness(), s.home(), s.learningUpdates(), s.values(), s.visits(), s.memories());
        AgentStore store = AgentStore.worldScoped(root);
        assertTrue(store.save(legacy), store.lastError());
        Path file = root.resolve(s.id().toString()).resolve("mind.json");
        assertEquals(AgentMind.SCHEMA_VERSION,
                new Gson().fromJson(Files.readString(file), JsonObject.class).get("schemaVersion").getAsInt());
        assertEquals(s.learningUpdates(), store.load(s.id()).learningUpdates());
    }

    @Test void unknownFutureSchemaRemainsUntouchedDuringMigration() throws Exception {
        UUID id = UUID.randomUUID();
        Path directory = Files.createDirectories(root.resolve(id.toString()));
        Path file = directory.resolve("mind.json");
        String future = "{\"schemaVersion\":" + (AgentMind.SCHEMA_VERSION + 1) + ",\"futureRelationshipData\":\"preserve\"}";
        Files.writeString(file, future);
        AgentStore store = AgentStore.worldScoped(root);
        assertEquals(id, store.load(id).id());
        assertFalse(store.save(new AgentMind(id)));
        assertFalse(AgentStore.worldScoped(root).save(new AgentMind(id)));
        assertEquals(future, Files.readString(file));
    }

    private static AgentMind trainedMind() {
        AgentMind mind = new AgentMind(UUID.fromString("77777777-1111-2222-3333-444444444444"));
        mind.home(new Home("minecraft:overworld", -12, 64, 7));
        mind.advanceDrives(1000);
        Observation before = new Observation(.7, .3, .4, 1, false, false, true, true);
        Observation after = new Observation(.25, .3, .4, 1, false, false, true, true);
        mind.learn(before, Action.FORAGE, after);
        mind.remember("Old household and action experience");
        return mind;
    }
}
