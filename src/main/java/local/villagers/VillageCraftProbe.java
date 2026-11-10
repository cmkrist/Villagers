package local.villagers;

import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.storage.LevelResource;

/** Explicit station/material fixture in the independent play copy, never a natural-resource result. */
final class VillageCraftProbe {
    private record Selected(UUID actor, int wheatBefore, int foodBefore, long craftedBefore, BlockPos table) {}
    private static final Map<MinecraftServer, Selected> SELECTED = new IdentityHashMap<>();
    private VillageCraftProbe() {}

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("craftprobe")
                        .then(Commands.literal("start").executes(c -> execute(c.getSource(), true)))
                        .then(Commands.literal("status").executes(c -> execute(c.getSource(), false)))));
        ServerLifecycleEvents.SERVER_STOPPING.register(SELECTED::remove);
    }

    private static int execute(CommandSourceStack source, boolean start) {
        var server = source.getServer();
        var level = source.getLevel();
        if (!DevMode.inWorld(server, DevMode.TRIAL_WORLD) || !"play".equals(System.getProperty("villagers.role"))) {
            source.sendFailure(Component.literal("Craft fixtures require the separate play copy.")); return 0;
        }
        try {
            if (start) {
                var observer = source.getPlayerOrException();
                if (!observer.getMainHandItem().isEmpty()) throw new IllegalStateException("Observer hand must be empty");
                Villager actor = level.getEntitiesOfClass(Villager.class, observer.getBoundingBox().inflate(24),
                                v -> v.isAlive() && Villagers.isManaged(v) && !v.isSleeping() && !v.isTrading())
                        .stream().min(Comparator.comparingDouble(observer::distanceToSqr)).orElseThrow(
                                () -> new IllegalStateException("No awake managed resident within 24 blocks"));
                if (observer.distanceToSqr(actor) > 9 || !observer.hasLineOfSight(actor))
                    throw new IllegalStateException("Approach within three blocks with sight before provisioning");
                Session session = Villagers.session(actor, level);
                BlockPos table = BreadCrafting.table(level, actor, session);
                boolean fixture = table == null;
                if (fixture) {
                    for (BlockPos p : BlockPos.betweenClosed(actor.blockPosition().offset(-3, 0, -3),
                            actor.blockPosition().offset(3, 0, 3))) {
                        if (actor.blockPosition().distSqr(p) < 4 || actor.blockPosition().distSqr(p) > 9
                                || !level.isLoaded(p) || !level.getBlockState(p).isAir()
                                || !level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)
                                || level.getBlockState(p.below()).is(Blocks.FARMLAND)) continue;
                        table = p.immutable(); break;
                    }
                    if (table == null) throw new IllegalStateException("No safe air block for a labelled station fixture");
                }
                if (!actor.getInventory().canAddItem(new ItemStack(Items.WHEAT, 3)))
                    throw new IllegalStateException("Resident inventory cannot receive the fixture wheat");
                VillageTrial.noteIntervention(level, "craftprobe: observer receives 3 wheat and transfers real items to "
                        + actor.getUUID() + "; " + (fixture ? "one operator crafting table placed in air at " : "existing table used at ")
                        + table.toShortString() + ". No resident movement, food-level change, or forced action.");
                Selected selected = new Selected(actor.getUUID(), BreadCrafting.wheatCount(actor.getInventory()),
                        session.mind.foodLevel(), session.breadCrafted, table);
                SELECTED.put(server, selected);
                if (fixture && !level.setBlockAndUpdate(table, Blocks.CRAFTING_TABLE.defaultBlockState()))
                    throw new IllegalStateException("Could not place operator fixture table");
                observer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 3));
                boolean crouching = observer.isShiftKeyDown();
                observer.setShiftKeyDown(true);
                int transferred = 0;
                try {
                    // The regular handoff checks reach and sight. No villager or observer teleport.
                    for (int i = 0; i < 3; i++)
                        if (Villagers.offerGift(level, actor, observer, InteractionHand.MAIN_HAND)) transferred++;
                } finally { observer.setShiftKeyDown(crouching); }
                session.nextCraftScan = 0;
                if (transferred != 3) throw new IllegalStateException("Only " + transferred + "/3 fixture wheat handed over; partial intervention retained");
            }
            Selected selected = SELECTED.get(server);
            if (selected == null) throw new IllegalStateException("Start a labelled fixture first");
            if (!(level.getEntity(selected.actor()) instanceof Villager actor)) throw new IllegalStateException("Selected resident is unloaded");
            Session session = Villagers.session(actor, level);
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("kind", "explicit play-only crafting station/material intervention, not natural supplies");
            report.put("gameTime", level.getGameTime()); report.put("actor", selected.actor());
            report.put("name", session.mind.name()); report.put("wheatBefore", selected.wheatBefore());
            report.put("fixtureWheat", 3); report.put("wheatNow", BreadCrafting.wheatCount(actor.getInventory()));
            report.put("foodBefore", selected.foodBefore()); report.put("foodNow", session.mind.foodLevel());
            report.put("craftedDelta", session.breadCrafted - selected.craftedBefore());
            report.put("craftingStatus", session.craftingStatus); report.put("table", selected.table().toShortString());
            report.put("tableStillPresent", level.getBlockState(selected.table()).is(Blocks.CRAFTING_TABLE));
            report.put("inventory", actor.getInventory().getItems().stream().map(ItemStack::toString).toList());
            Map<String, String> crops = new LinkedHashMap<>();
            for (BlockPos p : BlockPos.betweenClosed(actor.blockPosition().offset(-8, -3, -8), actor.blockPosition().offset(8, 3, 8)))
                if (level.isLoaded(p) && level.getBlockState(p).getBlock() instanceof CropBlock) crops.put(p.toShortString(), level.getBlockState(p).toString());
            report.put("nearbyCropStates", crops);
            var directory = server.getWorldPath(LevelResource.ROOT).resolve("villagers/crafting-evidence");
            Files.createDirectories(directory);
            Files.writeString(directory.resolve("craft-" + System.currentTimeMillis() + ".json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(report));
            source.sendSuccess(() -> Component.literal("Craft probe recorded: " + report), false);
            return 1;
        } catch (Exception failure) {
            source.sendFailure(Component.literal("Craft probe: " + failure.getMessage())); return 0;
        }
    }
}
