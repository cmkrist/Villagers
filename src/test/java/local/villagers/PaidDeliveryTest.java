package local.villagers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaidDeliveryTest {
    @Test void readyInputsShareOneJourneyWithOtherGoodsWithoutStarvationOrForcedRetargeting() {
        assertEquals(MarketGoods.Good.WOOD,PaidDelivery.choose(false,false,true,null,0));
        var order=new MarketGoods.Good[]{MarketGoods.Good.FOOD,MarketGoods.Good.HOE,MarketGoods.Good.WOOD};
        for(int i=0;i<12;i++)assertEquals(order[i%3],PaidDelivery.choose(true,true,true,null,i));
        assertEquals(MarketGoods.Good.WOOD,PaidDelivery.choose(true,true,true,MarketGoods.Good.WOOD,0));
        assertEquals(MarketGoods.Good.HOE,PaidDelivery.choose(false,true,false,MarketGoods.Good.WOOD,0));
        assertNull(PaidDelivery.choose(false,false,false,null,0));
    }
    @Test void readyToolDoesNotWaitForFoodStockAndReadyFoodDoesNotWaitForATool() {
        assertEquals(MarketGoods.Good.HOE,PaidDelivery.choose(false,true,null,0));
        assertEquals(MarketGoods.Good.FOOD,PaidDelivery.choose(true,false,null,1));
        assertNull(PaidDelivery.choose(false,false,null,0));
    }
    @Test void actualActiveRouteSurvivesOtherGoodsBecomingReadyButFinishingAlternatesDomains() {
        assertEquals(MarketGoods.Good.HOE,PaidDelivery.choose(true,true,MarketGoods.Good.HOE,0));
        assertEquals(MarketGoods.Good.FOOD,PaidDelivery.choose(true,true,MarketGoods.Good.FOOD,1));
        for(int i=0;i<20;i++)assertEquals(i%2==0?MarketGoods.Good.FOOD:MarketGoods.Good.HOE,PaidDelivery.choose(true,true,null,i));
        assertEquals(MarketGoods.Good.HOE,PaidDelivery.choose(false,true,MarketGoods.Good.FOOD,0));
        assertNull(PaidDelivery.choose(false,false,MarketGoods.Good.HOE,1));
    }
}
