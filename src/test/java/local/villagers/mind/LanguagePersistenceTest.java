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

    @Test void learnedPeoplePlacesAndTheirClaimsSurviveRestartWhileSchemaElevenDoesNotInventReferences(){
        var mind=new AgentMind(UUID.randomUUID());mind.establishOrigin("minecraft:forest");
        var names=local.villagers.language.Referents.empty().establish("fern",local.villagers.language.Referents.Target.person(UUID.randomUUID()),10)
                .establish("rivermill",local.villagers.language.Referents.Target.place(new Place("minecraft:overworld",3,64,4)),10);
        var language=local.villagers.language.BiomeDialect.foundingLanguage(mind.originBiome()).withReferents(names);
        mind.lexicon(language);
        var said=local.villagers.language.Sentence.speak(List.of(local.villagers.language.Clause.named(names.resolve("rivermill"),Concept.HAVE,Concept.FOOD)),language);
        mind.beliefs(mind.beliefs().hear(local.villagers.language.Sentence.interpret(said.sounds(),language),UUID.randomUUID(),mind.id(),.5,20));
        var store=AgentStore.worldScoped(root);assertTrue(store.save(mind));
        var loaded=store.load(mind.id());assertEquals(mind.lexicon(),loaded.lexicon());assertEquals(mind.beliefs(),loaded.beliefs());
        var json=JsonParser.parseString(new Gson().toJson(mind.snapshot())).getAsJsonObject();
        json.addProperty("schemaVersion",11);json.getAsJsonObject("lexicon").remove("referents");
        json.remove("beliefs");
        var old=AgentMind.fromSnapshot(new Gson().fromJson(json,AgentMind.Snapshot.class));
        assertTrue(old.lexicon().referents().entries().isEmpty());
        for(Concept c:Concept.values())assertEquals(language.wordFor(c),old.lexicon().wordFor(c));
    }
    @Test void schemaElevenPersonClaimsKeepTheirResolvedIdentityAndCanonicalRepresentation(){
        var mind=new AgentMind(UUID.randomUUID());var source=UUID.randomUUID();var person=UUID.randomUUID();
        var claim=new Beliefs.Claim(source,Concept.LIKE,Concept.YOU,person,false,false);
        mind.beliefs(new Beliefs(List.of(new Beliefs.Entry(claim,source,Beliefs.Evidence.HEARD,.4,10,100))));
        var json=JsonParser.parseString(new Gson().toJson(mind.snapshot())).getAsJsonObject();
        json.addProperty("schemaVersion",11);json.getAsJsonObject("lexicon").remove("referents");
        var loaded=AgentMind.fromSnapshot(new Gson().fromJson(json,AgentMind.Snapshot.class));
        assertEquals(mind.beliefs(),loaded.beliefs());
        assertEquals(person,loaded.beliefs().entries().getFirst().claim().objectPerson());
        assertEquals(Concept.YOU,loaded.beliefs().entries().getFirst().claim().object());
    }

    @Test void beliefsAndInducedConstructionsSurviveRestartWhileSchemaTenUpgradesWithoutInventedBeliefs() {
        var mind = new AgentMind(UUID.randomUUID());
        mind.establishOrigin("minecraft:plains");
        var lexicon=local.villagers.language.BiomeDialect.foundingLanguage(mind.originBiome());
        lexicon=lexicon.withGrammar(lexicon.grammar().learn(new local.villagers.language.Construction(
                List.of(local.villagers.language.Grammar.Role.SUBJECT,local.villagers.language.Grammar.Role.OBJECT),Concept.IS,7)));
        mind.lexicon(lexicon);
        var source=UUID.randomUUID();
        var interpretation=new local.villagers.language.Sentence.Interpretation(List.of(
                new local.villagers.language.Clause(Concept.I,Concept.HAVE,Concept.FOOD)),1,0);
        mind.beliefs(mind.beliefs().hear(interpretation,source,mind.id(),.5,100));
        var store=AgentStore.worldScoped(root);assertTrue(store.save(mind));
        var loaded=store.load(mind.id());
        assertEquals(mind.beliefs(),loaded.beliefs());assertEquals(mind.lexicon(),loaded.lexicon());
        var old=JsonParser.parseString(new Gson().toJson(mind.snapshot())).getAsJsonObject();
        old.addProperty("schemaVersion",10);old.remove("beliefs");
        old.getAsJsonObject("lexicon").getAsJsonObject("grammar").remove("constructions");
        var migrated=AgentMind.fromSnapshot(new Gson().fromJson(old,AgentMind.Snapshot.class));
        assertTrue(migrated.beliefs().entries().isEmpty());
        assertTrue(migrated.lexicon().grammar().constructions().isEmpty());
        for(Concept c:Concept.values())assertEquals(mind.lexicon().wordFor(c),migrated.lexicon().wordFor(c));
    }

    @Test void learnedGrammarAndChildConstructionPreferenceSurviveSaveAndReload() {
        var mind = new AgentMind(UUID.randomUUID());
        mind.establishOrigin("minecraft:desert");
        var lexicon = local.villagers.language.BiomeDialect.foundingLanguage(mind.originBiome());
        for (int i = 0; i < 7; i++) lexicon = lexicon.withGrammar(lexicon.grammar().reinforce(local.villagers.language.Grammar.Order.VSO, 1));
        mind.lexicon(lexicon);
        var store = AgentStore.worldScoped(root);
        assertTrue(store.save(mind));
        var loaded = store.load(mind.id());
        assertEquals(lexicon, loaded.lexicon());
        assertEquals(local.villagers.language.Grammar.Order.VSO, loaded.lexicon().grammar().preferred());
        var child = Lexicon.child(lexicon, lexicon, clearHearing());
        assertTrue(child.developing());
        assertEquals(lexicon.grammar().preferred(), child.grammar().preferred());
        assertEquals(2, child.grammar().evidence().stream().mapToInt(Integer::intValue).sum());
    }

    @Test void oldMindGrammarMigrationKeepsAllOldWordsAndUsesSavedOrigin() {
        var mind = new AgentMind(UUID.randomUUID());
        mind.establishOrigin("minecraft:desert");
        mind.lexicon(new Lexicon(List.of(new Lexicon.Entry(Concept.FOOD, "mora", 1))));
        var json = JsonParser.parseString(new Gson().toJson(mind.snapshot())).getAsJsonObject();
        json.addProperty("schemaVersion", 9);
        json.getAsJsonObject("lexicon").remove("grammar");
        var loaded = AgentMind.fromSnapshot(new Gson().fromJson(json, AgentMind.Snapshot.class));
        loaded.lexicon(loaded.lexicon().establishGrammar(loaded.originBiome()));
        assertEquals("mora", loaded.lexicon().wordFor(Concept.FOOD));
        assertEquals(local.villagers.language.Grammar.Order.SOV, loaded.lexicon().grammar().preferred());
        assertNotNull(loaded.lexicon().wordFor(Concept.I));
        assertTrue(new Lexicon(List.of(), List.of(), true).establishGrammar("minecraft:desert").entries().isEmpty());
    }

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

    @Test void sourceProvenanceSurvivesRestartAndSchemaTwelveMigratesWithoutInventingAnOrigin(){
        var mind=new AgentMind(UUID.randomUUID());var speaker=UUID.randomUUID();var rootSource=UUID.randomUUID();
        var claim=new Beliefs.Claim(speaker,Concept.HAVE,Concept.FOOD,null,false,false);
        mind.beliefs(new Beliefs(List.of(new Beliefs.Entry(claim,speaker,Beliefs.Evidence.HEARD,.6,100,600,
                new Beliefs.Provenance(Beliefs.Basis.REPORTED_HEARSAY,rootSource,50)))));
        var store=AgentStore.worldScoped(root);assertTrue(store.save(mind));assertEquals(mind.beliefs(),store.load(mind.id()).beliefs());
        var json=JsonParser.parseString(new Gson().toJson(mind.snapshot())).getAsJsonObject();json.addProperty("schemaVersion",12);
        json.getAsJsonObject("beliefs").getAsJsonArray("entries").get(0).getAsJsonObject().remove("provenance");
        var migrated=AgentMind.fromSnapshot(new Gson().fromJson(json,AgentMind.Snapshot.class));
        var p=migrated.beliefs().entries().getFirst().provenance();
        assertEquals(Beliefs.Basis.UNQUALIFIED,p.basis());assertEquals(speaker,p.root());assertEquals(100,p.basisTick());
    }
}
