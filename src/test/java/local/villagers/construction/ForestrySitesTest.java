package local.villagers.construction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ForestrySitesTest {
    @TempDir Path temp;
    static final WorkSites.Point SEED=new WorkSites.Point("minecraft:overworld",10,74,20);
    static final WorkSites.Point LOG=new WorkSites.Point("minecraft:overworld",11,76,20);
    static final WorkSites.Point PLANT=new WorkSites.Point("minecraft:overworld",13,72,20);
    static WorkSites.Forestry job() { return new WorkSites.Forestry(UUID.randomUUID(),UUID.randomUUID(),SEED,
            List.of(LOG,LOG.above(1)),PLANT,"minecraft:mud",false,List.of(LOG,LOG.above(1))); }
    @Test void exactFiniteJobProtectionAndExternalEditsWin() {
        var job=job();var sites=WorkSites.empty().add(job);
        assertTrue(sites.allowsForestry(job.id()));assertFalse(WorkSites.empty().allowsForestry(job.id()));
        var zone=new WorkSites.Protection(UUID.randomUUID(),UUID.randomUUID(),LOG,LOG);
        assertFalse(sites.add(zone).allowsForestry(job.id()));
        for(var p:job.footprint()) assertTrue(sites.touch(p).forestry().isEmpty());
        assertFalse(sites.allows(LOG));assertFalse(sites.allowsStation(PLANT));
    }
    @Test void progressShrinksAuthorityAndCannotRestoreAlreadyCutLogsOrSeedAuthority() {
        var job=job();var sites=WorkSites.empty().add(job);var seeded=job.gatheredSeed();
        assertThrows(IllegalArgumentException.class,()->job.cut(LOG));
        sites=sites.progress(seeded);assertFalse(sites.forestry().getFirst().footprint().contains(SEED));
        var cut=seeded.cut(LOG);sites=sites.progress(cut);assertEquals(List.of(LOG.above(1)),sites.forestry().getFirst().remaining());
        var current=sites;
        assertThrows(IllegalArgumentException.class,()->current.progress(seeded));
        assertThrows(IllegalArgumentException.class,()->current.progress(job));
        assertTrue(sites.touch(PLANT.above(-1)).forestry().isEmpty());
        assertTrue(sites.revokeEdits().forestry().isEmpty());
    }
    @Test void invalidQuotaRolesDimensionsAndProgressAreRejected() {
        var job=job();
        assertThrows(IllegalArgumentException.class,()->new WorkSites.Forestry(job.id(),job.owner(),SEED,List.of(SEED),PLANT,"minecraft:mud",false,List.of(SEED)));
        assertThrows(IllegalArgumentException.class,()->new WorkSites.Forestry(job.id(),job.owner(),SEED,job.logs(),PLANT,"minecraft:mud",false,List.of(LOG)));
        assertThrows(IllegalArgumentException.class,()->new WorkSites.Forestry(job.id(),job.owner(),SEED,List.of(new WorkSites.Point("minecraft:the_nether",11,76,20)),PLANT,"minecraft:mud",true,List.of()));
        assertThrows(IllegalArgumentException.class,()->WorkSites.empty().add(job).add(job));
    }
    @Test void schemaTwoMigratesWithoutInventingResourceAuthority() throws Exception {
        Path p=temp.resolve("sites.json");Files.writeString(p,"{\"schemaVersion\":2,\"farms\":[],\"protections\":[],\"stations\":[]}");
        try(var store=new WorkSiteStore(p)) { assertTrue(store.healthy());assertEquals(WorkSites.SCHEMA,store.snapshot().schemaVersion());assertTrue(store.snapshot().forestry().isEmpty()); }
    }
    @Test void partialCleanRestartRetainsQuotaButDirtyRestartRevokesAndPreservesProtection() {
        Path p=temp.resolve("sites.json");var job=job().gatheredSeed().cut(LOG);
        var zone=new WorkSites.Protection(UUID.randomUUID(),UUID.randomUUID(),PLANT,PLANT);
        try(var store=new WorkSiteStore(p)) { assertTrue(store.update(WorkSites.empty().add(job).add(zone))); }
        var abandoned=new WorkSiteStore(p);assertEquals(job,abandoned.snapshot().forestry().getFirst());
        try(var recovered=new WorkSiteStore(p)) { assertTrue(recovered.healthy());assertTrue(recovered.snapshot().forestry().isEmpty());assertEquals(1,recovered.snapshot().protections().size()); }
    }
    @Test void explicitLogReassignmentRequiresFreshIdentityRetainedSeedAndNonincreasingQuota() {
        var old=job().gatheredSeed();var log=new WorkSites.Point(SEED.dimension(),12,74,20);
        var next=new WorkSites.Forestry(UUID.randomUUID(),old.owner(),old.seed(),List.of(log),old.plant(),old.soil(),true,List.of(log));
        var moved=WorkSites.empty().add(old).moveLogs(old.id(),next);
        assertEquals(List.of(next),moved.forestry());assertFalse(moved.allowsForestry(old.id()));
        var unseeded=new WorkSites.Forestry(old.id(),old.owner(),old.seed(),old.logs(),old.plant(),old.soil(),false,old.logs());
        assertThrows(IllegalArgumentException.class,()->WorkSites.empty().add(unseeded).moveLogs(old.id(),next));
        var cut=old.cut(LOG).cut(LOG.above(1));
        assertThrows(IllegalArgumentException.class,()->WorkSites.empty().add(cut).moveLogs(cut.id(),next));
    }
    @Test void rootNeedsFreshSingleCellAuthorityAndCannotBeInheritedOrReplayed() {
        var old=job().gatheredSeed();var root=new WorkSites.Point(SEED.dimension(),12,74,21);
        assertTrue(old.roots().isEmpty());assertThrows(IllegalArgumentException.class,()->old.clearedRoot(root));
        var sites=WorkSites.empty().add(old).root(old.id(),root);var granted=sites.forestry().getFirst();
        assertNotEquals(old.id(),granted.id());assertEquals(old.remaining(),granted.remaining());
        assertThrows(IllegalArgumentException.class,()->sites.root(granted.id(),root.above(1)));
        assertThrows(IllegalArgumentException.class,()->granted.cut(LOG));
        assertTrue(sites.touch(root).forestry().isEmpty());
        var zone=new WorkSites.Protection(UUID.randomUUID(),UUID.randomUUID(),root,root);
        assertFalse(sites.add(zone).allowsForestry(granted.id()));
        var cleared=granted.clearedRoot(root);var progressed=sites.progress(cleared);
        assertEquals(old.remaining(),cleared.remaining());assertFalse(cleared.footprint().contains(root));
        assertThrows(IllegalArgumentException.class,()->progressed.progress(granted));
        assertThrows(IllegalArgumentException.class,()->cleared.clearedRoot(root));
        assertEquals(1,cleared.cut(LOG).remaining().size());
    }
    @Test void schemaFourMigratesWithoutRootAuthorityAndExplicitRootPersistsCleanly() throws Exception {
        var old=job().gatheredSeed();var json=new com.google.gson.Gson().toJson(WorkSites.empty().add(old));
        json=json.replace("\"schemaVersion\":5","\"schemaVersion\":4").replace(",\"roots\":[]","").replace(",\"unrooted\":[]","");
        Path path=temp.resolve("old.json");Files.writeString(path,json);
        var root=new WorkSites.Point(SEED.dimension(),12,74,21);
        try(var store=new WorkSiteStore(path)) {
            assertTrue(store.healthy());assertEquals(WorkSites.SCHEMA,store.snapshot().schemaVersion());
            assertTrue(store.snapshot().forestry().getFirst().roots().isEmpty());
            assertTrue(store.update(store.snapshot().root(old.id(),root)));
        }
        try(var store=new WorkSiteStore(path)) { assertTrue(store.healthy());assertEquals(List.of(root),store.snapshot().forestry().getFirst().unrooted()); }
    }
    @Test void clearanceIsExplicitFiniteAndMustFinishBeforeLogs() {
        var old=job().gatheredSeed();var leaf=new WorkSites.Point(SEED.dimension(),12,74,21);
        var sites=WorkSites.empty().add(old).clearance(old.id(),List.of(leaf));var next=sites.forestry().getFirst();
        assertNotEquals(old.id(),next.id());assertEquals(old.remaining(),next.remaining());
        assertThrows(IllegalArgumentException.class,()->next.cut(LOG));
        var done=next.cleared(leaf);assertEquals(1,done.cut(LOG).remaining().size());
        assertTrue(sites.touch(leaf).forestry().isEmpty());
        assertThrows(IllegalArgumentException.class,()->sites.progress(done.cleared(leaf)));
    }
}
