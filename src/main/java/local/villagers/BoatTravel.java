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

/** Finite food and stone expeditions using real boats and native paddling, riding and dismounts. */
public final class BoatTravel {
    private static final String TRIP = "villagers:sea:", OWNER = "villagers:boat_owner:";
    private static final String PARKED="villagers:parked_boat:";
    enum Phase { PREPARE, SAIL, ASHORE, RETURN }
    enum Purpose { FOOD, STONE, MINERALS }
    static final class Trip {
        BoatRoutes.Dock home, away;
        BoatRoutes.Route route;
        Phase phase = Phase.PREPARE;
        boolean harvest;
        Purpose purpose=Purpose.FOOD;
        int waypoint;
        long started, ashoreUntil, lastProgress;
        long footPathAt;
        boolean walkingHome;
        Map<String,Object> boardingRoute=Map.of();
        Vec3 progress;
        Trip(BoatRoutes.Route route, long now) { this.home = route.launch(); this.away = route.arrival(); this.route = route; started = now; lastProgress = now; }
        Trip(BoatRoutes.Dock home, BoatRoutes.Dock away, Phase phase, long now) {
            this.home = home; this.away = away; this.phase = phase; started = now; lastProgress = now; ashoreUntil = now + 1200;
        }
    }
    static boolean owned(AbstractBoat boat, Villager v) { return boat.entityTags().contains(OWNER + v.getUUID()); }
    public static boolean mayBoard(AbstractBoat boat,net.minecraft.world.entity.Entity passenger){
        if(!(passenger instanceof Villager v)||!Villagers.isManaged(v))return true;
        var s=Villagers.existingSession(v.getUUID());
        return s!=null&&s.boatTrip!=null&&owned(boat,v)&&s.boatTrip.phase!=Phase.ASHORE;
    }
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
        var result=new LinkedHashMap<String,Object>(Map.of("activity", s.boatStatus, "phase", s.boatTrip == null ? "NONE" : s.boatTrip.phase.name(),
            "recipes", s.boatRecipes, "logsGathered", s.boatLogsGathered, "launches", s.boatLaunches,
            "landings", s.boatLandings, "paddleTicks", s.boatPaddleTicks,"purpose",s.boatTrip==null?"NONE":s.boatTrip.purpose.name(),
            "footReturns",s.boatFootReturns,"lastReturn",s.boatReturnReceipt));
        result.put("boardingRoute",s.boatTrip==null?Map.of():s.boatTrip.boardingRoute);
        result.put("boatSurvey",Map.of("target",s.boatSurvey==null?"":s.boatSurvey.toShortString(),"lastRoute",s.boatSurveyReceipt));return result;
    }
    /** A trip grants only a16-block work area at its actual landing. Individual action permissions still apply. */
    static boolean withinTrip(ServerLevel level, AgentMind mind, Vec3 p) {
        var s = Villagers.existingSession(mind.id());
        return s != null && s.boatTrip != null && s.boatTrip.phase == Phase.ASHORE
            && level.isLoaded(s.boatTrip.away.land()) && Vec3.atBottomCenterOf(s.boatTrip.away.land()).distanceToSqr(p) <= 16 * 16;
    }
    static boolean ashore(Villager v) {
        var s = Villagers.existingSession(v.getUUID());
        return s != null && s.boatTrip != null && s.boatTrip.phase == Phase.ASHORE;
    }
    static boolean needsStone(Villager v){
        return AdventurePreparation.nominated(v)&&AdventurePreparation.pickaxe(v)&&!AdventurePreparation.stonePickaxe(v)
            &&WoodCrafting.stoneCount(v.getInventory())<3;
    }
    static boolean needsMinerals(Villager v){
        var s=Villagers.existingSession(v.getUUID());
        return s!=null&&AdventurePreparation.nominated(v)&&AdventurePreparation.stonePickaxe(v)
            &&AdventureIron.metalTarget(v)>0&&AdventureIron.needsMining(v,s);
    }
    static boolean mineralDeparture(MineStair.State mine,AdventureQuarry.State quarry){
        // These are the actor's actual retained mine floors, not operator ore
        // coordinates. An active local job finishes before a new expedition.
        return mine.floors.size()>=16&&mine.root==null&&quarry.root==null;
    }
    private static boolean resourceTrip(Trip trip){return trip.purpose!=Purpose.FOOD;}
    private static boolean stillNeedsResource(Villager v,Trip trip){return trip.purpose==Purpose.MINERALS?needsMinerals(v):needsStone(v);}
    static String tick(ServerLevel level, Villager v, Session s, LivingEntity threat, long now) {
        if (s.boatTrip == null) restore(v, s, now);
        if(s.boatTrip==null&&v.getVehicle() instanceof AbstractBoat incidental
            &&incidental.getPassengers().stream().noneMatch(p->p instanceof net.minecraft.world.entity.player.Player)
            &&incidental.getDeltaMovement().horizontalDistance()<=.09){
            for(var dock:BoatRoutes.launches(level,v))if(horizontal(incidental.position(),dock.point())<=4&&nativeExit(level,v,incidental,dock)){
                v.stopRiding();s.boatLandings++;return s.boatStatus="left an incidental boat at a native dry-bank exit";
            }
        }
        if(s.boatTrip==null&&v.getVehicle() instanceof AbstractBoat boat&&owned(boat,v)&&boat.getFirstPassenger()==v
            &&boat.getPassengers().stream().noneMatch(p->p instanceof net.minecraft.world.entity.player.Player))recoverBoarding(level,v,s,boat,now);
        if (s.boatTrip == null) {
            boolean minerals=NativeRegionsScenario.nativeRegions(level)&&Villagers.config().nativeAdventureQuarry()
                &&needsMinerals(v)&&mineralDeparture(s.mineStair,s.quarry);
            boolean stone=(needsStone(v)||minerals)&&FoodSupply.deficit(v.getInventory(),AdventureActions.canHarvest(v))<=10
                &&s.mind.foodLevel()>=16&&v.getHealth()>=v.getMaxHealth()*.9&&s.mind.fatigue()<.6;
            if(s.boatSurvey!=null){
                if(stone&&threat==null&&!v.isOnFire()&&!v.isInWater()&&!v.isSleeping()&&!VillageSurvival.scheduledRest(level,v)){
                    var survey=surveyRememberedBoat(level,v,s,now);if(survey!=null)return s.boatStatus=survey;
                }else s.boatSurvey=null;
            }
            if(minerals&&stone&&!VillageSurvival.scheduledRest(level,v)&&!v.isSleeping()&&!v.isInWater()&&threat==null){
                var exit=MineStair.returnToEntry(level,v,s,now);if(exit!=null)return s.boatStatus=exit;
            }
            if (now < s.nextBoatSearch || threat != null || v.isOnFire() || v.isPassenger() || v.isBaby()
                    || v.isSleeping() || v.isTrading() || v.isInWater() || s.courtship != null&&s.courtship.active(now)
                    || !FoodSupply.mayWork(v.getAge(), v.getHealth(), s.mind.fatigue(), !VillageSurvival.scheduledRest(level, v))
                    || s.mind.foodLevel() <= 6 || !stone&&FoodSupply.deficit(v.getInventory(), AdventureActions.canHarvest(v)) < 10) return null;
            s.nextBoatSearch = now + 400;
            if(stone&&BoatPreparation.boatSlot(v)<0){
                var survey=surveyRememberedBoat(level,v,s,now);
                if(survey!=null){s.nextBoatSearch=now+20;return s.boatStatus=survey;}
            }
            if (!WorkNavigation.admit(level, now, v)) { s.nextBoatSearch = now + 20; return null; }
            if(stone&&!minerals&&AdventureQuarry.prepare(level,v,s,now))return null;
            BlockPos goal = stone?AdventureMining.observed(level,v,s):visibleFood(level, v);
            // Existing nearby food comes first; a boat is a route fallback, not a competing food job.
            if (goal != null) {
                var path = stone?WoodBootstrap.approachPath(level,v,s,goal):v.getNavigation().createPath(goal, 1);
                if (dryPath(level, path)&&(!stone||AdventureMining.surface(level,v,s,goal))) return null;
            }
            var route = BoatRoutes.find(level, v, goal,land->!stone||unsearchedStoneShore(v,land));
            if (route == null) {
                if(lookFromOwnedBoat(level,v,s,now))return s.boatStatus;
                s.boatStatus = "no observed safe water crossing"; return null;
            }
            s.boatTrip = new Trip(route, now);
            s.boatTrip.purpose=stone?(minerals?Purpose.MINERALS:Purpose.STONE):Purpose.FOOD;
            // An exploratory shore can reveal crops only after landing: carry the tool before sailing.
            s.boatTrip.harvest = !stone&&(goal == null || level.getBlockState(goal).getBlock() instanceof CropBlock);
            save(v, s.boatTrip);
            s.mind.remember("Planned a "+(minerals?"mineral":stone?"stone":"food")+" expedition by boat to " + route.arrival().land().toShortString());
        }
        var trip = s.boatTrip;
        if (!v.isAlive() || v.isBaby() || v.isTrading()) { stop(v, s, now, "boat trip interrupted"); return null; }
        if (!v.isPassenger() && v.isInWater()) { stop(v, s, now, "seeking dry ground after leaving the boat"); return null; }
        if (trip.phase == Phase.ASHORE) {
            if (now < trip.ashoreUntil && !VillageSurvival.scheduledRest(level, v) && threat == null && !v.isOnFire()
                    &&(resourceTrip(trip)?stillNeedsResource(v,trip)&&s.mind.foodLevel()>=14&&v.getHealth()>=.9*v.getMaxHealth()
                        :FoodSupply.deficit(v.getInventory(), AdventureActions.canHarvest(v)) > 0)) {
                if(trip.purpose==Purpose.FOOD){
                    var food=visibleFood(level,v,p->withinTrip(level,s.mind,Vec3.atCenterOf(p)));
                    if(food!=null&&v.getEyePosition().distanceToSqr(Vec3.atCenterOf(food))>9){
                        if(now<s.nextBoatPath)return s.boatStatus="walking from the boat to observed food";
                        if(WorkNavigation.admit(level,now,v)){
                            s.nextBoatPath=now+40;var path=v.getNavigation().createPath(food,1,64);
                            if(dryPath(level,path)&&WorkNavigation.staysHome(level,v,s,path)&&v.getNavigation().moveTo(path,.6))
                                return s.boatStatus="walking from the boat to observed food";
                        }
                    }
                }
                return null;
            }
            if(resourceTrip(trip)&&stillNeedsResource(v,trip)&&now>=trip.ashoreUntil)rememberStoneShore(v,trip.away.land());
            trip.phase = Phase.RETURN; trip.route = null; trip.started = now; save(v, trip);
        }
        boolean returning = trip.phase == Phase.RETURN;
        if(returning&&!v.isPassenger()){
            if(VillageSurvival.scheduledRest(level,v)||s.mind.fatigue()>.65)return null;
            var walking=walkReturn(level,v,s,trip,now);if(walking!=null)return walking;
        }
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
            return s.boatStatus = returning ? "rowing "+(resourceTrip(trip)?"mineral expedition":"food supplies")+" home" : "rowing toward the far shore";
        }
        if (!returning && now - trip.started > 1200) { stop(v, s, now, "boat preparation timed out"); return null; }
        var dock = trip.route.launch();
        AbstractBoat existing = level.getEntitiesOfClass(AbstractBoat.class, new net.minecraft.world.phys.AABB(dock.water()).inflate(4),
            b -> b.isAlive() && owned(b, v) && !b.isVehicle()).stream().min(Comparator.comparingDouble(v::distanceToSqr)).orElse(null);
        if ((existing == null && BoatPreparation.boatSlot(v) < 0) || (!returning && trip.harvest && !AdventureActions.hasHoe(v))) {
            if (returning) return s.boatStatus = "return boat unavailable";
            return BoatPreparation.work(level, v, s, now);
        }
        // An owned hull can obstruct the exact bank-centre approach. Use the
        // actual native interaction reach from dry ground before walking into
        // that hull; walls and an invalid dock still reject boarding.
        if(existing!=null&&v.onGround()&&v.distanceToSqr(existing)<9&&v.hasLineOfSight(existing)&&BoatRoutes.dockValid(level,dock)
                &&v.startRiding(existing)){
            v.getNavigation().stop();trip.phase=returning?Phase.RETURN:Phase.SAIL;
            trip.started=trip.lastProgress=now;save(v,trip);return s.boatStatus="boarded my existing boat from the dry bank";
        }
        if(existing!=null&&Vec3.atBottomCenterOf(dock.land()).distanceToSqr(existing.position())>=9
                &&approachMovedBoat(level,v,s,trip,existing,returning,now))return s.boatStatus;
        if (v.position().distanceToSqr(Vec3.atBottomCenterOf(dock.land())) > 1.2 * 1.2) {
            boolean moving=false;
            if (now >= s.nextBoatPath && WorkNavigation.admit(level, now, v)) {
                s.nextBoatPath = now + 40;
                var path = v.getNavigation().createPath(dock.land(), 0,64);
                if (dryPath(level,path)) moving=v.getNavigation().moveTo(path, .6);
            }
            var active=v.getNavigation().getPath();
            moving|=active!=null&&!active.isDone()&&active.getTarget().equals(dock.land());
            s.boatStatus=moving?"walking to the boat launch":"needs a safe route to the boat launch";
            return moving?s.boatStatus:null;
        }
        v.getNavigation().stop();
        if (!BoatRoutes.dockValid(level, dock)) { stop(v, s, now, "boat launch changed"); return null; }
        if (existing == null) existing = deploy(level, v, s, dock);
        if (existing != null && v.distanceToSqr(existing) < 9 && v.startRiding(existing)) {
            trip.phase = returning ? Phase.RETURN : Phase.SAIL; trip.started = now; trip.lastProgress = now;
            save(v, trip); s.boatStatus = "boarded a real boat";
        }else s.boatStatus="my boat is outside boarding reach of this bank";
        return s.boatStatus;
    }
    private static boolean approachMovedBoat(ServerLevel level,Villager v,Session s,Trip trip,AbstractBoat boat,boolean returning,long now){
        if(!v.onGround()||!v.hasLineOfSight(boat)||now<s.nextBoatPath||!WorkNavigation.admit(level,now,v))return false;
        s.nextBoatPath=now+40;
        var candidates=BoatRoutes.launches(level,v).stream()
            .filter(d->!d.land().equals(trip.route.launch().land())&&Vec3.atBottomCenterOf(d.land()).distanceToSqr(boat.position())<9)
            .sorted(Comparator.comparingDouble(d->d.land().distSqr(v.blockPosition()))).limit(3).toList();
        for(var next:candidates){
            var path=NativePathLimits.search(v,2,()->v.getNavigation().createPath(next.land(),0,64));
            if(!dryPath(level,path))continue;
            var route=BoatRoutes.across(level,v,next,returning?trip.home.land():trip.away.land());
            if(route==null||!v.getNavigation().moveTo(path,.6))continue;
            trip.boardingRoute=Map.of("gameTime",now,"oldBank",trip.route.launch().land().toShortString(),
                "newBank",next.land().toShortString(),"boat",boat.position().toString(),"actor",v.position().toString(),"nodes",path.getNodeCount());
            trip.route=route;trip.waypoint=0;
            if(returning)trip.away=next;else trip.home=next;
            save(v,trip);s.boatStatus="walking to a reachable bank beside my moved boat";return true;
        }
        return false;
    }
    private static String surveyRememberedBoat(ServerLevel level,Villager v,Session s,long now){
        var seen=level.getEntitiesOfClass(AbstractBoat.class,v.getBoundingBox().inflate(48),
            b->b.isAlive()&&owned(b,v)&&!b.isVehicle()&&(v.distanceToSqr(b)<=16*16||v.hasLineOfSight(b)));
        if(!seen.isEmpty()){s.boatSurvey=null;return null;}
        s.boatSurveyed.entrySet().removeIf(e->e.getValue()<=now);
        if(s.boatSurvey!=null){
            if(now>=s.boatSurveyUntil||v.position().distanceToSqr(Vec3.atBottomCenterOf(s.boatSurvey))<=4){
                s.boatSurveyed.put(s.boatSurvey,now+6000);s.boatSurvey=null;s.nextBoatSurvey=now;
            }else{
                var current=v.getNavigation().getPath();
                if(current!=null&&!current.isDone()&&current.getTarget().equals(s.boatSurvey))return "looking for my boat at a remembered shore";
                if(now<s.nextBoatSurveyPath||!v.onGround()||!WorkNavigation.admit(level,now,v))return "waiting to continue the remembered shore survey";
                s.nextBoatSurveyPath=now+40;var target=s.boatSurvey;
                var path=NativePathLimits.search(v,2,()->v.getNavigation().createPath(target,0,128));
                if(WorkNavigation.staysHome(level,v,s,path)&&dryPath(level,path)&&v.getNavigation().moveTo(path,.6))return "looking for my boat at a remembered shore";
                v.getNavigation().stop();s.boatSurveyed.put(target,now+6000);s.boatSurvey=null;s.nextBoatSurvey=now+40;s.nextBoatSearch=now+20;
                return "checking the remaining remembered boat shores";
            }
        }
        var remembered=new LinkedHashSet<BlockPos>();var parked=new HashSet<BlockPos>();
        for(var tag:v.entityTags())try{
            if(tag.startsWith(PARKED)){var p=dock(tag.substring(PARKED.length())).land();remembered.add(p);parked.add(p);}
        }catch(RuntimeException ignored){}
        for(var tag:v.entityTags())try{
            if(tag.startsWith("villagers:stone_shore:"))remembered.add(BlockPos.of(Long.parseLong(tag.substring("villagers:stone_shore:".length()))));
        }catch(NumberFormatException ignored){}
        var targets=remembered.stream().filter(p->!s.boatSurveyed.containsKey(p)&&p.distSqr(v.blockPosition())<=64*64
            &&level.isLoaded(p)&&VillageSurvival.withinHomeRange(level,s.mind,Vec3.atBottomCenterOf(p)))
            .sorted(Comparator.<BlockPos>comparingInt(p->parked.contains(p)?0:1).thenComparingDouble(p->p.distSqr(v.blockPosition()))).toList();
        if(targets.isEmpty())return null;
        if(now<s.nextBoatSurvey||!v.onGround()||!WorkNavigation.admit(level,now,v))return "waiting to inspect a remembered boat shore";
        s.nextBoatSurvey=now+40;
        for(var target:targets.stream().limit(3).toList()){
            if(v.position().distanceToSqr(Vec3.atBottomCenterOf(target))<=4){s.boatSurveyed.put(target,now+6000);continue;}
            var path=NativePathLimits.search(v,2,()->v.getNavigation().createPath(target,0,128));
            if(!WorkNavigation.staysHome(level,v,s,path)||!dryPath(level,path)||!v.getNavigation().moveTo(path,.6)){
                s.boatSurveyed.put(target,now+6000);continue;
            }
            s.boatSurvey=target;s.boatSurveyUntil=now+1200;s.nextBoatSurveyPath=now+40;
            s.boatSurveyReceipt=Map.of("gameTime",now,"from",v.position().toString(),"rememberedShore",target.toShortString(),"nodes",path.getNodeCount());
            return "looking for my boat at a remembered shore";
        }
        return "checking the remaining remembered boat shores";
    }
    private static void rememberParked(Villager v,BoatRoutes.Dock dock){
        v.entityTags().stream().filter(t->t.startsWith(PARKED)).toList().forEach(v::removeTag);
        v.addTag(PARKED+coords(dock));
    }
    /** A recorded return can finish on foot when a real dry route connects the shores. Leave the boat where it is. */
    private static String walkReturn(ServerLevel level,Villager v,Session s,Trip trip,long now){
        if(!v.onGround()||v.isInWater()||v.isOnFire())return null;
        var home=trip.home.land();
        if(v.position().distanceToSqr(Vec3.atBottomCenterOf(home))<=4){
            s.boatFootReturns++;s.boatReturnReceipt=Map.of("gameTime",now,"method","walked home on dry ground",
                "homeDock",home.toShortString(),"position",v.position().toString());
            v.getNavigation().stop();stop(v,s,now,"returned home on foot with owned supplies");return s.boatStatus;
        }
        var current=v.getNavigation().getPath();
        if(trip.walkingHome&&current!=null&&!current.isDone()&&current.getTarget().equals(home))
            return s.boatStatus="walking owned supplies home on a dry route";
        if(now<trip.footPathAt||!WorkNavigation.admit(level,now,v))return null;
        trip.footPathAt=now+200;
        var path=NativePathLimits.search(v,1,()->v.getNavigation().createPath(home,0,128));
        trip.walkingHome=path!=null&&path.getNodeCount()<=128&&dryPath(level,path)&&v.getNavigation().moveTo(path,.6);
        return trip.walkingHome?s.boatStatus="walking owned supplies home on a dry route":null;
    }
    private static boolean lookFromOwnedBoat(ServerLevel level,Villager v,Session s,long now){
        var boats=level.getEntitiesOfClass(AbstractBoat.class,v.getBoundingBox().inflate(48),
            b->b.isAlive()&&!b.isVehicle()&&owned(b,v)&&(v.distanceToSqr(b)<=16*16||v.hasLineOfSight(b)));
        if(boats.isEmpty())return false;
        for(var dock:BoatRoutes.launches(level,v)){
            if(boats.stream().noneMatch(b->horizontal(b.position(),dock.point())<16))continue;
            if(v.position().distanceToSqr(Vec3.atBottomCenterOf(dock.land()))<=1.2*1.2)return false;
            var path=v.getNavigation().createPath(dock.land(),0,64);
            if(!dryPath(level,path)||!v.getNavigation().moveTo(path,.6))continue;
            s.nextBoatSearch=now+40;s.boatStatus="walking to my boat to observe the far shore";return true;
        }
        return false;
    }
    /** Recover an old incidental boarding through a real short paddle and the usual native dry-bank exit. */
    private static void recoverBoarding(ServerLevel level,Villager v,Session s,AbstractBoat boat,long now){
        boat.setInput(false,false,false,false);
        for(var dock:BoatRoutes.launches(level,v)){
            var start=new Vec3(boat.getX(),dock.point().y,boat.getZ());
            if(horizontal(start,dock.point())>4||!BoatRoutes.surface(level,BlockPos.containing(start.x,dock.water().getY(),start.z))
                // A native hull resting flush against a bank can be legal but
                // inside the route planner's extra .0025-block clearance margin.
                ||level.getBlockCollisions(boat,boat.getBoundingBox().move(0,dock.point().y-boat.getY(),0)).iterator().hasNext()
                ||!BoatRoutes.segmentClear(level,start,dock.point()))continue;
            var trip=new Trip(new BoatRoutes.Route(dock,dock,List.of(start,dock.point())),now);
            trip.phase=Phase.RETURN;trip.waypoint=1;s.boatTrip=trip;
            s.boatStatus="returning to a dry bank after incidental boarding";
            s.mind.remember("Seeking a native dry-bank exit after incidental boarding");return;
        }
        s.boatStatus="waiting for an observed native dry-bank exit";
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
        v.getInventory().removeItem(slot, 1); s.boatLaunches++;rememberParked(v,dock);return boat;
    }
    private static boolean land(ServerLevel level, Villager v, Session s, AbstractBoat boat, long now) {
        var trip = s.boatTrip; var dock = trip.route.arrival();
        if (horizontal(boat.position(), dock.point()) > .5 * .5 || boat.getDeltaMovement().horizontalDistance() > .09
                || !BoatRoutes.dockValid(level, dock)) return false;
        if(!nativeExit(level,v,boat,dock))return false;
        boat.setInput(false, false, false, false); v.stopRiding(); s.boatLandings++;rememberParked(v,dock);
        if (trip.phase == Phase.RETURN) stop(v, s, now, "landed home with the real boat retained at shore");
        else {
            trip.away=dock; // The actual landing is the centre of the bounded shore work area.
            trip.phase = Phase.ASHORE; trip.ashoreUntil = now + 1200; trip.route = null; save(v, trip);
            s.boatStatus = "landed to look for "+(resourceTrip(trip)?"exposed minerals":"food");
            s.nextCropScan = now; s.nextRootCropScan = now; s.nextFamilyProvision = now;
            s.mind.remember("Crossed water and landed at " + dock.land().toShortString());
        }
        return true;
    }
    private static boolean nativeExit(ServerLevel level,Villager v,AbstractBoat boat,BoatRoutes.Dock dock){
        var p=Vec3.atBottomCenterOf(dock.land());
        v.setYRot((float)(Math.atan2(boat.getX()-p.x,p.z-boat.getZ())*180/Math.PI));
        var exit=boat.getDismountLocationForPassenger(v);
        return BoatRoutes.dry(level,BlockPos.containing(exit))&&exit.distanceToSqr(p)<=2;
    }
    private static boolean unsearchedStoneShore(Villager v,BlockPos land){
        for(var tag:v.entityTags())if(tag.startsWith("villagers:stone_shore:"))try{
            if(BlockPos.of(Long.parseLong(tag.substring("villagers:stone_shore:".length()))).distSqr(land)<16*16)return false;
        }catch(NumberFormatException ignored){}
        return true;
    }
    private static void rememberStoneShore(Villager v,BlockPos land){
        if(v.entityTags().stream().filter(t->t.startsWith("villagers:stone_shore:")).count()<16)v.addTag("villagers:stone_shore:"+land.asLong());
    }
    private static BlockPos visibleFood(ServerLevel level, Villager v) {
        return visibleFood(level,v,p->true);
    }
    private static BlockPos visibleFood(ServerLevel level,Villager v,java.util.function.Predicate<BlockPos> allowed){
        var items = level.getEntitiesOfClass(ItemEntity.class, v.getBoundingBox().inflate(48),
            i -> i.isAlive() && FoodInventory.supported(i.getItem()) && v.hasLineOfSight(i)&&allowed.test(i.blockPosition()));
        var nearest = items.stream().min(Comparator.comparingDouble(v::distanceToSqr)).orElse(null);
        if (nearest != null) return nearest.blockPosition();
        var origin = v.blockPosition(); BlockPos best = null; double score = Double.MAX_VALUE;
        for (int x = -48; x <= 48; x++) for (int z = -48; z <= 48; z++) {
            if (x * x + z * z > 48 * 48 || !level.isLoaded(origin.offset(x, 0, z))) continue;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, origin.getX() + x, origin.getZ() + z) - 1;
            var p = new BlockPos(origin.getX() + x, y, origin.getZ() + z); var state = level.getBlockState(p);
            if (!allowed.test(p)||!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state) || ConstructionSites.isProtected(level, p)) continue;
            double distance = p.distSqr(origin);
            if (distance < score && BoatRoutes.visible(level, v, Vec3.atCenterOf(p).add(0, .5, 0))) { best = p; score = distance; }
        }
        return best;
    }
    static boolean dryPath(ServerLevel level, net.minecraft.world.level.pathfinder.Path path) {
        if (path == null || !path.canReach()) return false;
        for (int i = 0; i < path.getNodeCount(); i++)
            if (!level.isLoaded(path.getNodePos(i))||!level.getWorldBorder().isWithinBounds(path.getNodePos(i))
                ||!level.getFluidState(path.getNodePos(i)).isEmpty() || !level.getFluidState(path.getNodePos(i).below()).isEmpty()
                ||i>0&&path.getNodePos(i).getY()<path.getNodePos(i-1).getY()-2) return false;
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
        v.addTag(TRIP + trip.phase.name() + ":" + coords(trip.home) + ":" + coords(trip.away) + ":" + trip.harvest+":"+trip.purpose.name());
    }
    private static void restore(Villager v, Session s, long now) {
        for (String tag : v.entityTags()) if (tag.startsWith(TRIP)) {
            try {
                var parts = tag.substring(TRIP.length()).split(":"); if (parts.length < 3 || parts.length > 5) return;
                var trip = new Trip(dock(parts[1]), dock(parts[2]), Phase.valueOf(parts[0]), now);
                trip.harvest = parts.length>=4?Boolean.parseBoolean(parts[3]):trip.phase==Phase.PREPARE;
                if(parts.length==5)trip.purpose=Purpose.valueOf(parts[4]);
                if (trip.home.land().distSqr(trip.away.land()) <= 128 * 128) s.boatTrip = trip;
            } catch (RuntimeException invalid) { s.boatStatus = "invalid saved boat trip"; }
            return;
        }
    }
    private BoatTravel() {}
}
