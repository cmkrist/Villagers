package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

/** Normal food AI in a verified full-pack copy, using the actual saved hungry bodies and supplies. */
public final class FoodAccessRehearsalProbe implements ModInitializer {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private int ticks,elapsed;
    private long time;
    private boolean started,finished;
    private final List<UUID> ids=new ArrayList<>(),hungry=new ArrayList<>();
    private final Set<UUID> critical=new HashSet<>();
    private final boolean healing=Boolean.getBoolean("villagers.food.healing");
    private final boolean cycle=Boolean.getBoolean("villagers.food.cycle");
    private final Map<UUID,Float> minimumHealth=new HashMap<>();
    private final List<Map<String,Object>> initial=new ArrayList<>();
    private Path output(String name){return FabricLoader.getInstance().getGameDir().resolve(name);}
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private Map<String,Object> row(ServerLevel level,Villager v){
        var s=Villagers.session(v,level);var r=new LinkedHashMap<String,Object>();
        r.put("id",v.getUUID().toString());r.put("name",s.mind.name());r.put("position",List.of(v.getX(),v.getY(),v.getZ()));
        r.put("health",v.getHealth());r.put("food",s.mind.foodLevel());r.put("inventory",v.getInventory().toString());
        minimumHealth.merge(v.getUUID(),v.getHealth(),Math::min);r.put("minimumHealth",minimumHealth.get(v.getUUID()));
        r.put("activity",v.getName().getString());r.put("onGround",v.onGround());r.put("fallDistance",v.fallDistance);
        r.put("velocity",List.of(v.getDeltaMovement().x,v.getDeltaMovement().y,v.getDeltaMovement().z));
        r.put("recoveryStand",s.recoveryStand==null?"":s.recoveryStand.toShortString());
        r.put("foodAccess",FoodAccess.status(s));r.put("breadCrafted",s.breadCrafted);r.put("crafting",s.craftingStatus);
        r.put("craftingTable",s.craftingTable==null?"":s.craftingTable.toShortString());
        r.put("blockedTable",s.blockedCraftTable==null?"":s.blockedCraftTable.toShortString());
        r.put("woodPreparation",WoodBootstrap.status(s));
        r.put("starvationTimer",s.mind.snapshot().food().timer());r.put("foodHelp",s.foodHelpStatus);r.put("foodRequests",s.foodHelpRequests);r.put("lastFoodReceived",s.lastFoodReceived);r.put("lastFoodGiven",s.lastFoodGiven);
        r.put("memories",s.mind.memories().stream().filter(m->m.note()!=null&&(m.note().contains("hay")||m.note().contains("Ate owned")||m.note().contains("bread"))).skip(Math.max(0,s.mind.memories().stream().filter(m->m.note()!=null&&(m.note().contains("hay")||m.note().contains("Ate owned")||m.note().contains("bread"))).count()-12)).toList());
        return r;
    }
    @Override public void onInitialize(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            check(Files.exists(output("food-diagnostic-copy.json")),"copy marker required");
            check(Villagers.config().hayForaging(),"real hay foraging enabled in COPY only");
            time=server.overworld().getGameTime();server.tickRateManager().setFrozen(true);
            if(healing||cycle)server.tickRateManager().setTickRate(Integer.getInteger("villagers.farm.tps",20));
            for(int x=19;x<=26;x++)for(int z=5;z<=13;z++){server.overworld().setChunkForced(x,z,true);server.overworld().getChunk(x,z);}
            ProbeResidents.load(server.overworld());
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(finished)return;
            try{
                ticks++;var level=server.overworld();
                if(!started){
                    check(ticks<400,"initial saved body loading budget");check(level.getGameTime()==time,"no simulation before snapshot");
                    var bodies=new ArrayList<Villager>();
                    for(var e:level.getAllEntities())if(e instanceof Villager v&&v.isAlive()&&Villagers.isManaged(v)&&v.getX()>300&&v.getX()<420)bodies.add(v);
                    if(bodies.size()<5||ticks<40||!Villagers.ready())return;
                    for(var v:bodies){ids.add(v.getUUID());initial.add(row(level,v));if(cycle||(healing?v.getHealth()<.9*v.getMaxHealth():Villagers.existingSession(v.getUUID()).mind.foodLevel()<=4))hungry.add(v.getUUID());if(v.getHealth()<=3&&Villagers.existingSession(v.getUUID()).mind.foodLevel()==0)critical.add(v.getUUID());}
                    check(!hungry.isEmpty()&&(cycle||hungry.size()<=2),"existing saved survivors required; no replacement bodies");
                    Files.writeString(output("food-access-initial.json"),JSON.toJson(initial));started=true;server.tickRateManager().setFrozen(false);return;
                }
                elapsed++;var actors=new ArrayList<Map<String,Object>>();boolean fed=true,healthStop=false;
                int loaded=0;var residentHealth=new ArrayList<Map<String,Object>>();var residentDetails=new ArrayList<Map<String,Object>>();
                for(var entity:level.getAllEntities())if(entity instanceof Villager v&&Villagers.isManaged(v)&&v.isAlive()){
                    loaded++;var resident=Villagers.existingSession(v.getUUID());
                    residentHealth.add(Map.of("id",v.getUUID().toString(),"health",v.getHealth(),"food",resident==null?-1:resident.mind.foodLevel()));
                    if(Files.exists(output("managed-residents-before.json")))residentDetails.add(row(level,v));
                    if(Files.exists(output("managed-residents-before.json"))&&v.getHealth()<=4)healthStop=true;
                }
                for(var id:ids){
                    var v=(Villager)level.getEntity(id);check(v!=null&&v.isAlive(),"original saved survivor alive: "+id);
                    var s=Villagers.existingSession(id);actors.add(row(level,v));
                    if(cycle?v.getHealth()<=4:healing||critical.contains(id)?v.getHealth()<=1&&s.mind.foodLevel()==0&&s.mind.snapshot().food().timer()>=60:v.getHealth()<3)healthStop=true;
                    if(hungry.contains(id)&&(healing?v.getHealth()<.9*v.getMaxHealth()||s.mind.foodLevel()<18:s.mind.foodLevel()<(critical.contains(id)?1:12)))fed=false;
                }
                if(elapsed%40==0)Files.writeString(output("food-access-progress.json"),JSON.toJson(Map.of("elapsed",elapsed,"actors",actors,"residentHealth",residentHealth,"residentDetails",residentDetails)));
                if((cycle||!fed)&&!healthStop&&elapsed<(cycle?24000:healing?8000:4000))return;
                var report=new LinkedHashMap<String,Object>(Map.of("status","passed","stage",healthStop?"health_stop":cycle?"food_cycle_window_complete":fed?(hungry.size()==2?"both_hungry_survivors_fed":"remaining_hungry_survivor_fed"):"bounded_observation","hungry",hungry,"initialGameTime",time,"gameTime",level.getGameTime(),"elapsed",elapsed,"initial",initial,"actors",actors,
                    "scope","Actual saved survivors in a COPY; only enables existing hay-foraging option. No actor, food, health, resource or inventory setters; normal native controller work. A completed observation window alone does not prove food production or a passage.","loadedManagedVillagers",loaded));
                report.put("residentHealth",residentHealth);report.put("residentDetails",residentDetails);Files.writeString(output("food-access-rehearsal.json"),JSON.toJson(report));
                finished=true;server.halt(false);
            }catch(Throwable e){e.printStackTrace();finished=true;try{Files.writeString(output("food-access-rehearsal.json"),JSON.toJson(Map.of("status","failed","failure",e.toString())));}catch(Exception ignored){}server.tickRateManager().setFrozen(false);server.halt(false);}
        });
    }
}
