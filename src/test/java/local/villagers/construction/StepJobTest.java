package local.villagers.construction;

import com.google.gson.Gson;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class StepJobTest {
    @TempDir Path root;
    static WorkSites.Point p(int x,int y,int z) { return new WorkSites.Point("minecraft:overworld",x,y,z); }
    static StepJob job() {
        return new StepJob(UUID.randomUUID(),new UUID(1,2),new UUID(3,4),
                List.of(new StepJob.Source(p(-10,65,-40),"minecraft:grass_block"),new StepJob.Source(p(-11,65,-40),"minecraft:dirt")),
                List.of(p(10,70,20),p(11,71,20)),0,0);
    }
    @Test void noPlacementBeforeGatheringAndProgressCannotReplaySkipOrChangeAuthority() {
        var j=job();var sites=WorkSites.empty().add(j);
        assertThrows(IllegalStateException.class,j::nextPlaced);
        var skipped=new StepJob(j.id(),j.owner(),j.worker(),j.sources(),j.blocks(),2,0);
        assertThrows(IllegalArgumentException.class,()->sites.progress(skipped));
        var one=sites.progress(j.nextGathered());assertThrows(IllegalArgumentException.class,()->one.progress(j));
        assertThrows(IllegalArgumentException.class,()->one.progress(j.nextGathered()));
        var paid=one.progress(j.nextGathered().nextGathered());var placed=paid.progress(skipped.nextPlaced());
        assertEquals(1,placed.stepJobs().getFirst().placed());
        assertTrue(placed.progress(skipped.nextPlaced().nextPlaced()).stepJobs().isEmpty());
        var changed=new StepJob(j.id(),j.owner(),new UUID(9,9),j.sources(),j.blocks(),1,0);
        assertThrows(IllegalArgumentException.class,()->sites.progress(changed));
    }
    @Test void protectionAndAnyOutsideFootprintTouchRetireOrBlockTheWholePendingJob() {
        var j=job();var sites=WorkSites.empty().add(j);assertTrue(sites.allowsSteps(j.id()));
        for(var p:j.footprint()) {
            var zone=new WorkSites.Protection(UUID.randomUUID(),j.owner(),p,p);
            assertFalse(sites.add(zone).allowsSteps(j.id()));assertTrue(sites.touch(p).stepJobs().isEmpty());
        }
        var paid=j.nextGathered().nextGathered().nextPlaced();var current=WorkSites.empty().add(paid);
        assertTrue(current.touch(j.blocks().getFirst()).stepJobs().isEmpty(),"Already placed structure remains watched until complete");
        assertEquals(current,current.touch(j.sources().getFirst().point()),"Consumed source is never granted again");
    }
    @Test void otherTransitionsPreserveStepWorkButRevocationAndCrossJobOverlapStayStrict() {
        var j=job();var sites=WorkSites.empty().add(j);var zone=new WorkSites.Protection(UUID.randomUUID(),j.owner(),p(40,70,40),p(40,70,40));
        assertEquals(List.of(j),sites.add(zone).remove(zone.id()).stepJobs());
        var farm=new WorkSites.Farm(UUID.randomUUID(),j.owner(),List.of(p(40,70,0)));
        assertEquals(List.of(j),sites.add(farm).touch(farm.cells().getFirst()).stepJobs());
        var station=new WorkSites.Station(UUID.randomUUID(),j.owner(),p(60,70,0));
        assertEquals(List.of(j),sites.add(station).remove(station.id()).stepJobs());
        assertThrows(IllegalArgumentException.class,()->sites.add(new WorkSites.Farm(UUID.randomUUID(),j.owner(),List.of(j.blocks().getFirst()))));
        assertThrows(IllegalArgumentException.class,()->sites.add(j));
        assertTrue(sites.revokeEdits().stepJobs().isEmpty());
    }
    @Test void cleanRestartRetainsExactProgressButDirtyRestartKeepsOnlyProtection() throws Exception {
        var j=job().nextGathered();var file=root.resolve("work-sites.json");
        var zone=new WorkSites.Protection(UUID.randomUUID(),j.owner(),p(40,70,40),p(40,70,40));
        try(var store=new WorkSiteStore(file)) { assertTrue(store.update(WorkSites.empty().add(j).add(zone))); }
        try(var store=new WorkSiteStore(file)) { assertEquals(List.of(j),store.snapshot().stepJobs()); }
        Files.writeString(file.resolveSibling("work-sites.json.active"),"unclean\n");
        try(var store=new WorkSiteStore(file)) {
            assertTrue(store.healthy());assertTrue(store.snapshot().stepJobs().isEmpty());assertEquals(List.of(zone),store.snapshot().protections());
        }
    }
    @Test void legacyMigrationAddsNoStepAuthorityAndFutureOrIncompleteMetadataFailsClosed() {
        var gson=new Gson();var old=gson.toJsonTree(WorkSites.empty()).getAsJsonObject();old.addProperty("schemaVersion",6);old.remove("stepJobs");
        var migrated=gson.fromJson(old,WorkSites.class);assertEquals(7,migrated.schemaVersion());assertTrue(migrated.stepJobs().isEmpty());
        assertThrows(IllegalArgumentException.class,()->new WorkSites(6,List.of(),List.of(),List.of(),List.of(),List.of(),List.of(job())));
        assertThrows(IllegalArgumentException.class,()->new WorkSites(7,List.of(),List.of(),List.of(),List.of(),List.of(),null));
        assertThrows(IllegalArgumentException.class,()->new WorkSites(8,List.of(),List.of(),List.of(),List.of(),List.of(),List.of()));
        assertThrows(IllegalArgumentException.class,()->new StepJob.Source(p(0,70,0),"minecraft:chest"));
    }
    @Test void separateAssignedCorridorsDoNotWidenSoilJobsOrOrdinaryHome() {
        var home=p(0,70,0);var j=job();assertTrue(StepTripPolicy.joins(home,j));
        assertTrue(StepTripPolicy.contains(home,j,-10,66,-40));assertFalse(StepTripPolicy.contains(home,j,40,70,-40));
        assertFalse(StepTripPolicy.contains(home,j,0,70,-77));assertFalse(StepTripPolicy.contains(home,j,Double.NaN,0,0));
        assertFalse(StepTripPolicy.joins(new WorkSites.Point("minecraft:the_nether",0,70,0),j));
        assertFalse(local.villagers.survival.SurvivalPolicy.withinHomeRange(41));
        var far=new StepJob(j.id(),j.owner(),j.worker(),List.of(new StepJob.Source(p(0,70,-69),"minecraft:dirt")),List.of(p(0,70,-20)),0,0);
        assertFalse(StepTripPolicy.joins(home,far));
    }
}
