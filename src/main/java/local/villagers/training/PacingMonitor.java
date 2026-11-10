package local.villagers.training;

/** Requires sustained measured evidence; never bases pacing on entity action labels. */
public final class PacingMonitor {
    public enum Decision { HOLD, PROMOTE_TO_80, BACK_OFF_TO_20 }
    private int badWindows, healthyWindows;

    public Decision observe(int requestedTps, double currentTps, double achievedTps,
                            double meanMspt, double p95Mspt, double windowSeconds) {
        if (windowSeconds < 5 || currentTps <= 20) return Decision.HOLD;
        double budget = 1000 / currentTps;
        boolean bad = !Double.isFinite(achievedTps) || !Double.isFinite(meanMspt) || !Double.isFinite(p95Mspt)
            || achievedTps < currentTps * .9 || meanMspt > budget * .85 || p95Mspt > budget * 1.25;
        badWindows = bad ? badWindows + 1 : 0;
        double promotedBudget = 1000.0 / 80;
        boolean readyFor80 = !bad && requestedTps == 80 && currentTps == 40
            && achievedTps >= 38 && meanMspt <= promotedBudget * .65 && p95Mspt <= promotedBudget * .9;
        healthyWindows = readyFor80 ? healthyWindows + 1 : 0;
        if (badWindows >= 2) { reset(); return Decision.BACK_OFF_TO_20; }
        if (healthyWindows >= 2) { reset(); return Decision.PROMOTE_TO_80; }
        return Decision.HOLD;
    }
    public void reset() { badWindows = 0; healthyWindows = 0; }
}
