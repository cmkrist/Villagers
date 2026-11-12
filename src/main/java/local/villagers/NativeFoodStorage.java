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
        BlockPos target;boolean routeBlocked;long scanAt,pathAt,until;int transfers;
        net.minecraft.world.level.pathfinder.Path walking;
        final local.villagers.navigation.RouteProgress progress=new local.villagers.navigation.RouteProgress(120);
        final Map<BlockPos,Integer> standCursor=new LinkedHashMap<>();
        List<BlockPos> nextStands(BlockPos store,List<BlockPos> stands){
            if(stands.isEmpty())return List.of();int first=Math.floorMod(standCursor.getOrDefault(store,0),stands.size());
            int count=Math.min(4,stands.size());var batch=new ArrayList<BlockPos>();
            for(int i=0;i<count;i++)batch.add(stands.get((first+i)%stands.size()));
            standCursor.put(store,(first+count)%stands.size());
            while(standCursor.size()>16)standCursor.remove(standCursor.keySet().iterator().next());
            return batch;
        }
        String activity="not seeking stored food";
        Map<String,Object> receipt=Map.of(),pathReceipt=Map.of();final Map<BlockPos,Long> blocked=new HashMap<>();
    }
    static Map<String,Object> status(Session s){var w=s.nativeFoodStorage;return Map.of("activity",w.activity,"routeBlocked",w.routeBlocked,"transfers",w.transfers,"target",w.target==null?"":w.target.toShortString(),"lastReceipt",w.receipt,"lastPath",w.pathReceipt,"escape",NativeCellEscape.status(s),"mealRendezvous",s.campMealMeetReceipt);}
    private static net.minecraft.world.level.levelgen.structure.BoundingBox homeBounds(ServerLevel level,Villager v,Session s){
        var home=s.mind.home();
        // A homeless resident standing inside its actual native village may
        // inspect communal stores without first obtaining a scarce bed. No
        // bed, home memory or remote settlement is assigned by this fallback.
        if(home==null){
            var local=level.structureManager().getStructureWithPieceAt(v.blockPosition(),StructureTags.VILLAGE);
            return local.isValid()?local.getBoundingBox():null;
        }
        if(!home.dimension().equals(level.dimension().identifier().toString())||!level.isLoaded(new BlockPos(home.x(),home.y(),home.z())))return null;
        var origin=new BlockPos(home.x(),home.y(),home.z());
        var own=level.structureManager().getStructureWithPieceAt(origin,h->h.getRegisteredName().equals("nova_structures:illager_barracks"));
        if(!own.isValid())own=level.structureManager().getStructureWithPieceAt(origin,StructureTags.VILLAGE);
        // A native settlement includes its courtyards and outdoor stores, not
        // only the solid template pieces that make up individual buildings.
        return own.isValid()?own.getBoundingBox():null;
    }
    static boolean homeStructure(ServerLevel level,Villager v,Session s,BlockPos p){
        var bounds=homeBounds(level,v,s);return level.isLoaded(p)&&bounds!=null&&bounds.isInside(p);
    }
    static boolean visible(ServerLevel level,Villager v,BlockPos p){
        return visibleFrom(level,v,v.getEyePosition(),p);
    }
    private static boolean visibleFrom(ServerLevel level,Villager v,Vec3 eye,BlockPos p){
        var hit=level.clip(new net.minecraft.world.level.ClipContext(eye,Vec3.atCenterOf(p),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,v));
        return hit.getType()==HitResult.Type.MISS||hit.getBlockPos().equals(p);
    }
    static boolean waterBudget(boolean critical,int wetSteps){return wetSteps>=0&&(wetSteps==0||!critical&&wetSteps==1);}
    private static boolean shallowWater(ServerLevel level,BlockPos p,boolean critical){
        var fluid=level.getFluidState(p);
        return !critical&&fluid.is(net.minecraft.tags.FluidTags.WATER)&&fluid.isSource()
            &&level.getFluidState(p.above()).isEmpty()
            &&level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP);
    }
    /** The native starting node rounds partial-height feet upward; retain the real off-center body there. */
    static boolean actualStartFooting(int index,BlockPos node,BlockPos actor,double feetY){
        return index==0&&Double.isFinite(feetY)&&node.getX()==actor.getX()&&node.getZ()==actor.getZ()
            &&node.getY()==Math.ceil(feetY);
    }
    private static boolean safeRoute(ServerLevel level,Villager v,net.minecraft.world.level.levelgen.structure.BoundingBox bounds,
                                     net.minecraft.world.level.pathfinder.Path path,boolean critical){
        if(path==null||!path.canReach())return false;int y=v.getBlockY(),wet=0;
        for(int i=0;i<path.getNodeCount();i++){
            var n=path.getNodePos(i);
            if(!bounds.isInside(n)||!level.isLoaded(n)||!level.getWorldBorder().isWithinBounds(n)||n.getY()<y-(critical?1:2))return false;
            if(!level.getFluidState(n).isEmpty()){
                if(!shallowWater(level,n,critical)||!waterBudget(critical,++wet))return false;
            }
            if(critical&&!level.getBlockState(n).is(net.minecraft.tags.BlockTags.WOODEN_DOORS)){
                double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,n);
                var shape=level.getBlockState(n).getCollisionShape(level,n);
                if(!shape.isEmpty())floor=Math.max(floor,n.getY()+shape.max(Direction.Axis.Y));
                var feet=new Vec3(n.getX()+.5,floor,n.getZ()+.5);
                var body=actualStartFooting(i,n,v.blockPosition(),v.getY())?v.getBoundingBox()
                    :v.getBoundingBox().move(feet.subtract(v.position()));
                if(!level.noBlockCollision(v,body)||level.noBlockCollision(v,body.move(0,-.03,0)))return false;
            }
            y=n.getY();
        }
        return true;
    }
    static String work(ServerLevel level,Villager v,Session s,LivingEntity threat,long now){
        var w=s.nativeFoodStorage;
        boolean hungry=s.mind.foodLevel()<=12||local.villagers.mind.MealPolicy.needsRecoveryFood(s.mind.foodLevel(),v.getHealth(),v.getMaxHealth());
        if(NativeRegionsScenario.nativeRegions(level)&&hungry&&s.mind.foodLevel()<=6&&v.isSleeping())v.stopSleeping();
        if(!NativeRegionsScenario.nativeRegions(level)||!hungry||v.isBaby()||v.isSleeping()||v.isTrading()||v.isInWater()||v.isOnFire()||v.isPassenger()
            ||FoodInventory.anySlot(v.getInventory(),AdventureActions.canHarvest(v))>=0||!AdventureActions.PERMISSIONS.allows(local.villagers.capability.CapabilityPermissions.Capability.OPEN_NATIVE_FOOD_STORAGE))return null;
        boolean attacked=v.getLastHurtByMob()!=null&&v.getLastHurtByMob().isAlive()&&v.tickCount-v.getLastHurtByMobTimestamp()<100;
        boolean activelyHunted=threat instanceof net.minecraft.world.entity.Mob hostile&&hostile.getTarget()==v;
        boolean criticalMeal=s.mind.foodLevel()<=4&&v.getHealth()<v.getMaxHealth()*.5&&threat==null&&!attacked;
        boolean mayWalk=(v.getHealth()>=v.getMaxHealth()*.5||criticalMeal)&&!attacked&&(threat==null||!activelyHunted||s.mind.foodLevel()<=4);
        if(w.target!=null&&(now>=w.until||!homeStructure(level,v,s,w.target)||NativeContainers.accessible(level,w.target)==null)){
            w.blocked.put(w.target,now+400);w.target=null;
        }
        if(w.target==null&&now>=w.scanAt){
            w.scanAt=now+100;w.blocked.entrySet().removeIf(e->e.getValue()<=now);var origin=v.blockPosition();double best=Double.MAX_VALUE;boolean bestFloor=false;
            var home=s.mind.home();
            var villageOrigin=home==null?v.blockPosition():new BlockPos(home.x(),home.y(),home.z());
            boolean village=(home==null||home.dimension().equals(level.dimension().identifier().toString()))
                &&level.isLoaded(villageOrigin)&&level.structureManager().getStructureWithPieceAt(villageOrigin,StructureTags.VILLAGE).isValid();
            boolean villageSearch=mayWalk&&village&&(s.mind.foodLevel()<=6
                ||local.villagers.mind.MealPolicy.needsRecoveryFood(s.mind.foodLevel(),v.getHealth(),v.getMaxHealth()));
            int radius=mayWalk?(villageSearch?64:12):3;
            for(var p:BlockPos.betweenClosed(origin.offset(-radius,-6,-radius),origin.offset(radius,6,radius))){
                // Search the actual home building's physical storage locations.
                // Contents remain unread until contact, including behind doors.
                if(w.blocked.containsKey(p)||!level.isLoaded(p)||NativeContainers.accessible(level,p)==null||!homeStructure(level,v,s,p))continue;
                double d=v.getEyePosition().distanceToSqr(Vec3.atCenterOf(p));
                if(!mayWalk&&BlockContact.interactionPoint(level,v,v.getEyePosition(),p,3)==null)continue;
                // A barrel through the ceiling is geometrically close but
                // may require another floor's stairs. Try this floor first.
                boolean floor=Math.abs(p.getY()-origin.getY())<=2;
                if(floor&&!bestFloor||floor==bestFloor&&d<best){best=d;bestFloor=floor;w.target=p.immutable();}
            }
            // A genuine village store may be across the settlement. Keep its
            // unknown contents private until contact, but allow enough time
            // for fair path admission and the real walk there.
            w.until=now+(villageSearch?1200:400);
        }
        if(w.target==null)return null;
        var p=w.target;
        if(BlockContact.interactionPoint(level,v,v.getEyePosition(),p,3)!=null){
            var source=NativeContainers.accessible(level,p);if(source==null)return null;
            // Only this contact reads contents and triggers the saved native loot table.
            for(int i=0;i<source.getContainerSize();i++){
                var item=source.getItem(i);if(!FoodInventory.supported(item)||!v.getInventory().canAddItem(item.copyWithCount(1)))continue;
                int before=item.getCount();String name=item.getItem().toString();var actual=source.removeItem(i,1);
                if(actual.isEmpty())continue;var remaining=v.getInventory().addItem(actual);
                if(!remaining.isEmpty()){
                    if(source.getItem(i).isEmpty())source.setItem(i,remaining);else source.getItem(i).grow(remaining.getCount());return null;
                }
                source.setChanged();w.transfers++;w.target=null;w.routeBlocked=false;w.scanAt=now+20;
                w.receipt=Map.of("gameTime",now,"container",p.toShortString(),"item",name,"sourceBefore",before,"sourceAfter",source.getItem(i).getCount(),"moved",1);
                s.mind.remember("Collected one actual "+name+" from a native home food store at "+p.toShortString());
                level.gameEvent(v,GameEvent.CONTAINER_OPEN,p);v.getNavigation().stop();return w.activity="collected a real stored meal";
            }
            w.blocked.put(p,now+1200);w.target=null;return null;
        }
        if(!mayWalk)return null;
        if(!v.onGround())return w.activity="finishing a native landing before the food-store walk";
        var current=v.getNavigation().getPath();
        if(current!=null&&current==w.walking&&!current.isDone()){
            var bounds=homeBounds(level,v,s);
            if(bounds!=null&&!w.progress.stalledAdvance(p.toShortString(),now,current.getNextNodeIndex())&&safeRoute(level,v,bounds,current,criticalMeal))
                return w.activity="following the native route to my food store";
            v.getNavigation().stop();w.walking=null;w.pathAt=now;
        }
        boolean due=now>=w.pathAt;
        boolean admitted=due&&WorkNavigation.admit(level,now,v);
        boolean nearbyUrgent=due&&s.mind.foodLevel()<=4&&p.distSqr(v.blockPosition())<=16*16;
        if(due&&(admitted||nearbyUrgent)){
            w.pathAt=now+40;var stands=new ArrayList<BlockPos>();
            for(var q:BlockPos.betweenClosed(p.offset(-3,-2,-3),p.offset(3,2,3))){
                if(!level.isLoaded(q.above())||!level.getFluidState(q).isEmpty())continue;
                double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,q);
                var feet=new Vec3(q.getX()+.5,floor,q.getZ()+.5);var body=v.getBoundingBox().move(feet.subtract(v.position()));
                if(level.noBlockCollision(v,body)&&!level.noBlockCollision(v,body.move(0,-.03,0))
                    &&BlockContact.interactionPoint(level,v,feet.add(0,v.getEyeHeight(),0),p,3)!=null)
                    stands.add(BlockPos.containing(feet.x,Math.ceil(feet.y),feet.z));
            }
            stands=new ArrayList<>(new LinkedHashSet<>(stands));
            stands.sort(Comparator.comparingDouble(q->q.distSqr(v.blockPosition())));boolean started=false;
            for(var q:w.nextStands(p,stands)){
                var bounds=homeBounds(level,v,s);if(bounds==null)continue;
                net.minecraft.world.level.pathfinder.Path path;
                try{
                    v.getNavigation().setMaxVisitedNodesMultiplier(admitted?32:1);
                    path=NativePathLimits.search(v,criticalMeal?1:2,()->NativePathLimits.avoiding(v,
                        n->!level.isLoaded(n)||!bounds.isInside(n)||!level.getFluidState(n).isEmpty()&&!shallowWater(level,n,criticalMeal),
                        ()->v.getNavigation().createPath(q,0,admitted?128:16)));
                }finally{v.getNavigation().resetMaxVisitedNodesMultiplier();}
                w.pathReceipt=Map.of("gameTime",now,"store",p.toShortString(),"stand",q.toShortString(),
                    "reaches",path!=null&&path.canReach(),"nodes",path==null?0:path.getNodeCount(),"end",path==null?"":path.getEndNode().asBlockPos().toShortString(),"sharedBudget",admitted);
                if(path==null||path.getNodeCount()>(admitted?128:16)||!safeRoute(level,v,bounds,path,criticalMeal))continue;
                if(v.getNavigation().moveTo(path,.6)){
                    started=true;w.walking=v.getNavigation().getPath();w.progress.reset();w.routeBlocked=false;
                    if(!admitted)WorkNavigation.forget(v.getUUID());break;
                }
            }
            if(!started){
                v.getNavigation().stop();w.routeBlocked=true;w.blocked.put(p,now+(admitted?400:200));w.target=null;
                if(!admitted)w.scanAt=now+20;return null;
            }
        }
        if(w.walking!=null&&v.getNavigation().getPath()==w.walking&&!w.walking.isDone())
            return w.activity="walking to a native home food store";
        w.activity="waiting to plan a native food-store route";return null;
    }
    private NativeFoodStorage(){}
}
