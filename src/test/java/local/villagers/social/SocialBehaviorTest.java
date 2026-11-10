package local.villagers.social;

import local.villagers.mind.Observation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SocialBehaviorTest {
    @Test void survivalAndBodyNeedsPreemptAttention() {
        assertTrue(SocialBehavior.mayEngage(observe(.2, .2, false, false), false, false));
        assertFalse(SocialBehavior.mayEngage(observe(.8, .2, false, false), false, false));
        assertFalse(SocialBehavior.mayEngage(observe(.2, .8, false, false), false, false));
        assertFalse(SocialBehavior.mayEngage(observe(.2, .2, true, false), false, false));
        assertFalse(SocialBehavior.mayEngage(observe(.2, .2, false, true), false, false));
        assertFalse(SocialBehavior.mayEngage(observe(.2, .2, false, false), true, false));
        assertFalse(SocialBehavior.mayEngage(observe(.2, .2, false, false), false, true));
    }
    @Test void trustedAttentionCannotBecomeUnboundedFollowing() {
        assertEquals(0, SocialBehavior.approachStep(2));
        assertEquals(0, SocialBehavior.approachStep(3));
        assertEquals(1, SocialBehavior.approachStep(4));
        assertEquals(2, SocialBehavior.approachStep(100));
        assertFalse(SocialBehavior.withinHome(28.001));
        assertTrue(SocialBehavior.withinHome(28));
        assertTrue(SocialBehavior.EPISODE_TICKS <= 40);
        assertEquals(200, SocialBehavior.EPISODE_INTERVAL_TICKS);
        assertFalse(SocialBehavior.episodeDue(199, 200));
    }
    private static Observation observe(double hunger, double fatigue, boolean danger, boolean night) {
        return new Observation(hunger, fatigue, .2, 1, danger, night, true, true);
    }
}
