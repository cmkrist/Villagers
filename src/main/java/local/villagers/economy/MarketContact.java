package local.villagers.economy;

import java.util.UUID;

/** One actual understood market counterparty, not a paid order, reserved stock or remote availability claim. */
public final class MarketContact {
    private UUID peer;
    private long heard=-1,expires;
    public boolean hear(UUID participant,long now) {
        if(participant==null || now<0 || now>Long.MAX_VALUE-ToolDemand.LIFETIME)return false;
        peer(now);
        if(now<=heard || peer!=null && !peer.equals(participant))return false;
        peer=participant;heard=now;expires=now+ToolDemand.LIFETIME;return true;
    }
    public UUID peer(long now) {
        if(now<heard) {peer=null;heard=-1;}
        if(now>=expires)peer=null;
        return peer;
    }
    public void completed() {peer=null;}
}
