package local.villagers.training;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Small atomic restart marker. Invalid/unknown state is never treated as a new budget. */
public final class TrainingCheckpointStore {
    private static final Gson JSON = new Gson();
    private final Path file;

    public TrainingCheckpointStore(Path villagersRoot) {
        file = villagersRoot.resolve("training-checkpoint.json");
    }

    public TrainingCheckpoint load() throws IOException {
        if (!Files.exists(file)) return null;
        try {
            if (Files.size(file) > 16_384) throw new IOException("Training checkpoint exceeds size limit");
            var object = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            // Check required fields before Gson can replace missing primitives with zero/false.
            for (String key : new String[]{"schemaVersion", "runId", "role", "learningMode", "startGameTick",
                    "savedGameTick", "elapsedTicks", "stage", "promotionDisabled"})
                if (!object.has(key) || object.get(key).isJsonNull()) throw new IOException("Missing checkpoint field: " + key);
            return JSON.fromJson(object, TrainingCheckpoint.class);
        } catch (RuntimeException invalid) {
            throw new IOException("Invalid training checkpoint; automatic acceleration refused", invalid);
        }
    }

    public void save(TrainingCheckpoint checkpoint) throws IOException {
        // Preserve unsupported/invalid files, including when save is called before load.
        load();
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "training-checkpoint-", ".tmp");
        try {
            Files.writeString(temporary, JSON.toJson(checkpoint));
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
    }
}
