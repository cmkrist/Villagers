package local.villagers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import local.villagers.economy.FoodOffer;
import local.villagers.economy.TradePolicy;
import local.villagers.genetics.Trait;
import local.villagers.language.Concept;
import local.villagers.learning.LearningMode;
import local.villagers.navigation.WorkJourney;
import local.villagers.relations.FoodRequest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;

/** Real local quotes at contact. Partner discovery never inspects inventories or private needs. */
final class LocalTrade {
    private LocalTrade() {}
    static void interrupt(Session s,long now) {
        if(s.tradeJourney.active())s.tradeJourney.interrupt(now);
        s.tradePeer=null;s.tradeGood=null;s.tradeSearch.clear();
    }
    private static MarketGoods.Good need(Villager v,Session s) {
        if(v.getInventory().countItem(Items.EMERALD)<1)return null;
        return desired(v,s);
    }
    static boolean usefulMoney(Villager v,Session s) {
        return v.getInventory().countItem(Items.EMERALD)<2 && desired(v,s)!=null;
    }
    private static MarketGoods.Good desired(Villager v,Session s) {
        if(AdventureActions.hasHoe(v))s.toolSupplier.completed();
        if(FoodOffer.wantsFood(s.mind.hunger(),s.mind.has(Trait.GLUTTON),
                FoodInventory.anySlot(v.getInventory(),AdventureActions.canHarvest(v))>=0))return MarketGoods.Good.FOOD;
        if(ToolProduction.needsWood(v,s,v.level().getGameTime()))return MarketGoods.Good.WOOD;
        if(!AdventureActions.hasHoe(v) && v.getInventory().hasAnyMatching(PlantingMaterials::supported)
                && WoodCrafting.next(v.getInventory())==null)return MarketGoods.Good.HOE;
        return null;
    }
    private static boolean body(ServerLevel level,Villager v,Session s) {
        return v.isAlive() && Villagers.isManaged(v) && !v.isBaby() && v.level()==level && !v.isTrading()
                && s.action!=local.villagers.mind.Action.FLEE
                && !v.isSleeping() && !v.isOnFire() && !v.isInWater() && v.getAirSupply()>=200
                && !VillageSurvival.scheduledRest(level,v) && s.mind.fatigue()<.75;
    }
    private static boolean visible(ServerLevel level,Villager buyer,Session s,Villager other) {
        return other!=buyer && other.level()==level && other.isAlive() && Villagers.isManaged(other)
                && !other.isBaby() && !other.isSleeping() && !other.isTrading()
                && FoodRequest.mayApproach(buyer.distanceToSqr(other),buyer.hasLineOfSight(other),
                        VillageSurvival.withinHomeRange(level,s.mind,other.position()));
    }
    static String seekFood(ServerLevel level,Villager buyer,Session s,long now) {
        return need(buyer,s)==MarketGoods.Good.FOOD?seek(level,buyer,s,now):null;
    }
    /** Essential inputs for an admitted heard order precede optional pantry replenishment. */
    static String seekProductionInput(ServerLevel level,Villager buyer,Session s,long now) {
        return need(buyer,s)==MarketGoods.Good.WOOD?seek(level,buyer,s,now):null;
    }
    static String seekKnownSupplier(ServerLevel level,Villager buyer,Session s,long now) {
        var good=need(buyer,s);
        return good==MarketGoods.Good.HOE && s.toolSupplier.peer(now)!=null
                || good==MarketGoods.Good.WOOD && s.woodSupplier.peer(now)!=null?seek(level,buyer,s,now):null;
    }
    static String seek(ServerLevel level,Villager buyer,Session s,long now) {
        var good=need(buyer,s);
        if(WorkLife.bound(s,good))return null;
        if(Villagers.LEARNING_MODE==LearningMode.FROZEN || !body(level,buyer,s) || good==null || now<s.nextTradeAttempt) {
            if(s.tradePeer!=null)buyer.getNavigation().stop();interrupt(s,now);return null;
        }
        if(s.tradeGood!=good) {if(s.tradePeer!=null)buyer.getNavigation().stop();interrupt(s,now);}
        var peer=s.tradePeer!=null && level.getEntity(s.tradePeer) instanceof Villager v?v:null;
        var supplier=good==MarketGoods.Good.FOOD?s.foodSupplier.peer(now):good==MarketGoods.Good.HOE?s.toolSupplier.peer(now):s.woodSupplier.peer(now);
        if(supplier!=null && level.getEntity(supplier) instanceof Villager known && visible(level,buyer,s,known)) {
            if(!supplier.equals(s.tradePeer)) {buyer.getNavigation().stop();interrupt(s,now);}
            peer=known;s.tradePeer=supplier;s.tradeGood=good;
        }
        if(peer==null || !visible(level,buyer,s,peer)) {
            if(s.tradePeer!=null) {buyer.getNavigation().stop();interrupt(s,now);}
            if(now<s.nextTradeScan)return null;
            s.nextTradeScan=now+100;
            var peers=level.getEntitiesOfClass(Villager.class,buyer.getBoundingBox().inflate(FoodRequest.SEARCH_RADIUS),
                    v -> v!=buyer && v.isAlive() && Villagers.isManaged(v) && !v.isBaby() && !v.isSleeping() && !v.isTrading())
                    .stream().limit(FoodRequest.MAX_PEERS).filter(v->visible(level,buyer,s,v))
                    .sorted(Comparator.comparingDouble(buyer::distanceToSqr)).toList();
            if(peers.isEmpty())return null;
            var selected=s.tradePartners.next(peers.stream().map(Villager::getUUID).toList());
            peer=peers.stream().filter(v->v.getUUID().equals(selected)).findFirst().orElseThrow();s.tradePeer=selected;s.tradeGood=good;
        }
        boolean near=FoodRequest.inReach(buyer.distanceToSqr(peer),Villagers.config().hearingRadius(),
                buyer.hasLineOfSight(peer) && peer.hasLineOfSight(buyer));
        if(!near && now>=s.nextTradeInquiry
                && TradePolicy.canHearProposal(buyer.distanceToSqr(peer),Villagers.config().hearingRadius(),buyer.hasLineOfSight(peer) && peer.hasLineOfSight(buyer))) {
            s.nextTradeInquiry=now+TradePolicy.COOLDOWN;
            inquire(level,buyer,s,peer,good,now);
        }
        var step=s.tradeJourney.advance(peer.getUUID()+":"+good,now,near,buyer.getX(),buyer.getY(),buyer.getZ());
        if(step==WorkJourney.Step.WAIT)return null;
        s.action=null;s.before=null;s.succeeded=false;
        if(step==WorkJourney.Step.ARRIVED) {
            buyer.getNavigation().stop();s.nextTradeAttempt=now+TradePolicy.COOLDOWN;
            s.tradeStatus="local trade unavailable";
            boolean traded=exchange(level,buyer,s,peer,good,now);
            if(traded && good==MarketGoods.Good.FOOD)s.foodContactPurchases++;
            if(!traded && good==MarketGoods.Good.FOOD)s.foodSupplier.completed();
            if(!traded && good==MarketGoods.Good.WOOD)s.woodSupplier.completed();
            interrupt(s,now);
            return traded?"paid for local "+good.name().toLowerCase(java.util.Locale.ROOT):s.tradeStatus;
        }
        if(step==WorkJourney.Step.FAILED) {
            if(good==MarketGoods.Good.FOOD)s.foodSupplier.completed();
            buyer.getNavigation().stop();interrupt(s,now);s.nextTradeAttempt=now+WorkJourney.BACKOFF;return null;
        }
        if(step==WorkJourney.Step.START) {buyer.getNavigation().stop();s.nextTradePath=now;}
        if(now>=s.nextTradePath && WorkNavigation.admit(level,now,buyer)) {
            s.nextTradePath=now+40;
            var result=TradeApproach.move(level,buyer,s,peer);
            if(result==local.villagers.navigation.WorkSearch.Step.EXHAUSTED) {
                if(good==MarketGoods.Good.FOOD)s.foodSupplier.completed();
                buyer.getNavigation().stop();interrupt(s,now);s.nextTradeAttempt=now+WorkJourney.BACKOFF;return null;
            }
            if(result==local.villagers.navigation.WorkSearch.Step.FOUND)
                s.tradeJourney.navigating(peer.getUUID()+":"+good,now,buyer.getX(),buyer.getY(),buyer.getZ());
        }
        return "approaching neighbour for local trade";
    }
    /** Each participant evaluates only its own stock/need, after an actual locally understood proposal. */
    static boolean exchange(ServerLevel level,Villager buyer,Session from,Villager seller,MarketGoods.Good good,long now) {
        var to=Villagers.existingSession(seller.getUUID());
        if(Villagers.LEARNING_MODE==LearningMode.FROZEN || buyer==seller || to==null || !body(level,buyer,from) || !body(level,seller,to)
                || need(buyer,from)!=good || !VillageSurvival.withinHomeRange(level,to.mind,buyer.position())
                || !FoodRequest.inReach(buyer.distanceToSqr(seller),Villagers.config().hearingRadius(),buyer.hasLineOfSight(seller) && seller.hasLineOfSight(buyer)))return false;
        if(WorkLife.bound(from,good)||WorkLife.bound(to,good))return false;
        var concept=MarketGoods.concept(good);
        var request=List.of(Concept.WANT,Concept.TRADE,concept,Concept.EMERALD);
        if(!understands(level,buyer,from,seller,to,request))return declined(from,good,"proposal not understood");
        var opinion=to.mind.opinionOf(buyer.getUUID());
        int price=TradePolicy.ask(to.mind.has(Trait.GREEDY),to.mind.mood().low(),opinion.regard(),opinion.fear());
        if(price==0 || !TradePolicy.sellerSafe(to.mind.foodLevel(),seller.getHealth(),seller.getMaxHealth(),to.mind.fatigue()))return declined(from,good,"seller unavailable");
        int slot=MarketGoods.offer(seller.getInventory(),seller.getMainHandItem(),AdventureActions.canHarvest(seller),good);
        if(slot<0) {hearUnfilled(level,buyer,from,seller,to,good,price,now);return declined(from,good,"no spare stock");}
        var plan=MarketGoods.quote(buyer.getInventory(),AdventureActions.canHarvest(buyer),seller.getInventory(),seller.getMainHandItem(),AdventureActions.canHarvest(seller),good,price);
        if(plan==null)return declined(from,good,"payment or receipt does not fit");
        var item=Concept.forItem(BuiltInRegistries.ITEM.getKey(plan.goods.getItem()).toString());
        if(item==null)return declined(from,good,"item has no spoken concept");
        var quote=new ArrayList<Concept>();quote.add(Concept.TRADE);
        for(int i=0;i<plan.goods.getCount();i++)quote.add(item);
        for(int i=0;i<price;i++)quote.add(Concept.EMERALD); // Each actual coin in this bounded quote is spoken.
        if(!understands(level,seller,to,buyer,from,quote) || price>TradePolicy.bid(good!=MarketGoods.Good.FOOD,from.mind.foodLevel())
                || from.mind.opinionOf(seller.getUUID()).fear()>=.6)return declined(from,good,"quote not accepted");
        if(!understands(level,buyer,from,seller,to,List.of(Concept.YES,Concept.TRADE)))return declined(from,good,"acceptance not understood");
        int money=buyer.getInventory().countItem(Items.EMERALD),sellerMoney=seller.getInventory().countItem(Items.EMERALD);
        int buyerGoods=buyer.getInventory().countItem(plan.goods.getItem()),sellerGoods=seller.getInventory().countItem(plan.goods.getItem());
        if(!TradeMaterials.commit(buyer.getInventory(),seller.getInventory(),plan))return false;
        from.before=null;to.before=null;from.localPurchases++;to.localSales++;
        var receipt=new LinkedHashMap<String,Object>();receipt.put("gameTime",now);receipt.put("buyer",buyer.getUUID().toString());receipt.put("seller",seller.getUUID().toString());
        receipt.put("item",BuiltInRegistries.ITEM.getKey(plan.goods.getItem()).toString());receipt.put("quantity",plan.goods.getCount());receipt.put("damage",plan.goods.getDamageValue());receipt.put("price",price);
        receipt.put("buyerMoneyBefore",money);receipt.put("buyerMoneyAfter",buyer.getInventory().countItem(Items.EMERALD));
        receipt.put("sellerMoneyBefore",sellerMoney);receipt.put("sellerMoneyAfter",seller.getInventory().countItem(Items.EMERALD));
        receipt.put("buyerGoodsBefore",buyerGoods);receipt.put("buyerGoodsAfter",buyer.getInventory().countItem(plan.goods.getItem()));
        receipt.put("sellerGoodsBefore",sellerGoods);receipt.put("sellerGoodsAfter",seller.getInventory().countItem(plan.goods.getItem()));
        var immutable=Map.<String,Object>copyOf(receipt);from.lastTrade=immutable;to.lastTrade=immutable;
        from.mind.remember("Bought "+plan.goods.getCount()+" actual "+receipt.get("item")+" from "+seller.getUUID()+" for "+price+" held emeralds");
        to.mind.remember("Sold "+plan.goods.getCount()+" actual "+receipt.get("item")+" to "+buyer.getUUID()+" for "+price+" actual emeralds");
        if(good==MarketGoods.Good.FOOD)to.foodDemand.tradeFulfilled(buyer.getUUID());
        else if(good==MarketGoods.Good.HOE)to.toolDemand.fulfilled(buyer.getUUID());
        if(good==MarketGoods.Good.FOOD) {from.foodSupplier.completed();from.foodSupplier.hear(seller.getUUID(),now);}
        if(good==MarketGoods.Good.HOE)from.toolSupplier.completed();
        if(good==MarketGoods.Good.WOOD) {from.nextToolWork=now;from.woodSupplier.completed();from.woodSupplier.hear(seller.getUUID(),now);to.woodCustomer.completed();}
        Villagers.persist(from);Villagers.persist(to);return true;
    }
    private static boolean declined(Session s,MarketGoods.Good good,String reason) {
        s.tradeStatus="local "+good.name().toLowerCase(java.util.Locale.ROOT)+": "+reason;return false;
    }
    /** An affordable, understood request may prepare stock; no payment or persistent order is created. */
    private static void hearUnfilled(ServerLevel level,Villager buyer,Session from,Villager seller,Session to,
                                     MarketGoods.Good good,int price,long now) {
        rememberDemand(level,buyer,from,seller,to,good,price,now,false);
    }
    /** Receiver handles its own stock only after real words reach it. Nothing transfers at hearing range. */
    private static void inquire(ServerLevel level,Villager buyer,Session from,Villager seller,MarketGoods.Good good,long now) {
        var to=Villagers.existingSession(seller.getUUID());
        if(to==null || !body(level,buyer,from) || !body(level,seller,to)
                || !VillageSurvival.withinHomeRange(level,to.mind,buyer.position()))return;
        if(!understands(level,buyer,from,seller,to,List.of(Concept.WANT,Concept.TRADE,MarketGoods.concept(good),Concept.EMERALD)))return;
        var opinion=to.mind.opinionOf(buyer.getUUID());
        int price=TradePolicy.ask(to.mind.has(Trait.GREEDY),to.mind.mood().low(),opinion.regard(),opinion.fear());
        if(price==0 || !TradePolicy.sellerSafe(to.mind.foodLevel(),seller.getHealth(),seller.getMaxHealth(),to.mind.fatigue()))return;
        boolean ready=MarketGoods.offer(seller.getInventory(),seller.getMainHandItem(),AdventureActions.canHarvest(seller),good)>=0;
        rememberDemand(level,buyer,from,seller,to,good,price,now,ready);
    }
    private static void rememberDemand(ServerLevel level,Villager buyer,Session from,Villager seller,Session to,
                                       MarketGoods.Good good,int price,long now,boolean ready) {
        // A missing commodity is not authority to gather, plant or expand terrain work.
        if(good==MarketGoods.Good.WOOD && !ready)return;
        if(!ready && good==MarketGoods.Good.HOE && !ToolProduction.canPrepare(seller,to))return;
        if(!ready && good==MarketGoods.Good.FOOD && !FoodSupply.canPrepare(seller.getInventory(),AdventureActions.canHarvest(seller)))return;
        if(!ready&&(good==MarketGoods.Good.HOE||good==MarketGoods.Good.FOOD)){
            WorkLife.negotiate(level,buyer,from,seller,good==MarketGoods.Good.HOE?local.villagers.work.WorkOrders.Task.WOODEN_HOE:local.villagers.work.WorkOrders.Task.BREAD);return;
        }
        var concept=MarketGoods.concept(good);
        var quote=new ArrayList<Concept>();quote.add(Concept.TRADE);quote.add(concept);
        for(int i=0;i<price;i++)quote.add(Concept.EMERALD);
        if(price>TradePolicy.bid(good!=MarketGoods.Good.FOOD,from.mind.foodLevel())
                || buyer.getInventory().countItem(Items.EMERALD)<price || from.mind.opinionOf(seller.getUUID()).fear()>=.6
                || !understands(level,seller,to,buyer,from,quote)
                || !understands(level,buyer,from,seller,to,List.of(Concept.YES,Concept.TRADE)))return;
        boolean heard=switch(good) {
            case HOE->to.toolDemand.hear(buyer.getUUID(),now);
            case FOOD->to.foodDemand.hearTrade(buyer.getUUID(),now);
            case WOOD->to.woodCustomer.hear(buyer.getUUID(),now);
        };
        if(heard) {
            if(good==MarketGoods.Good.FOOD)from.foodSupplier.hear(seller.getUUID(),now);
            if(good==MarketGoods.Good.HOE)from.toolSupplier.hear(seller.getUUID(),now);
            if(good==MarketGoods.Good.WOOD)from.woodSupplier.hear(seller.getUUID(),now);
            var inquiry=Map.<String,Object>of("gameTime",now,"customer",buyer.getUUID().toString(),"kind",good.name(),"price",price,
                    "customerFood",from.mind.foodLevel(),"customerMoney",buyer.getInventory().countItem(Items.EMERALD),
                    "distanceSquared",buyer.distanceToSqr(seller),"ready",ready);
            if(good==MarketGoods.Good.WOOD)to.lastStockInquiry=inquiry;
            else {to.nextToolWork=now;to.nextFamilyProvision=now;to.productionRequestsHeard++;to.lastProductionRequest=inquiry;}
            from.tradeStatus=(ready?"heard local ":"requested local ")+good.name().toLowerCase(java.util.Locale.ROOT)+(ready?" availability":" production");
            to.mind.remember("Heard understood affordable "+(ready?"ready-stock inquiry":"production request")+" for "+concept.gloss()+" from "+buyer.getUUID()+"; no stock reserved or paid");
            Villagers.persist(to);
        }
    }
    private static boolean understands(ServerLevel level,Villager speaker,Session from,Villager listener,Session to,List<Concept> concepts) {
        return FoodRequest.understands(Speech.tradeSignal(level,speaker,from,listener,concepts),to.mind.lexicon(),concepts.toArray(Concept[]::new));
    }
}
