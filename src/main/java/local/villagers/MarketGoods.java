package local.villagers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Own-inventory affordances, never used to discover partners. Each seller retains its own working reserve. */
final class MarketGoods {
    static final int MEAL_NUTRITION=5;
    enum Good { FOOD,HOE,WOOD }
    static local.villagers.language.Concept concept(Good good) {
        return switch(good) {case FOOD->local.villagers.language.Concept.FOOD;case HOE->local.villagers.language.Concept.HOE;case WOOD->local.villagers.language.Concept.WOOD;};
    }
    private MarketGoods() {}
    static boolean usableHoe(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ItemTags.HOES) && stack.isDamageableItem()
                && stack.getMaxDamage()-stack.getDamageValue()>=16;
    }
    static boolean accepts(Container after,boolean farmer,Good good) {
        return switch(good) {
            case FOOD->FoodInventory.anySlot(after,farmer)>=0;
            case HOE->after.hasAnyMatching(MarketGoods::usableHoe);
            case WOOD->WoodCrafting.next(after)!=null;
        };
    }
    private static int portion(ItemStack stack) {
        var food=stack.get(DataComponents.FOOD);
        return FoodInventory.supported(stack) && food!=null && food.nutrition()>0
                ?(int)((MEAL_NUTRITION+(long)food.nutrition()-1)/food.nutrition()):0;
    }
    private static boolean maySell(Container bag,int slot,int quantity,boolean farmer) {
        var prototype=bag.getItem(slot);int spendable=0;
        for(int i=0;i<bag.getContainerSize();i++)if(ItemStack.isSameItemSameComponents(prototype,bag.getItem(i)))
            spendable+=FoodInventory.availableCount(bag.getItem(i),farmer);
        if(spendable<quantity)return false;
        var after=TradeMaterials.withoutGoods(bag,slot,quantity);
        return after!=null && FoodSupply.deficit(after,farmer)==0;
    }
    /** Contact-only negotiation. Each bag answers its own payment/capacity and retained-root constraints. */
    static TradeMaterials.Plan quote(Container buyer,boolean buyerFarmer,Container seller,ItemStack held,boolean sellerFarmer,Good good,int price) {
        if(good!=Good.FOOD) {
            var plan=TradeMaterials.reserve(buyer,seller,offer(seller,held,sellerFarmer,good),price);
            return plan!=null && accepts(plan.buyerAfter,buyerFarmer,good)?plan:null;
        }
        TradeMaterials.Plan best=null;int nutrition=0;
        for(int i=0;i<seller.getContainerSize();i++) {
            var stack=seller.getItem(i);int minimum=portion(stack);var food=stack.get(DataComponents.FOOD);
            if(minimum<1 || food.nutrition()<=nutrition)continue;
            for(int q=minimum;q<=TradeMaterials.MAX_QUANTITY;q++) {
                if(!maySell(seller,i,q,sellerFarmer))break;
                var plan=TradeMaterials.reserve(buyer,seller,i,q,price);
                if(plan!=null && accepts(plan.buyerAfter,buyerFarmer,good)) {best=plan;nutrition=food.nutrition();break;}
            }
        }
        return best;
    }
    static int offer(Container bag,ItemStack held,boolean farmer,Good good) {
        if(good==Good.WOOD) {
            // One supported raw wood pays a complete wooden-hoe recipe chain; keep one for own maintenance.
            int count=0,slot=-1;
            for(int i=0;i<bag.getContainerSize();i++)if(WoodCrafting.raw(bag.getItem(i))) {
                count+=bag.getItem(i).getCount();if(slot<0)slot=i;
            }
            return count>1?slot:-1;
        }
        if(good==Good.FOOD) {
            int best=-1,nutrition=0;
            for(int i=0;i<bag.getContainerSize();i++) {
                var stack=bag.getItem(i);var food=stack.get(DataComponents.FOOD);
                int quantity=portion(stack);
                if(quantity<1 || food.nutrition()<=nutrition)continue;
                if(maySell(bag,i,quantity,farmer)) {best=i;nutrition=food.nutrition();}
            }
            return best;
        }
        // Keep the actual working hoe. A newly made inventory tool can then be sold to its customer.
        if(usableHoe(held)) {
            for(int i=0;i<bag.getContainerSize();i++)if(usableHoe(bag.getItem(i)))return i;
            return -1;
        }
        // With no working hoe equipped, retain the most durable usable inventory tool.
        int retained=-1,uses=usableHoe(held)?held.getMaxDamage()-held.getDamageValue():-1;
        for(int i=0;i<bag.getContainerSize();i++)if(usableHoe(bag.getItem(i))) {
            var stack=bag.getItem(i);int n=stack.getMaxDamage()-stack.getDamageValue();
            if(n>uses) {retained=i;uses=n;}
        }
        if(uses<0)return -1;
        for(int i=0;i<bag.getContainerSize();i++)if(usableHoe(bag.getItem(i)) && (i!=retained || bag.getItem(i).getCount()>1))return i;
        return -1;
    }
}
