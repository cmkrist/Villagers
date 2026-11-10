package local.villagers;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WoodCraftingTest {
    @BeforeAll static void registries() {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        // Native data-pack recipes/components are checked separately in the rendered client.
        for (var item : java.util.List.of(Items.OAK_LOG, Items.OAK_PLANKS, Items.SPRUCE_PLANKS,
                Items.STICK, Items.WOODEN_HOE, Items.WOODEN_PICKAXE, Items.WHEAT_SEEDS, Items.EMERALD, Items.DIAMOND, Items.AIR))
            BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow()
                    .bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }
    @Test void oneHeldLogBecomesOneHoeAndTwoSpareSticksWithoutTouchingSeeds() {
        var bag = new SimpleContainer(new ItemStack(Items.OAK_LOG), new ItemStack(Items.WHEAT_SEEDS, 12),
                ItemStack.EMPTY, ItemStack.EMPTY);
        for (var step : java.util.List.of(WoodCrafting.Step.PLANKS, WoodCrafting.Step.STICKS, WoodCrafting.Step.HOE)) {
            var plan = WoodCrafting.next(bag);
            assertNotNull(plan); assertEquals(step, plan.step());
            assertTrue(WoodCrafting.commit(bag, plan, new ItemStack(plan.output(), plan.count())));
            assertFalse(WoodCrafting.commit(bag, plan, new ItemStack(plan.output(), plan.count())), "no replay");
        }
        assertEquals(1, bag.countItem(Items.WOODEN_HOE)); assertEquals(2, bag.countItem(Items.STICK));
        assertEquals(12, bag.countItem(Items.WHEAT_SEEDS)); assertEquals(0, bag.countItem(Items.OAK_LOG));
        assertEquals(0, bag.countItem(Items.OAK_PLANKS)); assertNull(WoodCrafting.next(bag));
    }
    @Test void pickaxeRequiresThreeRealPlanksAndTwoSticksAfterIntermediateRecipes() {
        var bag = new SimpleContainer(new ItemStack(Items.OAK_LOG, 2), new ItemStack(Items.EMERALD, 7),
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
        for (int i = 0; i < 4; i++) {
            var plan = WoodCrafting.pickaxe(bag); assertNotNull(plan);
            assertTrue(WoodCrafting.commit(bag, plan, new ItemStack(plan.output(), plan.count())));
        }
        assertEquals(1, bag.countItem(Items.WOODEN_PICKAXE)); assertEquals(0, bag.countItem(Items.OAK_LOG));
        assertEquals(3, bag.countItem(Items.OAK_PLANKS)); assertEquals(2, bag.countItem(Items.STICK));
        assertEquals(7, bag.countItem(Items.EMERALD));
    }
    @Test void pickaxeCannotBeFinishedFromOnlyOneLogAndDoesNotConsumeRejectedPlans() {
        var bag = new SimpleContainer(new ItemStack(Items.OAK_LOG), ItemStack.EMPTY, ItemStack.EMPTY);
        var plan = WoodCrafting.pickaxe(bag);
        assertTrue(WoodCrafting.commit(bag, plan, new ItemStack(Items.OAK_PLANKS, 4)));
        assertNull(WoodCrafting.pickaxe(bag)); assertEquals(4, bag.countItem(Items.OAK_PLANKS));
        assertEquals(0, bag.countItem(Items.WOODEN_PICKAXE));
    }
    @Test void mixedSplitPlanksAndSticksPayOnlyTheMissingHoeRecipe() {
        var bag = new SimpleContainer(new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.SPRUCE_PLANKS),
                new ItemStack(Items.STICK), new ItemStack(Items.STICK), new ItemStack(Items.EMERALD, 7));
        var plan = WoodCrafting.next(bag); assertEquals(WoodCrafting.Step.HOE, plan.step());
        assertEquals(2, plan.input().width()); assertEquals(3, plan.input().height());
        assertTrue(WoodCrafting.commit(bag, plan, new ItemStack(Items.WOODEN_HOE)));
        assertEquals(0, bag.countItem(Items.STICK)); assertEquals(7, bag.countItem(Items.EMERALD));
    }
    @Test void insufficientWoodDoesNotConsumePlanksForUselessIntermediateSticks() {
        var bag = new SimpleContainer(new ItemStack(Items.OAK_PLANKS, 3), ItemStack.EMPTY);
        assertNull(WoodCrafting.next(bag)); assertEquals(3, bag.countItem(Items.OAK_PLANKS));
        bag.setItem(1, new ItemStack(Items.STICK, 2));
        assertEquals(WoodCrafting.Step.HOE, WoodCrafting.next(bag).step());
    }
    @Test void staleUnrelatedSlotAndUnexpectedNativeOutputsNeverConsumeWood() {
        var bag = new SimpleContainer(new ItemStack(Items.OAK_LOG, 2), new ItemStack(Items.EMERALD), ItemStack.EMPTY);
        var plan = WoodCrafting.next(bag); bag.getItem(1).grow(1);
        assertFalse(WoodCrafting.commit(bag, plan, new ItemStack(Items.OAK_PLANKS, 4)));
        plan = WoodCrafting.next(bag);
        assertFalse(WoodCrafting.commit(bag, plan, new ItemStack(Items.DIAMOND, 4)));
        assertFalse(WoodCrafting.commit(bag, plan, new ItemStack(Items.OAK_PLANKS, 5)));
        assertEquals(2, bag.countItem(Items.OAK_LOG)); assertEquals(2, bag.countItem(Items.EMERALD));
    }
    @Test void fullBackpackKeepsInputsButAnExactlyConsumedStackCanFreeItsSlot() {
        var bag = new SimpleContainer(new ItemStack(Items.OAK_LOG, 2), new ItemStack(Items.EMERALD, 64));
        assertFalse(WoodCrafting.commit(bag, WoodCrafting.next(bag), new ItemStack(Items.OAK_PLANKS, 4)));
        assertEquals(2, bag.countItem(Items.OAK_LOG));
        bag.getItem(0).setCount(1);
        assertTrue(WoodCrafting.commit(bag, WoodCrafting.next(bag), new ItemStack(Items.OAK_PLANKS, 4)));
        assertEquals(4, bag.countItem(Items.OAK_PLANKS)); assertEquals(64, bag.countItem(Items.EMERALD));
    }
    @Test void intermediateProductsSurviveARejectedLaterStepWithoutInventingAHoe() {
        var bag = new SimpleContainer(new ItemStack(Items.OAK_LOG), ItemStack.EMPTY, ItemStack.EMPTY);
        assertTrue(WoodCrafting.commit(bag, WoodCrafting.next(bag), new ItemStack(Items.OAK_PLANKS, 4)));
        var sticks = WoodCrafting.next(bag);
        assertFalse(WoodCrafting.commit(bag, sticks, new ItemStack(Items.STICK, 3)));
        assertEquals(4, bag.countItem(Items.OAK_PLANKS)); assertEquals(0, bag.countItem(Items.WOODEN_HOE));
    }
    @Test void productionMaterialsAndSpecificPermissionDoNotAuthorizeTerrainEdits() {
        assertTrue(WoodCrafting.material(new ItemStack(Items.OAK_LOG)));
        assertTrue(WoodCrafting.material(new ItemStack(Items.STICK)));
        assertFalse(WoodCrafting.material(new ItemStack(Items.EMERALD)));
        var p = local.villagers.capability.CapabilityPermissions.adventure();
        assertTrue(p.allows(local.villagers.capability.CapabilityPermissions.Capability.CRAFT_WOODEN_HOE));
        assertFalse(p.allows(local.villagers.capability.CapabilityPermissions.Capability.BREAK_BLOCK));
        assertFalse(p.allows(local.villagers.capability.CapabilityPermissions.Capability.PLACE_BLOCK));
    }
}
