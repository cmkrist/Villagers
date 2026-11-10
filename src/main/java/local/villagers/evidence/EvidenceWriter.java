package local.villagers.evidence;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Bounded serial evidence IO. Failure is sticky: owners must stop the affected experiment. */
public final class EvidenceWriter {
    private final ThreadPoolExecutor executor;
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    private final Consumer<Throwable> report;

    public EvidenceWriter(String name, int capacity, Consumer<Throwable> report) {
        this.report = report;
        executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(capacity), task -> {
            Thread thread = new Thread(task, name);
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy());
    }

    /** False means no task was accepted; callers holding native resources must dispose of them. */
    public boolean execute(Runnable task) {
        if (failed()) return false;
        try {
            executor.execute(() -> {
                try { task.run(); }
                catch (RuntimeException error) { fail(error); }
            });
            return true;
        } catch (RejectedExecutionException rejected) {
            fail(rejected);
            return false;
        }
    }

    /** Ordered commit barrier for cohort admission. Never run a rejected barrier on the game thread. */
    public Future<?> submit(Runnable task) {
        if (failed()) throw new RejectedExecutionException("Evidence writer has failed", failure.get());
        try {
            return executor.submit(() -> {
                try { task.run(); }
                catch (RuntimeException error) { fail(error); throw error; }
            });
        }
        catch (RejectedExecutionException rejected) { fail(rejected); throw rejected; }
    }

    public void fail(Throwable error) { if (failure.compareAndSet(null, error)) report.accept(error); }
    public boolean failed() { return failure.get() != null; }
    public int queuedTasks() { return executor.getQueue().size(); }
    public void shutdown() { executor.shutdown(); }
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }
}
