package local.villagers.mind;

import java.util.*;
import local.villagers.language.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeliefsTest {
    private final UUID speaker = new UUID(1,1), listener = new UUID(2,2);
    private Sentence.Interpretation heard(Clause c) { return new Sentence.Interpretation(List.of(c),1,0); }
    @Test void testimonyIsAttributedFallibleAndDoesNotBecomeObservationThroughRepetition() {
        var c = new Clause(Concept.I,Concept.HAVE,Concept.FOOD);
        var book = Beliefs.empty();
        for(int i=0;i<50;i++) book=book.hear(heard(c),speaker,listener,.5,i);
        assertEquals(1,book.entries().size());
        assertEquals(speaker,book.entries().getFirst().source());
        assertEquals(Beliefs.Evidence.HEARD,book.entries().getFirst().evidence());
        assertEquals(.6,book.entries().getFirst().confidence(),1e-9);
        assertTrue(book.foodLead(speaker,50)>0);
        assertEquals(0,book.foodLead(speaker,1000));
    }
    @Test void observationContradictsTestimonyWithoutErasingItsSourceAndRepeatingCannotOverruleIt() {
        var c = new Clause(Concept.I,Concept.HAVE,Concept.FOOD);
        var claim = Beliefs.Claim.resolve(c,speaker,listener);
        var absent = new Beliefs.Claim(speaker,Concept.HAVE,Concept.FOOD,null,true,false);
        var book=Beliefs.empty().hear(heard(c),speaker,listener,1,10).observe(absent,listener,20);
        assertTrue(book.entries().stream().anyMatch(e->e.source().equals(speaker)&&e.evidence()==Beliefs.Evidence.CONTRADICTED));
        book=book.hear(heard(c),speaker,listener,1,30);
        assertEquals(0,book.foodLead(speaker,30));
        book=book.observe(claim,listener,40);
        assertEquals(1,book.entries().stream().filter(e->e.evidence()==Beliefs.Evidence.OBSERVED).count());
        assertTrue(book.foodLead(speaker,40)>0);
    }
    @Test void unknownOrGenericSubjectsNeverRecoverNamedPeopleFromPrivateContext() {
        var generic=heard(new Clause(Concept.FRIEND,Concept.LIKE,Concept.YOU));
        assertTrue(Beliefs.empty().hear(generic,speaker,listener,1,0).entries().isEmpty());
        var unknown=new Sentence.Interpretation(List.of(),0,2);
        assertTrue(Beliefs.empty().hear(unknown,speaker,listener,1,0).entries().isEmpty());
    }
    @Test void deicticPeopleAreResolvedOnlyFromTheActualParticipants() {
        var claim=Beliefs.Claim.resolve(new Clause(Concept.I,Concept.LIKE,Concept.YOU),speaker,listener);
        assertEquals(speaker,claim.subject()); assertEquals(listener,claim.objectPerson());
    }
    @Test void promiseMustBeUnderstoodAndActualFulfilmentConsumesItOnce() {
        var book=Beliefs.empty().hear(heard(new Clause(Concept.I,Concept.PROMISE,Concept.FRIEND)),speaker,listener,0,10);
        assertTrue(book.promisedCompany(speaker,11));
        book=book.fulfilledCompany(speaker,listener,20);
        assertFalse(book.promisedCompany(speaker,21));
        assertEquals(Concept.MEET,book.entries().getFirst().claim().verb());
        assertEquals(Beliefs.Evidence.OBSERVED,book.entries().getFirst().evidence());
    }
    @Test void deniedInterpretationIsRetractedButDirectObservationIsRetained() {
        var wrong=heard(new Clause(Concept.I,Concept.WANT,Concept.BED));
        var book=Beliefs.empty().hear(wrong,speaker,listener,0,10).retract(wrong,speaker,listener);
        assertEquals(Beliefs.Evidence.CONTRADICTED,book.entries().getFirst().evidence());
    }
    @Test void boundedSourceRecordsExpireAndDoNotCreateRemoteKnowledge() {
        var book=Beliefs.empty();
        for(int i=0;i<200;i++)book=book.hear(heard(new Clause(Concept.I,Concept.HAVE,Concept.FOOD)),new UUID(3,i),listener,0,10);
        assertEquals(Beliefs.MAX_ENTRIES,book.entries().size());
        assertEquals(0,book.foodLead(new UUID(9,9),11));
        assertTrue(book.hear(heard(new Clause(Concept.I,Concept.HAVE,Concept.FOOD)),speaker,listener,0,3000).entries().size()<Beliefs.MAX_ENTRIES);
    }
    @Test void interpretedErrorsRemainErrorsInTheBeliefRatherThanRevealingIntention() {
        var book=Beliefs.empty().hear(heard(new Clause(Concept.I,Concept.WANT,Concept.BED)),speaker,listener,0,10);
        assertEquals(Concept.BED,book.entries().getFirst().claim().object());
        assertFalse(book.entries().stream().anyMatch(e->e.claim().object()==Concept.FOOD));
    }
    @Test void sourceCanWithdrawAClaimWithoutTheListenerStillFollowingItsOldFoodLead() {
        var book=Beliefs.empty().hear(heard(new Clause(Concept.I,Concept.HAVE,Concept.FOOD)),speaker,listener,0,10);
        book=book.hear(heard(new Clause(Concept.I,Concept.HAVE,Concept.FOOD,true,false)),speaker,listener,0,20);
        assertEquals(0,book.foodLead(speaker,21));
        assertTrue(book.entries().stream().anyMatch(e->e.evidence()==Beliefs.Evidence.SUPERSEDED));
    }

    @Test void repeatedRumorCannotRefreshItsAgeOrAccumulateConfidence(){
        var c=new Clause(Concept.I,Concept.HAVE,Concept.FOOD);
        var b=Beliefs.empty().hear(heard(c),speaker,listener,1,0);
        var original=b.entries().getFirst();
        for(int tick=10;tick<600;tick+=10)b=b.hear(heard(c),speaker,listener,1,tick);
        assertEquals(original,b.entries().getFirst());
        assertEquals(0,b.hear(heard(c),speaker,listener,1,600).foodLead(speaker,601));
    }
    @Test void aSharedRootThroughMultipleSpeakersIsNotIndependentCorroboration(){
        var root=new UUID(8,8);var relay=new UUID(9,9);var person=new UUID(7,7);
        var names=Referents.empty().establish("fern",Referents.Target.person(person),0);
        var c=Clause.named(names.resolve("fern"),Concept.HAVE,Concept.FOOD);
        var claim=Beliefs.Claim.resolve(c,speaker,listener);
        var b=Beliefs.empty().hear(heard(c),speaker,listener,1,0)
                .qualify(claim,speaker,listener,new PracticalQuestions.Source(root,false,true),200);
        double strength=b.foodLead(person,201);
        b=b.hear(heard(c),relay,listener,1,200).qualify(claim,relay,listener,new PracticalQuestions.Source(root,false,true),200);
        assertEquals(strength,b.foodLead(person,201),1e-9);
        assertTrue(b.entries().stream().allMatch(e->e.provenance().root().equals(root)&&e.evidence()==Beliefs.Evidence.HEARD));
    }
    @Test void understoodNegativeTestimonyReducesALeadButDoesNotInventAnObservation(){
        var positive=new Beliefs.Claim(speaker,Concept.HAVE,Concept.FOOD,null,false,false);
        var rumor=Beliefs.empty().hear(heard(new Clause(Concept.I,Concept.HAVE,Concept.FOOD)),speaker,listener,1,0);
        double prior=rumor.foodLead(speaker,11);
        var denial=Clause.named(new Referents.Reference("fern",Referents.Target.person(speaker)),Concept.HAVE,Concept.FOOD);
        denial=new Clause(denial.subject(),denial.verb(),denial.object(),true,false,denial.subjectReference(),null);
        var denied=rumor.hear(heard(denial),new UUID(5,5),listener,1,10);
        assertTrue(denied.foodLead(speaker,11)<prior);
        assertTrue(denied.entries().stream().noneMatch(e->e.evidence()==Beliefs.Evidence.OBSERVED));
        var observed=denied.observe(new Beliefs.Claim(speaker,Concept.HAVE,Concept.FOOD,null,true,false),listener,20);
        assertEquals(0,observed.foodLead(speaker,21));
        assertTrue(observed.entries().stream().anyMatch(e->e.evidence()==Beliefs.Evidence.CONTRADICTED&&e.claim().equals(positive)));
    }
}
