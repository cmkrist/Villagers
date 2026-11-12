package local.villagers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class IronPickaxeCraftingTest {
    @BeforeAll static void items(){
        BreadCraftingTest.initializeItemRegistries();
        for(var item:java.util.List.of(Items.IRON_INGOT,Items.IRON_PICKAXE,Items.STONE_PICKAXE,Items.OAK_LOG,Items.OAK_PLANKS,Items.STICK,Items.WHEAT_SEEDS))
            BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow().bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }
    @Test void oneActualLogAndThreeIngotsPayForOneIronPickaxeAndLeaveOnlyTheRealRemainders(){
        var bag=new SimpleContainer(8);bag.addItem(new ItemStack(Items.IRON_INGOT,3));bag.addItem(new ItemStack(Items.OAK_LOG));bag.addItem(new ItemStack(Items.WHEAT_SEEDS,4));
        for(var step:java.util.List.of(WoodCrafting.Step.PLANKS,WoodCrafting.Step.STICKS,WoodCrafting.Step.IRON_PICKAXE)){
            var plan=WoodCrafting.ironPickaxe(bag);assertNotNull(plan);assertEquals(step,plan.step());
            assertTrue(WoodCrafting.commit(bag,plan,new ItemStack(plan.output(),plan.count())));
            assertFalse(WoodCrafting.commit(bag,plan,new ItemStack(plan.output(),plan.count())));
        }
        assertEquals(1,bag.countItem(Items.IRON_PICKAXE));assertEquals(0,bag.countItem(Items.IRON_INGOT));assertEquals(0,bag.countItem(Items.OAK_LOG));
        assertEquals(2,bag.countItem(Items.OAK_PLANKS));assertEquals(2,bag.countItem(Items.STICK));assertEquals(4,bag.countItem(Items.WHEAT_SEEDS));
    }
    @Test void missingMetalAndRejectedOrStaleOutputsNeverConsumeThePossessedIngredients(){
        var bag=new SimpleContainer(new ItemStack(Items.IRON_INGOT,2),new ItemStack(Items.STICK,2),ItemStack.EMPTY);
        assertNull(WoodCrafting.ironPickaxe(bag));assertEquals(2,bag.countItem(Items.STICK));
        bag.setItem(0,new ItemStack(Items.IRON_INGOT,3));var plan=WoodCrafting.ironPickaxe(bag);assertNotNull(plan);
        assertFalse(WoodCrafting.commit(bag,plan,new ItemStack(Items.STONE_PICKAXE)));assertEquals(3,bag.countItem(Items.IRON_INGOT));
        bag.removeItem(0,1);assertFalse(WoodCrafting.commit(bag,plan,new ItemStack(Items.IRON_PICKAXE)));assertEquals(2,bag.countItem(Items.IRON_INGOT));
    }
}
