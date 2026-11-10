package local.villagers.lab;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ObservationBudgetTest {
    @Test void loadedTickBudgetFiresExactlyOnceAtBoundary() {
        var budget = new ObservationBudget(600);
        assertFalse(budget.expired(20_000));
        assertFalse(budget.expired(20_599));
        assertTrue(budget.expired(20_600));
        assertFalse(budget.expired(20_601));
        assertEquals(20_000, budget.started());
    }
    @Test void defaultInteractivePlayHasNoAutomaticStop() {
        var budget = new ObservationBudget(0);
        assertFalse(budget.expired(0)); assertFalse(budget.expired(Long.MAX_VALUE));
    }
    @Test void newWorldOrClockRollbackStartsAFreshMeasuredInterval() {
        var budget = new ObservationBudget(100);
        budget.expired(1000); budget.expired(1050);
        assertFalse(budget.expired(100)); assertTrue(budget.expired(200));
        budget.reset(); assertFalse(budget.expired(1000)); assertTrue(budget.expired(1100));
    }
    @Test void invalidBudgetsCannotSilentlyBecomeUnboundedOrInstantStops() {
        for (int value : new int[]{-1, 1, 99, 12_001}) assertThrows(IllegalArgumentException.class, () -> new ObservationBudget(value));
    }
}
