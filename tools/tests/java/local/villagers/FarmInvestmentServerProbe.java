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
import java.util.regex.Pattern;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.LevelResource;

/** TEST MOD ONLY: real command grants, protection, ordinary investment and mixed planting while stocked. */
public final class FarmInvestmentServerProbe implements ModInitializer {
    private static final BlockPos A=new BlockPos(6,80,5),B=new BlockPos(6,80,6);
    private static final UUID ACTOR=new UUID(0x4641524d494e5645L,1);
    private Villager worker;
    private ServerPlayer operator;
    private UUID protection;
    private int ticks;
    private boolean reopen;
    private final ArrayList<Map<String,Object>> events=new ArrayList<>();
    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                ticks++;
                if(ticks==1) {server.overworld().setChunkForced(0,0,true);server.overworld().getChunk(0,0);server.tickRateManager().setTickRate(80);}
                if(ticks==40) {
                    var result=server.getWorldPath(LevelResource.ROOT).resolve("food-economy-result.json");
                    reopen=Files.exists(result);
                    if(reopen)check(JsonParser.parseString(Files.readString(result)).getAsJsonObject().get("status").getAsString().equals("passed"),"reopen only a passed fixture");
                    else terrain(server.overworld());
                }
                if(ticks==60)setup(server);
                if(worker==null)return;
                if(!reopen && ticks==200) {
                    check(worker.getInventory().countItem(Items.WHEAT_SEEDS)==8 && worker.getInventory().countItem(Items.BEETROOT_SEEDS)==8,"protection blocked all seed spending");
                    check(worker.getMainHandItem().getDamageValue()==0,"protection blocked hoe costs");
                    check(!ConstructionSites.allowsFarm(server.overworld(),A) && !ConstructionSites.allowsFarm(server.overworld(),B),"protection wins over explicit grants");
                    record(server,"protected: no costs or terrain changes");command(server,"villagers site remove "+protection);
                    check(ConstructionSites.allowsFarm(server.overworld(),A) && ConstructionSites.allowsFarm(server.overworld(),B),"unprotected finite grants remain");
                }
                var s=Villagers.existingSession(ACTOR);
                if(!reopen && ticks>200 && s.plotsCultivated==2)finish(server,false);
                if(reopen && ticks==200)finish(server,true);
                if(ticks>1200)throw new AssertionError("Ordinary controller did not complete the two approved plots");
            }catch(Throwable error) {
                error.printStackTrace();try{write(server,"failed",error.toString());}catch(Exception ignored){}
                System.out.println("FARM INVESTMENT FAILED");server.halt(false);
            }
        });
    }
    private static void terrain(ServerLevel level) {
        for(int x=1;x<=10;x++)for(int z=1;z<=10;z++)for(int y=79;y<=84;y++)
            level.setBlock(new BlockPos(x,y,z),y<=80 || (y<=83 && (x==1 || x==10 || z==1 || z==10))
                    ?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        for(int x=2;x<=4;x++)for(int z=2;z<=5;z++)level.setBlock(new BlockPos(x,84,z),Blocks.STONE.defaultBlockState(),3);
        var bed=Blocks.BED.white().defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH);
        level.setBlock(new BlockPos(3,81,3),bed.setValue(BedBlock.PART,BedPart.FOOT),3);
        level.setBlock(new BlockPos(3,81,4),bed.setValue(BedBlock.PART,BedPart.HEAD),3);
        level.setBlock(A,Blocks.DIRT.defaultBlockState(),3);level.setBlock(B,Blocks.DIRT.defaultBlockState(),3);
        level.setBlock(new BlockPos(5,80,4),Blocks.FARMLAND.defaultBlockState(),3);
        level.setBlock(new BlockPos(5,81,4),Blocks.WHEAT.defaultBlockState(),3);
        level.setBlock(new BlockPos(7,80,7),Blocks.WATER.defaultBlockState(),3);
        level.setBlock(new BlockPos(7,81,7),Blocks.STONE.defaultBlockState(),3);
    }
    private void setup(MinecraftServer server) {
        var level=server.overworld();
        if(reopen) {
            worker=(Villager)level.getEntity(ACTOR);check(worker!=null,"same saved body reloaded; no replacement");
            Villagers.session(worker,level);record(server,"reopen without supplies or world writes");return;
        }
        worker=new Villager(EntityTypes.VILLAGER,level);worker.setUUID(ACTOR);worker.snapTo(5.5,81,5.5,0,0);
        worker.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_HOE));
        worker.getInventory().addItem(new ItemStack(Items.BREAD,6));
        worker.getInventory().addItem(new ItemStack(Items.WHEAT_SEEDS,8));worker.getInventory().addItem(new ItemStack(Items.BEETROOT_SEEDS,8));
        level.addFreshEntity(worker);VillagerController.adopt(level,worker);
        operator=new ServerPlayer(server,level,new GameProfile(new UUID(0x4f50455241544f52L,1),"FarmOperator"),ClientInformation.createDefault()) {
            @Override public void sendSystemMessage(Component message) {
                var text=message.getString();System.out.println("FARM OPERATOR: "+text);
                if(text.contains("Protected area saved")) {
                    var match=Pattern.compile("[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}").matcher(text);
                    if(match.find())protection=UUID.fromString(match.group());
                }
            }
        };
        operator.snapTo(6.5,81,5.5,0,0);
        command(server,"villagers site protect 6 80 5 6 82 6");check(protection!=null,"real protection command recorded its identity");
        command(server,"villagers site farm 1");record(server,"stocked setup and exact finite command grants");
    }
    private void command(MinecraftServer server,String text) {
        boolean[] okay={false};
        var source=operator.createCommandSourceStack().withPermission(server.createCommandSourceStack().permissions())
                .withCallback((success,result)->okay[0]=success && result>0);
        server.getCommands().performPrefixedCommand(source,text);check(okay[0],"native command succeeded: "+text);
    }
    private void finish(MinecraftServer server,boolean restart) throws Exception {
        var level=server.overworld();var s=Villagers.existingSession(ACTOR);
        check(worker.isAlive() && Villagers.isManaged(worker),"same managed adult survives");
        check(level.getBlockState(A.above()).is(Blocks.BEETROOTS) && level.getBlockState(B.above()).is(Blocks.WHEAT),"visible layout selected paid beetroot alternative beside wheat");
        check(level.getBlockState(A).is(Blocks.FARMLAND) && level.getBlockState(B).is(Blocks.FARMLAND),"both soils tilled natively");
        check(worker.getInventory().countItem(Items.WHEAT_SEEDS)==7 && worker.getInventory().countItem(Items.BEETROOT_SEEDS)==7,"one real seed paid per plot; seed banks retained");
        check(worker.getMainHandItem().getDamageValue()==2,"two actual hoe costs, never replayed");
        check(worker.getInventory().countItem(Items.BREAD)==6 && !s.selfFoodGoal,"investment happened before personal reserve ran out");
        check(!ConstructionSites.allowsFarm(level,A) && !ConstructionSites.allowsFarm(level,B),"completed finite grants retired");
        if(restart)check(s.plotsCultivated==0,"no plot replay in reopened session");
        record(server,restart?"restart preserved plots/materials with no replay":"ordinary stocked controller completed paid mixed planting");
        write(server,restart?"restart-passed":"passed",null);
        System.out.println(restart?"FARM INVESTMENT RESTART PASS":"FARM INVESTMENT PASS");server.halt(false);
    }
    private void record(MinecraftServer server,String phase) {
        var s=Villagers.existingSession(ACTOR);var row=new LinkedHashMap<String,Object>();row.put("phase",phase);row.put("tick",server.overworld().getGameTime());
        row.put("plotsThisLoad",s.plotsCultivated);row.put("food",s.mind.foodLevel());row.put("bread",worker.getInventory().countItem(Items.BREAD));
        row.put("wheatSeeds",worker.getInventory().countItem(Items.WHEAT_SEEDS));row.put("beetSeeds",worker.getInventory().countItem(Items.BEETROOT_SEEDS));
        row.put("hoeWear",worker.getMainHandItem().getDamageValue());row.put("selfFoodGoal",s.selfFoodGoal);
        row.put("A",server.overworld().getBlockState(A.above()).toString());row.put("B",server.overworld().getBlockState(B.above()).toString());events.add(row);
        System.out.println("FARM INVESTMENT EVENT: "+new GsonBuilder().create().toJson(row));
    }
    private void write(MinecraftServer server,String status,String failure) throws Exception {
        var result=new LinkedHashMap<String,Object>();result.put("status",status);result.put("failure",failure);result.put("events",events);
        result.put("fixture","NEW isolated supplied platform,one existing age-zero wheat,two bare dirt cells,water/bed; one adult with6bread,8seeds each,onehoe; explicit operator protect/farm/remove commands; ordinary controller plants;80TPS,normal80exhaustion/day; no post-setup supplies or body/clock/growth setters; no primary world or survival adequacy claim");
        Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve(reopen?"food-economy-restart-result.json":"food-economy-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));
    }
    private static void check(boolean okay,String label) {if(!okay)throw new AssertionError(label);}
}
