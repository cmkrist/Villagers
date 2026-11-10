package local.villagers.mind;

import org.junit.jupiter.api.Test;
import java.util.Random;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AgentMindTest {
    private static final UUID ID = UUID.fromString("12345678-1234-1234-1234-123456789abc");

    @Test void masksRemainValidDuringExplorationAndDangerOverridesEverything() {
        AgentMind mind = new AgentMind(ID);
        Random random = new Random(18);
        Observation noFoodAtNight = new Observation(0.8, 0.7, 0.9, 0.2, false, true, false, false);
        Observation danger = new Observation(1, 1, 1, 0.01, true, true, true, true);
        for (int i = 0; i < 2_000; i++) {
            assertEquals(Action.REST, mind.choose(noFoodAtNight, random));
            assertEquals(Action.FLEE, mind.choose(danger, random));
        }
        mind.learn(danger, Action.FLEE, noFoodAtNight);
        assertEquals(0, mind.learningUpdates(), "Hard-coded survival shield must not masquerade as learning");
    }

    @Test void hungryAgentWithoutKnownFoodSearchesInsteadOfBeingForcedToRest() {
        AgentMind mind = new AgentMind(ID);
        Observation stranded = new Observation(1, 0.2, 1, 1, false, false, false, true);
        assertFalse(mind.allowed(Action.FORAGE, stranded), "No phantom food target");
        assertTrue(mind.allowed(Action.EXPLORE, stranded), "Hunger must not disable food search");
        Random random = new Random(28);
        long searches = java.util.stream.IntStream.range(0, 1_000)
                .filter(i -> mind.choose(stranded, random) == Action.EXPLORE).count();
        assertTrue(searches > 900, "An available search action must also outrank idle rest here");
        Observation danger = new Observation(1, 0.2, 1, 1, true, false, false, true);
        assertEquals(Action.FLEE, mind.choose(danger, random), "Searching cannot override danger");
    }

    @Test void actualDriveReliefChangesLaterChoicesComparedWithFrozenLearning() {
        AgentMind learner = new AgentMind(ID);
        AgentMind frozen = new AgentMind(ID);
        Observation before = new Observation(0.4, 0.6, 0.1, 1, false, false, true, true);
        Observation afterEating = new Observation(0, 0.6, 0.1, 1, false, false, true, true);
        for (int i = 0; i < 20; i++) learner.learn(before, Action.FORAGE, afterEating);
        int learnedForages = 0;
        int frozenForages = 0;
        Random learnerRandom = new Random(42);
        Random frozenRandom = new Random(42);
        for (int i = 0; i < 1_000; i++) {
            if (learner.choose(before, learnerRandom) == Action.FORAGE) learnedForages++;
            if (frozen.choose(before, frozenRandom) == Action.FORAGE) frozenForages++;
        }
        assertTrue(learnedForages > 900, "Successful eating should become a useful preference");
        assertTrue(frozenForages < 100, "Identical untrained agent must retain its different preference");
    }

    @Test void unrelatedDriveReliefCannotRewardTheWrongAction() {
        AgentMind mind = new AgentMind(ID);
        Observation before = new Observation(0.6, 0.3, 0.6, 0.8, false, false, true, true);
        Observation unrelatedFood = new Observation(0, 0.3, 0.6, 1, false, false, true, true);
        mind.learn(before, Action.SOCIALIZE, unrelatedFood);
        assertTrue(mind.memories().getLast().reward() <= 0, "Food and ambient healing cannot reward socializing");
    }

    @Test void experienceIsContextualAndBounded() {
        AgentMind mind = new AgentMind(ID);
        Observation day = new Observation(0.4, 0.6, 0.1, 1, false, false, true, true);
        Observation fed = new Observation(0, 0.6, 0.1, 1, false, false, true, true);
        for (int i = 0; i < 100; i++) mind.learn(day, Action.FORAGE, fed);
        assertEquals(48, mind.memories().size());
        Observation night = new Observation(0.4, 0.6, 0.1, 1, false, true, true, true);
        Random random = new Random(77);
        long rests = java.util.stream.IntStream.range(0, 1_000).filter(i -> mind.choose(night, random) == Action.REST).count();
        assertTrue(rests > 900, "Daytime experience must not overwrite an untrained nighttime context");
    }

    @Test void snapshotsAreDetachedAndImmutable() {
        AgentMind mind = new AgentMind(ID);
        AgentMind.Snapshot snapshot = mind.snapshot();
        snapshot.values()[0][0] = 99;
        assertEquals(0, snapshot.values()[0][0]);
        mind.advanceDrives(200);
        mind.remember("Observed a real interaction");
        assertEquals(0.2, snapshot.hunger());
        assertTrue(snapshot.memories().isEmpty());
    }

    @Test void rejectsNonFiniteObservationAndKeepsNeedsBounded() {
        assertThrows(IllegalArgumentException.class, () -> new Observation(Double.NaN, 0, 0, 1, false, false, false, false));
        Observation normalized = new Observation(-1, 3, 0, 2, false, false, false, false);
        assertEquals(0, normalized.hunger());
        assertEquals(1, normalized.fatigue());
        assertEquals(1, normalized.health());
        AgentMind mind = new AgentMind(ID);
        mind.advanceDrives(Long.MAX_VALUE);
        for (int i = 0; i < 100; i++) { mind.eat(5, 6f); mind.rest(); mind.socialize(); }
        assertEquals(0, mind.hunger());
        assertEquals(0, mind.fatigue());
        assertEquals(0, mind.loneliness());
        assertThrows(IllegalArgumentException.class, () -> mind.advanceDrives(-1));
    }
}
