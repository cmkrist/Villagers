package local.villagers.mood;

import java.util.Objects;

/**
 * How lived experience has shifted a villager's inherited tendencies. Each shift is bounded to
 * ±{@link #MAX_SHIFT}, so experience colours a temperament but never replaces it. Immutable.
 */
public record Experience(double boldness, double sociability, double curiosity) {
    public static final double MAX_SHIFT = 0.2;

    /** What shapes a villager over time. */
    public enum Event {
        /** Someone fought off a monster for it: a little bolder. */
        RESCUED(0.02, 0.01, 0),
        /** Hurt by a player: more timid and withdrawn. */
        HURT(-0.03, -0.01, 0),
        /** A good chat: a little more sociable. */
        CHATTED(0, 0.004, 0),
        /** Refused when hungry: a little less trusting of company. */
        REFUSED(0, -0.01, 0),
        /** Explored and came back safely: a little more curious. */
        EXPLORED(0.002, 0, 0.004);

        final double boldness, sociability, curiosity;
        Event(double boldness, double sociability, double curiosity) {
            this.boldness = boldness; this.sociability = sociability; this.curiosity = curiosity;
        }
    }

    public Experience {
        check(boldness); check(sociability); check(curiosity);
    }

    public static Experience none() { return new Experience(0, 0, 0); }

    public Experience after(Event event) {
        Objects.requireNonNull(event);
        return new Experience(bound(boldness + event.boldness), bound(sociability + event.sociability),
                bound(curiosity + event.curiosity));
    }

    private static double bound(double shift) { return Math.clamp(shift, -MAX_SHIFT, MAX_SHIFT); }

    private static void check(double shift) {
        if (!Double.isFinite(shift) || Math.abs(shift) > MAX_SHIFT) throw new IllegalArgumentException("Experience shift out of range");
    }
}
