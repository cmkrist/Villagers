package local.villagers;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import local.villagers.mind.AgentMind;
import local.villagers.war.WarMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.level.storage.LevelResource;

/** Isolated native control regression. Unspawned fixture bodies never alter the campaign population. */
public final class WarRecoveryServerProbe implements ModInitializer {
    private boolean finished;
    private int ticks;
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (finished) return;
            var level = server.overworld();
            var result = server.getWorldPath(LevelResource.ROOT).resolve("war-recovery-result.json");
            try {
                if (++ticks > 1200) throw new AssertionError("Scenario readiness timeout");
                if (!WarCampaign.ready(level) || level.getGameTime() < 210) return;
                var cases = new ArrayList<String>();
                for (int townIndex : new int[]{0, 1}) {
                    var town = WarMap.SETTLEMENTS.get(townIndex);
                    Mob body = townIndex == 0 ? new Villager(EntityTypes.VILLAGER, level)
                        : new Pillager(EntityTypes.PILLAGER, level);
                    body.addTag(WarScenario.ACTOR);
                    body.addTag(WarScenario.HOME + townIndex);
                    body.addTag(WarScenario.SOLDIER);
                    var mind = new AgentMind(body.getUUID());
                    int y = WarMap.height(town.x(), town.z()) + 1;
                    // Explicit fixture injury and positioning, never applied to saved residents.
                    body.setHealth(body.getMaxHealth() * .2f);
                    body.snapTo(town.x() + 16, y, town.z(), 0, 0);
                    check(WarCombat.tick(level, body, mind).startsWith("retreating"), "injured soldier returns home");
                    body.snapTo(town.x(), y, town.z(), 0, 0);
                    check(WarCombat.tick(level, body, mind) == null, "home arrival must allow local food and rest routines");
                    // Healing partway must not send the soldier straight back into battle.
                    body.setHealth(body.getMaxHealth() * .5f);
                    check(WarCombat.tick(level, body, mind) == null, "recovery hysteresis retains local care");
                    body.snapTo(town.x() + 16, y, town.z(), 0, 0);
                    check(WarCombat.tick(level, body, mind).startsWith("retreating"), "partly healed soldier still returns home");
                    body.snapTo(town.x(), y, town.z(), 0, 0);
                    body.setHealth(body.getMaxHealth());
                    String recovered = WarCombat.tick(level, body, mind);
                    check(recovered != null && !recovered.startsWith("retreating"), "recovered soldier resumes campaign");
                    check(WarCampaign.adultResidents(level, town) == 8, "eight actual adult residents, excluding trader and Enderman");
                    Mob removed = null;
                    for (var entity : level.getAllEntities()) if (entity instanceof Mob member
                        && WarCombat.faction(member) == town.faction() && member.entityTags().contains(WarScenario.HOME + townIndex)) { removed = member; break; }
                    check(removed != null, "actual town resident available");
                    // Isolated census fixture only: remove one adult, then add a tagged juvenile.
                    removed.discard();
                    body.snapTo(town.x() + 40, y, town.z(), 0, 0);
                    check(WarCombat.tick(level, body, mind).startsWith("regrouping"), "understrength expedition returns home");
                    body.snapTo(town.x(), y, town.z(), 0, 0);
                    check(WarCombat.tick(level, body, mind) == null, "home rebuilding hands control to food and family care");
                    Mob young = townIndex == 0 ? new Villager(EntityTypes.VILLAGER, level) : new Pillager(EntityTypes.PILLAGER, level);
                    young.addTag(WarScenario.ACTOR); young.addTag(WarScenario.HOME + townIndex);
                    young.snapTo(town.x() + 3, y, town.z(), 0, 0);
                    if (young instanceof Villager v) v.setAge(-24000); else IllagerFamilies.markJuvenile(young);
                    check(level.addFreshEntity(young), "juvenile census fixture added");
                    check(WarCampaign.adultResidents(level, town) == 7, "child never substitutes for an adult expedition garrison");
                    if (young instanceof Villager v) v.setAge(0);
                    else {
                        var grown = new AgentMind(young.getUUID()); grown.advanceDrives(24000, 0);
                        IllagerFamilies.care(level, young, new CreatureAgents.State(grown, level.getGameTime()));
                    }
                    check(WarCampaign.adultResidents(level, town) == 8, "adulthood restores garrison capacity");
                    check(WarCombat.tick(level, body, mind) != null, "rebuilt town can resume expedition");
                    WarCombat.forget(body.getUUID());
                    cases.add(body.typeHolder().getRegisteredName());
                }
                var town = WarMap.SETTLEMENTS.getFirst();
                var parent = new Villager(EntityTypes.VILLAGER, level);
                var other = new Villager(EntityTypes.VILLAGER, level);
                var baby = new Villager(EntityTypes.VILLAGER, level);
                parent.addTag(WarScenario.ACTOR); parent.addTag(WarScenario.HOME + 0);
                baby.setAge(-24000);
                baby.snapTo(town.x(), 75, town.z(), 0, 0);
                WarScenario.inheritTown(level, parent, other, baby, baby.blockPosition());
                check(WarCombat.faction(baby) == WarMap.Faction.VILLAGERS, "ordinary child joins faction census");
                check(baby.entityTags().contains(WarScenario.HOME + 0), "child inherits nursery town");
                check(!baby.entityTags().contains(WarScenario.SOLDIER), "birth does not enlist a soldier");
                baby.setHealth(1);
                check(WarCombat.tick(level, baby, new AgentMind(baby.getUUID())) == null, "child uses local survival instead of war retreat");
                var unrelated = new Villager(EntityTypes.VILLAGER, level);
                WarScenario.inheritTown(level, other, other, unrelated, baby.blockPosition());
                check(!unrelated.entityTags().contains(WarScenario.ACTOR), "unrelated family is not silently enrolled");
                Files.writeString(result, new GsonBuilder().setPrettyPrinting().create().toJson(
                    Map.of("status", "passed", "species", cases, "gameTime", level.getGameTime(),
                        "scope", "native control regression with explicitly injured unspawned fixture bodies; no sustained recovery claim")));
                finished = true;
                server.halt(false);
            } catch (Throwable failure) {
                failure.printStackTrace(); finished = true;
                try { Files.writeString(result, new GsonBuilder().create().toJson(
                    Map.of("status", "failed", "failure", failure.toString()))); } catch (Exception ignored) {}
                server.halt(false);
            }
        });
    }
}
