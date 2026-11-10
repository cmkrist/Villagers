package local.villagers.navigation;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkBudgetTest {
    @Test void earlierTickingActorCannotRepeatedlyOvertakeWaitingNeighbour() {
        var budget = new WorkBudget(); var first = UUID.randomUUID(); var second = UUID.randomUUID();
        assertTrue(budget.admit(first, 0));
        assertFalse(budget.admit(second, 1));
        assertFalse(budget.admit(first, 10));
        assertTrue(budget.admit(second, 11));
        assertFalse(budget.admit(second, 21));
        assertTrue(budget.admit(first, 22));
    }

    @Test void abandonedAndUnloadedWorkersCannotHoldTheQueueForever() {
        var budget = new WorkBudget(); var first = UUID.randomUUID(); var abandoned = UUID.randomUUID(); var active = UUID.randomUUID();
        budget.admit(first, 0); budget.admit(abandoned, 1);
        assertFalse(budget.admit(active, 10));
        assertFalse(budget.admit(active, 70));
        assertTrue(budget.admit(active, 81));
        budget.admit(abandoned, 82); budget.forget(abandoned);
        assertTrue(budget.inspect(91));
    }

    @Test void diagnosticsNeverOvertakeOrQueueAndClockRewindClearsOldReservations() {
        var budget = new WorkBudget(); var a = UUID.randomUUID(); var b = UUID.randomUUID();
        assertTrue(budget.inspect(100));
        assertFalse(budget.inspect(101)); assertEquals(0, budget.waiting());
        assertFalse(budget.admit(a, 102));
        assertFalse(budget.inspect(110));
        assertTrue(budget.admit(b, 0)); assertEquals(0, budget.waiting());
    }

    @Test void queueIsBoundedAndRepeatedRequestsKeepTheirPosition() {
        var budget = new WorkBudget(); budget.inspect(0);
        UUID first = new UUID(0, 1);
        for (int i = 1; i <= WorkBudget.MAX_WAITERS; i++) assertFalse(budget.admit(new UUID(0, i), 1));
        assertFalse(budget.admit(UUID.randomUUID(), 2));
        assertEquals(WorkBudget.MAX_WAITERS, budget.waiting());
        assertFalse(budget.admit(first, 3));
        assertTrue(budget.admit(first, 10));
    }

    @Test void staggeredVillageGetsServiceDespiteFixedEntityTickOrder() {
        var budget = new WorkBudget(); var served = new int[8];
        for (int tick = 0; tick < 1600; tick++) for (int actor = 0; actor < served.length; actor++)
            if (tick % 10 == actor && budget.admit(new UUID(0, actor), tick)) served[actor]++;
        for (int count : served) assertTrue(count >= 10, "Every continuously requesting actor receives planning time");
    }

    @Test void queuedTripsGetTheirFirstPathWithoutFalselyFailingForLackOfMovement() {
        var budget=new WorkBudget();var trips=new WorkJourney[WorkBudget.MAX_WAITERS];var admitted=new boolean[trips.length];var done=new boolean[trips.length];int failures=0;
        for(int i=0;i<trips.length;i++)trips[i]=new WorkJourney();
        for(int tick=0;tick<WorkJourney.QUEUE_TIMEOUT;tick++)for(int i=0;i<trips.length;i++) {
            if(done[i] || tick%10!=i%10)continue;
            var step=trips[i].advance("farm",tick,admitted[i],0,64,0);
            if(step==WorkJourney.Step.FAILED)failures++;
            if(step==WorkJourney.Step.ARRIVED) {done[i]=true;continue;}
            if(budget.admit(new UUID(0,i),tick)) {
                admitted[i]=true;
                assertTrue(trips[i].navigating("farm",tick,0,64,0));
            }
        }
        assertEquals(0,failures,"CPU queue waiting is not a physical route failure");
        for(boolean arrived:done)assertTrue(arrived);
    }
}
