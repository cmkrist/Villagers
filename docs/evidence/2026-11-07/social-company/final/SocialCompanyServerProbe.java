package local.villagers;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import local.villagers.mind.AgentMind;
import local.villagers.relations.Opinion;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;

/** TEST MOD ONLY. Declared initial needs/relationships; ordinary controller owns every subsequent decision. */
public final class SocialCompanyServerProbe implements ModInitializer {
    private static final UUID VISITOR=new UUID(0x534f4349414c0000L,1), FRIEND=new UUID(0x534f4349414c0000L,2), STRANGER=new UUID(0x534f4349414c0000L,3);
    private final ArrayList<Map<String,Object>> observations=new ArrayList<>();
    private Villager visitor;
    private int ticks;
    private boolean selectedFriend, travelled, completed;
    private final java.util.Set<UUID> reopened = new java.util.HashSet<>();
    private boolean restartFailure;
    @Override public void onInitialize() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity,level)-> {
            if (!(entity instanceof Villager actor)) return;
            var path=level.getServer().getWorldPath(LevelResource.ROOT).resolve("social-company-result.json");
            if (!Files.exists(path)) return;
            try {
                var report=com.google.gson.JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                check(report.get("status").getAsString().equals("passed"),"only passed worlds reopen");
                var expected=report.getAsJsonObject("minds").getAsJsonObject(actor.getUUID().toString());
                if(expected==null)return;
                var mindFile=level.getServer().getWorldPath(LevelResource.ROOT).resolve("villagers").resolve(actor.getUUID().toString()).resolve("mind.json");
                var saved=com.google.gson.JsonParser.parseString(Files.readString(mindFile)).getAsJsonObject();
                check(saved.get("opinions").equals(expected.get("opinions")),"saved relationships exact before restart decisions");
                check(saved.get("memories").equals(expected.get("memories")),"visit memories exact before restart decisions");
                check(saved.get("experience").equals(expected.get("experience")),"conversation experience exact");
                check(actor.isAlive() && Villagers.isManaged(actor),"original managed body reopens");
                check(actor.getInventory().countItem(Items.BREAD)==6,"original supplies preserved");
                reopened.add(actor.getUUID());
            } catch(Throwable e) {restartFailure=true;e.printStackTrace();level.getServer().halt(false);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                ticks++;
                if(ticks==1) {server.overworld().setChunkForced(0,0,true);server.overworld().getChunk(0,0);server.tickRateManager().setTickRate(80);}
                if(ticks==40) {
                    if(Files.exists(server.getWorldPath(LevelResource.ROOT).resolve("social-company-result.json"))) {
                        check(!restartFailure && reopened.size()==3,"all three originals verified at load");
                        Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("social-company-restart-result.json"),"{\"status\":\"restart-passed\",\"originalsVerifiedAtLoad\":3}");
                        System.out.println("SOCIAL COMPANY RESTART PASS");server.halt(false);return;
                    }
                    setup(server);
                }
                if(visitor==null)return;
                var s=Villagers.existingSession(VISITOR);
                if(FRIEND.equals(s.socialVisit.target()))selectedFriend=true;
                if(visitor.position().distanceToSqr(new net.minecraft.world.phys.Vec3(3.5,81,7.5))>1)travelled=true;
                completed=s.mind.memories().stream().anyMatch(m->m.note().startsWith("Visited "));
                if(ticks%20==0) {
                    var r=new LinkedHashMap<String,Object>();r.put("tick",server.overworld().getGameTime());r.put("x",visitor.getX());r.put("z",visitor.getZ());
                    r.put("action",String.valueOf(s.action));r.put("target",String.valueOf(s.socialVisit.target()));r.put("loneliness",s.mind.loneliness());
                    r.put("friendRegard",s.mind.opinionOf(FRIEND).regard());r.put("selectedFriend",selectedFriend);r.put("visited",completed);observations.add(r);
                }
                if(completed && selectedFriend && travelled) {
                    check(s.mind.loneliness()<.8,"real social relief");
                    check(s.mind.opinionOf(FRIEND).regard()>.69,"actual conversation changed relationship");
                    for(var id:new UUID[]{VISITOR,FRIEND,STRANGER})check(server.overworld().getEntity(id) instanceof Villager v && v.isAlive(),"all original bodies survive");
                    write(server,"passed",null);System.out.println("SOCIAL COMPANY PASS");server.halt(false);
                }
                if(ticks>1600)throw new AssertionError("No completed autonomous friend visit within bounded observation");
            } catch(Throwable e) {e.printStackTrace();try{write(server,"failed",e.toString());}catch(Exception ignored){}server.halt(false);}
        });
    }
    private void setup(MinecraftServer server) {
        var level=server.overworld();
        for(int x=1;x<=14;x++)for(int z=1;z<=14;z++)for(int y=80;y<=84;y++)
            level.setBlock(new BlockPos(x,y,z),y==80 || (y<=83 && (x==1 || x==14 || z==1 || z==14))?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        for(int i=1;i<=3;i++) {
            var id=i==1?VISITOR:i==2?FRIEND:STRANGER;
            var mind=new AgentMind(id);
            var gson=new GsonBuilder().create();var snapshot=gson.toJsonTree(mind.snapshot()).getAsJsonObject();
            snapshot.addProperty("loneliness",i==1?.85:.1);
            mind=AgentMind.fromSnapshot(gson.fromJson(snapshot,AgentMind.Snapshot.class));
            if(i==1)for(int n=0;n<3;n++)mind.witness(FRIEND,Opinion.Event.SHARED_FOOD,0);
            Villagers.registerNewborn(mind,level.getGameTime());
            var v=new Villager(EntityTypes.VILLAGER,level);v.setUUID(id);v.snapTo(i==1?3.5:i==2?10.5:4.5,81,i==3?10.5:7.5,0,0);
            v.getInventory().addItem(new ItemStack(Items.BREAD,6));level.addFreshEntity(v);Villagers.adopt(level,v);
            if(i==1)visitor=v;
        }
    }
    private void write(MinecraftServer server,String status,String failure)throws Exception {
        var r=new LinkedHashMap<String,Object>();r.put("status",status);r.put("failure",failure);r.put("selectedFriend",selectedFriend);r.put("nativeTravel",travelled);r.put("completedVisit",completed);r.put("observations",observations);
        var minds=new LinkedHashMap<String,Object>();for(var id:new UUID[]{VISITOR,FRIEND,STRANGER}) {var s=Villagers.existingSession(id);if(s!=null)minds.put(id.toString(),s.mind.snapshot());}r.put("minds",minds);
        r.put("fixture","NEW isolated peaceful walled platform; three adults,6 bread each; visitor starts loneliness.85 and three declared prior food-favour opinions of friend; others.1. No post-setup actor/drive/clock/position/action/world setters; normal80base exhaustion,80TPS; no natural-village/survival capacity claim.");
        Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("social-company-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(r));
    }
    private static void check(boolean okay,String message) {if(!okay)throw new AssertionError(message);}
}
