package local.villagers.language;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * One villager's vocabulary: for each concept, up to {@link #MAX_WORDS_PER_CONCEPT} competing words with a
 * strength in (0, 1]. The strongest is spoken. Contextual hearing builds receptive synonyms; explicit lessons
 * and confirmed exchanges can shift spoken preferences. Occasional mishearing lets dialects drift apart.
 * Immutable: every change returns a new lexicon.
 */
public record Lexicon(List<Entry> entries, List<Glossary.WordEvidence> evidence, boolean developing) {
    /** Old saves contain entries only: established vocabularies retain their meanings. */
    public Lexicon(List<Entry> entries) { this(entries, List.of(), false); }
    public static final double RECOGNITION_STRENGTH = 0.2;
    public static final Set<Concept> FIRST_WORDS = Set.of(
            Concept.HELLO, Concept.YES, Concept.NO, Concept.WANT, Concept.FOOD, Concept.DANGER);
    public static final int MAX_WORDS_PER_CONCEPT = 3;
    public static final double MIN_STRENGTH = 0.05;
    public static final double MISHEAR_RATE = 0.02;
    /** Strength a word starts with when copied from a neighbour or parent. */
    static final double COPIED_STRENGTH = 0.6;

    public record Entry(Concept concept, String word, double strength) {
        public Entry {
            Objects.requireNonNull(concept);
            if (!Words.valid(word)) throw new IllegalArgumentException("Invalid word " + word);
            if (!Double.isFinite(strength) || strength < MIN_STRENGTH || strength > 1)
                throw new IllegalArgumentException("Invalid word strength");
        }
    }

    public Lexicon {
        entries = List.copyOf(Objects.requireNonNull(entries));
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        if (evidence.size() > Glossary.MAX_WORDS
                || evidence.stream().map(Glossary.WordEvidence::word).distinct().count() != evidence.size())
            throw new IllegalArgumentException("Invalid vocabulary evidence");
        Map<Concept, Integer> perConcept = new EnumMap<>(Concept.class);
        Set<String> seen = new HashSet<>();
        for (Entry entry : entries) {
            if (perConcept.merge(entry.concept(), 1, Integer::sum) > MAX_WORDS_PER_CONCEPT)
                throw new IllegalArgumentException("Too many words for " + entry.concept());
            if (!seen.add(entry.concept() + ":" + entry.word())) throw new IllegalArgumentException("Duplicate word");
        }
    }

    public static Lexicon empty() { return new Lexicon(List.of()); }

    public boolean isEmpty() { return entries.isEmpty(); }

    /** Name a newly encountered concept in one's own language; listeners still have to learn the word. */
    public Lexicon nameMissing(Concept concept,RandomGenerator random) {
        Objects.requireNonNull(concept);Objects.requireNonNull(random);
        if(wordFor(concept)!=null)return this;
        Set<String> used=new HashSet<>();for(var entry:entries)used.add(entry.word());
        for(int i=0;i<64;i++) {
            String word=Words.invent(random);
            if(used.contains(word))continue;
            var named=new ArrayList<>(entries);named.add(new Entry(concept,word,COPIED_STRENGTH));return new Lexicon(named, evidence, developing);
        }
        return this;
    }

    /** A brand-new language: a distinct invented word, at full strength, for every concept. */
    public static Lexicon invent(RandomGenerator random) {
        List<Entry> entries = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (Concept concept : Concept.values()) {
            String word;
            do word = Words.invent(random); while (!used.add(word));
            entries.add(new Entry(concept, word, 1));
        }
        return new Lexicon(entries);
    }

    /** Learn a neighbour's language: copy each of its spoken words, occasionally misheard. */
    public static Lexicon learnFrom(Lexicon model, RandomGenerator random) {
        List<Entry> entries = new ArrayList<>();
        for (Concept concept : Concept.values()) {
            String word = model.wordFor(concept);
            if (word != null) entries.add(new Entry(concept, maybeMishear(word, random), COPIED_STRENGTH));
        }
        return new Lexicon(entries);
    }

    /** A child's small inherited vocabulary; other meanings must be acquired through life. */
    public static Lexicon child(Lexicon a, Lexicon b, RandomGenerator random) {
        List<Entry> entries = new ArrayList<>();
        for (Concept concept : Concept.values()) {
            if (!FIRST_WORDS.contains(concept)) continue;
            String first = a.wordFor(concept), second = b.wordFor(concept);
            String word = first == null ? second : second == null ? first : random.nextBoolean() ? first : second;
            if (word != null) entries.add(new Entry(concept, maybeMishear(word, random), COPIED_STRENGTH));
        }
        return new Lexicon(entries, List.of(), true);
    }

    /** The word this villager says for a concept, or null if it has none. */
    public String wordFor(Concept concept) {
        return entries.stream().filter(e -> e.concept() == concept)
                .max(Comparator.comparingDouble(Entry::strength)).map(Entry::word).orElse(null);
    }

    /** Familiar synonyms are understood even when not spoken. Conflicting homonyms remain ambiguous. */
    public Concept meaning(String word) {
        Concept result = null;
        for (Entry entry : entries) {
            if (!entry.word().equals(word) || entry.strength() < RECOGNITION_STRENGTH) continue;
            if (result != null && result != entry.concept()) return null;
            result = entry.concept();
        }
        return result;
    }

    public int knownConcepts() {
        return (int) entries.stream().filter(e -> meaning(e.word()) == e.concept())
                .map(Entry::concept).distinct().count();
    }

    /**
     * Learn from sounds and independently visible context, never from a speaker's private concept labels.
     * Repeated, varied situations resolve ambiguity before a word gains usable strength.
     */
    public Lexicon observe(List<String> sounds, Set<Concept> context, RandomGenerator random) {
        Objects.requireNonNull(random);
        if (context.isEmpty() || sounds.isEmpty()) return this;
        if (sounds.size() > Concept.values().length) throw new IllegalArgumentException("Utterance too long");
        // Known words anchor the remaining sounds: after learning FOOD, WANT FOOD can teach WANT.
        Set<Concept> unassigned = java.util.EnumSet.noneOf(Concept.class);
        unassigned.addAll(context);
        for (String sound : sounds) {
            Concept known = meaning(sound);
            if (known != null) unassigned.remove(known);
        }
        Lexicon result = this;
        for (String sound : new java.util.LinkedHashSet<>(sounds)) {
            if (!Words.valid(sound)) throw new IllegalArgumentException("Invalid heard word");
            String heard = maybeMishear(sound, random);
            Concept familiar = meaning(sound);
            Set<Concept> candidates = familiar != null && context.contains(familiar) ? Set.of(familiar)
                    : unassigned.isEmpty() ? context : unassigned;
            var next = Glossary.observe(result.evidence(), heard, candidates);
            result = new Lexicon(result.entries(), next, result.developing());
            var word = next.stream().filter(e -> e.word().equals(heard)).findFirst().orElseThrow();
            Concept inferred = word.translation();
            if (inferred != null && context.contains(inferred)) result = result.recognize(inferred, heard);
        }
        return result;
    }

    /** Passive comprehension adds a synonym without erasing the faction's preferred pronunciation. */
    private Lexicon recognize(Concept concept, String word) {
        List<Entry> others = new ArrayList<>(), candidates = new ArrayList<>();
        double strength = 0;
        for (Entry entry : entries) {
            if (entry.concept() != concept) others.add(entry);
            else if (entry.word().equals(word)) strength = entry.strength();
            else candidates.add(entry);
        }
        candidates.add(new Entry(concept, word, Math.max(strength, Math.min(COPIED_STRENGTH, strength + .15))));
        candidates.sort(Comparator.comparingDouble(Entry::strength).reversed());
        others.addAll(candidates.subList(0, Math.min(MAX_WORDS_PER_CONCEPT, candidates.size())));
        return new Lexicon(others, evidence, developing);
    }

    /** Successful understood communication strengthens the listener's own decoded meanings. */
    public Lexicon confirm(List<String> sounds) {
        Lexicon result = this;
        for (String sound : new java.util.LinkedHashSet<>(sounds)) {
            Concept understood = meaning(sound);
            if (understood != null) result = result.reinforce(understood, sound, 0.1);
        }
        return result;
    }

    /**
     * Strengthen {@code word} for {@code concept} by {@code amount}, adding it if new, and weaken rival words for the
     * same concept by half as much. Words that fall below {@link #MIN_STRENGTH}, or beyond the per-concept cap, are
     * forgotten (weakest first).
     */
    public Lexicon reinforce(Concept concept, String word, double amount) {
        Objects.requireNonNull(concept);
        if (!Words.valid(word)) throw new IllegalArgumentException("Invalid word " + word);
        if (!(amount > 0) || !Double.isFinite(amount)) throw new IllegalArgumentException("Amount must be positive");
        List<Entry> others = new ArrayList<>(), rivals = new ArrayList<>();
        double current = 0;
        for (Entry entry : entries) {
            if (entry.concept() != concept) others.add(entry);
            else if (entry.word().equals(word)) current = entry.strength();
            else rivals.add(entry);
        }
        List<Entry> forConcept = new ArrayList<>();
        forConcept.add(new Entry(concept, word, Math.min(1, Math.max(MIN_STRENGTH, current + amount))));
        for (Entry rival : rivals) {
            double weakened = rival.strength() - amount / 2;
            if (weakened >= MIN_STRENGTH) forConcept.add(new Entry(concept, rival.word(), weakened));
        }
        forConcept.sort(Comparator.comparingDouble(Entry::strength).reversed());
        others.addAll(forConcept.subList(0, Math.min(MAX_WORDS_PER_CONCEPT, forConcept.size())));
        return new Lexicon(others, evidence, developing);
    }

    /** Hear another villager say {@code word} for {@code concept}; sometimes it is misheard. */
    public Lexicon hear(Concept concept, String word, double amount, RandomGenerator random) {
        return reinforce(concept, maybeMishear(word, random), amount);
    }

    /** Fraction of concepts for which both villagers say the same word. 1 means fully mutually intelligible. */
    public static double intelligibility(Lexicon a, Lexicon b) {
        int shared = 0;
        for (Concept concept : Concept.values()) {
            String word = a.wordFor(concept);
            if (word != null && word.equals(b.wordFor(concept))) shared++;
        }
        return shared / (double) Concept.values().length;
    }

    public static double comprehension(Lexicon speaker, Lexicon listener) {
        int spoken = 0, understood = 0;
        for (Concept concept : Concept.values()) {
            String word = speaker.wordFor(concept);
            if (word == null) continue;
            spoken++;
            if (listener.meaning(word) == concept) understood++;
        }
        return spoken == 0 ? 0 : understood / (double) spoken;
    }

    private static String maybeMishear(String word, RandomGenerator random) {
        return random.nextDouble() < MISHEAR_RATE ? Words.mishear(word, random) : word;
    }
}
