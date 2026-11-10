package local.villagers.persistence;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Limits repeated failure reports for the same key, so a persistent fault cannot flood the log. */
public final class FailureThrottle<K> {
    private final long intervalNanos;
    private final LongSupplier clock;
    private final Map<K, Long> lastReported = new ConcurrentHashMap<>();

    public FailureThrottle(Duration interval) { this(interval, System::nanoTime); }

    public FailureThrottle(Duration interval, LongSupplier nanoClock) {
        if (interval.isNegative() || interval.isZero()) throw new IllegalArgumentException("Interval must be positive");
        this.intervalNanos = interval.toNanos();
        this.clock = Objects.requireNonNull(nanoClock);
    }

    /** Record a failure; true when it should be reported (first failure, or the interval has passed). */
    public boolean shouldReport(K key) {
        long now = clock.getAsLong();
        Long previous = lastReported.get(key);
        if (previous != null && now - previous < intervalNanos) return false;
        lastReported.put(key, now);
        return true;
    }

    /** Clear the key after success, so a later, new failure is reported immediately. */
    public void recovered(K key) { lastReported.remove(key); }

    public void clear() { lastReported.clear(); }
}
