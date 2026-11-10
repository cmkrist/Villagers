package local.villagers.mixin;

import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PoiTypes.class)
public interface PoiTypesAccessor {
    @Invoker("registerBlockStates")
    static void villagers$registerBeds(Holder<PoiType> type, Set<BlockState> states) { throw new AssertionError(); }
}
