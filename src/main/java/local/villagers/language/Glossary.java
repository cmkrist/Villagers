package local.villagers.language;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * What one player has worked out about villager words, by cross-situational learning: each time a player hears a
 * word, every concept apparent in the situation is counted as a possible meaning. A word is understood once one
 * meaning has been seen at least {@link #MIN_EVIDENCE} times and at least {@link #MARGIN} times as often as any
 * other. Words are global, not per village, so a word that means different things in two villages stays
 * ambiguous until the player unifies them. Immutable.
 */
public record Glossary(int schemaVersion, UUID player, List<WordEvidence> words) {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_WORDS = 256;
    public static final int MAX_MEANINGS = 8;
    public static final int MIN_EVIDENCE = 3;
    public static final double MARGIN = 1.5;

    public record Meaning(Concept concept, int count) {
        public Meaning {
            Objects.requireNonNull(concept);
            if (count < 1) throw new IllegalArgumentException("Meaning counts start at one");
        }
    }

    /** Evidence for one word; {@code heard} orders eviction (least-heard words are forgotten first). */
    public record WordEvidence(String word, long heard, List<Meaning> meanings) {
        public WordEvidence {
            if (!Words.valid(word)) throw new IllegalArgumentException("Invalid word " + word);
            if (heard < 1) throw new IllegalArgumentException("A recorded word was heard at least once");
            meanings = List.copyOf(Objects.requireNonNull(meanings));
            if (meanings.isEmpty() || meanings.size() > MAX_MEANINGS
                    || meanings.stream().map(Meaning::concept).distinct().count() != meanings.size()
                    || meanings.stream().anyMatch(m -> m.count() > heard))
                throw new IllegalArgumentException("Invalid meanings");
        }

        /** The understood meaning, or null while the evidence is too thin or ambiguous. */
        public Concept translation() {
            List<Meaning> sorted = meanings.stream().sorted(Comparator.comparingInt(Meaning::count).reversed()).toList();
            Meaning best = sorted.getFirst();
            if (best.count() < MIN_EVIDENCE) return null;
            if (sorted.size() > 1 && best.count() < MARGIN * sorted.get(1).count()) return null;
            return best.concept();
        }
    }

    public Glossary {
        if (schemaVersion != SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported glossary schema " + schemaVersion);
        Objects.requireNonNull(player);
        words = List.copyOf(Objects.requireNonNull(words));
        if (words.size() > MAX_WORDS) throw new IllegalArgumentException("Too many glossary words");
        if (words.stream().map(WordEvidence::word).distinct().count() != words.size())
            throw new IllegalArgumentException("Duplicate glossary word");
    }

    public static Glossary empty(UUID player) { return new Glossary(SCHEMA_VERSION, player, List.of()); }

    /** The understood meaning of a word, or null. */
    public Concept translate(String word) {
        for (WordEvidence evidence : words) if (evidence.word().equals(word)) return evidence.translation();
        return null;
    }

    /** Hear {@code word} in a situation where the {@code context} concepts were apparent. */
    public Glossary hear(String word, Collection<Concept> context) {
        List<WordEvidence> next = observe(words, word, context);
        return next == words ? this : new Glossary(SCHEMA_VERSION, player, next);
    }

    /** Shared bounded cross-situational evidence for players and villagers; no intended meaning is supplied. */
    public static List<WordEvidence> observe(List<WordEvidence> words, String word, Collection<Concept> context) {
        if (!Words.valid(word) || context.isEmpty()) return words;
        Map<String, WordEvidence> byWord = new LinkedHashMap<>();
        for (WordEvidence evidence : words) byWord.put(evidence.word(), evidence);
        WordEvidence previous = byWord.remove(word);
        Map<Concept, Integer> counts = new EnumMap<>(Concept.class);
        if (previous != null) for (Meaning meaning : previous.meanings()) counts.put(meaning.concept(), meaning.count());
        for (Concept concept : new java.util.HashSet<>(context)) counts.merge(concept, 1, (a, b) -> a == Integer.MAX_VALUE ? a : a + b);
        List<Meaning> meanings = counts.entrySet().stream()
                .map(e -> new Meaning(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(Meaning::count).reversed())
                .limit(MAX_MEANINGS).toList();
        long heard = previous == null ? 1 : previous.heard() == Long.MAX_VALUE ? Long.MAX_VALUE : previous.heard() + 1;
        List<WordEvidence> next = new ArrayList<>(byWord.values());
        next.add(new WordEvidence(word, heard, meanings));
        if (next.size() > MAX_WORDS) next.remove(next.stream().filter(e -> !e.word().equals(word))
                .min(Comparator.comparingLong(WordEvidence::heard)).orElseThrow());
        return List.copyOf(next);
    }

    /** Words this player understands, alphabetically. */
    public Map<String, Concept> understood() {
        Map<String, Concept> result = new java.util.TreeMap<>();
        for (WordEvidence evidence : words) {
            Concept meaning = evidence.translation();
            if (meaning != null) result.put(evidence.word(), meaning);
        }
        return result;
    }
}
