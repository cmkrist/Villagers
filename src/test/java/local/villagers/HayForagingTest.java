package local.villagers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HayForagingTest {
    @BeforeAll static void items() {
        BreadCraftingTest.initializeItemRegistries();
        BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(Items.HAY_BLOCK)).orElseThrow()
                .bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }

    @Test void oneRealHayBaleMakesExactlyThreeBreadWithNoRemainingGrain() {
        SimpleContainer inventory = new SimpleContainer(new ItemStack(Items.HAY_BLOCK),
                new ItemStack(Items.EMERALD, 7), ItemStack.EMPTY);
        var plan = HayForaging.reserve(inventory);
        assertEquals(1, inventory.countItem(Items.HAY_BLOCK), "planning must leave live inventory untouched");
        assertTrue(HayForaging.commit(inventory, plan, new ItemStack(Items.WHEAT, 9)));
        for (int i = 0; i < 3; i++)
            assertTrue(BreadCrafting.commit(inventory, BreadCrafting.reserve(inventory), new ItemStack(Items.BREAD)));
        assertEquals(0, inventory.countItem(Items.HAY_BLOCK));
        assertEquals(0, inventory.countItem(Items.WHEAT));
        assertEquals(3, inventory.countItem(Items.BREAD));
        assertEquals(7, inventory.countItem(Items.EMERALD));
        assertFalse(HayForaging.commit(inventory, plan, new ItemStack(Items.WHEAT, 9)), "no replay");
    }

    @Test void blockedOutputNeverConsumesHayOrPartiallyInsertsWheat() {
        SimpleContainer inventory = new SimpleContainer(new ItemStack(Items.HAY_BLOCK, 2),
                new ItemStack(Items.WHEAT, 60), new ItemStack(Items.EMERALD, 64));
        assertFalse(HayForaging.commit(inventory, HayForaging.reserve(inventory), new ItemStack(Items.WHEAT, 9)));
        assertEquals(2, inventory.countItem(Items.HAY_BLOCK));
        assertEquals(60, inventory.countItem(Items.WHEAT));
        assertEquals(64, inventory.countItem(Items.EMERALD));
    }

    @Test void consumingLastHaySlotMakesRoomWithoutRequiringAnEmptySlot() {
        SimpleContainer inventory = new SimpleContainer(new ItemStack(Items.HAY_BLOCK), new ItemStack(Items.EMERALD, 64));
        assertTrue(HayForaging.commit(inventory, HayForaging.reserve(inventory), new ItemStack(Items.WHEAT, 9)));
        assertEquals(9, inventory.countItem(Items.WHEAT));
        assertEquals(64, inventory.countItem(Items.EMERALD));
    }

    @Test void changedInventoryAndUnsupportedRecipeResultsNeverCommit() {
        SimpleContainer inventory = new SimpleContainer(new ItemStack(Items.HAY_BLOCK, 2), ItemStack.EMPTY);
        var stale = HayForaging.reserve(inventory);
        inventory.removeItem(0, 1);
        assertFalse(HayForaging.commit(inventory, stale, new ItemStack(Items.WHEAT, 9)));
        for (ItemStack output : new ItemStack[]{ItemStack.EMPTY, new ItemStack(Items.DIAMOND),
                new ItemStack(Items.WHEAT, 8), new ItemStack(Items.WHEAT, 10)})
            assertFalse(HayForaging.commit(inventory, HayForaging.reserve(inventory), output));
        assertEquals(1, inventory.countItem(Items.HAY_BLOCK));
        assertEquals(0, inventory.countItem(Items.WHEAT));
    }

    @Test void wheatCannotBeInventedWithoutHay() {
        SimpleContainer inventory = new SimpleContainer(new ItemStack(Items.WHEAT, 2), ItemStack.EMPTY);
        assertFalse(HayForaging.hasHay(inventory));
        assertNull(HayForaging.reserve(inventory));
        assertFalse(HayForaging.commit(inventory, null, new ItemStack(Items.WHEAT, 9)));
        assertEquals(2, inventory.countItem(Items.WHEAT));
    }
}
