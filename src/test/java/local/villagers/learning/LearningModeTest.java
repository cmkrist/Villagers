package local.villagers.learning;

import local.villagers.mind.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class LearningModeTest {
    @Test void fixedCheckpointKeepsWeightsVisitsAndCounterWhileNeedsStillAdvance() {
        var adaptive=new AgentMind(UUID.fromString("12345678-1234-1234-1234-123456789abc"));
        var before=new Observation(.4,.6,.1,1,false,false,true,true);
        var fed=new Observation(0,.6,.1,1,false,false,true,true);
        adaptive.learn(before,Action.FORAGE,fed);
        var frozen=AgentMind.fromSnapshot(adaptive.snapshot());
        var checkpoint=frozen.snapshot();
        for(int i=0;i<50;i++) { LearningMode.ADAPTIVE.apply(adaptive,before,Action.FORAGE,fed); LearningMode.FROZEN.apply(frozen,before,Action.FORAGE,fed); }
        frozen.advanceDrives(12_000); frozen.remember("Control still experiences the world"); // Saturation goes first.
        var after=frozen.snapshot();
        assertEquals(checkpoint.learningUpdates(),after.learningUpdates());
        assertTrue(java.util.Arrays.deepEquals(checkpoint.values(),after.values()));
        assertTrue(java.util.Arrays.deepEquals(checkpoint.visits(),after.visits()));
        assertTrue(after.hunger()>checkpoint.hunger());
        assertTrue(adaptive.learningUpdates()>frozen.learningUpdates());
    }
    @Test void invalidModesCannotSilentlyBecomeAdaptive() {
        assertEquals(LearningMode.FROZEN,LearningMode.parse("frozen"));
        assertThrows(IllegalArgumentException.class,()->LearningMode.parse("freezed"));
    }
}
