package local.villagers.family;

import java.util.List;
import java.util.UUID;
import local.villagers.mind.Lineage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BreedingPolicyTest {
    private static final UUID A = new UUID(1, 1), B = new UUID(2, 2), C = new UUID(3, 3), D = new UUID(4, 4);
    @Test void birthsRequireRealFoodAndKeepFarmersLastPlantingItem() {
        assertEquals(List.of(3), BreedingPolicy.foodCost(List.of(new BreedingPolicy.Stack(BreedingPolicy.Food.BREAD, 3, false))).orElseThrow());
        assertTrue(BreedingPolicy.foodCost(List.of(new BreedingPolicy.Stack(BreedingPolicy.Food.BREAD, 2, false))).isEmpty());
        assertTrue(BreedingPolicy.foodCost(List.of(new BreedingPolicy.Stack(BreedingPolicy.Food.ROOT, 12, true))).isEmpty());
        assertEquals(List.of(12), BreedingPolicy.foodCost(List.of(new BreedingPolicy.Stack(BreedingPolicy.Food.ROOT, 13, true))).orElseThrow());
        assertTrue(BreedingPolicy.foodCost(List.of(new BreedingPolicy.Stack(BreedingPolicy.Food.OTHER, 64, false))).isEmpty());
    }
    @Test void mixedStacksPayTheFullCostWithoutTakingOtherItems() {
        assertEquals(List.of(0, 2, 4), BreedingPolicy.foodCost(List.of(
                new BreedingPolicy.Stack(BreedingPolicy.Food.OTHER, 64, false),
                new BreedingPolicy.Stack(BreedingPolicy.Food.BREAD, 2, false),
                new BreedingPolicy.Stack(BreedingPolicy.Food.ROOT, 5, true))).orElseThrow());
    }
    @Test void physicalNeedsFearRelationshipAndCooldownGateWillingness() {
        assertTrue(BreedingPolicy.willing(1, 18, .2, false, .1, 100, 100));
        assertFalse(BreedingPolicy.willing(.5, 18, .2, false, .1, 100, 0));
        assertFalse(BreedingPolicy.willing(1, 10, .2, false, .1, 100, 0));
        assertFalse(BreedingPolicy.willing(1, 18, .9, false, .1, 100, 0));
        assertFalse(BreedingPolicy.willing(1, 18, .2, true, .1, 100, 0));
        assertFalse(BreedingPolicy.willing(1, 18, .2, false, -.1, 100, 0));
        assertFalse(BreedingPolicy.willing(1, 18, .2, false, .1, 100, 101));
    }
    @Test void nurserySafetyAndFertilityChangeAStillBoundedChance() {
        assertEquals(0, BreedingPolicy.chance(1, 1, 1, 1, 0));
        assertTrue(BreedingPolicy.chance(1, 1, 1, 1, 2) > BreedingPolicy.chance(0, 0, 0, 0, 1));
        assertEquals(.25, BreedingPolicy.chance(1, 1, 1, 1, 100));
    }
    @Test void parentChildAndSiblingsAreExcludedButUnrelatedFoundersAreNot() {
        Lineage first = new Lineage(A, "A", 1, false, B, "B");
        Lineage sibling = new Lineage(A, "A", 1, false, D, "D");
        assertTrue(BreedingPolicy.closeKin(C, first, A, null));
        assertTrue(BreedingPolicy.closeKin(C, first, D, sibling));
        assertFalse(BreedingPolicy.closeKin(A, null, B, null));
    }
}
