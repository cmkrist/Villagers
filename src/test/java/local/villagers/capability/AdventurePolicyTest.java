package local.villagers.capability;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static local.villagers.capability.CapabilityPermissions.Capability.*;
import static local.villagers.capability.DefensePolicy.TargetKind.*;

class AdventurePolicyTest {
    @Test void terrainEditingCannotBeGrantedByTheAdventureProfile() {
        var permissions = CapabilityPermissions.adventure();
        assertFalse(permissions.allows(BREAK_BLOCK));
        assertFalse(permissions.allows(PLACE_BLOCK));
        assertTrue(permissions.allows(OPEN_DOOR));
        assertTrue(permissions.allows(EQUIP_ITEM));
        assertTrue(permissions.allows(HARVEST_REPLANT));
        assertTrue(permissions.allows(GATHER_HAY));
        assertTrue(permissions.allows(UNPACK_HAY));
        assertTrue(permissions.allows(CRAFT_BREAD));
        assertThrows(UnsupportedOperationException.class, () -> permissions.allowed().add(BREAK_BLOCK));
    }

    @Test void farmingExceptionRequiresMaturityHoeCorrectSeedAndIntactFarmland() {
        var wheat = CropPolicy.CropKind.WHEAT;
        var seed = CropPolicy.PlantMaterial.WHEAT_SEED;
        assertTrue(CropPolicy.mayHarvest(wheat, 7, 7, true, seed, true, true));
        assertFalse(CropPolicy.mayHarvest(wheat, 6, 7, true, seed, true, true));
        assertFalse(CropPolicy.mayHarvest(wheat, 7, 7, false, seed, true, true));
        assertFalse(CropPolicy.mayHarvest(wheat, 7, 7, true, CropPolicy.PlantMaterial.NONE, true, true));
        assertFalse(CropPolicy.mayHarvest(wheat, 7, 7, true, CropPolicy.PlantMaterial.POTATO, true, true));
        assertFalse(CropPolicy.mayHarvest(wheat, 7, 7, true, seed, false, true));
        assertFalse(CropPolicy.mayHarvest(wheat, 7, 7, true, seed, true, false));
        assertFalse(CropPolicy.mayHarvest(CropPolicy.CropKind.OTHER, 7, 7, true, seed, true, true));
        assertTrue(CropPolicy.mayHarvest(CropPolicy.CropKind.BEETROOT, 3, 3, true,
            CropPolicy.PlantMaterial.BEETROOT_SEED, true, true));
    }

    @Test void matureCropInspectionDoesNotAuthorizeAnUnpaidReplant() {
        var beet=CropPolicy.CropKind.BEETROOT;
        assertTrue(CropPolicy.mayInspectHarvest(beet,3,3,true,true,true));
        assertFalse(CropPolicy.mayHarvest(beet,3,3,true,CropPolicy.PlantMaterial.NONE,true,true));
        assertFalse(CropPolicy.mayInspectHarvest(beet,2,3,true,true,true));
        assertFalse(CropPolicy.mayInspectHarvest(beet,3,3,false,true,true));
        assertFalse(CropPolicy.mayInspectHarvest(CropPolicy.CropKind.OTHER,3,3,true,true,true));
    }
    @Test void realWeaponAndAnActualImminentHostileAreRequired() {
        assertTrue(DefensePolicy.mayStrike(true, MONSTER, true, false, true, true));
        assertTrue(DefensePolicy.mayStrike(true, MONSTER, false, true, true, true));
        assertFalse(DefensePolicy.mayStrike(false, MONSTER, true, true, true, true));
        assertFalse(DefensePolicy.mayStrike(true, MONSTER, false, false, true, true));
        assertFalse(DefensePolicy.mayStrike(true, MONSTER, true, true, false, true));
        assertFalse(DefensePolicy.mayStrike(true, MONSTER, true, true, true, false));
    }

    @Test void beingAttackedDoesNotGrantPermissionToHitPeopleOrNeutralEntities() {
        for (var kind : new DefensePolicy.TargetKind[]{PLAYER, GOLEM, NEUTRAL_ANIMAL, OTHER}) {
            assertFalse(DefensePolicy.mayStrike(true, kind, true, true, true, true), kind.toString());
        }
    }

    @Test void weaponRecoveryNeverRoundsDownOrAcceleratesSlowWeapons() {
        assertEquals(13, DefensePolicy.cooldownTicks(1.6));
        assertEquals(20, DefensePolicy.cooldownTicks(1));
        assertEquals(200, DefensePolicy.cooldownTicks(.1));
        assertEquals(Integer.MAX_VALUE, DefensePolicy.cooldownTicks(0));
        assertEquals(Integer.MAX_VALUE, DefensePolicy.cooldownTicks(Double.NaN));
    }
}
