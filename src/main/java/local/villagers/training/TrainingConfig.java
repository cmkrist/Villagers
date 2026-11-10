package local.villagers.training;

import java.util.Locale;
import java.util.Properties;

/** Configuration only; the independent learning property is enforced by the main learner adapter. */
public record TrainingConfig(int requestedTps, Role role, String learningMode) {
    public enum Role { ADAPTIVE, FROZEN, PLAY }
    public static final long GAME_TICK_BUDGET = 48_000;

    public TrainingConfig {
        if (requestedTps != 20 && requestedTps != 40 && requestedTps != 80)
            throw new IllegalArgumentException("villagers.training.tps must be 20, 40 or 80");
        if (role == null || (!"adaptive".equals(learningMode) && !"frozen".equals(learningMode)))
            throw new IllegalArgumentException("villagers.learning must be adaptive or frozen");
        if ((role == Role.ADAPTIVE && !learningMode.equals("adaptive"))
                || (role == Role.FROZEN && !learningMode.equals("frozen")))
            throw new IllegalArgumentException("Trial role and independent villagers.learning mode must match");
    }

    public static TrainingConfig fromProperties(Properties properties) {
        int requested;
        try { requested = Integer.parseInt(properties.getProperty("villagers.training.tps", "20")); }
        catch (NumberFormatException failure) { throw new IllegalArgumentException("villagers.training.tps must be 20, 40 or 80", failure); }
        Role role;
        try { role = Role.valueOf(properties.getProperty("villagers.role", "play").toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException failure) { throw new IllegalArgumentException("villagers.role must be adaptive, frozen or play", failure); }
        return new TrainingConfig(requested, role, properties.getProperty("villagers.learning", "adaptive"));
    }

    public int initialTps() { return role == Role.PLAY ? 20 : Math.min(requestedTps, 40); }
    public String roleName() { return role.name().toLowerCase(Locale.ROOT); }
}
