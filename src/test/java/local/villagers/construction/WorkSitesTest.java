package local.villagers.construction;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkSitesTest {
    static final UUID OWNER = new UUID(3,1);
    static final WorkSites.Point SOIL = new WorkSites.Point("minecraft:overworld", 10, 70, 20);
    static WorkSites.Farm farm(WorkSites.Point... cells) { return new WorkSites.Farm(UUID.randomUUID(), OWNER, List.of(cells)); }
    static WorkSites.Protection protection() { return new WorkSites.Protection(UUID.randomUUID(), OWNER, SOIL.above(1), SOIL.above(2)); }

    @Test void onlyExplicitGroundIsAllowedAndProtectedHeadroomWins() {
        assertFalse(WorkSites.empty().allows(SOIL));
        var farm = farm(SOIL); var sites = WorkSites.empty().add(farm);
        assertTrue(sites.allows(SOIL));
        assertFalse(sites.allows(SOIL.above(1)));
        assertFalse(sites.allows(new WorkSites.Point("minecraft:the_nether", 10, 70, 20)));
        var protection = protection(); var protectedSites = sites.add(protection);
        assertFalse(protectedSites.allows(SOIL));
        assertTrue(protectedSites.remove(protection.id()).allows(SOIL));
        assertFalse(protectedSites.remove(farm.id()).allows(SOIL));
    }

    @Test void anOutsideEditOrCompletedPlotRetiresOnlyThatGroundPermission() {
        var other = new WorkSites.Point(SOIL.dimension(), 11, 70, 20);
        var sites = WorkSites.empty().add(farm(SOIL, other));
        var changed = sites.touch(SOIL.above(2));
        assertFalse(changed.allows(SOIL)); assertTrue(changed.allows(other));
        assertTrue(sites.allows(SOIL), "snapshots are immutable");
        assertFalse(changed.touch(other).allows(other));
        assertEquals(sites, sites.touch(new WorkSites.Point("minecraft:the_nether", 10, 70, 20)));
    }

    @Test void boundsDuplicateIdentitiesAndOverlappingFootprintsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> farm(SOIL, new WorkSites.Point(SOIL.dimension(), 19,70,20)));
        var a = farm(SOIL); var b = farm(SOIL.above(1));
        assertThrows(IllegalArgumentException.class, () -> WorkSites.empty().add(a).add(b));
        assertThrows(IllegalArgumentException.class, () -> WorkSites.empty().add(a).add(a));
        assertThrows(IllegalArgumentException.class, () -> new WorkSites.Protection(UUID.randomUUID(), OWNER, SOIL,
                new WorkSites.Point(SOIL.dimension(), 100, 150, 100)));
        assertThrows(IllegalArgumentException.class, () -> new WorkSites(WorkSites.SCHEMA + 1, List.of(), List.of()));
    }

    @Test void stationOnlyGrantsOneCenterAndProtectionCoversItsWatchedAccess() {
        var s = new WorkSites.Station(UUID.randomUUID(), OWNER, SOIL.above(1));
        var sites = WorkSites.empty().add(s);
        assertTrue(sites.allowsStation(s.target())); assertFalse(sites.allows(s.target()));
        assertFalse(sites.allowsStation(s.target().above(1)));
        var access = new WorkSites.Point(SOIL.dimension(), 11, 71, 20);
        var zone = new WorkSites.Protection(UUID.randomUUID(), OWNER, access, access);
        assertFalse(sites.add(zone).allowsStation(s.target()));
        assertTrue(sites.touch(access).stations().isEmpty());
        assertTrue(sites.remove(s.id()).stations().isEmpty());
        assertTrue(sites.allowsStation(s.target()), "immutable prior snapshot");
    }

    @Test void supportAndAccessEditsOrAnUncleanRestartRetireStationGrants() {
        var s = new WorkSites.Station(UUID.randomUUID(), OWNER, SOIL.above(1));
        var sites = WorkSites.empty().add(s).add(protection());
        for (var p : s.footprint()) assertTrue(sites.touch(p).stations().isEmpty());
        var revoked = sites.revokeEdits(); assertTrue(revoked.stations().isEmpty());
        assertEquals(1, revoked.protections().size());
        assertThrows(IllegalArgumentException.class, () -> WorkSites.empty().add(s).add(farm(SOIL)));
        assertThrows(IllegalArgumentException.class, () -> WorkSites.empty().add(s).add(s));
        assertThrows(IllegalArgumentException.class, () -> new WorkSites.Station(UUID.randomUUID(), OWNER,
                new WorkSites.Point(SOIL.dimension(), 30_000_000, 70, 20)));
    }
}
