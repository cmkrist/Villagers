package local.villagers.evidence;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EvidenceWriterTest {
    @Test void slowDiskCannotGrowAnUnboundedQueueOrRunRejectedWorkOnCaller() throws Exception {
        var reports = new AtomicInteger();
        var wrote = new AtomicInteger();
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        EvidenceWriter writer = new EvidenceWriter("test-evidence", 2, error -> reports.incrementAndGet());
        try {
            assertTrue(writer.execute(() -> {
                started.countDown();
                try { release.await(); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            }));
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertTrue(writer.execute(wrote::incrementAndGet));
            assertTrue(writer.execute(wrote::incrementAndGet));
            assertEquals(2, writer.queuedTasks());
            assertFalse(writer.execute(wrote::incrementAndGet));
            assertFalse(writer.execute(wrote::incrementAndGet));
            assertTrue(writer.failed());
            assertEquals(1, reports.get());
            assertEquals(0, wrote.get(), "rejection must not perform IO on the server thread");
        } finally {
            release.countDown();
            writer.shutdown();
            assertTrue(writer.awaitTermination(5, TimeUnit.SECONDS));
        }
        assertEquals(2, wrote.get(), "already accepted evidence drains in order");
    }

    @Test void diskFailureIsStickyAndReportedOnce() throws Exception {
        var reported = new CountDownLatch(1);
        var reports = new AtomicInteger();
        EvidenceWriter writer = new EvidenceWriter("test-evidence", 2, error -> {
            reports.incrementAndGet(); reported.countDown();
        });
        try {
            writer.execute(() -> writer.fail(new IOException("disk full")));
            assertTrue(reported.await(5, TimeUnit.SECONDS));
            assertFalse(writer.execute(() -> fail("must not accept more evidence after failure")));
            writer.fail(new IOException("still full"));
            assertEquals(1, reports.get());
        } finally {
            writer.shutdown();
            assertTrue(writer.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test void barrierOrdersAdmissionAfterPriorSnapshotsAndPropagatesFailures() throws Exception {
        EvidenceWriter writer = new EvidenceWriter("test-evidence", 4, error -> {});
        var state = new AtomicInteger();
        try {
            writer.execute(() -> state.set(1));
            writer.submit(() -> assertEquals(1, state.get())).get(5, TimeUnit.SECONDS);
            assertThrows(java.util.concurrent.ExecutionException.class,
                    () -> writer.submit(() -> { throw new IllegalStateException("write failed"); }).get(5, TimeUnit.SECONDS));
            assertTrue(writer.failed());
        } finally {
            writer.shutdown();
            assertTrue(writer.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
