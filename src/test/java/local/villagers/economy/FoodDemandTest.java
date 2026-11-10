package local.villagers.economy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FoodDemandTest {
    @Test void paidRequestPreparesTheSameBoundedStockButCannotAuthorizeAGiftOrGiftWithdrawal() {
        var signal=new FoodDemand();var peer=new UUID(7,1);
        assertTrue(signal.hearTrade(peer,100));
        assertEquals(5,signal.extraNutrition(101));
        assertEquals(FoodDemand.Intent.TRADE,signal.intent(101));
        assertNull(signal.giftRequester(101));
        signal.fulfilled(peer);signal.withdrawn(peer);
        assertEquals(peer,signal.requester(102));
        signal.tradeFulfilled(new UUID(7,2));assertEquals(peer,signal.requester(103));
        signal.tradeFulfilled(peer);assertNull(signal.requester(104));assertNull(signal.intent(104));
        assertFalse(signal.hearTrade(peer,100));
    }

    @Test void onlyALaterActualRequestFromTheSamePeerCanChangeItsIntent() {
        var signal=new FoodDemand();var peer=new UUID(8,1);
        signal.hearTrade(peer,100);
        assertFalse(signal.hear(peer,100));assertFalse(signal.hear(new UUID(8,2),101));
        assertEquals(FoodDemand.Intent.TRADE,signal.intent(101));
        assertTrue(signal.hear(peer,102));assertEquals(peer,signal.giftRequester(103));
        signal.tradeFulfilled(peer);assertEquals(peer,signal.giftRequester(104));
        assertTrue(signal.hearTrade(peer,105));assertNull(signal.giftRequester(106));
        assertNull(signal.intent(105+FoodDemand.LIFETIME));
        assertTrue(signal.hearTrade(peer,105+FoodDemand.LIFETIME+1));
        assertNull(signal.intent(20));assertNull(signal.giftRequester(20));
    }

    @Test void duplicateAndCrowdedRequestsCannotAccumulateProductionDebt() {
        var signal = new FoodDemand(); var peer = new UUID(1, 1);
        assertEquals(0, signal.extraNutrition(100));
        assertTrue(signal.hear(peer, 100));
        assertFalse(signal.hear(peer, 100));
        for (int i = 2; i < 100; i++) assertFalse(signal.hear(new UUID(1, i), 101));
        assertEquals(5, signal.extraNutrition(101));
    }

    @Test void expiredOrRewoundSignalsDoNotDriveWorkAndNewHearingCanWakeIt() {
        var signal = new FoodDemand(); var peer = new UUID(2, 1);
        signal.hear(peer, 100);
        assertEquals(5, signal.extraNutrition(100+FoodDemand.LIFETIME-1));
        assertEquals(0, signal.extraNutrition(100+FoodDemand.LIFETIME));
        assertTrue(signal.hear(peer, 100+FoodDemand.LIFETIME+1));
        assertEquals(0, signal.extraNutrition(20));
        assertTrue(signal.hear(peer, 30));
    }

    @Test void anotherActualRequestRefreshesOnlyTheSameBoundedReserve() {
        var signal = new FoodDemand(); var peer = new UUID(3, 1);
        signal.hear(peer, 100);
        assertTrue(signal.hear(peer, 700));
        assertEquals(5, signal.extraNutrition(100+FoodDemand.LIFETIME));
        assertEquals(0, signal.extraNutrition(700+FoodDemand.LIFETIME));
    }

    @Test void onlyActualTransferToTheRequestingNeighbourFulfillsTheSignal() {
        var signal = new FoodDemand(); var peer = new UUID(4, 1);
        signal.hear(peer, 100);
        signal.fulfilled(new UUID(4, 2)); assertEquals(5, signal.extraNutrition(200));
        signal.fulfilled(peer); assertEquals(0, signal.extraNutrition(200));
        assertFalse(signal.hear(null, 200)); assertFalse(signal.hear(peer, -1));
        assertFalse(signal.hear(peer, Long.MAX_VALUE));
    }

    @Test void replayOfAFulfilledRequestCannotCreateAnotherProductionGoal() {
        var signal = new FoodDemand(); var peer = new UUID(5, 1);
        assertTrue(signal.hear(peer, 0));
        signal.fulfilled(peer);
        assertFalse(signal.hear(peer, 0));
        assertEquals(0, signal.extraNutrition(0));
        assertTrue(signal.hear(peer, 600), "A later real request is a new signal");
    }

    @Test void deliveryNamesOnlyTheActuallyHeardRequesterAndDropsWithdrawnOrExpiredSignals() {
        var signal=new FoodDemand();var peer=new UUID(6,1);
        assertNull(signal.requester(0));signal.hear(peer,100);
        assertEquals(peer,signal.requester(101));
        signal.withdrawn(new UUID(6,2));assertEquals(peer,signal.requester(102));
        signal.withdrawn(peer);assertNull(signal.requester(103));
        assertFalse(signal.hear(peer,100));
        signal.hear(peer,700);assertNull(signal.requester(700+FoodDemand.LIFETIME));
    }
}
