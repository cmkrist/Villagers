package local.villagers.learning;

import local.villagers.mind.*;
import java.util.Locale;

/** A fixed checkpoint control still acts and accumulates needs, but never changes its learned policy. */
public enum LearningMode {
    ADAPTIVE, FROZEN;
    public static LearningMode parse(String value) {
        if (value == null) throw new IllegalArgumentException("Missing learning mode");
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "adaptive" -> ADAPTIVE;
            case "frozen" -> FROZEN;
            default -> throw new IllegalArgumentException("Learning mode must be adaptive or frozen");
        };
    }
    public void apply(AgentMind mind, Observation before, Action action, Observation after) {
        if (this == ADAPTIVE) mind.learn(before,action,after);
    }
}
