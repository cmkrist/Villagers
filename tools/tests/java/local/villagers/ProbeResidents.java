package local.villagers;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;

/** Observation residency follows actual saved managed bodies; never supplies or moves an actor. */
final class ProbeResidents {
    private static boolean watched;
    private static void watchLifecycle(){
        if(watched)return;watched=true;
        var folder=FabricLoader.getInstance().getGameDir();var before=folder.resolve("bodies-before.json");
        if(!Files.exists(before))return;
        try{
            var ids=new java.util.HashSet<java.util.UUID>();
            for(var row:JsonParser.parseString(Files.readString(before)).getAsJsonObject().getAsJsonArray("bodies"))
                ids.add(java.util.UUID.fromString(row.getAsJsonObject().get("id").getAsString()));
            var combatStop=new java.util.concurrent.atomic.AtomicReference<java.util.Map<String,Object>>();
            var recoveryFile=folder.resolve("combat-recovery-scope.json");
            var recovery=Files.exists(recoveryFile)?JsonParser.parseString(Files.readString(recoveryFile)).getAsJsonObject():null;
            var recoveryActor=recovery==null?null:java.util.UUID.fromString(recovery.get("actor").getAsString());
            var recoveryAttacker=recovery==null?null:java.util.UUID.fromString(recovery.get("attacker").getAsString());
            net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register((entity,source,base,taken,blocked)->{
                if((!ids.contains(entity.getUUID())&&!entity.getUUID().equals(recoveryAttacker))||taken<=0)return;var attacker=source.getEntity();
                lifecycle(folder,java.util.Map.of("event","damage","id",entity.getUUID().toString(),"gameTime",entity.level().getGameTime(),
                    "position",entity.position().toString(),"cause",source.getMsgId(),"damage",taken,"healthAfter",entity.getHealth(),
                    "attacker",attacker==null?"":attacker.getUUID().toString(),"attackerType",attacker==null?"":attacker.getType().toString()));
                if(!ids.contains(entity.getUUID()))return; // The known monster is observed; guards protect residents.
                if(Boolean.getBoolean("villagers.boats.rehearse")&&local.villagers.lab.ObservationHealthGuard.combatHit(taken,attacker!=null)){
                    if(entity.getUUID().equals(recoveryActor)&&attacker.getUUID().equals(recoveryAttacker)&&entity.getHealth()>8){
                        lifecycle(folder,java.util.Map.of("event","reviewed_combat_continuation","id",entity.getUUID().toString(),
                            "attacker",attacker.getUUID().toString(),"gameTime",entity.level().getGameTime(),"healthAfter",entity.getHealth(),"damage",taken));
                        return;
                    }
                    var event=java.util.Map.<String,Object>of("status","failed","stage","combat_stop","id",entity.getUUID().toString(),
                        "gameTime",entity.level().getGameTime(),"healthAfter",entity.getHealth(),"damage",taken,"cause",source.getMsgId(),
                        "attacker",attacker.getUUID().toString(),"attackerType",attacker.getType().toString());
                    if(combatStop.compareAndSet(null,event)){
                        try{Files.writeString(folder.resolve("native-combat-stop.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(event));}
                        catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
                    }
                    ((ServerLevel)entity.level()).getServer().tickRateManager().setFrozen(true);
                }
            });
            net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server->{
                if(recoveryActor!=null&&server.overworld().getEntity(recoveryActor) instanceof net.minecraft.world.entity.LivingEntity body&&body.getHealth()<=8){
                    var event=java.util.Map.<String,Object>of("status","failed","stage","reviewed_combat_health_stop","id",body.getUUID().toString(),"healthAfter",body.getHealth(),"gameTime",server.overworld().getGameTime());
                    if(combatStop.compareAndSet(null,event))try{Files.writeString(folder.resolve("native-combat-stop.json"),new com.google.gson.Gson().toJson(event));}
                    catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
                }
                if(combatStop.get()!=null){server.tickRateManager().setFrozen(true);server.halt(false);}
            });
            net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{
                if(!ids.contains(entity.getUUID())&&!entity.getUUID().equals(recoveryAttacker))return;
                var attacker=source.getEntity();
                lifecycle(folder,java.util.Map.of("event","death","id",entity.getUUID().toString(),"gameTime",entity.level().getGameTime(),
                    "position",entity.position().toString(),"cause",source.getMsgId(),"attacker",attacker==null?"":attacker.getUUID().toString(),
                    "attackerType",attacker==null?"":attacker.getType().toString()));
            });
            net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_UNLOAD.register((entity,level)->{
                if(ids.contains(entity.getUUID()))lifecycle(folder,java.util.Map.of("event","unload","id",entity.getUUID().toString(),
                    "gameTime",level.getGameTime(),"position",entity.position().toString(),"reason",String.valueOf(entity.getRemovalReason())));
            });
        }catch(Exception error){throw new IllegalStateException("Cannot observe original body lifecycle",error);}
    }
    private static void lifecycle(java.nio.file.Path folder,java.util.Map<String,Object> event){
        try{Files.writeString(folder.resolve("original-body-lifecycle.jsonl"),new com.google.gson.Gson().toJson(event)+"\n",
            java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);}
        catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
    }
    static void load(ServerLevel level) {
        watchLifecycle();
        var file=FabricLoader.getInstance().getGameDir().resolve("managed-residents-before.json");
        if(!Files.exists(file))return;
        try{
            var bodies=JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonArray("bodies");
            var chunks=new java.util.HashSet<Long>();
            for(var entry:bodies){
                var body=entry.getAsJsonObject();boolean managed=false;
                for(var tag:body.getAsJsonArray("tags"))if(tag.getAsString().equals("villagers:managed"))managed=true;
                if(!managed)continue;
                var p=body.getAsJsonArray("position");int cx=((int)Math.floor(p.get(0).getAsDouble()))>>4,cz=((int)Math.floor(p.get(2).getAsDouble()))>>4;
                for(int x=cx-3;x<=cx+3;x++)for(int z=cz-3;z<=cz+3;z++)chunks.add(net.minecraft.world.level.ChunkPos.pack(x,z));
            }
            for(long packed:chunks){int x=net.minecraft.world.level.ChunkPos.getX(packed),z=net.minecraft.world.level.ChunkPos.getZ(packed);level.setChunkForced(x,z,true);level.getChunk(x,z);}
        }catch(Exception error){throw new IllegalStateException("Cannot load the saved managed residents for observation",error);}
    }
    private ProbeResidents(){}
}
