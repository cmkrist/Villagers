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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.LevelResource;

/** TEST MOD ONLY: declared supplied chamber, selected founder traits, natural loaded-time hunger. */
public final class FoodEconomyServerProbe implements ModInitializer {
    private Villager producer, requester;
    private ServerPlayer supplier;
    private int ticks, hungerBeforeShare;
    private long requestedAt = -1;
    private JsonObject previous;
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
                    } else chamber(server.overworld());
                }
                if (ticks == 60) setup(server);
                if (producer != null) {
                    if (previous!=null) reopen(server); else observe(server);
                }
                if (ticks > 6000) throw new AssertionError("No completed request/production/sharing cycle within6000 actual ticks");
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
        producer=new Villager(EntityTypes.VILLAGER,level); producer.setUUID(founder(true)); producer.snapTo(5.0,81,4.4,0,0);
        requester=new Villager(EntityTypes.VILLAGER,level); requester.setUUID(founder(false)); requester.snapTo(5.0,81,5.5,0,0);
        level.addFreshEntity(producer); level.addFreshEntity(requester);
        VillagerController.adopt(level,producer); VillagerController.adopt(level,requester);
        supplier=new ServerPlayer(server,level,new GameProfile(new UUID(0x46495854555245L,1),"FiniteSupplier"),ClientInformation.createDefault());
        supplier.snapTo(5.5,81,4.5,0,0); supplier.setShiftKeyDown(true);
        supplier.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WHEAT,18));
        record(server,"setup");
        check(Villagers.existingSession(producer.getUUID()).mind.home()!=null,"producer claimed actual bed");
        check(Villagers.existingSession(requester.getUUID()).mind.home()!=null,"requester claimed actual bed");
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
    }

    private void write(MinecraftServer server,String status,String failure) throws Exception {
        var result=new LinkedHashMap<String,Object>();result.put("status",status);result.put("failure",failure);result.put("events",events);
        result.put("fixture","NEW isolated supplied chamber; selected generous/frugal producer and glutton requester;80TPS;configured400base exhaustion/day;18operator wheat AFTER unmet request; no hunger/health/clock/age/learning/position setters after initial setup; no natural-survival or farming claim");
        Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve(previous==null?"food-economy-result.json":"food-economy-restart-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));
    }
    private static void check(boolean okay,String label) { if(!okay)throw new AssertionError(label); }
}
