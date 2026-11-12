package local.villagers;

import java.util.*;
import local.villagers.language.*;
import local.villagers.mind.Beliefs;
import local.villagers.learning.LearningMode;
import local.villagers.relations.FoodRequest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

/** Practical dialogue chooses perceived leads. It grants neither private inventory access nor a route permission. */
final class FoodQuestions {
    private FoodQuestions() {}
    enum Result { WAIT, LEAD, PASS }
    private static boolean audible(Villager a,Villager b){return a!=b&&a.isAlive()&&b.isAlive()&&a.level()==b.level()
            &&!a.isSleeping()&&!b.isSleeping()&&!a.isTrading()&&!b.isTrading()&&FoodRequest.audible(a.distanceToSqr(b),Villagers.config().hearingRadius(),a.hasLineOfSight(b)&&b.hasLineOfSight(a));}
    private static Sentence.Interpretation heard(Utterance words,Session listener){return words==null?new Sentence.Interpretation(List.of(),0,1):Sentence.interpret(words.sounds(),listener.mind.lexicon());}
    private static Utterance say(ServerLevel level,Villager actor,Session session,Villager listener,List<Clause> clauses){return Speech.clauses(level,actor,session,clauses,false,listener,Set.of());}
    private static void perceive(ServerLevel level,Villager observer,Session s,long now){
        // Only one's own bag is accessible. Third parties supply public held-item evidence only.
        boolean own=FoodInventory.containsFood(observer.getInventory())
                ||observer.getMainHandItem().has(DataComponents.FOOD);
        s.mind.beliefs(s.mind.beliefs().observe(new Beliefs.Claim(observer.getUUID(),Concept.HAVE,Concept.FOOD,null,!own,false),observer.getUUID(),now));
        for(var peer:level.getEntitiesOfClass(Villager.class,observer.getBoundingBox().inflate(8),v->v!=observer&&v.isAlive()
                &&observer.distanceToSqr(v)<=64&&observer.hasLineOfSight(v)).stream().limit(8).toList()){
            if(!peer.getMainHandItem().has(DataComponents.FOOD))continue;
            if(s.mind.lexicon().referents().nameFor(Referents.Target.person(peer.getUUID()))==null)continue;
            s.mind.beliefs(s.mind.beliefs().observe(new Beliefs.Claim(peer.getUUID(),Concept.HAVE,Concept.FOOD,null,false,false),observer.getUUID(),now));
        }
    }
    static Result ask(ServerLevel level,Villager asker,Session s,Villager respondent,long now){
        if(Villagers.LEARNING_MODE==LearningMode.FROZEN||!audible(asker,respondent))return Result.PASS;
        Session r=Villagers.existingSession(respondent.getUUID());if(r==null)return Result.PASS;
        var c=s.foodQuestion;
        if(c!=null&&c.current(now)){
            if(c.stage()==PracticalQuestions.Stage.INVESTIGATING)return Result.LEAD;
            if(c.stage()!=PracticalQuestions.Stage.ANSWERED||!c.peer().equals(respondent.getUUID()))return Result.PASS;
        }else c=null;
        if(c==null&&now<s.nextFoodQuestion)return Result.PASS;
        if(!s.dialogueBudget.ready(now)||!r.dialogueBudget.ready(now))return c==null?Result.PASS:Result.WAIT;
        s.dialogueBudget.take(now);r.dialogueBudget.take(now);
        Speech.ensureLanguage(level,asker,s);Speech.ensureLanguage(level,respondent,r);
        if(c==null){
            s.nextFoodQuestion=now+1200;
            var question=say(level,asker,s,respondent,PracticalQuestions.question(PracticalQuestions.Kind.WHO_FOOD));s.foodQuestions++;
            var understood=PracticalQuestions.kind(heard(question,r));
            perceive(level,respondent,r,now);
            var answer=PracticalQuestions.answer(understood,r.mind.beliefs(),respondent.getUUID(),asker.getUUID(),r.mind.lexicon().referents(),now);
            r.foodAnswer=answer==null?null:new PracticalQuestions.Offer(asker.getUUID(),answer.entry(),now+PracticalQuestions.LIFETIME);
            var reply=say(level,respondent,r,asker,answer==null?PracticalQuestions.unknown(Concept.FOOD):List.of(answer.clause()));
            var interpretation=heard(reply,s);
            s.mind.beliefs(s.mind.beliefs().hear(interpretation,respondent.getUUID(),asker.getUUID(),s.mind.opinionOf(respondent.getUUID()).trust(),now));
            var claim=PracticalQuestions.decodedAnswer(interpretation,PracticalQuestions.Kind.WHO_FOOD,respondent.getUUID(),asker.getUUID());
            s.foodQuestion=new PracticalQuestions.Conversation(respondent.getUUID(),PracticalQuestions.Kind.WHO_FOOD,claim,
                    claim==null?PracticalQuestions.Stage.UNKNOWN:PracticalQuestions.Stage.ANSWERED,now,now+PracticalQuestions.LIFETIME);
            s.mind.remember("Asked who has food; heard "+(interpretation.complete()?interpretation.english():"unresolved reply"));
            return claim==null?Result.PASS:Result.WAIT;
        }
        var question=say(level,asker,s,respondent,PracticalQuestions.question(PracticalQuestions.Kind.SOURCE));s.sourceQuestions++;
        var offer=r.foodAnswer;
        List<Clause> explanation=PracticalQuestions.unknown(Concept.SOURCE);
        if(PracticalQuestions.kind(heard(question,r))==PracticalQuestions.Kind.SOURCE&&offer!=null
                &&offer.peer().equals(asker.getUUID())&&now<offer.expires()
                &&r.mind.beliefs().entries().stream().anyMatch(e->e.claim().equals(offer.entry().claim())&&e.strength(now)>0))
            explanation=PracticalQuestions.sourceReply(offer.entry(),respondent.getUUID(),asker.getUUID(),r.mind.lexicon().referents(),now);
        var reply=say(level,respondent,r,asker,explanation);
        var source=PracticalQuestions.decodedSource(heard(reply,s),respondent.getUUID(),asker.getUUID());
        s.mind.beliefs(s.mind.beliefs().qualify(c.claim(),respondent.getUUID(),asker.getUUID(),source,now));
        boolean usable=source.root()!=null&&source.recent()&&!source.root().equals(asker.getUUID())
                &&s.mind.beliefs().foodLead(c.claim().subject(),now)>0;
        s.foodQuestion=c.stage(usable?PracticalQuestions.Stage.INVESTIGATING:PracticalQuestions.Stage.UNKNOWN);
        if(usable)s.qualifiedFoodLeads++;
        s.mind.remember("Asked how the food report was known; "+(usable?"will investigate the attributed lead":"source unresolved, old or circular"));
        Villagers.persist(s);Villagers.persist(r);
        return usable?Result.LEAD:Result.PASS;
    }
    static UUID lead(Session s,long now){var c=s.foodQuestion;return c!=null&&c.current(now)&&c.stage()==PracticalQuestions.Stage.INVESTIGATING?c.claim().subject():null;}
    static void unavailable(Session s){if(s.foodQuestion!=null)s.foodQuestion=s.foodQuestion.stage(PracticalQuestions.Stage.DECLINED);}
    /** Actual handoff verifies that food existed, not that the donor still owns another meal. */
    static void received(Session receiver,UUID donor,long now){
        var c=receiver.foodQuestion;
        if(c==null||!c.current(now)||c.stage()!=PracticalQuestions.Stage.INVESTIGATING||!donor.equals(c.claim().subject()))return;
        receiver.foodQuestion=c.stage(PracticalQuestions.Stage.VERIFIED);receiver.foodLeadVerifications++;
        receiver.mind.beliefs(receiver.mind.beliefs().observe(new Beliefs.Claim(donor,Concept.GIVE,Concept.FOOD,null,false,true),receiver.mind.id(),now));
        receiver.mind.remember("Verified the food lead through an actual handoff from "+donor);
    }
}
