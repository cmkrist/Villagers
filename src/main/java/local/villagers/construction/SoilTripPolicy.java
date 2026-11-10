package local.villagers.construction;

/** Only an active finite soil job admits its source corridor; ordinary home territory stays unchanged. */
public final class SoilTripPolicy {
    private SoilTripPolicy() {}
    public static boolean joins(WorkSites.Point home,WorkSites.Soil job) {
        return home!=null && job!=null && home.dimension().equals(job.source().dimension())
                && distance(home,job.source())<=64 && distance(home,job.target().above(1))<=28;
    }
    public static boolean contains(WorkSites.Point home,WorkSites.Soil job,double x,double y,double z) {
        if(!joins(home,job) || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))return false;
        double ax=home.x()+.5,ay=home.y()+.5,az=home.z()+.5;
        double px=x-ax,py=y-ay,pz=z-az;
        if(px*px+py*py+pz*pz<=28*28)return true;
        double dx=job.source().x()-home.x(),dy=job.source().y()-home.y(),dz=job.source().z()-home.z();
        double length=dx*dx+dy*dy+dz*dz;
        double t=length==0?0:Math.clamp((px*dx+py*dy+pz*dz)/length,0,1);
        double ex=px-t*dx,ey=py-t*dy,ez=pz-t*dz;
        return ex*ex+ey*ey+ez*ez<=8*8 && px*px+py*py+pz*pz<=72*72;
    }
    private static double distance(WorkSites.Point a,WorkSites.Point b) {
        return Math.sqrt(Math.pow(a.x()-b.x(),2)+Math.pow(a.y()-b.y(),2)+Math.pow(a.z()-b.z(),2));
    }
}
