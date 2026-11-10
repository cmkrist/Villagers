package local.villagers.mind;

import java.util.Objects;

/** A block position in a named dimension, kept free of Minecraft types so the mind stays testable. */
public record Place(String dimension, int x, int y, int z) {
    public Place {
        if (dimension == null || dimension.isBlank() || dimension.length() > 256)
            throw new IllegalArgumentException("A place needs a dimension identifier");
    }

    public static Place of(Home home) {
        Objects.requireNonNull(home);
        return new Place(home.dimension(), home.x(), home.y(), home.z());
    }
}
