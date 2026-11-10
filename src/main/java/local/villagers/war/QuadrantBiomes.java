package local.villagers.war;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;

/** A persistent biome source: all vertical samples in a quarter have its declared biome. */
public final class QuadrantBiomes extends BiomeSource {
    public static final MapCodec<QuadrantBiomes> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Biome.CODEC.fieldOf("grassland").forGetter(b -> b.grassland),
        Biome.CODEC.fieldOf("forest").forGetter(b -> b.forest),
        Biome.CODEC.fieldOf("mountains").forGetter(b -> b.mountains),
        Biome.CODEC.fieldOf("desert").forGetter(b -> b.desert)
    ).apply(instance, QuadrantBiomes::new));
    private final Holder<Biome> grassland, forest, mountains, desert;
    public QuadrantBiomes(Holder<Biome> grassland, Holder<Biome> forest, Holder<Biome> mountains, Holder<Biome> desert) {
        this.grassland = grassland; this.forest = forest; this.mountains = mountains; this.desert = desert;
    }
    @Override protected MapCodec<? extends BiomeSource> codec() { return CODEC; }
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes() { return Stream.of(grassland,forest,mountains,desert); }
    @Override public BiomeResolver createResolver(Climate.Sampler sampler) {
        return (x,y,z) -> switch (WarMap.region(x * 4, z * 4)) {
            case GRASSLAND -> grassland; case FOREST -> forest;
            case SNOWY_MOUNTAINS -> mountains; case DESERT -> desert;
        };
    }
}
