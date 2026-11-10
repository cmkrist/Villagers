package local.villagers.survival;

/** Authored safety priorities; these decisions are deliberately separate from learned preferences. */
public final class SurvivalPolicy {
    private SurvivalPolicy() {}
    public enum Intent { NORMAL, SHELTER, SEEK_GOLEM, ESCAPE }
    public record Situation(boolean danger, boolean dusk, boolean coveredHome,
                            boolean reachableHome, boolean safeHome, double homeDistance,
                            boolean reachableGuardian) {
        public Situation {
            if (!Double.isFinite(homeDistance) || homeDistance < 0) throw new IllegalArgumentException("Invalid home distance");
        }
    }
    public static Intent choose(Situation s) {
        if (s.danger()) {
            if (s.coveredHome() && s.reachableHome() && s.safeHome()) return Intent.SHELTER;
            if (s.reachableGuardian()) return Intent.SEEK_GOLEM;
            return Intent.ESCAPE;
        }
        if ((s.dusk() || s.homeDistance() > 28) && s.coveredHome() && s.reachableHome()) return Intent.SHELTER;
        if (s.dusk() && s.reachableGuardian()) return Intent.SEEK_GOLEM;
        return Intent.NORMAL;
    }
    public static boolean withinHomeRange(double distance) { return Double.isFinite(distance) && distance >= 0 && distance <= 28; }
    public static boolean withinVillageWorkArea(double homeToBell,double pointToBell) {
        return Double.isFinite(homeToBell)&&Double.isFinite(pointToBell)&&homeToBell>=0&&homeToBell<=64
            &&pointToBell>=0&&pointToBell<=48;
    }
    /** A partial native path can bring an already-displaced resident home, never grant an excursion from home. */
    public static boolean usefulPartialReturn(double currentDistance, double endDistance, int nodes) {
        return Double.isFinite(currentDistance) && Double.isFinite(endDistance) && currentDistance > 28
                && endDistance >= 0 && currentDistance - endDistance >= 1 && nodes >= 2 && nodes <= 128;
    }
    public static boolean withinReturnBound(double currentDistance, double nodeDistance) {
        return Double.isFinite(currentDistance) && Double.isFinite(nodeDistance) && currentDistance >= 0
                && nodeDistance >= 0 && nodeDistance <= Math.max(28, currentDistance) + 8;
    }
    /** Broad-phase candidate only: the actor/path must still stay home and satisfy actual eye reach/sight. */
    public static boolean withinHomeInteractionRange(double distance, double reach) {
        return Double.isFinite(distance) && Double.isFinite(reach) && distance >= 0 && reach >= 0
                && reach <= 3 && distance <= 28 + reach;
    }
}
