package local.villagers;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import local.villagers.config.VillagersConfig;
import local.villagers.learning.LearningMode;
import local.villagers.mind.AgentMind;
import local.villagers.mind.AgentStore;
import local.villagers.mind.PlayerImpression;
import local.villagers.persistence.MindPersistence;
import net.fabricmc.api.ModInitializer;
import java.nio.file.Path;
import net.minecraft.world.entity.monster.Monster;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entrypoint. Owns server lifecycle wiring, the live session registry and the small public API used by
 * the mixin and other subsystems. Decision making lives in {@link VillagerController}; commands live in
 * {@link VillagerCommands} and, for development only, {@link LabCommands}.
 */
public class Villagers implements ModInitializer {
    public static final String MANAGED = "villagers:managed";
    public static final LearningMode LEARNING_MODE = LearningMode.parse(System.getProperty("villagers.learning", "adaptive"));
    static final Logger LOG = LoggerFactory.getLogger("villagers");
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static MindPersistence persistence;
    private static VillagersConfig config = VillagersConfig.defaults();

    @Override public void onInitialize() {
        local.villagers.war.WarWorldgen.initialize();
        KingSizeBeds.initialize();
        var loaded = VillagersConfig.load(FabricLoader.getInstance().getConfigDir().resolve("villagers.json"));
        config = loaded.config();
        loaded.problems().forEach(problem -> LOG.warn("Config: {}", problem));
        TrainingTrial.initialize();
        SocialReactions.initialize();
        GodChildren.initialize();
        TraderRecruitment.initialize();
        CreatureAgents.initialize();
        VocalSpeech.initialize();
        WarScenario.initialize();
        NativeRegionsScenario.initialize();
        NativeIllagers.initialize();
        WarCampaign.initialize();
        // Like a player, a hurt villager burns the damage type's food exhaustion (0.1 for most attacks).
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (entity instanceof Villager villager && damageTaken > 0) exert(villager, source.getFoodExhaustion());
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            // A villager that kills a monster attacking a neighbour has protected that neighbour.
            if (!(entity instanceof Monster monster) || !(entity.level() instanceof ServerLevel level)
                    || !(source.getEntity() instanceof Villager defender)) return;
            for (Villager victim : level.getEntitiesOfClass(Villager.class, monster.getBoundingBox().inflate(12),
                    candidate -> candidate != defender && candidate.isAlive() && isManaged(candidate))) {
                boolean attacked = monster.getTarget() == victim || (victim.getLastHurtByMob() == monster
                        && victim.tickCount - victim.getLastHurtByMobTimestamp() < 100);
                Session session = SESSIONS.get(victim.getUUID());
                if (attacked && session != null && LEARNING_MODE != LearningMode.FROZEN)
                    session.mind.witness(defender.getUUID(), local.villagers.relations.Opinion.Event.PROTECTED, level.getGameTime());
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Speech.join(handler.getPlayer().getUUID()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Speech.leave(handler.getPlayer().getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(server -> { if (server.getTickCount() % 6000 == 0) Speech.flush(); });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Path villagersRoot = server.getWorldPath(LevelResource.ROOT).resolve("villagers");
            AgentStore store = AgentStore.worldScoped(villagersRoot);
            persistence = new MindPersistence(store, LOG, Duration.ofSeconds(1));
            Speech.start(villagersRoot);
            clearRuntime();
            ConstructionSites.start(server);
            GodChildren.start(persistence);
            VillagerController.resetStatistics();
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            Session session = SESSIONS.remove(entity.getUUID());
            if (session != null) persist(session);
            forgetRuntime(entity.getUUID());
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ConstructionSites.stop();
            SESSIONS.values().forEach(Villagers::persist);
            GodChildren.checkpoint();
            if (persistence != null && !persistence.close(Duration.ofSeconds(15)))
                LOG.error("Villager mind/recovery saves failed or are still pending at shutdown");
            persistence = null;
            Speech.stop();
            clearRuntime();
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> {
            VillagerCommands.register(dispatcher);
            CreatureCommands.register(dispatcher);
            AdventurePreparation.register(dispatcher);
            VillagerCommands.registerPlayerCommands(dispatcher);
            ConstructionSites.register(dispatcher);
            if (!DevMode.enabled()) return;
            LabCommands.register(dispatcher);
            NativeExploration.register(dispatcher);
            NativeIllagers.register(dispatcher);
            ConstructionProbe.register(dispatcher);
            VillageTrial.register(dispatcher);
            VillageToolProbe.register(dispatcher);
            VillageCraftProbe.register(dispatcher);
            WoodProductionProbe.register(dispatcher);
            StationProductionProbe.register(dispatcher);
            HomeRouteProbe.register(dispatcher);
            ForestryProbe.register(dispatcher);
            VillageFamilyProbe.register(dispatcher);
            TrainingTrial.register(dispatcher);
            VillageSocialProbe.register(dispatcher);
        });
        NativeCareStartup.initialize();
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!(entity instanceof Villager villager) || !isManaged(villager) || !player.isShiftKeyDown())
                return InteractionResult.PASS;
            if (level.isClientSide()) return InteractionResult.PASS;
            if (player.getItemInHand(hand).isEmpty()) {
                // Talking with an empty hand: hear what it remembers. Only once per click (main hand).
                if (hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer serverPlayer) || !ready()
                        || player.distanceToSqr(villager) > 9) return InteractionResult.PASS;
                VillagerController.tellMemories((ServerLevel) level, villager, serverPlayer);
                return InteractionResult.SUCCESS;
            }
            return offerGift((ServerLevel) level, villager, player, hand) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
    }

    // ---- Public API used by the mixin and other subsystems ----

    /** Mixin hook: runs in place of the vanilla brain for adopted villagers. */
    public static void tick(ServerLevel level, Villager villager) { VillagerController.tick(level, villager); }
    /** Native decisions continue while an adopted mind loads asynchronously. */
    public static boolean tickIfReady(ServerLevel level,Villager villager){return VillagerController.tick(level,villager);}

    public static VillagersConfig config() { return config; }

    public static boolean isManaged(Villager villager) { return villager.entityTags().contains(MANAGED); }

    public static void adopt(ServerLevel level, Villager villager) { VillagerController.adopt(level, villager); }

    public static boolean offerGift(ServerLevel level, Villager villager, Player player, InteractionHand hand) {
        return VillagerController.offerGift(level, villager, player, hand);
    }

    /** Called only after a grounded native event; frozen controls retain their checkpoint impressions. */
    public static boolean observePlayerEvent(ServerLevel level, Villager villager, UUID player, PlayerImpression.Evidence evidence) {
        if (!ready() || !villager.isAlive() || !isManaged(villager) || LEARNING_MODE == LearningMode.FROZEN) return false;
        Session session = session(villager, level);
        List<UUID> others = VillagerController.witnesses(level, villager).stream().filter(id -> !id.equals(player)).toList();
        boolean accepted = session.mind.observePlayer(player, evidence, level.getGameTime(), others);
        if (accepted) persist(session);
        return accepted;
    }

    public static PlayerImpression impressionFor(ServerLevel level, Villager villager, UUID player) {
        return session(villager, level).mind.playerImpression(player);
    }

    /** Explicit trial request using the ordinary interruptible action executor, never an actor teleport. */
    public static void requestHarvestForProbe(ServerLevel level, Villager villager, BlockPos crop) {
        if (!DevMode.inWorld(level.getServer(), DevMode.TRIAL_WORLD) || !isManaged(villager)) return;
        VillagerController.requestHarvest(level, villager, session(villager, level), crop);
    }

    /** Food exhaustion from real activity, for a managed villager whose mind is loaded. */
    public static void exert(Villager villager, float exhaustion) {
        if (!isManaged(villager)) return;
        Session s = SESSIONS.get(villager.getUUID());
        if (s != null) s.mind.exert(exhaustion);
    }

    public static Map<String, Object> learningStatistics() { return VillagerController.statistics(SESSIONS.values()); }

    // ---- Session registry (server thread only) ----

    static boolean ready() { return persistence != null && GodChildren.ready(); }

    static void checkpointRecoveries(List<local.villagers.persistence.RecoveryCheckpoint.Ticket> requests) {
        if (persistence != null) persistence.queueRecoveries(requests);
    }

    /** Blocking lookup for rare paths (commands, gifts). Loads from disk if needed. */
    static Session session(Villager villager, ServerLevel level) {
        return SESSIONS.computeIfAbsent(villager.getUUID(),
                id -> new Session(persistence.acquire(id), level.getGameTime()));
    }

    /** Non-blocking lookup for the tick path; null while the mind is still loading in the background. */
    static Session sessionIfLoaded(Villager villager, ServerLevel level) {
        Session existing = SESSIONS.get(villager.getUUID());
        if (existing != null) return existing;
        AgentMind mind = persistence.tryAcquire(villager.getUUID());
        if (mind == null) return null;
        Session created = new Session(mind, level.getGameTime());
        SESSIONS.put(villager.getUUID(), created);
        return created;
    }

    static Session existingSession(UUID id) { return SESSIONS.get(id); }

    /** Install a freshly created mind for an entity that has never had one, before it is adopted. */
    static Session registerNewborn(AgentMind mind, long tick) {
        Session session = new Session(mind, tick);
        SESSIONS.put(mind.id(), session);
        return session;
    }

    /** Blocking diagnostic/export census; never used by the extinction tick path. */
    static List<AgentMind> census() { return persistence == null ? List.of() : persistence.census(); }

    static java.util.concurrent.CompletableFuture<AgentMind.Snapshot> ancestor(
            local.villagers.mind.Ancestry.Neighborhood query, AgentMind.Snapshot liveBest, java.util.Set<UUID> liveIds) {
        return persistence == null ? java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException("Server stopping"))
                : persistence.ancestor(query, liveBest, liveIds);
    }

    static Session removeSession(UUID id) { return SESSIONS.remove(id); }

    static Iterable<Session> sessions() { return SESSIONS.values(); }

    static int sessionCount() { return SESSIONS.size(); }

    static void persist(Session session) {
        if (persistence != null) persistence.queue(session.mind);
    }

    static void persistAll() { SESSIONS.values().forEach(Villagers::persist); }

    /** Drop per-villager runtime state in every subsystem. Add new subsystems here, and only here. */
    static void forgetRuntime(UUID id) {
        VillageFamilies.interrupt(id);
        WorkNavigation.forget(id);
        AdventureActions.forget(id);
        VillageSurvival.forget(id);
        SocialReactions.forget(id);
        if (persistence != null) persistence.cancelLoad(id);
    }

    private static void clearRuntime() {
        SESSIONS.clear();
        AdventureActions.clear();
        VillageSurvival.clear();
        SocialReactions.clear();
        GodChildren.clear();
        VillageFamilies.clear();
        WorkNavigation.clear();
    }
}
