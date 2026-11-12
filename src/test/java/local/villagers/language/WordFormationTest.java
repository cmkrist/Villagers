package local.villagers.language;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WordFormationTest {
    static final Random CLEAR=new Random(42){@Override public double nextDouble(){return .99;}};
    static Lexicon language(){
        var words=new EnumMap<Concept,String>(Concept.class);
        words.put(Concept.I,"mi");words.put(Concept.SEE,"vido");words.put(Concept.WANT,"vona");words.put(Concept.SAY,"sala");words.put(Concept.YES,"ya");words.put(Concept.NO,"na");
        words.put(Concept.WOOD,"luma");words.put(Concept.STONE,"pako");words.put(Concept.HOE,"seno");words.put(Concept.SWORD,"tavi");
        return new Lexicon(words.entrySet().stream().map(e->new Lexicon.Entry(e.getKey(),e.getValue(),1)).toList()).withGrammar(Grammar.founding(Grammar.Order.SVO))
                .withFormation(WordFormation.founding(WordFormation.Order.MATERIAL_HEAD,"n"));
    }
    static Lexicon roots(){return language().withFormation(WordFormation.empty());}
    static Lexicon hear(Lexicon child,Lexicon speaker,Concept topic,Set<Concept> features){return Tuition.observe(child,Sentence.speak(Tuition.demonstrate(topic),speaker).sounds(),features,CLEAR);}
    @Test void repeatedVisibleCompoundTeachesRuleAndNovelCombinationWithoutCopyingEntry(){
        var parent=language();var child=roots();
        for(int i=0;i<2;i++)child=hear(child,parent,Concept.WOODEN_HOE,Set.of(Concept.WOOD,Concept.HOE,Concept.WOODEN_HOE));
        assertNull(child.wordFor(Concept.STONE_SWORD));assertEquals(2,child.formation().patterns().getFirst().evidence());
        child=hear(child,parent,Concept.WOODEN_HOE,Set.of(Concept.WOOD,Concept.HOE,Concept.WOODEN_HOE));
        assertEquals("pakontavi",child.wordFor(Concept.STONE_SWORD));assertEquals(Concept.STONE_SWORD,child.meaning("pakontavi"));
        assertTrue(child.entries().stream().noneMatch(e->e.concept()==Concept.STONE_SWORD));
        assertTrue(child.evidence().stream().noneMatch(e->e.word().equals("pakontavi")));
        var newQuestion=Sentence.speak(Tuition.question(Concept.STONE_SWORD),parent);var target=UUID.randomUUID();
        var answer=Tuition.answer(newQuestion.sounds(),child,List.of(new Tuition.Seen(target,Set.of(Concept.STONE_SWORD,Concept.STONE,Concept.SWORD))));
        assertNotNull(answer);assertEquals(target,answer.pointedAt());assertEquals(Concept.STONE_SWORD,answer.meaning());
    }
    @Test void childrenStartWithoutAdultRulesAndAcquireRootsAcrossVariedExamples(){
        var parent=language();var child=Lexicon.child(parent,parent,CLEAR);
        assertNull(child.meaning("seno"));assertTrue(child.formation().patterns().isEmpty());
        for(int i=0;i<12;i++)child=hear(child,parent,Concept.HOE,i%2==0?Set.of(Concept.WOOD,Concept.HOE,Concept.WOODEN_HOE):Set.of(Concept.STONE,Concept.HOE,Concept.STONE_HOE));
        assertEquals(Concept.HOE,child.meaning("seno"));assertNull(child.meaning("tavi"));assertTrue(child.formation().patterns().isEmpty());
    }
    @Test void unknownRootsCannotBeRecoveredFromTeacherIntention(){
        var parent=language();var child=Lexicon.child(parent,parent,CLEAR);
        for(int i=0;i<8;i++)child=hear(child,parent,Concept.WOODEN_HOE,Set.of(Concept.WOOD,Concept.HOE,Concept.WOODEN_HOE));
        assertTrue(child.formation().patterns().isEmpty());assertNull(child.wordFor(Concept.STONE_SWORD));
    }
    @Test void ambiguityInAVisibleMaterialHeadPairDoesNotCreateACategory(){
        var roots=roots().reinforce(Concept.HOE,"luma",1);
        var heard=WordFormation.empty().hear(List.of("lumanluma"),roots::rootWords,roots::atomicMeaning,Set.of(Concept.WOODEN_HOE));
        assertTrue(heard.patterns().isEmpty());assertNull(roots.meaning("luma"));
    }
    @Test void repetitionWithinOneUtteranceIsOneObservation(){
        var l=roots();var f=WordFormation.empty().hear(Collections.nCopies(30,"lumanseno"),l::rootWords,l::atomicMeaning,Set.of(Concept.WOODEN_HOE));
        assertEquals(1,f.patterns().getFirst().evidence());assertNull(f.preferred());
    }
    @Test void hearingWithoutPublicExampleCannotTeachFormation(){
        var l=roots();var f=WordFormation.empty().hear(List.of("lumanseno"),l::rootWords,l::atomicMeaning,Set.of());assertTrue(f.patterns().isEmpty());
    }
    @Test void foreignFormationIsUnderstoodWhileProductionRetainsNativeConventionAfterSaturation(){
        var own=language();var foreign=own.withFormation(WordFormation.founding(WordFormation.Order.HEAD_MATERIAL,"z"));
        var f=own.formation();for(int i=0;i<2100;i++)f=f.hear(List.of(foreign.wordFor(Concept.WOODEN_HOE)),own::rootWords,own::atomicMeaning,Set.of(Concept.WOODEN_HOE));
        own=own.withFormation(f);assertEquals("lumanseno",own.wordFor(Concept.WOODEN_HOE));assertEquals(Concept.WOODEN_HOE,own.meaning("senozluma"));assertTrue(f.patterns().size()<=WordFormation.MAX_PATTERNS);
    }
    @Test void collidingStemsStayUnresolvedRatherThanReturningFirstConcept(){
        var l=language().reinforce(Concept.STONE,"lumani",1);assertNull(l.meaning("lumanseno"));assertNull(l.wordFor(Concept.WOODEN_HOE));
    }
    @Test void familiarAtomicWordsRemainStableEvenIfACompoundWouldHaveTheSameSound(){
        var l=language().reinforce(Concept.BREAD,"lumanseno",1);assertEquals(Concept.BREAD,l.meaning("lumanseno"));assertNull(l.wordFor(Concept.WOODEN_HOE));
    }
    @Test void legacyGenericCategoriesAndEnumOrdinalsStayStable(){
        assertEquals(27,Concept.WOOD.ordinal());assertEquals(Concept.HOE,Concept.forItem("minecraft:wooden_hoe"));assertEquals(Concept.WOODEN_HOE,Concept.specificItem("minecraft:wooden_hoe"));
        assertEquals(Concept.STONE,Concept.forItem("minecraft:cobblestone"));
    }
    @Test void foundedBiomesKeepDifferentConventionsAndValidBoundedWords(){
        var rules=new HashSet<String>();for(var biome:List.of("minecraft:plains","minecraft:desert","minecraft:jungle","minecraft:snowy_plains","minecraft:taiga","minecraft:savanna")){
            var l=BiomeDialect.foundingLanguage(biome);rules.add(l.formation().preferred().toString());
            for(var c:WordFormation.derived()){assertTrue(Words.valid(l.wordFor(c)));assertEquals(c,l.meaning(l.wordFor(c)));}
        }assertTrue(rules.size()>1);
    }
    @Test void playerCanGeneralizeOnlyAfterActualRootAndRuleEvidence(){
        var parent=language();var g=Glossary.empty(UUID.randomUUID()).withGrammar(parent.grammar());
        for(var c:List.of(Concept.I,Concept.SEE,Concept.WOOD,Concept.STONE,Concept.HOE,Concept.SWORD))for(int i=0;i<3;i++)g=g.hear(parent.wordFor(c),Set.of(c));
        for(int i=0;i<3;i++)g=LanguageLearning.hear(g,Sentence.speak(Tuition.demonstrate(Concept.WOODEN_HOE),parent).sounds(),Set.of(Concept.WOOD,Concept.HOE,Concept.WOODEN_HOE),null);
        assertEquals(Concept.STONE_SWORD,g.translate("pakontavi"));assertFalse(g.understood().containsKey("pakontavi"));
    }
}
