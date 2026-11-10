package local.villagers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;

/** Finite bootstrap recipes from actual held wood. The server still decides whether each recipe exists. */
final class WoodCrafting {
    enum Step { PLANKS, STICKS, HOE, TABLE, PICKAXE, BOAT, STONE_PICKAXE }
    record Plan(Step step, List<ItemStack> before, SimpleContainer remaining,
                CraftingInput input, Item output, int count) {}
    private static final Map<Item, Item> PLANKS = Map.ofEntries(
            Map.entry(Items.POPLAR_LOG, Items.POPLAR_PLANKS), Map.entry(Items.POPLAR_WOOD, Items.POPLAR_PLANKS),
            Map.entry(Items.STRIPPED_POPLAR_LOG, Items.POPLAR_PLANKS), Map.entry(Items.STRIPPED_POPLAR_WOOD, Items.POPLAR_PLANKS),
            Map.entry(Items.OAK_LOG, Items.OAK_PLANKS), Map.entry(Items.OAK_WOOD, Items.OAK_PLANKS),
            Map.entry(Items.STRIPPED_OAK_LOG, Items.OAK_PLANKS), Map.entry(Items.STRIPPED_OAK_WOOD, Items.OAK_PLANKS),
            Map.entry(Items.SPRUCE_LOG, Items.SPRUCE_PLANKS), Map.entry(Items.SPRUCE_WOOD, Items.SPRUCE_PLANKS),
            Map.entry(Items.STRIPPED_SPRUCE_LOG, Items.SPRUCE_PLANKS), Map.entry(Items.STRIPPED_SPRUCE_WOOD, Items.SPRUCE_PLANKS),
            Map.entry(Items.BIRCH_LOG, Items.BIRCH_PLANKS), Map.entry(Items.BIRCH_WOOD, Items.BIRCH_PLANKS),
            Map.entry(Items.STRIPPED_BIRCH_LOG, Items.BIRCH_PLANKS), Map.entry(Items.STRIPPED_BIRCH_WOOD, Items.BIRCH_PLANKS),
            Map.entry(Items.JUNGLE_LOG, Items.JUNGLE_PLANKS), Map.entry(Items.JUNGLE_WOOD, Items.JUNGLE_PLANKS),
            Map.entry(Items.STRIPPED_JUNGLE_LOG, Items.JUNGLE_PLANKS), Map.entry(Items.STRIPPED_JUNGLE_WOOD, Items.JUNGLE_PLANKS),
            Map.entry(Items.ACACIA_LOG, Items.ACACIA_PLANKS), Map.entry(Items.ACACIA_WOOD, Items.ACACIA_PLANKS),
            Map.entry(Items.STRIPPED_ACACIA_LOG, Items.ACACIA_PLANKS), Map.entry(Items.STRIPPED_ACACIA_WOOD, Items.ACACIA_PLANKS),
            Map.entry(Items.DARK_OAK_LOG, Items.DARK_OAK_PLANKS), Map.entry(Items.DARK_OAK_WOOD, Items.DARK_OAK_PLANKS),
            Map.entry(Items.STRIPPED_DARK_OAK_LOG, Items.DARK_OAK_PLANKS), Map.entry(Items.STRIPPED_DARK_OAK_WOOD, Items.DARK_OAK_PLANKS),
            Map.entry(Items.MANGROVE_LOG, Items.MANGROVE_PLANKS), Map.entry(Items.MANGROVE_WOOD, Items.MANGROVE_PLANKS),
            Map.entry(Items.STRIPPED_MANGROVE_LOG, Items.MANGROVE_PLANKS), Map.entry(Items.STRIPPED_MANGROVE_WOOD, Items.MANGROVE_PLANKS),
            Map.entry(Items.CHERRY_LOG, Items.CHERRY_PLANKS), Map.entry(Items.CHERRY_WOOD, Items.CHERRY_PLANKS),
            Map.entry(Items.STRIPPED_CHERRY_LOG, Items.CHERRY_PLANKS), Map.entry(Items.STRIPPED_CHERRY_WOOD, Items.CHERRY_PLANKS),
            Map.entry(Items.PALE_OAK_LOG, Items.PALE_OAK_PLANKS), Map.entry(Items.PALE_OAK_WOOD, Items.PALE_OAK_PLANKS),
            Map.entry(Items.STRIPPED_PALE_OAK_LOG, Items.PALE_OAK_PLANKS), Map.entry(Items.STRIPPED_PALE_OAK_WOOD, Items.PALE_OAK_PLANKS),
            Map.entry(Items.CRIMSON_STEM, Items.CRIMSON_PLANKS), Map.entry(Items.CRIMSON_HYPHAE, Items.CRIMSON_PLANKS),
            Map.entry(Items.STRIPPED_CRIMSON_STEM, Items.CRIMSON_PLANKS), Map.entry(Items.STRIPPED_CRIMSON_HYPHAE, Items.CRIMSON_PLANKS),
            Map.entry(Items.WARPED_STEM, Items.WARPED_PLANKS), Map.entry(Items.WARPED_HYPHAE, Items.WARPED_PLANKS),
            Map.entry(Items.STRIPPED_WARPED_STEM, Items.WARPED_PLANKS), Map.entry(Items.STRIPPED_WARPED_HYPHAE, Items.WARPED_PLANKS));
    private WoodCrafting() {}
    private static final java.util.Set<Item> PLANK_TYPES = java.util.Set.copyOf(PLANKS.values());
    static boolean plank(ItemStack stack) { return !stack.isEmpty() && PLANK_TYPES.contains(stack.getItem()); }
    static boolean raw(ItemStack stack) { return !stack.isEmpty() && PLANKS.containsKey(stack.getItem()); }
    static boolean material(ItemStack stack) {
        return !stack.isEmpty() && (PLANKS.containsKey(stack.getItem()) || plank(stack) || stack.is(Items.STICK));
    }
    /** No terrain search: a caller must already possess enough supported ingredients for one hoe. */
    static Plan next(Container inventory) {
        return next(inventory, false);
    }
    static Plan pickaxe(Container inventory) { return next(inventory, true); }
    static Plan stonePickaxe(Container inventory) { return next(inventory,true,true); }
    static boolean stoneMaterial(ItemStack stack) {
        return stack.is(Items.COBBLESTONE) || stack.is(Items.COBBLED_DEEPSLATE) || stack.is(Items.BLACKSTONE);
    }
    static int stoneCount(Container inventory) {
        int result=0;
        for(int i=0;i<inventory.getContainerSize();i++) if(stoneMaterial(inventory.getItem(i)))result+=inventory.getItem(i).getCount();
        return result;
    }
    /** Boats require five planks of one species; mixed planks and Nether wood cannot substitute. */
    static Plan boat(Container inventory) {
        Item wood = null;
        for (var entry : BoatMaterials.TYPES.entrySet()) {
            int units = inventory.countItem(entry.getKey());
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                var stack = inventory.getItem(i);
                if (!stack.isEmpty() && PLANKS.get(stack.getItem()) == entry.getKey()) units += 4 * stack.getCount();
            }
            if (units >= 5) { wood = entry.getKey(); break; }
        }
        if (wood == null) return null;
        var before = new ArrayList<ItemStack>();
        var remaining = new SimpleContainer(inventory.getContainerSize());
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            before.add(inventory.getItem(i).copy()); remaining.setItem(i, inventory.getItem(i).copy());
        }
        if (inventory.countItem(wood) < 5) {
            for (int i = 0; i < remaining.getContainerSize(); i++) {
                var stack = remaining.getItem(i);
                if (!stack.isEmpty() && PLANKS.get(stack.getItem()) == wood)
                    return new Plan(Step.PLANKS, List.copyOf(before), remaining,
                        CraftingInput.of(1, 1, List.of(remaining.removeItem(i, 1))), wood, 4);
            }
        }
        var input = new ArrayList<ItemStack>();
        for (int cell = 0; cell < 6; cell++) {
            if (cell == 1) { input.add(ItemStack.EMPTY); continue; }
            for (int i = 0; i < remaining.getContainerSize(); i++) if (remaining.getItem(i).is(wood)) {
                input.add(remaining.removeItem(i, 1)); break;
            }
        }
        return new Plan(Step.BOAT, List.copyOf(before), remaining, CraftingInput.of(3, 2, input), BoatMaterials.TYPES.get(wood).item(), 1);
    }
    private static Plan next(Container inventory, boolean pickaxe) {
        return next(inventory,pickaxe,false);
    }
    private static Plan next(Container inventory, boolean pickaxe, boolean stone) {
        if (stone && stoneCount(inventory)<3) return null;
        int planks = 0, sticks = 0, logSlot = -1;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (plank(stack)) planks += stack.getCount();
            else if (stack.is(Items.STICK)) sticks += stack.getCount();
            else if (logSlot < 0 && !stack.isEmpty() && PLANKS.containsKey(stack.getItem())) logSlot = slot;
        }
        int neededPlanks = (stone ? 0 : pickaxe ? 3 : 2) + (sticks >= 2 ? 0 : 2);
        if (planks < neededPlanks && logSlot < 0) return null;
        var before = new ArrayList<ItemStack>();
        var remaining = new SimpleContainer(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            before.add(inventory.getItem(slot).copy()); remaining.setItem(slot, inventory.getItem(slot).copy());
        }
        if (planks < neededPlanks) {
            var log = remaining.removeItem(logSlot, 1);
            return new Plan(Step.PLANKS, List.copyOf(before), remaining,
                    CraftingInput.of(1, 1, List.of(log)), PLANKS.get(log.getItem()), 4);
        }
        if (sticks < 2) return new Plan(Step.STICKS, List.copyOf(before), remaining,
                CraftingInput.of(1, 2, List.of(take(remaining,false), take(remaining,false))), Items.STICK, 4);
        if (stone) return new Plan(Step.STONE_PICKAXE,List.copyOf(before),remaining,
            CraftingInput.of(3,3,List.of(takeStone(remaining),takeStone(remaining),takeStone(remaining),ItemStack.EMPTY,
                take(remaining,true),ItemStack.EMPTY,ItemStack.EMPTY,take(remaining,true),ItemStack.EMPTY)),Items.STONE_PICKAXE,1);
        ItemStack a = take(remaining, false), b = take(remaining, false);
        if (pickaxe) return new Plan(Step.PICKAXE, List.copyOf(before), remaining,
            CraftingInput.of(3, 3, List.of(a, b, take(remaining, false), ItemStack.EMPTY,
                take(remaining, true), ItemStack.EMPTY, ItemStack.EMPTY, take(remaining, true), ItemStack.EMPTY)),
            Items.WOODEN_PICKAXE, 1);
        return new Plan(Step.HOE, List.copyOf(before), remaining,
                CraftingInput.of(2, 3, List.of(a, b, ItemStack.EMPTY, take(remaining, true),
                        ItemStack.EMPTY, take(remaining, true))), Items.WOODEN_HOE, 1);
    }
    private static ItemStack takeStone(SimpleContainer inventory) {
        for(int i=0;i<inventory.getContainerSize();i++)if(stoneMaterial(inventory.getItem(i)))return inventory.removeItem(i,1);
        throw new IllegalStateException("Missing reserved stone");
    }
    private static ItemStack take(SimpleContainer inventory, boolean stick) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
            if (stick ? inventory.getItem(slot).is(Items.STICK) : plank(inventory.getItem(slot)))
                return inventory.removeItem(slot, 1);
        throw new IllegalStateException("Missing reserved material");
    }
    /** Cheap intent check: no reservations, item copies or recipe/world lookups for idle nonbuilders. */
    static boolean canMakeTable(Container inventory) {
        int planks=0;
        for(int i=0;i<inventory.getContainerSize();i++) {
            var stack=inventory.getItem(i);
            if(!stack.isEmpty() && PLANKS.containsKey(stack.getItem()))return true;
            if(plank(stack)) {
                if(stack.getCount()>=4-planks)return true;
                planks+=stack.getCount();
            }
        }
        return false;
    }
    /** Portable 2x2 table bootstrap; the table is an actual intermediate item before placement. */
    static Plan table(Container inventory) {
        int planks = 0, logSlot = -1;
        var before = new ArrayList<ItemStack>(); var remaining = new SimpleContainer(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (plank(stack)) planks += stack.getCount();
            else if (logSlot < 0 && !stack.isEmpty() && PLANKS.containsKey(stack.getItem())) logSlot = slot;
            before.add(stack.copy()); remaining.setItem(slot, stack.copy());
        }
        if (planks < 4) {
            if (logSlot < 0) return null;
            var log = remaining.removeItem(logSlot, 1);
            return new Plan(Step.PLANKS, List.copyOf(before), remaining,
                    CraftingInput.of(1, 1, List.of(log)), PLANKS.get(log.getItem()), 4);
        }
        return new Plan(Step.TABLE, List.copyOf(before), remaining,
                CraftingInput.of(2, 2, List.of(take(remaining, false), take(remaining, false),
                        take(remaining, false), take(remaining, false))), Items.CRAFTING_TABLE, 1);
    }
    /** One exact native output, no overflow, stale plans or partially consumed live ingredients. */
    static boolean commit(Container inventory, Plan plan, ItemStack output) {
        if (plan == null || !output.is(plan.output()) || output.getCount() != plan.count()
                || inventory.getContainerSize() != plan.before().size()) return false;
        var next = new SimpleContainer(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (!ItemStack.matches(inventory.getItem(slot), plan.before().get(slot))) return false;
            next.setItem(slot, plan.remaining().getItem(slot).copy());
        }
        if (!next.addItem(output.copy()).isEmpty()) return false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) inventory.setItem(slot, next.getItem(slot).copy());
        return true;
    }
}
