package local.villagers;

import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Detached, capacity-checked native dirt income or one real dirt payment plus actual displaced sand. */
final class SoilMaterials {
    static final class Plan {
        final List<ItemStack> before;
        final SimpleContainer after;
        private boolean used;
        Plan(Container bag) {
            before=HarvestMaterials.snapshot(bag);after=new SimpleContainer(bag.getContainerSize());
            for(int i=0;i<before.size();i++)after.setItem(i,before.get(i).copy());
        }
    }
    private SoilMaterials() {}
    static Plan gather(Container bag,List<ItemStack> drops) {
        if(drops.size()!=1 || !drops.getFirst().is(Items.DIRT) || drops.getFirst().getCount()!=1)return null;
        var plan=new Plan(bag);return plan.after.addItem(drops.getFirst().copy()).isEmpty()?plan:null;
    }
    static Plan replace(Container bag,List<ItemStack> drops) {
        if(drops.size()!=1 || !drops.getFirst().is(Items.SAND) || drops.getFirst().getCount()!=1)return null;
        var plan=new Plan(bag);boolean paid=false;
        for(int i=0;i<plan.after.getContainerSize();i++)if(plan.after.getItem(i).is(Items.DIRT)) {
            plan.after.removeItem(i,1);paid=true;break;
        }
        return paid && plan.after.addItem(drops.getFirst().copy()).isEmpty()?plan:null;
    }
    /** One real dirt payment for an approved AIR cell; no displaced loot or synthetic income. */
    static Plan placeDirt(Container bag) {
        var plan=new Plan(bag);
        for(int i=0;i<plan.after.getContainerSize();i++)if(plan.after.getItem(i).is(Items.DIRT)) {
            plan.after.removeItem(i,1);return plan;
        }
        return null;
    }
    static boolean unchanged(Container bag,Plan plan) { return plan!=null && !plan.used && HarvestMaterials.unchanged(bag,plan.before); }
    static boolean commit(Container bag,Plan plan) {
        if(!unchanged(bag,plan))return false;
        plan.used=true;
        for(int i=0;i<bag.getContainerSize();i++)bag.setItem(i,plan.after.getItem(i).copy());
        return true;
    }
}
