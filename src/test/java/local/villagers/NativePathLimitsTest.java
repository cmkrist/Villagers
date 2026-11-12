package local.villagers;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativePathLimitsTest {
    @Test void aDryDetourRetainsOuterBodyAndFallConstraintsThenRestoresTheirScope(){
        var actor=new Object();var other=new Object();
        NativePathLimits.search(actor,1,()->NativePathLimits.avoiding(actor,p->p.getX()==1,()->{
            NativePathLimits.alsoAvoiding(actor,p->p.getZ()==2,()->{
                assertTrue(NativePathLimits.blocked(actor,1,0,0));
                assertTrue(NativePathLimits.blocked(actor,0,0,2));
                assertFalse(NativePathLimits.blocked(actor,0,0,0));
                assertFalse(NativePathLimits.blocked(other,1,0,2));
                assertEquals(1,NativePathLimits.maxFall(actor,3));return null;
            });
            assertTrue(NativePathLimits.blocked(actor,1,0,0));
            assertFalse(NativePathLimits.blocked(actor,0,0,2));return null;
        }));
        assertFalse(NativePathLimits.blocked(actor,1,0,2));
        assertEquals(3,NativePathLimits.maxFall(actor,3));
    }
    @Test void anInterruptedSearchDoesNotLeakItsExclusions(){
        var actor=new Object();
        assertThrows(IllegalStateException.class,()->NativePathLimits.alsoAvoiding(actor,p->true,()->{throw new IllegalStateException();}));
        assertFalse(NativePathLimits.blocked(actor,0,0,0));
    }
}
