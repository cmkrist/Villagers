package local.villagers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FoodInventoryTest {
    @BeforeAll static void items() {
        BreadCraftingTest.initializeItemRegistries();
        for (var item : java.util.List.of(Items.CARROT, Items.POTATO, Items.BEETROOT,Items.APPLE,Items.BAKED_POTATO,Items.PUMPKIN_PIE,Items.BEETROOT_SOUP,Items.ROTTEN_FLESH))
            BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow()
                    .bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }

    // These are explicitly component-bound fixtures; the native datapack loader is not running in unit tests.
    private static ItemStack ration(Item item, int count, int nutrition) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponents.FOOD, new FoodProperties.Builder().nutrition(nutrition).saturationModifier(.6F).build());
        return stack;
    }

    @Test void anOwnedFutureRationDoesNotBecomeAnImmediateMealOrDisappear() {
        var inventory = new SimpleContainer(ration(Items.BREAD, 2, 5));
        assertEquals(0, FoodInventory.anySlot(inventory, true));
        assertEquals(-1, FoodInventory.mealSlot(inventory, true, 19, 20, 20));
        assertEquals(2, inventory.countItem(Items.BREAD));
    }
    @Test void nativeStoreStaplesAreMealsWithoutDiscardingSoupBowlsOrEatingSpoiledFood(){
        for(var item:java.util.List.of(Items.APPLE,Items.BAKED_POTATO,Items.PUMPKIN_PIE)){
            var inv=new SimpleContainer(ration(item,1,4));assertEquals(0,FoodInventory.mealSlot(inv,true,12,20,20));
            assertEquals(1,inv.getItem(0).getCount());
        }
        assertFalse(FoodInventory.supported(ration(Items.BEETROOT_SOUP,1,6)));
        assertFalse(FoodInventory.supported(ration(Items.ROTTEN_FLESH,1,4)));
    }

    @Test void aFittingLaterSlotIsFoundWithoutWastingTheFirstBread() {
        var inventory = new SimpleContainer(ration(Items.BREAD, 2, 5), ration(Items.BEETROOT, 3, 1));
        assertEquals(1, FoodInventory.mealSlot(inventory, true, 19, 20, 20));
        assertEquals(0, FoodInventory.mealSlot(inventory, true, 15, 20, 20));
        assertEquals(2, inventory.countItem(Items.BREAD));
        assertEquals(3, inventory.countItem(Items.BEETROOT));
    }

    @Test void hungerDoesNotSpendAFarmersLastRootSeed() {
        var inventory = new SimpleContainer(ration(Items.CARROT, 1, 3), ration(Items.POTATO, 1, 1));
        assertEquals(-1, FoodInventory.anySlot(inventory, true));
        assertEquals(-1, FoodInventory.mealSlot(inventory, true, 0, 1, 20));
        assertEquals(0, FoodInventory.mealSlot(inventory, false, 0, 1, 20));
        inventory.setItem(0, ration(Items.CARROT, 2, 3));
        assertEquals(0, FoodInventory.mealSlot(inventory, true, 0, 1, 20));
    }

    @Test void injuryAndFullnessUseTheSamePolicyForAnySource() {
        var bread = ration(Items.BREAD, 1, 5);
        assertFalse(FoodInventory.fits(bread, 17, 20, 20));
        assertTrue(FoodInventory.fits(bread, 17, 12, 20));
        assertFalse(FoodInventory.fits(bread, 20, 12, 20));
        assertTrue(FoodInventory.fits(bread, 0, 1, 20));
    }

    @Test void actualComponentNutritionControlsTheDecisionInsteadOfVanillaItemAssumptions() {
        var differentBread = ration(Items.BREAD, 1, 7);
        assertFalse(FoodInventory.fits(differentBread, 15, 20, 20));
        assertTrue(FoodInventory.fits(differentBread, 13, 20, 20));
        assertTrue(FoodInventory.fits(differentBread, 17, 12, 20));
    }

    @Test void unsupportedItemsOrMissingFoodComponentsAreNotMealAffordances() {
        var fakeFood = ration(Items.EMERALD, 1, 5);
        assertFalse(FoodInventory.fits(fakeFood, 0, 1, 20));
        var bread = ration(Items.BREAD, 1, 5); bread.remove(DataComponents.FOOD);
        assertFalse(FoodInventory.supported(bread));
        assertFalse(FoodInventory.fits(bread, 0, 1, 20));
        assertEquals(-1, FoodInventory.mealSlot(new SimpleContainer(fakeFood, bread), false, 0, 1, 20));
    }

    @Test void oneItemGiftUsesActualNutritionRatherThanTheFirstInventorySlot() {
        var inv=new SimpleContainer(ration(Items.BEETROOT,12,1),ration(Items.BREAD,1,5));
        assertEquals(1,FoodInventory.sharingSlot(inv,true));
        assertEquals(12,inv.countItem(Items.BEETROOT));assertEquals(1,inv.countItem(Items.BREAD));
        inv.setItem(0,ration(Items.BEETROOT,12,7));
        assertEquals(0,FoodInventory.sharingSlot(inv,true),"Actual components outrank item names");
    }

    @Test void aSmallerGiftCanFitWhenTheRecipientCannotAcceptTheLargestMeal() {
        var donor=new SimpleContainer(ration(Items.BREAD,1,5),ration(Items.BEETROOT,2,1));
        var receiver=new SimpleContainer(ration(Items.BEETROOT,63,1),new ItemStack(Items.EMERALD,64));
        assertEquals(1,FoodInventory.sharingSlot(donor,true,one -> receiver.canAddItem(one)));
        assertEquals(63,receiver.countItem(Items.BEETROOT),"Capacity evaluation never performs the transfer");
        assertEquals(1,donor.countItem(Items.BREAD));assertEquals(2,donor.countItem(Items.BEETROOT));
        receiver.getItem(0).grow(1);
        assertEquals(-1,FoodInventory.sharingSlot(donor,true,one -> receiver.canAddItem(one)));
    }

    @Test void sharingCannotSpendTheLastPlantingRootOrOfferUnsupportedNutrition() {
        var inv=new SimpleContainer(ration(Items.CARROT,1,50),ration(Items.BREAD,1,5),ration(Items.EMERALD,1,100));
        assertEquals(1,FoodInventory.sharingSlot(inv,true));
        assertEquals(0,FoodInventory.sharingSlot(inv,false));
        inv.setItem(1,ration(Items.BREAD,1,0));
        assertEquals(-1,FoodInventory.sharingSlot(inv,true));
        assertEquals(1,inv.countItem(Items.CARROT));
    }
}
