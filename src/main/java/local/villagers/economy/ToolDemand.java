package local.villagers.economy;

import java.util.UUID;

/** One understood local customer, not an order, debt, material grant or permission to inspect remote stock. */
public final class ToolDemand {
    public static final long LIFETIME=12000;
    private UUID requester;
    private long heard=-1,expires;
    public boolean hear(UUID peer,long now) {
        if(peer==null || now<0 || now>Long.MAX_VALUE-LIFETIME)return false;
        requester(now);
        if(now<=heard || requester!=null && !requester.equals(peer))return false;
        requester=peer;heard=now;expires=now+LIFETIME;return true;
    }
    public UUID requester(long now) {
        if(now<heard) {requester=null;heard=-1;}
        if(now>=expires)requester=null;
        return requester;
    }
    public void fulfilled(UUID peer) {if(peer!=null && peer.equals(requester))requester=null;}
    /** Keep at least the former13body+15stored minimum, without forgetting nutrition actually eaten into the body. */
    public static boolean mayProduce(boolean heard,boolean ownWorkingHoe,boolean hasSpare,int food,int reserveDeficit) {
        return heard && ownWorkingHoe && !hasSpare && food>=13 && food<=20
                && reserveDeficit>=0 && reserveDeficit<=15 && food+15-reserveDeficit>=28;
    }
}
