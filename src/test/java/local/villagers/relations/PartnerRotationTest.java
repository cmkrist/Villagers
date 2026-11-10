package local.villagers.relations;

import java.util.List;
import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PartnerRotationTest {
    @Test void movingDistanceOrderCannotKeepChoosingOneNeighbourAndStarveTheStockedSupplier() {
        var a=new UUID(1,1);var b=new UUID(1,2);var c=new UUID(1,3);var rotation=new PartnerRotation();
        var seen=new HashSet<UUID>();
        // A cursor modulo this changing list would choose a on all three attempts.
        seen.add(rotation.next(List.of(a,b,c)));seen.add(rotation.next(List.of(b,a,c)));seen.add(rotation.next(List.of(b,c,a)));
        assertEquals(new HashSet<>(List.of(a,b,c)),seen);
        assertEquals(a,rotation.next(List.of(c,b,a)),"after discovery the least recently tried visible actor gets a turn");
        assertEquals(c,rotation.next(List.of(c,a)),"an absent actor cannot block a visible one");
    }
    @Test void changingPopulationsRemainBoundedAndUnloadedActorsAreNeverSelected() {
        var rotation=new PartnerRotation();assertNull(rotation.next(List.of()));
        for(int i=0;i<1000;i++) {
            var id=new UUID(2,i);assertEquals(id,rotation.next(List.of(id)));
            assertTrue(rotation.remembered()<=FoodRequest.MAX_PEERS);
        }
        assertThrows(IllegalArgumentException.class,()->rotation.next(java.util.stream.LongStream.range(0,17).mapToObj(n->new UUID(3,n)).toList()));
    }
}
