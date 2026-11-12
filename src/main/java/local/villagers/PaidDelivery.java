package local.villagers;

import java.util.UUID;
import local.villagers.economy.FoodDemand;
import local.villagers.economy.TradePolicy;
import local.villagers.learning.LearningMode;
import local.villagers.navigation.WorkJourney;
import local.villagers.navigation.WorkSearch;
import local.villagers.relations.FoodRequest;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

/** Ready owned goods wake a bounded offer to an actually heard requester, never an assumed sale. */
final class PaidDelivery {
    private record Offer(MarketGoods.Good good,Villager peer) {}
    private PaidDelivery() {}
    static void interrupt(Session s,long now) {
        if(s.paidJourney.active())s.paidJourney.interrupt(now);
        s.paidPeer=null;s.paidGood=null;s.paidSearch.clear();
    }
    /** Keep actual travel stable; completed/failed attempts alternate when both domains have ready goods. */
    static MarketGoods.Good choose(boolean food,boolean tool,MarketGoods.Good active,int cursor) {
        return choose(food,tool,false,active,cursor);
    }
    static MarketGoods.Good choose(boolean food,boolean tool,boolean wood,MarketGoods.Good active,int cursor) {
        if(active==MarketGoods.Good.FOOD && food || active==MarketGoods.Good.HOE && tool || active==MarketGoods.Good.WOOD && wood)return active;
        var available=new java.util.ArrayList<MarketGoods.Good>(3);
        if(food)available.add(MarketGoods.Good.FOOD);if(tool)available.add(MarketGoods.Good.HOE);if(wood)available.add(MarketGoods.Good.WOOD);
        return available.isEmpty()?null:available.get(Math.floorMod(cursor,available.size()));
    }
    private static Offer ready(ServerLevel level,Villager seller,Session s,MarketGoods.Good good,long now) {
        if(WorkLife.bound(s,good))return null;
        UUID id=switch(good) {
            case HOE->s.toolDemand.requester(now);
            case FOOD->s.foodDemand.intent(now)==FoodDemand.Intent.TRADE?s.foodDemand.requester(now):null;
            case WOOD->s.woodCustomer.peer(now);
        };
        if(id==null || MarketGoods.offer(seller.getInventory(),seller.getMainHandItem(),AdventureActions.canHarvest(seller),good)<0)return null;
        var peer=level.getEntity(id) instanceof Villager v?v:null;
        if(peer==null || peer==seller || !peer.isAlive() || !Villagers.isManaged(peer) || peer.isBaby() || peer.isSleeping() || peer.isTrading()
                || !FoodRequest.mayApproach(seller.distanceToSqr(peer),seller.hasLineOfSight(peer),VillageSurvival.withinHomeRange(level,s.mind,peer.position())))return null;
        return new Offer(good,peer);
    }
    static String seek(ServerLevel level,Villager seller,Session s,long now) {
        boolean owned=s.paidPeer!=null;
        if(Villagers.LEARNING_MODE==LearningMode.FROZEN || now<s.nextPaidAttempt || !seller.isAlive() || seller.isBaby()
                || seller.isSleeping() || seller.isTrading() || seller.isOnFire() || seller.isInWater() || seller.getAirSupply()<200
                || VillageSurvival.scheduledRest(level,seller) || !TradePolicy.sellerSafe(s.mind.foodLevel(),seller.getHealth(),seller.getMaxHealth(),s.mind.fatigue())) {
            if(owned)seller.getNavigation().stop();interrupt(s,now);return null;
        }
        var food=ready(level,seller,s,MarketGoods.Good.FOOD,now);var tool=ready(level,seller,s,MarketGoods.Good.HOE,now);
        var wood=ready(level,seller,s,MarketGoods.Good.WOOD,now);
        var good=choose(food!=null,tool!=null,wood!=null,s.paidGood,s.paidDeliveryCursor);
        var offer=good==MarketGoods.Good.FOOD?food:good==MarketGoods.Good.HOE?tool:wood;
        if(offer==null) {if(owned)seller.getNavigation().stop();interrupt(s,now);return null;}
        var peer=offer.peer();var id=peer.getUUID();String goal=id+":"+good;
        if(!id.equals(s.paidPeer) || good!=s.paidGood) {interrupt(s,now);s.paidPeer=id;s.paidGood=good;}
        boolean near=FoodRequest.inReach(seller.distanceToSqr(peer),Villagers.config().hearingRadius(),seller.hasLineOfSight(peer) && peer.hasLineOfSight(seller));
        var step=s.paidJourney.advance(goal,now,near,seller.getX(),seller.getY(),seller.getZ());
        if(step==WorkJourney.Step.WAIT)return null;
        if(step==WorkJourney.Step.ARRIVED) {
            seller.getNavigation().stop();s.nextPaidAttempt=now+TradePolicy.COOLDOWN;s.paidDeliveryCursor++;
            var buyer=Villagers.existingSession(id);
            boolean sold=buyer!=null && LocalTrade.exchange(level,peer,buyer,seller,good,now);
            if(sold) {
                if(good==MarketGoods.Good.FOOD)s.paidFoodDeliveries++;else if(good==MarketGoods.Good.HOE)s.paidToolDeliveries++;else s.paidWoodDeliveries++;
                buyer.nextTradeAttempt=now+TradePolicy.COOLDOWN;
                LocalTrade.interrupt(buyer,now);peer.getNavigation().stop();buyer.familyProvisionStarted=0;buyer.craftingProgress.reset();
            }
            interrupt(s,now);return sold?"delivered paid "+good.name().toLowerCase(java.util.Locale.ROOT):"local prepared-goods offer unavailable";
        }
        if(step==WorkJourney.Step.FAILED) {
            seller.getNavigation().stop();interrupt(s,now);s.nextPaidAttempt=now+WorkJourney.BACKOFF;s.paidDeliveryCursor++;return null;
        }
        s.action=null;s.before=null;s.succeeded=false;
        if(step==WorkJourney.Step.START) {seller.getNavigation().stop();s.nextPaidPath=now;}
        if(now>=s.nextPaidPath && WorkNavigation.admit(level,now,seller)) {
            s.nextPaidPath=now+40;
            var result=TradeApproach.move(level,seller,s,peer,s.paidSearch);
            if(result==WorkSearch.Step.EXHAUSTED) {
                seller.getNavigation().stop();interrupt(s,now);s.nextPaidAttempt=now+WorkJourney.BACKOFF;s.paidDeliveryCursor++;return null;
            }
            if(result==WorkSearch.Step.FOUND)s.paidJourney.navigating(goal,now,seller.getX(),seller.getY(),seller.getZ());
        }
        return "bringing prepared "+good.name().toLowerCase(java.util.Locale.ROOT)+" to a paying neighbour";
    }
}
