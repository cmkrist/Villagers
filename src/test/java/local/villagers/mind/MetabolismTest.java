package local.villagers.mind;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.UUID;
import local.villagers.genetics.Genome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** The player's FoodData rules, applied to villagers, plus god-child lineage and schema 5 persistence. */
class MetabolismTest {
    @TempDir Path root;

    @Test void exhaustionSpendsSaturationBeforeFood() {
        Metabolism food = new Metabolism(20, 2);
        food.exert(8.5f); // Two points: both from saturation.
        assertEquals(20, food.foodLevel());
        assertEquals(0, food.saturation());
        food.exert(4.0f); // Total remainder 4.5: one food point.
        assertEquals(19, food.foodLevel());
        assertEquals(0.5f, food.exhaustion(), 1e-6);
    }

    @Test void eatingUsesItemValuesAndCapsSaturationAtFoodLevel() {
        Metabolism food = new Metabolism(10, 0);
        food.eat(5, 6f); // Bread.
        assertEquals(15, food.foodLevel());
        assertEquals(6f, food.saturation(), 1e-6);
        food.eat(1, 20f);
        assertEquals(16, food.saturation(), 1e-6, "saturation never exceeds the food level");
        food.eat(10, 0);
        assertEquals(20, food.foodLevel());
        assertFalse(food.canEat(), "a full villager cannot eat, like a player");
    }

    @Test void regeneratesLikeAPlayerAndPaysForIt() {
        Metabolism nearlyFull = new Metabolism(18, 0);
        Metabolism.Vitals vitals = nearlyFull.tick(80, 10, 20, true);
        assertEquals(1, vitals.healing(), 1e-6, "one heart-half per 80 ticks at 18+ food");
        assertEquals(1.5f, nearlyFull.exhaustion() + 4 * (18 - nearlyFull.foodLevel()) - 4.5f, 1e-6, "costs 6.0 exhaustion");
        Metabolism saturated = new Metabolism(20, 12);
        assertEquals(1, saturated.tick(10, 10, 20, true).healing(), 1e-6, "fast saturated regeneration every 10 ticks");
        assertEquals(Metabolism.Vitals.NONE, new Metabolism(20, 5).tick(200, 20, 20, true), "no healing at full health");
        assertEquals(Metabolism.Vitals.NONE, new Metabolism(20, 5).tick(200, 10, 20, false), "gamerule respected");
    }

    @Test void starvationIsAlwaysLethal() {
        Metabolism empty = new Metabolism(0, 0);
        assertEquals(0, empty.tick(79, 1, 20, true).starvationHits());
        assertEquals(1, empty.tick(1, 1, 20, true).starvationHits(), "hits even at 1 health: never spared");
        assertEquals(20, new Metabolism(0, 0).tick(1600, 20, 20, true).starvationHits(), "a full villager starves in 80 s");
        assertEquals(0, new Metabolism(1, 0).tick(1600, 20, 20, true).starvationHits());
    }

    @Test void cannotRunWhenLowOnFood() {
        assertTrue(new Metabolism(7, 0).canSprint());
        assertFalse(new Metabolism(6, 0).canSprint());
    }

    @Test void baselineMetabolismEmptiesTheBarInAboutADay() {
        AgentMind mind = new AgentMind(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertEquals(16, mind.foodLevel());
        assertEquals(0.2, mind.hunger(), 1e-12);
        mind.advanceDrives(24_000, 0);
        assertEquals(16, mind.foodLevel(), "with no baseline only activity costs food, as for a player");
        mind.advanceDrives(24_000, 80);
        assertTrue(mind.foodLevel() <= 3, "80 exhaustion/day is 20 points: saturation 5 then most of the bar");
        assertEquals(48_000, mind.ageTicks());
    }

    @Test void foodAgeLineageAndDeathSurviveSaving() {
        AgentMind ancestor = new AgentMind(UUID.randomUUID());
        ancestor.advanceDrives(5_000);
        ancestor.die(1234, Death.STARVATION);
        AgentMind child = AgentMind.godChild(UUID.randomUUID(), ancestor, new Random(3));
        child.advanceDrives(3_000);
        child.exert(2.5f);
        AgentStore store = AgentStore.worldScoped(root);
        assertTrue(store.save(child));
        assertTrue(store.save(ancestor));
        AgentMind restored = store.load(child.id());
        assertEquals(child.snapshot().food(), restored.snapshot().food());
        assertEquals(child.ageTicks(), restored.ageTicks());
        assertEquals(new Lineage(ancestor.id(), ancestor.name(), 1, true), restored.lineage());
        assertTrue(store.load(ancestor.id()).death().starved());
        assertEquals(2, store.readAll().size(), "census sees living and dead");
    }

    @Test void schemaFourSavesKeepTheirHungerAsTheFoodBar() throws Exception {
        AgentMind veteran = new AgentMind(UUID.randomUUID());
        JsonObject legacy = new Gson().toJsonTree(veteran.snapshot()).getAsJsonObject();
        legacy.addProperty("schemaVersion", 4);
        legacy.addProperty("hunger", 0.75);
        for (String added : new String[] {"food", "ageTicks", "lineage", "death"}) legacy.remove(added);
        Path file = Files.createDirectories(root.resolve(veteran.id().toString())).resolve("mind.json");
        Files.writeString(file, legacy.toString());
        AgentMind upgraded = AgentStore.worldScoped(root).load(veteran.id());
        assertEquals(5, upgraded.foodLevel());
        assertEquals(0.75, upgraded.hunger(), 1e-12);
        assertEquals(0, upgraded.ageTicks());
        assertNull(upgraded.lineage());
        assertEquals(AgentMind.SCHEMA_VERSION, upgraded.snapshot().schemaVersion());
    }

    @Test void godChildrenContinueTheBloodlineWithMutationAndAFullBar() {
        AgentMind ancestor = new AgentMind(UUID.randomUUID());
        Random random = new Random(11);
        int differing = 0;
        for (int i = 0; i < 50; i++) {
            AgentMind child = AgentMind.godChild(UUID.randomUUID(), ancestor, random);
            assertEquals(20, child.foodLevel());
            assertEquals(1, child.generation());
            assertEquals(2, AgentMind.godChild(UUID.randomUUID(), child, random).generation());
            if (!child.genome().equals(ancestor.genome())) differing++;
        }
        assertEquals(50, differing, "the ancestor is never copied exactly");
        Genome selfed = Genome.godChild(ancestor.genome(), new Random(1));
        assertNotNull(selfed);
    }

    @Test void fullVillagersCannotForage() {
        AgentMind mind = new AgentMind(UUID.randomUUID());
        assertFalse(mind.allowed(Action.FORAGE, new Observation(0, 0, 0, 1, false, false, true, true)));
        assertTrue(mind.allowed(Action.FORAGE, new Observation(0.05, 0, 0, 1, false, false, true, true)));
    }
}
