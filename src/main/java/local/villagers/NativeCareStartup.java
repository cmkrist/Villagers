package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.npc.villager.Villager;

/** Explicit bounded-observation preflight. Freeze time, verify bodies, enroll, await explicit resume. */
final class NativeCareStartup {
    record Scope(UUID resident,UUID worker,long gameTime,String workerBody,float residentHealth,List<Double> residentPosition){
        Scope {
            Objects.requireNonNull(resident);Objects.requireNonNull(worker);Objects.requireNonNull(workerBody);
            residentPosition=List.copyOf(residentPosition);
            if(resident.equals(worker)||gameTime<0||workerBody.isBlank()||!workerBody.startsWith(worker+":"))throw new IllegalArgumentException("Invalid startup identity");
            if(!Float.isFinite(residentHealth)||residentHealth<=0||residentPosition.size()!=3||residentPosition.stream().anyMatch(v->!Double.isFinite(v)))throw new IllegalArgumentException("Invalid saved resident body");
        }
        void requireTime(long actual){if(actual!=gameTime)throw new IllegalStateException("Startup game time differs from the reviewed checkpoint");}
    }
    private static MinecraftServer owner;
    private static Scope scope;
    private static Path receipt;
    private static boolean ready,resumed,failed;
    private static int frames;
    private static Map<String,Object> adoption=Map.of();
    private static String before="",after="";
    static void initialize(){
        String property=System.getProperty("villagers.lab.careStartupScope","");if(property.isEmpty())return;
        if(!DevMode.enabled()||!"native-regions".equals(System.getProperty("villagers.trial"))
            ||Integer.getInteger("villagers.lab.maxPlayTicks",0)<=0||Integer.getInteger("villagers.lab.minHealth",0)!=1
            ||!Boolean.getBoolean("villagers.lab.lastHealthRecovery"))throw new IllegalStateException("Care startup requires bounded native observation with final-health recovery");
        try{
            Path input=Path.of(property).toRealPath();var json=JsonParser.parseString(Files.readString(input)).getAsJsonObject();
            if(json.get("schema").getAsInt()!=1)throw new IllegalArgumentException("Unknown startup scope schema");
            scope=new Scope(UUID.fromString(json.get("resident").getAsString()),UUID.fromString(json.get("worker").getAsString()),
                json.get("gameTime").getAsLong(),json.get("workerBody").getAsString(),json.get("residentHealth").getAsFloat(),
                java.util.stream.StreamSupport.stream(json.getAsJsonArray("residentPosition").spliterator(),false).map(JsonElement::getAsDouble).toList());
            receipt=input.resolveSibling("native-care-startup.json");
        }catch(Exception error){throw new IllegalStateException("Cannot load explicit care startup scope",error);}
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            owner=server;ready=resumed=failed=false;frames=0;adoption=Map.of();before=after="";
            server.tickRateManager().setFrozen(true);
            try{
                if(!DevMode.inWorld(server,"NativeFourRegions")||!NativeRegionsScenario.nativeRegions(server.overworld()))throw new IllegalStateException("Existing native world required");
                scope.requireTime(server.overworld().getGameTime());write("frozen","");
            }catch(Exception error){fail(error);}
        });
        ServerTickEvents.START_SERVER_TICK.register(server->{if(waiting(server))server.tickRateManager().setFrozen(true);});
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(server!=owner||ready||failed||resumed)return;
            try{
                scope.requireTime(server.overworld().getGameTime());
                if(++frames>400)throw new IllegalStateException("Saved startup bodies did not load within the frozen observation budget");
                var level=server.overworld();
                if(!Villagers.ready()||!(level.getEntity(scope.resident) instanceof Villager resident)
                    ||!(level.getEntity(scope.worker) instanceof Pillager worker))return;
                if(!resident.isAlive()||Math.abs(resident.getHealth()-scope.residentHealth)>.0001)throw new IllegalStateException("Resident health differs from reviewed checkpoint");
                for(int i=0;i<3;i++)if(Math.abs((i==0?resident.getX():i==1?resident.getY():resident.getZ())-scope.residentPosition.get(i))>.000001)
                    throw new IllegalStateException("Resident position differs from reviewed checkpoint");
                before=NativeIllagers.workerBody(worker);
                if(!before.equals(scope.workerBody))throw new IllegalStateException("Worker body or stock differs from reviewed checkpoint");
                Villagers.session(resident,level);
                if(CreatureAgents.managed(worker)){
                    if(!worker.isAlive()||!NativeIllagers.member(worker)||!worker.entityTags().contains("villagers:native_illager_worker"))throw new IllegalStateException("Expected existing worker role missing");
                    adoption=Map.of("enrolledExisting",0,"alreadyManaged",true,"id",worker.getUUID().toString());
                }else adoption=NativeIllagers.recruitWorker(level,scope.resident,scope.worker);
                if(!CreatureAgents.managed(worker)||!NativeIllagers.member(worker))throw new IllegalStateException("Expected worker was not enrolled");
                after=NativeIllagers.workerBody(worker);if(!before.equals(after))throw new IllegalStateException("Startup altered physical body or stock");
                scope.requireTime(level.getGameTime());write("ready","");ready=true;
            }catch(Exception error){fail(error);}
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{if(server==owner)owner=null;});
    }
    static boolean waiting(MinecraftServer server){return server==owner&&!resumed;}
    static boolean failed(MinecraftServer server){return server==owner&&failed;}
    static boolean ready(MinecraftServer server){return server==owner&&ready&&!failed;}
    static boolean active(MinecraftServer server){return server==owner;}
    static int resume(MinecraftServer server){
        if(!ready(server)||resumed)throw new IllegalStateException("No verified frozen care startup awaits resume");
        try{scope.requireTime(server.overworld().getGameTime());write("resumed","");resumed=true;server.tickRateManager().setFrozen(false);return 1;}
        catch(Exception error){fail(error);throw new IllegalStateException("Care startup remains frozen",error);}
    }
    private static void write(String stage,String message)throws java.io.IOException{
        var row=new LinkedHashMap<String,Object>();row.put("schema",1);row.put("stage",stage);row.put("gameTime",owner.overworld().getGameTime());
        row.put("expectedGameTime",scope.gameTime);row.put("resident",scope.resident.toString());row.put("worker",scope.worker.toString());
        row.put("bodyBefore",before);row.put("bodyAfter",after);row.put("adoption",adoption);row.put("frozenFrames",frames);row.put("error",message);
        var temp=receipt.resolveSibling(receipt.getFileName()+".tmp");Files.writeString(temp,new GsonBuilder().setPrettyPrinting().create().toJson(row));
        Files.move(temp,receipt,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
    }
    private static void fail(Exception error){
        failed=true;if(owner!=null)owner.tickRateManager().setFrozen(true);
        try{write("failed",String.valueOf(error.getMessage()));}catch(Exception failure){Villagers.LOG.error("Frozen care startup receipt failed",failure);}
        Villagers.LOG.error("Care startup stopped before simulation",error);
    }
    private NativeCareStartup(){}
}
