package local.villagers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class BoatCraftingTest {
    @Test void atSeaReopenTargetsTheForwardEndOfACompressedRouteSegment() {
        var points = java.util.List.of(new net.minecraft.world.phys.Vec3(0, 0, 0), new net.minecraft.world.phys.Vec3(1, 0, 0),
            new net.minecraft.world.phys.Vec3(31, 0, 0), new net.minecraft.world.phys.Vec3(32, 0, 0));
        assertEquals(2, BoatTravel.forwardWaypoint(points, new net.minecraft.world.phys.Vec3(12, 0, 0)));
        assertEquals(2, BoatTravel.forwardWaypoint(points, new net.minecraft.world.phys.Vec3(22, 0, 1)));
    }
    @BeforeAll static void items() {
        WoodCraftingTest.registries();
        for (var pair : BoatMaterials.TYPES.entrySet()) for (var item : java.util.List.of(pair.getKey(), pair.getValue().item()))
            BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow().bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
        for (var item : java.util.List.of(Items.CRIMSON_PLANKS, Items.WARPED_PLANKS))
            BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow().bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }
    @Test void twoLogsMakeOneBoatAndThreeSparePlanksWithNoReplay() {
        var bag = new SimpleContainer(new ItemStack(Items.OAK_LOG, 2), new ItemStack(Items.EMERALD, 7), ItemStack.EMPTY, ItemStack.EMPTY);
        for (var step : java.util.List.of(WoodCrafting.Step.PLANKS, WoodCrafting.Step.PLANKS, WoodCrafting.Step.BOAT)) {
            var plan = WoodCrafting.boat(bag); assertNotNull(plan); assertEquals(step, plan.step());
            if (step == WoodCrafting.Step.BOAT) {
                assertEquals(3, plan.input().width()); assertEquals(2, plan.input().height());
                assertTrue(plan.input().getItem(1).isEmpty());
            }
            assertTrue(WoodCrafting.commit(bag, plan, new ItemStack(plan.output(), plan.count())));
            assertFalse(WoodCrafting.commit(bag, plan, new ItemStack(plan.output(), plan.count())));
        }
        assertEquals(1, bag.countItem(Items.OAK_BOAT)); assertEquals(3, bag.countItem(Items.OAK_PLANKS));
        assertEquals(0, bag.countItem(Items.OAK_LOG)); assertEquals(7, bag.countItem(Items.EMERALD));
    }
    @Test void speciesCannotBeMixedAndAnInsufficientLogIsNotConsumed() {
        var bag = new SimpleContainer(new ItemStack(Items.OAK_PLANKS, 3), new ItemStack(Items.SPRUCE_PLANKS, 2));
        assertNull(WoodCrafting.boat(bag));
        assertNull(WoodCrafting.boat(new SimpleContainer(new ItemStack(Items.OAK_LOG))));
        bag.setItem(1, new ItemStack(Items.OAK_PLANKS, 2));
        assertEquals(Items.OAK_BOAT, WoodCrafting.boat(bag).output());
    }
    @Test void fullBagStalePlanAndWrongSpeciesLeaveInputsIntact() {
        var bag = new SimpleContainer(new ItemStack(Items.OAK_PLANKS, 6), new ItemStack(Items.EMERALD, 64));
        var plan = WoodCrafting.boat(bag);
        assertFalse(WoodCrafting.commit(bag, plan, new ItemStack(Items.OAK_BOAT)));
        assertFalse(WoodCrafting.commit(bag, plan, new ItemStack(Items.SPRUCE_BOAT)));
        bag.getItem(0).shrink(1);
        assertFalse(WoodCrafting.commit(bag, plan, new ItemStack(Items.OAK_BOAT)));
        assertTrue(WoodCrafting.commit(bag, WoodCrafting.boat(bag), new ItemStack(Items.OAK_BOAT)));
        assertEquals(64, bag.countItem(Items.EMERALD));
    }
    @Test void allSupportedSpeciesUseTheirOwnBoatAndNetherWoodIsRejected() {
        for (var pair : BoatMaterials.TYPES.entrySet()) {
            var bag = new SimpleContainer(new ItemStack(pair.getKey(), 5));
            assertEquals(pair.getValue().item(), WoodCrafting.boat(bag).output());
        }
        assertNull(WoodCrafting.boat(new SimpleContainer(new ItemStack(Items.CRIMSON_PLANKS, 5))));
        assertNull(WoodCrafting.boat(new SimpleContainer(new ItemStack(Items.WARPED_PLANKS, 5))));
        var permissions = local.villagers.capability.CapabilityPermissions.adventure();
        assertTrue(permissions.allows(local.villagers.capability.CapabilityPermissions.Capability.PILOT_BOAT));
        assertFalse(permissions.allows(local.villagers.capability.CapabilityPermissions.Capability.BREAK_BLOCK));
    }
}
