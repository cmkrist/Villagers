package local.villagers.language;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RelationshipDialogueTest {
    final Lexicon nativeLanguage=BiomeDialect.foundingLanguage("minecraft:plains");
    Sentence.Interpretation hear(List<Clause> clauses,Lexicon listener){var words=Sentence.speak(clauses,nativeLanguage);assertNotNull(words);return Sentence.interpret(words.sounds(),listener);}
    @Test void partnershipAndParenthoodAreDifferentUnderstoodActs(){
        for(var act:RelationshipDialogue.Act.values())if(act!=RelationshipDialogue.Act.OTHER){
            var parsed=hear(RelationshipDialogue.words(act),nativeLanguage);assertEquals(act,RelationshipDialogue.act(parsed));
            if(act!=RelationshipDialogue.Act.REFUSE)assertNotNull(RelationshipDialogue.english(parsed));
        }
        assertEquals(RelationshipDialogue.Act.OTHER,RelationshipDialogue.act(hear(Clause.fromConcepts(List.of(Concept.WANT,Concept.FRIEND,Concept.BED)),nativeLanguage)));
        assertEquals(RelationshipDialogue.Act.OTHER,RelationshipDialogue.act(hear(List.of(new Clause(Concept.I,Concept.SAY,Concept.YES)),nativeLanguage)));
    }
    @Test void wordsGrammarTenseAndRecipientAllMatter(){
        var proposal=RelationshipDialogue.words(RelationshipDialogue.Act.PROPOSE_PARTNER);
        assertEquals(RelationshipDialogue.Act.OTHER,RelationshipDialogue.act(hear(proposal,BiomeDialect.foundingLanguage("minecraft:desert"))));
        assertEquals(RelationshipDialogue.Act.OTHER,RelationshipDialogue.act(hear(List.of(proposal.getFirst()),nativeLanguage)));
        assertEquals(RelationshipDialogue.Act.OTHER,RelationshipDialogue.act(hear(List.of(proposal.getFirst(),new Clause(Concept.I,Concept.ASK,Concept.FRIEND)),nativeLanguage)));
        assertEquals(RelationshipDialogue.Act.OTHER,RelationshipDialogue.act(hear(List.of(new Clause(Concept.I,Concept.WANT,Concept.PARTNER,false,true),proposal.get(1)),nativeLanguage)));
    }
    @Test void confidentMistranslationDoesNotUnlockAHiddenIntendedCommitment(){
        var entries=new ArrayList<>(nativeLanguage.entries().stream().filter(e->e.concept()!=Concept.PARTNER).toList());
        entries.add(new Lexicon.Entry(Concept.CHILD,nativeLanguage.wordFor(Concept.PARTNER),1));
        var wrong=new Lexicon(entries,List.of(),false,nativeLanguage.grammar());
        var heard=hear(RelationshipDialogue.words(RelationshipDialogue.Act.PROPOSE_PARTNER),wrong);
        assertTrue(heard.complete());assertEquals(1,heard.confidence());
        assertEquals(RelationshipDialogue.Act.PROPOSE_CHILD,RelationshipDialogue.act(heard));
        assertNotEquals(RelationshipDialogue.Act.PROPOSE_PARTNER,RelationshipDialogue.act(heard));
    }
}
