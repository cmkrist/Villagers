package local.villagers;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Loaded, level surface water only. No chunk generation, waterfalls, ocean-floor paths or land shortcuts. */
final class BoatRoutes {
    record Dock(BlockPos land, BlockPos water) {
        Vec3 point() {
            // Leave hull clearance at the bank while remaining in native dismount range.
            return Vec3.atBottomCenterOf(water).add((water.getX() - land.getX()) * .25, .6,
                    (water.getZ() - land.getZ()) * .25);
        }
    }
    record Route(Dock launch, Dock arrival, List<Vec3> points) {}
    private record WaterStep(BlockPos pos,int cost) {}
    static boolean dry(ServerLevel level, BlockPos p) {
        return level.isLoaded(p) && level.isLoaded(p.above()) && level.isLoaded(p.below())
            && level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
            && level.getFluidState(p).isEmpty() && level.getFluidState(p.below()).isEmpty()
            && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)
            && level.getWorldBorder().isWithinBounds(p);
    }
    static boolean surface(ServerLevel level, BlockPos p) {
        return level.isLoaded(p) && level.isLoaded(p.above(2)) && level.getWorldBorder().isWithinBounds(p)
            && level.getFluidState(p).is(FluidTags.WATER) && level.getFluidState(p).isSource()
            && level.getBlockState(p.above()).isAir() && level.getBlockState(p.above(2)).isAir();
    }
    static boolean hullClear(ServerLevel level, Vec3 p) {
        for (double x : new double[]{p.x - .69, p.x + .69}) for (double z : new double[]{p.z - .69, p.z + .69}) {
            var corner = BlockPos.containing(x, p.y, z);
            if (!level.isLoaded(corner) || !level.getWorldBorder().isWithinBounds(corner)) return false;
        }
        // Terrain clearance must not reject the pilot's own boat during a live replan.
        return !level.getBlockCollisions(null, new AABB(p.x - .69, p.y, p.z - .69, p.x + .69, p.y + .57, p.z + .69)).iterator().hasNext();
    }
    static boolean dockValid(ServerLevel level, Dock dock) {
        return dock.land().getY() == dock.water().getY() + 1
            && Math.abs(dock.land().getX() - dock.water().getX()) + Math.abs(dock.land().getZ() - dock.water().getZ()) == 1
            && dry(level, dock.land()) && surface(level, dock.water()) && hullClear(level, dock.point());
    }
    static boolean visible(ServerLevel level, Villager v, Vec3 target) {
        var origin = v.blockPosition(); var end = BlockPos.containing(target);
        for (int x = Math.min(origin.getX() >> 4, end.getX() >> 4); x <= Math.max(origin.getX() >> 4, end.getX() >> 4); x++)
            for (int z = Math.min(origin.getZ() >> 4, end.getZ() >> 4); z <= Math.max(origin.getZ() >> 4, end.getZ() >> 4); z++)
                if (!level.isLoaded(new BlockPos(x << 4, end.getY(), z << 4))) return false;
        var hit = level.clip(new ClipContext(v.getEyePosition(), target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, v));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(end);
    }
    static List<Dock> launches(ServerLevel level, Villager v) {
        var docks = new ArrayList<Dock>(); var origin = v.blockPosition();
        for (BlockPos land : BlockPos.betweenClosed(origin.offset(-12, -3, -12), origin.offset(12, 3, 12))) {
            if (!dry(level, land)) continue;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                var dock = new Dock(land.immutable(), land.relative(d).below());
                if (dockValid(level, dock)) docks.add(dock);
            }
        }
        var owned = level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.boat.AbstractBoat.class,
            v.getBoundingBox().inflate(48), b -> b.isAlive() && !b.isVehicle() && BoatTravel.owned(b, v)
                &&(v.distanceToSqr(b)<=16*16||v.hasLineOfSight(b))).stream()
            .sorted(Comparator.comparingDouble(v::distanceToSqr)).limit(4).toList();
        // A parked owned boat can lie just beyond the local bank scan. Walk to
        // its observed shore instead of repeatedly crafting another boat from
        // an already depleted village edge. Route admission below still needs
        // a complete dry path and a valid native water crossing.
        for(var boat:owned)for(var land:BlockPos.betweenClosed(boat.blockPosition().offset(-3,0,-3),boat.blockPosition().offset(3,2,3))){
            if(!dry(level,land))continue;
            for(var d:Direction.Plane.HORIZONTAL){
                var dock=new Dock(land.immutable(),land.relative(d).below());
                if(dockValid(level,dock)&&BoatTravel.horizontal(boat.position(),dock.point())<=4&&!docks.contains(dock))docks.add(dock);
            }
        }
        docks.sort(Comparator.comparingDouble(d -> owned.isEmpty() ? d.land().distSqr(origin)
            : owned.stream().mapToDouble(b -> BoatTravel.horizontal(b.position(), d.point())).min().orElseThrow() * 100 + d.land().distSqr(origin)));
        return docks;
    }
    static Route find(ServerLevel level, Villager v, BlockPos destination) {
        return find(level,v,destination,p->true);
    }
    static Route find(ServerLevel level,Villager v,BlockPos destination,java.util.function.Predicate<BlockPos> arrivalAllowed){
        int queries = 0;
        for (Dock launch : launches(level, v)) {
            if (++queries > 3) break;
            var path = v.getNavigation().createPath(launch.land(), 0,64);
            if (path == null || !path.canReach() || path.getNodeCount() > 64) continue;
            if(!BoatTravel.dryPath(level,path))continue;
            Route route = across(level, v, launch, destination,arrivalAllowed);
            if (route != null) return route;
        }
        return null;
    }
    /** At most8192 water cells in a96-block radius, four-connected to preserve corner clearance. */
    static Route across(ServerLevel level, Villager v, Dock launch, BlockPos destination) {
        return across(level,v,launch,destination,p->true);
    }
    private static Route across(ServerLevel level,Villager v,Dock launch,BlockPos destination,java.util.function.Predicate<BlockPos> arrivalAllowed){
        if (!dockValid(level, launch)) return null;
        // A known return bank needs a directed search. A breadth-first ocean
        // flood can spend all8192 cells beside the launch before reaching home.
        Queue<WaterStep> queue=destination==null?new ArrayDeque<>():new PriorityQueue<>(Comparator
            .comparingInt((WaterStep step)->step.cost()+Math.abs(step.pos().getX()-destination.getX())+Math.abs(step.pos().getZ()-destination.getZ()))
            .thenComparingInt(WaterStep::cost));
        var previous = new HashMap<BlockPos, BlockPos>();var costs=new HashMap<BlockPos,Integer>();
        queue.add(new WaterStep(launch.water(),0)); previous.put(launch.water(), launch.water());costs.put(launch.water(),0);
        Dock best = null; BlockPos end = null; double bestScore = Double.MAX_VALUE;
        search: while (!queue.isEmpty() && previous.size() < 8192) {
            var step=queue.remove();var p=step.pos();if(step.cost()!=costs.get(p))continue;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                var land = p.relative(d).above(); var dock = new Dock(land, p);
                double distance = land.distSqr(launch.land());
                if (distance >= 144 && dockValid(level, dock)&&arrivalAllowed.test(land)
                        && (destination != null || visible(level, v, Vec3.atBottomCenterOf(land).add(0, 1, 0)))) {
                    double score = destination == null ? -Math.min(distance, 4096) : land.distSqr(destination);
                    if (score < bestScore && (destination == null || score <= 144)) {
                        best = dock; end = p; bestScore = score;
                        if(destination!=null&&score==0)break search;
                    }
                }
                var next = p.relative(d);
                int cost=step.cost()+1;
                if (cost>=costs.getOrDefault(next,Integer.MAX_VALUE) || next.distSqr(launch.water()) > 96 * 96 || !surface(level, next)) continue;
                // A docking cell can be inset from a bank; ordinary route cells must fit at their centers.
                var point = waterPoint(level, next);
                if (point == null || !segmentClear(level, p.equals(launch.water()) ? launch.point() : waterPoint(level, p), point)) continue;
                previous.put(next, p);costs.put(next,cost);queue.add(new WaterStep(next,cost));
            }
        }
        if (best == null) return null;
        var reversed = new ArrayList<Vec3>(); var cursor = end;
        while (!cursor.equals(launch.water())) {
            reversed.add(cursor.equals(end) ? best.point() : waterPoint(level, cursor));
            cursor = previous.get(cursor);
        }
        reversed.add(launch.point()); Collections.reverse(reversed);
        // Only retain straight-line interior nodes; preserve bends and the two inshore points.
        var points = new ArrayList<Vec3>();
        for (int i = 0; i < reversed.size(); i++) {
            if (i > 1 && i + 2 < reversed.size()) {
                var a = reversed.get(i).subtract(reversed.get(i - 1)); var b = reversed.get(i + 1).subtract(reversed.get(i));
                if (a.x == b.x && a.z == b.z) continue;
            }
            points.add(reversed.get(i));
        }
        return new Route(launch, best, List.copyOf(points));
    }
    private static Vec3 waterPoint(ServerLevel level, BlockPos p) {
        var center = Vec3.atBottomCenterOf(p).add(0, .6, 0);
        if (hullClear(level, center)) return center;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            var dock = new Dock(p.relative(d).above(), p);
            if (dockValid(level, dock)) return dock.point();
        }
        return null;
    }
    static boolean segmentClear(ServerLevel level, Vec3 a, Vec3 b) {
        if (a == null || b == null) return false;
        for (int i = 1; i <= 4; i++) if (!hullClear(level, a.lerp(b, i / 4.0))) return false;
        return true;
    }
    private BoatRoutes() {}
}
