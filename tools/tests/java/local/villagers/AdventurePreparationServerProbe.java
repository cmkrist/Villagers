package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;

/** New isolated supplied-input fixture: ordinary production consumes native recipes, no supplied tool. */
public final class AdventurePreparationServerProbe implements ModInitializer {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private Villager actor;
    private UUID id;
    private boolean finished, restart, loadVerified;
    private int ticks;
    private JsonObject prior;
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static Map<String, Integer> materials(Villager v) {
        return Map.of("logs", v.getInventory().countItem(Items.OAK_LOG), "planks", v.getInventory().countItem(Items.OAK_PLANKS),
            "sticks", v.getInventory().countItem(Items.STICK), "pickaxes", v.getInventory().countItem(Items.WOODEN_PICKAXE));
    }
    @Override public void onInitialize() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (!(entity instanceof Villager v)) return;
            var before = level.getServer().getWorldPath(LevelResource.ROOT).resolve("adventure-prepare-before-restart.json");
            if (!Files.exists(before)) return;
            try {
                prior = JsonParser.parseString(Files.readString(before)).getAsJsonObject();
                if (!v.getUUID().toString().equals(prior.get("actor").getAsString())) return;
                check(AdventurePreparation.nominated(v), "nomination persists in actual body");
                check(JSON.toJsonTree(materials(v)).equals(prior.get("materials")), "native crafting inputs, spares and pickaxe persist");
                check(v.getHealth() == prior.get("health").getAsFloat(), "health persisted at load");
                check(v.getMainHandItem().toString().equals(prior.get("held").getAsString()), "original equipped item preserved");
                actor = v; id = v.getUUID(); loadVerified = true;
            } catch (Exception error) { throw new RuntimeException(error); }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (finished) return;
            var level = server.overworld(); var file = server.getWorldPath(LevelResource.ROOT).resolve("adventure-prepare-result.json");
            try {
                ticks++;
                check(ticks < 2400, "preparation observation exhausted");
                if (!WarCampaign.ready(level)) return;
                restart = Files.exists(file.resolveSibling("adventure-prepare-before-restart.json"));
                if (!restart && actor == null && ticks >= 30) {
                    actor = AdventurePreparation.nominate(level);
                    if (actor == null) return;
                    id = actor.getUUID();
                    // Declared setup only: two real logs and eight rations in an existing resident's bag.
                    actor.getInventory().clearContent(); actor.getInventory().addItem(new ItemStack(Items.OAK_LOG, 2));
                    actor.getInventory().addItem(new ItemStack(Items.BREAD, 8));
                    var town = WarCombat.home(actor);
                    var table = new BlockPos(town.x() - 16, local.villagers.war.WarMap.height(town.x(), town.z()) + 1, town.z() - 16);
                    actor.snapTo(table.getX() - 1.0, table.getY(), table.getZ() + .5, 0, 0);
                    check(level.getBlockState(table).is(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE), "actual authored table");
                }
                if (actor == null) return;
                check(actor.isAlive() && actor.getHealth() == actor.getMaxHealth(), "candidate remains healthy");
                var state = Villagers.existingSession(id); if (state == null) return;
                if (restart && ticks < 100) return;
                if (!AdventurePreparation.pickaxe(actor)) return;
                check(materials(actor).equals(Map.of("logs", 0, "planks", 3, "sticks", 2, "pickaxes", 1)), "exact native recipe costs and spares");
                check(!restart || loadVerified, "same body verified before AI on restart");
                check(AdventurePreparation.nominate(level) == actor, "repeat nomination preserves identity and supplies nothing");
                Files.writeString(file, JSON.toJson(Map.of("status", "passed", "actor", id.toString(), "restart", restart,
                    "gameTime", level.getGameTime(), "health", actor.getHealth(), "held", actor.getMainHandItem().toString(),
                    "materials", materials(actor), "craftsThisSession", state.adventureCrafts,
                    "fixture", "Existing adult with two supplied logs/eight rations at an existing town table; ordinary controller and native recipes; no supplied pickaxe or post-setup setters. Does not prove wood gathering/mining/adventure victory.")));
                finished = true; server.halt(false);
            } catch (Throwable error) {
                error.printStackTrace(); finished = true;
                try { Files.writeString(file, JSON.toJson(Map.of("status", "failed", "failure", error.toString()))); } catch (Exception ignored) {}
                server.halt(false);
            }
        });
    }
}
