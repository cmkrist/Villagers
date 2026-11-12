package local.villagers.navigation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RouteProgressTest {
    @org.junit.jupiter.api.Test void repeatedShortApproachesMustBeatTheirClosestDistance(){
        var progress=new RouteProgress(120);
        org.junit.jupiter.api.Assertions.assertFalse(progress.stalledCloser("hay",0,10));
        org.junit.jupiter.api.Assertions.assertFalse(progress.stalledCloser("hay",40,9));
        org.junit.jupiter.api.Assertions.assertFalse(progress.stalledCloser("hay",80,10));
        org.junit.jupiter.api.Assertions.assertFalse(progress.stalledCloser("hay",120,9.1));
        org.junit.jupiter.api.Assertions.assertTrue(progress.stalledCloser("hay",160,9));
        org.junit.jupiter.api.Assertions.assertFalse(progress.stalledCloser("hay",170,8.5));
        org.junit.jupiter.api.Assertions.assertFalse(progress.stalledCloser("other hay",180,12));
    }

    @Test void jumpingOrRevisitingEarlierNodesCannotHideAnObstructedStep(){
        var progress=new RouteProgress(80);
        assertFalse(progress.stalledAdvance("voice",0,3));
        assertFalse(progress.stalledAdvance("voice",40,3));
        assertTrue(progress.stalledAdvance("voice",80,3));
        assertFalse(progress.stalledAdvance("voice",90,4));
        assertFalse(progress.stalledAdvance("voice",150,3));
        assertTrue(progress.stalledAdvance("voice",170,4));
        progress.reset();
        assertFalse(progress.stalledAdvance("voice",180,0));
    }
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
