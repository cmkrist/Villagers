package local.villagers.language;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SentenceTest {
    private static Random clear() { return new Random(1) { @Override public double nextDouble() { return .5; } }; }
    @Test void sameBiomeSpeakersComposeAndInterpretAllSupportedSituations() {
        for (String biome : List.of("plains", "desert", "forest", "snowy_slopes")) {
            var speaker = BiomeDialect.foundingLanguage("minecraft:" + biome);
            for (var situation : Phrases.Situation.values()) {
                var said = Utterance.compose(speaker, Phrases.concepts(situation, new Random(3)), true);
                assertNotNull(said, situation.toString());
                var heard = Sentence.interpret(said.sounds(), speaker);
                assertTrue(said.sentence().correct(heard), biome + " " + situation);
                assertFalse(heard.english().isBlank());
            }
        }
    }
    @Test void fourFoundingCulturesHaveDifferentGrammarAndStableWords() {
        var orders = new HashSet<Grammar.Order>();
        for (String biome : List.of("plains", "desert", "forest", "snowy_slopes")) {
            var a = BiomeDialect.foundingLanguage("minecraft:" + biome);
            assertEquals(a, BiomeDialect.foundingLanguage("minecraft:" + biome));
            orders.add(a.grammar().preferred());
        }
        assertEquals(4, orders.size());
    }
    @Test void repeatedVariedPublicEncountersTeachForeignWordsAndGrammarFromNothing() {
        var speaker = BiomeDialect.foundingLanguage("minecraft:snowy_slopes");
        Lexicon learner = Lexicon.empty();
        Glossary player = Glossary.empty(UUID.randomUUID());
        var events = List.of(new Clause(Concept.I, Concept.SEE, Concept.BED),
                new Clause(Concept.I, Concept.SEE, Concept.FOOD), new Clause(Concept.I, Concept.SEE, Concept.WOOD),
                new Clause(Concept.I, Concept.SAY, Concept.HELLO), new Clause(Concept.I, Concept.SAY, Concept.YES),
                new Clause(Concept.I, Concept.SAY, Concept.THANKS));
        for (int round = 0; round < 30; round++) for (var event : events) {
            var wire = Sentence.speak(List.of(event), speaker).sounds();
            learner = LanguageLearning.hear(learner, wire, Set.copyOf(event.concepts()), event, clear());
            player = LanguageLearning.hear(player, wire, Set.copyOf(event.concepts()), event);
        }
        var said = Sentence.speak(List.of(events.getFirst()), speaker);
        assertTrue(said.correct(Sentence.interpret(said.sounds(), learner)));
        assertTrue(said.correct(Sentence.interpret(said.sounds(), player::translate, player.grammar())));
        assertEquals(Grammar.Order.OVS, learner.grammar().preferred());
        assertNull(learner.wordFor(Concept.SWORD), "no unrelated vocabulary is supplied");
    }
    @Test void confidenceCanBePerfectWhileTheInterpretedMeaningIsWrong() {
        var speaker = BiomeDialect.foundingLanguage("minecraft:plains");
        var mistaken = new Lexicon(speaker.entries().stream().map(e ->
                e.concept() == Concept.FOOD ? new Lexicon.Entry(Concept.BED, e.word(), e.strength())
                : e.concept() == Concept.BED ? new Lexicon.Entry(Concept.FOOD, e.word(), e.strength()) : e).toList(),
                List.of(), false, speaker.grammar());
        var said = Utterance.compose(speaker, List.of(Concept.WANT, Concept.FOOD), true);
        var heard = Sentence.interpret(said.sounds(), mistaken);
        assertEquals(1, heard.confidence());
        assertEquals(List.of(new Clause(Concept.I, Concept.WANT, Concept.BED)), heard.clauses());
        assertFalse(said.sentence().correct(heard));
        assertFalse(local.villagers.relations.FoodRequest.understands(said, mistaken, Concept.WANT, Concept.FOOD));
        // AI policy operates on the interpreted request, not the sender's hidden intent.
        assertTrue(local.villagers.relations.FoodRequest.understands(said, mistaken, Concept.WANT, Concept.BED));
    }
    @Test void acousticallyIndistinguishableOrdersBecomeAmbiguousWithoutReplacingNativeProduction() {
        var speaker = BiomeDialect.foundingLanguage("minecraft:snowy_slopes");
        var listener = speaker.withGrammar(Grammar.founding(Grammar.Order.SVO));
        var event = new Clause(Concept.FRIEND, Concept.SEE, Concept.MONSTER);
        var said = Sentence.speak(List.of(event), speaker);
        var wrong = Sentence.interpret(said.sounds(), listener);
        assertEquals(new Clause(Concept.MONSTER, Concept.SEE, Concept.FRIEND), wrong.clauses().getFirst());
        assertEquals(1, wrong.confidence());
        var original = listener;
        for (int i = 0; i < 7; i++) listener = LanguageLearning.hear(listener, said.sounds(), Set.copyOf(event.concepts()), event, clear());
        assertEquals(Grammar.Order.SVO, listener.grammar().preferred());
        assertFalse(Sentence.interpret(said.sounds(),listener).complete(),"Identical lexical cues cannot tell reversed subjects apart");
        for (Concept c : Concept.values()) assertEquals(original.wordFor(c), listener.wordFor(c), "grammar learning does not replace spoken vocabulary");
        assertEquals(Grammar.Order.SVO, original.grammar().preferred());
    }
    @Test void privateMeaningCannotTeachOrRepairGrammarWithoutObservableEvidence() {
        var speaker = BiomeDialect.foundingLanguage("minecraft:desert");
        var heard = Utterance.compose(speaker, List.of(Concept.WANT, Concept.FOOD), false);
        var empty = Lexicon.empty();
        var next = LanguageLearning.hear(empty, heard.sounds(), Set.of(), null, clear());
        assertEquals(empty, next);
        var player = Glossary.empty(UUID.randomUUID());
        assertEquals(player, LanguageLearning.hear(player, heard.sounds(), Set.of(), null));
        var liar = new Sentence(List.of(new Clause(Concept.I, Concept.IS, Concept.HAPPY)), heard.sounds());
        assertEquals(Sentence.interpret(heard.sounds(), speaker), Sentence.interpret(liar.sounds(), speaker));
    }
    @Test void requiredWordsNegationAndTenseSurviveTheWireAndCannotBeDropped() {
        var lexicon = BiomeDialect.foundingLanguage("minecraft:forest");
        var clause = new Clause(Concept.I, Concept.WANT, Concept.FOOD, true, true);
        var said = Sentence.speak(List.of(clause), lexicon);
        assertTrue(said.correct(Sentence.interpret(said.sounds(), lexicon)));
        assertEquals("I did not want food.", Sentence.interpret(said.sounds(), lexicon).english());
        var limited = new Lexicon(lexicon.entries().stream().filter(e -> e.concept() != Concept.NOT).toList(), List.of(), false, lexicon.grammar());
        assertNull(Sentence.speak(List.of(clause), limited));
        assertFalse(Sentence.interpret(said.sounds(), limited).complete());
        assertNull(Utterance.compose(new Lexicon(List.of(new Lexicon.Entry(Concept.FOOD, "mora", 1))), List.of(Concept.WANT, Concept.FOOD), true));
    }
    @Test void playerWrongTranslationsDoNotBecomeObjectiveComprehension() {
        var words = new Utterance(List.of(new Utterance.Spoken(Concept.FOOD, "mora")), false);
        var player = Glossary.empty(UUID.randomUUID());
        for (int i = 0; i < 3; i++) player = player.hear("mora", Set.of(Concept.BED));
        assertEquals("bed", words.text(player));
        assertEquals(0, words.understoodBy(player));
        assertEquals(player, words.teach(player, Set.of()));
    }
    @Test void vocabularyMisinterpretationCanBeRepairedThroughContradictoryEvidence() {
        var learner = new Lexicon(List.of(new Lexicon.Entry(Concept.BED, "mora", 1)));
        for (int i = 0; i < 5; i++) learner = learner.observe(List.of("mora"), Set.of(Concept.FOOD), clear());
        assertEquals(Concept.FOOD, learner.meaning("mora"));
        assertNull(learner.wordFor(Concept.BED));
    }
    @Test void grammarEvidenceIsBoundedAndAmbiguousOrUnknownEventsTeachNothing() {
        Grammar grammar = Grammar.founding(Grammar.Order.SVO);
        assertSame(grammar, grammar.observe(List.of("aa", "bb", "aa"), w -> null, new Clause(Concept.I, Concept.SEE, Concept.I)));
        for (int i = 0; i < 10000; i++) grammar = grammar.reinforce(Grammar.Order.SOV, 1);
        assertTrue(grammar.evidence().stream().allMatch(n -> n <= 1000));
        assertThrows(IllegalArgumentException.class, () -> new Grammar(List.of(1)));
        assertThrows(IllegalArgumentException.class, () -> Sentence.interpret(Collections.nCopies(Sentence.MAX_SOUNDS + 1, "mora"), Lexicon.empty()));
    }
    @Test void equalContactEvidenceRetainsTheExistingConventionRatherThanFavoringSvo() {
        var grammar = Grammar.founding(Grammar.Order.OVS);
        grammar = grammar.reinforce(Grammar.Order.SVO, 6);
        assertEquals(Grammar.Order.OVS, grammar.preferred());
        assertEquals(Grammar.Order.SVO, grammar.reinforce(Grammar.Order.SVO, 1).preferred());
    }
    @Test void everyExistingCommerceAndCarePhraseStillHasACompleteGrammaticalForm() {
        var lexicon = BiomeDialect.foundingLanguage("minecraft:plains");
        for (var concepts : List.of(List.of(Concept.WANT, Concept.TRADE, Concept.BREAD, Concept.EMERALD),
                List.of(Concept.YES, Concept.TRADE), List.of(Concept.WANT, Concept.FRIEND, Concept.BED),
                List.of(Concept.WANT, Concept.FOOD, Concept.HUNGRY, Concept.HURT))) {
            var said = Utterance.compose(lexicon, concepts, true);
            assertNotNull(said);
            assertTrue(local.villagers.relations.FoodRequest.understands(said, lexicon, concepts.toArray(Concept[]::new)));
        }
    }
}
