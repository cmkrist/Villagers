package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

/** Native map acceptance: real noise generator, actual biome palettes, native jigsaw pieces and residents. */
public final class NativeRegionsServerProbe implements ModInitializer {
    private int ticks;
    private boolean loaded, finished;
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (finished) return;
            Path world = server.getWorldPath(LevelResource.ROOT), result = world.resolve("native-regions-result.json");
            try {
                check(++ticks < 1200, "native map acceptance timed out");
                var level = server.overworld();
                check(NativeRegionsScenario.nativeRegions(level), "vanilla noise generator with four-region biome source");
                check(server.getLevel(Level.NETHER) != null && server.getLevel(Level.END) != null, "standard adventure dimensions loaded");
                check(!Files.exists(world.resolve("four-biomes-war.json")), "authored war settlements were not constructed");
                var marker = JsonParser.parseString(Files.readString(world.resolve("native-four-regions.json"))).getAsJsonObject();
                var location = marker.getAsJsonArray("village");
                int cx = location.get(0).getAsInt() >> 4, cz = location.get(2).getAsInt() >> 4;
                if (!loaded) {
                    for (int x=cx-3;x<=cx+3;x++) for (int z=cz-3;z<=cz+3;z++) {
                        level.setChunkForced(x,z,true); level.getChunk(x,z);
                    }
                    loaded = true; return;
                }
                if (ticks < 40) return;
                var structures = new LinkedHashMap<String,Integer>();
                for (int x=cx-3;x<=cx+3;x++) for (int z=cz-3;z<=cz+3;z++)
                    for (var entry : level.getChunk(x,z).getAllStarts().entrySet()) if (entry.getValue().isValid()) {
                        String name = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(entry.getKey()).toString();
                        structures.merge(name, entry.getValue().getPieces().size(), Integer::sum);
                    }
                check(structures.entrySet().stream().anyMatch(e -> e.getKey().startsWith("minecraft:village_") && e.getValue()>1), "actual native village structure and jigsaw pieces");
                int villagers = 0;
                for (var entity : level.getAllEntities()) if (entity instanceof Villager v && v.isAlive()) {
                    check(!v.entityTags().contains(WarScenario.ACTOR), "native village resident, no authored founder tag"); villagers++;
                }
                check(villagers >= 2, "real naturally generated village inhabitants");
                var biomes = new LinkedHashMap<String,String>();
                int[] xs={-480,480,-480,480}, zs={-480,-480,480,480};
                String[] expected={"minecraft:plains","minecraft:forest","minecraft:frozen_peaks","minecraft:desert"};
                for (int i=0;i<4;i++) {
                    level.getChunk(xs[i]>>4,zs[i]>>4);
                    String biome = level.getBiome(new BlockPos(xs[i],80,zs[i])).getRegisteredName();
                    check(biome.equals(expected[i]), "actual biome palette in quarter"+i); biomes.put(xs[i]+","+zs[i], biome);
                }
                check(level.getWorldBorder().getSize()==1024, "original sixty-four-chunk map extent retained");
                Files.writeString(result, new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("status","passed",
                    "villagers",villagers,"structures",structures,"biomes",biomes,"gameTime",level.getGameTime(),
                    "generator",level.getChunkSource().getGenerator().getClass().getSimpleName(),"scope","Native terrain/village generation and reopen; no survival/growth/warfare claim")));
                finished=true;server.halt(false);
            } catch(Throwable failure) {
                failure.printStackTrace();finished=true;
                try {Files.writeString(result,new GsonBuilder().create().toJson(Map.of("status","failed","failure",failure.toString())));}catch(Exception ignored){}
                server.halt(false);
            }
        });
    }
}
