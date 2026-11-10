package local.villagers.genetics;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import local.villagers.genetics.TraitGene.Allele;

/**
 * A villager's inherited genes: one allele pair per {@link TraitGene}, and {@link #LOCI} additive allele pairs
 * per {@link Tendency}. Children take one allele of each pair from each parent, independently, with rare mutation.
 * Immutable and JSON-friendly.
 */
public record Genome(List<TraitPair> traitGenes, List<TendencyGenes> tendencyGenes) {
    public static final int LOCI = 4;
    public static final double MUTATION_RATE = 0.01;
    public static final double GOD_CHILD_MUTATION_RATE = 0.1;
    /** Founder allele frequencies: each trait allele 0.32, so each trait is expressed in about 10% of founders. */
    static final double FOUNDER_TRAIT_ALLELE = 0.32;
    static final double TENDENCY_MUTATION_SD = 0.15;
    private static final long FOUNDER_SALT = 0x5EED_6E40_4E5EL;

    /** The two alleles of one trait gene, one from each parent. */
    public record TraitPair(TraitGene gene, Allele first, Allele second) {
        public TraitPair { Objects.requireNonNull(gene); Objects.requireNonNull(first); Objects.requireNonNull(second); }
        public Trait expressed() { return first == second ? gene.trait(first) : null; }
    }

    /** {@link #LOCI} allele values from each parent for one tendency. */
    public record TendencyGenes(Tendency tendency, double[] first, double[] second) {
        public TendencyGenes {
            Objects.requireNonNull(tendency);
            first = checked(first);
            second = checked(second);
        }
        @Override public double[] first() { return first.clone(); }
        @Override public double[] second() { return second.clone(); }
        public double expressed() {
            double sum = 0;
            for (int i = 0; i < LOCI; i++) sum += first[i] + second[i];
            return sum / (2 * LOCI);
        }
        @Override public boolean equals(Object other) {
            return other instanceof TendencyGenes that && tendency == that.tendency
                    && java.util.Arrays.equals(first, that.first) && java.util.Arrays.equals(second, that.second);
        }
        @Override public int hashCode() {
            return Objects.hash(tendency, java.util.Arrays.hashCode(first), java.util.Arrays.hashCode(second));
        }
        private static double[] checked(double[] values) {
            if (values == null || values.length != LOCI) throw new IllegalArgumentException("Each tendency needs " + LOCI + " loci");
            for (double value : values)
                if (!Double.isFinite(value) || value < 0 || value > 1) throw new IllegalArgumentException("Allele values must be in [0, 1]");
            return values.clone();
        }
    }

    public Genome {
        traitGenes = List.copyOf(Objects.requireNonNull(traitGenes));
        tendencyGenes = List.copyOf(Objects.requireNonNull(tendencyGenes));
        Set<TraitGene> genes = EnumSet.noneOf(TraitGene.class);
        for (TraitPair pair : traitGenes) if (!genes.add(pair.gene())) throw new IllegalArgumentException("Duplicate gene " + pair.gene());
        if (genes.size() != TraitGene.values().length) throw new IllegalArgumentException("Every trait gene must be present");
        Set<Tendency> tendencies = EnumSet.noneOf(Tendency.class);
        for (TendencyGenes t : tendencyGenes) if (!tendencies.add(t.tendency())) throw new IllegalArgumentException("Duplicate tendency " + t.tendency());
        if (tendencies.size() != Tendency.values().length) throw new IllegalArgumentException("Every tendency must be present");
    }

    /** Traits this villager shows, in gene order. */
    public Set<Trait> traits() {
        Set<Trait> result = EnumSet.noneOf(Trait.class);
        for (TraitPair pair : traitGenes) {
            Trait trait = pair.expressed();
            if (trait != null) result.add(trait);
        }
        return java.util.Collections.unmodifiableSet(result);
    }

    public boolean has(Trait trait) { return traits().contains(trait); }

    public double tendency(Tendency tendency) {
        for (TendencyGenes genes : tendencyGenes) if (genes.tendency() == tendency) return genes.expressed();
        throw new IllegalStateException("Missing tendency " + tendency);
    }

    public TraitPair pair(TraitGene gene) {
        for (TraitPair pair : traitGenes) if (pair.gene() == gene) return pair;
        throw new IllegalStateException("Missing gene " + gene);
    }

    /**
     * A first-generation genome seeded from the villager's UUID. {@code fixed} pins tendencies that a villager
     * already has (from older saves), so upgrading doesn't change how bold, sociable or curious it is.
     */
    public static Genome founder(UUID id, Map<Tendency, Double> fixed) {
        Random random = new Random(id.getMostSignificantBits() * 31 ^ id.getLeastSignificantBits() ^ FOUNDER_SALT);
        List<TraitPair> traits = new ArrayList<>();
        for (TraitGene gene : TraitGene.values())
            traits.add(new TraitPair(gene, founderAllele(random), founderAllele(random)));
        List<TendencyGenes> tendencies = new ArrayList<>();
        for (Tendency tendency : Tendency.values()) {
            // Unpinned founder tendencies vary around the middle, so existing villagers keep similar drive rates.
            double target = fixed.containsKey(tendency) ? fixed.get(tendency) : 0.3 + 0.4 * random.nextDouble();
            target = Math.clamp(target, 0, 1);
            double[] first = new double[LOCI], second = new double[LOCI];
            // Pairs of +d / -d around the target: varied alleles whose average is exactly the target.
            double spread = Math.min(target, 1 - target);
            for (int i = 0; i < LOCI; i++) {
                double d = spread * random.nextDouble();
                first[i] = target + d;
                second[i] = target - d;
            }
            tendencies.add(new TendencyGenes(tendency, first, second));
        }
        return new Genome(traits, tendencies);
    }

    private static Allele founderAllele(Random random) {
        double roll = random.nextDouble();
        return roll < FOUNDER_TRAIT_ALLELE ? Allele.FIRST : roll < 2 * FOUNDER_TRAIT_ALLELE ? Allele.SECOND : Allele.NEUTRAL;
    }

    /** A child's genome: one allele of each pair from each parent, with mutation at {@link #MUTATION_RATE}. */
    public static Genome inherit(Genome mother, Genome father, RandomGenerator random) {
        return inherit(mother, father, random, MUTATION_RATE);
    }

    /**
     * A god child's genome: the ancestor's own genes recombined with themselves (one allele of each pair, picked
     * twice) and mutated at {@link #GOD_CHILD_MUTATION_RATE}, ten times the usual rate. The bloodline continues,
     * but the child is never a copy.
     */
    public static Genome godChild(Genome ancestor, RandomGenerator random) {
        return inherit(ancestor, ancestor, random, GOD_CHILD_MUTATION_RATE);
    }

    static Genome inherit(Genome a, Genome b, RandomGenerator random, double mutationRate) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        List<TraitPair> traits = new ArrayList<>();
        for (TraitGene gene : TraitGene.values()) {
            traits.add(new TraitPair(gene,
                    mutate(pick(a.pair(gene), random), random, mutationRate),
                    mutate(pick(b.pair(gene), random), random, mutationRate)));
        }
        Map<Tendency, TendencyGenes> fromA = byTendency(a), fromB = byTendency(b);
        List<TendencyGenes> tendencies = new ArrayList<>();
        for (Tendency tendency : Tendency.values()) {
            double[] first = new double[LOCI], second = new double[LOCI];
            for (int i = 0; i < LOCI; i++) {
                first[i] = mutate(pick(fromA.get(tendency), i, random), random, mutationRate);
                second[i] = mutate(pick(fromB.get(tendency), i, random), random, mutationRate);
            }
            tendencies.add(new TendencyGenes(tendency, first, second));
        }
        return new Genome(traits, tendencies);
    }

    private static Map<Tendency, TendencyGenes> byTendency(Genome genome) {
        Map<Tendency, TendencyGenes> result = new EnumMap<>(Tendency.class);
        for (TendencyGenes genes : genome.tendencyGenes) result.put(genes.tendency(), genes);
        return result;
    }

    private static Allele pick(TraitPair pair, RandomGenerator random) { return random.nextBoolean() ? pair.first() : pair.second(); }

    private static double pick(TendencyGenes genes, int locus, RandomGenerator random) {
        return random.nextBoolean() ? genes.first[locus] : genes.second[locus];
    }

    private static Allele mutate(Allele allele, RandomGenerator random, double rate) {
        if (random.nextDouble() >= rate) return allele;
        Allele[] others = java.util.Arrays.stream(Allele.values()).filter(a -> a != allele).toArray(Allele[]::new);
        return others[random.nextInt(others.length)];
    }

    private static double mutate(double value, RandomGenerator random, double rate) {
        if (random.nextDouble() >= rate) return value;
        return Math.clamp(value + random.nextGaussian() * TENDENCY_MUTATION_SD, 0, 1);
    }
}
