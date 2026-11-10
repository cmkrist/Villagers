package local.villagers;

import local.villagers.economy.FoodOffer;
import local.villagers.learning.LearningMode;
import local.villagers.navigation.WorkJourney;
import local.villagers.relations.FoodRequest;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

/** One bounded delivery to a known, actually visible requester. No donor/recipient stock search. */
final class FoodDelivery {
    private FoodDelivery() {}
    static void interrupt(Session s,long now) {
        if(s.foodDeliveryJourney.active())s.foodDeliveryJourney.interrupt(now);
        s.foodDeliveryPeer=null;s.foodDeliveryStarted=-1;
    }
    private static Villager peer(ServerLevel level,Villager actor,Session s,long now) {
        var id=s.foodDemand.giftRequester(now);
        if(id==null || now<s.nextFoodDeliveryAttempt || Villagers.LEARNING_MODE==LearningMode.FROZEN
                || !actor.isAlive() || actor.isBaby() || actor.isSleeping() || actor.isTrading() || actor.isOnFire()
                || actor.isInWater() || actor.getAirSupply()<200 || s.mind.fatigue()>=.75
                || !FoodOffer.mayDeferOwnMeal(s.mind.foodLevel(),actor.getHealth(),actor.getMaxHealth())
                || !Speech.willingToShare(s,id) || FoodInventory.sharingSlot(actor.getInventory(),AdventureActions.canHarvest(actor))<0)return null;
        if(!(level.getEntity(id) instanceof Villager peer) || peer==actor || !peer.isAlive() || !Villagers.isManaged(peer)
                || peer.isSleeping() || peer.isTrading() || !FoodRequest.mayApproach(actor.distanceToSqr(peer),actor.hasLineOfSight(peer),
                        VillageSurvival.withinHomeRange(level,s.mind,peer.position())))return null;
        if(!id.equals(s.foodDeliveryPeer)) {
            interrupt(s,now);s.foodDeliveryPeer=id;s.foodDeliveryStarted=now;
        }
        if(now-s.foodDeliveryStarted>=WorkJourney.TIMEOUT) {
            interrupt(s,now);s.nextFoodDeliveryAttempt=now+WorkJourney.BACKOFF;return null;
        }
        return peer;
    }
    static boolean handOffNearby(ServerLevel level,Villager actor,Session s,long now) {
        var peer=peer(level,actor,s,now);
        if(peer==null)return false;
        boolean given=Speech.deliverPendingFood(level,actor,s,peer,now);
        if(given)s.before=null;
        if(given || s.foodDemand.giftRequester(now)==null)interrupt(s,now);
        return given;
    }
    /** Hold at most one transferable item, releasing it for urgent hunger, injury, expiry or a failed trip. */
    static boolean reserveMeal(ServerLevel level,Villager actor,Session s,long now,int slot) {
        return slot>=0 && slot==FoodInventory.sharingSlot(actor.getInventory(),AdventureActions.canHarvest(actor))
                && !VillageSurvival.scheduledRest(level,actor)
                && FoodInventory.availableCount(actor.getInventory().getItem(slot),AdventureActions.canHarvest(actor))==1
                && peer(level,actor,s,now)!=null;
    }
    static String seek(ServerLevel level,Villager actor,Session s,long now) {
        if(VillageSurvival.scheduledRest(level,actor)) {interrupt(s,now);return null;}
        boolean owned=s.foodDeliveryPeer!=null;var peer=peer(level,actor,s,now);
        if(peer==null) {if(owned)actor.getNavigation().stop();interrupt(s,now);return null;}
        boolean near=FoodRequest.inReach(actor.distanceToSqr(peer),Villagers.config().hearingRadius(),
                actor.hasLineOfSight(peer) && peer.hasLineOfSight(actor));
        if(near) {
            actor.getNavigation().stop();boolean given=Speech.deliverPendingFood(level,actor,s,peer,now);
            interrupt(s,now);
            if(!given)s.nextFoodDeliveryAttempt=now+40;
            return given?"delivered requested food":null;
        }
        var step=s.foodDeliveryJourney.advance(peer.getUUID().toString(),now,false,actor.getX(),actor.getY(),actor.getZ());
        if(step==WorkJourney.Step.WAIT)return null;
        s.action=null;s.before=null;s.succeeded=false;
        if(step==WorkJourney.Step.FAILED) {
            actor.getNavigation().stop();interrupt(s,now);s.nextFoodDeliveryAttempt=now+WorkJourney.BACKOFF;return null;
        }
        if(step==WorkJourney.Step.START) {actor.getNavigation().stop();s.nextFoodDeliveryPath=now;}
        // A heard request and visibly injured recipient justify a short urgent
        // route; this observes no private recipient stock or hunger state.
        boolean urgent=peer.getHealth()<=5;
        if(now>=s.nextFoodDeliveryPath && (urgent||WorkNavigation.admit(level,now,actor))) {
            s.nextFoodDeliveryPath=now+(urgent?20:40);var path=actor.getNavigation().createPath(peer.blockPosition(),1);
            if(!WorkNavigation.staysHome(level,actor,s,path) || !actor.getNavigation().moveTo(path,urgent?VillageSurvival.HURRY:.5)) {
                s.foodDeliveryPathRejections++;actor.getNavigation().stop();interrupt(s,now);
                s.nextFoodDeliveryAttempt=now+WorkJourney.BACKOFF;return null;
            }
            s.foodDeliveryJourney.navigating(peer.getUUID().toString(),now,actor.getX(),actor.getY(),actor.getZ());
        }
        return "bringing requested food";
    }
}
