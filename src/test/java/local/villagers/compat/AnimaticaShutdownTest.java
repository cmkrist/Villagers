package local.villagers.compat;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnimaticaShutdownTest {
    @Test void normalStopDrainsEveryAcceptedTaskAndTerminatesRealNonDaemonWorkers() throws Exception {
        var pool = Executors.newFixedThreadPool(4);
        var completed = new AtomicInteger();
        try {
            for (int i = 0; i < 30; i++) pool.execute(() -> {
                assertFalse(Thread.currentThread().isDaemon());
                completed.incrementAndGet();
            });
            assertTrue(AnimaticaShutdown.drain(pool, Duration.ofSeconds(2)));
            assertEquals(30, completed.get());
            assertTrue(pool.isTerminated());
            assertThrows(RejectedExecutionException.class, () -> pool.execute(() -> {}));
        } finally { pool.shutdown(); assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS)); }
    }

    @Test void timeoutDoesNotInterruptOrDiscardInFlightPixelWork() throws Exception {
        var pool = Executors.newFixedThreadPool(4);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var completed = new AtomicInteger();
        var interruptions = new AtomicInteger();
        try {
            pool.execute(() -> {
                entered.countDown();
                try { release.await(); completed.incrementAndGet(); }
                catch (InterruptedException unwanted) { interruptions.incrementAndGet(); }
            });
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            assertFalse(AnimaticaShutdown.drain(pool, Duration.ZERO));
            assertEquals(0, interruptions.get());
            release.countDown();
            assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS));
            assertEquals(1, completed.get());
            assertEquals(0, interruptions.get());
        } finally { release.countDown(); pool.shutdown(); assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS)); }
    }

    @Test void callerInterruptionIsPreservedWhileShutdownStillBegins() throws Exception {
        var pool = Executors.newFixedThreadPool(4);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try {
            pool.execute(() -> {
                entered.countDown();
                try { release.await(); } catch (InterruptedException unexpected) { throw new AssertionError(unexpected); }
            });
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            Thread.currentThread().interrupt();
            assertFalse(AnimaticaShutdown.drain(pool, Duration.ofSeconds(1)));
            assertTrue(Thread.currentThread().isInterrupted());
            assertTrue(pool.isShutdown());
        } finally {
            Thread.interrupted(); release.countDown(); pool.shutdown();
            assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS));
        }
    }
}
