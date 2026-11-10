package local.villagers;

import java.util.UUID;
import local.villagers.family.Courtship;
import local.villagers.mind.AgentMind;
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

class FoodSupplyTest {
    @BeforeAll static void items() {
        BreadCraftingTest.initializeItemRegistries();
        for (var item : java.util.List.of(Items.CARROT, Items.POTATO, Items.BEETROOT))
            BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow()
                    .bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }

    private static ItemStack ration(Item item, int count, int nutrition) {
        var stack = new ItemStack(item, count);
        stack.set(DataComponents.FOOD, new FoodProperties.Builder().nutrition(nutrition).build());
        return stack;
    }

    @Test void productionEnquiriesRequireOwnIngredientsOrAnEquippedPlantingRoute() {
        var empty=new SimpleContainer(8);
        assertFalse(FoodSupply.canPrepare(empty,true));
        empty.addItem(new ItemStack(Items.WHEAT_SEEDS,3));
        assertFalse(FoodSupply.canPrepare(empty,false));
        assertTrue(FoodSupply.canPrepare(empty,true));
        var grain=new SimpleContainer(new ItemStack(Items.WHEAT,2));
        assertFalse(FoodSupply.canPrepare(grain,false));
        grain.getItem(0).grow(1);assertTrue(FoodSupply.canPrepare(grain,false));
        assertEquals(3,grain.countItem(Items.WHEAT),"an inquiry cannot consume its apparent inputs");
    }

    @Test void aParentInNativeAndPersistedBirthCooldownStillPreparesItsOwnFood() {
        Session s = new Session(new AgentMind(new UUID(21, 1)), 100);
        s.mind.bred(100);
        var inventory = new SimpleContainer(ration(Items.BREAD, 2, 5));
        assertTrue(s.mind.nextBreedingAt() > 200);
        assertTrue(FoodSupply.mayWork(5900, 20, .4, true));
        assertTrue(FoodSupply.update(s, true, inventory, false));
        assertFalse(s.familyFoodGoal, "self-supply must not manufacture reproductive intent");
        assertEquals(24100, s.mind.nextBreedingAt());
        assertEquals(2, inventory.countItem(Items.BREAD));
    }

    @Test void foodWorkExcludesChildrenDeadExhaustedOrUnsafeBodiesButAllowsHungryInjuredAdults() {
        assertFalse(FoodSupply.mayWork(-1, 20, .1, true));
        assertFalse(FoodSupply.mayWork(0, 0, .1, true));
        assertFalse(FoodSupply.mayWork(0, 20, .75, true));
        assertFalse(FoodSupply.mayWork(0, 20, .1, false));
        assertFalse(FoodSupply.mayWork(0, Float.NaN, .1, true));
        assertTrue(FoodSupply.mayWork(0, 1, .74, true));
    }

    @Test void urgentHungerStopsOptionalWoodWorkWithoutDisablingFoodPreparation() {
        assertTrue(FoodSupply.mayWork(5900,7,.2,true),"an injured parent can still seek real food");
        for(int food=0;food<=6;food++) assertFalse(FoodSupply.mayDoOptionalWork(food));
        assertTrue(FoodSupply.mayDoOptionalWork(7));assertTrue(FoodSupply.mayDoOptionalWork(20));
        assertFalse(FoodSupply.mayDoOptionalWork(-1));assertFalse(FoodSupply.mayDoOptionalWork(21));
    }

    @Test void nutritionComesFromActualEdibleComponentsNotRawWheatOrItemCounts() {
        var inventory = new SimpleContainer(new ItemStack(Items.WHEAT, 64), ration(Items.BREAD, 2, 5));
        assertEquals(5, FoodSupply.deficit(inventory, false));
        inventory.setItem(1, ration(Items.BREAD, 2, 7));
        assertEquals(1, FoodSupply.deficit(inventory, false));
        inventory.setItem(1, ration(Items.BREAD, 3, 5));
        assertEquals(0, FoodSupply.deficit(inventory, false));
        assertEquals(64, inventory.countItem(Items.WHEAT));
    }

    @Test void retainedPlantingRootsDoNotFundAReserve() {
        var inventory = new SimpleContainer(ration(Items.CARROT, 1, 3), ration(Items.POTATO, 1, 1));
        assertEquals(15, FoodSupply.deficit(inventory, true));
        assertEquals(11, FoodSupply.deficit(inventory, false));
        inventory.setItem(0, ration(Items.CARROT, 6, 3));
        assertEquals(0, FoodSupply.deficit(inventory, true));
        assertEquals(6, inventory.countItem(Items.CARROT));
        assertFalse(VillageFamilies.hasFamilyFood(inventory, true), "nutrition isn't reproductive-item units");
    }

    @Test void rawInputsCoverMissingNutritionOnlyAndHaveAFiniteBatchCap() {
        var inventory = new SimpleContainer(ration(Items.BREAD, 2, 5));
        assertEquals(3, FoodSupply.wheatTarget(inventory, false, 5));
        assertEquals(9, FoodSupply.wheatTarget(inventory, false, 1));
        assertEquals(0, FoodSupply.wheatTarget(inventory, false, 0));
        inventory.setItem(0, ration(Items.BREAD, 3, 5));
        assertEquals(0, FoodSupply.wheatTarget(inventory, false, 5));
        inventory.setItem(0, ration(Items.BREAD, 2, Integer.MAX_VALUE));
        assertEquals(0, FoodSupply.deficit(inventory, false), "large component nutrition must not overflow");
    }

    @Test void goalReconstructionAndCompletionDoNotModifyFamilyConsentOrStock() {
        Session s = new Session(new AgentMind(new UUID(21, 2)), 100);
        s.courtship = new Courtship.Agreement(new UUID(21, 3), 100, 1300);
        var agreement = s.courtship;
        var inventory = new SimpleContainer(ration(Items.BREAD, 2, 5));
        assertTrue(FoodSupply.update(s, true, inventory, false));
        inventory.getItem(0).grow(1);
        assertFalse(FoodSupply.update(s, true, inventory, false));
        assertSame(agreement, s.courtship);
        inventory.getItem(0).shrink(1);
        assertFalse(FoodSupply.update(s, false, inventory, false));
        assertEquals(2, inventory.countItem(Items.BREAD));
    }

    @Test void paidDemandUsesTheSameProductionReserveWithoutAuthorizingCharityOrPersistingAnOrder() {
        var s=new Session(new AgentMind(new UUID(21,10)),100);var peer=new UUID(21,11);
        var inventory=new SimpleContainer(ration(Items.BREAD,3,5));
        assertTrue(s.foodDemand.hearTrade(peer,100));
        assertEquals(20,FoodSupply.target(s,100));assertTrue(FoodSupply.update(s,true,inventory,false));
        assertNull(s.foodDemand.giftRequester(100));assertEquals(3,inventory.countItem(Items.BREAD));
        assertNull(new Session(AgentMind.fromSnapshot(s.mind.snapshot()),200).foodDemand.requester(200));
        s.foodDemand.tradeFulfilled(peer);assertEquals(15,FoodSupply.target(s,101));
        assertFalse(FoodSupply.update(s,true,inventory,false));
    }

    @Test void heardUnmetDemandCreatesOneBoundedSurplusGoalWithoutInventingFoodOrFamilyIntent() {
        var s = new Session(new AgentMind(new UUID(21, 4)), 100);
        var peer = new UUID(21, 5);
        var inventory = new SimpleContainer(ration(Items.BREAD, 3, 5), new ItemStack(Items.WHEAT, 3));
        assertFalse(FoodSupply.update(s, true, inventory, false));
        assertTrue(s.foodDemand.hear(peer, 100));
        assertTrue(FoodSupply.update(s, true, inventory, false));
        assertEquals(5, FoodSupply.deficit(inventory, false, FoodSupply.target(s, 100)));
        assertEquals(3, FoodSupply.wheatTarget(inventory, false, 5, FoodSupply.target(s, 100)));
        assertEquals(9, FoodSupply.wheatTarget(new SimpleContainer(), false, 1, FoodSupply.target(s, 100)));
        assertEquals(3, inventory.countItem(Items.BREAD)); assertEquals(3, inventory.countItem(Items.WHEAT));
        assertFalse(s.familyFoodGoal); assertNull(s.courtship);
        s.foodDemand.fulfilled(peer);
        assertFalse(FoodSupply.update(s, true, inventory, false));
    }

    @Test void demandNeverOverridesUnsafeBodyOrPlantingReservesAndExpiresWithoutTakingInventory() {
        var s = new Session(new AgentMind(new UUID(21, 6)), 100);
        s.foodDemand.hear(new UUID(21, 7), 100);
        var inventory = new SimpleContainer(ration(Items.CARROT, 6, 3));
        assertEquals(5, FoodSupply.deficit(inventory, true, FoodSupply.target(s, 100)));
        assertFalse(FoodSupply.update(s, false, inventory, true));
        assertTrue(FoodSupply.update(s, true, inventory, true));
        s.lastTick = 100 + local.villagers.economy.FoodDemand.LIFETIME;
        assertFalse(FoodSupply.update(s, true, inventory, true));
        assertEquals(6, inventory.countItem(Items.CARROT));
        assertEquals(0, new Session(AgentMind.fromSnapshot(s.mind.snapshot()), 2500).foodDemand.extraNutrition(2500),
                "Uncommitted demand is a per-loaded-session signal, not invented persisted debt");
    }

    @Test void satisfyingSurplusProductionConsumesRealRecipeInputsAndStopsAtItsTarget() {
        var s = new Session(new AgentMind(new UUID(21, 8)), 100);
        var inventory = new SimpleContainer(ration(Items.BREAD, 3, 5), new ItemStack(Items.WHEAT, 3));
        s.foodDemand.hear(new UUID(21, 9), 100);
        assertTrue(FoodSupply.update(s, true, inventory, false));
        var reservation = BreadCrafting.reserve(inventory);
        assertNotNull(reservation);
        assertTrue(BreadCrafting.commit(inventory, reservation, ration(Items.BREAD, 1, 5)));
        assertEquals(0, inventory.countItem(Items.WHEAT));
        assertEquals(4, inventory.countItem(Items.BREAD));
        assertFalse(FoodSupply.update(s, true, inventory, false));
        assertFalse(BreadCrafting.commit(inventory, reservation, ration(Items.BREAD, 1, 5)),
                "A replayed reservation cannot mint a second surplus meal");
        s.lastTick = 100 + local.villagers.economy.FoodDemand.LIFETIME;
        assertFalse(FoodSupply.update(s, true, inventory, false));
        assertEquals(4, inventory.countItem(Items.BREAD), "Expiry does not destroy earned output");
    }

    @Test void approvedFutureFoodWorkCanStartBeforePersonalReserveRunsOut() {
        var inventory=new SimpleContainer(ration(Items.BREAD,3,5),new ItemStack(Items.WHEAT_SEEDS,8));
        assertTrue(FoodSupply.mayInvest(FoodSupply.mayWork(5900,20,.2,true),16,FoodSupply.deficit(inventory,true)),
                "A stocked adult in birth cooldown may prepare approved future capacity");
        assertEquals(3,inventory.countItem(Items.BREAD));assertEquals(8,inventory.countItem(Items.WHEAT_SEEDS));
    }
    @Test void capacityInvestmentNeverTreatsRawGrainOrReservedRootsAsAnEdibleReserve() {
        var inventory=new SimpleContainer(new ItemStack(Items.WHEAT,64),ration(Items.CARROT,1,3));
        assertFalse(FoodSupply.mayInvest(true,20,FoodSupply.deficit(inventory,true)));
        assertEquals(64,inventory.countItem(Items.WHEAT));assertEquals(1,inventory.countItem(Items.CARROT));
    }
    @Test void capacityInvestmentYieldsToOwnNeedAndUnavailableBody() {
        for(int food=0;food<=12;food++)assertFalse(FoodSupply.mayInvest(true,food,0));
        assertFalse(FoodSupply.mayInvest(false,20,0));
        assertFalse(FoodSupply.mayInvest(true,20,1));
        assertFalse(FoodSupply.mayInvest(true,21,0));
    }
}
