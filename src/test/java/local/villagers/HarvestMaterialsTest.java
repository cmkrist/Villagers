package local.villagers;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HarvestMaterialsTest {
    @BeforeAll static void items() {
        WoodCraftingTest.registries();
        for(var item:List.of(Items.BEETROOT,Items.BEETROOT_SEEDS,Items.CARROT,Items.POTATO,Items.BREAD))
            BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow().bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }
    @Test void seedlessHarvestPaysOneActualDropAndKeepsAllOtherNativeLoot() {
        var bag=new SimpleContainer(new ItemStack(Items.WHEAT_SEEDS,12),new ItemStack(Items.BREAD),ItemStack.EMPTY,ItemStack.EMPTY);
        var seeds=new ItemStack(Items.BEETROOT_SEEDS,3);
        var plan=HarvestMaterials.reserve(bag,List.of(new ItemStack(Items.BEETROOT),seeds),Items.BEETROOT_SEEDS);
        assertNotNull(plan);assertTrue(plan.fromDrops);assertEquals(3,seeds.getCount(),"native input stacks are detached");
        assertTrue(HarvestMaterials.commit(bag,plan));assertFalse(HarvestMaterials.commit(bag,plan));
        assertEquals(1,bag.countItem(Items.BEETROOT));assertEquals(2,bag.countItem(Items.BEETROOT_SEEDS));
        assertEquals(12,bag.countItem(Items.WHEAT_SEEDS));assertEquals(1,bag.countItem(Items.BREAD));assertTrue(plan.overflow.isEmpty());
    }
    @Test void existingSeedCanPayButAnUnfundedNativeOutcomeNeverInventsOne() {
        var bag=new SimpleContainer(new ItemStack(Items.BEETROOT_SEEDS),ItemStack.EMPTY);
        var plan=HarvestMaterials.reserve(bag,List.of(new ItemStack(Items.BEETROOT)),Items.BEETROOT_SEEDS);
        assertNotNull(plan);assertFalse(plan.fromDrops);assertTrue(HarvestMaterials.commit(bag,plan));
        assertEquals(0,bag.countItem(Items.BEETROOT_SEEDS));assertEquals(1,bag.countItem(Items.BEETROOT));
        assertNull(HarvestMaterials.reserve(bag,List.of(new ItemStack(Items.BEETROOT)),Items.BEETROOT_SEEDS));
        assertNull(HarvestMaterials.reserve(bag,List.of(new ItemStack(Items.WHEAT_SEEDS)),Items.BEETROOT_SEEDS));
        assertNull(HarvestMaterials.reserve(bag,List.of(new ItemStack(Items.DIAMOND)),Items.DIAMOND));
        assertEquals(1,bag.countItem(Items.BEETROOT));
    }
    @Test void overflowReceiptIsOneUseEvenWhenLiveInventoryWouldNotChange() {
        var bag=new SimpleContainer(new ItemStack(Items.EMERALD,64));
        var plan=HarvestMaterials.reserve(bag,List.of(new ItemStack(Items.BEETROOT),new ItemStack(Items.BEETROOT_SEEDS)),Items.BEETROOT_SEEDS);
        assertNotNull(plan);assertEquals(1,plan.overflow.size());assertTrue(plan.overflow.getFirst().is(Items.BEETROOT));
        assertTrue(HarvestMaterials.commit(bag,plan));assertFalse(HarvestMaterials.commit(bag,plan));
        assertEquals(64,bag.countItem(Items.EMERALD));assertEquals(0,bag.countItem(Items.BEETROOT));
    }
    @Test void staleInventoryRejectsBothPaymentAndOutput() {
        var bag=new SimpleContainer(new ItemStack(Items.WHEAT_SEEDS,12),ItemStack.EMPTY,ItemStack.EMPTY);
        var before=HarvestMaterials.snapshot(bag);
        var plan=HarvestMaterials.reserve(bag,List.of(new ItemStack(Items.BEETROOT),new ItemStack(Items.BEETROOT_SEEDS,2)),Items.BEETROOT_SEEDS);
        bag.getItem(0).grow(1);
        assertFalse(HarvestMaterials.unchanged(bag,before));assertFalse(HarvestMaterials.commit(bag,plan));
        assertEquals(13,bag.countItem(Items.WHEAT_SEEDS));assertEquals(0,bag.countItem(Items.BEETROOT));assertEquals(0,bag.countItem(Items.BEETROOT_SEEDS));
    }
    @Test void edibleRootsReserveOneRealItemForTheNewCrop() {
        for(var item:List.of(Items.CARROT,Items.POTATO)) {
            var bag=new SimpleContainer(2);var plan=HarvestMaterials.reserve(bag,List.of(new ItemStack(item,4)),item);
            assertNotNull(plan);assertTrue(plan.fromDrops);assertTrue(HarvestMaterials.commit(bag,plan));
            assertEquals(3,bag.countItem(item));assertEquals(2,FoodInventory.availableCount(bag.getItem(0),true));
        }
    }
}
