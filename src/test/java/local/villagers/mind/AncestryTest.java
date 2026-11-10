package local.villagers.mind;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AncestryTest {
    @TempDir Path root;
    private static final String DIM = "minecraft:overworld";
    private static AgentMind mind(long id, long age, int x) {
        AgentMind mind = new AgentMind(new UUID(0, id));
        mind.home(new Home(DIM, x, 64, 0));
        mind.advanceDrives(age, 0);
        return mind;
    }
    private static Ancestry.Neighborhood query(UUID fallback) {
        return new Ancestry.Neighborhood(DIM, .5, 64.5, .5, 64, fallback);
    }

    @Test void streamingChampionMatchesAgeThenUpdatesThenIdentityWithoutRetainingPopulation() {
        AgentStore store = AgentStore.worldScoped(root);
        AgentMind fallback = mind(1, 1, 0), eldest = mind(3, 900, 2), younger = mind(2, 100, 1);
        assertTrue(store.save(eldest)); assertTrue(store.save(younger));
        assertEquals(eldest.id(), store.bestAncestor(query(fallback.id()), fallback.snapshot(), Set.of()).id());
        AgentMind tie = mind(4, 900, 3); assertTrue(store.save(tie));
        assertEquals(tie.id(), store.bestAncestor(query(fallback.id()), fallback.snapshot(), Set.of()).id());
        eldest.learn(new Observation(0, .8, .1, 1, false, false, false, true), Action.REST,
                new Observation(0, .2, .1, 1, false, false, false, true));
        assertTrue(store.save(eldest));
        assertEquals(eldest.id(), store.bestAncestor(query(fallback.id()), fallback.snapshot(), Set.of()).id(),
                "at equal age, learning updates take precedence over the UUID tie-break");
    }

    @Test void currentHomesOverrideStaleStoredVillageMembership() {
        AgentStore store = AgentStore.worldScoped(root);
        AgentMind fallback = mind(1, 1, 0), moved = mind(2, 1000, 0), resident = mind(3, 100, 1);
        assertTrue(store.save(moved)); assertTrue(store.save(resident));
        // The loaded resident has moved away; its old on-disk home must not win.
        moved.home(new Home(DIM, 1000, 64, 0));
        assertEquals(resident.id(), store.bestAncestor(query(fallback.id()), fallback.snapshot(), Set.of(moved.id())).id());
    }

    @Test void unknownCorruptAndWrongDimensionHistoryIsSkippedWithoutMutation() throws Exception {
        AgentStore store = AgentStore.worldScoped(root);
        AgentMind fallback = mind(1, 1, 0), otherWorld = mind(2, 1000, 0);
        otherWorld.home(new Home("minecraft:the_nether", 0, 64, 0)); assertTrue(store.save(otherWorld));
        UUID future = new UUID(0, 3), corrupt = new UUID(0, 4);
        Path futureFile = Files.createDirectories(root.resolve(future.toString())).resolve("mind.json");
        Path corruptFile = Files.createDirectories(root.resolve(corrupt.toString())).resolve("mind.json");
        Files.writeString(futureFile, "{\"schemaVersion\":99}"); Files.writeString(corruptFile, "{broken");
        assertEquals(fallback.id(), store.bestAncestor(query(fallback.id()), fallback.snapshot(), Set.of()).id());
        assertEquals("{\"schemaVersion\":99}", Files.readString(futureFile));
        assertEquals("{broken", Files.readString(corruptFile));
    }

    @Test void neighborhoodUsesExactDimensionAndInclusiveDistanceAndKeepsFallbackWithoutHome() {
        AgentMind fallback = mind(1, 1, 0); var query = query(fallback.id());
        assertTrue(query.includes(fallback.id(), null));
        assertTrue(query.includes(new UUID(0, 2), new Home(DIM, 64, 64, 0)));
        assertFalse(query.includes(new UUID(0, 2), new Home(DIM, 65, 64, 0)));
        assertFalse(query.includes(new UUID(0, 2), new Home("minecraft:the_nether", 0, 64, 0)));
        assertThrows(IllegalArgumentException.class, () -> new Ancestry.Neighborhood(DIM, Double.NaN, 0, 0, 64, fallback.id()));
    }
}
