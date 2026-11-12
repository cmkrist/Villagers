package local.villagers.language;

import java.util.*;
import local.villagers.mind.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReferentsTest {
    private final UUID speaker=new UUID(501,1),listener=new UUID(501,2),other=new UUID(501,3);
    private final Lexicon language=BiomeDialect.foundingLanguage("minecraft:plains");
    private Referents learn(Referents names,String word,Referents.Target target){for(int i=0;i<3;i++)names=names.observe(word,target,i*200);return names;}
    @Test void introductionsNeedKnownNamingGrammarVisibleSpeakerAndSeparateEncounters(){
        var self=Referents.Target.person(speaker);var producer=language.withReferents(Referents.empty().establish("clover",self,0));
        var introduction=new Clause(Concept.I,Concept.NAMED,Concept.PERSON,false,false,null,producer.referents().resolve("clover"));
        var wire=Sentence.speak(List.of(introduction),producer).sounds();
        Referents names=Referents.empty();
        assertEquals(names,names.hearIntroduction(wire,language::meaning,language.grammar(),null,null,0));
        names=names.hearIntroduction(wire,language::meaning,language.grammar(),speaker,null,0);
        names=names.hearIntroduction(wire,language::meaning,language.grammar(),speaker,null,1);
        assertNull(names.resolve("clover"));
        names=names.hearIntroduction(wire,language::meaning,language.grammar(),speaker,null,200);
        assertNull(names.resolve("clover"));
        names=names.hearIntroduction(wire,language::meaning,language.grammar(),speaker,null,400);
        assertEquals(self,names.resolve("clover").target());
        assertTrue(Sentence.interpret(wire,language.withReferents(names)).complete());
    }
    @Test void namedThirdPartyClaimsUseTheListenersBindingsNotSpeakerMetadata(){
        var actual=Referents.Target.person(other);
        var producer=language.withReferents(Referents.empty().establish("fern",actual,0));
        var clause=Clause.named(producer.referents().resolve("fern"),Concept.HAVE,Concept.FOOD);
        var said=Sentence.speak(List.of(clause),producer);
        assertFalse(Sentence.interpret(said.sounds(),language).complete());
        var mistaken=language.withReferents(learn(Referents.empty(),"fern",Referents.Target.person(listener)));
        var interpreted=Sentence.interpret(said.sounds(),mistaken);
        assertTrue(interpreted.complete());assertEquals(1,interpreted.confidence());assertFalse(said.correct(interpreted));
        var book=Beliefs.empty().hear(interpreted,speaker,listener,0,500);
        assertEquals(listener,book.entries().getFirst().claim().subject());
        assertEquals(0,book.foodLead(other,501));assertTrue(book.foodLead(listener,501)>0);
        var fake=new Sentence(List.of(Clause.named(new Referents.Reference("fern",Referents.Target.person(speaker)),Concept.HAVE,Concept.FOOD)),said.sounds());
        assertEquals(interpreted,Sentence.interpret(fake.sounds(),mistaken));
    }
    @Test void homonymousPeopleAreAmbiguousRatherThanResolvedUsingTheWorldRegistry(){
        var a=Referents.Target.person(speaker);var b=Referents.Target.person(other);
        var producer=language.withReferents(Referents.empty().establish("fern",a,0));
        var wire=Sentence.speak(List.of(Clause.named(producer.referents().resolve("fern"),Concept.HAVE,Concept.FOOD)),producer).sounds();
        var names=learn(learn(Referents.empty(),"fern",a),"fern",b);
        assertNull(names.resolve("fern"));assertFalse(Sentence.interpret(wire,language.withReferents(names)).complete());
    }
    @Test void namingPlacesNeedsJointAttentionAndLaterClaimsContainNoSpokenCoordinates(){
        var site=new Place("minecraft:overworld",123,64,-456);var target=Referents.Target.place(site);
        var producer=language.withReferents(Referents.empty().establish("rivermill",target,0));
        var introduction=Sentence.speak(List.of(new Clause(Concept.HERE,Concept.NAMED,Concept.PLACE,false,false,null,producer.referents().resolve("rivermill"))),producer);
        Referents names=Referents.empty();
        for(int i=0;i<3;i++)names=names.hearIntroduction(introduction.sounds(),language::meaning,language.grammar(),speaker,null,i*200);
        assertTrue(names.entries().isEmpty());
        for(int i=0;i<3;i++)names=names.hearIntroduction(introduction.sounds(),language::meaning,language.grammar(),speaker,site,i*200);
        assertEquals(site,names.resolve("rivermill").target().place());
        var said=Sentence.speak(List.of(Clause.named(producer.referents().resolve("rivermill"),Concept.HAVE,Concept.FOOD)),producer);
        assertTrue(said.sounds().stream().allMatch(Words::valid));
        assertFalse(String.join(" ",said.sounds()).contains("123"));
        var parsed=Sentence.interpret(said.sounds(),language.withReferents(names));
        var claim=Beliefs.Claim.resolve(parsed.clauses().getFirst(),speaker,listener);
        assertNull(claim.subject());assertEquals(site,claim.subjectPlace());
        assertEquals(0,Beliefs.empty().hear(parsed,speaker,listener,0,500).foodLead(speaker,501));
    }
    @Test void sourceCannotSpeakAnUnknownOrForgedBindingAndBareClassNounsDoNotIdentifyPeople(){
        var unknown=new Referents.Reference("fern",Referents.Target.person(other));
        assertNull(Sentence.speak(List.of(Clause.named(unknown,Concept.HAVE,Concept.FOOD)),language));
        assertNull(Beliefs.Claim.resolve(new Clause(Concept.PERSON,Concept.HAVE,Concept.FOOD),speaker,listener));
        assertNull(Beliefs.Claim.resolve(new Clause(Concept.I,Concept.SEE,Concept.PERSON),speaker,listener));
    }
    @Test void knownNamesAreNotRelearnedAsGeneralConceptLabels(){
        var lexicon=language.withReferents(learn(Referents.empty(),"fern",Referents.Target.person(other)));
        for(int i=0;i<10;i++)lexicon=lexicon.observe(List.of("fern"),Set.of(Concept.FOOD),new Random(3));
        assertNull(lexicon.meaning("fern"));assertNotNull(lexicon.referents().resolve("fern"));
        var player=Glossary.empty(listener).withReferents(lexicon.referents());
        for(int i=0;i<10;i++)player=LanguageLearning.hear(player,List.of("fern"),Set.of(Concept.FOOD),null);
        assertNull(player.translate("fern"));assertNotNull(player.referents().resolve("fern"));
    }
    @Test void lexicalAndProperNameCollisionsRemainUncertain(){
        var foodWord=language.wordFor(Concept.FOOD);
        var named=language.withReferents(Referents.empty().establish(foodWord,Referents.Target.person(other),0));
        var said=Sentence.speak(List.of(new Clause(Concept.I,Concept.WANT,Concept.FOOD)),language);
        assertFalse(Sentence.interpret(said.sounds(),named).complete());
    }
    @Test void inheritedVocabularyDoesNotIncludeParentsPrivateAcquaintancesOrPlaces(){
        var parent=language.withReferents(Referents.empty().establish("fern",Referents.Target.person(other),0));
        assertTrue(Lexicon.child(parent,parent,new Random(3)).referents().entries().isEmpty());
        assertEquals(parent.referents(),parent.withGrammar(parent.grammar()).referents());
        assertEquals(parent.referents(),parent.reinforce(Concept.FOOD,parent.wordFor(Concept.FOOD),.1).referents());
    }
    @Test void namedObjectsAndLearnedConstructionsPreserveIndependentIdentity(){
        var names=Referents.empty().establish("fern",Referents.Target.person(other),0);
        var lexicon=language.withReferents(names);
        var clause=new Clause(Concept.I,Concept.LIKE,Concept.PERSON,false,false,null,names.resolve("fern"));
        var said=Sentence.speak(List.of(clause),lexicon);
        var parsed=Sentence.interpret(said.sounds(),lexicon);
        assertTrue(said.correct(parsed));assertEquals(other,Beliefs.Claim.resolve(parsed.clauses().getFirst(),speaker,listener).objectPerson());
        var echo=Dialogue.confirmation(parsed);
        assertEquals(names.resolve("fern"),echo.getFirst().objectReference());
        var missingName=new Sentence.Interpretation(List.of(new Clause(Concept.YOU,Concept.LIKE,Concept.PERSON)),1,0);
        assertFalse(Dialogue.confirms(missingName,List.of(clause)),"a generic person is not the same statement as an identified person");
    }
    @Test void referenceMemoryIsBoundedAndMalformedOrDuplicateBindingsAreRejected(){
        var names=Referents.empty();for(int i=0;i<100;i++)names=names.observe("fern",Referents.Target.person(new UUID(5,i)),i*40);
        assertEquals(Referents.MAX_ENTRIES,names.entries().size());assertNull(names.resolve("fern"));
        var entry=names.entries().getFirst();
        assertThrows(IllegalArgumentException.class,()->new Referents(List.of(entry,entry)));
        assertThrows(IllegalArgumentException.class,()->Referents.Target.place(null));
    }
}
