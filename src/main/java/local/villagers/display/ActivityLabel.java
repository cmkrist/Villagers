package local.villagers.display;

import java.util.Objects;

/**
 * The floating "name · activity" label. The mod only writes over names it wrote itself,
 * so a player's name tag is never replaced or erased.
 */
public final class ActivityLabel {
    public static final String SEPARATOR = " · ";

    private ActivityLabel() {}

    public static String text(String mindName, String activity) {
        return Objects.requireNonNull(mindName) + SEPARATOR + Objects.requireNonNull(activity);
    }

    /** True when the current custom name is absent or is one of this mind's own activity labels. */
    public static boolean ownedBy(String mindName, String currentName) {
        return currentName == null || currentName.startsWith(Objects.requireNonNull(mindName) + SEPARATOR);
    }

    /** True when the current custom name is one of this mind's labels and can be cleared on release. */
    public static boolean isLabel(String mindName, String currentName) {
        return currentName != null && ownedBy(mindName, currentName);
    }
}
