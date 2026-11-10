package local.villagers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlantingMaterialsTest {
    @BeforeAll static void items() {
        BreadCraftingTest.initializeItemRegistries();
        for (var item : java.util.List.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.CARROT, Items.POTATO))
            BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow().bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }
    @Test void oneRealSeedIsConsumedOnceAndAReplantingSeedRemainsAcrossSplitSlots() {
        var inv = new SimpleContainer(new ItemStack(Items.WHEAT_SEEDS), new ItemStack(Items.WHEAT_SEEDS));
        var plan = PlantingMaterials.reserve(inv); assertNotNull(plan);
        assertTrue(PlantingMaterials.commit(inv, plan)); assertEquals(1, inv.countItem(Items.WHEAT_SEEDS));
        assertFalse(PlantingMaterials.commit(inv, plan)); assertNull(PlantingMaterials.reserve(inv));
    }
    @Test void changedMaterialPlansCannotRemoveUnrelatedItemsOrSpendTheLastSeed() {
        var inv = new SimpleContainer(new ItemStack(Items.CARROT, 2)); var plan = PlantingMaterials.reserve(inv);
        inv.setItem(0, new ItemStack(Items.EMERALD, 2)); assertFalse(PlantingMaterials.commit(inv, plan));
        assertEquals(2, inv.countItem(Items.EMERALD)); assertNull(PlantingMaterials.reserve(inv));
        inv.setItem(0, new ItemStack(Items.CARROT)); assertNull(PlantingMaterials.reserve(inv));
    }
    @Test void narrowCultivationDoesNotGrantGeneralDemolitionOrPlacement() {
        var p = local.villagers.capability.CapabilityPermissions.adventure();
        assertTrue(p.allows(local.villagers.capability.CapabilityPermissions.Capability.CULTIVATE_PLANT));
        assertFalse(p.allows(local.villagers.capability.CapabilityPermissions.Capability.BREAK_BLOCK));
        assertFalse(p.allows(local.villagers.capability.CapabilityPermissions.Capability.PLACE_BLOCK));
    }

    @Test void aMixedPlantingChoicePaysOneRealAlternativeSeedAndRetainsBothSeedBanks() {
        var inv=new SimpleContainer(new ItemStack(Items.WHEAT_SEEDS,3),new ItemStack(Items.BEETROOT_SEEDS,2));
        var seen=java.util.List.of(new local.villagers.capability.CropLayout.Neighbour(1,1,local.villagers.capability.CropPolicy.CropKind.WHEAT));
        var plan=PlantingMaterials.reserve(inv,item -> local.villagers.capability.CropLayout.preference(
                item==Items.WHEAT_SEEDS?local.villagers.capability.CropPolicy.CropKind.WHEAT:local.villagers.capability.CropPolicy.CropKind.BEETROOT,seen));
        assertTrue(plan.before().is(Items.BEETROOT_SEEDS));
        assertEquals(2,inv.countItem(Items.BEETROOT_SEEDS),"Planning does not consume anything");
        assertTrue(PlantingMaterials.commit(inv,plan));
        assertEquals(3,inv.countItem(Items.WHEAT_SEEDS));assertEquals(1,inv.countItem(Items.BEETROOT_SEEDS));
        assertFalse(PlantingMaterials.commit(inv,plan));
    }

    @Test void layoutPreferenceNeverInventsAnAlternativeOrSpendsItsLastPlantingItem() {
        var inv=new SimpleContainer(new ItemStack(Items.WHEAT_SEEDS,2),new ItemStack(Items.BEETROOT_SEEDS));
        var plan=PlantingMaterials.reserve(inv,item -> item==Items.BEETROOT_SEEDS?100:-1);
        assertTrue(plan.before().is(Items.WHEAT_SEEDS));assertTrue(PlantingMaterials.commit(inv,plan));
        assertEquals(1,inv.countItem(Items.WHEAT_SEEDS));assertEquals(1,inv.countItem(Items.BEETROOT_SEEDS));
        assertNull(PlantingMaterials.reserve(inv,item -> 100));
    }

    @Test void cropRowsKeepStableSeedOrderButCrossCrowdingFavorsAPossessedAlternative() {
        var row=java.util.List.of(new local.villagers.capability.CropLayout.Neighbour(-1,0,local.villagers.capability.CropPolicy.CropKind.WHEAT),
                new local.villagers.capability.CropLayout.Neighbour(1,0,local.villagers.capability.CropPolicy.CropKind.WHEAT));
        assertEquals(0,local.villagers.capability.CropLayout.preference(local.villagers.capability.CropPolicy.CropKind.WHEAT,row));
        var cross=new java.util.ArrayList<>(row);cross.add(new local.villagers.capability.CropLayout.Neighbour(0,1,local.villagers.capability.CropPolicy.CropKind.WHEAT));
        var inv=new SimpleContainer(new ItemStack(Items.WHEAT_SEEDS,2),new ItemStack(Items.CARROT,2));
        var plan=PlantingMaterials.reserve(inv,item -> local.villagers.capability.CropLayout.preference(
                item==Items.WHEAT_SEEDS?local.villagers.capability.CropPolicy.CropKind.WHEAT:local.villagers.capability.CropPolicy.CropKind.CARROT,cross));
        assertTrue(plan.before().is(Items.CARROT));assertTrue(PlantingMaterials.commit(inv,plan));
        assertEquals(1,inv.countItem(Items.CARROT));assertEquals(2,inv.countItem(Items.WHEAT_SEEDS));
    }
}
