package local.villagers.navigation;

/** Queue waiting and actual travel have separate bounds. Replanning cannot reset physical progress. */
public final class WorkJourney {
    public static final long TIMEOUT = 400, BACKOFF = 400;
    /** Enough for a full bounded128actor queue at staggered10tick thinking, without waiting forever. */
    public static final long QUEUE_TIMEOUT = 2560;
    public enum Step { WAIT, START, TRAVEL, ARRIVED, FAILED }
    private final RouteProgress progress = new RouteProgress(80);
    private String goal;
    private long started, nextAllowed, lastTick;
    private boolean navigating;
    public boolean active() { return goal != null; }
    public boolean awaitingPath() { return goal != null && !navigating; }

    /** The first actually accepted moveTo starts travel clocks. Stale callbacks cannot start a new trip. */
    public boolean navigating(String target,long now,double x,double y,double z) {
        if (goal==null || !goal.equals(target) || now!=lastTick) return false;
        if (!navigating) {
            navigating=true;started=now;progress.reset();
            progress.stalled(goal,now,x,y,z);
        }
        return true;
    }

    public Step advance(String target, long now, boolean arrived, double x, double y, double z) {
        if (now < 0) throw new IllegalArgumentException("Invalid work clock");
        if (now < lastTick) { goal = null; nextAllowed = now; progress.reset(); }
        lastTick = now;
        if (target == null) { interrupt(now); return Step.WAIT; }
        if (goal != null && !goal.equals(target)) { goal = null; progress.reset(); }
        if (goal == null) {
            if (now < nextAllowed) return Step.WAIT;
            if (arrived) { nextAllowed = now + BACKOFF; return Step.ARRIVED; }
            goal = target; started = now; navigating=false; progress.reset();
            return Step.START;
        }
        if (arrived) { reject(now); return Step.ARRIVED; }
        if (!navigating) {
            if (now-started<QUEUE_TIMEOUT) return Step.TRAVEL;
            reject(now);return Step.FAILED;
        }
        if (now - started >= TIMEOUT || progress.stalled(goal, now, x, y, z)) {
            reject(now); return Step.FAILED;
        }
        return Step.TRAVEL;
    }

    public void reject(long now) { goal = null; navigating=false; nextAllowed = now + BACKOFF; progress.reset(); }
    public void interrupt(long now) { goal = null; navigating=false; nextAllowed = Math.max(nextAllowed, now + 40); progress.reset(); }
}
