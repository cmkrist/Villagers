package local.villagers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import local.villagers.genetics.Trait;
import local.villagers.mind.AgentMind;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.LevelResource;

/** TEST MOD ONLY: declared supplied chamber, selected founder traits, natural loaded-time hunger. */
public final class FoodEconomyServerProbe implements ModInitializer {
    private Villager producer, requester;
    private ServerPlayer supplier;
    private int ticks, hungerBeforeShare;
    private long requestedAt = -1;
    private JsonObject previous;
    private final boolean renewable=Boolean.getBoolean("villagers.economy.renewable");
    private final ArrayList<BlockPos> crops=new ArrayList<>();
    private final Map<String,Integer> harvestsByCell=new LinkedHashMap<>();
    private long lastHarvestTick=-1;
    private boolean earnedBreadTransferred;
    private final ArrayList<Map<String,Object>> events = new ArrayList<>();

    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                ticks++;
                if (ticks == 1) { server.overworld().setChunkForced(0,0,true); server.overworld().getChunk(0,0); server.tickRateManager().setTickRate(80); }
                if (ticks == 40) {
                    var report=server.getWorldPath(LevelResource.ROOT).resolve("food-economy-result.json");
                    if (Files.exists(report)) {
                        previous=JsonParser.parseString(Files.readString(report)).getAsJsonObject();
                        check(previous.get("status").getAsString().equals("passed"),"only a passed fixture may reopen");
                    } else if(renewable) farm(server.overworld()); else chamber(server.overworld());
                }
                if (ticks == 60) setup(server);
                if (producer != null) {
                    if (previous!=null) reopen(server); else if(renewable) observeFarm(server); else observe(server);
                }
                if (ticks > (renewable?48000:6000)) throw new AssertionError("Native economy observation budget exhausted");
            } catch (Throwable failure) {
                failure.printStackTrace(); System.out.println("FOOD ECONOMY FAILED");
                try { write(server, "failed", failure.toString()); } catch (Exception ignored) {}
                server.halt(false);
            }
        });
    }

    private static UUID founder(boolean donor) {
        for (long i=1;i<100000;i++) {
            UUID id=new UUID(0x464f4f4445434f4eL,i); var mind=new AgentMind(id);
            if (donor ? mind.has(Trait.GENEROUS) && mind.has(Trait.FRUGAL) : mind.has(Trait.GLUTTON)) return id;
        }
        throw new AssertionError("No matching selected founder");
    }

    private static void chamber(ServerLevel level) {
        for (int x=3;x<=7;x++) for (int z=3;z<=6;z++) for (int y=80;y<=84;y++)
            level.setBlock(new BlockPos(x,y,z),(y==80 || y==84 || x==3 || x==7 || z==3 || z==6)
                    ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(),3);
        level.setBlock(new BlockPos(7,81,4),Blocks.CRAFTING_TABLE.defaultBlockState(),3);
        for (int x : new int[]{4,6}) {
            var state=Blocks.BED.white().defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH);
            level.setBlock(new BlockPos(x,81,4),state.setValue(BedBlock.PART,BedPart.FOOT),3);
            level.setBlock(new BlockPos(x,81,5),state.setValue(BedBlock.PART,BedPart.HEAD),3);
        }
    }

    private void farm(ServerLevel level) {
        for(int x=1;x<=12;x++)for(int z=0;z<=11;z++)for(int y=79;y<=84;y++)
            level.setBlock(new BlockPos(x,y,z),y==79?Blocks.DIRT.defaultBlockState():y==80?Blocks.STONE.defaultBlockState():
                    y==84?(x%3==0 && z%3==0?Blocks.GLOWSTONE:Blocks.GLASS).defaultBlockState():
                    x==1 || x==12 || z==0 || z==11?Blocks.GLASS.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        for(int x:new int[]{6,9}) {
            level.setBlock(new BlockPos(x,80,5),Blocks.WATER.defaultBlockState(),3);
            level.setBlock(new BlockPos(x,81,5),Blocks.STONE.defaultBlockState(),3);
        }
        for(int x:new int[]{4,5,7,8,10,11})for(int z=2;z<=9;z++) {
            var p=new BlockPos(x,81,z);crops.add(p);
            level.setBlock(p.below(),Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7),3);
            level.setBlock(p,Blocks.WHEAT.defaultBlockState(),3); // All48 plants start at age0, once, during setup.
        }
        level.setBlock(new BlockPos(3,81,4),Blocks.CRAFTING_TABLE.defaultBlockState(),3);
        for(int z:new int[]{3,7}) {
            var state=Blocks.BED.white().defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH);
            level.setBlock(new BlockPos(2,81,z),state.setValue(BedBlock.PART,BedPart.FOOT),3);
            level.setBlock(new BlockPos(2,81,z+1),state.setValue(BedBlock.PART,BedPart.HEAD),3);
        }
        check(level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED)==3,"native default random tick speed");
    }

    private void setup(MinecraftServer server) {
        var level=server.overworld();
        if (previous!=null) {
            var history=previous.getAsJsonArray("events");var last=history.get(history.size()-1).getAsJsonObject();
            producer=(Villager)level.getEntity(UUID.fromString(last.getAsJsonObject("producer").get("id").getAsString()));
            requester=(Villager)level.getEntity(UUID.fromString(last.getAsJsonObject("requester").get("id").getAsString()));
            check(producer!=null && requester!=null,"same saved fixture bodies reloaded; no replacement");
            Villagers.session(producer,level); Villagers.session(requester,level);
            record(server,"reopened saved fixture WITHOUT supplies, chamber edits or actor setters");
            return;
        }
        producer=new Villager(EntityTypes.VILLAGER,level); producer.setUUID(founder(true)); producer.snapTo(renewable?3:5.0,81,4.4,0,0);
        requester=new Villager(EntityTypes.VILLAGER,level); requester.setUUID(founder(false)); requester.snapTo(renewable?3:5.0,81,5.5,0,0);
        level.addFreshEntity(producer); level.addFreshEntity(requester);
        VillagerController.adopt(level,producer); VillagerController.adopt(level,requester);
        supplier=new ServerPlayer(server,level,new GameProfile(new UUID(0x46495854555245L,1),"FiniteSupplier"),ClientInformation.createDefault());
        supplier.snapTo(renewable?2.5:5.5,81,renewable?2.5:4.5,0,0); supplier.setShiftKeyDown(true);
        if(renewable) {
            // A stationary test observer makes this an actual player-nearby random-ticking farm.
            supplier.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),supplier,
                    CommonListenerCookie.createInitial(supplier.getGameProfile(),false)) {
                @Override public void send(Packet<?> packet) {}
                @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener) {}
            };
            level.addNewPlayer(supplier);
            var ration=new ItemStack(Items.BREAD,6);
            ration.set(DataComponents.CUSTOM_NAME,Component.literal("Fixture starting ration"));
            producer.getInventory().addItem(ration.copy());requester.getInventory().addItem(ration.copy());
            producer.getInventory().addItem(new ItemStack(Items.WHEAT_SEEDS,64));
            producer.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_HOE));
        } else supplier.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WHEAT,18));
        record(server,"setup");
        check(Villagers.existingSession(producer.getUUID()).mind.home()!=null,"producer claimed actual bed");
        check(Villagers.existingSession(requester.getUUID()).mind.home()!=null,"requester claimed actual bed");
    }

    private static int earnedBread(Villager actor) {
        int count=0;
        for(int i=0;i<actor.getInventory().getContainerSize();i++) {
            var stack=actor.getInventory().getItem(i);
            if(stack.is(Items.BREAD) && !stack.has(DataComponents.CUSTOM_NAME)) count+=stack.getCount();
        }
        return count;
    }

    private void observeFarm(MinecraftServer server) throws Exception {
        check(producer.isAlive() && requester.isAlive(),"both fixture adults must survive; never replaced");
        var p=Villagers.existingSession(producer.getUUID());var r=Villagers.existingSession(requester.getUUID());
        var receipt=AdventureActions.lastHarvest(producer.getUUID());
        if(!receipt.isEmpty() && ((Number)receipt.get("gameTime")).longValue()!=lastHarvestTick) {
            lastHarvestTick=((Number)receipt.get("gameTime")).longValue();
            harvestsByCell.merge(receipt.get("position").toString(),1,Integer::sum);
            var event=new LinkedHashMap<String,Object>();event.put("phase","native paid harvest/replant");event.put("receipt",receipt);events.add(event);
        }
        if(earnedBread(requester)>0 && r.foodSharedReceived>0 && !earnedBreadTransferred) {
            earnedBreadTransferred=true;record(server,"unmarked native-recipe bread reached requester; starter rations are marked");
        }
        if(ticks%1200==0)record(server,"renewable observation");
        if(p.breadCrafted>0 && earnedBreadTransferred && earnedBread(requester)==0
                && harvestsByCell.values().stream().anyMatch(n->n>=2)) {
            record(server,"native regrowth, repeat harvest, paid bread production, sharing and meal passed");
            write(server,"passed",null);System.out.println("FOOD ECONOMY RENEWABLE PASS: native regrowth -> repeat paid harvest -> bread -> earned transfer/meal");server.halt(false);
        }
    }

    private void reopen(MinecraftServer server) throws Exception {
        if (ticks!=160) return;
        var history=previous.getAsJsonArray("events");var last=history.get(history.size()-1).getAsJsonObject();
        for (var v:new Villager[]{producer,requester}) {
            var expected=last.getAsJsonObject(v==producer?"producer":"requester");var s=Villagers.existingSession(v.getUUID());
            check(v.isAlive() && Villagers.isManaged(v),"same saved managed adult remains alive");
            check(s.mind.foodLevel()==expected.get("food").getAsInt(),"meal nutrition survives restart");
            check(v.getInventory().countItem(Items.BREAD)==expected.get("bread").getAsInt(),"bread inventory not replayed on restart");
            check(v.getInventory().countItem(Items.WHEAT)==expected.get("wheat").getAsInt(),"wheat costs not replayed on restart");
            check(s.breadCrafted==0 && s.foodSharedGiven==0 && s.foodSharedReceived==0,"no repeated recipe or transfer in new session");
            check(s.foodDemand.extraNutrition(server.overworld().getGameTime())==0,"fulfilled signal creates no invented persisted debt");
            check(s.mind.home()!=null,"claimed home persists");
        }
        record(server,"restart persistence and no-replay passed");write(server,"restart-passed",null);
        System.out.println("FOOD ECONOMY RESTART PASS: earned inventories and consumed meals preserved, no repeated income or costs");
        server.halt(false);
    }

    private void observe(MinecraftServer server) throws Exception {
        var p=Villagers.existingSession(producer.getUUID()); var r=Villagers.existingSession(requester.getUUID());
        if (requestedAt<0 && p.foodDemand.extraNutrition(server.overworld().getGameTime())>0) {
            requestedAt=server.overworld().getGameTime(); hungerBeforeShare=r.mind.foodLevel();
            record(server,"understood unmet request BEFORE supplying wheat");
            check(p.foodSharedGiven==0 && r.foodSharedReceived==0,"no food already handed off");
            check(producer.getInventory().countItem(Items.BREAD)==0,"producer has no bread at request");
            check(FoodSupply.target(p,requestedAt)==20,"actual request raised production target to20");
            for (int i=0;i<18;i++) check(Villagers.offerGift(server.overworld(),producer,supplier,InteractionHand.MAIN_HAND),"one real supplier wheat transferred");
            check(supplier.getMainHandItem().isEmpty(),"finite18wheat supply exhausted exactly once");
            record(server,"finite operator supply18wheat AFTER real unmet request");
        }
        if (ticks%200==0) record(server,"observe");
        if (requestedAt>=0 && p.foodSharedGiven>0 && r.foodSharedReceived>0 && r.mind.foodLevel()>hungerBeforeShare) {
            record(server,"completed real production/handoff/meal");
            check(p.breadCrafted>0,"native bread recipes completed");
            check(p.foodDemand.extraNutrition(server.overworld().getGameTime())==0,"handoff fulfilled the heard demand");
            check(producer.getInventory().countItem(Items.WHEAT)+3*p.breadCrafted==18,"all wheat conserved across actual recipes");
            check(producer.isAlive() && requester.isAlive(),"both synthetic fixture adults alive");
            write(server,"passed",null); System.out.println("FOOD ECONOMY PASS: request -> paid recipes -> real handoff -> native meal");
            server.halt(false);
        }
    }

    private void record(MinecraftServer server,String phase) {
        var row=new LinkedHashMap<String,Object>(); row.put("phase",phase); row.put("tick",server.overworld().getGameTime());
        if (producer!=null) for (var v:new Villager[]{producer,requester}) {
            var s=Villagers.existingSession(v.getUUID());
            row.put(v==producer?"producer":"requester",Map.of("id",v.getUUID().toString(),"food",s.mind.foodLevel(),"mood",s.mind.mood().band().name(),
                    "bread",v.getInventory().countItem(Items.BREAD),"wheat",v.getInventory().countItem(Items.WHEAT),"crafted",s.breadCrafted,
                    "given",s.foodSharedGiven,"received",s.foodSharedReceived,"target",FoodSupply.target(s,server.overworld().getGameTime()),"requestCount",s.foodHelpRequests));
        }
        events.add(row); System.out.println("FOOD ECONOMY EVENT: "+row);
        if(renewable) {
            int[] ages=new int[8];
            for(var pos:crops) {
                var state=server.overworld().getBlockState(pos);
                if(state.getBlock() instanceof CropBlock crop)ages[crop.getAge(state)]++;
            }
            row.put("cropAges",ages);row.put("harvestsByCell",new LinkedHashMap<>(harvestsByCell));
            row.put("nearbyObserverCount",server.overworld().players().size());row.put("earnedBreadInRequesterBag",earnedBread(requester));
        }
    }

    private void write(MinecraftServer server,String status,String failure) throws Exception {
        var result=new LinkedHashMap<String,Object>();result.put("status",status);result.put("failure",failure);result.put("events",events);
        result.put("fixture","NEW isolated supplied chamber; selected generous/frugal producer and glutton requester;80TPS;configured400base exhaustion/day;18operator wheat AFTER unmet request; no hunger/health/clock/age/learning/position setters after initial setup; no natural-survival or farming claim");
        if(renewable)result.put("fixture","NEW isolated48age-zero wheat farm with water/light/beds/table; selected founders; normal80base exhaustion/day and3random tick speed;80TPS; initial6marked bread each,64seeds,oneironhoe; stationary test observer; no supplied wheat, crop/clock/body setters, forced recipes or gifts after setup; not primary history or full-threat survival acceptance");
        Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve(previous==null?"food-economy-result.json":"food-economy-restart-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));
    }
    private static void check(boolean okay,String label) { if(!okay)throw new AssertionError(label); }
}
