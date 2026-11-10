package local.villagers.mind;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import local.villagers.language.Concept;
import local.villagers.language.Lexicon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LanguagePersistenceTest {
    @TempDir java.nio.file.Path root;
    private Random clearHearing() { return new Random(1) { @Override public double nextDouble() { return .5; } }; }

    @Test void incompleteLearningAndBiomeFactionSurviveRestartAndMigration() {
        var mind = new AgentMind(UUID.randomUUID());
        mind.establishOrigin("minecraft:desert");
        var learner = new Lexicon(List.of(), List.of(), true);
        learner = learner.observe(List.of("mora"), Set.of(Concept.FOOD), clearHearing());
        mind.lexicon(learner);
        var store = AgentStore.worldScoped(root);
        assertTrue(store.save(mind));
        var loaded = store.load(mind.id());
        assertEquals(mind.lexicon(), loaded.lexicon());
        loaded.establishOrigin("minecraft:plains");
        assertEquals("minecraft:desert", loaded.faction());
        assertTrue(loaded.lexicon().developing());
        assertNull(loaded.lexicon().meaning("mora"));
        for (int i = 0; i < 3; i++)
            loaded.lexicon(loaded.lexicon().observe(List.of("mora"), Set.of(Concept.FOOD), clearHearing()));
        assertEquals(Concept.FOOD, loaded.lexicon().meaning("mora"));
        assertTrue(store.save(loaded));
        assertEquals(loaded.lexicon(), store.load(mind.id()).lexicon());
    }

    @Test void schemaEightVocabularyUpgradesWithoutReplacingWordsOrInventingAnOrigin() {
        var mind = new AgentMind(UUID.randomUUID());
        mind.lexicon(Lexicon.invent(new Random(8)));
        var json = JsonParser.parseString(new Gson().toJson(mind.snapshot())).getAsJsonObject();
        json.addProperty("schemaVersion", 8);
        json.remove("originBiome");
        json.getAsJsonObject("lexicon").remove("evidence");
        json.getAsJsonObject("lexicon").remove("developing");
        var loaded = AgentMind.fromSnapshot(new Gson().fromJson(json, AgentMind.Snapshot.class));
        assertEquals(mind.lexicon(), loaded.lexicon());
        assertNull(loaded.originBiome());
        assertEquals(AgentMind.SCHEMA_VERSION, loaded.snapshot().schemaVersion());
    }

    @Test void malformedEvidenceAndOriginAreRejected() {
        var mind = new AgentMind(UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () -> mind.establishOrigin("../../bad"));
        var evidence = new local.villagers.language.Glossary.WordEvidence("mora", 1,
                List.of(new local.villagers.language.Glossary.Meaning(Concept.FOOD, 1)));
        assertThrows(IllegalArgumentException.class, () -> new Lexicon(List.of(), List.of(evidence, evidence), true));
    }
}
