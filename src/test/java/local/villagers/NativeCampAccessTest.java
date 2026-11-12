package local.villagers;

import java.util.*;
import net.minecraft.core.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeCampAccessTest {
    @Test void aSmallEntranceRetainsTheFloorAndNeverIncludesItsTargetBunk(){
        var bed=new BlockPos(-664,65,-630);
        for(var outward:Direction.Plane.HORIZONTAL){
            var outer=bed.below().relative(outward,2);var inner=outer.relative(outward.getOpposite());
            var cells=NativeCampAccess.cells(outer,outward);var unique=new HashSet<>(cells);
            assertEquals(6,unique.size());assertTrue(cells.stream().allMatch(p->p.getY()>=64&&p.getY()<=66));
            assertFalse(unique.contains(outer.below()));assertFalse(unique.contains(inner.below()));
            assertFalse(unique.contains(bed));assertFalse(unique.contains(bed.below()));
            assertTrue(unique.contains(outer.above(2)),"entry jump has head clearance");
            assertTrue(unique.contains(inner.above(2)),"raised bunk requires head clearance through the inner wall too");
        }
    }
}
