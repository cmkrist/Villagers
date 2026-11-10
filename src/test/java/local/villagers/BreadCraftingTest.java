package local.villagers;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BreadCraftingTest {
    @BeforeAll static void initializeItemRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // The 26.3 data-pack loader normally binds these. These transaction tests
        // need only ordinary 64-stack items, not a running world or a resource reload.
        for (var item : java.util.List.of(Items.WHEAT, Items.BREAD, Items.EMERALD, Items.DIAMOND, Items.AIR))
            net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item)).orElseThrow()
                    .bindComponents(net.minecraft.core.component.DataComponents.COMMON_ITEM_COMPONENTS);
    }

    @Test void splitWheatStacksBecomeBreadWithoutChangingUnrelatedItems() {
        SimpleContainer inventory = new SimpleContainer(new ItemStack(Items.WHEAT, 2),
                new ItemStack(Items.WHEAT, 2), new ItemStack(Items.EMERALD, 7), ItemStack.EMPTY);
        var reservation = BreadCrafting.reserve(inventory);
        assertEquals(4, BreadCrafting.wheatCount(inventory), "reservation must not consume live ingredients");
        assertTrue(BreadCrafting.commit(inventory, reservation, new ItemStack(Items.BREAD)));
        assertEquals(1, BreadCrafting.wheatCount(inventory));
        assertEquals(1, count(inventory, Items.BREAD));
        assertEquals(7, count(inventory, Items.EMERALD));
    }

    @Test void fullInventoryRejectsCraftWithoutConsumingAnyWheat() {
        SimpleContainer inventory = new SimpleContainer(new ItemStack(Items.WHEAT, 64), new ItemStack(Items.EMERALD, 64));
        assertFalse(BreadCrafting.commit(inventory, BreadCrafting.reserve(inventory), new ItemStack(Items.BREAD)));
        assertEquals(64, BreadCrafting.wheatCount(inventory));
        assertEquals(64, count(inventory, Items.EMERALD));
    }

    @Test void consumingTheLastThreeWheatCanFreeTheOutputSlot() {
        SimpleContainer inventory = new SimpleContainer(new ItemStack(Items.WHEAT, 3), new ItemStack(Items.EMERALD, 64));
        assertTrue(BreadCrafting.commit(inventory, BreadCrafting.reserve(inventory), new ItemStack(Items.BREAD)));
        assertEquals(0, BreadCrafting.wheatCount(inventory));
        assertEquals(1, count(inventory, Items.BREAD));
    }

    @Test void staleInventoryOrUnexpectedRecipeOutputNeverCommits() {
        SimpleContainer inventory = new SimpleContainer(new ItemStack(Items.WHEAT, 6), ItemStack.EMPTY);
        var reserved = BreadCrafting.reserve(inventory);
        inventory.removeItem(0, 1);
        assertFalse(BreadCrafting.commit(inventory, reserved, new ItemStack(Items.BREAD)));
        assertEquals(5, BreadCrafting.wheatCount(inventory));
        assertFalse(BreadCrafting.commit(inventory, BreadCrafting.reserve(inventory), new ItemStack(Items.DIAMOND)));
        assertEquals(5, BreadCrafting.wheatCount(inventory));
    }

    @Test void insufficientWheatCannotBeReserved() {
        assertNull(BreadCrafting.reserve(new SimpleContainer(new ItemStack(Items.WHEAT, 2))));
    }

    @Test void surplusWheatCanBuildOneParentsRealBreedingReserve() {
        SimpleContainer inventory = new SimpleContainer(new ItemStack(Items.WHEAT, 9), ItemStack.EMPTY,
                new ItemStack(Items.EMERALD, 7));
        for (int batch = 0; batch < 3; batch++)
            assertTrue(BreadCrafting.commit(inventory, BreadCrafting.reserve(inventory), new ItemStack(Items.BREAD)));
        assertEquals(0, BreadCrafting.wheatCount(inventory));
        assertEquals(3, count(inventory, Items.BREAD));
        assertEquals(7, count(inventory, Items.EMERALD));
        assertTrue(local.villagers.family.BreedingPolicy.foodCost(java.util.List.of(
                new local.villagers.family.BreedingPolicy.Stack(local.villagers.family.BreedingPolicy.Food.BREAD,
                        count(inventory, Items.BREAD), false))).isPresent());
    }

    private static int count(SimpleContainer inventory, net.minecraft.world.item.Item item) {
        int count = 0;
        for (var stack : inventory.getItems()) if (stack.is(item)) count += stack.getCount();
        return count;
    }
}
