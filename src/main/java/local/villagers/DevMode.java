package local.villagers;

import net.minecraft.server.MinecraftServer;

/**
 * Development and research tooling (lab fixtures, trials, probes, reports) is only registered when the
 * JVM is started with {@code -Dvillagers.lab=true}, as the Gradle dev runs do. Release installs never see it.
 */
public final class DevMode {
    public static final String LAB_WORLD = "VillagerLab";
    public static final String TRIAL_WORLD = "native-regions".equals(System.getProperty("villagers.trial")) ? "NativeFourRegions" : "VillageTrial";

    private DevMode() {}

    public static boolean enabled() { return Boolean.getBoolean("villagers.lab"); }

    /** Dev mode is on and the server is running the named disposable world. */
    public static boolean inWorld(MinecraftServer server, String worldName) {
        return enabled() && worldName.equals(server.getWorldData().getLevelName());
    }
}
