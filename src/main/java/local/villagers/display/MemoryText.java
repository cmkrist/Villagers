package local.villagers.display;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import local.villagers.mind.Episode;

/** Plain-text rendering of episodes, and the rule for who may read them. */
public final class MemoryText {
    public static final String UNKNOWN = "???";
    private static final long TICKS_PER_DAY = 24_000;

    private MemoryText() {}

    /**
     * Survival and adventure players see a memory only if they share it. Creative, spectator and console
     * viewers ({@code omniscient}) see everything.
     */
    public static boolean visible(Episode episode, UUID viewer, boolean omniscient) {
        return omniscient || (viewer != null && episode.isKnownBy(viewer));
    }

    /** One line such as "Day 3: You gave me bread", or "???" when hidden from this viewer. */
    public static String line(Episode episode, UUID viewer, boolean omniscient, Function<UUID, String> playerNames) {
        if (!visible(episode, viewer, omniscient)) return UNKNOWN;
        return "Day " + (episode.tick() / TICKS_PER_DAY + 1) + ": " + describe(episode, viewer, playerNames);
    }

    static String describe(Episode episode, UUID viewer, Function<UUID, String> playerNames) {
        String who = episode.subject() == null ? "someone"
                : episode.subject().equals(viewer) ? "you"
                : Objects.requireNonNullElse(playerNames.apply(episode.subject()), "a player");
        return switch (episode.kind()) {
            case CLAIMED_BED -> episode.place() == null ? "I claimed a bed"
                    : "I claimed a bed at " + episode.place().x() + " " + episode.place().y() + " " + episode.place().z();
            case RECEIVED_ITEM -> capitalize(who) + " gave me " + itemName(episode.detail());
            case HURT_BY_PLAYER -> capitalize(who) + " hurt me";
            case PROTECTED_BY_PLAYER -> capitalize(who) + " saved me from a monster";
            case BORN -> "I was born to " + episode.detail();
            case HAD_CHILD -> "My child " + episode.detail() + " was born";
            case OBSERVED_FARM -> "I saw crops growing at " + episode.place().x() + " " + episode.place().y() + " " + episode.place().z();
            case GOD_CHILD -> "The gods sent me to carry on " + (episode.detail() == null ? "my ancestor" : episode.detail())
                    + "'s line after the village starved";
        };
    }

    /** "minecraft:iron_hoe" becomes "iron hoe". */
    static String itemName(String itemId) {
        if (itemId == null || itemId.isBlank()) return "something";
        String path = itemId.substring(itemId.indexOf(':') + 1);
        return path.replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
