package local.villagers.navigation;

import java.util.Objects;

/** Progress is physical movement, not repeated successful path construction. */
public final class RouteProgress {
    private final long stallTicks;
    private String objective;
    private double x,y,z;
    private long lastMoved;
    public RouteProgress(long stallTicks) {
        if(stallTicks<1)throw new IllegalArgumentException("Invalid stall interval");
        this.stallTicks=stallTicks;
    }
    public boolean stalled(String goal,long tick,double px,double py,double pz) {
        Objects.requireNonNull(goal);
        if(!Double.isFinite(px)||!Double.isFinite(py)||!Double.isFinite(pz))throw new IllegalArgumentException("Invalid position");
        if(!goal.equals(objective)||tick<lastMoved) { objective=goal; x=px;y=py;z=pz;lastMoved=tick; return false; }
        double moved=(px-x)*(px-x)+(py-y)*(py-y)+(pz-z)*(pz-z);
        if(moved>=.25) {x=px;y=py;z=pz;lastMoved=tick;}
        return tick-lastMoved>=stallTicks;
    }
    public void reset(){objective=null;}
}
