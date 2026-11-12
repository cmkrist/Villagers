package local.villagers;

import java.util.List;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeBodyRoutesTest {
    @org.junit.jupiter.api.Test void aCarpetDepartureUsesTheActualSupportedBody(){
        var feet=new net.minecraft.world.phys.Vec3(-662.0793724820495,64.0625,-613.9038017670711);
        var actual=new net.minecraft.world.phys.AABB(feet.x-.3,feet.y,feet.z-.3,feet.x+.3,feet.y+1.95,feet.z+.3);
        var start=net.minecraft.core.BlockPos.containing(feet);
        double floor=NativeBodyRoutes.partialFloor(64,64,.0625);
        org.junit.jupiter.api.Assertions.assertEquals(64.0625,floor);
        org.junit.jupiter.api.Assertions.assertEquals(actual,NativeBodyRoutes.edgeStart(actual,feet,start,start,floor));
        org.junit.jupiter.api.Assertions.assertEquals(64.0625,NativeBodyRoutes.partialFloor(64.0625,65,0));
        org.junit.jupiter.api.Assertions.assertEquals(64,NativeBodyRoutes.partialFloor(64,64,2));
        org.junit.jupiter.api.Assertions.assertEquals(64.5,NativeBodyRoutes.partialFloor(64,64,.5));
    }

    @Test void aTentCornerIsCheckedFromEachActualStepRatherThanDiagonallyFromTheOriginalBody(){
        var feet=new net.minecraft.world.phys.Vec3(-684.6471910637123,63,-595.5208415545598);
        var body=new AABB(feet.x-.3,63,feet.z-.3,feet.x+.3,64.95,feet.z+.3);
        var start=net.minecraft.core.BlockPos.containing(feet);
        var west=new net.minecraft.core.BlockPos(-686,63,-596);
        var north=new net.minecraft.core.BlockPos(-686,63,-597);
        var fence=new AABB(-684.875,63,-597,-684.625,64.5,-596);
        var turnBody=body.move(north.getX()+.5-feet.x,0,north.getZ()+.5-feet.z);
        assertTrue(body.minmax(turnBody).intersects(fence));
        var edgeStart=NativeBodyRoutes.edgeStart(body,feet,start,west,63);
        assertFalse(body.minmax(edgeStart).intersects(fence));
        assertFalse(edgeStart.minmax(turnBody).intersects(fence));
        assertEquals(body,NativeBodyRoutes.edgeStart(body,feet,start,start,63));
    }

    @Test void aClearNodeBeyondTheRecordedVindicatorCannotAuthorizeWalkingThroughIt(){
        var actor=new AABB(-629,66.5,-614.36881,-628.4,68.45,-613.76881);
        var vindicator=new AABB(-628.6,66,-615,-628,67.95,-614.4);
        var beyond=new AABB(-628.8,66,-615.8,-628.2,67.95,-615.2);
        assertFalse(actor.intersects(vindicator));assertFalse(beyond.intersects(vindicator));
        assertTrue(NativeBodyRoutes.crossesBody(actor,beyond,List.of(vindicator)));
        var away=new AABB(-628.8,67,-613.8,-628.2,68.95,-613.2);
        assertFalse(NativeBodyRoutes.crossesBody(actor,away,List.of(vindicator)));
    }
    @Test void anExistingOverlapMayBeExitedButAnotherBodyCannotBeEntered(){
        var from=new AABB(0,0,0,.6,1.95,.6);var neighbor=new AABB(.4,0,0,1,1.95,.6);
        var away=from.move(-.8,0,0);
        assertFalse(NativeBodyRoutes.crossesBody(from,away,List.of(neighbor)));
        assertTrue(NativeBodyRoutes.crossesBody(from,from.move(.5,0,0),List.of(neighbor)));
    }
}
