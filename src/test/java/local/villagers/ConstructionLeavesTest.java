package local.villagers;

import java.util.List;
import java.util.UUID;
import local.villagers.construction.WorkSites;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionLeavesTest {
    @BeforeAll static void registries() { WoodCraftingTest.registries(); }
    private static WorkSites.Point p(int x,int y) { return new WorkSites.Point("minecraft:overworld",x,y,0); }
    @Test void onlyUnchangedPendingApprovedMangroveLeavesQualify() {
        var leaf=p(1,73);var log=p(0,74);
        var job=new WorkSites.Forestry(UUID.randomUUID(),UUID.randomUUID(),p(0,72),List.of(log),p(3,72),"minecraft:mud",true,List.of(log),List.of(leaf),List.of(leaf));
        var state=Blocks.MANGROVE_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE,3);
        assertTrue(ConstructionSites.unchangedApprovedLeaf(job,leaf,state,state));
        assertFalse(ConstructionSites.unchangedApprovedLeaf(job,leaf,state,state.setValue(LeavesBlock.DISTANCE,4)));
        assertFalse(ConstructionSites.unchangedApprovedLeaf(job,leaf,state,state.setValue(LeavesBlock.PERSISTENT,true)));
        assertFalse(ConstructionSites.unchangedApprovedLeaf(job,leaf,state,Blocks.AIR.defaultBlockState()));
        assertFalse(ConstructionSites.unchangedApprovedLeaf(job,log,state,state));
        assertFalse(ConstructionSites.unchangedApprovedLeaf(job,leaf,Blocks.OAK_LEAVES.defaultBlockState(),Blocks.OAK_LEAVES.defaultBlockState()));
        assertFalse(ConstructionSites.unchangedApprovedLeaf(job.cleared(leaf),leaf,state,state));
        assertFalse(ConstructionSites.unchangedApprovedLeaf(null,leaf,state,state));
        // A direct same-state write still revokes the permission; only the native caller is wrapped.
        assertTrue(WorkSites.empty().add(job).touch(leaf).forestry().isEmpty());
    }
}
