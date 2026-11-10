package local.villagers.training;

import java.util.Objects;

/** Immutable restart state; elapsed exposure is never refunded by a world-clock rollback. */
public record TrainingCheckpoint(int schemaVersion, String runId, String role, String learningMode,
                                 long startGameTick, long savedGameTick, long elapsedTicks,
                                 String stage, boolean promotionDisabled) {
    public static final int SCHEMA = 1;

    public TrainingCheckpoint {
        if (schemaVersion != SCHEMA) throw new IllegalArgumentException("Unsupported training checkpoint schema");
        if (!Objects.requireNonNull(runId).matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}") || Objects.requireNonNull(stage).isBlank()
                || startGameTick < 0 || savedGameTick < 0 || elapsedTicks < 0
                || elapsedTicks > TrainingConfig.GAME_TICK_BUDGET)
            throw new IllegalArgumentException("Invalid training checkpoint");
        if (!("adaptive".equals(role) || "frozen".equals(role) || "play".equals(role))
                || !("adaptive".equals(learningMode) || "frozen".equals(learningMode)))
            throw new IllegalArgumentException("Invalid training checkpoint role or learning mode");
    }

    public boolean resumable() { return stage.equals("running") || stage.equals("suspended_shutdown"); }

    public long elapsedAt(long gameTick) {
        if (!resumable()) return elapsedTicks;
        long delta = Math.max(0, gameTick - savedGameTick);
        return elapsedTicks + Math.min(delta, TrainingConfig.GAME_TICK_BUDGET - elapsedTicks);
    }

    public void requireCompatible(TrainingConfig config) {
        if (!role.equals(config.roleName()) || !learningMode.equals(config.learningMode()))
            throw new IllegalArgumentException("Training checkpoint role/learner differs from this runtime");
    }
}
