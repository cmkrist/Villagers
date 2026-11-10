package local.villagers.war;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

/** Deterministic terrain for the finite war map; native biome decoration supplies vegetation. */
public final class WarChunkGenerator extends ChunkGenerator {
    public static final MapCodec<WarChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BiomeSource.CODEC.fieldOf("biome_source").forGetter(WarChunkGenerator::getBiomeSource)
    ).apply(instance, WarChunkGenerator::new));
    public WarChunkGenerator(BiomeSource biomes) { super(biomes); }
    @Override protected MapCodec<? extends ChunkGenerator> codec() { return CODEC; }
    @Override public int getGenDepth() { return 384; }
    @Override public int getMinY() { return -64; }
    @Override public int getSeaLevel() { return 63; }
    @Override public void spawnOriginalMobs(WorldGenRegion region) {}
    /** The four authored settlements are the only structures in this scenario. */
    @Override public void createStructures(net.minecraft.core.RegistryAccess registries,
            ChunkGeneratorStructureState state, StructureManager structures, ChunkAccess chunk,
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager templates,
            net.minecraft.resources.ResourceKey<Level> dimension) {}
    @Override public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return WarMap.height(x,z) + 1;
    }
    private BlockState block(int x, int y, int z, int surface) {
        if (y > surface) return Blocks.AIR.defaultBlockState();
        if (y == -64) return Blocks.BEDROCK.defaultBlockState();
        if (y < surface - 3) return (y < 0 ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState();
        return switch (WarMap.region(x,z)) {
            case DESERT -> (y == surface ? Blocks.SAND : Blocks.SANDSTONE).defaultBlockState();
            case SNOWY_MOUNTAINS -> (y == surface ? Blocks.SNOW_BLOCK : Blocks.STONE).defaultBlockState();
            default -> (y == surface ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState();
        };
    }
    @Override public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        BlockState[] states = new BlockState[getGenDepth()];
        int surface = WarMap.height(x,z);
        for (int i=0; i<states.length; i++) states[i] = block(x, i + getMinY(), z, surface);
        return new NoiseColumn(getMinY(), states);
    }
    @Override public CompletableFuture<ChunkAccess> buildTerrain(ChunkAccess chunk, Blender blender, RandomState random,
            StructureManager structures, BiomeManager biomes, WorldGenRegion region, Set<Holder<Biome>> skipped) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int startX=chunk.getPos().getMinBlockX(), startZ=chunk.getPos().getMinBlockZ();
        for (int x=startX; x<startX+16; x++) for (int z=startZ; z<startZ+16; z++) {
            int surface=WarMap.height(x,z);
            for (int y=getMinY(); y<=surface; y++) chunk.setBlockState(pos.set(x,y,z),block(x,y,z,surface));
        }
        Heightmap.primeHeightmaps(chunk, Set.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
        return CompletableFuture.completedFuture(chunk);
    }
    @Override public void addDebugScreenInfo(List<String> lines, RandomState random, BlockPos pos, SamplerContext context) {
        lines.add("Four Biomes War: " + WarMap.region(pos.getX(),pos.getZ()));
    }
}
