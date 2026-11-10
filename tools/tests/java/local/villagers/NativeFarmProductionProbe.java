package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.CropBlock;

/** Actual island bodies and planted crops, through normal time and AI in a marked save COPY. */
public final class NativeFarmProductionProbe implements ModInitializer {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private int ticks,elapsed;
    private long time;
    private boolean started,finished;
    private final List<UUID> ids=new ArrayList<>();
    private final List<UUID> hungry=new ArrayList<>();
    private final boolean meal=Boolean.getBoolean("villagers.farm.meal");
    private final boolean grain=Boolean.getBoolean("villagers.farm.grain");
    private final boolean sustain=Boolean.getBoolean("villagers.farm.sustain");
    private final int windowTicks=Integer.getInteger("villagers.farm.windowTicks",14000);
    private final Set<String> receipts=new HashSet<>(),harvestedCells=new HashSet<>();
    private int repeatYields;
    private Path file(String n){return FabricLoader.getInstance().getGameDir().resolve(n);}
    private static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
    @Override public void onInitialize(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            check(Files.exists(file("food-diagnostic-copy.json")),"marked COPY required");
            time=server.overworld().getGameTime();server.tickRateManager().setFrozen(true);
            if(sustain)server.tickRateManager().setTickRate(Integer.getInteger("villagers.farm.tps",20));
            for(int x=-33;x<=-25;x++)for(int z=-27;z<=-15;z++){server.overworld().setChunkForced(x,z,true);server.overworld().getChunk(x,z);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(finished)return;
            try{
                ticks++;var level=server.overworld();
                if(!started){
                    check(ticks<400,"saved body loading budget");check(time==level.getGameTime(),"frozen initial state");
                    for(var e:level.getAllEntities())if(e instanceof Villager v&&v.isAlive()&&Villagers.isManaged(v)&&v.getX()<0&&!ids.contains(v.getUUID()))ids.add(v.getUUID());
                    if(ids.size()<7||ticks<40||!Villagers.ready())return;
                    if(Boolean.getBoolean("villagers.audit.expandedBorder"))NativeExploration.expand(level);
                    if(meal){for(var id:ids){var v=(Villager)level.getEntity(id);var s=Villagers.session(v,level);
                        if(grain?s.mind.foodLevel()<=3&&(BreadCrafting.wheatCount(v.getInventory())>=3||HayForaging.hasHay(v.getInventory())):s.mind.foodLevel()==0)hungry.add(id);
                    }check(!hungry.isEmpty(),"actual saved hungry food worker required");}
                    started=true;server.tickRateManager().setFrozen(false);return;
                }
                elapsed++;if(elapsed%40!=0)return;
                boolean operatorStop=Files.exists(file("stop-observation"));
                var actors=new ArrayList<Map<String,Object>>();boolean healthStop=false,harvested=false,fed=meal;
                for(var id:ids){
                    var v=(Villager)level.getEntity(id);check(v!=null&&v.isAlive(),"same island body alive: "+id);
                    var s=Villagers.existingSession(id);check(s!=null,"loaded mind");
                    var harvest=AdventureActions.activityStatus(id);if(((Number)harvest.get("cropsHarvested")).longValue()>0)harvested=true;
                    if(v.getHealth()<(sustain?2:3)||!sustain&&!meal&&elapsed>=320&&v.getHealth()<8&&s.mind.foodLevel()==0)healthStop=true;
                    if(hungry.contains(id)&&(s.mind.foodLevel()<5||grain&&s.breadCrafted==0))fed=false;
                    var r=new LinkedHashMap<String,Object>();r.put("id",id.toString());r.put("name",s.mind.name());r.put("food",s.mind.foodLevel());r.put("health",v.getHealth());
                    r.put("position",List.of(v.getX(),v.getY(),v.getZ()));r.put("inventory",v.getInventory().toString());r.put("held",v.getMainHandItem().toString());
                    r.put("wood",WoodBootstrap.status(s));r.put("boating",BoatTravel.status(s));r.put("harvest",harvest);
                    r.put("foodAccess",FoodAccess.status(s));r.put("sleeping",v.isSleeping());r.put("crafting",s.craftingStatus);actors.add(r);
                    r.put("breadCrafted",s.breadCrafted);
                    r.put("family",Map.of("status",s.familyStatus,"seekingFood",s.familyFoodGoal,
                        "fatigue",s.mind.fatigue(),"mood",s.mind.mood().band().name(),"nurseries",VillageFamilies.nurseries(level,v).stream().map(BlockPos::toShortString).toList()));
                    var receipt=AdventureActions.lastHarvest(id);r.put("harvestReceipt",receipt);
                    if(!receipt.isEmpty()&&receipts.add(id+":"+receipt.get("gameTime")))
                        if(!harvestedCells.add(receipt.get("position").toString()))repeatYields++;
                }
                var newActors=new ArrayList<Map<String,Object>>();
                for(var e:level.getAllEntities())if(e instanceof Villager v&&Villagers.isManaged(v)){
                    if(v.getHealth()<(sustain?2:3))healthStop=true;
                    if(v.getX()<0&&!ids.contains(v.getUUID()))newActors.add(Map.of("id",v.getUUID().toString(),"baby",v.isBaby(),"health",v.getHealth()));
                }
                int planted=0,mature=0,totalAge=0;
                for(var p:BlockPos.betweenClosed(new BlockPos(-490,61,-326),new BlockPos(-484,69,-318))){
                    var state=level.getBlockState(p);if(state.getBlock() instanceof CropBlock crop){planted++;totalAge+=crop.getAge(state);if(crop.isMaxAge(state))mature++;}
                }
                var data=new LinkedHashMap<String,Object>();data.put("status","passed");data.put("stage",healthStop?"health_stop":operatorStop?"operator_stop":sustain?(elapsed>=windowTicks?"native_food_window_complete":"bounded_food_window"):fed?"saved_harvester_fed":!meal&&harvested?"native_harvest_observed":"bounded_observation");
                data.put("elapsed",elapsed);data.put("initialGameTime",time);data.put("gameTime",level.getGameTime());data.put("actors",actors);
                data.put("farm",Map.of("planted",planted,"mature",mature,"totalAge",totalAge,"rawBrightness",level.getRawBrightness(new BlockPos(-487,65,-322),0)));
                data.put("harvestReceipts",receipts.size());data.put("repeatYields",repeatYields);data.put("newManagedActors",newActors);
                data.put("targetTps",server.tickRateManager().tickrate());
                data.put("copyBorderSize",level.getWorldBorder().getSize());
                data.put("adventure",actors.stream().map(a->{var v=(Villager)level.getEntity(UUID.fromString(a.get("id").toString()));var s=Villagers.existingSession(v.getUUID());return Map.of("id",v.getUUID().toString(),"status",s.adventureStatus,"mining",AdventureMining.status(s));}).toList());
                data.put("scope","Actual copied island residents and native crops; normal clock, random growth and controller work only. No actor, food, crop, inventory or resource setters. A first harvest alone is not sustained food or birth proof.");
                Files.writeString(file("native-farm-progress.json"),JSON.toJson(data));
                Files.writeString(file("native-farm-frames.jsonl"),new Gson().toJson(data)+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
                if((sustain||!(meal?fed:harvested))&&!healthStop&&!operatorStop&&elapsed<(meal?2000:sustain?windowTicks:14000))return;
                Files.writeString(file("native-farm-production.json"),JSON.toJson(data));finished=true;server.halt(false);
            }catch(Throwable e){e.printStackTrace();finished=true;try{Files.writeString(file("native-farm-production.json"),JSON.toJson(Map.of("status","failed","failure",e.toString())));}catch(Exception ignored){}server.tickRateManager().setFrozen(false);server.halt(false);}
        });
    }
}
