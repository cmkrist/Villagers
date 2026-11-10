package local.villagers.training;

import org.junit.jupiter.api.Test;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class TrainingConfigTest {
    @Test void ordinaryPlayDefaultsToNormalSpeedAndCannotAutoramp() {
        var defaults = TrainingConfig.fromProperties(new Properties());
        assertEquals(TrainingConfig.Role.PLAY, defaults.role());
        assertEquals(20, defaults.initialTps());
        assertEquals(20, new TrainingConfig(80, TrainingConfig.Role.PLAY, "adaptive").initialTps());
    }
    @Test void invalidRatesAndMislabeledControlRunsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new TrainingConfig(1000, TrainingConfig.Role.ADAPTIVE, "adaptive"));
        assertThrows(IllegalArgumentException.class, () -> new TrainingConfig(80, TrainingConfig.Role.FROZEN, "adaptive"));
        assertEquals(40, new TrainingConfig(80, TrainingConfig.Role.FROZEN, "frozen").initialTps());
        assertEquals(48_000, TrainingConfig.GAME_TICK_BUDGET);
    }
    @Test void accelerationRequiresSustainedCapacityAndExplicitConfiguredPermission() {
        var pacing = new PacingMonitor();
        assertEquals(PacingMonitor.Decision.HOLD, pacing.observe(80, 40, 40, 4, 7, 2.5));
        assertEquals(PacingMonitor.Decision.HOLD, pacing.observe(40, 40, 40, 4, 7, 5));
        assertEquals(PacingMonitor.Decision.HOLD, pacing.observe(80, 40, 40, 4, 7, 5));
        assertEquals(PacingMonitor.Decision.PROMOTE_TO_80, pacing.observe(80, 40, 40, 4, 7, 5));
    }
    @Test void persistentUnderachievementBacksOffEvenWhenTickCostLooksCheap() {
        var pacing = new PacingMonitor();
        assertEquals(PacingMonitor.Decision.HOLD, pacing.observe(80, 80, 50, 3, 5, 5));
        assertEquals(PacingMonitor.Decision.BACK_OFF_TO_20, pacing.observe(80, 80, 50, 3, 5, 5));
    }
    @Test void percentilesAndCostOverrunsExcludeEmptyNativeRingSlots() {
        var stats = TickStatistics.summarize(new long[]{0, 1_000_000, 2_000_000, 3_000_000, 40_000_000}, 40);
        assertEquals(4, stats.samples());
        assertEquals(2, stats.p50Mspt());
        assertEquals(40, stats.p95Mspt());
        assertEquals(1, stats.overBudgetTicks());
    }
}
