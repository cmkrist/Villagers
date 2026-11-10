package local.villagers.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.villagers.ConstructionSites;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A native scheduled leaf refresh can write the identical state; player writes still use the ordinary hook. */
@Mixin(LeavesBlock.class)
public abstract class ConstructionLeavesMixin {
    @WrapOperation(method="tick",at=@At(value="INVOKE",
            target="Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean villagers$nativeRefresh(ServerLevel level,BlockPos pos,BlockState state,Operation<Boolean> original) {
        return ConstructionSites.nativeLeafRefresh(level,pos,state,() -> original.call(level,pos,state));
    }
}
