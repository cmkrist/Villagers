package local.villagers;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/** A boat job may gather a small natural trunk quota and bootstrap one paid outdoor workbench. */
final class BoatPreparation {
    static int boatSlot(Villager v) {
        for (int i = 0; i < v.getInventory().getContainerSize(); i++)
            if (BoatMaterials.type(v.getInventory().getItem(i)) != null) return i;
        return -1;
    }
    static String work(ServerLevel level, Villager v, Session s, long now) {
        boolean needsHoe = s.boatTrip != null && s.boatTrip.harvest && !AdventureActions.hasHoe(v);
        if (boatSlot(v) >= 0 && !needsHoe) return null;
        if (now < s.nextBoatWork) return s.boatStatus;
        var table = BreadCrafting.table(level, v, s);
        if(table==null&&BreadCrafting.rememberedTable(level,v,s)!=null) {
            s.nextBoatWork=Math.max(now+20,s.blockedCraftUntil);
            return s.boatStatus="waiting for access to the remembered workbench";
        }
        // Native plank/table intermediates are portable. A boat itself requires an actual 3x3 workbench.
        var plan = table == null && !v.getInventory().hasAnyMatching(a -> a.is(Items.CRAFTING_TABLE))
            ? WoodCrafting.table(v.getInventory()) : boatSlot(v) >= 0 && needsHoe
                ? WoodCrafting.next(v.getInventory()) : WoodCrafting.boat(v.getInventory());
        if (table == null && v.getInventory().hasAnyMatching(a -> a.is(Items.CRAFTING_TABLE))) {
            if (placeTable(level, v, s)) return s.boatStatus = "placed owned boat workbench";
            return s.boatStatus = "boat needs a clear workbench site";
        }
        if (plan == null) return gather(level, v, s, now);
        if ((plan.step() == WoodCrafting.Step.BOAT || plan.step() == WoodCrafting.Step.HOE) && !BreadCrafting.canWorkAt(level, v, s, table)) {
            BreadCrafting.approach(level, v, s, table, now);
            return s.boatStatus = "visiting workbench to build a boat";
        }
        if (!craft(level, v, s, plan, table)) return s.boatStatus = "boat recipe or inventory blocked";
        s.nextBoatWork = now + 30;
        return s.boatStatus = "crafting boat supplies from owned wood";
    }
    static boolean craft(ServerLevel level, Villager v, Session s, WoodCrafting.Plan plan, BlockPos table) {
        if (!AdventureActions.PERMISSIONS.allows(local.villagers.capability.CapabilityPermissions.Capability.CRAFT_BOAT)
                || !Villagers.isManaged(v) || !v.isAlive() || v.isBaby() || v.isTrading() || v.isSleeping() || v.isInWater()
                || plan == null || plan.step() == WoodCrafting.Step.PICKAXE || plan.step() == WoodCrafting.Step.STONE_PICKAXE
                || plan.step() == WoodCrafting.Step.HOE && !AdventureActions.PERMISSIONS.allows(local.villagers.capability.CapabilityPermissions.Capability.CRAFT_WOODEN_HOE)
                || (plan.step() == WoodCrafting.Step.BOAT || plan.step() == WoodCrafting.Step.HOE) && !BreadCrafting.canWorkAt(level, v, s, table)) return false;
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, plan.input(), level);
        if (recipe.isEmpty() || recipe.get().value().getRemainingItems(plan.input()).stream().anyMatch(a -> !a.isEmpty())
                || !WoodCrafting.commit(v.getInventory(), plan, recipe.get().value().assemble(plan.input()))) return false;
        s.boatRecipes++; s.mind.remember("Made " + plan.output() + " from owned wood for a boat journey");
        Villagers.persist(s); return true;
    }
    static boolean placeTable(ServerLevel level, Villager v, Session s) {
        var origin = v.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-2, 0, -2), origin.offset(2, 0, 2))) {
            if (p.distSqr(origin) < 1 || !StationProduction.safeSite(level, p)
                    || ConstructionSites.isProtected(level, p) || ConstructionSites.isProtected(level, p.below())
                    || !VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(p))
                    || !level.getEntities(null, new net.minecraft.world.phys.AABB(p)).isEmpty()
                    || v.getEyePosition().distanceToSqr(Vec3.atCenterOf(p)) > 9
                    || !BoatRoutes.visible(level, v, Vec3.atCenterOf(p))) continue;
            for (int i = 0; i < v.getInventory().getContainerSize(); i++) if (v.getInventory().getItem(i).is(Items.CRAFTING_TABLE)) {
                if (!level.setBlock(p, Blocks.CRAFTING_TABLE.defaultBlockState(), Block.UPDATE_ALL)) return false;
                v.getInventory().removeItem(i, 1); BreadCrafting.rememberTable(level,v,s,p);
                level.gameEvent(v, GameEvent.BLOCK_PLACE, p); return true;
            }
        }
        return false;
    }
    /** Exclude buildings: rooted upright unstripped trunks with a leaf crown and no nearby constructed blocks. */
    static boolean naturalTrunk(ServerLevel level, BlockPos base) {
        if (!level.isLoaded(base.below()) || !level.getWorldBorder().isWithinBounds(base)) return false;
        var soil = level.getBlockState(base.below());
        if (!(soil.is(BlockTags.DIRT) || soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.PODZOL)
                || soil.is(Blocks.MYCELIUM) || soil.is(Blocks.MOSS_BLOCK) || soil.is(Blocks.PALE_MOSS_BLOCK))) return false;
        var state = level.getBlockState(base);
        if (!state.is(BlockTags.LOGS) || !(state.getBlock() instanceof RotatedPillarBlock)
                || state.getValue(RotatedPillarBlock.AXIS) != net.minecraft.core.Direction.Axis.Y
                || !WoodCrafting.raw(state.getBlock().asItem().getDefaultInstance())
                || net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().startsWith("stripped_")) return false;
        int height = 0;
        while (height < 9 && level.isLoaded(base.above(height)) && level.getBlockState(base.above(height)).is(state.getBlock())) height++;
        if (height < 3 || height > 7) return false;
        boolean leaves = false;
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-2, height - 1, -2), base.offset(2, height + 1, 2))) {
            if (!level.isLoaded(p)) return false;
            if (level.getBlockState(p).is(BlockTags.LEAVES)
                    && !level.getBlockState(p).getValue(LeavesBlock.PERSISTENT)) leaves = true;
        }
        if (!leaves) return false;
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-3, -1, -3), base.offset(3, 3, 3))) {
            if (!level.isLoaded(p) || ConstructionSites.isProtected(level, p)) return false;
            var block = level.getBlockState(p);
            if (block.is(BlockTags.PLANKS) && p.distManhattan(base) <= 1 || block.getBlock() instanceof BedBlock || block.getBlock() instanceof DoorBlock
                    || level.getBlockEntity(p) != null) return false;
        }
        return true;
    }
    private static String gather(ServerLevel level, Villager v, Session s, long now) {
        var litter=LooseWood.collect(level,v,s,now);
        if(litter!=null)return s.boatStatus=litter;
        // Three logs bootstrap table+boat; a crop trip may pay one extra log for a real hoe.
        int quota = s.boatTrip != null && s.boatTrip.harvest ? 4 : 3;
        if (s.boatTree == null) {
            if (!WorkNavigation.admit(level, now, v)) return s.boatStatus = "looking for boat wood";
            var origin = v.blockPosition(); var trees = new ArrayList<BlockPos>();
            // A depleted village edge can leave the next intact tree beyond the old twelve-block scan.
            // Keep the ordinary home limit and require an actual walking approach before touching wood.
            for (BlockPos p : BlockPos.betweenClosed(origin.offset(-28, -3, -28), origin.offset(28, 3, 28)))
                if (naturalTrunk(level, p) && woodInReachOfHome(level, s, p)) trees.add(p.immutable());
            trees.sort(Comparator.comparingDouble(p -> p.distSqr(origin)));
            if (trees.isEmpty()) { s.nextBoatWork = now + 200; return s.boatStatus = "needs accessible natural boat wood"; }
            s.boatTree = trees.getFirst(); s.boatLogsTaken = 0;
            s.boatTreeBlock = level.getBlockState(s.boatTree).getBlock();
        }
        var p = s.boatTree.above(s.boatLogsTaken); var state = level.getBlockState(p);
        if (s.boatLogsTaken >= quota || !state.is(s.boatTreeBlock) || ConstructionSites.isProtected(level, p)
                || !level.getWorldBorder().isWithinBounds(p)) {
            s.boatTree = null; s.nextBoatWork = now + 100; return s.boatStatus = "rechecking boat wood";
        }
        if (v.getEyePosition().distanceToSqr(Vec3.atCenterOf(p)) > 9
                || !BoatRoutes.visible(level, v, Vec3.atCenterOf(p).add(0, .01, 0))) {
            if (now >= s.nextBoatPath && WorkNavigation.admit(level, now, v)) {
                s.nextBoatPath = now + 40;
                var path = WoodBootstrap.approachPath(level,v,s,p);
                if (path != null && path.canReach() && WorkNavigation.staysHome(level, v, s, path)) v.getNavigation().moveTo(path, .6);
                else { s.boatTree = null; s.nextBoatWork = now + 200; }
            }
            return s.boatStatus = "walking to a natural tree for boat wood";
        }
        v.getNavigation().stop();
        // Native log loot with the actual tool; capacity is checked before touching the real tree.
        var drops = Block.getDrops(state, level, p, null, v, v.getMainHandItem());
        var capacity = new net.minecraft.world.SimpleContainer(v.getInventory().getContainerSize());
        for (int i = 0; i < capacity.getContainerSize(); i++) capacity.setItem(i, v.getInventory().getItem(i).copy());
        if (drops.isEmpty() || drops.stream().anyMatch(a -> !capacity.addItem(a.copy()).isEmpty())) {
            s.nextBoatWork = now + 200; return s.boatStatus = "no room for boat wood";
        }
        // Two seconds of bare-hand work for each native log, reset on approach/interruption.
        if (s.boatLogWork == 0) s.boatLogWork = now;
        if (now - s.boatLogWork < 40) return s.boatStatus = "gathering real boat wood";
        if (!level.destroyBlock(p, false, v)) return null;
        for (var drop : drops) v.getInventory().addItem(drop);
        s.boatLogsTaken++; s.boatLogsGathered++; s.boatLogWork = 0;
        if (s.boatLogsTaken == quota) s.boatTree = null;
        s.nextBoatWork = now + 10; return s.boatStatus = "gathered a natural log for boat supplies";
    }
    private static boolean woodInReachOfHome(ServerLevel level, Session s, BlockPos p) {
        if (VillageSurvival.withinHomeRange(level, s.mind, Vec3.atCenterOf(p))) return true;
        var home=s.mind.home();
        return home!=null && home.dimension().equals(level.dimension().identifier().toString())
            && local.villagers.survival.SurvivalPolicy.withinHomeInteractionRange(
                Vec3.atCenterOf(new BlockPos(home.x(),home.y(),home.z())).distanceTo(Vec3.atCenterOf(p)),3);
    }
    private BoatPreparation() {}
}
