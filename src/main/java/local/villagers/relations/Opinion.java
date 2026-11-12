package local.villagers.relations;

import java.util.Objects;
import java.util.UUID;

/**
 * One villager's view of another: liking and trust in [-1, 1], fear in [0, 1]. {@code witnessed} counts events
 * it saw itself; {@code hearsay} counts things it was told, which move the view far less. Immutable.
 */
public record Opinion(UUID other, double liking, double trust, double fear, long witnessed, long hearsay, long lastTick) {
    /** Hearsay moves an opinion by this fraction of a witnessed event, scaled by trust in the teller. */
    public static final double HEARSAY_WEIGHT = 0.3;

    /** Things one villager can see another do. */
    public enum Event {
        /** Spent time talking together. */
        CHATTED(0.04, 0.02, 0),
        /** Gave food when this villager was hungry. */
        SHARED_FOOD(0.25, 0.2, -0.05),
        /** Fought off a monster that was attacking this villager. */
        PROTECTED(0.3, 0.3, -0.1),
        /** Refused food when this villager was hungry and asked. */
        REFUSED(-0.12, -0.06, 0),
        /** An understood promise was followed by actual shared time. */
        KEPT_PROMISE(0.06, 0.12, 0);

        final double liking, trust, fear;
        Event(double liking, double trust, double fear) { this.liking = liking; this.trust = trust; this.fear = fear; }
    }

    public Opinion {
        Objects.requireNonNull(other);
        check(liking, -1, 1); check(trust, -1, 1); check(fear, 0, 1);
        if (witnessed < 0 || hearsay < 0 || lastTick < -1) throw new IllegalArgumentException("Invalid opinion history");
    }

    public static Opinion neutral(UUID other) { return new Opinion(other, 0, 0, 0, 0, 0, -1); }

    public Opinion witness(Event event, long tick) {
        return new Opinion(other, clamp(liking + event.liking, -1, 1), clamp(trust + event.trust, -1, 1),
                clamp(fear + event.fear, 0, 1), saturating(witnessed), hearsay, Math.max(lastTick, tick));
    }

    /**
     * Being told how a teller feels about this villager. The view moves toward the teller's by
     * {@link #HEARSAY_WEIGHT} times the listener's trust in the teller (distrusted tellers change nothing).
     */
    public Opinion hear(double tellerLiking, double tellerTrust, double trustInTeller, long tick) {
        double weight = HEARSAY_WEIGHT * Math.max(0, trustInTeller);
        if (weight == 0) return this;
        return new Opinion(other, clamp(liking + weight * (tellerLiking - liking), -1, 1),
                clamp(trust + weight * (tellerTrust - trust), -1, 1), fear, witnessed, saturating(hearsay), Math.max(lastTick, tick));
    }

    /** Rough overall regard in [-1, 1], used to pick friends and to price favours. */
    public double regard() { return clamp(0.6 * liking + 0.4 * trust - 0.5 * fear, -1, 1); }

    private static long saturating(long count) { return count == Long.MAX_VALUE ? count : count + 1; }
    private static double clamp(double value, double min, double max) { return Math.clamp(value, min, max); }
    private static void check(double value, double min, double max) {
        if (!Double.isFinite(value) || value < min || value > max) throw new IllegalArgumentException("Opinion values out of range");
    }
}
