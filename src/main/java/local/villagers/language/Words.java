package local.villagers.language;

import java.util.random.RandomGenerator;

/** Invented words: one to three consonant-vowel syllables, sometimes closed by a consonant. */
public final class Words {
    static final String CONSONANTS = "bdfgklmnprstvz";
    static final String VOWELS = "aeiou";
    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 12;

    private Words() {}

    public static String invent(RandomGenerator random) {
        int syllables = 1 + random.nextInt(3);
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < syllables; i++) {
            word.append(CONSONANTS.charAt(random.nextInt(CONSONANTS.length())));
            word.append(VOWELS.charAt(random.nextInt(VOWELS.length())));
        }
        if (random.nextDouble() < 0.3) word.append(CONSONANTS.charAt(random.nextInt(CONSONANTS.length())));
        return word.toString();
    }

    /** A plausible mishearing: one letter swapped for another of the same kind (vowel for vowel, etc.). */
    public static String mishear(String word, RandomGenerator random) {
        int index = random.nextInt(word.length());
        char original = word.charAt(index);
        String pool = VOWELS.indexOf(original) >= 0 ? VOWELS : CONSONANTS.indexOf(original) >= 0 ? CONSONANTS : null;
        if (pool == null) return word;
        char replacement;
        do replacement = pool.charAt(random.nextInt(pool.length())); while (replacement == original);
        return word.substring(0, index) + replacement + word.substring(index + 1);
    }

    /** Words players may teach: 2 to 12 lowercase letters a-z. */
    public static boolean valid(String word) {
        if (word == null || word.length() < MIN_LENGTH || word.length() > MAX_LENGTH) return false;
        for (int i = 0; i < word.length(); i++) if (word.charAt(i) < 'a' || word.charAt(i) > 'z') return false;
        return true;
    }
}
