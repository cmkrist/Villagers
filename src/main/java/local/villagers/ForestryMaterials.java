package local.villagers;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Exact native resource receipt and one real retained propagule. No overflow or invented loot. */
final class ForestryMaterials {
    record Receipt(List<ItemStack> before, SimpleContainer after) {}
    record Seed(int slot, ItemStack before) {}
    private ForestryMaterials() {}
    static Seed seed(Container inventory) {
        for (int slot=0;slot<inventory.getContainerSize();slot++)
            if (inventory.getItem(slot).is(Items.MANGROVE_PROPAGULE)) return new Seed(slot,inventory.getItem(slot).copy());
        return null;
    }
    static boolean unchanged(Container inventory, Seed seed) {
        return seed != null && seed.slot()>=0 && seed.slot()<inventory.getContainerSize()
                && ItemStack.matches(inventory.getItem(seed.slot()),seed.before());
    }
    static boolean plant(Container inventory, Seed seed) {
        return unchanged(inventory,seed) && !inventory.removeItem(seed.slot(),1).isEmpty();
    }
    static Receipt receive(Container inventory,List<ItemStack> drops,Item expected) {
        if (drops.size()!=1 || !drops.getFirst().is(expected) || drops.getFirst().getCount()!=1) return null;
        return receipt(inventory,drops);
    }
    static Receipt leaves(Container inventory,List<ItemStack> drops) {
        if (drops.stream().anyMatch(s -> !s.is(Items.STICK) || s.getCount()<1 || s.getCount()>2)
                || drops.stream().mapToInt(ItemStack::getCount).sum()>2) return null;
        return receipt(inventory,drops);
    }
    private static Receipt receipt(Container inventory,List<ItemStack> drops) {
        var before=new ArrayList<ItemStack>();var after=new SimpleContainer(inventory.getContainerSize());
        for (int i=0;i<inventory.getContainerSize();i++) { before.add(inventory.getItem(i).copy());after.setItem(i,inventory.getItem(i).copy()); }
        for(var drop:drops) if (!after.addItem(drop.copy()).isEmpty()) return null;
        return new Receipt(List.copyOf(before),after);
    }
    static boolean unchanged(Container inventory,Receipt plan) {
        if (plan==null || inventory.getContainerSize()!=plan.before().size()) return false;
        for (int i=0;i<inventory.getContainerSize();i++) if (!ItemStack.matches(inventory.getItem(i),plan.before().get(i))) return false;
        return true;
    }
    static boolean commit(Container inventory,Receipt plan) {
        if (!unchanged(inventory,plan)) return false;
        for (int i=0;i<inventory.getContainerSize();i++) inventory.setItem(i,plan.after().getItem(i).copy());
        return true;
    }
}
