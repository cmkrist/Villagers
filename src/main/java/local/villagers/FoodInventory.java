package local.villagers;

import local.villagers.mind.MealPolicy;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Read-only inventory affordances; selecting a ration never consumes stock or the last planting root. */
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

    static int mealSlot(Container inventory, boolean farmer, int food, float health, float maxHealth) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (availableCount(item, farmer) > 0 && fits(item, food, health, maxHealth)) return i;
        }
        return -1;
    }
}
