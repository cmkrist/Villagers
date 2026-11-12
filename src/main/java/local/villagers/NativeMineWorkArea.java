package local.villagers;

import local.villagers.survival.SurvivalPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

/** A retained, paid mine grants its nominated worker a bounded vertical work area.
 * This is a navigation boundary, never a block-action or ore-observation grant. */
final class NativeMineWorkArea {
    static boolean column(BlockPos entry,Vec3 point,int minimumY){
        if(entry==null||!Double.isFinite(point.x)||!Double.isFinite(point.y)||!Double.isFinite(point.z))return false;
        double dx=point.x-entry.getX()-.5,dz=point.z-entry.getZ()-.5;
        return dx*dx+dz*dz<=32*32&&point.y>=minimumY+3&&point.y<=entry.getY()+2;
    }
    static boolean contains(ServerLevel level,local.villagers.mind.AgentMind mind,Vec3 point){
        if(!NativeRegionsScenario.nativeRegions(level)||!Villagers.config().nativeAdventureQuarry()
            ||!(level.getEntity(mind.id()) instanceof Villager v)||!AdventurePreparation.nominated(v))return false;
        var s=Villagers.existingSession(v.getUUID());if(s==null)return false;
        MineStair.resume(v,s);var w=s.mineStair;
        if(w.floors.isEmpty())return false;
        var home=mind.home();
        if(home==null||!home.dimension().equals(level.dimension().identifier().toString()))return false;
        var target=BlockPos.containing(point);
        if(!level.isLoaded(target)||!level.getWorldBorder().isWithinBounds(target))return false;
        var entries=new java.util.LinkedHashSet<BlockPos>(w.entries);if(w.entry!=null)entries.add(w.entry);
        for(var entry:entries){
            if(!level.isLoaded(entry)||!column(entry,point,level.getMinY()))continue;
            var entrance=Vec3.atCenterOf(entry);
            boolean local=SurvivalPolicy.withinHomeRange(entrance.distanceTo(Vec3.atCenterOf(new BlockPos(home.x(),home.y(),home.z()))))
                ||NativeVillageWorkArea.contains(level,mind,entrance);
            if(local)return true;
        }
        return false;
    }
    private NativeMineWorkArea(){}
}
