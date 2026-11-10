package local.villagers.relations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * A villager's opinions of up to {@link #MAX_KNOWN} other villagers. When full, the least-engaged acquaintance
 * (fewest witnessed and heard events, then oldest) is forgotten. Looking someone up never adds them. Immutable.
 */
public record Opinions(List<Opinion> known) {
    public static final int MAX_KNOWN = 16;

    public Opinions {
        known = List.copyOf(Objects.requireNonNull(known));
        if (known.size() > MAX_KNOWN) throw new IllegalArgumentException("Too many opinions");
        Set<UUID> ids = new HashSet<>();
        for (Opinion opinion : known) if (!ids.add(opinion.other())) throw new IllegalArgumentException("Duplicate opinion");
    }

    public static Opinions empty() { return new Opinions(List.of()); }

    /** The current opinion, or neutral for a stranger. */
    public Opinion of(UUID other) {
        return known.stream().filter(o -> o.other().equals(other)).findFirst().orElse(Opinion.neutral(other));
    }

    public Opinions witness(UUID other, Opinion.Event event, long tick) { return update(other, o -> o.witness(event, tick)); }

    public Opinions hear(UUID about, double tellerLiking, double tellerTrust, double trustInTeller, long tick) {
        return update(about, o -> o.hear(tellerLiking, tellerTrust, trustInTeller, tick));
    }

    /** The best-regarded known villager among {@code candidates}, if any is liked at all. */
    public Optional<UUID> favourite(Set<UUID> candidates) {
        return known.stream().filter(o -> candidates.contains(o.other()) && o.regard() > 0)
                .max(Comparator.comparingDouble(Opinion::regard)).map(Opinion::other);
    }

    private Opinions update(UUID other, UnaryOperator<Opinion> change) {
        List<Opinion> next = new ArrayList<>();
        Opinion current = Opinion.neutral(other);
        for (Opinion opinion : known) {
            if (opinion.other().equals(other)) current = opinion;
            else next.add(opinion);
        }
        Opinion changed = change.apply(current);
        if (changed.equals(current) && current.witnessed() == 0 && current.hearsay() == 0) return this; // Nothing learned.
        if (next.size() == MAX_KNOWN)
            next.remove(next.stream().min(Comparator.comparingLong((Opinion o) -> o.witnessed() + o.hearsay())
                    .thenComparingLong(Opinion::lastTick)).orElseThrow());
        next.add(changed);
        return new Opinions(next);
    }
}
