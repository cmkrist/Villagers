package local.villagers;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** One actual planting payment from owned stock or the same native harvest; no invented items. */
final class HarvestMaterials {
    static final class Plan {
        final List<ItemStack> before,overflow;
        final SimpleContainer after;
        final boolean fromDrops;
        final Item planting;
        private boolean used;
        Plan(List<ItemStack> before,SimpleContainer after,List<ItemStack> overflow,boolean fromDrops,Item planting) {
            this.before=before;this.after=after;this.overflow=overflow;this.fromDrops=fromDrops;this.planting=planting;
        }
    }
    private HarvestMaterials() {}
    static List<ItemStack> snapshot(Container inventory) {
        var result=new ArrayList<ItemStack>();
        for(int i=0;i<inventory.getContainerSize();i++)result.add(inventory.getItem(i).copy());
        return List.copyOf(result);
    }
    static boolean unchanged(Container inventory,List<ItemStack> before) {
        if(inventory.getContainerSize()!=before.size())return false;
        for(int i=0;i<before.size();i++)if(!ItemStack.matches(inventory.getItem(i),before.get(i)))return false;
        return true;
    }
    static Plan reserve(Container inventory,List<ItemStack> nativeDrops,Item planting) {
        if(planting!=Items.WHEAT_SEEDS && planting!=Items.BEETROOT_SEEDS && planting!=Items.CARROT && planting!=Items.POTATO)return null;
        var before=snapshot(inventory);var after=new SimpleContainer(inventory.getContainerSize());
        for(int i=0;i<before.size();i++)after.setItem(i,before.get(i).copy());
        var drops=nativeDrops.stream().map(ItemStack::copy).toList();boolean paid=false,fromDrops=false;
        for(int i=0;i<after.getContainerSize();i++)if(after.getItem(i).is(planting) && !after.getItem(i).isEmpty()) {
            after.removeItem(i,1);paid=true;break;
        }
        if(!paid)for(var drop:drops)if(drop.is(planting) && !drop.isEmpty()) {
            drop.shrink(1);paid=true;fromDrops=true;break;
        }
        if(!paid)return null;
        var overflow=new ArrayList<ItemStack>();
        for(var drop:drops)if(!drop.isEmpty()) {
            var left=after.addItem(drop.copy());if(!left.isEmpty())overflow.add(left);
        }
        return new Plan(before,after,List.copyOf(overflow),fromDrops,planting);
    }
    static boolean unchanged(Container inventory,Plan plan) { return plan!=null && !plan.used && unchanged(inventory,plan.before); }
    static boolean commit(Container inventory,Plan plan) {
        if(!unchanged(inventory,plan))return false;
        // A full bag can leave inventory unchanged while emitting actual overflow: still one-use.
        plan.used=true;
        for(int i=0;i<inventory.getContainerSize();i++)inventory.setItem(i,plan.after.getItem(i).copy());
        return true;
    }
}
