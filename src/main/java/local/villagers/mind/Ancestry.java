package local.villagers.mind;

import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

/** Pure immutable ancestry query; no live level/entity object crosses onto the IO worker. */
public final class Ancestry {
    public static final Comparator<AgentMind> ORDER = Comparator.comparingLong(AgentMind::ageTicks)
            .thenComparingLong(AgentMind::learningUpdates).thenComparing(AgentMind::id);
    public record Neighborhood(String dimension, double x, double y, double z, int radius, UUID fallback) {
        public Neighborhood {
            if (dimension == null || dimension.isBlank() || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || radius < 1 || radius > 256) throw new IllegalArgumentException("Invalid ancestry neighborhood");
            Objects.requireNonNull(fallback);
        }
        public boolean includes(UUID id, Home home) {
            if (fallback.equals(id)) return true;
            if (home == null || !dimension.equals(home.dimension())) return false;
            double dx = home.x() + .5 - x, dy = home.y() + .5 - y, dz = home.z() + .5 - z;
            return dx * dx + dy * dy + dz * dz <= (double) radius * radius;
        }
        public boolean includes(AgentMind.Snapshot snapshot) { return includes(snapshot.id(), snapshot.home()); }
    }
    private Ancestry() {}

    public static AgentMind.Snapshot better(AgentMind.Snapshot a, AgentMind.Snapshot b) {
        int age = Long.compare(a.ageTicks(), b.ageTicks());
        if (age != 0) return age > 0 ? a : b;
        int updates = Long.compare(a.learningUpdates(), b.learningUpdates());
        if (updates != 0) return updates > 0 ? a : b;
        return a.id().compareTo(b.id()) >= 0 ? a : b;
    }
}
