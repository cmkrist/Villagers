package local.villagers.mind;

public record Home(String dimension, int x, int y, int z) {
    public Home {
        if (dimension == null || dimension.isBlank() || dimension.length() > 256)
            throw new IllegalArgumentException("A home needs a dimension identifier");
    }
}
