package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.*;

/** Read-only native path queries in a verified COPY; freezes before simulation and never moves a body. */
public final class FoodAccessAuditProbe implements ModInitializer {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private int ticks;
    private long time;
    private boolean finished;
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    private static Map<String,Object> path(Path p){
        if(p==null)return Map.of("present",false);
        return Map.of("present",true,"canReach",p.canReach(),"nodes",p.getNodeCount(),
            "end",p.getNodeCount()==0?"none":p.getNodePos(p.getNodeCount()-1).toShortString());
    }
    @Override public void onInitialize(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            check(Files.exists(FabricLoader.getInstance().getGameDir().resolve("food-diagnostic-copy.json")),"copy marker required");
            time=server.overworld().getGameTime();server.tickRateManager().setFrozen(true);
            for(int x=19;x<=26;x++)for(int z=5;z<=13;z++){server.overworld().setChunkForced(x,z,true);server.overworld().getChunk(x,z);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(finished)return;
            var file=FabricLoader.getInstance().getGameDir().resolve("food-access-audit.json");
            try{
                check(++ticks<400,"entity loading exhausted");var level=server.overworld();check(level.getGameTime()==time,"frozen game time unchanged");
                var bodies=new ArrayList<Villager>();
                for(var e:level.getAllEntities())if(e instanceof Villager v&&v.isAlive()&&Villagers.isManaged(v)&&v.getX()>300&&v.getX()<420)bodies.add(v);
                if(bodies.size()<5||ticks<40||!Villagers.ready())return;
                var actors=new ArrayList<Map<String,Object>>();var routes=new ArrayList<Map<String,Object>>();
                var animals=new ArrayList<Map<String,Object>>();var resources=new ArrayList<Map<String,Object>>();
                for(var e:level.getAllEntities())if(Set.of(EntityTypes.RABBIT,EntityTypes.COW,EntityTypes.PIG,EntityTypes.SHEEP).contains(e.getType())) {
                    var row=new LinkedHashMap<String,Object>();row.put("id",e.getUUID().toString());row.put("type",BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString());
                    row.put("position",List.of(e.getX(),e.getY(),e.getZ()));
                    var approaches=new ArrayList<Map<String,Object>>();for(var v:bodies)if(v.distanceToSqr(e)<64*64)approaches.add(Map.of("actor",v.getUUID().toString(),"distance",v.distanceTo(e),"visible",v.hasLineOfSight(e),"path",path(v.getNavigation().createPath(e.blockPosition(),1))));
                    row.put("approaches",approaches);animals.add(row);
                }
                for(var v:bodies){
                    var s=Villagers.session(v,level);
                    v.getNavigation().setCanOpenDoors(true);v.getNavigation().getNodeEvaluator().setCanPassDoors(true);v.getNavigation().setCanFloat(true);
                    var station=BreadCrafting.table(level,v,s);
                    if(station!=null&&BreadCrafting.wheatCount(v.getInventory())>=3){
                        var stands=new ArrayList<BlockPos>();
                        for(var p:BlockPos.betweenClosed(station.offset(-2,-1,-2),station.offset(2,1,2))){
                            if(!level.isLoaded(p)||!level.getBlockState(p).isAir()||!level.getBlockState(p.above()).isAir()
                                ||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),net.minecraft.core.Direction.UP))continue;
                            var eye=Vec3.atBottomCenterOf(p).add(0,v.getEyeHeight(),0);
                            var hit=level.clip(new net.minecraft.world.level.ClipContext(eye,Vec3.atCenterOf(station),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,v));
                            if(eye.distanceToSqr(Vec3.atCenterOf(station))<=9&&(hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS||hit.getBlockPos().equals(station)))stands.add(p.immutable());
                        }
                        stands.sort(Comparator.comparingDouble(p->v.position().distanceToSqr(Vec3.atBottomCenterOf(p))));
                        var approaches=new ArrayList<Map<String,Object>>();
                        for(var stand:stands){var route=v.getNavigation().createPath(stand,0,64);approaches.add(Map.of("stand",stand.toShortString(),"path",path(route),"bounded",WorkNavigation.staysHome(level,v,s,route)));}
                        routes.add(Map.of("from",s.mind.name(),"purpose","workbench-approaches","table",station.toShortString(),"stands",approaches));
                    }
                    v.getNavigation().setCanOpenDoors(true);v.getNavigation().getNodeEvaluator().setCanPassDoors(true);v.getNavigation().setCanFloat(true);
                    actors.add(Map.of("id",v.getUUID().toString(),"name",s.mind.name(),"position",List.of(v.getX(),v.getY(),v.getZ()),"health",v.getHealth(),
                        "food",s.mind.foodLevel(),"inventory",v.getInventory().toString(),"home",String.valueOf(s.mind.home())));
                    if(s.mind.foodLevel()<8){
                        var option=FoodAccess.preview(level,v);
                        boolean expanded=false;if(option==null){expanded=true;option=FoodAccess.preview(level,v,128,64000);}
                        var route=option==null?null:option.route();
                        var row=new LinkedHashMap<String,Object>();row.put("from",s.mind.name());row.put("purpose","hay-passage-plan");
                        row.put("found",route!=null);
                        row.put("expandedDiagnosticBudget",expanded);
                        if(option!=null){row.put("store",option.store().toShortString());row.put("table",option.table()==null?"":option.table().toShortString());row.put("edible",option.edible());}
                        row.put("search",s.foodAccess.lastPlan);
                        var nativePath=v.getNavigation().createPath(new BlockPos(362,63,135),1);
                        var terrain=new ArrayList<Map<String,Object>>();
                        if(nativePath!=null)for(int i=0;i<nativePath.getNodeCount();i++){
                            var p=nativePath.getNodePos(i);
                            terrain.add(Map.of("feet",p.toShortString(),"feetBlock",level.getBlockState(p).toString(),"support",level.getBlockState(p.below()).toString(),
                                "sturdy",level.getBlockState(p.below()).isFaceSturdy(level,p.below(),net.minecraft.core.Direction.UP)));
                        }
                        row.put("nativePartialTerrain",terrain);
                        if(route!=null){row.put("digs",route.digCount());row.put("visited",route.visited());row.put("steps",route.steps().stream().map(n->Map.of("feet",n.feet().toShortString(),"dig",n.dig().stream().map(BlockPos::toShortString).toList())).toList());}
                        routes.add(row);
                    }
                    for(var other:bodies)if(other!=v){
                        var row=new LinkedHashMap<String,Object>();row.put("from",s.mind.name());row.put("to",other.getUUID().toString());
                        row.put("distance",v.distanceTo(other));row.put("visible",v.hasLineOfSight(other));row.put("withinHome",VillageSurvival.withinHomeRange(level,s.mind,other.position()));
                        row.put("willingToShare",Speech.willingToShare(s,other.getUUID()));
                        row.put("native0",path(v.getNavigation().createPath(other.blockPosition(),0)));
                        row.put("native1",path(v.getNavigation().createPath(other.blockPosition(),1)));
                        row.put("native2",path(v.getNavigation().createPath(other.blockPosition(),2)));
                        var stands=new ArrayList<Map<String,Object>>();
                        for(var p:BlockPos.betweenClosed(other.blockPosition().offset(-2,-1,-2),other.blockPosition().offset(2,1,2))){
                            if(!BoatRoutes.dry(level,p)||Vec3.atBottomCenterOf(p).distanceToSqr(other.position())>6.25)continue;
                            var candidate=v.getNavigation().createPath(p,0);
                            if(candidate!=null&&candidate.canReach())stands.add(Map.of("stand",p.toShortString(),"path",path(candidate)));
                        }
                        row.put("reachableHandoffStands",stands);routes.add(row);
                    }
                    int found=0;
                    for(var p:BlockPos.betweenClosed(v.blockPosition().offset(-16,-20,-16),v.blockPosition().offset(16,3,16))){
                        if(!level.isLoaded(p))continue;var block=level.getBlockState(p);
                        if(!(block.getBlock() instanceof CropBlock)&&!block.is(Blocks.HAY_BLOCK)&&!block.is(Blocks.CRAFTING_TABLE)
                                &&!block.is(net.minecraft.tags.BlockTags.LOGS)&&!block.is(Blocks.DEAD_BUSH))continue;
                        if(++found>80)break;
                        resources.add(Map.of("actor",v.getUUID().toString(),"position",p.toShortString(),"block",BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString(),
                            "visible",BoatRoutes.visible(level,v,Vec3.atCenterOf(p)),"home",VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(p))));
                    }
                }
                Files.writeString(file,JSON.toJson(Map.of("status","passed","gameTime",time,"ticks",ticks,"actors",actors,"routes",routes,"animals",animals,"resources",resources,
                    "scope","Frozen full-pack copy; native line-of-sight/path queries only. No survival/growth claim, actor movement, food/health mutation or primary-world write.")));
                finished=true;server.tickRateManager().setFrozen(false);server.halt(false);
            }catch(Throwable error){error.printStackTrace();finished=true;try{Files.writeString(file,JSON.toJson(Map.of("status","failed","failure",error.toString())));}catch(Exception ignored){}server.tickRateManager().setFrozen(false);server.halt(false);}
        });
    }
}
