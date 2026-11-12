package local.villagers;

import java.util.ArrayList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;

/** Reserve possessed inputs; the native recipe and output still decide whether crafting succeeds. */
final class IronCrafting {
    record Furnace(java.util.List<ItemStack> before,net.minecraft.world.SimpleContainer remaining,CraftingInput input){}
    static Furnace furnace(Container bag){
        if(WoodCrafting.stoneCount(bag)<8)return null;
        var before=HarvestMaterials.snapshot(bag);var remaining=new net.minecraft.world.SimpleContainer(bag.getContainerSize());
        for(int i=0;i<bag.getContainerSize();i++)remaining.setItem(i,bag.getItem(i).copy());
        var cells=new ArrayList<ItemStack>();
        for(int cell=0;cell<9;cell++){
            if(cell==4){cells.add(ItemStack.EMPTY);continue;}
            for(int slot=0;slot<remaining.getContainerSize();slot++)if(WoodCrafting.stoneMaterial(remaining.getItem(slot))){cells.add(remaining.removeItem(slot,1));break;}
        }
        return new Furnace(before,remaining,CraftingInput.of(3,3,cells));
    }
    static boolean commit(Container bag,Furnace plan,ItemStack result){
        if(plan==null||!result.is(Items.FURNACE)||result.getCount()!=1||!HarvestMaterials.unchanged(bag,plan.before()))return false;
        var next=new net.minecraft.world.SimpleContainer(bag.getContainerSize());
        for(int i=0;i<next.getContainerSize();i++)next.setItem(i,plan.remaining().getItem(i).copy());
        if(!next.addItem(result.copy()).isEmpty())return false;
        for(int i=0;i<next.getContainerSize();i++)bag.setItem(i,next.getItem(i));return true;
    }
    private IronCrafting(){}
}
