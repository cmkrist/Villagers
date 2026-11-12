package local.villagers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FoodDigContactTest {
    @Test void sideGravityWorkNeverAdmitsAWorkerUnderOrOverlappingTheFallingColumn(){
        var feet=new Vec3(.5,0,.5);var body=new AABB(.2,0,.2,.8,1.95,.8);
        assertTrue(FoodAccess.besideColumn(feet,body,new BlockPos(1,1,0),2));
        assertFalse(FoodAccess.besideColumn(feet,body,new BlockPos(0,2,0),2),"An overhead support is not a side operation");
        assertFalse(FoodAccess.besideColumn(feet,new AABB(.5,0,.2,1.1,1.95,.8),new BlockPos(1,1,0),2));
        assertFalse(FoodAccess.besideColumn(feet,body,new BlockPos(1,1,0),5),"Column height stays bounded");
    }
    @Test void anUpperBlockHasAReachableFrontFaceEvenWhenALowerBlockHidesItsCenter(){
        var eye=new Vec3(-.7,1.62,.7);var upper=new BlockPos(0,3,0);var lower=upper.below();
        var center=Vec3.atCenterOf(upper);var front=center.add(-.499,0,0);
        assertNotNull(Shapes.block().clip(eye,center,lower),"A center-only ray strikes the lower block first");
        assertNull(Shapes.block().clip(eye,front,lower),"The upper front face is visible above the occluder");
        var actual=Shapes.block().clip(eye,front,upper);assertNotNull(actual);
        assertTrue(eye.distanceToSqr(actual.getLocation())<=9,"Actual block contact stays inside three-block reach");
    }
}
