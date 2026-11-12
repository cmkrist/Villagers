package local.villagers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeMineWorkAreaTest {
    @Test void anEarnedEntranceBoundsHorizontalExplorationWhileAllowingDeepResources(){
        var entry=new BlockPos(20,63,-15);
        assertTrue(NativeMineWorkArea.column(entry,new Vec3(20.5,-55,-14.5),-64));
        assertTrue(NativeMineWorkArea.column(entry,new Vec3(52.5,-55,-14.5),-64));
        assertFalse(NativeMineWorkArea.column(entry,new Vec3(52.501,-55,-14.5),-64));
        assertFalse(NativeMineWorkArea.column(entry,new Vec3(50,-55,15),-64));
    }
    @Test void aWorkColumnDoesNotGrantSkyExcursionsOrCrossTheWorldFloor(){
        var entry=new BlockPos(0,63,0);
        assertTrue(NativeMineWorkArea.column(entry,new Vec3(.5,-61,.5),-64));
        assertFalse(NativeMineWorkArea.column(entry,new Vec3(.5,-62,.5),-64));
        assertFalse(NativeMineWorkArea.column(entry,new Vec3(.5,66,.5),-64));
        assertFalse(NativeMineWorkArea.column(null,new Vec3(.5,0,.5),-64));
        assertFalse(NativeMineWorkArea.column(entry,new Vec3(Double.NaN,0,.5),-64));
    }
}
