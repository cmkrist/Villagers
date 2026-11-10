package local.villagers.mood;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import local.villagers.genetics.Trait;
import local.villagers.mind.Episode;

/**
 * How a villager feels, in [-1, 1], with the reasons behind it. Derived, not saved: it is recomputed from needs,
 * health, memories of the last {@link #RECENT_TICKS} ticks, and traits.
 */
public record Mood(double value, Band band, List<String> reasons) {
    public static final long RECENT_TICKS = 48_000; // Two in-game days.

    public enum Band {
        MISERABLE, UNHAPPY, CONTENT, HAPPY;
        static Band of(double value) {
            return value < -0.5 ? MISERABLE : value < -0.15 ? UNHAPPY : value > 0.45 ? HAPPY : CONTENT;
        }
    }

    public Mood { reasons = List.copyOf(reasons); }

    public static Mood neutral() { return new Mood(0.2, Band.CONTENT, List.of()); }

    public boolean low() { return band == Band.UNHAPPY || band == Band.MISERABLE; }

    private record Factor(String reason, double effect) {}

    /**
     * @param hunger, fatigue, loneliness needs in [0, 1]; {@code health} fraction of maximum
     * @param episodes the villager's memorable events; only recent ones count
     */
    public static Mood compute(double hunger, double fatigue, double loneliness, double health, List<Episode> episodes,
                               long now, Set<Trait> traits) {
        List<Factor> factors = new ArrayList<>();
        // Gluttons mind hunger more; loners barely mind solitude; the lazy mind tiredness more.
        factors.add(new Factor("hungry", -0.6 * hunger * (traits.contains(Trait.GLUTTON) ? 1.3 : 1)));
        factors.add(new Factor("tired", -0.35 * fatigue * (traits.contains(Trait.LAZY) ? 1.3 : 1)));
        factors.add(new Factor("lonely", -0.35 * loneliness * (traits.contains(Trait.LONER) ? 0.3 : traits.contains(Trait.GOSSIP) ? 1.3 : 1)));
        factors.add(new Factor("hurt", -0.5 * (1 - health)));
        for (Episode episode : episodes) {
            if (episode.tick() < now - RECENT_TICKS || episode.tick() > now) continue;
            switch (episode.kind()) {
                case RECEIVED_ITEM -> factors.add(new Factor("was given a gift", 0.12));
                case PROTECTED_BY_PLAYER -> factors.add(new Factor("was saved from a monster", 0.18));
                case HURT_BY_PLAYER -> factors.add(new Factor("was hurt by a player", traits.contains(Trait.BRAVE) ? -0.2 : -0.3));
                case CLAIMED_BED -> factors.add(new Factor("has a bed", 0.08));
                case BORN, HAD_CHILD -> factors.add(new Factor("family grew", 0.15));
                case GOD_CHILD -> factors.add(new Factor("is new to the village", 0.05));
            }
        }
        double value = 0.35;
        for (Factor factor : factors) value += factor.effect();
        value = Math.clamp(value, -1, 1);
        // Reasons: the strongest contributions, merged by name, that matter (|effect| >= 0.08).
        java.util.Map<String, Double> merged = new java.util.LinkedHashMap<>();
        for (Factor factor : factors) merged.merge(factor.reason(), factor.effect(), Double::sum);
        List<String> reasons = merged.entrySet().stream().filter(e -> Math.abs(e.getValue()) >= 0.08)
                .sorted(Comparator.comparingDouble((java.util.Map.Entry<String, Double> e) -> -Math.abs(e.getValue())))
                .limit(3).map(java.util.Map.Entry::getKey).toList();
        return new Mood(value, Band.of(value), reasons);
    }
}
