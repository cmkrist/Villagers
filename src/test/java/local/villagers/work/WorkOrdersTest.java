package local.villagers.work;

import java.util.*;
import local.villagers.mind.Place;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static local.villagers.work.WorkOrders.*;

class WorkOrdersTest {
    final UUID buyer=new UUID(201,1),worker=new UUID(201,2);
    final Terms terms=new Terms(buyer,worker,Task.WOODEN_HOE,1,2,100,2500);
    Production made(long tick){return new Production(tick,new Place("minecraft:overworld",1,81,2),1);}
    @Test void independentlySavedTermsMustMatchAndPartialCommitsAuthorizeNothing(){
        var a=WorkOrders.empty().accept(terms);var b=WorkOrders.empty();
        assertFalse(mutual(a.open(),b.open(),100));b=b.accept(terms);assertTrue(mutual(a.open(),b.open(),100));
        var changed=new Terms(buyer,worker,Task.WOODEN_HOE,1,1,100,2500);
        assertFalse(mutual(a.open(),WorkOrders.empty().accept(changed).open(),100));
        assertFalse(mutual(a.open(),b.open(),99));assertFalse(mutual(a.open(),b.open(),2500));
        var active=b;assertThrows(IllegalStateException.class,()->active.accept(new Terms(buyer,worker,Task.BREAD,1,1,200,2600)));
    }
    @Test void promisesAndProductionAloneNeverAwardCompletionOrPayment(){
        var b=WorkOrders.empty().accept(terms);assertEquals(State.AGREED,b.open().state());
        var promised=b;assertThrows(IllegalStateException.class,()->promised.complete(terms,worker,new Settlement(200,1,0,0,2),200));
        b=b.produced(terms,made(200),200);assertEquals(State.READY,b.open().state());assertNull(b.open().settlement());
        var done=b.complete(terms,worker,new Settlement(300,1,0,0,2),300);assertEquals(State.COMPLETED,done.find(terms).state());assertNull(done.open());
        assertThrows(IllegalStateException.class,()->done.complete(terms,worker,new Settlement(301,1,0,0,2),301));
    }
    @Test void settlementRequiresExactOwnedGoodsAndCoinsAndNeverCopiesPrivateProductionToCustomer(){
        var b=WorkOrders.empty().accept(terms).produced(terms,made(200),200);
        assertThrows(IllegalStateException.class,()->b.complete(terms,worker,new Settlement(300,1,0,0,1),300));
        assertThrows(IllegalStateException.class,()->b.complete(terms,worker,new Settlement(300,1,1,0,2),300));
        var customer=WorkOrders.empty().accept(terms).complete(terms,buyer,new Settlement(300,0,1,2,0),300);
        assertNull(customer.find(terms).production());assertEquals(State.COMPLETED,customer.find(terms).state());
    }
    @Test void interruptionsPreserveWorkCreditButNeverAwardACompletionOrMalice(){
        var b=WorkOrders.empty().accept(terms).state(terms,State.WORKING,Reason.NONE,110).produced(terms,made(120),120);
        b=b.state(terms,State.PAUSED,Reason.UNSAFE,130);for(int i=131;i<150;i++)b=b.state(terms,State.PAUSED,Reason.UNSAFE,i);
        assertEquals(1,b.open().interruptions());assertEquals(120,b.open().production().tick());assertNull(b.open().settlement());
        b=b.resume(terms,150);assertEquals(State.READY,b.open().state());
        assertEquals(State.EXPIRED,b.expire(2500).find(terms).state());
        var interrupted=b;assertThrows(IllegalStateException.class,()->interrupted.expire(2500).complete(terms,worker,new Settlement(2500,1,0,0,2),2500));
    }
    @Test void cancellationOrExpiryInvalidatesJointConsentWithoutMovingResources(){
        var a=WorkOrders.empty().accept(terms);var b=WorkOrders.empty().accept(terms);
        a=a.state(terms,State.CANCELLED,Reason.WITHDREW,200);assertFalse(mutual(a.open(),b.open(),201));
        assertNull(a.find(terms).settlement());assertNull(a.find(terms).production());
        b=b.state(terms,State.PAUSED,Reason.STATION,200).expire(2500);
        assertEquals(State.EXPIRED,b.find(terms).state());assertEquals(Reason.STATION,b.find(terms).reason());assertEquals(1,b.find(terms).interruptions());
    }
    @Test void historyIsFiniteAndInvalidTermsCannotInventQuantitiesOrDeadlines(){
        assertThrows(IllegalArgumentException.class,()->new Terms(buyer,worker,Task.BREAD,2,1,0,2400));
        assertThrows(IllegalArgumentException.class,()->new Terms(buyer,worker,Task.BREAD,1,3,0,2400));
        assertThrows(IllegalArgumentException.class,()->new Terms(buyer,worker,Task.BREAD,1,1,0,2401));
        var b=WorkOrders.empty();for(int i=0;i<40;i++){var t=new Terms(buyer,worker,Task.BREAD,1,1,3000L*i,3000L*i+2400);b=b.accept(t).expire(t.dueAt());}
        assertEquals(MAX_HISTORY,b.history().size());assertNull(b.open());
    }
}
