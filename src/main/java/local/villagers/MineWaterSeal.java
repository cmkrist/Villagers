package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;

/** Spend carried dirt to isolate a real water neighbor before opening a dry mine face. */
final class MineWaterSeal {
    private static final String TAG="villagers:mine_water_seal:";
    record Choice(BlockPos origin,BlockPos face,BlockPos water,Vec3 stand){}
    static final class State {
        Choice choice;boolean returning;long until;int placed;
        Map<String,Object> receipt=Map.of();String activity="no water sealing job";
        void clear(){choice=null;returning=false;}
        Map<String,Object> status(){return Map.of("placed",placed,"activity",activity,"active",choice!=null,"lastReceipt",receipt);}
    }
    static boolean retained(Villager v,BlockPos p){return v.entityTags().contains(TAG+p.asLong());}
    private static boolean enabled(ServerLevel level,Villager v){
        return NativeRegionsScenario.nativeRegions(level)&&Villagers.config().nativeAdventureQuarry()
            &&AdventurePreparation.nominated(v)&&AdventureActions.PERMISSIONS.allows(Capability.REPAIR_QUARRY_ACCESS);
    }
    private static boolean dry(ServerLevel level,AABB body){
        var a=BlockPos.containing(body.minX+1e-6,body.minY+1e-6,body.minZ+1e-6);
        var b=BlockPos.containing(body.maxX-1e-6,body.maxY-1e-6,body.maxZ-1e-6);
        for(var p:BlockPos.betweenClosed(a,b))if(!level.isLoaded(p)||!level.getFluidState(p).isEmpty())return false;
        return true;
    }
    /** Both virtual inspection and actual walking use the same real body/support checks. */
    static boolean supportedWalk(ServerLevel level,Villager v,Vec3 from,Vec3 to){
        if(Math.abs(to.y-from.y)>.01||from.distanceToSqr(to)>1.25*1.25)return false;
        var a=v.getBoundingBox().move(from.subtract(v.position()));var b=v.getBoundingBox().move(to.subtract(v.position()));
        var swept=a.minmax(b);
        if(!level.noBorderCollision(v,b)||!level.noBlockCollision(v,swept)||!dry(level,swept))return false;
        var neighbors=level.getEntities(v,swept,e->e instanceof net.minecraft.world.entity.LivingEntity&&e.isAlive());
        if(NativeBodyRoutes.crossesBody(a,b,neighbors.stream().map(e->e.getBoundingBox()).toList()))return false;
        int samples=Math.max(1,(int)Math.ceil(from.distanceTo(to)/.1));
        for(int i=0;i<=samples;i++){
            var feet=from.lerp(to,(double)i/samples);var body=v.getBoundingBox().move(feet.subtract(v.position()));
            var below=BlockPos.containing(feet).below();
            if(!FloorCentering.safeSupport(level.getBlockState(below))||level.noBlockCollision(v,body.move(0,-.03,0)))return false;
        }
        return true;
    }
    static Set<BlockPos> waters(ServerLevel level,BlockPos face){
        var result=new LinkedHashSet<BlockPos>();
        for(var d:Direction.values()){
            var p=face.relative(d);if(!level.isLoaded(p))return Set.of();
            if(!level.getFluidState(p).isEmpty()){
                if(!level.getBlockState(p).is(Blocks.WATER))return Set.of();
                result.add(p.immutable());
            }
        }
        return result;
    }
    static Choice preview(ServerLevel level,Villager v,Session s,BlockPos origin,Direction direction,boolean flat){
        if(!enabled(level,v)||v.getInventory().countItem(Items.DIRT)<1)return null;
        var cells=MineStair.step(origin,direction,1,flat);var foot=cells.getLast();
        if(!MineStair.newWorkFloor(s.mineStair,cells)||cells.stream().anyMatch(p->AdventureQuarry.accessSupport(v,p))||!level.isLoaded(foot.below())
            ||!level.getBlockState(foot.below()).isFaceSturdy(level,foot.below(),Direction.UP))return null;
        var face=cells.stream().filter(p->!MineStair.clearCell(level.getBlockState(p))).findFirst().orElse(null);
        if(face==null||MineStair.retainsFloor(s.mineStair,cells))return null;
        var water=waters(level,face);
        if(!AdventureMining.afterWaterSeals(level,v,face,origin.getY(),water)
            ||BlockContact.point(level,v,ForestryWork.standEye(v,origin),face,3)==null)return null;
        var center=Vec3.atBottomCenterOf(origin);var stances=new ArrayList<Vec3>();stances.add(center);
        for(var d:Direction.Plane.HORIZONTAL)stances.add(center.add(d.getStepX()*.6,0,d.getStepZ()*.6));
        for(var target:water){
            if(ConstructionSites.isProtected(level,target)||NativeTerrainWork.structuralCover(level,target)!=null
                ||!level.getWorldBorder().isWithinBounds(target)||!VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(target))
                ||!level.getEntities(null,new AABB(target)).isEmpty())continue;
            for(var stand:stances){
                if(!supportedWalk(level,v,center,stand))continue;
                var eye=stand.add(0,v.getEyeHeight(),0);
                if(MineFoundation.contact(level,v,target,eye)!=null)return new Choice(origin.immutable(),face.immutable(),target.immutable(),stand);
            }
        }
        return null;
    }
    static String work(ServerLevel level,Villager v,Session s,BlockPos origin,List<Direction> directions,long now){
        if(!enabled(level,v)||!v.onGround()||!WoodBootstrap.safe(level,v,s))return null;
        for(boolean flat:List.of(false,true))for(var d:directions){
            var choice=preview(level,v,s,origin,d,flat);if(choice==null)continue;
            var w=s.mineStair.waterSeals;w.choice=choice;w.returning=false;w.until=now+400;
            return resume(level,v,s,now);
        }
        return null;
    }
    static String resume(ServerLevel level,Villager v,Session s,long now){
        var w=s.mineStair.waterSeals;if(w.choice==null)return null;
        if(!enabled(level,v)||!WoodBootstrap.safe(level,v,s)){w.clear();return null;}
        if(now>=w.until)w.returning=true;
        var choice=w.choice;var feet=w.returning?Vec3.atBottomCenterOf(choice.origin()):choice.stand();
        if(v.position().distanceToSqr(feet)>.05*.05){
            if(!v.onGround()||!supportedWalk(level,v,v.position(),feet)){w.clear();v.getNavigation().stop();return null;}
            v.getNavigation().stop();v.getMoveControl().setWantedPosition(feet.x,feet.y,feet.z,.25);
            return w.activity=w.returning?"returning from a supported water-sealing stance":"walking to a supported stance to seal native mine water";
        }
        if(w.returning){w.clear();return null;}
        w.returning=true;w.until=now+400;
        var target=choice.water();var beforeState=level.getBlockState(target);
        var hit=MineFoundation.contact(level,v,target);
        var water=waters(level,choice.face());
        if(!v.onGround()||!beforeState.is(Blocks.WATER)||hit==null||!water.contains(target)
            ||!AdventureMining.afterWaterSeals(level,v,choice.face(),choice.origin().getY(),water)
            ||ConstructionSites.isProtected(level,target)||NativeTerrainWork.structuralCover(level,target)!=null
            ||!VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(target))||!level.getEntities(null,new AABB(target)).isEmpty())return w.activity="rechecking actual water-sealing contact";
        var bag=v.getInventory();int slot=-1;for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).is(Items.DIRT)){slot=i;break;}
        if(slot<0)return w.activity="water seal needs actual carried dirt";
        int before=bag.countItem(Items.DIRT);
        if(!level.setBlock(target,Blocks.DIRT.defaultBlockState(),Block.UPDATE_ALL))return null;
        bag.removeItem(slot,1);v.addTag(TAG+target.asLong());w.placed++;
        s.mineStair.rejected.remove(choice.origin());
        w.receipt=Map.of("gameTime",now,"position",target.toShortString(),"replaced",beforeState.toString(),
            "dirtBefore",before,"dirtAfter",bag.countItem(Items.DIRT),"faceToMine",choice.face().toShortString(),
            "anchor",hit.getBlockPos().toShortString(),"contact",hit.getLocation().toString(),"reach",v.getEyePosition().distanceTo(hit.getLocation()));
        v.getNavigation().stop();v.swingForAttack(InteractionHand.MAIN_HAND);level.gameEvent(v,GameEvent.BLOCK_PLACE,target);Villagers.exert(v,.005f);
        s.mind.remember("Used one owned dirt block to seal native water beside a dry mine face at "+target.toShortString());
        return w.activity="sealed native mine water with one carried dirt block";
    }
    private MineWaterSeal(){}
}
