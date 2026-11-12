package local.villagers;

import java.util.Comparator;
import local.villagers.capability.CapabilityPermissions.Capability;
import local.villagers.construction.WorkSites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Finite mangrove seed→bare-hand log quota→paid planting. Only explicit resource cells may change. */
final class ForestryWork {
    private record Step(WorkSites.Forestry revision, String kind, BlockPos target) {}
    private ForestryWork() {}
    static boolean matureSeed(ServerLevel level,BlockPos p) {
        if (!level.isLoaded(p) || !level.isLoaded(p.above())) return false;
        var state=level.getBlockState(p);
        return state.is(Blocks.MANGROVE_PROPAGULE) && state.getValue(MangrovePropaguleBlock.HANGING)
                && state.getValue(MangrovePropaguleBlock.AGE)==4 && state.getFluidState().isEmpty() && state.canSurvive(level,p);
    }
    private static net.minecraft.world.level.block.state.BlockState planted() {
        return Blocks.MANGROVE_PROPAGULE.defaultBlockState().setValue(MangrovePropaguleBlock.AGE,4);
    }
    static boolean plantSite(ServerLevel level,BlockPos p) {
        return level.isLoaded(p) && level.isLoaded(p.above()) && level.isLoaded(p.below())
                && level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
                && level.getFluidState(p).isEmpty() && planted().canSurvive(level,p);
    }
    private static boolean eligible(ServerLevel level,Villager v,Session s) {
        return AdventureActions.PERMISSIONS.allows(Capability.FORESTRY) && Villagers.isManaged(v) && v.level()==level
                && (v.getMainHandItem().isEmpty() || v.getMainHandItem().is(net.minecraft.tags.ItemTags.HOES))
                && FoodSupply.mayDoOptionalWork(s.mind.foodLevel())
                && FoodSupply.mayWork(v.getAge(),v.getHealth(),s.mind.fatigue(),
                !level.isDarkOutside() && !v.isSleeping() && !v.isTrading() && !v.isOnFire() && !v.isInWater() && v.getAirSupply()>=200);
    }
    /** Keep a safely stored hoe put away while this actor resumes its actual granted bare-hand job. */
    static boolean wantsBareHand(ServerLevel level,Villager v) {
        var s=Villagers.existingSession(v.getUUID());
        return s!=null && s.forestryJob!=null && eligible(level,v,s)
                && ConstructionSites.forestry(level,s.forestryJob)!=null;
    }
    static boolean home(ServerLevel level,Session s,WorkSites.Forestry job) {
        var points=new java.util.ArrayList<>(job.logs());points.addAll(job.clearance());points.addAll(job.roots());points.add(job.seed());points.add(job.plant());
        return points.stream().allMatch(p -> VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(ConstructionSites.position(p))));
    }
    private static boolean reachableEye(ServerLevel level,Villager v,BlockPos p) {
        if (v.getEyePosition().distanceToSqr(Vec3.atCenterOf(p))>9) return false;
        return visible(level,v,v.getEyePosition(),p);
    }
    private static boolean visible(ServerLevel level,Villager v,Vec3 eye,BlockPos p) {
        var hit=clip(level,v,eye,p);
        return hit!=null && (hit.getType()==HitResult.Type.MISS || hit.getBlockPos().equals(p));
    }
    static net.minecraft.world.phys.BlockHitResult clip(ServerLevel level,Villager v,Vec3 eye,BlockPos p) {
        var origin=BlockPos.containing(eye);
        for(int cx=Math.min(origin.getX()>>4,p.getX()>>4);cx<=Math.max(origin.getX()>>4,p.getX()>>4);cx++)
            for(int cz=Math.min(origin.getZ()>>4,p.getZ()>>4);cz<=Math.max(origin.getZ()>>4,p.getZ()>>4);cz++)
                if(!level.isLoaded(new BlockPos(cx<<4,p.getY(),cz<<4)))return null;
        return level.clip(new ClipContext(eye,Vec3.atCenterOf(p),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,v));
    }
    static Vec3 standEye(Villager v,BlockPos p) {
        return Vec3.atBottomCenterOf(p).add(0,v.getEyePosition().y-v.getY(),0);
    }
    /** Shared by production planning and the read-only probe; first failing native condition. */
    static String standFailure(ServerLevel level,Villager v,Session s,BlockPos target,BlockPos p) {
        if (!level.isLoaded(p) || !level.isLoaded(p.below())) return "unloaded";
        if (!level.getBlockState(p).getCollisionShape(level,p).isEmpty()) return "feet blocked";
        if (!level.getBlockState(p.above()).getCollisionShape(level,p.above()).isEmpty()) return "head blocked";
        if (!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP)) return "support not sturdy";
        var eye=standEye(v,p);
        if (eye.distanceToSqr(Vec3.atCenterOf(target))>9) return "out of reach";
        if (!visible(level,v,eye,target)) return "sight blocked";
        if (!VillageSurvival.withinHomeRange(level,s.mind,Vec3.atBottomCenterOf(p))) return "outside home";
        return null;
    }
    private static boolean valid(ServerLevel level,Villager v,Session s,WorkSites.Forestry job,BlockPos p,String kind) {
        var live=ConstructionSites.forestry(level,job.id());
        if (!job.equals(live) || !home(level,s,job) || !level.isLoaded(p)) return false;
        if (kind.equals("seed")) return !job.seedTaken() && matureSeed(level,p);
        if (ForestryMaterials.seed(v.getInventory())==null) return false;
        if (kind.equals("leaf")) return job.seedTaken() && job.uncleared().contains(new WorkSites.Point(job.seed().dimension(),p.getX(),p.getY(),p.getZ()))
                && level.getBlockState(p).is(Blocks.MANGROVE_LEAVES) && level.getEntities(null,new AABB(p).expandTowards(0,1,0)).isEmpty();
        if (kind.equals("root")) return job.seedTaken() && job.uncleared().isEmpty()
                && job.unrooted().contains(new WorkSites.Point(job.seed().dimension(),p.getX(),p.getY(),p.getZ()))
                && level.getBlockState(p).is(Blocks.MANGROVE_ROOTS) && level.getFluidState(p).isEmpty()
                && level.getEntities(null,new AABB(p).expandTowards(0,1,0)).isEmpty();
        if (kind.equals("log")) return job.seedTaken() && job.remaining().contains(new WorkSites.Point(job.seed().dimension(),p.getX(),p.getY(),p.getZ()))
                && job.uncleared().isEmpty() && job.unrooted().isEmpty() && level.getBlockState(p).is(Blocks.MANGROVE_LOG) && level.getEntities(null,new AABB(p).expandTowards(0,1,0)).isEmpty();
        return job.seedTaken() && job.remaining().isEmpty() && plantSite(level,p)
                && BuiltInRegistries.BLOCK.getKey(level.getBlockState(p.below()).getBlock()).toString().equals(job.soil());
    }
    private static void stop(Session s,long now,long delay) {
        s.forestryInterval.interrupt();
        s.forestryJob=null;s.forestryTarget=null;s.forestryWorkStarted=0;s.nextForestryScan=now+delay;s.forestryProgress.reset();
    }
    static String work(ServerLevel level,Villager v,Session s,long now) {
        if (s.forestryJob==null) s.forestryStatus="not working";
        if (!FoodSupply.mayDoOptionalWork(s.mind.foodLevel())) {
            if (s.forestryJob!=null) v.getNavigation().stop();
            s.forestryStatus="waiting for food before wood work";stop(s,now,40);return null;
        }
        if (!eligible(level,v,s)) { s.forestryStatus="not working";stop(s,now,40);return null; }
        var job=s.forestryJob==null ? null : ConstructionSites.forestry(level,s.forestryJob);
        if (job==null) {
            if (now<s.nextForestryScan) return null;
            s.nextForestryScan=now+100;
            job=ConstructionSites.forestry(level).stream().filter(f -> home(level,s,f))
                    .filter(f -> Math.abs(f.seed().x()-v.getX())<=40 && Math.abs(f.seed().z()-v.getZ())<=40)
                    .min(Comparator.comparingDouble(f -> v.position().distanceToSqr(Vec3.atCenterOf(ConstructionSites.position(f.seed()))))).orElse(null);
            if (job==null) return null;
            s.forestryJob=job.id();s.forestryStarted=now;
        }
        String kind=!job.seedTaken() ? "seed" : !job.uncleared().isEmpty() ? "leaf" : !job.unrooted().isEmpty() ? "root" : !job.remaining().isEmpty() ? "log" : "plant";
        var point=kind.equals("seed") ? job.seed() : kind.equals("leaf") ? job.uncleared().getFirst() : kind.equals("root") ? job.unrooted().getFirst() : kind.equals("log") ? job.remaining().getFirst() : job.plant();
        BlockPos target=ConstructionSites.position(point);
        if (!valid(level,v,s,job,target,kind) || now-s.forestryStarted>=1200) {
            s.forestryStatus="resource job changed,blocked or missing retained seed";stop(s,now,400);v.getNavigation().stop();return null;
        }
        // A food interruption may have equipped a newly crafted hoe. Preserve it in a real free slot
        // before continuing bare-hand loot/work; full inventories and weapons never lose equipment.
        if (!AdventureActions.prepareBareHandForSoil(level,v)) {
            s.forestryStatus="waiting for room to store hoe";stop(s,now,100);v.getNavigation().stop();return null;
        }
        if (!target.equals(s.forestryTarget) || now<s.lastForestryWork || now-s.lastForestryWork>10) {
            s.forestryTarget=target;s.forestryWorkStarted=0;s.forestryProgress.reset();
        }
        s.lastForestryWork=now;s.action=null;s.before=null;s.succeeded=false;
        int ticks=kind.equals("log") || kind.equals("root") ? Math.max(20,(int)Math.ceil(level.getBlockState(target).getDestroySpeed(level,target)*30)) : 20;
        var phase=s.forestryInterval.advance(new Step(job,kind,target),now,reachableEye(level,v,target),ticks);
        if (phase==local.villagers.navigation.WorkInterval.Phase.TRAVEL) {
            s.forestryWorkStarted=0;
            if (s.forestryProgress.stalled(target.toShortString(),now,v.getX(),v.getY(),v.getZ())) { stop(s,now,400);v.getNavigation().stop();return null; }
            if (now>=s.nextForestryPath && WorkNavigation.admit(level,now,v)) {
                s.nextForestryPath=now+40;
                // Native stands around each target; at most3actual paths per admitted batch.
                var stands=new java.util.ArrayList<BlockPos>();
                for (BlockPos p : BlockPos.betweenClosed(target.offset(-2,-3,-2),target.offset(2,0,2))) {
                    if (standFailure(level,v,s,target,p)==null) stands.add(p.immutable());
                }
                stands.sort(Comparator.comparingDouble(p -> v.position().distanceToSqr(Vec3.atBottomCenterOf(p))));
                if (!target.equals(s.forestryPathObjective)) { s.forestryPathObjective=target.immutable();s.forestryPathCursor=0; }
                s.forestryStandCandidates=stands.size();s.forestryPathBatches++;
                boolean moved=false;
                for (int attempt=0;attempt<Math.min(3,stands.size());attempt++) {
                    // Retain the cursor across a blocked-job backoff: other valid stands get a turn.
                    var stand=stands.get(Math.floorMod(s.forestryPathCursor++,stands.size()));
                    var path=v.getNavigation().createPath(stand,0);
                    boolean bounded=WorkNavigation.staysHome(level,v,s,path);
                    s.forestryLastPath=stand.toShortString()+": nodes="+(path==null ? 0 : path.getNodeCount())
                            +", canReach="+(path!=null && path.canReach())+", bounded="+bounded;
                    if (bounded) { moved=v.getNavigation().moveTo(path,.6);if (moved) break; }
                    s.forestryPathRejections++;
                }
                if (!moved) { s.forestryStatus="resource route blocked";stop(s,now,400);return null; }
            }
            return s.forestryStatus="approaching approved "+kind;
        }
        v.getNavigation().stop();
        v.swingForAttack(InteractionHand.MAIN_HAND);
        if (phase==local.villagers.navigation.WorkInterval.Phase.COMMIT) {
            boolean done=kind.equals("plant") ? plant(level,v,s,job,target) : gather(level,v,s,job,target,kind);
            s.forestryTarget=null;s.forestryWorkStarted=0;
            s.forestryInterval.interrupt();
            if (!done) { s.forestryStatus="resource commit changed or blocked";stop(s,now,200);return null; }
            if (kind.equals("plant")) stop(s,now,100);
        }
        return s.forestryStatus="working approved "+kind;
    }
    static boolean gather(ServerLevel level,Villager v,Session s,WorkSites.Forestry job,BlockPos p,String kind) {
        if (!eligible(level,v,s) || !v.getMainHandItem().isEmpty() || !valid(level,v,s,job,p,kind) || !reachableEye(level,v,p)) return false;
        var state=level.getBlockState(p);var item=kind.equals("seed") ? Items.MANGROVE_PROPAGULE : kind.equals("root") ? Items.MANGROVE_ROOTS : Items.MANGROVE_LOG;
        var drops=Block.getDrops(state,level,p,null,v,ItemStack.EMPTY);
        var plan=kind.equals("leaf") ? ForestryMaterials.leaves(v.getInventory(),drops) : ForestryMaterials.receive(v.getInventory(),drops,item);if (plan==null) return false;
        return ConstructionSites.ownPlacement(level,p,() -> {
            if (!v.getMainHandItem().isEmpty() || !valid(level,v,s,job,p,kind) || !ForestryMaterials.unchanged(v.getInventory(),plan)
                    || !level.getBlockState(p).equals(state) || !level.setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL)) return false;
            if (!eligible(level,v,s) || !v.getMainHandItem().isEmpty() || ConstructionSites.forestry(level,job.id())==null || !level.getBlockState(p).isAir()
                    || !ForestryMaterials.commit(v.getInventory(),plan)) {
                if (level.getBlockState(p).isAir()) level.setBlock(p,state,Block.UPDATE_ALL);return false;
            }
            var point=new WorkSites.Point(job.seed().dimension(),p.getX(),p.getY(),p.getZ());
            ConstructionSites.progress(kind.equals("seed") ? job.gatheredSeed() : kind.equals("leaf") ? job.cleared(point) : kind.equals("root") ? job.clearedRoot(point) : job.cut(point));
            if (kind.equals("seed")) s.propagulesGathered++;else if(kind.equals("leaf")) s.leavesCleared++;else if(kind.equals("root")) s.rootsCleared++;else s.logsGathered++;
            s.mind.exert(.005f);s.mind.remember(kind.equals("leaf") ? "Cleared one explicitly approved mangrove leaf at "+p.toShortString() : "Gathered actual "+item+" from approved resource at "+p.toShortString());
            level.levelEvent(2001,p,Block.getId(state));level.gameEvent(v,GameEvent.BLOCK_DESTROY,p);Villagers.persist(s);return true;
        });
    }
    static boolean plant(ServerLevel level,Villager v,Session s,WorkSites.Forestry job,BlockPos p) {
        if (!eligible(level,v,s) || !v.getMainHandItem().isEmpty() || !valid(level,v,s,job,p,"plant") || !reachableEye(level,v,p)
                || !level.getEntities(null,new AABB(p)).isEmpty()) return false;
        var seed=ForestryMaterials.seed(v.getInventory());var state=planted();
        return ConstructionSites.ownPlacement(level,p,() -> {
            if (!v.getMainHandItem().isEmpty() || !valid(level,v,s,job,p,"plant") || !ForestryMaterials.unchanged(v.getInventory(),seed)
                    || !level.setBlock(p,state,Block.UPDATE_ALL)) return false;
            if (!eligible(level,v,s) || !v.getMainHandItem().isEmpty() || ConstructionSites.forestry(level,job.id())==null || !level.getBlockState(p).equals(state)
                    || !state.canSurvive(level,p) || !ForestryMaterials.plant(v.getInventory(),seed)) {
                if (level.getBlockState(p).equals(state)) level.setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);return false;
            }
            ConstructionSites.finishForestry(job.id());s.propagulesPlanted++;s.mind.remember("Planted one real retained mangrove propagule at "+p.toShortString());
            level.gameEvent(v,GameEvent.BLOCK_PLACE,p);Villagers.persist(s);return true;
        });
    }
}
