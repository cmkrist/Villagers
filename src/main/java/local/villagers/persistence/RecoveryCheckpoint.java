package local.villagers.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Compact world-scoped checkpoint. Run its disk methods on the existing save worker. */
public final class RecoveryCheckpoint {
    public static final int LIMIT = 64;
    private static final int MAX_BYTES = 131_072;
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> FIELDS = List.of("dimension", "deceased", "x", "y", "z", "centerX", "centerY", "centerZ");
    public record Ticket(String dimension, UUID deceased, double x, double y, double z,
                         double centerX, double centerY, double centerZ) {
        public Ticket {
            Objects.requireNonNull(deceased);
            if (dimension == null || dimension.length() > 256 || !dimension.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))
                throw new IllegalArgumentException("Invalid recovery dimension");
            for (double coordinate : new double[]{x, y, z, centerX, centerY, centerZ})
                if (!Double.isFinite(coordinate) || Math.abs(coordinate) > 30_000_000)
                    throw new IllegalArgumentException("Invalid recovery coordinate");
        }
    }
    private record Document(int schemaVersion, List<Ticket> requests) {}
    private final Path file;
    private boolean protectedFile;
    private String lastError;

    public RecoveryCheckpoint(Path file) { this.file = file; }
    public String lastError() { return lastError; }

    public static List<Ticket> checked(List<Ticket> requests) {
        List<Ticket> copy = List.copyOf(requests);
        if (copy.size() > LIMIT) throw new IllegalArgumentException("Recovery checkpoint capacity exceeded");
        var ids = new HashSet<UUID>();
        for (Ticket ticket : copy)
            if (!ids.add(ticket.deceased())) throw new IllegalArgumentException("Duplicate recovery identity");
        return copy;
    }

    /** Invalid/future documents are left untouched and protect subsequent writes for this store lifetime. */
    public List<Ticket> load() throws IOException {
        try {
            if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return List.of();
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Recovery checkpoint is not a regular file");
            byte[] bytes;
            try (var input = Files.newInputStream(file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                bytes = input.readNBytes(MAX_BYTES + 1);
            }
            if (bytes.length > MAX_BYTES) throw new IOException("Recovery checkpoint exceeds size limit");
            var object = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            var version = object.get("schemaVersion");
            if (version == null || !version.isJsonPrimitive() || !version.getAsJsonPrimitive().isNumber()
                    || version.getAsDouble() != 1) throw new IOException("Unsupported recovery checkpoint schema");
            var requests = object.getAsJsonArray("requests");
            if (requests == null || requests.size() > LIMIT) throw new IOException("Invalid recovery request list");
            var result = new java.util.ArrayList<Ticket>();
            for (var value : requests) {
                var entry = value.getAsJsonObject();
                for (String field : FIELDS)
                    if (!entry.has(field) || entry.get(field).isJsonNull()) throw new IOException("Missing recovery field " + field);
                result.add(JSON.fromJson(entry, Ticket.class));
            }
            return checked(result);
        } catch (IOException | RuntimeException failure) {
            protectedFile = true;
            lastError = "Could not read recovery checkpoint " + file + ": " + failure.getMessage();
            throw new IOException(lastError, failure);
        }
    }

    public boolean save(List<Ticket> requests) {
        Path temporary = null;
        if (protectedFile) return false;
        try {
            List<Ticket> current = checked(requests);
            // A fresh store must protect unknown documents even before its first explicit load.
            load();
            byte[] bytes = JSON.toJson(new Document(1, current)).getBytes(StandardCharsets.UTF_8);
            if (bytes.length > MAX_BYTES) throw new IOException("Recovery checkpoint exceeds size limit");
            Files.createDirectories(file.toAbsolutePath().getParent());
            temporary = Files.createTempFile(file.toAbsolutePath().getParent(), "recoveries-", ".tmp");
            Files.write(temporary, bytes);
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            lastError = null;
            return true;
        } catch (IOException | RuntimeException failure) {
            lastError = "Could not save recovery checkpoint " + file + ": " + failure.getMessage();
            return false;
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }
}
