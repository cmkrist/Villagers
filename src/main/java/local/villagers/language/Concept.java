package local.villagers.language;

import java.util.Locale;
import java.util.Map;

/** The fixed set of things villagers have words for. A word means one concept in a given villager's lexicon. */
public enum Concept {
    HELLO, YES, NO, THANKS, WANT, TRADE,
    FOOD, BED, FRIEND, WALK, DANGER, MONSTER, HURT,
    HUNGRY, TIRED, LONELY, HAPPY, AFRAID,
    EMERALD, BREAD, WHEAT, CARROT, POTATO, BEETROOT, SEEDS, HOE, SWORD, WOOD,
    I, YOU, IS, SEE, SAY, GIVE, NOT, PAST,
    HAVE, LIKE, ASK, PROMISE, MEET, AGAIN, PERSON, PLACE, NAMED, HERE, SOURCE, RECENT, OLD, FROM, KNOW, PARTNER, CHILD, MAKE, PAY, WORK, ONE, TWO, SOON, STONE, WOODEN_HOE, STONE_HOE, WOODEN_SWORD, STONE_SWORD;

    private static final Map<String, Concept> ITEMS = Map.ofEntries(
            Map.entry("minecraft:emerald", EMERALD), Map.entry("minecraft:bread", BREAD),
            Map.entry("minecraft:wheat", WHEAT), Map.entry("minecraft:carrot", CARROT),
            Map.entry("minecraft:potato", POTATO), Map.entry("minecraft:beetroot", BEETROOT),
            Map.entry("minecraft:wheat_seeds", SEEDS), Map.entry("minecraft:beetroot_seeds", SEEDS));

    /** The concept for an item id such as "minecraft:bread", or null if villagers have no word for it. */
    public static Concept forItem(String itemId) {
        if (itemId == null) return null;
        Concept direct = ITEMS.get(itemId);
        if (direct != null) return direct;
        if(itemId.equals("minecraft:stone")||itemId.equals("minecraft:cobblestone"))return STONE;
        if (itemId.endsWith("_hoe")) return HOE;
        if (itemId.endsWith("_sword")) return SWORD;
        if (itemId.endsWith("_log") || itemId.endsWith("_wood") || itemId.endsWith("_stem") || itemId.endsWith("_hyphae")) return WOOD;
        return null;
    }

    /** Public appearance of supported tools; this refines rather than replaces legacy generic item categories. */
    public static Concept specificItem(String itemId){return switch(itemId){
        case "minecraft:wooden_hoe" -> WOODEN_HOE;case "minecraft:stone_hoe" -> STONE_HOE;
        case "minecraft:wooden_sword" -> WOODEN_SWORD;case "minecraft:stone_sword" -> STONE_SWORD;default -> null;
    };}

    /** The English gloss shown once a player has learned a word, e.g. "bread". */
    public String gloss() { return name().toLowerCase(Locale.ROOT).replace('_',' '); }

    /** Parses a gloss typed by a player, case-insensitively; null if unknown. */
    public static Concept parse(String text) {
        try {
            return valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }
}
