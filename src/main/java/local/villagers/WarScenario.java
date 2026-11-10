package local.villagers;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import local.villagers.war.WarChunkGenerator;
import local.villagers.war.WarMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.LevelResource;

/** First-generation scenario construction. Never alters other worlds or replays an active scenario. */
public final class WarScenario {
    public static final String ACTOR = "villagers:war_actor";
    public static final String SOLDIER = "villagers:war_soldier";
    public static final String HOME = "villagers:war_home:";
    private static ServerLevel level;
    private static Path marker;
    private static int next;
    private static boolean active;
    private WarScenario() {}
    public static boolean active(ServerLevel world) { return world == level && active; }
    /** Ordinary paid births join a parent's town when their actual nursery is there. */
    static void inheritTown(ServerLevel world, Villager first, Villager second, Villager baby, BlockPos nursery) {
        if (!active(world)) return;
        for (int i = 0; i < WarMap.SETTLEMENTS.size(); i++) {
            var town = WarMap.SETTLEMENTS.get(i);
            if (town.faction() != WarMap.Faction.VILLAGERS
                || Math.abs(nursery.getX() - town.x()) > 34 || Math.abs(nursery.getZ() - town.z()) > 34) continue;
            boolean parentLivesHere = (first.entityTags().contains(ACTOR) && first.entityTags().contains(HOME + i))
                || (second.entityTags().contains(ACTOR) && second.entityTags().contains(HOME + i));
            if (!parentLivesHere) continue;
            baby.addTag(ACTOR);
            baby.addTag(HOME + i);
            return;
        }
    }
    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            level = null; active = false; next = 0;
            if (!(server.overworld().getChunkSource().getGenerator() instanceof WarChunkGenerator)) return;
            level = server.overworld();
            marker = server.getWorldPath(LevelResource.ROOT).resolve("four-biomes-war.json");
            try {
                if (Files.exists(marker)) {
                    var saved = com.google.gson.JsonParser.parseString(Files.readString(marker)).getAsJsonObject();
                    if (!saved.get("state").getAsString().equals("active"))
                        throw new IllegalStateException("Interrupted scenario setup; inspect the new world before resuming. Setup will not replay.");
                    active = true; next = 4;
                } else {
                    // Write before construction; interruption must not duplicate founders or restore deaths.
                    write("preparing");
                    level.getWorldBorder().setCenter(0,0);
                    level.getWorldBorder().setSize(WarMap.SIZE);
                    level.getWorldBorder().setWarningBlocks(8);
                    server.setRespawnData(net.minecraft.world.level.storage.LevelData.RespawnData.of(
                        net.minecraft.world.level.Level.OVERWORLD,new BlockPos(-96,75,-88),90,0));
                    // Keep the four towns and intervening battlefield ticking without an observer.
                }
                // Also upgrade active saves. Residents may explore beyond their home before returning;
                // loading a border chunk is insufficient: it must keep ticking their AI and metabolism.
                for (int x=WarMap.SIMULATION_MIN_CHUNK;x<=WarMap.SIMULATION_MAX_CHUNK;x++)
                    for (int z=WarMap.SIMULATION_MIN_CHUNK;z<=WarMap.SIMULATION_MAX_CHUNK;z++)
                        level.setChunkForced(x,z,true);
            } catch (Exception failure) {
                Villagers.LOG.error("War scenario initialization stopped to preserve the world", failure);
                level = null;
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (level == null || active || !Villagers.ready()) return;
            try {
                construct(WarMap.SETTLEMENTS.get(next));
                next++;
                if (next == WarMap.SETTLEMENTS.size()) {
                    active = true; write("active");
                    Villagers.LOG.info("Four Biomes War ready: four settlements, border1024, four32x32-chunk regions");
                }
            } catch (Exception failure) {
                Villagers.LOG.error("War scenario construction stopped; no replay will occur", failure);
                level = null;
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { level=null; active=false; });
    }
    private static void write(String state) throws Exception {
        var data = Map.of("schema",1,"state",state,"world",WarMap.WORLD,"chunks",WarMap.CHUNKS,
            "quarterChunks",WarMap.QUARTER_CHUNKS,"settlements",WarMap.SETTLEMENTS,
            "startingSupplies","Authored scenario: buildings, farms, weapons and 24 bread per villager. No reinforcements or death restoration.");
        Path temporary = marker.resolveSibling(marker.getFileName()+".tmp");
        Files.writeString(temporary,new GsonBuilder().setPrettyPrinting().create().toJson(data));
        Files.move(temporary,marker,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }
    private static void put(int x,int y,int z,BlockState state) { level.setBlock(new BlockPos(x,y,z),state,3); }
    private static void put(int x,int y,int z,Block block) { put(x,y,z,block.defaultBlockState()); }
    private static void construct(WarMap.Settlement town) {
        int cx=town.x(), cz=town.z(), ground=WarMap.height(cx,cz), y=ground+1;
        boolean desert=town.region()==WarMap.Region.DESERT;
        Block wall=desert?Blocks.SANDSTONE:Blocks.OAK_PLANKS;
        Block roof=town.faction()==WarMap.Faction.ILLAGERS?Blocks.DARK_OAK_PLANKS:Blocks.OAK_PLANKS;
        // The terrace is part of generation. Clear only the authored town footprint before population.
        for (int x=cx-29;x<=cx+29;x++) for (int z=cz-29;z<=cz+29;z++) {
            for (int h=y;h<=y+16;h++) put(x,h,z,Blocks.AIR);
            if (Math.abs(x-cx)<=2 || Math.abs(z-cz)<=2) put(x,ground,z,Blocks.GRAVEL);
        }
        put(cx,y,cz,Blocks.BELL);
        for (int dx : new int[]{-18,18}) for (int dz : new int[]{-18,18}) {
            int hx=cx+dx,hz=cz+dz;
            for (int x=hx-4;x<=hx+4;x++) for (int z=hz-4;z<=hz+4;z++) {
                put(x,ground,z,Blocks.COBBLESTONE);
                for (int h=y;h<y+4;h++)
                    put(x,h,z,Math.abs(x-hx)==4 || Math.abs(z-hz)==4?wall:Blocks.AIR);
                put(x,y+4,z,roof);
            }
            put(hx,y,hz+4,Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.SOUTH));
            put(hx,y+1,hz+4,Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.SOUTH)
                .setValue(DoorBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
            put(hx-4,y+2,hz,Blocks.GLASS_PANE); put(hx+4,y+2,hz,Blocks.GLASS_PANE);
            for(int bx:new int[]{hx-2,hx+2}) {
                put(bx,y,hz-1,Blocks.BED.red().defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH).setValue(BedBlock.PART,BedPart.FOOT));
                put(bx,y,hz-2,Blocks.BED.red().defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH).setValue(BedBlock.PART,BedPart.HEAD));
            }
            put(hx,y,hz,Blocks.TORCH);
            put(hx+2,y,hz+2,Blocks.CRAFTING_TABLE);
        }
        for(int dx:new int[]{-12,12}) {
            int fx=cx+dx;
            for(int x=fx-4;x<=fx+4;x++) for(int z=cz-4;z<=cz+4;z++) {
                if(x==fx && z==cz) {put(x,ground,z,Blocks.WATER);continue;}
                put(x,ground,z,Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7));
                put(x,y,z,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));
            }
            put(fx,y,cz,Blocks.OAK_SLAB);
            put(fx,y,cz+6,Blocks.COMPOSTER);
        }
        for(int i=0;i<8;i++) {
            Mob mob;
            if(town.faction()==WarMap.Faction.VILLAGERS) mob=new Villager(EntityTypes.VILLAGER,level);
            else mob=i<6?new Pillager(EntityTypes.PILLAGER,level):new Vindicator(EntityTypes.VINDICATOR,level);
            mob.snapTo(cx-7+i*2,y,cz+7,0,0);
            mob.finalizeSpawn(level,level.getCurrentDifficultyAt(mob.blockPosition()),EntitySpawnReason.COMMAND,null);
            mob.setPersistenceRequired(); mob.addTag(ACTOR); mob.addTag(HOME+WarMap.SETTLEMENTS.indexOf(town));
            if(i<4) mob.addTag(SOLDIER);
            if(mob instanceof Villager villager) {
                mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(i<4?Items.IRON_SWORD:Items.IRON_HOE));
                villager.getInventory().addItem(new ItemStack(Items.BREAD,24));
                if(i>=4) villager.getInventory().addItem(new ItemStack(Items.WHEAT_SEEDS,32));
            } else {
                mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(mob instanceof Pillager?Items.CROSSBOW:Items.IRON_AXE));
                mob.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.BREAD,24));
            }
            if(!level.addFreshEntity(mob)) throw new IllegalStateException("Could not add founder");
            if(mob instanceof Villager villager) {
                Villagers.adopt(level,villager);
                int hx=cx+(i<4?-18:18), hz=cz+(i%4<2?-18:18), bx=hx+(i%2==0?-2:2);
                VillagerController.setHome(level,villager,Villagers.session(villager,level),new BlockPos(bx,y,hz-2));
            } else CreatureAgents.adopt(level,mob);
        }
        WanderingTrader trader=new WanderingTrader(EntityTypes.WANDERING_TRADER,level);
        trader.snapTo(cx,y,cz+12,0,0); trader.setDespawnDelay(Integer.MAX_VALUE); trader.setPersistenceRequired();
        trader.addTag(ACTOR); trader.addTag(HOME+WarMap.SETTLEMENTS.indexOf(town));
        trader.setCustomName(Component.literal(town.name()+" trader")); level.addFreshEntity(trader);
        trader.getInventory().addItem(new ItemStack(Items.BREAD,24));CreatureAgents.adopt(level,trader);
        Enderman enderman=new Enderman(EntityTypes.ENDERMAN,level);
        enderman.snapTo(cx+26,y,cz+26,0,0); enderman.setPersistenceRequired(); enderman.addTag(ACTOR);
        enderman.addTag(HOME+WarMap.SETTLEMENTS.indexOf(town)); level.addFreshEntity(enderman);
        CreatureAgents.adopt(level,enderman);
        Villagers.LOG.info("Built {} in {} with eight residents, a trader and an Enderman",town.name(),town.region());
    }
}
