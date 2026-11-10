package local.villagers;

import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;

/** Explicit material input through real handoff. Never invokes production, movement, hunger or terrain setters. */
final class WoodProductionProbe {
    private WoodProductionProbe() {}
    static void register(CommandDispatcher<CommandSourceStack> d) {
        var wood = Commands.literal("woodprobe");
        for (int count : new int[] {1, 2}) {
            wood.then(Commands.literal(count == 1 ? "supply" : "station")
                    .then(Commands.argument("uuid", StringArgumentType.word()).executes(c -> supply(c.getSource(),
                            UUID.fromString(StringArgumentType.getString(c, "uuid")), count))));
        }
        d.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(wood));
    }
    private static int supply(CommandSourceStack source, UUID id, int logs) {
        if (!DevMode.inWorld(source.getServer(), DevMode.TRIAL_WORLD) || !"play".equals(System.getProperty("villagers.role"))) return 0;
        try {
            var level = source.getLevel(); var observer = source.getPlayerOrException();
            if (!(level.getEntity(id) instanceof Villager actor) || !Villagers.isManaged(actor)
                    || !actor.isAlive() || actor.isBaby() || actor.isSleeping() || actor.isTrading()
                    || AdventureActions.hasHoe(actor)) throw new IllegalStateException("Choose an awake managed adult without a hoe");
            if (!observer.getMainHandItem().isEmpty() || observer.distanceToSqr(actor) > 9 || !observer.hasLineOfSight(actor))
                throw new IllegalStateException("Empty observer hand and real three-block gift reach/sight required");
            var capacity = new net.minecraft.world.SimpleContainer(actor.getInventory().getContainerSize());
            for (int slot = 0; slot < capacity.getContainerSize(); slot++) capacity.setItem(slot, actor.getInventory().getItem(slot).copy());
            if (!capacity.addItem(new ItemStack(Items.OAK_LOG, logs)).isEmpty()
                    || !capacity.addItem(new ItemStack(Items.WHEAT_SEEDS)).isEmpty()) throw new IllegalStateException("No room for both input items");
            var report = new LinkedHashMap<String, Object>();
            report.put("kind", "LABELLED material supply, not autonomous resource acquisition");
            report.put("gameTime", level.getGameTime()); report.put("actor", id);
            report.put("inventoryBefore", actor.getInventory().getItems().stream().map(ItemStack::toString).toList());
            report.put("handBefore", actor.getMainHandItem().toString());
            report.put("inputs", java.util.List.of(logs + " minecraft:oak_log", "1 minecraft:wheat_seeds"));
            var folder = source.getServer().getWorldPath(LevelResource.ROOT).resolve("villagers/wood-evidence"); Files.createDirectories(folder);
            var gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(folder.resolve("supply-before-" + id + ".json"), gson.toJson(report), StandardOpenOption.CREATE_NEW);
            VillageTrial.noteIntervention(level, "woodprobe: observer supplies " + logs + " oak log and 1 wheat seed to " + id
                    + " through ordinary handoff; no tool output, terrain, actor movement, hunger, time or forced production");
            boolean crouched = observer.isShiftKeyDown(); int given = 0;
            try {
                observer.setShiftKeyDown(true);
                var items = new java.util.ArrayList<net.minecraft.world.item.Item>();
                for (int i = 0; i < logs; i++) items.add(Items.OAK_LOG); items.add(Items.WHEAT_SEEDS);
                for (var item : items) {
                    observer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
                    if (!Villagers.offerGift(level, actor, observer, InteractionHand.MAIN_HAND)) break;
                    given++;
                }
            } finally {
                observer.setShiftKeyDown(crouched);
                report.put("given", given);
                report.put("inventoryAfter", actor.getInventory().getItems().stream().map(ItemStack::toString).toList());
                Files.writeString(folder.resolve("supply-after-" + id + ".json"), gson.toJson(report), StandardOpenOption.CREATE_NEW);
            }
            if (given != logs + 1) throw new IllegalStateException("Partial supply retained: " + given + "/" + (logs + 1) + " handoffs");
            VillagerCommands.tell(source, (logs + 1) + " real material handoffs complete; no production action invoked."); return 1;
        } catch (Exception failure) { VillagerCommands.tell(source, "Wood probe: " + failure.getMessage()); return 0; }
    }
}
