package local.villagers;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TreeClaimsTest {
    @Test void aWorkerRetainsTheWholeTrunkWhileApproachingAndWorking() {
        var board=new TreeClaims.Board();var a=UUID.randomUUID();var b=UUID.randomUUID();var tree=new BlockPos(7,68,-4);
        assertTrue(board.claim(tree,a,100));assertFalse(board.claim(tree.above(),b,110));
        assertTrue(board.claim(tree,a,480));assertFalse(board.available(tree.below(5),b,500));
        assertTrue(board.claim(tree,b,880));assertFalse(board.available(tree,a,881));
    }
    @Test void abandonedClaimsExpireAndOtherColumnsRemainAvailable() {
        var board=new TreeClaims.Board();var a=UUID.randomUUID();var b=UUID.randomUUID();var tree=new BlockPos(7,68,-4);
        assertTrue(board.claim(tree,a,100));assertTrue(board.claim(tree.east(),b,100));
        assertFalse(board.available(tree,b,499));assertTrue(board.available(tree,b,500));
    }
}
