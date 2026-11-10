package local.villagers;

import java.util.Comparator;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Finite, paid table construction at an operator-approved safe air cell. No replacement/demolition. */
final class StationProduction {
    private StationProduction() {}
    /** An explicit table job can employ a fed woodworker; food-critical station work keeps its old exception. */
    static boolean hasPurpose(net.minecraft.world.Container inventory,int food,boolean foodWork) {
        return foodWork || FoodSupply.mayDoOptionalWork(food)
                && (inventory.hasAnyMatching(stack -> stack.is(net.minecraft.world.item.Items.CRAFTING_TABLE))
                || WoodCrafting.canMakeTable(inventory));
    }
    private static boolean eligible(ServerLevel level, Villager v, Session s) {
        return AdventureActions.PERMISSIONS.allows(Capability.BUILD_CRAFTING_TABLE) && Villagers.isManaged(v)
                && v.level() == level && FoodSupply.mayWork(v.getAge(), v.getHealth(), s.mind.fatigue(),
                !level.isDarkOutside() && !v.isSleeping() && !v.isTrading() && !v.isInWater()
                        && !v.isOnFire() && v.getAirSupply() >= 200)
                && hasPurpose(v.getInventory(),s.mind.foodLevel(),BreadCrafting.wheatCount(v.getInventory()) >= 3
                || !AdventureActions.canHarvest(v) && v.getInventory().hasAnyMatching(PlantingMaterials::supported));
    }
    static boolean safeSite(ServerLevel level, BlockPos pos) { return surroundings(level, pos, Blocks.AIR.defaultBlockState()); }
    private static boolean surroundings(ServerLevel level, BlockPos pos, BlockState expected) {
        if (!level.isLoaded(pos) || !level.isLoaded(pos.above()) || !level.isLoaded(pos.below())
                || !level.getBlockState(pos).equals(expected) || !level.getBlockState(pos.above()).isAir()
                || !level.getFluidState(pos).isEmpty() || level.getBlockState(pos.below()).is(Blocks.FARMLAND)
                || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) return false;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2,-1,-2), pos.offset(2,1,2))) {
            if (!level.isLoaded(p)) return false;
            var state = level.getBlockState(p);
            if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof BedBlock || level.getBlockEntity(p) != null) return false;
        }
        int access = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            var p = pos.relative(d);
            if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
                    && level.getFluidState(p).isEmpty() && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) access++;
        }
        return access >= 2;
    }
    private static boolean site(ServerLevel level, Villager v, Session s, BlockPos pos) {
        return ConstructionSites.allowsStation(level, pos) && safeSite(level, pos)
                && VillageSurvival.withinHomeRange(level, s.mind, Vec3.atCenterOf(pos));
    }
    private static boolean reach(ServerLevel level, Villager v, BlockPos pos) {
        if (v.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > 9) return false;
        var hit = level.clip(new ClipContext(v.getEyePosition(), Vec3.atCenterOf(pos), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, v));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }
    private static void interrupt(Session s) { s.stationTarget = null; s.stationWorkStarted = 0; s.stationTripStarted = 0; s.stationProgress.reset(); }
    static String work(ServerLevel level, Villager v, Session s, long now) {
        if (!eligible(level, v, s)) { interrupt(s); return null; }
        if (now < s.nextStationScan && s.stationTarget == null) return null;
        // With no admitted job, do not scan native terrain for tables or allocate recipe reservations.
        if (s.stationTarget==null && ConstructionSites.stationCandidates(level,v.blockPosition()).isEmpty()) {
            s.nextStationScan=now+100;s.stationStatus="no approved clear table site";return null;
        }
        if (BreadCrafting.table(level, v, s) != null) { interrupt(s); s.nextStationScan = now + 100; return null; }
        var held = StationMaterials.reserve(v.getInventory());
        var recipe = held == null ? WoodCrafting.table(v.getInventory()) : null;
        if (held == null && recipe == null) { interrupt(s); s.nextStationScan = now + 100; s.stationStatus = "missing table materials"; return null; }
        if (s.stationTarget != null && (!site(level, v, s, s.stationTarget) || now < s.lastStationWork || now - s.lastStationWork > 10)) interrupt(s);
        if (s.stationTarget == null) {
            if (now < s.nextStationScan) return null;
            s.nextStationScan = now + 100;
            s.stationTarget = ConstructionSites.stationCandidates(level, v.blockPosition()).stream()
                    .filter(p -> site(level, v, s, p)).min(Comparator.comparingDouble(p -> p.distSqr(v.blockPosition()))).orElse(null);
            if (s.stationTarget == null) { s.stationStatus = "no approved clear table site"; return null; }
            s.stationTripStarted = now; s.stationProgress.reset();
        }
        var pos = s.stationTarget; s.lastStationWork = now;
        s.action = null; s.before = null; s.succeeded = false;
        if (now - s.stationTripStarted >= 400) { interrupt(s); s.nextStationScan = now + 400; v.getNavigation().stop(); return null; }
        if (!reach(level, v, pos)) {
            s.stationWorkStarted = 0;
            if (s.stationProgress.stalled(pos.toShortString(), now, v.getX(), v.getY(), v.getZ())) {
                interrupt(s); s.nextStationScan = now + 400; v.getNavigation().stop(); return null;
            }
            if (now >= s.nextStationPath && WorkNavigation.admit(level, now, v)) {
                s.nextStationPath = now + 40; int attempts = 0; boolean moved = false;
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    var stand = pos.relative(direction);
                    if (!level.getBlockState(stand).isAir() || !level.getBlockState(stand.above()).isAir()
                            || !level.getBlockState(stand.below()).isFaceSturdy(level, stand.below(), Direction.UP)) continue;
                    if (++attempts > 3) break;
                    var path = v.getNavigation().createPath(stand, 0);
                    if (WorkNavigation.staysHome(level, v, s, path)) { moved = v.getNavigation().moveTo(path, .6); if (moved) break; }
                }
                if (!moved) { interrupt(s); s.nextStationScan = now + 400; return null; }
            }
            return s.stationStatus = "approaching approved table site";
        }
        v.getNavigation().stop();
        String kind = held == null ? recipe.step().name() : "PLACE";
        if (s.stationWorkStarted == 0 || !kind.equals(s.stationWorkKind)) { s.stationWorkStarted = now; s.stationWorkKind = kind; }
        if (now - s.stationWorkStarted >= 20) {
            boolean okay = held == null ? craft(level, v, s, pos) : place(level, v, s, pos);
            s.stationWorkStarted = 0;
            if (!okay) { interrupt(s); s.nextStationScan = now + 200; return null; }
            if (held != null) interrupt(s);
        }
        return s.stationStatus = held == null ? "crafting a real table" : "building a real table";
    }
    static boolean craft(ServerLevel level, Villager v, Session s, BlockPos pos) {
        if (!eligible(level, v, s) || !site(level, v, s, pos) || !reach(level, v, pos)) return false;
        var plan = WoodCrafting.table(v.getInventory()); if (plan == null) return false;
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, plan.input(), level); if (recipe.isEmpty()) return false;
        var rule = recipe.get().value(); var output = rule.assemble(plan.input());
        if (rule.getRemainingItems(plan.input()).stream().anyMatch(stack -> !stack.isEmpty())
                || !eligible(level, v, s) || !site(level, v, s, pos) || !reach(level, v, pos)
                || !WoodCrafting.commit(v.getInventory(), plan, output)) return false;
        s.stationRecipesCrafted++; v.swingForAttack(InteractionHand.MAIN_HAND);
        s.mind.remember("Crafted " + output.getCount() + " " + output.getItem() + " from owned wood for an approved table site");
        Villagers.persist(s); return true;
    }
    static boolean place(ServerLevel level, Villager v, Session s, BlockPos pos) {
        if (!eligible(level, v, s) || !site(level, v, s, pos) || !reach(level, v, pos)
                || !level.getEntities(null, new AABB(pos)).isEmpty()) return false;
        var plan = StationMaterials.reserve(v.getInventory()); if (plan == null) return false;
        var table = Blocks.CRAFTING_TABLE.defaultBlockState();
        return ConstructionSites.ownPlacement(level, pos, () -> {
            if (!site(level, v, s, pos) || !StationMaterials.unchanged(v.getInventory(), plan)
                    || !level.setBlock(pos, table, Block.UPDATE_ALL)) return false;
            if (!eligible(level, v, s) || !ConstructionSites.allowsStation(level, pos) || !reach(level, v, pos)
                    || !surroundings(level, pos, table) || !level.getEntities(null, new AABB(pos)).isEmpty()
                    || !StationMaterials.commit(v.getInventory(), plan)) {
                if (level.getBlockState(pos).equals(table)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                return false;
            }
            ConstructionSites.completed(level, pos); s.tablesBuilt++; s.craftingTable = pos.immutable(); s.nextCraftScan = level.getGameTime();
            s.nextToolWork = level.getGameTime();
            level.gameEvent(v, GameEvent.BLOCK_PLACE, pos); v.swingForAttack(InteractionHand.MAIN_HAND);
            s.mind.remember("Built crafting table at " + pos.toShortString() + " using one real crafted/owned table item");
            Villagers.persist(s); return true;
        });
    }
}
