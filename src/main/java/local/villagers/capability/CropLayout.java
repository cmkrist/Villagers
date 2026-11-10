package local.villagers.capability;

import java.util.List;
import local.villagers.capability.CropPolicy.CropKind;

/** Minecraft's same-crop crowding preference, used only to select a possessed seed for a new plot. */
public final class CropLayout {
    public record Neighbour(int x,int z,CropKind crop) {
        public Neighbour {
            if(x<-1 || x>1 || z<-1 || z>1 || x==0 && z==0 || crop==null)
                throw new IllegalArgumentException("Expected one local crop neighbour");
        }
    }
    private CropLayout() {}
    public static int preference(CropKind candidate,List<Neighbour> seen) {
        if(candidate==null || candidate==CropKind.OTHER || seen.size()>8)
            throw new IllegalArgumentException("Invalid bounded crop layout");
        boolean alongX=false,alongZ=false;
        for(var neighbour:seen)if(neighbour.crop()==candidate) {
            if(neighbour.x()!=0 && neighbour.z()!=0)return -1;
            alongX |= neighbour.x()!=0;
            alongZ |= neighbour.z()!=0;
        }
        return alongX && alongZ ? -1 : 0;
    }
}
