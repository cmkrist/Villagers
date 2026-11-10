package local.villagers.language;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class GlossaryPersistenceTest {
    @TempDir Path root;
    private static final UUID PLAYER = new UUID(8, 9);

    @Test void reconnectUsesPendingLessonsWhileWriterIsBlocked() throws Exception {
        var writer = Executors.newSingleThreadExecutor();
        var release = new CountDownLatch(1);
        writer.execute(() -> {
            try { release.await(); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        });
        GlossaryBook book = new GlossaryBook(new GlossaryStore(root), p -> fail(p), writer);
        try {
            book.join(PLAYER);
            book.update(PLAYER, g -> g.hear("mora", Set.of(Concept.FOOD)));
            book.leave(PLAYER);
            book.join(PLAYER);
            assertEquals(1, book.get(PLAYER).words().size());
            book.update(PLAYER, g -> g.hear("mora", Set.of(Concept.FOOD)));
        } finally {
            release.countDown();
            assertTrue(book.close(5000));
        }
        Glossary saved = new GlossaryStore(root).load(PLAYER, new StringBuilder());
        assertEquals(2, saved.words().getFirst().heard());
    }

    @Test void failedWriteIsRetainedAndRetriedAfterDiskRecovers() throws Exception {
        Path obstruction = root.resolve("players");
        Files.writeString(obstruction, "not a directory");
        var writer = Executors.newSingleThreadExecutor();
        GlossaryBook book = new GlossaryBook(new GlossaryStore(root), p -> {}, writer);
        try {
            book.update(PLAYER, g -> g.hear("mora", Set.of(Concept.FOOD)));
            book.leave(PLAYER);
            writer.submit(() -> {}).get(5, TimeUnit.SECONDS);
            book.join(PLAYER);
            assertEquals(1, book.get(PLAYER).words().size());
            Files.delete(obstruction);
        } finally {
            assertTrue(book.close(5000));
        }
        assertEquals(1, new GlossaryStore(root).load(PLAYER, new StringBuilder()).words().size());
    }

    @Test void freshStoreNeverOverwritesFutureSchemaWithoutLoad() throws Exception {
        Path file = Files.createDirectories(root.resolve("players")).resolve(PLAYER + ".json");
        String future = "{\"schemaVersion\":99,\"futureData\":true}";
        Files.writeString(file, future);
        assertFalse(new GlossaryStore(root).save(Glossary.empty(PLAYER)));
        assertEquals(future, Files.readString(file));
    }

    @Test void shutdownReportsUnsavedChanges() throws Exception {
        Files.writeString(root.resolve("players"), "blocked");
        GlossaryBook book = new GlossaryBook(new GlossaryStore(root), p -> {});
        book.update(PLAYER, g -> g.hear("mora", Set.of(Concept.FOOD)));
        assertFalse(book.close(5000));
    }
}
