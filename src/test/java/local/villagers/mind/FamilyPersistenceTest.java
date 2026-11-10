package local.villagers.mind;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.util.Random;
import java.util.UUID;
import local.villagers.language.Lexicon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FamilyPersistenceTest {
    @TempDir java.nio.file.Path root;
    @Test void ordinaryChildInheritsBothParentsAndWordsButNotLearnedValues() {
        AgentMind a = new AgentMind(new UUID(3, 7)), b = new AgentMind(new UUID(4, 8));
        a.lexicon(Lexicon.invent(new Random(1))); b.lexicon(Lexicon.invent(new Random(2)));
        AgentMind child = AgentMind.born(new UUID(5, 9), a, b, new Random(3));
        assertEquals(a.id(), child.lineage().ancestor());
        assertEquals(b.id(), child.lineage().secondParent());
        assertFalse(child.lineage().godChild());
        assertEquals(1, child.generation());
        assertFalse(child.lexicon().isEmpty());
        assertEquals(0, child.learningUpdates());
        assertEquals(20, child.foodLevel());
        assertThrows(IllegalArgumentException.class, () -> AgentMind.born(UUID.randomUUID(), a, a, new Random(2)));
    }
    @Test void parentageAndParentCooldownSurviveReloadWithoutReset() {
        AgentMind a = new AgentMind(new UUID(3, 7)), b = new AgentMind(new UUID(4, 8));
        AgentMind child = AgentMind.born(new UUID(5, 9), a, b, new Random(3));
        a.bred(10_000); b.bred(10_000);
        AgentStore store = AgentStore.worldScoped(root);
        assertTrue(store.save(a)); assertTrue(store.save(b)); assertTrue(store.save(child));
        assertEquals(34_000, store.load(a.id()).nextBreedingAt());
        assertEquals(child.lineage(), store.load(child.id()).lineage());
        assertFalse(store.load(child.id()).lineage().godChild());
    }
    @Test void schemaSixGodChildrenUpgradeWithoutInventingASecondParent() {
        AgentMind a = new AgentMind(new UUID(3, 7));
        AgentMind divine = AgentMind.godChild(new UUID(4, 8), a, new Random(2));
        Gson gson = new Gson();
        var object = JsonParser.parseString(gson.toJson(divine.snapshot())).getAsJsonObject();
        object.addProperty("schemaVersion", 6); object.remove("nextBreedingAt");
        object.getAsJsonObject("lineage").remove("secondParent");
        object.getAsJsonObject("lineage").remove("secondParentName");
        AgentMind loaded = AgentMind.fromSnapshot(gson.fromJson(object, AgentMind.Snapshot.class));
        assertTrue(loaded.lineage().godChild());
        assertNull(loaded.lineage().secondParent());
        assertEquals(0, loaded.nextBreedingAt());
        assertEquals(a.id(), loaded.lineage().ancestor());
        assertEquals(AgentMind.SCHEMA_VERSION, loaded.snapshot().schemaVersion());
    }
    @Test void generationAndCooldownCountersSaturateAndInvalidParentageIsRefused() {
        assertEquals(Integer.MAX_VALUE, Lineage.nextGeneration(Integer.MAX_VALUE, 0));
        AgentMind a = new AgentMind(new UUID(3, 7)); a.bred(Long.MAX_VALUE - 1);
        assertEquals(Long.MAX_VALUE, a.nextBreedingAt());
        assertThrows(IllegalArgumentException.class, () -> new Lineage(a.id(), "A", 1, false));
        assertThrows(IllegalArgumentException.class, () -> new Lineage(a.id(), "A", 1, false, a.id(), "A"));
    }
}
