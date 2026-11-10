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

class ForestryMaterialsTest {
    @BeforeAll static void items() {
        WoodCraftingTest.registries();
        for(var item:List.of(Items.MANGROVE_PROPAGULE,Items.MANGROVE_LOG,Items.MANGROVE_ROOTS)) BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow().bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }
    @Test void actualSeedThenTwoLogsThenOnePaidPlantAreConservedAndNotReplayed() {
        var bag=new SimpleContainer(3);
        var receive=ForestryMaterials.receive(bag,List.of(new ItemStack(Items.MANGROVE_PROPAGULE)),Items.MANGROVE_PROPAGULE);
        assertTrue(ForestryMaterials.commit(bag,receive));assertFalse(ForestryMaterials.commit(bag,receive));
        for(int i=0;i<2;i++) assertTrue(ForestryMaterials.commit(bag,ForestryMaterials.receive(bag,List.of(new ItemStack(Items.MANGROVE_LOG)),Items.MANGROVE_LOG)));
        assertEquals(1,bag.countItem(Items.MANGROVE_PROPAGULE));assertEquals(2,bag.countItem(Items.MANGROVE_LOG));
        var seed=ForestryMaterials.seed(bag);assertTrue(ForestryMaterials.plant(bag,seed));assertFalse(ForestryMaterials.plant(bag,seed));
        assertEquals(0,bag.countItem(Items.MANGROVE_PROPAGULE));assertEquals(2,bag.countItem(Items.MANGROVE_LOG));
    }
    @Test void staleInventoryAndUnsupportedLootNeverCommit() {
        var bag=new SimpleContainer(new ItemStack(Items.EMERALD),ItemStack.EMPTY);
        var plan=ForestryMaterials.receive(bag,List.of(new ItemStack(Items.MANGROVE_LOG)),Items.MANGROVE_LOG);bag.getItem(0).grow(1);
        assertFalse(ForestryMaterials.commit(bag,plan));assertEquals(0,bag.countItem(Items.MANGROVE_LOG));
        assertNull(ForestryMaterials.receive(bag,List.of(new ItemStack(Items.DIAMOND)),Items.MANGROVE_LOG));
        assertNull(ForestryMaterials.receive(bag,List.of(new ItemStack(Items.MANGROVE_LOG,2)),Items.MANGROVE_LOG));
    }
    @Test void actualRootLootIsConservedAndCannotBecomeFreePlanks() {
        var bag=new SimpleContainer(new ItemStack(Items.MANGROVE_PROPAGULE),ItemStack.EMPTY,ItemStack.EMPTY);
        var plan=ForestryMaterials.receive(bag,List.of(new ItemStack(Items.MANGROVE_ROOTS)),Items.MANGROVE_ROOTS);
        assertTrue(ForestryMaterials.commit(bag,plan));assertFalse(ForestryMaterials.commit(bag,plan));
        assertEquals(1,bag.countItem(Items.MANGROVE_ROOTS));assertEquals(1,bag.countItem(Items.MANGROVE_PROPAGULE));
        assertEquals(0,bag.countItem(Items.MANGROVE_LOG));assertNull(WoodCrafting.next(bag));
    }
    @Test void noOverflowAndNoPlantingWithAChangedSeed() {
        var full=new SimpleContainer(new ItemStack(Items.EMERALD,64));
        assertNull(ForestryMaterials.receive(full,List.of(new ItemStack(Items.MANGROVE_LOG)),Items.MANGROVE_LOG));
        var seedBag=new SimpleContainer(new ItemStack(Items.MANGROVE_PROPAGULE));var seed=ForestryMaterials.seed(seedBag);
        seedBag.setItem(0,new ItemStack(Items.EMERALD));assertFalse(ForestryMaterials.plant(seedBag,seed));assertEquals(1,seedBag.countItem(Items.EMERALD));
    }
}
