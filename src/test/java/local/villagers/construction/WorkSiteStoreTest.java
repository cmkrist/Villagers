package local.villagers.construction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static local.villagers.construction.WorkSitesTest.*;

class WorkSiteStoreTest {
    @TempDir Path temp;
    private Path file() { return temp.resolve("work-sites.json"); }

    @Test void cleanReopenPreservesExplicitPermissionAndProtectedAreas() {
        var allowed = WorkSites.empty().add(farm(SOIL));
        try (var store = new WorkSiteStore(file())) { assertTrue(store.update(allowed)); assertTrue(store.allows(SOIL)); }
        try (var loaded = new WorkSiteStore(file())) {
            assertTrue(loaded.healthy()); assertTrue(loaded.allows(SOIL));
            assertTrue(loaded.update(loaded.snapshot().add(protection())));
        }
        try (var protectedStore = new WorkSiteStore(file())) { assertFalse(protectedStore.allows(SOIL)); assertEquals(1, protectedStore.snapshot().protections().size()); }
    }

    @Test void uncleanReopenRevokesOldGrantsAndRetainsProtection() {
        var abandoned = new WorkSiteStore(file()); // Simulate a dead process: do not close it later.
        assertTrue(abandoned.update(WorkSites.empty().add(farm(SOIL)).add(protection())));
        try (var recovered = new WorkSiteStore(file())) {
            assertTrue(recovered.healthy()); assertTrue(recovered.snapshot().farms().isEmpty());
            assertEquals(1, recovered.snapshot().protections().size()); assertFalse(recovered.allows(SOIL));
        }
    }

    @Test void failedRevocationDisablesNowAndCannotReenableStalePermissionOnRestart() {
        class Failing extends WorkSiteStore {
            boolean fail;
            Failing() { super(file()); }
            @Override protected void write(WorkSites next) throws IOException {
                if (fail) throw new IOException("simulated full disk"); super.write(next);
            }
        }
        var broken = new Failing(); assertTrue(broken.update(WorkSites.empty().add(farm(SOIL))));
        broken.fail = true; assertFalse(broken.update(broken.snapshot().touch(SOIL)));
        assertFalse(broken.healthy()); assertFalse(broken.allows(SOIL)); broken.close();
        try (var recovered = new WorkSiteStore(file())) { assertTrue(recovered.healthy()); assertFalse(recovered.allows(SOIL)); }
    }

    @Test void unknownSchemaRemainsUntouchedAndNeverGrantsEdits() throws Exception {
        String future = "{\"schemaVersion\":99,\"farms\":[],\"protections\":[]}"; Files.writeString(file(), future);
        try (var store = new WorkSiteStore(file())) { assertFalse(store.healthy()); assertFalse(store.allows(SOIL)); assertFalse(store.update(WorkSites.empty())); }
        assertEquals(future, Files.readString(file()));
    }

    @Test void oversizedMetadataIsPreservedAndUnavailable() throws Exception {
        byte[] bytes = new byte[WorkSiteStore.MAX_BYTES + 1]; Files.write(file(), bytes);
        try (var store = new WorkSiteStore(file())) { assertFalse(store.healthy()); }
        assertArrayEquals(bytes, Files.readAllBytes(file()));
    }

    @Test void symlinksAreRefusedWithoutTouchingTheirTarget() throws Exception {
        Path target = temp.resolve("keep.json"); Files.writeString(target, "keep"); Files.createSymbolicLink(file(), target);
        try (var store = new WorkSiteStore(file())) { assertFalse(store.healthy()); }
        assertEquals("keep", Files.readString(target));
    }

    @Test void legacySchemaOneMigratesWithoutInventingStationAuthority() throws Exception {
        var old = new com.google.gson.JsonObject(); old.addProperty("schemaVersion", 1);
        var gson = new com.google.gson.Gson();
        old.add("farms", gson.toJsonTree(java.util.List.of(farm(SOIL))));
        old.add("protections", gson.toJsonTree(java.util.List.of(protection())));
        Files.writeString(file(), gson.toJson(old));
        try (var loaded = new WorkSiteStore(file())) {
            assertTrue(loaded.healthy()); assertEquals(WorkSites.SCHEMA, loaded.snapshot().schemaVersion());
            assertEquals(1, loaded.snapshot().farms().size()); assertEquals(1, loaded.snapshot().protections().size());
            assertTrue(loaded.snapshot().stations().isEmpty());
        }
        assertTrue(Files.readString(file()).contains("\"stations\":[]"));
    }

    @Test void stationSurvivesCleanReopenButDirtySessionRevokesItAndPreservesProtection() {
        var station = new WorkSites.Station(java.util.UUID.randomUUID(), OWNER, SOIL.above(1));
        try (var store = new WorkSiteStore(file())) { assertTrue(store.update(WorkSites.empty().add(station))); }
        var abandoned = new WorkSiteStore(file());
        assertTrue(abandoned.snapshot().allowsStation(station.target()));
        assertTrue(abandoned.update(abandoned.snapshot().add(protection())));
        try (var recovered = new WorkSiteStore(file())) {
            assertTrue(recovered.healthy()); assertTrue(recovered.snapshot().stations().isEmpty());
            assertEquals(1, recovered.snapshot().protections().size());
        }
    }
}
