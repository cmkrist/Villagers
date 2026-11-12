package local.villagers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** One carried dirt block can provide a native jump-up stand under a remaining natural trunk. */
final class ForestryStep {
    static final class State {
        BlockPos tree, block, approach;
        long until, pathAt, placed;
        Map<String,Object> receipt = Map.of();
        void clear() { tree = block = approach = null; }
    }
    private static boolean local(ServerLevel level, Session s, BlockPos p) {
        return level.isLoaded(p) && level.getWorldBorder().isWithinBounds(p)
            && !ConstructionSites.isProtected(level,p)
            && VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(p));
    }
    private static boolean groundStand(ServerLevel level,Villager v,Session s,BlockPos p) {
        return local(level,s,p) && level.isLoaded(p.above()) && level.isLoaded(p.below())
            && FloorCentering.safeSupport(level.getBlockState(p.below()))
            && level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP)
            && level.getFluidState(p).isEmpty() && level.getFluidState(p.above()).isEmpty()
            && level.noCollision(v,v.getBoundingBox().move(Vec3.atBottomCenterOf(p).subtract(v.position())));
    }
    private static net.minecraft.world.phys.BlockHitResult contact(ServerLevel level,Villager v,Vec3 eye,BlockPos p) {
        var hit=level.clip(new ClipContext(eye,Vec3.atBottomCenterOf(p).add(0,-.001,0),
            ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,v));
        return hit.getType()==HitResult.Type.BLOCK && hit.getBlockPos().equals(p.below())
            && hit.getDirection()==Direction.UP && eye.distanceToSqr(hit.getLocation())<=9 ? hit:null;
    }
    private static boolean site(ServerLevel level,Villager v,Session s,BlockPos tree,BlockPos p) {
        if (!BoatPreparation.naturalTrunk(level,tree) || !local(level,s,p) || !local(level,s,p.below())
            || !level.getBlockState(p).isAir() || !level.getFluidState(p).isEmpty()
            || !(level.getBlockState(p.below()).is(BlockTags.DIRT) || level.getBlockState(p.below()).is(Blocks.GRASS_BLOCK))
            || !level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP)
            || !groundStand(level,v,s,p)) return false;
        var top=p.above();
        if (!local(level,s,top) || !level.isLoaded(top.above())
            || !level.getFluidState(top).isEmpty() || !level.getFluidState(top.above()).isEmpty()
            || !level.noCollision(v,v.getBoundingBox().move(Vec3.atBottomCenterOf(top).subtract(v.position())))
            || BlockContact.point(level,v,ForestryWork.standEye(v,top),tree,2.5)==null) return false;
        // Never add a step to cultivated ground or obstruct a nearby crop.
        for(var at:BlockPos.betweenClosed(p.offset(-1,-1,-1),p.offset(1,1,1)))
            if(!level.isLoaded(at) || level.getBlockState(at).is(Blocks.FARMLAND)
                || level.getBlockState(at).is(BlockTags.CROPS)) return false;
        return true;
    }
    /** Called only within an admitted, bounded tree survey. No placement during planning. */
    static boolean plan(ServerLevel level,Villager v,Session s,BlockPos tree,long now) {
        if(!NativeRegionsScenario.nativeRegions(level)
            || !(AdventurePreparation.nominated(v)||s.woodBootstrap.goal==WoodBootstrap.Goal.BOAT)
            || !AdventureActions.PERMISSIONS.allows(Capability.BUILD_FORESTRY_STEP)
            || v.getInventory().countItem(Items.DIRT)==0) return false;
        var p=tree.below();
        for(int gap=0;gap<5 && level.isLoaded(p.below()) && level.getBlockState(p.below()).isAir();gap++) p=p.below();
        if(!site(level,v,s,tree,p)) return false;
        var stands=new ArrayList<BlockPos>();
        for(var side:Direction.Plane.HORIZONTAL) {
            var stand=p.relative(side);
            if(groundStand(level,v,s,stand) && contact(level,v,ForestryWork.standEye(v,stand),p)!=null) stands.add(stand);
        }
        stands.sort(Comparator.comparingDouble(at->at.distSqr(v.blockPosition())));
        for(var stand:stands.stream().limit(3).toList()) {
            var path=WorkNavigation.pathTo(level,v,s,stand,0);
            if(!WorkNavigation.staysHome(level,v,s,path) || !BoatTravel.dryPath(level,path)) continue;
            if(!TreeClaims.claim(level,tree,v.getUUID(),now))return false;
            var step=s.woodBootstrap.step;
            step.tree=tree;step.block=p;step.approach=stand;step.until=now+1200;step.pathAt=now+40;
            v.getNavigation().moveTo(path,.6);return true;
        }
        return false;
    }
    static String work(ServerLevel level,Villager v,Session s,long now) {
        var step=s.woodBootstrap.step;
        if(step.tree==null) return null;
        if(now>=step.until || !WoodBootstrap.safe(level,v,s)
            || !TreeClaims.claim(level,step.tree,v.getUUID(),now)
            || !AdventureActions.PERMISSIONS.allows(Capability.BUILD_FORESTRY_STEP)
            || !site(level,v,s,step.tree,step.block)) { step.clear();return null; }
        var bag=v.getInventory();int slot=-1;
        for(int i=0;i<bag.getContainerSize();i++) if(bag.getItem(i).is(Items.DIRT)) {slot=i;break;}
        if(slot<0) {step.clear();return null;}
        var hit=contact(level,v,v.getEyePosition(),step.block);
        if(hit==null || !v.onGround() || !level.getEntities(null,new AABB(step.block)).isEmpty()) {
            if(now>=step.pathAt && WorkNavigation.admit(level,now,v)) {
                step.pathAt=now+40;
                var path=WorkNavigation.pathTo(level,v,s,step.approach,0);
                if(WorkNavigation.staysHome(level,v,s,path) && BoatTravel.dryPath(level,path)) v.getNavigation().moveTo(path,.6);
                else {step.clear();return null;}
            }
            return "carrying dirt to a safe forestry step";
        }
        int before=bag.getItem(slot).getCount();
        if(!level.setBlock(step.block,Blocks.DIRT.defaultBlockState(),Block.UPDATE_ALL)) return null;
        bag.removeItem(slot,1);step.placed++;
        step.receipt=Map.of("gameTime",now,"position",step.block.toShortString(),"tree",step.tree.toShortString(),
            "dirtBefore",before,"dirtAfter",before-1,"contact",hit.getLocation().toString(),
            "reach",v.getEyePosition().distanceTo(hit.getLocation()));
        var w=s.woodBootstrap;w.tree=step.tree;w.treeBlock=level.getBlockState(step.tree).getBlock();w.taken=0;w.nextPath=0;
        v.getNavigation().stop();v.swingForAttack(InteractionHand.MAIN_HAND);level.gameEvent(v,GameEvent.BLOCK_PLACE,step.block);
        s.mind.remember("Placed one owned dirt block as a forestry step at "+step.block.toShortString());
        step.clear();return "placed an owned dirt step to reach natural wood";
    }
    private ForestryStep() {}
}
