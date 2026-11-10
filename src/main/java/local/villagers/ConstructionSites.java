package local.villagers;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import local.villagers.construction.WorkSites;
import local.villagers.construction.WorkSiteStore;
import local.villagers.construction.StepJob;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;

/** World-scoped explicit farm permissions and protected structures. No automatic ownership guesses. */
public final class ConstructionSites {
    private static WorkSiteStore store;
    private static MinecraftServer server;
    private static final Map<ServerLevel, Long2ObjectOpenHashMap<WorkSites.Point>> TRACKED = new IdentityHashMap<>();
    private static final Map<ServerLevel, Long2ObjectOpenHashMap<WorkSites.Point>> STATIONS = new IdentityHashMap<>();
    private static final Map<ServerLevel, Long2ObjectOpenHashMap<UUID>> RESOURCES = new IdentityHashMap<>();
    private static final Map<ServerLevel, Long2ObjectOpenHashMap<UUID>> SOILS = new IdentityHashMap<>();
    private static final Map<ServerLevel, Long2ObjectOpenHashMap<UUID>> STEPS = new IdentityHashMap<>();
    private static final Map<ServerLevel, java.util.List<WorkSites.Protection>> PROTECTED = new IdentityHashMap<>();
    private record OwnEdit(ServerLevel level, Set<BlockPos> blocks) {}
    private static final ThreadLocal<OwnEdit> OWN = new ThreadLocal<>();
    private ConstructionSites() {}

    static void start(MinecraftServer running) {
        server = running;
        store = new WorkSiteStore(running.getWorldPath(LevelResource.ROOT).resolve("villagers/work-sites.json"));
        if (store.problem() != null) Villagers.LOG.warn("Construction sites: {}", store.problem());
        rebuild();
    }
    static void stop() { TRACKED.clear(); STATIONS.clear(); RESOURCES.clear(); SOILS.clear(); STEPS.clear(); SoilWork.clear(); PROTECTED.clear(); if (store != null) store.close(); store = null; server = null; OWN.remove(); }
    private static WorkSites.Point point(ServerLevel level, BlockPos p) {
        return new WorkSites.Point(level.dimension().identifier().toString(), p.getX(), p.getY(), p.getZ());
    }
    private static void rebuild() {
        TRACKED.clear();
        STATIONS.clear();
        RESOURCES.clear();
        SOILS.clear();
        STEPS.clear();
        PROTECTED.clear();
        if (store == null || !store.healthy() || server == null) { SoilWork.clear();return; }
        SoilWork.retainClaims(store.snapshot().soilJobs().stream().map(WorkSites.Soil::id).collect(java.util.stream.Collectors.toSet()));
        for (ServerLevel level : server.getAllLevels()) {
            var index = new Long2ObjectOpenHashMap<WorkSites.Point>();
            for (var farm : store.snapshot().farms()) for (var cell : farm.cells())
                if (cell.dimension().equals(level.dimension().identifier().toString()))
                    for (int y = 0; y <= 2; y++) index.put(BlockPos.asLong(cell.x(), cell.y() + y, cell.z()), cell);
            if (!index.isEmpty()) TRACKED.put(level, index);
            var stations = new Long2ObjectOpenHashMap<WorkSites.Point>();
            for (var station : store.snapshot().stations()) if (station.target().dimension().equals(level.dimension().identifier().toString()))
                for (var p : station.footprint()) stations.put(BlockPos.asLong(p.x(), p.y(), p.z()), station.target());
            if (!stations.isEmpty()) STATIONS.put(level, stations);
            var resources = new Long2ObjectOpenHashMap<UUID>();
            for (var job : store.snapshot().forestry()) if (job.seed().dimension().equals(level.dimension().identifier().toString()))
                for (var p : job.footprint()) resources.put(BlockPos.asLong(p.x(),p.y(),p.z()),job.id());
            if (!resources.isEmpty()) RESOURCES.put(level,resources);
            var soils=new Long2ObjectOpenHashMap<UUID>();
            for(var job:store.snapshot().soilJobs()) if(job.source().dimension().equals(level.dimension().identifier().toString()))
                for(var p:job.footprint())soils.put(BlockPos.asLong(p.x(),p.y(),p.z()),job.id());
            if(!soils.isEmpty())SOILS.put(level,soils);
            var steps=new Long2ObjectOpenHashMap<UUID>();
            for(var job:store.snapshot().stepJobs())if(job.blocks().getFirst().dimension().equals(level.dimension().identifier().toString()))
                for(var p:job.footprint())steps.put(BlockPos.asLong(p.x(),p.y(),p.z()),job.id());
            if(!steps.isEmpty())STEPS.put(level,steps);
            var protectedAreas = store.snapshot().protections().stream()
                    .filter(p -> p.min().dimension().equals(level.dimension().identifier().toString())).toList();
            if (!protectedAreas.isEmpty()) PROTECTED.put(level, protectedAreas);
        }
    }
    private static boolean update(WorkSites next) {
        boolean okay = store != null && store.update(next); rebuild();
        if (!okay && store != null) Villagers.LOG.error("Construction suspended: {}", store.problem());
        return okay;
    }
    static boolean allowsFarm(ServerLevel level, BlockPos soil) {
        var index = TRACKED.get(level);
        var cell = index == null ? null : index.get(soil.asLong());
        return cell != null && cell.y() == soil.getY() && store.healthy() && !store.snapshot().protectedFootprint(cell);
    }
    static java.util.List<BlockPos> candidates(ServerLevel level, BlockPos origin) {
        if (store == null || !store.healthy() || !TRACKED.containsKey(level)) return java.util.List.of();
        String dimension = level.dimension().identifier().toString();
        return store.snapshot().farms().stream().flatMap(f -> f.cells().stream())
                .filter(p -> p.dimension().equals(dimension) && Math.abs(p.x() - origin.getX()) <= 8
                        && Math.abs(p.y() - origin.getY()) <= 3 && Math.abs(p.z() - origin.getZ()) <= 8)
                .limit(81).filter(p -> !store.snapshot().protectedFootprint(p))
                .map(p -> new BlockPos(p.x(), p.y(), p.z())).toList();
    }
    static boolean isProtected(ServerLevel level, BlockPos pos) {
        if (store == null) return false;
        if (!store.healthy()) return true;
        var areas = PROTECTED.get(level);
        if (areas == null) return false;
        for (var p : areas) if (pos.getX() >= p.min().x() && pos.getX() <= p.max().x()
                && pos.getY() >= p.min().y() && pos.getY() <= p.max().y()
                && pos.getZ() >= p.min().z() && pos.getZ() <= p.max().z()) return true;
        return false;
    }
    static boolean allowsStation(ServerLevel level, BlockPos target) {
        var index = STATIONS.get(level); var p = index == null ? null : index.get(target.asLong());
        return p != null && p.x() == target.getX() && p.y() == target.getY() && p.z() == target.getZ()
                && store.healthy() && store.snapshot().allowsStation(p);
    }
    static java.util.List<BlockPos> stationCandidates(ServerLevel level, BlockPos origin) {
        if (store == null || !store.healthy() || !STATIONS.containsKey(level)) return java.util.List.of();
        return store.snapshot().stations().stream().map(WorkSites.Station::target)
                // These are explicit assigned jobs, not discovery of unseen natural resources. The caller enforces home range.
                .filter(p -> p.dimension().equals(level.dimension().identifier().toString()) && Math.abs(p.x() - origin.getX()) <= 32
                        && Math.abs(p.y() - origin.getY()) <= 8 && Math.abs(p.z() - origin.getZ()) <= 32)
                .filter(p -> store.snapshot().allowsStation(p)).map(p -> new BlockPos(p.x(), p.y(), p.z())).toList();
    }
    /** Native Level.setBlock entry: an outside edit retires the affected finite ground grant before the edit. */
    public static void beforeBlockWrite(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel actual) || TRACKED.isEmpty() && STATIONS.isEmpty() && RESOURCES.isEmpty() && SOILS.isEmpty() && STEPS.isEmpty()) return;
        var index = TRACKED.get(actual);
        var stations = STATIONS.get(actual);
        var resources = RESOURCES.get(actual);
        var soils = SOILS.get(actual);
        var steps = STEPS.get(actual);
        if ((index == null || !index.containsKey(pos.asLong())) && (stations == null || !stations.containsKey(pos.asLong()))
                && (resources == null || !resources.containsKey(pos.asLong())) && (soils == null || !soils.containsKey(pos.asLong()))
                && (steps == null || !steps.containsKey(pos.asLong()))) return;
        OwnEdit own = OWN.get();
        if (own != null && own.level() == actual && own.blocks().contains(pos)) return;
        if (resources != null && resources.containsKey(pos.asLong()))
            Villagers.LOG.info("Retiring forestry {} before outside write at {} (current {})", resources.get(pos.asLong()), pos.toShortString(), actual.getBlockState(pos));
        update(store.snapshot().touch(point(actual, pos)));
    }
    /** Only called around the native LeavesBlock.tick refresh. No changed states or other cells are exempt. */
    public static boolean nativeLeafRefresh(ServerLevel level, BlockPos pos,
            net.minecraft.world.level.block.state.BlockState next, Supplier<Boolean> write) {
        var job=forestryAt(level,pos);
        if (job==null || OWN.get()!=null || !unchangedApprovedLeaf(job,point(level,pos),level.getBlockState(pos),next)) return write.get();
        return ownPlacement(level,pos,write);
    }
    static boolean unchangedApprovedLeaf(WorkSites.Forestry job,WorkSites.Point p,
            net.minecraft.world.level.block.state.BlockState before,net.minecraft.world.level.block.state.BlockState after) {
        return job!=null && job.uncleared().contains(p) && before.is(Blocks.MANGROVE_LEAVES) && before.equals(after);
    }
    static <T> T ownEdit(ServerLevel level, BlockPos soil, Supplier<T> action) {
        return edit(level, Set.of(soil.immutable(), soil.above()), action);
    }
    static <T> T ownPlacement(ServerLevel level, BlockPos target, Supplier<T> action) {
        return edit(level, Set.of(target.immutable()), action);
    }
    private static <T> T edit(ServerLevel level, Set<BlockPos> blocks, Supplier<T> action) {
        if (OWN.get() != null) throw new IllegalStateException("Nested construction transaction");
        OWN.set(new OwnEdit(level, blocks));
        try { return action.get(); } finally { OWN.remove(); }
    }
    static void completed(ServerLevel level, BlockPos soil) { update(store.snapshot().touch(point(level, soil))); }
    static java.util.List<WorkSites.Forestry> forestry(ServerLevel level) {
        if (store == null || !store.healthy() || !RESOURCES.containsKey(level)) return java.util.List.of();
        return store.snapshot().forestry().stream().filter(f -> f.seed().dimension().equals(level.dimension().identifier().toString())
                && store.snapshot().allowsForestry(f.id())).toList();
    }
    static WorkSites.Forestry forestry(ServerLevel level, UUID id) {
        return forestry(level).stream().filter(f -> f.id().equals(id)).findFirst().orElse(null);
    }
    static WorkSites.Forestry forestryAt(ServerLevel level,BlockPos p) {
        var index=RESOURCES.get(level);var id=index==null ? null : index.get(p.asLong());
        return id==null ? null : forestry(level,id);
    }
    static boolean progress(WorkSites.Forestry job) { return update(store.snapshot().progress(job)); }
    static boolean finishForestry(UUID id) { return update(store.snapshot().remove(id)); }
    static BlockPos position(WorkSites.Point p) { return new BlockPos(p.x(),p.y(),p.z()); }

    static java.util.List<WorkSites.Soil> soilJobs(ServerLevel level) {
        if(store==null || !store.healthy() || !SOILS.containsKey(level))return java.util.List.of();
        return store.snapshot().soilJobs().stream().filter(j -> j.source().dimension().equals(level.dimension().identifier().toString())
                && store.snapshot().allowsSoil(j.id())).toList();
    }
    static WorkSites.Soil soilJob(ServerLevel level,UUID id) { return soilJobs(level).stream().filter(j -> j.id().equals(id)).findFirst().orElse(null); }
    static boolean soilGathered(WorkSites.Soil job) { return update(store.snapshot().progress(job.markGathered())); }
    static boolean soilDelivered(WorkSites.Soil job) { return update(store.snapshot().soilReadyForPlanting(job.id())); }
    static void retireSoil(UUID id) { if(store!=null && store.healthy())update(store.snapshot().remove(id)); }
    static java.util.List<StepJob> stepJobs(ServerLevel level) {
        if(store==null || !store.healthy() || !STEPS.containsKey(level))return java.util.List.of();
        return store.snapshot().stepJobs().stream().filter(j -> j.blocks().getFirst().dimension().equals(level.dimension().identifier().toString())
                && store.snapshot().allowsSteps(j.id())).toList();
    }
    static StepJob stepJob(ServerLevel level,UUID id) { return stepJobs(level).stream().filter(j -> j.id().equals(id)).findFirst().orElse(null); }
    static boolean stepProgress(StepJob job) { return update(store.snapshot().progress(job)); }
    static void retireSteps(UUID id) { if(store!=null && store.healthy())update(store.snapshot().remove(id)); }

    static void register(CommandDispatcher<CommandSourceStack> d) {
        var site = Commands.literal("site")
                .then(Commands.literal("table").executes(c -> station(c.getSource())))
                .then(Commands.literal("farm").then(Commands.argument("radius", IntegerArgumentType.integer(0, 4))
                        .executes(c -> farm(c.getSource(), IntegerArgumentType.getInteger(c, "radius")))))
                .then(Commands.literal("list").executes(c -> list(c.getSource())))
                .then(Commands.literal("remove").then(Commands.argument("id", StringArgumentType.word())
                        .executes(c -> remove(c.getSource(), UUID.fromString(StringArgumentType.getString(c, "id"))))));
        String[] soilNames={"sx","sy","sz","tx","ty","tz"};
        var soilLast=Commands.argument("tz",IntegerArgumentType.integer(-30_000_000,30_000_000)).executes(c -> {
            int[] v=new int[6];for(int i=0;i<6;i++)v[i]=IntegerArgumentType.getInteger(c,soilNames[i]);
            return soil(c.getSource(),new BlockPos(v[0],v[1],v[2]),new BlockPos(v[3],v[4],v[5]));
        });
        for(int i=4;i>=0;i--)soilLast=Commands.argument(soilNames[i],IntegerArgumentType.integer(-30_000_000,30_000_000)).then(soilLast);
        site.then(Commands.literal("soil").then(soilLast));
        String[] forestNames = {"sx","sy","sz","lx","ly","lz","count","px","py","pz"};
        var forestLast = Commands.argument("pz",IntegerArgumentType.integer(-30_000_000,30_000_000)).executes(c -> {
            int[] v = new int[10]; for (int i=0;i<v.length;i++) v[i]=IntegerArgumentType.getInteger(c,forestNames[i]);
            return forestry(c.getSource(),new BlockPos(v[0],v[1],v[2]),new BlockPos(v[3],v[4],v[5]),v[6],new BlockPos(v[7],v[8],v[9]));
        });
        for (int i=forestNames.length-2;i>=0;i--) forestLast = Commands.argument(forestNames[i],
                i==6 ? IntegerArgumentType.integer(1,4) : IntegerArgumentType.integer(-30_000_000,30_000_000)).then(forestLast);
        var forest=Commands.literal("forestry").then(forestLast);
        var moveCount=Commands.argument("count",IntegerArgumentType.integer(1,4)).executes(c -> moveForestry(c.getSource(),
                UUID.fromString(StringArgumentType.getString(c,"id")),new BlockPos(IntegerArgumentType.getInteger(c,"lx"),
                IntegerArgumentType.getInteger(c,"ly"),IntegerArgumentType.getInteger(c,"lz")),IntegerArgumentType.getInteger(c,"count")));
        var moveZ=Commands.argument("lz",IntegerArgumentType.integer(-30_000_000,30_000_000)).then(moveCount);
        var moveY=Commands.argument("ly",IntegerArgumentType.integer(-2032,2032)).then(moveZ);
        var moveX=Commands.argument("lx",IntegerArgumentType.integer(-30_000_000,30_000_000)).then(moveY);
        forest.then(Commands.literal("move").then(Commands.argument("id",StringArgumentType.word()).then(moveX)));
        String[] leafNames={"x1","y1","z1","x2","y2","z2"};
        var leafLast=Commands.argument("z2",IntegerArgumentType.integer(-30_000_000,30_000_000)).executes(c -> {
            int[] v=new int[6];for(int i=0;i<6;i++) v[i]=IntegerArgumentType.getInteger(c,leafNames[i]);
            return clearance(c.getSource(),UUID.fromString(StringArgumentType.getString(c,"id")),new BlockPos(v[0],v[1],v[2]),new BlockPos(v[3],v[4],v[5]));
        });
        for(int i=4;i>=0;i--) leafLast=Commands.argument(leafNames[i],IntegerArgumentType.integer(-30_000_000,30_000_000)).then(leafLast);
        forest.then(Commands.literal("leaves").then(Commands.argument("id",StringArgumentType.word()).then(leafLast)));
        var rootZ=Commands.argument("z",IntegerArgumentType.integer(-30_000_000,30_000_000)).executes(c -> root(c.getSource(),
                UUID.fromString(StringArgumentType.getString(c,"id")),new BlockPos(IntegerArgumentType.getInteger(c,"x"),
                IntegerArgumentType.getInteger(c,"y"),IntegerArgumentType.getInteger(c,"z"))));
        forest.then(Commands.literal("root").then(Commands.argument("id",StringArgumentType.word()).then(
                Commands.argument("x",IntegerArgumentType.integer(-30_000_000,30_000_000)).then(
                Commands.argument("y",IntegerArgumentType.integer(-2032,2032)).then(rootZ)))));
        site.then(forest);
        var coordinates = Commands.argument("x1", IntegerArgumentType.integer(-30_000_000, 30_000_000));
        String[] rest = {"y1", "z1", "x2", "y2", "z2"};
        // Build from the final argument backwards so each immutable builder has its complete child.
        var last = Commands.argument("z2", IntegerArgumentType.integer(-30_000_000, 30_000_000)).executes(c -> {
            int x1 = IntegerArgumentType.getInteger(c, "x1"), y1 = IntegerArgumentType.getInteger(c, "y1"), z1 = IntegerArgumentType.getInteger(c, "z1");
            int x2 = IntegerArgumentType.getInteger(c, "x2"), y2 = IntegerArgumentType.getInteger(c, "y2"), z2 = IntegerArgumentType.getInteger(c, "z2");
            return protect(c.getSource(), new BlockPos(Math.min(x1,x2), Math.min(y1,y2), Math.min(z1,z2)),
                    new BlockPos(Math.max(x1,x2), Math.max(y1,y2), Math.max(z1,z2)));
        });
        for (int i = rest.length - 2; i >= 0; i--) last = Commands.argument(rest[i], IntegerArgumentType.integer(-30_000_000, 30_000_000)).then(last);
        site.then(Commands.literal("protect").then(coordinates.then(last)));
        d.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(site));
    }
    private static boolean available(CommandSourceStack s) {
        if (store != null && store.healthy()) return true;
        VillagerCommands.tell(s, "Construction is unavailable; no site permissions changed. See the server log."); return false;
    }
    private static int farm(CommandSourceStack s, int radius) {
        if (!available(s)) return 0;
        try {
            var player = s.getPlayerOrException(); var level = s.getLevel(); BlockPos center = player.blockPosition().below();
            var cells = new ArrayList<WorkSites.Point>();
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius,0,-radius), center.offset(radius,0,radius))) {
                if (!level.isLoaded(p) || !level.isLoaded(p.above(2))) continue;
                var soil = level.getBlockState(p);
                if ((soil.is(Blocks.DIRT) || soil.is(Blocks.GRASS_BLOCK)) && level.getBlockState(p.above()).isAir()
                        && level.getBlockState(p.above(2)).isAir() && level.canSeeSky(p.above())) cells.add(point(level, p));
            }
            var grant = new WorkSites.Farm(UUID.randomUUID(), player.getUUID(), cells);
            if (!update(store.snapshot().add(grant))) return 0;
            VillagerCommands.tell(s, "Approved " + cells.size() + " ground cells for farm expansion: " + grant.id()
                    + ". Protected areas still win; later outside edits revoke affected cells."); return 1;
        } catch (Exception e) { VillagerCommands.tell(s, "Farm site not created: " + e.getMessage()); return 0; }
    }
    private static int soil(CommandSourceStack s,BlockPos source,BlockPos target) {
        if(!available(s))return 0;
        try {
            var player=s.getPlayerOrException();var level=s.getLevel();
            if(!SoilWork.sourceSite(level,source) || !SoilWork.targetSite(level,target))
                throw new IllegalArgumentException("Need one bare native dirt/grass source and one supported wet bare sand destination");
            if(player.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(source))>1024
                    || player.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(target))>1024)
                throw new IllegalArgumentException("Approach both exact cells before approving a soil transfer");
            String kind=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(source).getBlock()).toString();
            var job=new WorkSites.Soil(UUID.randomUUID(),player.getUUID(),point(level,source),point(level,target),kind,false);
            if(!update(store.snapshot().add(job)))return 0;
            VillagerCommands.tell(s,"Approved one finite soil transfer "+job.id()+": actual dirt source, paid sand replacement, then one planted plot. Assigned day trips may reach64blocks from home along the source corridor; ordinary roaming remains28. Protection wins.");return 1;
        } catch(Exception e) { VillagerCommands.tell(s,"Soil site not created: "+e.getMessage());return 0; }
    }
    private static int protect(CommandSourceStack s, BlockPos min, BlockPos max) {
        if (!available(s)) return 0;
        try {
            var p = new WorkSites.Protection(UUID.randomUUID(), s.getPlayerOrException().getUUID(), point(s.getLevel(), min), point(s.getLevel(), max));
            if (!update(store.snapshot().add(p))) return 0;
            VillagerCommands.tell(s, "Protected area saved: " + p.id()); return 1;
        } catch (Exception e) { VillagerCommands.tell(s, "Area not protected: " + e.getMessage()); return 0; }
    }
    private static int station(CommandSourceStack s) {
        if (!available(s)) return 0;
        try {
            var player = s.getPlayerOrException(); var pos = player.blockPosition();
            if (!StationProduction.safeSite(s.getLevel(), pos)) throw new IllegalStateException("Need air over safe support, clear access and distance from beds/doors/containers");
            var grant = new WorkSites.Station(UUID.randomUUID(), player.getUUID(), point(s.getLevel(), pos));
            if (!update(store.snapshot().add(grant))) return 0;
            VillagerCommands.tell(s, "Approved one crafting table at " + pos.toShortString() + ": " + grant.id()
                    + ". Protection still wins; outside edits revoke its footprint."); return 1;
        } catch (Exception e) { VillagerCommands.tell(s, "Station site not created: " + e.getMessage()); return 0; }
    }
    private static int list(CommandSourceStack s) {
        if (!available(s)) return 0;
        for (var f : store.snapshot().farms()) VillagerCommands.tell(s, "Farm " + f.id() + ": " + f.cells().size() + " remaining cells");
        for (var p : store.snapshot().protections()) VillagerCommands.tell(s, "Protected " + p.id() + ": " + p.min() + " to " + p.max());
        for (var p : store.snapshot().stations()) VillagerCommands.tell(s, "Table " + p.id() + ": " + p.target());
        for (var p : store.snapshot().forestry()) VillagerCommands.tell(s,"Forestry " + p.id()+": seedTaken="+p.seedTaken()+", logs remaining="+p.remaining().size()+", plant="+p.plant());
        for(var j:store.snapshot().soilJobs()) VillagerCommands.tell(s,"Soil "+j.id()+": gathered="+j.gathered()+", source="+j.source()+", target="+j.target());
        return store.snapshot().soilJobs().size() + store.snapshot().farms().size() + store.snapshot().protections().size() + store.snapshot().stations().size() + store.snapshot().forestry().size();
    }
    private static int forestry(CommandSourceStack s, BlockPos seed, BlockPos log, int count, BlockPos plant) {
        if (!available(s)) return 0;
        try {
            var player = s.getPlayerOrException(); var level = s.getLevel(); var logs = new ArrayList<WorkSites.Point>();
            if (!ForestryWork.matureSeed(level,seed) || !ForestryWork.plantSite(level,plant)) throw new IllegalStateException("Need a mature hanging propagule and a clear native planting cell");
            for (int i=0;i<count;i++) {
                var p=log.above(i);
                if (!level.isLoaded(p) || !level.getBlockState(p).is(Blocks.MANGROVE_LOG)) throw new IllegalStateException("Approve only existing exact mangrove log cells");
                logs.add(point(level,p));
            }
            for (BlockPos p : java.util.List.of(seed,log,log.above(count-1),plant))
                if (player.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p)) > 1024) throw new IllegalStateException("Approach the bounded resource job before approving it");
            String soil = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(plant.below()).getBlock()).toString();
            var grant = new WorkSites.Forestry(UUID.randomUUID(),player.getUUID(),point(level,seed),logs,point(level,plant),soil,false,logs);
            if (!update(store.snapshot().add(grant))) return 0;
            VillagerCommands.tell(s,"Approved finite forestry job "+grant.id()+": native seed, "+count+" exact logs, then paid planting. Protection wins."); return 1;
        } catch (Exception e) { VillagerCommands.tell(s,"Forestry site not created: "+e.getMessage()); return 0; }
    }
    private static int moveForestry(CommandSourceStack s,UUID id,BlockPos log,int count) {
        if (!available(s)) return 0;
        try {
            var player=s.getPlayerOrException();var level=s.getLevel();
            var old=store.snapshot().forestry().stream().filter(f -> f.id().equals(id)).findFirst().orElseThrow();
            var logs=new ArrayList<WorkSites.Point>();
            if (!old.seed().dimension().equals(level.dimension().identifier().toString()) || !ForestryWork.plantSite(level,position(old.plant())))
                throw new IllegalStateException("Original planting cell must remain valid in this dimension");
            for(int i=0;i<count;i++) {
                var p=log.above(i);
                if (!level.isLoaded(p) || !level.getBlockState(p).is(Blocks.MANGROVE_LOG)
                        || player.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p))>1024) throw new IllegalStateException("Approach existing exact log cells");
                logs.add(point(level,p));
            }
            var next=new WorkSites.Forestry(UUID.randomUUID(),old.owner(),old.seed(),logs,old.plant(),old.soil(),true,logs,old.clearance(),old.uncleared(),old.roots(),old.unrooted());
            if (!update(store.snapshot().moveLogs(id,next))) return 0;
            VillagerCommands.tell(s,"Replaced old forestry authority with "+next.id()+" for "+count+" logs; prior seed collection and planting obligation retained.");return 1;
        } catch(Exception e) { VillagerCommands.tell(s,"Forestry logs not reassigned: "+e.getMessage());return 0; }
    }
    private static int root(CommandSourceStack s,UUID id,BlockPos p) {
        if (!available(s)) return 0;
        try {
            var player=s.getPlayerOrException();var level=s.getLevel();
            if (!level.isLoaded(p) || player.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p))>1024
                    || !level.getBlockState(p).is(Blocks.MANGROVE_ROOTS) || !level.getFluidState(p).isEmpty())
                throw new IllegalStateException("Approach one existing loaded dry mangrove root");
            if (!update(store.snapshot().root(id,point(level,p)))) return 0;
            VillagerCommands.tell(s,"Approved one exact dry root with fresh forestry authority; no log quota increase. Use site list for the new identity.");return 1;
        } catch(Exception e) { VillagerCommands.tell(s,"Root clearance not added: "+e.getMessage());return 0; }
    }
    private static int clearance(CommandSourceStack s,UUID id,BlockPos a,BlockPos b) {
        if (!available(s)) return 0;
        try {
            var player=s.getPlayerOrException();var level=s.getLevel();
            var min=new BlockPos(Math.min(a.getX(),b.getX()),Math.min(a.getY(),b.getY()),Math.min(a.getZ(),b.getZ()));
            var max=new BlockPos(Math.max(a.getX(),b.getX()),Math.max(a.getY(),b.getY()),Math.max(a.getZ(),b.getZ()));
            long volume=((long)max.getX()-min.getX()+1)*((long)max.getY()-min.getY()+1)*((long)max.getZ()-min.getZ()+1);
            if (volume<1 || volume>4 || max.getY()-min.getY()>3 || max.getX()-min.getX()>3 || max.getZ()-min.getZ()>3) throw new IllegalArgumentException("Approve at most4exact leaf cells");
            var cells=new ArrayList<BlockPos>();
            for(var p:BlockPos.betweenClosed(min,max)) {
                if (!level.isLoaded(p) || player.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p))>1024) throw new IllegalStateException("Approach loaded clearance cells");
                if (level.getBlockState(p).is(Blocks.MANGROVE_LEAVES)) cells.add(p.immutable());
                else if (!level.getBlockState(p).isAir()) throw new IllegalArgumentException("Clearance box must contain only leaves/air");
            }
            // Prune outer leaves first: removing a greater-distance leaf cannot shorten a remaining leaf's log-distance path.
            cells.sort(java.util.Comparator.comparingInt((BlockPos p) -> level.getBlockState(p).getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)).reversed());
            var next=store.snapshot().clearance(id,cells.stream().map(p -> point(level,p)).toList());
            if (!update(next)) return 0;
            VillagerCommands.tell(s,"Approved "+cells.size()+" exact clearance leaves with fresh forestry authority; log quota and planting retained. Use site list for its new identity.");return 1;
        } catch(Exception e) { VillagerCommands.tell(s,"Forestry clearance not added: "+e.getMessage());return 0; }
    }
    private static int remove(CommandSourceStack s, UUID id) {
        if (!available(s)) return 0;
        WorkSites next = store.snapshot().remove(id);
        if (next.equals(store.snapshot())) { VillagerCommands.tell(s, "No site has that identity."); return 0; }
        if (!update(next)) return 0;
        VillagerCommands.tell(s, "Removed site " + id); return 1;
    }
}
