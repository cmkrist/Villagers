package local.villagers.lab;

/** Opt-in loaded-world tick budget. Fires one stop request at the measured boundary. */
public final class ObservationBudget {
    private final int limit;
    private long started = -1;
    private boolean fired;
    public ObservationBudget(int ticks) {
        if (ticks != 0 && (ticks < 100 || ticks > 12_000)) throw new IllegalArgumentException("Observation budget must be 0 or100..12000 ticks");
        limit = ticks;
    }
    public int limit() { return limit; }
    public long started() { return started; }
    public void reset() { started = -1; fired = false; }
    public boolean expired(long tick) {
        if (tick < 0) throw new IllegalArgumentException("Invalid observation clock");
        if (limit == 0 || fired) return false;
        if (started < 0 || tick < started) started = tick;
        if (tick - started < limit) return false;
        fired = true; return true;
    }
}
