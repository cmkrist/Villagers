package local.villagers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PickaxeSelectionTest {
    @Test void aFreshStoneReplacementIsEquippedInsteadOfRepeatedlyReservingTheWornHeldStoneTool(){
        assertTrue(AdventureActions.preferPickaxe(4,132,4,8));
        assertFalse(AdventureActions.preferPickaxe(4,8,4,132));
    }
    @Test void aUsableSlowerToolCanWorkWhileTheLastUsesOfTheHigherTierAreRetained(){
        assertTrue(AdventureActions.preferPickaxe(4,100,6,8));
        assertFalse(AdventureActions.preferPickaxe(6,8,4,100));
        assertTrue(AdventureActions.preferPickaxe(6,100,4,100));
    }
}
