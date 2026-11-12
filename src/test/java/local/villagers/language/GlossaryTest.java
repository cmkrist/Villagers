package local.villagers.language;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class GlossaryTest {
    private static final UUID PLAYER = new UUID(3, 3);
    @TempDir Path root;

    @Test void aWordIsUnderstoodAfterBeingHeardInVaryingSituations() {
        Glossary glossary = Glossary.empty(PLAYER);
        glossary = glossary.hear("mora", Set.of(Concept.FOOD, Concept.WALK));
        glossary = glossary.hear("mora", Set.of(Concept.FOOD, Concept.BREAD));
        assertNull(glossary.translate("mora"), "two hearings are not enough");
        glossary = glossary.hear("mora", Set.of(Concept.FOOD, Concept.DANGER));
        assertEquals(Concept.FOOD, glossary.translate("mora"));
    }

    @Test void aWordAlwaysHeardWithTheSameDistractorStaysAmbiguous() {
        Glossary glossary = Glossary.empty(PLAYER);
        for (int i = 0; i < 10; i++) glossary = glossary.hear("kalu", Set.of(Concept.WANT, Concept.FOOD));
        assertNull(glossary.translate("kalu"), "nothing tells WANT from FOOD apart");
        for (int i = 0; i < 10; i++) glossary = glossary.hear("kalu", Set.of(Concept.WANT, Concept.BED));
        assertEquals(Concept.WANT, glossary.translate("kalu"), "hearing it with other nouns resolves it");
    }

    @Test void aWordWithDifferentMeaningsInTwoVillagesIsAmbiguousUntilUnified() {
        Glossary glossary = Glossary.empty(PLAYER);
        for (int i = 0; i < 5; i++) glossary = glossary.hear("tepi", Set.of(Concept.BREAD));
        for (int i = 0; i < 5; i++) glossary = glossary.hear("tepi", Set.of(Concept.BED));
        assertNull(glossary.translate("tepi"));
        for (int i = 0; i < 10; i++) glossary = glossary.hear("tepi", Set.of(Concept.BREAD));
        assertEquals(Concept.BREAD, glossary.translate("tepi"));
    }

    @Test void onlyObservableContextTeachesRegardlessOfTheSpeakerGroundingFlag() {
        Lexicon lexicon = Lexicon.invent(new Random(1));
        Utterance grounded = Utterance.of(lexicon, List.of(Concept.HUNGRY), true);
        Utterance gossip = Utterance.of(lexicon, List.of(Concept.HUNGRY), false);
        Glossary fromGossip = Glossary.empty(PLAYER), fromSituation = Glossary.empty(PLAYER);
        for (int i = 0; i < 5; i++) {
            fromGossip = gossip.teach(fromGossip, Set.of());
            fromSituation = grounded.teach(fromSituation, Set.of(Concept.HUNGRY, i % 2 == 0 ? Concept.WALK : Concept.FOOD));
        }
        String word = lexicon.wordFor(Concept.HUNGRY);
        assertNull(fromGossip.translate(word));
        assertEquals(Concept.HUNGRY, fromSituation.translate(word));
        assertEquals("hungry", grounded.text(fromSituation));
        assertEquals(word, grounded.text(Glossary.empty(PLAYER)));
        assertEquals(1.0, grounded.understoodBy(fromSituation));
    }

    @Test void glossariesAreBoundedAndForgetTheLeastHeardWords() {
        Glossary glossary = Glossary.empty(PLAYER);
        glossary = glossary.hear("keep", Set.of(Concept.FOOD));
        glossary = glossary.hear("keep", Set.of(Concept.FOOD));
        Random random = new Random(2);
        for (int i = 0; i < Glossary.MAX_WORDS + 20; i++) glossary = glossary.hear(Words.invent(random) + "x", Set.of(Concept.BED));
        assertEquals(Glossary.MAX_WORDS, glossary.words().size());
        assertTrue(glossary.words().stream().anyMatch(w -> w.word().equals("keep")));
    }

    @Test void glossariesSurviveReloadAndUnknownFormatsAreNeverOverwritten() throws Exception {
        GlossaryStore store = new GlossaryStore(root);
        Glossary glossary = Glossary.empty(PLAYER);
        for (int i = 0; i < 3; i++) glossary = glossary.hear("mora", Set.of(Concept.FOOD));
        assertTrue(store.save(glossary));
        StringBuilder problem = new StringBuilder();
        assertEquals(glossary, new GlossaryStore(root).load(PLAYER, problem));
        assertTrue(problem.isEmpty());

        UUID other = new UUID(4, 4);
        Path file = Files.createDirectories(root.resolve("players")).resolve(other + ".json");
        String future = "{\"schemaVersion\":99}";
        Files.writeString(file, future);
        GlossaryStore fresh = new GlossaryStore(root);
        assertTrue(fresh.load(other, new StringBuilder()).words().isEmpty());
        assertFalse(fresh.save(Glossary.empty(other)));
        assertEquals(future, Files.readString(file));
    }

    @Test void bookSavesChangesInTheBackground() {
        GlossaryBook book = new GlossaryBook(new GlossaryStore(root), problem -> fail(problem));
        book.join(PLAYER);
        book.update(PLAYER, g -> g.hear("mora", Set.of(Concept.FOOD)));
        book.leave(PLAYER);
        assertTrue(book.close(5000));
        assertEquals(1, new GlossaryStore(root).load(PLAYER, new StringBuilder()).words().size());
    }

    @Test void itemsMapToConcepts() {
        assertEquals(Concept.BREAD, Concept.forItem("minecraft:bread"));
        assertEquals(Concept.HOE, Concept.forItem("minecraft:iron_hoe"));
        assertEquals(Concept.SEEDS, Concept.forItem("minecraft:beetroot_seeds"));
        assertNull(Concept.forItem("minecraft:dirt"));
        assertEquals(Concept.FOOD, Concept.parse(" Food "));
        assertNull(Concept.parse("spaceship"));
    }
}
