package local.villagers;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

/** Rest on actual supported ground with a buffer against ordinary crowd pushing. */
final class RecoveryRest {
    static boolean stable(ServerLevel level,Villager v,Session s,BlockPos p){
        if(!level.isLoaded(p.above())||!level.isLoaded(p.below())||!level.getWorldBorder().isWithinBounds(p)
                ||!VillageSurvival.withinHomeRange(level,s.mind,Vec3.atBottomCenterOf(p))
                ||!level.getBlockState(p).getCollisionShape(level,p).isEmpty()
                ||!level.getBlockState(p.above()).getCollisionShape(level,p.above()).isEmpty()
                ||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))return false;
        for(var q:BlockPos.betweenClosed(p.offset(-1,0,-1),p.offset(1,0,1))){
            if(!level.isLoaded(q.below())||!level.getFluidState(q).isEmpty())return false;
            var shape=level.getBlockState(q).getCollisionShape(level,q);
            if((shape.isEmpty()||shape.max(Direction.Axis.Y)<1)&&!level.getBlockState(q.below()).isFaceSturdy(level,q.below(),Direction.UP))return false;
        }
        return true;
    }
    static String work(ServerLevel level,Villager v,Session s,long now){
        if(s.recoveryStand!=null&&(!stable(level,v,s,s.recoveryStand)||s.recoveryStand.getY()!=v.getBlockY()))s.recoveryStand=null;
        if(s.recoveryStand!=null&&v.position().distanceToSqr(Vec3.atBottomCenterOf(s.recoveryStand))<.65*.65){
            v.getNavigation().stop();if(now%80<10)s.mind.rest();return "resting between recovery meals";
        }
        if(now>=s.nextRecoveryStand){
            s.nextRecoveryStand=now+40;var stands=new ArrayList<BlockPos>();var origin=v.blockPosition();
            for(var p:BlockPos.betweenClosed(origin.offset(-5,0,-5),origin.offset(5,0,5)))if(stable(level,v,s,p))stands.add(p.immutable());
            stands.sort(Comparator.comparingDouble(p->v.position().distanceToSqr(Vec3.atBottomCenterOf(p))));
            for(var p:stands.stream().limit(3).toList()){
                var path=WorkNavigation.pathTo(level,v,s,p,0);if(path==null)continue;boolean safe=true;
                for(int i=1;i<path.getNodeCount();i++)if(path.getNodePos(i).getY()!=origin.getY()||!stable(level,v,s,path.getNodePos(i))){safe=false;break;}
                if(safe&&v.getNavigation().moveTo(path,.4)){s.recoveryStand=p;return "seeking stable recovery ground";}
            }
            v.getNavigation().stop();
        }
        return s.recoveryStand==null?"resting carefully while awaiting recovery food":"seeking stable recovery ground";
    }
    private RecoveryRest(){}
}
