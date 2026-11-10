package local.villagers.war;

import java.util.List;

/** The scenario's chunk-aligned geometry. Coordinates are blocks unless stated otherwise. */
public final class WarMap {
    public static final String WORLD = "FourBiomesWar";
    public static final int CHUNKS = 64, QUARTER_CHUNKS = 32, HALF = 512, SIZE = 1024;
    public static final int TOWN_RADIUS = 34, HOME_ROAM_RADIUS = 48, ROAM_STEP = 12;
    public static final int SIMULATION_MIN_CHUNK = -12, SIMULATION_MAX_CHUNK = 11;
    public enum Region { GRASSLAND, FOREST, SNOWY_MOUNTAINS, DESERT }
    public enum Faction { VILLAGERS, ILLAGERS, NEUTRAL }
    public record Settlement(String name, Region region, Faction faction, int x, int z) {}
    public static final List<Settlement> SETTLEMENTS = List.of(
        new Settlement("Greenhaven", Region.GRASSLAND, Faction.VILLAGERS, -96, -96),
        new Settlement("Blackwood", Region.FOREST, Faction.ILLAGERS, 96, -96),
        new Settlement("Frostwatch", Region.SNOWY_MOUNTAINS, Faction.ILLAGERS, -96, 96),
        new Settlement("Sunwell", Region.DESERT, Faction.VILLAGERS, 96, 96));

    private WarMap() {}
    public static boolean contains(int x, int z) { return x >= -HALF && x < HALF && z >= -HALF && z < HALF; }
    public static Region region(int x, int z) {
        return z < 0 ? (x < 0 ? Region.GRASSLAND : Region.FOREST)
            : (x < 0 ? Region.SNOWY_MOUNTAINS : Region.DESERT);
    }

    /** Smooth lowlands, mountain ridges and desert dunes, with accessible settlement terraces. */
    public static int height(int x, int z) {
        double hills = 3 * Math.sin(x / 43.0) * Math.sin(z / 51.0);
        double height = 72 + hills;
        if (region(x,z) == Region.SNOWY_MOUNTAINS) {
            double ramp = Math.min(1, Math.min(-x, z) / 90.0);
            height += ramp * (38 + 32 * Math.pow(Math.sin(x / 95.0) * Math.cos(z / 100.0), 2));
        } else if (region(x,z) == Region.DESERT) {
            height += Math.min(1, Math.min(x,z) / 48.0) * (4 + 4 * Math.sin(x / 30.0) * Math.cos(z / 38.0));
        }
        for (Settlement settlement : SETTLEMENTS) {
            double distance = Math.max(Math.abs(x - settlement.x), Math.abs(z - settlement.z));
            double blend = Math.clamp((distance - 34) / 22.0, 0, 1);
            if (blend < 1) {
                int terrace = settlement.region == Region.SNOWY_MOUNTAINS ? 104 : 74;
                height = terrace * (1 - blend) + height * blend;
            }
        }
        return (int) Math.round(height);
    }
}
