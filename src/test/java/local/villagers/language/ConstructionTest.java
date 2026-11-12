package local.villagers.language;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionTest {
    @Test void unknownCompressionIsLearnedFromRepeatedVisibleInjuryAndThenGeneralizes() {
        var adult=BiomeDialect.foundingLanguage("minecraft:plains");
        var event=new Clause(Concept.I,Concept.IS,Concept.HURT);
        var compact=Sentence.speak(List.of(event),adult,true);
        assertEquals(2,compact.sounds().size());
        assertFalse(Sentence.interpret(compact.sounds(),adult).complete());
        var learner=adult;
        for(int i=0;i<7;i++) learner=LanguageLearning.hear(learner,compact.sounds(),Set.copyOf(event.concepts()),event,new Random(1){@Override public double nextDouble(){return .5;}});
        assertTrue(compact.correct(Sentence.interpret(compact.sounds(),learner)));
        var newState=new Clause(Concept.YOU,Concept.IS,Concept.HAPPY);
        var generated=Sentence.speak(List.of(newState),learner);
        assertEquals(2,generated.sounds().size(),"learned slots combine words never used in the original example");
        assertTrue(generated.correct(Sentence.interpret(generated.sounds(),learner)));
        assertFalse(Sentence.interpret(generated.sounds(),adult).complete(),"unexposed observer does not silently recover missing grammar");
    }
    @Test void arbitraryRepeatedRolePatternCanBeInducedRatherThanSelectingOneOfSixOrders() {
        var language=BiomeDialect.foundingLanguage("minecraft:forest");
        var event=new Clause(Concept.I,Concept.SEE,Concept.BED);
        var words=List.of(language.wordFor(Concept.I),language.wordFor(Concept.SEE),language.wordFor(Concept.I),language.wordFor(Concept.BED));
        var grammar=language.grammar();
        for(int i=0;i<7;i++)grammar=grammar.observe(words,language::meaning,event);
        var learned=language.withGrammar(grammar);
        assertEquals(List.of(event),Sentence.interpret(words,learned).clauses());
        assertEquals(3,Sentence.speak(List.of(event),learned).sounds().size(),"Reception of a different role order retains native production");
        assertEquals(Grammar.Order.VSO,learned.grammar().preferred());
    }
    @Test void omittedRequestsAndUnknownMarkersAreNotReconstructedFromIntention() {
        var language=BiomeDialect.foundingLanguage("minecraft:plains");
        var want=new Clause(Concept.I,Concept.WANT,Concept.FOOD);
        var missing=List.of(language.wordFor(Concept.I),language.wordFor(Concept.FOOD));
        assertNull(Construction.infer(missing,language::meaning,want));
        assertEquals(language.grammar(),language.grammar().observe(missing,language::meaning,want));
        var hurt=new Clause(Concept.I,Concept.IS,Concept.HURT);
        var compact=Sentence.speak(List.of(hurt),language,true);
        var grammar=language.grammar();for(int i=0;i<7;i++)grammar=grammar.observe(compact.sounds(),language::meaning,hurt);
        var wire=new ArrayList<String>();wire.add("zzzzzzzzzz");wire.addAll(compact.sounds());
        assertFalse(Sentence.interpret(wire,language.withGrammar(grammar)).complete());
    }
    @Test void constructionRequiresRepeatedUnambiguousGroundingAndHasFiniteState() {
        var language=BiomeDialect.foundingLanguage("minecraft:plains");
        var hurt=new Clause(Concept.I,Concept.IS,Concept.HURT);
        var wire=Sentence.speak(List.of(hurt),language,true).sounds();
        assertEquals(language.grammar(),language.grammar().observe(wire,language::meaning,null));
        var grammar=language.grammar().observe(wire,language::meaning,hurt);
        assertFalse(Sentence.interpret(wire,language.withGrammar(grammar)).complete());
        for(int i=0;i<10000;i++)grammar=grammar.observe(wire,language::meaning,hurt);
        assertTrue(grammar.receptive().dialects().stream().flatMap(d->d.patterns().stream()).allMatch(p->p.evidence()<=1000));
        assertThrows(IllegalArgumentException.class,()->new Construction(List.of(Grammar.Role.SUBJECT),null,1));
    }
    @Test void childrenInheritExposureWithoutInstantConstructionFluency() {
        var parent=BiomeDialect.foundingLanguage("minecraft:plains");
        var pattern=new Construction(List.of(Grammar.Role.SUBJECT,Grammar.Role.OBJECT),Concept.IS,7);
        parent=parent.withGrammar(parent.grammar().learn(pattern));
        var child=Lexicon.child(parent,parent,new Random(3));
        assertEquals(1,child.grammar().constructions().size());
        assertEquals(1,child.grammar().constructions().getFirst().evidence());
    }
    @Test void competingLearnedConstructionsDoNotUseIntentionToResolveAmbiguity() {
        var language=BiomeDialect.foundingLanguage("minecraft:plains");
        var grammar=language.grammar().learn(new Construction(List.of(Grammar.Role.SUBJECT,Grammar.Role.OBJECT),Concept.IS,4))
                .learn(new Construction(List.of(Grammar.Role.OBJECT,Grammar.Role.SUBJECT),Concept.IS,4));
        var wire=List.of(language.wordFor(Concept.FRIEND),language.wordFor(Concept.HURT));
        assertFalse(Sentence.interpret(wire,language.withGrammar(grammar)).complete());
    }
}
