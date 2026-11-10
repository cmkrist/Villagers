package local.villagers.social;

import java.util.List;
import java.util.UUID;
import local.villagers.mind.Observation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CompanionVisitTest {
    private static final UUID FRIEND=new UUID(0,1), STRANGER=new UUID(0,2), DISLIKED=new UUID(0,3);
    private static final List<CompanionVisit.Candidate> PEERS=List.of(
            new CompanionVisit.Candidate(FRIEND,.2,8,false),
            new CompanionVisit.Candidate(STRANGER,0,3,true),
            new CompanionVisit.Candidate(DISLIKED,-.4,1,false));
    @Test void relationshipsAndCuriosityChangeWhoIsVisitedWithoutReadingPeerNeeds() {
        assertEquals(FRIEND,new CompanionVisit().select(PEERS,0,0));
        assertEquals(STRANGER,new CompanionVisit().select(PEERS,1,0));
        assertNull(new CompanionVisit().select(List.of(PEERS.get(2)),1,0));
        assertThrows(IllegalArgumentException.class,()->new CompanionVisit.Candidate(FRIEND,0,17,true));
        assertThrows(IllegalArgumentException.class,()->new CompanionVisit().select(java.util.Collections.nCopies(17,PEERS.getFirst()),0,0));
    }
    @Test void visitTargetsStayStableAndRecentFriendsGiveOtherPeersATurn() {
        var visit=new CompanionVisit();
        assertEquals(FRIEND,visit.select(PEERS,0,0));
        assertEquals(FRIEND,visit.select(PEERS,1,10));
        visit.completed(FRIEND,50);
        assertFalse(visit.maySelect(449));assertNull(visit.select(PEERS,0,449));
        assertEquals(STRANGER,visit.select(PEERS,0,450));
        assertFalse(visit.mayReceive(FRIEND,1249));
        assertTrue(visit.mayReceive(FRIEND,1250));
    }
    @Test void contactRequiresARealContinuousQuietIntervalRatherThanTravelTime() {
        var visit=new CompanionVisit();visit.select(PEERS,0,0);
        assertFalse(visit.contact(0,false));assertFalse(visit.contact(100,true));
        assertFalse(visit.contact(110,true));assertFalse(visit.contact(120,true));
        assertFalse(visit.contact(130,true));assertTrue(visit.contact(140,true));
        assertFalse(visit.contact(150,false));assertFalse(visit.contact(160,true));
        assertFalse(visit.contact(200,true),"A missed think or a work interruption resets contact");
        visit.interrupt(210);assertFalse(visit.active());
        assertFalse(visit.contact(220,true));
    }
    @Test void TemperamentAffectsInitiativeButPhysicalNeedsAndDangerAlwaysWin() {
        var own=new Observation(.2,.2,.4,1,false,false,false,true);
        assertTrue(CompanionVisit.wantsVisit(own,1,true,false));
        assertFalse(CompanionVisit.wantsVisit(own,0,false,true));
        assertFalse(CompanionVisit.wantsVisit(new Observation(.8,.2,1,1,false,false,false,true),1,true,false));
        assertFalse(CompanionVisit.wantsVisit(new Observation(.2,.8,1,1,false,false,false,true),1,true,false));
        assertFalse(CompanionVisit.wantsVisit(new Observation(.2,.2,1,1,true,false,false,true),1,true,false));
        assertFalse(CompanionVisit.wantsVisit(new Observation(.2,.2,1,1,false,true,false,true),1,true,false));
    }
    @Test void refusalBacksOffAndReceiverDoesNotHaveAnUnlimitedAttentionBudget() {
        var visit=new CompanionVisit();visit.select(PEERS,0,0);visit.reject(10);
        assertFalse(visit.active());assertFalse(visit.maySelect(409));
        assertFalse(visit.mayReceive(STRANGER,409));assertTrue(visit.mayReceive(STRANGER,410));
        assertFalse(visit.mayReceive(FRIEND,1209));assertTrue(visit.mayReceive(FRIEND,1210));
    }
}
