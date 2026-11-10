package local.villagers;

import local.villagers.mind.AgentMind;
import local.villagers.mind.Home;
import local.villagers.navigation.RouteProgress;
import local.villagers.survival.SurvivalPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Budgeted, authored refuge seeking. Golems keep their own vanilla target selection and combat. */
public final class VillageSurvival {
    private VillageSurvival() {}
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static class State {
        SurvivalPolicy.Intent intent = SurvivalPolicy.Intent.NORMAL;
        long nextPlan, restAt, rejectedPaths;
        boolean danger, dusk, lastCovered;
        long plans, homePathPlans, partialHomeRoutes;
        Map<String,Object> lastRefugePlan = Map.of();
        Map<String,Object> lastFloorRoute = Map.of();
        BlockPos refugeStand;
        BlockPos floorSearchStart;
        long nextFloorSearch;
        long floorRoutes, refugePathQueries, refugeStandInspections;
        BlockPos lastBed, goal;
        final RouteProgress progress = new RouteProgress(120);
        SurvivalPolicy.Intent blockedIntent = SurvivalPolicy.Intent.NORMAL;
        long blockedUntil, recoveryUntil, routeStalls;
        /** The last route was issued at running speed; the controller charges sprint exhaustion for it. */
        boolean running;
        final FloorCentering centering = new FloorCentering();
    }
    /** Running speed. Like a sprinting player, a villager with 6 or fewer food points cannot run. */
    static final double RUN = 1.1, HURRY = .7;
    public static void forget(UUID id) { STATES.remove(id); }
    /** True while this layer moves the villager at running speed. */
    public static boolean running(UUID id) {
        State s = STATES.get(id);
        return s != null && s.running && s.intent != SurvivalPolicy.Intent.NORMAL;
    }
    private static double run(State s, AgentMind mind) {
        s.running = mind.canSprint();
        return s.running ? RUN : HURRY;
    }
    public static void clear() { STATES.clear(); }
    public static Map<String,Object> status(UUID id) {
        State s = STATES.get(id);
        if (s == null) return Map.of("intent","NORMAL");
        return Map.ofEntries(Map.entry("intent",s.intent.name()),Map.entry("rejectedPaths",s.rejectedPaths),Map.entry("routeStalls",s.routeStalls),Map.entry("recoveryUntil",s.recoveryUntil),
                Map.entry("plans",s.plans),Map.entry("homePathPlans",s.homePathPlans),Map.entry("partialHomeRoutes",s.partialHomeRoutes),
                Map.entry("centeringAttempts",s.centering.attempts),Map.entry("centeringSuccesses",s.centering.successes),
                Map.entry("centeringFailures",s.centering.failures),Map.entry("centeringActive",s.centering.active()),
                Map.entry("lastRefugePlan",s.lastRefugePlan),Map.entry("lastFloorRoute",s.lastFloorRoute),
                Map.entry("floorRoutes",s.floorRoutes),Map.entry("refugePathQueries",s.refugePathQueries),
                Map.entry("refugeStandInspections",s.refugeStandInspections));
    }
    public static boolean withinHomeRange(ServerLevel level, AgentMind mind, Vec3 pos) {
        Home home = mind.home();
        return NativeVillageWorkArea.contains(level,mind,pos)||FoodAccess.within(level,mind,pos)||BoatTravel.withinTrip(level, mind, pos) || home == null || !home.dimension().equals(level.dimension().identifier().toString())
            || SurvivalPolicy.withinHomeRange(Vec3.atCenterOf(new BlockPos(home.x(),home.y(),home.z())).distanceTo(pos));
    }

    /** Returns a grounded action label when this layer owns movement, otherwise null. */
    static boolean scheduledRest(ServerLevel level,Villager v) {
        return level.environmentAttributes().getValue(v.isBaby()?EnvironmentAttributes.BABY_VILLAGER_ACTIVITY:EnvironmentAttributes.VILLAGER_ACTIVITY,v.position())==Activity.REST;
    }
    public static String tick(ServerLevel level, Villager v, AgentMind mind, LivingEntity threat, long now) {
        State s = STATES.computeIfAbsent(v.getUUID(),id -> new State());
        boolean danger = threat != null || v.isOnFire();
        Home home = mind.home();
        if (home == null) home = TraderRecruitment.anchor(v);
        BlockPos bed = home != null && home.dimension().equals(level.dimension().identifier().toString())
            ? new BlockPos(home.x(),home.y(),home.z()) : null;
        boolean covered = bed != null && level.isLoaded(bed) && level.getBlockState(bed).getBlock() instanceof BedBlock
            && !level.canSeeSky(bed.above());
        double distance = bed == null ? 0 : v.position().distanceTo(Vec3.atCenterOf(bed));
        boolean inside = insideHome(level,v,bed,covered,distance);
        if (!Objects.equals(s.lastBed,bed) || s.lastCovered != covered) {
            s.nextPlan = now; s.lastBed = bed; s.lastCovered = covered; s.blockedUntil=0; s.progress.reset();
            s.refugeStand=null;
            s.floorSearchStart=null;s.nextFloorSearch=0;
        }
        // Sky darkening includes rain: the native rest schedule keeps rainy days available for food work.
        Activity nativeActivity=level.environmentAttributes().getValue(v.isBaby()
                ? EnvironmentAttributes.BABY_VILLAGER_ACTIVITY : EnvironmentAttributes.VILLAGER_ACTIVITY,v.position());
        boolean dusk = nativeActivity==Activity.REST;
        if (dusk != s.dusk) { s.dusk = dusk; s.nextPlan = now; }
        String centering = s.centering.tick(level,v,danger,now);
        if (centering != null) { s.running = false; return centering; }
        boolean motionExpected = s.intent == SurvivalPolicy.Intent.SHELTER && !inside
            || s.intent == SurvivalPolicy.Intent.SEEK_GOLEM && s.goal != null && v.position().distanceToSqr(Vec3.atCenterOf(s.goal)) > 9;
        if (motionExpected && s.progress.stalled(s.intent.name()+":"+s.lastBed,now,v.getX(),v.getY(),v.getZ())) {
            s.routeStalls++; s.rejectedPaths++; s.blockedIntent=s.intent; s.blockedUntil=now+200;
            s.intent=SurvivalPolicy.Intent.NORMAL; s.progress.reset(); s.nextPlan=now;
            v.getNavigation().stop(); mind.remember("Refuge route stalled; trying another reachable route");
            if (!danger) {
                Vec3 step=DefaultRandomPos.getPos(v,4,2);
                Path recovery=step == null ? null : v.getNavigation().createPath(BlockPos.containing(step),0);
                if (recovery != null && recovery.canReach()) { v.getNavigation().moveTo(recovery,.6); s.recoveryUntil=now+40; }
            }
        } else if (!motionExpected) s.progress.reset();
        if (now < s.recoveryUntil && !danger) return "recovering route";
        if (danger) s.recoveryUntil=0;
        if (!danger && !dusk && (distance <= 28 || NativeVillageWorkArea.contains(level,mind,v.position()) || SoilWork.authorizedTrip(level,v,now) || BoatTravel.ashore(v)||FoodAccess.active(v))) {
            if (s.intent != SurvivalPolicy.Intent.NORMAL) v.getNavigation().stop();
            // Native sleep survives a load; this session's intent starts NORMAL and cannot be its wake-up gate.
            if (v.isSleeping()) v.stopSleeping();
            s.intent = SurvivalPolicy.Intent.NORMAL; s.danger = false; s.running = false;
            return null;
        }
        if (danger) {
            if (v.isTrading()) v.setTradingPlayer(null);
            AdventureActions.tryDefend(level,v,threat);
        }
        if (danger != s.danger) { s.nextPlan = now; s.danger = danger; }
        if (now >= s.nextPlan) {
            s.plans++;
            s.nextPlan = now + 40;
            Path homePath = null;
            if (covered && !inside && distance <= 48) {
                s.homePathPlans++;
                BlockPos start=v.blockPosition();
                var route=RefugeRoutes.find(level,v,bed,s.refugeStand,now>=s.nextFloorSearch || !start.equals(s.floorSearchStart));
                homePath=route.path();s.refugeStand=route.stand();
                if (route.inspected()>0) { s.floorSearchStart=start.immutable();s.nextFloorSearch=now+200; }
                s.refugePathQueries+=route.queries();s.refugeStandInspections+=route.inspected();
            }
            if (covered && !inside && distance > 48) {
                s.refugeStand=null;
                Vec3 step = DefaultRandomPos.getPosTowards(v,16,5,Vec3.atCenterOf(bed),Math.PI/2);
                if (step != null && step.distanceToSqr(Vec3.atCenterOf(bed)) < distance * distance) {
                    s.homePathPlans++;
                    homePath = v.getNavigation().createPath(BlockPos.containing(step),0);
                }
            }
            Path observedHomePath = DevMode.enabled() ? homePath : null;
            boolean partial = false;
            if (!danger && homePath != null && !homePath.canReach() && homePath.getNodeCount() > 0) {
                double end = Vec3.atCenterOf(homePath.getNodePos(homePath.getNodeCount()-1)).distanceTo(Vec3.atCenterOf(bed));
                partial = SurvivalPolicy.usefulPartialReturn(distance, end, homePath.getNodeCount());
            }
            boolean bounded = danger || homePath != null && homePath.getNodeCount() <= 128;
            if (!danger && bounded) for (int i = 0; i < homePath.getNodeCount(); i++) {
                var p = homePath.getNodePos(i);
                if (!level.isLoaded(p) || !SurvivalPolicy.withinReturnBound(distance, Vec3.atCenterOf(p).distanceTo(Vec3.atCenterOf(bed)))) { bounded = false; break; }
            }
            if (!bounded) { homePath = null; partial = false; }
            boolean homeReachable = covered && (inside || homePath != null && (homePath.canReach() || partial))
                && !(s.blockedIntent == SurvivalPolicy.Intent.SHELTER && now<s.blockedUntil && !inside);
            boolean homeSafe = threat == null || bed != null && threat.position().distanceToSqr(Vec3.atCenterOf(bed)) > 16;
            if (homePath != null && threat != null && crossesThreat(homePath,threat)) homeSafe = false;
            Path guardianPath = null;
            if ((danger || dusk) && !(homeReachable && homeSafe)
                && !(s.blockedIntent == SurvivalPolicy.Intent.SEEK_GOLEM && now<s.blockedUntil)) {
                List<IronGolem> guardians = level.getEntitiesOfClass(IronGolem.class,v.getBoundingBox().inflate(24),
                    g -> g.isAlive() && g.getTarget() != v);
                guardians.sort(Comparator.comparingDouble(v::distanceToSqr));
                for (IronGolem guardian : guardians.stream().limit(3).toList()) {
                    Vec3 away = threat == null ? new Vec3(0,0,1) : guardian.position().subtract(threat.position()).multiply(1,0,1).normalize();
                    Vec3 destination = guardian.position().add(away.scale(3));
                    Path candidate = v.getNavigation().createPath(BlockPos.containing(destination),1);
                    if (candidate != null && candidate.canReach() && (threat == null || !crossesThreat(candidate,threat))) { guardianPath = candidate; break; }
                }
            }
            SurvivalPolicy.Intent intent = SurvivalPolicy.choose(new SurvivalPolicy.Situation(danger,dusk,covered,
                homeReachable,homeSafe,distance,guardianPath != null));
            if (DevMode.enabled()) {
                var plan = new LinkedHashMap<String,Object>();
                plan.put("gameTime",now); plan.put("position",List.of(v.getX(),v.getY(),v.getZ()));
                plan.put("bed",bed == null ? "none" : bed.toShortString()); plan.put("distance",distance);
                plan.put("covered",covered); plan.put("inside",inside); plan.put("dusk",dusk); plan.put("danger",danger);
                plan.put("nativeActivity",nativeActivity.toString());plan.put("clockTime",level.getOverworldClockTime());
                plan.put("skyDarken",level.getSkyDarken());plan.put("raining",level.isRaining());
                plan.put("pathPresent",observedHomePath != null);
                plan.put("pathCanReach",observedHomePath != null && observedHomePath.canReach());
                int nodes = observedHomePath == null ? 0 : observedHomePath.getNodeCount(); plan.put("pathNodes",nodes);
                if (nodes > 0) {
                    var end = observedHomePath.getNodePos(nodes-1);
                    plan.put("pathEnd",end.toShortString());
                    if (bed != null) plan.put("endDistance",Vec3.atCenterOf(end).distanceTo(Vec3.atCenterOf(bed)));
                }
                plan.put("partialAccepted",partial); plan.put("bounded",bounded); plan.put("homeReachable",homeReachable);
                plan.put("homeSafe",homeSafe); plan.put("blockedUntil",s.blockedUntil);
                plan.put("guardianReachable",guardianPath != null); plan.put("chosenIntent",intent.name());
                s.lastRefugePlan=Collections.unmodifiableMap(plan);
            }
            if (intent != s.intent) {
                v.getNavigation().stop();
                mind.remember("Safety decision " + intent + " at " + v.blockPosition().toShortString());
                s.intent = intent; s.restAt = now;
            }
            switch (intent) {
                case SHELTER -> {
                    if (!inside && homePath != null) {
                        if (v.isSleeping()) v.stopSleeping();
                        double speed = HURRY;
                        if (danger) speed = run(s,mind); else s.running = false;
                        if (partial) s.partialHomeRoutes++;
                        boolean moved=v.getNavigation().moveTo(homePath,speed);
                        if (moved && s.refugeStand!=null) {
                            s.floorRoutes++;
                            if (DevMode.enabled()) s.lastFloorRoute=Map.of("gameTime",now,"position",List.of(v.getX(),v.getY(),v.getZ()),
                                    "stand",s.refugeStand.toShortString(),"bed",bed.toShortString(),"pathCanReach",homePath.canReach(),
                                    "pathNodes",homePath.getNodeCount(),"pathEnd",homePath.getNodePos(homePath.getNodeCount()-1).toShortString());
                        }
                    }
                    else { v.getNavigation().stop(); s.running = false; }
                }
                case SEEK_GOLEM -> {
                    if (v.isSleeping()) v.stopSleeping();
                    s.goal=guardianPath.getTarget(); v.getNavigation().moveTo(guardianPath,run(s,mind));
                }
                case ESCAPE -> {
                    if (v.isSleeping()) v.stopSleeping();
                    Vec3 avoid = threat == null ? v.position().add(0,-1,0) : threat.position();
                    boolean found = false;
                    for (int attempt=0;attempt<3;attempt++) {
                        Vec3 point = DefaultRandomPos.getPosAway(v,12,5,avoid);
                        if (point == null) continue;
                        Path escape = v.getNavigation().createPath(BlockPos.containing(point),0);
                        if (escape != null && escape.canReach() && (threat == null || !crossesThreat(escape,threat))) {
                            v.getNavigation().moveTo(escape,run(s,mind)); found = true; break;
                        }
                    }
                    if (!found) { s.rejectedPaths++; v.getNavigation().stop(); s.running = false; }
                }
                case NORMAL -> { /* No invented shelter: leave movement to the bounded action layer. */ }
            }
        }
        if (s.intent == SurvivalPolicy.Intent.SHELTER && inside && bed != null && covered && !danger) {
            if (!v.isSleeping() && level.getBlockState(bed).getBlock() instanceof BedBlock
                && TraderRecruitment.allowedBed(level, bed)
                && !level.getBlockState(bed).getValue(BedBlock.OCCUPIED)) v.startSleeping(bed);
            if (v.isSleeping() && now - s.restAt >= 80) { mind.rest(); s.restAt = now; }
        }
        return switch (s.intent) {
            case NORMAL -> null;
            case SHELTER -> inside ? "sheltered" : "returning home";
            case SEEK_GOLEM -> "seeking golem protection";
            case ESCAPE -> "afraid / escaping";
        };
    }
    public static boolean canRestAtHome(ServerLevel level,Villager v,Home home) {
        if (home == null || !TraderRecruitment.allowedBed(level, new BlockPos(home.x(), home.y(), home.z())) || !home.dimension().equals(level.dimension().identifier().toString())) return false;
        BlockPos bed=new BlockPos(home.x(),home.y(),home.z());
        boolean covered=level.isLoaded(bed)&&level.getBlockState(bed).getBlock() instanceof BedBlock&&!level.canSeeSky(bed.above());
        return insideHome(level,v,bed,covered,v.position().distanceTo(Vec3.atCenterOf(bed)));
    }
    private static boolean insideHome(ServerLevel level, Villager v, BlockPos bed, boolean covered, double distance) {
        if (bed == null || !covered || distance > 2.5 || level.canSeeSky(v.blockPosition().above())) return false;
        if (v.isSleeping()) return v.getSleepingPos().filter(bed::equals).isPresent();
        var hit = level.clip(new ClipContext(v.getEyePosition(),Vec3.atCenterOf(bed),
            ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,v));
        // A bed may be the first collision; an intervening wall/closed door may not be bypassed.
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(bed);
    }
    private static boolean crossesThreat(Path path, LivingEntity threat) {
        // Skip the starting nodes when already in contact; reject walking through the attacker later.
        for (int i=3;i<path.getNodeCount();i++)
            if (Vec3.atCenterOf(path.getNode(i).asBlockPos()).distanceToSqr(threat.position()) < 6.25) return true;
        return false;
    }
}
