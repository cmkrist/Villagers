package local.villagers;

import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Explicit play-copy native path/collision inspection. Does not assign a path or change the resident/world. */
final class HomeRouteProbe {
    private HomeRouteProbe() {}
    static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("routeprobe").then(Commands.argument("uuid", StringArgumentType.word())
                        .executes(c -> inspect(c.getSource(), UUID.fromString(StringArgumentType.getString(c, "uuid")))))));
    }
    private static Map<String, Object> path(Path path) {
        if (path == null) return Map.of("present", false);
        var positions = new ArrayList<String>();
        for (int i = 0; i < Math.min(128, path.getNodeCount()); i++) positions.add(path.getNodePos(i).toShortString());
        return Map.of("present", true, "canReach", path.canReach(), "nodes", path.getNodeCount(),
                "target", path.getTarget().toShortString(), "positions", positions);
    }
    private static int inspect(CommandSourceStack source, UUID id) {
        if (!DevMode.inWorld(source.getServer(), DevMode.TRIAL_WORLD) || !"play".equals(System.getProperty("villagers.role"))) return 0;
        try {
            ServerLevel level = source.getLevel();
            if (!(level.getEntity(id) instanceof Villager v) || !Villagers.isManaged(v)) throw new IllegalStateException("Managed resident not loaded yet");
            if (!v.getNavigation().isDone() || v.isSleeping() || v.isTrading()) throw new IllegalStateException("Inspect only an idle, awake, nontrading resident");
            Session s = Villagers.sessionIfLoaded(v, level); if (s == null) throw new IllegalStateException("Mind not loaded yet");
            var result = new LinkedHashMap<String, Object>(); result.put("kind", "READ ONLY native home/collision/path inspection; no path assignment or setters");
            result.put("gameTime", level.getGameTime()); result.put("actor", id); result.put("home", s.mind.home());
            result.put("position", List.of(v.getX(), v.getY(), v.getZ())); result.put("pose", v.getPose().name());
            result.put("width", v.getBbWidth()); result.put("height", v.getBbHeight()); result.put("onGround", v.onGround());
            AABB box = v.getBoundingBox(); result.put("box", List.of(box.minX,box.minY,box.minZ,box.maxX,box.maxY,box.maxZ));
            result.put("collisionFree", level.noCollision(v)); result.put("canOpenDoors", v.getNavigation().getNodeEvaluator().canOpenDoors());
            if (s.mind.home() != null) {
                var home = s.mind.home(); var p = new BlockPos(home.x(),home.y(),home.z());
                result.put("homePath", path(v.getNavigation().createPath(p,1)));
            }
            var cells = new ArrayList<Map<String, Object>>();
            for (BlockPos p : BlockPos.betweenClosed(v.blockPosition().offset(-1,-2,-1),v.blockPosition().offset(1,2,1))) {
                if (!level.isLoaded(p)) continue;
                var state = level.getBlockState(p); var shape = state.getCollisionShape(level,p);
                var entry = new LinkedHashMap<String,Object>(); entry.put("position",p.toShortString()); entry.put("state",state.toString());
                entry.put("intersectsBody",shape.toAabbs().stream().anyMatch(b -> b.move(p).intersects(box)));
                entry.put("shape",shape.toAabbs().stream().map(AABB::toString).toList()); cells.add(entry);
            }
            result.put("nearby",cells);
            var candidates = new ArrayList<Map<String,Object>>();
            // Four bounded local diagnostic paths. No candidate is assigned to navigation.
            for (int[] delta : new int[][] {{-1,0},{1,0},{0,-1},{0,1}}) {
                double x = v.getX()+delta[0], z=v.getZ()+delta[1];
                AABB lower = box.move(delta[0],-1,delta[1]);
                candidates.add(Map.of("feet",List.of(x,v.getY()-1,z),"lowerCollisionFree",level.noCollision(v,lower),
                        "path",path(v.getNavigation().createPath(BlockPos.containing(x,v.getY()-1,z),0))));
            }
            result.put("localCandidates",candidates);
            var folder = source.getServer().getWorldPath(LevelResource.ROOT).resolve("villagers/route-evidence"); Files.createDirectories(folder);
            Files.writeString(folder.resolve("inspect-"+id+"-"+level.getGameTime()+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(result),StandardOpenOption.CREATE_NEW);
            VillagerCommands.tell(source,"Native path inspection recorded: collisionFree="+result.get("collisionFree")); return 1;
        } catch (Exception failure) { VillagerCommands.tell(source,"Route probe: "+failure.getMessage()); return 0; }
    }
}
