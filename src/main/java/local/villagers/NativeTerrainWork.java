package local.villagers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Native structure bounds include untouched ground. Preserve actual construction and its footing. */
final class NativeTerrainWork {
    static BlockPos structuralCover(ServerLevel level,BlockPos excavation){
        if(!level.structureManager().getStructureWithPieceAt(excavation,h->true).isValid())return null;
        for(int dy=1;dy<=8;dy++){
            var p=excavation.above(dy);if(!level.isLoaded(p))return p;
            var block=level.getBlockState(p);
            if(block.is(Blocks.DIRT)||block.is(Blocks.GRASS_BLOCK)||block.is(Blocks.COARSE_DIRT))continue;
            if(!block.getCollisionShape(level,p).isEmpty()||level.getBlockEntity(p)!=null)return p;
        }
        return null;
    }
    private NativeTerrainWork(){}
}
