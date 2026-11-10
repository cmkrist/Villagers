package local.villagers.relations;

import java.util.List;
import java.util.random.RandomGenerator;
import local.villagers.genetics.Trait;

/** When villagers gossip, and what they pass on. */
public final class Gossip {
    private Gossip() {}

    /** Chance that a finished chat includes gossip, by the speaker's traits. */
    public static double chance(java.util.Set<Trait> traits) {
        if (traits.contains(Trait.GOSSIP)) return 0.8;
        if (traits.contains(Trait.LONER)) return 0.1;
        return 0.35;
    }

    /** Opinions worth passing on: strongly felt ones. */
    public static boolean worthTelling(Opinion opinion) {
        return opinion.witnessed() + opinion.hearsay() > 0 && Math.abs(opinion.regard()) >= 0.15;
    }

    /** Pick one opinion to tell, preferring the most strongly felt; null if none is worth telling. */
    public static Opinion pick(List<Opinion> opinions, java.util.UUID listener, RandomGenerator random) {
        List<Opinion> candidates = opinions.stream()
                .filter(o -> !o.other().equals(listener) && worthTelling(o)).toList();
        if (candidates.isEmpty()) return null;
        // Weighted by strength of feeling, so grudges and friendships travel furthest.
        double total = candidates.stream().mapToDouble(o -> Math.abs(o.regard())).sum();
        double roll = random.nextDouble() * total;
        for (Opinion candidate : candidates) {
            roll -= Math.abs(candidate.regard());
            if (roll <= 0) return candidate;
        }
        return candidates.getLast();
    }
}
