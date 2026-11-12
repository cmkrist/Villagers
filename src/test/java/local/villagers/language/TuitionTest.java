package local.villagers.language;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TuitionTest {
    @Test void confidentWrongTranslationPointsAtWrongPublicObjectAndTeacherRejectsIt(){
        var teacher=WordFormationTest.language();var entries=teacher.entries().stream().filter(e->e.concept()!=Concept.HOE&&e.concept()!=Concept.SWORD).toList();
        var child=new Lexicon(entries).withGrammar(teacher.grammar()).reinforce(Concept.SWORD,"seno",1).reinforce(Concept.HOE,"tavi",1);
        var hoe=new Tuition.Seen(UUID.randomUUID(),Set.of(Concept.HOE));var sword=new Tuition.Seen(UUID.randomUUID(),Set.of(Concept.SWORD));
        var question=Sentence.speak(Tuition.question(Concept.HOE),teacher);var parsed=Sentence.interpret(question.sounds(),child);assertTrue(parsed.complete());assertEquals(1,parsed.confidence());
        var answer=Tuition.answer(question.sounds(),child,List.of(hoe,sword));assertNotNull(answer);assertEquals(sword.target(),answer.pointedAt());
        var response=Sentence.speak(answer.words(),child);assertEquals(Concept.HOE,Sentence.interpret(response.sounds(),teacher).clauses().getFirst().object());
        assertFalse(Tuition.confirms(Concept.HOE,Sentence.interpret(response.sounds(),teacher),sword));
    }
    @Test void unknownRequestAndAbsentObjectDoNotProduceAnswers(){
        var parent=WordFormationTest.language();var child=Lexicon.child(parent,parent,WordFormationTest.CLEAR);var words=Sentence.speak(Tuition.question(Concept.HOE),parent);
        assertNull(Tuition.answer(words.sounds(),child,List.of(new Tuition.Seen(UUID.randomUUID(),Set.of(Concept.HOE)))));
        assertNull(Tuition.answer(words.sounds(),parent,List.of(new Tuition.Seen(UUID.randomUUID(),Set.of(Concept.SWORD)))));
    }
    @Test void adultWithMissingVocabularyCanLearnAndAnswerWithoutBeingMarkedAsAChild(){
        var parent=WordFormationTest.language();var adult=new Lexicon(parent.entries().stream().filter(e->e.concept()!=Concept.HOE).toList()).withGrammar(parent.grammar());
        assertFalse(adult.developing());var target=new Tuition.Seen(UUID.randomUUID(),Set.of(Concept.HOE));var question=Sentence.speak(Tuition.question(Concept.HOE),parent);
        assertNull(Tuition.answer(question.sounds(),adult,List.of(target)));
        for(int i=0;i<4;i++)adult=WordFormationTest.hear(adult,parent,Concept.HOE,Set.of(Concept.HOE));
        assertFalse(adult.developing());assertNotNull(Tuition.answer(question.sounds(),adult,List.of(target)));
    }
    @Test void teacherRequiresBothSpokenMeaningAndPointingEvidence(){
        var l=WordFormationTest.language();var good=Sentence.interpret(Sentence.speak(Tuition.demonstrate(Concept.HOE),l).sounds(),l);
        assertTrue(Tuition.confirms(Concept.HOE,good,new Tuition.Seen(UUID.randomUUID(),Set.of(Concept.HOE))));
        assertFalse(Tuition.confirms(Concept.HOE,good,null));
        var wrong=Sentence.interpret(Sentence.speak(Tuition.demonstrate(Concept.SWORD),l).sounds(),l);assertFalse(Tuition.confirms(Concept.HOE,wrong,new Tuition.Seen(UUID.randomUUID(),Set.of(Concept.HOE))));
    }
}
