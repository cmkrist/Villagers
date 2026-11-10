package local.villagers.mind;

/** How a villager's life ended. {@code cause} is the damage message id, e.g. "starve". */
public record Death(long tick, String cause) {
    public static final String STARVATION = "starve";
    public static final int MAX_CAUSE = 64;

    public Death {
        if (tick < 0) throw new IllegalArgumentException("Death tick cannot be negative");
        if (cause == null || cause.isBlank() || cause.length() > MAX_CAUSE) throw new IllegalArgumentException("Invalid death cause");
    }

    public boolean starved() { return STARVATION.equals(cause); }
}
