package local.villagers.navigation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FloorCenteringPolicyTest {
    @Test void clippedResidentCanCentreWithinTheSameNegativeCoordinateCellAndStepDown() {
        assertTrue(FloorCenteringPolicy.local(-234.206902,73,766.7,-234.5,72,766.5,.6));
    }
    @Test void centeringCannotBecomeARouteToAnotherCellOrAnUpwardEscape() {
        assertFalse(FloorCenteringPolicy.local(-234.206902,73,766.7,-235.5,72,766.5,.6));
        assertFalse(FloorCenteringPolicy.local(-234.206902,73,766.7,-234.5,73.1,766.5,.6));
        assertFalse(FloorCenteringPolicy.local(-234.206902,73,766.7,-234.5,71.5,766.5,.6));
        assertFalse(FloorCenteringPolicy.local(-234.206902,73,766.7,-234.9,72,766.5,.6));
    }
    @Test void negativeCellBoundaryAndMaximumHorizontalDisplacementAreCorrect() {
        assertTrue(FloorCenteringPolicy.local(-.001,73,.999,-.5,72,.5,.6));
        assertFalse(FloorCenteringPolicy.local(-.001,73,.999,.5,72,.5,.6));
    }
    @Test void oversizedOrInvalidBodiesNeverReceiveALocalStep() {
        assertFalse(FloorCenteringPolicy.local(0,73,0,.5,72,.5,1));
        assertFalse(FloorCenteringPolicy.local(0,73,0,.5,72,.5,0));
        assertFalse(FloorCenteringPolicy.local(Double.NaN,73,0,.5,72,.5,.6));
        assertFalse(FloorCenteringPolicy.local(0,73,0,.5,Double.POSITIVE_INFINITY,.5,.6));
    }
}
