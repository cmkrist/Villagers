package local.villagers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.ThreadPoolExecutor;
import local.villagers.evidence.EvidenceWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Explicit play-only title-screen diagnostic. Never loads a save or starts/stops a foreign executor. */
final class ClientShutdownProbe {
    private static final Logger LOG = LoggerFactory.getLogger("villagers-shutdown-probe");
    private static int titleTicks;
    private static Path output;

    private ClientShutdownProbe() {}

    static void tick(Minecraft client, EvidenceWriter writer) {
        if (!(client.gui.screen() instanceof TitleScreen) || client.gui.overlay() != null) return;
        if (output == null) {
            output = client.gameDirectory.toPath().resolve("evidence/title-shutdown-"
                    + Instant.now().toString().replace(':', '-'));
            LOG.info("Title-only shutdown diagnostic; no world will open. Evidence: {}", output);
        }
        titleTicks++;
        if (titleTicks == 40) {
            JsonObject report = inspectAnimatica();
            report.addProperty("at", Instant.now().toString());
            report.addProperty("worldLoaded", client.level != null || client.getSingleplayerServer() != null);
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(report);
            LOG.info("Executor observation: {}", json);
            writer.execute(() -> {
                try { Files.createDirectories(output); Files.writeString(output.resolve("executor.json"), json); }
                catch (IOException failure) { writer.fail(failure); }
            });
            Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), image -> {
                if (!writer.execute(() -> {
                    try (image) { Files.createDirectories(output); image.writeToFile(output.resolve("title.png")); }
                    catch (IOException failure) { writer.fail(failure); }
                })) image.close();
            });
        }
        if (titleTicks == 100) {
            LOG.info("Title-only diagnostic requesting normal client stop; worldLoaded={}", client.level != null);
            client.stop();
        }
    }

    private static JsonObject inspectAnimatica() {
        JsonObject report = new JsonObject();
        report.addProperty("target", "io.github.foundationgames.animatica.animation.AnimatedTexture.EXECUTORS");
        report.addProperty("method", "Read exact public executor field and allocate one unstarted factory probe; no tasks submitted");
        try {
            Class<?> type = Class.forName("io.github.foundationgames.animatica.animation.AnimatedTexture", false,
                    ClientShutdownProbe.class.getClassLoader());
            Object value = type.getField("EXECUTORS").get(null);
            report.addProperty("executorClass", value.getClass().getName());
            if (value instanceof ThreadPoolExecutor pool) {
                // Reading a static field may initialize its class. The pinned initializer creates
                // an empty executor, so nonzero pool size here cannot be caused by this field read.
                report.addProperty("poolSizeBeforeFactoryProbe", pool.getPoolSize());
                report.addProperty("corePoolSize", pool.getCorePoolSize());
                report.addProperty("maximumPoolSize", pool.getMaximumPoolSize());
                report.addProperty("activeCount", pool.getActiveCount());
                report.addProperty("queuedTasks", pool.getQueue().size());
                report.addProperty("completedTasks", pool.getCompletedTaskCount());
                report.addProperty("shutdown", pool.isShutdown());
                Thread unstarted = pool.getThreadFactory().newThread(() -> {});
                report.addProperty("unstartedFactoryThreadName", unstarted.getName());
                report.addProperty("unstartedFactoryThreadDaemon", unstarted.isDaemon());
                report.addProperty("unstartedFactoryThreadState", unstarted.getState().name());
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException unavailable) {
            report.addProperty("unavailable", unavailable.toString());
        }
        JsonArray pools = new JsonArray();
        Thread.getAllStackTraces().forEach((thread, stack) -> {
            if (!thread.getName().startsWith("pool-") && !thread.getName().startsWith("villagers-animatica-")) return;
            JsonObject item = new JsonObject();
            item.addProperty("name", thread.getName()); item.addProperty("daemon", thread.isDaemon());
            item.addProperty("state", thread.getState().name());
            JsonArray frames = new JsonArray();
            for (StackTraceElement frame : stack) frames.add(frame.toString());
            item.add("stack", frames); pools.add(item);
        });
        report.add("livePoolThreads", pools);
        return report;
    }
}
