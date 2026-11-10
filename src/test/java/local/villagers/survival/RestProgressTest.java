package local.villagers.survival;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RestProgressTest {
    @Test void eightyConsecutiveSafeQuietTicksGiveOneBoutAndCannotReplayIt() {
        var rest = new RestProgress();
        for (long tick = 0; tick < 80; tick += 10) assertFalse(rest.quiet("ground", tick, true, 1, 70, 2));
        assertTrue(rest.quiet("ground", 80, true, 1, 70, 2));
        assertFalse(rest.quiet("ground", 80, true, 1, 70, 2));
        assertFalse(rest.quiet("ground", 90, true, 1, 70, 2));
    }
    @Test void walkingToABedCannotCountAsGroundRestAfterArrival() {
        var rest = new RestProgress();
        for (long tick = 0; tick <= 200; tick += 10) assertFalse(rest.quiet("ground", tick, true, tick*.1, 70, 2));
        for (long tick = 210; tick < 280; tick += 10) assertFalse(rest.quiet("ground", tick, true, 20, 70, 2));
        assertTrue(rest.quiet("ground", 280, true, 20, 70, 2));
    }
    @Test void dangerOrUnsafeFootingInterruptsTheWholeQuietInterval() {
        var rest = new RestProgress();
        for (long tick = 0; tick <= 70; tick += 10) assertFalse(rest.quiet("ground", tick, true, 1, 70, 2));
        assertFalse(rest.quiet("ground", 80, false, 1, 70, 2));
        for (long tick = 90; tick < 170; tick += 10) assertFalse(rest.quiet("ground", tick, true, 1, 70, 2));
        assertTrue(rest.quiet("ground", 170, true, 1, 70, 2));
    }
    @Test void otherWorkBedChangesAndClockRollbackDoNotCompleteOldRest() {
        var rest = new RestProgress();
        assertFalse(rest.quiet("bed:a", 100, true, 1, 70, 2));
        assertFalse(rest.quiet("bed:a", 170, true, 1, 70, 2), "unobserved work gap restarts interval");
        assertFalse(rest.quiet("bed:b", 180, true, 1, 70, 2));
        assertFalse(rest.quiet("bed:b", 10, true, 1, 70, 2));
        rest.reset(); assertFalse(rest.quiet("bed:b", 90, true, 1, 70, 2));
    }
    @Test void invalidPhysicalObservationsAreRejected() {
        var rest = new RestProgress();
        assertThrows(IllegalArgumentException.class, () -> rest.quiet("ground", -1, true, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> rest.quiet("ground", 0, true, Double.NaN, 0, 0));
    }
}
