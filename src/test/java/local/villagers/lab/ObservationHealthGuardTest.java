package local.villagers.lab;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ObservationHealthGuardTest {
    @Test void finalRecoveryRequiresExplicitOneHealthScopeAndValidSavedMetabolism(){
        assertTrue(ObservationHealthGuard.criticalDuringRecovery(1,300,false,1,false,4,0));
        assertFalse(ObservationHealthGuard.criticalDuringRecovery(1,300,false,1,true,4,0));
        assertFalse(ObservationHealthGuard.criticalDuringRecovery(1,300,false,1,true,0,59));
        assertTrue(ObservationHealthGuard.criticalDuringRecovery(1,300,false,1,true,0,60));
        assertTrue(ObservationHealthGuard.criticalDuringRecovery(1,300,false,4,true,4,0));
        assertTrue(ObservationHealthGuard.criticalDuringRecovery(1,0,true,1,true,4,0));
        assertTrue(ObservationHealthGuard.criticalDuringRecovery(0,300,false,1,true,20,0));
        assertTrue(ObservationHealthGuard.criticalDuringRecovery(1,300,false,1,true,-1,-1));
    }
    @org.junit.jupiter.api.Test void finalRecoveryStopsBeforeTheNextStarvationHit(){
        org.junit.jupiter.api.Assertions.assertTrue(ObservationHealthGuard.lastHealthRecovery(1,0,0));
        org.junit.jupiter.api.Assertions.assertTrue(ObservationHealthGuard.lastHealthRecovery(1,0,59));
        org.junit.jupiter.api.Assertions.assertFalse(ObservationHealthGuard.lastHealthRecovery(1,0,60));
        org.junit.jupiter.api.Assertions.assertFalse(ObservationHealthGuard.lastHealthRecovery(1,0,79));
        org.junit.jupiter.api.Assertions.assertTrue(ObservationHealthGuard.lastHealthRecovery(1,1,70));
        org.junit.jupiter.api.Assertions.assertFalse(ObservationHealthGuard.lastHealthRecovery(0,20,0));
        org.junit.jupiter.api.Assertions.assertFalse(ObservationHealthGuard.lastHealthRecovery(Float.NaN,0,0));
    }
    @Test void aThirteenDamageHitStopsBeforeTheNextLethalAttackEvenAboveTheFourHealthFloor(){
        assertFalse(ObservationHealthGuard.critical(7,300,false,4));
        assertTrue(ObservationHealthGuard.combatHit(13,true));
        assertFalse(ObservationHealthGuard.combatHit(0,true));
        assertFalse(ObservationHealthGuard.combatHit(1,false));
    }
    @Test void depletedAirStopsBeforeAHealthyBodyTakesItsFirstDrowningHit(){
        assertFalse(ObservationHealthGuard.critical(24,1,true,4));
        assertTrue(ObservationHealthGuard.critical(24,0,true,4));
        assertTrue(ObservationHealthGuard.critical(24,-19,true,4));
        assertFalse(ObservationHealthGuard.critical(24,0,false,4));
    }
    @Test void theHealthBoundaryCoversIllagerSizedBodiesAndDoesNotWaitForDeath(){
        assertFalse(ObservationHealthGuard.critical(6,300,false,4));
        assertTrue(ObservationHealthGuard.critical(4,300,false,4));
        assertTrue(ObservationHealthGuard.critical(0,300,false,0));
        assertTrue(ObservationHealthGuard.critical(Float.NaN,300,false,4));
    }
}
