package local.villagers;

import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** One server-thread exchange of possessed goods for possessed emerald items. No wallet or created output. */
final class TradeMaterials {
    static final int MAX_QUANTITY=8;
    static final class Plan {
        final List<ItemStack> buyerBefore,sellerBefore;
        final SimpleContainer buyerAfter,sellerAfter;
        final ItemStack goods;
        final int price;
        private final Container buyerOwner,sellerOwner;
        private boolean used;
        private Plan(Container buyer,Container seller,SimpleContainer buyerAfter,SimpleContainer sellerAfter,ItemStack goods,int price) {
            buyerBefore=HarvestMaterials.snapshot(buyer);sellerBefore=HarvestMaterials.snapshot(seller);
            buyerOwner=buyer;sellerOwner=seller;
            this.buyerAfter=buyerAfter;this.sellerAfter=sellerAfter;this.goods=goods.copy();this.price=price;
        }
    }
    private TradeMaterials() {}
    private static SimpleContainer copy(Container bag) {
        var out=new SimpleContainer(bag.getContainerSize());
        for(int i=0;i<bag.getContainerSize();i++)out.setItem(i,bag.getItem(i).copy());
        return out;
    }
    /** SimpleContainer's empty-slot shortcut can store an oversized input; split real receipts explicitly. */
    private static boolean insert(SimpleContainer bag,ItemStack incoming) {
        var remaining=incoming.copy();int maximum=Math.min(bag.getMaxStackSize(),incoming.getMaxStackSize());
        if(maximum<1)return false;
        for(int i=0;i<bag.getContainerSize() && !remaining.isEmpty();i++) {
            var stack=bag.getItem(i);
            if(!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack,remaining)) {
                int n=Math.min(remaining.getCount(),Math.max(0,maximum-stack.getCount()));
                stack.grow(n);remaining.shrink(n);
            }
        }
        for(int i=0;i<bag.getContainerSize() && !remaining.isEmpty();i++)if(bag.getItem(i).isEmpty())
            bag.setItem(i,remaining.split(Math.min(maximum,remaining.getCount())));
        return remaining.isEmpty();
    }
    static Plan reserve(Container buyer,Container seller,int slot,int price) {
        return reserve(buyer,seller,slot,1,price);
    }
    /** A single commodity can span slots only when its actual components match exactly. */
    static SimpleContainer withoutGoods(Container seller,int slot,int quantity) {
        if(quantity<1 || quantity>MAX_QUANTITY || slot<0 || slot>=seller.getContainerSize())return null;
        var prototype=seller.getItem(slot);
        if(prototype.isEmpty() || prototype.is(Items.EMERALD))return null;
        var after=copy(seller);int owed=quantity;
        for(int i=0;i<after.getContainerSize() && owed>0;i++)if(ItemStack.isSameItemSameComponents(prototype,after.getItem(i))) {
            int n=Math.min(owed,after.getItem(i).getCount());after.removeItem(i,n);owed-=n;
        }
        return owed==0?after:null;
    }
    static Plan reserve(Container buyer,Container seller,int slot,int quantity,int price) {
        if(buyer==seller || price<1 || price>2 || slot<0 || slot>=seller.getContainerSize())return null;
        var b=withoutGoods(seller,slot,quantity);if(b==null)return null;
        var goods=seller.getItem(slot).copyWithCount(quantity);
        var a=copy(buyer);int owed=price;
        var payment=new java.util.ArrayList<ItemStack>();
        for(int i=0;i<a.getContainerSize() && owed>0;i++)if(a.getItem(i).is(Items.EMERALD)) {
            int n=Math.min(owed,a.getItem(i).getCount());payment.add(a.removeItem(i,n));owed-=n;
        }
        if(owed!=0)return null;
        // Both receipts must fit, including any slots freed by the actual payment/offered item.
        if(!insert(a,goods))return null;
        for(var coins:payment)if(!insert(b,coins))return null;
        return new Plan(buyer,seller,a,b,goods,price);
    }
    static boolean commit(Container buyer,Container seller,Plan plan) {
        if(plan==null || plan.used || buyer!=plan.buyerOwner || seller!=plan.sellerOwner || buyer==seller || !HarvestMaterials.unchanged(buyer,plan.buyerBefore)
                || !HarvestMaterials.unchanged(seller,plan.sellerBefore))return false;
        plan.used=true;
        for(int i=0;i<buyer.getContainerSize();i++)buyer.setItem(i,plan.buyerAfter.getItem(i).copy());
        for(int i=0;i<seller.getContainerSize();i++)seller.setItem(i,plan.sellerAfter.getItem(i).copy());
        return true;
    }
}
