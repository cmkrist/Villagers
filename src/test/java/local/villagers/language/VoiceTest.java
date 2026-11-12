package local.villagers.language;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class VoiceTest {
    @Test void hearingAndPracticingSoundDoesNotSupplyMeanings(){
        var l=Lexicon.empty();var v=Voice.empty().hear(Voice.Kind.PILLAGER,List.of("zaga","lumi"),10);
        assertNull(l.meaning("zaga"));var attempted=v.imitate(Voice.Kind.VILLAGER,Voice.Kind.PILLAGER,11);
        assertEquals(List.of("saka","lumi"),attempted.sounds());assertEquals(1,attempted.practiced().model(Voice.Kind.PILLAGER).practice());
        assertTrue(l.entries().isEmpty());
    }
    @Test void imitationErrorsCrossTheActualInterpretationBoundary(){
        var listener=new Lexicon(List.of(new Lexicon.Entry(Concept.FOOD,"zaga",1),new Lexicon.Entry(Concept.DANGER,"saka",1)));
        var voice=Voice.empty().hear(Voice.Kind.PILLAGER,List.of("zaga"),0);
        var a=voice.imitate(Voice.Kind.VILLAGER,Voice.Kind.PILLAGER,1);assertEquals(Concept.DANGER,listener.meaning(a.sounds().getFirst()));
        for(int i=0;i<3;i++)a=a.practiced().imitate(Voice.Kind.VILLAGER,Voice.Kind.PILLAGER,2+i);
        assertEquals(Concept.FOOD,listener.meaning(a.sounds().getFirst()));
    }
    @Test void reciprocalPillagerAttemptHasItsOwnMotorConstraints(){
        var v=Voice.empty().hear(Voice.Kind.VILLAGER,List.of("biva",";","velo"),20);assertEquals(List.of("pifa",";","felo"),v.imitate(Voice.Kind.PILLAGER,Voice.Kind.VILLAGER,21).sounds());
        assertNull(v.imitate(Voice.Kind.VILLAGER,Voice.Kind.VILLAGER,21));
    }
    @Test void noExemplarOrStaleExemplarCannotBeImitated(){assertNull(Voice.empty().imitate(Voice.Kind.PILLAGER,Voice.Kind.VILLAGER,5));var v=Voice.empty().hear(Voice.Kind.VILLAGER,List.of("lumi"),2);assertNull(v.imitate(Voice.Kind.PILLAGER,Voice.Kind.VILLAGER,2403));assertNull(v.imitate(Voice.Kind.PILLAGER,Voice.Kind.VILLAGER,1));}
    @Test void memoryAndCountersStayBoundedWithoutCopyingPrivateSemanticState(){
        var v=Voice.empty();for(int i=0;i<1300;i++){v=v.hear(Voice.Kind.PILLAGER,List.of("lumi"),i);v=v.imitate(Voice.Kind.VILLAGER,Voice.Kind.PILLAGER,i).practiced();}
        assertEquals(1,v.models().size());assertEquals(1000,v.model(Voice.Kind.PILLAGER).heard());assertEquals(1000,v.model(Voice.Kind.PILLAGER).practice());
        assertEquals(v,v.hear(Voice.Kind.VILLAGER,Collections.nCopies(13,"lumi"),1400));
    }
    @Test void identicalWordsAndBodyRetainIndividualPitchAndRhythmAcrossReload(){
        var id=new UUID(33,17);var sounds=List.of("luma","vido","seno");var plan=Voice.plan(id,Voice.Kind.VILLAGER,sounds,false);
        assertEquals(plan,Voice.plan(id,Voice.Kind.VILLAGER,sounds,false));assertNotEquals(plan,Voice.plan(new UUID(33,18),Voice.Kind.VILLAGER,sounds,false));
        assertNotEquals(plan,Voice.plan(id,Voice.Kind.PILLAGER,sounds,false));assertNotEquals(plan,Voice.plan(id,Voice.Kind.VILLAGER,List.of("pako","vido","seno"),false));
        assertEquals(sounds,plan.stream().map(Voice.Pulse::word).toList());assertTrue(plan.stream().allMatch(p->p.pitch()>=.5&&p.pitch()<=1.7));
    }
    @Test void outputIsBoundedAndPunctuationCreatesAPause(){
        var id=UUID.randomUUID();var plan=Voice.plan(id,Voice.Kind.VILLAGER,List.of("lumi",";","lumi"),true);assertEquals(2,plan.size());assertTrue(plan.get(1).at()>plan.getFirst().at()+5);
        assertThrows(IllegalArgumentException.class,()->Voice.plan(id,Voice.Kind.VILLAGER,Collections.nCopies(64,"lumi"),false));
    }
}
