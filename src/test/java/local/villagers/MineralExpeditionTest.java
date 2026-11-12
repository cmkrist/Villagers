package local.villagers;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MineralExpeditionTest {
    @Test void mineralScoutingWaitsForLocalWorkAndNeverInterruptsAnActiveShaftOrQuarry(){
        var mine=new MineStair.State();var quarry=new AdventureQuarry.State();
        assertFalse(BoatTravel.mineralDeparture(mine,quarry));
        for(int i=0;i<16;i++)mine.floors.add(new BlockPos(i,55,0));
        assertTrue(BoatTravel.mineralDeparture(mine,quarry));
        mine.root=new BlockPos(15,56,0);assertFalse(BoatTravel.mineralDeparture(mine,quarry));
        mine.root=null;quarry.root=new BlockPos(0,64,0);assertFalse(BoatTravel.mineralDeparture(mine,quarry));
        quarry.root=null;assertTrue(BoatTravel.mineralDeparture(mine,quarry));
        assertEquals(16,mine.floors.size(),"Exploration retains all existing exit supports");
    }
}
