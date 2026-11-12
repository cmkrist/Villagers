package local.villagers.survival;

import org.junit.jupiter.api.Test;
import static local.villagers.survival.SurvivalPolicy.*;
import static org.junit.jupiter.api.Assertions.*;

class SurvivalPolicyTest {
    @Test void hungryWakeupDoesNotImmediatelyOrderAnotherBedTrip(){
        for(int food=0;food<=6;food++){
            assertFalse(scheduledShelter(true,food));
            assertEquals(Intent.NORMAL,choose(new Situation(false,scheduledShelter(true,food),true,true,true,4,false)));
            assertEquals(Intent.SHELTER,choose(new Situation(true,scheduledShelter(true,food),true,true,true,4,false)));
        }
        assertTrue(scheduledShelter(true,7));
        assertTrue(scheduledShelter(true,20));
        assertFalse(scheduledShelter(false,20));
    }
    @Test void calmGuardProximityAllowsRestButNeverSuppressesActualDanger(){
        assertTrue(mayRestByGuardian(false,64,true));
        assertFalse(mayRestByGuardian(false,Math.nextUp(64d),true));
        assertFalse(mayRestByGuardian(true,1,true));
        assertFalse(mayRestByGuardian(false,1,false));
        assertFalse(mayRestByGuardian(false,Double.NaN,true));
        assertFalse(mayRestByGuardian(false,-1,true));
        assertEquals(Intent.ESCAPE,choose(new Situation(true,false,false,false,false,0,false)));
    }
    @Test void sharedVillageWorkIsBoundedToTheResidentsOwnBell(){
        assertTrue(withinVillageWorkArea(64,48));
        assertFalse(withinVillageWorkArea(64.01,10));assertFalse(withinVillageWorkArea(10,48.01));
        assertFalse(withinVillageWorkArea(Double.NaN,10));assertFalse(withinVillageWorkArea(10,Double.POSITIVE_INFINITY));
        assertFalse(withinVillageWorkArea(-1,10));assertFalse(withinVillageWorkArea(10,-1));
    }
    @Test void usableSafeHousePrecedesRandomFlight() {
        assertEquals(Intent.SHELTER,choose(new Situation(true,false,true,true,true,12,true)));
    }
    @Test void unsafeOrUnreachableHouseDoesNotTrapTheAgent() {
        assertEquals(Intent.SEEK_GOLEM,choose(new Situation(true,true,true,false,true,18,true)));
        assertEquals(Intent.SEEK_GOLEM,choose(new Situation(true,true,true,true,false,18,true)));
        assertEquals(Intent.ESCAPE,choose(new Situation(true,true,false,false,false,18,false)));
        assertEquals(Intent.SEEK_GOLEM,choose(new Situation(false,true,false,false,false,0,true)));
    }
    @Test void DuskAndHomeRangeTriggerReturnButDaytimeRemainsFree() {
        assertEquals(Intent.SHELTER,choose(new Situation(false,true,true,true,true,20,false)));
        assertEquals(Intent.SHELTER,choose(new Situation(false,false,true,true,true,32,false)));
        assertEquals(Intent.NORMAL,choose(new Situation(false,false,true,true,true,12,false)));
        assertEquals(Intent.NORMAL,choose(new Situation(false,true,true,false,true,12,false)));
    }
    @Test void homeRangeRejectsInvalidDistances() {
        assertTrue(withinHomeRange(28)); assertFalse(withinHomeRange(28.01));
        assertFalse(withinHomeRange(Double.NaN)); assertFalse(withinHomeRange(-1));
        assertThrows(IllegalArgumentException.class,()->new Situation(false,true,true,true,true,Double.NaN,false));
    }
    @Test void partialRoutesOnlyReturnAlreadyDisplacedResidentsAndRequireRealProgress() {
        assertTrue(usefulPartialReturn(48, 40, 12));
        assertFalse(usefulPartialReturn(28, 12, 12));
        assertFalse(usefulPartialReturn(48, 47.5, 12));
        assertFalse(usefulPartialReturn(48, 49, 12));
        assertFalse(usefulPartialReturn(48, 40, 1));
        assertFalse(usefulPartialReturn(48, 40, 129));
        assertFalse(usefulPartialReturn(Double.NaN, 40, 12));
    }
    @Test void returnDetoursAreFiniteAndDoNotChangeOrdinaryHomeTerritory() {
        assertTrue(withinReturnBound(48, 56)); assertFalse(withinReturnBound(48, 56.01));
        assertTrue(withinReturnBound(20, 36)); assertFalse(withinHomeRange(36));
        assertFalse(withinReturnBound(48, Double.NaN)); assertFalse(withinReturnBound(-1, 20));
    }
}
