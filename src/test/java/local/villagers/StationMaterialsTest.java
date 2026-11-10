package local.villagers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StationMaterialsTest {
    @BeforeAll static void items() {
        WoodCraftingTest.registries();
        BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(Items.CRAFTING_TABLE)).orElseThrow()
                .bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }
    @Test void portableNativeGridUsesFourRealPlanksAndOutputCapacity() {
        var bag = new SimpleContainer(new ItemStack(Items.OAK_LOG), new ItemStack(Items.EMERALD, 7), ItemStack.EMPTY);
        var log = WoodCrafting.table(bag); assertEquals(WoodCrafting.Step.PLANKS, log.step());
        assertTrue(WoodCrafting.commit(bag, log, new ItemStack(Items.OAK_PLANKS, 4)));
        var table = WoodCrafting.table(bag); assertEquals(WoodCrafting.Step.TABLE, table.step());
        assertEquals(2, table.input().width()); assertEquals(2, table.input().height());
        assertFalse(WoodCrafting.commit(bag, table, new ItemStack(Items.CRAFTING_TABLE, 2)));
        assertEquals(4, bag.countItem(Items.OAK_PLANKS));
        assertTrue(WoodCrafting.commit(bag, table, new ItemStack(Items.CRAFTING_TABLE)));
        assertEquals(7, bag.countItem(Items.EMERALD)); assertEquals(0, bag.countItem(Items.OAK_PLANKS));
        var plan = StationMaterials.reserve(bag); assertNotNull(plan);
        assertTrue(StationMaterials.commit(bag, plan)); assertFalse(StationMaterials.commit(bag, plan));
        assertEquals(0, bag.countItem(Items.CRAFTING_TABLE));
    }
    @Test void staleOrMissingTableCannotSpendAnotherItem() {
        var bag = new SimpleContainer(new ItemStack(Items.CRAFTING_TABLE), new ItemStack(Items.EMERALD, 7));
        var plan = StationMaterials.reserve(bag); bag.setItem(0, new ItemStack(Items.DIAMOND));
        assertFalse(StationMaterials.commit(bag, plan)); assertNull(StationMaterials.reserve(bag));
        assertEquals(1, bag.countItem(Items.DIAMOND)); assertEquals(7, bag.countItem(Items.EMERALD));
    }
    @Test void insufficientPlanksAndNoOutputSlotNeverProduceATable() {
        assertNull(WoodCrafting.table(new SimpleContainer(new ItemStack(Items.OAK_PLANKS, 3))));
        var full = new SimpleContainer(new ItemStack(Items.OAK_PLANKS, 64), new ItemStack(Items.EMERALD, 64));
        assertFalse(WoodCrafting.commit(full, WoodCrafting.table(full), new ItemStack(Items.CRAFTING_TABLE)));
        assertEquals(64, full.countItem(Items.OAK_PLANKS));
    }
}
