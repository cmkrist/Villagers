package local.villagers;

import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Research fixture commands for the disposable VillagerLab world, plus the evidence report.
 * Registered only in dev mode (see {@link DevMode}); none of this exists in a release install.
 */
final class LabCommands {
    private static final String LAB_TAG = "villagers:lab";
    private static final String THREAT_TAG = "villagers:lab-threat";
    private static final int CABINS = 8;

    private LabCommands() {}

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("lab").executes(c -> lab(c.getSource())))
                .then(Commands.literal("repairlab").executes(c -> repairLab(c.getSource())))
                .then(Commands.literal("openlab").executes(c -> openLab(c.getSource())))
                .then(Commands.literal("danger").executes(c -> danger(c.getSource())))
                .then(Commands.literal("calm").executes(c -> calm(c.getSource())))
                .then(Commands.literal("report").executes(c -> report(c.getSource()))));
    }

    private static boolean labWorld(CommandSourceStack source) {
        if (DevMode.inWorld(source.getServer(), DevMode.LAB_WORLD) && Villagers.ready()) return true;
        VillagerCommands.tell(source, "Lab commands require the disposable " + DevMode.LAB_WORLD + " dev world.");
        return false;
    }

    private static void run(CommandSourceStack source, String command) {
        source.getServer().getCommands().performPrefixedCommand(source, command);
    }

    private static int cabinX(int index) { return -11 + index * 3; }

    private static int lab(CommandSourceStack source) {
        if (!labWorld(source)) return 0;
        ServerLevel level = source.getLevel();
        if (!level.getEntitiesOfClass(Villager.class, new AABB(-40, -65, -40, 40, -40, 40), v -> v.entityTags().contains(LAB_TAG)).isEmpty()) {
            repairLab(source);
            VillagerCommands.tell(source, "Existing lab resumed; identities and resources preserved.");
            return 1;
        }
        // Floor and a glass perimeter around the courtyard, then fixed daylight.
        for (String command : List.of("fill -14 -61 -12 14 -61 12 oak_planks",
                "fill -15 -60 -13 15 -58 -13 glass", "fill -15 -60 13 15 -58 13 glass",
                "fill -15 -60 -13 -15 -58 13 glass", "fill 15 -60 -13 15 -58 13 glass",
                "time set day", "weather clear", "tp @a 18 -49 24 145 28")) run(source, command);
        for (int i = 0; i < CABINS; i++) {
            int x = cabinX(i);
            // One hollow cabin per villager, with a doorway, a bed and a torch.
            for (String command : List.of(
                    "fill " + (x - 1) + " -61 -11 " + (x + 1) + " -57 -6 oak_planks hollow",
                    "fill " + (x - 1) + " -56 -11 " + (x + 1) + " -56 -6 oak_planks",
                    "fill " + x + " -60 -6 " + x + " -59 -6 air",
                    "setblock " + x + " -60 -9 red_bed[part=foot,facing=north]",
                    "setblock " + x + " -60 -10 red_bed[part=head,facing=north]",
                    "setblock " + x + " -58 -8 wall_torch[facing=south]")) run(source, command);
            Villager v = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
            if (v == null) continue;
            v.snapTo(x, -60, -3, 0, 0);
            v.addTag(LAB_TAG);
            level.addFreshEntity(v);
            VillagerController.setHome(level, v, Villagers.session(v, level), new BlockPos(x, -60, -10));
            Villagers.adopt(level, v);
            v.getInventory().addItem(new ItemStack(Items.BREAD, 12));
        }
        for (int x : new int[]{-9, -3, 3, 9}) {
            ItemEntity bread = new ItemEntity(level, x, -60, 4, new ItemStack(Items.BREAD, 64));
            bread.setUnlimitedLifetime();
            level.addFreshEntity(bread);
        }
        VillagerCommands.tell(source, "Lab: eight identities, eight bedded homes, finite food. No invulnerability.");
        return 1;
    }

    /** Repair legacy fixture geometry without moving entities, replenishing food, or resetting minds. */
    private static int repairLab(CommandSourceStack source) {
        if (!labWorld(source)) return 0;
        int cleared = 0;
        var level = source.getLevel();
        for (int i = 0; i < CABINS; i++) {
            int x = cabinX(i);
            for (BlockPos pos : BlockPos.betweenClosed(x, -60, -8, x, -58, -6)) {
                // The aisle and doorway are fixture-owned; beds at z=-9/-10 remain untouched.
                if (!level.getBlockState(pos).isAir()) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    cleared++;
                }
            }
        }
        VillagerCommands.tell(source, "Repaired cabin aisles and headroom; cleared " + cleared + " blocks. No villager was teleported.");
        return cleared;
    }

    private static int openLab(CommandSourceStack source) {
        if (!labWorld(source)) return 0;
        var level = source.getLevel();
        for (BlockPos pos : BlockPos.betweenClosed(-2, -60, 13, 2, -58, 13))
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        VillagerCommands.tell(source, "Opened a five-block courtyard gate to the surrounding world.");
        return 1;
    }

    private static int danger(CommandSourceStack source) {
        if (!labWorld(source)) return 0;
        var level = source.getLevel();
        var husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
        if (husk != null) {
            husk.snapTo(0, -60, 7, 0, 0);
            husk.setPersistenceRequired();
            husk.addTag(THREAT_TAG);
            level.addFreshEntity(husk);
        }
        return 1;
    }

    private static int calm(CommandSourceStack source) {
        if (!labWorld(source)) return 0;
        source.getLevel().getEntitiesOfClass(Monster.class, new AABB(-50, -65, -50, 50, -40, 50),
                m -> m.entityTags().contains(THREAT_TAG)).forEach(Monster::discard);
        return 1;
    }

    /** Writes a JSON snapshot of every live mind and nearby managed villager to {@code <server>/evidence/}. */
    private static int report(CommandSourceStack source) {
        if (!Villagers.ready()) { VillagerCommands.tell(source, "Villager storage is not available yet."); return 0; }
        Map<String, Object> report = VillagerController.summary();
        report.put("loadedDimensions", StreamSupport.stream(source.getServer().getAllLevels().spliterator(), false)
            .map(level -> level.dimension().identifier().toString()).toList());
        report.put("agents", StreamSupport.stream(Villagers.sessions().spliterator(), false).map(s -> s.mind.snapshot()).toList());
        Vec3 origin = source.getPosition();
        if (!(source.getEntity() instanceof ServerPlayer) && source.getServer().getPlayerList().getPlayers().size() == 1)
            origin = source.getServer().getPlayerList().getPlayers().getFirst().position();
        report.put("navigation", source.getLevel().getEntitiesOfClass(Villager.class, new AABB(origin, origin).inflate(256),
                Villagers::isManaged).stream().map(LabCommands::navigationRow).toList());
        report.put("creatures", StreamSupport.stream(source.getLevel().getAllEntities().spliterator(), false)
            .filter(e -> e instanceof net.minecraft.world.entity.Mob mob && CreatureAgents.managed(mob))
            .map(e -> CreatureAgents.status((net.minecraft.world.entity.Mob)e)).toList());
        try {
            Path evidence = source.getServer().getServerDirectory().resolve("evidence");
            Files.createDirectories(evidence);
            Files.writeString(evidence.resolve("report-" + System.currentTimeMillis() + ".json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(report));
            VillagerCommands.tell(source, "Report saved; " + report.get("decisions") + " decisions, "
                    + report.get("completedSkills") + " completed skills, " + report.get("safetyInterrupts") + " danger responses.");
        } catch (IOException e) {
            Villagers.LOG.error("Report write failed", e);
            return 0;
        }
        return 1;
    }

    private static Map<String, Object> navigationRow(Villager v) {
        Session state = Villagers.existingSession(v.getUUID());
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", v.getUUID());
        row.put("position", List.of(v.getX(), v.getY(), v.getZ()));
        row.put("health", v.getHealth());
        row.put("airSupply",v.getAirSupply());
        if(state!=null)row.put("looseWoodCollected",state.looseWoodCollected);
        row.put("sleeping", v.isSleeping());
        row.put("capabilities", AdventureActions.equipmentStatus(v));
        row.put("interactions", AdventureActions.activityStatus(v.getUUID()));
        row.put("survival", VillageSurvival.status(v.getUUID()));
        if (state != null) row.put("boating", BoatTravel.status(state));
        if (state != null) row.put("woodPreparation", WoodBootstrap.status(state));
        if (state != null) row.put("foodAccess",FoodAccess.status(state));
        if (state != null) row.put("nativeFoodStores",NativeFoodStorage.status(state));
        if (state != null) row.put("mining", AdventureMining.status(state));
        row.put("social", SocialReactions.status(v.getUUID()));
        row.put("action", state == null ? null : state.action);
        if (state != null) row.put("adventure", Map.of("nominated", AdventurePreparation.nominated(v),
            "activity", state.adventureStatus, "craftsThisSession", state.adventureCrafts));
        if (state != null) row.put("family", Map.of("activity", state.familyStatus,
            "savingFood", state.familyFoodGoal, "nextBirth", state.mind.nextBreedingAt()));
        row.put("navigationDone", v.getNavigation().isDone());
        var path = v.getNavigation().getPath();
        row.put("pathReachesTarget", path != null && path.canReach());
        row.put("pathTarget", path == null ? null : path.getTarget().toShortString());
        return row;
    }
}
