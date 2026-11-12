package local.villagers;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

/** Native routes around nearby living bodies, with the same movement and collision physics. */
public final class NativeBodyRoutes {
    static final class State {
        final Map<net.minecraft.core.BlockPos,Long> failedSteps=new HashMap<>();
        final ArrayDeque<Map<String,Object>> waypoints=new ArrayDeque<>();

        int searches,blockedNodes;Map<String,Object> receipt=Map.of();
        Map<String,Object> status(){return Map.of("searches",searches,"blockedNodes",blockedNodes,"lastReceipt",receipt,"waypoints",List.copyOf(waypoints),"failedSteps",failedSteps.keySet().stream().map(net.minecraft.core.BlockPos::toShortString).sorted().toList());}
    }
    private static final Map<Mob,State> STATES=new WeakHashMap<>();
    private static final ThreadLocal<Mob> ACTIVE=new ThreadLocal<>();
    private record EdgeScope(ServerLevel level,Mob actor,List<AABB> bodies){}
    private static final ThreadLocal<EdgeScope> EDGES=new ThreadLocal<>();
    /** Hypothetical path edges start at their own node, not at the actor's
     * original position. The first edge retains the real off-center body. */
    static AABB edgeStart(AABB actual,Vec3 actualFeet,net.minecraft.core.BlockPos start,net.minecraft.core.BlockPos parent,double floor){
        return parent.getX()==start.getX()&&parent.getZ()==start.getZ()&&Math.abs(floor-actualFeet.y)<.01
            ?actual:actual.move(parent.getX()+.5-actualFeet.x,floor-actualFeet.y,parent.getZ()+.5-actualFeet.z);
    }
    /** Native nodes inside carpet or another low surface stand on its top.
     * Vertical door/fence shapes are not walking floors. */
    static double partialFloor(double nativeFloor,int nodeY,double ownTop){
        return ownTop>0&&ownTop<1?Math.max(nativeFloor,nodeY+ownTop):nativeFloor;
    }
    private static double nodeFloor(ServerLevel level,net.minecraft.core.BlockPos node){
        var shape=level.getBlockState(node).getCollisionShape(level,node);
        return partialFloor(net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,node),node.getY(),
            shape.isEmpty()?0:shape.max(net.minecraft.core.Direction.Axis.Y));
    }
    public static boolean blockedEdge(Mob mob,net.minecraft.world.level.pathfinder.Node parent,net.minecraft.world.level.pathfinder.Node next){
        var scope=EDGES.get();boolean bodySearch=scope!=null&&scope.actor()==mob;
        // A native start node may be inside a closed door without intersecting
        // its thin plane. Path types alone then accept a first step through
        // that plane. Scoped work searches must validate the actual departure
        // collision even when they are not avoiding other living bodies.
        if(!bodySearch&&(!NativePathLimits.mustReplan(mob)||!(mob.level() instanceof ServerLevel)
            ||!NativeRegionsScenario.nativeRegions((ServerLevel)mob.level())))return false;
        var level=bodySearch?scope.level():(ServerLevel)mob.level();var from=parent.asBlockPos();var to=next.asBlockPos();
        double before=nodeFloor(level,from);
        double after=nodeFloor(level,to);
        var origin=edgeStart(mob.getBoundingBox(),mob.position(),mob.blockPosition(),from,before);
        var target=mob.getBoundingBox().move(new Vec3(to.getX()+.5,after,to.getZ()+.5).subtract(mob.position()));
        if(bodySearch&&crossesBody(origin,target,scope.bodies()))return true;
        // Validate the local transition only near the current body, matching
        // the prior clearance limit. Subsequent bends remain ordinary edges.
        if(Vec3.atBottomCenterOf(from).subtract(mob.position()).horizontalDistanceSqr()>2.25
            ||Math.abs(before-mob.getY())>1.01||Math.abs(after-before)>1.01)return false;
        boolean door=mob.getNavigation().getNodeEvaluator().canOpenDoors()
            &&(level.getBlockState(from).is(net.minecraft.tags.BlockTags.WOODEN_DOORS)||level.getBlockState(to).is(net.minecraft.tags.BlockTags.WOODEN_DOORS));
        if(door)return false; // Native door handling still operates at physical reach.
        if(after>before+.001){
            var raised=origin.move(0,after-before,0);
            return !level.noBlockCollision(mob,origin.minmax(raised).deflate(1e-6))
                ||!level.noBlockCollision(mob,raised.minmax(target).deflate(1e-6));
        }
        var horizontal=origin.move(target.getCenter().x-origin.getCenter().x,0,target.getCenter().z-origin.getCenter().z);
        return !level.noBlockCollision(mob,origin.minmax(horizontal).deflate(1e-6));
    }
    static State state(Mob mob){return STATES.computeIfAbsent(mob,m->new State());}
    public static void observeWaypoint(Mob mob,net.minecraft.world.level.pathfinder.Path path){
        if(path==null||path.isDone()||!(mob.level() instanceof ServerLevel level)
            ||!NativeRegionsScenario.nativeRegions(level)||!NativeIllagers.member(mob))return;
        var at=path.getNextNodePos();
        double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,at);
        var shape=level.getBlockState(at).getCollisionShape(level,at);
        if(!shape.isEmpty())floor=Math.max(floor,at.getY()+shape.max(net.minecraft.core.Direction.Axis.Y));
        var w=state(mob);long now=level.getGameTime();
        w.waypoints.addLast(Map.of("gameTime",now,"position",mob.position().toString(),"node",at.toShortString(),
            "floor",floor,"grounded",mob.onGround()));
        while(w.waypoints.size()>24)w.waypoints.removeFirst();
    }
    static void failedStep(Mob mob,net.minecraft.core.BlockPos at,long now){
        state(mob).failedSteps.put(at.immutable(),now+400);
    }
    static <T> T search(ServerLevel level,Mob mob,Supplier<T> planner){
        return search(level,mob,state(mob),level.getGameTime(),planner);
    }
    static boolean clearBodyWalk(ServerLevel level,Mob mob,AABB destination){
        var bodies=level.getEntities(mob,mob.getBoundingBox().minmax(destination),e->e instanceof LivingEntity&&e.isAlive()&&!e.isPassengerOfSameVehicle(mob));
        return !crossesBody(mob.getBoundingBox(),destination,bodies.stream().map(e->e.getBoundingBox()).toList());
    }
    static boolean crossesBody(AABB from,AABB to,List<AABB> neighbors){
        var sweep=from.minmax(to);
        return neighbors.stream().anyMatch(b->b.intersects(to)||!b.intersects(from)&&b.intersects(sweep));
    }
    static <T> T search(ServerLevel level,Mob mob,State w,long now,Supplier<T> planner){
        if(ACTIVE.get()==mob||!NativeRegionsScenario.nativeRegions(level)||!NativeIllagers.member(mob))return planner.get();
        var bodies=level.getEntities(mob,mob.getBoundingBox().inflate(8),e->e instanceof LivingEntity&&e.isAlive()
            &&!e.isPassengerOfSameVehicle(mob));
        w.failedSteps.values().removeIf(until->now>=until);
        if(bodies.isEmpty()&&w.failedSteps.isEmpty())return planner.get();
        // A scoped native cache bypass forces a fresh search without stopping
        // an existing walk when this helper is used only for inspection.
        var start=mob.blockPosition();var bounds=bodies.stream().map(e->e.getBoundingBox()).toList();w.searches++;
        var previous=ACTIVE.get();var previousEdges=EDGES.get();ACTIVE.set(mob);EDGES.set(new EdgeScope(level,mob,bounds));T result;
        try{result=NativePathLimits.avoiding(mob,p->{
            if(p.getX()==start.getX()&&p.getZ()==start.getZ()&&Math.abs(p.getY()-mob.getY())<=1)return false;
            // A stair's native start can be one grid level above its actual feet.
            double floor=nodeFloor(level,p);
            var feet=new Vec3(p.getX()+.5,floor,p.getZ()+.5);
            var body=mob.getBoundingBox().move(feet.subtract(mob.position()));
            boolean blocked=w.failedSteps.containsKey(p)||bounds.stream().anyMatch(b->b.inflate(.05).intersects(body));
            if(blocked)w.blockedNodes++;return blocked;
        },planner);}finally{
            if(previous==null)ACTIVE.remove();else ACTIVE.set(previous);
            if(previousEdges==null)EDGES.remove();else EDGES.set(previousEdges);
        }
        w.receipt=Map.of("gameTime",now,"from",mob.position().toString(),"bodies",bodies.stream().map(e->e.getUUID().toString()).toList(),
            "action","native route search avoids the actual neighboring living bodies");
        return result;
    }
    private NativeBodyRoutes(){}
}
