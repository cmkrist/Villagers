package local.villagers.mind;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MealPolicyTest {
    @Test void healthyReflexWaitsUntilNutritionFitsRatherThanWastingMostOfABread() {
        assertFalse(MealPolicy.shouldEat(19, 5, 20, 20));
        assertFalse(MealPolicy.shouldEat(16, 5, 20, 20));
        assertTrue(MealPolicy.shouldEat(15, 5, 20, 20));
        assertTrue(MealPolicy.shouldEat(17, 3, 20, 20));
        assertTrue(MealPolicy.shouldEat(19, 1, 20, 20));
    }
    @Test void injuryCanUseARealRationToRestoreRegenerationEligibility() {
        assertTrue(MealPolicy.shouldEat(17, 5, 12, 20));
        assertFalse(MealPolicy.shouldEat(18, 5, 12, 20), "already eligible for normal regeneration");
        assertFalse(MealPolicy.shouldEat(20, 5, 12, 20), "a full body still cannot eat");
    }
    @Test void urgentHungerDoesNotWaitForAnOversizedMealsNutritionToFit() {
        assertTrue(MealPolicy.shouldEat(6, 20, 20, 20));
        assertFalse(MealPolicy.shouldEat(7, 20, 20, 20));
    }
    @Test void deadOrInvalidBodiesAndNonfoodCannotTriggerAReflex() {
        assertFalse(MealPolicy.shouldEat(0, 5, 0, 20));
        assertFalse(MealPolicy.shouldEat(0, 0, 1, 20));
        assertFalse(MealPolicy.shouldEat(0, 5, Float.NaN, 20));
        assertFalse(MealPolicy.shouldEat(-1, 5, 1, 20));
    }
    @Test void possessedNutritionBeforeTheFinalStarvationTickPreventsAFalseLethalOrdering() {
        var late = new Metabolism(new Metabolism.State(0, 0, 0, 79));
        assertEquals(1, late.tick(1, 1, 20, true).starvationHits());
        var fed = new Metabolism(new Metabolism.State(0, 0, 0, 79));
        assertTrue(MealPolicy.shouldEat(fed.foodLevel(), 5, 1, 20));
        fed.eat(5, 6); // The adapter must first remove one real existing bread.
        var result = fed.tick(1, 1, 20, true);
        assertEquals(0, result.starvationHits()); assertEquals(0, result.healing());
        assertEquals(5, fed.foodLevel()); assertEquals(5, fed.saturation());
    }
}
