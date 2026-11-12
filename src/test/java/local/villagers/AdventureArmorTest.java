package local.villagers;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class AdventureArmorTest {
    @BeforeAll static void items(){
        BreadCraftingTest.initializeItemRegistries();
        for(var item:List.of(Items.IRON_INGOT,Items.IRON_CHESTPLATE,Items.IRON_LEGGINGS,Items.IRON_HELMET,Items.IRON_BOOTS,Items.BREAD))
            BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow().bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }
    @Test void aFullSetCostsTwentyFourOwnedIngotsAndLeavesFoodUntouched(){
        var bag=new SimpleContainer(8);bag.addItem(new ItemStack(Items.IRON_INGOT,24));bag.addItem(new ItemStack(Items.BREAD,6));
        for(var piece:AdventureArmor.Piece.values()){
            var plan=AdventureArmor.plan(bag,piece);assertNotNull(plan);
            assertTrue(AdventureArmor.commit(bag,plan,new ItemStack(piece.item)));
            assertFalse(AdventureArmor.commit(bag,plan,new ItemStack(piece.item)));
        }
        assertEquals(0,bag.countItem(Items.IRON_INGOT));assertEquals(6,bag.countItem(Items.BREAD));
        for(var piece:AdventureArmor.Piece.values())assertEquals(1,bag.countItem(piece.item));
    }
    @Test void missingInputsStalePlansAndWrongOutputsLeaveTheActualBagUntouched(){
        var bag=new SimpleContainer(new ItemStack(Items.IRON_INGOT,7),ItemStack.EMPTY);
        assertNull(AdventureArmor.plan(bag,AdventureArmor.Piece.CHEST));bag.setItem(0,new ItemStack(Items.IRON_INGOT,8));
        var plan=AdventureArmor.plan(bag,AdventureArmor.Piece.CHEST);assertNotNull(plan);
        assertFalse(AdventureArmor.commit(bag,plan,new ItemStack(Items.IRON_BOOTS)));assertEquals(8,bag.countItem(Items.IRON_INGOT));
        assertFalse(AdventureArmor.commit(bag,plan,new ItemStack(Items.IRON_CHESTPLATE,2)));assertEquals(8,bag.countItem(Items.IRON_INGOT));
        bag.removeItem(0,1);assertFalse(AdventureArmor.commit(bag,plan,new ItemStack(Items.IRON_CHESTPLATE)));assertEquals(7,bag.countItem(Items.IRON_INGOT));
    }
    @Test void aFullBagCannotLoseMetalWhenTheActualCraftedPieceDoesNotFit(){
        var bag=new SimpleContainer(new ItemStack(Items.IRON_INGOT,64),new ItemStack(Items.BREAD,64));
        var plan=AdventureArmor.plan(bag,AdventureArmor.Piece.CHEST);assertNotNull(plan);
        assertFalse(AdventureArmor.commit(bag,plan,new ItemStack(Items.IRON_CHESTPLATE)));
        assertEquals(64,bag.countItem(Items.IRON_INGOT));assertEquals(64,bag.countItem(Items.BREAD));
    }
}
