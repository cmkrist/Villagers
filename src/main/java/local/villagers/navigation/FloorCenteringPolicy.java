package local.villagers.navigation;

/** A physical unstick may only centre within the resident's current cell, dropping at most one step. */
public final class FloorCenteringPolicy {
    private FloorCenteringPolicy() {}
    public static boolean local(double x, double y, double z, double targetX, double targetY, double targetZ, double width) {
        for (double v : new double[] {x,y,z,targetX,targetY,targetZ,width}) if (!Double.isFinite(v)) return false;
        if (width <= 0 || width >= 1 || Math.floor(x) != Math.floor(targetX) || Math.floor(z) != Math.floor(targetZ)) return false;
        if (Math.abs(targetX - (Math.floor(x)+.5)) > 1e-6 || Math.abs(targetZ - (Math.floor(z)+.5)) > 1e-6) return false;
        double drop = y-targetY, dx=x-targetX, dz=z-targetZ;
        return drop >= 0 && drop <= 1.25 && dx*dx + dz*dz <= .5 + 1e-9;
    }
}
