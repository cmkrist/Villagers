package local.villagers.family;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import local.villagers.mind.Lineage;

/** Ordinary reproduction rules. Food costs are actual vanilla breeding-item units, not hunger setters. */
public final class BreedingPolicy {
    public static final int FOOD_COST = 12;
    public static final int MIN_FOOD = 14;
    public static final long COOLDOWN = 24_000;
    public static final int LOCAL_POPULATION_CAP = 64;
    public enum Food {
        BREAD(4), ROOT(1), OTHER(0);
        final int units;
        Food(int units) { this.units = units; }
        public int units() { return units; }
    }
    public record Stack(Food food, int count, boolean reserveSeed) {
        public Stack { if (food == null || count < 0 || count > 99) throw new IllegalArgumentException("Invalid food stack"); }
    }
    private BreedingPolicy() {}

    /** Counts to remove per slot, or empty when the whole cost cannot be paid while retaining planting stock. */
    public static Optional<List<Integer>> foodCost(List<Stack> slots) {
        List<Integer> cost = new ArrayList<>();
        int needed = FOOD_COST;
        for (Stack stack : slots) {
            int available = Math.max(0, stack.count() - (stack.reserveSeed() ? 1 : 0));
            int take = stack.food().units == 0 ? 0 : Math.min(available, (needed + stack.food().units - 1) / stack.food().units);
            cost.add(take);
            needed = Math.max(0, needed - take * stack.food().units);
        }
        return needed == 0 ? Optional.of(List.copyOf(cost)) : Optional.empty();
    }

    public static boolean willing(double health, int food, double fatigue, boolean lowMood, double regard,
                                  long now, long nextBirth) {
        return Double.isFinite(health) && Double.isFinite(fatigue) && Double.isFinite(regard)
                && health >= .9 && food >= MIN_FOOD && fatigue < .6 && !lowMood && regard >= 0 && now >= nextBirth;
    }

    public static double chance(double fertilityA, double fertilityB, double regardA, double regardB, int spareBeds) {
        if (spareBeds <= 0 || !Double.isFinite(fertilityA) || !Double.isFinite(fertilityB)
                || !Double.isFinite(regardA) || !Double.isFinite(regardB)) return 0;
        return Math.clamp((.05 + .2 * (fertilityA + fertilityB) / 2)
                * (.5 + .5 * Math.max(0, Math.min(regardA, regardB))) * Math.min(1, spareBeds / 2.0), 0, .25);
    }

    public static boolean closeKin(UUID a, Lineage first, UUID b, Lineage second) {
        if (a.equals(b)) return true;
        if (first != null && first.parents().contains(b) || second != null && second.parents().contains(a)) return true;
        return first != null && second != null && first.parents().stream().anyMatch(second.parents()::contains);
    }
}
