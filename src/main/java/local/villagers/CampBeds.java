package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.village.poi.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;

/** Move free or personally owned cramped upper bunks; preserve other actors’ claims and occupied beds. */
final class CampBeds {
    /** A failed nearest batch must not hide a reachable stand on the other
     * side of a wall. Cache only bounded navigation attempts, never supplies. */
    static final class StandSearch {
        final Set<BlockPos> tried=new HashSet<>(),retried=new HashSet<>();BlockPos preferred;
        List<BlockPos> next(List<BlockPos> sorted){
            tried.retainAll(sorted);
            if(tried.containsAll(sorted))tried.clear();
            var batch=new ArrayList<BlockPos>();
            if(preferred!=null&&sorted.contains(preferred))batch.add(preferred);
            for(var p:sorted)if(batch.size()<8&&!tried.contains(p)&&!batch.contains(p))batch.add(p);
            tried.addAll(batch);preferred=null;return batch;
        }
        List<BlockPos> larger(List<BlockPos> sorted,BlockPos known){
            retried.retainAll(sorted);
            if(retried.containsAll(sorted))retried.clear();
            var batch=new ArrayList<BlockPos>();
            if(known!=null&&sorted.contains(known))batch.add(known);
            for(var p:sorted)if(batch.size()<3&&!retried.contains(p)&&!batch.contains(p))batch.add(p);
            retried.addAll(batch);return batch;
        }
    }
    private static final Map<Mob,LinkedHashMap<Set<BlockPos>,StandSearch>> STAND_SEARCHES=new WeakHashMap<>();
    static final class State {BlockPos target;long scanAt,pathAt,started,lastWork,until;int moved,rejections;String status="no bunk job";Map<String,Object> receipt=Map.of();final Map<BlockPos,Long> blocked=new HashMap<>();}
    static boolean bedItem(ItemStack stack){return !stack.isEmpty()&&Items.BED.asList().contains(stack.getItem());}
    private static BlockPos foot(ServerLevel level,BlockPos head){return head.relative(level.getBlockState(head).getValue(BedBlock.FACING).getOpposite());}
    private static boolean touch(ServerLevel level,Mob mob,Vec3 eye,BlockPos head,BlockPos foot){
        for(var end:List.of(head,foot)){
            if(eye.distanceToSqr(Vec3.atCenterOf(end))>9)continue;
            var hit=level.clip(new net.minecraft.world.level.ClipContext(eye,Vec3.atCenterOf(end),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,mob));
            if(hit.getType()==HitResult.Type.MISS||hit.getBlockPos().equals(head)||hit.getBlockPos().equals(foot))return true;
        }
        return false;
    }
    static net.minecraft.world.level.pathfinder.Path approachPath(ServerLevel level,Mob mob,BlockPos head,BlockPos foot){
        var stands=new ArrayList<BlockPos>();
        for(var end:List.of(head,foot))for(var p:BlockPos.betweenClosed(end.offset(-2,-1,-2),end.offset(2,1,2))){
            if(NativeCampWork.local(level,mob,p)&&level.isLoaded(p.above())&&level.getBlockState(p).isAir()&&level.getBlockState(p.above()).isAir()
                &&level.getFluidState(p).isEmpty()&&level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP)
                &&touch(level,mob,Vec3.atBottomCenterOf(p).add(0,mob.getEyeHeight(),0),head,foot))stands.add(p.immutable());
        }
        return pathToStands(level,mob,stands);
    }
    /** Shared bounded planning for physically valid camp interaction stands. */
    static net.minecraft.world.level.pathfinder.Path pathToStands(ServerLevel level,Mob mob,List<BlockPos> candidates){
        return NativeBodyRoutes.search(level,mob,()->NativePathLimits.search(mob,mob.getHealth()<=5?1:2,()->limitedPaths(level,mob,candidates)));
    }
    private static net.minecraft.world.level.pathfinder.Path limitedPaths(ServerLevel level,Mob mob,List<BlockPos> candidates){
        var stands=new ArrayList<BlockPos>();
        for(var p:candidates){
            if(!level.isLoaded(p))continue;
            // A candidate cell above air can describe the same supported feet
            // as the cell below it. Target the native node at that real floor,
            // preserving the rounded-up node for fractional slab/stair tops.
            double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,p);
            stands.add(BlockPos.containing(p.getX()+.5,Math.ceil(floor),p.getZ()+.5));
        }
        stands.sort(Comparator.comparingDouble(p->mob.position().distanceToSqr(Vec3.atBottomCenterOf(p))));
        stands=new ArrayList<>(new LinkedHashSet<>(stands));
        if(stands.isEmpty())return null;
        var searches=STAND_SEARCHES.computeIfAbsent(mob,m->new LinkedHashMap<>());
        var key=Set.copyOf(stands);var search=searches.computeIfAbsent(key,k->new StandSearch());
        while(searches.size()>8)searches.remove(searches.keySet().iterator().next());
        var known=search.preferred;var normal=search.next(stands);
        net.minecraft.world.level.pathfinder.Path best=null;BlockPos chosen=null;
        for(var p:normal){
            var path=mob.getNavigation().createPath(p,0,64);
            if(NativeIllagers.safePath(level,mob,path)&&(best==null||path.getNodeCount()<best.getNodeCount())){best=path;chosen=p;}
        }
        if(best!=null){search.preferred=chosen;return best;}
        // Native camp detours can exhaust the small follow-range search budget.
        // Retry at most three observed stands, including a normal search that
        // ended at its starting node. Later calls cover the remaining stands.
        // All native path and
        // drop checks still apply, and the normal navigation budget is restored.
        try{
            mob.getNavigation().setMaxVisitedNodesMultiplier(32);
            for(var p:search.larger(stands,known)){
                // A nearby stand across the camp stream can require over128
                // walking nodes around it. Search a longer native detour;
                // normal dry/body/drop/camp checks still admit the result.
                var path=mob.getNavigation().createPath(p,0,256);
                if(NativeIllagers.safePath(level,mob,path)&&(best==null||path.getNodeCount()<best.getNodeCount())){best=path;chosen=p;}
            }
        }finally{mob.getNavigation().resetMaxVisitedNodesMultiplier();}
        if(best!=null){search.preferred=chosen;return best;}
        return null;
    }
    static Map<String,Object> inspect(ServerLevel level,Pillager mob,BlockPos head){
        if(!(level.getBlockState(head).getBlock() instanceof BedBlock))return Map.of("position",head.toShortString(),"bed",false);
        mob.getNavigation().setCanOpenDoors(true);var foot=foot(level,head);
        var path=approachPath(level,mob,head,foot);net.minecraft.world.level.pathfinder.Path larger;
        try{mob.getNavigation().setMaxVisitedNodesMultiplier(4);larger=approachPath(level,mob,head,foot);}finally{mob.getNavigation().resetMaxVisitedNodesMultiplier();}
        var stands=new ArrayList<Map<String,Object>>();
        for(var end:List.of(head,foot))for(var p:BlockPos.betweenClosed(end.offset(-2,-1,-2),end.offset(2,1,2))){
            if(!level.getBlockState(p).isAir()||!level.getBlockState(p.above()).isAir()||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))continue;
            var route=mob.getNavigation().createPath(p,0,64);
            net.minecraft.world.level.pathfinder.Path longer;
            try{mob.getNavigation().setMaxVisitedNodesMultiplier(32);longer=mob.getNavigation().createPath(p,0,128);}finally{mob.getNavigation().resetMaxVisitedNodesMultiplier();}
            stands.add(Map.of("position",p.toShortString(),"touch",touch(level,mob,Vec3.atBottomCenterOf(p).add(0,mob.getEyeHeight(),0),head,foot),
                "nodes",route==null?0:route.getNodeCount(),"reachable",route!=null&&route.canReach(),"safe",NativeIllagers.safePath(level,mob,route),
                "end",route==null?"":route.getEndNode().asBlockPos().toShortString(),"longReachable",longer!=null&&longer.canReach(),"longSafe",NativeIllagers.safePath(level,mob,longer)));
        }
        return Map.of("position",head.toShortString(),"reusable",reusable(level,mob,head),"touching",touch(level,mob,mob.getEyePosition(),head,foot),
            "nativePathNodes",path==null?0:path.getNodeCount(),"largerBudgetPathNodes",larger==null?0:larger.getNodeCount(),"stands",stands);

    }
    static boolean reusable(ServerLevel level,Pillager mob,BlockPos head){
        if(!NativeCampWork.local(level,mob,head)||!NativeIllagers.insideCamp(level,mob,head)||!level.isLoaded(head.above(2)))return false;
        var b=level.getBlockState(head);
        if(!(b.getBlock() instanceof BedBlock)||b.getValue(BedBlock.PART)!=BedPart.HEAD||b.getValue(BedBlock.OCCUPIED)
                ||level.getBlockState(head.above()).isAir()&&level.getBlockState(head.above(2)).isAir()
                ||!(level.getBlockState(head.below()).getBlock() instanceof BedBlock)||!TraderRecruitment.allowedBed(level,head))return false;
        var foot=foot(level,head);var f=level.getBlockState(foot);
        if(!NativeCampWork.local(level,mob,foot)||!NativeIllagers.insideCamp(level,mob,foot)||!f.is(b.getBlock())||f.getValue(BedBlock.PART)!=BedPart.FOOT
                ||f.getValue(BedBlock.FACING)!=b.getValue(BedBlock.FACING)||f.getValue(BedBlock.OCCUPIED)||!TraderRecruitment.allowedBed(level,foot)
                ||!level.getEntities(null,new AABB(head).minmax(new AABB(foot)).expandTowards(0,2,0)).isEmpty())return false;
        return IllagerFamilies.ownsBed(mob,head)||level.getPoiManager().findAll(t->t.is(PoiTypes.HOME),head::equals,head,1,PoiManager.Occupancy.HAS_SPACE).findAny().isPresent();
    }
    static String work(ServerLevel level,Pillager mob,CreatureAgents.State agent,State w,long now){
        if(!AdventureActions.PERMISSIONS.allows(Capability.RELOCATE_CAMP_BED))return null;
        if(w.target!=null&&(now>=w.until||!reusable(level,mob,w.target))){w.target=null;w.started=0;}
        if(w.target==null&&now>=w.scanAt){
            w.scanAt=now+200;w.blocked.entrySet().removeIf(e->e.getValue()<=now);var origin=mob.blockPosition();
            // Use the same native HOME discovery as nursery claims. These are
            // existing free camp beds, not remotely inspected storage contents.
            var beds=level.getPoiManager().findAll(t->t.is(PoiTypes.HOME),p->!w.blocked.containsKey(p)&&reusable(level,mob,p),origin,64,PoiManager.Occupancy.ANY)
                .sorted(Comparator.comparingDouble(p->p.distSqr(origin))).limit(3).toList();
            for(var p:beds){
                // The manipulation route targets a supported adjacent stand,
                // not the upper bed's solid, low-headroom block itself.
                w.target=p.immutable();break;
            }
            w.until=now+1200;
        }
        if(w.target==null){w.status="no reusable free upper bunk nearby";return null;}
        var head=w.target;var foot=foot(level,head);
        if(!touch(level,mob,mob.getEyePosition(),head,foot)){
            w.started=0;
            if(now>=w.pathAt){w.pathAt=now+40;var path=approachPath(level,mob,head,foot);if(path==null||!mob.getNavigation().moveTo(path,.8)){w.rejections++;w.status="no complete route to a visible bed half";w.blocked.put(head,now+1200);w.target=null;w.scanAt=now+200;return null;}}
            return w.status="visiting an available cramped upper bunk";
        }
        mob.getNavigation().stop();mob.swingForAttack(InteractionHand.MAIN_HAND);
        if(w.started==0||now-w.lastWork>20)w.started=now;w.lastWork=now;
        var original=level.getBlockState(head);var originalFoot=level.getBlockState(foot);
        if(now-w.started<Math.max(1,(int)Math.ceil(original.getDestroySpeed(level,head)*30)))return w.status="dismantling an available cramped upper bunk";
        // Read native drops, reserve bag space, then remove both exact halves
        // without neighbour-generated duplicate drops. The lower bunk is retained.
        var drops=Block.getDrops(original,level,head,level.getBlockEntity(head),mob,mob.getMainHandItem());
        if(drops.size()!=1||!bedItem(drops.getFirst())||drops.getFirst().getCount()!=1||!mob.getInventory().canAddItem(drops.getFirst())){
            w.status="native bed drop or carrying capacity unavailable";w.target=null;w.scanAt=now+400;return null;
        }
        if(!reusable(level,mob,head)||!level.getBlockState(head).equals(original)||!level.getBlockState(foot).equals(originalFoot))return null;
        if(!level.setBlock(foot,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS))return null;
        if(!level.setBlock(head,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS)){
            level.setBlock(foot,originalFoot,Block.UPDATE_CLIENTS);return null;
        }
        var remaining=mob.getInventory().addItem(drops.getFirst().copy());
        if(!remaining.isEmpty()){
            level.setBlock(foot,originalFoot,Block.UPDATE_CLIENTS);level.setBlock(head,original,Block.UPDATE_CLIENTS);return null;
        }
        level.updateNeighborsAt(foot,originalFoot.getBlock());level.updateNeighborsAt(head,original.getBlock());
        IllagerFamilies.removedOwnBed(mob,head,agent);
        level.levelEvent(2001,head,Block.getId(original));level.gameEvent(mob,GameEvent.BLOCK_DESTROY,head);
        w.moved++;w.receipt=Map.of("gameTime",now,"head",head.toShortString(),"foot",foot.toShortString(),"item",drops.getFirst().toString(),"removedHalves",2,"collectedBeds",1);
        agent.mind.exert(.005f);agent.mind.remember("Collected one real available cramped upper bunk for relocation at "+head.toShortString());
        w.target=null;w.started=0;return w.status="carrying a real available bed to usable shelter";
    }
    private CampBeds(){}
}
