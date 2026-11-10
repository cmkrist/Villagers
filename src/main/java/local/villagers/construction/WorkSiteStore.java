package local.villagers.construction;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Bounded control metadata, never called by ordinary AI polling. A dirty-session marker fails closed after crashes. */
public class WorkSiteStore implements AutoCloseable {
    public static final int MAX_BYTES = 131_072;
    private static final Gson JSON = new Gson();
    private final Path file, marker;
    private WorkSites state = WorkSites.empty();
    private boolean healthy;
    private String problem;

    public WorkSiteStore(Path file) {
        this.file = file; marker = file.resolveSibling(file.getFileName() + ".active");
        try {
            if (Files.isSymbolicLink(file) || Files.isSymbolicLink(marker)) throw new IOException("Site metadata must be regular");
            if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isRegularFile(file) || Files.size(file) > MAX_BYTES) throw new IOException("Invalid site metadata size/type");
                state = JSON.fromJson(Files.readString(file), WorkSites.class);
                if (state == null) throw new IOException("Empty site metadata");
            }
            boolean unclean = Files.exists(marker, LinkOption.NOFOLLOW_LINKS);
            if (unclean && !Files.isRegularFile(marker)) throw new IOException("Invalid site marker");
            Files.createDirectories(file.getParent());
            if (!unclean) {
                try (var channel = FileChannel.open(marker, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                    channel.write(ByteBuffer.wrap("active\n".getBytes(StandardCharsets.UTF_8))); channel.force(true);
                }
                forceDirectory();
            }
            if (unclean) {
                state = state.revokeEdits(); write(state);
                problem = "Unclean prior shutdown: farm/station/forestry/soil/step grants revoked; protected areas retained";
            }
            healthy = true;
        } catch (Exception failure) { problem = failure.toString(); healthy = false; }
    }
    public WorkSites snapshot() { return state; }
    public boolean healthy() { return healthy; }
    public String problem() { return problem; }
    public boolean allows(WorkSites.Point soil) { return healthy && state.allows(soil); }
    public boolean update(WorkSites next) {
        if (!healthy) return false;
        state = next; // Revocations take effect even if persistence fails.
        try { write(next); return true; }
        catch (Exception failure) { problem = failure.toString(); healthy = false; return false; }
    }
    protected void write(WorkSites next) throws IOException {
        byte[] bytes = JSON.toJson(next).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_BYTES) throw new IOException("Site metadata exceeds bound");
        if (Files.isSymbolicLink(file)) throw new IOException("Site metadata became a symlink");
        Path temporary = Files.createTempFile(file.getParent(), ".work-sites-", ".tmp");
        try {
            try (var channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                var buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) channel.write(buffer); channel.force(true);
            }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            forceDirectory();
        } finally { Files.deleteIfExists(temporary); }
    }
    private void forceDirectory() throws IOException {
        try (var directory = FileChannel.open(file.getParent(), StandardOpenOption.READ)) { directory.force(true); }
    }
    @Override public void close() {
        if (!healthy) return; // Keep the active marker: next boot must revoke old grants.
        try { write(state); Files.delete(marker); forceDirectory(); healthy = false; }
        catch (Exception failure) { problem = failure.toString(); healthy = false; }
    }
}
