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

    public static Grammar.Order order(String biome) {
        validate(biome);
        if (biome.contains("desert") || biome.contains("badlands")) return Grammar.Order.SOV;
        if (biome.contains("snow") || biome.contains("frozen") || biome.contains("peak")) return Grammar.Order.OVS;
        if (biome.contains("forest") || biome.contains("jungle") || biome.contains("taiga")) return Grammar.Order.VSO;
        return Grammar.Order.SVO;
    }

    public static WordFormation formation(String biome){return switch(order(biome)){
        case SOV -> WordFormation.founding(WordFormation.Order.HEAD_MATERIAL,"z");
        case OVS -> WordFormation.founding(WordFormation.Order.HEAD_MATERIAL,"k");
        case VSO -> WordFormation.founding(WordFormation.Order.MATERIAL_HEAD,"r");
        default -> WordFormation.founding(WordFormation.Order.MATERIAL_HEAD,"n");
    };}
    public static Lexicon foundingLanguage(String biome) {
        validate(biome);
        // Stable across JVMs and sessions; unlike UUID seeding, residents share their biome's founding words.
        long seed = 0xcbf29ce484222325L;
        for (byte b : biome.getBytes(StandardCharsets.UTF_8)) seed = (seed ^ (b & 255)) * 0x100000001b3L;
        return Lexicon.invent(new Random(seed)).withGrammar(Grammar.founding(order(biome))).withFormation(formation(biome));
    }
}
