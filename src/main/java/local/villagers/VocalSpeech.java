package local.villagers;

import java.util.*;
import local.villagers.language.*;
import local.villagers.learning.LearningMode;
import local.villagers.mind.AgentMind;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.npc.villager.Villager;

/** Native grunt playback and actual cross-species acoustic learning. Meaning still comes from the listener's own lexicon. */
final class VocalSpeech {
    private VocalSpeech(){}
    private static final int MAX_VOICES=128,MAX_PENDING=Sentence.MAX_SOUNDS*2;
    private static final Random RANDOM=new Random(11071);
    private static final LinkedHashMap<UUID,Playback> VOICES=new LinkedHashMap<>();
    private record Frame(long at,float pitch,Voice.Kind sample){}
    private static final class Playback {
        Mob body;long nextSpeech;int played,imitations,interrupted;
        final ArrayDeque<Frame> frames=new ArrayDeque<>();
        List<String> lastWords=List.of();String lastSample="";
    }
    static void initialize(){
        ServerLifecycleEvents.SERVER_STARTED.register(s->VOICES.clear());ServerLifecycleEvents.SERVER_STOPPING.register(s->VOICES.clear());
        ServerTickEvents.END_SERVER_TICK.register(server->{
            VOICES.values().removeIf(p->!p.body.isAlive()||p.body.isRemoved());
            for(var p:VOICES.values())if(!p.frames.isEmpty()){
                var level=(ServerLevel)p.body.level();var frame=p.frames.peekFirst();if(level.getGameTime()<frame.at())continue;
                p.body.playSound(frame.sample()==Voice.Kind.VILLAGER?SoundEvents.VILLAGER_AMBIENT:SoundEvents.PILLAGER_AMBIENT,.45f,frame.pitch());
                p.lastSample=frame.sample()==Voice.Kind.VILLAGER?"minecraft:entity.villager.ambient":"minecraft:entity.pillager.ambient";
                p.frames.removeFirst();p.played++;
            }
        });
    }
    static Voice.Kind kind(Mob body){return body instanceof Pillager?Voice.Kind.PILLAGER:Voice.Kind.VILLAGER;}
    static AgentMind mind(Mob body){if(body instanceof Villager v){var s=Villagers.existingSession(v.getUUID());return s==null?null:s.mind;}return body instanceof Pillager?CreatureAgents.mind(body):null;}
    static String dialect(Voice.Kind kind,String biome){
        BiomeDialect.validate(biome);if(kind==Voice.Kind.VILLAGER)return biome;
        String path=biome.replace(':','/');if(path.length()>110)path=path.substring(0,110)+"_"+Integer.toUnsignedString(biome.hashCode(),36);
        return "pillager:"+path;
    }
    static Lexicon founding(Voice.Kind kind,String biome){return BiomeDialect.foundingLanguage(dialect(kind,biome));}
    static void ensureLanguage(ServerLevel level,Mob body,AgentMind mind){
        if(mind.originBiome()==null)mind.establishOrigin(level.getBiome(body.blockPosition()).unwrapKey().orElseThrow().identifier().toString());
        if(mind.lexicon().isEmpty()&&!mind.lexicon().developing())mind.lexicon(founding(kind(body),mind.originBiome()));
        String dialect=dialect(kind(body),mind.originBiome());
        mind.lexicon(mind.lexicon().establishGrammar(dialect).establishFormation(dialect));
    }
    private static Playback state(Mob speaker){
        var p=VOICES.get(speaker.getUUID());if(p!=null)return p;
        if(VOICES.size()==MAX_VOICES)VOICES.remove(VOICES.firstEntry().getKey());p=new Playback();p.body=speaker;VOICES.put(speaker.getUUID(),p);return p;
    }
    /** World adapter inputs are actual sounds plus independently visible physical facts, never intended clauses. */
    static void say(ServerLevel level,Mob speaker,AgentMind own,List<String> sounds,Set<Concept> publicFeatures,BlockPos pointed,Voice.Kind imitate){
        if(speaker.isSilent())return;
        var p=state(speaker);var pulses=Voice.plan(speaker.getUUID(),kind(speaker),sounds,imitate!=null);
        if(p.frames.size()+pulses.size()>MAX_PENDING){p.frames.clear();p.interrupted++;}
        long began=p.frames.isEmpty()?level.getGameTime():Math.max(level.getGameTime(),p.frames.peekLast().at()+8);
        boolean borrowed=imitate!=null&&own.voice().model(imitate)!=null&&own.voice().model(imitate).practice()>=3;
        for(int i=0;i<pulses.size();i++){var pulse=pulses.get(i);p.frames.addLast(new Frame(began+pulse.at(),pulse.pitch(),borrowed&&i%2==1?imitate:kind(speaker)));}
        p.lastWords=List.copyOf(sounds);if(imitate!=null)p.imitations++;
        if(Villagers.LEARNING_MODE==LearningMode.FROZEN)return;
        double radius=Math.min(16,Villagers.config().hearingRadius());
        for(var listener:level.getEntitiesOfClass(Mob.class,speaker.getBoundingBox().inflate(radius),m->m!=speaker&&m.isAlive()
                &&(m instanceof Villager&&Villagers.isManaged((Villager)m)||m instanceof Pillager&&CreatureAgents.managed(m))
                &&speaker.distanceToSqr(m)<=radius*radius&&m.hasLineOfSight(speaker)).stream().sorted(Comparator.comparingDouble(speaker::distanceToSqr)).limit(8).toList()){
            var learned=mind(listener);if(learned==null)continue;
            learned.voice(learned.voice().hear(kind(speaker),sounds,level.getGameTime()));
            // Existing villager Speech owns villager-to-villager semantic learning; do not count an example twice.
            if(speaker instanceof Villager&&listener instanceof Villager){
                if(imitate!=null)hearClaim(learned,sounds,speaker,listener,level.getGameTime());
                continue;
            }
            Set<Concept> visible=publicFeatures;
            if(pointed!=null&&!Speech.canSee(level,listener,pointed))visible=Set.of();
            ensureLanguage(level,listener,learned);
            Clause event=Speech.observedClause(visible);var context=EnumSet.noneOf(Concept.class);context.addAll(visible);if(event!=null)context.addAll(event.concepts());
            learned.lexicon(LanguageLearning.hear(learned.lexicon(),sounds,context,event,RANDOM));
            hearClaim(learned,sounds,speaker,listener,level.getGameTime());
        }
    }
    private static void hearClaim(AgentMind listener,List<String> sounds,Mob speaker,Mob body,long now){
        listener.beliefs(listener.beliefs().hear(Sentence.interpret(sounds,listener.lexicon()),speaker.getUUID(),body.getUUID(),listener.opinionOf(speaker.getUUID()).trust(),now));
    }
    static boolean imitate(ServerLevel level,Mob speaker,Voice.Kind target){
        var own=mind(speaker);if(own==null||speaker.isSilent()||!speaker.isAlive()||Villagers.LEARNING_MODE==LearningMode.FROZEN)return false;
        var state=state(speaker);long now=level.getGameTime();if(now<state.nextSpeech)return false;
        var attempt=own.voice().imitate(kind(speaker),target,now);if(attempt==null)return false;
        own.voice(attempt.practiced());state.nextSpeech=now+400;
        // A parroting attempt has no private semantic intention and supplies no grounded word-meaning evidence.
        say(level,speaker,own,attempt.sounds(),Set.of(),null,target);
        Speech.showForeign(level,speaker,own.name()+" (imitating "+target.name().toLowerCase(Locale.ROOT)+")",attempt.sounds(),Set.of());return true;
    }
    static void tick(ServerLevel level,Mob speaker){
        if(!(speaker instanceof Villager)&&!(speaker instanceof Pillager)||!speaker.isAlive()||speaker.isSilent()||speaker.isOnFire()||speaker.isInWater()||speaker.getTarget()!=null)return;
        var own=mind(speaker);if(own==null||own.foodLevel()<=10||own.fatigue()>=.65)return;
        if(speaker instanceof Villager v&&(v.isSleeping()||v.isTrading()))return;
        ensureLanguage(level,speaker,own);var p=state(speaker);long now=level.getGameTime();if(now<p.nextSpeech)return;
        var others=level.getEntitiesOfClass(Mob.class,speaker.getBoundingBox().inflate(4),m->m!=speaker&&m.isAlive()
                &&(m instanceof Villager&&Villagers.isManaged((Villager)m)||m instanceof Pillager&&CreatureAgents.managed(m))
                &&kind(m)!=kind(speaker)&&speaker.distanceToSqr(m)<=16&&speaker.hasLineOfSight(m)&&m.hasLineOfSight(speaker));
        if(!others.isEmpty()&&imitate(level,speaker,kind(others.getFirst())))return;
        if(!(speaker instanceof Pillager))return;
        p.nextSpeech=now+400;var visible=TutorLife.features(speaker.getMainHandItem());
        Concept noun=visible.stream().filter(c->Concept.forItem(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(speaker.getMainHandItem().getItem()).toString())==c).findFirst().orElse(Concept.HELLO);
        var clauses=List.of(new Clause(Concept.I,noun==Concept.HELLO?Concept.SAY:Concept.SEE,noun));var sentence=Sentence.speak(clauses,own.lexicon());if(sentence==null)return;
        if(noun==Concept.HELLO)visible=Set.of(Concept.HELLO);
        say(level,speaker,own,sentence.sounds(),visible,null,null);Speech.showForeign(level,speaker,own.name(),sentence.sounds(),visible);
    }
    static Map<String,Object> status(Mob body){var p=VOICES.get(body.getUUID());return p==null?Map.of():Map.of("played",p.played,"imitations",p.imitations,"lastWords",p.lastWords,"lastSample",p.lastSample,"remaining",p.frames.size(),"interrupted",p.interrupted);}
}
