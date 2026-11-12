package local.villagers.language;

import java.util.*;
import local.villagers.work.WorkOrders.Task;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkDialogueTest {
    final Lexicon language=BiomeDialect.foundingLanguage("minecraft:plains");
    Sentence.Interpretation hear(List<Clause> clauses,Lexicon listener){var words=Sentence.speak(clauses,language);assertNotNull(words);return Sentence.interpret(words.sounds(),listener);}
    @Test void allConcreteTermsAreDecodedFromSoundsInEverySupportedFrame(){
        for(var act:WorkDialogue.Act.values())for(var task:Task.values())for(int price=1;price<=2;price++){
            var f=new WorkDialogue.Frame(act,task,price);var h=hear(WorkDialogue.words(f),language);assertEquals(f,WorkDialogue.decode(h));assertNotNull(WorkDialogue.english(h));
        }
    }
    @Test void genericAssentPartialTermsAndForeignGrammarDoNotCreateAnOrder(){
        var clauses=WorkDialogue.words(new WorkDialogue.Frame(WorkDialogue.Act.ACCEPT,Task.WOODEN_HOE,2));
        assertNull(WorkDialogue.decode(hear(List.of(new Clause(Concept.I,Concept.SAY,Concept.YES)),language)));
        assertNull(WorkDialogue.decode(hear(clauses.subList(0,5),language)));
        assertNull(WorkDialogue.decode(hear(clauses,BiomeDialect.foundingLanguage("minecraft:forest"))));
        var changed=new ArrayList<>(clauses);changed.set(1,new Clause(Concept.HOE,Concept.FROM,Concept.WHEAT));
        assertNull(WorkDialogue.decode(hear(changed,language)));
        changed=new ArrayList<>(clauses);changed.set(2,new Clause(Concept.HOE,Concept.IS,Concept.TWO));assertNull(WorkDialogue.decode(hear(changed,language)));
        changed=new ArrayList<>(clauses);changed.set(5,new Clause(Concept.WORK,Concept.IS,Concept.OLD));assertNull(WorkDialogue.decode(hear(changed,language)));
    }
    @Test void aConfidentWrongPriceCannotRecoverTheSpeakersIntendedQuote(){
        var entries=new ArrayList<>(language.entries().stream().filter(e->e.concept()!=Concept.TWO).toList());entries.add(new Lexicon.Entry(Concept.ONE,language.wordFor(Concept.TWO),1));
        var wrong=new Lexicon(entries,List.of(),false,language.grammar());
        var h=hear(WorkDialogue.words(new WorkDialogue.Frame(WorkDialogue.Act.QUOTE,Task.BREAD,2)),wrong);
        assertTrue(h.complete());assertEquals(1,h.confidence());assertEquals(1,WorkDialogue.decode(h).price());
    }
}
