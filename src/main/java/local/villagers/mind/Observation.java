package local.villagers.mind;

/** Normalized, controller-observed needs and affordances; health is a fraction of maximum. */
public record Observation(double hunger, double fatigue, double loneliness, double health,
                          boolean danger, boolean night, boolean foodAvailable, boolean homeAvailable) {
    public Observation {
        hunger = normalized(hunger);
        fatigue = normalized(fatigue);
        loneliness = normalized(loneliness);
        health = normalized(health);
    }

    static double normalized(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Observation must be finite");
        return Math.clamp(value, 0.0, 1.0);
    }
}
