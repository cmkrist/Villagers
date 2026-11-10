package local.villagers;

import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A real interior standing goal when the native bed goal is unreachable; no terrain or actor setters. */
final class RefugeRoutes {
    record Result(Path path, BlockPos stand, int inspected, int queries) {}
    private RefugeRoutes() {}

    static Result find(ServerLevel level, Villager v, BlockPos bed, BlockPos cached, boolean allowFloorSearch) {
        int queries=0;
        if (cached!=null && standingPlace(level,v,bed,cached)) {
            var path=v.getNavigation().createPath(cached,0);queries++;
            if (reaches(path,cached)) return new Result(path,cached,0,queries);
        }
        // Preserve the normal native bed route when it already works.
        Path bedPath=v.getNavigation().createPath(bed,1);queries++;
        if (bedPath!=null && bedPath.canReach()) return new Result(bedPath,null,0,queries);
        if (!allowFloorSearch) return new Result(bedPath,null,0,queries);
        var candidates=new ArrayList<BlockPos>();int inspected=0;
        for (BlockPos p:BlockPos.betweenClosed(bed.offset(-2,-1,-2),bed.offset(2,1,2))) {
            inspected++;
            if (!p.equals(cached) && standingPlace(level,v,bed,p)) candidates.add(p.immutable());
        }
        candidates.sort(Comparator.comparingDouble(p -> v.position().distanceToSqr(Vec3.atBottomCenterOf(p))));
        for (var stand:candidates) {
            if (queries>=3) break;
            Path path=v.getNavigation().createPath(stand,0);queries++;
            if (reaches(path,stand)) return new Result(path,stand,inspected,queries);
        }
        return new Result(bedPath,null,inspected,queries);
    }
    private static boolean reaches(Path path,BlockPos stand) {
        return path!=null && path.canReach() && path.getNodeCount()>0 && path.getNodeCount()<=128
                && path.getNodePos(path.getNodeCount()-1).equals(stand);
    }
    private static boolean standingPlace(ServerLevel level,Villager v,BlockPos bed,BlockPos p) {
        if (v.isSleeping() || v.isTrading() || !TraderRecruitment.allowedBed(level,bed)
                || Vec3.atBottomCenterOf(p).distanceToSqr(Vec3.atCenterOf(bed))>6.25) return false;
        for (int cx=(Math.min(p.getX(),bed.getX())-1)>>4;cx<=(Math.max(p.getX(),bed.getX())+1)>>4;cx++)
            for (int cz=(Math.min(p.getZ(),bed.getZ())-1)>>4;cz<=(Math.max(p.getZ(),bed.getZ())+1)>>4;cz++)
                if (!level.isLoaded(new BlockPos(cx<<4,p.getY(),cz<<4))) return false;
        if (!level.isLoaded(p.above(2)) || !level.isLoaded(p.below()) || level.canSeeSky(p.above())) return false;
        var support=level.getBlockState(p.below());
        if (!support.isFaceSturdy(level,p.below(),Direction.UP) || !safe(support)
                || !safe(level.getBlockState(p)) || !safe(level.getBlockState(p.above()))) return false;
        var dimensions=v.getDimensions(Pose.STANDING);Vec3 feet=Vec3.atBottomCenterOf(p);
        double half=dimensions.width()/2;
        if (!level.noCollision(v,new AABB(feet.x-half,feet.y,feet.z-half,feet.x+half,feet.y+dimensions.height(),feet.z+half))) return false;
        Vec3 eyes=feet.add(0,v.getEyeHeight(),0);
        var hit=level.clip(new ClipContext(eyes,Vec3.atCenterOf(bed),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,v));
        return hit.getType()==HitResult.Type.MISS || hit.getBlockPos().equals(bed);
    }
    private static boolean safe(net.minecraft.world.level.block.state.BlockState s) {
        return s.getFluidState().isEmpty() && !s.is(BlockTags.FIRE) && !s.is(Blocks.MAGMA_BLOCK) && !s.is(Blocks.CACTUS)
                && !s.is(Blocks.CAMPFIRE) && !s.is(Blocks.SOUL_CAMPFIRE) && !s.is(Blocks.POWDER_SNOW)
                && !s.is(Blocks.SWEET_BERRY_BUSH) && !s.is(Blocks.WITHER_ROSE);
    }
}
