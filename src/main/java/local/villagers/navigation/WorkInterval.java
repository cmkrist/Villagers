package local.villagers.navigation;

import java.util.Objects;

/** One interruptible physical work interval, keyed by the authoritative job revision and target. */
public final class WorkInterval {
    public enum Phase { TRAVEL, WORK, COMMIT, DONE }
    private Object revision;
    private long started, lastTick, duration;
    private boolean working, issued;

    /** COMMIT is emitted once. The caller must still validate live authority, reach and materials. */
    public Phase advance(Object current, long now, boolean inReach, long ticks) {
        Objects.requireNonNull(current);
        if (now < 0 || ticks < 1 || ticks > Long.MAX_VALUE - now)
            throw new IllegalArgumentException("Invalid work interval");
        if (!current.equals(revision) || now < lastTick || now - lastTick > 10 || ticks != duration) interrupt();
        revision = current; lastTick = now; duration = ticks;
        if (!inReach) { working = false; issued = false; return Phase.TRAVEL; }
        if (issued) return Phase.DONE;
        if (!working) { started = now; working = true; }
        if (now - started < ticks) return Phase.WORK;
        issued = true;
        return Phase.COMMIT;
    }

    /** No partial labor credit survives shelter, hunger, trading, displacement or entity reload. */
    public void interrupt() { revision = null; working = false; issued = false; }
}
