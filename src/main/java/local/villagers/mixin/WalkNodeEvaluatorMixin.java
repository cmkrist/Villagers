package local.villagers.mixin;

import local.villagers.NativePathLimits;
import local.villagers.NativeBodyRoutes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only a scoped, actor-specific work search can exclude an occupied node. */
@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin extends NodeEvaluator {
    @Inject(method="getNeighbors",at=@At("RETURN"),cancellable=true)
    private void villagers$clearEdges(Node[] neighbors,Node parent,CallbackInfoReturnable<Integer> callback){
        int count=callback.getReturnValue(),kept=0;
        // Native step/fall handling can return a node at a different height
        // from the cell whose path type was queried. Apply the scoped exclusion
        // to that final neighbor as well, including air directly over water.
        for(int i=0;i<count;i++){
            var next=neighbors[i];
            if(!NativePathLimits.blocked(mob,next.x,next.y,next.z)&&!NativeBodyRoutes.blockedEdge(mob,parent,next))
                neighbors[kept++]=next;
        }
        if(kept!=count)callback.setReturnValue(kept);
    }
    @Inject(method="getPathTypeOfMob",at=@At("HEAD"),cancellable=true)
    private void villagers$occupiedNode(PathfindingContext context,int x,int y,int z,Mob mob,CallbackInfoReturnable<PathType> callback){
        if(NativePathLimits.blocked(mob,x,y,z))callback.setReturnValue(PathType.BLOCKED);
    }
}
