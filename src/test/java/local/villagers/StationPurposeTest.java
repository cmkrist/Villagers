package local.villagers;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StationPurposeTest {
    @BeforeAll static void items() {
        WoodCraftingTest.registries();
        for(var item:List.of(Items.MANGROVE_LOG,Items.MANGROVE_PLANKS,Items.MANGROVE_ROOTS,Items.CRAFTING_TABLE))
            BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow().bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }
    @Test void fedWoodworkerWithoutGrainOrSeedsCanPayForATableFromOneOfTwoLogs() {
        var bag=new SimpleContainer(new ItemStack(Items.MANGROVE_ROOTS),new ItemStack(Items.MANGROVE_LOG,2),ItemStack.EMPTY,ItemStack.EMPTY);
        assertTrue(StationProduction.hasPurpose(bag,10,false));
        var planks=WoodCrafting.table(bag);assertEquals(WoodCrafting.Step.PLANKS,planks.step());
        assertTrue(WoodCrafting.commit(bag,planks,new ItemStack(Items.MANGROVE_PLANKS,4)));
        assertTrue(StationProduction.hasPurpose(bag,10,false));
        var table=WoodCrafting.table(bag);assertEquals(WoodCrafting.Step.TABLE,table.step());
        assertTrue(WoodCrafting.commit(bag,table,new ItemStack(Items.CRAFTING_TABLE)));
        assertTrue(StationProduction.hasPurpose(bag,10,false));
        assertEquals(1,bag.countItem(Items.MANGROVE_LOG));assertEquals(1,bag.countItem(Items.MANGROVE_ROOTS));
        assertEquals(1,bag.countItem(Items.CRAFTING_TABLE));assertEquals(0,bag.countItem(Items.MANGROVE_PLANKS));
    }
    @Test void urgentHungerYieldsOptionalBuildingButDoesNotDisableFoodCriticalPreparation() {
        var wood=new SimpleContainer(new ItemStack(Items.MANGROVE_LOG));
        assertFalse(StationProduction.hasPurpose(wood,6,false));assertTrue(StationProduction.hasPurpose(wood,7,false));
        assertTrue(StationProduction.hasPurpose(wood,0,true));
        for(var item:List.of(Items.MANGROVE_ROOTS,Items.STICK))
            assertFalse(StationProduction.hasPurpose(new SimpleContainer(new ItemStack(item,64)),20,false));
        assertFalse(StationProduction.hasPurpose(new SimpleContainer(8),20,false));
        assertFalse(WoodCrafting.canMakeTable(new SimpleContainer(new ItemStack(Items.MANGROVE_PLANKS,3))));
        assertTrue(WoodCrafting.canMakeTable(new SimpleContainer(new ItemStack(Items.MANGROVE_PLANKS,4))));
    }
}
