package local.villagers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;

/** Finite own-food preparation. Body work, stock and recipe inputs are independent of reproductive readiness. */
final class FoodSupply {
    static final int NUTRITION_TARGET = 15;
    static final int MAX_RAW_WHEAT = 9;
    private FoodSupply() {}

    /** Optional wood work yields at the existing urgent-meal/sprint threshold; food work stays available. */
    static boolean mayDoOptionalWork(int food) {
        return food > local.villagers.mind.Metabolism.SPRINT_FOOD_LEVEL
                && food <= local.villagers.mind.Metabolism.MAX_FOOD;
    }

    /** Invest in approved future food capacity while a real personal reserve still covers the body. */
    static boolean mayInvest(boolean canWork,int food,int reserveDeficit) {
        return canWork && food>=13 && food<=20 && reserveDeficit==0;
    }

    /** Positive native age is an adult birth cooldown, not a reason to stop eating or working. */
    static boolean mayWork(int nativeAge, float health, double fatigue, boolean safeDay) {
        return nativeAge >= 0 && Float.isFinite(health) && health > 0 && Double.isFinite(fatigue)
                && fatigue >= 0 && fatigue < .75 && safeDay;
    }

    /** Remaining nutritional reserve, using actual food components. No item snapshot, mutation or saturation guess. */
    static int deficit(Container inventory, boolean farmer) {
        return deficit(inventory, farmer, NUTRITION_TARGET);
    }

    /** Own possessed inputs can justify preparing a meal; an empty-handed neighbour cannot promise one. */
    static boolean canPrepare(Container inventory,boolean farmer) {
        return BreadCrafting.wheatCount(inventory)>=3
                || farmer && inventory.hasAnyMatching(PlantingMaterials::supported);
    }

    static int target(Session s, long now) {
        return NUTRITION_TARGET + s.foodDemand.extraNutrition(now);
    }

    static int deficit(Container inventory, boolean farmer, int target) {
        if (target < NUTRITION_TARGET || target > NUTRITION_TARGET + local.villagers.economy.FoodDemand.EXTRA_NUTRITION)
            throw new IllegalArgumentException("Invalid food reserve target");
        int missing = target;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (!FoodInventory.supported(stack)) continue;
            var food = stack.get(DataComponents.FOOD);
            if (food == null || food.nutrition() <= 0) continue;
            long nutrition = (long) FoodInventory.availableCount(stack, farmer) * food.nutrition();
            if (nutrition >= missing) return 0;
            missing -= (int) nutrition;
        }
        return missing;
    }

    static boolean update(Session s, boolean canWork, Container inventory, boolean farmer) {
        s.selfFoodGoal = canWork && deficit(inventory, farmer, target(s, s.lastTick)) > 0;
        return s.selfFoodGoal;
    }

    /** Only enough raw input for missing stock, capped per batch. Raw wheat itself is never edible stock. */
    static int wheatTarget(Container inventory, boolean farmer, int breadNutrition) {
        return wheatTarget(inventory, farmer, breadNutrition, NUTRITION_TARGET);
    }

    static int wheatTarget(Container inventory, boolean farmer, int breadNutrition, int target) {
        if (breadNutrition <= 0) return 0;
        int missing = deficit(inventory, farmer, target);
        return (int) Math.min(MAX_RAW_WHEAT, ((missing + (long) breadNutrition - 1) / breadNutrition) * 3);
    }
}
