package local.villagers;

import java.util.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingInput;

/** Real portable string/wool and wood intermediates; a bed still needs a native workbench recipe. */
final class BedCrafting {
    record Plan(List<ItemStack> before,SimpleContainer remaining,CraftingInput input,Item output,int count,boolean table){}
    static Plan next(Container bag){
        var before=HarvestMaterials.snapshot(bag);var remaining=new SimpleContainer(bag.getContainerSize());
        int planks=0;
        for(int i=0;i<bag.getContainerSize();i++){
            remaining.setItem(i,bag.getItem(i).copy());if(WoodCrafting.plank(bag.getItem(i)))planks+=bag.getItem(i).getCount();
        }
        if(planks<3){
            var wood=WoodCrafting.table(bag);
            if(wood!=null&&wood.step()==WoodCrafting.Step.PLANKS)
                return new Plan(wood.before(),wood.remaining(),wood.input(),wood.output(),wood.count(),false);
        }
        if(bag.countItem(Items.WOOL.white())<3){
            if(bag.countItem(Items.STRING)<4)return null;
            var input=new ArrayList<ItemStack>();for(int i=0;i<4;i++)input.add(take(remaining,s->s.is(Items.STRING)));
            return new Plan(before,remaining,CraftingInput.of(2,2,input),Items.WOOL.white(),1,false);
        }
        if(planks<3)return null;
        var input=new ArrayList<ItemStack>();
        for(int i=0;i<3;i++)input.add(take(remaining,s->s.is(Items.WOOL.white())));
        for(int i=0;i<3;i++)input.add(take(remaining,WoodCrafting::plank));
        return new Plan(before,remaining,CraftingInput.of(3,2,input),Items.BED.white(),1,true);
    }
    private static ItemStack take(Container bag,java.util.function.Predicate<ItemStack> test){
        for(int i=0;i<bag.getContainerSize();i++)if(test.test(bag.getItem(i)))return bag.removeItem(i,1);
        throw new IllegalStateException("Missing reserved bed material");
    }
    static boolean commit(Container bag,Plan plan,ItemStack output){
        if(plan==null||!output.is(plan.output)||output.getCount()!=plan.count||!HarvestMaterials.unchanged(bag,plan.before))return false;
        var next=new SimpleContainer(bag.getContainerSize());
        for(int i=0;i<next.getContainerSize();i++)next.setItem(i,plan.remaining.getItem(i).copy());
        if(!next.addItem(output.copy()).isEmpty())return false;
        for(int i=0;i<next.getContainerSize();i++)bag.setItem(i,next.getItem(i));
        return true;
    }
    private BedCrafting(){}
}
