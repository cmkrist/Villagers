package local.villagers;

import local.villagers.mind.AgentMind;
import local.villagers.mind.Home;
import local.villagers.mind.Observation;
import local.villagers.mind.PlayerImpression;
import local.villagers.mind.PlayerImpression.Evidence;
import local.villagers.mind.PlayerImpression.Reaction;
import local.villagers.social.SocialBehavior;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Grounded player encounters. All methods/hooks run on the server thread and leave survival first. */
public final class SocialReactions {
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static boolean initialized;
    private static final class State {
        UUID player;
        Reaction reaction = Reaction.NEUTRAL;
        long activeUntil, nextEpisodeAt, nextExposureAt;
        boolean ownsNavigation;
        Path ownedPath;
        long attentionEpisodes, approachEpisodes, avoidanceEpisodes, rejectedPaths;
    }
    private SocialReactions() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (!(entity instanceof Villager villager) || !(entity.level() instanceof ServerLevel level)
                    || !villager.entityTags().contains(Villagers.MANAGED) || damageTaken <= 0
                    || !(source.getEntity() instanceof ServerPlayer player)) return;
            Villagers.observePlayerEvent(level, villager, player.getUUID(), Evidence.PLAYER_HURT);
            State state = STATES.get(villager.getUUID());
            if (state != null) {
                stop(villager, state);
                state.nextEpisodeAt = Math.max(state.nextEpisodeAt, level.getGameTime() + 100);
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof Monster monster) || !(entity.level() instanceof ServerLevel level)
                    || !(source.getEntity() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) return;
            for (Villager witness : level.getEntitiesOfClass(Villager.class, monster.getBoundingBox().inflate(12),
                    villager -> villager.isAlive() && villager.entityTags().contains(Villagers.MANAGED))) {
                boolean targetingWitness = monster.getTarget() == witness;
                boolean recentAttacker = witness.getLastHurtByMob() == monster
                    && witness.tickCount - witness.getLastHurtByMobTimestamp() < 100;
                if ((!targetingWitness && !recentAttacker) || witness.distanceToSqr(monster) > 144
                        || witness.distanceToSqr(player) > 144 || !witness.hasLineOfSight(monster)
                        || !witness.hasLineOfSight(player)) continue;
                Villagers.observePlayerEvent(level, witness, player.getUUID(), Evidence.WITNESSED_PROTECTION);
            }
        });
    }

    /** Nonnull owns this short episode: root must discard pending elective credit, never reward its interruption. */
    public static String tick(ServerLevel level, Villager villager, AgentMind mind, Observation observation) {
        if (!villager.isAlive() || villager.level() != level || !villager.entityTags().contains(Villagers.MANAGED)) return null;
        State state = STATES.computeIfAbsent(villager.getUUID(), ignored -> new State());
        long now = level.getGameTime();
        if (!SocialBehavior.mayEngage(observation, villager.isTrading(), villager.isSleeping()) || villager.isOnFire()
                || villager.isInWater() || villager.getAirSupply() < 200) {
            stop(villager, state);
            return null;
        }
        ServerPlayer player = level.getEntitiesOfClass(ServerPlayer.class, villager.getBoundingBox().inflate(10),
            person -> person.isAlive() && !person.isSpectator() && !person.isInvisible()
                && villager.distanceToSqr(person) <= 100 && villager.hasLineOfSight(person))
            .stream().min(Comparator.comparingDouble(villager::distanceToSqr)).orElse(null);
        if (player == null) { stop(villager, state); return null; }
        if (now >= state.nextExposureAt || !player.getUUID().equals(state.player)) {
            Villagers.observePlayerEvent(level, villager, player.getUUID(), Evidence.EXPOSURE);
            state.nextExposureAt = now + 40;
        }
        if (state.activeUntil > now) {
            if (!player.getUUID().equals(state.player)) { stop(villager, state); return null; }
            // Do not keep walking into personal space if the player approaches during the episode.
            if (state.reaction == Reaction.APPROACH && villager.distanceToSqr(player) <= 9) {
                if (villager.getNavigation().getPath() == state.ownedPath) villager.getNavigation().stop();
                state.ownsNavigation = false; state.ownedPath = null;
            }
            villager.getLookControl().setLookAt(player, 30, 25);
            return label(state.reaction);
        }
        stop(villager, state);
        if (!SocialBehavior.episodeDue(now, state.nextEpisodeAt)) return null;
        PlayerImpression impression = mind.playerImpression(player.getUUID());
        Reaction reaction = impression.reaction(observation);
        if (reaction == Reaction.NEUTRAL) return null;
        state.player = player.getUUID();
        state.reaction = reaction;
        state.activeUntil = now + SocialBehavior.EPISODE_TICKS;
        state.nextEpisodeAt = now + SocialBehavior.EPISODE_INTERVAL_TICKS;
        villager.getNavigation().stop();
        villager.getLookControl().setLookAt(player, 30, 25);
        if (reaction == Reaction.ATTEND) state.attentionEpisodes++;
        else {
            Vec3 direction = player.position().subtract(villager.position()).multiply(1, 0, 1);
            double separation = direction.length();
            if (separation > .01 && Math.abs(player.getY() - villager.getY()) <= 2) {
                double step = reaction == Reaction.APPROACH ? SocialBehavior.approachStep(separation) : SocialBehavior.MAX_STEP;
                Vec3 destination = villager.position().add(direction.normalize().scale(reaction == Reaction.AVOID ? -step : step));
                if (step > .1 && safeHomeDestination(level, mind, destination)) {
                    Path path = villager.getNavigation().createPath(BlockPos.containing(destination), 0);
                    if (safePath(level, mind, path)) {
                        state.ownsNavigation = villager.getNavigation().moveTo(path, reaction == Reaction.AVOID ? .65 : .4);
                        state.ownedPath = state.ownsNavigation ? villager.getNavigation().getPath() : null;
                        if (state.ownsNavigation) {
                            if (reaction == Reaction.APPROACH) state.approachEpisodes++;
                            else state.avoidanceEpisodes++;
                        }
                    } else state.rejectedPaths++;
                }
            }
        }
        return label(reaction);
    }

    private static String label(Reaction reaction) {
        return switch (reaction) {
            case ATTEND -> "watching powerful visitor";
            case APPROACH -> "greeting trusted visitor";
            case AVOID -> "keeping distance from harmful visitor";
            case NEUTRAL -> "noticing visitor";
        };
    }

    private static boolean safeHomeDestination(ServerLevel level, AgentMind mind, Vec3 destination) {
        Home home = mind.home();
        // Without a known local home, attention is possible but social wandering is not authorized.
        if (home == null || !home.dimension().equals(level.dimension().identifier().toString())) return false;
        return SocialBehavior.withinHome(Vec3.atCenterOf(new BlockPos(home.x(), home.y(), home.z())).distanceTo(destination));
    }

    private static boolean safePath(ServerLevel level, AgentMind mind, Path path) {
        if (path == null || !path.canReach() || path.getNodeCount() > 16) return false;
        double distance = 0;
        BlockPos previous = null;
        for (int index = 0; index < path.getNodeCount(); index++) {
            BlockPos node = path.getNodePos(index);
            if (!level.isLoaded(node) || !safeHomeDestination(level, mind, Vec3.atCenterOf(node))) return false;
            for (BlockPos check : new BlockPos[]{node.below(), node, node.above()}) {
                var block = level.getBlockState(check);
                if (!block.getFluidState().isEmpty() || block.is(BlockTags.FIRE) || block.is(Blocks.MAGMA_BLOCK)
                        || block.is(Blocks.CACTUS) || block.is(Blocks.SWEET_BERRY_BUSH)
                        || block.is(Blocks.CAMPFIRE) || block.is(Blocks.SOUL_CAMPFIRE)) return false;
            }
            if (previous != null) {
                if (Math.abs(node.getY() - previous.getY()) > 1) return false;
                distance += Math.sqrt(node.distSqr(previous));
            }
            previous = node;
        }
        return distance <= 4;
    }

    private static void stop(Villager villager, State state) {
        if (state.ownsNavigation && villager.getNavigation().getPath() == state.ownedPath) villager.getNavigation().stop();
        state.ownsNavigation = false;
        state.ownedPath = null;
        state.activeUntil = 0;
        state.reaction = Reaction.NEUTRAL;
    }

    public static Map<String, Object> status(UUID id) {
        State state = STATES.get(id);
        if (state == null) return Map.of("reaction", "NEUTRAL");
        return Map.of("reaction", state.reaction.name(), "active_until", state.activeUntil,
            "next_episode_at", state.nextEpisodeAt, "attention_episodes", state.attentionEpisodes,
            "approach_episodes", state.approachEpisodes, "avoidance_episodes", state.avoidanceEpisodes,
            "rejected_paths", state.rejectedPaths);
    }
    public static void forget(UUID id) { STATES.remove(id); }
    public static void clear() { STATES.clear(); }
}
