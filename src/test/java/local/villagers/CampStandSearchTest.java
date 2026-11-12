package local.villagers;

import java.util.*;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CampStandSearchTest {
    @Test void evenOneNodeFailuresEventuallyGetTheSameBoundedLargerSearch(){
        var stands=IntStream.range(0,17).mapToObj(x->new BlockPos(x,64,0)).toList();
        var search=new CampBeds.StandSearch();var checked=new HashSet<BlockPos>();
        for(int call=0;call<6;call++){
            var batch=search.larger(stands,null);assertTrue(batch.size()<=3);
            assertTrue(Collections.disjoint(checked,batch));checked.addAll(batch);
        }
        assertEquals(new HashSet<>(stands),checked);
    }
    @Test void successiveBoundedCallsReachTheFartherStandsBehindTheNearestFailures(){
        var stands=IntStream.range(0,17).mapToObj(x->new BlockPos(x,64,0)).toList();
        var search=new CampBeds.StandSearch();var checked=new HashSet<BlockPos>();
        for(int call=0;call<3;call++){
            var batch=search.next(stands);assertTrue(batch.size()<=8);assertTrue(Collections.disjoint(checked,batch));checked.addAll(batch);
        }
        assertEquals(new HashSet<>(stands),checked);
        assertEquals(8,search.next(stands).size(),"After all failures a new bounded pass can recheck changed terrain");
    }
    @Test void aWorkingStandCanBeRetriedWithoutLosingCoverageOfOtherCandidates(){
        var stands=IntStream.range(0,17).mapToObj(x->new BlockPos(x,64,0)).toList();var search=new CampBeds.StandSearch();
        search.next(stands);search.preferred=stands.get(2);var second=search.next(stands);
        assertEquals(stands.get(2),second.getFirst());assertEquals(8,second.size());
        assertTrue(second.contains(stands.get(8)));assertTrue(search.next(stands).contains(stands.get(16)));
    }
}
