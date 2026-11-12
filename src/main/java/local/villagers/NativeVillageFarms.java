package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Bare native village ground may be cultivated with real seeds, a real hoe and natural hydration. */
final class NativeVillageFarms {
    static boolean allowed(ServerLevel level,Villager v,Session s,BlockPos soil){
        return NativeRegionsScenario.nativeRegions(level)&&AdventureActions.PERMISSIONS.allows(Capability.CULTIVATE_NATIVE_VILLAGE)
            &&level.isLoaded(soil)&&level.isLoaded(soil.above(2))&&level.getWorldBorder().isWithinBounds(soil)
            &&!ConstructionSites.isProtected(level,soil)&&!ConstructionSites.isProtected(level,soil.above())
            &&NativeVillageWorkArea.contains(level,s.mind,Vec3.atCenterOf(soil.above()))
            &&!level.structureManager().getStructureWithPieceAt(soil,h->true).isValid();
    }
    static List<BlockPos> candidates(ServerLevel level,Villager v,Session s){
        if(!NativeRegionsScenario.nativeRegions(level))return List.of();
        var found=new ArrayList<BlockPos>();var origin=v.blockPosition();
        for(var p:BlockPos.betweenClosed(origin.offset(-8,-3,-8),origin.offset(8,2,8))){
            if(!level.isLoaded(p))continue;
            var b=level.getBlockState(p);
            if((b.is(Blocks.DIRT)||b.is(Blocks.GRASS_BLOCK))&&level.getBlockState(p.above()).isAir()&&allowed(level,v,s,p)
                &&BoatRoutes.visible(level,v,Vec3.atCenterOf(p).add(0,.5,0)))found.add(p.immutable());
        }
        return found;
    }
    private NativeVillageFarms(){}
}
