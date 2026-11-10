package local.villagers;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import local.villagers.family.BreedingPolicy;
import local.villagers.family.Courtship;
import local.villagers.mind.AgentMind;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FamilyPreparationTest {
    @BeforeAll static void items() {
        BreadCraftingTest.initializeItemRegistries();
        for (var item : java.util.List.of(Items.CARROT, Items.POTATO, Items.BEETROOT))
            BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow()
                    .bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }

    @Test void privateReservePreparationPrecedesAnOfferAndRecoversAfterSharing() {
        Session s = new Session(new AgentMind(new UUID(101, 202)), 0);
        SimpleContainer inventory = new SimpleContainer(new ItemStack(Items.WHEAT, 9), ItemStack.EMPTY,
                new ItemStack(Items.EMERALD, 7));
        assertFalse(VillageFamilies.updateFoodIntent(s, true, inventory, false));
        assertTrue(s.familyFoodGoal, "ready bodies can prepare without a partner encounter");
        assertNull(s.courtship, "preparing is not agreement");
        assertEquals(9, inventory.countItem(Items.WHEAT), "intent must not consume or invent food");
        for (int i = 0; i < 3; i++)
            assertTrue(BreadCrafting.commit(inventory, BreadCrafting.reserve(inventory), new ItemStack(Items.BREAD)));
        assertTrue(VillageFamilies.updateFoodIntent(s, true, inventory, false));
        assertFalse(s.familyFoodGoal);
        assertNull(s.courtship, "funding is still not consent");
        s.courtship = new Courtship.Agreement(new UUID(303, 404), 1, 1201);
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (!inventory.getItem(slot).is(Items.BREAD)) continue;
            inventory.removeItem(slot, 1); break;
        }
        assertEquals(2, inventory.countItem(Items.BREAD));
        assertFalse(VillageFamilies.updateFoodIntent(s, true, inventory, false));
        assertTrue(s.familyFoodGoal, "lost stock reacquires preparation without waiting for another contact");
        assertNull(s.courtship, "unfunded invitation is no longer valid");
        assertEquals(7, inventory.countItem(Items.EMERALD));
    }

    @Test void lossOfOtherReadinessSuspendsGoalWithoutSpendingFood() {
        Session s = new Session(new AgentMind(new UUID(101, 203)), 0);
        SimpleContainer inventory = new SimpleContainer(new ItemStack(Items.BREAD, 2));
        VillageFamilies.updateFoodIntent(s, true, inventory, false);
        assertTrue(s.familyFoodGoal);
        assertFalse(VillageFamilies.updateFoodIntent(s, false, inventory, false));
        assertFalse(s.familyFoodGoal);
        assertEquals(2, inventory.countItem(Items.BREAD));
        assertNull(s.courtship);
    }

    @Test void reloadReestablishesIntentFromActualInventoryWithoutAPartnerOrSavedGoal() {
        Session reloaded = new Session(AgentMind.fromSnapshot(new AgentMind(new UUID(101, 204)).snapshot()), 500);
        SimpleContainer inventory = new SimpleContainer(new ItemStack(Items.BREAD, 2), new ItemStack(Items.CARROT));
        assertFalse(reloaded.familyFoodGoal);
        assertFalse(VillageFamilies.updateFoodIntent(reloaded, true, inventory, true));
        assertTrue(reloaded.familyFoodGoal);
        assertEquals(1, inventory.countItem(Items.CARROT), "retained seed cannot fund a child");
    }

    @Test void fastReadinessMatchesPaymentPlannerAcrossMixedNativeInventoriesWithoutMutation() {
        Random random = new Random(59173);
        var kinds = java.util.List.of(Items.BREAD, Items.CARROT, Items.POTATO, Items.BEETROOT, Items.WHEAT, Items.EMERALD);
        for (int sample = 0; sample < 1000; sample++) {
            SimpleContainer inventory = new SimpleContainer(8);
            var rule = new ArrayList<BreedingPolicy.Stack>();
            var before = new ArrayList<ItemStack>();
            boolean farmer = random.nextBoolean();
            for (int slot = 0; slot < 8; slot++) {
                var item = kinds.get(random.nextInt(kinds.size()));
                int count = random.nextInt(9);
                ItemStack stack = count == 0 ? ItemStack.EMPTY : new ItemStack(item, count);
                inventory.setItem(slot, stack); before.add(stack.copy());
                var food = item == Items.BREAD ? BreedingPolicy.Food.BREAD
                        : item == Items.CARROT || item == Items.POTATO || item == Items.BEETROOT
                        ? BreedingPolicy.Food.ROOT : BreedingPolicy.Food.OTHER;
                rule.add(new BreedingPolicy.Stack(food, count, farmer && (item == Items.CARROT || item == Items.POTATO)));
            }
            assertEquals(BreedingPolicy.foodCost(rule).isPresent(), VillageFamilies.hasFamilyFood(inventory, farmer),
                    "readiness must never advertise food that payment refuses, sample " + sample);
            for (int slot = 0; slot < 8; slot++) assertTrue(ItemStack.matches(before.get(slot), inventory.getItem(slot)));
        }
    }

    @Test void rawWheatPlanOnlyCoversTheMissingRealCostAndPreservesPlantingStock() {
        SimpleContainer inventory = new SimpleContainer(new ItemStack(Items.WHEAT, 64), ItemStack.EMPTY);
        assertEquals(9, VillageFamilies.wheatReserveNeeded(inventory, true));
        assertFalse(VillageFamilies.hasFamilyFood(inventory, true), "raw grain is not reproductive payment");
        inventory.setItem(1, new ItemStack(Items.BREAD, 2));
        assertEquals(3, VillageFamilies.wheatReserveNeeded(inventory, true));
        inventory.setItem(1, new ItemStack(Items.CARROT, 12));
        assertEquals(3, VillageFamilies.wheatReserveNeeded(inventory, true), "retain the last root seed");
        assertEquals(0, VillageFamilies.wheatReserveNeeded(inventory, false));
        inventory.setItem(1, new ItemStack(Items.CARROT, 13));
        assertEquals(0, VillageFamilies.wheatReserveNeeded(inventory, true));
        assertEquals(64, inventory.countItem(Items.WHEAT));
        assertEquals(13, inventory.countItem(Items.CARROT));
    }
}
