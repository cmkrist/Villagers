package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import local.villagers.economy.TradePolicy;
import local.villagers.language.*;
import local.villagers.learning.LearningMode;
import local.villagers.mind.Place;
import local.villagers.navigation.WorkJourney;
import local.villagers.navigation.WorkSearch;
import local.villagers.relations.Opinion;
import local.villagers.work.WorkOrders;
import local.villagers.work.WorkOrders.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;

/** Explicit commissioned production. Existing crafting, stock, movement and contact-payment rules remain authoritative. */
final class WorkLife {
    private WorkLife() {}
    enum Outcome { AGREED, REFUSED, UNKNOWN, UNAVAILABLE, MISMATCH }
    private static boolean body(ServerLevel level,Villager v){return Villagers.isManaged(v)&&v.isAlive()&&!v.isBaby()&&v.level()==level
            &&!v.isSleeping()&&!v.isTrading()&&!v.isOnFire()&&!v.isInWater()&&v.getAirSupply()>=200;}
    private static boolean safe(ServerLevel level,Villager v,Session s){return body(level,v)&&!level.isDarkOutside()&&s.action!=local.villagers.mind.Action.FLEE
            &&(v.getLastHurtByMob()==null||v.tickCount-v.getLastHurtByMobTimestamp()>=200)
            &&TradePolicy.sellerSafe(s.mind.foodLevel(),v.getHealth(),v.getMaxHealth(),s.mind.fatigue());}
    private static boolean audible(ServerLevel level,Villager a,Villager b){return a!=b&&body(level,a)&&body(level,b)
            &&TradePolicy.canHearProposal(a.distanceToSqr(b),Villagers.config().hearingRadius(),a.hasLineOfSight(b)&&b.hasLineOfSight(a));}
    private static boolean canMake(Task task){return AdventureActions.PERMISSIONS.allows(task==Task.BREAD?Capability.CRAFT_BREAD:Capability.CRAFT_WOODEN_HOE);}
    private static boolean materials(Villager v,Task task){return task==Task.BREAD?BreadCrafting.wheatCount(v.getInventory())>=3:WoodCrafting.next(v.getInventory())!=null;}
    private static boolean wants(Villager v,Session s,Task task){return task==Task.BREAD?s.mind.foodLevel()>6&&FoodSupply.deficit(v.getInventory(),AdventureActions.canHarvest(v))>0
            :!AdventureActions.hasHoe(v)&&v.getInventory().hasAnyMatching(PlantingMaterials::supported);}
    private static boolean readyWorker(ServerLevel level,Villager v,Session s,Task task){return safe(level,v,s)&&canMake(task)&&materials(v,task)
            &&FoodSupply.deficit(v.getInventory(),AdventureActions.canHarvest(v))==0
            &&(task==Task.BREAD||ToolProduction.canPrepare(v,s))&&BreadCrafting.table(level,v,s)!=null;}
    private static WorkDialogue.Frame heard(Utterance words,Session listener){return words==null?null:WorkDialogue.decode(Sentence.interpret(words.sounds(),listener.mind.lexicon()));}
    private static Utterance say(ServerLevel level,Villager v,Session s,Villager other,WorkDialogue.Frame frame){return Speech.clauses(level,v,s,WorkDialogue.words(frame),false,other,Set.of());}
    private static boolean is(WorkDialogue.Frame frame,WorkDialogue.Act act,Task task,int price){return frame!=null&&frame.act()==act&&frame.task()==task&&frame.price()==price;}
    static Outcome negotiate(ServerLevel level,Villager buyer,Session from,Villager worker,Task wanted){
        long now=level.getGameTime();var to=Villagers.existingSession(worker.getUUID());
        if(Villagers.LEARNING_MODE==LearningMode.FROZEN||to==null||now>Long.MAX_VALUE-WorkOrders.LIFETIME||!audible(level,buyer,worker)
                ||now<from.nextOrderOffer||from.mind.workOrders().open()!=null||to.mind.workOrders().open()!=null
                ||!wants(buyer,from,wanted)||buyer.getInventory().countItem(Items.EMERALD)<1
                ||!from.workDialogueBudget.ready(now)||!to.workDialogueBudget.ready(now))return Outcome.UNAVAILABLE;
        from.workDialogueBudget.take(now);to.workDialogueBudget.take(now);from.nextOrderOffer=now+600;from.orderProposals++;
        Speech.ensureLanguage(level,buyer,from);Speech.ensureLanguage(level,worker,to);
        var request=heard(say(level,buyer,from,worker,new WorkDialogue.Frame(WorkDialogue.Act.REQUEST,wanted,1)),to);
        if(request==null||request.act()!=WorkDialogue.Act.REQUEST){from.ordersUnknown++;return Outcome.UNKNOWN;}
        var opinion=to.mind.opinionOf(buyer.getUUID());int ask=TradePolicy.ask(to.mind.has(local.villagers.genetics.Trait.GREEDY),to.mind.mood().low(),opinion.regard(),opinion.fear());
        if(ask==0||!readyWorker(level,worker,to,request.task())){
            var no=Speech.clauses(level,worker,to,List.of(new Clause(Concept.I,Concept.SAY,Concept.NO)),false,buyer,Set.of());
            if(no!=null&&local.villagers.language.Dialogue.response(Sentence.interpret(no.sounds(),from.mind.lexicon()))==Dialogue.Response.REFUSAL)from.ordersRefused++;
            return Outcome.REFUSED;
        }
        var quote=heard(say(level,worker,to,buyer,new WorkDialogue.Frame(WorkDialogue.Act.QUOTE,request.task(),ask)),from);
        if(quote==null||quote.act()!=WorkDialogue.Act.QUOTE||quote.task()!=wanted||quote.price()>TradePolicy.bid(wanted==Task.WOODEN_HOE,from.mind.foodLevel())
                ||buyer.getInventory().countItem(Items.EMERALD)<quote.price()||from.mind.opinionOf(worker.getUUID()).fear()>=.6){from.ordersUnknown++;return Outcome.UNKNOWN;}
        var accepted=heard(say(level,buyer,from,worker,new WorkDialogue.Frame(WorkDialogue.Act.ACCEPT,quote.task(),quote.price())),to);
        if(!is(accepted,WorkDialogue.Act.ACCEPT,request.task(),ask)){from.ordersUnknown++;return Outcome.UNKNOWN;}
        var ack=heard(say(level,worker,to,buyer,new WorkDialogue.Frame(WorkDialogue.Act.ACKNOWLEDGE,accepted.task(),accepted.price())),from);
        if(!is(ack,WorkDialogue.Act.ACKNOWLEDGE,quote.task(),quote.price())){from.ordersUnknown++;return Outcome.UNKNOWN;}
        // Each receipt uses that actor's independently decoded terms. The shared gate never supplies missing meanings.
        var buyerTerms=new Terms(buyer.getUUID(),worker.getUUID(),ack.task(),1,ack.price(),now,now+WorkOrders.LIFETIME);
        var workerTerms=new Terms(buyer.getUUID(),worker.getUUID(),accepted.task(),1,accepted.price(),now,now+WorkOrders.LIFETIME);
        from.mind.workOrders(from.mind.workOrders().accept(buyerTerms));to.mind.workOrders(to.mind.workOrders().accept(workerTerms));
        boolean matches=buyerTerms.equals(workerTerms);
        if(!matches){pause(from,buyerTerms,Reason.TERMS,now);pause(to,workerTerms,Reason.TERMS,now);}
        else {from.ordersAccepted++;to.ordersAccepted++;}
        from.mind.remember("Accepted spoken work terms: "+ack.task()+" x1 for "+ack.price()+" emeralds, due "+buyerTerms.dueAt());
        to.mind.remember("Promised production from understood terms: "+accepted.task()+" x1 for "+accepted.price()+" emeralds, due "+workerTerms.dueAt());
        Villagers.persist(from);Villagers.persist(to);return matches?Outcome.AGREED:Outcome.MISMATCH;
    }
    static boolean bound(Session s,MarketGoods.Good good){var e=s.mind.workOrders().open();return e!=null&&(e.terms().task()==Task.BREAD?good==MarketGoods.Good.FOOD:good==MarketGoods.Good.HOE);}
    private static void update(Session s,WorkOrders next){if(!next.equals(s.mind.workOrders())){s.mind.workOrders(next);Villagers.persist(s);}}
    private static void pause(Session s,Terms t,Reason reason,long now){update(s,s.mind.workOrders().state(t,State.PAUSED,reason,now));if(t.worker().equals(s.mind.id()))clearSignal(s,t);s.orderStatus="work paused: "+reason;}
    private static void clearSignal(Session s,Terms t){if(t.task()==Task.WOODEN_HOE)s.toolDemand.fulfilled(t.buyer());else s.foodDemand.tradeFulfilled(t.buyer());}
    static void review(ServerLevel level,Villager v,Session s){
        if(Villagers.LEARNING_MODE==LearningMode.FROZEN)return;
        long now=level.getGameTime();var old=s.mind.workOrders().open();update(s,s.mind.workOrders().expire(now));
        if(old!=null&&s.mind.workOrders().open()==null){clearSignal(s,old.terms());s.orderJourney.interrupt(now);s.orderSearch.clear();s.orderStatus="work unfulfilled at deadline";
            s.mind.remember("Work agreement expired unfulfilled; last own obstacle was "+old.reason()+"; no payment or blame awarded");}
        var e=s.mind.workOrders().open();if(e!=null&&e.terms().worker().equals(v.getUUID())&&!safe(level,v,s))pause(s,e.terms(),Reason.UNSAFE,now);
    }
    /** Actual successful recipe callbacks only, with own station memory. No customer memory is edited here. */
    static void produced(ServerLevel level,Villager v,Session s,Task task,BlockPos station){
        if(Villagers.LEARNING_MODE==LearningMode.FROZEN)return;
        var e=s.mind.workOrders().open();long now=level.getGameTime();
        if(e==null||e.state()!=State.WORKING||e.terms().task()!=task||!e.terms().worker().equals(v.getUUID()))return;
        update(s,s.mind.workOrders().produced(e.terms(),new Production(now,new Place(level.dimension().identifier().toString(),station.getX(),station.getY(),station.getZ()),1),now));
    }
    static String work(ServerLevel level,Villager v,Session s,long now){
        var e=s.mind.workOrders().open();if(Villagers.LEARNING_MODE==LearningMode.FROZEN||e==null||!e.current(now)||!e.terms().worker().equals(v.getUUID()))return null;
        var t=e.terms();
        if(!safe(level,v,s)){pause(s,t,Reason.UNSAFE,now);return null;}
        // Perception bounds the physical target. Its durable matching receipt is a consent guard, not a knowledge source.
        if(!(level.getEntity(t.buyer()) instanceof Villager buyer)||!body(level,buyer)||!v.hasLineOfSight(buyer)
                ||!local.villagers.relations.FoodRequest.mayApproach(v.distanceToSqr(buyer),true,VillageSurvival.withinHomeRange(level,s.mind,buyer.position()))){pause(s,t,Reason.PEER_UNAVAILABLE,now);return null;}
        var peer=Villagers.existingSession(t.buyer());
        if(peer==null){pause(s,t,Reason.PEER_UNAVAILABLE,now);return null;}
        if(!WorkOrders.mutual(e,peer.mind.workOrders().open(),now)){pause(s,t,Reason.TERMS,now);return null;}
        if(e.production()!=null)return deliver(level,v,s,buyer,peer,e,now);
        if(!canMake(t.task())||!materials(v,t.task())||FoodSupply.deficit(v.getInventory(),AdventureActions.canHarvest(v))>0){pause(s,t,Reason.MATERIALS,now);return null;}
        var table=BreadCrafting.table(level,v,s);if(table==null){pause(s,t,Reason.STATION,now);return null;}
        s.action=null;s.before=null;
        if(t.task()==Task.WOODEN_HOE){
            if(now<s.nextToolWork)return null;
            if(!ToolProduction.canPrepare(v,s)){pause(s,t,Reason.MATERIALS,now);return null;}
            update(s,s.mind.workOrders().state(t,State.WORKING,Reason.NONE,now));
            s.toolDemand.hear(t.buyer(),now);
            var status=ToolProduction.work(level,v,s,now);
            if(status==null)pause(s,t,Reason.ROUTE,now);
            return s.orderStatus=status==null?"waiting for permitted hoe work":status;
        }
        if(!BreadCrafting.canWorkAt(level,v,s,table)){
            s.orderWorkStarted=0;
            if(!BreadCrafting.approach(level,v,s,table,now)){pause(s,t,Reason.ROUTE,now);return null;}
            update(s,s.mind.workOrders().state(t,State.WORKING,Reason.NONE,now));
            return s.orderStatus="approaching station for agreed bread work";
        }
        update(s,s.mind.workOrders().state(t,State.WORKING,Reason.NONE,now));
        v.getNavigation().stop();
        if(s.orderWorkStarted==0||now-s.orderWorkLast>10)s.orderWorkStarted=now;
        s.orderWorkLast=now;
        if(now-s.orderWorkStarted>=20){
            if(!BreadCrafting.tryCraft(level,v,s,table))pause(s,t,Reason.MATERIALS,now);
            s.orderWorkStarted=0;
        }
        return s.orderStatus="making the agreed bread from owned wheat";
    }
    private static int offered(Villager worker,Task task){
        int slot=MarketGoods.offer(worker.getInventory(),worker.getMainHandItem(),AdventureActions.canHarvest(worker),task==Task.BREAD?MarketGoods.Good.FOOD:MarketGoods.Good.HOE);
        if(slot<0)return -1;var stack=worker.getInventory().getItem(slot);
        return task==Task.BREAD?stack.is(Items.BREAD)?slot:-1:stack.is(Items.WOODEN_HOE)&&stack.getDamageValue()==0?slot:-1;
    }
    private static String deliver(ServerLevel level,Villager worker,Session s,Villager buyer,Session peer,Order e,long now){
        var t=e.terms();if(now<s.nextOrderAttempt)return null;
        int slot=offered(worker,t.task());if(slot<0){pause(s,t,Reason.STOCK,now);return null;}
        boolean near=local.villagers.relations.FoodRequest.inReach(worker.distanceToSqr(buyer),Villagers.config().hearingRadius(),worker.hasLineOfSight(buyer)&&buyer.hasLineOfSight(worker));
        var step=s.orderJourney.advance(t.toString(),now,near,worker.getX(),worker.getY(),worker.getZ());
        if(step==WorkJourney.Step.WAIT)return null;
        if(step==WorkJourney.Step.ARRIVED){
            worker.getNavigation().stop();s.nextOrderAttempt=now+40;
            if(buyer.getInventory().countItem(Items.EMERALD)<t.price()){pause(s,t,Reason.PAYMENT,now);return null;}
            var plan=TradeMaterials.reserve(buyer.getInventory(),worker.getInventory(),slot,1,t.price());
            if(plan==null){pause(s,t,Reason.CAPACITY,now);return null;}
            var item=plan.goods.getItem();int bg=buyer.getInventory().countItem(item),wg=worker.getInventory().countItem(item),bc=buyer.getInventory().countItem(Items.EMERALD),wc=worker.getInventory().countItem(Items.EMERALD);
            if(!WorkOrders.mutual(s.mind.workOrders().open(),peer.mind.workOrders().open(),now)||!TradeMaterials.commit(buyer.getInventory(),worker.getInventory(),plan))return null;
            var own=new Settlement(now,wg,worker.getInventory().countItem(item),wc,worker.getInventory().countItem(Items.EMERALD));
            var received=new Settlement(now,bg,buyer.getInventory().countItem(item),bc,buyer.getInventory().countItem(Items.EMERALD));
            update(s,s.mind.workOrders().complete(t,worker.getUUID(),own,now));update(peer,peer.mind.workOrders().complete(t,buyer.getUUID(),received,now));
            s.mind.witness(buyer.getUUID(),Opinion.Event.KEPT_PROMISE,now);peer.mind.witness(worker.getUUID(),Opinion.Event.KEPT_PROMISE,now);clearSignal(s,t);
            var receipt=Map.<String,Object>of("gameTime",now,"terms",t,"workerOwnInventory",own,"buyerOwnInventory",received,"production",e.production());
            s.lastWorkSettlement=receipt;peer.lastWorkSettlement=receipt;s.orderStatus="completed agreed production and paid delivery";
            s.mind.remember("Completed actual "+t.task()+" production and delivery for "+t.price()+" emeralds");peer.mind.remember("Received agreed "+t.task()+" and paid "+t.price()+" actual emeralds");
            Villagers.persist(s);Villagers.persist(peer);return s.orderStatus;
        }
        if(step==WorkJourney.Step.FAILED){pause(s,t,Reason.ROUTE,now);s.nextOrderAttempt=now+WorkJourney.BACKOFF;worker.getNavigation().stop();return null;}
        s.action=null;s.before=null;
        if(step==WorkJourney.Step.START){worker.getNavigation().stop();s.nextOrderPath=now;}
        if(now>=s.nextOrderPath&&WorkNavigation.admit(level,now,worker)){
            s.nextOrderPath=now+40;var result=TradeApproach.move(level,worker,s,buyer,s.orderSearch);
            if(result==WorkSearch.Step.EXHAUSTED){pause(s,t,Reason.ROUTE,now);s.orderJourney.reject(now);s.nextOrderAttempt=now+WorkJourney.BACKOFF;return null;}
            if(result==WorkSearch.Step.FOUND){update(s,s.mind.workOrders().resume(t,now));s.orderJourney.navigating(t.toString(),now,worker.getX(),worker.getY(),worker.getZ());}
        }
        return s.orderStatus="bringing agreed output for the agreed payment";
    }
    static boolean cancel(ServerLevel level,Villager actor,Session s){
        var e=s.mind.workOrders().open();if(e==null||Villagers.LEARNING_MODE==LearningMode.FROZEN)return false;
        var t=e.terms();long now=level.getGameTime();if(!e.current(now)){update(s,s.mind.workOrders().expire(now));return false;}update(s,s.mind.workOrders().state(t,State.CANCELLED,Reason.WITHDREW,now));clearSignal(s,t);
        if(level.getEntity(t.peer(actor.getUUID())) instanceof Villager other&&audible(level,actor,other)){
            var peer=Villagers.existingSession(other.getUUID());
            if(peer!=null&&s.workDialogueBudget.ready(now)&&peer.workDialogueBudget.ready(now)){
                s.workDialogueBudget.take(now);peer.workDialogueBudget.take(now);
                var act=t.buyer().equals(actor.getUUID())?WorkDialogue.Act.CANCEL_BUYER:WorkDialogue.Act.CANCEL_WORKER;
                var decoded=heard(say(level,actor,s,other,new WorkDialogue.Frame(act,t.task(),t.price())),peer);
                var otherOrder=peer.mind.workOrders().open();
                if(is(decoded,act,t.task(),t.price())&&otherOrder!=null&&otherOrder.terms().peer(other.getUUID()).equals(actor.getUUID())&&otherOrder.terms().task()==decoded.task()&&otherOrder.terms().price()==decoded.price()){
                    update(peer,peer.mind.workOrders().state(otherOrder.terms(),State.CANCELLED,Reason.HEARD_WITHDRAWAL,now));clearSignal(peer,otherOrder.terms());
                }
            }
        }
        return true;
    }
}
