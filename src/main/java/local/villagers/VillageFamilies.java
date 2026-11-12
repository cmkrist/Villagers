package local.villagers;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import local.villagers.family.BreedingPolicy;
import local.villagers.family.Courtship;
import local.villagers.language.Utterance;
import local.villagers.language.Lexicon;
import local.villagers.language.Concept;
import local.villagers.genetics.Tendency;
import local.villagers.mind.AgentMind;
import local.villagers.mind.Episode;
import local.villagers.mind.Home;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;

/** Real-food, spare-home births during ordinary social contact. Does not build beds or load chunks. */
final class VillageFamilies {
    private static final Random RANDOM = new Random();
    private static final Map<ServerLevel, Long> NEXT_SEARCH = new IdentityHashMap<>();
    private static final Map<ServerLevel, Long> NEXT_SIGNAL = new IdentityHashMap<>();
    private static final Map<java.util.UUID, BedVisit> VISITS = new java.util.HashMap<>();
    private static final class BedVisit {
        final ServerLevel level;
        final java.util.UUID a, b;
        final BlockPos bed, nursery;
        final long started;
        long together = -1, nextEffect;
        BedVisit(ServerLevel level, Villager a, Villager b, BlockPos bed, BlockPos nursery, long now) {
            this.level = level; this.a = a.getUUID(); this.b = b.getUUID();
            this.bed = bed; this.nursery = nursery; started = now;
        }
    }
    private record FoodPlan(List<ItemStack> before, List<Integer> take) {}
    private VillageFamilies() {}
    static void clear() { NEXT_SEARCH.clear(); NEXT_SIGNAL.clear(); VISITS.values().stream().distinct().toList().forEach(VillageFamilies::endVisit); VISITS.clear(); }

    /** One local offer at most per dimension/second; listeners evaluate only their own private readiness. */
    static void signal(ServerLevel level, Villager sender, Session s, long now) {
        // Preparation is the actor's own goal, before contact. Reacquire it after food is
        // eaten/shared or a session reloads; no private partner inventory is consulted.
        if(Villagers.LEARNING_MODE==local.villagers.learning.LearningMode.FROZEN)return;
        boolean preparing = readyToPrepare(level, sender, s, now);
        if (!updateFoodIntent(s, preparing, sender.getInventory(),
                preparing && AdventureActions.canHarvest(sender))) return;
        if (s.courtship != null) {
            if (s.courtship.active(now)) return;
            s.courtship = null;
        }
        if (now < s.nextFamilySignal || now < NEXT_SIGNAL.getOrDefault(level, 0L)) return;
        s.nextFamilySignal = now + 100;
        NEXT_SIGNAL.put(level, now + 20);
        double radius = Math.min(16, Villagers.config().hearingRadius());
        var hearers = level.getEntitiesOfClass(Villager.class, sender.getBoundingBox().inflate(radius),
                v -> v != sender && s.mind.partnerships().committedTo(v.getUUID()) && Villagers.isManaged(v) && v.isAlive() && v.distanceToSqr(sender) <= radius * radius
                        && v.hasLineOfSight(sender)).stream().limit(Courtship.MAX_HEARERS).toList();
        if (hearers.isEmpty()) return;
        Utterance words = Speech.familySignal(level, sender, s, false);
        if (words == null) return;
        s.nextFamilySignal = now + Courtship.LIFETIME;
        Courtship.Offer offer = Courtship.Offer.at(sender.getUUID(), now);
        for (Villager listener : hearers) {
            Session receiver = Villagers.existingSession(listener.getUUID());
            if (receiver == null || !PartnershipLife.mutual(s,receiver) || now < receiver.nextFamilyReply || s.mind.opinionOf(listener.getUUID()).regard() < 0
                    || !Courtship.mayAccept(offer, listener.getUUID(), receiver.courtship, now,
                    true, true, readyToSignal(level, listener, receiver, now),
                    receiver.mind.opinionOf(sender.getUUID()).regard(),
                    false, understands(words, receiver.mind.lexicon(),false))) continue;
            // An actual locally audible reply is required, not access to the listener's private mind.
            Utterance reply = Speech.familySignal(level, listener, receiver, true);
            if (reply == null) continue;
            receiver.nextFamilyReply = now + 600;
            if (!understands(reply, s.mind.lexicon(),true)) continue;
            s.courtship = new Courtship.Agreement(listener.getUUID(), now, offer.expiresAt());
            receiver.courtship = new Courtship.Agreement(sender.getUUID(), now, offer.expiresAt());
            s.familyStatus = receiver.familyStatus = "agreed to meet a family partner";
            s.mind.remember("Heard and accepted a family invitation from " + receiver.mind.name());
            receiver.mind.remember("Heard and accepted a family invitation from " + s.mind.name());
            return;
        }
    }

    static boolean courting(ServerLevel level, Villager v, Session s, long now) {
        return s.courtship != null && s.courtship.active(now) && readyToSignal(level, v, s, now)
                && !level.getEntitiesOfClass(Villager.class, v.getBoundingBox().inflate(16), other ->
                        other.getUUID().equals(s.courtship.partner()) && Villagers.isManaged(other) && other.isAlive() && !other.isSleeping()
                                && v.distanceToSqr(other) <= 256 && v.hasLineOfSight(other)
                                && VillageSurvival.withinHomeRange(level, s.mind, other.position())).isEmpty();
    }

    private static boolean readyToSignal(ServerLevel level, Villager v, Session s, long now) {
        return readyToPrepare(level, v, s, now) && hasFamilyFood(v);
    }

    static boolean readyForCompany(ServerLevel level, Villager v, Session s, long now) {
        return (s.courtship == null || !s.courtship.active(now)) && readyToSignal(level, v, s, now);
    }

    static boolean readyToPrepare(ServerLevel level, Villager v, Session s, long now) {
        return Villagers.config().breeding() && !level.isDarkOutside() && adult(v)
                && willing(v, s, 0, now);
    }

    /** Returns funded readiness; an unfunded actor prepares, without promising or selecting a partner. */
    static boolean updateFoodIntent(Session s, boolean otherwiseReady, Container inventory, boolean farmer) {
        boolean funded = otherwiseReady && hasFamilyFood(inventory, farmer);
        s.familyFoodGoal = otherwiseReady && !funded;
        if (!funded) {
            s.courtship = null;
            if (s.familyFoodGoal) s.familyStatus = "saving food for a child";
        }
        return funded;
    }

    private static boolean understands(Utterance utterance, Lexicon listener,boolean reply) {
        return utterance!=null&&local.villagers.language.RelationshipDialogue.act(local.villagers.language.Sentence.interpret(utterance.sounds(),listener))
                ==(reply?local.villagers.language.RelationshipDialogue.Act.PROMISE_CHILD:local.villagers.language.RelationshipDialogue.Act.PROPOSE_CHILD);
    }

    static void consider(ServerLevel level, Villager a, Session first, Villager b) {
        if (!Villagers.config().breeding() || VISITS.containsKey(a.getUUID()) || VISITS.containsKey(b.getUUID())) return;
        Session second = Villagers.existingSession(b.getUUID());
        if (second == null) return;
        long now = level.getGameTime();
        if (now < first.nextFamilyAttempt || now < second.nextFamilyAttempt) return;
        first.nextFamilyAttempt = second.nextFamilyAttempt = now + 200;
        if (level.isDarkOutside() || !adult(a) || !adult(b) || a.distanceToSqr(b) > 9 || !a.hasLineOfSight(b)) return;
        if (BreedingPolicy.closeKin(a.getUUID(), first.mind.lineage(), b.getUUID(), second.mind.lineage())) {
            first.familyStatus = second.familyStatus = "close relatives"; return;
        }
        double regardA = first.mind.opinionOf(b.getUUID()).regard(), regardB = second.mind.opinionOf(a.getUUID()).regard();
        if (!willing(a, first, regardA, now) || !willing(b, second, regardB, now)) {
            first.familyStatus = second.familyStatus = "not ready for a child"; return;
        }
        FoodPlan costA = foodPlan(a), costB = foodPlan(b);
        if (costA == null || costB == null) {
            first.familyFoodGoal = costA == null; second.familyFoodGoal = costB == null;
            first.familyStatus = second.familyStatus = "saving food for a child"; return;
        }
        if (!consented(first, second, now)) {
            first.familyStatus = second.familyStatus = "awaiting a mutual family invitation"; return;
        }
        if (now < NEXT_SEARCH.getOrDefault(level, 0L)) return;
        NEXT_SEARCH.put(level, now + 40); // At most one expensive family search per dimension per two seconds.
        if (level.getEntitiesOfClass(Villager.class, a.getBoundingBox().inflate(32), Villager::isAlive).size()
                >= BreedingPolicy.LOCAL_POPULATION_CAP) { first.familyStatus = second.familyStatus = "local population limit"; return; }
        if (!level.getEntitiesOfClass(Monster.class, a.getBoundingBox().inflate(16), m -> m.isAlive()
                && (a.hasLineOfSight(m) || b.hasLineOfSight(m))).isEmpty()) {
            first.familyStatus = second.familyStatus = "village feels unsafe"; return;
        }
        List<BlockPos> beds = nurseries(level, a);
        if (beds.isEmpty()) { first.familyStatus = second.familyStatus = "needs a spare sheltered bed"; return; }
        first.familyStatus = second.familyStatus = "considering a child";
        if (RANDOM.nextDouble() >= BreedingPolicy.chance(first.mind.genome().tendency(Tendency.FERTILITY),
                second.mind.genome().tendency(Tendency.FERTILITY), regardA, regardB, beds.size())) return;
        for (BlockPos bed : beds.stream().limit(3).toList()) {
            var path = a.getNavigation().createPath(bed, 1);
            if (path == null || !path.canReach()) continue;
            BlockPos king = kingBed(level, a, b);
            if (king != null) {
                BedVisit visit = new BedVisit(level, a, b, king, bed, now);
                VISITS.put(a.getUUID(), visit); VISITS.put(b.getUUID(), visit);
                first.familyStatus = second.familyStatus = "meeting at the King Size Bed";
                return;
            }
            if (birth(level, a, first, costA, b, second, costB, bed, now)) return;
        }
        first.familyStatus = second.familyStatus = "no reachable nursery";
    }

    private static boolean adult(Villager v) {
        return Villagers.isManaged(v) && v.isAlive() && v.getAge() == 0 && !v.isSleeping() && !v.isTrading()
                && !v.isOnFire() && !v.isInWater() && v.getAirSupply() >= 200;
    }
    private static boolean willing(Villager v, Session s, double regard, long now) {
        return s.action != local.villagers.mind.Action.FLEE && (v.getLastHurtByMob() == null
                || v.tickCount - v.getLastHurtByMobTimestamp() >= 200)
                && BreedingPolicy.willing(v.getHealth() / v.getMaxHealth(), s.mind.foodLevel(), s.mind.fatigue(),
                        s.mind.mood().low(), regard, now, s.mind.nextBreedingAt());
    }
    private static FoodPlan foodPlan(Villager v) {
        List<ItemStack> original = new ArrayList<>();
        List<BreedingPolicy.Stack> food = new ArrayList<>();
        for (ItemStack stack : v.getInventory().getItems()) {
            original.add(stack.copy());
            var kind = foodKind(stack);
            food.add(new BreedingPolicy.Stack(kind, stack.getCount(), AdventureActions.canHarvest(v)
                    && (stack.is(Items.CARROT) || stack.is(Items.POTATO))));
        }
        return BreedingPolicy.foodCost(food).map(cost -> new FoodPlan(List.copyOf(original), cost)).orElse(null);
    }
    static boolean hasFamilyFood(Villager v) { return hasFamilyFood(v.getInventory(), AdventureActions.canHarvest(v)); }

    /** Read-only bounded scan, no stack snapshots or payment-plan allocation on ordinary think ticks. */
    static boolean hasFamilyFood(Container inventory, boolean farmer) {
        return foodDeficit(inventory, farmer) == 0;
    }

    /** Raw grain target for only the missing part of one parent's cost; wheat is never payment itself. */
    static int wheatReserveNeeded(Container inventory, boolean farmer) {
        return ((foodDeficit(inventory, farmer) + 3) / 4) * 3;
    }

    private static int foodDeficit(Container inventory, boolean farmer) {
        int needed = BreedingPolicy.FOOD_COST;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            int units = foodKind(stack).units();
            if (units == 0) continue;
            int available = Math.max(0, stack.getCount() - (farmer && (stack.is(Items.CARROT) || stack.is(Items.POTATO)) ? 1 : 0));
            int take = Math.min(available, (needed + units - 1) / units);
            needed = Math.max(0, needed - take * units);
            if (needed == 0) return 0;
        }
        return needed;
    }

    private static BreedingPolicy.Food foodKind(ItemStack stack) {
        return stack.is(Items.BREAD) ? BreedingPolicy.Food.BREAD
                : stack.is(Items.CARROT) || stack.is(Items.POTATO) || stack.is(Items.BEETROOT)
                ? BreedingPolicy.Food.ROOT : BreedingPolicy.Food.OTHER;
    }

    private static boolean unchanged(Villager v, FoodPlan plan) {
        for (int i = 0; i < plan.before().size(); i++)
            if (!ItemStack.matches(v.getInventory().getItem(i), plan.before().get(i))) return false;
        return true;
    }
    private static void consume(Villager v, FoodPlan plan) {
        for (int i = 0; i < plan.take().size(); i++) v.getInventory().removeItem(i, plan.take().get(i));
    }
    /** Authored towns span more than the ordinary sixteen-block nursery search. */
    static List<BlockPos> nurseries(ServerLevel level, Villager parent) {
        var town = WarScenario.active(level) ? WarCombat.home(parent) : null;
        int radius = town == null ? 16 : 64;
        return level.getPoiManager().findAll(type -> type.is(PoiTypes.HOME), p -> level.isLoaded(p)
                && (town == null || Math.abs(p.getX() - town.x()) <= local.villagers.war.WarMap.TOWN_RADIUS
                    && Math.abs(p.getZ() - town.z()) <= local.villagers.war.WarMap.TOWN_RADIUS),
                parent.blockPosition(), radius, PoiManager.Occupancy.HAS_SPACE)
            .filter(p -> !level.getBlockState(p).is(KingSizeBeds.BLOCK) && shelteredBed(level, p) && TraderRecruitment.allowedBed(level, p)).limit(8).toList();
    }
    static boolean shelteredBed(ServerLevel level, BlockPos pos) { return shelteredBed(level, pos, false); }
    private static boolean shelteredBed(ServerLevel level, BlockPos pos, boolean allowOccupied) {
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BedBlock) || state.getValue(BedBlock.PART) != BedPart.HEAD
                || !allowOccupied && state.getValue(BedBlock.OCCUPIED) || level.canSeeSky(pos.above())
                || !level.getBlockState(pos.above()).isAir() || !level.getBlockState(pos.above(2)).isAir()) return false;
        BlockPos foot = pos.relative(state.getValue(BedBlock.FACING).getOpposite());
        if (!level.isLoaded(foot)) return false;
        var footState = level.getBlockState(foot);
        if (!footState.is(state.getBlock()) || footState.getValue(BedBlock.PART) != BedPart.FOOT
                || footState.getValue(BedBlock.FACING) != state.getValue(BedBlock.FACING)
                || !allowOccupied && footState.getValue(BedBlock.OCCUPIED) || level.canSeeSky(foot.above())
                || !level.getBlockState(foot.above()).isAir() || !level.getBlockState(foot.above(2)).isAir()) return false;
        int doorRange = WarScenario.active(level) && local.villagers.war.WarMap.SETTLEMENTS.stream().anyMatch(town ->
            Math.abs(pos.getX() - town.x()) <= local.villagers.war.WarMap.TOWN_RADIUS
                && Math.abs(pos.getZ() - town.z()) <= local.villagers.war.WarMap.TOWN_RADIUS) ? 8 : 4;
        for (BlockPos door : BlockPos.betweenClosed(pos.offset(-doorRange, -1, -doorRange), pos.offset(doorRange, 2, doorRange)))
            if (level.isLoaded(door) && level.getBlockState(door).is(BlockTags.WOODEN_DOORS)
                    && level.getBlockState(door).getBlock() instanceof DoorBlock) return true;
        return false;
    }
    private static boolean birth(ServerLevel level, Villager a, Session first, FoodPlan costA,
                                 Villager b, Session second, FoodPlan costB, BlockPos bed, long now) {
        if (!consented(first, second, now) || !unchanged(a, costA) || !unchanged(b, costB) || !shelteredBed(level, bed) || !TraderRecruitment.allowedBed(level, bed)) return false;
        Villager baby = new Villager(EntityTypes.VILLAGER, level);
        boolean space = false;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            Vec3 pos = a.position().add(side.getStepX() * 1.3, 0, side.getStepZ() * 1.3);
            baby.snapTo(pos.x, pos.y, pos.z, 0, 0);
            if (level.noCollision(baby) && level.getFluidState(baby.blockPosition()).isEmpty()
                    && level.getBlockState(baby.blockPosition().below()).isFaceSturdy(level, baby.blockPosition().below(), Direction.UP)) {
                space = true; break;
            }
        }
        if (!space) return false;
        var reserved = level.getPoiManager().take(type -> type.is(PoiTypes.HOME), (type, point) -> point.equals(bed), bed, 1);
        if (reserved.isEmpty()) return false;
        baby.finalizeSpawn(level, level.getCurrentDifficultyAt(baby.blockPosition()), EntitySpawnReason.BREEDING, null);
        baby.setAge(AgeableMob.BABY_START_AGE);
        if (!level.addFreshEntity(baby)) { level.getPoiManager().release(bed); return false; }
        // Spawn callbacks may change parent inventory; abort before charging or installing a managed mind.
        if (!a.isAlive() || !b.isAlive() || !consented(first, second, now) || !unchanged(a, costA) || !unchanged(b, costB)) {
            baby.discard(); level.getPoiManager().release(bed); return false;
        }
        consume(a, costA); consume(b, costB);
        WarScenario.inheritTown(level, a, b, baby, bed);
        AgentMind child = AgentMind.born(baby.getUUID(), first.mind, second.mind, RANDOM);
        child.establishOrigin(Speech.originBiome(level, baby));
        child.home(new Home(level.dimension().identifier().toString(), bed.getX(), bed.getY(), bed.getZ()));
        baby.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(level.dimension(), bed));
        Session newborn = Villagers.registerNewborn(child, now);
        VillagerController.adopt(level, baby);
        first.mind.bred(now); second.mind.bred(now);
        a.setAge(6000); b.setAge(6000);
        first.familyFoodGoal = second.familyFoodGoal = false;
        first.courtship = second.courtship = null;
        first.familyStatus = second.familyStatus = "raising " + child.name();
        String parents = first.mind.name() + " and " + second.mind.name();
        child.recordEpisode(Episode.Kind.BORN, now, a.getUUID(), null, parents.substring(0, Math.min(parents.length(), Episode.MAX_DETAIL)),
                VillagerController.witnesses(level, baby));
        for (Session parent : List.of(first, second)) {
            parent.mind.recordEpisode(Episode.Kind.HAD_CHILD, now, child.id(), null, child.name(), VillagerController.witnesses(level, baby));
            Villagers.persist(parent);
        }
        Villagers.persist(newborn);
        if (level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS))
            net.minecraft.world.entity.ExperienceOrb.award(level, baby.position(), 1 + RANDOM.nextInt(7));
        level.sendParticles(ParticleTypes.HEART, baby.getX(), baby.getY() + 1, baby.getZ(), 8, .4, .4, .4, 0);
        Villagers.LOG.info("Ordinary birth {} ({}) parents={} and {} nursery={}", child.name(), child.id(), a.getUUID(), b.getUUID(), bed);
        return true;
    }


    /** Interrupted visits never charge food, spawn children, or award XP. */
    static void interrupt(java.util.UUID id) {
        BedVisit visit = VISITS.get(id);
        if (visit != null) endVisit(visit);
    }

    private static void endVisit(BedVisit visit) {
        VISITS.remove(visit.a, visit); VISITS.remove(visit.b, visit);
        for (java.util.UUID id : List.of(visit.a, visit.b)) {
            if (visit.level.getEntity(id) instanceof Villager parent) {
                if (parent.isSleeping()) parent.stopSleeping();
                parent.getNavigation().stop();
            }
        }
    }

    private static BlockPos kingBed(ServerLevel level, Villager a, Villager b) {
        return level.getPoiManager().findAll(type -> type.is(PoiTypes.HOME), level::isLoaded,
                a.blockPosition(), 16, PoiManager.Occupancy.ANY)
            .filter(pos -> level.getBlockState(pos).is(KingSizeBeds.BLOCK)
                && !level.getBlockState(pos).getValue(local.villagers.block.KingSizeBedBlock.RIGHT))
            .limit(8).filter(pos -> availableKing(level, pos, a, b)).findFirst().orElse(null);
    }

    private static boolean availableKing(ServerLevel level, BlockPos bed, Villager a, Villager b) {
        if (!level.isLoaded(bed) || !level.getBlockState(bed).is(KingSizeBeds.BLOCK)) return false;
        BlockPos right = bed.relative(level.getBlockState(bed).getValue(BedBlock.FACING).getClockWise());
        if (!shelteredBed(level, bed) || !shelteredBed(level, right)
                || !TraderRecruitment.canClaim(level, a, bed) || !TraderRecruitment.canClaim(level, b, right)) return false;
        for (BedVisit other : VISITS.values()) if (other.level == level && other.bed.equals(bed)) return false;
        for (Session resident : Villagers.sessions()) {
            Home home = resident.mind.home();
            if (home != null && home.dimension().equals(level.dimension().identifier().toString())
                    && (new BlockPos(home.x(), home.y(), home.z()).equals(bed) || new BlockPos(home.x(), home.y(), home.z()).equals(right))
                    && !resident.mind.id().equals(a.getUUID()) && !resident.mind.id().equals(b.getUUID())) return false;
        }
        return true;
    }

    /** Ordinary sleeping poses and hearts provide a discreet, non-explicit courtship cutaway. */
    static boolean tickVisit(ServerLevel level, Villager actor, long now, boolean threatened) {
        BedVisit visit = VISITS.get(actor.getUUID());
        if (visit == null) return false;
        if (visit.level != level || threatened || !Villagers.config().breeding() || level.isDarkOutside()
                || now - visit.started > 600) { endVisit(visit); return false; }
        if (!(level.getEntity(visit.a) instanceof Villager a) || !(level.getEntity(visit.b) instanceof Villager b)) {
            endVisit(visit); return false;
        }
        Session first = Villagers.existingSession(visit.a), second = Villagers.existingSession(visit.b);
        if (first == null || second == null || !consented(first, second, now)
                || !visitParent(a, first, b, now) || !visitParent(b, second, a, now)
                || !intactKing(level, visit) || !shelteredBed(level, visit.nursery)
                || level.getEntitiesOfClass(Villager.class, a.getBoundingBox().inflate(32), Villager::isAlive).size() >= BreedingPolicy.LOCAL_POPULATION_CAP
                || !level.getEntitiesOfClass(Monster.class, a.getBoundingBox().inflate(16), m -> m.isAlive()
                    && (a.hasLineOfSight(m) || b.hasLineOfSight(m))).isEmpty()) {
            endVisit(visit); return false;
        }
        BlockPos right = visit.bed.relative(level.getBlockState(visit.bed).getValue(BedBlock.FACING).getClockWise());
        BlockPos target = actor.getUUID().equals(visit.a) ? visit.bed : right;
        if (visit.together < 0) {
            if (actor.distanceToSqr(Vec3.atBottomCenterOf(target)) > 4) {
                if (actor.getNavigation().isDone()) actor.getNavigation().moveTo(target.getX() + .5, target.getY(), target.getZ() + .5, .5);
            } else actor.getNavigation().stop();
            actor.getLookControl().setLookAt(actor == a ? b : a, 30, 30);
            if (a.distanceToSqr(Vec3.atBottomCenterOf(visit.bed)) <= 4 && b.distanceToSqr(Vec3.atBottomCenterOf(right)) <= 4) {
                a.getNavigation().stop(); b.getNavigation().stop();
                a.startSleeping(visit.bed); b.startSleeping(right);
                visit.together = now;
            }
        } else {
            if (!a.getSleepingPos().filter(visit.bed::equals).isPresent()
                    || !b.getSleepingPos().filter(right::equals).isPresent()) { endVisit(visit); return false; }
            if (now >= visit.nextEffect) {
                visit.nextEffect = now + 30;
                level.sendParticles(ParticleTypes.HEART, visit.bed.getX() + .5, visit.bed.getY() + 1.2, visit.bed.getZ() + .5, 4, .6, .2, .6, 0);
                actor.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_AMBIENT, .35f, 1f);
            }
            if (now - visit.together >= 100) {
                endVisit(visit);
                FoodPlan costA = foodPlan(a), costB = foodPlan(b);
                if (costA != null && costB != null) birth(level, a, first, costA, b, second, costB, visit.nursery, now);
                return false;
            }
        }
        first.familyStatus = second.familyStatus = "courtship at the King Size Bed";
        return true;
    }

    private static boolean visitParent(Villager parent, Session session, Villager partner, long now) {
        return Villagers.isManaged(parent) && parent.isAlive() && parent.getAge() == 0 && !parent.isTrading()
                && !parent.isOnFire() && !parent.isInWater() && parent.getAirSupply() >= 200
                && willing(parent, session, session.mind.opinionOf(partner.getUUID()).regard(), now) && hasFamilyFood(parent);
    }

    private static boolean intactKing(ServerLevel level, BedVisit visit) {
        if (!level.isLoaded(visit.bed) || !level.getBlockState(visit.bed).is(KingSizeBeds.BLOCK)) return false;
        var state = level.getBlockState(visit.bed);
        for (int lane = 0; lane < 2; lane++) for (int part = 0; part < 2; part++) {
            BlockPos pos = visit.bed.relative(state.getValue(BedBlock.FACING).getClockWise(), lane)
                    .relative(state.getValue(BedBlock.FACING).getOpposite(), part);
            if (!level.isLoaded(pos)) return false;
            var actual = level.getBlockState(pos);
            if (!actual.is(KingSizeBeds.BLOCK) || actual.getValue(BedBlock.FACING) != state.getValue(BedBlock.FACING)
                    || actual.getValue(local.villagers.block.KingSizeBedBlock.RIGHT) != (lane == 1)
                    || actual.getValue(BedBlock.PART) != (part == 0 ? BedPart.HEAD : BedPart.FOOT)) return false;
        }
        BlockPos right = visit.bed.relative(state.getValue(BedBlock.FACING).getClockWise());
        return shelteredBed(level, visit.bed, true) && shelteredBed(level, right, true)
                && TraderRecruitment.allowedBed(level, visit.bed) && TraderRecruitment.allowedBed(level, right);
    }

    static boolean consented(Session first, Session second, long now) {
        return PartnershipLife.mutual(first,second)&&first.mind.partnerships().active().agreedAt()<=now&&Courtship.mutual(first.mind.id(), first.courtship, second.mind.id(), second.courtship, now);
    }
}
