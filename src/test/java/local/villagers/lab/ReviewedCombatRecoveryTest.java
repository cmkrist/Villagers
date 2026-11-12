package local.villagers.lab;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReviewedCombatRecoveryTest {
    private final UUID actor=UUID.randomUUID(),attacker=UUID.randomUUID();
    private final ReviewedCombatRecovery scope=new ReviewedCombatRecovery(actor,attacker);
    @Test void onlyTheRecordedPairCanContinueAboveTheFloor(){
        assertTrue(scope.permits(actor,attacker,17));
        assertFalse(scope.permits(UUID.randomUUID(),attacker,20));
        assertFalse(scope.permits(actor,UUID.randomUUID(),20));
        assertFalse(scope.permits(actor,null,20));
    }
    @Test void reachingTheFloorStopsEvenTheRecordedAttacker(){
        assertTrue(scope.permits(actor,attacker,8.01f));
        assertFalse(scope.permits(actor,attacker,8));
        assertTrue(scope.critical(actor,8));assertTrue(scope.critical(actor,7));
        assertFalse(scope.critical(actor,9));
    }
    @Test void invalidHealthCannotAuthorizeContinuation(){
        assertFalse(scope.permits(actor,attacker,Float.NaN));
        assertFalse(scope.permits(actor,attacker,Float.POSITIVE_INFINITY));
        assertTrue(scope.critical(actor,Float.NaN));
    }
}
