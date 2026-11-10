package local.villagers;

import java.util.UUID;
import local.villagers.mind.Action;
import local.villagers.mind.AgentMind;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActionLifecycleTest {
    @Test void aRejectedFamilyRouteCanBeRenderedWithoutCompletingAnAbsentAction() {
        var s=new Session(new AgentMind(new UUID(441,1)),0);
        s.action=Action.SOCIALIZE;s.familyCompany=new UUID(441,2);
        assertEquals("socialize",VillagerController.currentActivity(s));
        FamilyCompany.reject(s,23529);
        assertNull(s.action);assertNull(s.before);assertFalse(s.succeeded);
        assertFalse(VillagerController.attemptFinished(s,23529),"elapsed timeout cannot complete or train a cleared action");
        assertEquals("choosing next activity",VillagerController.currentActivity(s));
        assertEquals(0,s.mind.learningUpdates());
    }
    @Test void aNewActionAfterRejectionStillCompletesNormally() {
        var s=new Session(new AgentMind(new UUID(442,1)),0);
        s.action=Action.SOCIALIZE;FamilyCompany.reject(s,100);
        s.action=Action.EXPLORE;s.started=110;
        assertFalse(VillagerController.attemptFinished(s,120));
        assertTrue(VillagerController.attemptFinished(s,270));
        s.started=300;s.succeeded=true;
        assertTrue(VillagerController.attemptFinished(s,300));
        assertEquals("explore",VillagerController.currentActivity(s));
    }
}
