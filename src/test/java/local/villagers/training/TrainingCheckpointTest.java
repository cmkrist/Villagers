package local.villagers.training;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class TrainingCheckpointTest {
    @TempDir Path root;
    private TrainingCheckpoint state(String stage, long elapsed) {
        return new TrainingCheckpoint(1, "adaptive-stable-run", "adaptive", "adaptive", 10_000,
                10_000 + elapsed, elapsed, stage, true);
    }

    @Test void shutdownAndRestartKeepIdentityExposureAndBackoff() throws Exception {
        var store = new TrainingCheckpointStore(root);
        store.save(state("suspended_shutdown", 32_000));
        var restored = new TrainingCheckpointStore(root).load();
        assertEquals("adaptive-stable-run", restored.runId());
        assertTrue(restored.resumable());
        assertTrue(restored.promotionDisabled());
        assertEquals(32_000, restored.elapsedAt(42_000));
        assertEquals(33_000, restored.elapsedAt(43_000));
        assertEquals(48_000, restored.elapsedAt(99_000));
    }

    @Test void rolledBackWorldDoesNotRefundPreviouslyRecordedExposure() {
        var saved = state("running", 30_000);
        assertEquals(30_000, saved.elapsedAt(39_000));
        // Next session anchors at the rolled-back world's time but keeps all prior exposure.
        var resumed = new TrainingCheckpoint(1, saved.runId(), saved.role(), saved.learningMode(),
                saved.startGameTick(), 39_000, saved.elapsedAt(39_000), "running", true);
        assertEquals(31_000, resumed.elapsedAt(40_000));
    }

    @Test void completedFailedAndPlayStatesNeverResumeOrAccumulateMoreExposure() throws Exception {
        for (String terminal : new String[]{"completed_bounded_run", "play", "failed_evidence", "failed_cohort_extinct"}) {
            var saved = state(terminal, 12_000);
            new TrainingCheckpointStore(root).save(saved);
            var restored = new TrainingCheckpointStore(root).load();
            assertFalse(restored.resumable());
            assertEquals(12_000, restored.elapsedAt(999_999));
            assertEquals(terminal, restored.stage());
        }
    }

    @Test void absentCheckpointIsDistinctFromInvalidOrFutureState() throws Exception {
        var store = new TrainingCheckpointStore(root);
        assertNull(store.load());
        Path file = root.resolve("training-checkpoint.json");
        for (String invalid : new String[]{"{", "{}", "{\"schemaVersion\":99}"}) {
            Files.writeString(file, invalid);
            assertThrows(IOException.class, store::load);
            assertThrows(IOException.class, () -> store.save(state("running", 1)));
            assertEquals(invalid, Files.readString(file));
        }
    }

    @Test void completeFutureSchemaIsPreservedWithoutPriorLoad() throws Exception {
        var store = new TrainingCheckpointStore(root);
        store.save(state("running", 5));
        Path file = root.resolve("training-checkpoint.json");
        String future = Files.readString(file).replace("\"schemaVersion\":1", "\"schemaVersion\":99");
        Files.writeString(file, future);
        assertThrows(IOException.class, () -> new TrainingCheckpointStore(root).save(state("running", 6)));
        assertEquals(future, Files.readString(file));
    }

    @Test void learnerRoleChangesCannotReuseAnExistingExperiment() {
        assertThrows(IllegalArgumentException.class, () -> state("running", 1)
                .requireCompatible(new TrainingConfig(40, TrainingConfig.Role.FROZEN, "frozen")));
        assertDoesNotThrow(() -> state("running", 1)
                .requireCompatible(new TrainingConfig(80, TrainingConfig.Role.ADAPTIVE, "adaptive")));
    }

    @Test void invalidPathsAndBudgetsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> state("running", 48_001));
        assertThrows(IllegalArgumentException.class, () -> new TrainingCheckpoint(1, "../other", "adaptive", "adaptive",
                0, 1, 1, "running", false));
    }
}
