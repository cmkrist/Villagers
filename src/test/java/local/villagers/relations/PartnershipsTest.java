package local.villagers.relations;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PartnershipsTest {
    final UUID a=new UUID(81,1),b=new UUID(81,2),c=new UUID(81,3);
    Opinion friend(){return new Opinion(b,.6,.5,0,4,0,100);}
    @Test void activeCommitmentsAreExclusiveButFriendsAreNotPartnerships(){
        assertNull(Partnerships.empty().active());
        var p=Partnerships.empty().form(b,100);
        assertTrue(p.committedTo(b));assertFalse(p.committedTo(c));assertFalse(p.mayBegin(200));
        assertThrows(IllegalStateException.class,()->p.form(c,200));
        assertThrows(IllegalArgumentException.class,()->new Partnerships(List.of(p.active(),new Partnerships.Bond(c,200,-1,Partnerships.End.NONE,0,-1))));
    }
    @Test void separateSavedReceiptsMustMatchAndHalfCommitsNeverAuthorizeMutualActions(){
        var first=Partnerships.empty().form(b,100);var second=Partnerships.empty().form(a,100);
        assertTrue(Partnerships.mutual(a,first,b,second));
        assertFalse(Partnerships.mutual(a,first,b,Partnerships.empty()));
        assertFalse(Partnerships.mutual(a,first,b,Partnerships.empty().form(a,101)));
        assertFalse(Partnerships.mutual(a,first,b,Partnerships.empty().form(c,100)));
    }
    @Test void unilateralWithdrawalImmediatelyRevokesMutualConsentAndRetainsHistory(){
        var first=Partnerships.empty().form(b,100);var second=Partnerships.empty().form(a,100);
        var ended=first.shared(b,150).end(b,200,Partnerships.End.WITHDREW);
        assertFalse(Partnerships.mutual(a,ended,b,second));assertNull(ended.active());
        assertTrue(second.committedTo(a),"The other's uncorrected memory does not force continued consent");
        assertEquals(1,ended.history().getFirst().sharedVisits());assertEquals(200,ended.history().getFirst().endedAt());
        assertFalse(ended.mayBegin(2599));assertTrue(ended.mayBegin(2600));
    }
    @Test void sharedContactCannotBeAwardedByRepeatedTicksOrTravelBeforeAgreement(){
        var p=Partnerships.empty().form(b,100);assertEquals(p,p.shared(b,99));assertEquals(p,p.shared(c,200));
        p=p.shared(b,150);for(int t=151;t<550;t++)p=p.shared(b,t);
        assertEquals(1,p.active().sharedVisits());assertEquals(2,p.shared(b,550).active().sharedVisits());
        var ended=p.end(b,600,Partnerships.End.HEARD_WITHDRAWAL);assertEquals(ended,ended.shared(b,1000));
    }
    @Test void ownNeedsAffectionHistoryAndFreedomAreAllRequired(){
        assertTrue(Partnerships.willing(true,true,false,1,16,.2,false,friend(),.7,false));
        assertFalse(Partnerships.willing(false,true,false,1,16,.2,false,friend(),.7,false));
        assertFalse(Partnerships.willing(true,false,false,1,16,.2,false,friend(),.7,false));
        assertFalse(Partnerships.willing(true,true,true,1,16,.2,false,friend(),.7,false));
        assertFalse(Partnerships.willing(true,true,false,.5,16,.2,false,friend(),.7,false));
        assertFalse(Partnerships.willing(true,true,false,1,5,.2,false,friend(),.7,false));
        assertFalse(Partnerships.willing(true,true,false,1,16,.8,false,friend(),.7,false));
        assertFalse(Partnerships.willing(true,true,false,1,16,.2,true,friend(),.7,false));
        assertFalse(Partnerships.willing(true,true,false,1,16,.2,false,Opinion.neutral(b),.7,false));
        var gossipOnly=new Opinion(b,1,1,0,0,40,100);
        assertFalse(Partnerships.willing(true,true,false,1,16,.2,false,gossipOnly,.7,false));
        var fearful=new Opinion(b,.6,.5,.6,4,0,100);
        assertFalse(Partnerships.willing(true,true,false,1,16,.2,false,fearful,.7,false));assertTrue(Partnerships.wantsToEnd(fearful));
    }
    @Test void boundedHistoryEvictsEndedAccountsWithoutLosingTheActiveCommitment(){
        var p=Partnerships.empty();for(int i=0;i<20;i++)p=p.form(b,3000L*i).end(b,3000L*i+1,Partnerships.End.WITHDREW);
        p=p.form(c,60000);assertEquals(Partnerships.MAX_HISTORY,p.history().size());assertEquals(c,p.active().peer());
    }
    @Test void freshReciprocalPromisesCanReconcileAHalfSaveWhilePreservingPriorSharedHistory(){
        var first=Partnerships.empty().form(b,100).shared(b,200);var second=Partnerships.empty();
        assertFalse(Partnerships.mutual(a,first,b,second));
        first=first.agree(b,500);assertFalse(Partnerships.mutual(a,first,b,second),"One new promise cannot manufacture the missing consent");
        second=second.agree(a,500);assertTrue(Partnerships.mutual(a,first,b,second));
        assertEquals(2,first.history().size());assertEquals(Partnerships.End.RECONFIRMED,first.history().getFirst().end());
        assertEquals(1,first.history().getFirst().sharedVisits());assertEquals(0,first.active().sharedVisits());
        var committed=first;assertThrows(IllegalStateException.class,()->committed.agree(c,600));
        assertThrows(IllegalStateException.class,()->committed.agree(b,400));
    }
}
