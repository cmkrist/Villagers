package local.villagers.mind;

import java.util.*;
import com.google.gson.*;
import local.villagers.language.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ReceptiveGrammarPersistenceTest {
    @TempDir java.nio.file.Path root;
    @Test void incompleteReceptionAndStableNativeSpeechSurviveAnIndependentReload(){
        var m=new AgentMind(UUID.randomUUID());m.establishOrigin("minecraft:plains");
        var language=BiomeDialect.foundingLanguage(m.originBiome());var other=new Lexicon(language.entries(),List.of(),false,Grammar.founding(Grammar.Order.SOV));
        var event=new Clause(Concept.I,Concept.SEE,Concept.BREAD);var words=Sentence.speak(List.of(event),other).sounds();
        language=language.withGrammar(language.grammar().observe(words,language::meaning,event));m.lexicon(language);
        var store=AgentStore.worldScoped(root);assertTrue(store.save(m));var loaded=AgentStore.worldScoped(root).load(m.id());
        assertEquals(language,loaded.lexicon());assertEquals(Grammar.Order.SVO,loaded.lexicon().grammar().preferred());
        assertEquals(1,loaded.lexicon().grammar().receptive().dialects().getFirst().patterns().getFirst().evidence());
        assertFalse(Sentence.interpret(words,loaded.lexicon()).complete());
        var g=loaded.lexicon().grammar();for(int i=0;i<2;i++)g=g.observe(words,loaded.lexicon()::meaning,event);
        loaded.lexicon(loaded.lexicon().withGrammar(g));assertTrue(Sentence.interpret(words,loaded.lexicon()).complete());
        assertTrue(store.save(loaded));assertEquals(loaded.lexicon(),AgentStore.worldScoped(root).load(m.id()).lexicon());
    }
    @Test void schemaFifteenDoesNotInventForeignReceptionOrEraseExistingCommitments(){
        var m=new AgentMind(UUID.randomUUID());m.establishOrigin("minecraft:plains");m.lexicon(BiomeDialect.foundingLanguage(m.originBiome()));
        m.partnerships(m.partnerships().form(UUID.randomUUID(),100));
        var json=new Gson().toJsonTree(m.snapshot()).getAsJsonObject();json.addProperty("schemaVersion",15);json.getAsJsonObject("lexicon").getAsJsonObject("grammar").remove("receptive");
        var loaded=AgentMind.fromSnapshot(new Gson().fromJson(json,AgentMind.Snapshot.class));
        assertTrue(loaded.lexicon().grammar().receptive().dialects().isEmpty());assertEquals(m.partnerships(),loaded.partnerships());
        assertEquals(m.lexicon().entries(),loaded.lexicon().entries());assertEquals(m.lexicon().grammar().preferred(),loaded.lexicon().grammar().preferred());
    }
    @Test void playerReceptiveEvidenceRoundTripsAndSchemaFourStartsWithoutFabricatedProfiles(){
        var language=BiomeDialect.foundingLanguage("minecraft:plains");var event=new Clause(Concept.I,Concept.SEE,Concept.BREAD);var words=Sentence.speak(List.of(event),language).sounds();
        var g=Glossary.empty(UUID.randomUUID()).withGrammar(language.grammar().observe(words,language::meaning,event));
        var gson=new Gson();assertEquals(g,gson.fromJson(gson.toJson(g),Glossary.class));
        var json=gson.toJsonTree(g).getAsJsonObject();json.addProperty("schemaVersion",4);json.getAsJsonObject("grammar").remove("receptive");
        var migrated=gson.fromJson(json,Glossary.class);assertTrue(migrated.grammar().receptive().dialects().isEmpty());assertEquals(Glossary.SCHEMA_VERSION,migrated.schemaVersion());
    }
}
