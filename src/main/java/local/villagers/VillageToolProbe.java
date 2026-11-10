package local.villagers;

import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.util.*;

/** Explicit, recorded tool intervention in the rendered trial. Never part of baseline admission. */
public final class VillageToolProbe {
    private VillageToolProbe() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("testharvest").executes(c -> harvest(c.getSource()))));
    }
    private static int harvest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var level = source.getLevel(); var server = source.getServer();
        if (!DevMode.inWorld(server, DevMode.TRIAL_WORLD)) return 0;
        if (!"play".equals(System.getProperty("villagers.role", "adaptive"))) {
            say(source, "Harvest interventions require the separate play copy; comparison worlds are protected.");
            return 0;
        }
        var directory = server.getWorldPath(LevelResource.ROOT).resolve("villagers/village-evidence");
        if (!Files.isRegularFile(directory.resolve("baseline.json"))) { say(source,"Wait for the natural baseline to freeze."); return 0; }
        ServerPlayer player = server.getPlayerList().getPlayers().stream().findFirst().orElse(null);
        if (player == null || !player.getMainHandItem().isEmpty()) { say(source,"The observer must have an empty hand; no items were changed."); return 0; }
        Villager actor = null; BlockPos target = null; Item seed = null;
        search: for (var entity : level.getAllEntities()) {
            if (!(entity instanceof Villager v) || !v.entityTags().contains(Villagers.MANAGED) || v.isSleeping() || !v.getMainHandItem().isEmpty()) continue;
            for (BlockPos p : BlockPos.betweenClosed(v.blockPosition().offset(-8,-3,-8),v.blockPosition().offset(8,3,8))) {
                var state = level.getBlockState(p);
                if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state) || !level.getBlockState(p.below()).is(Blocks.FARMLAND)
                    || v.getEyePosition().distanceToSqr(Vec3.atCenterOf(p)) > 64) continue;
                Item planting = state.is(Blocks.WHEAT) ? Items.WHEAT_SEEDS : state.is(Blocks.CARROTS) ? Items.CARROT
                    : state.is(Blocks.POTATOES) ? Items.POTATO : state.is(Blocks.BEETROOTS) ? Items.BEETROOT_SEEDS : null;
                if (planting != null) { actor = v; target = p.immutable(); seed = planting; break search; }
            }
        }
        if (actor == null) { say(source,"No resident is currently within eight blocks of a ripe field crop; no world/items changed."); return 0; }
        Vec3 original = player.position(); float yaw = player.getYRot(), pitch = player.getXRot(); boolean crouching = player.isShiftKeyDown();
        boolean hoeGiven = false, seedGiven = false, harvested = false;
        var before = level.getBlockState(target); var farmland = level.getBlockState(target.below());
        Map<BlockPos,BlockState> neighbors = new LinkedHashMap<>();
        for (BlockPos p : BlockPos.betweenClosed(target.offset(-1,-1,-1),target.offset(1,1,1))) if (!p.equals(target)) neighbors.put(p.immutable(),level.getBlockState(p));
        Map<String,Object> evidence = new LinkedHashMap<>();
        evidence.put("kind","explicit post-baseline tool intervention, not a natural resource baseline");
        evidence.put("gameTime",level.getGameTime()); evidence.put("villager",actor.getUUID()); evidence.put("crop",target.toShortString());
        evidence.put("before",before.toString()); evidence.put("fixtureOperatorKit",List.of("1 minecraft:iron_hoe","1 "+BuiltInRegistries.ITEM.getKey(seed)));
        try {
            VillageTrial.noteIntervention(level,"testharvest: operator receives one iron hoe and one planting item, then transfers actual items to an existing resident. No actor or crop teleport/growth/spawn.");
            for (int[] offset : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                double x=actor.getX()+offset[0]*1.3,z=actor.getZ()+offset[1]*1.3;
                BlockPos feet=BlockPos.containing(x,actor.getY(),z);
                if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) continue;
                player.teleportTo(level,x,actor.getY(),z,Set.of(),yaw,pitch,true);
                if (player.hasLineOfSight(actor)) break;
            }
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_HOE));
            hoeGiven=Villagers.offerGift(level,actor,player,InteractionHand.MAIN_HAND);
            if (hoeGiven) {
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(seed));
                seedGiven=Villagers.offerGift(level,actor,player,InteractionHand.MAIN_HAND);
            }
            if (hoeGiven && seedGiven) {
                harvested=AdventureActions.tryHarvest(level,actor,target);
                if (!harvested) Villagers.requestHarvestForProbe(level,actor,target);
            }
            evidence.put("hoeTransferred",hoeGiven); evidence.put("plantingItemTransferred",seedGiven); evidence.put("nativeHarvestSucceeded",harvested); evidence.put("awaitingControllerTravel",hoeGiven && seedGiven && !harvested);
            evidence.put("after",level.getBlockState(target).toString());
            evidence.put("farmlandUnchanged",level.getBlockState(target.below()).equals(farmland));
            evidence.put("otherNeighborBlocksUnchanged",neighbors.entrySet().stream().allMatch(e -> level.getBlockState(e.getKey()).equals(e.getValue())));
            evidence.put("held",AdventureActions.equipmentStatus(actor)); evidence.put("activity",AdventureActions.activityStatus(actor.getUUID()));
            List<String> inventory=new ArrayList<>(); for(int i=0;i<actor.getInventory().getContainerSize();i++) inventory.add(actor.getInventory().getItem(i).toString());
            evidence.put("actualInventory",inventory);
            evidence.put("operatorRemainingHand",player.getMainHandItem().toString());
            Files.createDirectories(directory);
            Files.writeString(directory.resolve("harvest-probe-"+System.currentTimeMillis()+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(evidence));
            say(source,"Harvest probe "+(harvested ? "passed" : hoeGiven && seedGiven ? "provisioned; awaiting real controller travel/harvest" : "did not complete")+"; intervention and native state recorded.");
        } catch (Exception failure) { say(source,"Harvest probe failed: "+failure.getMessage()); return 0; }
        finally { player.setShiftKeyDown(crouching); player.teleportTo(level,original.x,original.y,original.z,Set.of(),yaw,pitch,true); }
        return harvested ? 1 : 0;
    }
    private static void say(CommandSourceStack source,String message) { source.sendSuccess(() -> Component.literal("[Villagers] "+message),false); }
}
