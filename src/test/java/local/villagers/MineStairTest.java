package local.villagers;

import java.util.*;
import net.minecraft.core.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MineStairTest {
    @Test void aReachedJunctionCanBeSavedBeforeChoosingItsNextDirection(){
        var pending=new MineStair.State();pending.root=new BlockPos(2,55,3);pending.entry=new BlockPos(0,63,0);pending.reselect=true;
        var tag=MineStair.passageTag(pending);var restored=new MineStair.State();
        assertTrue(MineStair.restorePassage(restored,tag));assertEquals(pending.root,restored.root);assertEquals(pending.entry,restored.entry);
        assertNull(restored.direction);assertTrue(restored.reselect);assertEquals(0,restored.depth);assertEquals(0,restored.part);
        assertFalse(MineStair.restorePassage(new MineStair.State(),tag.replace(":select",":active")));
        assertFalse(MineStair.restorePassage(new MineStair.State(),tag.replace(":NONE:0:0:",":NONE:1:0:")));
    }
    @Test void anActivePassageRoundTripRetainsItsRealDirectionAndExitFloors(){
        var active=new MineStair.State();active.root=new BlockPos(2,55,3);active.entry=new BlockPos(0,63,0);
        active.direction=Direction.WEST;active.depth=3;active.part=2;active.level=true;
        var restored=new MineStair.State();assertTrue(MineStair.restorePassage(restored,MineStair.passageTag(active)));
        assertEquals(Direction.WEST,restored.direction);assertEquals(3,restored.depth);assertEquals(2,restored.part);assertFalse(restored.reselect);
        assertEquals(4,restored.floors.size());assertTrue(restored.floors.contains(active.root.west(3).below()));
    }
    @Test void OnlyANewLowestFloorRestartsTheBoundedProspectingBudget(){
        assertEquals(0,MineStair.stepsWithoutDepth(24,53,54));
        assertEquals(24,MineStair.stepsWithoutDepth(24,54,54));
        assertEquals(24,MineStair.stepsWithoutDepth(23,57,54));
        assertEquals(1,MineStair.stepsWithoutDepth(0,54,54));
    }
    @Test void knownDescentWinsOverNearbyLevelWorkAndShallowerJunctions(){
        var actor=new BlockPos(0,56,0);var flat=actor.east(3);var shallow=new BlockPos(4,60,0);var deep=new BlockPos(12,58,0);
        var candidates=new ArrayList<>(List.of(flat,shallow,deep));
        candidates.sort(MineStair.frontierOrder(Set.of(shallow,deep),actor));
        assertEquals(List.of(deep,shallow,flat),candidates);
    }
    @Test void everyDescendedLevelRetainsAContinuousExitAndThreeBlocksOfHeadroom(){
        var root=new BlockPos(0,60,0);
        for(var direction:Direction.Plane.HORIZONTAL){
            var excavated=new HashSet<BlockPos>();var floors=new HashSet<BlockPos>();floors.add(root.below());
            for(int depth=1;depth<=MineStair.DEPTH;depth++){
                var cells=MineStair.step(root,direction,depth);assertEquals(3,cells.size());
                var feet=root.relative(direction,depth).below(depth);assertEquals(feet,cells.getLast());
                assertEquals(feet.above(2),cells.getFirst());excavated.addAll(cells);floors.add(feet.below());
            }
            assertEquals(3*MineStair.DEPTH,excavated.size());assertTrue(Collections.disjoint(excavated,floors));
        }
    }
    @Test void theExcavationCannotExceedItsBudgetOrTurnIntoAVerticalDrop(){
        assertThrows(IllegalArgumentException.class,()->MineStair.step(BlockPos.ZERO,Direction.NORTH,MineStair.DEPTH+1));
        assertThrows(IllegalArgumentException.class,()->MineStair.step(BlockPos.ZERO,Direction.DOWN,1));
    }
    @Test void aTurnCannotDigOutAnEarlierLegsRetainedExitFloor(){
        var root=new BlockPos(0,60,0);var w=new MineStair.State();
        for(int i=0;i<=5;i++)w.floors.add(root.south(i).below(i+1));
        var end=root.south(5).below(5);
        assertTrue(MineStair.retainsFloor(w,MineStair.step(end,Direction.NORTH,1)));
        assertFalse(MineStair.retainsFloor(w,MineStair.step(end,Direction.EAST,1)));
        assertFalse(MineStair.retainsFloor(w,MineStair.step(end,Direction.WEST,1)));
    }
    @Test void aLevelPassageOverACavernKeepsItsSupportedFloorAtEveryStep(){
        var root=new BlockPos(0,57,0);var excavated=new HashSet<BlockPos>();var floors=new HashSet<BlockPos>();
        for(int i=1;i<=MineStair.DEPTH;i++){
            var cells=MineStair.step(root,Direction.EAST,i,true);excavated.addAll(cells);
            assertEquals(57,cells.getLast().getY());floors.add(cells.getLast().below());
        }
        assertTrue(Collections.disjoint(excavated,floors));assertEquals(MineStair.DEPTH,floors.size());
    }
    @Test void exploredLevelCorridorsCannotBecomeAnEndlessNewExcavationLoop(){
        var w=new MineStair.State();var origin=new BlockPos(0,55,0);
        // A west/east corridor has already been walked. Its blocks may all be
        // air and safe, but neither direction is an unexplored work front.
        w.floors.add(origin.west().below());w.floors.add(origin.below());w.floors.add(origin.east().below());
        assertFalse(MineStair.newWorkFloor(w,MineStair.step(origin,Direction.WEST,1,true)));
        assertFalse(MineStair.newWorkFloor(w,MineStair.step(origin,Direction.EAST,1,true)));
        assertTrue(MineStair.newWorkFloor(w,MineStair.step(origin,Direction.NORTH,1,true)));
        assertTrue(MineStair.newWorkFloor(w,MineStair.step(origin,Direction.SOUTH,1,true)));
        assertEquals(3,w.floors.size(),"Return floors must remain intact");
    }
    @Test void beingUnderTheEntranceDoesNotCountAsHavingClimbedOut(){
        var entry=new BlockPos(0,63,0);
        assertFalse(MineStair.atEntry(new net.minecraft.world.phys.Vec3(.5,62,.5),entry));
        assertTrue(MineStair.atEntry(new net.minecraft.world.phys.Vec3(1.5,63,.5),entry));
        assertFalse(MineStair.atEntry(new net.minecraft.world.phys.Vec3(3.5,63,.5),entry));
    }
    @Test void returningFromAnOutlyingWorkbenchUsesTheKnownEntranceBeforeTheDeepFloor(){
        var entry=new BlockPos(0,63,0);var floor=new BlockPos(8,55,0);
        assertEquals(entry,MineStair.approachTarget(new net.minecraft.world.phys.Vec3(-40,63,.5),entry,floor));
        assertEquals(floor,MineStair.approachTarget(new net.minecraft.world.phys.Vec3(.5,63,.5),entry,floor));
        assertEquals(floor,MineStair.approachTarget(new net.minecraft.world.phys.Vec3(3,60,.5),entry,floor));
        assertEquals(floor,MineStair.approachTarget(new net.minecraft.world.phys.Vec3(-40,63,.5),null,floor));
    }
    @Test void junctionRetriesRenewTheirQueueLeaseUnderContinuouslyCompetingWork(){
        assertEquals(0,planningGrants(100),"The old perception interval lets every reservation expire");
        assertTrue(planningGrants(MineStair.FRONTIER_RETRY_TICKS)>5,"A waiting explorer must actually receive planning time");
    }
    @Test void aDeepRecordedStairReturnsInBoundedActualSupportLegs(){
        var floors=new HashSet<BlockPos>();var entrance=new BlockPos(0,64,0);
        for(int depth=0;depth<=150;depth++)floors.add(entrance.east(depth).below(depth+1));
        var position=net.minecraft.world.phys.Vec3.atBottomCenterOf(entrance.east(150).below(150));
        assertEquals(entrance.east(126).below(126),MineStair.recordedWaypoint(floors,position,entrance));
        // Each subsequent ascent leg uses only floors this worker recorded.
        var second=MineStair.recordedWaypoint(floors,net.minecraft.world.phys.Vec3.atBottomCenterOf(entrance.east(126).below(126)),entrance);
        assertEquals(entrance.east(102).below(102),second);
    }
    @Test void aWaypointCannotBridgeDisconnectedOrUnwalkedMineFloors(){
        var floors=Set.of(new BlockPos(0,0,0),new BlockPos(10,0,0));
        assertNull(MineStair.recordedWaypoint(floors,new net.minecraft.world.phys.Vec3(.5,1,.5),new BlockPos(10,1,0)));
        assertNull(MineStair.recordedWaypoint(floors,new net.minecraft.world.phys.Vec3(5,1,0),new BlockPos(10,1,0)));
        assertNull(MineStair.recordedWaypoint(Set.of(),net.minecraft.world.phys.Vec3.ZERO,BlockPos.ZERO));
    }
    private static int planningGrants(int retry){
        var budget=new local.villagers.navigation.WorkBudget();var busy=new UUID(0,1);var explorer=new UUID(0,2);int grants=0;
        for(int tick=0;tick<400;tick++){
            if(tick%10==0)budget.admit(busy,tick);
            if(tick>=1&&(tick-1)%retry==0&&budget.admit(explorer,tick))grants++;
        }
        return grants;
    }
}
