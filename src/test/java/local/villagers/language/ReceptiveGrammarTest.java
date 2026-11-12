package local.villagers.language;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReceptiveGrammarTest {
    final Lexicon nativeLanguage=BiomeDialect.foundingLanguage("minecraft:plains");
    Lexicon foreign(Grammar.Order order,String prefix){return new Lexicon(nativeLanguage.entries().stream().map(e->new Lexicon.Entry(e.concept(),prefix+e.word(),1)).toList(),List.of(),false,Grammar.founding(order));}
    Lexicon acquainted(Lexicon other){var entries=new ArrayList<>(nativeLanguage.entries());for(var e:other.entries())entries.add(new Lexicon.Entry(e.concept(),e.word(),.35));return new Lexicon(entries,List.of(),false,nativeLanguage.grammar());}
    Random clear(){return new Random(1){@Override public double nextDouble(){return .5;}};}
    Lexicon encounter(Lexicon learner,Lexicon speaker,Clause event,boolean compact,int count){
        var wire=Sentence.speak(List.of(event),speaker,compact).sounds();
        for(int i=0;i<count;i++)learner=LanguageLearning.hear(learner,wire,Set.copyOf(event.concepts()),event,clear());return learner;
    }
    @Test void bilingualReceptionKeepsNativeOrderAndWordsAndGeneralizesBeyondTheGroundedExample(){
        var other=foreign(Grammar.Order.OVS,"q");var learner=acquainted(other);var original=learner;
        var event=new Clause(Concept.I,Concept.SEE,Concept.BREAD);
        learner=encounter(learner,other,event,false,3);
        var unseen=new Clause(Concept.I,Concept.WANT,Concept.FOOD);
        var request=Sentence.speak(List.of(unseen),other);
        assertTrue(request.correct(Sentence.interpret(request.sounds(),learner)));
        assertTrue(Sentence.speak(List.of(unseen),nativeLanguage).correct(Sentence.interpret(Sentence.speak(List.of(unseen),nativeLanguage).sounds(),learner)));
        assertEquals(Grammar.Order.SVO,learner.grammar().preferred());assertEquals(original.grammar().evidence(),learner.grammar().evidence());
        for(var c:Concept.values())assertEquals(original.wordFor(c),learner.wordFor(c));
        assertEquals(1,learner.grammar().receptive().dialects().size());
    }
    @Test void foreignCompressedGrammarCanBeUnderstoodWithoutAdoptingItsProduction(){
        var other=foreign(Grammar.Order.OVS,"q");var learner=acquainted(other);
        var event=new Clause(Concept.I,Concept.IS,Concept.HURT);
        var wire=Sentence.speak(List.of(event),other,true);
        learner=encounter(learner,other,event,true,2);assertFalse(Sentence.interpret(wire.sounds(),learner).complete());
        learner=encounter(learner,other,event,true,5);assertTrue(wire.correct(Sentence.interpret(wire.sounds(),learner)));
        assertEquals(3,Sentence.speak(List.of(event),learner).sounds().size());assertTrue(learner.grammar().constructions().isEmpty());
    }
    @Test void distinctLexicalCuesKeepTwoForeignDialectsApart(){
        var a=foreign(Grammar.Order.SOV,"q");var b=foreign(Grammar.Order.OVS,"x");var l=acquainted(a);
        var entries=new ArrayList<>(l.entries());for(var e:b.entries())entries.add(new Lexicon.Entry(e.concept(),e.word(),.35));l=new Lexicon(entries,List.of(),false,l.grammar());
        var event=new Clause(Concept.I,Concept.SEE,Concept.BREAD);l=encounter(l,a,event,false,4);l=encounter(l,b,event,false,4);
        for(var speaker:List.of(a,b,nativeLanguage)){var sentence=Sentence.speak(List.of(event),speaker);assertTrue(sentence.correct(Sentence.interpret(sentence.sounds(),l)));}
        assertEquals(2,l.grammar().receptive().dialects().size());assertEquals(Grammar.Order.SVO,l.grammar().preferred());
    }
    @Test void unknownOrPrivateMeaningDoesNotTeachGrammarAndNamesAreNotASecretDialectIdentifier(){
        var other=foreign(Grammar.Order.OVS,"q");var l=acquainted(other);var event=new Clause(Concept.I,Concept.SEE,Concept.BREAD);var sounds=Sentence.speak(List.of(event),other).sounds();
        assertEquals(l,LanguageLearning.hear(l,sounds,Set.of(),null,clear()));
        var learned=encounter(l,other,event,false,3);
        var names=Referents.empty().establish("fern",Referents.Target.person(UUID.randomUUID()),0);
        var speaker=other.withReferents(names);var named=Sentence.speak(List.of(Clause.named(names.resolve("fern"),Concept.SEE,Concept.BREAD)),speaker);
        assertFalse(Sentence.interpret(named.sounds(),learned).complete(),"Grammar cannot supply an unknown name binding");
    }
    @Test void receptiveNamingUsesLearnedSoundCuesAndActualVisibleReferents(){
        var other=foreign(Grammar.Order.OVS,"q");var l=encounter(acquainted(other),other,new Clause(Concept.I,Concept.SEE,Concept.BREAD),false,3);
        var id=UUID.randomUUID();var names=Referents.empty().establish("fern",Referents.Target.person(id),0);
        var wire=Sentence.speak(List.of(new Clause(Concept.I,Concept.NAMED,Concept.PERSON,false,false,null,names.resolve("fern"))),other.withReferents(names));
        var known=Referents.empty();for(long now:List.of(0L,40L,80L))known=known.hearIntroduction(wire.sounds(),l::meaning,l.grammar(),id,null,now);
        assertNotNull(known.resolve("fern"));assertEquals(id,known.resolve("fern").target().person());
        assertEquals(Referents.empty(),Referents.empty().hearIntroduction(wire.sounds(),l::meaning,l.grammar(),null,null,100));
    }
    @Test void childrenDoNotInheritTheirParentsAdultReceptiveDialectMastery(){
        var other=foreign(Grammar.Order.OVS,"q");var parent=encounter(acquainted(other),other,new Clause(Concept.I,Concept.SEE,Concept.BREAD),false,10);
        var child=Lexicon.child(parent,parent,clear());assertTrue(child.grammar().receptive().dialects().isEmpty());
        assertEquals(2,child.grammar().evidence().stream().mapToInt(Integer::intValue).sum());
    }
    @Test void receptiveProfilesAndCuesAreBoundedAndRepetitionDoesNotOverflow(){
        var base=Grammar.founding(Grammar.Order.SVO);var event=new Clause(Concept.I,Concept.SEE,Concept.BREAD);
        for(int dialect=0;dialect<12;dialect++){
            String stem="q"+Character.toString('a'+dialect);var other=foreign(Grammar.Order.OVS,stem);var words=Sentence.speak(List.of(event),other).sounds();
            for(int i=0;i<1200;i++)base=base.observe(words,other::meaning,event);
        }
        assertEquals(ReceptiveGrammar.MAX_DIALECTS,base.receptive().dialects().size());assertEquals(Grammar.Order.SVO,base.preferred());
        assertTrue(base.receptive().dialects().stream().allMatch(d->d.cues().size()<=ReceptiveGrammar.MAX_CUES&&d.patterns().stream().allMatch(p->p.evidence()<=1000)));
    }
    @Test void bootstrapPreservesPartialReceptiveLearningWithoutGivingADevelopingChildAdultGrammar(){
        var event=new Clause(Concept.I,Concept.IS,Concept.HURT);var wire=Sentence.speak(List.of(event),nativeLanguage,true).sounds();
        var child=new Lexicon(List.of(new Lexicon.Entry(Concept.I,nativeLanguage.wordFor(Concept.I),.6),new Lexicon.Entry(Concept.HURT,nativeLanguage.wordFor(Concept.HURT),.6)),List.of(),true,Grammar.empty());
        for(int i=0;i<3;i++)child=LanguageLearning.hear(child,wire,Set.copyOf(event.concepts()),event,clear());
        assertFalse(child.grammar().initialized());assertFalse(child.grammar().receptive().dialects().isEmpty());
        assertEquals(child,child.establishGrammar("minecraft:desert"));
        var adult=new Lexicon(child.entries(),List.of(),false,child.grammar()).establishGrammar("minecraft:desert");
        assertEquals(Grammar.Order.SOV,adult.grammar().preferred());assertEquals(child.grammar().receptive(),adult.grammar().receptive());
    }
}
