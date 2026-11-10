package local.villagers.mind;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static local.villagers.mind.PlayerImpression.Evidence.*;
import static org.junit.jupiter.api.Assertions.*;

class EpisodeTest {
    private static final UUID PLAYER = new UUID(7, 7);
    @TempDir Path root;

    @Test void harmAndProtectionBecomeEpisodesSharedByThePlayerInvolved() {
        AgentMind mind = new AgentMind(UUID.randomUUID());
        UUID bystander = new UUID(8, 8);
        mind.observePlayer(PLAYER, WITNESSED_PROTECTION, 100, List.of(bystander));
        mind.observePlayer(PLAYER, PLAYER_HURT, 200);
        List<Episode> episodes = mind.episodes();
        assertEquals(List.of(Episode.Kind.PROTECTED_BY_PLAYER, Episode.Kind.HURT_BY_PLAYER),
                episodes.stream().map(Episode::kind).toList());
        assertEquals(List.of(PLAYER, bystander), episodes.getFirst().knownBy());
        assertTrue(episodes.getLast().isKnownBy(PLAYER));
        assertFalse(episodes.getLast().isKnownBy(bystander), "a player who wasn't there doesn't share it");
    }

    @Test void routineExposureAndGiftEvidenceDoNotFloodEpisodes() {
        AgentMind mind = new AgentMind(UUID.randomUUID());
        for (int i = 0; i < 20; i++) mind.observePlayer(PLAYER, EXPOSURE, i * 200L);
        mind.observePlayer(PLAYER, USEFUL_GIFT, 5000);
        assertTrue(mind.episodes().isEmpty());
    }

    @Test void episodesOutliveRoutineMemoryTurnover() {
        AgentMind mind = new AgentMind(UUID.randomUUID());
        mind.recordEpisode(Episode.Kind.RECEIVED_ITEM, 10, PLAYER, null, "minecraft:bread", List.of(PLAYER));
        for (int i = 0; i < 500; i++) mind.remember("routine " + i);
        assertEquals(1, mind.episodes().size(), "routine outcomes must not push out memorable events");
    }

    @Test void episodeListAndWitnessesAreBoundedAndIdsKeepIncreasing() {
        AgentMind mind = new AgentMind(UUID.randomUUID());
        List<UUID> crowd = new ArrayList<>();
        for (int i = 0; i < 20; i++) crowd.add(new UUID(1, i));
        crowd.add(crowd.getFirst());
        for (int i = 0; i < AgentMind.MAX_EPISODES + 10; i++)
            mind.recordEpisode(Episode.Kind.CLAIMED_BED, i, null, new Place("minecraft:overworld", i, 64, 0), null, crowd);
        List<Episode> episodes = mind.episodes();
        assertEquals(AgentMind.MAX_EPISODES, episodes.size());
        assertEquals(10, episodes.getFirst().id(), "oldest are forgotten first");
        assertEquals(Episode.MAX_KNOWERS, episodes.getLast().knownBy().size());
        assertEquals(crowd.subList(0, Episode.MAX_KNOWERS), episodes.getLast().knownBy(), "nearest witnesses are kept");
    }

    @Test void episodesAndIdCounterSurviveReload() {
        AgentMind mind = new AgentMind(UUID.randomUUID());
        mind.recordEpisode(Episode.Kind.RECEIVED_ITEM, 10, PLAYER, null, "minecraft:bread", List.of(PLAYER));
        AgentStore store = AgentStore.worldScoped(root);
        assertTrue(store.save(mind), store.lastError());
        AgentMind restored = AgentStore.worldScoped(root).load(mind.id());
        assertEquals(mind.episodes(), restored.episodes());
        Episode next = restored.recordEpisode(Episode.Kind.HURT_BY_PLAYER, 20, PLAYER, null, null, List.of());
        assertEquals(1, next.id(), "ids are never reused after a reload");
    }

    @Test void schemaTwoFilesUpgradeWithNoEpisodesAndNoOneKnowingOldNotes() throws Exception {
        AgentMind original = new AgentMind(UUID.randomUUID());
        original.remember("Old note from schema two");
        original.observePlayer(PLAYER, USEFUL_GIFT, 100);
        JsonObject legacy = new Gson().toJsonTree(original.snapshot()).getAsJsonObject();
        legacy.addProperty("schemaVersion", 2);
        legacy.remove("episodes");
        legacy.remove("nextEpisodeId");
        Path file = Files.createDirectories(root.resolve(original.id().toString())).resolve("mind.json");
        Files.writeString(file, legacy.toString());
        AgentStore store = AgentStore.worldScoped(root);
        AgentMind migrated = store.load(original.id());
        assertNull(store.lastError(), "schema two must load, not be quarantined");
        assertEquals(original.memories(), migrated.memories());
        assertEquals(original.impressions(), migrated.impressions());
        assertTrue(migrated.episodes().isEmpty());
        assertTrue(store.save(migrated), store.lastError());
        assertEquals(AgentMind.SCHEMA_VERSION,
                new Gson().fromJson(Files.readString(file), JsonObject.class).get("schemaVersion").getAsInt());
    }

    @Test void rejectsInvalidEpisodes() {
        assertThrows(IllegalArgumentException.class, () -> new Episode(-1, Episode.Kind.HURT_BY_PLAYER, 0, null, null, null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Episode(0, Episode.Kind.HURT_BY_PLAYER, -5, null, null, null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Episode(0, Episode.Kind.HURT_BY_PLAYER, 0, null, null, null, List.of(PLAYER, PLAYER)));
        assertThrows(IllegalArgumentException.class, () -> new Episode(0, Episode.Kind.HURT_BY_PLAYER, 0, null, null, "x".repeat(200), List.of()));
    }
}
