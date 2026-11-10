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

class SoilMaterialsTest {
    @BeforeAll static void items() {
        WoodCraftingTest.registries();
        for(var item:List.of(Items.DIRT,Items.SAND,Items.STONE))
            BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow().bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }
    @Test void realNativeDirtFundsOneReplacementAndConservesDisplacedSand() {
        var bag=new SimpleContainer(2);var nativeDirt=new ItemStack(Items.DIRT);
        var take=SoilMaterials.gather(bag,List.of(nativeDirt));assertNotNull(take);assertTrue(SoilMaterials.commit(bag,take));
        assertEquals(1,nativeDirt.getCount());assertEquals(1,bag.countItem(Items.DIRT));assertFalse(SoilMaterials.commit(bag,take));
        var put=SoilMaterials.replace(bag,List.of(new ItemStack(Items.SAND)));assertNotNull(put);assertTrue(SoilMaterials.commit(bag,put));
        assertEquals(0,bag.countItem(Items.DIRT));assertEquals(1,bag.countItem(Items.SAND));assertFalse(SoilMaterials.commit(bag,put));
        assertNull(SoilMaterials.replace(bag,List.of(new ItemStack(Items.SAND))));
    }
    @Test void unsupportedOrMissingNativeLootNeverCreatesAResource() {
        var bag=new SimpleContainer(1);
        assertNull(SoilMaterials.gather(bag,List.of()));assertNull(SoilMaterials.gather(bag,List.of(new ItemStack(Items.SAND))));
        assertNull(SoilMaterials.gather(bag,List.of(new ItemStack(Items.DIRT,2))));assertTrue(bag.isEmpty());
        bag.setItem(0,new ItemStack(Items.DIRT));assertNull(SoilMaterials.replace(bag,List.of(new ItemStack(Items.STONE))));
        assertEquals(1,bag.countItem(Items.DIRT));
    }
    @Test void paymentCanFreeCapacityButIncomeNeverSilentlyDiscardsStock() {
        var bag=new SimpleContainer(new ItemStack(Items.DIRT));
        assertNull(SoilMaterials.gather(new SimpleContainer(new ItemStack(Items.STONE,64)),List.of(new ItemStack(Items.DIRT))));
        var put=SoilMaterials.replace(bag,List.of(new ItemStack(Items.SAND)));assertNotNull(put);assertTrue(SoilMaterials.commit(bag,put));
        assertEquals(1,bag.countItem(Items.SAND));
    }
    @Test void staleWholeInventoryCannotCommitEitherDirection() {
        var bag=new SimpleContainer(new ItemStack(Items.DIRT),ItemStack.EMPTY);
        var plan=SoilMaterials.replace(bag,List.of(new ItemStack(Items.SAND)));bag.setItem(1,new ItemStack(Items.STONE));
        assertFalse(SoilMaterials.commit(bag,plan));assertEquals(1,bag.countItem(Items.DIRT));assertEquals(0,bag.countItem(Items.SAND));
    }
    @Test void finiteAirPlacementSpendsActualDirtWithoutLootOrReplay() {
        var bag=new SimpleContainer(new ItemStack(Items.DIRT,7),new ItemStack(Items.SAND));
        for(int i=0;i<7;i++) {
            var payment=SoilMaterials.placeDirt(bag);assertNotNull(payment);assertEquals(7-i,bag.countItem(Items.DIRT));
            assertTrue(SoilMaterials.commit(bag,payment));assertFalse(SoilMaterials.commit(bag,payment));
        }
        assertNull(SoilMaterials.placeDirt(bag));assertEquals(0,bag.countItem(Items.DIRT));assertEquals(1,bag.countItem(Items.SAND));
        bag.setItem(0,new ItemStack(Items.DIRT));var stale=SoilMaterials.placeDirt(bag);bag.setItem(1,new ItemStack(Items.STONE));
        assertFalse(SoilMaterials.commit(bag,stale));assertEquals(1,bag.countItem(Items.DIRT));
    }
}
