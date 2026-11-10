package local.villagers.persistence;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FailureThrottleTest {
    private final AtomicLong clock = new AtomicLong();
    private final FailureThrottle<String> throttle = new FailureThrottle<>(Duration.ofSeconds(60), clock::get);

    @Test void reportsFirstFailureThenSuppressesUntilIntervalPasses() {
        assertTrue(throttle.shouldReport("a"));
        clock.addAndGet(Duration.ofSeconds(59).toNanos());
        assertFalse(throttle.shouldReport("a"));
        clock.addAndGet(Duration.ofSeconds(1).toNanos());
        assertTrue(throttle.shouldReport("a"));
    }

    @Test void keysAreIndependentAndRecoveryResetsSuppression() {
        assertTrue(throttle.shouldReport("a"));
        assertTrue(throttle.shouldReport("b"));
        assertFalse(throttle.shouldReport("a"));
        throttle.recovered("a");
        assertTrue(throttle.shouldReport("a"));
    }

    @Test void rejectsNonPositiveInterval() {
        assertThrows(IllegalArgumentException.class, () -> new FailureThrottle<String>(Duration.ZERO));
    }
}
