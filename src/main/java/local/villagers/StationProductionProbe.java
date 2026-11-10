package local.villagers;

import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Only withdraw the known operator-supplied old table in the play fixture; retain its actual native drop. */
final class StationProductionProbe {
    private StationProductionProbe() {}
    static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stationprobe").then(Commands.literal("withdraw").executes(c -> withdraw(c.getSource())))));
    }
    private static int withdraw(CommandSourceStack source) {
        if (!DevMode.inWorld(source.getServer(), DevMode.TRIAL_WORLD) || !"play".equals(System.getProperty("villagers.role"))) return 0;
        try {
            var level = source.getLevel(); var player = source.getPlayerOrException(); var pos = new BlockPos(-226,71,744);
            if (!level.isLoaded(pos) || !level.getBlockState(pos).is(Blocks.CRAFTING_TABLE)
                    || player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > 9 || !player.getInventory().getItem(9).isEmpty())
                throw new IllegalStateException("Approach the known operator table with empty backpack slot9");
            var hit = level.clip(new ClipContext(player.getEyePosition(), Vec3.atCenterOf(pos), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS && !hit.getBlockPos().equals(pos)) throw new IllegalStateException("Native sight required");
            var state = level.getBlockState(pos); var drops = Block.getDrops(state, level, pos, null, player, ItemStack.EMPTY);
            if (drops.size() != 1 || !drops.getFirst().is(Items.CRAFTING_TABLE) || drops.getFirst().getCount() != 1)
                throw new IllegalStateException("Unsupported actual table loot");
            var report = Map.of("kind", "EXPLICIT removal of prior operator station, retained by observer, not villager work",
                    "gameTime", level.getGameTime(), "position", pos.toShortString(), "nativeDrops", drops.stream().map(ItemStack::toString).toList(), "observerStorageSlot", 9);
            var folder = source.getServer().getWorldPath(LevelResource.ROOT).resolve("villagers/station-evidence"); Files.createDirectories(folder);
            Files.writeString(folder.resolve("withdraw-before.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report), StandardOpenOption.CREATE_NEW);
            VillageTrial.noteIntervention(level, "stationprobe: withdraw prior OPERATOR SUPPLIED crafting table at " + pos
                    + " into observer backpack slot9; no villager material, movement, work or action supplied");
            if (!level.getBlockState(pos).equals(state) || !level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) return 0;
            player.getInventory().setItem(9, drops.getFirst().copy());
            Files.writeString(folder.resolve("withdraw-after.json"), new GsonBuilder().setPrettyPrinting().create().toJson(
                    Map.of("oldPositionAir", level.getBlockState(pos).isAir(), "observerSlot9", player.getInventory().getItem(9).toString())), StandardOpenOption.CREATE_NEW);
            VillagerCommands.tell(source, "Known operator table withdrawn; real item retained in observer backpack slot9."); return 1;
        } catch (Exception failure) { VillagerCommands.tell(source, "Station probe: " + failure.getMessage()); return 0; }
    }
}
