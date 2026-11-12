package local.villagers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeCampFoodBatchTest {
    private static ItemStack potatoes(int count){
        var stack=new ItemStack(Items.POTATO,count);
        stack.set(DataComponents.FOOD,new FoodProperties.Builder().nutrition(1).saturationModifier(.6f).build());return stack;
    }
    @Test void oneInteractionCollectsOnlyTheFourRealRootsAcrossThreeSlots(){
        var source=new SimpleContainer(4);source.setItem(0,potatoes(1));source.setItem(1,potatoes(1));source.setItem(3,potatoes(2));
        var bag=new SimpleContainer(5);
        assertEquals(4,NativeCampWork.takeFoodBatch(source,bag,potatoes(1),8));
        assertEquals(0,source.countItem(Items.POTATO));assertEquals(4,bag.countItem(Items.POTATO));
        assertEquals(0,NativeCampWork.takeFoodBatch(source,bag,potatoes(1),8));assertEquals(4,bag.countItem(Items.POTATO));
    }
    @Test void carryingLimitAndPartialCapacityLeaveTheRemainderInTheRealSource(){
        var source=new SimpleContainer(2);source.setItem(0,potatoes(2));source.setItem(1,potatoes(2));
        var bag=new SimpleContainer(1);bag.setItem(0,potatoes(63));
        assertEquals(1,NativeCampWork.takeFoodBatch(source,bag,potatoes(1),3));
        assertEquals(64,bag.countItem(Items.POTATO));assertEquals(3,source.countItem(Items.POTATO));
        assertEquals(0,NativeCampWork.takeFoodBatch(source,bag,potatoes(1),3));assertEquals(3,source.countItem(Items.POTATO));
        var other=new SimpleContainer(1);assertEquals(2,NativeCampWork.takeFoodBatch(source,other,potatoes(1),2));
        assertEquals(2,other.countItem(Items.POTATO));assertEquals(1,source.countItem(Items.POTATO));
    }
}
