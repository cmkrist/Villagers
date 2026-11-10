package local.villagers;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import local.villagers.war.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.storage.LevelResource;

/** Native world-generation acceptance. Loaded structures and entities, not merely generator metadata. */
public final class WarWorldServerProbe implements ModInitializer {
    private int ticks;
    private boolean finished;
    private static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if(finished)return;
            try {
                if(++ticks>1200)throw new AssertionError("Scenario setup timed out");
                var level=server.overworld();
                if(!WarScenario.active(level) || ticks<20)return;
                check(level.getChunkSource().getGenerator() instanceof WarChunkGenerator,"custom generator loaded");
                check(level.getWorldBorder().getSize()==1024,"exact64-chunk border");
                check(server.getRespawnData().pos().equals(new BlockPos(-96,75,-88)),"player spawn at Greenhaven");
                var samples=new ArrayList<Map<String,Object>>();
                String[] expected={"minecraft:plains","minecraft:forest","minecraft:frozen_peaks","minecraft:desert"};
                for(var town:WarMap.SETTLEMENTS) {
                    int index=WarMap.SETTLEMENTS.indexOf(town);
                    int y=WarMap.height(town.x(),town.z())+1;
                    var pos=new BlockPos(town.x(),y,town.z());
                    String biome=level.getBiome(pos).getRegisteredName();
                    check(biome.equals(expected[index]),"town biome "+town.name()+" got "+biome);
                    int beds=0;
                    for(int x=town.x()-23;x<=town.x()+23;x++)for(int z=town.z()-23;z<=town.z()+23;z++)
                        if(level.getBlockState(new BlockPos(x,y,z)).getBlock() instanceof BedBlock) beds++;
                    check(beds==16,"eight complete beds in "+town.name()+" got "+beds);
                    var residents=java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false)
                        .filter(e->e.entityTags().contains(WarScenario.ACTOR) && e.entityTags().contains(WarScenario.HOME+index)).toList();
                    check(residents.stream().filter(e->e instanceof Villager || e instanceof AbstractIllager).count()==8,"eight residents "+town.name());
                    check(residents.stream().anyMatch(e->e instanceof WanderingTrader),"trader "+town.name());
                    check(residents.stream().anyMatch(e->e instanceof Enderman),"Enderman "+town.name());
                    samples.add(Map.of("town",town.name(),"biome",biome,"beds",beds,"actors",residents.size()));
                }
                // Sample the actual biome palettes and terrain in the remote quarters as well.
                for(int x:new int[]{-480,480})for(int z:new int[]{-480,480}) {
                    level.getChunk(x>>4,z>>4);
                    var pos=new BlockPos(x,WarMap.height(x,z),z);
                    check(level.getBiome(pos).getRegisteredName().equals(expected[WarMap.region(x,z).ordinal()]),"remote biome");
                    check(!level.getBlockState(pos).isAir(),"remote generated terrain");
                    check(level.getChunk(x>>4,z>>4).getAllStarts().isEmpty(),"no extra native structures");
                }
                long actors=java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false)
                    .filter(e->e.entityTags().contains(WarScenario.ACTOR)).count();
                check(actors==40,"forty founders in actual loaded world got "+actors);
                Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("war-world-result.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("status","passed","actors",actors,"towns",samples,"gameTime",level.getGameTime())));
                finished=true; server.halt(false);
            } catch(Throwable failure) {
                failure.printStackTrace(); finished=true;
                try { Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("war-world-result.json"),
                    new GsonBuilder().create().toJson(Map.of("status","failed","failure",failure.toString()))); } catch(Exception ignored) {}
                server.halt(false);
            }
        });
    }
}
