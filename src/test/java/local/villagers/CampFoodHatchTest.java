package local.villagers;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CampFoodHatchTest {
    @Test void aRaisedRecipientCanBeInActualHandReachBeyondThreeBlocksBetweenFeet(){
        var body=new AABB(.2,64,.2,.8,65.95,.8);
        var giverFeet=new net.minecraft.world.phys.Vec3(-2.4,63,.5);
        var eyes=giverFeet.add(0,1.62,0);
        assertTrue(giverFeet.distanceToSqr(new net.minecraft.world.phys.Vec3(.5,64,.5))>9);
        assertTrue(CampFoodHatch.contactPoints(body).stream().anyMatch(p->eyes.distanceToSqr(p)<=9));
    }
    @Test void contactSamplesCanReachAVisibleBodyEdgeWithoutCrossingItsBounds(){
        var body=new AABB(2,5,3,2.6,6.95,3.6);
        var points=CampFoodHatch.contactPoints(body);
        assertTrue(points.stream().allMatch(body::contains));
        var eye=new net.minecraft.world.phys.Vec3(2.3,6.6,0);
        var jamb=Shapes.create(new AABB(2.25,5,2,3,8,2.1));
        assertNotNull(jamb.clip(eye,new net.minecraft.world.phys.Vec3(2.3,5.8,3.3),BlockPos.ZERO));
        assertTrue(points.stream().anyMatch(p->jamb.clip(eye,p,BlockPos.ZERO)==null),
            "A real clear ray to the body edge can exist beside an obstructed centre ray");
    }
    @Test void retainedNativeUpperFenceBlocksAnAdultButLeavesALowerFeedingRay(){
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        var upper=Blocks.DARK_OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH,true).setValue(FenceBlock.SOUTH,true)
            .getCollisionShape(EmptyBlockGetter.INSTANCE,BlockPos.ZERO,CollisionContext.empty()).move(0,1,0);
        var adult=Shapes.create(new AABB(.2,0,.2,.8,1.95,.8));
        var mealOpening=Shapes.create(new AABB(.45,.5,.45,.55,.8,.55));
        assertTrue(Shapes.joinIsNotEmpty(upper,adult,BooleanOp.AND));
        assertFalse(Shapes.joinIsNotEmpty(upper,mealOpening,BooleanOp.AND));
    }
    @Test void removingTwoLowerPanelsLeavesActualAdultClearanceUnderTheThirdFence(){
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        var fence=Blocks.DARK_OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH,true).setValue(FenceBlock.SOUTH,true)
            .getCollisionShape(EmptyBlockGetter.INSTANCE,BlockPos.ZERO,CollisionContext.empty());
        var adult=Shapes.create(new AABB(.2,0,.2,.8,1.95,.8));
        assertTrue(Shapes.joinIsNotEmpty(fence.move(0,1,0),adult,BooleanOp.AND));
        assertFalse(Shapes.joinIsNotEmpty(fence.move(0,2,0),adult,BooleanOp.AND));
    }

}
