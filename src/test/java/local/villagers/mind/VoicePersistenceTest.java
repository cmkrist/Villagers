package local.villagers.mind;
import java.util.*;
import com.google.gson.*;
import local.villagers.language.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class VoicePersistenceTest {
    @TempDir java.nio.file.Path root;
    @Test void soundExemplarsAndIncompleteMotorPracticeSurviveDiskWithoutChangingVocabulary(){
        var m=new AgentMind(UUID.randomUUID());m.lexicon(BiomeDialect.foundingLanguage("minecraft:plains"));
        m.voice(Voice.empty().hear(Voice.Kind.PILLAGER,List.of("zaga","lumi"),100).imitate(Voice.Kind.VILLAGER,Voice.Kind.PILLAGER,110).practiced());
        var store=AgentStore.worldScoped(root);assertTrue(store.save(m));var loaded=AgentStore.worldScoped(root).load(m.id());assertEquals(m.voice(),loaded.voice());assertEquals(m.lexicon(),loaded.lexicon());
        assertEquals(List.of("saka","lumi"),loaded.voice().imitate(Voice.Kind.VILLAGER,Voice.Kind.PILLAGER,120).sounds());
    }
    @Test void schemaSeventeenDoesNotInventHeardVoicesOrLoseLessonHistory(){var m=new AgentMind(UUID.randomUUID());m.lessons(m.lessons().demonstrate(UUID.randomUUID(),Concept.HOE,1));var gson=new Gson();var j=gson.toJsonTree(m.snapshot()).getAsJsonObject();j.addProperty("schemaVersion",17);j.remove("voice");var old=AgentMind.fromSnapshot(gson.fromJson(j,AgentMind.Snapshot.class));assertTrue(old.voice().models().isEmpty());assertEquals(m.lessons(),old.lessons());}
}
