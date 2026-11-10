package local.villagers.survival;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkInteractionRangeTest {
    @Test void stationBeyondHomeCenterCanBeUsedFromAPhysicallyInBoundsStand() {
        Vec3 home = new Vec3(-226.5, 72.5, 773.5), table = new Vec3(-225.5, 71.5, 744.5);
        Vec3 stand = new Vec3(-225.5, 72, 746.5), eye = stand.add(0, 1.62, 0);
        assertFalse(SurvivalPolicy.withinHomeRange(home.distanceTo(table)));
        assertTrue(SurvivalPolicy.withinHomeInteractionRange(home.distanceTo(table), 3));
        assertTrue(SurvivalPolicy.withinHomeRange(home.distanceTo(stand)));
        assertTrue(eye.distanceToSqr(table) <= 9);
        assertFalse(SurvivalPolicy.withinHomeRange(28.01), "actor roaming boundary is unchanged");
    }
    @Test void stationBroadPhaseCannotGrantUnlimitedReachOrPermitNonfiniteGeometry() {
        assertFalse(SurvivalPolicy.withinHomeInteractionRange(31.01, 3));
        assertFalse(SurvivalPolicy.withinHomeInteractionRange(28, 4));
        assertFalse(SurvivalPolicy.withinHomeInteractionRange(Double.NaN, 3));
        assertFalse(SurvivalPolicy.withinHomeInteractionRange(28, -1));
    }
}
