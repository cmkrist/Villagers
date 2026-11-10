package local.villagers;

import java.util.Comparator;
import local.villagers.capability.CapabilityPermissions.Capability;
import local.villagers.capability.CropLayout;
import local.villagers.capability.CropPolicy.CropKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Bounded till-and-plant on explicitly approved bare ground. No demolition, supplied seed or instant maturity. */
final class FarmExpansion {
    private FarmExpansion() {}
    static String invest(ServerLevel level,Villager v,Session s,long now) {
        boolean available=!VillageSurvival.scheduledRest(level,v) && !v.isSleeping() && !v.isTrading()
                && !v.isOnFire() && !v.isInWater() && !v.isInLava() && v.getAirSupply()>=200
                && v.getHealth()>=v.getMaxHealth()*.9 && s.mind.fatigue()<.5;
        if(!FoodSupply.mayInvest(FoodSupply.mayWork(v.getAge(),v.getHealth(),s.mind.fatigue(),available),
                s.mind.foodLevel(),FoodSupply.deficit(v.getInventory(),AdventureActions.canHarvest(v))))return null;
        return work(level,v,s,now);
    }
    /** Local visible crop layout is a preference, never a new grant or a source of planting items. */
    private static PlantingMaterials.Plan planting(ServerLevel level,Villager v,BlockPos crop) {
        var seen=new java.util.ArrayList<CropLayout.Neighbour>();
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
            if(dx==0 && dz==0)continue;
            var p=crop.offset(dx,0,dz);
            if(!level.isLoaded(p) || v.getEyePosition().distanceToSqr(Vec3.atCenterOf(p))>64)continue;
            var state=level.getBlockState(p);
            var kind=state.is(Blocks.WHEAT)?CropKind.WHEAT:
                    state.is(Blocks.BEETROOTS)?CropKind.BEETROOT:
                    state.is(Blocks.CARROTS)?CropKind.CARROT:
                    state.is(Blocks.POTATOES)?CropKind.POTATO:CropKind.OTHER;
            if(kind==CropKind.OTHER)continue;
            var hit=ForestryWork.clip(level,v,v.getEyePosition(),p);
            if(hit!=null && (hit.getType()==HitResult.Type.MISS || hit.getBlockPos().equals(p)))
                seen.add(new CropLayout.Neighbour(dx,dz,kind));
        }
        return PlantingMaterials.reserve(v.getInventory(),item -> {
            var kind=item==Items.WHEAT_SEEDS?CropKind.WHEAT:
                    item==Items.BEETROOT_SEEDS?CropKind.BEETROOT:
                    item==Items.CARROT?CropKind.CARROT:CropKind.POTATO;
            return CropLayout.preference(kind,seen);
        });
    }
    static String work(ServerLevel level, Villager v, Session s, long now) {
        if (!AdventureActions.PERMISSIONS.allows(Capability.CULTIVATE_PLANT) || !AdventureActions.canHarvest(v)) return null;
        if (s.expansionTarget != null && !ground(level, v, s, s.expansionTarget)) s.expansionTarget = null;
        if (s.expansionTarget == null) {
            if (now < s.nextExpansionScan) return null;
            s.nextExpansionScan = now + 100;
            if (PlantingMaterials.reserve(v.getInventory()) == null) return null;
            s.expansionTarget = ConstructionSites.candidates(level, v.blockPosition()).stream()
                    .filter(p -> p.distSqr(v.blockPosition()) <= 64 && ground(level, v, s, p))
                    .min(Comparator.comparingDouble(p -> v.position().distanceToSqr(Vec3.atCenterOf(p)))).orElse(null);
        }
        BlockPos soil = s.expansionTarget;
        if (soil == null) return null;
        s.action = null; s.before = null; s.succeeded = false;
        if (visibleReach(level, v, soil)) {
            v.getNavigation().stop();
            if (!soil.equals(s.expansionWorking) || now - s.lastExpansionWork > 10) {
                s.expansionWorking = soil; s.expansionStarted = now;
            }
            s.lastExpansionWork = now;
            if (now - s.expansionStarted >= 20) {
                boolean planted = plant(level, v, s, soil);
                s.expansionTarget = s.expansionWorking = null; s.nextExpansionScan = now + 100;
                s.foodStatus = planted ? "planted a new farm plot" : "farm plot changed or blocked";
                return s.foodStatus;
            }
            return "tilling and planting";
        }
        s.expansionWorking = null;
        if (now >= s.nextExpansionPath && WorkNavigation.admit(level, now, v)) {
            s.nextExpansionPath = now + 40;
            int attempts = 0; boolean moved = false;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos stand = soil.above().relative(direction);
                if (!level.isLoaded(stand) || !level.getFluidState(stand).isEmpty()
                        || !level.getBlockState(stand).getCollisionShape(level, stand).isEmpty()
                        || !level.getBlockState(stand.above()).getCollisionShape(level, stand.above()).isEmpty()
                        || level.getBlockState(stand.below()).getCollisionShape(level, stand.below()).isEmpty()) continue;
                if (++attempts > 3) break;
                var path = v.getNavigation().createPath(stand, 0);
                if (WorkNavigation.staysHome(level, v, s, path)) { moved = v.getNavigation().moveTo(path, .6); if (moved) break; }
            }
            if (!moved) { s.expansionTarget = null; s.nextExpansionScan = now + 200; return "farm expansion route blocked"; }
        }
        return "approaching approved farm ground";
    }
    private static boolean ground(ServerLevel level, Villager v, Session s, BlockPos soil) {
        if (!ConstructionSites.allowsFarm(level, soil) || !level.isLoaded(soil) || !level.isLoaded(soil.above(2))
                || !VillageSurvival.withinHomeRange(level, s.mind, Vec3.atCenterOf(soil.above()))) return false;
        var state = level.getBlockState(soil);
        if ((!state.is(Blocks.DIRT) && !state.is(Blocks.GRASS_BLOCK)) || level.getBlockEntity(soil) != null
                || !level.getBlockState(soil.above()).isAir() || !level.getBlockState(soil.above(2)).isAir()
                || !level.canSeeSky(soil.above()) || !level.getBlockState(soil.below()).isFaceSturdy(level, soil.below(), Direction.UP)
                || !level.getEntities(null, new AABB(soil.above()).expandTowards(0, 1, 0)).isEmpty()) return false;
        for (BlockPos p : BlockPos.betweenClosed(soil.offset(-4,0,-4), soil.offset(4,1,4)))
            if (level.isLoaded(p) && level.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER)) return true;
        return false;
    }
    private static boolean visibleReach(ServerLevel level, Villager v, BlockPos soil) {
        Vec3 target = Vec3.atCenterOf(soil).add(0, .49, 0);
        if (v.getEyePosition().distanceToSqr(target) > 9) return false;
        var hit = level.clip(new ClipContext(v.getEyePosition(), target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, v));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(soil);
    }
    static boolean plant(ServerLevel level, Villager v, Session s, BlockPos soil) {
        if (!AdventureActions.PERMISSIONS.allows(Capability.CULTIVATE_PLANT) || !Villagers.isManaged(v) || !v.isAlive() || v.level() != level || v.isBaby() || v.isSleeping()
                || v.isTrading() || v.isOnFire() || v.isInWater() || !ground(level, v, s, soil)
                || !visibleReach(level, v, soil) || !AdventureActions.prepareHoeForWork(level, v)) return false;
        var plan = planting(level,v,soil.above());
        if (plan == null) return false;
        ItemStack hoe = v.getMainHandItem(), hoeBefore = hoe.copy();
        BlockState before = level.getBlockState(soil), farmland = Blocks.FARMLAND.defaultBlockState();
        BlockState crop = plan.before().is(Items.WHEAT_SEEDS) ? Blocks.WHEAT.defaultBlockState()
                : plan.before().is(Items.CARROT) ? Blocks.CARROTS.defaultBlockState()
                : plan.before().is(Items.POTATO) ? Blocks.POTATOES.defaultBlockState() : Blocks.BEETROOTS.defaultBlockState();
        return ConstructionSites.ownEdit(level, soil, () -> {
            if (!ground(level, v, s, soil) || !PlantingMaterials.unchanged(v.getInventory(), plan)) return false;
            if (!level.setBlock(soil, farmland, Block.UPDATE_ALL)) return false;
            boolean placed = crop.canSurvive(level, soil.above()) && level.setBlock(soil.above(), crop, Block.UPDATE_ALL);
            if (!placed || !ConstructionSites.allowsFarm(level, soil) || !level.getBlockState(soil).equals(farmland)
                    || !level.getBlockState(soil.above()).equals(crop) || !ItemStack.matches(v.getMainHandItem(), hoeBefore)
                    || !PlantingMaterials.commit(v.getInventory(), plan)) {
                if (level.getBlockState(soil.above()).equals(crop)) level.setBlock(soil.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                if (level.getBlockState(soil).equals(farmland)) level.setBlock(soil, before, Block.UPDATE_ALL);
                return false;
            }
            hoe.hurtAndBreak(1, v, EquipmentSlot.MAINHAND);
            v.swingForAttack(InteractionHand.MAIN_HAND); level.gameEvent(v, GameEvent.BLOCK_CHANGE, soil);
            ConstructionSites.completed(level, soil); s.plotsCultivated++;
            s.mind.remember("Expanded farm: planted " + crop.getBlock() + " at " + soil.above().toShortString()
                    + " using one real planting item and hoe wear");
            return true;
        });
    }
}
