package local.villagers.language;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DialogueTest {
    private Sentence.Interpretation heard(Clause c) { return new Sentence.Interpretation(List.of(c),1,0); }
    @Test void misunderstoodNeedsProduceConfirmationWithoutOracleAccess() {
        var wrong=heard(new Clause(Concept.I,Concept.WANT,Concept.BED));
        assertEquals(Dialogue.Response.OTHER,Dialogue.response(wrong));
        var echoed=Dialogue.confirmation(wrong);
        assertEquals(List.of(new Clause(Concept.YOU,Concept.WANT,Concept.BED)),echoed);
        assertFalse(Dialogue.confirms(heard(echoed.getFirst()),List.of(new Clause(Concept.I,Concept.WANT,Concept.FOOD))));
        assertTrue(Dialogue.confirms(heard(echoed.getFirst()),List.of(new Clause(Concept.I,Concept.WANT,Concept.BED))));
    }
    @Test void invitationsAndPromisesRequireTheDecodedPresentPositiveMeanings() {
        assertEquals(Dialogue.Response.INVITATION,Dialogue.response(heard(new Clause(Concept.I,Concept.WANT,Concept.FRIEND))));
        assertEquals(Dialogue.Response.PROMISE,Dialogue.response(heard(new Clause(Concept.I,Concept.PROMISE,Concept.FRIEND))));
        assertEquals(Dialogue.Response.OTHER,Dialogue.response(heard(new Clause(Concept.I,Concept.PROMISE,Concept.FRIEND,true,false))));
        assertEquals(Dialogue.Response.OTHER,Dialogue.response(heard(new Clause(Concept.I,Concept.PROMISE,Concept.FRIEND,false,true))));
        assertEquals(Dialogue.Response.OTHER,Dialogue.response(heard(new Clause(Concept.YOU,Concept.PROMISE,Concept.FRIEND))));
    }
    @Test void unknownSpeechAsksAgainAndNoWordsCannotAgreeToAnInvitation() {
        var unknown=new Sentence.Interpretation(List.of(),0,1);
        assertEquals(Dialogue.Response.CLARIFY,Dialogue.response(unknown));
        assertEquals(List.of(new Clause(Concept.I,Concept.ASK,Concept.AGAIN)),Dialogue.confirmation(unknown));
        assertFalse(Dialogue.confirms(unknown,List.of(new Clause(Concept.I,Concept.WANT,Concept.FRIEND))));
    }
    @Test void repairAttentionIsBoundedPerActor() {
        var budget=new Dialogue.Budget();
        assertTrue(budget.take(100)); assertFalse(budget.take(101));
        assertFalse(budget.take(299));assertTrue(budget.take(300));
        assertFalse(budget.take(Long.MAX_VALUE));
    }
    @Test void symmetricTranslationErrorsCanSurviveEchoConfirmationUntilTheWorldDisambiguatesThem() {
        var nativeWords=BiomeDialect.foundingLanguage("minecraft:plains");
        var confused=new Lexicon(nativeWords.entries().stream().map(e->e.concept()==Concept.FOOD
                ?new Lexicon.Entry(Concept.BED,e.word(),e.strength()):e.concept()==Concept.BED
                ?new Lexicon.Entry(Concept.FOOD,e.word(),e.strength()):e).toList(),List.of(),false,nativeWords.grammar());
        var intention=List.of(new Clause(Concept.I,Concept.WANT,Concept.FOOD));
        var request=Sentence.speak(intention,nativeWords);
        var wrong=Sentence.interpret(request.sounds(),confused);
        assertEquals(Concept.BED,wrong.clauses().getFirst().object());
        var echo=Sentence.speak(Dialogue.confirmation(wrong),confused);
        assertTrue(Dialogue.confirms(Sentence.interpret(echo.sounds(),nativeWords),intention));
        assertEquals(Concept.BED,confused.meaning(nativeWords.wordFor(Concept.FOOD)),"confirmation alone supplies no semantic truth");
    }
}
