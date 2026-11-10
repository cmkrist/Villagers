package local.villagers.navigation;

import org.junit.jupiter.api.Test;
import static local.villagers.navigation.WorkInterval.Phase.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkIntervalTest {
    @Test void travelTimeIsNotLaborAndCompletionIsEmittedOnce() {
        var work = new WorkInterval();
        assertEquals(TRAVEL, work.advance("job/revision/target", 0, false, 20));
        assertEquals(WORK, work.advance("job/revision/target", 10, true, 20));
        assertEquals(WORK, work.advance("job/revision/target", 20, true, 20));
        assertEquals(COMMIT, work.advance("job/revision/target", 30, true, 20));
        assertEquals(DONE, work.advance("job/revision/target", 30, true, 20));
        assertEquals(DONE, work.advance("job/revision/target", 40, true, 20));
    }

    @Test void safetyOrUnobservedTimeCannotCompleteAnOldInterval() {
        var work = new WorkInterval(); work.advance("job", 0, true, 20);
        work.advance("job", 10, true, 20); work.interrupt();
        assertEquals(WORK, work.advance("job", 20, true, 20));
        assertEquals(WORK, work.advance("job", 100, true, 20));
        assertEquals(WORK, work.advance("job", 110, true, 20));
        assertEquals(COMMIT, work.advance("job", 120, true, 20));
    }

    @Test void changedAuthorityTargetDurationAndLostReachRestartPhysicalWork() {
        var work = new WorkInterval(); work.advance("old", 0, true, 20);
        assertEquals(WORK, work.advance("new", 10, true, 20));
        assertEquals(WORK, work.advance("new", 20, true, 30));
        assertEquals(TRAVEL, work.advance("new", 30, false, 30));
        assertEquals(WORK, work.advance("new", 40, true, 30));
        assertEquals(WORK, work.advance("new", 0, true, 30));
    }

    @Test void restartReconstructsJobButNeverGrantsUnfinishedLabor() {
        var old = new WorkInterval(); old.advance("persisted job phase", 0, true, 20);
        old.advance("persisted job phase", 10, true, 20);
        var reopened = new WorkInterval();
        assertEquals(WORK, reopened.advance("persisted job phase", 200, true, 20));
        assertEquals(WORK, reopened.advance("persisted job phase", 210, true, 20));
        assertEquals(COMMIT, reopened.advance("persisted job phase", 220, true, 20));
    }
}
