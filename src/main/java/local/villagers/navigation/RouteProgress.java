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
    private String pathObjective;
    private int furthestNode;
    private long lastAdvance;
    /** Jumping at one obstructed stair is movement but does not advance a route. */
    public boolean stalledAdvance(String goal,long tick,int nextNode) {
        Objects.requireNonNull(goal);
        if(!goal.equals(pathObjective)||tick<lastAdvance) {
            pathObjective=goal;furthestNode=nextNode;lastAdvance=tick;return false;
        }
        if(nextNode>furthestNode){furthestNode=nextNode;lastAdvance=tick;}
        return tick-lastAdvance>=stallTicks;
    }
    private String nearObjective;
    private double closest;
    private long lastCloser;
    /** A short repeated approach must get closer, rather than oscillate. */
    public boolean stalledCloser(String goal,long tick,double distance){
        Objects.requireNonNull(goal);
        if(!Double.isFinite(distance)||distance<0)throw new IllegalArgumentException("Invalid distance");
        if(!goal.equals(nearObjective)||tick<lastCloser){nearObjective=goal;closest=distance;lastCloser=tick;return false;}
        if(distance<=closest-.5){closest=distance;lastCloser=tick;}
        return tick-lastCloser>=stallTicks;
    }
    public void reset(){objective=null;pathObjective=null;nearObjective=null;}
}
