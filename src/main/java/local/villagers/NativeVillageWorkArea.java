package local.villagers;

import local.villagers.mind.AgentMind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** The actual remembered native village bell bounds shared daytime work; it grants no block actions. */
final class NativeVillageWorkArea {
    static boolean contains(ServerLevel level,AgentMind mind,Vec3 point){
        if(!NativeRegionsScenario.nativeRegions(level)||!(level.getEntity(mind.id()) instanceof Villager v))return false;
        var meeting=v.getBrain().getMemory(MemoryModuleType.MEETING_POINT);
        var home=mind.home();
        if(meeting.isEmpty()||home==null||!home.dimension().equals(level.dimension().identifier().toString())
                ||!meeting.get().dimension().equals(level.dimension()))return false;
        var bell=meeting.get().pos();var target=BlockPos.containing(point);
        return level.isLoaded(bell)&&level.getBlockState(bell).is(Blocks.BELL)
            &&level.isLoaded(target)&&level.getWorldBorder().isWithinBounds(target)
            &&local.villagers.survival.SurvivalPolicy.withinVillageWorkArea(
                Vec3.atCenterOf(bell).distanceTo(Vec3.atCenterOf(new BlockPos(home.x(),home.y(),home.z()))),
                Vec3.atCenterOf(bell).distanceTo(point));
    }
    private NativeVillageWorkArea(){}
}
