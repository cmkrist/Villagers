package local.villagers;

import java.util.Comparator;
import local.villagers.family.Courtship;
import local.villagers.navigation.WorkJourney;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

/** Seek visible adult company from one's own readiness. An approach neither reads peer needs nor grants consent. */
final class FamilyCompany {
    private FamilyCompany() {}

    static void interrupt(Session s, long now) {
        if (s.familyCompany != null || s.companyJourney.active()) s.companyJourney.interrupt(now);
        s.familyCompany = null;
    }

    static boolean seek(ServerLevel level, Villager v, Session s, long now) {
        if (!VillageFamilies.readyForCompany(level, v, s, now)) {
            s.companyStatus = s.courtship != null && s.courtship.active(now) ? "agreement already active" : "own readiness unmet";
            interrupt(s, now); return false;
        }
        boolean currentVisible = s.familyCompany != null && level.getEntity(s.familyCompany) instanceof Villager current
                && eligible(level, v, s, current);
        if (!currentVisible && now >= s.nextCompanyScan) {
            s.companyScans++;
            s.nextCompanyScan = now + 100;
            var candidate = level.getEntitiesOfClass(Villager.class, v.getBoundingBox().inflate(Courtship.COMPANY_RADIUS),
                    other -> eligible(level, v, s, other)).stream().limit(Courtship.MAX_HEARERS)
                    .min(Comparator.comparingDouble(v::distanceToSqr)).orElse(null);
            s.familyCompany = candidate == null ? null : candidate.getUUID();
        }
        if (s.familyCompany == null || !(level.getEntity(s.familyCompany) instanceof Villager other)
                || !eligible(level, v, s, other)) {
            s.companyStatus = "no visible adult in permitted range";
            interrupt(s, now); return false;
        }
        var step = s.companyJourney.advance(other.getUUID().toString(), now, v.distanceToSqr(other) < 9,
                v.getX(), v.getY(), v.getZ());
        if (step == WorkJourney.Step.FAILED) {
            s.companyStatus = "trip stalled or expired";
            s.mind.remember("Stopped seeking company without reaching " + other.getUUID());
            s.familyCompany = null; s.nextCompanyScan = now + WorkJourney.BACKOFF; return false;
        }
        s.companyStatus = step.name().toLowerCase(java.util.Locale.ROOT);
        return step != WorkJourney.Step.WAIT;
    }

    private static boolean eligible(ServerLevel level, Villager v, Session s, Villager other) {
        return other != v && other.level() == level && other.isAlive() && Villagers.isManaged(other)
                && !other.isSleeping() && Courtship.mayApproach(v.distanceToSqr(other), v.hasLineOfSight(other),
                !other.isBaby(), VillageSurvival.withinHomeRange(level, s.mind, other.position()),
                s.mind.opinionOf(other.getUUID()).regard());
    }

    static void reject(Session s, long now) {
        s.companyPathRejections++; s.companyStatus = "no permitted home route";
        s.companyJourney.reject(now); s.familyCompany = null;
        s.action = null; s.before = null; s.succeeded = false;
    }
}
