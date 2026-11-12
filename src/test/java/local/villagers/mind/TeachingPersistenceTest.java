package local.villagers.mind;

import java.util.*;
import com.google.gson.*;
import local.villagers.language.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class TeachingPersistenceTest {
    @TempDir java.nio.file.Path root;
    @Test void partialRulesAndTeachersPublicHistoryRoundTripWithoutGrantingMastery(){
        var m=new AgentMind(UUID.randomUUID());m.establishOrigin("minecraft:plains");var parent=BiomeDialect.foundingLanguage(m.originBiome());
        var learner=new Lexicon(parent.entries(),List.of(),true,parent.grammar()).withFormation(WordFormation.empty());
        var raw=Sentence.speak(Tuition.demonstrate(Concept.WOODEN_HOE),parent).sounds();
        for(int i=0;i<2;i++)learner=Tuition.observe(learner,raw,Set.of(Concept.WOOD,Concept.HOE,Concept.WOODEN_HOE),new Random(i));
        m.lexicon(learner);var peer=UUID.randomUUID();m.lessons(m.lessons().demonstrate(peer,Concept.WOOD,50));
        var store=AgentStore.worldScoped(root);assertTrue(store.save(m));var loaded=AgentStore.worldScoped(root).load(m.id());
        assertEquals(m.lexicon(),loaded.lexicon());assertEquals(m.lessons(),loaded.lessons());assertEquals(2,loaded.lexicon().formation().patterns().getFirst().evidence());
        assertNull(loaded.lexicon().wordFor(Concept.STONE_SWORD));assertFalse(loaded.lessons().confirmed(peer,Concept.WOOD));
        loaded.lexicon(Tuition.observe(loaded.lexicon(),raw,Set.of(Concept.WOOD,Concept.HOE,Concept.WOODEN_HOE),new Random(99)));
        assertEquals(Concept.STONE_SWORD,loaded.lexicon().meaning(parent.wordFor(Concept.STONE_SWORD)));
    }
    @Test void schemaSixteenPreservesOldWordsAndDoesNotFabricateLessonsOrRules(){
        var m=new AgentMind(UUID.randomUUID());m.lexicon(BiomeDialect.foundingLanguage("minecraft:plains"));m.partnerships(m.partnerships().form(UUID.randomUUID(),3));
        var gson=new Gson();var json=gson.toJsonTree(m.snapshot()).getAsJsonObject();json.addProperty("schemaVersion",16);json.remove("lessons");json.getAsJsonObject("lexicon").remove("formation");
        var loaded=AgentMind.fromSnapshot(gson.fromJson(json,AgentMind.Snapshot.class));assertEquals(m.id(),loaded.id());assertEquals(m.lexicon().entries(),loaded.lexicon().entries());
        assertEquals(m.partnerships(),loaded.partnerships());assertTrue(loaded.lessons().entries().isEmpty());assertTrue(loaded.lexicon().formation().patterns().isEmpty());
    }
    @Test void playerPartialRuleAndOldSchemaHaveIndependentMigration(){
        var p=new WordFormation.Pattern(WordFormation.Order.HEAD_MATERIAL,"z",2);var g=Glossary.empty(UUID.randomUUID()).withFormation(new WordFormation(null,List.of(p)));
        var gson=new Gson();assertEquals(g,gson.fromJson(gson.toJson(g),Glossary.class));var old=gson.toJsonTree(g).getAsJsonObject();old.addProperty("schemaVersion",5);old.remove("formation");
        assertTrue(gson.fromJson(old,Glossary.class).formation().patterns().isEmpty());
    }
}
