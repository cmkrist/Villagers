package local.villagers.mind;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Founders have no lineage. Legacy ancestor fields retain their meaning for divine succession. */
public record Lineage(UUID ancestor, String ancestorName, int generation, boolean godChild,
                      UUID secondParent, String secondParentName) {
    public static final int MAX_NAME = 64;

    /** Source and JSON compatibility with schema5/6 god children. */
    public Lineage(UUID ancestor, String ancestorName, int generation, boolean godChild) {
        this(ancestor, ancestorName, generation, godChild, null, null);
    }

    public Lineage {
        Objects.requireNonNull(ancestor);
        checkName(ancestorName);
        if (generation < 1) throw new IllegalArgumentException("A descendant is at least generation 1");
        if (godChild) {
            if (secondParent != null || secondParentName != null) throw new IllegalArgumentException("Divine succession has one ancestor");
        } else {
            if (secondParent == null) throw new IllegalArgumentException("Ordinary births require a second parent");
            checkName(secondParentName);
            if (ancestor.equals(secondParent)) throw new IllegalArgumentException("Ordinary births require distinct parents");
        }
    }

    private static void checkName(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME) throw new IllegalArgumentException("Invalid parent/ancestor name");
    }

    public Set<UUID> parents() { return godChild ? Set.of(ancestor) : Set.of(ancestor, secondParent); }
    public static int nextGeneration(int first, int second) {
        return Math.max(first, second) == Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(first, second) + 1;
    }
}
