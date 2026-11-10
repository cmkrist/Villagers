package local.villagers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import local.villagers.training.PacingMonitor;
import local.villagers.training.TickStatistics;
import local.villagers.training.TrainingConfig;
import local.villagers.training.TrainingCheckpoint;
import local.villagers.training.TrainingCheckpointStore;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Bounded, rendered development trials. Never modifies random tick speed, spawns replacements or sprints. */
public final class TrainingTrial {
    private static final Logger LOG = LoggerFactory.getLogger("villagers-training");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson COMPACT = new Gson();
    private static final Map<MinecraftServer, Run> RUNS = new IdentityHashMap<>();
    private static final Set<MinecraftServer> PLAY_REQUESTED = ConcurrentHashMap.newKeySet();
    private static final Map<MinecraftServer, Map<String, Object>> PUBLISHED = new ConcurrentHashMap<>();
    private static boolean initialized;
    private static volatile TrainingConfig config;

    private TrainingTrial() {}

    /** Call once from the main initializer; registration itself is inert outside opted-in dev worlds. */
    public static void initialize() {
        if (initialized) return;
        initialized = true;
        if (!DevMode.enabled()) return;
        config = TrainingConfig.fromProperties(System.getProperties());
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (permitted(server)) server.tickRateManager().setTickRate(20);
        });
        ServerTickEvents.END_SERVER_TICK.register(TrainingTrial::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            Run run = RUNS.remove(server);
            if (run != null) run.close();
            PLAY_REQUESTED.remove(server);
            PUBLISHED.remove(server);
        });
    }

    /** Root may call from its existing command-registration callback. */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("trainingstatus").executes(context -> {
                var source = context.getSource();
                source.sendSuccess(() -> Component.literal(COMPACT.toJson(statusSnapshot(source.getServer()))), false);
                return 1;
            }))
            .then(Commands.literal("play").executes(context -> play(context.getSource()))));
    }

    private static boolean permitted(MinecraftServer server) {
        return DevMode.inWorld(server, DevMode.TRIAL_WORLD);
    }

    private static void tick(MinecraftServer server) {
        if (!permitted(server) || config == null) return;
        Run run = RUNS.get(server);
        if (run == null) {
            if (!VillageTrial.isReady(server)) return;
            run = new Run(server, PLAY_REQUESTED.contains(server));
            RUNS.put(server, run);
        }
        run.tick();
    }

    private static int play(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        if (!permitted(server)) {
            source.sendFailure(Component.literal("Training controls require the opted-in VillageTrial development world."));
            return 0;
        }
        PLAY_REQUESTED.add(server);
        server.tickRateManager().stopSprinting();
        server.tickRateManager().setTickRate(20);
        for (var player : server.getPlayerList().getPlayers()) player.setGameMode(GameType.CREATIVE);
        Run run = RUNS.get(server);
        if (run == null) {
            run = new Run(server, true);
            RUNS.put(server, run);
        }
        if (run != null) run.finish("play", "Acceleration stopped explicitly; observer is creative; residents retain normal vulnerability.");
        source.sendSuccess(() -> Component.literal("Play mode: 20 TPS, creative observer, automatic acceleration disabled across restarts."), false);
        return 1;
    }

    /** Called on the server thread when a required observation channel can no longer record evidence. */
    public static void stopForEvidence(MinecraftServer server, String reason) {
        if (!permitted(server) || config == null) return;
        Run run = RUNS.get(server);
        if (run == null) {
            run = new Run(server, true);
            RUNS.put(server, run);
        }
        run.finish("failed_evidence", reason + "; returned to 20 TPS, automatic resume disabled.");
    }

    /** Read-only immutable publication safe for the rendered client's observation thread. */
    public static Map<String, Object> statusSnapshot(MinecraftServer server) {
        Map<String, Object> current = PUBLISHED.get(server);
        if (current != null) return current;
        if (config == null) return Map.of("state", "disabled");
        return Map.of("state", PLAY_REQUESTED.contains(server) ? "play_waiting_for_trial" : "waiting_for_trial",
            "role", config.roleName(), "learning_mode", config.learningMode(),
            "configured_target_tps", config.requestedTps(), "game_tick_budget", TrainingConfig.GAME_TICK_BUDGET);
    }

    /** Called only after an explicit allowed /tick rate command actually returns through its native callback. */
    public static void recordRateCommand(MinecraftServer server, int requestedRate) {
        Run run = RUNS.get(server);
        if (run != null) {
            run.windowStartNano = System.nanoTime();
            run.windowStartTick = server.getTickCount();
            run.pacing.reset();
            run.currentRate = server.tickRateManager().tickrate();
            run.emit("explicit_tick_rate_command", "Requested=" + requestedRate + "; native actual=" + run.currentRate);
        }
    }

    private static final class Run {
        final MinecraftServer server;
        final String id;
        final Path evidence;
        final ThreadPoolExecutor writer;
        final PacingMonitor pacing = new PacingMonitor();
        final long startNano = System.nanoTime();
        final Instant wallStarted = Instant.now();
        final long startGameTick;
        final long segmentStartGameTick;
        final long resumedElapsedTicks;
        final TrainingCheckpointStore checkpoints;
        boolean checkpointWritable;
        final int startServerTick;
        long windowStartNano = startNano, previousTickNano = startNano;
        int windowStartTick, lastAuditTick;
        long lateIntervals, measuredIntervals;
        long stoppedElapsedTicks = -1;
        double achievedTps, windowSeconds, currentRate;
        boolean active, automaticPromotionDisabled;
        volatile boolean evidenceFailed;
        String stage;

        Run(MinecraftServer server, boolean playRequested) {
            this.server = server;
            segmentStartGameTick = server.overworld().getGameTime();
            startServerTick = windowStartTick = lastAuditTick = server.getTickCount();
            Path root = server.getWorldPath(LevelResource.ROOT).resolve("villagers");
            checkpoints = new TrainingCheckpointStore(root);
            TrainingCheckpoint previous = null;
            String checkpointError = null;
            boolean legacy = false;
            try {
                previous = checkpoints.load();
                if (previous != null) previous.requireCompatible(config);
                else if (Files.isDirectory(root.resolve("training-evidence"))) {
                    try (var entries = Files.list(root.resolve("training-evidence"))) {
                        legacy = entries.findAny().isPresent();
                    }
                }
                checkpointWritable = true;
            } catch (IOException | IllegalArgumentException failure) {
                previous = null; // Never append this runtime to evidence from an incompatible role.
                checkpointError = failure.getMessage();
                LOG.error("Training restart state refused; retaining 20 TPS", failure);
            }
            id = previous != null ? previous.runId() : config.roleName() + "-"
                    + wallStarted.toString().replace(':', '-') + "-" + UUID.randomUUID().toString().substring(0, 8);
            startGameTick = previous == null ? segmentStartGameTick : previous.startGameTick();
            resumedElapsedTicks = previous == null ? 0 : previous.elapsedAt(segmentStartGameTick);
            evidence = root.resolve("training-evidence").resolve(id);
            writer = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(256), task -> {
                Thread thread = new Thread(task, "villagers-training-evidence"); thread.setDaemon(true); return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
            automaticPromotionDisabled = previous != null && previous.promotionDisabled();
            active = checkpointError == null && !legacy && !playRequested && config.role() != TrainingConfig.Role.PLAY
                    && (previous == null || previous.resumable()) && resumedElapsedTicks < TrainingConfig.GAME_TICK_BUDGET;
            stage = checkpointError != null ? "failed_checkpoint" : legacy ? "legacy_untracked"
                    : playRequested || config.role() == TrainingConfig.Role.PLAY ? "play"
                    : active ? "running" : resumedElapsedTicks >= TrainingConfig.GAME_TICK_BUDGET
                    ? "completed_bounded_run" : previous.stage();
            if (!active) stoppedElapsedTicks = resumedElapsedTicks;
            // Reserve identity/budget durably before enabling acceleration. No writer is running yet.
            if (checkpointWritable) {
                try { checkpoints.save(checkpoint()); }
                catch (IOException failure) {
                    checkpointWritable = false;
                    stoppedElapsedTicks = elapsedGameTicks();
                    active = false; stage = "failed_checkpoint";
                    LOG.error("Could not reserve training restart state; retaining 20 TPS", failure);
                }
            }
            currentRate = active && !automaticPromotionDisabled ? config.initialTps() : 20;
            server.tickRateManager().setTickRate((float) currentRate);
            emit(previous == null ? "run_started" : "run_restored",
                    checkpointError != null ? checkpointError : legacy
                    ? "Historical evidence lacks a restart marker; no new budget granted. Use a fresh independent experiment world."
                    : "Persistent identity, consumed exposure and terminal state restored; measurements are session-local.");
        }

        void tick() {
            long now = System.nanoTime();
            double intervalMs = (now - previousTickNano) / 1e6;
            previousTickNano = now;
            measuredIntervals++;
            if (intervalMs > 1500.0 / Math.max(1, currentRate)) lateIntervals++;
            if (!active) return;
            if (evidenceFailed) { finish("failed_evidence", "Evidence persistence failed; returned to 20 TPS."); return; }
            if (server.tickRateManager().isSprinting()) {
                server.tickRateManager().stopSprinting();
                finish("stopped_sprint_detected", "Unbounded/sprint pacing is outside this trial; returned to 20 TPS.");
                return;
            }
            double actualRate = server.tickRateManager().tickrate();
            if (actualRate != currentRate) {
                currentRate = actualRate;
                pacing.reset();
                windowStartNano = now; windowStartTick = server.getTickCount();
                emit("tick_rate_changed_externally", "Observed native rate=" + actualRate + "; automatic maximum remains configured property.");
            }
            if (actualRate != 20 && actualRate != 40 && actualRate != 80) {
                finish("stopped_invalid_rate", "Only 20, 40 or 80 TPS are permitted by this training controller."); return;
            }
            if (elapsedGameTicks() >= TrainingConfig.GAME_TICK_BUDGET) {
                finish("completed_bounded_run", "48000 additional game ticks reached; returned to 20 TPS. Inspect cohort outcomes before drawing learning conclusions."); return;
            }
            if (server.getTickCount() - lastAuditTick < 200) return;
            lastAuditTick = server.getTickCount();
            Map<String, Integer> cohort = VillageTrial.cohortCounts(server);
            int original = cohort.getOrDefault("original", 0), alive = cohort.getOrDefault("alive", 0);
            int terminal = cohort.getOrDefault("terminal", 0), missing = cohort.getOrDefault("missing", 0);
            if (original == 0) { finish("failed_no_cohort", "No original cohort; no replacements were spawned."); return; }
            if (alive == 0 && missing == 0 && terminal == original) {
                finish("failed_cohort_extinct", "Every original UUID has a confirmed terminal loss; no replacements were spawned."); return;
            }
            windowSeconds = (now - windowStartNano) / 1e9;
            achievedTps = (server.getTickCount() - windowStartTick) / Math.max(.001, windowSeconds);
            TickStatistics costs = costs();
            emit("tick_audit", "Native tick-cost ring and monotonic wall-clock measurements; missing UUIDs are not presumed dead.");
            if (windowSeconds >= 5) {
                var decision = pacing.observe(automaticPromotionDisabled ? 20 : config.requestedTps(), currentRate,
                    achievedTps, costs.meanMspt(), costs.p95Mspt(), windowSeconds);
                if (decision == PacingMonitor.Decision.BACK_OFF_TO_20) {
                    automaticPromotionDisabled = true;
                    changeRate(20, "safety_backoff", "Two sustained overload/underachievement windows; no automatic reacceleration this session.");
                } else if (decision == PacingMonitor.Decision.PROMOTE_TO_80) {
                    changeRate(80, "capacity_promotion", "Two measured healthy 40-TPS windows and an explicit configured 80-TPS maximum.");
                }
                windowStartNano = now; windowStartTick = server.getTickCount();
            }
        }

        TickStatistics costs() {
            long[] nativeSamples = server.getTickTimesNanos();
            return TickStatistics.summarize(Arrays.copyOf(nativeSamples, nativeSamples.length), Math.max(1, currentRate));
        }

        void changeRate(int rate, String event, String reason) {
            server.tickRateManager().setTickRate(rate);
            currentRate = rate;
            windowStartNano = System.nanoTime(); windowStartTick = server.getTickCount();
            pacing.reset();
            emit(event, reason);
        }

        void finish(String outcome, String detail) {
            stoppedElapsedTicks = elapsedGameTicks();
            active = false;
            stage = outcome;
            server.tickRateManager().stopSprinting();
            server.tickRateManager().setTickRate(20);
            currentRate = 20;
            emit("run_stopped", detail);
        }

        Map<String, Object> snapshot(String event, String detail) {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("at", Instant.now().toString()); report.put("run_id", id);
            report.put("event", event); report.put("detail", detail); report.put("state", stage);
            report.put("role", config.roleName()); report.put("learning_mode", config.learningMode());
            report.put("wall_started_at", wallStarted.toString()); report.put("elapsed_wall_seconds", (System.nanoTime() - startNano) / 1e9);
            report.put("server_tick", server.getTickCount()); report.put("start_server_tick", startServerTick);
            report.put("elapsed_server_ticks", server.getTickCount() - startServerTick);
            report.put("game_time", server.overworld().getGameTime()); report.put("start_game_time", startGameTick);
            report.put("elapsed_game_ticks", elapsedGameTicks());
            report.put("resumed_elapsed_game_ticks", resumedElapsedTicks);
            report.put("measurement_scope", "wall-clock and server-tick measurements cover this process session only");
            report.put("game_tick_budget", TrainingConfig.GAME_TICK_BUDGET);
            report.put("configured_target_tps", config.requestedTps()); report.put("native_target_tps", server.tickRateManager().tickrate());
            report.put("achieved_wall_tps", achievedTps); report.put("measurement_window_seconds", windowSeconds);
            report.put("measurement_window_server_ticks", server.getTickCount() - windowStartTick);
            report.put("native_mean_mspt", server.getAverageTickTimeNanos() / 1e6);
            report.put("native_tick_cost_last_100", costs());
            report.put("wall_late_intervals", lateIntervals); report.put("wall_measured_intervals", measuredIntervals);
            report.put("late_interval_definition", "Elapsed interval between observed server ticks exceeds 1.5 times the current target interval; distinct from native tick work cost.");
            report.put("frozen", server.tickRateManager().isFrozen()); report.put("sprinting", server.tickRateManager().isSprinting());
            report.put("automatic_promotion_disabled", automaticPromotionDisabled || !active);
            report.put("cohort", Map.copyOf(VillageTrial.cohortCounts(server)));
            report.put("learning_statistics", Villagers.learningStatistics());
            report.put("evidence_directory", evidence.toString());
            report.put("learning_claim", "No improvement inferred from speed, elapsed ticks, labels or survival alone; compare independent controlled cohorts.");
            return Map.copyOf(report);
        }

        long elapsedGameTicks() {
            if (stoppedElapsedTicks >= 0) return stoppedElapsedTicks;
            return resumedElapsedTicks + Math.min(Math.max(0, server.overworld().getGameTime() - segmentStartGameTick),
                    TrainingConfig.GAME_TICK_BUDGET - resumedElapsedTicks);
        }

        TrainingCheckpoint checkpoint() {
            return new TrainingCheckpoint(TrainingCheckpoint.SCHEMA, id, config.roleName(), config.learningMode(),
                    startGameTick, server.overworld().getGameTime(), elapsedGameTicks(), stage, automaticPromotionDisabled);
        }

        void emit(String event, String detail) {
            Map<String, Object> report = snapshot(event, detail);
            TrainingCheckpoint restartState = checkpoint();
            PUBLISHED.put(server, report);
            String line = COMPACT.toJson(report) + "\n", status = JSON.toJson(report);
            try {
                writer.execute(() -> {
                    try {
                        if (checkpointWritable) checkpoints.save(restartState);
                        Files.createDirectories(evidence);
                        Files.writeString(evidence.resolve("ticks.jsonl"), line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        Path temporary = evidence.resolve("status.tmp");
                        Files.writeString(temporary, status);
                        Files.move(temporary, evidence.resolve("status.json"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (IOException failure) { evidenceFailed = true; LOG.error("Could not persist training evidence", failure); }
                });
            } catch (RejectedExecutionException failure) { evidenceFailed = true; LOG.error("Bounded training evidence queue is full", failure); }
            if (!event.equals("tick_audit")) LOG.info("Training {} role={} state={} TPS={} reason={}", id, config.roleName(), stage, currentRate, detail);
        }

        void close() {
            if (active && evidenceFailed) finish("failed_evidence", "Evidence persistence failed before shutdown; no automatic resume.");
            if (active) {
                stoppedElapsedTicks = elapsedGameTicks();
                active = false; stage = "suspended_shutdown";
                server.tickRateManager().stopSprinting();
                server.tickRateManager().setTickRate(20);
                currentRate = 20;
                emit("interrupted_shutdown", "Graceful shutdown; remaining exposure budget retained for restart, no completion claimed.");
            }
            else emit("world_shutdown", "Preserved the existing run outcome.");
            writer.shutdown();
            try {
                if (!writer.awaitTermination(10, TimeUnit.SECONDS)) {
                    LOG.error("Training evidence did not finish before shutdown; checkpoint may lag");
                } else if (checkpointWritable) {
                    // Drain older writes first, then commit the final exact shutdown/terminal state.
                    try { checkpoints.save(checkpoint()); }
                    catch (IOException failure) { LOG.error("Final training checkpoint could not be saved", failure); }
                }
            }
            catch (InterruptedException failure) { Thread.currentThread().interrupt(); }
        }
    }
}
