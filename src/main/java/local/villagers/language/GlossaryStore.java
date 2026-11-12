package local.villagers.language;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player glossaries under {@code <world>/villagers/players/<uuid>.json}. Same guarantees as mind files: atomic
 * replacement, corrupt files are moved aside rather than lost, and files from a newer version are never overwritten.
 */
public final class GlossaryStore {
    private static final Gson JSON = new Gson();
    private static final long MAX_FILE_BYTES = 1_048_576;
    private final Path root;
    private final Set<UUID> protectedFiles = ConcurrentHashMap.newKeySet();

    public GlossaryStore(Path villagersRoot) { this.root = villagersRoot.resolve("players").toAbsolutePath().normalize(); }

    private Path file(UUID player) { return root.resolve(player + ".json"); }

    /** The saved glossary, or an empty one. Never throws; problems are returned to the caller via the result. */
    public synchronized Glossary load(UUID player, StringBuilder problem) {
        Path file = file(player);
        if (!Files.exists(file)) return Glossary.empty(player);
        try {
            if (Files.size(file) > MAX_FILE_BYTES) throw new IOException("Glossary exceeds size limit");
            JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            JsonObject object = parsed.getAsJsonObject();
            if (!supportedSchema(object)) {
                protectedFiles.add(player);
                problem.append("Unsupported glossary schema at ").append(file).append("; leaving it untouched");
                return Glossary.empty(player);
            }
            Glossary glossary = JSON.fromJson(object, Glossary.class);
            if (!player.equals(glossary.player())) throw new IOException("Glossary belongs to another player");
            return glossary;
        } catch (IOException | RuntimeException failure) {
            problem.append("Could not load ").append(file).append(": ").append(failure.getMessage());
            try {
                Files.move(file, file.resolveSibling(player + ".corrupt." + UUID.randomUUID() + ".json"));
            } catch (IOException quarantine) {
                protectedFiles.add(player);
            }
            return Glossary.empty(player);
        }
    }

    private static boolean supportedSchema(JsonObject object) {
        JsonElement version = object.get("schemaVersion");
        return version != null && version.isJsonPrimitive() && version.getAsJsonPrimitive().isNumber()
                && version.getAsDouble() == Math.rint(version.getAsDouble())
                && version.getAsDouble() >= 1 && version.getAsDouble() <= Glossary.SCHEMA_VERSION;
    }

    /** Returns false, without writing, for protected files or on IO failure. */
    public synchronized boolean save(Glossary glossary) {
        if (protectedFiles.contains(glossary.player())) return false;
        Path file = file(glossary.player());
        Path temporary = null;
        try {
            // Saving without a preceding load must preserve unknown formats too.
            if (Files.exists(file)) {
                if (Files.size(file) > MAX_FILE_BYTES) return false;
                JsonElement existing = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
                if (!existing.isJsonObject() || !supportedSchema(existing.getAsJsonObject())) {
                    protectedFiles.add(glossary.player());
                    return false;
                }
            }
            Files.createDirectories(root);
            temporary = Files.createTempFile(root, "glossary-", ".tmp");
            Files.writeString(temporary, JSON.toJson(glossary), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | RuntimeException failure) {
            return false;
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { /* Keep the original outcome. */ }
            }
        }
    }
}
