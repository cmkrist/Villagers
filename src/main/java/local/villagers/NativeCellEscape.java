package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.*;

/** Hungry captives may physically dismantle their own native iron door or wooden cage exit. */
final class NativeCellEscape {
    static final class State {
        BlockPos target,fenceBase;int fences;long scanAt,pathAt,started,lastWork;int broken;
        ItemStack tool=ItemStack.EMPTY;String activity="no cell escape";
        Map<String,Object> receipt=Map.of();final Map<BlockPos,Long> blocked=new HashMap<>();
    }
    static Map<String,Object> status(Session s){var w=s.nativeCellEscape;return Map.of("activity",w.activity,"brokenDoors",w.broken,"brokenFenceBlocks",w.fences,"target",w.target==null?"":w.target.toShortString(),"lastReceipt",w.receipt,"workTicks",w.started==0?0:w.lastWork-w.started);}
    private static boolean door(ServerLevel level,Villager v,Session s,BlockPos p){
        if(!level.isLoaded(p.above())||!level.getWorldBorder().isWithinBounds(p)||ConstructionSites.isProtected(level,p)||ConstructionSites.isProtected(level,p.above()))return false;
        var b=level.getBlockState(p);var top=level.getBlockState(p.above());
        if(!b.is(Blocks.IRON_DOOR)||b.getValue(DoorBlock.HALF)!=DoubleBlockHalf.LOWER||b.getValue(DoorBlock.OPEN)
            ||!top.is(Blocks.IRON_DOOR)||top.getValue(DoorBlock.HALF)!=DoubleBlockHalf.UPPER)return false;
        var h=s.mind.home();if(h==null||!h.dimension().equals(level.dimension().identifier().toString()))return false;
        var home=new BlockPos(h.x(),h.y(),h.z());if(!level.isLoaded(home))return false;
        var building=level.structureManager().getStructureWithPieceAt(home,t->t.getRegisteredName().equals("nova_structures:illager_barracks"));
        if(!building.isValid()||!building.getBoundingBox().isInside(p)||!building.getBoundingBox().isInside(v.blockPosition()))return false;
        // Both sides must be actual supported dry floor, not an exit over a drop or into water.
        var facing=b.getValue(DoorBlock.FACING);
        return exitFloor(level,p.relative(facing))&&exitFloor(level,p.relative(facing.getOpposite()));
    }
    private static boolean fence(ServerLevel level,Villager v,Session s,BlockPos base,boolean lowerRemoved){
        if(!level.isLoaded(base.above(2))||!level.getWorldBorder().isWithinBounds(base)
            ||ConstructionSites.isProtected(level,base)||ConstructionSites.isProtected(level,base.above())
            ||!NativeFoodStorage.homeStructure(level,v,s,base)||!NativeFoodStorage.homeStructure(level,v,s,v.blockPosition()))return false;
        var h=s.mind.home();if(h==null)return false;
        var home=new BlockPos(h.x(),h.y(),h.z());
        if(!level.structureManager().getStructureWithPieceAt(home,t->t.getRegisteredName().equals("nova_structures:illager_barracks")).isValid())return false;
        var middle=level.getBlockState(base.above());var top=level.getBlockState(base.above(2));
        if(!middle.is(net.minecraft.tags.BlockTags.WOODEN_FENCES)||!top.is(middle.getBlock()))return false;
        if(lowerRemoved?!level.getBlockState(base).isAir():!level.getBlockState(base).is(middle.getBlock()))return false;
        // The two removed panels provide actual adult clearance under the retained top fence.
        return exitFloor(level,base.east())&&exitFloor(level,base.west())
            ||exitFloor(level,base.north())&&exitFloor(level,base.south());
    }
    private static boolean exitFloor(ServerLevel level,BlockPos side){
        for(var p:List.of(side,side.below()))if(level.isLoaded(p.below())&&level.isLoaded(p.above())
            &&level.getWorldBorder().isWithinBounds(p)&&level.getBlockState(p).getCollisionShape(level,p).isEmpty()
            &&level.getBlockState(p.above()).getCollisionShape(level,p.above()).isEmpty()
            &&level.getFluidState(p).isEmpty()&&level.getFluidState(p.above()).isEmpty()
            &&level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))return true;
        return false;
    }
    private static boolean homeRoute(ServerLevel level,Villager v,Session s,net.minecraft.world.level.pathfinder.Path path){
        if(path==null||!path.canReach()||path.getNodeCount()<1||path.getNodeCount()>128)return false;
        int y=v.getBlockY();
        for(int i=0;i<path.getNodeCount();i++){
            var at=path.getNodePos(i);
            if(!NativeFoodStorage.homeStructure(level,v,s,at)||!level.getWorldBorder().isWithinBounds(at)
                ||!level.getFluidState(at).isEmpty()||at.getY()<y-(v.getHealth()<=5?1:2))return false;
            y=at.getY();
        }
        return true;
    }
    private static boolean touch(ServerLevel level,Villager v,BlockPos p){
        return v.getEyePosition().distanceToSqr(Vec3.atCenterOf(p))<=9&&NativeFoodStorage.visible(level,v,p);
    }
    static String work(ServerLevel level,Villager v,Session s,long now){
        var w=s.nativeCellEscape;
        if(!NativeRegionsScenario.nativeRegions(level)||!Villagers.config().nativeCellEscape()||!AdventureActions.PERMISSIONS.allows(Capability.ESCAPE_NATIVE_CELL)
            ||s.mind.foodLevel()>6||FoodInventory.anySlot(v.getInventory(),AdventureActions.canHarvest(v))>=0||v.isBaby()||v.isTrading()||v.isPassenger()||v.isInWater()||v.isOnFire()
            ||v.getLastHurtByMob()!=null&&v.getLastHurtByMob().isAlive()&&v.tickCount-v.getLastHurtByMobTimestamp()<100){w.started=0;return null;}
        if(v.isSleeping())v.stopSleeping();
        if(w.target!=null&&!(w.fenceBase==null?door(level,v,s,w.target):fence(level,v,s,w.fenceBase,!w.target.equals(w.fenceBase)))){w.target=null;w.fenceBase=null;w.started=0;}
        if(w.target==null){
            if(!s.nativeFoodStorage.routeBlocked)return null;
            if(now<w.scanAt||!WorkNavigation.admit(level,now,v))return null;
            w.scanAt=now+200;w.blocked.entrySet().removeIf(e->e.getValue()<=now);
            var origin=v.blockPosition();var doors=new ArrayList<BlockPos>();
            for(var p:BlockPos.betweenClosed(origin.offset(-10,-1,-10),origin.offset(10,1,10)))
                if(!w.blocked.containsKey(p)&&(door(level,v,s,p)||p.getY()==v.getBlockY()&&fence(level,v,s,p,false)))doors.add(p.immutable());
            doors.sort(Comparator.comparingDouble(p->p.distSqr(origin)));
            for(var p:doors){
                var path=v.getNavigation().createPath(p,1,32);
                if(path!=null&&path.canReach()&&homeRoute(level,v,s,path)){w.target=p;w.fenceBase=door(level,v,s,p)?null:p;break;}
            }
        }
        if(w.target==null)return null;
        var p=w.target;
        if(!touch(level,v,p)){
            w.started=0;
            if(now>=w.pathAt&&WorkNavigation.admit(level,now,v)){
                w.pathAt=now+40;var path=v.getNavigation().createPath(p,1,32);
                if(path==null||!path.canReach()||!homeRoute(level,v,s,path)||!v.getNavigation().moveTo(path,.6)){
                    w.blocked.put(p,now+1200);w.target=null;w.fenceBase=null;return null;
                }
            }
            return w.activity="approaching a native cell barrier to escape hunger";
        }
        v.getNavigation().stop();v.swingForAttack(InteractionHand.MAIN_HAND);
        var block=level.getBlockState(p);var held=v.getMainHandItem();
        if(w.started==0||now-w.lastWork>20||!ItemStack.matches(w.tool,held)){w.started=now;w.tool=held.copy();}
        w.lastWork=now;
        boolean correct=!block.requiresCorrectToolForDrops()||held.isCorrectToolForDrops(block);
        int duration=(int)Math.ceil(block.getDestroySpeed(level,p)*(correct?30:100)/Math.max(1,held.getDestroySpeed(block)));
        if(now-w.started<duration)return w.activity=w.fenceBase==null?"breaking an iron cell door with the actual held tool":"dismantling a wooden cage panel with the actual held tool";
        if(!(w.fenceBase==null?door(level,v,s,p):fence(level,v,s,w.fenceBase,!p.equals(w.fenceBase)))||!ItemStack.matches(w.tool,held)||!touch(level,v,p))return null;
        // Native removal handles the other door half. Incorrect tools receive no drops.
        if(!level.destroyBlock(p,correct,v))return null;
        var tool=held.get(DataComponents.TOOL);if(tool!=null)held.hurtAndBreak(tool.damagePerBlock(),v,EquipmentSlot.MAINHAND);
        if(w.fenceBase==null)w.broken++;else w.fences++;
        w.receipt=Map.of("gameTime",now,"barrier",p.toShortString(),"workTicks",now-w.started,"tool",w.tool.toString(),"correctTool",correct,
            "block",block.toString(),"nativeDrops",correct);
        s.mind.remember("Physically dismantled my native cell barrier to seek food at "+p.toShortString());
        if(w.fenceBase!=null&&p.equals(w.fenceBase))w.target=p.above();else {
            w.target=null;w.fenceBase=null;
            // Retry the actual meal route through the new exit before any more demolition.
            s.nativeFoodStorage.routeBlocked=false;s.nativeFoodStorage.blocked.clear();s.nativeFoodStorage.target=null;
        }
        w.started=0;s.nativeFoodStorage.scanAt=now;return w.activity="opened a real part of the native cell exit";
    }
    private NativeCellEscape(){}
}
