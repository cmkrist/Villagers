package local.villagers.relations;

import java.util.List;
import java.util.Random;
import local.villagers.language.Concept;
import local.villagers.language.Lexicon;
import local.villagers.language.Utterance;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FoodRequestTest {
    private final Lexicon shared = Lexicon.invent(new Random(43));
    @Test void injuryPriorityRequiresTheActuallySpokenHurtWordToBeUnderstood(){
        var words=Utterance.of(shared,List.of(Concept.WANT,Concept.FOOD,Concept.HUNGRY,Concept.HURT),true);
        assertTrue(FoodRequest.understands(words,shared,Concept.WANT,Concept.FOOD,Concept.HUNGRY,Concept.HURT));
        var partial=new Lexicon(List.of(new Lexicon.Entry(Concept.WANT,shared.wordFor(Concept.WANT),1),
            new Lexicon.Entry(Concept.FOOD,shared.wordFor(Concept.FOOD),1),new Lexicon.Entry(Concept.HUNGRY,shared.wordFor(Concept.HUNGRY),1)));
        assertFalse(FoodRequest.understands(words,partial,Concept.WANT,Concept.FOOD,Concept.HUNGRY,Concept.HURT));
        assertFalse(FoodRequest.understands(words,shared,Concept.WANT,Concept.FOOD,Concept.HURT));
    }
    @Test void recoveryNeedCanJustifyARequestBelowOrdinaryHungerThreshold(){
        assertTrue(FoodRequest.dueForCare(100,0,15,1,20,false));
        assertFalse(FoodRequest.dueForCare(100,0,15,20,20,false));
        assertFalse(FoodRequest.dueForCare(100,101,15,1,20,false));
        assertFalse(FoodRequest.dueForCare(100,0,18,1,20,false));
    }

    @Test void requestsUseOnlyOwnNeedAndOneFiniteRequestBudget() {
        assertTrue(FoodRequest.due(100, 0, .5, false));
        assertFalse(FoodRequest.due(101, 100 + FoodRequest.COOLDOWN, .9, false));
        assertTrue(FoodRequest.due(700, 700, .9, false));
        assertFalse(FoodRequest.due(100, 0, .49, false));
        assertTrue(FoodRequest.due(100, 0, .35, true));
        assertFalse(FoodRequest.due(100, 0, Double.NaN, true));
        assertFalse(FoodRequest.due(Long.MAX_VALUE, 0, .9, false));
    }

    @Test void silentPartialOrForeignRequestsCannotTransferFood() {
        assertFalse(FoodRequest.understands(null, shared, Concept.WANT, Concept.FOOD));
        assertFalse(FoodRequest.understands(Utterance.of(shared, List.of(Concept.FOOD), true), shared, Concept.WANT, Concept.FOOD));
        var request = Utterance.of(shared, List.of(Concept.WANT, Concept.FOOD), true);
        assertTrue(FoodRequest.understands(request, shared, Concept.WANT, Concept.FOOD));
        assertFalse(FoodRequest.understands(request, Lexicon.empty(), Concept.WANT, Concept.FOOD));
        assertFalse(FoodRequest.understands(request, Lexicon.invent(new Random(44)), Concept.WANT, Concept.FOOD));
    }

    @Test void anUnspokenWordIsNotRecoveredFromIntentOrGrounding() {
        Lexicon onlyFood = new Lexicon(List.of(new Lexicon.Entry(Concept.FOOD, "mora", 1)));
        var incomplete = Utterance.of(onlyFood, List.of(Concept.WANT, Concept.FOOD), true);
        assertFalse(FoodRequest.understands(incomplete, onlyFood, Concept.WANT, Concept.FOOD));
    }

    @Test void aDifferentMeaningForTheSameSoundsCannotAuthorizeFood() {
        var request = Utterance.of(shared, List.of(Concept.WANT, Concept.FOOD), true);
        Lexicon confused = new Lexicon(List.of(
                new Lexicon.Entry(Concept.YES, shared.wordFor(Concept.WANT), 1),
                new Lexicon.Entry(Concept.FOOD, shared.wordFor(Concept.FOOD), 1)));
        assertFalse(FoodRequest.understands(request, confused, Concept.WANT, Concept.FOOD));
        var reversed = Utterance.of(shared, List.of(Concept.FOOD, Concept.WANT), true);
        assertFalse(FoodRequest.understands(reversed, shared, Concept.WANT, Concept.FOOD));
    }

    @Test void onlyAnActuallyUnderstoodNoCanCountAsRefusal() {
        var refusal = Utterance.of(shared, List.of(Concept.NO), true);
        assertTrue(FoodRequest.understands(refusal, shared, Concept.NO));
        assertFalse(FoodRequest.understands(refusal, Lexicon.empty(), Concept.NO));
        assertFalse(FoodRequest.understands(Utterance.of(shared, List.of(Concept.YES, Concept.FOOD), true), shared, Concept.NO));
    }

    @Test void physicalHandoffsNeedBothReachAndHearingAndSight() {
        assertTrue(FoodRequest.inReach(9, 12, true));
        assertFalse(FoodRequest.inReach(9.01, 12, true));
        assertFalse(FoodRequest.inReach(4, 1, true));
        assertFalse(FoodRequest.inReach(4, 12, false));
        assertFalse(FoodRequest.inReach(Double.NaN, 12, true));
    }

    @Test void anAudibleRequestDoesNotPermitARemoteHandoff() {
        assertTrue(FoodRequest.audible(36,8,true));
        assertFalse(FoodRequest.inReach(36,8,true));
        assertFalse(FoodRequest.audible(36,5,true));
        assertFalse(FoodRequest.audible(36,8,false));
        assertFalse(FoodRequest.audible(Double.NaN,8,true));
        assertFalse(FoodRequest.audible(257,32,true));
    }

    @Test void askingCanApproachVisibleLocalCompanyButCannotTransferAtApproachDistance() {
        assertTrue(FoodRequest.mayApproach(256,true,true));
        assertFalse(FoodRequest.inReach(256,32,true));
        assertFalse(FoodRequest.mayApproach(256.01,true,true));
        assertFalse(FoodRequest.mayApproach(4,false,true));
        assertFalse(FoodRequest.mayApproach(4,true,false));
        assertFalse(FoodRequest.mayApproach(Double.NaN,true,true));
        assertFalse(FoodRequest.mayApproach(-1,true,true));
    }
}
