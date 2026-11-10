package local.villagers.mind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A memorable event in a villager's life, kept apart from routine action outcomes so it is not pushed out
 * within minutes. {@code knownBy} lists the players who share the memory: they witnessed it, or (later) were
 * told it. In survival, anyone else sees it as unknown.
 */
public record Episode(long id, Kind kind, long tick, UUID subject, Place place, String detail, List<UUID> knownBy) {
    public static final int MAX_KNOWERS = 8;
    public static final int MAX_DETAIL = 128;

    public enum Kind {
        /** Took a bed as home. {@code place} is the bed. */
        CLAIMED_BED,
        /** A player handed over an item. {@code subject} is the player, {@code detail} the item id. */
        RECEIVED_ITEM,
        /** A player hurt this villager. */
        HURT_BY_PLAYER,
        /** A player killed a monster that was attacking this villager. */
        PROTECTED_BY_PLAYER,
        /**
         * Sent by the gods after this village's last villager starved. {@code subject} is the ancestor whose
         * bloodline the child continues, {@code detail} the ancestor's name.
         */
        GOD_CHILD,
        /** Ordinary birth: subject is one parent, detail names both. */
        BORN,
        /** A real child was born to this parent; subject is the child. */
        HAD_CHILD,
        /** Actually saw a crop growing on farmland; not a guarantee of ripe food or permission to harvest. */
        OBSERVED_FARM
    }

    public Episode {
        if (id < 0) throw new IllegalArgumentException("Episode id cannot be negative");
        Objects.requireNonNull(kind);
        if (tick < 0) throw new IllegalArgumentException("Episode tick cannot be negative");
        if (detail != null && detail.length() > MAX_DETAIL) throw new IllegalArgumentException("Episode detail too long");
        if (kind == Kind.OBSERVED_FARM && (place == null || Math.abs((long)place.x()) > 30_000_000
                || Math.abs((long)place.z()) > 30_000_000 || place.y() < -2032 || place.y() > 2032
                || detail == null || !List.of("minecraft:wheat", "minecraft:carrots", "minecraft:potatoes", "minecraft:beetroots").contains(detail)))
            throw new IllegalArgumentException("Invalid observed farm");
        knownBy = List.copyOf(Objects.requireNonNull(knownBy));
        if (knownBy.size() > MAX_KNOWERS || new LinkedHashSet<>(knownBy).size() != knownBy.size())
            throw new IllegalArgumentException("Invalid episode witnesses");
    }

    public boolean isKnownBy(UUID player) { return knownBy.contains(player); }

    /** This episode, also known by {@code player}; when full, the most recently added knower makes room. */
    public Episode withKnower(UUID player) {
        if (isKnownBy(player)) return this;
        List<UUID> next = new ArrayList<>(knownBy);
        if (next.size() == MAX_KNOWERS) next.removeLast();
        next.add(player);
        return new Episode(id, kind, tick, subject, place, detail, next);
    }

    /** Distinct witnesses in the order given (nearest first, by convention), capped at {@link #MAX_KNOWERS}. */
    static List<UUID> boundedWitnesses(Collection<UUID> witnesses) {
        List<UUID> result = new ArrayList<>();
        for (UUID witness : new LinkedHashSet<>(witnesses)) {
            if (witness == null) continue;
            if (result.size() == MAX_KNOWERS) break;
            result.add(witness);
        }
        return List.copyOf(result);
    }
}
