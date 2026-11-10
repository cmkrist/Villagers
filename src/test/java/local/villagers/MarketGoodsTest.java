package local.villagers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MarketGoodsTest {
    @Test void rawWoodSaleRetainsOneRealLogAndWakesActualRecipeInputsAtTheBuyer() {
        var seller=new SimpleContainer(new ItemStack(Items.OAK_LOG,2),ItemStack.EMPTY);
        var buyer=new SimpleContainer(new ItemStack(Items.EMERALD),ItemStack.EMPTY);
        var plan=MarketGoods.quote(buyer,false,seller,ItemStack.EMPTY,false,MarketGoods.Good.WOOD,1);
        assertNotNull(plan);assertEquals(1,plan.goods.getCount());
        assertTrue(TradeMaterials.commit(buyer,seller,plan));
        assertEquals(1,seller.countItem(Items.OAK_LOG));assertEquals(1,seller.countItem(Items.EMERALD));
        assertEquals(WoodCrafting.Step.PLANKS,WoodCrafting.next(buyer).step());
        assertEquals(-1,MarketGoods.offer(seller,ItemStack.EMPTY,false,MarketGoods.Good.WOOD));
    }
    @Test void rawWoodDoesNotSubstitutePlanksSticksOrUnusableGoodsForTheQuotedInput() {
        var seller=new SimpleContainer(new ItemStack(Items.OAK_LOG),new ItemStack(Items.OAK_PLANKS,64),new ItemStack(Items.STICK,64));
        assertEquals(-1,MarketGoods.offer(seller,ItemStack.EMPTY,false,MarketGoods.Good.WOOD));
        assertFalse(MarketGoods.accepts(new SimpleContainer(new ItemStack(Items.STICK,64)),false,MarketGoods.Good.WOOD));
        assertEquals(local.villagers.language.Concept.WOOD,MarketGoods.concept(MarketGoods.Good.WOOD));
    }
    @Test void markedRawWoodAndCoinsNeedActualCapacityAndRetainTheirComponents() {
        var logs=new ItemStack(Items.OAK_LOG,2);logs.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Owned wood"));
        var coin=new ItemStack(Items.EMERALD);coin.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Owned coin"));
        var seller=new SimpleContainer(logs,ItemStack.EMPTY);var buyer=new SimpleContainer(coin);
        var plan=MarketGoods.quote(buyer,false,seller,ItemStack.EMPTY,false,MarketGoods.Good.WOOD,1);
        assertNotNull(plan);assertTrue(TradeMaterials.commit(buyer,seller,plan));
        assertEquals("Owned wood",buyer.getItem(0).get(DataComponents.CUSTOM_NAME).getString());
        assertEquals("Owned coin",seller.getItem(1).get(DataComponents.CUSTOM_NAME).getString());
        assertNull(MarketGoods.quote(new SimpleContainer(new ItemStack(Items.EMERALD,2)),false,
                new SimpleContainer(new ItemStack(Items.OAK_LOG,2),ItemStack.EMPTY),ItemStack.EMPTY,false,MarketGoods.Good.WOOD,1));
    }
    @BeforeAll static void items() {
        WoodCraftingTest.registries();BreadCraftingTest.initializeItemRegistries();
        net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(Items.CARROT)).orElseThrow()
                .bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
        net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(Items.BEETROOT)).orElseThrow()
                .bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }
    private static ItemStack bread(int count) {
        var stack=new ItemStack(Items.BREAD,count);stack.set(DataComponents.FOOD,new FoodProperties.Builder().nutrition(5).saturationModifier(.6F).build());return stack;
    }
    @Test void foodSaleLeavesThePersonalEdibleReserveAndIsOnlyARead() {
        var bag=new SimpleContainer(bread(3),new ItemStack(Items.WHEAT,64));
        assertEquals(-1,MarketGoods.offer(bag,ItemStack.EMPTY,false,MarketGoods.Good.FOOD));
        bag.getItem(0).grow(1);
        assertEquals(0,MarketGoods.offer(bag,ItemStack.EMPTY,false,MarketGoods.Good.FOOD));assertEquals(4,bag.countItem(Items.BREAD));
    }
    @Test void rawGrainDoesNotAuthorizeSellingTheLastEdibleRation() {
        var bag=new SimpleContainer(bread(1),new ItemStack(Items.WHEAT,64),new ItemStack(Items.EMERALD,64));
        assertEquals(-1,MarketGoods.offer(bag,ItemStack.EMPTY,true,MarketGoods.Good.FOOD));
    }
    @Test void aFarmerDoesNotPayForASingleRootItMustKeepInsteadOfEat() {
        var carrot=new ItemStack(Items.CARROT);carrot.set(DataComponents.FOOD,new FoodProperties.Builder().nutrition(3).saturationModifier(.6F).build());
        var received=new SimpleContainer(carrot);
        assertFalse(MarketGoods.accepts(received,true,MarketGoods.Good.FOOD));
        assertTrue(MarketGoods.accepts(received,false,MarketGoods.Good.FOOD));
        carrot.grow(1);assertTrue(MarketGoods.accepts(received,true,MarketGoods.Good.FOOD));
        assertFalse(MarketGoods.accepts(new SimpleContainer(new ItemStack(Items.WHEAT,64)),true,MarketGoods.Good.FOOD));
    }
    private static ItemStack ration(net.minecraft.world.item.Item item,int count,int nutrition) {
        var stack=new ItemStack(item,count);stack.set(DataComponents.FOOD,new FoodProperties.Builder().nutrition(nutrition).saturationModifier(.6F).build());return stack;
    }
    @Test void smallFoodIsACompleteMealSizedBundleWithTheSellersReserveRetained() {
        var seller=new SimpleContainer(ration(Items.BEETROOT,20,1),ItemStack.EMPTY);
        var buyer=new SimpleContainer(new ItemStack(Items.EMERALD),ItemStack.EMPTY);
        var plan=MarketGoods.quote(buyer,true,seller,ItemStack.EMPTY,true,MarketGoods.Good.FOOD,1);
        assertNotNull(plan);assertEquals(5,plan.goods.getCount());
        assertEquals(20,seller.countItem(Items.BEETROOT));assertEquals(1,buyer.countItem(Items.EMERALD));
        assertTrue(TradeMaterials.commit(buyer,seller,plan));assertEquals(15,seller.countItem(Items.BEETROOT));
        assertEquals(5,buyer.countItem(Items.BEETROOT));assertEquals(0,FoodSupply.deficit(seller,true));
    }
    @Test void rootBundlesLeaveARealMealAndAPlantingRootAtTheBuyer() {
        var seller=new SimpleContainer(ration(Items.CARROT,8,3),ItemStack.EMPTY);
        var buyer=new SimpleContainer(new ItemStack(Items.EMERALD,2),ItemStack.EMPTY);
        var plan=MarketGoods.quote(buyer,true,seller,ItemStack.EMPTY,true,MarketGoods.Good.FOOD,2);
        assertNotNull(plan);assertEquals(2,plan.goods.getCount());
        assertTrue(TradeMaterials.commit(buyer,seller,plan));assertEquals(6,seller.countItem(Items.CARROT));
        assertEquals(2,buyer.countItem(Items.CARROT));assertEquals(1,FoodInventory.availableCount(buyer.getItem(0),true));
        assertEquals(0,FoodSupply.deficit(seller,true));
    }
    @Test void aSmallerFittingFoodBundleCanSucceedWhenTheBreadReceiptCannotFit() {
        var buyer=new SimpleContainer(new ItemStack(Items.EMERALD,2),ration(Items.CARROT,1,3));
        var seller=new SimpleContainer(bread(4),ration(Items.CARROT,64,3),ItemStack.EMPTY);
        var plan=MarketGoods.quote(buyer,true,seller,ItemStack.EMPTY,true,MarketGoods.Good.FOOD,1);
        assertNotNull(plan);assertTrue(plan.goods.is(Items.CARROT));assertEquals(2,plan.goods.getCount());
        assertEquals(2,buyer.countItem(Items.EMERALD));assertEquals(1,buyer.countItem(Items.CARROT));
        assertTrue(TradeMaterials.commit(buyer,seller,plan));assertEquals(1,buyer.countItem(Items.EMERALD));assertEquals(3,buyer.countItem(Items.CARROT));
    }
    @Test void extraMoneyCannotBuyTheSellersRetainedPlantingRootOrFoodReserve() {
        var buyer=new SimpleContainer(new ItemStack(Items.EMERALD,64),ItemStack.EMPTY);
        var seller=new SimpleContainer(bread(3),ration(Items.CARROT,1,3),ItemStack.EMPTY);
        assertNull(MarketGoods.quote(buyer,true,seller,ItemStack.EMPTY,true,MarketGoods.Good.FOOD,2));
        assertEquals(64,buyer.countItem(Items.EMERALD));assertEquals(1,seller.countItem(Items.CARROT));assertEquals(3,seller.countItem(Items.BREAD));
    }
}
