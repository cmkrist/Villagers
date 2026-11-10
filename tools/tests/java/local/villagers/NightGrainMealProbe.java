package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.npc.villager.Villager;

/** Reopen the actual saved night-starvation case. No fixture supplies, body or time setters. */
public final class NightGrainMealProbe implements ModInitializer {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private int ticks,elapsed;
    private boolean started,done;
    private UUID id;
    private Map<String,Object> before;
    private Path file(String n){return FabricLoader.getInstance().getGameDir().resolve(n);}
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private Map<String,Object> row(Villager v){var s=Villagers.existingSession(v.getUUID());return Map.of("id",v.getUUID().toString(),"food",s.mind.foodLevel(),"health",v.getHealth(),"inventory",v.getInventory().toString(),"sleeping",v.isSleeping(),"position",List.of(v.getX(),v.getY(),v.getZ()),"breadCrafted",s.breadCrafted,"crafting",s.craftingStatus);}
    @Override public void onInitialize(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            check(Files.exists(file("food-diagnostic-copy.json")),"marked copy required");server.tickRateManager().setFrozen(true);
            for(int x=-33;x<=-25;x++)for(int z=-27;z<=-15;z++){server.overworld().setChunkForced(x,z,true);server.overworld().getChunk(x,z);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(done)return;
            try{
                ticks++;var level=server.overworld();
                if(!started){
                    check(ticks<400,"body loading budget");if(ticks<40||!Villagers.ready())return;
                    for(var e:level.getAllEntities())if(e instanceof Villager v&&Villagers.isManaged(v)&&v.isAlive()){
                        var s=Villagers.session(v,level);
                        if(s.mind.foodLevel()==0&&HayForaging.hasHay(v.getInventory())&&VillageSurvival.scheduledRest(level,v)){id=v.getUUID();before=row(v);break;}
                    }
                    check(id!=null,"actual saved starving hay owner during native REST");started=true;server.tickRateManager().setFrozen(false);return;
                }
                elapsed++;var v=(Villager)level.getEntity(id);check(v!=null&&v.isAlive(),"same owner alive");
                var s=Villagers.existingSession(id);boolean ate=s.breadCrafted>=2&&s.mind.foodLevel()>=10;
                if(elapsed%20==0)Files.writeString(file("night-grain-progress.json"),JSON.toJson(row(v)));
                if(!ate&&v.getHealth()>2&&elapsed<600)return;
                check(ate,"saved owner must really craft and eat: "+row(v));
                check(VillageSurvival.scheduledRest(level,v),"still native night rest schedule");
                Files.writeString(file("night-grain-meal.json"),JSON.toJson(Map.of("status","passed","stage","ate_owned_grain_at_night","elapsed",elapsed,"before",before,"after",row(v),"scope","Actual saved starving body, hay, station, night clock and injury in a COPY. No supplies or actor/health/food/time setters. Native wake, movement, recipes and eating.")));
                done=true;server.halt(false);
            }catch(Throwable e){e.printStackTrace();done=true;try{Files.writeString(file("night-grain-meal.json"),JSON.toJson(Map.of("status","failed","failure",e.toString())));}catch(Exception ignored){}server.tickRateManager().setFrozen(false);server.halt(false);}
        });
    }
}
