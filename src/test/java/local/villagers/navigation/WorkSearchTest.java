package local.villagers.navigation;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkSearchTest {
    @Test void aReachableFourthStandSurvivesThreeRejectedPathsAndTheNextAdmission() {
        var search=new WorkSearch<Integer>();var attempted=new ArrayList<Integer>();
        var mutable=new ArrayList<>(List.of(1,2,3,4));search.begin(mutable);mutable.clear();
        assertEquals(WorkSearch.Step.SEARCHING,search.advance(n->true,n->{attempted.add(n);return n==4;}));
        assertEquals(List.of(1,2,3),attempted);assertTrue(search.active());
        assertEquals(WorkSearch.Step.FOUND,search.advance(n->true,n->{attempted.add(n);return n==4;}));
        assertEquals(List.of(1,2,3,4),attempted);assertFalse(search.active());
    }
    @Test void changedStandEligibilityAndInterruptionDoNotQueryStaleCandidates() {
        var search=new WorkSearch<Integer>();var attempted=new ArrayList<Integer>();
        search.begin(List.of(1,2,3,4,5));
        assertEquals(WorkSearch.Step.SEARCHING,search.advance(n->true,n->{attempted.add(n);return false;}));
        assertEquals(WorkSearch.Step.EXHAUSTED,search.advance(n->false,n->{fail("Changed stand must not reach native pathfinding");return true;}));
        search.begin(List.of(8,9));search.clear();
        assertEquals(WorkSearch.Step.EXHAUSTED,search.advance(n->true,n->{fail("Interrupted search must not query");return true;}));
        assertEquals(List.of(1,2,3),attempted);
    }
    @Test void wholeSearchIsFiniteAndEachAdmissionKeepsItsNativeQueryBound() {
        var search=new WorkSearch<Integer>();var counts=new int[1];search.begin(IntStream.range(0,343).boxed().toList());
        int batches=0;
        while(search.active()) {
            int before=counts[0];search.advance(n->true,n->{counts[0]++;return false;});
            assertTrue(counts[0]-before<=3);batches++;assertTrue(batches<=115);
        }
        assertEquals(343,counts[0]);assertEquals(115,batches);
        assertThrows(IllegalArgumentException.class,()->search.begin(IntStream.range(0,344).boxed().toList()));
    }
}
