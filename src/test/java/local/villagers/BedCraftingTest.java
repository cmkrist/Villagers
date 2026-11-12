package local.villagers;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class BedCraftingTest {
    @BeforeAll static void bootstrap(){
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        var materials=new java.util.ArrayList<>(java.util.List.of(Items.DARK_OAK_LOG,Items.DARK_OAK_PLANKS,Items.OAK_LOG,Items.OAK_PLANKS,Items.STRING,Items.AIR,Items.IRON_INGOT,Items.SHEARS));
        materials.addAll(Items.WOOL.asList());materials.addAll(Items.BED.asList());
        for(var item:materials)
            net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item)).orElseThrow()
                .bindComponents(net.minecraft.core.component.DataComponents.COMMON_ITEM_COMPONENTS);
    }
    @Test void aLogAndTwelveStringsProduceOnlyOnePaidBed(){
        var bag=new SimpleContainer(5);bag.setItem(0,new ItemStack(Items.DARK_OAK_LOG));bag.setItem(1,new ItemStack(Items.STRING,12));
        for(int i=0;i<5;i++){
            var plan=BedCrafting.next(bag);assertNotNull(plan);
            assertEquals(i==4,plan.table());assertTrue(BedCrafting.commit(bag,plan,new ItemStack(plan.output(),plan.count())));
        }
        assertEquals(1,bag.countItem(Items.BED.white()));assertEquals(1,bag.countItem(Items.DARK_OAK_PLANKS));
        assertEquals(0,bag.countItem(Items.STRING));assertEquals(0,bag.countItem(Items.WOOL.white()));assertNull(BedCrafting.next(bag));
    }
    @Test void rejectsStaleIngredientsAndIncorrectNativeOutput(){
        var bag=new SimpleContainer(5);bag.setItem(0,new ItemStack(Items.WOOL.white(),3));bag.setItem(1,new ItemStack(Items.OAK_PLANKS,3));
        var plan=BedCrafting.next(bag);assertNotNull(plan);
        assertFalse(BedCrafting.commit(bag,plan,new ItemStack(Items.BED.white(),2)));
        bag.removeItem(0,1);assertFalse(BedCrafting.commit(bag,plan,new ItemStack(Items.BED.white())));
        assertEquals(2,bag.countItem(Items.WOOL.white()));assertEquals(3,bag.countItem(Items.OAK_PLANKS));
    }
    @Test void capacityFailureDoesNotSpendAStackOfLogs(){
        var bag=new SimpleContainer(2);bag.setItem(0,new ItemStack(Items.OAK_LOG,2));bag.setItem(1,new ItemStack(Items.STRING,12));
        var plan=BedCrafting.next(bag);assertNotNull(plan);assertFalse(BedCrafting.commit(bag,plan,new ItemStack(Items.OAK_PLANKS,4)));
        assertEquals(2,bag.countItem(Items.OAK_LOG));assertEquals(12,bag.countItem(Items.STRING));
    }
    @Test void nativeColoredWoolPaysForTheMatchingBedWithoutRecoloringOtherStock(){
        var bag=new SimpleContainer(5);bag.setItem(0,new ItemStack(Items.WOOL.gray(),3));
        bag.setItem(1,new ItemStack(Items.WOOL.white(),2));bag.setItem(2,new ItemStack(Items.OAK_PLANKS,3));
        var plan=BedCrafting.next(bag);assertNotNull(plan);assertTrue(plan.table());assertEquals(Items.BED.gray(),plan.output());
        assertFalse(BedCrafting.commit(bag,plan,new ItemStack(Items.BED.white())));
        assertEquals(3,bag.countItem(Items.WOOL.gray()));
        assertTrue(BedCrafting.commit(bag,plan,new ItemStack(Items.BED.gray())));
        assertEquals(1,bag.countItem(Items.BED.gray()));assertEquals(2,bag.countItem(Items.WOOL.white()));
        assertEquals(0,bag.countItem(Items.WOOL.gray()));assertEquals(0,bag.countItem(Items.OAK_PLANKS));
    }
    @Test void threeDifferentWoolColorsCannotBeCombinedIntoABed(){
        var bag=new SimpleContainer(5);bag.setItem(0,new ItemStack(Items.WOOL.gray()));
        bag.setItem(1,new ItemStack(Items.WOOL.white()));bag.setItem(2,new ItemStack(Items.WOOL.black()));
        bag.setItem(3,new ItemStack(Items.OAK_PLANKS,3));
        assertNull(BedCrafting.next(bag));assertEquals(3,bag.countItem(Items.OAK_PLANKS));
    }
    @Test void portableShearsConsumeExactlyTwoOwnedIngots(){
        var bag=new SimpleContainer(new ItemStack(Items.IRON_INGOT,2),new ItemStack(Items.OAK_PLANKS,3));
        var plan=BedCrafting.shears(bag);assertNotNull(plan);assertFalse(plan.table());
        assertEquals(2,bag.countItem(Items.IRON_INGOT));assertTrue(BedCrafting.commit(bag,plan,new ItemStack(Items.SHEARS)));
        assertEquals(0,bag.countItem(Items.IRON_INGOT));assertEquals(1,bag.countItem(Items.SHEARS));assertEquals(3,bag.countItem(Items.OAK_PLANKS));
    }
    @Test void insufficientOrChangedIronCannotProduceShears(){
        var bag=new SimpleContainer(new ItemStack(Items.IRON_INGOT));assertNull(BedCrafting.shears(bag));
        bag.setItem(0,new ItemStack(Items.IRON_INGOT,2));var plan=BedCrafting.shears(bag);assertNotNull(plan);
        bag.removeItem(0,1);assertFalse(BedCrafting.commit(bag,plan,new ItemStack(Items.SHEARS)));
        assertEquals(1,bag.countItem(Items.IRON_INGOT));assertEquals(0,bag.countItem(Items.SHEARS));
    }
}
