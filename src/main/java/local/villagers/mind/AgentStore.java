package local.villagers.mind;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** A store rooted in one world's directory. Call disk operations on an IO worker, not every entity tick. */
public final class AgentStore {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long MAX_FILE_BYTES = 1_048_576;
    private final Path root;
    // Concurrent so the server thread can check protection without waiting on in-progress disk IO.
    private final Set<UUID> protectedFiles = ConcurrentHashMap.newKeySet();
    private String lastError;

    private AgentStore(Path root) { this.root = root.toAbsolutePath().normalize(); }
    public static AgentStore worldScoped(Path root) { return new AgentStore(root); }
    public Path root() { return root; }
    public synchronized String lastError() { return lastError; }
    /** True once a file is known to be unsafe to overwrite; every later save for this identity will be refused. */
    public boolean isProtected(UUID id) { return protectedFiles.contains(id); }

    /** Worker-only historical read; absent/corrupt/future history never becomes a fresh founder or a write. */
    public synchronized AgentMind.Snapshot readExisting(UUID id) {
        Path file = root.resolve(id.toString()).resolve("mind.json");
        try {
            if (!Files.isRegularFile(file) || Files.size(file) > MAX_FILE_BYTES) return null;
            var object = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!supportedSchema(object)) return null;
            var state = JSON.fromJson(object, AgentMind.Snapshot.class);
            return id.equals(state.id()) ? AgentMind.fromSnapshot(state).snapshot() : null;
        } catch (IOException | RuntimeException unreadable) { return null; }
    }

    public synchronized AgentMind load(UUID id) {
        Path file = root.resolve(id.toString()).resolve("mind.json");
        if (!Files.exists(file)) return new AgentMind(id);
        try {
            if (Files.size(file) > MAX_FILE_BYTES) throw new IOException("Mind file exceeds size limit");
            JsonObject object = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!supportedSchema(object)) {
                protectedFiles.add(id);
                lastError = "Protected unsupported mind schema at " + file;
                return new AgentMind(id);
            }
            AgentMind.Snapshot state = JSON.fromJson(object, AgentMind.Snapshot.class);
            if (!id.equals(state.id())) throw new IOException("Mind identity does not match its directory");
            return AgentMind.fromSnapshot(state);
        } catch (IOException | RuntimeException failure) {
            lastError = "Could not load " + file + ": " + failure.getMessage();
            try {
                Files.move(file, file.resolveSibling("mind.corrupt." + UUID.randomUUID() + ".json"));
            } catch (IOException quarantineFailure) {
                protectedFiles.add(id);
                lastError += "; quarantine failed: " + quarantineFailure.getMessage();
            }
            return new AgentMind(id);
        }
    }

    /**
     * Every readable mind in this world, living or dead, without side effects: unreadable, corrupt or newer-schema
     * files are skipped rather than quarantined. Blocking disk IO; for rare events only.
     */
    public synchronized List<AgentMind> readAll() {
        List<AgentMind> minds = new ArrayList<>();
        if (!Files.isDirectory(root)) return minds;
        try (var directories = Files.list(root)) {
            for (Path directory : (Iterable<Path>) directories::iterator) {
                UUID id;
                try { id = UUID.fromString(directory.getFileName().toString()); } catch (IllegalArgumentException notAMind) { continue; }
                Path file = directory.resolve("mind.json");
                try {
                    if (!Files.isRegularFile(file) || Files.size(file) > MAX_FILE_BYTES) continue;
                    JsonObject object = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                    if (!supportedSchema(object)) continue;
                    AgentMind.Snapshot state = JSON.fromJson(object, AgentMind.Snapshot.class);
                    if (id.equals(state.id())) minds.add(AgentMind.fromSnapshot(state));
                } catch (IOException | RuntimeException unreadable) { /* Skipped; a normal load reports and quarantines it. */ }
            }
        } catch (IOException failure) {
            lastError = "Could not list " + root + ": " + failure.getMessage();
        }
        return minds;
    }

    /** Worker-only streaming reduction: retain one champion, not a list of the entire lifetime population. */
    public synchronized AgentMind.Snapshot bestAncestor(Ancestry.Neighborhood query, AgentMind.Snapshot current,
                                                       Set<UUID> overriddenIds) {
        AgentMind.Snapshot best = AgentMind.fromSnapshot(current).snapshot();
        if (!query.includes(best)) throw new IllegalArgumentException("Current ancestry candidate is outside the query");
        if (!Files.isDirectory(root)) return best;
        try (var directories = Files.list(root)) {
            for (Path directory : (Iterable<Path>) directories::iterator) {
                UUID id;
                try { id = UUID.fromString(directory.getFileName().toString()); }
                catch (IllegalArgumentException notAMind) { continue; }
                if (overriddenIds.contains(id)) continue;
                Path file = directory.resolve("mind.json");
                try {
                    if (!Files.isRegularFile(file) || Files.size(file) > MAX_FILE_BYTES) continue;
                    var object = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                    if (!supportedSchema(object)) continue;
                    var snapshot = JSON.fromJson(object, AgentMind.Snapshot.class);
                    if (!id.equals(snapshot.id())) continue;
                    snapshot = AgentMind.fromSnapshot(snapshot).snapshot();
                    if (query.includes(snapshot)) best = Ancestry.better(best, snapshot);
                } catch (IOException | RuntimeException unreadable) { /* No quarantine or writes in historical lookup. */ }
            }
        } catch (IOException failure) {
            throw new java.io.UncheckedIOException("Could not enumerate historical ancestry", failure);
        }
        return best;
    }

    public boolean save(AgentMind mind) { return save(mind.snapshot()); }

    public synchronized boolean save(AgentMind.Snapshot snapshot) {
        UUID id = snapshot.id();
        Path directory = root.resolve(id.toString());
        Path file = directory.resolve("mind.json");
        Path temporary = null;
        if (protectedFiles.contains(id)) {
            lastError = "Refusing to overwrite protected mind " + id;
            return false;
        }
        try {
            // Validate caller-created DTOs as well as deserialized state.
            AgentMind.Snapshot currentSnapshot = AgentMind.fromSnapshot(snapshot).snapshot();
            // A fresh store must also protect files written by a newer version before load was called.
            if (Files.exists(file)) {
                if (Files.size(file) > MAX_FILE_BYTES) throw new IOException("Existing mind exceeds size limit");
                JsonElement existing = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
                if (!existing.isJsonObject() || !supportedSchema(existing.getAsJsonObject())) {
                    protectedFiles.add(id);
                    lastError = "Refusing to overwrite unsupported mind schema at " + file;
                    return false;
                }
            }
            Files.createDirectories(directory);
            temporary = Files.createTempFile(directory, "mind-", ".tmp");
            Files.writeString(temporary, JSON.toJson(currentSnapshot), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            lastError = null;
            return true;
        } catch (IOException | RuntimeException failure) {
            lastError = "Could not save " + file + ": " + failure.getMessage();
            return false;
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { /* Preserve the original failure. */ }
            }
        }
    }

    private static boolean supportedSchema(JsonObject object) {
        JsonElement version = object.get("schemaVersion");
        if (version == null || !version.isJsonPrimitive() || !version.getAsJsonPrimitive().isNumber()) return false;
        // Accept every released schema up to the current one; anything newer is protected from overwrite.
        double number = version.getAsDouble();
        return number == Math.rint(number) && number >= 1 && number <= AgentMind.SCHEMA_VERSION;
    }
}
