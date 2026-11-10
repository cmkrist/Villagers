package local.villagers;

import com.google.gson.*;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import local.villagers.evidence.EvidenceWriter;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

/** World-scoped campaign history. Facts come from native movement, damage and death events. */
public final class WarCampaign {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static JsonObject data;
    private static Path file;
    private static EvidenceWriter writer;
    private WarCampaign() {}
    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            data=null;file=null;writer=null;WarCombat.clear();
            if(!(server.overworld().getChunkSource().getGenerator() instanceof local.villagers.war.WarChunkGenerator))return;
            file=server.getWorldPath(LevelResource.ROOT).resolve("war-campaign.json");
            try {
                if(Files.exists(file)) {
                    data=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                    if(data.get("schema").getAsInt()!=1)throw new IllegalStateException("Unsupported war campaign schema");
                } else {
                    data=new JsonObject();data.addProperty("schema",1);
                    data.add("casualties",new JsonObject());data.add("hits",new JsonObject());
                    data.add("damage",new JsonObject());data.add("marches",new JsonObject());data.add("recentEvents",new JsonArray());
                }
                writer=new EvidenceWriter("villagers-war-save",8,error->Villagers.LOG.error("Campaign persistence failed; warfare stopped",error));
            } catch(Exception failure) {data=null;Villagers.LOG.error("Campaign could not load; warfare stopped to preserve saved history",failure);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(ready(server.overworld()) && server.getTickCount()%200==0)save(server.overworld());
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity,source,base,damage,blocked)->{
            if(damage<=0 || !(entity.level() instanceof ServerLevel level) || !ready(level)
                || !(entity instanceof Mob defender) || !(source.getEntity() instanceof Mob attacker)
                || !WarCombat.enemies(attacker,defender))return;
            String faction=WarCombat.faction(attacker).name();
            increment("hits",faction,1);increment("damage",faction,damage);
            event(level,"hit",attacker.getUUID(),defender.getUUID(),source.getMsgId()+":"+damage);
            var mind=attacker instanceof net.minecraft.world.entity.npc.villager.Villager v
                ? (Villagers.existingSession(v.getUUID())==null?null:Villagers.existingSession(v.getUUID()).mind)
                : CreatureAgents.mind(attacker);
            if(mind!=null)mind.remember("Hit enemy "+defender.getUUID()+" for "+damage);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{
            if(!(entity.level() instanceof ServerLevel level) || !ready(level) || !entity.entityTags().contains(WarScenario.ACTOR))return;
            JsonObject death=new JsonObject();death.addProperty("tick",level.getGameTime());
            death.addProperty("cause",source.getMsgId());
            death.addProperty("faction",entity instanceof Mob mob?WarCombat.faction(mob).name():"NEUTRAL");
            if(source.getEntity()!=null)death.addProperty("killer",source.getEntity().getUUID().toString());
            if(source.getEntity() instanceof Mob killer)death.addProperty("killerFaction",WarCombat.faction(killer).name());
            data.getAsJsonObject("casualties").add(entity.getUUID().toString(),death);
            event(level,"death",entity.getUUID(),source.getEntity()==null?null:source.getEntity().getUUID(),source.getMsgId());
            save(level);
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity,level)->WarCombat.forget(entity.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{
            if(writer==null)return;
            save(server.overworld());writer.shutdown();
            try {if(!writer.awaitTermination(15,TimeUnit.SECONDS))Villagers.LOG.error("Campaign save timed out");}
            catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
            writer=null;data=null;WarCombat.clear();
        });
    }
    public static boolean ready(ServerLevel level) {return data!=null && writer!=null && !writer.failed() && WarScenario.active(level);}
    static void nominated(ServerLevel level, net.minecraft.world.entity.npc.villager.Villager v) {
        if (!ready(level)) return;
        var objective = new JsonObject(); objective.addProperty("villager", v.getUUID().toString());
        objective.addProperty("nominatedAt", level.getGameTime()); objective.addProperty("status", "preparing");
        data.add("adventureObjective", objective);
        event(level, "adventure_nomination", v.getUUID(), null, "Prepare to defeat the Ender Dragon"); save(level);
    }
    /** Authored campaign director's garrison census; this never supplies individual combat targets. */
    static int adultResidents(ServerLevel level,local.villagers.war.WarMap.Settlement town) {
        int index=local.villagers.war.WarMap.SETTLEMENTS.indexOf(town), count=0;
        for (var entity:level.getAllEntities()) if (entity instanceof Mob mob && mob.isAlive()
            && WarCombat.faction(mob)==town.faction() && mob.entityTags().contains(WarScenario.HOME+index)
            && !(mob instanceof net.minecraft.world.entity.npc.villager.Villager v && v.isBaby())
            && !IllagerFamilies.juvenile(mob)) count++;
        return count;
    }
    static boolean occupied(ServerLevel level,local.villagers.war.WarMap.Settlement town) {
        int index=local.villagers.war.WarMap.SETTLEMENTS.indexOf(town);
        for(var entity:level.getAllEntities())if(entity instanceof Mob mob && mob.isAlive()
            && WarCombat.faction(mob)==town.faction() && mob.entityTags().contains(WarScenario.HOME+index))return true;
        return false;
    }
    static void marched(ServerLevel level,Mob mob) {
        if(!ready(level))return;
        increment("marches",WarCombat.faction(mob).name(),1);
        event(level,"marched",mob.getUUID(),null,"Left home territory on foot");
    }
    private static void increment(String group,String key,double amount) {
        JsonObject values=data.getAsJsonObject(group);
        values.addProperty(key,(values.has(key)?values.get(key).getAsDouble():0)+amount);
    }
    private static void event(ServerLevel level,String kind,UUID actor,UUID subject,String detail) {
        JsonObject event=new JsonObject();event.addProperty("tick",level.getGameTime());event.addProperty("kind",kind);
        event.addProperty("actor",actor.toString());if(subject!=null)event.addProperty("subject",subject.toString());
        event.addProperty("detail",detail);
        JsonArray events=data.getAsJsonArray("recentEvents");if(events.size()>=96)events.remove(0);events.add(event);
    }
    public static JsonObject snapshot(ServerLevel level) {
        if(data==null)return new JsonObject();
        JsonObject copy=data.deepCopy();copy.addProperty("gameTime",level.getGameTime());
        JsonObject alive=new JsonObject();
        for(var faction:local.villagers.war.WarMap.Faction.values())alive.addProperty(faction.name(),0);
        for(var entity:level.getAllEntities())if(entity instanceof Mob mob && mob.isAlive() && mob.entityTags().contains(WarScenario.ACTOR)) {
            String faction=WarCombat.faction(mob).name();alive.addProperty(faction,alive.get(faction).getAsInt()+1);
        }
        copy.add("aliveLoaded",alive);
        JsonObject towns = new JsonObject();
        for (var town : local.villagers.war.WarMap.SETTLEMENTS) towns.addProperty(town.name(), adultResidents(level, town));
        copy.add("adultTownPopulations", towns); return copy;
    }
    private static void save(ServerLevel level) {
        if(data==null || writer==null)return;
        String contents=JSON.toJson(snapshot(level));Path destination=file;
        writer.execute(()->{
            try {
                Path temp=destination.resolveSibling(destination.getFileName()+".tmp");
                Files.writeString(temp,contents);Files.move(temp,destination,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            } catch(java.io.IOException failure){throw new UncheckedIOException(failure);}
        });
    }
}
