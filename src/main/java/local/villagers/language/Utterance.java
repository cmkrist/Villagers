package local.villagers.language;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Spoken sentence plus legacy concept-word diagnostics. Only sounds cross the interpretation boundary;
 * learning receives independently observable context, never these intended concept labels.
 */
public record Utterance(List<Spoken> words, boolean grounded, Sentence sentence) {
    public Utterance(List<Spoken> words, boolean grounded) { this(words, grounded, null); }
    public record Spoken(Concept concept, String word) {
        public Spoken { Objects.requireNonNull(concept); if (!Words.valid(word)) throw new IllegalArgumentException("Invalid word"); }
    }

    public Utterance { words = List.copyOf(words); }

    /** Say {@code concepts} with this lexicon, skipping concepts it has no word for. Null if nothing can be said. */
    public static Utterance of(Lexicon lexicon, List<Concept> concepts, boolean grounded) {
        List<Spoken> words = new ArrayList<>();
        for (Concept concept : concepts) {
            String word = lexicon.wordFor(concept);
            if (word != null) words.add(new Spoken(concept, word));
        }
        return words.isEmpty() ? null : new Utterance(words, grounded);
    }

    /** Production speech uses complete clauses. Unknown required words cause silence, never omitted meanings. */
    public static Utterance compose(Lexicon lexicon, List<Concept> concepts, boolean grounded) {
        var clauses = Clause.fromConcepts(concepts);
        if (clauses.isEmpty()) return null;
        var sentence = Sentence.speak(clauses, lexicon);
        if (sentence == null) return null;
        var legacy = of(lexicon, concepts, grounded);
        return legacy == null ? null : new Utterance(legacy.words(), grounded, sentence);
    }

    public static Utterance clauses(Lexicon lexicon, List<Clause> clauses, boolean grounded, boolean innovate) {
        var sentence = Sentence.speak(clauses,lexicon,innovate);
        if (sentence == null) return null;
        var words = new ArrayList<Spoken>();
        for (Clause clause : clauses) for (Concept c : clause.concepts()) {
            String word = lexicon.wordFor(c);
            if (word != null) words.add(new Spoken(c,word));
        }
        return new Utterance(words,grounded,sentence);
    }

    public Set<Concept> concepts() {
        Set<Concept> result = EnumSet.noneOf(Concept.class);
        words.forEach(spoken -> result.add(spoken.concept()));
        return result;
    }

    /**
     * Update a listener's glossary from observable candidate meanings supplied by the caller.
     * The speaker's grounding flag and private concept labels supply no evidence.
     */
    public Glossary teach(Glossary glossary, Set<Concept> ambient) {
        Set<Concept> context = EnumSet.noneOf(Concept.class);
        context.addAll(ambient);
        if (context.isEmpty()) return glossary;
        Glossary result = glossary;
        for (String sound : sounds()) if (!sound.equals(";")) result = result.hear(sound, context);
        return result;
    }

    /** Only audible sounds cross the acquisition boundary; context comes from the world adapter. */
    public Lexicon teach(Lexicon listener, Set<Concept> visibleContext, java.util.random.RandomGenerator random) {
        return listener.observe(sounds().stream().filter(w -> !w.equals(";")).toList(), visibleContext, random);
    }

    public List<String> sounds() { return sentence == null ? words.stream().map(Spoken::word).toList() : sentence.sounds(); }

    /** Correctly decoded fraction; familiar sounds with a different meaning do not count. */
    public double understoodBy(Lexicon listener) {
        if (words.isEmpty()) return 0;
        return words.stream().filter(w -> listener.meaning(w.word()) == w.concept()).count() / (double) words.size();
    }

    /** One rendered piece: the invented word, and its translation if this listener understands it. */
    public record Segment(String word, Concept translation) {}

    public List<Segment> render(Glossary glossary) {
        return words.stream().map(spoken -> new Segment(spoken.word(), glossary.translate(spoken.word()))).toList();
    }

    /** Plain-text rendering: understood words in English, others as spoken, e.g. "kalu food!". */
    public String text(Glossary glossary) {
        StringBuilder text = new StringBuilder();
        for (Segment segment : render(glossary)) {
            if (!text.isEmpty()) text.append(' ');
            text.append(segment.translation() == null ? segment.word() : segment.translation().gloss());
        }
        return text.toString();
    }

    /** Diagnostic correctness for legacy words, not the listener's subjective confidence or an AI permission. */
    public double understoodBy(Glossary glossary) {
        long known = words.stream().filter(w -> glossary.translate(w.word()) == w.concept()).count();
        return words.isEmpty() ? 0 : known / (double) words.size();
    }
}
