package local.villagers;

import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class MineVegetationTest {
    @BeforeAll static void registries(){BreadCraftingTest.initializeItemRegistries();}
    @Test void ordinaryFlowersAndGrassNeedNoExcavationForAnActualWalkingRoute(){
        for(var block:java.util.List.of(Blocks.AIR,Blocks.CORNFLOWER,Blocks.POPPY,Blocks.SHORT_GRASS,Blocks.FERN,Blocks.TALL_GRASS,Blocks.LARGE_FERN))
            assertTrue(MineStair.clearCell(block.defaultBlockState()),block.toString());
    }
    @Test void fluidsDangerousPlantsAndCollidingBlocksAreNeverImplicitlyCleared(){
        for(var block:java.util.List.of(Blocks.WATER,Blocks.LAVA,Blocks.FIRE,Blocks.WITHER_ROSE,Blocks.SWEET_BERRY_BUSH,Blocks.CACTUS,Blocks.OAK_LEAVES,Blocks.STONE,Blocks.OAK_FENCE))
            assertFalse(MineStair.clearCell(block.defaultBlockState()),block.toString());
    }
}
