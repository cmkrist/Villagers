package local.villagers.construction;

/** Only an active separately approved construction job may admit these bounded work corridors. */
public final class StepTripPolicy {
    private StepTripPolicy() {}
    public static boolean joins(WorkSites.Point home,StepJob job) {
        if(home==null || job==null || !home.dimension().equals(job.blocks().getFirst().dimension()))return false;
        for(var s:job.sources())if(distance(home,s.point())>68)return false;
        for(var p:job.blocks())if(distance(home,p)>40)return false;
        return true;
    }
    private static double distance(WorkSites.Point a,WorkSites.Point b) {
        return Math.sqrt(Math.pow((double)a.x()-b.x(),2)+Math.pow((double)a.y()-b.y(),2)+Math.pow((double)a.z()-b.z(),2));
    }
    public static boolean contains(WorkSites.Point home,StepJob job,double x,double y,double z) {
        if(!joins(home,job) || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))return false;
        double px=x-home.x()-.5,py=y-home.y()-.5,pz=z-home.z()-.5;
        double radius=px*px+py*py+pz*pz;
        if(radius<=28*28)return true;if(radius>76*76)return false;
        for(var s:job.sources())if(corridor(home,s.point(),px,py,pz))return true;
        for(var p:job.blocks())if(corridor(home,p,px,py,pz))return true;
        return false;
    }
    private static boolean corridor(WorkSites.Point h,WorkSites.Point p,double x,double y,double z) {
        double dx=p.x()-h.x(),dy=p.y()-h.y(),dz=p.z()-h.z(),length=dx*dx+dy*dy+dz*dz;
        double t=length==0?0:Math.clamp((x*dx+y*dy+z*dz)/length,0,1);
        return Math.pow(x-t*dx,2)+Math.pow(y-t*dy,2)+Math.pow(z-t*dz,2)<=8*8;
    }
}
