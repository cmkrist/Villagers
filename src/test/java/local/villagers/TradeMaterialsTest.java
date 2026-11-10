package local.villagers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TradeMaterialsTest {
    @BeforeAll static void items() {WoodCraftingTest.registries();BreadCraftingTest.initializeItemRegistries();}
    @Test void realMoneyAndOneActualComponentBearingToolMoveBothWays() {
        var tool=new ItemStack(Items.WOODEN_HOE);tool.set(DataComponents.MAX_DAMAGE,59);tool.set(DataComponents.DAMAGE,17);
        tool.set(DataComponents.CUSTOM_NAME,Component.literal("Earned hoe"));
        var buyer=new SimpleContainer(new ItemStack(Items.EMERALD,3),ItemStack.EMPTY);
        var seller=new SimpleContainer(tool,ItemStack.EMPTY);
        var plan=TradeMaterials.reserve(buyer,seller,0,2);assertNotNull(plan);
        assertEquals(3,buyer.countItem(Items.EMERALD));assertEquals(1,seller.countItem(Items.WOODEN_HOE));
        assertTrue(TradeMaterials.commit(buyer,seller,plan));assertFalse(TradeMaterials.commit(buyer,seller,plan));
        assertEquals(1,buyer.countItem(Items.EMERALD));assertEquals(2,seller.countItem(Items.EMERALD));
        assertEquals(0,seller.countItem(Items.WOODEN_HOE));
        var acquired=buyer.getItem(1);assertTrue(ItemStack.matches(tool,acquired));assertEquals(17,acquired.getDamageValue());
    }
    @Test void actualSpentSlotsCanReceiveTheOtherHalfOfAnExchange() {
        var buyer=new SimpleContainer(new ItemStack(Items.EMERALD));var seller=new SimpleContainer(new ItemStack(Items.BREAD));
        assertTrue(TradeMaterials.commit(buyer,seller,TradeMaterials.reserve(buyer,seller,0,1)));
        assertEquals(1,buyer.countItem(Items.BREAD));assertEquals(1,seller.countItem(Items.EMERALD));
    }
    @Test void eitherFullReceiptOrInsufficientMoneyLeavesBothBagsUntouched() {
        var buyer=new SimpleContainer(new ItemStack(Items.EMERALD,2));var seller=new SimpleContainer(new ItemStack(Items.BREAD));
        assertNull(TradeMaterials.reserve(buyer,seller,0,1));assertEquals(2,buyer.countItem(Items.EMERALD));assertEquals(1,seller.countItem(Items.BREAD));
        buyer=new SimpleContainer(new ItemStack(Items.EMERALD),ItemStack.EMPTY);
        seller=new SimpleContainer(new ItemStack(Items.BREAD,2));
        assertNull(TradeMaterials.reserve(buyer,seller,0,1));assertEquals(1,buyer.countItem(Items.EMERALD));assertEquals(2,seller.countItem(Items.BREAD));
        assertNull(TradeMaterials.reserve(buyer,seller,0,2));
    }
    @Test void eitherStaleInventoryRejectsBeforeTakingAnything() {
        var buyer=new SimpleContainer(new ItemStack(Items.EMERALD,2),ItemStack.EMPTY);
        var seller=new SimpleContainer(new ItemStack(Items.BREAD,4),ItemStack.EMPTY);
        var plan=TradeMaterials.reserve(buyer,seller,0,1);seller.getItem(0).shrink(1);
        assertFalse(TradeMaterials.commit(buyer,seller,plan));assertEquals(2,buyer.countItem(Items.EMERALD));
        plan=TradeMaterials.reserve(buyer,seller,0,1);buyer.getItem(0).shrink(1);
        assertFalse(TradeMaterials.commit(buyer,seller,plan));assertEquals(0,seller.countItem(Items.EMERALD));
    }
    @Test void aPlanCannotChangeOwnersOrReplayEvenIfPriorBalancesReturn() {
        var buyer=new SimpleContainer(new ItemStack(Items.EMERALD));var seller=new SimpleContainer(new ItemStack(Items.BREAD));
        var plan=TradeMaterials.reserve(buyer,seller,0,1);
        assertFalse(TradeMaterials.commit(new SimpleContainer(new ItemStack(Items.EMERALD)),seller,plan));
        assertTrue(TradeMaterials.commit(buyer,seller,plan));
        buyer.setItem(0,new ItemStack(Items.EMERALD));seller.setItem(0,new ItemStack(Items.BREAD));
        assertFalse(TradeMaterials.commit(buyer,seller,plan));
        assertNull(TradeMaterials.reserve(buyer,buyer,0,1));assertNull(TradeMaterials.reserve(buyer,seller,0,0));
        assertNull(TradeMaterials.reserve(buyer,seller,0,3));
    }
    @Test void distinctRealCoinComponentsSurviveThePayment() {
        var named=new ItemStack(Items.EMERALD);named.set(DataComponents.CUSTOM_NAME,Component.literal("Saved coin"));
        var buyer=new SimpleContainer(named,new ItemStack(Items.EMERALD),ItemStack.EMPTY);
        var seller=new SimpleContainer(new ItemStack(Items.BREAD),ItemStack.EMPTY,ItemStack.EMPTY);
        assertTrue(TradeMaterials.commit(buyer,seller,TradeMaterials.reserve(buyer,seller,0,2)));
        assertTrue(ItemStack.matches(named,seller.getItem(0)));assertEquals(2,seller.countItem(Items.EMERALD));
        assertEquals(0,buyer.countItem(Items.EMERALD));assertEquals(1,buyer.countItem(Items.BREAD));
    }
    @Test void twoDifferentlyMarkedCoinsNeedRealReceivingSpace() {
        var named=new ItemStack(Items.EMERALD);named.set(DataComponents.CUSTOM_NAME,Component.literal("Saved coin"));
        var buyer=new SimpleContainer(named,new ItemStack(Items.EMERALD),ItemStack.EMPTY);
        var seller=new SimpleContainer(new ItemStack(Items.BREAD));
        assertNull(TradeMaterials.reserve(buyer,seller,0,2));assertEquals(2,buyer.countItem(Items.EMERALD));assertEquals(1,seller.countItem(Items.BREAD));
    }
    @Test void aBundleUsesExactMatchingStockAcrossSlotsWithoutLosingComponents() {
        var food=new ItemStack(Items.BREAD,2);food.set(DataComponents.CUSTOM_NAME,Component.literal("Owned food"));
        var more=food.copyWithCount(3);var other=new ItemStack(Items.BREAD,8);
        var seller=new SimpleContainer(food,more,other,ItemStack.EMPTY);
        var buyer=new SimpleContainer(new ItemStack(Items.EMERALD,2),ItemStack.EMPTY);
        var plan=TradeMaterials.reserve(buyer,seller,0,5,2);assertNotNull(plan);
        assertTrue(TradeMaterials.commit(buyer,seller,plan));assertFalse(TradeMaterials.commit(buyer,seller,plan));
        assertEquals(5,buyer.countItem(Items.BREAD));assertEquals(8,seller.countItem(Items.BREAD));
        assertTrue(ItemStack.matches(food.copyWithCount(5),buyer.getItem(0)));
        assertEquals(2,seller.countItem(Items.EMERALD));assertEquals(0,buyer.countItem(Items.EMERALD));
    }
    @Test void componentDifferencesCannotFillAMissingBundleAndPartialCapacityNeverPays() {
        var named=new ItemStack(Items.BREAD,2);named.set(DataComponents.CUSTOM_NAME,Component.literal("Distinct food"));
        var seller=new SimpleContainer(named,new ItemStack(Items.BREAD,8),ItemStack.EMPTY);
        var buyer=new SimpleContainer(new ItemStack(Items.EMERALD),new ItemStack(Items.BREAD,62));
        assertNull(TradeMaterials.reserve(buyer,seller,0,3,1));
        buyer=new SimpleContainer(new ItemStack(Items.EMERALD),new ItemStack(Items.BREAD,62));
        // Paying frees one slot, so the complete bundle fits, including ordinary overflow into that slot.
        assertNotNull(TradeMaterials.reserve(buyer,seller,1,5,1));
        buyer=new SimpleContainer(new ItemStack(Items.EMERALD,2),new ItemStack(Items.BREAD,62));
        assertNull(TradeMaterials.reserve(buyer,seller,1,5,1));
        assertEquals(2,buyer.countItem(Items.EMERALD));assertEquals(10,seller.countItem(Items.BREAD));
        assertNull(TradeMaterials.reserve(buyer,seller,1,0,1));assertNull(TradeMaterials.reserve(buyer,seller,1,9,1));
    }
    @Test void aStaleBundleCannotSpendMoneyAndTinyStacksNeedCompleteRealCapacity() {
        var food=new ItemStack(Items.BREAD);food.set(DataComponents.MAX_STACK_SIZE,1);
        var seller=new SimpleContainer(food,food.copy(),food.copy(),ItemStack.EMPTY);
        var buyer=new SimpleContainer(new ItemStack(Items.EMERALD),ItemStack.EMPTY);
        assertNull(TradeMaterials.reserve(buyer,seller,0,3,1));
        buyer=new SimpleContainer(new ItemStack(Items.EMERALD),ItemStack.EMPTY,ItemStack.EMPTY);
        var plan=TradeMaterials.reserve(buyer,seller,0,3,1);assertNotNull(plan);
        seller.getItem(0).shrink(1);assertFalse(TradeMaterials.commit(buyer,seller,plan));
        assertEquals(1,buyer.countItem(Items.EMERALD));assertEquals(0,buyer.countItem(Items.BREAD));
        plan=TradeMaterials.reserve(buyer,seller,1,2,1);assertNotNull(plan);assertTrue(TradeMaterials.commit(buyer,seller,plan));
        assertEquals(2,buyer.countItem(Items.BREAD));
        for(int i=0;i<buyer.getContainerSize();i++)assertTrue(buyer.getItem(i).getCount()<=buyer.getItem(i).getMaxStackSize());
    }
}
