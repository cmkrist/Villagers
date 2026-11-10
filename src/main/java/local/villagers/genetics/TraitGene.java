package local.villagers.genetics;

/**
 * A gene with three alleles: a dominant neutral allele and two recessive trait alleles.
 * A trait shows only when both copies carry it, so two calm parents can have a hot-headed child,
 * and nobody can express both opposites.
 */
public enum TraitGene {
    COURAGE(Trait.COWARD, Trait.BRAVE),
    GENEROSITY(Trait.GREEDY, Trait.GENEROUS),
    SOCIABILITY(Trait.GOSSIP, Trait.LONER),
    APPETITE(Trait.GLUTTON, Trait.FRUGAL),
    INDUSTRY(Trait.DILIGENT, Trait.LAZY);

    private final Trait first, second;

    TraitGene(Trait first, Trait second) { this.first = first; this.second = second; }

    public Trait trait(Allele allele) {
        return switch (allele) {
            case NEUTRAL -> null;
            case FIRST -> first;
            case SECOND -> second;
        };
    }

    public enum Allele { NEUTRAL, FIRST, SECOND }
}
