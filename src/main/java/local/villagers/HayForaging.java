package local.villagers;

import java.util.ArrayList;
import java.util.List;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Finite grain bootstrap. Only visible exposed hay is eligible; no general terrain permission. */
final class HayForaging {
    private HayForaging() {}
    record Reservation(List<ItemStack> before, SimpleContainer remaining, CraftingInput input) {}

    static boolean hasHay(Container inventory) {
        for (int i = 0; i < inventory.getContainerSize(); i++)
            if (inventory.getItem(i).is(Items.HAY_BLOCK)) return true;
        return false;
    }

    static Reservation reserve(Container inventory) {
        int hay = -1;
        for (int i = 0; i < inventory.getContainerSize(); i++)
            if (inventory.getItem(i).is(Items.HAY_BLOCK)) { hay = i; break; }
        if (hay < 0) return null;
        List<ItemStack> before = new ArrayList<>();
        SimpleContainer remaining = new SimpleContainer(inventory.getContainerSize());
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            before.add(inventory.getItem(i).copy());
            remaining.setItem(i, inventory.getItem(i).copy());
        }
        return new Reservation(List.copyOf(before), remaining,
                CraftingInput.of(1, 1, List.of(remaining.removeItem(hay, 1))));
    }

    /** The one-hay recipe is deliberately fixed to nine wheat and no supported byproducts. */
    static boolean commit(Container inventory, Reservation plan, ItemStack output) {
        if (plan == null || !output.is(Items.WHEAT) || output.getCount() != 9
                || inventory.getContainerSize() != plan.before().size()) return false;
        SimpleContainer next = new SimpleContainer(inventory.getContainerSize());
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (!ItemStack.matches(inventory.getItem(i), plan.before().get(i))) return false;
            next.setItem(i, plan.remaining().getItem(i).copy());
        }
        if (!next.addItem(output.copy()).isEmpty()) return false;
        for (int i = 0; i < inventory.getContainerSize(); i++) inventory.setItem(i, next.getItem(i).copy());
        return true;
    }

    static boolean unpack(ServerLevel level, Villager v, Session s) {
        if (!AdventureActions.PERMISSIONS.allows(Capability.UNPACK_HAY) || !eligible(level, v)) return false;
        Reservation plan = reserve(v.getInventory());
        if (plan == null) return false;
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, plan.input(), level);
        if (recipe.isEmpty()) return false;
        var rule = recipe.get().value();
        ItemStack output = rule.assemble(plan.input());
        if (rule.getRemainingItems(plan.input()).stream().anyMatch(stack -> !stack.isEmpty())
                || !commit(v.getInventory(), plan, output)) return false;
        s.mind.remember("Unpacked 1 hay bale into 9 wheat");
        Villagers.persist(s);
        return true;
    }

    private static boolean eligible(ServerLevel level, Villager v) {
        return Villagers.isManaged(v) && v.isAlive() && v.level() == level && !v.isSleeping() && !v.isTrading();
    }

    private static boolean visible(ServerLevel level, Villager v, BlockPos pos) {
        var hit = level.clip(new ClipContext(v.getEyePosition(), Vec3.atCenterOf(pos),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, v));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    private static boolean exposed(ServerLevel level, Villager v, Session s, BlockPos pos) {
        return !ConstructionSites.isProtected(level, pos) && level.isLoaded(pos) && level.isLoaded(pos.above()) && level.getBlockState(pos).is(Blocks.HAY_BLOCK)
                && level.getBlockState(pos.above()).isAir()
                && !v.blockPosition().below().equals(pos)
                && !v.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(pos))
                && VillageSurvival.withinHomeRange(level, s.mind, Vec3.atCenterOf(pos)) && visible(level, v, pos);
    }

    /** At most 405 positions per five simulation seconds, only when grain is needed and a table is known. */
    static BlockPos target(ServerLevel level, Villager v, Session s) {
        if (!Villagers.config().hayForaging() || !AdventureActions.PERMISSIONS.allows(Capability.GATHER_HAY)
                || !eligible(level, v)) return null;
        if (s.hayTarget != null && (v.blockPosition().distSqr(s.hayTarget) > 36 || !exposed(level, v, s, s.hayTarget)))
            s.hayTarget = null;
        long now = level.getGameTime();
        if (now < s.nextHayScan) return s.hayTarget;
        s.nextHayScan = now + 100;
        BlockPos origin = v.blockPosition(), best = null;
        double distance = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-4, -2, -4), origin.offset(4, 2, 4))) {
            if (!exposed(level, v, s, p)) continue;
            double next = v.position().distanceToSqr(Vec3.atCenterOf(p));
            if (next < distance) { best = p.immutable(); distance = next; }
        }
        return s.hayTarget = best;
    }

    /** Consecutive work at reach, then native loot and one exact block removal on the server thread. */
    static boolean gather(ServerLevel level, Villager v, Session s, BlockPos pos, long now) {
        if (!Villagers.config().hayForaging() || !AdventureActions.PERMISSIONS.allows(Capability.GATHER_HAY) || !eligible(level, v)
                || !exposed(level, v, s, pos) || v.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > 9
                || !v.getInventory().canAddItem(new ItemStack(Items.HAY_BLOCK))) { s.workingHay = null; return false; }
        v.getNavigation().stop();
        v.swingForAttack(InteractionHand.MAIN_HAND);
        if (!pos.equals(s.workingHay) || now < s.lastHayWork || now - s.lastHayWork > 20) {
            s.workingHay = pos; s.hayWorkStarted = now;
        }
        s.lastHayWork = now;
        if (now - s.hayWorkStarted < 20) return false;
        var original = level.getBlockState(pos);
        // Bare-hand loot prevents the capability from silently granting an equipped tool's special drops.
        var drops = Block.getDrops(original, level, pos, null, v, ItemStack.EMPTY);
        if (!exposed(level, v, s, pos) || !level.getBlockState(pos).equals(original)
                || !level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) return false;
        for (ItemStack drop : drops) {
            ItemStack overflow = v.getInventory().addItem(drop);
            if (!overflow.isEmpty()) Block.popResource(level, pos, overflow);
        }
        level.levelEvent(2001, pos, Block.getId(original));
        level.gameEvent(v, GameEvent.BLOCK_DESTROY, pos);
        s.mind.exert(.005f); // Player block-breaking exhaustion.
        s.mind.remember("Gathered existing hay at " + pos.toShortString());
        s.hayTarget = null; s.workingHay = null; s.nextHayScan = now + 100;
        Villagers.persist(s);
        return true;
    }
}
