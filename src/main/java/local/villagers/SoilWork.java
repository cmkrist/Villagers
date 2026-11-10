package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import local.villagers.construction.SoilTripPolicy;
import local.villagers.construction.WorkSites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** One finite native dirt source -> actual held dirt -> paid sand replacement -> existing till/plant job. */
final class SoilWork {
    private record Claim(UUID actor,long until) {}
    private static final Map<UUID,Claim> CLAIMS=new HashMap<>();
    private SoilWork() {}
    static void clear() { CLAIMS.clear(); }
    static void retainClaims(Set<UUID> jobs) { CLAIMS.keySet().retainAll(jobs); }
    static boolean sourceSite(ServerLevel level,BlockPos p) {
        if(!level.isLoaded(p.below()) || !level.isLoaded(p.above(2)))return false;
        var block=level.getBlockState(p);
        return (block.is(Blocks.DIRT) || block.is(Blocks.GRASS_BLOCK)) && bare(level,p)
                && level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP)
                && FloorCentering.safeSupport(level.getBlockState(p.below()));
    }
    static boolean targetSite(ServerLevel level,BlockPos p) {
        if(!level.isLoaded(p.below()) || !level.isLoaded(p.above(2)) || !level.getBlockState(p).is(Blocks.SAND)
                || !bare(level,p) || !level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))return false;
        for(BlockPos q:BlockPos.betweenClosed(p.offset(-4,0,-4),p.offset(4,1,4)))
            if(level.isLoaded(q) && level.getFluidState(q).is(FluidTags.WATER))return true;
        return false;
    }
    private static boolean bare(ServerLevel level,BlockPos p) {
        return level.getBlockEntity(p)==null && level.getBlockState(p.above()).isAir() && level.getBlockState(p.above(2)).isAir()
                && level.canSeeSky(p.above()) && level.getEntities(null,new AABB(p.above()).expandTowards(0,1,0)).isEmpty();
    }
    private static WorkSites.Point home(Session s) {
        var h=s.mind.home();return h==null?null:new WorkSites.Point(h.dimension(),h.x(),h.y(),h.z());
    }
    private static boolean eligible(ServerLevel level,Villager v,Session s) {
        return AdventureActions.PERMISSIONS.allows(Capability.SOIL_TRANSFER) && Villagers.isManaged(v) && v.level()==level
                && !v.isPassenger() && v.getPose()==net.minecraft.world.entity.Pose.STANDING
                && !v.getMainHandItem().is(ItemTags.SWORDS) && FoodSupply.mayDoOptionalWork(s.mind.foodLevel())
                && FoodSupply.mayWork(v.getAge(),v.getHealth(),s.mind.fatigue(),!level.isDarkOutside() && !VillageSurvival.scheduledRest(level,v)
                && !v.isSleeping() && !v.isTrading() && !v.isOnFire() && !v.isInWater() && !v.isInLava() && v.getAirSupply()>=200);
    }
    private static boolean claimed(WorkSites.Soil job,Villager v,long now) {
        var c=CLAIMS.get(job.id());return c!=null && c.actor().equals(v.getUUID()) && now<c.until();
    }
    private static boolean claim(WorkSites.Soil job,Villager v,Session s,long now) {
        var c=CLAIMS.get(job.id());if(c!=null && !c.actor().equals(v.getUUID()) && now<c.until())return false;
        CLAIMS.put(job.id(),new Claim(v.getUUID(),s.soilStarted+1200));return true;
    }
    static boolean wantsBareHand(ServerLevel level,Villager v) {
        var s=Villagers.existingSession(v.getUUID());if(s==null || s.soilJob==null || !eligible(level,v,s))return false;
        var j=ConstructionSites.soilJob(level,s.soilJob);return j!=null && claimed(j,v,level.getGameTime());
    }
    static boolean authorizedTrip(ServerLevel level,Villager v,long now) {
        var s=Villagers.existingSession(v.getUUID());if(s==null || !s.soilTrip || s.soilJob==null || now-s.soilStarted>=1200 || !eligible(level,v,s))return false;
        var job=ConstructionSites.soilJob(level,s.soilJob);
        return job!=null && claimed(job,v,now) && SoilTripPolicy.contains(home(s),job,v.getX(),v.getY(),v.getZ());
    }
    private static void stop(Session s,Villager v,long now,long delay) {
        if(s.soilJob!=null) { var c=CLAIMS.get(s.soilJob);if(c!=null && c.actor().equals(v.getUUID()))CLAIMS.remove(s.soilJob); }
        s.soilJob=null;s.soilTrip=false;s.soilWorking=null;s.soilWorkStarted=0;s.nextSoilScan=now+delay;s.soilProgress.reset();
    }
    private static boolean valid(ServerLevel level,Villager v,Session s,WorkSites.Soil job) {
        if(!job.equals(ConstructionSites.soilJob(level,job.id())) || !SoilTripPolicy.joins(home(s),job) || !targetSite(level,ConstructionSites.position(job.target())))return false;
        if(job.gathered())return v.getInventory().hasAnyMatching(i -> i.is(Items.DIRT));
        var p=ConstructionSites.position(job.source());
        return sourceSite(level,p) && BuiltInRegistries.BLOCK.getKey(level.getBlockState(p).getBlock()).toString().equals(job.sourceKind());
    }
    private static boolean sight(ServerLevel level,Villager v,BlockPos p) {
        var hit=ForestryWork.clip(level,v,v.getEyePosition(),p);
        return v.getEyePosition().distanceToSqr(Vec3.atCenterOf(p))<=9 && hit!=null && (hit.getType()==HitResult.Type.MISS || hit.getBlockPos().equals(p));
    }
    private static boolean route(ServerLevel level,Villager v,Session s,WorkSites.Soil job,BlockPos target,long now) {
        if(now<s.nextSoilPath || !WorkNavigation.admit(level,now,v))return true;
        s.nextSoilPath=now+40;var stands=new ArrayList<BlockPos>();
        for(BlockPos p:BlockPos.betweenClosed(target.offset(-2,0,-2),target.offset(2,2,2))) {
            if(p.getX()==target.getX() && p.getZ()==target.getZ() || !level.isLoaded(p.below()) || !level.isLoaded(p.above()))continue;
            if(!level.getBlockState(p).isAir() || !level.getBlockState(p.above()).isAir()
                    || !level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP) || !FloorCentering.safeSupport(level.getBlockState(p.below()))
                    || !SoilTripPolicy.contains(home(s),job,p.getX()+.5,p.getY(),p.getZ()+.5))continue;
            var eye=ForestryWork.standEye(v,p);var hit=ForestryWork.clip(level,v,eye,target);
            if(eye.distanceToSqr(Vec3.atCenterOf(target))<=9 && hit!=null && (hit.getType()==HitResult.Type.MISS || hit.getBlockPos().equals(target)))stands.add(p.immutable());
        }
        stands.sort(Comparator.comparingDouble(p -> v.position().distanceToSqr(Vec3.atBottomCenterOf(p))));
        if(!target.equals(s.soilPathObjective)) { s.soilPathObjective=target;s.soilPathCursor=0; }
        int attempts=0;
        while(s.soilPathCursor<stands.size() && attempts++<3) {
            var stand=stands.get(s.soilPathCursor++);s.soilPathQueries++;
            var path=v.getNavigation().createPath(stand,0);
            s.lastSoilPath=Map.of("target",target.toShortString(),"stand",stand.toShortString(),"canReach",path!=null && path.canReach(),
                    "nodes",path==null?0:path.getNodeCount(),"cursor",s.soilPathCursor,"candidates",stands.size());
            if(path==null || !path.canReach() || path.getNodeCount()<1 || path.getNodeCount()>128
                    || !path.getNodePos(path.getNodeCount()-1).equals(stand))continue;
            boolean permitted=true;
            for(int i=0;i<path.getNodeCount();i++) {
                var p=path.getNodePos(i);
                if(!level.isLoaded(p) || !SoilTripPolicy.contains(home(s),job,p.getX()+.5,p.getY(),p.getZ()+.5)) { permitted=false;break; }
            }
            if(permitted && claim(job,v,s,now) && v.getNavigation().moveTo(path,.6)) { s.soilTrip=true;s.soilPathCursor=0;return true; }
        }
        if(s.soilPathCursor<stands.size())return true;
        s.soilPathRejections++;return false;
    }
    static String work(ServerLevel level,Villager v,Session s,long now) {
        if(!eligible(level,v,s)) { if(s.soilTrip)v.getNavigation().stop();stop(s,v,now,40);s.soilStatus="not ready for soil work";return null; }
        var job=s.soilJob==null?null:ConstructionSites.soilJob(level,s.soilJob);
        if(job==null) {
            if(now<s.nextSoilScan)return null;s.nextSoilScan=now+100;
            job=ConstructionSites.soilJobs(level).stream().filter(j -> SoilTripPolicy.joins(home(s),j))
                    .filter(j -> !j.gathered() || v.getInventory().hasAnyMatching(i -> i.is(Items.DIRT)))
                    .filter(j -> {var c=CLAIMS.get(j.id());return c==null || now>=c.until() || c.actor().equals(v.getUUID());})
                    .min(Comparator.comparingDouble(j -> v.position().distanceToSqr(Vec3.atCenterOf(ConstructionSites.position(j.gathered()?j.target():j.source()))))).orElse(null);
            if(job==null)return null;s.soilJob=job.id();s.soilTrip=false;s.soilStarted=now;s.soilProgress.reset();s.soilPathObjective=null;
            v.getNavigation().stop();
        }
        var owner=CLAIMS.get(job.id());
        if(owner!=null && !owner.actor().equals(v.getUUID()) && now<owner.until()) { if(s.soilTrip)v.getNavigation().stop();stop(s,v,now,100);return null; }
        if(now-s.soilStarted>=1200 || !valid(level,v,s,job) || s.soilTrip && s.soilWorking==null
                && s.soilProgress.stalled(job.id()+":"+job.gathered(),now,v.getX(),v.getY(),v.getZ())) {
            v.getNavigation().stop();stop(s,v,now,400);return s.soilStatus="soil trip changed, stalled or expired";
        }
        BlockPos target=ConstructionSites.position(job.gathered()?job.target():job.source());
        s.action=null;s.before=null;s.succeeded=false;
        if(sight(level,v,target)) {
            if(!claim(job,v,s,now) || !AdventureActions.prepareBareHandForSoil(level,v)) { stop(s,v,now,200);return null; }
            v.getNavigation().stop();
            if(!target.equals(s.soilWorking) || now-s.lastSoilWork>10) { s.soilWorking=target;s.soilWorkStarted=now; }
            s.lastSoilWork=now;v.swingForAttack(InteractionHand.MAIN_HAND);
            if(now-s.soilWorkStarted>=40) {
                boolean delivered=job.gathered();boolean done=commit(level,v,s,job,target);s.soilWorking=null;s.soilWorkStarted=0;s.nextSoilPath=now;
                if(done && delivered)stop(s,v,now,100);
                else if(!done) { stop(s,v,now,200);return s.soilStatus="soil commit changed or refused"; }
            }
            return s.soilStatus=job.gathered()?"placing real farm soil":"gathering approved soil";
        }
        s.soilWorking=null;
        if(!route(level,v,s,job,target,now)) { v.getNavigation().stop();stop(s,v,now,400);return s.soilStatus="no complete permitted soil route"; }
        return s.soilStatus=job.gathered()?"carrying real dirt to farm":"travelling to approved soil";
    }
    private static boolean commit(ServerLevel level,Villager v,Session s,WorkSites.Soil job,BlockPos p) {
        if(!eligible(level,v,s) || !v.getMainHandItem().isEmpty() || !valid(level,v,s,job) || !sight(level,v,p))return false;
        var original=level.getBlockState(p);var below=level.getBlockState(p.below());var before=HarvestMaterials.snapshot(v.getInventory());
        var drops=Block.getDrops(original,level,p,null,v,ItemStack.EMPTY);
        var plan=job.gathered()?SoilMaterials.replace(v.getInventory(),drops):SoilMaterials.gather(v.getInventory(),drops);
        if(plan==null || !HarvestMaterials.unchanged(v.getInventory(),before) || !v.getMainHandItem().isEmpty()
                || !level.getBlockState(p).equals(original) || !level.getBlockState(p.below()).equals(below) || !valid(level,v,s,job))return false;
        var authored=job.gathered()?Blocks.DIRT.defaultBlockState():Blocks.AIR.defaultBlockState();
        return ConstructionSites.ownPlacement(level,p,() -> {
            if(!eligible(level,v,s) || !valid(level,v,s,job) || !sight(level,v,p) || !SoilMaterials.unchanged(v.getInventory(),plan))return false;
            if(!level.setBlock(p,authored,Block.UPDATE_ALL))return false;
            if(!job.equals(ConstructionSites.soilJob(level,job.id())) || !eligible(level,v,s) || !v.getMainHandItem().isEmpty()
                    || !level.getBlockState(p).equals(authored) || !level.getBlockState(p.below()).equals(below) || !sight(level,v,p)
                    || !SoilMaterials.commit(v.getInventory(),plan)) {
                if(level.getBlockState(p).equals(authored))level.setBlock(p,original,Block.UPDATE_ALL);
                ConstructionSites.retireSoil(job.id());return false;
            }
            if(job.gathered()) { s.soilDelivered++;ConstructionSites.soilDelivered(job); }
            else { s.soilGathered++;ConstructionSites.soilGathered(job); }
            level.levelEvent(2001,p,Block.getId(original));
            level.gameEvent(v,job.gathered()?GameEvent.BLOCK_PLACE:GameEvent.BLOCK_DESTROY,p);
            s.mind.exert(.005f);
            s.mind.remember((job.gathered()?"Placed one real dirt for farm at ":"Gathered one native dirt at ")+p.toShortString());Villagers.persist(s);
            s.lastSoilReceipt=Map.of("stage",job.gathered()?"delivery":"gather","position",p.toShortString(),"job",job.id().toString(),
                    "nativeDrops",drops.stream().map(ItemStack::toString).toList(),"inventoryBefore",plan.before.stream().map(ItemStack::toString).toList(),
                    "inventoryAfter",HarvestMaterials.snapshot(v.getInventory()).stream().map(ItemStack::toString).toList(),"blockAfter",level.getBlockState(p).toString());
            return true;
        });
    }
}
