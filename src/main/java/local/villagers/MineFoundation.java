package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;

/** A real carried block may support an otherwise dry, new mining step. */
final class MineFoundation {
    static final class State {
        BlockPos origin,target;Vec3 stand;Direction direction;boolean flat,returning;long until;
        void clear(){origin=target=null;stand=null;returning=false;}
    }
    static BlockHitResult contact(ServerLevel level,Villager v,BlockPos target){return contact(level,v,target,v.getEyePosition());}
    static BlockHitResult contact(ServerLevel level,Villager v,BlockPos target,Vec3 eye){
        for(var side:Direction.values()){
            var anchor=target.relative(side.getOpposite());
            if(!level.isLoaded(anchor)||!level.getBlockState(anchor).isFaceSturdy(level,anchor,side))continue;
            for(double h:new double[]{.5,.9,.1}){
                var point=Vec3.atCenterOf(anchor).add(side.getStepX()*.499,side.getStepY()*.499,side.getStepZ()*.499);
                if(side.getAxis().isHorizontal())point=point.add(0,h-.5,0);
                var hit=level.clip(new ClipContext(eye,point,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,v));
                if(hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(anchor)&&hit.getDirection()==side
                    &&eye.distanceToSqr(hit.getLocation())<=9)return hit;
            }
        }
        return null;
    }
    static boolean candidate(ServerLevel level,Villager v,Session s,BlockPos origin,Direction direction,boolean flat){
        return candidate(level,v,s,origin,direction,flat,v.getEyePosition());
    }
    private static boolean candidate(ServerLevel level,Villager v,Session s,BlockPos origin,Direction direction,boolean flat,Vec3 eye){
        var cells=MineStair.step(origin,direction,1,flat);var target=cells.getLast().below();
        if(!MineStair.newWorkFloor(s.mineStair,cells)||s.mineStair.floors.contains(target)
            ||!level.isLoaded(target)||!level.getWorldBorder().isWithinBounds(target)
            ||ConstructionSites.isProtected(level,target)||AdventureQuarry.accessSupport(v,target)
            ||!VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(target))
            ||!(level.getBlockState(target).isAir()||level.getBlockState(target).is(Blocks.WATER))
            ||!level.getEntities(null,new AABB(target)).isEmpty())return false;
        // This ability bridges clear air; it never opens a wet excavation face.
        for(var p:cells){
            if(!level.isLoaded(p)||ConstructionSites.isProtected(level,p)||NativeTerrainWork.structuralCover(level,p)!=null)return false;
            if(MineStair.clearCell(level.getBlockState(p)))continue;
            // The real body/head gap must be dry and open. A reachable natural
            // overhead face may be mined after the carried floor is placed;
            // actual excavation still rechecks all ordinary mining guards.
            if(!p.equals(cells.getFirst())||!AdventureMining.exposedAt(level,v,p,true,origin.getY())
                ||BlockContact.point(level,v,eye,p,3)==null)return false;
        }
        return contact(level,v,target,eye)!=null;
    }
    private static boolean supportedWalk(ServerLevel level,Villager v,Vec3 feet){
        if(!v.onGround()||Math.abs(feet.y-v.getY())>.1||feet.distanceToSqr(v.position())>1.25*1.25)return false;
        var body=v.getBoundingBox().move(feet.subtract(v.position()));
        return level.noBorderCollision(v,body)&&level.noBlockCollision(v,v.getBoundingBox().minmax(body))
            &&!level.noBlockCollision(v,body.move(0,-.03,0));
    }
    static String resume(ServerLevel level,Villager v,Session s,long now){
        var w=s.mineStair.foundation;if(w.origin==null)return null;
        if(now>=w.until)w.returning=true;
        var feet=w.returning?Vec3.atBottomCenterOf(w.origin):w.stand;
        if(v.position().distanceToSqr(feet)>.0025){
            if(!supportedWalk(level,v,feet)){w.clear();v.getNavigation().stop();return null;}
            v.getNavigation().stop();v.getMoveControl().setWantedPosition(feet.x,feet.y,feet.z,.2);
            return w.returning?"returning from the supported foundation stance":"walking to a supported edge to reach a mine foundation";
        }
        if(w.returning){w.clear();return null;}
        var target=w.target;w.returning=true;w.until=now+200;
        var hit=contact(level,v,target);
        if(hit==null||!replaceable(level,target)||!candidate(level,v,s,w.origin,w.direction,w.flat))return "rechecking the real foundation contact";
        return place(level,v,s,target,hit,now);
    }
    private static boolean replaceable(ServerLevel level,BlockPos p){
        return level.isLoaded(p)&&(level.getBlockState(p).isAir()||level.getBlockState(p).is(Blocks.WATER))
            &&level.getEntities(null,new AABB(p)).isEmpty();
    }
    private static String place(ServerLevel level,Villager v,Session s,BlockPos target,BlockHitResult hit,long now){
        if(!v.onGround()||!WoodBootstrap.safe(level,v,s)||!replaceable(level,target)
            ||!AdventureActions.PERMISSIONS.allows(Capability.REPAIR_QUARRY_ACCESS))return null;
        var bag=v.getInventory();int slot=-1;
        for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).is(Items.DIRT)){slot=i;break;}
        if(slot<0)return null;
        var beforeState=level.getBlockState(target);int before=bag.countItem(Items.DIRT);
        if(!level.setBlock(target,Blocks.DIRT.defaultBlockState(),Block.UPDATE_ALL))return null;
        bag.removeItem(slot,1);s.mineStair.foundations++;
        s.mineStair.foundationReceipt=Map.of("gameTime",now,"position",target.toShortString(),
            "replaced",beforeState.toString(),"dirtBefore",before,"dirtAfter",bag.countItem(Items.DIRT),
            "anchor",hit.getBlockPos().toShortString(),"face",hit.getDirection().name(),
            "contact",hit.getLocation().toString(),"reach",v.getEyePosition().distanceTo(hit.getLocation()));
        v.getNavigation().stop();v.swingForAttack(InteractionHand.MAIN_HAND);
        level.gameEvent(v,GameEvent.BLOCK_PLACE,target);Villagers.exert(v,.005f);
        s.mind.remember("Used one owned dirt block to support a dry mine step at "+target.toShortString());
        return "placed carried dirt under a dry prospective mine step";
    }
    static String work(ServerLevel level,Villager v,Session s,BlockPos origin,List<Direction> directions,long now){
        if(!v.onGround()||!WoodBootstrap.safe(level,v,s)
            ||!AdventureActions.PERMISSIONS.allows(Capability.REPAIR_QUARRY_ACCESS))return null;
        var bag=v.getInventory();int slot=-1;
        for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).is(Items.DIRT)){slot=i;break;}
        if(slot<0)return null;
        for(boolean flat:List.of(false,true))for(var direction:directions){
            var target=MineStair.step(origin,direction,1,flat).getLast().below();
            if(candidate(level,v,s,origin,direction,flat)){
                var hit=contact(level,v,target);if(hit!=null)return place(level,v,s,target,hit,now);
            }
            // Keep at least .2 of the .6-wide body above its current footing.
            // This is a real short supported walk, never a body-position setter.
            var stand=Vec3.atBottomCenterOf(origin).add(direction.getStepX()*.6,0,direction.getStepZ()*.6);
            var eye=stand.add(0,v.getEyeHeight(),0);
            if(!supportedWalk(level,v,stand)||!candidate(level,v,s,origin,direction,flat,eye))continue;
            var w=s.mineStair.foundation;w.origin=origin;w.target=target;w.stand=stand;w.direction=direction;w.flat=flat;w.until=now+200;
            return resume(level,v,s,now);
        }
        return null;
    }
    private MineFoundation(){}
}
