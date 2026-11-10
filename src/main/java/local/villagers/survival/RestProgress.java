package local.villagers.survival;

/** A real consecutive quiet interval. Travel, interruption, unsafe footing and clock rollback earn no rest. */
public final class RestProgress {
    private String place;
    private long since, last;
    private double x, y, z;
    public void reset() { place = null; }
    public boolean quiet(String location, long now, boolean safe, double px, double py, double pz) {
        if (location == null || now < 0 || !Double.isFinite(px) || !Double.isFinite(py) || !Double.isFinite(pz))
            throw new IllegalArgumentException("Invalid rest observation");
        if (!safe) { reset(); return false; }
        double moved = (px-x)*(px-x) + (py-y)*(py-y) + (pz-z)*(pz-z);
        if (!location.equals(place) || now < last || now-last > 10 || moved >= .0625) {
            place = location; since = now; x = px; y = py; z = pz;
        }
        last = now;
        if (now-since < 80) return false;
        reset(); return true;
    }
}
