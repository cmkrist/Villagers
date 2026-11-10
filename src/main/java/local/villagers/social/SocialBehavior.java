package local.villagers.social;

import local.villagers.mind.Observation;

/** Small authored attention budget; this does not assign beliefs or unconditional trust. */
public final class SocialBehavior {
    public static final int EPISODE_TICKS = 30;
    public static final int EPISODE_INTERVAL_TICKS = 200;
    public static final double PERSONAL_SPACE = 3;
    public static final double MAX_STEP = 2;
    public static final double HOME_RADIUS = 28;
    private SocialBehavior() {}

    public static boolean mayEngage(Observation observed, boolean trading, boolean sleeping) {
        return !trading && !sleeping && !observed.danger() && !observed.night()
            && observed.hunger() < .65 && observed.fatigue() < .65 && observed.health() > .4;
    }
    public static double approachStep(double separation) {
        return Double.isFinite(separation) ? Math.max(0, Math.min(MAX_STEP, separation - PERSONAL_SPACE)) : 0;
    }
    public static boolean withinHome(double distance) {
        return Double.isFinite(distance) && distance >= 0 && distance <= HOME_RADIUS;
    }
    public static boolean episodeDue(long now, long nextAllowed) { return now >= nextAllowed; }
}
