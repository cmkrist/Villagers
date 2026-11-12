package local.villagers;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NativeCareStartupTest {
    private final UUID resident=UUID.randomUUID(),worker=UUID.randomUUID();
    private NativeCareStartup.Scope scope(){return new NativeCareStartup.Scope(resident,worker,304508,worker+":reviewed-body",4,List.of(-597d,64d,-614d));}
    @Test void onlyTheReviewedCheckpointCanResume(){
        assertDoesNotThrow(()->scope().requireTime(304508));
        assertThrows(IllegalStateException.class,()->scope().requireTime(304509));
        assertThrows(IllegalStateException.class,()->scope().requireTime(304507));
    }
    @Test void anotherEligibleWorkerCannotBeSilentlyAdopted(){
        assertDoesNotThrow(()->NativeIllagers.requireExpectedWorker(worker,worker));
        assertDoesNotThrow(()->NativeIllagers.requireExpectedWorker(worker,null));
        assertThrows(IllegalStateException.class,()->NativeIllagers.requireExpectedWorker(resident,worker));
    }
    @Test void invalidReviewedBodiesAreRejected(){
        assertThrows(IllegalArgumentException.class,()->new NativeCareStartup.Scope(resident,worker,304508,resident+":body",4,List.of(1d,2d,3d)));
        assertThrows(IllegalArgumentException.class,()->new NativeCareStartup.Scope(resident,worker,304508,worker+":body",Float.NaN,List.of(1d,2d,3d)));
        assertThrows(IllegalArgumentException.class,()->new NativeCareStartup.Scope(resident,worker,304508,worker+":body",4,List.of(1d,Double.NaN,3d)));
        assertThrows(IllegalArgumentException.class,()->new NativeCareStartup.Scope(worker,worker,304508,worker+":body",4,List.of(1d,2d,3d)));
    }
}
