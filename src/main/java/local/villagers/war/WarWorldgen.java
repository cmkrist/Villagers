package local.villagers.war;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class WarWorldgen {
    private WarWorldgen() {}
    public static void initialize() {
        Registry.register(BuiltInRegistries.BIOME_SOURCE, Identifier.fromNamespaceAndPath("villagers","quadrants"), QuadrantBiomes.CODEC);
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, Identifier.fromNamespaceAndPath("villagers","war"), WarChunkGenerator.CODEC);
    }
}
