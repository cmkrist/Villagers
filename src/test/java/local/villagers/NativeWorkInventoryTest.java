package local.villagers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class NativeWorkInventoryTest {
    @BeforeAll static void items(){
        BreadCraftingTest.initializeItemRegistries();
        for(var item:java.util.List.of(Items.WHEAT_SEEDS,Items.BEETROOT_SEEDS,Items.CARROT,Items.POTATO,Items.WOODEN_PICKAXE,Items.STONE_PICKAXE,Items.IRON_PICKAXE,Items.CLAY_BALL,Items.DIRT,Items.ANDESITE,Items.DIORITE,Items.GRANITE,Items.TUFF,Items.COBBLESTONE,Items.IRON_INGOT))
            BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow().bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }
    @Test void thePrimaryPlantingReserveAndUsableMealsAreRetained(){
        var bag=new SimpleContainer(new ItemStack(Items.WHEAT_SEEDS,4),new ItemStack(Items.BEETROOT_SEEDS),new ItemStack(Items.CARROT));
        assertEquals(1,NativeWorkInventory.secondaryCrop(bag));bag.setItem(1,ItemStack.EMPTY);
        assertEquals(2,NativeWorkInventory.secondaryCrop(bag));bag.setItem(2,new ItemStack(Items.CARROT,2));
        assertEquals(-1,NativeWorkInventory.secondaryCrop(bag));assertEquals(4,bag.countItem(Items.WHEAT_SEEDS));
    }
    @Test void noSecondaryCropIsDroppedWithoutASufficientPrimaryPlantingReserve(){
        var bag=new SimpleContainer(new ItemStack(Items.WHEAT_SEEDS),new ItemStack(Items.BEETROOT_SEEDS),new ItemStack(Items.POTATO));
        assertEquals(-1,NativeWorkInventory.secondaryCrop(bag));assertEquals(1,bag.countItem(Items.BEETROOT_SEEDS));
    }
    @Test void aHeldUpgradeCanFreeTheOldWoodenPickaxesSlotWithoutSpendingPlantingStock(){
        var bag=new SimpleContainer(new ItemStack(Items.WHEAT_SEEDS),new ItemStack(Items.WOODEN_PICKAXE));
        assertEquals(1,NativeWorkInventory.supersededPickaxe(bag,new ItemStack(Items.STONE_PICKAXE)));
        assertEquals(1,bag.countItem(Items.WHEAT_SEEDS));assertEquals(1,bag.countItem(Items.WOODEN_PICKAXE));
    }
    @Test void theOnlyPickaxeIsNeverSelectedForPuttingDown(){
        var bag=new SimpleContainer(new ItemStack(Items.WOODEN_PICKAXE));
        assertEquals(-1,NativeWorkInventory.supersededPickaxe(bag,ItemStack.EMPTY));
        assertEquals(-1,NativeWorkInventory.supersededPickaxe(bag,new ItemStack(Items.WOODEN_PICKAXE)));
    }
    @Test void aWornStonePickaxeIsRetainedUntilAnActualUsableReplacementExists(){
        var worn=new ItemStack(Items.STONE_PICKAXE);worn.set(DataComponents.MAX_DAMAGE,132);worn.set(DataComponents.DAMAGE,124);
        var fresh=new ItemStack(Items.STONE_PICKAXE);fresh.set(DataComponents.MAX_DAMAGE,132);fresh.set(DataComponents.DAMAGE,0);
        var bag=new SimpleContainer(worn);
        assertEquals(-1,NativeWorkInventory.supersededPickaxe(bag,ItemStack.EMPTY));
        assertEquals(0,NativeWorkInventory.supersededPickaxe(bag,fresh));
        assertEquals(1,bag.countItem(Items.STONE_PICKAXE));
    }
    @Test void aBackupCanUseAnEmptyOffhandButNeverDisplacesAnExistingHeldItem(){
        var bag=new SimpleContainer(new ItemStack(Items.WHEAT_SEEDS),new ItemStack(Items.WOODEN_PICKAXE));
        assertEquals(1,NativeWorkInventory.offhandBackup(bag,ItemStack.EMPTY));
        assertEquals(-1,NativeWorkInventory.offhandBackup(bag,new ItemStack(Items.STONE_PICKAXE)));
        assertEquals(1,bag.countItem(Items.WOODEN_PICKAXE),"Planning a hand transfer must retain the real tool");
        assertEquals(1,bag.countItem(Items.WHEAT_SEEDS));
    }
    @Test void theUsableStoneBackupCanReplaceWoodInTheOffhandWhileTheMainUpgradeRemains(){
        var stone=new ItemStack(Items.STONE_PICKAXE);stone.set(DataComponents.MAX_DAMAGE,132);stone.set(DataComponents.DAMAGE,72);
        var iron=new ItemStack(Items.IRON_PICKAXE);iron.set(DataComponents.MAX_DAMAGE,250);iron.set(DataComponents.DAMAGE,10);
        var bag=new SimpleContainer(new ItemStack(Items.WHEAT_SEEDS,4),stone);
        assertEquals(1,NativeWorkInventory.offhandStoneBackup(bag,iron,new ItemStack(Items.WOODEN_PICKAXE)));
        assertEquals(-1,NativeWorkInventory.offhandStoneBackup(bag,iron,new ItemStack(Items.STONE_PICKAXE)));
        assertEquals(-1,NativeWorkInventory.offhandStoneBackup(bag,ItemStack.EMPTY,new ItemStack(Items.WOODEN_PICKAXE)));
        assertEquals(4,bag.countItem(Items.WHEAT_SEEDS));assertEquals(72,bag.getItem(1).getDamageValue());
        stone.set(DataComponents.DAMAGE,124);
        assertEquals(-1,NativeWorkInventory.offhandStoneBackup(bag,iron,new ItemStack(Items.WOODEN_PICKAXE)));
    }
    @Test void quarryByproductCanFreeAWorkingSlotWithoutDroppingToolsDirtOrPlantingReserves(){
        var bag=new SimpleContainer(new ItemStack(Items.STONE_PICKAXE),new ItemStack(Items.DIRT,33),
            new ItemStack(Items.WHEAT_SEEDS),new ItemStack(Items.CLAY_BALL,4));
        assertEquals(3,NativeWorkInventory.quarryByproduct(bag));bag.setItem(3,ItemStack.EMPTY);
        assertEquals(-1,NativeWorkInventory.quarryByproduct(bag));assertEquals(33,bag.countItem(Items.DIRT));
        assertEquals(1,bag.countItem(Items.STONE_PICKAXE));assertEquals(1,bag.countItem(Items.WHEAT_SEEDS));
    }
    @Test void decorativeMiningDropsCannotOccupyTheOreSlotAtTheExpenseOfRecipeInputs(){
        for(var rock:java.util.List.of(Items.ANDESITE,Items.DIORITE,Items.GRANITE,Items.TUFF)){
            var bag=new SimpleContainer(new ItemStack(Items.IRON_INGOT,2),new ItemStack(Items.COBBLESTONE,8),
                new ItemStack(Items.WHEAT_SEEDS,4),new ItemStack(Items.STONE_PICKAXE),new ItemStack(rock,16));
            assertEquals(4,NativeWorkInventory.quarryByproduct(bag));bag.setItem(4,ItemStack.EMPTY);
            assertEquals(-1,NativeWorkInventory.quarryByproduct(bag));assertEquals(8,bag.countItem(Items.COBBLESTONE));
            assertEquals(2,bag.countItem(Items.IRON_INGOT));assertEquals(4,bag.countItem(Items.WHEAT_SEEDS));
        }
    }

    @Test void aSecondDirtStackCanBePutDownOnlyWhileAFullConstructionStackRemains(){
        var bag=new SimpleContainer(new ItemStack(Items.DIRT,64),new ItemStack(Items.DIRT,6),
            new ItemStack(Items.IRON_INGOT,2),new ItemStack(Items.WHEAT_SEEDS,4));
        assertEquals(1,NativeWorkInventory.quarryByproduct(bag));
        assertEquals(70,bag.countItem(Items.DIRT),"Selection must not delete the proposed world drop");
        bag.setItem(1,ItemStack.EMPTY);assertEquals(-1,NativeWorkInventory.quarryByproduct(bag));
        assertEquals(64,bag.countItem(Items.DIRT));assertEquals(2,bag.countItem(Items.IRON_INGOT));
        bag.setItem(0,new ItemStack(Items.DIRT,63));bag.setItem(1,new ItemStack(Items.DIRT,6));
        assertEquals(-1,NativeWorkInventory.quarryByproduct(bag));
    }

}
