package local.villagers;

import java.util.*;
import local.villagers.family.BreedingPolicy;
import local.villagers.genetics.Trait;
import local.villagers.language.*;
import local.villagers.learning.LearningMode;
import local.villagers.mind.Action;
import local.villagers.relations.Partnerships;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

/** Explicit local relationship dialogue. No parenthood, remote peer discovery or shared stock is implied. */
final class PartnershipLife {
    private PartnershipLife() {}
    enum Outcome { AGREED, REFUSED, UNKNOWN, UNAVAILABLE }
    private static boolean audible(Villager a,Villager b){
        return a!=b&&a.level()==b.level()&&a.isAlive()&&b.isAlive()&&!a.isSleeping()&&!b.isSleeping()
                &&!a.isTrading()&&!b.isTrading()&&local.villagers.relations.FoodRequest.audible(a.distanceToSqr(b),Villagers.config().hearingRadius(),a.hasLineOfSight(b)&&b.hasLineOfSight(a));
    }
    private static boolean available(Villager actor,Session s){
        return Villagers.isManaged(actor)&&actor.isAlive()&&!actor.isBaby()&&!actor.isSleeping()&&!actor.isTrading()
                &&!actor.isOnFire()&&!actor.isInWater()&&actor.getAirSupply()>=200&&s.action!=Action.FLEE
                &&(actor.getLastHurtByMob()==null||actor.tickCount-actor.getLastHurtByMobTimestamp()>=200);
    }
    private static boolean willing(Villager actor,Session s,UUID peer,long now){
        return Partnerships.willing(available(actor,s),(s.mind.partnerships().mayBegin(now)||s.mind.partnerships().committedTo(peer)&&s.mind.partnerships().active().agreedAt()<=now),false,actor.getHealth()/actor.getMaxHealth(),s.mind.foodLevel(),s.mind.fatigue(),
                s.mind.mood().low(),s.mind.opinionOf(peer),s.mind.expressedPersonality().sociability(),s.mind.has(Trait.LONER));
    }
    private static Utterance say(ServerLevel level,Villager actor,Session s,Villager listener,List<Clause> clauses){return Speech.clauses(level,actor,s,clauses,false,listener,Set.of());}
    private static Sentence.Interpretation hear(Utterance words,Session listener){return words==null?new Sentence.Interpretation(List.of(),0,1):Sentence.interpret(words.sounds(),listener.mind.lexicon());}
    private static RelationshipDialogue.Act decoded(Utterance words,Session listener){return RelationshipDialogue.act(hear(words,listener));}
    static boolean mutual(Session a,Session b){return Partnerships.mutual(a.mind.id(),a.mind.partnerships(),b.mind.id(),b.mind.partnerships());}

    static Outcome propose(ServerLevel level,Villager actor,Session s,Villager other){
        long now=level.getGameTime();var peer=Villagers.existingSession(other.getUUID());
        if(Villagers.LEARNING_MODE==LearningMode.FROZEN||peer==null||!audible(actor,other)||!available(other,peer)
                ||level.isDarkOutside()||now<s.nextPartnershipOffer||!willing(actor,s,other.getUUID(),now)
                ||!s.relationshipBudget.ready(now)||!peer.relationshipBudget.ready(now))return Outcome.UNAVAILABLE;
        // This is a structural kinship safeguard, not an ancestry lesson or access to a peer's private feelings.
        if(BreedingPolicy.closeKin(actor.getUUID(),s.mind.lineage(),other.getUUID(),peer.mind.lineage()))return Outcome.UNAVAILABLE;
        s.relationshipBudget.take(now);peer.relationshipBudget.take(now);s.nextPartnershipOffer=now+1200;s.partnershipProposals++;
        Speech.ensureLanguage(level,actor,s);Speech.ensureLanguage(level,other,peer);
        var proposal=say(level,actor,s,other,RelationshipDialogue.words(RelationshipDialogue.Act.PROPOSE_PARTNER));
        if(decoded(proposal,peer)!=RelationshipDialogue.Act.PROPOSE_PARTNER){
            say(level,other,peer,actor,List.of(new Clause(Concept.I,Concept.ASK,Concept.AGAIN)));
            s.partnershipUnknown++;s.partnershipStatus="proposal unresolved in the listener's language";return Outcome.UNKNOWN;
        }
        // Only the receiver evaluates its own needs, history, affection and existing commitment.
        boolean accepts=willing(other,peer,actor.getUUID(),now);
        var reply=say(level,other,peer,actor,RelationshipDialogue.words(accepts?RelationshipDialogue.Act.PROMISE_PARTNER:RelationshipDialogue.Act.REFUSE));
        var response=decoded(reply,s);
        if(!accepts){
            if(response==RelationshipDialogue.Act.REFUSE){s.partnershipRefusals++;s.partnershipStatus="understood an independent refusal";return Outcome.REFUSED;}
            s.partnershipUnknown++;s.partnershipStatus="reply unresolved";return Outcome.UNKNOWN;
        }
        if(response!=RelationshipDialogue.Act.PROMISE_PARTNER){s.partnershipUnknown++;s.partnershipStatus="partner promise unresolved";return Outcome.UNKNOWN;}
        // The proposer must make its own commitment, heard by the other person. Generic YES is insufficient.
        var confirmation=say(level,actor,s,other,RelationshipDialogue.words(RelationshipDialogue.Act.PROMISE_PARTNER));
        if(decoded(confirmation,peer)!=RelationshipDialogue.Act.PROMISE_PARTNER){s.partnershipUnknown++;s.partnershipStatus="reciprocal promise unresolved";return Outcome.UNKNOWN;}
        if(!willing(actor,s,other.getUUID(),now)||!willing(other,peer,actor.getUUID(),now))return Outcome.UNAVAILABLE;
        s.mind.partnerships(s.mind.partnerships().agree(other.getUUID(),now));
        peer.mind.partnerships(peer.mind.partnerships().agree(actor.getUUID(),now));
        s.partnershipAgreements++;peer.partnershipAgreements++;s.partnershipStatus=peer.partnershipStatus="mutually understood partnership";
        s.mind.remember("Understood mutual partner promises with "+peer.mind.name());peer.mind.remember("Understood mutual partner promises with "+s.mind.name());
        Villagers.persist(s);Villagers.persist(peer);return Outcome.AGREED;
    }
    /** Called after an actual completed conversation. Friendship is evidence for attraction, not automatic partnership. */
    static void contact(ServerLevel level,Villager actor,Session s,Villager other,Session peer){
        if(Villagers.LEARNING_MODE==LearningMode.FROZEN||!audible(actor,other)||actor.distanceToSqr(other)>9)return;
        if(s.mind.partnerships().committedTo(other.getUUID())&&Partnerships.wantsToEnd(s.mind.opinionOf(other.getUUID())))withdraw(level,actor,s,other);
        if(peer.mind.partnerships().committedTo(actor.getUUID())&&Partnerships.wantsToEnd(peer.mind.opinionOf(actor.getUUID())))withdraw(level,other,peer,actor);
        if(mutual(s,peer)&&available(actor,s)&&available(other,peer)){
            s.mind.partnerships(s.mind.partnerships().shared(other.getUUID(),level.getGameTime()));
            peer.mind.partnerships(peer.mind.partnerships().shared(actor.getUUID(),level.getGameTime()));
            Villagers.persist(s);Villagers.persist(peer);return;
        }
        if(s.mind.partnerships().active()==null){
            var previous=s.mind.partnerships().history().stream().filter(b->b.peer().equals(other.getUUID())).reduce((a,b)->b).orElse(null);
            if(previous!=null&&previous.end()==Partnerships.End.WITHDREW)announceWithdrawal(level,actor,s,other);
            propose(level,actor,s,other);
        }else if(s.mind.partnerships().committedTo(other.getUUID()))propose(level,actor,s,other);
    }
    /** Own consent can end while a partner is absent; an unloaded actor's location or state is not learned. */
    static void review(ServerLevel level,Villager actor,Session s){
        var bond=s.mind.partnerships().active();
        if(Villagers.LEARNING_MODE!=LearningMode.FROZEN&&bond!=null&&Partnerships.wantsToEnd(s.mind.opinionOf(bond.peer())))
            withdraw(level,actor,s,bond.peer());
    }
    private static void announceWithdrawal(ServerLevel level,Villager actor,Session s,Villager other){
        var peer=Villagers.existingSession(other.getUUID());long now=level.getGameTime();
        if(peer==null||!audible(actor,other)||!s.relationshipBudget.ready(now)||!peer.relationshipBudget.ready(now))return;
        s.relationshipBudget.take(now);peer.relationshipBudget.take(now);
        var words=say(level,actor,s,other,RelationshipDialogue.words(RelationshipDialogue.Act.WITHDRAW_PARTNER));
        if(decoded(words,peer)==RelationshipDialogue.Act.WITHDRAW_PARTNER){
            peer.mind.partnerships(peer.mind.partnerships().end(actor.getUUID(),now,Partnerships.End.HEARD_WITHDRAWAL));
            peer.courtship=null;peer.partnershipStatus="understood partner's withdrawal";Villagers.persist(peer);
        }
    }
    static boolean withdraw(ServerLevel level,Villager actor,Session s,Villager other){return withdraw(level,actor,s,other.getUUID());}
    static boolean withdraw(ServerLevel level,Villager actor,Session s,UUID other){
        if(Villagers.LEARNING_MODE==LearningMode.FROZEN||!s.mind.partnerships().committedTo(other))return false;
        long now=level.getGameTime();
        s.mind.partnerships(s.mind.partnerships().end(other,now,Partnerships.End.WITHDREW));
        s.courtship=null;s.partnershipWithdrawals++;s.partnershipStatus="withdrew own partnership consent";Villagers.persist(s);
        if(level.getEntity(other) instanceof Villager person)announceWithdrawal(level,actor,s,person);
        return true;
    }
}
