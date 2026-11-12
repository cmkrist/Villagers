package local.villagers;
import java.util.*;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NativeFoodStorageRouteTest {
    @Test void roundedNativeStartRetainsTheActualPartialHeightBodyOnlyAtTheSameStartingCell(){
        var actor=new BlockPos(-440,64,-823);var start=new BlockPos(-440,65,-823);
        assertTrue(NativeFoodStorage.actualStartFooting(0,start,actor,64.5));
        assertTrue(NativeFoodStorage.actualStartFooting(0,actor,actor,64));
        assertFalse(NativeFoodStorage.actualStartFooting(1,start,actor,64.5));
        assertFalse(NativeFoodStorage.actualStartFooting(0,start.offset(1,0,0),actor,64.5));
        assertFalse(NativeFoodStorage.actualStartFooting(0,start.above(),actor,64.5));
        assertFalse(NativeFoodStorage.actualStartFooting(0,start,actor,Double.NaN));
    }
    @Test void aHealthyFoodTripCanCrossTheSingleRecordedShallowWaterCellButNotAChannel(){
        assertTrue(NativeFoodStorage.waterBudget(false,0));
        assertTrue(NativeFoodStorage.waterBudget(false,1));
        assertFalse(NativeFoodStorage.waterBudget(false,2));
        assertTrue(NativeFoodStorage.waterBudget(true,0));
        assertFalse(NativeFoodStorage.waterBudget(true,1));
        assertFalse(NativeFoodStorage.waterBudget(false,-1));
    }

    @Test void repeatedFoodSearchesEventuallyTryStandsBeyondTheNearestUnreachableFour(){
        var state=new NativeFoodStorage.State();var store=new BlockPos(10,64,10);
        var stands=java.util.stream.IntStream.range(0,11).mapToObj(x->new BlockPos(x,64,0)).toList();
        var tried=new HashSet<BlockPos>();
        for(int i=0;i<3;i++)tried.addAll(state.nextStands(store,stands));
        assertEquals(new HashSet<>(stands),tried);
        assertEquals(4,state.nextStands(store,stands).size());
    }
    @Test void smallAndChangingCandidateSetsRemainBounded(){
        var state=new NativeFoodStorage.State();var store=BlockPos.ZERO;
        assertTrue(state.nextStands(store,List.of()).isEmpty());
        assertEquals(List.of(store),state.nextStands(store,List.of(store)));
        for(int i=0;i<25;i++)state.nextStands(new BlockPos(i,64,0),List.of(store));
        assertEquals(16,state.standCursor.size());
        assertEquals(List.of(store),state.nextStands(store,List.of(store)));
    }
}
