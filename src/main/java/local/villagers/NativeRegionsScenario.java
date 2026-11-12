package local.villagers;

import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import local.villagers.war.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;

/** Native terrain and structures with four fixed biome regions. Never authors houses, mobs or supplies. */
public final class NativeRegionsScenario {
    static boolean enrollResident(ServerLevel level,net.minecraft.world.entity.npc.villager.Villager v){
        if(!nativeRegions(level)||!v.isAlive()||!level.getWorldBorder().isWithinBounds(v.blockPosition())||v.entityTags().contains("villagers:native_resident_seen"))return false;
        boolean adopted=!Villagers.isManaged(v);
        if(adopted){
            Villagers.adopt(level,v);
            Villagers.session(v,level).mind.remember("Joined native-region learning with my existing body and possessions");
            Villagers.LOG.info("Adopted native-region resident {} at {}; no body or supplies replaced",v.getUUID(),v.blockPosition());
        }
        v.addTag("villagers:native_resident_seen");return adopted;
    }
    public static boolean nativeRegions(ServerLevel level) {
        return level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator
            && level.getChunkSource().getGenerator().getBiomeSource() instanceof QuadrantBiomes;
    }
    public static void initialize() {
        // The original trial cohort remains fixed. Other native villages learn on their own existing bodies.
        // A persisted seen tag also respects a later explicit release from managed AI.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 100 != 0 || !Villagers.ready()) return;
            var level = server.overworld(); if (!nativeRegions(level)) return;
            for (var entity : level.getAllEntities()) if (entity instanceof net.minecraft.world.entity.npc.villager.Villager v && v.isAlive()) {
                if (!level.getWorldBorder().isWithinBounds(v.blockPosition())) {
                    // Restore any outside-border resident enrolled by the initial development build.
                    if (v.entityTags().contains("villagers:native_resident_seen")) {
                        if (Villagers.isManaged(v)) VillagerController.release(level, v);
                        v.removeTag("villagers:native_resident_seen");
                    }
                    continue;
                }
                if (!VillageTrial.isReady(server)) continue;
                enrollResident(level,v);
            }
            if (VillageTrial.isReady(server)) AdventurePreparation.nominate(level);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var level = server.overworld();
            if (!nativeRegions(level)) return;
            Path marker = server.getWorldPath(LevelResource.ROOT).resolve("native-four-regions.json");
            if (Files.exists(marker)) return;
            try {
                BlockPos village = null;
                for (var origin : List.of(new BlockPos(-256, 64, -256), new BlockPos(256, 64, 256), BlockPos.ZERO)) {
                    var candidate = level.findNearestMapStructure(StructureTags.VILLAGE, origin, 4, false);
                    if (candidate != null && Math.abs(candidate.getX()) <= WarMap.HALF - 96
                        && Math.abs(candidate.getZ()) <= WarMap.HALF - 96) { village = candidate; break; }
                }
                if (village == null) throw new IllegalStateException("No native village found within the four-region map; no structures or residents were supplied");
                level.getChunk(village.getX() >> 4, village.getZ() >> 4);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, village.getX(), village.getZ());
                var spawn = new BlockPos(village.getX(), y + 2, village.getZ());
                level.getWorldBorder().setCenter(0, 0); level.getWorldBorder().setSize(WarMap.SIZE);
                server.setRespawnData(net.minecraft.world.level.storage.LevelData.RespawnData.of(net.minecraft.world.level.Level.OVERWORLD, spawn, 0, 0));
                Path temporary = marker.resolveSibling(marker.getFileName() + ".tmp");
                Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(Map.of(
                    "schema", 1, "seed", level.getSeed(), "generator", "minecraft:noise", "settings", "minecraft:overworld",
                    "border", WarMap.SIZE, "village", List.of(village.getX(), village.getY(), village.getZ()),
                    "authoredStructures", 0, "suppliedResidents", 0, "suppliedItems", 0)));
                Files.move(temporary, marker, StandardCopyOption.ATOMIC_MOVE);
                Villagers.LOG.info("Native four-region map ready; native village at {}; no authored settlement or supplied resident", village);
            } catch (Exception failure) { throw new IllegalStateException("Native four-region setup failed without authoring a replacement village", failure); }
        });
    }
}
