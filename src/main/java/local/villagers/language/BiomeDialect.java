package local.villagers.language;

import java.nio.charset.StandardCharsets;
import java.util.Random;

/** A biome is a cultural faction, with a stable founding dialect independent of nearby foreign speakers. */
public final class BiomeDialect {
    private BiomeDialect() {}

    public static String validate(String biome) {
        if (biome == null || biome.length() > 128 || !biome.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
            throw new IllegalArgumentException("Invalid origin biome");
        return biome;
    }

    public static Lexicon foundingLanguage(String biome) {
        validate(biome);
        // Stable across JVMs and sessions; unlike UUID seeding, residents share their biome's founding words.
        long seed = 0xcbf29ce484222325L;
        for (byte b : biome.getBytes(StandardCharsets.UTF_8)) seed = (seed ^ (b & 255)) * 0x100000001b3L;
        return Lexicon.invent(new Random(seed));
    }
}
