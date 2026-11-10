package local.villagers;

import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelResource;

/** Play-copy-only native hook check. Writes the same block state: no supplies, terrain change or actor setters. */
final class ConstructionProbe {
    private ConstructionProbe() {}
    static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("constructionprobe")
                        .then(Commands.literal("touch").executes(c -> touch(c.getSource(), 0)))
                        .then(Commands.literal("touchstation").executes(c -> touch(c.getSource(), 1)))
                        .then(Commands.literal("touchforestry").executes(c -> touch(c.getSource(), 2)))));
        var routeZ=Commands.argument("z",IntegerArgumentType.integer(-30_000_000,30_000_000))
                .executes(c -> route(c.getSource(),UUID.fromString(StringArgumentType.getString(c,"actor")),
                        new BlockPos(IntegerArgumentType.getInteger(c,"x"),IntegerArgumentType.getInteger(c,"y"),IntegerArgumentType.getInteger(c,"z"))));
        var routeY=Commands.argument("y",IntegerArgumentType.integer(-2032,2032)).then(routeZ);
        var routeX=Commands.argument("x",IntegerArgumentType.integer(-30_000_000,30_000_000)).then(routeY);
        var routeActor=Commands.argument("actor",StringArgumentType.word()).then(routeX);
        d.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("constructionprobe").then(Commands.literal("route").then(routeActor))));
    }
    /** One explicit loaded native path query; no moveTo, block/entity writes, grant or AI decision. */
    private static int route(CommandSourceStack source,UUID actor,BlockPos goal) {
        if(!DevMode.inWorld(source.getServer(),DevMode.TRIAL_WORLD) || !"play".equals(System.getProperty("villagers.role")))return 0;
        try {
            var level=source.getLevel();
            if(!(level.getEntity(actor) instanceof net.minecraft.world.entity.npc.villager.Villager v)
                    || !v.isAlive() || !Villagers.isManaged(v) || !level.isLoaded(goal) || !level.isLoaded(goal.above())
                    || v.position().distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(goal))>64*64)
                throw new IllegalArgumentException("Need a current managed actor and loaded goal within64");
            var session=Villagers.existingSession(actor);
            boolean queried=session!=null && WorkNavigation.admit(level,level.getGameTime());
            var before=v.getNavigation().getPath();boolean doneBefore=v.getNavigation().isDone();
            var path=queried?v.getNavigation().createPath(goal,0):null;
            var nodes=new ArrayList<List<Integer>>();boolean loaded=path!=null && path.getNodeCount()>0 && path.getNodeCount()<=128,home=loaded;
            if(path!=null && path.getNodeCount()<=128)for(int i=0;i<path.getNodeCount();i++) {
                var p=path.getNodePos(i);nodes.add(List.of(p.getX(),p.getY(),p.getZ()));loaded &= level.isLoaded(p);
                home &= VillageSurvival.withinHomeRange(level,session.mind,net.minecraft.world.phys.Vec3.atBottomCenterOf(p));
            }
            var report=new LinkedHashMap<String,Object>();report.put("kind","READ_ONLY native path query; no movement, world writes or authority granted");
            report.put("gameTime",level.getGameTime());report.put("actor",actor.toString());report.put("position",List.of(v.getX(),v.getY(),v.getZ()));
            report.put("goal",List.of(goal.getX(),goal.getY(),goal.getZ()));report.put("queryPerformed",queried);
            report.put("canReach",queried?Boolean.valueOf(path!=null && path.canReach()):null);
            report.put("nodeCount",path==null?0:path.getNodeCount());report.put("nodes",nodes);report.put("allNodesLoaded",loaded);
            report.put("insideOrdinaryHome",home);report.put("bounded",path!=null && path.getNodeCount()>0 && path.getNodeCount()<=128);
            report.put("navigationPathUnchanged",before==v.getNavigation().getPath() && doneBefore==v.getNavigation().isDone());
            var terrain=new ArrayList<Map<String,Object>>();
            for(var p:BlockPos.betweenClosed(goal.offset(-1,-1,-1),goal.offset(1,2,1)))if(level.isLoaded(p))
                terrain.add(Map.of("position",List.of(p.getX(),p.getY(),p.getZ()),"state",level.getBlockState(p).toString()));
            report.put("goalTerrain",terrain);
            var body=v.getBoundingBox().move(net.minecraft.world.phys.Vec3.atBottomCenterOf(goal).subtract(v.position()));
            report.put("goalBodyClear",level.noCollision(v,body));
            var folder=source.getServer().getWorldPath(LevelResource.ROOT).resolve("villagers/construction-evidence");Files.createDirectories(folder);
            Files.writeString(folder.resolve("route-"+actor+"-"+level.getGameTime()+".json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(report),StandardOpenOption.CREATE_NEW);
            VillagerCommands.tell(source,queried?"Read-only native route/terrain recorded; no movement or work permission granted."
                    :"Native terrain recorded; session/path budget unavailable, NO path query performed.");return 1;
        }catch(Exception e) { VillagerCommands.tell(source,"Route query: "+e.getMessage());return 0; }
    }
    private static int touch(CommandSourceStack source, int mode) {
        if (!DevMode.inWorld(source.getServer(), DevMode.TRIAL_WORLD) || !"play".equals(System.getProperty("villagers.role"))) return 0;
        try {
            var soil = source.getPlayerOrException().blockPosition(); if (mode==0) soil = soil.below(); var level = source.getLevel();
            boolean before = mode==2 ? ConstructionSites.forestryAt(level,soil)!=null : mode==1 ? ConstructionSites.allowsStation(level, soil) : ConstructionSites.allowsFarm(level, soil);
            if (!before) throw new IllegalStateException("Approve this ground cell first");
            var state = level.getBlockState(soil);
            boolean result = level.setBlock(soil, state, Block.UPDATE_ALL);
            boolean after = mode==2 ? ConstructionSites.forestryAt(level,soil)!=null : mode==1 ? ConstructionSites.allowsStation(level, soil) : ConstructionSites.allowsFarm(level, soil), unchanged = level.getBlockState(soil).equals(state);
            var report = Map.of("gameTime", level.getGameTime(), "soil", soil.toShortString(), "authorizedBefore", before,
                    "authorizedAfter", after, "blockUnchanged", unchanged, "nativeSetBlockReturned", result);
            var folder = source.getServer().getWorldPath(LevelResource.ROOT).resolve("villagers/construction-evidence"); Files.createDirectories(folder);
            Files.writeString(folder.resolve((mode==2 ? "outside-forestry-touch-" : mode==1 ? "outside-station-touch-" : "outside-touch-") + level.getGameTime() + ".json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(report), StandardOpenOption.CREATE_NEW);
            VillageTrial.noteIntervention(level, "constructionprobe: outside native same-state block write at " + soil
                    + "; tests conservative permission revocation, no terrain or resource change");
            if (after || !unchanged) throw new IllegalStateException("Outside-edit revocation failed");
            VillagerCommands.tell(source, "Native outside-edit check passed; this ground permission was revoked."); return 1;
        } catch (Exception failure) { VillagerCommands.tell(source, "Construction probe: " + failure.getMessage()); return 0; }
    }
}
