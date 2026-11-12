package local.villagers.social;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import local.villagers.mind.Observation;
import local.villagers.navigation.WorkJourney;

/** One voluntary visit and a small recent-contact budget; opinions themselves remain in the saved mind. */
public final class CompanionVisit {
    public static final int MAX_PEERS = 16, CONTACT_TICKS = 40;
    public static final long PEER_COOLDOWN = 1200, VISIT_INTERVAL = 400;
    public record Candidate(UUID id, double regard, double distance, boolean stranger, boolean partner) {
        public Candidate(UUID id,double regard,double distance,boolean stranger){this(id,regard,distance,stranger,false);}
        public Candidate {
            if (id == null || !Double.isFinite(regard) || regard < -1 || regard > 1
                    || !Double.isFinite(distance) || distance < 0 || distance > 16)
                throw new IllegalArgumentException("Invalid visible companion");
        }
    }
    private final LinkedHashMap<UUID, Long> recent = new LinkedHashMap<>();
    public final WorkJourney journey = new WorkJourney();
    private UUID target;
    private long nextAllowed, contactSince = -1, lastContact = -1;
    public long nextPath;
    public boolean active() { return target != null; }
    public UUID target() { return target; }
    public boolean maySelect(long now) { return target == null && now >= nextAllowed; }

    public static boolean wantsVisit(Observation own, double sociability, boolean gossip, boolean loner) {
        return SocialBehavior.mayEngage(own, false, false)
                && own.loneliness() >= .55 - .2 * Math.clamp(sociability, 0, 1) - (gossip ? .1 : 0) + (loner ? .15 : 0);
    }

    /** A live target is stable. Selection uses only the visitor's own regard and actually visible candidates. */
    public UUID select(List<Candidate> visible, double curiosity, long now) {
        if (visible.size() > MAX_PEERS || !Double.isFinite(curiosity) || curiosity < 0 || curiosity > 1)
            throw new IllegalArgumentException("Unbounded companion selection");
        if (target != null || now < nextAllowed) return target;
        target = visible.stream().filter(c -> c.regard() > -.3 && now >= recent.getOrDefault(c.id(), 0L))
                .max(Comparator.comparingDouble((Candidate c) -> 2 * c.regard()
                        + (c.stranger() ? curiosity : 0) + (c.partner() ? .4 : 0) - c.distance() / 32)
                        .thenComparing(c -> c.id().toString())).map(Candidate::id).orElse(null);
        if (target == null) nextAllowed = now + 100;
        return target;
    }

    /** Both bodies must remain in willing contact; travel, interruption, or an unavailable peer earns no credit. */
    public boolean contact(long now, boolean willingAndNear) {
        if (!willingAndNear || lastContact < 0 || now - lastContact > 10 || now < lastContact) contactSince = -1;
        lastContact = now;
        if (!willingAndNear) return false;
        if (contactSince < 0) contactSince = now;
        return now - contactSince >= CONTACT_TICKS;
    }

    /** A successful visit also gives the receiver a break, preventing a crowd from repeatedly claiming its time. */
    public void completed(UUID peer, long now) {
        recent.remove(peer);
        if (recent.size() == MAX_PEERS) recent.remove(recent.keySet().iterator().next());
        recent.put(peer, now + PEER_COOLDOWN);
        interrupt(now);
        nextAllowed = now + VISIT_INTERVAL;
    }
    public boolean mayReceive(UUID peer, long now) {
        return now >= nextAllowed && now >= recent.getOrDefault(peer, 0L);
    }
    public void interrupt(long now) {
        if (target != null) { journey.interrupt(now); nextAllowed = Math.max(nextAllowed, now + 100); }
        target = null; contactSince = lastContact = -1;
    }
    public void reject(long now) {
        if (target != null) {
            UUID failed = target;
            completed(failed, now);
            journey.reject(now);
        }
    }
}
