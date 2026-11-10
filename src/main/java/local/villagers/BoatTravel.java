package local.villagers;

import java.util.*;
import local.villagers.mind.AgentMind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Finite food expeditions. Real boats, native paddle input, native riding and native dry-bank dismounts. */
public final class BoatTravel {
    private static final String TRIP = "villagers:sea:", OWNER = "villagers:boat_owner:";
    enum Phase { PREPARE, SAIL, ASHORE, RETURN }
    static final class Trip {
        final BoatRoutes.Dock home, away;
        BoatRoutes.Route route;
        Phase phase = Phase.PREPARE;
        boolean harvest;
        int waypoint;
        long started, ashoreUntil, lastProgress;
        Vec3 progress;
        Trip(BoatRoutes.Route route, long now) { this.home = route.launch(); this.away = route.arrival(); this.route = route; started = now; lastProgress = now; }
        Trip(BoatRoutes.Dock home, BoatRoutes.Dock away, Phase phase, long now) {
            this.home = home; this.away = away; this.phase = phase; started = now; lastProgress = now; ashoreUntil = now + 1200;
        }
    }
    static boolean owned(AbstractBoat boat, Villager v) { return boat.entityTags().contains(OWNER + v.getUUID()); }
    public static LivingEntity pilot(AbstractBoat boat) {
        if (!(boat.level() instanceof ServerLevel) || !(boat.getFirstPassenger() instanceof Villager v)
                || !v.isAlive() || !Villagers.isManaged(v) || !owned(boat, v)
                || !AdventureActions.PERMISSIONS.allows(local.villagers.capability.CapabilityPermissions.Capability.PILOT_BOAT)
                || boat.getPassengers().stream().anyMatch(p -> p instanceof net.minecraft.world.entity.player.Player)) return null;
        return v;
    }
    /** Called immediately after native floatBoat and immediately before native move. Never sets position/velocity. */
    public static boolean steer(AbstractBoat boat) {
        if (!(pilot(boat) instanceof Villager v)) return false;
        var s = Villagers.existingSession(v.getUUID());
        if (s == null || s.boatTrip == null || s.boatTrip.route == null || boat.isUnderWater()) {
            boat.setInput(false, false, false, false); return false;
        }
        var trip = s.boatTrip;
        if (trip.phase != Phase.SAIL && trip.phase != Phase.RETURN) { boat.setInput(false, false, false, false); return false; }
        var level = (ServerLevel) boat.level(); var points = trip.route.points();
        while (trip.waypoint + 1 < points.size() && horizontal(boat.position(), points.get(trip.waypoint)) < .65 * .65) trip.waypoint++;
        var target = points.get(trip.waypoint);
        if (!BoatRoutes.surface(level, BlockPos.containing(target.x, trip.route.launch().water().getY(), target.z))
                || !BoatRoutes.hullClear(level, target)) { boat.setInput(false, false, false, false); return false; }
        double dx = target.x - boat.getX(), dz = target.z - boat.getZ();
        float yaw = (float) (Math.atan2(-dx, dz) * 180 / Math.PI);
        float error = Mth.wrapDegrees(yaw - boat.getYRot());
        double distance = Math.sqrt(dx * dx + dz * dz);
        boolean finalPoint = trip.waypoint == points.size() - 1;
        double speed = boat.getDeltaMovement().horizontalDistance();
        // Brake early at turns/banks rather than overrunning a one-cell waypoint at full ocean speed.
        double desired = Math.min(finalPoint ? .16 : .27, distance * .2);
        boolean forward = Math.abs(error) < 20 && speed < desired && (!finalPoint || distance > .22);
        boolean brake = speed > desired + .025 && Math.abs(error) < 60;
        boat.setInput(error < -4, error > 4, forward, brake);
        s.boatPaddleTicks++;
        return true;
    }
    static double horizontal(Vec3 a, Vec3 b) { double x = a.x - b.x, z = a.z - b.z; return x * x + z * z; }
    static Map<String, Object> status(Session s) {
        return Map.of("activity", s.boatStatus, "phase", s.boatTrip == null ? "NONE" : s.boatTrip.phase.name(),
            "recipes", s.boatRecipes, "logsGathered", s.boatLogsGathered, "launches", s.boatLaunches,
            "landings", s.boatLandings, "paddleTicks", s.boatPaddleTicks);
    }
    /** A trip grants only a16-block food-work area at its actual landing, never unrestricted home range. */
    static boolean withinTrip(ServerLevel level, AgentMind mind, Vec3 p) {
        var s = Villagers.existingSession(mind.id());
        return s != null && s.boatTrip != null && s.boatTrip.phase == Phase.ASHORE
            && level.isLoaded(s.boatTrip.away.land()) && Vec3.atBottomCenterOf(s.boatTrip.away.land()).distanceToSqr(p) <= 16 * 16;
    }
    static boolean ashore(Villager v) {
        var s = Villagers.existingSession(v.getUUID());
        return s != null && s.boatTrip != null && s.boatTrip.phase == Phase.ASHORE;
    }
    static String tick(ServerLevel level, Villager v, Session s, LivingEntity threat, long now) {
        if (s.boatTrip == null) restore(v, s, now);
        if (s.boatTrip == null) {
            if (now < s.nextBoatSearch || threat != null || v.isOnFire() || v.isPassenger() || v.isBaby()
                    || v.isSleeping() || v.isTrading() || v.isInWater() || s.courtship != null
                    || !FoodSupply.mayWork(v.getAge(), v.getHealth(), s.mind.fatigue(), !VillageSurvival.scheduledRest(level, v))
                    || s.mind.foodLevel() <= 6 || FoodSupply.deficit(v.getInventory(), AdventureActions.canHarvest(v)) < 10) return null;
            s.nextBoatSearch = now + 400;
            if (!WorkNavigation.admit(level, now, v)) { s.nextBoatSearch = now + 20; return null; }
            BlockPos goal = visibleFood(level, v);
            // Existing nearby food comes first; a boat is a route fallback, not a competing food job.
            if (goal != null) {
                var path = v.getNavigation().createPath(goal, 1);
                if (dryPath(level, path)) return null;
            }
            var route = BoatRoutes.find(level, v, goal);
            if (route == null) { s.boatStatus = "no observed safe water crossing"; return null; }
            s.boatTrip = new Trip(route, now);
            // An exploratory shore can reveal crops only after landing: carry the tool before sailing.
            s.boatTrip.harvest = goal == null || level.getBlockState(goal).getBlock() instanceof CropBlock;
            save(v, s.boatTrip);
            s.mind.remember("Planned a food expedition by boat to " + route.arrival().land().toShortString());
        }
        var trip = s.boatTrip;
        if (!v.isAlive() || v.isBaby() || v.isTrading()) { stop(v, s, now, "boat trip interrupted"); return null; }
        if (!v.isPassenger() && v.isInWater()) { stop(v, s, now, "seeking dry ground after leaving the boat"); return null; }
        if (trip.phase == Phase.ASHORE) {
            if (now < trip.ashoreUntil && !VillageSurvival.scheduledRest(level, v) && threat == null && !v.isOnFire()
                    && FoodSupply.deficit(v.getInventory(), AdventureActions.canHarvest(v)) > 0) return null;
            trip.phase = Phase.RETURN; trip.route = null; trip.started = now; save(v, trip);
        }
        boolean returning = trip.phase == Phase.RETURN;
        if (!(v.getVehicle() instanceof AbstractBoat) && (threat != null || v.isOnFire() || v.isSleeping())) {
            if (!returning) stop(v, s, now, "boat preparation yielded to safety");
            return null;
        }
        if (trip.route == null) {
            trip.route = BoatRoutes.across(level, v, returning ? trip.away : trip.home, returning ? trip.home.land() : trip.away.land());
            if (trip.route == null) { s.boatStatus = "waiting for a safe loaded return crossing"; return v.isPassenger() ? s.boatStatus : null; }
            trip.waypoint = 0;
            if (v.getVehicle() instanceof AbstractBoat boat) {
                trip.waypoint = forwardWaypoint(trip.route.points(), boat.position());
            }
        }
        if (v.getVehicle() instanceof AbstractBoat boat) {
            if (!owned(boat, v) || boat.getFirstPassenger() != v) return null;
            if (trip.phase == Phase.PREPARE) { trip.phase = Phase.SAIL; save(v, trip); }
            v.getNavigation().stop();
            if (land(level, v, s, boat, now)) return s.boatStatus;
            if (trip.progress == null || horizontal(boat.position(), trip.progress) > .25) { trip.progress = boat.position(); trip.lastProgress = now; }
            if (now - trip.lastProgress > 160 || now - trip.started > 2400) {
                // Stay afloat and stop paddling on an invalid/stuck route. Never teleport out of a failed crossing.
                trip.route = null; boat.setInput(false, false, false, false); s.boatStatus = "replanning a blocked boat crossing";
                trip.lastProgress = now; trip.started = now; return s.boatStatus;
            }
            return s.boatStatus = returning ? "rowing food supplies home" : "rowing toward the far shore";
        }
        if (!returning && now - trip.started > 1200) { stop(v, s, now, "boat preparation timed out"); return null; }
        var dock = trip.route.launch();
        AbstractBoat existing = level.getEntitiesOfClass(AbstractBoat.class, new net.minecraft.world.phys.AABB(dock.water()).inflate(4),
            b -> b.isAlive() && owned(b, v) && !b.isVehicle()).stream().min(Comparator.comparingDouble(v::distanceToSqr)).orElse(null);
        if ((existing == null && BoatPreparation.boatSlot(v) < 0) || (!returning && trip.harvest && !AdventureActions.hasHoe(v))) {
            if (returning) return s.boatStatus = "return boat unavailable";
            return BoatPreparation.work(level, v, s, now);
        }
        if (v.position().distanceToSqr(Vec3.atBottomCenterOf(dock.land())) > 1.2 * 1.2) {
            if (now >= s.nextBoatPath && WorkNavigation.admit(level, now, v)) {
                s.nextBoatPath = now + 40;
                var path = v.getNavigation().createPath(dock.land(), 0);
                if (path != null && path.canReach()) v.getNavigation().moveTo(path, .6);
            }
            return s.boatStatus = "walking to the boat launch";
        }
        v.getNavigation().stop();
        if (!BoatRoutes.dockValid(level, dock)) { stop(v, s, now, "boat launch changed"); return null; }
        if (existing == null) existing = deploy(level, v, s, dock);
        if (existing != null && v.distanceToSqr(existing) < 9 && v.startRiding(existing)) {
            trip.phase = returning ? Phase.RETURN : Phase.SAIL; trip.started = now; trip.lastProgress = now;
            save(v, trip); s.boatStatus = "boarded a real boat";
        }
        return s.boatStatus;
    }
    private static AbstractBoat deploy(ServerLevel level, Villager v, Session s, BoatRoutes.Dock dock) {
        int slot = BoatPreparation.boatSlot(v); if (slot < 0) return null;
        var stack = v.getInventory().getItem(slot); var type = BoatMaterials.type(stack);
        var boat = type.entity().create(level, EntitySpawnReason.SPAWN_ITEM_USE); if (boat == null) return null;
        var p = dock.point(); boat.setInitialPos(p.x, p.y, p.z);
        boat.addTag(OWNER + v.getUUID());
        var next = s.boatTrip.route.points().get(Math.min(1, s.boatTrip.route.points().size() - 1));
        boat.setYRot((float) (Math.atan2(p.x - next.x, next.z - p.z) * 180 / Math.PI));
        if (!level.noCollision(boat, boat.getBoundingBox()) || !level.addFreshEntity(boat)) return null;
        // Consumption happens only after the actual body has entered the world.
        v.getInventory().removeItem(slot, 1); s.boatLaunches++; return boat;
    }
    private static boolean land(ServerLevel level, Villager v, Session s, AbstractBoat boat, long now) {
        var trip = s.boatTrip; var dock = trip.route.arrival();
        if (horizontal(boat.position(), dock.point()) > .5 * .5 || boat.getDeltaMovement().horizontalDistance() > .09
                || !BoatRoutes.dockValid(level, dock)) return false;
        var p = Vec3.atBottomCenterOf(dock.land());
        v.setYRot((float) (Math.atan2(boat.getX() - p.x, p.z - boat.getZ()) * 180 / Math.PI));
        var nativeExit = boat.getDismountLocationForPassenger(v);
        if (!BoatRoutes.dry(level, BlockPos.containing(nativeExit)) || nativeExit.distanceToSqr(p) > 2) return false;
        boat.setInput(false, false, false, false); v.stopRiding(); s.boatLandings++;
        if (trip.phase == Phase.RETURN) stop(v, s, now, "landed home with the real boat retained at shore");
        else {
            trip.phase = Phase.ASHORE; trip.ashoreUntil = now + 1200; trip.route = null; save(v, trip);
            s.boatStatus = "landed to look for food";
            s.nextCropScan = now; s.nextRootCropScan = now; s.nextFamilyProvision = now;
            s.mind.remember("Crossed water and landed at " + dock.land().toShortString());
        }
        return true;
    }
    private static BlockPos visibleFood(ServerLevel level, Villager v) {
        var items = level.getEntitiesOfClass(ItemEntity.class, v.getBoundingBox().inflate(48),
            i -> i.isAlive() && FoodInventory.supported(i.getItem()) && v.hasLineOfSight(i));
        var nearest = items.stream().min(Comparator.comparingDouble(v::distanceToSqr)).orElse(null);
        if (nearest != null) return nearest.blockPosition();
        var origin = v.blockPosition(); BlockPos best = null; double score = Double.MAX_VALUE;
        for (int x = -48; x <= 48; x++) for (int z = -48; z <= 48; z++) {
            if (x * x + z * z > 48 * 48 || !level.isLoaded(origin.offset(x, 0, z))) continue;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, origin.getX() + x, origin.getZ() + z) - 1;
            var p = new BlockPos(origin.getX() + x, y, origin.getZ() + z); var state = level.getBlockState(p);
            if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state) || ConstructionSites.isProtected(level, p)) continue;
            double distance = p.distSqr(origin);
            if (distance < score && BoatRoutes.visible(level, v, Vec3.atCenterOf(p).add(0, .5, 0))) { best = p; score = distance; }
        }
        return best;
    }
    private static boolean dryPath(ServerLevel level, net.minecraft.world.level.pathfinder.Path path) {
        if (path == null || !path.canReach()) return false;
        for (int i = 0; i < path.getNodeCount(); i++)
            if (!level.getFluidState(path.getNodePos(i)).isEmpty() || !level.getFluidState(path.getNodePos(i).below()).isEmpty()) return false;
        return true;
    }
    /** Resume on the nearest segment, heading toward its end even when a compressed start node is closer. */
    static int forwardWaypoint(List<Vec3> points, Vec3 position) {
        int index = 0; double distance = Double.MAX_VALUE;
        for (int i = 1; i < points.size(); i++) {
            var a = points.get(i - 1); var b = points.get(i); var segment = b.subtract(a);
            double length = horizontal(a, b);
            double t = length == 0 ? 1 : Mth.clamp(((position.x - a.x) * segment.x + (position.z - a.z) * segment.z) / length, 0, 1);
            double candidate = horizontal(position, a.lerp(b, t));
            if (candidate < distance) { distance = candidate; index = i; }
        }
        return index;
    }
    private static void stop(Villager v, Session s, long now, String reason) {
        if (v.getVehicle() instanceof AbstractBoat boat && owned(boat, v)) boat.setInput(false, false, false, false);
        s.boatTrip = null; s.boatTree = null; s.boatLogWork = 0; s.nextBoatSearch = now + 600; s.boatStatus = reason;
        v.entityTags().stream().filter(t -> t.startsWith(TRIP)).toList().forEach(v::removeTag);
    }
    private static String coords(BoatRoutes.Dock d) {
        return d.land().getX() + "," + d.land().getY() + "," + d.land().getZ() + "," + d.water().getX() + "," + d.water().getZ();
    }
    private static BoatRoutes.Dock dock(String value) {
        var n = value.split(","); if (n.length != 5) throw new IllegalArgumentException();
        return new BoatRoutes.Dock(new BlockPos(Integer.parseInt(n[0]), Integer.parseInt(n[1]), Integer.parseInt(n[2])),
            new BlockPos(Integer.parseInt(n[3]), Integer.parseInt(n[1]) - 1, Integer.parseInt(n[4])));
    }
    private static void save(Villager v, Trip trip) {
        v.entityTags().stream().filter(t -> t.startsWith(TRIP)).toList().forEach(v::removeTag);
        v.addTag(TRIP + trip.phase.name() + ":" + coords(trip.home) + ":" + coords(trip.away) + ":" + trip.harvest);
    }
    private static void restore(Villager v, Session s, long now) {
        for (String tag : v.entityTags()) if (tag.startsWith(TRIP)) {
            try {
                var parts = tag.substring(TRIP.length()).split(":"); if (parts.length != 3 && parts.length != 4) return;
                var trip = new Trip(dock(parts[1]), dock(parts[2]), Phase.valueOf(parts[0]), now);
                trip.harvest = trip.phase == Phase.PREPARE || parts.length == 4 && Boolean.parseBoolean(parts[3]);
                if (trip.home.land().distSqr(trip.away.land()) <= 128 * 128) s.boatTrip = trip;
            } catch (RuntimeException invalid) { s.boatStatus = "invalid saved boat trip"; }
            return;
        }
    }
    private BoatTravel() {}
}
