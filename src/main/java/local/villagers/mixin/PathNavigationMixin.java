package local.villagers.mixin;

import java.util.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.villagers.NativePathLimits;
import net.minecraft.core.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Scoped planning is fresh and observational until moveTo commits the chosen route. */
@Mixin(PathNavigation.class)
public abstract class PathNavigationMixin {
    @Shadow @Final protected Mob mob;

    @WrapOperation(method={"followThePath","tick"},at=@At(value="INVOKE",target="Lnet/minecraft/world/level/pathfinder/Path;advance()V"))
    private void villagers$observeNativeWaypoint(Path route,Operation<Void> original){
        local.villagers.NativeBodyRoutes.observeWaypoint(mob,route);
        original.call(route);
    }
    @Shadow private BlockPos targetPos;
    @Shadow private int reachRange;
    @Shadow protected Vec3i timeoutCachedNode;
    @Shadow protected long timeoutTimer;
    @Shadow protected double timeoutLimit;
    @Shadow private boolean isStuck;
    @Unique private final Map<Path,Integer> villagers$plannedRanges=new IdentityHashMap<>();

    @WrapOperation(method="createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;",
        at=@At(value="INVOKE",target="Lnet/minecraft/world/level/pathfinder/Path;isDone()Z"))
    private boolean villagers$scopedFreshSearch(Path path,Operation<Boolean> original){
        return original.call(path)||NativePathLimits.mustReplan(mob);
    }
    @WrapMethod(method="createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;")
    private Path villagers$inspectWithoutRedirecting(Set<BlockPos> targets,int padding,boolean above,int reach,float distance,Operation<Path> original){
        if(!NativePathLimits.mustReplan(mob))return original.call(targets,padding,above,reach,distance);
        var oldTarget=targetPos;int oldReach=reachRange;var oldNode=timeoutCachedNode;
        long oldTimer=timeoutTimer;double oldLimit=timeoutLimit;boolean oldStuck=isStuck;
        try{
            var result=original.call(targets,padding,above,reach,distance);
            if(result!=null){
                if(villagers$plannedRanges.size()>=64)villagers$plannedRanges.clear();
                villagers$plannedRanges.put(result,reach);
            }
            return result;
        }finally{
            targetPos=oldTarget;reachRange=oldReach;timeoutCachedNode=oldNode;
            timeoutTimer=oldTimer;timeoutLimit=oldLimit;isStuck=oldStuck;
        }
    }
    @Inject(method="moveTo(Lnet/minecraft/world/level/pathfinder/Path;D)Z",at=@At("RETURN"))
    private void villagers$commitPlannedDestination(Path route,double speed,CallbackInfoReturnable<Boolean> callback){
        if(!callback.getReturnValue()||route==null)return;
        var reach=villagers$plannedRanges.remove(route);if(reach==null)return;
        targetPos=route.getTarget();reachRange=reach;
        timeoutCachedNode=Vec3i.ZERO;timeoutTimer=0;timeoutLimit=0;isStuck=false;
    }
}
