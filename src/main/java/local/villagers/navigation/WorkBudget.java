package local.villagers.navigation;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UUID;

/** Server-thread fair admission. Waiting work is bounded data, never a task or a native path. */
public final class WorkBudget {
    public static final int MAX_WAITERS = 128;
    public static final long INTERVAL = 10, LEASE = 80;
    private final LinkedHashMap<UUID, Long> waiting = new LinkedHashMap<>();
    private long next, lastTick;

    public boolean admit(UUID actor, long now) {
        Objects.requireNonNull(actor);
        expire(now);
        if (!waiting.containsKey(actor) && waiting.size() >= MAX_WAITERS) return false;
        // Updating a request renews its lease without changing its position.
        waiting.put(actor, now);
        if (now < next || !waiting.firstEntry().getKey().equals(actor)) return false;
        waiting.remove(actor);
        next = now + INTERVAL;
        return true;
    }

    /** One-shot diagnostics may use an empty slot, but cannot reserve it or overtake workers. */
    public boolean inspect(long now) {
        expire(now);
        if (now < next || !waiting.isEmpty()) return false;
        next = now + INTERVAL;
        return true;
    }

    public void forget(UUID actor) { waiting.remove(actor); }
    public int waiting() { return waiting.size(); }

    private void expire(long now) {
        if (now < 0 || now > Long.MAX_VALUE - LEASE) throw new IllegalArgumentException("Invalid work clock");
        if (now < lastTick) { waiting.clear(); next = now; }
        lastTick = now;
        waiting.values().removeIf(tick -> now - tick >= LEASE);
    }
}
