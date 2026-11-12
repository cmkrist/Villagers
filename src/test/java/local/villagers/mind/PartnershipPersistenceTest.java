package local.villagers.mind;

import java.util.*;
import com.google.gson.*;
import local.villagers.relations.Partnerships;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PartnershipPersistenceTest {
    @TempDir java.nio.file.Path root;
    @Test void mutualCommitmentAndSharedHistorySurviveAnIndependentDiskReload(){
        var a=new AgentMind(new UUID(91,1));var b=new AgentMind(new UUID(91,2));
        a.partnerships(a.partnerships().form(b.id(),100).shared(b.id(),200));b.partnerships(b.partnerships().form(a.id(),100).shared(a.id(),200));
        var store=AgentStore.worldScoped(root);assertTrue(store.save(a));assertTrue(store.save(b));
        var first=AgentStore.worldScoped(root).load(a.id());var second=AgentStore.worldScoped(root).load(b.id());
        assertEquals(a.partnerships(),first.partnerships());assertEquals(b.partnerships(),second.partnerships());
        assertTrue(Partnerships.mutual(first.id(),first.partnerships(),second.id(),second.partnerships()));
        assertNull(first.lineage());assertNull(second.lineage());assertEquals(a.ageTicks(),first.ageTicks());
        a.partnerships(a.partnerships().end(b.id(),300,Partnerships.End.WITHDREW));assertTrue(store.save(a));
        assertFalse(Partnerships.mutual(a.id(),store.load(a.id()).partnerships(),b.id(),store.load(b.id()).partnerships()));
    }
    @Test void schemaThirteenKeepsExistingFamilyHistoryWithoutInventingAPartner(){
        var m=new AgentMind(new UUID(91,3));m.recordEpisode(Episode.Kind.HAD_CHILD,10,new UUID(91,4),null,"historic child",Set.of());
        var state=new Gson().toJsonTree(m.snapshot()).getAsJsonObject();state.addProperty("schemaVersion",13);state.remove("partnerships");
        var loaded=AgentMind.fromSnapshot(new Gson().fromJson(state,AgentMind.Snapshot.class));
        assertTrue(loaded.partnerships().history().isEmpty());assertEquals(m.episodes(),loaded.episodes());assertEquals(m.id(),loaded.id());assertEquals(m.name(),loaded.name());
    }
    @Test void selfCommitmentsAreRejectedAndChildrenDoNotInheritTheirParentsMarriage(){
        var a=new AgentMind(new UUID(91,1));var b=new AgentMind(new UUID(91,2));
        assertThrows(IllegalArgumentException.class,()->a.partnerships(Partnerships.empty().form(a.id(),100)));
        a.partnerships(a.partnerships().form(b.id(),100));b.partnerships(b.partnerships().form(a.id(),100));
        var child=AgentMind.born(new UUID(91,3),a,b,new Random(0));assertTrue(child.partnerships().history().isEmpty());assertNotNull(child.lineage());
    }
}
