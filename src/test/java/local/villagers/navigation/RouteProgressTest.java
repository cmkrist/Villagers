package local.villagers.navigation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RouteProgressTest {
    @Test void repeatedPathPlanningWithoutMovementStillTimesOut(){
        var progress=new RouteProgress(120);
        assertFalse(progress.stalled("home",0,1,2,3));
        assertFalse(progress.stalled("home",40,1,2,3));
        assertFalse(progress.stalled("home",80,1.01,2,3));
        assertTrue(progress.stalled("home",120,1,2,3));
    }
    @Test void realProgressAndNewObjectiveResetTheDeadline(){
        var progress=new RouteProgress(120);
        progress.stalled("home",0,0,0,0);
        assertFalse(progress.stalled("home",100,1,0,0));
        assertFalse(progress.stalled("home",200,2,0,0));
        assertFalse(progress.stalled("guardian",300,2,0,0));
        assertTrue(progress.stalled("guardian",420,2,0,0));
    }
}
