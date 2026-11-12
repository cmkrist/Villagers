package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.villagers.genetics.Trait;
import local.villagers.language.*;
import local.villagers.mind.*;
import local.villagers.work.WorkOrders;
import local.villagers.work.WorkOrders.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;

/** New-world production orders. No society opened; the only later stimuli are declared station loss and native injury. */
public final class WorkServerProbe implements ModInitializer {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private final List<UUID> ids=new ArrayList<>();
    private final Set<UUID> reopened=new HashSet<>();
    private final List<Map<String,Object>> trace=new ArrayList<>();
    private int ticks;
    private boolean started,foreignBlocked,unsafeBlocked,injured,sawUnsafe;
    private static final String PHASE=System.getProperty("villagers.workProbe","first");
    private Villager hb,hw,bb,bw,lb,lw;
    private Session session(Villager v){return Villagers.session(v,(net.minecraft.server.level.ServerLevel)v.level());}
    public void onInitialize(){
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            var root=level.getServer().getWorldPath(LevelResource.ROOT);var path=root.resolve("work-pending-result.json");
            if(!(entity instanceof Villager v)||!Files.exists(path))return;
            try{
                var document=JsonParser.parseString(Files.readString(root.resolve(Files.exists(root.resolve("work-result.json"))?"work-result.json":"work-pending-result.json"))).getAsJsonObject();
                var expected=document.getAsJsonObject("minds").getAsJsonObject(v.getUUID().toString());if(expected==null)return;
                var actual=JsonParser.parseString(Files.readString(root.resolve("villagers").resolve(v.getUUID().toString()).resolve("mind.json"))).getAsJsonObject();
                for(var field:List.of("workOrders","lexicon","beliefs","partnerships"))check(expected.get(field).equals(actual.get(field)),field+" exact before next decisions");
                check(v.isAlive()&&Villagers.isManaged(v),"original body alive");reopened.add(v.getUUID());
            }catch(Throwable t){fail(level.getServer(),t);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            try{
                ticks++;var root=server.getWorldPath(LevelResource.ROOT);
                if(ticks==1){for(int x=0;x<=3;x++){server.overworld().setChunkForced(x,0,true);server.overworld().getChunk(x,0);}server.tickRateManager().setTickRate(80);}
                if(ticks==40){
                    if(PHASE.equals("first")){setup(server);return;}
                    check(reopened.size()==10,"ten original minds verified before restart decisions");
                    var old=JsonParser.parseString(Files.readString(root.resolve("work-pending-result.json"))).getAsJsonObject();
                    for(var id:old.getAsJsonObject("minds").keySet())ids.add(UUID.fromString(id));
                    hb=body(server,old,"hoeBuyer");hw=body(server,old,"hoeWorker");bb=body(server,old,"breadBuyer");bw=body(server,old,"breadWorker");lb=body(server,old,"lostBuyer");lw=body(server,old,"lostWorker");
                    for(var id:ids)session((Villager)server.overworld().getEntity(id));
                    if(PHASE.equals("frozen")){
                        var before=session(hw).mind.snapshot();var saved=session(hw).mind.workOrders();
                        check(saved.open()!=null,"frozen control retains a readable saved order");
                        WorkLife.review(server.overworld(),hw,session(hw));WorkLife.work(server.overworld(),hw,session(hw),server.overworld().getGameTime());
                        check(saved.equals(session(hw).mind.workOrders()),"frozen checkpoint does not advance one-shot obligations");
                        check(before.lexicon().equals(session(hw).mind.lexicon())&&before.beliefs().equals(session(hw).mind.beliefs()),"frozen work attempt changes no learned knowledge");
                        Files.writeString(root.resolve("work-frozen-result.json"),"{\"status\":\"frozen-passed\",\"originalsVerifiedAtLoad\":10,\"orderReadable\":true,\"noPaymentOrCompletion\":true}");server.halt(false);return;
                    }
                    if(PHASE.equals("restart")){
                        check(session(hw).mind.workOrders().history().getLast().state()==State.COMPLETED&&session(bw).mind.workOrders().history().getLast().state()==State.COMPLETED,"completed orders remain closed");
                        check(session(lw).mind.workOrders().history().getLast().state()==State.EXPIRED,"unfulfilled outcome survives");
                        Files.writeString(root.resolve("work-restart-result.json"),"{\"status\":\"restart-passed\",\"originalsVerifiedAtLoad\":10,\"noReplay\":true}");server.halt(false);return;
                    }
                    injured=bw.hurtServer(server.overworld(),bw.damageSources().generic(),5);check(injured,"one declared native injury stimulus");started=true;
                }
                if(!started)return;
                var h=session(hw);var b=session(bw);var l=session(lw);var now=server.overworld().getGameTime();
                var be=b.mind.workOrders().history().getLast();sawUnsafe|=be.state()==State.PAUSED&&be.reason()==Reason.UNSAFE;
                if(ticks%40==0){trace.add(Map.of("tick",ticks,"gameTime",now,"hoe",h.mind.workOrders(),"bread",b.mind.workOrders(),"lost",l.mind.workOrders(),"hoePosition",hw.position().toString(),"breadHealth",bw.getHealth()));Files.writeString(root.resolve("work-progress.json"),JSON.toJson(trace));}
                if(h.mind.workOrders().open()==null&&b.mind.workOrders().open()==null&&l.mind.workOrders().open()==null&&session(lb).mind.workOrders().open()==null){
                    var ho=h.mind.workOrders().history().getLast();var bo=b.mind.workOrders().history().getLast();var lo=l.mind.workOrders().history().getLast();
                    check(ho.state()==State.COMPLETED&&bo.state()==State.COMPLETED,"two actual production settlements complete");
                    check(ho.production()!=null&&bo.production()!=null,"real recipe callbacks precede delivery");
                    check(session(hb).mind.workOrders().history().getLast().production()==null&&session(bb).mind.workOrders().history().getLast().production()==null,"customer never receives private station/production evidence");
                    check(sawUnsafe&&bo.interruptions()>0,"injury really paused work before recovery");
                    check(lo.state()==State.EXPIRED&&lo.settlement()==null&&lo.production()==null,"lost station prevents production/completion");
                    check(session(lb).mind.workOrders().history().getLast().state()==State.EXPIRED,"customer independently expires its unfulfilled order");
                    check(lw.getInventory().countItem(Items.WHEAT)==3&&lw.getInventory().countItem(Items.EMERALD)==0&&lb.getInventory().countItem(Items.EMERALD)==1,"unfulfilled job consumes no inputs or payment");
                    check(hw.getInventory().countItem(Items.OAK_LOG)==0&&hw.getInventory().countItem(Items.STICK)==2,"wood recipe chain spent its actual log and retained native stick remainder");
                    check(bw.getInventory().countItem(Items.WHEAT)==0&&bw.getInventory().countItem(Items.BREAD)==3&&bb.getInventory().countItem(Items.BREAD)==1,"wheat-to-bread production and delivery conserved actual items");
                    check(hw.getInventory().countItem(Items.EMERALD)==2&&hb.getInventory().countItem(Items.EMERALD)==0&&bw.getInventory().countItem(Items.EMERALD)==1&&bb.getInventory().countItem(Items.EMERALD)==0,"the agreed one/two-coin prices settled exactly");
                    var r=new LinkedHashMap<String,Object>();r.put("status","passed");r.put("ticks",ticks);r.put("originals",ids);r.put("nativeInjuryPausedWork",sawUnsafe);r.put("hoeSettlement",h.lastWorkSettlement);r.put("breadSettlement",b.lastWorkSettlement);r.put("lostStationOutcome",lo);r.put("minds",snapshots(server));r.put("trace",trace);r.put("fixture",fixture());
                    Files.writeString(root.resolve("work-result.json"),JSON.toJson(r));System.out.println("WORK PASS at "+ticks);server.halt(false);
                }
                if(ticks>3600)throw new AssertionError("Work acceptance incomplete: "+h.orderStatus+" / "+b.orderStatus+" / "+l.orderStatus);
            }catch(Throwable t){fail(server,t);}
        });
    }
    private Villager body(MinecraftServer server,JsonObject document,String field){return (Villager)server.overworld().getEntity(UUID.fromString(document.get(field).getAsString()));}
    private Map<String,Object> snapshots(MinecraftServer server){var result=new LinkedHashMap<String,Object>();for(var id:ids){var v=(Villager)server.overworld().getEntity(id);check(v!=null&&v.isAlive(),"all originals survive");result.put(id.toString(),session(v).mind.snapshot());}return result;}
    private AgentMind mind(UUID id,String biome,int food){var m=new AgentMind(id);m.establishOrigin(biome);m.lexicon(BiomeDialect.foundingLanguage(biome));var json=JSON.toJsonTree(m.snapshot()).getAsJsonObject();json.addProperty("loneliness",.1);json.getAsJsonObject("food").addProperty("foodLevel",food);json.getAsJsonObject("food").addProperty("saturation",food);return AgentMind.fromSnapshot(JSON.fromJson(json,AgentMind.Snapshot.class));}
    private Villager spawn(MinecraftServer server,AgentMind mind,double x,double z,boolean fixed,boolean worker,Task task){
        ids.add(mind.id());Villagers.registerNewborn(mind,server.overworld().getGameTime());var v=new Villager(EntityTypes.VILLAGER,server.overworld());v.setUUID(mind.id());v.snapTo(x,81,z,0,0);v.setNoAi(fixed);
        if(worker){v.getInventory().addItem(new ItemStack(Items.BREAD,3));if(task==Task.WOODEN_HOE){v.getInventory().addItem(new ItemStack(Items.OAK_LOG,1));v.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STONE_HOE,1));}else v.getInventory().addItem(new ItemStack(Items.WHEAT,3));}
        else {v.getInventory().addItem(new ItemStack(Items.EMERALD,task==Task.BREAD?1:2));if(task==Task.WOODEN_HOE)v.getInventory().addItem(new ItemStack(Items.WHEAT_SEEDS,1));}
        server.overworld().addFreshEntity(v);Villagers.adopt(server.overworld(),v);return v;
    }
    private void setup(MinecraftServer server)throws Exception{
        for(int x=1;x<=50;x++)for(int z=1;z<=14;z++)for(int y=80;y<=84;y++)server.overworld().setBlock(new BlockPos(x,y,z),y==80||y<=83&&(x==1||x%10==0||x%10==1||z==1||z==14)?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        for(int x:List.of(8,18,28,38,48))server.overworld().setBlock(new BlockPos(x,81,7),Blocks.CRAFTING_TABLE.defaultBlockState(),3);
        AgentMind greedy=null;for(int i=1;i<10000;i++){var m=mind(new UUID(901,i),"minecraft:plains",20);if(m.has(Trait.GREEDY)){greedy=m;break;}}check(greedy!=null,"greedy worker fixture chosen for a two-coin counterquote");
        hb=spawn(server,mind(new UUID(902,1),"minecraft:plains",20),2.5,5.5,false,false,Task.WOODEN_HOE);hw=spawn(server,greedy,7.5,5.5,false,true,Task.WOODEN_HOE);
        bb=spawn(server,mind(new UUID(903,1),"minecraft:plains",20),12.5,5.5,false,false,Task.BREAD);var bm=mind(new UUID(903,2),"minecraft:plains",20);while(bm.has(Trait.GREEDY))bm=mind(new UUID(903,bm.id().getLeastSignificantBits()+1),"minecraft:plains",20);bw=spawn(server,bm,17.5,5.5,false,true,Task.BREAD);
        var fb=spawn(server,mind(new UUID(904,1),"minecraft:plains",20),22.5,5.5,true,false,Task.WOODEN_HOE);var fw=spawn(server,mind(new UUID(904,2),"minecraft:forest",20),27.5,5.5,true,true,Task.WOODEN_HOE);
        var ub=spawn(server,mind(new UUID(905,1),"minecraft:plains",20),32.5,5.5,true,false,Task.WOODEN_HOE);var uw=spawn(server,mind(new UUID(905,2),"minecraft:plains",5),37.5,5.5,true,true,Task.WOODEN_HOE);
        lb=spawn(server,mind(new UUID(906,1),"minecraft:plains",20),42.5,5.5,false,false,Task.BREAD);var lm=mind(new UUID(906,2),"minecraft:plains",20);while(lm.has(Trait.GREEDY))lm=mind(new UUID(906,lm.id().getLeastSignificantBits()+1),"minecraft:plains",20);lw=spawn(server,lm,47.5,5.5,false,true,Task.BREAD);
        check(WorkLife.negotiate(server.overworld(),hb,session(hb),hw,Task.WOODEN_HOE)==WorkLife.Outcome.AGREED,"explicit two-coin hoe terms accepted");
        check(WorkLife.negotiate(server.overworld(),bb,session(bb),bw,Task.BREAD)==WorkLife.Outcome.AGREED,"explicit bread terms accepted");
        check(WorkLife.negotiate(server.overworld(),lb,session(lb),lw,Task.BREAD)==WorkLife.Outcome.AGREED,"future station-loss order initially legitimate");
        foreignBlocked=WorkLife.negotiate(server.overworld(),fb,session(fb),fw,Task.WOODEN_HOE)==WorkLife.Outcome.UNKNOWN;
        unsafeBlocked=WorkLife.negotiate(server.overworld(),ub,session(ub),uw,Task.WOODEN_HOE)==WorkLife.Outcome.REFUSED;
        check(foreignBlocked&&unsafeBlocked&&session(fb).mind.workOrders().history().isEmpty()&&session(fw).mind.workOrders().history().isEmpty()&&session(ub).mind.workOrders().history().isEmpty()&&session(uw).mind.workOrders().history().isEmpty(),"foreign language and unwilling body grant no order");
        check(server.overworld().destroyBlock(new BlockPos(48,81,7),true),"declared station loss through native block destruction");
        check(session(hw).mind.workOrders().open().production()==null&&hw.getInventory().countItem(Items.OAK_LOG)==1&&bw.getInventory().countItem(Items.WHEAT)==3,"save pending promises before any production or payment");
        var r=new LinkedHashMap<String,Object>();r.put("status","pending-passed");r.put("hoeBuyer",hb.getUUID());r.put("hoeWorker",hw.getUUID());r.put("breadBuyer",bb.getUUID());r.put("breadWorker",bw.getUUID());r.put("lostBuyer",lb.getUUID());r.put("lostWorker",lw.getUUID());r.put("foreignBlocked",foreignBlocked);r.put("unsafeBlocked",unsafeBlocked);r.put("minds",snapshots(server));r.put("fixture",fixture());
        Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("work-pending-result.json"),JSON.toJson(r));server.halt(false);
    }
    private static String fixture(){return "NEW peaceful five-room platform, four loaded fixture chunks, ten original adults. Five controlled production negotiation opportunities use actual WorkLife speech: two legitimate jobs, one legitimate job whose station is then destroyed with native drops, a foreign-language control and an undernourished refusing worker. Six ordinary AI bodies resume autonomous production/delivery/expiry after a clean pending-order restart; four controls are NoAI stationary. Inputs, existing tables, a retained working hoe, actual emeralds and food reserves are explicitly seeded fixture conditions. No post-setup actor/inventory/mind setters; one declared five-point native injury tests real interruption/recovery, and one declared station destruction tests unfulfilled work. Exhaustion0 and breedingFalse are controlled conditions. Paid native recipes, movement and physical transfers remain production code; no existing society opened, no resource grants, no natural job emergence or visual/scaling claim.";}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void fail(MinecraftServer server,Throwable t){t.printStackTrace();try{Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("work-failure.txt"),t.toString());}catch(Exception ignored){}server.halt(false);}
}
