package local.villagers.mood;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import local.villagers.genetics.Trait;
import local.villagers.genetics.Genome;
import local.villagers.genetics.TraitGene;
import local.villagers.genetics.Tendency;
import java.util.Arrays;
import local.villagers.mind.Action;
import local.villagers.mind.AgentMind;
import local.villagers.mind.AgentStore;
import local.villagers.mind.Episode;
import local.villagers.mind.Observation;
import local.villagers.mind.PlayerImpression;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class MoodTest {
    private static final UUID PLAYER = new UUID(1, 1);
    @TempDir java.nio.file.Path root;

    private static Episode episode(Episode.Kind kind, long tick) {
        return new Episode(tick, kind, tick, PLAYER, null, null, List.of());
    }

    @Test void wellFedRestedVillagersAreContentAndStarvingOnesMiserableWithReasons() {
        Mood fine = Mood.compute(0, 0, 0, 1, List.of(), 1000, Set.of());
        assertFalse(fine.low());
        Mood starving = Mood.compute(1, 0.8, 0.8, 0.4, List.of(), 1000, Set.of());
        assertEquals(Mood.Band.MISERABLE, starving.band());
        assertEquals("hungry", starving.reasons().getFirst());
    }

    @Test void recentEventsCountAndOldOnesFade() {
        long now = 100_000;
        Mood hurtRecently = Mood.compute(.3, .3, .3, 1, List.of(episode(Episode.Kind.HURT_BY_PLAYER, now - 1000)), now, Set.of());
        Mood hurtLongAgo = Mood.compute(.3, .3, .3, 1, List.of(episode(Episode.Kind.HURT_BY_PLAYER, now - Mood.RECENT_TICKS - 1)), now, Set.of());
        assertTrue(hurtRecently.value() < hurtLongAgo.value());
        assertTrue(hurtRecently.reasons().contains("was hurt by a player"));
        Mood gifted = Mood.compute(.3, .3, .3, 1, List.of(episode(Episode.Kind.RECEIVED_ITEM, now - 10),
                episode(Episode.Kind.PROTECTED_BY_PLAYER, now - 5)), now, Set.of());
        assertTrue(gifted.value() > hurtLongAgo.value());
    }

    @Test void traitsChangeWhatHurts() {
        Mood lonelyLoner = Mood.compute(0, 0, 1, 1, List.of(), 0, Set.of(Trait.LONER));
        Mood lonelyGossip = Mood.compute(0, 0, 1, 1, List.of(), 0, Set.of(Trait.GOSSIP));
        assertTrue(lonelyLoner.value() > lonelyGossip.value() + 0.2);
    }

    @Test void experienceIsBoundedAndShiftsExpressedTendencies() {
        Experience shifted = Experience.none();
        for (int i = 0; i < 1000; i++) shifted = shifted.after(Experience.Event.RESCUED);
        assertEquals(Experience.MAX_SHIFT, shifted.boldness(), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> new Experience(0.5, 0, 0));
        AgentMind coward = new AgentMind(UUID.randomUUID());
        double before = coward.expressedPersonality().boldness();
        coward.observePlayer(PLAYER, PlayerImpression.Evidence.PLAYER_HURT, 0);
        assertTrue(coward.expressedPersonality().boldness() < before);
        assertEquals(coward.personality(), new AgentMind(coward.id()).personality(), "inherited tendencies themselves don't change");
    }

    @Test void miserableVillagersSulkInsteadOfSeekingCompany() {
        // A random founder can be a loner/lazy villager that already prefers rest.
        // Hold inherited traits and tendencies fixed to isolate mood's effect.
        Genome neutral = new Genome(Arrays.stream(TraitGene.values())
                .map(g -> new Genome.TraitPair(g, TraitGene.Allele.NEUTRAL, TraitGene.Allele.NEUTRAL)).toList(),
                Arrays.stream(Tendency.values()).map(t -> new Genome.TendencyGenes(t,
                        new double[]{.5, .5, .5, .5}, new double[]{.5, .5, .5, .5})).toList());
        AgentMind mind = AgentMind.born(new UUID(2, 4), neutral);
        Observation lonely = new Observation(.2, .3, .6, 1, false, false, true, true);
        int happySocial = count(mind, lonely, Action.SOCIALIZE);
        mind.updateMood(0, 0.2); // Badly hurt.
        assertTrue(mind.mood().low());
        int sulkingSocial = count(mind, lonely, Action.SOCIALIZE);
        assertTrue(sulkingSocial < happySocial, sulkingSocial + " vs " + happySocial);
    }

    private static int count(AgentMind mind, Observation observation, Action action) {
        Map<Action, Integer> counts = new EnumMap<>(Action.class);
        Random random = new Random(3);
        for (int i = 0; i < 2000; i++) counts.merge(mind.choose(observation, random), 1, Integer::sum);
        return counts.getOrDefault(action, 0);
    }

    @Test void experienceSurvivesReload() {
        AgentMind mind = new AgentMind(UUID.randomUUID());
        mind.experience(Experience.Event.RESCUED);
        AgentStore store = AgentStore.worldScoped(root);
        assertTrue(store.save(mind), store.lastError());
        assertEquals(mind.experience(), AgentStore.worldScoped(root).load(mind.id()).experience());
    }
}
