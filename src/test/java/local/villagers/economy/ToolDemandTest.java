package local.villagers.economy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToolDemandTest {
    @Test void movingOwnedNutritionIntoTheBodyDoesNotDestroyCapacityButMissingEnergyStillStopsWork() {
        assertTrue(ToolDemand.mayProduce(true,true,false,15,0),"fifteen body plus fifteen stored nutrition totals thirty");
        assertTrue(ToolDemand.mayProduce(true,true,false,20,5),"after actually eating five, twenty body plus ten stored still totals thirty");
        assertFalse(ToolDemand.mayProduce(true,true,false,20,8),"twenty body plus seven stored falls below the28 floor");
        assertFalse(ToolDemand.mayProduce(true,true,false,16,5));
        assertFalse(ToolDemand.mayProduce(true,true,false,12,0));
        assertFalse(ToolDemand.mayProduce(true,false,false,20,0));
        assertFalse(ToolDemand.mayProduce(true,true,true,20,0));
        assertFalse(ToolDemand.mayProduce(false,true,false,20,0));
        assertFalse(ToolDemand.mayProduce(true,true,false,20,-1));
        assertFalse(ToolDemand.mayProduce(true,true,false,20,16));
    }
    @Test void oneActualCustomerCannotBecomeAnUnboundedQueueOrReplayedOrder() {
        var d=new ToolDemand();var a=new UUID(1,1);var b=new UUID(1,2);
        assertTrue(d.hear(a,100));assertFalse(d.hear(a,100));assertFalse(d.hear(b,101));
        d.fulfilled(b);assertEquals(a,d.requester(101));d.fulfilled(a);assertNull(d.requester(101));
        assertFalse(d.hear(a,100));assertTrue(d.hear(b,700));assertEquals(b,d.requester(701));
    }
    @Test void expiryAndClockRewindDropOnlyTheSignalAndLaterHearingCanWakeIt() {
        var d=new ToolDemand();var a=new UUID(2,1);
        assertFalse(d.hear(null,0));assertFalse(d.hear(a,-1));assertFalse(d.hear(a,Long.MAX_VALUE));
        d.hear(a,100);assertEquals(a,d.requester(100+ToolDemand.LIFETIME-1));assertNull(d.requester(100+ToolDemand.LIFETIME));
        d.hear(a,13000);assertNull(d.requester(20));assertTrue(d.hear(a,30));
    }
    @Test void optionalCustomerWorkNeverSpendsTheOwnToolOrEdibleReserveToSpeculate() {
        assertTrue(ToolDemand.mayProduce(true,true,false,13,0));
        assertFalse(ToolDemand.mayProduce(false,true,false,20,0));
        assertFalse(ToolDemand.mayProduce(true,false,false,20,0));
        assertFalse(ToolDemand.mayProduce(true,true,true,20,0));
        assertFalse(ToolDemand.mayProduce(true,true,false,12,0));
        assertFalse(ToolDemand.mayProduce(true,true,false,20,8));
    }
}
