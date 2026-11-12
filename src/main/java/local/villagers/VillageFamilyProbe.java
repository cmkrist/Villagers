package local.villagers;

import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import local.villagers.family.BreedingPolicy;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;

/** Labelled play-only supplies and nursery. Never calls family consideration or changes actor decisions. */
final class VillageFamilyProbe {
    private static final class Fixture {
        final List<UUID> parents = new ArrayList<>();
        Map<String, Object> previous;
        long deadline;
        long lastBirthDeadline;
        BlockPos bed;
    }
    private static final Map<MinecraftServer, Fixture> FIXTURES = new IdentityHashMap<>();
    private VillageFamilyProbe() {}

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("familyprobe")
                        .then(Commands.literal("status").executes(c -> execute(c.getSource(), "status", null, 0)))
                        .then(Commands.literal("nursery").executes(c -> execute(c.getSource(), "nursery", null, 0)))
                        .then(Commands.literal("feed")
                                .then(Commands.argument("uuid", StringArgumentType.word())
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 12))
                                                .executes(c -> execute(c.getSource(), "feed",
                                                        UUID.fromString(StringArgumentType.getString(c, "uuid")),
                                                        IntegerArgumentType.getInteger(c, "count"))))))));
        var watch = Commands.literal("watch").then(Commands.argument("first", StringArgumentType.word())
                .then(Commands.argument("second", StringArgumentType.word())
                        .then(Commands.argument("x", IntegerArgumentType.integer(-30_000_000, 30_000_000))
                                .then(Commands.argument("y", IntegerArgumentType.integer(-2048, 2048))
                                        .then(Commands.argument("z", IntegerArgumentType.integer(-30_000_000, 30_000_000))
                                                .executes(c -> watch(c.getSource(), StringArgumentType.getString(c, "first"),
                                                        StringArgumentType.getString(c, "second"),
                                                        new BlockPos(IntegerArgumentType.getInteger(c, "x"),
                                                                IntegerArgumentType.getInteger(c, "y"), IntegerArgumentType.getInteger(c, "z")))))))));
        dispatcher.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("familyprobe").then(watch)));
        ServerLifecycleEvents.SERVER_STOPPING.register(FIXTURES::remove);
        ServerTickEvents.END_SERVER_TICK.register(VillageFamilyProbe::observe);
    }

    /** Observe explicit identities before their chunks finish loading; creates no supplies, actors or decisions. */
    private static int watch(CommandSourceStack source, String first, String second, BlockPos bed) {
        var server = source.getServer();
        if (!allowed(server)) { source.sendFailure(Component.literal("Family observation requires the separate play copy.")); return 0; }
        try {
            UUID a = UUID.fromString(first), b = UUID.fromString(second);
            if (a.equals(b)) throw new IllegalArgumentException("Choose two distinct existing adult UUIDs");
            Fixture fixture = FIXTURES.computeIfAbsent(server, unused -> new Fixture());
            if (!fixture.parents.isEmpty() && !new java.util.HashSet<>(fixture.parents).equals(java.util.Set.of(a, b)))
                throw new IllegalStateException("This session already selected a different pair");
            fixture.parents.clear(); fixture.parents.add(a); fixture.parents.add(b);
            fixture.bed = bed;
            fixture.deadline = source.getLevel().getGameTime() + 12_000;
            write(source.getLevel(), "watch-only", report(source.getLevel(), fixture));
            source.sendSuccess(() -> Component.literal("Read-only family observation armed for " + a + " and " + b
                    + "; bed=" + bed.toShortString() + "; no supplies, movement or birth action."), false);
            return 1;
        } catch (Exception failure) {
            source.sendFailure(Component.literal("Family watch: " + failure.getMessage())); return 0;
        }
    }

    private static boolean allowed(MinecraftServer server) {
        return DevMode.inWorld(server, DevMode.TRIAL_WORLD) && "play".equals(System.getProperty("villagers.role"));
    }

    private static int execute(CommandSourceStack source, String mode, UUID target, int count) {
        var server = source.getServer();
        if (!allowed(server)) { source.sendFailure(Component.literal("Family fixtures require the separate play copy.")); return 0; }
        var level = source.getLevel();
        Fixture fixture = FIXTURES.computeIfAbsent(server, unused -> new Fixture());
        try {
            if (mode.equals("feed")) {
                var observer = source.getPlayerOrException();
                if (!(level.getEntity(target) instanceof Villager actor) || !actor.isAlive() || actor.isBaby()
                        || !Villagers.isManaged(actor)) throw new IllegalStateException("Choose an existing loaded managed adult");
                if (!fixture.parents.contains(target) && fixture.parents.size() >= 2)
                    throw new IllegalStateException("This session already selected two supplied adults");
                if (!observer.getMainHandItem().isEmpty() || observer.distanceToSqr(actor) > 9 || !observer.hasLineOfSight(actor))
                    throw new IllegalStateException("Empty hand, sight and native three-block gift reach required");
                if (!actor.getInventory().canAddItem(new ItemStack(Items.BREAD, count)))
                    throw new IllegalStateException("Recipient cannot hold fixture bread");
                write(level, "before-food", report(level, fixture));
                VillageTrial.noteIntervention(level, "familyprobe: operator supplies " + count + " real bread to existing adult "
                        + target + " through ordinary handoff; no actor movement, age, health, mind, mood or action setter.");
                if (!fixture.parents.contains(target)) fixture.parents.add(target);
                observer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD, count));
                boolean crouching = observer.isShiftKeyDown();
                int given = 0;
                observer.setShiftKeyDown(true);
                try { for (int i = 0; i < count; i++) if (Villagers.offerGift(level, actor, observer, InteractionHand.MAIN_HAND)) given++; }
                finally { observer.setShiftKeyDown(crouching); }
                fixture.deadline = level.getGameTime() + 12_000;
                if (given != count) throw new IllegalStateException("Only " + given + "/" + count + " actual bread transferred; partial intervention retained");
            } else if (mode.equals("nursery")) {
                if (fixture.parents.size() != 2) throw new IllegalStateException("Supply two existing adults before the nursery fixture");
                if (fixture.bed != null) throw new IllegalStateException("One nursery fixture per session; existing nursery retained");
                BlockPos origin = source.getPlayerOrException().blockPosition();
                BlockPos site = null;
                search: for (BlockPos p : BlockPos.betweenClosed(origin.offset(-12, -3, -12), origin.offset(12, 2, 12))) {
                    BlockPos bed = p.offset(2, 0, 1);
                    for (UUID parent : fixture.parents) {
                        if (!(level.getEntity(parent) instanceof Villager actor) || actor.blockPosition().distSqr(bed) > 225) continue search;
                    }
                    if (clearSite(level, p)) { site = p.immutable(); break; }
                }
                if (site == null) throw new IllegalStateException("No clear grounded 5x5 nursery site within fifteen blocks of both adults");
                VillageTrial.noteIntervention(level, "familyprobe: one supplied oak shelter, door and white bed at " + site.toShortString()
                        + "; verified air/solid ground, no terrain removal, actor movement, POI reservation or forced birth.");
                buildNursery(level, site);
                fixture.bed = site.offset(2, 0, 1);
            }
            Map<String, Object> result = report(level, fixture);
            write(level, mode, result);
            source.sendSuccess(() -> Component.literal("Family probe " + mode + " recorded; adults=" + fixture.parents
                    + "; nursery=" + fixture.bed + ". See family-evidence."), false);
            return 1;
        } catch (Exception failure) {
            source.sendFailure(Component.literal("Family probe: " + failure.getMessage()));
            return 0;
        }
    }

    private static boolean clearSite(ServerLevel level, BlockPos p) {
        for (BlockPos ground : BlockPos.betweenClosed(p.offset(0, -1, 0), p.offset(4, -1, 4)))
            if (!level.isLoaded(ground) || !level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)
                    || level.getBlockState(ground).is(Blocks.FARMLAND)) return false;
        for (BlockPos air : BlockPos.betweenClosed(p, p.offset(4, 3, 4)))
            if (!level.isLoaded(air) || !level.getBlockState(air).isAir()) return false;
        return level.getEntities(null, new AABB(p).expandTowards(4, 3, 4).inflate(.3)).isEmpty();
    }

    private static void buildNursery(ServerLevel level, BlockPos p) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
            level.setBlockAndUpdate(p.offset(x, 3, z), Blocks.OAK_PLANKS.defaultBlockState());
            if (x == 0 || x == 4 || z == 0 || z == 4) for (int y = 0; y < 3; y++) {
                if (x == 2 && z == 4 && y < 2) continue;
                level.setBlockAndUpdate(p.offset(x, y, z), Blocks.OAK_PLANKS.defaultBlockState());
            }
        }
        var door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
        level.setBlock(p.offset(2, 0, 4), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 2);
        level.setBlockAndUpdate(p.offset(2, 1, 4), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        var bed = Blocks.BED.white().defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
        level.setBlock(p.offset(2, 0, 2), bed.setValue(BedBlock.PART, BedPart.FOOT), 2);
        level.setBlockAndUpdate(p.offset(2, 0, 1), bed.setValue(BedBlock.PART, BedPart.HEAD));
    }

    private static Map<String, Object> report(ServerLevel level, Fixture fixture) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kind", "play-only family observation; any food/shelter interventions recorded separately; controller chooses actions and births");
        result.put("gameTime", level.getGameTime()); result.put("selectedAdults", fixture.parents);
        result.put("night", level.isDarkOutside());
        result.put("clockTime",level.getOverworldClockTime());result.put("skyDarken",level.getSkyDarken());
        result.put("raining",level.isRaining());result.put("thundering",level.isThundering());
        List<Map<String, Object>> actors = new ArrayList<>();
        for (var entity : level.getAllEntities()) if (entity instanceof Villager v && Villagers.isManaged(v)) {
            Session s = Villagers.existingSession(v.getUUID());
            if (s == null) continue;
            Map<String, Object> actor = nativeState(v, s);
            actor.put("mind", s.mind.snapshot()); actor.put("familyStatus", s.familyStatus);
            actor.put("nativeHome", v.getBrain().getMemory(MemoryModuleType.HOME).map(Object::toString).orElse("none"));
            actor.put("spareHomePoisWithin16", level.getPoiManager().findAll(t -> t.is(PoiTypes.HOME), level::isLoaded,
                    v.blockPosition(), 16, PoiManager.Occupancy.HAS_SPACE).map(BlockPos::toShortString).limit(8).toList());
            actors.add(actor);
        }
        result.put("actors", actors);
        // Include already-loaded creature minds in the same read-only report
        // and guarded-exit snapshot. Their session receipts disappear after
        // shutdown, so saved body audits alone cannot establish paid gifts or
        // family attempts during the actual client run.
        var creatures=new ArrayList<Map<String,Object>>();
        for(var entity:level.getAllEntities())if(entity instanceof net.minecraft.world.entity.Mob mob
            &&CreatureAgents.managed(mob)&&CreatureAgents.loadedState(mob)!=null)creatures.add(CreatureAgents.status(mob));
        result.put("creatures",creatures);
        if (fixture.bed != null) {
            result.put("fixtureBed", fixture.bed.toShortString());
            boolean loaded = level.isLoaded(fixture.bed);
            result.put("fixtureBedState", loaded ? level.getBlockState(fixture.bed).toString() : "unloaded");
            result.put("fixtureBedPoiAvailable", loaded && level.getPoiManager().findAll(t -> t.is(PoiTypes.HOME), level::isLoaded,
                    fixture.bed, 1, PoiManager.Occupancy.HAS_SPACE).anyMatch(fixture.bed::equals));
        }
        return result;
    }

    /** Immutable native values for the bounded observer's existing evidence writer; no operation is triggered. */
    static Map<String, Object> budgetSnapshot(MinecraftServer server) {
        return allowed(server) ? report(server.overworld(), FIXTURES.getOrDefault(server, new Fixture()))
                : Map.of("unavailable", "not the play trial");
    }

    private static Map<String, Object> nativeState(Villager v, Session s) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("uuid", v.getUUID()); result.put("position", List.of(v.getX(), v.getY(), v.getZ()));
        result.put("health", v.getHealth()); result.put("nativeAge", v.getAge()); result.put("baby", v.isBaby());
        result.put("sleeping", v.isSleeping()); result.put("food", s.mind.foodLevel()); result.put("fatigue", s.mind.fatigue());
        result.put("mood", s.mind.mood().band().name()); result.put("nextBreedingAt", s.mind.nextBreedingAt());
        result.put("courtship", s.courtship == null ? "none" : s.courtship);
        result.put("courtshipActive", s.courtship != null && s.courtship.active(v.level().getGameTime()));
        result.put("nextFamilySignal", s.nextFamilySignal); result.put("nextFamilyReply", s.nextFamilyReply);
        result.put("familyStatus", s.familyStatus); result.put("familyFoodGoal", s.familyFoodGoal);
        result.put("selfFoodGoal", s.selfFoodGoal);
        result.put("selfFoodDeficit", FoodSupply.deficit(v.getInventory(), AdventureActions.canHarvest(v)));
        result.put("stockWheatTarget", s.stockWheatTarget); result.put("foodStatus", s.foodStatus);
        result.put("plotsCultivatedThisLoad", s.plotsCultivated);
        result.put("expansionTarget", s.expansionTarget == null ? "none" : s.expansionTarget.toShortString());
        result.put("farmJourneyActive", s.farmJourney.active());
        result.put("farmRecall", Map.of("target",s.rememberedFarmTarget == null ? "none" : s.rememberedFarmTarget.toShortString(),
                "trips",s.farmRecallTrips,"arrivals",s.farmRecallArrivals,"rejected",s.farmRecallRejected,
                "status",s.farmRecallStatus,"knownFarms",s.mind.observedFarms(),"lastPath",s.lastFarmPath));
        result.put("companyTarget", s.familyCompany == null ? "none" : s.familyCompany.toString());
        result.put("companyJourneyActive", s.companyJourney.active());
        result.put("companyStatus", s.companyStatus);
        result.put("companyScans", s.companyScans);
        result.put("companyPathRejections", s.companyPathRejections);
        result.put("nextCompanyScan", s.nextCompanyScan);
        result.put("cropTarget",s.cropTarget==null ? "none" : s.cropTarget.toShortString());
        result.put("edibleCropTarget",s.rootCropTarget==null ? "none" : s.rootCropTarget.toShortString());
        result.put("nativeJobSite",v.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.JOB_SITE).map(Object::toString).orElse("none"));
        result.put("navigationDone", v.getNavigation().isDone());
        result.put("navigationTarget", v.getNavigation().getPath() == null ? "none"
                : v.getNavigation().getPath().getTarget().toShortString());
        result.put("craftingStatus", s.craftingStatus); result.put("breadCraftedThisLoad", s.breadCrafted);
        result.put("toolStatus", s.toolStatus); result.put("woodRecipesThisLoad", s.woodRecipesCrafted);
        result.put("hoesCraftedThisLoad", s.hoesCrafted);
        result.put("stationStatus", s.stationStatus); result.put("stationRecipesThisLoad", s.stationRecipesCrafted);
        result.put("tablesBuiltThisLoad", s.tablesBuilt);
        result.put("villageBedding",VillageBedding.status(s));
        result.put("restStatus", s.restStatus); result.put("groundRestsThisLoad", s.groundRests);
        result.put("bedRestsThisLoad", s.bedRests); result.put("restPathRejections", s.restPathRejections);
        result.put("survival", VillageSurvival.status(v.getUUID()));
        result.put("forestryStatus",s.forestryStatus);result.put("logsGatheredThisLoad",s.logsGathered);
        result.put("propagulesGatheredThisLoad",s.propagulesGathered);result.put("propagulesPlantedThisLoad",s.propagulesPlanted);
        result.put("leavesClearedThisLoad",s.leavesCleared);
        result.put("rootsClearedThisLoad",s.rootsCleared);
        result.put("foodHelp",Map.of("status",s.foodHelpStatus,"target",s.foodHelpPeer==null ? "none" : s.foodHelpPeer.toString(),
                "journeyActive",s.foodHelpJourney.active(),"scans",s.foodHelpScans,"requests",s.foodHelpRequests,"pathRejections",s.foodHelpPathRejections,
                "given",s.foodSharedGiven,"received",s.foodSharedReceived,
                "lastGiven",s.lastFoodGiven,"lastReceived",s.lastFoodReceived));
        result.put("forestryRoute",Map.of("batches",s.forestryPathBatches,"rejections",s.forestryPathRejections,
                "standCandidates",s.forestryStandCandidates,"cursor",s.forestryPathCursor,"lastPath",s.forestryLastPath));
        result.put("adventureActivity", AdventureActions.activityStatus(v.getUUID()));
        result.put("quarry",AdventureQuarry.status(s));
        result.put("mining",AdventureMining.status(s));
        result.put("woodPreparation",WoodBootstrap.status(s));
        result.put("inventoryRoom",Map.of("actions",s.inventoryRoomActions,"lastReceipt",s.inventoryRoomReceipt));
        result.put("stonePickaxePrepared",AdventurePreparation.stonePickaxe(v));
        result.put("ironArmor",AdventureArmor.status(v,s));result.put("ironPreparation",AdventureIron.status(s));result.put("ironIngots",v.getInventory().countItem(Items.IRON_INGOT));
        result.put("villageSupplies",AdventureSupplies.status(s));result.put("ironPickaxePrepared",AdventurePreparation.ironPickaxe(v));
        result.put("nativeActivity",v.level().environmentAttributes().getValue(v.isBaby()
                ? net.minecraft.world.attribute.EnvironmentAttributes.BABY_VILLAGER_ACTIVITY
                : net.minecraft.world.attribute.EnvironmentAttributes.VILLAGER_ACTIVITY,v.position()).toString());
        result.put("lastHarvest",AdventureActions.lastHarvest(v.getUUID()));
        result.put("soilWork",Map.of("status",s.soilStatus,"job",s.soilJob==null?"none":s.soilJob.toString(),
                "tripActive",s.soilTrip,"gathered",s.soilGathered,"delivered",s.soilDelivered,
                "pathQueries",s.soilPathQueries,"pathRejections",s.soilPathRejections,"lastReceipt",s.lastSoilReceipt,"lastPath",s.lastSoilPath));
        result.put("action", s.action == null ? "none" : s.action.name());
        result.put("electiveLearningCredit", s.before != null);
        result.put("inventory", v.getInventory().getItems().stream().map(ItemStack::toString).toList());
        result.put("hasActualBreedingFood", VillageFamilies.hasFamilyFood(v));
        return result;
    }

    /** Only two supplied actors are sampled; no family operation is triggered by this watcher. */
    private static void observe(MinecraftServer server) {
        Fixture fixture = FIXTURES.get(server);
        if (fixture == null || fixture.parents.size() != 2 || !allowed(server)) return;
        ServerLevel level = server.overworld();
        if (level.getGameTime() > fixture.deadline) return;
        Map<String, Object> current = new LinkedHashMap<>();
        long birthDeadline = 0;
        for (UUID id : fixture.parents) if (level.getEntity(id) instanceof Villager v) {
            Session s = Villagers.existingSession(id);
            if (s == null) continue;
            current.put(id.toString(), nativeState(v, s));
            birthDeadline = Math.max(birthDeadline, s.mind.nextBreedingAt());
        }
        boolean departure = fixture.previous != null && fixture.previous.keySet().stream().anyMatch(id -> !current.containsKey(id));
        if (fixture.previous != null && (birthDeadline > fixture.lastBirthDeadline || departure)) {
            Map<String, Object> event = report(level, fixture);
            event.put("previousTickSelectedNativeState", fixture.previous);
            event.put("currentTickSelectedNativeState", current);
            // A disappearance may be unloading or death. The separate lifecycle event establishes which.
            try { write(level, departure ? "observed-departure" : "birth-transition", event); }
            catch (Exception failure) { Villagers.LOG.error("Family fixture transition evidence failed", failure); }
        }
        fixture.previous = current;
        fixture.lastBirthDeadline = birthDeadline;
    }

    private static void write(ServerLevel level, String label, Map<String, Object> report) throws java.io.IOException {
        var directory = level.getServer().getWorldPath(LevelResource.ROOT).resolve("villagers/family-evidence");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(label + "-" + level.getGameTime() + "-" + System.nanoTime() + ".json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }
}
