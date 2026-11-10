package local.villagers.navigation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkJourneyTest {
    @Test void successfulReplanningWithoutMovementStillStallsAndBacksOff() {
        var trip = new WorkJourney();
        assertEquals(WorkJourney.Step.START, trip.advance("remembered farm", 0, false, 0, 64, 0));
        assertTrue(trip.navigating("remembered farm",0,0,64,0));
        for (int tick = 10; tick < 80; tick += 10)
            assertEquals(WorkJourney.Step.TRAVEL, trip.advance("remembered farm", tick, false, 0, 64, 0));
        assertEquals(WorkJourney.Step.FAILED, trip.advance("remembered farm", 80, false, 0, 64, 0));
        assertEquals(WorkJourney.Step.WAIT, trip.advance("remembered farm", 479, false, 0, 64, 0));
        assertEquals(WorkJourney.Step.START, trip.advance("remembered farm", 480, false, 0, 64, 0));
    }
    @Test void movementCannotExtendTotalTripAndArrivalDoesNotCreateAnotherImmediateTrip() {
        var trip = new WorkJourney();
        trip.advance("farm", 0, false, 0, 64, 0);
        trip.navigating("farm",0,0,64,0);
        for (int tick = 10; tick < 400; tick += 10)
            assertEquals(WorkJourney.Step.TRAVEL, trip.advance("farm", tick, false, tick / 10., 64, 0));
        assertEquals(WorkJourney.Step.FAILED, trip.advance("farm", 400, false, 40, 64, 0));
        var arrived = new WorkJourney();
        arrived.advance("farm", 0, false, 0, 64, 0);
        assertEquals(WorkJourney.Step.ARRIVED, arrived.advance("farm", 20, true, 2, 64, 0));
        assertEquals(WorkJourney.Step.WAIT, arrived.advance("farm", 21, true, 2, 64, 0));
    }
    @Test void safetyInterruptionDropsOldJourneyAndClockResetCannotLockOutFutureWork() {
        var trip = new WorkJourney();
        trip.advance("farm", 100, false, 0, 64, 0);
        trip.interrupt(110);
        assertEquals(WorkJourney.Step.WAIT, trip.advance("farm", 120, false, 0, 64, 0));
        assertEquals(WorkJourney.Step.START, trip.advance("farm", 150, false, 0, 64, 0));
        assertEquals(WorkJourney.Step.START, trip.advance("farm", 1, false, 0, 64, 0));
    }

    @Test void queueWaitDoesNotPretendTheActorHasWalkedOrStalled() {
        var trip = new WorkJourney();
        trip.advance("farm",0,false,0,64,0);
        for(int tick=10;tick<=200;tick+=10)
            assertEquals(WorkJourney.Step.TRAVEL,trip.advance("farm",tick,false,0,64,0));
        assertTrue(trip.awaitingPath());
        assertTrue(trip.navigating("farm",200,0,64,0));
        assertFalse(trip.awaitingPath());
        for(int tick=210;tick<280;tick+=10) {
            assertEquals(WorkJourney.Step.TRAVEL,trip.advance("farm",tick,false,0,64,0));
            assertTrue(trip.navigating("farm",tick,0,64,0),"Repeated admitted replanning cannot postpone physical stall");
        }
        assertEquals(WorkJourney.Step.FAILED,trip.advance("farm",280,false,0,64,0));
    }

    @Test void queueWaitingIsBoundedAndStaleDispatchCannotResurrectInterruptedWork() {
        var trip=new WorkJourney();trip.advance("farm",0,false,0,64,0);
        assertEquals(WorkJourney.Step.FAILED,trip.advance("farm",WorkJourney.QUEUE_TIMEOUT,false,0,64,0));
        assertFalse(trip.navigating("farm",WorkJourney.QUEUE_TIMEOUT,0,64,0));
        trip.advance("farm",3000,false,0,64,0);trip.interrupt(3001);
        assertFalse(trip.navigating("farm",3000,0,64,0));
        trip.advance("new farm",3050,false,0,64,0);
        assertFalse(trip.navigating("farm",3050,0,64,0));
        assertFalse(trip.navigating("new farm",3040,0,64,0));
    }

    @Test void fullTravelDeadlineStartsAtDispatchAndCannotBeExtendedByReplanning() {
        var trip=new WorkJourney();trip.advance("farm",0,false,0,64,0);
        trip.advance("farm",200,false,0,64,0);trip.navigating("farm",200,0,64,0);
        for(int tick=210;tick<600;tick+=10) {
            assertEquals(WorkJourney.Step.TRAVEL,trip.advance("farm",tick,false,(tick-200)/10.,64,0));
            trip.navigating("farm",tick,(tick-200)/10.,64,0);
        }
        assertEquals(WorkJourney.Step.FAILED,trip.advance("farm",600,false,40,64,0));
    }
}
