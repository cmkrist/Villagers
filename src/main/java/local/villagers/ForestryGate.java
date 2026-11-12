package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;

/** Reach and operate a real unpowered village gate beside observed preparation wood. */
final class ForestryGate {
    private static final class Opened {
        final BlockState before;final double entrySide;boolean entered;
        Opened(BlockState before,double side){this.before=before;entrySide=side;}
    }
    static final class State {
        BlockPos gate,tree;long until,pathAt;int opened,closed;
        final Map<BlockPos,Long> tried=new HashMap<>();
        final Map<BlockPos,Opened> doors=new LinkedHashMap<>();
        Map<String,Object> receipt=Map.of();
        void clear(){gate=tree=null;}
    }
    static Map<String,Object> status(Session s){
        var w=s.forestryGate;return Map.of("gate",w.gate==null?"":w.gate.toShortString(),"opened",w.opened,"closed",w.closed,"lastReceipt",w.receipt);
    }
    private static boolean valid(ServerLevel level,Villager v,Session s,BlockPos p){
        if(!AdventureSupplies.inVillage(level,v,s,p))return false;
        var b=level.getBlockState(p);return b.getBlock() instanceof FenceGateBlock&&!b.getValue(FenceGateBlock.POWERED)&&!b.getValue(FenceGateBlock.OPEN);
    }
    private static double side(Villager v,BlockPos gate,net.minecraft.core.Direction facing){
        return (v.getX()-gate.getX()-.5)*facing.getStepX()+(v.getZ()-gate.getZ()-.5)*facing.getStepZ();
    }
    private static net.minecraft.world.level.pathfinder.Path approach(ServerLevel level,Villager v,Session s,BlockPos gate){
        var facing=level.getBlockState(gate).getValue(FenceGateBlock.FACING);var stands=new ArrayList<BlockPos>();
        for(var d:List.of(facing,facing.getOpposite()))for(int distance=1;distance<=2;distance++)for(int dy=-1;dy<=1;dy++){
            var p=gate.relative(d,distance).offset(0,dy,0);
            if(!level.isLoaded(p.above())||!level.getBlockState(p).isAir()||!level.getBlockState(p.above()).isAir()
                ||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP)
                ||BlockContact.interactionPoint(level,v,ForestryWork.standEye(v,p),gate,3)==null)continue;
            stands.add(p);
        }
        stands.sort(Comparator.comparingDouble(p->p.distSqr(v.blockPosition())));
        for(var stand:stands.stream().limit(6).toList()){
            var path=NativePathLimits.search(v,1,()->WorkNavigation.pathTo(level,v,s,stand,0));
            if(WorkNavigation.staysHome(level,v,s,path)&&BoatTravel.dryPath(level,path))return path;
        }
        return null;
    }
    /** Called within the existing admitted tree survey, after direct routes failed. */
    static boolean plan(ServerLevel level,Villager v,Session s,BlockPos tree,long now){
        if(!NativeRegionsScenario.nativeRegions(level)||!AdventureActions.PERMISSIONS.allows(Capability.OPEN_FENCE_GATE)
            ||!BoatPreparation.naturalTrunk(level,tree)||!TreeClaims.available(level,tree,v.getUUID(),now))return false;
        var w=s.forestryGate;w.tried.entrySet().removeIf(e->e.getValue()<=now);
        var gates=new ArrayList<BlockPos>();
        for(var p:BlockPos.betweenClosed(tree.offset(-4,-1,-4),tree.offset(4,1,4)))
            if(!w.tried.containsKey(p)&&valid(level,v,s,p)&&BoatRoutes.visible(level,v,Vec3.atCenterOf(p)))gates.add(p.immutable());
        gates.sort(Comparator.comparingDouble(p->p.distSqr(v.blockPosition())));
        for(var gate:gates.stream().limit(3).toList()){
            var path=approach(level,v,s,gate);
            if(path==null){w.tried.put(gate,now+1200);continue;}
            if(!TreeClaims.claim(level,tree,v.getUUID(),now)||!v.getNavigation().moveTo(path,.6))return false;
            w.gate=gate;w.tree=tree;w.until=now+1200;w.pathAt=now+40;return true;
        }
        return false;
    }
    static String work(ServerLevel level,Villager v,Session s,long now){
        var w=s.forestryGate;if(w.gate==null)return null;
        if(now>=w.until||!WoodBootstrap.safe(level,v,s)||!valid(level,v,s,w.gate)
            ||!AdventureActions.PERMISSIONS.allows(Capability.OPEN_FENCE_GATE)
            ||!BoatPreparation.naturalTrunk(level,w.tree)||!TreeClaims.claim(level,w.tree,v.getUUID(),now)){
            w.clear();return null;
        }
        var hit=BlockContact.interactionPoint(level,v,v.getEyePosition(),w.gate,3);
        if(hit==null){
            if(now>=w.pathAt&&WorkNavigation.admit(level,now,v)){
                w.pathAt=now+40;var path=approach(level,v,s,w.gate);
                if(path==null){w.tried.put(w.gate,now+1200);w.clear();return null;}
                v.getNavigation().moveTo(path,.6);
            }
            return "walking to an observed village gate beside natural wood";
        }
        var before=level.getBlockState(w.gate);var facing=before.getValue(FenceGateBlock.FACING);
        var opened=before.setValue(FenceGateBlock.OPEN,true);
        if(facing==v.getDirection().getOpposite())opened=opened.setValue(FenceGateBlock.FACING,v.getDirection());
        // FenceGateBlock's player interaction directly changes these same
        // native properties. No player stand-in or collision override is used.
        if(!level.setBlock(w.gate,opened,10))return null;
        w.doors.put(w.gate,new Opened(before,side(v,w.gate,facing)));w.opened++;w.tried.put(w.gate,now+2400);
        level.playSound(null,w.gate,SoundEvents.FENCE_GATE_OPEN,SoundSource.BLOCKS,1,1);
        level.gameEvent(v,GameEvent.BLOCK_OPEN,w.gate);
        w.receipt=Map.of("gameTime",now,"action","opened actual village fence gate","gate",w.gate.toShortString(),"tree",w.tree.toShortString(),"contact",hit.toString(),"reach",v.getEyePosition().distanceTo(hit));
        s.woodBootstrap.tree=w.tree;s.woodBootstrap.treeBlock=level.getBlockState(w.tree).getBlock();s.woodBootstrap.taken=0;s.woodBootstrap.nextPath=0;
        s.mind.remember("Opened the real village gate beside observed preparation wood at "+w.gate.toShortString());
        w.clear();return "opened an actual village gate to reach natural preparation wood";
    }
    static void closeReturned(ServerLevel level,Villager v,Session s){
        if(!AdventureActions.PERMISSIONS.allows(Capability.CLOSE_FENCE_GATE))return;
        var w=s.forestryGate;var it=w.doors.entrySet().iterator();
        while(it.hasNext()){
            var e=it.next();var p=e.getKey();var door=e.getValue();
            if(!level.isLoaded(p)||v.position().distanceToSqr(Vec3.atCenterOf(p))>64)continue;
            var b=level.getBlockState(p);
            if(!b.is(door.before.getBlock())||!b.getValue(FenceGateBlock.OPEN)||b.getValue(FenceGateBlock.POWERED)){it.remove();continue;}
            double current=side(v,p,door.before.getValue(FenceGateBlock.FACING));
            if(current*door.entrySide<0&&Math.abs(current)>.5)door.entered=true;
            if(!door.entered||current*door.entrySide<=0||Math.abs(current)<1
                ||BlockContact.interactionPoint(level,v,v.getEyePosition(),p,3)==null
                ||!level.getEntities(null,new AABB(p).inflate(.55,.1,.55)).isEmpty())continue;
            if(!level.setBlock(p,b.setValue(FenceGateBlock.OPEN,false).setValue(FenceGateBlock.FACING,door.before.getValue(FenceGateBlock.FACING)),10))continue;
            level.playSound(null,p,SoundEvents.FENCE_GATE_CLOSE,SoundSource.BLOCKS,1,1);level.gameEvent(v,GameEvent.BLOCK_CLOSE,p);
            w.closed++;it.remove();
        }
    }
    private ForestryGate(){}
}
