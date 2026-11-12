package local.villagers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class IronCraftingTest {
    @BeforeAll static void items(){
        BreadCraftingTest.initializeItemRegistries();
        for(var item:java.util.List.of(Items.COBBLESTONE,Items.FURNACE,Items.STONE_PICKAXE))
            BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow().bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }
    @Test void eightOwnedStonePayForOneFurnaceWithoutSpendingThePickaxe(){
        var bag=new SimpleContainer(8);bag.addItem(new ItemStack(Items.COBBLESTONE,8));bag.addItem(new ItemStack(Items.STONE_PICKAXE));
        var plan=IronCrafting.furnace(bag);assertNotNull(plan);assertEquals(8,bag.countItem(Items.COBBLESTONE));
        assertTrue(IronCrafting.commit(bag,plan,new ItemStack(Items.FURNACE)));assertEquals(0,bag.countItem(Items.COBBLESTONE));
        assertEquals(1,bag.countItem(Items.FURNACE));assertEquals(1,bag.countItem(Items.STONE_PICKAXE));
    }
    @Test void MissingOrChangedInputsAndWrongOutputsDoNotCreateAnything(){
        var bag=new SimpleContainer(new ItemStack(Items.COBBLESTONE,7),ItemStack.EMPTY);
        assertNull(IronCrafting.furnace(bag));bag.setItem(0,new ItemStack(Items.COBBLESTONE,8));var plan=IronCrafting.furnace(bag);
        assertFalse(IronCrafting.commit(bag,plan,new ItemStack(Items.STONE_PICKAXE)));assertEquals(8,bag.countItem(Items.COBBLESTONE));
        bag.removeItem(0,1);assertFalse(IronCrafting.commit(bag,plan,new ItemStack(Items.FURNACE)));assertEquals(7,bag.countItem(Items.COBBLESTONE));
    }
}
