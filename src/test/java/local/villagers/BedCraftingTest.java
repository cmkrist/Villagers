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
        for(var item:java.util.List.of(Items.DARK_OAK_LOG,Items.DARK_OAK_PLANKS,Items.OAK_LOG,Items.OAK_PLANKS,Items.STRING,Items.WOOL.white(),Items.BED.white(),Items.AIR))
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
}
