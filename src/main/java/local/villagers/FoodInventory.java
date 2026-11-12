package local.villagers;

import local.villagers.mind.MealPolicy;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Read-only food selection. Ordinary meals preserve planting stock; survival may spend an owned root. */
final class FoodInventory {
    private FoodInventory() {}

    static boolean supported(ItemStack item) {
        return item.has(DataComponents.FOOD) && (item.is(Items.BREAD) || item.is(Items.CARROT)
                || item.is(Items.POTATO) || item.is(Items.BEETROOT)||item.is(Items.APPLE)||item.is(Items.BAKED_POTATO)||item.is(Items.PUMPKIN_PIE));
    }

    static boolean fits(ItemStack item, int food, float health, float maxHealth) {
        var properties = item.get(DataComponents.FOOD);
        return supported(item) && properties != null
                && MealPolicy.shouldEat(food, properties.nutrition(), health, maxHealth);
    }

    static int availableCount(ItemStack item, boolean farmer) {
        return Math.max(0, item.getCount() - (farmer && (item.is(Items.CARROT) || item.is(Items.POTATO)) ? 1 : 0));
    }

    /** Factual possession differs from a meal one is able or willing to spend. Only inspect the owner's own bag. */
    static boolean containsFood(Container inventory) {
        for(int slot=0;slot<inventory.getContainerSize();slot++)
            if(inventory.getItem(slot).has(DataComponents.FOOD))return true;
        return false;
    }

    static int anySlot(Container inventory, boolean farmer) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (supported(item) && availableCount(item, farmer) > 0) return i;
        }
        return -1;
    }

    static int sharingSlot(Container inventory,boolean farmer) {
        return sharingSlot(inventory,farmer,one -> true);
    }

    /** One-item offers favour useful meals. The recipient answers capacity only during actual contact. */
    static int sharingSlot(Container inventory,boolean farmer,java.util.function.Predicate<ItemStack> canReceive) {
        int best=-1,nutrition=0;
        for(int slot=0;slot<inventory.getContainerSize();slot++) {
            var stack=inventory.getItem(slot);
            if(!supported(stack) || availableCount(stack,farmer)<1)continue;
            var food=stack.get(DataComponents.FOOD);
            if(food!=null && food.nutrition()>nutrition && canReceive.test(stack.copyWithCount(1))) {
                best=slot;nutrition=food.nutrition();
            }
        }
        return best;
    }

    /** At zero food and critical health, living to replant takes precedence.
     * Prefer every ordinary ration before the actor's last actual planting root.
     * Selection itself never creates or removes an item. */
    static int survivalMealSlot(Container inventory,boolean farmer,int food,float health,float maxHealth){
        int ordinary=mealSlot(inventory,farmer,food,health,maxHealth);
        if(ordinary>=0||!farmer||food!=0||!Float.isFinite(health)||!Float.isFinite(maxHealth)
                ||health<=0||maxHealth<=0||health>maxHealth*.25f)return ordinary;
        for(int i=0;i<inventory.getContainerSize();i++){
            var item=inventory.getItem(i);
            if((item.is(Items.CARROT)||item.is(Items.POTATO))&&item.getCount()>0&&fits(item,food,health,maxHealth))return i;
        }
        return -1;
    }

    /** One last planting root may answer an understood request from a visibly
     * dying neighbour. This never changes ordinary meals or sharing reserves. */
    static int emergencyRootGiftSlot(Container bag,boolean farmer,int donorFood,float donorHealth,float donorMax,
            float receiverHealth,float receiverMax){
        if(!farmer||donorFood<14||donorFood>20||!Float.isFinite(donorHealth)||!Float.isFinite(donorMax)
            ||!Float.isFinite(receiverHealth)||!Float.isFinite(receiverMax)||donorMax<=0||receiverMax<=0
            ||donorHealth<donorMax*.5f||donorHealth>donorMax||receiverHealth<=0||receiverHealth>receiverMax*.25f)return -1;
        for(int i=0;i<bag.getContainerSize();i++){
            var item=bag.getItem(i);
            if(item.getCount()==1&&(item.is(Items.CARROT)||item.is(Items.POTATO))&&!item.has(DataComponents.CUSTOM_NAME))return i;
        }
        return -1;
    }

    static int mealSlot(Container inventory, boolean farmer, int food, float health, float maxHealth) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (availableCount(item, farmer) > 0 && fits(item, food, health, maxHealth)) return i;
        }
        return -1;
    }
}
