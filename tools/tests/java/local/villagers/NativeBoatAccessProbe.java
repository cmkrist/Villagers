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
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;

/** Actual saved island residents in a marked COPY. Never creates bodies, resources or routes. */
public final class NativeBoatAccessProbe implements ModInitializer {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private final boolean rehearse=Boolean.getBoolean("villagers.boats.rehearse");
    private final boolean expanded=Boolean.getBoolean("villagers.audit.expandedBorder");
    private int ticks, elapsed;
    private long initialTime;
    private boolean started, finished;
    private final List<UUID> ids=new ArrayList<>();
    private final List<Map<String,Object>> initial=new ArrayList<>();
    private final Map<UUID,BlockPos> returningHomes=new HashMap<>();
    private Path output(String name){return FabricLoader.getInstance().getGameDir().resolve(name);}
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private Map<String,Object> actor(ServerLevel level,Villager v,boolean inspect){
        var s=Villagers.session(v,level);var row=new LinkedHashMap<String,Object>();
        row.put("id",v.getUUID().toString());row.put("name",s.mind.name());row.put("position",List.of(v.getX(),v.getY(),v.getZ()));
        row.put("health",v.getHealth());row.put("food",s.mind.foodLevel());row.put("inventory",v.getInventory().toString());
        row.put("held",v.getMainHandItem().toString());row.put("boat",BoatTravel.status(s));row.put("tags",v.entityTags());
        row.put("woodJob",Map.of("tree",String.valueOf(s.boatTree),"taken",s.boatLogsTaken,"table",String.valueOf(s.craftingTable),"blockedTable",String.valueOf(s.blockedCraftTable)));
        if(inspect){
            v.getNavigation().setCanOpenDoors(true);v.getNavigation().getNodeEvaluator().setCanPassDoors(true);v.getNavigation().setCanFloat(true);
            var station=BreadCrafting.table(level,v,s);
            if(station!=null){
                var stands=new ArrayList<BlockPos>();var routes=new ArrayList<Map<String,Object>>();
                for(var p:BlockPos.betweenClosed(station.offset(-2,-1,-2),station.offset(2,1,2))){
                    if(!level.isLoaded(p)||!level.getBlockState(p).isAir()||!level.getBlockState(p.above()).isAir()
                        ||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),net.minecraft.core.Direction.UP))continue;
                    var eye=Vec3.atBottomCenterOf(p).add(0,v.getEyeHeight(),0);
                    var sight=level.clip(new net.minecraft.world.level.ClipContext(eye,Vec3.atCenterOf(station),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,v));
                    if(eye.distanceToSqr(Vec3.atCenterOf(station))<=9&&(sight.getType()==net.minecraft.world.phys.HitResult.Type.MISS||sight.getBlockPos().equals(station)))stands.add(p.immutable());
                }
                stands.sort(Comparator.comparingDouble(p->v.position().distanceToSqr(Vec3.atBottomCenterOf(p))));
                for(var p:stands.stream().limit(3).toList()){
                    var ordinary=v.getNavigation().createPath(p,0);
                    var range64=v.getNavigation().createPath(p,0,64);
                    net.minecraft.world.level.pathfinder.Path budget64;
                    try{v.getNavigation().setMaxVisitedNodesMultiplier(2);budget64=v.getNavigation().createPath(p,0,64);}
                    finally{v.getNavigation().resetMaxVisitedNodesMultiplier();}
                    var waterType=net.minecraft.world.level.pathfinder.PathType.WATER;
                    float old=v.getPathfindingMalus(waterType);
                    net.minecraft.world.level.pathfinder.Path dry;
                    try{v.setPathfindingMalus(waterType,-1);dry=v.getNavigation().createPath(p,0);}
                    finally{v.setPathfindingMalus(waterType,old);}
                    var data=new LinkedHashMap<String,Object>();data.put("stand",p.toShortString());
                    data.put("ordinary",routeSummary(level,v,s,ordinary));data.put("waterExcluded",routeSummary(level,v,s,dry));
                    data.put("range64",routeSummary(level,v,s,range64));data.put("range64Budget2",routeSummary(level,v,s,budget64));routes.add(data);
                }
                row.put("station",station.toShortString());row.put("stationRoutes",routes);
                row.put("followRange",v.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE));
            }
            if(s.mind.foodLevel()<=6){
                var food=FoodAccess.preview(level,v);
                row.put("foodPlan",food==null?Map.of("found",false,"search",s.foodAccess.lastPlan):Map.of("found",true,"store",food.store().toShortString(),"table",food.table().toShortString(),"steps",food.route().steps().size()));
            }
            v.getNavigation().setCanOpenDoors(true);v.getNavigation().getNodeEvaluator().setCanPassDoors(true);v.getNavigation().setCanFloat(true);
            var trees=new ArrayList<String>();var tables=new ArrayList<Map<String,Object>>();var crops=new ArrayList<String>();
            var origin=v.blockPosition();
            for(var p:BlockPos.betweenClosed(origin.offset(-16,-3,-16),origin.offset(16,3,16))){
                if(!level.isLoaded(p))continue;
                if(BoatPreparation.naturalTrunk(level,p))trees.add(p.toShortString());
                if(level.getBlockState(p).is(Blocks.CRAFTING_TABLE))tables.add(Map.of("position",p.toShortString(),"visible",BoatRoutes.visible(level,v,Vec3.atCenterOf(p))));
                var block=level.getBlockState(p);if(block.getBlock() instanceof CropBlock crop&&crop.isMaxAge(block))crops.add(p.toShortString());
            }
            row.put("trees",trees);row.put("tables",tables);row.put("ripeCrops",crops);
            var meeting=v.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.MEETING_POINT);
            if(meeting.isPresent()){
                var bell=meeting.get().pos();var fields=new ArrayList<Map<String,Object>>();
                for(var p:BlockPos.betweenClosed(bell.offset(-16,-3,-16),bell.offset(16,3,16))){
                    if(!level.isLoaded(p)||!(level.getBlockState(p).getBlock() instanceof CropBlock crop))continue;
                    var path=v.getNavigation().createPath(p,1);boolean dry=path!=null&&path.canReach();
                    if(dry)for(int i=0;i<path.getNodeCount();i++)if(!level.getFluidState(path.getNodePos(i)).isEmpty()){dry=false;break;}
                    fields.add(Map.of("position",p.toShortString(),"mature",crop.isMaxAge(level.getBlockState(p)),"visible",BoatRoutes.visible(level,v,Vec3.atCenterOf(p)),"nativeDryRoute",dry,"boundedRoute",dry&&WorkNavigation.staysHome(level,v,s,path)));
                }
                row.put("meetingPoint",bell.toShortString());row.put("fields",fields);
                var villageTrees=new ArrayList<Map<String,Object>>();
                for(var p:BlockPos.betweenClosed(bell.offset(-48,-6,-48),bell.offset(48,6,48))){
                    if(!BoatPreparation.naturalTrunk(level,p)||!NativeVillageWorkArea.contains(level,s.mind,Vec3.atCenterOf(p)))continue;
                    var path=WoodBootstrap.approachPath(level,v,s,p);boolean dry=path!=null&&path.canReach();
                    if(dry)for(int i=0;i<path.getNodeCount();i++)if(!level.getFluidState(path.getNodePos(i)).isEmpty()){dry=false;break;}
                    villageTrees.add(Map.of("root",p.toShortString(),"boundedApproach",path!=null&&path.canReach(),"dryApproach",dry));
                }
                row.put("villageTrees",villageTrees);
                var stones=new ArrayList<Map<String,Object>>();
                for(var p:BlockPos.betweenClosed(bell.offset(-48,-8,-48),bell.offset(48,8,48))){
                    if(!level.isLoaded(p)||!NativeVillageWorkArea.contains(level,s.mind,Vec3.atCenterOf(p)))continue;
                    var state=level.getBlockState(p);
                    if(!(state.is(Blocks.STONE)||state.is(Blocks.DEEPSLATE))||!level.getBlockState(p.above()).isAir()
                        ||level.structureManager().getStructureWithPieceAt(p,h->true).isValid())continue;
                    stones.add(Map.of("position",p.toShortString(),"eligible",AdventureMining.surface(level,v,s,p),"visible",BoatRoutes.visible(level,v,Vec3.atCenterOf(p))));
                    if(stones.size()>=40)break;
                }
                row.put("surfaceStone",stones);
            }
            var docks=new ArrayList<Map<String,Object>>();
            for(var dock:BoatRoutes.launches(level,v).stream().limit(3).toList()){
                var path=v.getNavigation().createPath(dock.land(),0);
                docks.add(Map.of("land",dock.land().toShortString(),"water",dock.water().toShortString(),"reachable",path!=null&&path.canReach()));
            }
            row.put("launches",docks);var route=BoatRoutes.find(level,v,null);
            row.put("observedCrossing",route==null?Map.of("found",false):Map.of("found",true,"launch",route.launch().land().toShortString(),"arrival",route.arrival().land().toShortString(),"points",route.points().size()));
        }
        return row;
    }
    private Map<String,Object> routeSummary(ServerLevel level,Villager v,Session s,net.minecraft.world.level.pathfinder.Path path){
        if(path==null)return Map.of("exists",false);
        var wet=new ArrayList<String>();for(int i=0;i<path.getNodeCount();i++)if(!level.getFluidState(path.getNodePos(i)).isEmpty())wet.add(path.getNodePos(i).toShortString());
        return Map.of("exists",true,"canReach",path.canReach(),"bounded",WorkNavigation.staysHome(level,v,s,path),"nodes",path.getNodeCount(),"wet",wet,"end",path.getEndNode().asBlockPos().toShortString());
    }
    @Override public void onInitialize(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            check(Files.exists(output("food-diagnostic-copy.json")),"verified copy marker required");
            initialTime=server.overworld().getGameTime();server.tickRateManager().setFrozen(true);
            for(int x=expanded?-35:-33;x<=-25;x++)for(int z=-27;z<=-15;z++){server.overworld().setChunkForced(x,z,true);server.overworld().getChunk(x,z);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(finished)return;
            try{
                ticks++;var level=server.overworld();
                if(!started){
                    check(ticks<400,"copy entity loading budget");check(level.getGameTime()==initialTime,"no simulation before initial snapshot");
                    var bodies=new ArrayList<Villager>();
                    for(var e:level.getAllEntities())if(e instanceof Villager v&&v.isAlive()&&Villagers.isManaged(v)&&v.getX()<0)bodies.add(v);
                    if(bodies.size()<7||ticks<40||!Villagers.ready())return;
                    if(expanded){
                        NativeExploration.expand(level);
                        var marker=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("native-exploration.json");
                        var before=Files.readString(marker);NativeExploration.expand(level);
                        check(before.equals(Files.readString(marker)),"exploration configuration must be idempotent");
                        check(level.getWorldBorder().getSize()==8192,"native exploration border");
                    }
                    for(var v:bodies){
                        ids.add(v.getUUID());initial.add(actor(level,v,true));
                        for(var tag:v.entityTags())if(tag.startsWith("villagers:sea:RETURN:")){
                            var p=tag.substring("villagers:sea:RETURN:".length()).split(":")[0].split(",");
                            returningHomes.put(v.getUUID(),new BlockPos(Integer.parseInt(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2])));
                        }
                    }
                    started=true;
                    if(rehearse){Files.writeString(output("native-boat-initial.json"),JSON.toJson(initial));server.tickRateManager().setFrozen(false);return;}
                }
                var current=new ArrayList<Map<String,Object>>();boolean returned=false,returnedWithFood=false;
                for(var id:ids){
                    var v=(Villager)level.getEntity(id);check(v!=null&&v.isAlive(),"existing island resident remains alive: "+id);
                    current.add(actor(level,v,false));var s=Villagers.existingSession(id);
                    if(rehearse)check(v.getHealth()>=8,"copy health stop: "+s.mind.name());
                    boolean coldReturn=returningHomes.containsKey(id)&&s.boatLandings>=1
                        &&v.position().distanceToSqr(Vec3.atBottomCenterOf(returningHomes.get(id)))<4;
                    if((s.boatLandings>=2||coldReturn)&&s.boatTrip==null&&!v.isPassenger()&&BoatRoutes.dry(level,v.blockPosition())){
                        returned=true;
                        if(FoodInventory.anySlot(v.getInventory(),AdventureActions.canHarvest(v))>=0)returnedWithFood=true;
                    }
                }
                if(rehearse){
                    elapsed++;
                    if(elapsed%40==0)Files.writeString(output("native-boat-progress.json"),JSON.toJson(Map.of("elapsed",elapsed,"actors",current)));
                    if(!returned&&elapsed<2400)return;
                }
                Files.writeString(output("native-boat-access.json"),JSON.toJson(Map.of("status","passed","stage",rehearse?(returned?(returnedWithFood?"returned_with_food":"returned_without_food"):"bounded_observation"):"frozen_routes", "initialGameTime",initialTime,"gameTime",level.getGameTime(),"elapsed",elapsed,"initial",initial,"actors",current,"copyBorderSize",level.getWorldBorder().getSize(),
                    "scope","Actual saved island residents in a copy; no actor/resources/inventory/health setters. Primary unchanged. Bounded observation alone is not food-voyage proof.")));
                finished=true;server.tickRateManager().setFrozen(false);server.halt(false);
            }catch(Throwable error){error.printStackTrace();finished=true;try{Files.writeString(output("native-boat-access.json"),JSON.toJson(Map.of("status","failed","failure",error.toString())));}catch(Exception ignored){}server.tickRateManager().setFrozen(false);server.halt(false);}
        });
    }
}
