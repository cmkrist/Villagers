package local.villagers;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Container access only; callers supply their actual native territory and physical reach checks. */
final class NativeContainers {
    static Container accessible(ServerLevel level,BlockPos p){
        if(!level.isLoaded(p)||!level.getWorldBorder().isWithinBounds(p)||ConstructionSites.isProtected(level,p)
            ||!(level.getBlockEntity(p) instanceof BaseContainerBlockEntity be)||be.isLocked()||be.hasCustomName())return null;
        var b=level.getBlockState(p);
        if(b.getBlock() instanceof ChestBlock chest){
            if(b.getValue(ChestBlock.TYPE)!=ChestType.SINGLE)for(var d:Direction.Plane.HORIZONTAL)
                if(level.getBlockEntity(p.relative(d)) instanceof BaseContainerBlockEntity other&&(other.isLocked()||other.hasCustomName()))return null;
            return ChestBlock.getContainer(chest,b,level,p,false);
        }
        return b.is(Blocks.BARREL)?be:null;
    }
    private NativeContainers(){}
}
