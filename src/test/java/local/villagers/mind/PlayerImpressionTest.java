package local.villagers.mind;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static local.villagers.mind.PlayerImpression.Evidence.*;
import static local.villagers.mind.PlayerImpression.Reaction.*;
import static org.junit.jupiter.api.Assertions.*;

class PlayerImpressionTest {
    private static final UUID PLAYER = UUID.fromString("12345678-0000-0000-0000-000000000001");
    private static final UUID OTHER = UUID.fromString("12345678-0000-0000-0000-000000000002");
    private static final Observation CALM = new Observation(.2, .2, .3, 1, false, false, false, true);
    private static AgentMind mind() { return new AgentMind(UUID.fromString("22222222-3333-4444-5555-666666666666")); }

    @Test void initialReverenceIsUncertainAndLookupsArePure() {
        AgentMind mind = mind();
        PlayerImpression initial = mind.playerImpression(PLAYER);
        assertTrue(initial.awe() > .7);
        assertTrue(initial.confidence() < .2);
        assertTrue(initial.trust() < .3);
        assertEquals(ATTEND, initial.reaction(CALM));
        assertNotEquals(APPROACH, initial.reaction(CALM));
        for (int i = 0; i < 100; i++) mind.playerImpression(new UUID(0, i));
        assertTrue(mind.impressions().isEmpty(), "Read-only frozen-policy lookups cannot add experience");
        assertTrue(mind.memories().isEmpty());
    }

    @Test void realHarmReversesTrustInThatPlayerWithoutGlobalReputation() {
        AgentMind mind = mind();
        assertTrue(mind.observePlayer(PLAYER, USEFUL_GIFT, 0));
        assertTrue(mind.observePlayer(PLAYER, USEFUL_GIFT, 600));
        assertEquals(APPROACH, mind.playerImpression(PLAYER).reaction(CALM));
        PlayerImpression untouched = mind.playerImpression(OTHER);
        assertTrue(mind.observePlayer(PLAYER, PLAYER_HURT, 601));
        assertEquals(AVOID, mind.playerImpression(PLAYER).reaction(CALM));
        assertEquals(untouched, mind.playerImpression(OTHER));
        assertNotEquals(mind.playerImpression(PLAYER).awe(), mind.playerImpression(PLAYER).trust());
    }

    @Test void repeatedHarmCanOverturnTheInitialPowerImpression() {
        AgentMind mind = mind();
        double initialAwe = mind.playerImpression(PLAYER).awe();
        mind.observePlayer(PLAYER, PLAYER_HURT, 0);
        assertTrue(mind.playerImpression(PLAYER).awe() < initialAwe);
        mind.observePlayer(PLAYER, PLAYER_HURT, 1);
        assertTrue(mind.playerImpression(PLAYER).awe() < .6, "Initial reverence has no permanent floor");
        for (int tick = 2; tick < 10; tick++) mind.observePlayer(PLAYER, PLAYER_HURT, tick);
        assertEquals(0, mind.playerImpression(PLAYER).awe());
        assertEquals(AVOID, mind.playerImpression(PLAYER).reaction(CALM));
    }

    @Test void exposureAndGiftCooldownsPreventSpamButDoNotDelayHarm() {
        AgentMind mind = mind();
        assertTrue(mind.observePlayer(PLAYER, EXPOSURE, 0));
        assertTrue(mind.observePlayer(PLAYER, USEFUL_GIFT, 0));
        PlayerImpression afterGift = mind.playerImpression(PLAYER);
        for (int i = 1; i < 100; i++) {
            assertFalse(mind.observePlayer(PLAYER, USEFUL_GIFT, i));
            assertFalse(mind.observePlayer(PLAYER, EXPOSURE, i));
        }
        assertEquals(afterGift, mind.playerImpression(PLAYER));
        assertTrue(mind.observePlayer(PLAYER, PLAYER_HURT, 100));
        assertTrue(mind.observePlayer(PLAYER, PLAYER_HURT, 100), "Distinct actual hits need no cooldown");
        assertEquals(AVOID, mind.playerImpression(PLAYER).reaction(CALM));
        assertFalse(mind.observePlayer(PLAYER, PLAYER_HURT, 99), "Backdated evidence must not rewrite chronology");
        assertFalse(mind.observePlayer(PLAYER, USEFUL_GIFT, 599));
        assertTrue(mind.observePlayer(PLAYER, USEFUL_GIFT, 600));
    }

    @Test void relationshipCanRecoverFromHarmWithRepeatedGroundedPositiveEvidence() {
        AgentMind mind = mind();
        mind.observePlayer(PLAYER, PLAYER_HURT, 0);
        assertEquals(AVOID, mind.playerImpression(PLAYER).reaction(CALM));
        for (int i = 1; i <= 10; i++) mind.observePlayer(PLAYER, WITNESSED_PROTECTION, i * 200L);
        assertEquals(APPROACH, mind.playerImpression(PLAYER).reaction(CALM));
        assertTrue(mind.playerImpression(PLAYER).confidence() > .5);
    }

    @Test void playerMemoryAndPerPlayerEvidenceAreBounded() {
        AgentMind mind = mind();
        for (int i = 0; i < 80; i++) mind.observePlayer(PLAYER, EXPOSURE, i * 200L);
        assertEquals(PlayerImpression.MAX_NOTES, mind.playerImpression(PLAYER).notes().size());
        assertTrue(mind.playerImpression(PLAYER).trust() <= .25, "Familiarity alone is not strong trust");
        for (int i = 0; i < 64; i++) mind.observePlayer(new UUID(1, i), EXPOSURE, 20_000L + i);
        assertEquals(AgentMind.MAX_PLAYER_IMPRESSIONS, mind.impressions().size());
        assertEquals(0, mind.playerImpression(PLAYER).evidenceCount(), "Evicted players return to the uncertain prior");
        assertThrows(UnsupportedOperationException.class, () -> mind.impressions().clear());
        assertTrue(mind.memories().size() <= 48);
    }

    @Test void urgentNeedsNightAndDangerOverrideFriendlyApproach() {
        AgentMind mind = mind();
        mind.observePlayer(PLAYER, USEFUL_GIFT, 0);
        mind.observePlayer(PLAYER, USEFUL_GIFT, 600);
        PlayerImpression impression = mind.playerImpression(PLAYER);
        assertEquals(APPROACH, impression.reaction(CALM));
        assertEquals(NEUTRAL, impression.reaction(new Observation(1, .2, .2, 1, false, false, false, true)));
        assertEquals(NEUTRAL, impression.reaction(new Observation(.2, 1, .2, 1, false, false, false, true)));
        assertEquals(NEUTRAL, impression.reaction(new Observation(.2, .2, .2, 1, false, true, false, true)));
        assertEquals(NEUTRAL, impression.reaction(new Observation(.2, .2, .2, 1, true, false, false, true)));
    }

    @Test void evidenceClockSurvivesSnapshotAndCannotRunBackwards() {
        AgentMind original = mind();
        assertFalse(original.observePlayer(PLAYER, EXPOSURE, -1));
        assertTrue(original.observePlayer(PLAYER, USEFUL_GIFT, 500));
        AgentMind restored = AgentMind.fromSnapshot(original.snapshot());
        assertFalse(restored.observePlayer(PLAYER, USEFUL_GIFT, 600));
        assertFalse(restored.observePlayer(PLAYER, PLAYER_HURT, 499));
        assertTrue(restored.observePlayer(PLAYER, PLAYER_HURT, 500));
    }
}
