package local.villagers;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

/** Short native shore routes take precedence over distant home trips while swimming. */
final class WaterEscape {
    static String work(ServerLevel level,Villager v,Session s,LivingEntity threat,long now){
        if(v.isPassenger()){s.waterExit=null;return null;}
        if(!v.isInWater()&&v.onGround()){s.waterExit=null;return null;}
        if(!v.isInWater()&&s.waterExit==null)return null;
        if(s.waterExit!=null&&now<s.nextWaterExit&&!v.getNavigation().isDone())return "swimming toward a dry shore";
        if(now<s.nextWaterExit)return s.waterExit==null?null:"swimming toward a dry shore";
        s.nextWaterExit=now+40;
        var stands=new ArrayList<BlockPos>();var origin=v.blockPosition();
        for(var p:BlockPos.betweenClosed(origin.offset(-8,-2,-8),origin.offset(8,3,8))){
            if(!BoatRoutes.dry(level,p))continue;
            if(threat!=null&&Vec3.atBottomCenterOf(p).distanceToSqr(threat.position())<Math.max(16,v.distanceToSqr(threat)))continue;
            stands.add(p.immutable());
        }
        stands.sort(Comparator.comparingDouble(p->v.position().distanceToSqr(Vec3.atBottomCenterOf(p))));
        for(var p:stands.stream().limit(3).toList()){
            var path=v.getNavigation().createPath(p,0);
            if(path==null||!path.canReach()||path.getNodeCount()>32)continue;
            boolean safe=true;
            for(int i=0;i<path.getNodeCount();i++){
                var n=path.getNodePos(i);
                if(!level.isLoaded(n)||!level.getWorldBorder().isWithinBounds(n)
                        ||threat!=null&&Vec3.atBottomCenterOf(n).distanceToSqr(threat.position())<16){safe=false;break;}
            }
            if(safe&&v.getNavigation().moveTo(path,.8)){s.waterExit=p;s.waterExitsPlanned++;return "swimming toward a dry shore";}
        }
        s.waterExit=null;return null;
    }
    private WaterEscape(){}
}
