package local.villagers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Aim at an actually visible face; the first native collision still decides contact and reach. */
final class BlockContact {
    static Vec3 point(ServerLevel level,Entity actor,Vec3 eye,BlockPos target,double reach){
        return point(level,actor,eye,target,reach,ClipContext.Block.COLLIDER);
    }
    /** Native interactions select outlines, including non-colliding controls such as levers. */
    static Vec3 interactionPoint(ServerLevel level,Entity actor,Vec3 eye,BlockPos target,double reach){
        return point(level,actor,eye,target,reach,ClipContext.Block.OUTLINE);
    }
    private static Vec3 point(ServerLevel level,Entity actor,Vec3 eye,BlockPos target,double reach,ClipContext.Block mode){
        var origin=BlockPos.containing(eye);
        for(int x=Math.min(origin.getX()>>4,target.getX()>>4);x<=Math.max(origin.getX()>>4,target.getX()>>4);x++)
            for(int z=Math.min(origin.getZ()>>4,target.getZ()>>4);z<=Math.max(origin.getZ()>>4,target.getZ()>>4);z++)
                if(!level.isLoaded(new BlockPos(x<<4,target.getY(),z<<4)))return null;
        var center=Vec3.atCenterOf(target);
        for(int face=-1;face<Direction.values().length;face++){
            var end=center;
            if(face>=0){var side=Direction.values()[face];end=end.add(.499*side.getStepX(),.499*side.getStepY(),.499*side.getStepZ());}
            var hit=level.clip(new ClipContext(eye,end,mode,ClipContext.Fluid.NONE,actor));
            if(hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(target)&&eye.distanceToSqr(hit.getLocation())<=reach*reach)
                return hit.getLocation();
        }
        return null;
    }
    private BlockContact(){}
}
