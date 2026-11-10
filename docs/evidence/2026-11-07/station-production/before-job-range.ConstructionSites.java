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
    static void stop() { TRACKED.clear(); STATIONS.clear(); PROTECTED.clear(); if (store != null) store.close(); store = null; server = null; OWN.remove(); }
    private static WorkSites.Point point(ServerLevel level, BlockPos p) {
        return new WorkSites.Point(level.dimension().identifier().toString(), p.getX(), p.getY(), p.getZ());
    }
    private static void rebuild() {
        TRACKED.clear();
        STATIONS.clear();
        PROTECTED.clear();
        if (store == null || !store.healthy() || server == null) return;
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
                .filter(p -> p.dimension().equals(level.dimension().identifier().toString()) && Math.abs(p.x() - origin.getX()) <= 8
                        && Math.abs(p.y() - origin.getY()) <= 3 && Math.abs(p.z() - origin.getZ()) <= 8)
                .filter(p -> store.snapshot().allowsStation(p)).map(p -> new BlockPos(p.x(), p.y(), p.z())).toList();
    }
    /** Native Level.setBlock entry: an outside edit retires the affected finite ground grant before the edit. */
    public static void beforeBlockWrite(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel actual) || TRACKED.isEmpty() && STATIONS.isEmpty()) return;
        var index = TRACKED.get(actual);
        var stations = STATIONS.get(actual);
        if ((index == null || !index.containsKey(pos.asLong())) && (stations == null || !stations.containsKey(pos.asLong()))) return;
        OwnEdit own = OWN.get();
        if (own != null && own.level() == actual && own.blocks().contains(pos)) return;
        update(store.snapshot().touch(point(actual, pos)));
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

    static void register(CommandDispatcher<CommandSourceStack> d) {
        var site = Commands.literal("site")
                .then(Commands.literal("table").executes(c -> station(c.getSource())))
                .then(Commands.literal("farm").then(Commands.argument("radius", IntegerArgumentType.integer(0, 4))
                        .executes(c -> farm(c.getSource(), IntegerArgumentType.getInteger(c, "radius")))))
                .then(Commands.literal("list").executes(c -> list(c.getSource())))
                .then(Commands.literal("remove").then(Commands.argument("id", StringArgumentType.word())
                        .executes(c -> remove(c.getSource(), UUID.fromString(StringArgumentType.getString(c, "id"))))));
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
        return store.snapshot().farms().size() + store.snapshot().protections().size() + store.snapshot().stations().size();
    }
    private static int remove(CommandSourceStack s, UUID id) {
        if (!available(s)) return 0;
        WorkSites next = store.snapshot().remove(id);
        if (next.equals(store.snapshot())) { VillagerCommands.tell(s, "No site has that identity."); return 0; }
        if (!update(next)) return 0;
        VillagerCommands.tell(s, "Removed site " + id); return 1;
    }
}
