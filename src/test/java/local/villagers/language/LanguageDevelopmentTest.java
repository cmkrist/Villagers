package local.villagers.language;

import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LanguageDevelopmentTest {
    private static Random clearHearing() {
        return new Random(1) { @Override public double nextDouble() { return .5; } };
    }

    private static Lexicon hear(Lexicon learner, String word, Set<Concept> context, int repetitions) {
        for (int i = 0; i < repetitions; i++) learner = learner.observe(List.of(word), context, clearHearing());
        return learner;
    }

    @Test void ambiguityMustResolveBeforeAWordBecomesUsable() {
        Lexicon learner = hear(Lexicon.empty(), "mora", Set.of(Concept.FOOD, Concept.BED), 4);
        assertNull(learner.meaning("mora"));
        learner = hear(learner, "mora", Set.of(Concept.FOOD), 3);
        assertEquals(Concept.FOOD, learner.meaning("mora"));
        assertNull(learner.wordFor(Concept.BED), "no false label copied from the speaker");
    }

    @Test void oneEncounterIsNotFluencyAndEvidenceAccumulatesWithoutChangingTheOriginal() {
        Lexicon original = Lexicon.empty();
        Lexicon learner = hear(original, "mora", Set.of(Concept.FOOD), 1);
        assertNull(learner.meaning("mora"));
        assertTrue(original.evidence().isEmpty());
        learner = hear(learner, "mora", Set.of(Concept.FOOD), 3);
        assertEquals(Concept.FOOD, learner.meaning("mora"));
    }

    @Test void learningReceivesSoundsAndContextRatherThanHiddenSpeakerLabels() {
        var foodLabel = new Utterance(List.of(new Utterance.Spoken(Concept.FOOD, "mora")), true);
        var falseLabel = new Utterance(List.of(new Utterance.Spoken(Concept.MONSTER, "mora")), true);
        var a = foodLabel.teach(Lexicon.empty(), Set.of(Concept.BED), clearHearing());
        var b = falseLabel.teach(Lexicon.empty(), Set.of(Concept.BED), clearHearing());
        assertEquals(a, b);
        assertSame(a, foodLabel.teach(a, Set.of(), clearHearing()));
    }

    @Test void aKnownNounHelpsLearnTheOtherWordInARequest() {
        Lexicon learner = new Lexicon(List.of(new Lexicon.Entry(Concept.FOOD, "mora", 1)));
        for (int i = 0; i < 4; i++)
            learner = learner.observe(List.of("badu", "mora"), Set.of(Concept.WANT, Concept.FOOD), clearHearing());
        assertEquals(Concept.WANT, learner.meaning("badu"));
        assertEquals(Concept.FOOD, learner.meaning("mora"));
    }

    @Test void synonymsCanBeUnderstoodBeforeTheyReplaceNativeSpeech() {
        Lexicon local = new Lexicon(List.of(new Lexicon.Entry(Concept.FOOD, "mora", 1)));
        var bilingual = hear(local, "badu", Set.of(Concept.FOOD), 4);
        assertEquals("mora", bilingual.wordFor(Concept.FOOD));
        assertEquals(Concept.FOOD, bilingual.meaning("badu"));
        var foreign = new Lexicon(List.of(new Lexicon.Entry(Concept.FOOD, "badu", 1)));
        assertEquals(0, Lexicon.intelligibility(local, foreign));
        assertEquals(1, Lexicon.comprehension(foreign, bilingual));
        assertEquals(0, Lexicon.comprehension(bilingual, foreign), "comprehension is directional");
    }

    @Test void conflictingHomonymsDoNotCountAsUnderstanding() {
        var learner = new Lexicon(List.of(new Lexicon.Entry(Concept.FOOD, "mora", 1),
                new Lexicon.Entry(Concept.MONSTER, "mora", 1)));
        assertNull(learner.meaning("mora"));
        var message = new Utterance(List.of(new Utterance.Spoken(Concept.FOOD, "mora")), false);
        assertEquals(0, message.understoodBy(learner));
        assertEquals(0, new Utterance(List.of(), false).understoodBy(learner));
    }

    @Test void understoodExchangesStrengthenOnlyDecodedMeanings() {
        var learner = new Lexicon(List.of(new Lexicon.Entry(Concept.FOOD, "mora", .3)));
        var confirmed = learner.confirm(List.of("mora", "badu"));
        assertEquals(.4, confirmed.entries().getFirst().strength(), 1e-10);
        assertNull(confirmed.meaning("badu"));
    }

    @Test void unfamiliarConversationsCannotTransferFoodUntilBothWordsAreLearned() {
        var speaker = BiomeDialect.foundingLanguage("minecraft:desert");
        var learner = BiomeDialect.foundingLanguage("minecraft:plains");
        var request = Utterance.of(speaker, List.of(Concept.WANT, Concept.FOOD), true);
        assertFalse(local.villagers.relations.FoodRequest.understands(request, learner, Concept.WANT, Concept.FOOD));
        // Meals ground FOOD, then understood FOOD provides an anchor in the request.
        learner = hear(learner, speaker.wordFor(Concept.FOOD), Set.of(Concept.FOOD), 4);
        for (int i = 0; i < 4; i++) learner = request.teach(learner, Set.of(Concept.WANT, Concept.FOOD), clearHearing());
        assertTrue(local.villagers.relations.FoodRequest.understands(request, learner, Concept.WANT, Concept.FOOD));
        assertEquals(BiomeDialect.foundingLanguage("minecraft:plains").wordFor(Concept.FOOD), learner.wordFor(Concept.FOOD));
    }

    @Test void biomeResidentsShareFoundingWordsButForeignDialectsDiffer() {
        var plains = BiomeDialect.foundingLanguage("minecraft:plains");
        assertEquals(plains, BiomeDialect.foundingLanguage("minecraft:plains"));
        for (String biome : List.of("minecraft:desert", "minecraft:forest", "minecraft:snowy_slopes"))
            assertTrue(Lexicon.intelligibility(plains, BiomeDialect.foundingLanguage(biome)) < .2, biome);
        assertThrows(IllegalArgumentException.class, () -> BiomeDialect.foundingLanguage("bad biome"));
    }

    @Test void aChildAcquiresAParentsLaterWordsThroughLocalEvidence() {
        var parent = BiomeDialect.foundingLanguage("minecraft:forest");
        var child = Lexicon.child(parent, parent, clearHearing());
        assertNull(child.wordFor(Concept.BED));
        var lesson = Utterance.of(parent, List.of(Concept.BED), true);
        for (int i = 0; i < 4; i++) child = lesson.teach(child, Set.of(Concept.BED), clearHearing());
        assertEquals(Concept.BED, child.meaning(parent.wordFor(Concept.BED)));
        assertEquals(parent.wordFor(Concept.BED), child.wordFor(Concept.BED));
        assertNull(child.wordFor(Concept.SWORD), "learning a bed word grants no unrelated knowledge");
        assertEquals(Lexicon.FIRST_WORDS.size() + 1, child.knownConcepts());
    }

    @Test void contextualLearningRetainsBoundsEvenAfterManyNewWords() {
        Lexicon learner = Lexicon.empty();
        for (int i = 0; i < 300; i++) {
            String word = "ba" + (char) ('a' + i / 26) + (char) ('a' + i % 26);
            learner = hear(learner, word, Set.of(Concept.FOOD), 4);
        }
        assertEquals(Glossary.MAX_WORDS, learner.evidence().size());
        assertTrue(learner.entries().size() <= Lexicon.MAX_WORDS_PER_CONCEPT);
    }
}
