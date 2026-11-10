package local.villagers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Explicit post-baseline social smoke test, restricted to the independent playable development copy. */
public final class VillageSocialProbe {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<MinecraftServer, Probe> SELECTED = new IdentityHashMap<>();
    private static boolean hooksRegistered;
    private record Probe(UUID observer, UUID villager, boolean harmAttempted) {}
    private VillageSocialProbe() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var command = Commands.literal("socialprobe");
        for (String operation : new String[]{"start", "gift", "harm", "status"}) {
            command.then(Commands.literal(operation).executes(context -> execute(context.getSource(), operation)));
        }
        dispatcher.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(command));
        if (!hooksRegistered) {
            hooksRegistered = true;
            ServerLifecycleEvents.SERVER_STOPPING.register(SELECTED::remove);
        }
    }

    private static int execute(CommandSourceStack source, String operation) {
        MinecraftServer server = source.getServer();
        ServerLevel level = source.getLevel();
        Path root = server.getWorldPath(LevelResource.ROOT).resolve("villagers");
        Path directory = root.resolve("social-evidence");
        try {
            if (!DevMode.enabled() || !System.getProperty("villagers.role", "play").equals("play")
                    || !DevMode.TRIAL_WORLD.equals(server.getWorldData().getLevelName()) || !VillageTrial.isReady(server)
                    || !Files.isRegularFile(root.resolve("village-evidence/baseline.json"))) {
                say(source, "Social probes require the ready, baseline-frozen VillageTrial play copy; adaptive/frozen comparison runs are protected.");
                return 0;
            }
            ServerPlayer player = source.getEntity() instanceof ServerPlayer caller ? caller
                : server.getPlayerList().getPlayerCount() == 1 ? server.getPlayerList().getPlayers().getFirst() : null;
            if (player == null || !player.isAlive() || player.isSpectator() || player.level() != level) {
                say(source, "Use a real observer in this world, or a console with exactly one connected observer."); return 0;
            }
            Probe probe = SELECTED.get(server);
            Path stateFile = directory.resolve("probe-state.json");
            if (probe == null && Files.isRegularFile(stateFile)) {
                if (Files.size(stateFile) > 65_536) throw new IOException("Probe state is oversized; refusing to reset it");
                probe = JSON.fromJson(Files.readString(stateFile), Probe.class);
                if (probe == null || probe.observer() == null || probe.villager() == null) throw new IOException("Invalid persistent probe selection");
                SELECTED.put(server, probe);
            }
            if (probe != null && !probe.observer().equals(player.getUUID())) {
                say(source, "This probe belongs to its original observer; per-player impressions are not interchangeable."); return 0;
            }
            if (operation.equals("start") && probe == null) {
                Path markerFile = root.resolve("village-trial.json");
                if (!Files.isRegularFile(markerFile) || Files.size(markerFile) > 4_194_304) throw new IOException("Missing or oversized original-cohort marker");
                JsonObject marker = JsonParser.parseString(Files.readString(markerFile)).getAsJsonObject();
                JsonObject cohort = marker.getAsJsonObject("cohort"), center = marker.getAsJsonObject("center");
                if (cohort == null || center == null) throw new IOException("Original-cohort marker is incomplete");
                Villager selected = level.getEntitiesOfClass(Villager.class,
                        new net.minecraft.world.phys.AABB(center.get("x").getAsDouble() - 96, level.getMinY(), center.get("z").getAsDouble() - 96,
                            center.get("x").getAsDouble() + 96, level.getMaxY(), center.get("z").getAsDouble() + 96),
                        actor -> cohort.has(actor.getUUID().toString()) && healthyAwakeAdult(actor) && actor.getHealth() >= 16)
                    .stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
                if (selected == null) { say(source, "No healthy, awake, original adult is available; no resident was replaced or moved."); return 0; }
                VillageTrial.noteIntervention(level, "socialprobe start: observer relocation near one original resident; native social behavior only, no belief setter or resident teleport.");
                if (!moveObserverNear(level, player, selected)) { say(source, "No safe observer position within two blocks and line of sight was found."); return 0; }
                probe = new Probe(player.getUUID(), selected.getUUID(), false);
                SELECTED.put(server, probe);
                atomicWrite(stateFile, JSON.toJson(probe));
            }
            if (probe == null) { say(source, "Run /villagers socialprobe start first."); return 0; }
            var entity = level.getEntityInAnyDimension(probe.villager());
            if (!(entity instanceof Villager actor) || actor.level() != level || !actor.isAlive()) {
                write(directory, operation, Map.of("at", Instant.now().toString(), "selected_villager", probe.villager(),
                    "status", "selected actor unloaded, missing, converted or dead; never replaced"));
                say(source, "The selected original resident is unavailable; no replacement will be selected."); return 0;
            }
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("at", Instant.now().toString()); event.put("game_time", level.getGameTime());
            event.put("operation", operation); event.put("role", "play");
            event.put("learning_mode", System.getProperty("villagers.learning", "adaptive"));
            event.put("before", snapshot(level, actor, player));
            boolean success = true;
            if (operation.equals("start")) {
                success = healthyAwakeAdult(actor) && moveObserverNear(level, player, actor);
                event.put("intervention", "Only the observer is positioned near the same original resident. Wait for normal social ticks to observe attention.");
            } else if (operation.equals("gift")) {
                if (!player.getMainHandItem().isEmpty() || !healthyAwakeAdult(actor) || !moveObserverNear(level, player, actor)) {
                    say(source, "Gift requires an empty observer hand and a safely reachable awake resident; no item created."); return 0;
                }
                VillageTrial.noteIntervention(level, "socialprobe gift: operator creates exactly one bread in an empty observer hand, then calls actual sneak gift transfer. Explicit fixture supply; never part of natural baseline.");
                event.put("intervention", "Explicit operator fixture: exactly one minecraft:bread, followed by real Villagers.offerGift; failed leftovers remain in the observer hand.");
                int breadBefore = bread(actor);
                boolean sneaking = player.isShiftKeyDown();
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD, 1));
                try { player.setShiftKeyDown(true); success = Villagers.offerGift(level, actor, player, InteractionHand.MAIN_HAND); }
                finally { player.setShiftKeyDown(sneaking); }
                event.put("native_gift_transferred", success); event.put("resident_bread_before", breadBefore);
                event.put("resident_bread_after", bread(actor)); event.put("observer_hand_after", player.getMainHandItem().toString());
            } else if (operation.equals("harm")) {
                if (probe.harmAttempted()) { say(source, "The once-only native harm attempt was already used; it will not be repeated."); return 0; }
                if (!player.getMainHandItem().isEmpty() || !healthyAwakeAdult(actor) || actor.getHealth() < 10
                        || player.getAttributeValue(Attributes.ATTACK_DAMAGE) > 2 || !moveObserverNear(level, player, actor)) {
                    say(source, "Harm requires an empty, unboosted observer hand, resident health at least ten, and safe range/line of sight."); return 0;
                }
                VillageTrial.noteIntervention(level, "socialprobe harm: exactly one actual unarmed observer attack against the selected original resident, recorded as an explicit harmful intervention.");
                probe = new Probe(probe.observer(), probe.villager(), true);
                atomicWrite(stateFile, JSON.toJson(probe)); // Durable once-only guard precedes the physical attack.
                SELECTED.put(server, probe);
                float beforeHealth = actor.getHealth();
                event.put("intervention", "One native player.attack; no direct health or impression setters. Guard committed before attack.");
                player.attack(actor);
                float actualDamage = Math.max(0, beforeHealth - actor.getHealth());
                success = actualDamage > 0;
                event.put("accepted_health_damage", actualDamage); event.put("damage_accepted", success);
                event.put("harm_attempt_used", true);
            }
            event.put("after", snapshot(level, actor, player));
            event.put("harm_attempt_used", probe.harmAttempted());
            write(directory, operation, event);
            say(source, "Social " + operation + " " + (success ? "recorded" : "did not complete") + "; actual body state and per-player impression saved.");
            return success ? 1 : 0;
        } catch (Exception failure) {
            say(source, "Social probe failed without resetting its cohort or beliefs: " + failure.getMessage());
            return 0;
        }
    }

    private static boolean healthyAwakeAdult(Villager actor) {
        return actor.isAlive() && actor.entityTags().contains(Villagers.MANAGED) && !actor.isBaby()
            && !actor.isSleeping() && !actor.isTrading() && !actor.isOnFire();
    }

    private static boolean moveObserverNear(ServerLevel level, ServerPlayer player, Villager actor) {
        if (player.distanceToSqr(actor) <= 4 && player.hasLineOfSight(actor) && !player.isInWater()) return true;
        for (int[] offset : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}}) {
            double scale = 1.6 / Math.hypot(offset[0], offset[1]);
            for (int dy : new int[]{0, 1, -1}) {
                BlockPos feet = BlockPos.containing(actor.getX() + offset[0] * scale, actor.getY() + dy, actor.getZ() + offset[1] * scale);
                if (!level.isLoaded(feet) || !level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) continue;
                var below = level.getBlockState(feet.below());
                if (below.getCollisionShape(level, feet.below()).isEmpty() || !below.getFluidState().isEmpty()
                        || below.is(BlockTags.FIRE) || below.is(Blocks.MAGMA_BLOCK) || below.is(Blocks.CACTUS)
                        || below.is(Blocks.FARMLAND) || below.is(Blocks.CAMPFIRE) || below.is(Blocks.SOUL_CAMPFIRE)) continue;
                Vec3 candidate = new Vec3(feet.getX() + .5, feet.getY(), feet.getZ() + .5);
                if (candidate.distanceToSqr(actor.position()) > 4) continue;
                var hit = level.clip(new ClipContext(candidate.add(0, player.getEyeHeight(), 0), actor.getEyePosition(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (hit.getType() != HitResult.Type.MISS) continue;
                if (!level.noCollision(player, player.getBoundingBox().move(candidate.subtract(player.position())))) continue;
                Vec3 look = actor.getEyePosition().subtract(candidate.add(0, player.getEyeHeight(), 0));
                float yaw = (float) Math.toDegrees(Math.atan2(look.z, look.x)) - 90;
                float pitch = (float) -Math.toDegrees(Math.atan2(look.y, Math.hypot(look.x, look.z)));
                player.teleportTo(level, candidate.x, candidate.y, candidate.z, Set.of(), yaw, pitch, true);
                return player.distanceToSqr(actor) <= 4 && player.hasLineOfSight(actor);
            }
        }
        return false;
    }

    private static Map<String, Object> snapshot(ServerLevel level, Villager actor, ServerPlayer player) {
        return Map.of("villager_uuid", actor.getUUID(), "observer_uuid", player.getUUID(),
            "health", actor.getHealth(), "villager_position", actor.position().toString(),
            "observer_position", player.position().toString(), "distance", player.distanceTo(actor),
            "line_of_sight", player.hasLineOfSight(actor), "observer_hand", player.getMainHandItem().toString(),
            "impression", Villagers.impressionFor(level, actor, player.getUUID()), "social_activity", SocialReactions.status(actor.getUUID()));
    }
    private static int bread(Villager actor) {
        int count = 0;
        for (int slot = 0; slot < actor.getInventory().getContainerSize(); slot++) {
            ItemStack stack = actor.getInventory().getItem(slot); if (stack.is(Items.BREAD)) count += stack.getCount();
        }
        return count;
    }
    private static void write(Path directory, String operation, Object evidence) throws IOException {
        atomicWrite(directory.resolve(operation + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8) + ".json"), JSON.toJson(evidence));
    }
    private static void atomicWrite(Path destination, String text) throws IOException {
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), "social-", ".tmp");
        try { Files.writeString(temporary, text); Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        finally { Files.deleteIfExists(temporary); }
    }
    private static void say(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal("[Villagers] " + message), false);
    }
}
