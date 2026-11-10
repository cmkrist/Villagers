package local.villagers;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;

/** Physically reached food stores in the villager's own native village or captive home structure. */
final class NativeFoodStorage {
    static final class State {
        BlockPos target;long scanAt,pathAt,until;int transfers;
        String activity="not seeking stored food";
        Map<String,Object> receipt=Map.of();final Map<BlockPos,Long> blocked=new HashMap<>();
    }
    static Map<String,Object> status(Session s){var w=s.nativeFoodStorage;return Map.of("activity",w.activity,"transfers",w.transfers,"target",w.target==null?"":w.target.toShortString(),"lastReceipt",w.receipt);}
    static boolean homeStructure(ServerLevel level,Villager v,Session s,BlockPos p){
        var home=s.mind.home();
        if(home==null||!home.dimension().equals(level.dimension().identifier().toString())||!level.isLoaded(p)||!level.isLoaded(new BlockPos(home.x(),home.y(),home.z()))
            ||!VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(p)))return false;
        var start=level.structureManager().getStructureWithPieceAt(p,h->h.is(StructureTags.VILLAGE)
            ||h.getRegisteredName().equals("nova_structures:illager_barracks"));
        if(!start.isValid())return false;
        var own=level.structureManager().getStructureWithPieceAt(new BlockPos(home.x(),home.y(),home.z()),h->h.value()==start.getStructure());
        return own.isValid()&&own.getBoundingBox().getCenter().equals(start.getBoundingBox().getCenter());
    }
    static boolean visible(ServerLevel level,Villager v,BlockPos p){
        return visibleFrom(level,v,v.getEyePosition(),p);
    }
    private static boolean visibleFrom(ServerLevel level,Villager v,Vec3 eye,BlockPos p){
        var hit=level.clip(new net.minecraft.world.level.ClipContext(eye,Vec3.atCenterOf(p),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,v));
        return hit.getType()==HitResult.Type.MISS||hit.getBlockPos().equals(p);
    }
    static String work(ServerLevel level,Villager v,Session s,LivingEntity threat,long now){
        var w=s.nativeFoodStorage;
        boolean hungry=s.mind.foodLevel()<=12||local.villagers.mind.MealPolicy.needsRecoveryFood(s.mind.foodLevel(),v.getHealth(),v.getMaxHealth());
        if(!NativeRegionsScenario.nativeRegions(level)||!hungry||v.isBaby()||v.isSleeping()||v.isTrading()||v.isInWater()||v.isOnFire()||v.isPassenger()
            ||FoodInventory.anySlot(v.getInventory(),AdventureActions.canHarvest(v))>=0||!AdventureActions.PERMISSIONS.allows(local.villagers.capability.CapabilityPermissions.Capability.OPEN_NATIVE_FOOD_STORAGE))return null;
        boolean attacked=v.getLastHurtByMob()!=null&&v.getLastHurtByMob().isAlive()&&v.tickCount-v.getLastHurtByMobTimestamp()<100;
        boolean mayWalk=v.getHealth()>=v.getMaxHealth()*.5&&!attacked&&(threat==null||s.mind.foodLevel()<=4);
        if(w.target!=null&&(now>=w.until||!homeStructure(level,v,s,w.target)||NativeContainers.accessible(level,w.target)==null)){
            w.blocked.put(w.target,now+400);w.target=null;
        }
        if(w.target==null&&now>=w.scanAt){
            w.scanAt=now+100;w.blocked.entrySet().removeIf(e->e.getValue()<=now);var origin=v.blockPosition();double best=Double.MAX_VALUE;
            int radius=mayWalk?12:3;
            for(var p:BlockPos.betweenClosed(origin.offset(-radius,-3,-radius),origin.offset(radius,3,radius))){
                if(w.blocked.containsKey(p)||!level.isLoaded(p)||NativeContainers.accessible(level,p)==null||!homeStructure(level,v,s,p)||!visible(level,v,p))continue;
                double d=v.getEyePosition().distanceToSqr(Vec3.atCenterOf(p));if(!mayWalk&&d>9)continue;
                if(d<best){best=d;w.target=p.immutable();}
            }
            w.until=now+400;
        }
        if(w.target==null)return null;
        var p=w.target;
        if(v.getEyePosition().distanceToSqr(Vec3.atCenterOf(p))<=9&&visible(level,v,p)){
            var source=NativeContainers.accessible(level,p);if(source==null)return null;
            // Only this contact reads contents and triggers the saved native loot table.
            for(int i=0;i<source.getContainerSize();i++){
                var item=source.getItem(i);if(!FoodInventory.supported(item)||!v.getInventory().canAddItem(item.copyWithCount(1)))continue;
                int before=item.getCount();String name=item.getItem().toString();var actual=source.removeItem(i,1);
                if(actual.isEmpty())continue;var remaining=v.getInventory().addItem(actual);
                if(!remaining.isEmpty()){
                    if(source.getItem(i).isEmpty())source.setItem(i,remaining);else source.getItem(i).grow(remaining.getCount());return null;
                }
                source.setChanged();w.transfers++;w.target=null;w.scanAt=now+20;
                w.receipt=Map.of("gameTime",now,"container",p.toShortString(),"item",name,"sourceBefore",before,"sourceAfter",source.getItem(i).getCount(),"moved",1);
                s.mind.remember("Collected one actual "+name+" from a native home food store at "+p.toShortString());
                level.gameEvent(v,GameEvent.CONTAINER_OPEN,p);v.getNavigation().stop();return w.activity="collected a real stored meal";
            }
            w.blocked.put(p,now+1200);w.target=null;return null;
        }
        if(!mayWalk)return null;
        if(now>=w.pathAt&&WorkNavigation.admit(level,now,v)){
            w.pathAt=now+40;var stands=new ArrayList<BlockPos>();
            for(var q:BlockPos.betweenClosed(p.offset(-2,-1,-2),p.offset(2,1,2))){
                var eye=Vec3.atBottomCenterOf(q).add(0,v.getEyeHeight(),0);
                if(level.isLoaded(q.above())&&level.getBlockState(q).isAir()&&level.getBlockState(q.above()).isAir()&&level.getFluidState(q).isEmpty()
                    &&level.getBlockState(q.below()).isFaceSturdy(level,q.below(),Direction.UP)&&eye.distanceToSqr(Vec3.atCenterOf(p))<=9&&visibleFrom(level,v,eye,p))stands.add(q.immutable());
            }
            stands.sort(Comparator.comparingDouble(q->q.distSqr(v.blockPosition())));boolean started=false;
            for(var q:stands.stream().limit(4).toList()){
                var path=WorkNavigation.pathTo(level,v,s,q,0);if(path==null)continue;int y=v.getBlockY();boolean safe=true;
                for(int i=0;i<path.getNodeCount();i++){var n=path.getNodePos(i);if(n.getY()<y-2||!level.getFluidState(n).isEmpty()){safe=false;break;}y=n.getY();}
                if(safe&&v.getNavigation().moveTo(path,.6)){started=true;break;}
            }
            if(!started){w.blocked.put(p,now+400);w.target=null;return null;}
        }
        return w.activity="walking to a native home food store";
    }
    private NativeFoodStorage(){}
}
