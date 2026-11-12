package local.villagers;

import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;

/** Prepare a real hoe for a physically observed field, or a table for owned or locally observed grain. */
final class SelfProvisioning {
    static String work(ServerLevel level, Villager v, Session s, long now) {
        if (!WoodBootstrap.safe(level,v,s) || s.boatTrip != null || s.courtship != null
                || FoodSupply.deficit(v.getInventory(),AdventureActions.canHarvest(v)) == 0) return null;
        // Hay is only treated as a meal when a table exists. Requiring owned
        // wheat here left an empty-handed resident beside visible hay unable
        // to begin either job. This is the ordinary local visible hay scan;
        // wood gathering, recipes and placement still pay their real costs.
        boolean grain=BreadCrafting.wheatCount(v.getInventory())>=3||HayForaging.hasHay(v.getInventory())
            ||Villagers.config().hayForaging()&&HayForaging.target(level,v,s)!=null;
        if (grain && BreadCrafting.table(level,v,s) == null)
            return WoodBootstrap.work(level,v,s,WoodBootstrap.Goal.TABLE,now);
        // Prepared farmers use the ordinary food work below in the controller.
        // Repeatedly surveying their field here consumed every navigation grant
        // before wood gathering or quarrying could use the actor's turn.
        if(AdventureActions.hasHoe(v))return null;
        if (s.preparationCrop != null && (!level.isLoaded(s.preparationCrop)
                || !(level.getBlockState(s.preparationCrop).getBlock() instanceof CropBlock)
                || !level.getBlockState(s.preparationCrop.below()).is(Blocks.FARMLAND))) s.preparationCrop = null;
        if (s.preparationCrop == null) {
            if (now < s.nextPreparationScan || !WorkNavigation.admit(level,now,v)) return null;
            s.nextPreparationScan = now + 100;
            var origin = v.blockPosition(); BlockPos best = null; double score = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(origin.offset(-48,-3,-48),origin.offset(48,3,48))) {
                if (!level.isLoaded(p) || ConstructionSites.isProtected(level,p) || ConstructionSites.isProtected(level,p.below())) continue;
                var state = level.getBlockState(p);
                if (!(state.getBlock() instanceof CropBlock)
                        || !level.getBlockState(p.below()).is(Blocks.FARMLAND) || !VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(p))) continue;
                double distance = p.distSqr(origin)-(((CropBlock)state.getBlock()).isMaxAge(state)?10000:0);
                if (distance < score && BoatRoutes.visible(level,v,Vec3.atCenterOf(p))) { best = p.immutable(); score = distance; }
            }
            if (best == null) return null;
            var path = WorkNavigation.pathTo(level,v,s,best,1);
            if (!WorkNavigation.staysHome(level,v,s,path)) return null;
            for (int i=0;i<path.getNodeCount();i++) if (!level.getFluidState(path.getNodePos(i)).isEmpty()) return null;
            s.preparationCrop = best;
        }
        var crop=level.getBlockState(s.preparationCrop);
        boolean ripe=crop.getBlock() instanceof CropBlock c&&c.isMaxAge(crop);
        // Existing ripe crops can be harvested by hand. Prepare a hoe while
        // waiting for growth, rather than delaying a real meal for scarce wood.
        if(!ripe&&!AdventureActions.hasHoe(v)&&FoodSupply.mayDoOptionalWork(s.mind.foodLevel())
                &&AdventureActions.PERMISSIONS.allows(Capability.CRAFT_WOODEN_HOE)){
            var preparing=WoodBootstrap.work(level,v,s,WoodBootstrap.Goal.HOE,now);
            if(preparing!=null)return preparing;
        }
        if(!ripe||v.getEyePosition().distanceToSqr(Vec3.atCenterOf(s.preparationCrop))<=9)return null;
        if(now>=s.woodBootstrap.nextPath&&WorkNavigation.admit(level,now,v)){
            s.woodBootstrap.nextPath=now+40;
            // Crops are passable native destinations, unlike solid logs. Use the same dry
            // native crop route validated during observation, rather than a different log stand.
            var path=WorkNavigation.pathTo(level,v,s,s.preparationCrop,1);
            boolean dry=path!=null&&path.canReach();
            if(dry)for(int i=0;i<path.getNodeCount();i++)if(!level.getFluidState(path.getNodePos(i)).isEmpty()){dry=false;break;}
            if(dry&&WorkNavigation.staysHome(level,v,s,path))v.getNavigation().moveTo(path,.6);
            else {s.preparationCrop=null;s.nextPreparationScan=now+100;return null;}
        }
        return "walking to observed ripe crops";
    }
    private SelfProvisioning() {}
}
