package local.villagers.compat;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Exact-version compatibility for Animatica's otherwise unclosed static animation executor. */
public final class AnimaticaShutdown {
    private static final Logger LOG = LoggerFactory.getLogger("villagers-animatica-shutdown");
    private static final String VERSION = "0.6.2+26.3";

    private AnimaticaShutdown() {}

    public static void initialize() {
        boolean supported = FabricLoader.getInstance().getModContainer("animatica")
                .map(mod -> VERSION.equals(mod.getMetadata().getVersion().getFriendlyString())).orElse(false);
        if (supported) ClientLifecycleEvents.CLIENT_STOPPING.register(client -> stop());
    }

    private static void stop() {
        try {
            Class<?> type = Class.forName("io.github.foundationgames.animatica.animation.AnimatedTexture", false,
                    AnimaticaShutdown.class.getClassLoader());
            Object value = type.getField("EXECUTORS").get(null);
            if (!(value instanceof ThreadPoolExecutor pool) || pool.getCorePoolSize() != 4
                    || pool.getMaximumPoolSize() != 4) {
                LOG.warn("Animatica {} executor layout changed; leaving it untouched", VERSION);
                return;
            }
            int workers = pool.getPoolSize(), active = pool.getActiveCount(), queued = pool.getQueue().size();
            // Fabric fires CLIENT_STOPPING in exitWorldAndClose, after the render loop ends.
            // Drain only this exact field's work before texture resources are closed. No thread
            // enumeration, shutdownNow, VM exit, daemon mutation, or interrupted pixel work.
            if (drain(pool, Duration.ofSeconds(2)))
                LOG.info("Animatica animation executor drained and terminated: workers={}, active={}, queued={}",
                        workers, active, queued);
            else LOG.error("Animatica animation executor did not terminate within 2 seconds; normal client shutdown may still fail");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            LOG.warn("Could not close the pinned Animatica executor; no other executor was changed", failure);
        }
    }

    static boolean drain(ExecutorService executor, Duration timeout) {
        executor.shutdown();
        try { return executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return false; }
    }
}
