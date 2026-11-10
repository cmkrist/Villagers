package local.villagers.economy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MarketContactTest {
    @Test void actualSupplierIsRememberedWithoutGrowingAQueueAndCompletionCannotReplayItsResponse() {
        var known=new MarketContact();var peer=new UUID(1,2);
        assertTrue(known.hear(peer,100));assertEquals(peer,known.peer(101));
        assertFalse(known.hear(new UUID(1,3),102));assertFalse(known.hear(peer,100));
        known.completed();assertNull(known.peer(103));assertFalse(known.hear(peer,100));
        assertTrue(known.hear(new UUID(1,3),104));
    }
    @Test void expiryRewindAndUnloadCannotPreserveAnUncommittedPurchase() {
        var known=new MarketContact();var peer=new UUID(2,3);known.hear(peer,100);
        assertEquals(peer,known.peer(100+ToolDemand.LIFETIME-1));assertNull(known.peer(100+ToolDemand.LIFETIME));
        assertTrue(known.hear(peer,100+ToolDemand.LIFETIME+1));assertNull(known.peer(5));
        assertNull(new MarketContact().peer(200));assertFalse(known.hear(null,200));assertFalse(known.hear(peer,Long.MAX_VALUE));
    }
}
