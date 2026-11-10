package local.villagers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import java.nio.file.Files;
import java.util.*;
import local.villagers.genetics.Trait;
import local.villagers.language.Lexicon;
import local.villagers.mind.AgentMind;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.LevelResource;

/** TEST ONLY: two producers, two dependents, six beds, ordinary food sharing and breeding. */
public final class CivilizationServerProbe implements ModInitializer {
    private final Map<UUID,Villager> originals=new LinkedHashMap<>();
    private final Set<UUID> cohort=new HashSet<>(),loads=new HashSet<>(),fed=new HashSet<>();
    private final Map<UUID,Long> harvestTicks=new HashMap<>(),transferTicks=new HashMap<>();
    private final Map<String,Integer> harvestCells=new LinkedHashMap<>();
    private final List<Map<String,Object>> events=new ArrayList<>();
    private final Map<UUID,Integer> foodBeforeEarnedMeal=new HashMap<>();
    private final Set<UUID> earnedMeals=new HashSet<>();
    private int ticks,minimumFood=20,harvests,transfers,births;
    private final boolean continuing=Boolean.getBoolean("villagers.civilization.continue");
    private boolean reopening;
    private JsonObject previous;
    private static UUID founder(int index) {
        for(long n=1;n<100000;n++) {
            var id=new UUID(0x434956494c495a00L+index,n);var m=new AgentMind(id);
            if(index<2?m.has(Trait.GENEROUS)&&m.has(Trait.FRUGAL):m.has(Trait.GLUTTON))return id;
        }
        throw new AssertionError("founder missing");
    }
    @Override public void onInitialize() {
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            if(!(entity instanceof Villager v))return;
            var file=level.getServer().getWorldPath(LevelResource.ROOT).resolve(System.getProperty("villagers.civilization.restartFrom","civilization-result.json"));
            if(!Files.exists(file))return;
            try {
                var saved=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                for(var row:saved.getAsJsonArray("finalActors")) {
                    var old=row.getAsJsonObject();if(!v.getUUID().toString().equals(old.get("id").getAsString()))continue;
                    check(v.isAlive()&&Villagers.isManaged(v),"same saved body at load");
                    check(v.getHealth()==old.get("health").getAsFloat(),"saved health at load");
                    check(HarvestMaterials.snapshot(v.getInventory()).stream().map(Object::toString).toList().toString().equals(old.get("inventory").getAsString()),"exact native bag at load");
                    check(v.getMainHandItem().toString().equals(old.get("held").getAsString())&&v.getMainHandItem().getDamageValue()==old.get("wear").getAsInt(),"exact tool and wear at load");
                    var mind=JsonParser.parseString(Files.readString(level.getServer().getWorldPath(LevelResource.ROOT).resolve("villagers").resolve(v.getUUID().toString()).resolve("mind.json"))).getAsJsonObject();
                    check(mind.getAsJsonObject("food").get("foodLevel").getAsInt()==old.get("food").getAsInt(),"exact mind nutrition at load");
                    loads.add(v.getUUID());
                }
            }catch(Throwable e){e.printStackTrace();level.getServer().halt(false);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{try {
            ticks++;
            if(ticks==1){server.overworld().setChunkForced(0,0,true);server.overworld().getChunk(0,0);server.tickRateManager().setTickRate(80);}
            if(ticks==40){var file=server.getWorldPath(LevelResource.ROOT).resolve(System.getProperty("villagers.civilization.restartFrom","civilization-result.json"));reopening=Files.exists(file)&&!continuing;if(reopening||continuing){previous=JsonParser.parseString(Files.readString(file)).getAsJsonObject();if(continuing){check(previous.get("failure").getAsString().equals("java.lang.AssertionError: civilization endurance budget exhausted"),"only pure observation exhaustion continues");restoreObservation();}else check(previous.get("status").getAsString().equals("passed"),"only successful world reopens");}else terrain(server);}
            if(ticks==60)setup(server);
            if(!originals.isEmpty())observe(server);
            if(ticks>96000||server.overworld().getGameTime()>96000)throw new AssertionError("civilization endurance budget exhausted");
        }catch(Throwable e){e.printStackTrace();try{write(server,"failed",e.toString());}catch(Exception ignored){}server.halt(false);}});
    }
    private void restoreObservation() {
        minimumFood=previous.get("minimumFood").getAsInt();births=previous.get("births").getAsInt();
        previous.getAsJsonObject("harvestsByCell").entrySet().forEach(e->harvestCells.put(e.getKey(),e.getValue().getAsInt()));
        var history=previous.getAsJsonArray("events");
        for(var event:history) {
            events.add(new GsonBuilder().create().fromJson(event,new com.google.gson.reflect.TypeToken<LinkedHashMap<String,Object>>(){}.getType()));
            var row=event.getAsJsonObject();
            if(row.has("receipt")&&row.get("phase").getAsString().equals("real food transfer")) {
                var receipt=row.getAsJsonObject("receipt");if(!receipt.get("named").getAsBoolean())fed.add(UUID.fromString(receipt.get("to").getAsString()));
            }
        }
        var last=history.get(history.size()-1).getAsJsonObject();harvests=last.get("harvests").getAsInt();transfers=last.get("transfers").getAsInt();
        for(var id:last.getAsJsonArray("earnedMeals"))earnedMeals.add(UUID.fromString(id.getAsString()));
    }
    private void terrain(MinecraftServer server) {
        var l=server.overworld();
        for(int x=0;x<=15;x++)for(int z=0;z<=15;z++)for(int y=80;y<=84;y++)l.setBlock(new BlockPos(x,y,z),
            y==80?Blocks.STONE.defaultBlockState():y==84?(x%3==0&&z%3==0?Blocks.GLOWSTONE:Blocks.GLASS).defaultBlockState():x==0||x==15||z==0||z==15?Blocks.GLASS.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        for(int x:new int[]{2,4,6,9,11,13}){var bed=Blocks.BED.white().defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH);l.setBlock(new BlockPos(x,81,2),bed.setValue(BedBlock.PART,BedPart.FOOT),3);l.setBlock(new BlockPos(x,81,3),bed.setValue(BedBlock.PART,BedPart.HEAD),3);}
        for(int x:new int[]{3,12})l.setBlock(new BlockPos(x,81,4),Blocks.CRAFTING_TABLE.defaultBlockState(),3);
        for(int x=4;x<=11;x++)for(int z=5;z<=10;z++){l.setBlock(new BlockPos(x,80,z),Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7),3);l.setBlock(new BlockPos(x,81,z),(x%2==0?Blocks.WHEAT:Blocks.BEETROOTS).defaultBlockState(),3);}
        for(int x:new int[]{3,12})for(int z:new int[]{6,9}){l.setBlock(new BlockPos(x,80,z),Blocks.WATER.defaultBlockState(),3);l.setBlock(new BlockPos(x,81,z),Blocks.STONE.defaultBlockState(),3);}
    }
    private void setup(MinecraftServer server) {
        var l=server.overworld();var words=Lexicon.invent(new Random(771));
        for(int i=0;i<4;i++) {
            Villager v;
            if(reopening||continuing){v=(Villager)l.getEntity(founder(i));check(v!=null,"all original residents reload");Villagers.session(v,l);}
            else {v=new Villager(EntityTypes.VILLAGER,l);v.setUUID(founder(i));v.snapTo(2.5+i*3,81,12.5,0,0);var rations=new ItemStack(Items.BREAD,4);rations.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Civilization starting ration"));v.getInventory().addItem(rations);if(i<2){v.getInventory().addItem(new ItemStack(Items.WHEAT_SEEDS,64));v.getInventory().addItem(new ItemStack(Items.BEETROOT_SEEDS,64));v.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_HOE));}l.addFreshEntity(v);VillagerController.adopt(l,v);Villagers.existingSession(v.getUUID()).mind.lexicon(words);}
            originals.put(v.getUUID(),v);cohort.add(v.getUUID());
        }
        if(reopening||continuing)for(var row:previous.getAsJsonArray("finalActors"))cohort.add(UUID.fromString(row.getAsJsonObject().get("id").getAsString()));
        var observer=new ServerPlayer(server,l,new GameProfile(new UUID(0x4349564f42534552L,1),"VillageObserver"),ClientInformation.createDefault());observer.snapTo(8,81,13,0,0);
        observer.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),observer,CommonListenerCookie.createInitial(observer.getGameProfile(),false)){
            @Override public void send(Packet<?> packet){}
            @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener){}
        };l.addNewPlayer(observer);
        record(server,reopening||continuing?"reopened without resident or resource setters":"declared initial infrastructure and supplies; ordinary controller begins");
    }
    private void observe(MinecraftServer server)throws Exception {
        var l=server.overworld();
        for(var v:originals.values())check(v.isAlive(),"no original resident replacement or death");
        var living=l.getEntitiesOfClass(Villager.class,new net.minecraft.world.phys.AABB(0,79,0,16,86,16),Villagers::isManaged);
        for(var v:living) {
            if(cohort.add(v.getUUID()))births++;
            var s=Villagers.existingSession(v.getUUID());if(s==null)continue;
            minimumFood=Math.min(minimumFood,s.mind.foodLevel());
            if(foodBeforeEarnedMeal.containsKey(v.getUUID())&&s.mind.foodLevel()>foodBeforeEarnedMeal.get(v.getUUID()))earnedMeals.add(v.getUUID());
            check(v.isAlive()&&v.getHealth()==v.getMaxHealth()&&s.mind.foodLevel()>0,"no zero-food interval, starvation or other damage during peaceful endurance");
            if(!reopening) {
                var h=AdventureActions.lastHarvest(v.getUUID());
                if(!h.isEmpty()&&!Objects.equals(harvestTicks.get(v.getUUID()),((Number)h.get("gameTime")).longValue())){harvestTicks.put(v.getUUID(),((Number)h.get("gameTime")).longValue());harvests++;harvestCells.merge(h.get("position").toString(),1,Integer::sum);events.add(Map.of("phase","native harvest","actor",v.getUUID().toString(),"receipt",h));}
                var t=s.lastFoodReceived;
                if(!t.isEmpty()&&!Objects.equals(transferTicks.get(v.getUUID()),((Number)t.get("gameTime")).longValue())){transferTicks.put(v.getUUID(),((Number)t.get("gameTime")).longValue());transfers++;if(Boolean.FALSE.equals(t.get("named"))){fed.add(v.getUUID());foodBeforeEarnedMeal.put(v.getUUID(),s.mind.foodLevel());}events.add(Map.of("phase","real food transfer","receipt",t));}
            }
        }
        for(var id:cohort)check(l.getEntity(id)instanceof Villager v&&v.isAlive(),"every admitted descendant remains present and alive");
        if(reopening&&ticks==160){check(loads.containsAll(cohort),"every cohort inventory and mind checked before autonomous ticks");record(server,"clean restart passed");write(server,"restart-passed",null);server.halt(false);}
        if(!reopening&&ticks%1200==0)record(server,"endurance observation");
        if(!reopening&&server.overworld().getGameTime()>=48000&&harvestCells.values().stream().anyMatch(n->n>=2)&&fed.contains(founder(2))&&fed.contains(founder(3))&&earnedMeals.contains(founder(2))&&earnedMeals.contains(founder(3))){record(server,"two loaded days of ordinary four-founder society passed");write(server,"passed",null);server.halt(false);}
    }
    private List<Map<String,Object>> census(MinecraftServer server){
        var rows=new ArrayList<Map<String,Object>>();for(var id:cohort){var body=server.overworld().getEntity(id);if(!(body instanceof Villager v))continue;var s=Villagers.existingSession(id);if(s==null)continue;
            var row=new LinkedHashMap<String,Object>();row.put("id",id.toString());row.put("health",v.getHealth());row.put("food",s.mind.foodLevel());row.put("inventory",HarvestMaterials.snapshot(v.getInventory()).stream().map(Object::toString).toList().toString());row.put("held",v.getMainHandItem().toString());row.put("wear",v.getMainHandItem().getDamageValue());row.put("given",s.foodSharedGiven);row.put("received",s.foodSharedReceived);row.put("breadCrafted",s.breadCrafted);row.put("age",v.getAge());row.put("lineage",s.mind.lineage());rows.add(row);
        }return rows;
    }
    private void record(MinecraftServer server,String phase){var row=new LinkedHashMap<String,Object>();row.put("phase",phase);row.put("tick",server.overworld().getGameTime());row.put("minimumFood",minimumFood);row.put("harvests",harvests);row.put("transfers",transfers);row.put("births",births);row.put("earnedMeals",earnedMeals);row.put("actors",census(server));row.put("meanTickMillis",server.getAverageTickTimeNanos()/1e6);events.add(row);System.out.println("CIVILIZATION EVENT: "+new GsonBuilder().create().toJson(row));}
    private void write(MinecraftServer server,String status,String failure)throws Exception{var result=new LinkedHashMap<String,Object>();result.put("status",status);result.put("failure",failure);result.put("events",events);result.put("finalActors",census(server));result.put("minimumFood",minimumFood);result.put("harvestsByCell",harvestCells);result.put("births",births);result.put("fixture","Isolated peaceful lit chamber;48age-zero mixed crops,water,two tables,six beds;four selected founders/common initial language/four marked bread each;two farmers get actual iron hoes and64 of each seed.80TPS,normal80base exhaustion/day,ordinary sharing and breeding enabled. Stationary test observer. No post-setup resident/resource/growth/clock setters; no replacement actors. Not hostile natural-terrain or renewable-tool acceptance.");Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve(status.equals("restart-passed")?"civilization-restart-result.json":continuing?"civilization-continuation-result.json":"civilization-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
