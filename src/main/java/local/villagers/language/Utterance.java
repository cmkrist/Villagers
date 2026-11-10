package local.villagers.language;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Something a villager says: concepts in order, the words its lexicon has for them, and whether the concepts are
 * apparent from the situation. Grounded speech (a villager saying "hungry" as it goes for food) teaches listeners;
 * ungrounded speech (gossip about something elsewhere) does not, because nothing shows what the words mean.
 */
public record Utterance(List<Spoken> words, boolean grounded) {
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

    public Set<Concept> concepts() {
        Set<Concept> result = EnumSet.noneOf(Concept.class);
        words.forEach(spoken -> result.add(spoken.concept()));
        return result;
    }

    /**
     * Update a listener's glossary. Each word counts every concept apparent to the listener as a possible meaning:
     * the utterance's own concepts when grounded, plus {@code ambient} (what else the listener can see, such as the
     * item they hold or the villager's current activity).
     */
    public Glossary teach(Glossary glossary, Set<Concept> ambient) {
        Set<Concept> context = EnumSet.noneOf(Concept.class);
        context.addAll(ambient);
        if (grounded) context.addAll(concepts());
        if (context.isEmpty()) return glossary;
        Glossary result = glossary;
        for (Spoken spoken : words) result = result.hear(spoken.word(), context);
        return result;
    }

    /** Only audible sounds cross the acquisition boundary; context comes from the world adapter. */
    public Lexicon teach(Lexicon listener, Set<Concept> visibleContext, java.util.random.RandomGenerator random) {
        return listener.observe(sounds(), visibleContext, random);
    }

    public List<String> sounds() { return words.stream().map(Spoken::word).toList(); }

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

    /** Fraction of the words this listener understands, 0 to 1. */
    public double understoodBy(Glossary glossary) {
        long known = render(glossary).stream().filter(segment -> segment.translation() != null).count();
        return known / (double) words.size();
    }
}
