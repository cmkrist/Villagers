package local.villagers.mind;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import local.villagers.language.Lexicon;
import local.villagers.relations.Opinion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static local.villagers.mind.PlayerImpression.Evidence.*;
import static org.junit.jupiter.api.Assertions.*;

class SocialMindTest {
    private static final UUID PLAYER = new UUID(5, 5);
    @TempDir Path root;

    @Test void reputationSpreadsByGossipButWeakerThanExperience() {
        AgentMind victim = new AgentMind(UUID.randomUUID()), neighbour = new AgentMind(UUID.randomUUID());
        victim.observePlayer(PLAYER, PLAYER_HURT, 100);
        neighbour.hearAboutPlayer(victim.id(), victim.playerImpression(PLAYER));
        double heard = neighbour.playerImpression(PLAYER).trust();
        assertTrue(heard < PlayerImpression.prior(PLAYER).trust(), "being told about harm lowers trust");
        assertTrue(heard > victim.playerImpression(PLAYER).trust(), "but not as far as being hurt");
        assertEquals(0, neighbour.playerImpression(PLAYER).evidenceCount(), "hearsay is not evidence");
    }

    @Test void trustedTellersAreBelievedMore() {
        AgentMind victim = new AgentMind(UUID.randomUUID());
        victim.observePlayer(PLAYER, PLAYER_HURT, 100);
        AgentMind friend = new AgentMind(UUID.randomUUID()), stranger = new AgentMind(UUID.randomUUID());
        for (int i = 0; i < 4; i++) friend.witness(victim.id(), Opinion.Event.SHARED_FOOD, i);
        friend.hearAboutPlayer(victim.id(), victim.playerImpression(PLAYER));
        stranger.hearAboutPlayer(victim.id(), victim.playerImpression(PLAYER));
        assertTrue(friend.playerImpression(PLAYER).trust() < stranger.playerImpression(PLAYER).trust());
    }

    @Test void toldMemoriesBecomeKnownToThePlayer() {
        AgentMind mind = new AgentMind(UUID.randomUUID());
        Episode episode = mind.recordEpisode(Episode.Kind.CLAIMED_BED, 10, null, new Place("minecraft:overworld", 1, 2, 3), null, List.of());
        assertEquals(episode, mind.untoldTo(PLAYER));
        assertTrue(mind.reveal(episode.id(), PLAYER));
        assertFalse(mind.reveal(episode.id(), PLAYER), "already known");
        assertNull(mind.untoldTo(PLAYER));
        assertTrue(mind.episodes().getFirst().isKnownBy(PLAYER));
    }

    @Test void receptivenessFallsWithFearAndRisesWithTrust() {
        AgentMind mind = new AgentMind(UUID.randomUUID());
        double stranger = mind.receptivenessTo(PLAYER);
        mind.observePlayer(PLAYER, PLAYER_HURT, 0);
        assertTrue(mind.receptivenessTo(PLAYER) < stranger);
        UUID friend = new UUID(6, 6);
        mind.observePlayer(friend, USEFUL_GIFT, 0);
        mind.observePlayer(friend, USEFUL_GIFT, 700);
        assertTrue(mind.receptivenessTo(friend) > stranger);
    }

    @Test void lexiconAndOpinionsSurviveReloadAndSchemaFiveUpgradesEmpty() throws Exception {
        AgentMind mind = new AgentMind(UUID.randomUUID());
        mind.lexicon(Lexicon.invent(new Random(1)));
        mind.witness(new UUID(7, 7), Opinion.Event.PROTECTED, 3);
        AgentStore store = AgentStore.worldScoped(root);
        assertTrue(store.save(mind), store.lastError());
        AgentMind restored = AgentStore.worldScoped(root).load(mind.id());
        assertEquals(mind.lexicon(), restored.lexicon());
        assertEquals(mind.opinions(), restored.opinions());

        AgentMind older = new AgentMind(UUID.randomUUID());
        JsonObject legacy = new Gson().toJsonTree(older.snapshot()).getAsJsonObject();
        legacy.addProperty("schemaVersion", 5);
        legacy.remove("lexicon");
        legacy.remove("opinions");
        Path file = Files.createDirectories(root.resolve(older.id().toString())).resolve("mind.json");
        Files.writeString(file, legacy.toString());
        AgentMind upgraded = store.load(older.id());
        assertNull(store.lastError());
        assertTrue(upgraded.lexicon().isEmpty(), "gets a language from neighbours when next loaded in the world");
        assertTrue(upgraded.opinions().known().isEmpty());
    }

    @Test void villagersDoNotFormOpinionsOfThemselves() {
        AgentMind mind = new AgentMind(UUID.randomUUID());
        mind.witness(mind.id(), Opinion.Event.SHARED_FOOD, 0);
        assertTrue(mind.opinions().known().isEmpty());
    }
}
