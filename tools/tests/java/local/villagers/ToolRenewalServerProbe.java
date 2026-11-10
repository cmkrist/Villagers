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
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.LevelResource;

/** Isolated supplied fixture. Only the initial hoe has one use left; ordinary harvesting must break it. */
public final class ToolRenewalServerProbe implements ModInitializer {
    private static final UUID ACTOR=new UUID(0x544f4f4c52454e45L,1);
    private static final BlockPos SEED=new BlockPos(7,82,3), LOG=new BlockPos(8,81,5), PLANT=new BlockPos(8,81,7);
    private Villager worker;
    private int ticks,harvests;
    private long lastHarvest=-1;
    private boolean broke,renewedHarvest,reopen,continuing;
    private final boolean fullStorage=Boolean.getBoolean("villagers.toolRenewal.fullStorage");
    private final boolean partialRestart=Boolean.getBoolean("villagers.toolRenewal.partialRestart");
    private JsonObject previous;
    private final ArrayList<Map<String,Object>> events=new ArrayList<>();

    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                ticks++;
                if(ticks==1) {server.overworld().setChunkForced(0,0,true);server.overworld().getChunk(0,0);server.tickRateManager().setTickRate(80);}
                if(ticks==40) {
                    var file=server.getWorldPath(LevelResource.ROOT).resolve(System.getProperty("villagers.toolRenewal.restartFrom","tool-renewal-result.json"));
                    reopen=Files.exists(file);
                    if(reopen) {
                        previous=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                        continuing=partialRestart && previous.get("status").getAsString().equals("partial-passed");
                        check(continuing || previous.get("status").getAsString().equals("passed"),"only passed fixture checkpoint may reopen");
                    } else terrain(server.overworld());
                }
                if(ticks==60)setup(server);
                if(worker!=null) {
                    check(worker.isAlive() && Villagers.isManaged(worker),"same managed adult remains alive");
                    if(reopen && !continuing && ticks==160)finish(server);
                    if(continuing && Villagers.existingSession(ACTOR).propagulesPlanted==1 && worker.getMainHandItem().is(Items.WOODEN_HOE))finish(server);
                    if(!reopen)observe(server);
                }
                if(ticks>5000)throw new AssertionError("Tool renewal observation exhausted");
            } catch(Throwable failure) {
                failure.printStackTrace();try{write(server,"failed",failure.toString());}catch(Exception ignored){}
                server.halt(false);
            }
        });
    }
    private static void terrain(ServerLevel level) {
        for(int x=1;x<=11;x++)for(int z=1;z<=10;z++)for(int y=79;y<=84;y++)
            level.setBlock(new BlockPos(x,y,z),y<=80 || y<=83 && (x==1 || x==11 || z==1 || z==10)
                    ?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        for(int z:new int[]{4,6}) {
            level.setBlock(new BlockPos(5,80,z),Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7),3);
            level.setBlock(new BlockPos(5,81,z),((CropBlock)Blocks.WHEAT).getStateForAge(7),3);
        }
        level.setBlock(new BlockPos(6,80,7),Blocks.WATER.defaultBlockState(),3);
        level.setBlock(new BlockPos(6,81,7),Blocks.STONE.defaultBlockState(),3);
        var bed=Blocks.BED.white().defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH);
        level.setBlock(new BlockPos(3,81,3),bed.setValue(BedBlock.PART,BedPart.FOOT),3);
        level.setBlock(new BlockPos(3,81,4),bed.setValue(BedBlock.PART,BedPart.HEAD),3);
        level.setBlock(new BlockPos(4,81,3),Blocks.CRAFTING_TABLE.defaultBlockState(),3);
        level.setBlock(SEED.above(),Blocks.MANGROVE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),3);
        level.setBlock(SEED,Blocks.MANGROVE_PROPAGULE.defaultBlockState().setValue(MangrovePropaguleBlock.HANGING,true)
                .setValue(MangrovePropaguleBlock.AGE,4),3);
        level.setBlock(LOG,Blocks.MANGROVE_LOG.defaultBlockState(),3);
        level.setBlock(LOG.above(),Blocks.MANGROVE_LOG.defaultBlockState(),3);
        level.setBlock(PLANT.below(),Blocks.DIRT.defaultBlockState(),3);
    }
    private void setup(MinecraftServer server) {
        var level=server.overworld();
        if(reopen) {
            worker=(Villager)level.getEntity(ACTOR);check(worker!=null,"same saved body reloaded");
            Villagers.session(worker,level);record(server,"reopened without resident, resource or terrain setters");return;
        }
        worker=new Villager(EntityTypes.VILLAGER,level);worker.setUUID(ACTOR);worker.snapTo(5.5,81,5.5,0,0);
        var hoe=new ItemStack(Items.IRON_HOE);hoe.setDamageValue(hoe.getMaxDamage()-1);
        worker.setItemSlot(EquipmentSlot.MAINHAND,hoe);
        worker.getInventory().addItem(new ItemStack(Items.WHEAT_SEEDS,8));
        worker.getInventory().addItem(new ItemStack(Items.BREAD,fullStorage?6:2));
        if(fullStorage)for(int i=0;i<6;i++)worker.getInventory().addItem(new ItemStack(Items.EMERALD,64));
        level.addFreshEntity(worker);VillagerController.adopt(level,worker);
        var operator=new ServerPlayer(server,level,new GameProfile(new UUID(0x544f4f4c4f504552L,1),"ToolOperator"),ClientInformation.createDefault()) {
            @Override public void sendSystemMessage(net.minecraft.network.chat.Component message) {
                System.out.println("TOOL OPERATOR: "+message.getString());
            }
        };
        operator.snapTo(7.5,81,5.5,0,0);
        boolean[] okay={false};var source=operator.createCommandSourceStack().withPermission(server.createCommandSourceStack().permissions())
                .withCallback((success,result)->okay[0]=success && result>0);
        server.getCommands().performPrefixedCommand(source,"villagers site forestry 7 82 3 8 81 5 2 8 81 7");
        check(okay[0],"native command approved two exact logs, one hanging seed and paid planting obligation");
        check(worker.getInventory().countItem(Items.MANGROVE_LOG)==0,"no initially supplied wood");
        record(server,"declared initial fixture; all subsequent work uses ordinary controller");
    }
    private void observe(MinecraftServer server) throws Exception {
        var s=Villagers.existingSession(ACTOR);var receipt=AdventureActions.lastHarvest(ACTOR);
        if(fullStorage) {
            if(ticks==300) {
                check(worker.getMainHandItem().is(Items.IRON_HOE) && worker.getMainHandItem().getDamageValue()==249,"full bag preserves actual held tool");
                check(worker.getInventory().countItem(Items.EMERALD)==384 && worker.getInventory().countItem(Items.BREAD)==6
                        && worker.getInventory().countItem(Items.WHEAT_SEEDS)==8,"no storage loss or invented space");
                check(s.logsGathered==0 && s.propagulesGathered==0 && s.propagulesPlanted==0 && s.hoesCrafted==0,"blocked storage performs no resource or recipe work");
                check(ConstructionSites.forestry(server.overworld()).size()==1 && ForestryWork.matureSeed(server.overworld(),SEED)
                        && server.overworld().getBlockState(LOG).is(Blocks.MANGROVE_LOG) && server.overworld().getBlockState(PLANT).isAir(),"blocked storage preserves exact terrain and grant");
                record(server,"full-storage denial preserved equipment, materials, terrain and authority");write(server,"passed",null);server.halt(false);
            }
            return;
        }
        if(!receipt.isEmpty() && ((Number)receipt.get("gameTime")).longValue()!=lastHarvest) {
            lastHarvest=((Number)receipt.get("gameTime")).longValue();harvests++;
            if(Boolean.TRUE.equals(receipt.get("hoeBroke")))broke=true;
            else if(broke && s.hoesCrafted==1 && worker.getMainHandItem().is(Items.WOODEN_HOE))renewedHarvest=true;
            events.add(Map.of("phase","native harvest receipt","receipt",receipt));
            record(server,"harvest observed");
        }
        if(ticks%200==0)record(server,"observe");
        if(partialRestart && renewedHarvest && s.logsGathered==1 && s.propagulesPlanted==0) {
            check(ConstructionSites.forestry(server.overworld()).size()==1 && worker.getInventory().countItem(Items.MANGROVE_PROPAGULE)==1,"partial job retains actual seed and outstanding authority");
            record(server,"clean partial checkpoint after real wear, replacement and resumed harvest; planting still owed");
            write(server,"partial-passed",null);server.halt(false);return;
        }
        if(renewedHarvest && s.propagulesPlanted==1 && worker.getMainHandItem().is(Items.WOODEN_HOE))finish(server);
    }
    private void finish(MinecraftServer server) throws Exception {
        var level=server.overworld();var s=Villagers.existingSession(ACTOR);
        check(level.getBlockState(LOG).isAir() && level.getBlockState(LOG.above()).isAir(),"only exact logs consumed");
        check(level.getBlockState(SEED).isAir() && level.getBlockState(PLANT).is(Blocks.MANGROVE_PROPAGULE),"real seed gathered and planted");
        check(ConstructionSites.forestry(level).isEmpty(),"finite forestry authority retired");
        check(worker.getMainHandItem().is(Items.WOODEN_HOE) && worker.getMainHandItem().getDamageValue()==1,"replacement used once in actual resumed harvest");
        check(worker.getInventory().countItem(Items.MANGROVE_LOG)==1 && worker.getInventory().countItem(Items.MANGROVE_PLANKS)==0
                && worker.getInventory().countItem(Items.STICK)==2,"one gathered log funds four planks, four sticks, one hoe; other log retained");
        check(worker.getInventory().countItem(Items.IRON_HOE)==0 && worker.getInventory().countItem(Items.MANGROVE_PROPAGULE)==0,"broken tool and planted seed not replayed");
        if(continuing) {
            check(s.logsGathered==1 && s.propagulesGathered==0 && s.propagulesPlanted==1
                    && s.hoesCrafted==0 && s.woodRecipesCrafted==0,"saved partial forestry resumes without replaying seed, first log or recipes");
            var last=previous.getAsJsonArray("events").get(previous.getAsJsonArray("events").size()-1).getAsJsonObject();
            var expected=counts(last.getAsJsonArray("inventory").asList().stream().map(e->e.getAsString()).toList());
            expected.merge("minecraft:mangrove_propagule",-1,Integer::sum);expected.remove("minecraft:mangrove_propagule",0);
            expected.merge("minecraft:mangrove_log",1,Integer::sum);
            check(counts(inventory()).equals(expected),"resumption adds only second granted log and spends only retained planting seed");
        } else if(reopen) {
            var last=previous.getAsJsonArray("events").get(previous.getAsJsonArray("events").size()-1).getAsJsonObject();
            check(s.mind.foodLevel()==last.get("food").getAsInt(),"saved meal nutrition retained");
            var expected=last.getAsJsonArray("inventory").asList().stream().map(e->e.getAsString())
                    .filter(e->!e.startsWith("0 ")).sorted().toList();
            check(inventory().stream().filter(e->!e.startsWith("0 ")).sorted().toList().equals(expected),"native saved items preserved despite slot compaction");
            check(s.hoesCrafted==0 && s.woodRecipesCrafted==0 && s.logsGathered==0 && s.propagulesPlanted==0,"no resource or recipe replay after reopen");
        } else {
            check(broke && harvests==2 && renewedHarvest,"first native harvest broke initial hoe; second used earned replacement");
            check(s.logsGathered==2 && s.propagulesGathered==1 && s.propagulesPlanted==1,"paid native forestry chain");
            check(s.hoesCrafted==1 && s.woodRecipesCrafted==3,"exact native log/plank/stick/hoe recipe chain");
        }
        record(server,continuing?"partial forestry resumed from native save, paid planting and retired authority":reopen?"restart saved items and no replay passed":"actual wear, forestry, tool replacement and resumed harvest passed");
        write(server,reopen && !continuing?"restart-passed":"passed",null);server.halt(false);
    }
    private void record(MinecraftServer server,String phase) {
        var s=Villagers.existingSession(ACTOR);var row=new LinkedHashMap<String,Object>();
        row.put("phase",phase);row.put("tick",server.overworld().getGameTime());row.put("food",s.mind.foodLevel());
        row.put("inventory",inventory());row.put("held",worker.getMainHandItem().toString());
        row.put("hoeDamage",worker.getMainHandItem().getDamageValue());row.put("hoesCrafted",s.hoesCrafted);row.put("recipes",s.woodRecipesCrafted);
        row.put("logsGathered",s.logsGathered);row.put("seedGathered",s.propagulesGathered);row.put("seedPlanted",s.propagulesPlanted);
        row.put("forestryStatus",s.forestryStatus);row.put("toolStatus",s.toolStatus);
        row.put("remainingJobs",ConstructionSites.forestry(server.overworld()).size());
        row.put("meanServerTickMillis",server.getAverageTickTimeNanos()/1e6);events.add(row);
        System.out.println("TOOL RENEWAL EVENT: "+new GsonBuilder().create().toJson(row));
    }
    private java.util.List<String> inventory() {
        return HarvestMaterials.snapshot(worker.getInventory()).stream().map(ItemStack::toString).toList();
    }
    private static Map<String,Integer> counts(java.util.List<String> items) {
        var totals=new java.util.TreeMap<String,Integer>();
        for(var value:items) {
            var parts=value.split(" ",2);int count=Integer.parseInt(parts[0]);
            if(count>0)totals.merge(parts[1],count,Integer::sum);
        }
        return totals;
    }
    private void write(MinecraftServer server,String status,String failure) throws Exception {
        var result=new LinkedHashMap<String,Object>();result.put("status",status);result.put("failure",failure);result.put("events",events);
        result.put("fixture","NEW isolated supplied platform,bed,table,water,two mature wheat,eight seeds,two bread,two mangrove logs,hanging propagule,plant soil; one adult and an iron hoe with one use left. Native exact forestry grant.80TPS,normal80base exhaustion/day. No supplies, resident, clock, growth or terrain setters after setup. Tests real wear and replacement, not renewable tree regrowth or survival adequacy.");
        if(fullStorage)result.put("fixture","NEW isolated storage-denial variant: same supplied terrain, adult with eight seeds,six bread,384emeralds filling all eight inventory slots,and iron hoe with one use left. Existing exact forestry grant. No post-setup supplies or resident/terrain setters. No survival/economic-wealth claim.");
        Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve(continuing?"tool-renewal-resume-result.json":reopen?"tool-renewal-restart-result.json":"tool-renewal-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));
    }
    private static void check(boolean condition,String label) {if(!condition)throw new AssertionError(label);}
}
