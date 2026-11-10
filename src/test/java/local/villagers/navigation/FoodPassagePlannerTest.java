package local.villagers.navigation;

import java.util.Set;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FoodPassagePlannerTest {
    @Test void nativeWalkingStopsBeforeAnUnpaidExcavationButCanReuseClearedBlocks(){
        var block=new BlockPos(2,1,0);
        var steps=List.of(new FoodPassagePlanner.Step(new BlockPos(1,1,0),List.of()),new FoodPassagePlanner.Step(block,List.of(block)),new FoodPassagePlanner.Step(new BlockPos(3,1,0),List.of()));
        assertEquals(0,FoodPassagePlanner.clearWalkEnd(steps,0,p->p.equals(block)));
        assertEquals(2,FoodPassagePlanner.clearWalkEnd(steps,0,p->false));
    }
    @Test void descendingStairClearsTheHeadSweepBeforeTheBodyCanFall(){
        var start=new BlockPos(0,2,0);var step=new BlockPos(1,1,0);var goal=new BlockPos(2,0,0);
        FoodPassagePlanner.Grid roof=new FoodPassagePlanner.Grid(){
            public int clearance(BlockPos p){int floor=2-p.getX();return p.getX()>0&&p.getY()>=floor&&p.getY()<=floor+2?10:0;}
            public boolean support(BlockPos p){return p.getY()<2-p.getX();}
            public boolean allowed(BlockPos p){return Set.of(start,step,goal).contains(p);}
        };
        var plan=FoodPassagePlanner.find(roof,start,Set.of(goal),6,100);
        assertNotNull(plan);assertEquals(6,plan.digCount());
        assertEquals(new BlockPos(1,3,0),plan.steps().getFirst().dig().getFirst());
        assertEquals(new BlockPos(2,2,0),plan.steps().getLast().dig().getFirst());
    }
    @Test void switchbackRemembersTheColumnAlreadyClearedAboveTheNextStep(){
        var start=new BlockPos(0,3,0);var first=new BlockPos(1,2,0);var second=new BlockPos(0,1,0);var goal=new BlockPos(1,0,0);
        FoodPassagePlanner.Grid sand=new FoodPassagePlanner.Grid(){
            public int clearance(BlockPos p){return p.getX()==1&&p.getY()>=0&&p.getY()<=3||p.getX()==0&&p.getY()>=1&&p.getY()<=2?1:0;}
            public boolean support(BlockPos p){return true;}
            public boolean allowed(BlockPos p){return Set.of(start,first,second,goal).contains(p);}
            public List<BlockPos> excavation(BlockPos from,List<BlockPos> blocks,Set<BlockPos> cleared){
                if(blocks.contains(goal)&&!cleared.containsAll(Set.of(new BlockPos(1,2,0),new BlockPos(1,3,0))))return null;
                return blocks;
            }
        };
        var plan=FoodPassagePlanner.find(sand,start,Set.of(goal),7,100);
        assertNotNull(plan);assertEquals(6,plan.digCount());assertEquals(List.of(first,second,goal),plan.steps().stream().map(FoodPassagePlanner.Step::feet).toList());
        assertEquals(6,plan.steps().stream().flatMap(s->s.dig().stream()).distinct().count());
    }
    private static FoodPassagePlanner.Grid hill(boolean protectedWall){return new FoodPassagePlanner.Grid(){
        public int clearance(BlockPos p){
            if(p.getZ()!=0)return -1;
            int surface=Math.max(0,4-p.getX());
            if(p.getY()>=surface)return 0;
            return protectedWall?-1:40;
        }
        public boolean support(BlockPos p){return p.getY()<Math.max(0,4-p.getX());}
        public boolean allowed(BlockPos p){return p.getX()>=0&&p.getX()<=6&&p.getZ()==0&&p.getY()>=0&&p.getY()<=4;}
    };}
    @Test void canWalkANativeDescendingSlopeWithoutInventingDigging(){
        var plan=FoodPassagePlanner.find(hill(false),new BlockPos(0,4,0),Set.of(new BlockPos(4,0,0)),8,1000);
        assertNotNull(plan);assertEquals(0,plan.digCount());assertTrue(plan.steps().size()<=4);
    }
    @Test void blockedTerrainNeedsARealFiniteExcavationBudget(){
        FoodPassagePlanner.Grid wall=new FoodPassagePlanner.Grid(){
            public int clearance(BlockPos p){return p.getX()==1&&p.getY()<3?30:0;}
            public boolean support(BlockPos p){return p.getY()==0;}
            public boolean allowed(BlockPos p){return p.getZ()==0&&p.getX()>=0&&p.getX()<=2&&p.getY()==1;}
        };
        assertNull(FoodPassagePlanner.find(wall,new BlockPos(0,1,0),Set.of(new BlockPos(2,1,0)),1,100));
        var plan=FoodPassagePlanner.find(wall,new BlockPos(0,1,0),Set.of(new BlockPos(2,1,0)),2,100);
        assertNotNull(plan);assertEquals(2,plan.digCount());assertEquals(new BlockPos(1,2,0),plan.steps().getFirst().dig().getFirst());
    }
    @Test void aForbiddenCellHasNoGeneralBreakFallback(){
        FoodPassagePlanner.Grid blocked=new FoodPassagePlanner.Grid(){
            public int clearance(BlockPos p){return p.getX()==1?-1:0;}
            public boolean support(BlockPos p){return p.getY()==0;}
            public boolean allowed(BlockPos p){return p.getZ()==0&&p.getX()>=0&&p.getX()<=2&&p.getY()==1;}
        };
        assertNull(FoodPassagePlanner.find(blocked,new BlockPos(0,1,0),Set.of(new BlockPos(2,1,0)),24,100));
    }
}
