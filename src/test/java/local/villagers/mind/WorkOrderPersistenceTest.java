package local.villagers.mind;

import java.util.*;
import com.google.gson.*;
import local.villagers.work.WorkOrders;
import static local.villagers.work.WorkOrders.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WorkOrderPersistenceTest {
    @TempDir java.nio.file.Path root;
    @Test void pendingProgressAndPersonalSettlementsSurviveDiskWithoutCopyingPeerEvidence(){
        var a=new AgentMind(new UUID(211,1));var b=new AgentMind(new UUID(211,2));var t=new Terms(a.id(),b.id(),Task.BREAD,1,1,100,2500);
        a.workOrders(a.workOrders().accept(t));b.workOrders(b.workOrders().accept(t).produced(t,new Production(200,new Place("minecraft:overworld",1,81,2),1),200));
        var store=AgentStore.worldScoped(root);assertTrue(store.save(a));assertTrue(store.save(b));
        assertEquals(a.workOrders(),store.load(a.id()).workOrders());assertEquals(b.workOrders(),store.load(b.id()).workOrders());
        assertTrue(mutual(store.load(a.id()).workOrders().open(),store.load(b.id()).workOrders().open(),201));
        a.workOrders(a.workOrders().complete(t,a.id(),new Settlement(300,0,1,1,0),300));assertTrue(store.save(a));assertNull(store.load(a.id()).workOrders().open());
        assertThrows(IllegalArgumentException.class,()->a.workOrders(b.workOrders()));
    }
    @Test void schemaFourteenMigratesWithoutGrantingWorkOrLosingPartnershipHistory(){
        var m=new AgentMind(new UUID(211,1));m.partnerships(m.partnerships().form(new UUID(211,2),100));
        var json=new Gson().toJsonTree(m.snapshot()).getAsJsonObject();json.addProperty("schemaVersion",14);json.remove("workOrders");
        var old=AgentMind.fromSnapshot(new Gson().fromJson(json,AgentMind.Snapshot.class));assertTrue(old.workOrders().history().isEmpty());assertEquals(m.partnerships(),old.partnerships());assertEquals(m.id(),old.id());
    }
    @Test void unrelatedParticipantsAndChildrenCannotInheritAnotherPersonsJob(){
        var a=new AgentMind(new UUID(211,1));var b=new AgentMind(new UUID(211,2));var t=new Terms(a.id(),b.id(),Task.BREAD,1,1,100,2500);
        a.workOrders(a.workOrders().accept(t));assertThrows(IllegalArgumentException.class,()->new AgentMind(new UUID(211,3)).workOrders(a.workOrders()));
        assertTrue(AgentMind.born(new UUID(211,4),a,b,new Random(1)).workOrders().history().isEmpty());
    }
}
