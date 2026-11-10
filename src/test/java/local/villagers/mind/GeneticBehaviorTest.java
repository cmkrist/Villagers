package local.villagers.mind;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import local.villagers.genetics.Genome;
import local.villagers.genetics.Trait;
import local.villagers.genetics.TraitGene;
import local.villagers.genetics.TraitGene.Allele;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class GeneticBehaviorTest {
    @TempDir Path root;

    /** A villager whose genes are neutral except for the given homozygous trait, with fixed middling tendencies. */
    private static AgentMind with(Trait trait) {
        Genome genome = Genome.founder(new UUID(9, 9), Map.of(
                local.villagers.genetics.Tendency.BOLDNESS, .5, local.villagers.genetics.Tendency.SOCIABILITY, .5,
                local.villagers.genetics.Tendency.CURIOSITY, .5, local.villagers.genetics.Tendency.APPETITE, .5));
        List<Genome.TraitPair> pairs = new ArrayList<>();
        for (Genome.TraitPair pair : genome.traitGenes()) {
            Allele allele = Allele.NEUTRAL;
            if (trait != null && pair.gene().trait(Allele.FIRST) == trait) allele = Allele.FIRST;
            if (trait != null && pair.gene().trait(Allele.SECOND) == trait) allele = Allele.SECOND;
            pairs.add(new Genome.TraitPair(pair.gene(), allele, allele));
        }
        AgentMind mind = AgentMind.born(UUID.randomUUID(), new Genome(pairs, genome.tendencyGenes()));
        assertEquals(trait == null ? java.util.Set.of() : java.util.Set.of(trait), mind.traits());
        return mind;
    }

    private static Map<Action, Integer> choices(AgentMind mind, Observation observation) {
        Map<Action, Integer> counts = new EnumMap<>(Action.class);
        Random random = new Random(11);
        for (int i = 0; i < 2000; i++) counts.merge(mind.choose(observation, random), 1, Integer::sum);
        return counts;
    }

    @Test void gossipsAndLonersChooseDifferentlyUnderTheSameNeeds() {
        Observation lonely = new Observation(.3, .3, .5, 1, false, false, true, true);
        int gossip = choices(with(Trait.GOSSIP), lonely).getOrDefault(Action.SOCIALIZE, 0);
        int loner = choices(with(Trait.LONER), lonely).getOrDefault(Action.SOCIALIZE, 0);
        assertTrue(gossip > loner + 1000, "gossip " + gossip + " vs loner " + loner);
    }

    @Test void lazyVillagersRestMoreThanDiligentOnes() {
        // Mildly hungry and mildly tired: diligent villagers go for food, lazy ones lie down.
        Observation tired = new Observation(.3, .3, .1, 1, false, false, true, true);
        int lazy = choices(with(Trait.LAZY), tired).getOrDefault(Action.REST, 0);
        int diligent = choices(with(Trait.DILIGENT), tired).getOrDefault(Action.REST, 0);
        assertTrue(lazy > diligent + 1000, "lazy " + lazy + " vs diligent " + diligent);
    }

    @Test void braveVillagersMayWanderAtNightAndCowardsStayInWhenHurt() {
        Observation night = new Observation(.1, .1, .1, 1, false, true, true, true);
        assertTrue(with(Trait.BRAVE).allowed(Action.EXPLORE, night));
        assertFalse(with(null).allowed(Action.EXPLORE, night));
        Observation hurt = new Observation(.1, .1, .1, .5, false, false, true, true);
        assertFalse(with(Trait.COWARD).allowed(Action.EXPLORE, hurt));
        assertTrue(with(null).allowed(Action.EXPLORE, hurt));
        assertFalse(with(Trait.BRAVE).allowed(Action.EXPLORE, new Observation(.1, .1, .1, .5, false, true, true, true)),
                "even the brave don't wander hurt at night");
    }

    @Test void traitsChangeHowFastNeedsGrow() {
        AgentMind glutton = with(Trait.GLUTTON), frugal = with(Trait.FRUGAL), loner = with(Trait.LONER), gossip = with(Trait.GOSSIP);
        for (AgentMind mind : List.of(glutton, frugal, loner, gossip)) mind.advanceDrives(12_000);
        assertTrue(glutton.hunger() > frugal.hunger() + .1);
        assertTrue(gossip.loneliness() > loner.loneliness() + .1);
    }

    @Test void genomeSurvivesReloadAndOlderSavesKeepTheirPersonality() throws Exception {
        AgentMind mind = with(Trait.GREEDY);
        AgentStore store = AgentStore.worldScoped(root);
        assertTrue(store.save(mind), store.lastError());
        AgentMind restored = AgentStore.worldScoped(root).load(mind.id());
        assertEquals(mind.genome(), restored.genome());
        assertEquals(mind.traits(), restored.traits());

        AgentMind veteran = new AgentMind(UUID.randomUUID());
        JsonObject legacy = new Gson().toJsonTree(veteran.snapshot()).getAsJsonObject();
        legacy.addProperty("schemaVersion", 3);
        legacy.remove("genome");
        Path file = Files.createDirectories(root.resolve(veteran.id().toString())).resolve("mind.json");
        Files.writeString(file, legacy.toString());
        AgentMind upgraded = store.load(veteran.id());
        assertNull(store.lastError());
        assertEquals(veteran.personality(), upgraded.personality(), "upgrading doesn't change who a villager is");
        assertEquals(veteran.genome(), upgraded.genome(), "the derived founder genome is the same one a new villager gets");
    }

    @Test void newbornsExpressTheirInheritedGenome() {
        AgentMind mother = with(Trait.GOSSIP), father = with(Trait.GOSSIP);
        AgentMind child = AgentMind.born(UUID.randomUUID(), Genome.inherit(mother.genome(), father.genome(), new Random(1)));
        assertEquals(child.genome().tendency(local.villagers.genetics.Tendency.CURIOSITY), child.personality().curiosity(), 1e-12);
        assertTrue(child.genome().pair(TraitGene.SOCIABILITY).first() != null);
    }
}
