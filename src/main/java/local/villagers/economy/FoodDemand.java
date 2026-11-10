package local.villagers.economy;

import java.util.UUID;

/** One short-lived, understood local request. No remote stock query, debt or transferable authority. */
public final class FoodDemand {
    public enum Intent { GIFT, TRADE }
    public static final int EXTRA_NUTRITION = 5;
    public static final long LIFETIME = 12000;
    private UUID requester;
    private Intent intent;
    private long observed = -1, expires;

    /** Called only after actual hearing/understanding, willingness and an unsuccessful real handoff. */
    public boolean hear(UUID peer, long now) {
        return hear(peer, now, Intent.GIFT);
    }

    /** An understood paid proposal authorizes preparation, never a charitable delivery. */
    public boolean hearTrade(UUID peer, long now) {
        return hear(peer, now, Intent.TRADE);
    }

    private boolean hear(UUID peer, long now, Intent purpose) {
        if (peer == null || now < 0 || now > Long.MAX_VALUE - LIFETIME) return false;
        extraNutrition(now);
        if (now <= observed || requester != null && !requester.equals(peer)) return false;
        requester = peer; intent = purpose; observed = now; expires = now + LIFETIME;
        return true;
    }

    public int extraNutrition(long now) {
        if (now < observed) { clear(); observed = -1; }
        if (now >= expires) clear();
        return requester == null ? 0 : EXTRA_NUTRITION;
    }

    public UUID requester(long now) { extraNutrition(now); return requester; }
    public Intent intent(long now) { extraNutrition(now); return intent; }
    public UUID giftRequester(long now) { return intent(now) == Intent.GIFT ? requester : null; }

    private void clear() { requester = null; intent = null; }

    /** Only a completed real transfer to the requester fulfills this signal. */
    public void fulfilled(UUID peer) {
        if (intent == Intent.GIFT && peer != null && peer.equals(requester)) clear();
    }
    public void tradeFulfilled(UUID peer) {
        if (intent == Intent.TRADE && peer != null && peer.equals(requester)) clear();
    }
    /** Recipient has explicitly declined an offer; this is not a transfer or social reward. */
    public void withdrawn(UUID peer) { fulfilled(peer); }
}
