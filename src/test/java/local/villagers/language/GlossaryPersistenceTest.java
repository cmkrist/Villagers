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

    @Test void learnedPlayerReferencesSurviveRestartWithoutGrantingNamesToOldGlossaries(){
        var names=Referents.empty().establish("fern",Referents.Target.person(new UUID(9,9)),0);
        var glossary=Glossary.empty(PLAYER).withReferents(names);
        var store=new GlossaryStore(root);assertTrue(store.save(glossary));
        assertEquals(glossary,store.load(PLAYER,new StringBuilder()));
        var old=new com.google.gson.Gson().fromJson("{\"schemaVersion\":3,\"player\":\""+PLAYER+"\",\"words\":[]}",Glossary.class);
        assertTrue(old.referents().entries().isEmpty());
    }

    @Test void playerGrammarSurvivesReloadAndSchemaOneUpgradesWithoutLosingEvidence() throws Exception {
        var old = "{\"schemaVersion\":1,\"player\":\"" + PLAYER + "\",\"words\":[{\"word\":\"mora\",\"heard\":3,\"meanings\":[{\"concept\":\"FOOD\",\"count\":3}]}]}";
        Files.createDirectories(root.resolve("players"));
        Files.writeString(root.resolve("players").resolve(PLAYER + ".json"), old);
        var store = new GlossaryStore(root);
        var loaded = store.load(PLAYER, new StringBuilder());
        assertEquals(Concept.FOOD, loaded.translate("mora"));
        assertFalse(loaded.grammar().initialized());
        var next = loaded.withGrammar(Grammar.founding(Grammar.Order.VSO));
        assertTrue(store.save(next));
        assertEquals(next, new GlossaryStore(root).load(PLAYER, new StringBuilder()));
    }

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
