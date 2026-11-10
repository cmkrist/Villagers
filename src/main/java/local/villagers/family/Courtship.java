package local.villagers.family;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Local, transient consent. An offer describes public readiness, never private inventory contents. */
public final class Courtship {
    public static final long LIFETIME = 1200;
    public static final int MAX_HEARERS = 8;
    public static final int COMPANY_RADIUS = 24;
    public record Offer(UUID sender, long sentAt, long expiresAt, Set<UUID> announcedParents) {
        public Offer {
            Objects.requireNonNull(sender);
            announcedParents = Set.copyOf(announcedParents);
            if (announcedParents.size() > 2 || announcedParents.contains(sender)) throw new IllegalArgumentException("Invalid declared parentage");
            if (sentAt < 0 || expiresAt <= sentAt || expiresAt - sentAt > LIFETIME)
                throw new IllegalArgumentException("Invalid courtship lifetime");
        }
        public static Offer at(UUID sender, long now) {
            if (now > Long.MAX_VALUE - LIFETIME) throw new IllegalArgumentException("Clock exhausted");
            return at(sender, now, Set.of());
        }
        public static Offer at(UUID sender, long now, Set<UUID> parents) {
            if (now < 0 || now > Long.MAX_VALUE - LIFETIME) throw new IllegalArgumentException("Invalid clock");
            return new Offer(sender, now, now + LIFETIME, parents);
        }
    }
    public record Agreement(UUID partner, long agreedAt, long expiresAt) {
        public Agreement { Objects.requireNonNull(partner); if (agreedAt < 0 || expiresAt <= agreedAt || expiresAt - agreedAt > LIFETIME) throw new IllegalArgumentException("Invalid agreement"); }
        public boolean active(long now) { return now >= agreedAt && now < expiresAt; }
    }
    private Courtship() {}

    public static boolean mayApproach(double distanceSquared, boolean visible, boolean adult,
                                      boolean withinHome, double ownRegard) {
        return Double.isFinite(distanceSquared) && distanceSquared >= 0 && distanceSquared <= COMPANY_RADIUS * COMPANY_RADIUS
                && visible && adult && withinHome && Double.isFinite(ownRegard) && ownRegard >= 0;
    }

    /** Both actors must retain the same live spoken agreement; stock or one-sided intent is insufficient. */
    public static boolean mutual(UUID first, Agreement a, UUID second, Agreement b, long now) {
        return !first.equals(second) && a != null && b != null && a.active(now) && b.active(now)
                && a.partner().equals(second) && b.partner().equals(first)
                && a.agreedAt() == b.agreedAt() && a.expiresAt() == b.expiresAt();
    }

    public static boolean mayAccept(Offer offer, UUID listener, Agreement current, long now, boolean inRange,
                                    boolean visible, boolean ready, double regard, boolean closeKin, boolean understood) {
        return !offer.sender().equals(listener) && now >= offer.sentAt() && now < offer.expiresAt()
                && inRange && visible && ready && Double.isFinite(regard) && regard >= 0 && !closeKin && understood
                && (current == null || !current.active(now) || current.partner().equals(offer.sender()));
    }
    /** Compare only the listener's own ancestry with the sender's explicit public declaration. */
    public static boolean related(Offer offer, UUID listener, Set<UUID> ownParents) {
        return ownParents.contains(offer.sender()) || offer.announcedParents().contains(listener)
                || ownParents.stream().anyMatch(offer.announcedParents()::contains);
    }
    public static UUID preferred(Agreement agreement, Set<UUID> visibleNearby, long now) {
        return agreement != null && agreement.active(now) && visibleNearby.contains(agreement.partner()) ? agreement.partner() : null;
    }
}
