package local.villagers;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class VillageBeddingTest {
    @BeforeAll static void bootstrap(){
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        var items=new java.util.ArrayList<Item>(java.util.List.of(Items.STRING,Items.OAK_LOG,Items.OAK_PLANKS,Items.BREAD));
        items.addAll(Items.WOOL.asList());items.addAll(Items.BED.asList());
        for(var item:items)BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow().bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }
    @Test void mixedColoursDoNotPretendToFundABedButOwnedStringCanCompleteWhiteWool(){
        var bag=new SimpleContainer(8);bag.setItem(0,new ItemStack(Items.WOOL.white(),2));bag.setItem(1,new ItemStack(Items.WOOL.black()));
        assertFalse(VillageBedding.hasBeddingFiber(bag));
        bag.setItem(2,new ItemStack(Items.STRING,4));assertTrue(VillageBedding.hasBeddingFiber(bag));
        bag.setItem(2,new ItemStack(Items.STRING,3));assertFalse(VillageBedding.hasBeddingFiber(bag));
    }
    @Test void aBuilderCompletesItsExistingColourInsteadOfHoardingEveryWoolStack(){
        var bag=new SimpleContainer(8);bag.setItem(0,new ItemStack(Items.WOOL.black(),2));
        assertEquals(1,VillageBedding.wanted(bag,new ItemStack(Items.WOOL.black(),20)));
        assertEquals(0,VillageBedding.wanted(bag,new ItemStack(Items.WOOL.white(),20)));
        assertEquals(0,VillageBedding.wanted(bag,new ItemStack(Items.BREAD,20)));
        assertEquals(2,bag.countItem(Items.WOOL.black()),"A supply quote must not mutate owned stock");
    }
    @Test void anOwnedBedStopsFurtherMaterialsAndWoodRequiresRealBeddingFiber(){
        var bag=new SimpleContainer(8);
        assertEquals(0,VillageBedding.wanted(bag,new ItemStack(Items.OAK_LOG)));
        bag.setItem(0,new ItemStack(Items.WOOL.white(),3));
        assertEquals(1,VillageBedding.wanted(bag,new ItemStack(Items.OAK_LOG)));
        bag.setItem(1,new ItemStack(Items.BED.white()));
        assertEquals(0,VillageBedding.wanted(bag,new ItemStack(Items.STRING)));
        assertEquals(0,VillageBedding.wanted(bag,new ItemStack(Items.BED.black())));
        assertEquals(0,VillageBedding.wanted(bag,new ItemStack(Items.OAK_LOG)));
    }
}
