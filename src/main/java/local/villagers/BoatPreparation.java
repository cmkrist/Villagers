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
        var result=WoodBootstrap.work(level,v,s,needsHoe?WoodBootstrap.Goal.HOE:WoodBootstrap.Goal.BOAT,now);
        s.boatStatus=s.woodBootstrap.status;
        // A bounded preparation job owns its search pauses too. Otherwise
        // the mining controller can walk back underground between surveys.
        return result!=null?result:WoodBootstrap.safe(level,v,s)?s.boatStatus:null;
    }
    static boolean craft(ServerLevel level, Villager v, Session s, WoodCrafting.Plan plan, BlockPos table) {
        if (!AdventureActions.PERMISSIONS.allows(local.villagers.capability.CapabilityPermissions.Capability.CRAFT_BOAT)
                || !Villagers.isManaged(v) || !v.isAlive() || v.isBaby() || v.isTrading() || v.isSleeping() || v.isInWater()
                || plan == null || plan.step() == WoodCrafting.Step.PICKAXE || plan.step() == WoodCrafting.Step.STONE_PICKAXE || plan.step() == WoodCrafting.Step.IRON_PICKAXE || plan.step() == WoodCrafting.Step.DIAMOND_PICKAXE
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
    /** Upright unstripped trunks or their remaining upper segments, with a natural leaf crown and no nearby building. */
    static boolean naturalTrunk(ServerLevel level, BlockPos base) {
        if (!level.isLoaded(base.below()) || !level.getWorldBorder().isWithinBounds(base)) return false;
        var ground=base.below();int gap=0;
        while(gap<5&&level.isLoaded(ground)&&level.getBlockState(ground).isAir()){ground=ground.below();gap++;}
        if(!level.isLoaded(ground))return false;var soil = level.getBlockState(ground);
        if (!(soil.is(BlockTags.DIRT) || soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.PODZOL)
                || soil.is(Blocks.MYCELIUM) || soil.is(Blocks.MOSS_BLOCK) || soil.is(Blocks.PALE_MOSS_BLOCK)
                ||soil.is(Blocks.SAND)||soil.is(Blocks.RED_SAND))) return false;
        var state = level.getBlockState(base);
        if (!state.is(BlockTags.LOGS) || !(state.getBlock() instanceof RotatedPillarBlock)
                || state.getValue(RotatedPillarBlock.AXIS) != net.minecraft.core.Direction.Axis.Y
                || !WoodCrafting.raw(state.getBlock().asItem().getDefaultInstance())
                || net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().startsWith("stripped_")) return false;
        int height = 0;
        while (height < 9 && level.isLoaded(base.above(height)) && level.getBlockState(base.above(height)).is(state.getBlock())) height++;
        if (height < (gap==0?3:1) || height > 7) return false;
        boolean leaves = false;
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-2, height - 1, -2), base.offset(2, height + 1, 2))) {
            if (!level.isLoaded(p)) return false;
            if (level.getBlockState(p).is(BlockTags.LEAVES)
                    && !level.getBlockState(p).getValue(LeavesBlock.PERSISTENT)) leaves = true;
        }
        if (!leaves) return false;
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(base.getX()-3,ground.getY(),base.getZ()-3),base.offset(3,3,3))) {
            if (!level.isLoaded(p) || ConstructionSites.isProtected(level, p)) return false;
            var block = level.getBlockState(p);
            if (block.is(BlockTags.PLANKS) && Math.abs(p.getX()-base.getX())+Math.abs(p.getZ()-base.getZ())<=1 || block.getBlock() instanceof BedBlock || block.getBlock() instanceof DoorBlock
                    || level.getBlockEntity(p) != null) return false;
        }
        return true;
    }
    static boolean woodInReachOfHome(ServerLevel level, Session s, BlockPos p) {
        if (VillageSurvival.withinHomeRange(level, s.mind, Vec3.atCenterOf(p))) return true;
        var home=s.mind.home();
        return home!=null && home.dimension().equals(level.dimension().identifier().toString())
            && local.villagers.survival.SurvivalPolicy.withinHomeInteractionRange(
                Vec3.atCenterOf(new BlockPos(home.x(),home.y(),home.z())).distanceTo(Vec3.atCenterOf(p)),3);
    }
    private BoatPreparation() {}
}
