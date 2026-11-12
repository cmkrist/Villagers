package local.villagers;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class NativeCampFarmsTest {
    @BeforeAll static void items(){
        WoodCraftingTest.registries();
        for(var item:List.of(Items.OAK_LOG,Items.OAK_PLANKS,Items.STICK,Items.WOODEN_HOE,Items.WHEAT,Items.BREAD,Items.WHEAT_SEEDS,Items.CARROT))
            BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow().bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }
    @Test void oneOwnedLogFundsAHoeInAFiveSlotBagWithoutSpendingMealsOrSeeds(){
        var bag=new SimpleContainer(5);bag.addItem(new ItemStack(Items.WHEAT,3));bag.addItem(new ItemStack(Items.BREAD));
        bag.addItem(new ItemStack(Items.WHEAT_SEEDS,2));bag.addItem(new ItemStack(Items.OAK_LOG));
        for(var step:List.of(WoodCrafting.Step.PLANKS,WoodCrafting.Step.STICKS,WoodCrafting.Step.HOE)){
            var plan=WoodCrafting.next(bag);assertNotNull(plan);assertEquals(step,plan.step());
            assertTrue(WoodCrafting.commit(bag,plan,new ItemStack(plan.output(),plan.count())));
        }
        assertEquals(1,bag.countItem(Items.WOODEN_HOE));assertEquals(2,bag.countItem(Items.STICK));
        assertEquals(0,bag.countItem(Items.OAK_LOG));assertEquals(0,bag.countItem(Items.OAK_PLANKS));
        assertEquals(3,bag.countItem(Items.WHEAT));assertEquals(1,bag.countItem(Items.BREAD));assertEquals(2,bag.countItem(Items.WHEAT_SEEDS));
    }
    @Test void wheatPlantingNeverConsumesAnEdibleRootOrTheLastSeed(){
        var bag=new SimpleContainer(new ItemStack(Items.CARROT,3),new ItemStack(Items.WHEAT_SEEDS,2));
        var plan=NativeCampFarms.planting(bag);assertNotNull(plan);assertTrue(PlantingMaterials.commit(bag,plan));
        assertEquals(3,bag.countItem(Items.CARROT));assertEquals(1,bag.countItem(Items.WHEAT_SEEDS));assertNull(NativeCampFarms.planting(bag));
    }
    @Test void woodRequestsStopAtEnoughActualHoeIngredients(){
        var bag=new SimpleContainer(5);assertEquals(1,NativeCampFarms.wanted(bag,false,new ItemStack(Items.OAK_LOG)));
        bag.addItem(new ItemStack(Items.OAK_LOG));assertEquals(0,NativeCampFarms.wanted(bag,false,new ItemStack(Items.OAK_LOG)));
        assertEquals(0,NativeCampFarms.wanted(bag,false,new ItemStack(Items.OAK_PLANKS)));
        assertEquals(0,NativeCampFarms.wanted(bag,true,new ItemStack(Items.STICK)));
        assertEquals(8,NativeCampFarms.wanted(bag,true,new ItemStack(Items.WHEAT_SEEDS)));
    }
}
