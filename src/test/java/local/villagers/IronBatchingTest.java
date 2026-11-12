package local.villagers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IronBatchingTest {
    @Test void chestplateMiningDoesNotReturnToTheFurnaceAfterEverySingleOre(){
        assertFalse(AdventureIron.readyOreBatch(8,1,1,0));
        assertFalse(AdventureIron.readyOreBatch(8,1,6,0));
        assertTrue(AdventureIron.readyOreBatch(8,1,7,0));
        assertTrue(AdventureIron.readyOreBatch(8,0,8,0));
    }
    @Test void realPendingOreIsCollectedAndTheOriginalPickaxeStillUsesThreeIngots(){
        assertTrue(AdventureIron.readyOreBatch(8,0,0,3));
        assertTrue(AdventureIron.readyOreBatch(8,1,1,1));
        assertFalse(AdventureIron.readyOreBatch(3,0,2,0));
        assertTrue(AdventureIron.readyOreBatch(3,0,3,0));
        assertFalse(AdventureIron.readyOreBatch(8,8,0,0),"No carried or pending ore means no smelting trip");
    }
}
