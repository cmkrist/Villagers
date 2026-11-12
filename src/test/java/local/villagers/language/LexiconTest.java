package local.villagers.language;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LexiconTest {
    @Test void aNewMaterialNamePreservesSavedWordsAndDoesNotTranslateItForAListener() {
        var old=new Lexicon(java.util.List.of(new Lexicon.Entry(Concept.HOE,"badu",1),new Lexicon.Entry(Concept.TRADE,"dafa",1)));
        var named=old.nameMissing(Concept.WOOD,new Random(55));
        assertEquals("badu",named.wordFor(Concept.HOE));assertEquals("dafa",named.wordFor(Concept.TRADE));
        assertNotNull(named.wordFor(Concept.WOOD));assertNull(old.meaning(named.wordFor(Concept.WOOD)));
        assertEquals(Concept.WOOD,named.meaning(named.wordFor(Concept.WOOD)));
        assertSame(named,named.nameMissing(Concept.WOOD,new Random(99)));
    }
    @Test void materialQuotesUseTheWoodConceptButExistingConceptOrderStaysStable() {
        assertEquals(Concept.WOOD,Concept.forItem("minecraft:mangrove_log"));
        assertEquals(Concept.WOOD,Concept.forItem("minecraft:stripped_warped_hyphae"));
        assertNull(Concept.forItem("minecraft:oak_planks"));
        assertEquals(Concept.WOOD,Concept.values()[27]);
    }
    @Test void inventedLanguagesHaveADistinctValidWordForEveryConcept() {
        Lexicon lexicon = Lexicon.invent(new Random(1));
        Set<String> words = new HashSet<>();
        for (Concept concept : Concept.values()) {
            String word = lexicon.wordFor(concept);
            assertTrue(Words.valid(word), word);
            assertTrue(words.add(word), "words are distinct within a language");
            assertEquals(concept, lexicon.meaning(word));
        }
        assertEquals(Lexicon.invent(new Random(1)), lexicon, "deterministic for a seed");
        assertTrue(Lexicon.intelligibility(lexicon, Lexicon.invent(new Random(2))) < 0.1, "separate inventions are foreign");
    }

    @Test void neighboursLearnALanguageAlmostExactlyAndMishearSometimes() {
        Lexicon founder = Lexicon.invent(new Random(3));
        Random random = new Random(4);
        double total = 0;
        for (int i = 0; i < 200; i++) total += Lexicon.intelligibility(founder, Lexicon.learnFrom(founder, random));
        double mean = total / 200;
        assertTrue(mean > 0.95 && mean < 1.0, "mostly shared, with occasional mishearing: " + mean);
    }

    @Test void repeatedTeachingReplacesAWordAndRivalsFade() {
        Lexicon lexicon = Lexicon.invent(new Random(5));
        String old = lexicon.wordFor(Concept.FOOD);
        lexicon = lexicon.reinforce(Concept.FOOD, "mora", 0.3);
        assertEquals(old, lexicon.wordFor(Concept.FOOD), "one lesson doesn't override a native word");
        for (int i = 0; i < 4; i++) lexicon = lexicon.reinforce(Concept.FOOD, "mora", 0.3);
        assertEquals("mora", lexicon.wordFor(Concept.FOOD));
        for (int i = 0; i < 10; i++) lexicon = lexicon.reinforce(Concept.FOOD, "mora", 0.3);
        Lexicon finalLexicon = lexicon;
        assertEquals(1, lexicon.entries().stream().filter(e -> e.concept() == Concept.FOOD).count(), "the old word is forgotten");
        assertNotEquals(old, finalLexicon.wordFor(Concept.FOOD));
    }

    @Test void wordsPerConceptAreCapped() {
        Lexicon lexicon = Lexicon.empty();
        for (int round = 0; round < 40; round++)
            for (String word : List.of("ba", "be", "bi", "bo", "bu")) lexicon = lexicon.reinforce(Concept.BED, word, 0.3);
        assertTrue(lexicon.entries().size() <= Lexicon.MAX_WORDS_PER_CONCEPT, "competing words stay capped");
        assertThrows(IllegalArgumentException.class, () -> new Lexicon(List.of(new Lexicon.Entry(Concept.BED, "ba", 1),
                new Lexicon.Entry(Concept.BED, "be", 1), new Lexicon.Entry(Concept.BED, "bi", 1), new Lexicon.Entry(Concept.BED, "bo", 1))));
    }

    @Test void aVillageConvergesOnSharedWordsThroughConversation() {
        Random random = new Random(6);
        Lexicon[] village = new Lexicon[6];
        for (int i = 0; i < village.length; i++) village[i] = Lexicon.invent(new Random(100 + i)); // Six strangers.
        for (int round = 0; round < 6000; round++) {
            int speaker = random.nextInt(village.length), listener = random.nextInt(village.length);
            if (speaker == listener) continue;
            Concept concept = Concept.values()[random.nextInt(Concept.values().length)];
            village[listener] = village[listener].hear(concept, village[speaker].wordFor(concept), 0.2, random);
        }
        double total = 0; int pairs = 0;
        for (int a = 0; a < village.length; a++) for (int b = a + 1; b < village.length; b++) { total += Lexicon.intelligibility(village[a], village[b]); pairs++; }
        assertTrue(total / pairs > 0.7, "conversation produces a shared village language: " + total / pairs);
    }

    @Test void childrenStartWithOnlyCoreWordsFromTheirParents() {
        Lexicon a = Lexicon.invent(new Random(7)), b = Lexicon.invent(new Random(8));
        Lexicon child = Lexicon.child(a, b, new Random(9));
        assertTrue(child.developing());
        assertEquals(Lexicon.FIRST_WORDS.size(), child.entries().size());
        int fromA = 0, fromB = 0;
        for (Concept concept : Concept.values()) {
            String word = child.wordFor(concept);
            if (!Lexicon.FIRST_WORDS.contains(concept)) {
                assertNull(word, "later words require experience: " + concept);
                continue;
            }
            if (word.equals(a.wordFor(concept))) fromA++;
            else if (word.equals(b.wordFor(concept))) fromB++;
        }
        assertTrue(fromA > 0 && fromB > 0, fromA + " / " + fromB);
        assertTrue(child.evidence().isEmpty(), "parents do not transmit their accumulated learning evidence");
    }

    @Test void mishearingChangesExactlyOneLetterOfTheSameKind() {
        Random random = new Random(10);
        for (int i = 0; i < 500; i++) {
            String word = Words.invent(random), heard = Words.mishear(word, random);
            assertEquals(word.length(), heard.length());
            int diff = 0;
            for (int c = 0; c < word.length(); c++) if (word.charAt(c) != heard.charAt(c)) {
                diff++;
                assertEquals(Words.VOWELS.indexOf(word.charAt(c)) >= 0, Words.VOWELS.indexOf(heard.charAt(c)) >= 0);
            }
            assertEquals(1, diff);
        }
    }

    @Test void validatesWordsAndEntries() {
        assertTrue(Words.valid("mora"));
        assertFalse(Words.valid("a"));
        assertFalse(Words.valid("Mora"));
        assertFalse(Words.valid("mo ra"));
        assertFalse(Words.valid("x".repeat(13)));
        assertThrows(IllegalArgumentException.class, () -> new Lexicon.Entry(Concept.FOOD, "mora", 2));
        assertThrows(IllegalArgumentException.class, () -> new Lexicon(List.of(
                new Lexicon.Entry(Concept.FOOD, "mora", 1), new Lexicon.Entry(Concept.FOOD, "mora", .5))));
    }
}
