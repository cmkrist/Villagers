package local.villagers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import local.villagers.mind.Home;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.LevelResource;

/** Server-only, consent-based recruitment. Entity tags and a world-scoped bed registry survive reloads. */
final class TraderRecruitment {
    private static final String ROLLED = "villagers:recruitment_rolled";
    private static final String WILLING = "villagers:recruitment_willing";
    private static final String OFFER = "villagers:offer:";
    private static final String HOME = "villagers:household:";
    private static final Set<Home> RESERVED = new HashSet<>();
    private static Path file;
    private static boolean writable;

    static void initialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            RESERVED.clear(); writable = true;
            file = server.getWorldPath(LevelResource.ROOT).resolve("villagers/player-beds.json");
            if (Files.exists(file)) {
                try {
                    Set<Home> beds = new Gson().fromJson(Files.readString(file), new TypeToken<Set<Home>>() {}.getType());
                    if (beds == null || beds.contains(null)) throw new IllegalStateException("Invalid player bed registry");
                    RESERVED.addAll(beds);
                } catch (Exception e) {
                    writable = false;
                    Villagers.LOG.error("Cannot read player beds; recruitment disabled to preserve bed protection", e);
                }
            }
        });
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            boolean changed = false;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                var spawn = player.getRespawnConfig();
                if (spawn != null) {
                    ServerLevel level = server.getLevel(spawn.respawnData().dimension());
                    BlockPos pos = spawn.respawnData().pos();
                    if (level != null && level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof BedBlock)
                        changed |= RESERVED.add(home(level, bedHead(level, pos)));
                }
            }
            if (changed) save();
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!(entity instanceof WanderingTrader trader) || !player.isShiftKeyDown()
                    || hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer owner) || !(world instanceof ServerLevel level)) return InteractionResult.PASS;
            if (!Villagers.ready() || !writable || player.distanceToSqr(trader) > 9 || !trader.isAlive()
                    || owner.isSpectator() || trader.isTrading()) return InteractionResult.PASS;
            var spawn = owner.getRespawnConfig();
            if (spawn == null || !spawn.respawnData().dimension().equals(level.dimension())
                    || !level.isLoaded(spawn.respawnData().pos())
                    || !(level.getBlockState(spawn.respawnData().pos()).getBlock() instanceof BedBlock)) {
                say(owner, "Set your home by sleeping in a bed in this dimension first. Your home bed must be loaded.");
                return InteractionResult.SUCCESS;
            }
            if (!trader.entityTags().contains(ROLLED)) {
                trader.addTag(ROLLED);
                if (level.getRandom().nextInt(100) < Villagers.config().traderRecruitmentChancePercent()) trader.addTag(WILLING);
            }
            if (!trader.entityTags().contains(WILLING)) {
                say(owner, "This trader prefers life on the road.");
                return InteractionResult.SUCCESS;
            }
            String offer = OFFER + owner.getUUID();
            if (!trader.entityTags().contains(offer)) {
                trader.addTag(offer);
                say(owner, "I'd like to settle at your home. Sneak-right-click me again with an empty hand to welcome me. I'll need my own spare bed.");
                return InteractionResult.SUCCESS;
            }
            Home anchor = home(level, bedHead(level, spawn.respawnData().pos()));
            RESERVED.add(anchor);
            if (!save()) {
                say(owner, "Your home could not be saved. Please try again later.");
                return InteractionResult.SUCCESS;
            }
            Villager recruit = new Villager(EntityTypes.VILLAGER, level);
            recruit.snapTo(trader.getX(), trader.getY(), trader.getZ(), trader.getYRot(), trader.getXRot());
            recruit.finalizeSpawn(level, level.getCurrentDifficultyAt(recruit.blockPosition()), EntitySpawnReason.CONVERSION, null);
            recruit.setPersistenceRequired();
            if (trader.hasCustomName()) recruit.setCustomName(trader.getCustomName());
            recruit.addTag(HOME + encode(anchor));
            recruit.addTag("villagers:householder:" + owner.getUUID());
            if (!level.addFreshEntity(recruit)) return InteractionResult.SUCCESS;
            Villagers.adopt(level, recruit);
            trader.discard();
            say(owner, "Welcome home! Your new resident will use a separate spare bed near your home, and will never take your bed.");
            return InteractionResult.SUCCESS;
        });
    }

    static Home anchor(Villager villager) {
        for (String tag : villager.entityTags()) if (tag.startsWith(HOME)) {
            try {
                String[] fields = tag.substring(HOME.length()).split("\\|", -1);
                return new Home(fields[0], Integer.parseInt(fields[1]), Integer.parseInt(fields[2]), Integer.parseInt(fields[3]));
            } catch (RuntimeException ignored) { return null; }
        }
        return null;
    }

    static boolean allowedBed(ServerLevel level, BlockPos pos) {
        if (!writable) return false;
        BlockPos head = bedHead(level, pos);
        if (RESERVED.contains(home(level, head))) return false;
        // Immediate protection even between registry ticks, including both halves of an occupied bed.
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            var spawn = player.getRespawnConfig();
            if (spawn != null && spawn.respawnData().dimension().equals(level.dimension())
                    && bedHead(level, spawn.respawnData().pos()).equals(head)) return false;
            if (player.level() == level && player.getSleepingPos().map(p -> bedHead(level, p).equals(head)).orElse(false)) return false;
        }
        return true;
    }

    static boolean canClaim(ServerLevel level, Villager villager, BlockPos pos) {
        Home anchor = anchor(villager);
        return allowedBed(level, pos) && (anchor == null || anchor.dimension().equals(level.dimension().identifier().toString())
                && new BlockPos(anchor.x(), anchor.y(), anchor.z()).distSqr(pos) <= 16 * 16);
    }

    private static BlockPos bedHead(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return pos;
        var state = level.getBlockState(pos);
        return state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.FOOT
                ? pos.relative(state.getValue(BedBlock.FACING)) : pos;
    }
    private static Home home(ServerLevel level, BlockPos pos) {
        return new Home(level.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ());
    }
    private static String encode(Home home) { return home.dimension() + "|" + home.x() + "|" + home.y() + "|" + home.z(); }
    private static void say(ServerPlayer player, String text) { player.sendSystemMessage(Component.literal(text)); }
    private static boolean save() {
        if (!writable || file == null) return false;
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, new Gson().toJson(RESERVED));
            Files.move(temporary, file, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) { Villagers.LOG.error("Cannot save protected player beds", e); return false; }
    }
}
