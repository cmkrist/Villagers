package local.villagers;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;

/** A critically starving body seeks ripe edible produce before grain that still needs a recipe. */
final class UrgentCropMeal {
    private static final double URGENT_SPEED=.8;
    private static boolean ripe(ServerLevel level,Session s,BlockPos p){
        if(!level.isLoaded(p)||!level.getWorldBorder().isWithinBounds(p)||ConstructionSites.isProtected(level,p)
                ||ConstructionSites.isProtected(level,p.below())||!level.getBlockState(p.below()).is(Blocks.FARMLAND)
                ||!VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(p)))return false;
        var b=level.getBlockState(p);
        return (b.is(Blocks.CARROTS)||b.is(Blocks.POTATOES)||b.is(Blocks.BEETROOTS))
            &&b.getBlock() instanceof CropBlock c&&c.isMaxAge(b);
    }
    static String work(ServerLevel level,Villager v,Session s,LivingEntity threat,long now){
        if(s.mind.foodLevel()!=0||v.getHealth()>5||threat!=null||v.isOnFire()||v.isTrading()||v.isPassenger()
                ||v.getAirSupply()<200||!NativeRegionsScenario.nativeRegions(level)||!AdventureActions.canHarvest(v)){
            s.urgentCrop=null;return null;
        }
        if(s.urgentCrop!=null&&!ripe(level,s,s.urgentCrop))s.urgentCrop=null;
        if(s.urgentCrop==null){
            if(now<s.nextUrgentCrop)return null;s.nextUrgentCrop=now+40;
            var found=new ArrayList<BlockPos>();var origin=v.blockPosition();
            for(var p:BlockPos.betweenClosed(origin.offset(-48,-3,-48),origin.offset(48,3,48)))if(ripe(level,s,p))found.add(p.immutable());
            found.sort(Comparator.comparingDouble(p->p.distSqr(origin)));
            for(var p:found.stream().limit(3).toList()){
                var path=v.getNavigation().createPath(p,1);
                if(path==null||!path.canReach()||!WorkNavigation.staysHome(level,v,s,path))continue;
                int wet=0;boolean safe=true;
                for(int i=0;i<path.getNodeCount();i++){
                    wet=level.getFluidState(path.getNodePos(i)).isEmpty()?0:wet+1;
                    if(wet>8){safe=false;break;}
                }
                if(safe){if(v.isSleeping())v.stopSleeping();s.urgentCrop=p;v.getNavigation().moveTo(path,URGENT_SPEED);break;}
            }
        }
        if(s.urgentCrop==null)return null;
        if(v.getEyePosition().distanceToSqr(Vec3.atCenterOf(s.urgentCrop))<=9){
            if(AdventureActions.tryHarvest(level,v,s.urgentCrop)){s.urgentCrop=null;return "gathered ripe emergency food and replanted";}
        }
        // Fluid/standing flags change during shore jumps. Re-querying mid-step can
        // reject the route that native navigation is still successfully following.
        if(now>=s.nextUrgentCrop&&v.getNavigation().isDone()){
            s.nextUrgentCrop=now+40;var path=v.getNavigation().createPath(s.urgentCrop,1);
            if(WorkNavigation.staysHome(level,v,s,path))v.getNavigation().moveTo(path,URGENT_SPEED);
            else {s.urgentCrop=null;return null;}
        }
        return "approaching ripe edible crops before processed grain";
    }
    private UrgentCropMeal(){}
}
