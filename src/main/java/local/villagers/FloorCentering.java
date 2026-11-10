package local.villagers;

import local.villagers.navigation.FloorCenteringPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Escape a clipped start cell using normal MoveControl/gravity. Never sets position,velocity or blocks. */
final class FloorCentering {
    private Vec3 target, start;
    private BlockPos support;
    private BlockState supportState;
    private long nextProbe, until;
    long attempts, successes, failures;
    boolean active() { return target != null; }
    private void stop(long now, boolean success) {
        if (target != null) { if (success) successes++; else failures++; }
        target = start = null; support = null; supportState = null; nextProbe = now + 400;
    }
    static boolean safeSupport(BlockState s) {
        return s.getFluidState().isEmpty() && !s.is(BlockTags.FIRE) && !s.is(Blocks.MAGMA_BLOCK) && !s.is(Blocks.CACTUS)
                && !s.is(Blocks.CAMPFIRE) && !s.is(Blocks.SOUL_CAMPFIRE) && !s.is(Blocks.POWDER_SNOW)
                && !s.is(Blocks.SWEET_BERRY_BUSH) && !s.is(Blocks.WITHER_ROSE);
    }
    private boolean clearTarget(ServerLevel level, Villager v) {
        return level.isLoaded(support) && level.isLoaded(BlockPos.containing(target).above(2))
                && level.getBlockState(support).equals(supportState)
                && level.noCollision(v, v.getBoundingBox().move(target.subtract(v.position())));
    }
    String tick(ServerLevel level, Villager v, boolean danger, long now) {
        if (danger || v.isSleeping() || v.isTrading() || v.isPassenger() || v.getPose() != Pose.STANDING
                || v.isInWater() || v.isInLava() || v.isOnFire() || v.getAirSupply() < 200) {
            if (active()) stop(now,false); return null;
        }
        if (active()) {
            if (v.onGround() && level.noCollision(v) && Math.abs(v.getY()-target.y) < .1
                    && v.position().subtract(target).horizontalDistanceSqr() <= .5) {
                stop(now,true); return null;
            }
            if (now >= until || !FloorCenteringPolicy.local(start.x,start.y,start.z,target.x,target.y,target.z,v.getBbWidth())
                    || Math.floor(v.getX()) != Math.floor(target.x) || Math.floor(v.getZ()) != Math.floor(target.z)
                    || !clearTarget(level,v)) { stop(now,false); return null; }
        } else {
            if (now < nextProbe || !v.onGround() || !v.getNavigation().isDone()) return null;
            nextProbe = now + 40;
            // Rare collision query, not a terrain scan or a response to another resident merely standing close.
            if (level.noCollision(v) || !level.getBlockCollisions(v,v.getBoundingBox()).iterator().hasNext()) return null;
            nextProbe = now + 200;
            BlockPos cell = v.blockPosition();
            for (int down = 1; down <= 2; down++) {
                BlockPos floor = cell.below(down);
                if (!level.isLoaded(floor)) continue;
                var state = level.getBlockState(floor);
                Vec3 candidate = new Vec3(cell.getX()+.5, floor.getY()+1, cell.getZ()+.5);
                if (!safeSupport(state) || !state.isFaceSturdy(level,floor,Direction.UP)
                        || !FloorCenteringPolicy.local(v.getX(),v.getY(),v.getZ(),candidate.x,candidate.y,candidate.z,v.getBbWidth())
                        || !level.isLoaded(BlockPos.containing(candidate).above(2))
                        || !level.noCollision(v,v.getBoundingBox().move(candidate.subtract(v.position())))) continue;
                target = candidate; start = v.position(); support = floor; supportState = state; until = now + 40; attempts++; break;
            }
            if (!active()) return null;
        }
        v.getNavigation().stop();
        v.getMoveControl().setWantedPosition(target.x,target.y,target.z,.35);
        return "centering on safe floor";
    }
}
