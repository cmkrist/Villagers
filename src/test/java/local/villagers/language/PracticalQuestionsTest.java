package local.villagers.language;

import java.util.*;
import local.villagers.mind.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PracticalQuestionsTest {
    final UUID observer=new UUID(11,1),seeker=new UUID(11,2),provider=new UUID(11,3),root=new UUID(11,4);
    final Lexicon language=BiomeDialect.foundingLanguage("minecraft:plains");
    Sentence.Interpretation hear(List<Clause> clauses,Lexicon speaker,Lexicon listener){var s=Sentence.speak(clauses,speaker);assertNotNull(s);return Sentence.interpret(s.sounds(),listener);}
    Referents names(){return Referents.empty().establish("fern",Referents.Target.person(provider),0).establish("willow",Referents.Target.person(root),0);}
    Beliefs.Claim claim(){return new Beliefs.Claim(provider,Concept.HAVE,Concept.FOOD,null,false,false);}
    @Test void questionFramesRequireBothMeaningAndGrammarAndRenderFromTheListenersParse(){
        for(var kind:List.of(PracticalQuestions.Kind.WHO_FOOD,PracticalQuestions.Kind.WHERE_FOOD,PracticalQuestions.Kind.SOURCE)){
            var heard=hear(PracticalQuestions.question(kind),language,language);assertEquals(kind,PracticalQuestions.kind(heard));assertTrue(heard.english().endsWith("?"));
            assertEquals(PracticalQuestions.Kind.OTHER,PracticalQuestions.kind(hear(PracticalQuestions.question(kind),language,BiomeDialect.foundingLanguage("minecraft:desert"))));
        }
        assertEquals(PracticalQuestions.Kind.OTHER,PracticalQuestions.kind(hear(List.of(new Clause(Concept.I,Concept.ASK,Concept.FOOD)),language,language)));
    }
    @Test void responderCanUseOnlyPersonalEvidenceAndExpressibleNames(){
        var book=Beliefs.empty().observe(claim(),observer,0);
        assertNull(PracticalQuestions.answer(PracticalQuestions.Kind.WHO_FOOD,book,observer,seeker,Referents.empty(),10));
        var answer=PracticalQuestions.answer(PracticalQuestions.Kind.WHO_FOOD,book,observer,seeker,names(),10);assertNotNull(answer);
        var parsed=hear(List.of(answer.clause()),language.withReferents(names()),language.withReferents(names()));
        assertEquals(claim(),PracticalQuestions.decodedAnswer(parsed,PracticalQuestions.Kind.WHO_FOOD,observer,seeker));
        assertNull(PracticalQuestions.decodedAnswer(parsed,PracticalQuestions.Kind.WHERE_FOOD,observer,seeker));
        var unknown=hear(List.of(answer.clause()),language.withReferents(names()),language);
        assertNull(PracticalQuestions.decodedAnswer(unknown,PracticalQuestions.Kind.WHO_FOOD,observer,seeker));
    }
    @Test void confidentlyWrongNameIsTheListenersPendingLeadRatherThanTheSpeakersIntention(){
        var own=language.withReferents(names());
        var mistaken=language.withReferents(Referents.empty().establish("fern",Referents.Target.person(root),0));
        var heard=hear(List.of(Clause.named(names().resolve("fern"),Concept.HAVE,Concept.FOOD)),own,mistaken);
        assertTrue(heard.complete());assertEquals(1,heard.confidence());
        assertEquals(root,PracticalQuestions.decodedAnswer(heard,PracticalQuestions.Kind.WHO_FOOD,observer,seeker).subject());
    }
    @Test void sourceIsDecodedFromSoundAndAReportedSightingNeverBecomesObservation(){
        var entry=Beliefs.empty().observe(claim(),observer,100).entries().getFirst();
        var explanation=PracticalQuestions.sourceReply(entry,observer,seeker,names(),300);
        var source=PracticalQuestions.decodedSource(hear(explanation,language,language),observer,seeker);
        assertEquals(new PracticalQuestions.Source(observer,true,true),source);
        var fact=hear(List.of(Clause.named(names().resolve("fern"),Concept.HAVE,Concept.FOOD)),language.withReferents(names()),language.withReferents(names()));
        var book=Beliefs.empty().hear(fact,observer,seeker,1,110).qualify(claim(),observer,seeker,source,300);
        var accepted=book.entries().getFirst();assertEquals(Beliefs.Evidence.HEARD,accepted.evidence());
        assertEquals(Beliefs.Basis.REPORTED_DIRECT,accepted.provenance().basis());assertEquals(observer,accepted.provenance().root());
        assertTrue(book.foodLead(provider,301)>0);
    }
    @Test void oldCircularAndUnresolvedAttributionsDoNotBecomeGoodLeads(){
        var fact=hear(List.of(Clause.named(names().resolve("fern"),Concept.HAVE,Concept.FOOD)),language.withReferents(names()),language.withReferents(names()));
        var book=Beliefs.empty().hear(fact,observer,seeker,1,10);
        assertEquals(0,book.qualify(claim(),observer,seeker,new PracticalQuestions.Source(seeker,false,true),210).foodLead(provider,211));
        assertEquals(0,book.qualify(claim(),observer,seeker,new PracticalQuestions.Source(root,false,false),210).foodLead(provider,211));
        assertTrue(book.qualify(claim(),observer,seeker,PracticalQuestions.Source.unknown(),210).foodLead(provider,211)<book.foodLead(provider,211));
        assertTrue(PracticalQuestions.unknown(hear(PracticalQuestions.unknown(Concept.FOOD),language,language),Concept.FOOD));
        assertTrue(Beliefs.empty().hear(hear(PracticalQuestions.unknown(Concept.FOOD),language,language),observer,seeker,1,0).foodLead(provider,1)==0);
    }
    @Test void relaysMustSpeakTheirSourceAndCannotRecoverAnUnknownRootFromMetadata(){
        var e=new Beliefs.Entry(claim(),observer,Beliefs.Evidence.HEARD,.6,100,600,new Beliefs.Provenance(Beliefs.Basis.REPORTED_HEARSAY,root,0));
        var own=language.withReferents(names());
        var reply=PracticalQuestions.sourceReply(e,observer,seeker,names(),200);
        assertEquals(new PracticalQuestions.Source(root,false,true),PracticalQuestions.decodedSource(hear(reply,own,own),observer,seeker));
        assertNull(PracticalQuestions.decodedSource(hear(reply,own,language),observer,seeker).root());
        assertEquals(PracticalQuestions.unknown(Concept.SOURCE),PracticalQuestions.sourceReply(e,observer,seeker,Referents.empty(),200));
        var old=PracticalQuestions.sourceReply(e,observer,seeker,names(),450);
        assertFalse(PracticalQuestions.decodedSource(hear(old,own,own),observer,seeker).recent());
    }
    @Test void whereAnswersIdentifyOnlyKnownPlacesAndRemainSeparateFromPersonalProviders(){
        var site=new Place("minecraft:overworld",4,64,5);var refs=Referents.empty().establish("orchard",Referents.Target.place(site),0);
        var c=new Beliefs.Claim(null,Concept.HAVE,Concept.FOOD,null,false,false,site,null);
        var b=Beliefs.empty().observe(c,observer,0);
        assertNull(PracticalQuestions.answer(PracticalQuestions.Kind.WHO_FOOD,b,observer,seeker,refs,10));
        var a=PracticalQuestions.answer(PracticalQuestions.Kind.WHERE_FOOD,b,observer,seeker,refs,10);
        assertEquals(c,PracticalQuestions.decodedAnswer(hear(List.of(a.clause()),language.withReferents(refs),language.withReferents(refs)),PracticalQuestions.Kind.WHERE_FOOD,observer,seeker));
    }
}
