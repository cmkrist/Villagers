package local.villagers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import local.villagers.evidence.EvidenceWriter;
import local.villagers.lab.LabCommandPolicy;
import java.util.concurrent.TimeUnit;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import local.villagers.lab.ObservationBudget;

/** Opt-in rendered integration harness. Never active in an ordinary player session. */
public final class LabClient implements ClientModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("villagers-lab");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson COMPACT_JSON = new Gson();
    private static final boolean LEGACY_LAB = "lab".equals(System.getProperty("villagers.trial", "village"));
    private static final boolean WAR = "war".equals(System.getProperty("villagers.trial", "village"));
    private static final boolean NATIVE_REGIONS = "native-regions".equals(System.getProperty("villagers.trial", "village"));
    private static final String WORLD = WAR ? local.villagers.war.WarMap.WORLD : LEGACY_LAB ? DevMode.LAB_WORLD : DevMode.TRIAL_WORLD;
    private static final String ROLE = System.getProperty("villagers.role", "play");
    private final EvidenceWriter evidenceWriter = new EvidenceWriter("villagers-lab-evidence", 16,
            failure -> LOG.error("Rendered evidence failed; automatic observation stopped and training will return to 20 TPS", failure));
    private boolean evidenceFailureReported;
    private Path evidence;
    private boolean worldRequested;
    private boolean newWorld;
    private boolean labStarted;
    private boolean capturePending;
    private long startedAt;
    private long lastObservation;
    private long nextCapture;
    private long lastTitleUpdate;
    private int ticks;
    private int captureNumber;
    private int artifactNumber;
    private final ObservationBudget observationBudget = new ObservationBudget("play".equals(ROLE)
            ? Integer.getInteger("villagers.lab.maxPlayTicks", 0) : 0);
    private net.minecraft.server.MinecraftServer budgetServer;
    private boolean observationStopping;

    @Override
    public void onInitializeClient() {
        local.villagers.compat.AnimaticaShutdown.initialize();
        if (!DevMode.enabled()) return;
        if (observationBudget.limit() > 0) ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!DevMode.inWorld(server, WORLD)) return;
            if (budgetServer != server) { budgetServer = server; observationBudget.reset(); observationStopping=false; }
            if(observationStopping)return;
            long now = server.overworld().getGameTime();
            int minHealth=Integer.getInteger("villagers.lab.minHealth",0);
            Villager critical=null;
            boolean starvationGuard=Boolean.getBoolean("villagers.lab.starvationGuard");
            if(minHealth>0||starvationGuard)for(var entity:server.overworld().getAllEntities())
                if(entity instanceof Villager v&&Villagers.isManaged(v)){
                    var s=Villagers.existingSession(v.getUUID());
                    boolean lastStarvationInterval=starvationGuard&&v.getHealth()<=1&&s!=null
                        &&s.mind.foodLevel()==0&&s.mind.snapshot().food().timer()>=70;
                    if(minHealth>0&&v.getHealth()<=minHealth||lastStarvationInterval){critical=v;break;}
                }
            if (critical==null&&!observationBudget.expired(now)) return;
            observationStopping=true;
            // Stop exposure at the measured boundary, before the client/save
            // shutdown queue drains. A critical body must not tick on unseen.
            server.tickRateManager().setFrozen(true);
            if(critical!=null)LOG.info("Observation health stop: actor={} health={} threshold={} gameTime={}",critical.getUUID(),critical.getHealth(),minHealth,now);
            LOG.info("Observation stop: reason={} startTick={} endTick={} limit={}; requesting normal quit",
                    critical==null?"tick budget":"health guard",observationBudget.started(), now, observationBudget.limit());
            String nativeSnapshot = JSON.toJson(VillageFamilyProbe.budgetSnapshot(server));
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (client.getSingleplayerServer() != server) return;
                if (evidence != null) evidenceWriter.execute(() -> {
                    try { Files.createDirectories(evidence); Files.writeString(evidence.resolve("budget-native.json"), nativeSnapshot); }
                    catch (IOException failure) { evidenceWriter.fail(failure); }
                });
                if (client.player != null && client.level != null) saveObservation(client, "budget-end");
                client.stop();
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            evidenceWriter.shutdown();
            try {
                if (!evidenceWriter.awaitTermination(10, TimeUnit.SECONDS)) LOG.error("Rendered evidence still pending at client shutdown");
            } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        });
        LOG.info("Rendered {} enabled; command file is run/lab-command.txt", WORLD);
    }

    private void tick(Minecraft client) {
        if ("play".equals(ROLE) && Boolean.getBoolean("villagers.lab.titleShutdown")) {
            ClientShutdownProbe.tick(client, evidenceWriter);
            return; // This diagnostic never opens, creates or ticks a world.
        }
        ticks++;
        if (evidence == null) {
            Path evidenceRoot = Path.of(System.getProperty("villagers.evidence.root",
                client.gameDirectory.toPath().resolve("evidence").toString()));
            evidence = evidenceRoot.resolve(ROLE + "-" + Instant.now().toString().replace(':', '-')
                + "-" + UUID.randomUUID().toString().substring(0, 8));
            client.options.pauseOnLostFocus = false;
            client.options.framerateLimit().set(60);
            client.getWindow().setTitle("Villagers: Learning Lives — " + WORLD);
            LOG.info("Unique rendered evidence directory: {}", evidence);
        }
        if (!worldRequested && client.gui.screen() instanceof TitleScreen && client.gui.overlay() == null) {
            worldRequested = true;
            if (Files.isRegularFile(client.gameDirectory.toPath().resolve("saves/" + WORLD + "/level.dat"))) {
                client.createWorldOpenFlows().openWorld(WORLD, () -> worldRequested = false);
            } else {
                newWorld = true;
                client.createWorldOpenFlows().createFreshLevel(WORLD,
                    new LevelSettings(WORLD, GameType.CREATIVE,
                        new LevelSettings.DifficultySettings(Difficulty.NORMAL, false, false),
                        true, WorldDataConfiguration.DEFAULT),
                    new WorldOptions(846206L, !LEGACY_LAB, false),
                    registries -> WAR || NATIVE_REGIONS ? registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(
                        net.minecraft.resources.ResourceKey.create(Registries.WORLD_PRESET,
                            net.minecraft.resources.Identifier.fromNamespaceAndPath("villagers",NATIVE_REGIONS ? "native_four_regions" : "four_biomes_war")))
                        .value().createWorldDimensions() : LEGACY_LAB ? WorldPresets.createNormalWorldDimensions(registries)
                        .replaceOverworldGenerator(registries, new FlatLevelSource(
                            FlatLevelGeneratorSettings.getDefault(
                                registries.lookupOrThrow(Registries.BIOME),
                                registries.lookupOrThrow(Registries.STRUCTURE_SET),
                                registries.lookupOrThrow(Registries.PLACED_FEATURE))))
                        : WorldPresets.createNormalWorldDimensions(registries),
                    client.gui.screen());
            }
        }
        if (client.player == null || client.level == null || client.getSingleplayerServer() == null) return;
        // The command channel and automation belong exclusively to this disposable world.
        if (!WORLD.equals(client.getSingleplayerServer().getWorldData().getLevelName())) return;
        if (evidenceWriter.failed()) {
            client.getWindow().setTitle("Villagers — EVIDENCE FAILED — " + WORLD);
            if (!evidenceFailureReported) {
                evidenceFailureReported = true;
                var server = client.getSingleplayerServer();
                server.execute(() -> TrainingTrial.stopForEvidence(server, "Rendered observation persistence failed"));
            }
            // Keep the explicit command channel available for inspection/normal shutdown.
            if (ticks % 10 == 0) consumeCommands(client);
            return;
        }
        if (!labStarted) {
            labStarted = true;
            startedAt = System.currentTimeMillis();
            nextCapture = startedAt + 15_000;
            // Release once after loading; the user can click back in to explore normally.
            client.mouseHandler.releaseMouse();
            if (WAR) {
                if (newWorld) runCommand(client, "tp @a -96 80 -88 -90 15");
            } else if (LEGACY_LAB) {
                runCommand(client, "villagers lab");
                runCommand(client, "gamemode spectator @a");
                runCommand(client, "tp @a 15 -48 24 145 28");
            } else {
                runCommand(client, "villagers village");
            }
        }
        long now = System.currentTimeMillis();
        if (now - lastTitleUpdate >= 1000) {
            lastTitleUpdate = now;
            var training = TrainingTrial.statusSnapshot(client.getSingleplayerServer());
            client.getWindow().setTitle("Villagers — " + ROLE + " / " + System.getProperty("villagers.learning", "adaptive")
                + " — " + client.getSingleplayerServer().tickRateManager().tickrate() + " TPS — "
                + training.getOrDefault("state", "waiting") + " — " + WORLD);
        }
        if (ticks % 10 == 0) consumeCommands(client);
        if (now - lastObservation >= 5_000) {
            lastObservation = now;
            saveObservation(client, "periodic");
        }
        if (now >= nextCapture && !capturePending && client.gui.screen() == null && client.gui.overlay() == null) {
            capture(client, String.format("%s-%03d-%ds", WAR ? "war" : LEGACY_LAB ? "lab" : "village", ++captureNumber, (now - startedAt) / 1000));
            nextCapture = captureNumber == 1 ? startedAt + 60_000 : now + 60_000;
        }
    }

    /** Write a temporary command file and rename to lab-command.txt for atomic delivery. */
    private void consumeCommands(Minecraft client) {
        Path queue = client.gameDirectory.toPath().resolve("lab-command.txt");
        if (!Files.isRegularFile(queue)) return;
        Path claimed = client.gameDirectory.toPath().resolve("lab-command.processing.txt");
        try {
            if (Files.size(queue) > 8_192) {
                Files.move(queue, client.gameDirectory.toPath().resolve("lab-command.rejected.txt"),
                    StandardCopyOption.REPLACE_EXISTING);
                LOG.warn("Rejected oversized lab command file");
                return;
            }
            Files.move(queue, claimed, StandardCopyOption.REPLACE_EXISTING);
            List<String> commands = Files.readAllLines(claimed);
            Files.deleteIfExists(claimed);
            for (String line : commands.stream().limit(32).toList()) {
                String command = line.strip();
                if (command.isEmpty() || command.startsWith("#")) continue;
                if (command.equals("quit")) {
                    commandEvent(command, "graceful-stop-requested", null);
                    client.stop();
                    return;
                } else if (command.equals("resume")) {
                    // Explicit command only: never automatically dismiss a user's menu.
                    if ("play".equals(ROLE) && Integer.getInteger("villagers.lab.maxPlayTicks", 0) > 0
                            && client.gui.screen() instanceof net.minecraft.client.gui.screens.PauseScreen
                            && client.gui.overlay() == null) {
                        commandEvent(command, "bounded-play-resume", null);
                        client.gui.setScreen(null);
                        client.mouseHandler.releaseMouse();
                    } else commandEvent(command, "rejected", "Requires a paused bounded play-copy observation");
                } else if (command.matches("screenshot [A-Za-z0-9_-]{1,64}")) {
                    capture(client, command.substring(11));
                } else if (LabCommandPolicy.allowed(command)) {
                    runCommand(client, command);
                } else {
                    LOG.warn("Rejected lab command: {}", command);
                    commandEvent(command, "rejected", null);
                }
            }
        } catch (IOException exception) {
            LOG.error("Could not consume lab command file", exception);
        }
    }

    private void runCommand(Minecraft client, String command) {
        var server = client.getSingleplayerServer();
        if (server == null || client.player == null) return;
        UUID observerId = client.player.getUUID();
        server.execute(() -> {
            try {
                var observer = server.getPlayerList().getPlayer(observerId);
                if (observer == null) {
                    commandEvent(command, "observer_disconnected", null);
                    return;
                }
                boolean[] resultReported = {false};
                var source = observer.createCommandSourceStack().withCallback((success, result) -> {
                    resultReported[0] = true;
                    commandResult(command, success, result, observerId);
                    if (success && command.matches("tick rate (20|40|80)")) {
                        TrainingTrial.recordRateCommand(server, Integer.parseInt(command.substring(10)));
                    }
                });
                server.getCommands().performPrefixedCommand(source, command);
                if (!resultReported[0]) commandEvent(command, "dispatched_without_result", null);
            } catch (Exception exception) {
                commandEvent(command, "exception", exception.toString());
                LOG.error("Lab command failed: {}", command, exception);
            }
        });
    }

    private void commandResult(String command, boolean success, int result, UUID observerId) {
        JsonObject event = new JsonObject();
        event.addProperty("at", Instant.now().toString());
        event.addProperty("world", WORLD); event.addProperty("role", ROLE);
        event.addProperty("command", command); event.addProperty("status", "native_result");
        event.addProperty("source_player", observerId.toString());
        event.addProperty("success", success); event.addProperty("result_code", result);
        String json = COMPACT_JSON.toJson(event) + "\n";
        evidenceWriter.execute(() -> writeAppend(evidence.resolve("commands.jsonl"), json));
    }

    private void commandEvent(String command, String status, String error) {
        JsonObject event = new JsonObject();
        event.addProperty("at", Instant.now().toString());
        event.addProperty("world", WORLD);
        event.addProperty("command", command);
        event.addProperty("status", status);
        if (error != null) event.addProperty("error", error);
        String json = COMPACT_JSON.toJson(event) + "\n";
        evidenceWriter.execute(() -> writeAppend(evidence.resolve("commands.jsonl"), json));
    }

    private void capture(Minecraft client, String label) {
        if (evidenceWriter.failed() || capturePending || client.gui.screen() != null || client.gui.overlay() != null) return;
        capturePending = true;
        String artifact = label + "-" + System.currentTimeMillis() + "-" + (++artifactNumber);
        saveObservation(client, artifact);
        try {
            Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), nativeImage -> {
                boolean accepted = evidenceWriter.execute(() -> {
                    try (nativeImage) {
                        Files.createDirectories(evidence);
                        nativeImage.writeToFile(evidence.resolve(artifact + ".png"));
                        LOG.info("Rendered lab screenshot saved: {}", artifact);
                    } catch (IOException exception) {
                        evidenceWriter.fail(exception);
                    } finally {
                        client.execute(() -> capturePending = false);
                    }
                });
                if (!accepted) {
                    nativeImage.close();
                    client.execute(() -> capturePending = false);
                }
            });
        } catch (Exception exception) {
            capturePending = false;
            LOG.error("Could not capture rendered lab", exception);
        }
    }

    private void saveObservation(Minecraft client, String label) {
        if (evidenceWriter.failed()) return;
        JsonObject observation = new JsonObject();
        observation.addProperty("at", Instant.now().toString());
        observation.addProperty("elapsed_ms", System.currentTimeMillis() - startedAt);
        observation.addProperty("label", label);
        observation.addProperty("source", "rendered client; only client-tracked entities");
        observation.addProperty("world", WORLD);
        observation.addProperty("role", ROLE);
        observation.addProperty("learning_mode", System.getProperty("villagers.learning", "adaptive"));
        observation.addProperty("native_target_tps", client.getSingleplayerServer().tickRateManager().tickrate());
        observation.add("training", COMPACT_JSON.toJsonTree(TrainingTrial.statusSnapshot(client.getSingleplayerServer())));
        observation.addProperty("game_time", client.getSingleplayerServer().overworld().getGameTime());
        observation.addProperty("trial", WAR ? "authored four-biome war scenario" : LEGACY_LAB ? "legacy authored lab" : "natural generated village");
        observation.addProperty("server_tick", client.getSingleplayerServer().getTickCount());
        observation.addProperty("screen", client.gui.screen() == null ? "" : client.gui.screen().getClass().getSimpleName());
        observation.addProperty("frame_ns", client.getFrameTimeNs());
        JsonObject camera = new JsonObject();
        camera.addProperty("x", client.player.getX());
        camera.addProperty("y", client.player.getY());
        camera.addProperty("z", client.player.getZ());
        camera.addProperty("yaw", client.player.getYRot());
        camera.addProperty("pitch", client.player.getXRot());
        observation.add("camera", camera);
        JsonArray villagers = new JsonArray();
        JsonArray golems = new JsonArray();
        int tracked = 0;
        for (var entity : client.level.entitiesForRendering()) {
            tracked++;
            if (!(entity instanceof Villager) && !(entity instanceof IronGolem)) continue;
            JsonObject villager = new JsonObject();
            villager.addProperty("uuid", entity.getUUID().toString());
            villager.addProperty("name", entity.getName().getString());
            villager.addProperty("x", entity.getX());
            villager.addProperty("y", entity.getY());
            villager.addProperty("z", entity.getZ());
            villager.addProperty("alive", entity.isAlive());
            if (entity instanceof LivingEntity living) villager.addProperty("health", living.getHealth());
            if (entity instanceof Villager) villagers.add(villager);
            else golems.add(villager);
        }
        observation.addProperty("tracked_entities", tracked);
        observation.addProperty("tracked_villagers", villagers.size());
        observation.add("villagers", villagers);
        observation.addProperty("tracked_iron_golems", golems.size());
        observation.add("iron_golems", golems);
        String json = JSON.toJson(observation);
        String line = COMPACT_JSON.toJson(observation) + "\n";
        evidenceWriter.execute(() -> {
            try {
                Files.createDirectories(evidence);
                Files.writeString(evidence.resolve("latest.json"), json);
                if (!label.equals("periodic")) Files.writeString(evidence.resolve(label + ".json"), json);
                writeAppend(evidence.resolve("observations.jsonl"), line);
            } catch (IOException exception) {
                evidenceWriter.fail(exception);
            }
        });
    }

    private void writeAppend(Path file, String text) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            evidenceWriter.fail(exception);
        }
    }
}
