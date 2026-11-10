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
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Explicit play-copy inspection. No path creation/assignment, resource work, actor or terrain setters. */
final class ForestryProbe {
    private ForestryProbe() {}
    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("forestryprobe").then(Commands.argument("uuid",StringArgumentType.word())
                        .executes(c -> inspect(c.getSource(),UUID.fromString(StringArgumentType.getString(c,"uuid")))))));
    }
    private static Map<String,Object> sample(ServerLevel level,Villager v,Session s,BlockPos target,BlockPos p) {
        var result=new LinkedHashMap<String,Object>();result.put("feet",p.toShortString());
        String failure=ForestryWork.standFailure(level,v,s,target,p);result.put("failure",failure==null ? "accepted" : failure);
        if (!level.isLoaded(p) || !level.isLoaded(p.below())) return result;
        result.put("feetState",level.getBlockState(p).toString());result.put("headState",level.getBlockState(p.above()).toString());
        result.put("supportState",level.getBlockState(p.below()).toString());
        var eye=ForestryWork.standEye(v,p);result.put("eye",List.of(eye.x,eye.y,eye.z));
        result.put("reachSquared",eye.distanceToSqr(Vec3.atCenterOf(target)));
        if(eye.distanceToSqr(Vec3.atCenterOf(target))>9)return result;
        var hit=ForestryWork.clip(level,v,eye,target);
        if(hit==null){result.put("hitType","unloaded ray window");return result;}
        result.put("hitType",hit.getType().name());
        if (hit.getType()==HitResult.Type.BLOCK) {
            result.put("hitBlock",hit.getBlockPos().toShortString());
            if (level.isLoaded(hit.getBlockPos())) result.put("hitState",level.getBlockState(hit.getBlockPos()).toString());
        }
        return result;
    }
    private static int inspect(CommandSourceStack source,UUID id) {
        if (!DevMode.inWorld(source.getServer(),DevMode.TRIAL_WORLD) || !"play".equals(System.getProperty("villagers.role"))) return 0;
        try {
            ServerLevel level=source.getLevel();
            if (!(level.getEntity(id) instanceof Villager v) || !Villagers.isManaged(v)) throw new IllegalStateException("Managed resident not loaded yet");
            Session s=Villagers.sessionIfLoaded(v,level);if(s==null)throw new IllegalStateException("Mind not loaded yet");
            var job=ConstructionSites.forestry(level).stream().filter(f -> ForestryWork.home(level,s,f)).findFirst().orElseThrow();
            var report=new LinkedHashMap<String,Object>();report.put("kind","READ ONLY native forestry stand inspection; no path creation or setters");
            report.put("gameTime",level.getGameTime());report.put("actor",id);report.put("job",job);report.put("home",s.mind.home());
            report.put("position",List.of(v.getX(),v.getY(),v.getZ()));report.put("pose",v.getPose().name());
            report.put("eyeOffset",v.getEyePosition().y-v.getY());report.put("height",v.getBbHeight());report.put("width",v.getBbWidth());
            report.put("food",s.mind.foodLevel());report.put("onGround",v.onGround());report.put("inWater",v.isInWater());
            var targets=new ArrayList<>(job.remaining());targets.add(job.plant());
            var results=new ArrayList<Map<String,Object>>();
            for(var point:targets) {
                BlockPos target=ConstructionSites.position(point);var counts=new LinkedHashMap<String,Integer>();
                var examples=new LinkedHashMap<String,List<Map<String,Object>>>();var accepted=new ArrayList<String>();
                for(var p:BlockPos.betweenClosed(target.offset(-2,-3,-2),target.offset(2,0,2))) {
                    String reason=ForestryWork.standFailure(level,v,s,target,p);if(reason==null)reason="accepted";
                    counts.merge(reason,1,Integer::sum);if(reason.equals("accepted"))accepted.add(p.toShortString());
                    var list=examples.computeIfAbsent(reason,k -> new ArrayList<>());
                    if(list.size()<3)list.add(sample(level,v,s,target,p));
                }
                results.add(Map.of("target",target.toShortString(),"counts",counts,"accepted",accepted,"examples",examples,
                        "referencePlantStand",sample(level,v,s,target,ConstructionSites.position(job.plant()))));
            }
            report.put("targets",results);
            var folder=source.getServer().getWorldPath(LevelResource.ROOT).resolve("villagers/forestry-evidence");Files.createDirectories(folder);
            Files.writeString(folder.resolve("inspect-"+id+"-"+level.getGameTime()+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report),StandardOpenOption.CREATE_NEW);
            VillagerCommands.tell(source,"Read-only native forestry stands recorded for "+results.size()+"targets");return 1;
        } catch(Exception failure) { VillagerCommands.tell(source,"Forestry probe: "+failure.getMessage());return 0; }
    }
}
