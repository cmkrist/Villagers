package local.villagers;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** One real crafted/owned table, consumed only after a successful checked placement. */
final class StationMaterials {
    record Plan(int slot, ItemStack before) {}
    private StationMaterials() {}
    static Plan reserve(Container inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
            if (inventory.getItem(slot).is(Items.CRAFTING_TABLE)) return new Plan(slot, inventory.getItem(slot).copy());
        return null;
    }
    static boolean unchanged(Container inventory, Plan plan) {
        return plan != null && plan.slot() >= 0 && plan.slot() < inventory.getContainerSize()
                && ItemStack.matches(inventory.getItem(plan.slot()), plan.before());
    }
    static boolean commit(Container inventory, Plan plan) {
        return unchanged(inventory, plan) && !inventory.removeItem(plan.slot(), 1).isEmpty();
    }
}
