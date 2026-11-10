package local.villagers;

import java.util.Comparator;
import local.villagers.genetics.Trait;
import local.villagers.learning.LearningMode;
import local.villagers.navigation.WorkJourney;
import local.villagers.relations.FoodRequest;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

/** Seek visible local company for an actual food request. Never inspect a candidate's private stock/needs. */
final class FoodHelp {
    private FoodHelp() {}
    static void interrupt(Session s,long now) {
        if (s.foodHelpPeer!=null || s.foodHelpJourney.active()) s.foodHelpJourney.interrupt(now);
        s.foodHelpPeer=null;
    }
    private static boolean eligible(ServerLevel level,Villager v,Session s,Villager other) {
        return other!=v && other.level()==level && other.isAlive() && Villagers.isManaged(other)
                &&(v.getHealth()>5||Math.abs(v.getY()-other.getY())<=1.25)
                && !other.isBaby() && !other.isSleeping() && !other.isTrading()
                && FoodRequest.mayApproach(v.distanceToSqr(other),v.hasLineOfSight(other),
                        VillageSurvival.withinHomeRange(level,s.mind,other.position()));
    }
    static String seek(ServerLevel level,Villager v,Session s,long now) {
        boolean critical=v.getHealth()<=5&&local.villagers.mind.MealPolicy.needsRecoveryFood(s.mind.foodLevel(),v.getHealth(),v.getMaxHealth());
        boolean care=local.villagers.mind.MealPolicy.needsRecoveryFood(s.mind.foodLevel(),v.getHealth(),v.getMaxHealth());
        boolean need=FoodRequest.dueForCare(now,s.nextFoodRequest,s.mind.foodLevel(),v.getHealth(),v.getMaxHealth(),s.mind.has(Trait.GLUTTON))
                && FoodInventory.anySlot(v.getInventory(),AdventureActions.canHarvest(v))<0;
        boolean available=Villagers.LEARNING_MODE!=LearningMode.FROZEN&&v.isAlive()&&!v.isSleeping()
            &&!v.isTrading()&&!v.isOnFire()&&!v.isInWater()&&v.getAirSupply()>=200
            &&!level.isDarkOutside()&&s.mind.fatigue()<.75;
        if((critical||care)&&available&&s.foodHelpPeer!=null&&now<s.nextFoodHelpScan&&now<s.nextFoodRequest
                &&level.getEntity(s.foodHelpPeer) instanceof Villager waiting&&eligible(level,v,s,waiting)){
            v.getNavigation().stop();return s.foodHelpStatus="waiting for a nearby food reply";
        }
        if (!available || !need) {
            if (s.foodHelpPeer!=null) v.getNavigation().stop();
            s.foodHelpStatus=need ? "body unavailable for food request" : "own need or request budget unmet";
            interrupt(s,now);return null;
        }
        var other=s.foodHelpPeer!=null && level.getEntity(s.foodHelpPeer) instanceof Villager peer ? peer : null;
        if (other==null || !eligible(level,v,s,other)) {
            if (s.foodHelpPeer!=null) { v.getNavigation().stop();interrupt(s,now); }
            if (now<s.nextFoodHelpScan) return null;
            s.nextFoodHelpScan=now+100;s.foodHelpScans++;
            var candidates=level.getEntitiesOfClass(Villager.class,v.getBoundingBox().inflate(FoodRequest.SEARCH_RADIUS),
                    peer -> peer!=v && peer.isAlive() && Villagers.isManaged(peer) && !peer.isBaby()
                            && !peer.isSleeping() && !peer.isTrading()).stream().limit(FoodRequest.MAX_PEERS)
                    .filter(peer -> eligible(level,v,s,peer))
                    .sorted(Comparator.comparingDouble(v::distanceToSqr)).toList();
            var selected=s.foodHelpPartners.next(candidates.stream().map(Villager::getUUID).toList());
            other=candidates.stream().filter(peer -> peer.getUUID().equals(selected)).findFirst().orElse(null);
            s.foodHelpPeer=other==null ? null : other.getUUID();
        }
        if (other==null) { s.foodHelpStatus="no visible neighbour in home range";return null; }
        boolean arrived=FoodRequest.audible(v.distanceToSqr(other),Villagers.config().hearingRadius(),
                v.hasLineOfSight(other) && other.hasLineOfSight(v));
        if(critical&&arrived){
            v.getNavigation().stop();v.getLookControl().setLookAt(other,30,30);
            if(now<s.nextFoodHelpPath)return s.foodHelpStatus="waiting for a neighbour to be ready";
            boolean asked=Speech.requestFood(level,v,s,other);
            if(asked){s.foodHelpRequests++;s.mind.remember("Asked visible neighbour "+other.getUUID()+" for food");}
            // A neighbour's asynchronous mind may not yet be loaded. An unsent
            // request must not consume the ordinary400tick journey backoff.
            s.nextFoodHelpPath=now+10;s.nextFoodHelpScan=now+(asked?80:10);
            return s.foodHelpStatus=asked?"asked neighbour for food":"food request awaiting neighbour readiness";
        }
        var step=s.foodHelpJourney.advance(other.getUUID().toString(),now,arrived,v.getX(),v.getY(),v.getZ());
        if (step==WorkJourney.Step.WAIT) return null;
        s.action=null;s.before=null;s.succeeded=false;
        if (step==WorkJourney.Step.ARRIVED) {
            v.getNavigation().stop();v.getLookControl().setLookAt(other,30,30);
            boolean asked=Speech.requestFood(level,v,s,other);
            if (asked) { s.foodHelpRequests++;s.mind.remember("Asked visible neighbour "+other.getUUID()+" for food"); }
            s.foodHelpStatus=asked ? "asked neighbour for food" : "food request unavailable";
            if(!(critical||care)||!asked)s.foodHelpPeer=null;s.nextFoodHelpScan=now+((critical||care)&&asked?80:100);return s.foodHelpStatus;
        }
        if (step==WorkJourney.Step.FAILED) {
            v.getNavigation().stop();s.foodHelpPeer=null;s.nextFoodHelpScan=now+WorkJourney.BACKOFF;
            return s.foodHelpStatus="food-help trip stalled or expired";
        }
        if (step==WorkJourney.Step.START) { v.getNavigation().stop();s.nextFoodHelpPath=now; }
        // One short perceived-neighbour query per second for a dying body;
        // ordinary work reservations must not consume its remaining meal time.
        if (now>=s.nextFoodHelpPath && (critical||WorkNavigation.admit(level,now,v))) {
            s.nextFoodHelpPath=now+(critical?20:40);
            var path=v.getNavigation().createPath(other.blockPosition(),1);
            if (!WorkNavigation.staysHome(level,v,s,path) || !v.getNavigation().moveTo(path,critical?VillageSurvival.HURRY:.5)) {
                s.foodHelpPathRejections++;s.foodHelpJourney.reject(now);s.foodHelpPeer=null;
                s.nextFoodHelpScan=now+WorkJourney.BACKOFF;v.getNavigation().stop();
                return s.foodHelpStatus="no permitted route to food help";
            }
            s.foodHelpJourney.navigating(other.getUUID().toString(),now,v.getX(),v.getY(),v.getZ());
        }
        return s.foodHelpStatus="approaching neighbour to ask for food";
    }
}
