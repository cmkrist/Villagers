package local.villagers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.random.RandomGenerator;
import local.villagers.mind.AgentMind;
import local.villagers.mind.Ancestry;
import local.villagers.persistence.MindPersistence;
import local.villagers.persistence.RecoveryCheckpoint;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import local.villagers.mind.Death;
import local.villagers.mind.Episode;
import local.villagers.mind.Home;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ChunkPos;

/**
 * Deaths of managed villagers, and divine succession: when the last villager of a village starves, the
 * longest-lived villager the village ever had (living or dead) gets a god child, a baby carrying a mutated copy
 * of that ancestor's genome, so the bloodline and the village continue. Only starvation triggers it.
 */
final class GodChildren {
    /** A village is every villager, and every remembered home, within this many blocks of the deceased's home. */
    static final int VILLAGE_RADIUS = 64;
    private static final RandomGenerator RANDOM = new Random();
    private static final int MAX_PENDING = RecoveryCheckpoint.LIMIT;
    private static final List<Recovery> PENDING = new ArrayList<>();
    private static Recovery active;
    private static boolean overflowReported;
    private static CompletableFuture<List<MindPersistence.RestoredRecovery>> restoring;
    private static boolean bootReady, checkpointUsable;
    private static final class Recovery {
        ServerLevel level;
        final String dimension;
        final UUID savedDeceased;
        final Vec3 center;
        AgentMind.Snapshot deceased;
        Vec3 at;
        CompletableFuture<AgentMind.Snapshot> lookup;
        AgentMind.Snapshot champion;
        boolean abandoned;
        long retryAt, nextResidencyCheck;
        Recovery(ServerLevel level, AgentMind.Snapshot deceased, Vec3 at, Vec3 center) {
            this.level = level; this.dimension = dimensionId(level); this.savedDeceased = deceased.id();
            this.deceased = deceased; this.at = at; this.center = center;
        }
        Recovery(ServerLevel level, RecoveryCheckpoint.Ticket ticket, AgentMind.Snapshot deceased) {
            this.level = level; this.dimension = ticket.dimension(); this.savedDeceased = ticket.deceased();
            this.deceased = deceased; this.at = new Vec3(ticket.x(), ticket.y(), ticket.z());
            this.center = new Vec3(ticket.centerX(), ticket.centerY(), ticket.centerZ());
        }
        RecoveryCheckpoint.Ticket ticket() {
            return new RecoveryCheckpoint.Ticket(dimension, deceased == null ? savedDeceased : deceased.id(),
                    at.x, at.y, at.z, center.x, center.y, center.z);
        }
    }
    private static boolean initialized;

    private GodChildren() {}

    static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof Villager villager) || !(entity.level() instanceof ServerLevel level)
                    || !Villagers.isManaged(villager) || !Villagers.ready()) return;
            Session s = Villagers.existingSession(villager.getUUID());
            if (s == null) return; // Never ticked, so it cannot have starved under our metabolism.
            s.mind.die(level.getGameTime(), source.getMsgId());
            Villagers.persist(s);
            if (source.is(DamageTypes.STARVE) && Villagers.config().godChildren()
                    && !(level.getChunkSource().getGenerator() instanceof local.villagers.war.WarChunkGenerator)) {
                // Only admit immutable recovery state here. Survivors are checked after all entity ticks.
                enqueue(level, s.mind.snapshot(), villager.position(), villageCenter(level, s.mind, villager.position()));
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(GodChildren::process);
    }

    static void clear() {
        PENDING.clear(); active = null; overflowReported = false;
        restoring = null; bootReady = false; checkpointUsable = false;
    }

    static void start(MindPersistence persistence) {
        if (!Villagers.config().godChildren()) { bootReady = true; return; } // Preserve dormant file without reading or replacing it.
        restoring = persistence.restoreRecoveries();
    }
    static boolean ready() { return bootReady; }

    /** Coalesced immutable state only; persistence owns disk access, retry and final shutdown drain. */
    static void checkpoint() {
        if (!checkpointUsable) return;
        try { Villagers.checkpointRecoveries(PENDING.stream().map(Recovery::ticket).toList()); }
        catch (RuntimeException failure) {
            checkpointUsable = false;
            Villagers.LOG.error("Could not queue divine recovery checkpoint; recovery disabled for this world session, no completion claimed", failure);
        }
    }

    private static void restore(MinecraftServer server) {
        if (restoring == null || !restoring.isDone()) return;
        try {
            for (var record : restoring.join()) {
                ServerLevel level = null;
                for (ServerLevel candidate : server.getAllLevels())
                    if (dimensionId(candidate).equals(record.ticket().dimension())) { level = candidate; break; }
                AgentMind.Snapshot deceased = record.deceased();
                if (deceased == null || deceased.death() == null || !"starve".equals(deceased.death().cause())) {
                    deceased = null;
                    Villagers.LOG.error("Pending recovery {} has no readable starvation history; retaining checkpoint without inventing an ancestor", record.ticket().deceased());
                }
                if (level == null) Villagers.LOG.warn("Retaining divine recovery in unavailable dimension {}", record.ticket().dimension());
                PENDING.add(new Recovery(level, record.ticket(), deceased));
            }
            checkpointUsable = true;
        } catch (RuntimeException failure) {
            Villagers.LOG.error("Divine recovery checkpoint could not be restored; recovery is disabled for this world session and its file is left untouched", failure);
        }
        restoring = null; bootReady = true;
    }

    private static void enqueue(ServerLevel level, AgentMind.Snapshot deceased, Vec3 at, Vec3 center) {
        if (!checkpointUsable) return;
        for (Recovery request : PENDING) {
            if (request.dimension.equals(dimensionId(level)) && request.center.distanceToSqr(center) <= VILLAGE_RADIUS * VILLAGE_RADIUS) {
                request.level = level;
                request.abandoned = false; // A new starvation is a fresh recovery trigger after a prior survivor arrival.
                AgentMind.Snapshot best = request.deceased == null ? deceased : Ancestry.better(request.deceased, deceased);
                if (best == deceased) { request.deceased = deceased; request.at = at; }
                if (request.champion != null) request.champion = Ancestry.better(request.champion, deceased);
                checkpoint();
                return;
            }
        }
        if (PENDING.size() >= MAX_PENDING) {
            if (!overflowReported) Villagers.LOG.error("Divine recovery backlog full ({} village requests); further recoveries are not admitted until it drains. Death records remain saved.", MAX_PENDING);
            overflowReported = true;
            return;
        }
        PENDING.add(new Recovery(level, deceased, at, center));
        checkpoint();
    }

    private static boolean survivors(Recovery request) {
        return request.level != null && !request.level.getEntitiesOfClass(Villager.class, new AABB(request.center, request.center).inflate(VILLAGE_RADIUS),
                Villager::isAlive).isEmpty();
    }

    /** Missing chunks cannot be mistaken for extinction; never force them loaded. At most81 checks/40ticks. */
    private static boolean resident(Recovery request) {
        if (request.level == null || request.level.getGameTime() < request.nextResidencyCheck) return false;
        request.nextResidencyCheck = request.level.getGameTime() + 40;
        BlockPos center = BlockPos.containing(request.center);
        for (int x = Math.floorDiv(center.getX() - VILLAGE_RADIUS, 16); x <= Math.floorDiv(center.getX() + VILLAGE_RADIUS, 16); x++)
            for (int z = Math.floorDiv(center.getZ() - VILLAGE_RADIUS, 16); z <= Math.floorDiv(center.getZ() + VILLAGE_RADIUS, 16); z++)
                if (!request.level.areEntitiesActuallyLoadedAndTicking(new ChunkPos(x, z))) return false;
        return request.level.isPositionEntityTicking(BlockPos.containing(request.at));
    }

    private static void process(MinecraftServer server) {
        if (!bootReady) restore(server);
        if (!Villagers.ready() || !Villagers.config().godChildren() || !checkpointUsable) return;
        boolean changed = false;
        if (active != null) {
            if (survivors(active)) active.abandoned = true;
            // Do not cancel or replace a still-running lookup: cancellation must not queue unlimited hidden IO.
            if (!active.lookup.isDone()) return;
            try {
                AgentMind.Snapshot ancestor = Ancestry.better(active.lookup.join(), active.deceased);
                if (!active.abandoned && !survivors(active)) active.champion = ancestor;
                if (active.champion == null) { PENDING.remove(active); changed = true; }
            } catch (RuntimeException failure) {
                Villagers.LOG.error("Divine ancestry lookup failed; no speculative replacement was spawned", failure);
                active.lookup = null;
                active.retryAt = active.level.getGameTime() + 200;
            }
            active = null;
        }
        changed |= PENDING.removeIf(GodChildren::survivors);
        var completed = PENDING.iterator();
        while (completed.hasNext()) {
            Recovery request = completed.next();
            if (request.champion != null && request.level.getGameTime() >= request.retryAt && resident(request)) {
                // Wait for natural residency; ancestry completion must not load a departed village.
                if (survivors(request) || spawn(request.level,
                        AgentMind.fromSnapshot(Ancestry.better(request.champion, request.deceased)), request.at)) {
                    completed.remove(); changed = true;
                } else request.retryAt = request.level.getGameTime() + 200;
            }
        }
        if (changed) checkpoint();
        if (PENDING.size() < MAX_PENDING) overflowReported = false;
        if (PENDING.isEmpty()) return;
        Recovery next = PENDING.stream().filter(request -> request.lookup == null && request.champion == null
                && request.deceased != null && request.level != null && request.level.getGameTime() >= request.retryAt
                && resident(request)).findFirst().orElse(null);
        if (next == null) return;
        Ancestry.Neighborhood query = new Ancestry.Neighborhood(dimensionId(next.level), next.center.x, next.center.y,
                next.center.z, VILLAGE_RADIUS, next.deceased.id());
        AgentMind.Snapshot best = next.deceased;
        AgentMind liveBest = null;
        var liveIds = new HashSet<UUID>();
        liveIds.add(next.deceased.id());
        for (Session session : Villagers.sessions()) {
            AgentMind mind = session.mind;
            liveIds.add(mind.id());
            if (query.includes(mind.id(), mind.home()) && (liveBest == null || Ancestry.ORDER.compare(mind, liveBest) > 0))
                liveBest = mind;
        }
        if (liveBest != null) best = Ancestry.better(best, liveBest.snapshot());
        next.lookup = Villagers.ancestor(query, best, liveIds);
        active = next;
    }

    /** The deceased's home if it is in this dimension, else where it died. */
    private static Vec3 villageCenter(ServerLevel level, AgentMind mind, Vec3 deathPos) {
        Home home = mind.home();
        return home != null && home.dimension().equals(dimensionId(level))
                ? Vec3.atCenterOf(new BlockPos(home.x(), home.y(), home.z())) : deathPos;
    }

    private static boolean spawn(ServerLevel level, AgentMind ancestor, Vec3 at) {
        Villager baby = new Villager(EntityTypes.VILLAGER, level);
        baby.snapTo(at.x, at.y, at.z, RANDOM.nextFloat() * 360, 0);
        baby.finalizeSpawn(level, level.getCurrentDifficultyAt(baby.blockPosition()), EntitySpawnReason.BREEDING, null);
        baby.setAge(AgeableMob.BABY_START_AGE);
        AgentMind child = AgentMind.godChild(baby.getUUID(), ancestor, RANDOM);
        Session session = Villagers.registerNewborn(child, level.getGameTime());
        if (!level.addFreshEntity(baby)) {
            Villagers.removeSession(baby.getUUID());
            Villagers.LOG.warn("Could not add the god child of {} at {}", ancestor.name(), BlockPos.containing(at).toShortString());
            return false;
        }
        VillagerController.adopt(level, baby);
        child.recordEpisode(Episode.Kind.GOD_CHILD, level.getGameTime(), ancestor.id(), null, ancestor.name(),
                VillagerController.witnesses(level, baby));
        Villagers.persist(session);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, at.x, at.y + 1, at.z, 40, .4, .6, .4, .3);
        Component message = Component.literal("The village starved. The gods sent " + child.name()
                + " to carry on " + ancestor.name() + "'s line (generation " + child.generation() + ").");
        level.getPlayers(p -> p.position().distanceTo(at) <= VILLAGE_RADIUS * 2).forEach(p -> p.sendSystemMessage(message));
        Villagers.LOG.info("God child {} ({}) continues {} ({}, lived {} ticks{}), generation {}, at {}",
                child.name(), child.id(), ancestor.name(), ancestor.id(), ancestor.ageTicks(),
                ancestor.death() == null ? "" : ", " + describe(ancestor.death()), child.generation(),
                BlockPos.containing(at).toShortString());
        return true;
    }

    private static String describe(Death death) { return "died of " + death.cause(); }

    private static String dimensionId(ServerLevel level) { return level.dimension().identifier().toString(); }
}
