package local.villagers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import local.villagers.evidence.EvidenceWriter;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/** A persistent observational trial in an actual generated village. Never constructs or stocks a village. */
public final class VillageTrial {
    private static final Logger LOG = LoggerFactory.getLogger("villagers-village-trial");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson COMPACT = new Gson();
    private static final String WORLD = DevMode.TRIAL_WORLD;
    private static final int RADIUS = 96;
    // Placement-region rings, not a promise to search only this many literal chunks.
    private static final int LOCATE_RADIUS = 8;
    private static final Map<MinecraftServer, Trial> TRIALS = new IdentityHashMap<>();
    private static boolean hooksRegistered;
    private static final Set<Block> RESOURCE_BLOCKS = Set.of(Blocks.FARMLAND, Blocks.CHEST, Blocks.BARREL,
        Blocks.HAY_BLOCK, Blocks.BELL, Blocks.COMPOSTER, Blocks.FURNACE, Blocks.BLAST_FURNACE,
        Blocks.SMOKER, Blocks.CARTOGRAPHY_TABLE, Blocks.SMITHING_TABLE, Blocks.FLETCHING_TABLE,
        Blocks.LOOM, Blocks.STONECUTTER, Blocks.GRINDSTONE, Blocks.LECTERN, Blocks.BREWING_STAND,
        Blocks.CAULDRON, Blocks.WATER_CAULDRON);

    private VillageTrial() {}
    public static boolean isReady(MinecraftServer server) {
        Trial trial=TRIALS.get(server);
        return trial != null && trial.readyThisSession && trial.stage().equals("active");
    }
    public static Map<String,Integer> cohortCounts(MinecraftServer server) {
        Trial trial=TRIALS.get(server);
        if (trial == null) return Map.of("original",0,"alive",0,"terminal",0,"missing",0);
        int alive=0,terminal=0,missing=0;
        for (var entry:trial.marker.getAsJsonObject("cohort").entrySet()) {
            var entity=trial.level.getEntityInAnyDimension(UUID.fromString(entry.getKey()));
            var known=trial.marker.getAsJsonObject("known_actors").getAsJsonObject(entry.getKey());
            if (known != null && known.has("terminal_event")) terminal++;
            else if (entity instanceof Villager villager && villager.isAlive()) alive++;
            else if (entity instanceof LivingEntity living && !living.isAlive()) terminal++;
            else missing++;
        }
        return Map.of("original",trial.marker.getAsJsonObject("cohort").size(),"alive",alive,"terminal",terminal,"missing",missing);
    }

    public static void noteIntervention(ServerLevel level, String detail) {
        Trial trial=TRIALS.get(level.getServer());
        if (trial == null || !trial.stage().equals("active")) throw new IllegalStateException("Trial is not active");
        JsonArray events=trial.marker.has("post_baseline_interventions") ? trial.marker.getAsJsonArray("post_baseline_interventions") : new JsonArray();
        JsonObject entry=new JsonObject(); entry.addProperty("game_time",level.getGameTime()); entry.addProperty("detail",detail);
        events.add(entry); trial.marker.add("post_baseline_interventions",events); trial.persist();
        trial.event("explicit_intervention",null,detail,null);
    }


    /** The coordinator calls this from its command registration callback. */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("villagers")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("village").executes(context -> begin(context.getSource())))
            .then(Commands.literal("villageaudit").executes(context -> audit(context.getSource()))));
        if (hooksRegistered) return;
        hooksRegistered = true;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Trial trial = TRIALS.get(server);
            if (trial != null) trial.tick();
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity.level() instanceof ServerLevel level)) return;
            Trial trial = TRIALS.get(level.getServer());
            if (trial != null) trial.lifecycle(entity, "death", source.getMsgId(), true, null);
        });
        ServerLivingEntityEvents.MOB_CONVERSION.register((previous, replacement, params) -> {
            if (!(previous.level() instanceof ServerLevel level)) return;
            Trial trial = TRIALS.get(level.getServer());
            if (trial != null) trial.lifecycle(previous, "conversion", replacement.typeHolder().getRegisteredName(),
                true, replacement.getUUID());
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            Trial trial = TRIALS.get(level.getServer());
            if (trial != null && (entity instanceof Villager || entity instanceof IronGolem)) {
                var reason = entity.getRemovalReason();
                trial.lifecycle(entity, "unload", reason == null ? "unknown" : reason.name(),
                    reason == Entity.RemovalReason.KILLED, null);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            Trial trial = TRIALS.remove(server);
            if (trial != null) trial.close();
        });
    }

    private static boolean permitted(CommandSourceStack source) {
        if (DevMode.inWorld(source.getServer(), WORLD)) return true;
        source.sendFailure(Component.literal("Natural village commands require the opt-in VillageTrial development world."));
        return false;
    }

    private static int begin(CommandSourceStack source) {
        if (!permitted(source)) return 0;
        Trial existing = TRIALS.get(source.getServer());
        if (existing != null) {
            source.sendSuccess(() -> Component.literal("Village trial already " + existing.stage() + "; its original cohort is preserved."), false);
            return 1;
        }
        try {
            ServerPlayer observer = source.getEntity() instanceof ServerPlayer player ? player
                : source.getServer().getPlayerList().getPlayers().stream().findFirst().orElse(null);
            if (observer == null) {
                source.sendFailure(Component.literal("Wait for the visible human observer to join."));
                return 0;
            }
            Trial trial = new Trial(source.getServer(), observer);
            TRIALS.put(source.getServer(), trial);
            source.sendSuccess(() -> Component.literal("Natural village trial " + trial.stage() + "; no residents, golems, buildings or supplies will be spawned."), false);
            return 1;
        } catch (IOException | RuntimeException failure) {
            LOG.error("Village trial could not start; existing marker was preserved", failure);
            source.sendFailure(Component.literal("Village trial failed: " + failure.getMessage()));
            return 0;
        }
    }

    private static int audit(CommandSourceStack source) {
        if (!permitted(source)) return 0;
        Trial trial = TRIALS.get(source.getServer());
        if (trial == null) {
            source.sendFailure(Component.literal("Start or resume the original trial with /villagers village first."));
            return 0;
        }
        trial.audit("requested");
        source.sendSuccess(() -> Component.literal("Server village census queued under this world's villagers/village-evidence/."), false);
        return 1;
    }

    private static final class Trial {
        final MinecraftServer server;
        final ServerLevel level;
        final ServerPlayer observer;
        final Path markerFile;
        final Path evidence;
        final JsonObject marker;
        final BlockPos center;
        final List<ChunkPos> chunks = new ArrayList<>();
        final Map<ChunkPos, CompletableFuture<?>> loading = new LinkedHashMap<>();
        final List<ChunkPos> ownedTickets = new ArrayList<>();
        final EvidenceWriter writer = new EvidenceWriter("villagers-village-evidence", 128,
                failure -> LOG.error("Village evidence failed; trial will stop and release its loading tickets", failure));
        final JsonObject resources = new JsonObject();
        final JsonArray beds = new JsonArray();
        final JsonArray doors = new JsonArray();
        final JsonObject structures = new JsonObject();
        final boolean resumed;
        int requestedChunks;
        int scannedChunks;
        long lastChanged;
        long allLoadedAt;
        long lastAudit;
        String fingerprint = "";
        boolean cameraMoved;
        boolean readyThisSession;

        Trial(MinecraftServer server, ServerPlayer observer) throws IOException {
            this.server = server;
            this.level = server.overworld();
            this.observer = observer;
            markerFile = server.getWorldPath(LevelResource.ROOT).resolve("villagers/village-trial.json");
            evidence = markerFile.getParent().resolve("village-evidence");
            resumed = Files.exists(markerFile);
            if (resumed) {
                if (Files.size(markerFile) > 4_194_304) throw new IOException("Trial marker is oversized; refusing to reset it");
                marker = JsonParser.parseString(Files.readString(markerFile)).getAsJsonObject();
                if (marker.get("schema_version").getAsInt() != 1 || !WORLD.equals(marker.get("world").getAsString())
                    || marker.get("radius").getAsInt() != RADIUS) throw new IOException("Unsupported trial marker; refusing to relocate or overwrite it");
                JsonObject position = marker.getAsJsonObject("center");
                center = new BlockPos(position.get("x").getAsInt(), position.get("y").getAsInt(), position.get("z").getAsInt());
                if (Math.abs((long) center.getX()) > 29_999_000 || Math.abs((long) center.getZ()) > 29_999_000)
                    throw new IOException("Invalid persisted village center");
                if (!marker.has("cohort") || !marker.has("known_actors")) throw new IOException("Incomplete trial marker; refusing replacement adoption");
            } else {
                long locateStarted = System.nanoTime();
                BlockPos origin = server.getRespawnData().pos();
                BlockPos found = level.findNearestMapStructure(StructureTags.VILLAGE, origin, LOCATE_RADIUS, false);
                if (found == null) throw new IOException("No generated village found in the capped locate search; no actors were spawned");
                center = found.immutable();
                marker = new JsonObject();
                marker.addProperty("schema_version", 1);
                marker.addProperty("world", WORLD);
                marker.addProperty("dimension", level.dimension().identifier().toString());
                marker.addProperty("seed", level.getSeed());
                marker.addProperty("radius", RADIUS);
                marker.add("center", position(center));
                marker.add("locate_origin", position(origin));
                marker.addProperty("locate_search_radius_placement_rings", LOCATE_RADIUS);
                marker.addProperty("locate_millis", (System.nanoTime() - locateStarted) / 1e6);
                marker.addProperty("created_at", Instant.now().toString());
                marker.addProperty("created_game_time", level.getGameTime());
                marker.addProperty("stage", "loading");
                marker.add("cohort", new JsonObject());
                marker.add("known_actors", new JsonObject());
                marker.addProperty("interventions", "Observer creative-flight/teleport; bounded nonpersistent loading/simulation tickets; opt-in existing resident AI only. No actor spawns, actor teleports, buildings, supplies, time or gamerule edits.");
                atomicWrite(markerFile, JSON.toJson(marker));
            }
            // Resuming the native four-region map must preserve the observer's
            // chosen village, rather than silently unloading it for the baseline.
            cameraMoved=resumed&&NativeRegionsScenario.nativeRegions(level);
            for (int x = Math.floorDiv(center.getX() - RADIUS, 16); x <= Math.floorDiv(center.getX() + RADIUS, 16); x++) {
                for (int z = Math.floorDiv(center.getZ() - RADIUS, 16); z <= Math.floorDiv(center.getZ() + RADIUS, 16); z++) chunks.add(new ChunkPos(x, z));
            }
            chunks.sort(Comparator.comparingInt(pos -> Math.max(Math.abs(pos.x() - (center.getX() >> 4)), Math.abs(pos.z() - (center.getZ() >> 4)))));
            lastChanged = level.getGameTime();
            event("session_start", null, resumed ? "Original marker resumed; no relocation or cohort replacement" : "Natural village located", null);
        }

        String stage() { return marker.get("stage").getAsString(); }

        void tick() {
            if (stage().equals("failed")) { releaseTickets(); return; }
            if (writer.failed()) {
                failTrial(new IllegalStateException("Evidence persistence failed or its bounded queue filled"));
                return;
            }
            long now = level.getGameTime();
            try {
                if (now - lastAudit >= 200) audit("periodic");
                loading.entrySet().removeIf(entry -> {
                    if (entry.getValue().isCompletedExceptionally()) throw new IllegalStateException("Chunk generation failed at " + entry.getKey());
                    return entry.getValue().isDone();
                });
                // At most one asynchronous request per five ticks and two requests in flight.
                // DRAGON has loading+simulation flags and does not persist in the world save.
                if (requestedChunks < chunks.size() && loading.size() < 2 && server.getTickCount() % 5 == 0) {
                    ChunkPos next = chunks.get(requestedChunks++);
                    ownedTickets.add(next);
                    loading.put(next, level.getChunkSource().addTicketAndLoadWithRadius(TicketType.DRAGON, next, 2));
                }
                if (!cameraMoved && !ownedTickets.isEmpty() && level.getChunkSource().getChunkNow(center.getX() >> 4, center.getZ() >> 4) != null) {
                    observer.setGameMode(GameType.CREATIVE);
                    observer.getAbilities().flying = true;
                    observer.onUpdateAbilities();
                    double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.getX(), center.getZ()) + 32;
                    observer.teleportTo(level, center.getX() + 28.5, y, center.getZ() + 28.5, Set.of(), 135, 35, true);
                    cameraMoved = true;
                    event("observer_relocated", observer.getUUID(), "Only the observer moved; residents remain at generated positions", null);
                }
                if (server.getTickCount() % 20 == 0) rememberActors();
                if (!readyThisSession) {
                    if (requestedChunks < chunks.size() || !loading.isEmpty()) return;
                    if (allLoadedAt == 0) { allLoadedAt = now; lastChanged = now; }
                    String current = localActors().stream().map(entity -> entity.getUUID().toString()).sorted().reduce("", (a, b) -> a + b);
                    if (!current.equals(fingerprint)) { fingerprint = current; lastChanged = now; }
                    if (now - allLoadedAt < 200 || now - lastChanged < 100) return;
                    if (!marker.has("baseline_file")) {
                        if (localActors().stream().noneMatch(entity -> entity instanceof Villager && entity.isAlive())) {
                            if (now - allLoadedAt < 1200) return;
                            throw new IllegalStateException("Generated village has no living residents after stabilization; replacement spawning is forbidden");
                        }
                        marker.addProperty("stage", "scanning_baseline");
                        if (scannedChunks == 0) marker.addProperty("resource_scan_started_game_time", now);
                        // Resource inventory is also staged: one ready chunk per server tick.
                        if (scannedChunks < chunks.size()) { scan(chunks.get(scannedChunks++)); return; }
                        freezeBaselineAndAdopt();
                    } else if (!stage().equals("active")) {
                        // A shutdown between baseline and adoption can resume only the frozen UUID cohort.
                        adoptFrozenCohort();
                    }
                    readyThisSession = true;
                    audit(resumed ? "resumed" : "after-adoption");
                }
            } catch (RuntimeException | IOException failure) {
                failTrial(failure);
            }
        }

        void releaseTickets() {
            for (ChunkPos pos : ownedTickets) level.getChunkSource().removeTicketWithRadius(TicketType.DRAGON, pos, 2);
            ownedTickets.clear();
            loading.clear();
        }

        void failTrial(Throwable failure) {
            marker.addProperty("stage", "failed");
            marker.addProperty("failure", failure.toString());
            marker.addProperty("failure_game_time", level.getGameTime());
            readyThisSession = false;
            releaseTickets();
            TrainingTrial.stopForEvidence(server, "Natural village trial failed: " + failure);
            persist();
            event("trial_failed", null, failure.toString(), null);
            LOG.error("Natural village trial stopped without replacing its cohort", failure);
        }

        List<LivingEntity> localActors() {
            List<LivingEntity> actors = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if ((entity instanceof Villager || entity instanceof IronGolem) && inside(entity.blockPosition())) actors.add((LivingEntity) entity);
            }
            actors.sort(Comparator.comparing(entity -> entity.getUUID().toString()));
            return actors;
        }

        boolean inside(BlockPos pos) {
            return Math.abs((long) pos.getX() - center.getX()) <= RADIUS && Math.abs((long) pos.getZ() - center.getZ()) <= RADIUS;
        }

        void rememberActors() {
            JsonObject known = marker.getAsJsonObject("known_actors");
            for (LivingEntity actor : localActors()) {
                String id = actor.getUUID().toString();
                if (!known.has(id)) {
                    JsonObject first = actor(actor);
                    first.addProperty("first_seen_game_time", level.getGameTime());
                    first.addProperty("state", "observed_alive");
                    known.add(id, first);
                    event("first_seen", actor.getUUID(), actor.typeHolder().getRegisteredName(), null);
                }
                known.getAsJsonObject(id).addProperty("last_seen_game_time", level.getGameTime());
            }
        }

        void scan(ChunkPos chunkPos) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z());
            if (chunk == null) throw new IllegalStateException("Loaded baseline chunk became unavailable: " + chunkPos);
            var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            chunk.getAllStarts().forEach((structure, start) -> {
                if (!start.isValid()) return;
                String key = registry.getKey(structure) + "@" + start.getChunkPos().x() + "," + start.getChunkPos().z();
                if (structures.has(key)) return;
                JsonObject data = new JsonObject();
                data.addProperty("structure", String.valueOf(registry.getKey(structure)));
                data.addProperty("village", registry.wrapAsHolder(structure).is(StructureTags.VILLAGE));
                data.addProperty("bounding_box", start.getBoundingBox().toString());
                JsonArray pieces = new JsonArray();
                for (var piece : start.getPieces()) {
                    JsonObject record = new JsonObject();
                    record.addProperty("bounding_box", piece.getBoundingBox().toString());
                    if (piece instanceof PoolElementStructurePiece pool) record.addProperty("template", pool.getElement().toString());
                    pieces.add(record);
                }
                data.add("pieces", pieces);
                structures.add(key, data);
            });
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
                int x = chunkPos.x() * 16 + dx, z = chunkPos.z() * 16 + dz;
                if (!inside(new BlockPos(x, center.getY(), z))) continue;
                int top = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz);
                for (int y = Math.max(level.getMinY(), top - 48); y <= Math.min(level.getMaxY(), top + 8); y++) {
                    pos.set(x, y, z);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD) beds.add(position(pos));
                    else if (state.is(BlockTags.WOODEN_DOORS) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) doors.add(position(pos));
                    if (RESOURCE_BLOCKS.contains(state.getBlock()) || state.getBlock() instanceof CropBlock) {
                        String key = String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
                        resources.addProperty(key, resources.has(key) ? resources.get(key).getAsInt() + 1 : 1);
                    }
                }
            }
        }

        void freezeBaselineAndAdopt() throws IOException {
            rememberActors();
            JsonObject cohort = marker.getAsJsonObject("cohort");
            for (LivingEntity entity : localActors()) if (entity instanceof Villager && entity.isAlive()) {
                JsonObject entry = actor(entity);
                entry.addProperty("adopted", false);
                cohort.add(entity.getUUID().toString(), entry);
            }
            if (cohort.isEmpty()) throw new IOException("No living natural villagers remain at baseline; no replacement cohort will be created");
            JsonObject baseline = census("before-adoption");
            JsonObject terrain = new JsonObject();
            terrain.addProperty("scope", "Square radius 96; each column from heightmap minus 48 through plus 8; one chunk per tick, not an instantaneous full-world inventory");
            terrain.addProperty("scan_started_game_time", marker.get("resource_scan_started_game_time").getAsLong());
            terrain.addProperty("scan_finished_game_time", level.getGameTime());
            terrain.addProperty("bed_heads", beds.size());
            terrain.addProperty("wooden_door_lower_halves", doors.size());
            terrain.add("beds", beds); terrain.add("doors", doors);
            terrain.add("resource_blocks", resources);
            terrain.add("generated_structure_starts", structures);
            baseline.add("resources_before_adoption", terrain);
            baseline.addProperty("no_golem_requirement", "Actual naturally generated golem count is recorded; none is manufactured if absent");
            Path file = evidence.resolve("baseline.json");
            if (Files.exists(file)) throw new IOException("Baseline file already exists without matching marker; refusing to overwrite evidence");
            atomicWrite(file, JSON.toJson(baseline));
            marker.addProperty("baseline_file", file.getFileName().toString());
            marker.addProperty("baseline_game_time", level.getGameTime());
            marker.addProperty("stage", "baseline_frozen");
            // Persist the exact original cohort before changing any resident's AI.
            persistBarrier();
            adoptFrozenCohort();
        }

        void adoptFrozenCohort() throws IOException {
            JsonObject cohort = marker.getAsJsonObject("cohort");
            for (var entry : cohort.entrySet()) {
                JsonObject original = entry.getValue().getAsJsonObject();
                if (original.get("adopted").getAsBoolean()) continue;
                Entity entity = level.getEntity(UUID.fromString(entry.getKey()));
                if (entity instanceof Villager villager && villager.isAlive() && inside(villager.blockPosition())) {
                    if (!villager.entityTags().contains(Villagers.MANAGED)) Villagers.adopt(level, villager);
                    original.addProperty("adopted", true);
                    original.addProperty("adopted_game_time", level.getGameTime());
                    event("adopted_original_resident", villager.getUUID(), "UUID, inventory, generated home/job data and position retained", null);
                } else {
                    original.addProperty("adoption_skipped", "Original villager dead, missing, unloaded or outside trial radius; never replaced");
                }
            }
            marker.addProperty("stage", "active");
            persistBarrier();
        }

        JsonObject census(String label) {
            JsonObject result = new JsonObject();
            result.addProperty("at", Instant.now().toString());
            result.addProperty("label", label);
            result.addProperty("world", WORLD);
            result.addProperty("dimension", level.dimension().identifier().toString());
            result.addProperty("source", "Server ground truth for currently loaded actors; missing UUIDs are not presumed dead");
            result.addProperty("seed", level.getSeed());
            result.addProperty("game_time", level.getGameTime());
            result.addProperty("server_tick", server.getTickCount());
            result.addProperty("tick_rate", server.tickRateManager().tickrate());
            result.addProperty("tick_frozen", server.tickRateManager().isFrozen());
            result.addProperty("mean_server_tick_millis", server.getAverageTickTimeNanos() / 1e6);
            result.addProperty("difficulty", level.getDifficulty().name());
            result.addProperty("is_raining", level.isRaining());
            result.addProperty("is_thundering", level.isThundering());
            result.addProperty("night", level.isDarkOutside());
            result.addProperty("observer_mode", observer.gameMode.getGameModeForPlayer().getName());
            result.addProperty("observer_spawning_context", "Creative flight observer permits normal hostile spawning eligibility but retains the normal player-distance exclusion. NPCs receive no invulnerability; no mobs are injected.");
            result.add("center", position(center));
            result.addProperty("radius", RADIUS);
            result.addProperty("requested_chunks", requestedChunks);
            result.addProperty("total_trial_chunks", chunks.size());
            result.addProperty("pending_chunk_requests", loading.size());
            result.addProperty("ready_this_session", readyThisSession);
            result.addProperty("nonpersistent_trial_tickets", ownedTickets.size());
            JsonObject rules = new JsonObject();
            level.getGameRules().availableRules().forEach(rule -> rules.addProperty(rule.getIdentifierWithFallback().toString(), level.getGameRules().getAsString(rule)));
            result.add("game_rules", rules);
            JsonArray villagers = new JsonArray(), golems = new JsonArray();
            for (LivingEntity entity : localActors()) {
                if (entity instanceof Villager) villagers.add(actor(entity));
                else golems.add(actor(entity));
            }
            result.add("local_loaded_villagers", villagers);
            result.add("local_loaded_iron_golems", golems);
            result.addProperty("local_loaded_villager_count", villagers.size());
            result.addProperty("local_loaded_iron_golem_count", golems.size());
            JsonArray originalActors = new JsonArray();
            int alive = 0, missing = 0, lost = 0;
            for (var entry : marker.getAsJsonObject("cohort").entrySet()) {
                UUID id = UUID.fromString(entry.getKey());
                Entity entity = level.getEntityInAnyDimension(id);
                JsonObject record;
                if (entity instanceof LivingEntity living) {
                    record = actor(living);
                    record.addProperty("status", living.isAlive() ? "loaded_alive" : "loaded_dead");
                    if (living.isAlive()) alive++; else lost++;
                } else {
                    record = new JsonObject(); record.addProperty("uuid", id.toString());
                    JsonObject known = marker.getAsJsonObject("known_actors").getAsJsonObject(entry.getKey());
                    boolean terminal = known != null && known.has("terminal_event");
                    record.addProperty("status", terminal ? known.get("terminal_event").getAsString() : "unloaded_or_missing");
                    if (terminal) lost++; else missing++;
                }
                record.add("baseline", entry.getValue().deepCopy());
                originalActors.add(record);
            }
            result.add("original_cohort", originalActors);
            result.addProperty("original_cohort_size", marker.getAsJsonObject("cohort").size());
            result.addProperty("original_cohort_loaded_alive", alive);
            result.addProperty("original_cohort_known_dead_or_converted", lost);
            result.addProperty("original_cohort_unloaded_or_missing", missing);
            result.add("known_actor_lifecycle", marker.getAsJsonObject("known_actors").deepCopy());
            result.addProperty("stage", stage());
            return result;
        }

        JsonObject actor(LivingEntity entity) {
            JsonObject record = new JsonObject();
            record.addProperty("uuid", entity.getUUID().toString());
            record.addProperty("type", entity.typeHolder().getRegisteredName());
            record.addProperty("name", entity.getName().getString());
            record.addProperty("alive", entity.isAlive());
            record.addProperty("health", entity.getHealth());
            record.addProperty("max_health", entity.getMaxHealth());
            record.addProperty("x", entity.getX()); record.addProperty("y", entity.getY()); record.addProperty("z", entity.getZ());
            record.addProperty("dimension", entity.level().dimension().identifier().toString());
            record.addProperty("inside_trial", entity.level() == level && inside(entity.blockPosition()));
            record.addProperty("managed", entity.entityTags().contains(Villagers.MANAGED));
            JsonObject equipment = new JsonObject();
            for (EquipmentSlot slot : EquipmentSlot.VALUES) equipment.add(slot.getSerializedName(), item(entity.getItemBySlot(slot)));
            record.add("equipment", equipment);
            if (entity instanceof Villager villager) {
                record.addProperty("equipment_status", AdventureActions.equipmentStatus(villager));
                record.add("activity", JSON.toJsonTree(AdventureActions.activityStatus(villager.getUUID())));
                record.add("last_harvest",JSON.toJsonTree(AdventureActions.lastHarvest(villager.getUUID())));
                Path diskHome = markerFile.getParent().resolve(entity.getUUID().toString());
                record.addProperty("disk_home", diskHome.toString());
                record.addProperty("mind_file", diskHome.resolve("mind.json").toString());
                record.addProperty("mind_file_exists", Files.isRegularFile(diskHome.resolve("mind.json")));
                record.addProperty("profession", villager.getVillagerData().profession().getRegisteredName());
                record.addProperty("level", villager.getVillagerData().level());
                record.addProperty("baby", villager.isBaby());
                record.addProperty("sleeping", villager.isSleeping());
                record.addProperty("passenger",villager.isPassenger()); record.addProperty("no_ai",villager.isNoAi());
                record.add("survival",JSON.toJsonTree(VillageSurvival.status(villager.getUUID())));
                villager.getBrain().getMemory(MemoryModuleType.HOME).ifPresent(home -> record.add("home", globalPosition(home)));
                villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).ifPresent(job -> record.add("job_site", globalPosition(job)));
                JsonArray inventory = new JsonArray();
                for (int slot = 0; slot < villager.getInventory().getContainerSize(); slot++) inventory.add(item(villager.getInventory().getItem(slot)));
                record.add("inventory", inventory);
            }
            return record;
        }

        void audit(String label) {
            lastAudit = level.getGameTime();
            rememberActors();
            JsonObject record = census(label);
            String text = JSON.toJson(record), line = COMPACT.toJson(record) + "\n";
            long gameTime = level.getGameTime();
            writer.execute(() -> {
                try {
                    atomicWrite(evidence.resolve("latest.json"), text);
                    append(evidence.resolve("census.jsonl"), line);
                    if (!label.equals("periodic")) atomicWrite(evidence.resolve(label + "-" + gameTime + ".json"), text);
                } catch (IOException failure) { writer.fail(failure); }
            });
            persist();
        }

        void lifecycle(Entity entity, String kind, String detail, boolean terminal, UUID replacement) {
            if (!(entity instanceof Villager || entity instanceof IronGolem)) return;
            JsonObject known = marker.getAsJsonObject("known_actors");
            String id = entity.getUUID().toString();
            if (!known.has(id) && !(entity.level() == level && inside(entity.blockPosition()))) return;
            if (!known.has(id) && entity instanceof LivingEntity living) known.add(id, actor(living));
            JsonObject state = known.getAsJsonObject(id);
            state.addProperty("last_event", kind);
            state.addProperty("last_event_game_time", level.getGameTime());
            if (terminal && !state.has("terminal_event")) {
                state.addProperty("terminal_event", kind.equals("unload") ? "death" : kind);
                state.addProperty("terminal_detail", detail);
                state.addProperty("terminal_game_time", level.getGameTime());
            }
            if (replacement != null) state.addProperty("replacement_uuid", replacement.toString());
            event(kind, entity.getUUID(), detail, replacement);
            persist();
        }

        void persist() {
            String snapshot = JSON.toJson(marker);
            writer.execute(() -> {
                try { atomicWrite(markerFile, snapshot); }
                catch (IOException failure) { writer.fail(failure); }
            });
        }

        void persistBarrier() throws IOException {
            // Use the same serial writer as lifecycle snapshots so an older queued
            // marker cannot overwrite the frozen baseline or completed adoption.
            String snapshot = JSON.toJson(marker);
            try {
                writer.submit(() -> {
                    try { atomicWrite(markerFile, snapshot); }
                    catch (IOException failure) { throw new java.io.UncheckedIOException(failure); }
                }).get(15, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while committing original village cohort", interrupted);
            } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException | RejectedExecutionException failure) {
                throw new IOException("Could not commit original village cohort before adoption", failure);
            }
        }

        void event(String kind, UUID id, String detail, UUID replacement) {
            JsonObject event = new JsonObject();
            event.addProperty("at", Instant.now().toString());
            event.addProperty("game_time", level.getGameTime());
            event.addProperty("event", kind);
            if (id != null) event.addProperty("uuid", id.toString());
            event.addProperty("detail", detail);
            if (replacement != null) event.addProperty("replacement_uuid", replacement.toString());
            String text = COMPACT.toJson(event) + "\n";
            writer.execute(() -> {
                try { append(evidence.resolve("lifecycle.jsonl"), text); }
                catch (IOException failure) { writer.fail(failure); }
            });
        }

        void close() {
            try { audit("shutdown"); }
            catch (RuntimeException failure) { writer.fail(failure); }
            finally {
                try { releaseTickets(); }
                finally { writer.shutdown(); }
            }
            try {
                if (!writer.awaitTermination(15, TimeUnit.SECONDS)) LOG.error("Village evidence writes still pending at shutdown");
                else {
                    // Older queued snapshots have drained; commit terminal failure even after queue rejection.
                    if (writer.failed() && !stage().equals("failed")) {
                        marker.addProperty("stage", "failed");
                        marker.addProperty("failure", "Evidence writer failed; output is incomplete");
                    }
                    try { atomicWrite(markerFile, JSON.toJson(marker)); }
                    catch (IOException failure) { LOG.error("Could not persist final village marker", failure); }
                }
            } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        }
    }

    private static JsonObject position(BlockPos pos) {
        JsonObject record = new JsonObject();
        record.addProperty("x", pos.getX()); record.addProperty("y", pos.getY()); record.addProperty("z", pos.getZ());
        return record;
    }

    private static JsonObject globalPosition(GlobalPos pos) {
        JsonObject record = position(pos.pos());
        record.addProperty("dimension", pos.dimension().identifier().toString());
        return record;
    }

    private static JsonObject item(ItemStack stack) {
        JsonObject record = new JsonObject();
        record.addProperty("item", String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem())));
        record.addProperty("count", stack.getCount());
        return record;
    }

    private static void atomicWrite(Path target, String text) throws IOException {
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), "village-", ".tmp");
        try {
            Files.writeString(temporary, text);
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    private static void append(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
