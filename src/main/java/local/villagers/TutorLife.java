package local.villagers;

import java.util.*;
import local.villagers.language.*;
import local.villagers.learning.LearningMode;
import local.villagers.mind.Action;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;

/** Voluntary nearby teaching for children and adults requesting help. Ordinary needs/work preempt its bounded attention turns. */
final class TutorLife {
    private TutorLife(){}
    private static final List<Concept> CURRICULUM=List.of(Concept.WOOD,Concept.STONE,Concept.HOE,Concept.SWORD,
            Concept.WOODEN_HOE,Concept.STONE_HOE,Concept.WOODEN_SWORD,Concept.STONE_SWORD,Concept.BREAD);
    static Set<Concept> features(ItemStack item){
        if(item.isEmpty())return Set.of();String id=BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
        var seen=EnumSet.noneOf(Concept.class);var generic=Concept.forItem(id);if(generic!=null)seen.add(generic);
        var specific=Concept.specificItem(id);if(specific!=null){seen.add(specific);var roots=WordFormation.roots(specific);seen.add(roots.material());seen.add(roots.head());}
        return Set.copyOf(seen);
    }
    private static boolean safe(ServerLevel level,Villager v,Session s){return v.isAlive()&&Villagers.isManaged(v)&&v.level()==level
            &&!level.isDarkOutside()&&!v.isSleeping()&&!v.isTrading()&&!v.isOnFire()&&!v.isInWater()&&v.getAirSupply()>=200
            &&s.action!=Action.FLEE&&s.mind.foodLevel()>10&&s.mind.fatigue()<.65&&v.getHealth()>=v.getMaxHealth()*.8
            &&(v.getLastHurtByMob()==null||v.tickCount-v.getLastHurtByMobTimestamp()>=200);}
    private static boolean contact(Villager a,Villager b){double range=Math.min(4,Villagers.config().hearingRadius());return a!=b&&a.distanceToSqr(b)<=range*range&&a.hasLineOfSight(b)&&b.hasLineOfSight(a);}
    private static void close(Session s,Session child,long now){
        if(child!=null&&Objects.equals(child.tuitionPeer,s.mind.id())){child.tuitionPeer=null;child.tuitionUntil=0;}
        s.tuitionPeer=null;s.tuitionTopic=null;s.tuitionUntil=0;s.nextTuitionOffer=now+200;
    }
    /** Display only actual owned examples, varying tools across demonstrations of a shared material/head. */
    private static boolean display(ServerLevel level,Villager v,Concept topic,long attempt){
        var slots=new ArrayList<Integer>();if(features(v.getMainHandItem()).contains(topic))slots.add(-1);
        for(int i=0;i<v.getInventory().getContainerSize();i++)if(features(v.getInventory().getItem(i)).contains(topic))slots.add(i);
        if(slots.isEmpty())return false;
        // Sort by actual item id so swaps do not make one exemplar permanently first.
        slots.sort(Comparator.comparing(i->BuiltInRegistries.ITEM.getKey((i<0?v.getMainHandItem():v.getInventory().getItem(i)).getItem()).toString()));
        int chosen=slots.get((int)(attempt%slots.size()));return chosen<0||AdventureActions.displayForLesson(level,v,chosen);
    }
    private static List<Tuition.Seen> perceive(ServerLevel level,Villager viewer){
        return level.getEntitiesOfClass(Villager.class,viewer.getBoundingBox().inflate(4),v->v.isAlive()&&viewer.hasLineOfSight(v)&&viewer.distanceToSqr(v)<=16)
                .stream().sorted(Comparator.comparingDouble(viewer::distanceToSqr)).limit(8)
                .map(v->new AbstractMap.SimpleEntry<>(v,features(v.getMainHandItem()))).filter(e->!e.getValue().isEmpty())
                .map(e->new Tuition.Seen(e.getKey().getUUID(),e.getValue())).toList();
    }
    /** A listener notices its own comprehension gap, without copying the speaker's intended meaning. */
    static void misunderstood(Session learner,UUID speaker,List<String> words,long now){
        if(Villagers.LEARNING_MODE==LearningMode.FROZEN||Objects.equals(learner.mind.id(),speaker)
                ||Sentence.interpret(words,learner.mind.lexicon()).complete())return;
        learner.tuitionHelpFrom=speaker;learner.tuitionHelpUntil=now+8000;
    }
    /** A publicly directed repair request includes a visible questioning gesture, usable before common vocabulary. */
    private static String requestHelp(ServerLevel level,Villager learner,Session s,long now){
        if(s.tuitionHelpFrom==null||now>s.tuitionHelpUntil||now<s.nextTuitionHelp)return null;
        if(!(level.getEntity(s.tuitionHelpFrom) instanceof Villager teacher)||!contact(learner,teacher))return null;
        var t=Villagers.existingSession(teacher.getUUID());if(t==null||teacher.isBaby()||!safe(level,teacher,t)||!s.tuitionBudget.take(now))return null;
        learner.getLookControl().setLookAt(teacher,30,30);
        Speech.clauses(level,learner,s,List.of(new Clause(Concept.I,Concept.ASK,Concept.AGAIN)),false,teacher,Set.of());
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,learner.getX(),learner.getY()+1.6,learner.getZ(),2,.05,.05,.05,0);
        if(t.tuitionRequests.size()>=8&&!t.tuitionRequests.containsKey(learner.getUUID()))t.tuitionRequests.remove(t.tuitionRequests.firstEntry().getKey());
        t.tuitionRequests.put(learner.getUUID(),now+8000);s.nextTuitionHelp=now+1200;
        learner.getNavigation().stop();return "asking for language help";
    }
    static String work(ServerLevel level,Villager v,Session s,long now){
        if(Villagers.LEARNING_MODE==LearningMode.FROZEN)return null;
        if(!safe(level,v,s))return null;
        if(s.tuitionTopic==null&&s.tuitionPeer!=null&&now<=s.tuitionUntil){v.getNavigation().stop();s.action=null;s.before=null;return "watching a lesson";}
        if(s.tuitionTopic==null)s.tuitionPeer=null;
        if(v.isBaby())return null;
        if(s.tuitionTopic!=null){
            var entity=level.getEntity(s.tuitionPeer);var child=entity instanceof Villager other?other:null;
            var c=child==null?null:Villagers.existingSession(child.getUUID());
            if(child==null||c==null||(!child.isBaby()&&now>s.tuitionRequests.getOrDefault(child.getUUID(),-1L))||!safe(level,child,c)||!contact(v,child)||now>s.tuitionUntil){close(s,c,now);return null;}
            v.getNavigation().stop();s.action=null;s.before=null;
            if(!s.tuitionBudget.take(now))return "teaching "+s.tuitionTopic.gloss();
            var topic=s.tuitionTopic;var history=s.mind.lessons().of(child.getUUID(),topic);
            if(!display(level,v,topic,history==null?0:history.demonstrations()-1)){close(s,c,now);return null;}
            var request=Speech.lesson(level,v,s,child,Tuition.question(topic),v,features(v.getMainHandItem()),false);s.tuitionQuestions++;
            Tuition.Answer answer=request==null?null:Tuition.answer(request.sounds(),c.mind.lexicon(),perceive(level,child));
            boolean correct=false;
            if(answer!=null&&c.tuitionBudget.take(now)&&level.getEntity(answer.pointedAt()) instanceof Villager pointed&&v.hasLineOfSight(pointed)){
                var response=Speech.lesson(level,child,c,v,answer.words(),pointed,features(pointed.getMainHandItem()),false);
                if(response!=null){
                    var publicPoint=perceive(level,v).stream().filter(p->p.target().equals(answer.pointedAt())).findFirst().orElse(null);
                    correct=Tuition.confirms(topic,Sentence.interpret(response.sounds(),s.mind.lexicon()),publicPoint);
                    var feedback=Speech.clauses(level,v,s,List.of(new Clause(Concept.I,Concept.SAY,correct?Concept.YES:Concept.NO)),false,child,Set.of());
                    if(correct&&feedback!=null&&Sentence.interpret(feedback.sounds(),c.mind.lexicon()).clauses().equals(List.of(new Clause(Concept.I,Concept.SAY,Concept.YES))))
                        c.mind.lexicon(c.mind.lexicon().confirm(response.sounds()));
                }
            }
            if(correct){s.mind.lessons(s.mind.lessons().confirm(child.getUUID(),topic,now));s.tuitionCorrect++;}else s.tuitionUnresolved++;
            s.tuitionStatus=correct?"confirmed "+topic.gloss():"unresolved "+topic.gloss();close(s,c,now);return s.tuitionStatus;
        }
        String request=requestHelp(level,v,s,now);if(request!=null)return request;
        s.tuitionRequests.entrySet().removeIf(e->now>e.getValue());
        if(now<s.nextTuitionOffer||now<s.nextTuitionScan||!s.tuitionBudget.ready(now))return null;
        s.nextTuitionScan=now+100;
        var children=level.getEntitiesOfClass(Villager.class,v.getBoundingBox().inflate(4),other->(other.isBaby()||s.tuitionRequests.containsKey(other.getUUID()))&&contact(v,other)).stream()
                .sorted(Comparator.comparingDouble(v::distanceToSqr)).limit(8).toList();
        for(var child:children){var c=Villagers.existingSession(child.getUUID());if(c==null||!safe(level,child,c)||c.tuitionPeer!=null&&now<=c.tuitionUntil)continue;
            for(var topic:CURRICULUM){
                if(s.mind.lessons().confirmed(child.getUUID(),topic))continue;
                var old=s.mind.lessons().of(child.getUUID(),topic);
                if(!display(level,v,topic,old==null?0:old.demonstrations()))continue;
                if(!s.tuitionBudget.take(now))return null;
                if(Speech.lesson(level,v,s,child,Tuition.demonstrate(topic),v,features(v.getMainHandItem()),true)==null)return null;
                s.mind.lessons(s.mind.lessons().demonstrate(child.getUUID(),topic,now));s.tuitionDemonstrations++;
                s.tuitionPeer=child.getUUID();s.tuitionTopic=topic;s.tuitionUntil=now+260;
                c.tuitionPeer=v.getUUID();c.tuitionUntil=now+260;
                v.getNavigation().stop();child.getNavigation().stop();s.action=null;s.before=null;
                s.tuitionStatus="demonstrating "+topic.gloss();return s.tuitionStatus;
            }
        }
        return null;
    }
}
