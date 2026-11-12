package local.villagers;

import java.util.*;
import net.minecraft.core.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdventureQuarryTest {
    static { net.minecraft.SharedConstants.tryDetectVersion(); net.minecraft.server.Bootstrap.bootStrap(); }
    @Test void prospectingCrossesNaturalRockVariantsWhileLeavingMasonryOutsideTheWhitelist(){
        for(var block:List.of(net.minecraft.world.level.block.Blocks.STONE,net.minecraft.world.level.block.Blocks.DEEPSLATE,
                net.minecraft.world.level.block.Blocks.ANDESITE,net.minecraft.world.level.block.Blocks.DIORITE,
                net.minecraft.world.level.block.Blocks.GRANITE,net.minecraft.world.level.block.Blocks.TUFF))
            assertTrue(AdventureMining.naturalRock(block.defaultBlockState()));
        for(var block:List.of(net.minecraft.world.level.block.Blocks.POLISHED_ANDESITE,net.minecraft.world.level.block.Blocks.STONE_BRICKS,
                net.minecraft.world.level.block.Blocks.COBBLESTONE,net.minecraft.world.level.block.Blocks.SAND))
            assertFalse(AdventureMining.naturalRock(block.defaultBlockState()));
    }
    @Test void aFiniteQuarryCanCrossClayWithoutAdmittingSandOrStructuralBlocks(){
        for(var block:List.of(net.minecraft.world.level.block.Blocks.DIRT,net.minecraft.world.level.block.Blocks.GRASS_BLOCK,
                net.minecraft.world.level.block.Blocks.COARSE_DIRT,net.minecraft.world.level.block.Blocks.CLAY))
            assertTrue(AdventureQuarry.overburden(block.defaultBlockState()));
        for(var block:List.of(net.minecraft.world.level.block.Blocks.SAND,net.minecraft.world.level.block.Blocks.GRAVEL,
                net.minecraft.world.level.block.Blocks.OAK_PLANKS,net.minecraft.world.level.block.Blocks.COBBLESTONE,
                net.minecraft.world.level.block.Blocks.CHEST,net.minecraft.world.level.block.Blocks.STONE))
            assertFalse(AdventureQuarry.overburden(block.defaultBlockState()));
    }
    @Test void failedSitesCanBeReconsideredAfterOneRealDayAndKeepTheirDateAcrossReloads(){
        var root=new BlockPos(-482,63,-369);String tag="villagers:quarry_failed:"+root.asLong()+":120000";
        var remembered=AdventureQuarry.failure(tag,130000);assertNotNull(remembered);assertEquals(root,remembered.root());
        assertFalse(remembered.expired(143999));assertTrue(remembered.expired(144000));
        var reloaded=AdventureQuarry.failure(remembered.tag(),140000);assertEquals(120000,reloaded.observedAt());assertTrue(reloaded.expired(144000));
        assertFalse(reloaded.expired(119999));
    }
    @Test void legacyUndatedAvoidanceGetsOnePersistentReviewInterval(){
        var root=new BlockPos(-482,63,-373);var legacy=AdventureQuarry.failure("villagers:quarry_failed:"+root.asLong(),130144);
        assertNotNull(legacy);assertEquals(130144,legacy.observedAt());
        var loaded=AdventureQuarry.failure(legacy.tag(),140000);assertFalse(loaded.expired(154143));assertTrue(loaded.expired(154144));
        assertNull(AdventureQuarry.failure("villagers:quarry_failed:bad",130144));
        assertNull(AdventureQuarry.failure("villagers:quarry_failed:"+root.asLong()+":-1",130144));
    }
    @Test void excavationPreservesASupportedOneBlockDescentAndThreeAccessibleWorkBays(){
        var root=new BlockPos(0,64,0);
        for(var direction:Direction.Plane.HORIZONTAL){
            var cells=AdventureQuarry.cells(root,direction);var removed=new HashSet<>(cells);
            assertEquals(cells.size(),removed.size(),"never charge or remove the same soil twice");
            assertTrue(cells.size()<=16);
            assertTrue(cells.stream().allMatch(p->p.getY()>=60&&p.getY()<64));
            assertFalse(removed.contains(root.below()),"do not dig away the original stand");
            var supports=AdventureQuarry.accessSupports(root,direction);
            assertEquals(5,supports.size());
            assertTrue(Collections.disjoint(removed,supports),"soil clearance retains the exit; replacing mined support requires paid dirt");
            for(int depth=1;depth<=4;depth++){
                var feet=root.relative(direction,depth).below(depth);
                assertTrue(removed.contains(feet));
                assertTrue(feet.above().getY()>=64||removed.contains(feet.above()),"native adult headroom along every step");
                assertFalse(removed.contains(feet.below()),"each step retains its actual supporting floor");
                assertTrue(supports.contains(feet.below()),"protect every step even while the miner is standing elsewhere");
            }
            var bottom=root.relative(direction,4).below(4);
            for(var side:List.of(direction,direction.getClockWise(),direction.getCounterClockWise())){
                var bay=bottom.relative(side);
                assertTrue(removed.contains(bay)&&removed.contains(bay.above()),"two-block work bay");
                assertFalse(removed.contains(bay.below()),"leave each of the three stone targets for the actual pickaxe");
                assertFalse(bay.below().equals(bottom.below()),"stone targets are not the miner's own floor");
            }
        }
    }
}
