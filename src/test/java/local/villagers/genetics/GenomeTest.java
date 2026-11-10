package local.villagers.genetics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import local.villagers.genetics.Genome.TraitPair;
import local.villagers.genetics.TraitGene.Allele;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GenomeTest {
    private static final int BIRTHS = 20_000;

    /** A founder genome with one trait gene replaced. */
    static Genome withGene(Genome base, TraitGene gene, Allele first, Allele second) {
        List<TraitPair> pairs = new ArrayList<>();
        for (TraitPair pair : base.traitGenes()) pairs.add(pair.gene() == gene ? new TraitPair(gene, first, second) : pair);
        return new Genome(pairs, base.tendencyGenes());
    }

    private static Genome calm(long seed) {
        Genome base = Genome.founder(new UUID(seed, seed), Map.of());
        for (TraitGene gene : TraitGene.values()) base = withGene(base, gene, Allele.NEUTRAL, Allele.NEUTRAL);
        return base;
    }

    @Test void twoCalmCarriersHaveAQuarterHotHeadedChildren() {
        Genome mother = withGene(calm(1), TraitGene.COURAGE, Allele.NEUTRAL, Allele.FIRST);
        Genome father = withGene(calm(2), TraitGene.COURAGE, Allele.FIRST, Allele.NEUTRAL);
        assertFalse(mother.has(Trait.COWARD));
        assertFalse(father.has(Trait.COWARD));
        Random random = new Random(42);
        int cowards = 0;
        for (int i = 0; i < BIRTHS; i++) if (Genome.inherit(mother, father, random, 0).has(Trait.COWARD)) cowards++;
        assertEquals(0.25, cowards / (double) BIRTHS, 0.015, "a recessive trait shows in a quarter of children");
    }

    @Test void homozygousParentsAlwaysPassATraitWithoutMutation() {
        Genome mother = withGene(calm(1), TraitGene.APPETITE, Allele.SECOND, Allele.SECOND);
        Genome father = withGene(calm(2), TraitGene.APPETITE, Allele.SECOND, Allele.SECOND);
        Random random = new Random(1);
        for (int i = 0; i < 1000; i++) assertTrue(Genome.inherit(mother, father, random, 0).has(Trait.FRUGAL));
    }

    @Test void opposingAllelesExpressNeitherTrait() {
        Genome genome = withGene(calm(1), TraitGene.COURAGE, Allele.FIRST, Allele.SECOND);
        assertFalse(genome.has(Trait.COWARD));
        assertFalse(genome.has(Trait.BRAVE));
    }

    @Test void mutationChangesAllelesAtTheConfiguredRate() {
        Genome parent = calm(3);
        Random random = new Random(7);
        int alleles = 0, changed = 0;
        for (int i = 0; i < BIRTHS; i++) {
            Genome child = Genome.inherit(parent, parent, random);
            for (TraitPair pair : child.traitGenes()) {
                alleles += 2;
                if (pair.first() != Allele.NEUTRAL) changed++;
                if (pair.second() != Allele.NEUTRAL) changed++;
            }
        }
        assertEquals(Genome.MUTATION_RATE, changed / (double) alleles, 0.002);
    }

    @Test void childTendenciesAverageTheirParents() {
        Genome low = Genome.founder(new UUID(1, 1), Map.of(Tendency.CURIOSITY, 0.2));
        Genome high = Genome.founder(new UUID(2, 2), Map.of(Tendency.CURIOSITY, 0.8));
        Random random = new Random(3);
        double sum = 0, min = 1, max = 0;
        for (int i = 0; i < BIRTHS; i++) {
            double value = Genome.inherit(low, high, random, 0).tendency(Tendency.CURIOSITY);
            sum += value; min = Math.min(min, value); max = Math.max(max, value);
        }
        assertEquals(0.5, sum / BIRTHS, 0.01);
        assertTrue(max - min > 0.1, "siblings differ");
    }

    @Test void foundersAreDeterministicKeepPinnedTendenciesAndShowEachTraitInAboutOneInTen() {
        UUID id = UUID.randomUUID();
        assertEquals(Genome.founder(id, Map.of()), Genome.founder(id, Map.of()));
        Genome pinned = Genome.founder(id, Map.of(Tendency.BOLDNESS, 0.93));
        assertEquals(0.93, pinned.tendency(Tendency.BOLDNESS), 1e-12);
        int[] counts = new int[Trait.values().length];
        Random random = new Random(5);
        for (int i = 0; i < BIRTHS; i++)
            for (Trait trait : Genome.founder(new UUID(random.nextLong(), random.nextLong()), Map.of()).traits()) counts[trait.ordinal()]++;
        double expected = Genome.FOUNDER_TRAIT_ALLELE * Genome.FOUNDER_TRAIT_ALLELE;
        for (Trait trait : Trait.values()) assertEquals(expected, counts[trait.ordinal()] / (double) BIRTHS, 0.015, trait.name());
    }

    @Test void rejectsIncompleteOrInvalidGenomes() {
        Genome base = calm(1);
        assertThrows(IllegalArgumentException.class, () -> new Genome(base.traitGenes().subList(1, base.traitGenes().size()), base.tendencyGenes()));
        assertThrows(IllegalArgumentException.class, () -> new Genome.TendencyGenes(Tendency.APPETITE, new double[]{.5, .5, .5, 2}, new double[4]));
        assertThrows(IllegalArgumentException.class, () -> new Genome.TendencyGenes(Tendency.APPETITE, new double[3], new double[4]));
    }
}
