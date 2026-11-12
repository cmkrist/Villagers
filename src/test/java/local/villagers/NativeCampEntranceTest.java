package local.villagers;

import java.util.*;
import net.minecraft.core.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeCampEntranceTest {
    @Test void aTwoLayerEntranceClearsTheHeadBeforeTheFootAndRetainsFloorAndLintel(){
        var outer=new BlockPos(-612,65,-602);var voice=new BlockPos(-633,64,-602);
        for(var direction:Direction.Plane.HORIZONTAL){
            var p=new NativeCampEntrance.Plan(outer,direction,2,voice);var cells=NativeCampEntrance.cells(p);
            assertEquals(6,new HashSet<>(cells).size());
            for(int i=0;i<2;i++){
                var base=outer.relative(direction,i);
                assertFalse(cells.contains(base.below()));assertFalse(cells.contains(base.above(3)));
                assertTrue(cells.indexOf(base.above())<cells.indexOf(base));
                assertTrue(cells.indexOf(base.above(2))<cells.indexOf(base));
            }
        }
    }
    @Test void thePlanCannotBecomeATunnelOrAVerticalShaft(){
        var p=BlockPos.ZERO;
        assertThrows(IllegalArgumentException.class,()->new NativeCampEntrance.Plan(p,Direction.WEST,3,p));
        assertThrows(IllegalArgumentException.class,()->new NativeCampEntrance.Plan(p,Direction.UP,1,p));
    }
}
