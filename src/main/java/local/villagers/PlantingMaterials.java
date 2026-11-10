package local.villagers;

import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** One actual planting item, retaining at least one of that material for existing-field replanting. */
final class PlantingMaterials {
    record Plan(int slot, ItemStack before) {}
    private PlantingMaterials() {}
    static boolean supported(ItemStack s) { return s.is(Items.WHEAT_SEEDS) || s.is(Items.BEETROOT_SEEDS)
            || s.is(Items.CARROT) || s.is(Items.POTATO); }
    static Plan reserve(Container inventory) {
        return reserve(inventory, item -> 0);
    }
    /** Rank only possessed, spendable seeds. Ties retain inventory order; the final seed is never eligible. */
    static Plan reserve(Container inventory, java.util.function.ToIntFunction<Item> preference) {
        Plan best=null;int bestScore=Integer.MIN_VALUE;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (supported(s) && inventory.countItem(s.getItem()) > 1) {
                int score=preference.applyAsInt(s.getItem());
                if(best==null || score>bestScore) {best=new Plan(i,s.copy());bestScore=score;}
            }
        }
        return best;
    }
    static boolean unchanged(Container inventory, Plan p) {
        return p != null && p.slot() >= 0 && p.slot() < inventory.getContainerSize()
                && ItemStack.matches(inventory.getItem(p.slot()), p.before())
                && inventory.countItem(p.before().getItem()) > 1;
    }
    static boolean commit(Container inventory, Plan p) {
        if (!unchanged(inventory, p)) return false;
        return !inventory.removeItem(p.slot(), 1).isEmpty();
    }
}
