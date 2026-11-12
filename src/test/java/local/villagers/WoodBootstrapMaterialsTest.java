package local.villagers;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class WoodBootstrapMaterialsTest {
    @BeforeAll static void items() {
        WoodCraftingTest.registries();
        for (var item : List.of(Items.CRAFTING_TABLE,Items.OAK_LOG,Items.OAK_PLANKS,Items.STICK,Items.WOODEN_HOE,Items.WOODEN_PICKAXE,
                Items.STONE_PICKAXE,Items.COBBLESTONE,Items.COBBLED_DEEPSLATE,Items.BLACKSTONE))
            BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow().bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }
    @Test void stoneUpgradeConsumesActualStoneAndSticksWhileKeepingTheWoodenPickaxe() {
        var bag=new SimpleContainer(8);bag.addItem(new ItemStack(Items.WOODEN_PICKAXE));bag.addItem(new ItemStack(Items.OAK_PLANKS));
        bag.addItem(new ItemStack(Items.OAK_LOG));bag.addItem(new ItemStack(Items.COBBLESTONE,3));
        for(var step:List.of(WoodCrafting.Step.PLANKS,WoodCrafting.Step.STICKS,WoodCrafting.Step.STONE_PICKAXE)){
            var plan=WoodCrafting.stonePickaxe(bag);assertNotNull(plan);assertEquals(step,plan.step());commit(bag,plan);
        }
        assertEquals(1,bag.countItem(Items.WOODEN_PICKAXE));assertEquals(1,bag.countItem(Items.STONE_PICKAXE));
        assertEquals(0,bag.countItem(Items.COBBLESTONE));assertEquals(3,bag.countItem(Items.OAK_PLANKS));assertEquals(2,bag.countItem(Items.STICK));
    }
    @Test void missingStoneNeverSpendsWoodAndMixedNativeStoneInputsRemainFinite() {
        var bag=new SimpleContainer(new ItemStack(Items.OAK_LOG),new ItemStack(Items.COBBLESTONE,2),ItemStack.EMPTY);
        assertNull(WoodCrafting.stonePickaxe(bag));assertEquals(1,bag.countItem(Items.OAK_LOG));
        bag=new SimpleContainer(new ItemStack(Items.COBBLESTONE),new ItemStack(Items.COBBLED_DEEPSLATE),new ItemStack(Items.BLACKSTONE),new ItemStack(Items.STICK,2));
        var plan=WoodCrafting.stonePickaxe(bag);assertNotNull(plan);
        assertFalse(WoodCrafting.commit(bag,plan,new ItemStack(Items.WOODEN_PICKAXE)));assertEquals(3,WoodCrafting.stoneCount(bag));
        commit(bag,plan);assertEquals(0,WoodCrafting.stoneCount(bag));
    }
    @Test void threeRealLogsPayForWorkbenchHoePickaxeAndLeaveOnlyOnePlank() {
        var bag = new SimpleContainer(8); bag.addItem(new ItemStack(Items.OAK_LOG,3));
        int crafts=0;
        while (bag.countItem(Items.CRAFTING_TABLE)==0) { commit(bag,WoodCrafting.table(bag));crafts++; }
        while (bag.countItem(Items.WOODEN_HOE)==0) { commit(bag,WoodCrafting.next(bag));crafts++; }
        while (bag.countItem(Items.WOODEN_PICKAXE)==0) { commit(bag,WoodCrafting.pickaxe(bag));crafts++; }
        assertEquals(7,crafts); assertEquals(0,bag.countItem(Items.OAK_LOG)); assertEquals(1,bag.countItem(Items.OAK_PLANKS));
        assertEquals(0,bag.countItem(Items.STICK)); assertEquals(1,bag.countItem(Items.CRAFTING_TABLE));
        assertEquals(1,bag.countItem(Items.WOODEN_HOE));assertEquals(1,bag.countItem(Items.WOODEN_PICKAXE));
    }
    @Test void sparePlanksCanFreeOneSlotWhileConservingEveryOtherMiningSupply(){
        var bag=new SimpleContainer(8);
        var stock=List.of(Items.STONE_PICKAXE,Items.WOODEN_HOE,Items.COBBLESTONE,Items.WOODEN_PICKAXE,Items.CRAFTING_TABLE,Items.OAK_LOG);
        for(int slot=0;slot<stock.size();slot++)bag.setItem(slot,new ItemStack(stock.get(slot)));
        bag.setItem(6,new ItemStack(Items.OAK_PLANKS,2));bag.setItem(7,new ItemStack(Items.STICK,2));
        var plan=WoodCrafting.compactPlanks(bag);assertNotNull(plan);
        assertEquals(2,bag.countItem(Items.OAK_PLANKS),"Planning cannot spend owned resources");
        commit(bag,plan);
        assertEquals(0,bag.countItem(Items.OAK_PLANKS));assertEquals(6,bag.countItem(Items.STICK));
        for(var item:stock)assertEquals(1,bag.countItem(item));
        int empty=0;for(int slot=0;slot<8;slot++)if(bag.getItem(slot).isEmpty())empty++;
        assertEquals(1,empty);
    }
    @Test void compactionCannotConsumeBoatSizedPlankStockOrCreateAnotherStickStack(){
        for(int planks:List.of(1,3,4,5)){
            var bag=new SimpleContainer(new ItemStack(Items.OAK_PLANKS,planks),new ItemStack(Items.STICK,2));
            assertNull(WoodCrafting.compactPlanks(bag));assertEquals(planks,bag.countItem(Items.OAK_PLANKS));
        }
        var bag=new SimpleContainer(new ItemStack(Items.OAK_PLANKS,2),new ItemStack(Items.STICK,64));
        assertNull(WoodCrafting.compactPlanks(bag));assertEquals(2,bag.countItem(Items.OAK_PLANKS));
        assertNull(WoodCrafting.compactPlanks(new SimpleContainer(new ItemStack(Items.OAK_PLANKS,2),ItemStack.EMPTY)));
    }
    private static void commit(SimpleContainer bag,WoodCrafting.Plan plan) {
        assertNotNull(plan);assertTrue(WoodCrafting.commit(bag,plan,new ItemStack(plan.output(),plan.count())));
    }
}
