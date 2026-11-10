package local.villagers.mind;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import local.villagers.display.MemoryText;
import local.villagers.language.Concept;
import local.villagers.language.Telling;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FarmMemoryTest {
    @TempDir Path root;
    private static Place p(int x) { return new Place("minecraft:overworld",x,72,0); }
    private static AgentMind mind() { return new AgentMind(new UUID(4,5)); }

    @Test void observationsSurviveStoreReloadWithoutChangingFoodFamilyOrLearning() {
        var m=mind();m.bred(400);var before=m.snapshot();UUID player=new UUID(8,9);
        assertTrue(m.observeFarm(p(10),"minecraft:wheat",500,List.of(player)));
        var store=AgentStore.worldScoped(root);assertTrue(store.save(m));var loaded=store.load(m.id());
        assertEquals(m.observedFarms(),loaded.observedFarms());assertEquals(before.food(),loaded.snapshot().food());
        assertEquals(before.nextBreedingAt(),loaded.nextBreedingAt());assertEquals(before.learningUpdates(),loaded.learningUpdates());
        assertEquals(List.of(Concept.FOOD),Telling.concepts(loaded.observedFarms().getFirst()));
        assertTrue(MemoryText.line(loaded.observedFarms().getFirst(),player,false,id -> "Player").contains("10 72 0"));
        assertEquals("???",MemoryText.line(loaded.observedFarms().getFirst(),new UUID(1,2),false,id -> "Player"));
    }
    @Test void aPatchIsDeduplicatedButOtherFloorsAndDimensionsAreDistinct() {
        var m=mind();assertTrue(m.observeFarm(p(0),"minecraft:wheat",1,List.of()));
        assertFalse(m.observeFarm(p(4),"minecraft:beetroots",2,List.of()));
        assertTrue(m.observeFarm(new Place("minecraft:overworld",0,75,0),"minecraft:carrots",3,List.of()));
        assertTrue(m.observeFarm(new Place("minecraft:the_nether",0,72,0),"minecraft:potatoes",4,List.of()));
        assertEquals(3,m.observedFarms().size());
    }
    @Test void farmMemoryHasItsOwnBoundWithoutDiscardingAnUnrelatedLifeEvent() {
        var m=mind();var birth=m.recordEpisode(Episode.Kind.HAD_CHILD,1,new UUID(2,3),null,"Child",List.of());
        for(int i=0;i<40;i++)m.observeFarm(p(i*10),"minecraft:wheat",i+2,List.of());
        assertEquals(AgentMind.MAX_FARMS,m.observedFarms().size());assertTrue(m.episodes().contains(birth));
        assertFalse(m.observedFarms().stream().anyMatch(e -> e.place().equals(p(0))));
        var before=m.episodes();assertThrows(IllegalArgumentException.class,()->m.observeFarm(p(1000),"minecraft:chest",100,List.of()));
        assertEquals(before,m.episodes());assertTrue(m.forgetFarm(p(390)));assertFalse(m.forgetFarm(p(390)));
    }
    @Test void legacyMindsGainNoUnseenFarmsAndCannotSmuggleTheNewKind() {
        Gson gson=new Gson();var m=mind();var old=JsonParser.parseString(gson.toJson(m.snapshot())).getAsJsonObject();
        old.addProperty("schemaVersion",7);var loaded=AgentMind.fromSnapshot(gson.fromJson(old,AgentMind.Snapshot.class));
        assertTrue(loaded.observedFarms().isEmpty());assertEquals(AgentMind.SCHEMA_VERSION,loaded.snapshot().schemaVersion());
        m.observeFarm(p(10),"minecraft:wheat",100,List.of());var spoof=JsonParser.parseString(gson.toJson(m.snapshot())).getAsJsonObject();
        spoof.addProperty("schemaVersion",7);assertThrows(IllegalArgumentException.class,()->AgentMind.fromSnapshot(gson.fromJson(spoof,AgentMind.Snapshot.class)));
        assertThrows(IllegalArgumentException.class,()->new Episode(0,Episode.Kind.OBSERVED_FARM,0,null,null,"minecraft:wheat",List.of()));
        assertThrows(IllegalArgumentException.class,()->m.observeFarm(p(Integer.MAX_VALUE),"minecraft:wheat",100,List.of()));
    }
}
