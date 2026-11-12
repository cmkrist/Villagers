package local.villagers;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.*;

/** Floating remains Minecraft's existing FloatGoal; this only chooses a visible dry shore to walk toward. */
final class NativeSwimming {
    static final class State {
        long pathAt;
        Map<String,Object> receipt=Map.of();
    }
    private static boolean local(ServerLevel level,Mob mob,BlockPos p){
        return level.isLoaded(p)&&level.isLoaded(p.above(2))&&level.isLoaded(p.below())
            &&level.getWorldBorder().isWithinBounds(p)
            &&(!NativeIllagers.member(mob)||NativeCampWork.local(level,mob,p));
    }
    static Vec3 dryFeet(ServerLevel level,Mob mob,BlockPos p){
        if(!local(level,mob,p)||!level.getFluidState(p).isEmpty()||!level.getFluidState(p.below()).isEmpty())return null;
        double floor=WalkNodeEvaluator.getFloorLevel(level,p);
        var feet=new Vec3(p.getX()+.5,floor,p.getZ()+.5);
        var body=mob.getBoundingBox().move(feet.subtract(mob.position()));
        if(!level.noCollision(mob,body)||level.noBlockCollision(mob,body.move(0,-.03,0)))return null;
        var hit=level.clip(new ClipContext(mob.getEyePosition(),feet.add(0,.1,0),
            ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mob));
        return hit.getType()==HitResult.Type.MISS?feet:null;
    }
    private static boolean safe(ServerLevel level,Mob mob,Path path){
        if(path==null||!path.canReach()||path.getNodeCount()==0)return false;
        int y=mob.getBlockY();
        for(int i=0;i<path.getNodeCount();i++){
            var p=path.getNodePos(i);
            if(!local(level,mob,p)||level.getFluidState(p).is(FluidTags.LAVA)||y-p.getY()>2)return false;
            y=p.getY();
        }
        return dryFeet(level,mob,path.getEndNode().asBlockPos())!=null;
    }
    static void escape(ServerLevel level,Mob mob,State w,long now){
        if(now<w.pathAt)return;
        w.pathAt=now+40;
        var shores=new ArrayList<BlockPos>();var origin=mob.blockPosition();
        for(var p:BlockPos.betweenClosed(origin.offset(-12,-1,-12),origin.offset(12,8,12))){
            if(mob.getEyePosition().distanceToSqr(Vec3.atCenterOf(p))>16*16)continue;
            if(dryFeet(level,mob,p)!=null)shores.add(p.immutable());
        }
        shores.sort(Comparator.comparingDouble(p->mob.position().distanceToSqr(Vec3.atBottomCenterOf(p))));
        Path chosen=NativeBodyRoutes.search(level,mob,()->NativePathLimits.search(mob,2,()->{
            for(var shore:shores.stream().limit(6).toList()){
                var path=mob.getNavigation().createPath(shore,0,32);
                if(safe(level,mob,path))return path;
            }
            return null;
        }));
        boolean moved=chosen!=null&&mob.getNavigation().moveTo(chosen,1);
        if(!moved)mob.getNavigation().stop();
        w.receipt=Map.of("gameTime",now,"from",mob.position().toString(),"air",mob.getAirSupply(),
            "visibleDryStands",shores.size(),"moved",moved,"nodes",chosen==null?0:chosen.getNodeCount(),
            "end",chosen==null?"":chosen.getEndNode().asBlockPos().toShortString());
    }
    private NativeSwimming(){}
}
