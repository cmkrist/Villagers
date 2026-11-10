package local.villagers.capability;

/** Testable defensive eligibility shared by the real-world adapter. No pursuit or unarmed damage. */
public final class DefensePolicy {
    public enum TargetKind { MONSTER, PLAYER, GOLEM, NEUTRAL_ANIMAL, OTHER }
    private DefensePolicy() {}

    public static boolean mayStrike(boolean realSword, TargetKind kind, boolean targetingActor,
                                    boolean recentlyHurtActor, boolean visible, boolean withinReach) {
        return realSword && kind == TargetKind.MONSTER && (targetingActor || recentlyHurtActor)
            && visible && withinReach;
    }

    /** Full weapon recovery, based on the actual equipped attack-speed attribute. */
    public static int cooldownTicks(double attacksPerSecond) {
        if (!Double.isFinite(attacksPerSecond) || attacksPerSecond <= 0) return Integer.MAX_VALUE;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1, Math.ceil(20.0 / attacksPerSecond)));
    }
}
