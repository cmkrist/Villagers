package local.villagers;

import java.util.ArrayList;
import local.villagers.genetics.Trait;
import local.villagers.mind.Action;
import local.villagers.mind.Observation;
import local.villagers.navigation.WorkJourney;
import local.villagers.social.CompanionVisit;
import local.villagers.social.SocialBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

/** Ordinary friendship visits. No remote peer inventory, needs, or private memories enter target selection. */
final class SocialCompany {
    private SocialCompany() {}
    static boolean initiate(ServerLevel level, Villager v, Session s, Observation own, long now) {
        if (!available(v,s,level) || !CompanionVisit.wantsVisit(own, s.mind.expressedPersonality().sociability(), s.mind.has(Trait.GOSSIP), s.mind.has(Trait.LONER))) return false;
        return select(level, v, s, now);
    }
    private static boolean visible(ServerLevel level, Villager v, Session s, Villager other) {
        return other != v && other.isAlive() && other.level() == level && Villagers.isManaged(other)
                && !other.isSleeping() && !other.isTrading() && !other.isOnFire()
                && v.distanceToSqr(other) <= 256 && v.hasLineOfSight(other)
                && VillageSurvival.withinHomeRange(level, s.mind, other.position())
                && s.mind.opinionOf(other.getUUID()).regard() > -.3;
    }
    private static boolean select(ServerLevel level, Villager v, Session s, long now) {
        if (s.socialVisit.active()) return true;
        if (!s.socialVisit.maySelect(now)) return false;
        var candidates = new ArrayList<CompanionVisit.Candidate>();
        for (var other : level.getEntitiesOfClass(Villager.class, v.getBoundingBox().inflate(16),
                e -> e != v && e.isAlive() && Villagers.isManaged(e)).stream().limit(CompanionVisit.MAX_PEERS).toList()) {
            if (!visible(level,v,s,other)) continue;
            var opinion = s.mind.opinionOf(other.getUUID());
            candidates.add(new CompanionVisit.Candidate(other.getUUID(), opinion.regard(), Math.sqrt(v.distanceToSqr(other)),
                    opinion.witnessed() == 0 && opinion.hearsay() == 0));
            if (candidates.size() == CompanionVisit.MAX_PEERS) break;
        }
        return s.socialVisit.select(candidates, s.mind.expressedPersonality().curiosity(), now) != null;
    }
    private static boolean available(Villager v, Session s, ServerLevel level) {
        var own = new Observation(s.mind.hunger(),s.mind.fatigue(),s.mind.loneliness(),v.getHealth()/v.getMaxHealth(),
                s.action == Action.FLEE || v.isOnFire() || v.getLastHurtByMob() != null && v.tickCount-v.getLastHurtByMobTimestamp()<100,
                level.isDarkOutside(),false,false);
        return SocialBehavior.mayEngage(own,v.isTrading(),v.isSleeping()) && !s.selfFoodGoal
                && !s.foodDeliveryJourney.active() && !s.foodHelpJourney.active()
                && !s.companyJourney.active() && !s.farmJourney.active()
                && (s.action == null || s.action == Action.SOCIALIZE || s.action == Action.EXPLORE);
    }
    static void tick(ServerLevel level, Villager v, Session s, long now) {
        if (!available(v,s,level)) {
            if (s.socialVisit.active()) v.getNavigation().stop();
            s.socialVisit.interrupt(now);
            return;
        }
        if (!select(level,v,s,now)) return;
        var visit = s.socialVisit;
        var entity = level.getEntity(visit.target());
        if (!(entity instanceof Villager other) || !visible(level,v,s,other)) { visit.reject(now);v.getNavigation().stop();return; }
        var peer = Villagers.existingSession(other.getUUID());
        boolean near = v.distanceToSqr(other) < 9 && other.hasLineOfSight(v);
        boolean willing = peer != null && available(other,peer,level)
                && peer.mind.opinionOf(v.getUUID()).regard() > -.3 && peer.socialVisit.mayReceive(v.getUUID(),now);
        v.getLookControl().setLookAt(other,30,30);
        var step = visit.journey.advance(other.getUUID().toString(),now,near,v.getX(),v.getY(),v.getZ());
        if (near) {
            v.getNavigation().stop();
            if (!willing) { visit.reject(now);return; }
            if (visit.contact(now,true)) {
                s.mind.socialize();peer.mind.socialize();
                s.mind.remember("Visited " + peer.mind.name() + " for company");
                peer.mind.remember("Welcomed " + s.mind.name() + " for company");
                Speech.converse(level,v,s,other);
                visit.completed(other.getUUID(),now);peer.socialVisit.completed(v.getUUID(),now);
                s.succeeded=true;
                VillageFamilies.consider(level,v,s,other);
            }
            return;
        }
        visit.contact(now,false);
        if (step == WorkJourney.Step.FAILED) { visit.reject(now);v.getNavigation().stop();return; }
        if (step == WorkJourney.Step.WAIT || now < visit.nextPath || !WorkNavigation.admit(level,now,v)) return;
        visit.nextPath=now+40;
        var path=v.getNavigation().createPath(other.blockPosition(),1);
        if (!WorkNavigation.staysHome(level,v,s,path) || !v.getNavigation().moveTo(path,.5)) {
            visit.reject(now);v.getNavigation().stop();return;
        }
        visit.journey.navigating(other.getUUID().toString(),now,v.getX(),v.getY(),v.getZ());
    }
}
