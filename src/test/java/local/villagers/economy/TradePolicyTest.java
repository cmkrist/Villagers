package local.villagers.economy;

import java.util.List;
import java.util.Random;
import local.villagers.language.Concept;
import local.villagers.language.Lexicon;
import local.villagers.language.Utterance;
import local.villagers.relations.FoodRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TradePolicyTest {
    @Test void aProposalCanBeHeardAcrossACounterWithoutClaimingPhysicalTransferReach() {
        assertTrue(TradePolicy.canHearProposal(100,16,true));
        assertFalse(FoodRequest.inReach(100,16,true));
        assertTrue(TradePolicy.canHearProposal(256,32,true));
        assertFalse(TradePolicy.canHearProposal(257,32,true));
        assertFalse(TradePolicy.canHearProposal(10,3,true));
        assertFalse(TradePolicy.canHearProposal(4,16,false));
        assertFalse(TradePolicy.canHearProposal(-1,16,true));
        assertFalse(TradePolicy.canHearProposal(Double.NaN,16,true));
        assertFalse(TradePolicy.canHearProposal(4,Double.POSITIVE_INFINITY,true));
        assertFalse(TradePolicy.canHearProposal(0,0,true));
    }
    @Test void quotesReflectGreedButFearAndLowMoodNeverDiscountConsent() {
        assertEquals(1,TradePolicy.ask(false,false,0,0));assertEquals(2,TradePolicy.ask(true,false,0,0));
        assertEquals(0,TradePolicy.ask(false,true,1,0));assertEquals(0,TradePolicy.ask(false,false,1,.6));
        assertEquals(0,TradePolicy.ask(false,false,-.26,0));assertEquals(0,TradePolicy.ask(false,false,Double.NaN,0));
    }
    @Test void unmetHungerCanFundProductionBeforeTheEmergencyThresholdWhileSellersRetainBodySafety() {
        assertEquals(1,TradePolicy.bid(false,14));assertEquals(2,TradePolicy.bid(false,13));assertEquals(2,TradePolicy.bid(false,10));
        assertEquals(2,TradePolicy.bid(false,6));assertEquals(2,TradePolicy.bid(true,16));
        assertTrue(TradePolicy.sellerSafe(13,20,20,.5));assertFalse(TradePolicy.sellerSafe(12,20,20,.5));
        assertFalse(TradePolicy.sellerSafe(16,17,20,.5));assertFalse(TradePolicy.sellerSafe(16,20,20,.75));
    }
    @Test void foodBidsStayBoundedAndRejectInvalidBodyStates() {
        for(int food=0;food<=20;food++)assertTrue(TradePolicy.bid(false,food)>=1 && TradePolicy.bid(false,food)<=2);
        assertEquals(0,TradePolicy.bid(false,-1));assertEquals(0,TradePolicy.bid(false,21));
        assertEquals(0,TradePolicy.bid(true,-1));assertEquals(0,TradePolicy.bid(true,21));
    }
    @Test void aQuoteMustCommunicateEveryCoinAndCannotUseIntentToFillMissingWords() {
        var language=Lexicon.invent(new Random(51));
        var twoCoins=List.of(Concept.TRADE,Concept.HOE,Concept.EMERALD,Concept.EMERALD);
        var words=Utterance.of(language,twoCoins,true);
        assertTrue(FoodRequest.understands(words,language,twoCoins.toArray(Concept[]::new)));
        assertFalse(FoodRequest.understands(words,language,Concept.TRADE,Concept.HOE,Concept.EMERALD));
        assertFalse(FoodRequest.understands(words,Lexicon.empty(),twoCoins.toArray(Concept[]::new)));
        assertFalse(FoodRequest.understands(Utterance.of(language,List.of(Concept.YES),true),language,Concept.YES,Concept.TRADE));
    }
    @Test void everyItemInABundleMustActuallyBeSpokenAndDecoded() {
        var language=Lexicon.invent(new Random(51));
        var bundle=List.of(Concept.TRADE,Concept.CARROT,Concept.CARROT,Concept.EMERALD,Concept.EMERALD);
        var quote=Utterance.of(language,bundle,true);
        assertTrue(FoodRequest.understands(quote,language,bundle.toArray(Concept[]::new)));
        assertFalse(FoodRequest.understands(Utterance.of(language,List.of(Concept.TRADE,Concept.CARROT,Concept.EMERALD,Concept.EMERALD),true),language,bundle.toArray(Concept[]::new)));
        assertFalse(FoodRequest.understands(quote,Lexicon.empty(),bundle.toArray(Concept[]::new)));
    }
}
