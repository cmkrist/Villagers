package local.villagers.construction;

import com.google.gson.Gson;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SoilSitesTest {
    static WorkSites.Point p(int x,int y,int z) { return new WorkSites.Point("minecraft:overworld",x,y,z); }
    static WorkSites.Soil job() { return new WorkSites.Soil(UUID.randomUUID(),new UUID(9,1),p(-20,65,-60),p(0,71,-20),"minecraft:grass_block",false); }
    @Test void sourceAndDestinationProtectionBlockTheWholeTransfer() {
        var j=job();var sites=WorkSites.empty().add(j);assertTrue(sites.allowsSoil(j.id()));
        for(var p:j.footprint()) {
            var zone=new WorkSites.Protection(UUID.randomUUID(),j.owner(),p,p);assertFalse(sites.add(zone).allowsSoil(j.id()));
            assertTrue(sites.touch(p).soilJobs().isEmpty());
        }
        assertTrue(sites.revokeEdits().soilJobs().isEmpty());
    }
    @Test void sourceIncomeProgressCannotGrowAuthorityAndCompletedDeliveryBecomesExactlyOneFarmCell() {
        var j=job();var sites=WorkSites.empty().add(j);assertThrows(IllegalArgumentException.class,()->sites.soilReadyForPlanting(j.id()));
        var paid=sites.progress(j.markGathered());assertThrows(IllegalArgumentException.class,()->paid.progress(j));
        var changed=new WorkSites.Soil(j.id(),j.owner(),p(-19,65,-60),j.target(),j.sourceKind(),true);
        assertThrows(IllegalArgumentException.class,()->paid.progress(changed));
        var farm=paid.soilReadyForPlanting(j.id());assertTrue(farm.soilJobs().isEmpty());assertEquals(1,farm.farms().size());
        assertEquals(List.of(j.target()),farm.farms().getFirst().cells());assertNotEquals(j.id(),farm.farms().getFirst().id());
        assertFalse(farm.allows(j.source()));assertTrue(farm.allows(j.target()));
        assertEquals(paid,paid.touch(j.source()),"Consumed source grants are not recreated or watched as pending work");
    }
    @Test void olderSchemaMigrationAddsNoSoilPermissionAndCannotSmuggleFutureAuthority() {
        var gson=new Gson();var old=new com.google.gson.JsonObject();old.addProperty("schemaVersion",5);
        for(String k:List.of("farms","protections","stations","forestry"))old.add(k,new com.google.gson.JsonArray());
        var migrated=gson.fromJson(old,WorkSites.class);assertEquals(WorkSites.SCHEMA,migrated.schemaVersion());assertTrue(migrated.soilJobs().isEmpty());
        assertThrows(IllegalArgumentException.class,()->new WorkSites(5,List.of(),List.of(),List.of(),List.of(),List.of(job())));
        assertThrows(IllegalArgumentException.class,()->new WorkSites(6,List.of(),List.of(),List.of(),List.of(),null));
    }
    @Test void unrelatedTransitionsPreserveSoilAndOutsideOverlapCannotCreateDoubleSpending() {
        var j=job();var sites=WorkSites.empty().add(j);var other=new WorkSites.Protection(UUID.randomUUID(),j.owner(),p(40,70,40),p(40,70,40));
        assertEquals(List.of(j),sites.add(other).soilJobs());assertEquals(List.of(j),sites.add(other).remove(other.id()).soilJobs());
        assertThrows(IllegalArgumentException.class,()->sites.add(new WorkSites.Farm(UUID.randomUUID(),j.owner(),List.of(j.target()))));
        assertThrows(IllegalArgumentException.class,()->sites.add(j));
    }
    @Test void eachPendingTransferReservesOneFiniteFarmSlot() {
        var sites=WorkSites.empty();
        for(int i=0;i<8;i++)sites=sites.add(new WorkSites.Farm(UUID.randomUUID(),new UUID(9,1),List.of(p(i*10,70,100))));
        var full=sites;assertThrows(IllegalArgumentException.class,()->full.add(job()));
    }
    @Test void assignedCorridorDoesNotExpandOrdinaryHomeOrAdmitAnotherDimension() {
        var home=p(0,72,0);var j=job();assertTrue(SoilTripPolicy.joins(home,j));
        assertTrue(SoilTripPolicy.contains(home,j,-20,66,-60));assertFalse(local.villagers.survival.SurvivalPolicy.withinHomeRange(63));
        assertFalse(SoilTripPolicy.contains(home,j,30,72,-60));assertFalse(SoilTripPolicy.contains(home,j,0,72,-90));
        assertFalse(SoilTripPolicy.joins(new WorkSites.Point("minecraft:the_nether",0,72,0),j));
        assertFalse(SoilTripPolicy.contains(home,null,0,72,0));assertFalse(SoilTripPolicy.contains(home,j,Double.NaN,72,0));
    }
}
