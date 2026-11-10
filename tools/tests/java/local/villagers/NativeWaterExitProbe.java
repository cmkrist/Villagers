package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.npc.villager.Villager;

/** One real initialization tick, then frozen route comparison. Border intervention affects only COPY. */
public final class NativeWaterExitProbe implements ModInitializer {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private int ticks;
    private boolean warming,done;
    private UUID id;
    private long time;
    private final boolean recovery=Boolean.getBoolean("villagers.water.recovery");
    private boolean reachedShore;
    private Map<String,Object> paths(net.minecraft.server.level.ServerLevel level,Villager v){
        v.getNavigation().setCanFloat(true);v.getNavigation().setCanOpenDoors(true);v.getNavigation().getNodeEvaluator().setCanPassDoors(true);
        var available=new ArrayList<Map<String,Object>>();var rejected=new ArrayList<String>();
        var crops=new ArrayList<Map<String,Object>>();
        for(var p:BlockPos.betweenClosed(new BlockPos(-490,61,-326),new BlockPos(-484,69,-318))){
            var block=level.getBlockState(p);
            if(!(block.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop))continue;
            var path=v.getNavigation().createPath(p,1);
            crops.add(Map.of("position",p.toShortString(),"state",block.toString(),"mature",crop.isMaxAge(block),
                "path",path==null?"none":"reachable="+path.canReach()+" nodes="+path.getNodeCount()+" end="+path.getNodePos(path.getNodeCount()-1)));
        }
        for(var p:BlockPos.betweenClosed(v.blockPosition().offset(-8,-2,-8),v.blockPosition().offset(8,4,8))){
            if(!BoatRoutes.dry(level,p))continue;
            var path=v.getNavigation().createPath(p,0);
            if(path!=null&&path.canReach())available.add(Map.of("stand",p.toShortString(),"nodes",path.getNodeCount()));
            else if(rejected.size()<12)rejected.add(p.toShortString());
        }
        return Map.of("borderSize",level.getWorldBorder().getSize(),"position",List.of(v.getX(),v.getY(),v.getZ()),"inWater",v.isInWater(),"feetBlock",level.getBlockState(v.blockPosition()).toString(),"health",v.getHealth(),"reachableDryStands",available,"sampleRejected",rejected,"farmCrops",crops);
    }
    @Override public void onInitialize(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            if(!Files.exists(FabricLoader.getInstance().getGameDir().resolve("food-diagnostic-copy.json")))throw new IllegalStateException("copy marker required");
            time=server.overworld().getGameTime();server.tickRateManager().setFrozen(true);
            for(int x=-33;x<=-25;x++)for(int z=-27;z<=-15;z++){server.overworld().setChunkForced(x,z,true);server.overworld().getChunk(x,z);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(done)return;
            var file=FabricLoader.getInstance().getGameDir().resolve("native-water-exit.json");
            try{
                ticks++;var level=server.overworld();
                if(!warming){
                    if(ticks<40||!Villagers.ready())return;
                    for(var e:level.getAllEntities())if(e instanceof Villager v&&Villagers.isManaged(v)&&v.isAlive()&&level.getFluidState(v.blockPosition()).is(FluidTags.WATER)&&Villagers.session(v,level).mind.foodLevel()==0){id=v.getUUID();break;}
                    if(id==null)throw new IllegalStateException("saved starving swimmer required");
                    var resident=(Villager)level.getEntity(id);
                    // Unspawned native object: validate safety cases without
                    // adding a mob or changing a saved actor/resource.
                    var creeper=new net.minecraft.world.entity.monster.Creeper(net.minecraft.world.entity.EntityTypes.CREEPER,level);
                    if(VillagerController.threatens(creeper,resident))throw new AssertionError("idle creeper is neutral to villager");
                    creeper.setSwellDir(1);
                    if(!VillagerController.threatens(creeper,resident))throw new AssertionError("swelling creeper must interrupt food work");
                    creeper.setSwellDir(-1);creeper.setTarget(resident);
                    if(!VillagerController.threatens(creeper,resident))throw new AssertionError("attacking creeper must interrupt food work");
                    creeper.setTarget(null);creeper.ignite();
                    if(!VillagerController.threatens(creeper,resident))throw new AssertionError("ignited creeper must interrupt food work");
                    warming=true;server.tickRateManager().setFrozen(false);return;
                }
                server.tickRateManager().setFrozen(true);
                if(recovery){
                    var v=(Villager)level.getEntity(id);if(v==null||!v.isAlive())throw new IllegalStateException("saved swimmer must remain alive");
                    var s=Villagers.existingSession(id);if(v.onGround()&&!v.isInWater())reachedShore=true;
                    long elapsed=level.getGameTime()-time;
                    boolean healthStop=v.getHealth()<=1&&s.mind.snapshot().food().timer()>=60;
                    var data=new LinkedHashMap<String,Object>(Map.of("status","passed","stage",s.mind.foodLevel()>0?"swimmer_reached_food":healthStop?"health_stop":"bounded_observation",
                        "elapsed",elapsed,"actor",id.toString(),"food",s.mind.foodLevel(),"health",v.getHealth(),"shoreReached",reachedShore,
                        "position",List.of(v.getX(),v.getY(),v.getZ()),"foodAccess",FoodAccess.status(s),"inventory",v.getInventory().toString()));
                    data.put("urgentCrop",s.urgentCrop==null?"none":s.urgentCrop.toShortString());
                    data.put("harvest",AdventureActions.lastHarvest(id));
                    data.put("ripeBeetroot",level.getBlockState(new BlockPos(-488,64,-325)).toString());
                    data.put("navigation",v.getNavigation().getPath()==null?"none":v.getNavigation().getPath().getEndNode().asBlockPos().toShortString());
                    data.put("air",v.getAirSupply());
                    data.put("starvationTimer",s.mind.snapshot().food().timer());
                    data.put("nearbyMonsters",level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,v.getBoundingBox().inflate(12),m->m.isAlive()&&v.hasLineOfSight(m)).stream().map(m->m.getType()+" "+m.blockPosition()+" distance="+v.distanceTo(m)).toList());
                    if(elapsed%10==0){
                        Files.writeString(file.resolveSibling("native-water-progress.json"),JSON.toJson(data));
                        Files.writeString(file.resolveSibling("native-water-frames.jsonl"),new Gson().toJson(data)+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
                    }
                    if(s.mind.foodLevel()==0&&!healthStop&&elapsed<320){server.tickRateManager().setFrozen(false);return;}
                    Files.writeString(file,JSON.toJson(data));done=true;server.tickRateManager().setFrozen(false);server.halt(false);return;
                }
                if(level.getGameTime()!=time+1)throw new IllegalStateException("exactly one native initialization tick");
                var v=(Villager)level.getEntity(id);var before=paths(level,v);
                level.getWorldBorder().setSize(4096);var expanded=paths(level,v);
                Files.writeString(file,JSON.toJson(Map.of("status","passed","actor",id.toString(),"gameTime",level.getGameTime(),"before",before,"expandedCopyOnly",expanded,"scope","Only COPY border changed. One native initialization tick; then frozen native path queries. No actor/resource/health/food setters and no primary mutation.")));
                done=true;server.tickRateManager().setFrozen(false);server.halt(false);
            }catch(Throwable e){e.printStackTrace();done=true;try{Files.writeString(file,JSON.toJson(Map.of("status","failed","failure",e.toString())));}catch(Exception ignored){}server.tickRateManager().setFrozen(false);server.halt(false);}
        });
    }
}
