package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.villagers.language.*;
import local.villagers.learning.LearningMode;
import local.villagers.mind.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;

/** Isolated, declared teaching opportunities plus an independently running ordinary controller pair. */
public final class TeachingServerProbe implements ModInitializer {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final UUID TEACHER=new UUID(1101,1),CHILD=new UUID(1101,2),CONTROL=new UUID(1101,3),AUTO_TEACHER=new UUID(1101,4),AUTO_CHILD=new UUID(1101,5);
    private static final UUID ADULT_TEACHER=new UUID(1101,6),ADULT_LEARNER=new UUID(1101,7);
    private static final List<UUID> IDS=List.of(TEACHER,CHILD,CONTROL,AUTO_TEACHER,AUTO_CHILD,ADULT_TEACHER,ADULT_LEARNER);
    private final Set<UUID> reopened=new HashSet<>();
    private final List<Map<String,Object>> trace=new ArrayList<>();
    private int ticks;private boolean resumed,done;
    public void onInitialize(){
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            var root=level.getServer().getWorldPath(LevelResource.ROOT);var file=root.resolve(Files.exists(root.resolve("teaching-result.json"))?"teaching-result.json":"teaching-partial.json");
            if(!(entity instanceof Villager v)||!Files.exists(file)||!IDS.contains(v.getUUID()))return;
            try{var expected=JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonObject("minds").getAsJsonObject(v.getUUID().toString());
                var actual=JsonParser.parseString(Files.readString(root.resolve("villagers").resolve(v.getUUID().toString()).resolve("mind.json"))).getAsJsonObject();
                for(var field:List.of("lexicon","lessons","beliefs","workOrders","partnerships"))check(expected.get(field).equals(actual.get(field)),field+" exact before restarted decisions");
                check(v.isAlive()&&Villagers.isManaged(v),"original alive");reopened.add(v.getUUID());
            }catch(Throwable t){fail(level.getServer(),t);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{try{
            if(done)return;ticks++;var level=server.overworld();var root=server.getWorldPath(LevelResource.ROOT);
            if(ticks==1){level.setChunkForced(0,0,true);level.getChunk(0,0);server.tickRateManager().setTickRate(200);}
            if(ticks==40){
                if(Files.exists(root.resolve("teaching-result.json"))){
                    check(reopened.size()==7,"all five originals verified");var c=session(server,CHILD);var old=c.mind.lexicon();
                    var teacher=body(server,TEACHER);var child=body(server,CHILD);var t=session(server,TEACHER);
                    var question=Sentence.speak(Tuition.question(Concept.STONE_SWORD),t.mind.lexicon());
                    check(Tuition.answer(question.sounds(),old,List.of(new Tuition.Seen(teacher.getUUID(),Set.of(Concept.STONE,Concept.SWORD,Concept.STONE_SWORD))))!=null,"novel compound remains usable");
                    if(Villagers.LEARNING_MODE==LearningMode.FROZEN){
                        check(TutorLife.work(level,teacher,t,level.getGameTime())==null,"frozen deliberate learning disabled");
                        Speech.lesson(level,teacher,t,child,Tuition.demonstrate(Concept.STONE_SWORD),teacher,TutorLife.features(teacher.getMainHandItem()),true);
                        check(old.equals(c.mind.lexicon()),"frozen public hearing leaves language unchanged");
                    }
                    var phase=Villagers.LEARNING_MODE==LearningMode.FROZEN?"frozen":"restart";
                    Files.writeString(root.resolve("teaching-"+phase+"-result.json"),JSON.toJson(Map.of("status",phase+"-passed","originalsVerifiedAtLoad",7,"novelMeaningUsable",true)));done=true;server.halt(false);return;
                }
                if(Files.exists(root.resolve("teaching-partial.json"))){check(reopened.size()==7,"all originals reopened with partial evidence");resumed=true;
                    var c=session(server,CHILD);check(c.mind.lexicon().formation().preferred()==null,"partial rule is still unusable at restart");check(c.mind.lexicon().formation().patterns().getFirst().evidence()==2,"two observations persisted");
                }else setup(server);
            }
            if(ticks<40)return;var teacher=body(server,TEACHER);var child=body(server,CHILD);var t=session(server,TEACHER);var c=session(server,CHILD);
            if(ticks%10==0){
                var adult=body(server,ADULT_LEARNER);var adultTeacher=body(server,ADULT_TEACHER);
                TutorLife.work(level,adult,session(server,ADULT_LEARNER),level.getGameTime());
                TutorLife.work(level,adultTeacher,session(server,ADULT_TEACHER),level.getGameTime());
                if(c.mind.lexicon().formation().preferred()==null)TutorLife.work(level,teacher,t,level.getGameTime());
            }
            if(ticks%200==0){trace.add(Map.of("tick",ticks,"resumed",resumed,"topic",t.tuitionTopic==null?"between lessons":t.tuitionTopic,"history",t.mind.lessons(),"formation",c.mind.lexicon().formation(),"knownRoots",List.of(Concept.WOOD,Concept.STONE,Concept.HOE,Concept.SWORD).stream().filter(x->c.mind.lexicon().wordFor(x)!=null).toList()));Files.writeString(root.resolve("teaching-progress.json"),JSON.toJson(trace));}
            var formation=c.mind.lexicon().formation();
            if(!resumed&&formation.preferred()==null&&!formation.patterns().isEmpty()&&formation.patterns().getFirst().evidence()==2){
                check(c.mind.lexicon().wordFor(Concept.STONE_SWORD)==null,"not enough evidence to generalize");
                var report=report(server,"partial-passed");report.put("partialRuleEvidence",2);Files.writeString(root.resolve("teaching-partial.json"),JSON.toJson(report));done=true;server.halt(false);return;
            }
            if(resumed&&formation.preferred()!=null){
                if(!t.tuitionBudget.take(level.getGameTime()))return;
                String novel=t.mind.lexicon().wordFor(Concept.STONE_SWORD);
                check(c.mind.lexicon().entries().stream().noneMatch(e->e.concept()==Concept.STONE_SWORD),"held-out meaning has no lexical entry");
                check(c.mind.lexicon().evidence().stream().noneMatch(e->e.word().equals(novel)),"held-out form was never heard");
                check(c.mind.lexicon().meaning(novel)==Concept.STONE_SWORD,"held-out form independently understood");
                check(c.mind.lexicon().wordFor(Concept.STONE_SWORD).equals(novel),"held-out form independently produced");
                check(t.mind.lessons().entries().stream().filter(e->WordFormation.root(e.topic())&&e.confirmed()>0).count()==4,"all four roots were publicly confirmed");
                var a=session(server,AUTO_TEACHER);check(a.tuitionCorrect>0||!a.mind.lessons().entries().isEmpty()&&a.mind.lessons().entries().stream().anyMatch(e->e.confirmed()>0),"ordinary AI controller completed a childhood lesson");
                check(session(server,AUTO_CHILD).mind.lexicon().meaning(a.mind.lexicon().wordFor(Concept.BREAD))==Concept.BREAD,"ordinary child learned bread");
                var adult=session(server,ADULT_LEARNER);var adultTeacher=session(server,ADULT_TEACHER);
                check(!body(server,ADULT_LEARNER).isBaby()&&!adult.mind.lexicon().developing(),"adult retained adult status throughout learning");
                check(adultTeacher.mind.lessons().confirmed(ADULT_LEARNER,Concept.HOE),"adult requested and completed deliberate teaching");
                check(adult.mind.lexicon().meaning(adultTeacher.mind.lexicon().wordFor(Concept.HOE))==Concept.HOE,"adult independently acquired missing vocabulary");
                var control=session(server,CONTROL);check(control.mind.lexicon().formation().patterns().isEmpty()&&WordFormation.derived().stream().allMatch(x->control.mind.lexicon().wordFor(x)==null),"walled unexposed control learned nothing");
                check(c.mind.lexicon().meaning(t.mind.lexicon().wordFor(Concept.EMERALD))==null,"unexposed unrelated vocabulary was not granted");
                check(child.isBaby()&&c.mind.partnerships().history().isEmpty()&&c.mind.workOrders().history().isEmpty(),"learning granted no adult status or agreements");
                for(int slot=0;slot<teacher.getInventory().getContainerSize();slot++)if(teacher.getInventory().getItem(slot).is(Items.STONE_SWORD)){
                    check(AdventureActions.displayForLesson(level,teacher,slot),"actual held-out sword displayed through permitted conserved swap");break;}
                check(teacher.getMainHandItem().is(Items.STONE_SWORD),"held-out target actually visible");
                var request=Speech.lesson(level,teacher,t,child,Tuition.question(Concept.STONE_SWORD),teacher,TutorLife.features(teacher.getMainHandItem()),false);
                var seen=new Tuition.Seen(teacher.getUUID(),TutorLife.features(teacher.getMainHandItem()));
                var reply=Tuition.answer(request.sounds(),c.mind.lexicon(),List.of(seen));
                check(reply!=null&&reply.pointedAt().equals(teacher.getUUID()),"child answers novel request from its own decoding");
                check(c.tuitionBudget.take(level.getGameTime()),"child attention budget permits response");
                var spoken=Speech.lesson(level,child,c,teacher,reply.words(),teacher,TutorLife.features(teacher.getMainHandItem()),false);
                check(spoken!=null&&Tuition.confirms(Concept.STONE_SWORD,Sentence.interpret(spoken.sounds(),t.mind.lexicon()),seen),"teacher independently hears correct novel reply and public point");
                var r=report(server,"passed");r.put("novelWord",novel);r.put("neverHeardOrStoredNovelWord",true);r.put("ordinaryControllerLessonConfirmed",true);r.put("adultRequestedAndLearned",true);r.put("partialLearningResumed",true);r.put("trace",trace);Files.writeString(root.resolve("teaching-result.json"),JSON.toJson(r));done=true;server.halt(false);return;
            }
            if(ticks>18000)throw new AssertionError("Teaching incomplete: "+JSON.toJson(trace.isEmpty()?Map.of():trace.getLast()));
        }catch(Throwable t){fail(server,t);}});
    }
    private static Session session(MinecraftServer s,UUID id){return Villagers.session(body(s,id),s.overworld());}
    private static Villager body(MinecraftServer s,UUID id){var v=s.overworld().getEntity(id);if(!(v instanceof Villager body))throw new AssertionError("Original missing "+id);return body;}
    private static AgentMind mind(UUID id,Lexicon language){var m=new AgentMind(id);m.establishOrigin("minecraft:plains");m.lexicon(language);var j=JSON.toJsonTree(m.snapshot()).getAsJsonObject();j.getAsJsonObject("food").addProperty("foodLevel",20);j.getAsJsonObject("food").addProperty("saturation",20);return AgentMind.fromSnapshot(JSON.fromJson(j,AgentMind.Snapshot.class));}
    private static Villager spawn(MinecraftServer s,UUID id,Lexicon language,int x,int z,boolean child,boolean ordinary){
        Villagers.registerNewborn(mind(id,language),s.overworld().getGameTime());var v=new Villager(EntityTypes.VILLAGER,s.overworld());v.setUUID(id);v.snapTo(x+.5,81,z+.5,0,0);v.setNoAi(!ordinary);if(child)v.setAge(-24000);s.overworld().addFreshEntity(v);Villagers.adopt(s.overworld(),v);return v;
    }
    private void setup(MinecraftServer s){
        for(int x=1;x<=14;x++)for(int z=1;z<=14;z++)for(int y=80;y<=84;y++)s.overworld().setBlock(new BlockPos(x,y,z),y==80||y<=83&&(x==1||x==7||x==10||x==14||z==1||z==7||z==10||z==14)?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        var parent=BiomeDialect.foundingLanguage("minecraft:plains");var clear=new Random(123){@Override public double nextDouble(){return .99;}};
        var child=Lexicon.child(parent,parent,clear);check(child.formation().patterns().isEmpty()&&child.wordFor(Concept.HOE)==null,"sparse child has no inherited rule or tools");
        var teacher=spawn(s,TEACHER,parent,4,4,false,false);spawn(s,CHILD,child,5,4,true,false);spawn(s,CONTROL,child,12,12,true,false);
        for(var item:List.of(Items.OAK_LOG,Items.COBBLESTONE,Items.WOODEN_HOE,Items.STONE_HOE,Items.WOODEN_SWORD,Items.STONE_SWORD))teacher.getInventory().addItem(new ItemStack(item));
        var a=spawn(s,AUTO_TEACHER,parent,12,4,false,true);a.getInventory().addItem(new ItemStack(Items.BREAD,3));spawn(s,AUTO_CHILD,child,12,5,true,true);
        var adultTeacher=spawn(s,ADULT_TEACHER,parent,4,12,false,false);
        adultTeacher.getInventory().addItem(new ItemStack(Items.WOODEN_HOE));adultTeacher.getInventory().addItem(new ItemStack(Items.STONE_HOE));
        var sparseAdult=new Lexicon(parent.entries().stream().filter(e->e.concept()!=Concept.HOE).toList(),List.of(),false,parent.grammar());
        var adult=spawn(s,ADULT_LEARNER,sparseAdult,5,12,false,false);
        check(session(s,ADULT_LEARNER).mind.lexicon().meaning(parent.wordFor(Concept.HOE))==null,"adult starts without hoe vocabulary");
        check(AdventureActions.displayForLesson(s.overworld(),adultTeacher,0),"adult teacher shows a real hoe");
        Speech.lesson(s.overworld(),adultTeacher,session(s,ADULT_TEACHER),adult,Tuition.demonstrate(Concept.HOE),adultTeacher,TutorLife.features(adultTeacher.getMainHandItem()),true);
        check(session(s,ADULT_LEARNER).tuitionHelpFrom.equals(ADULT_TEACHER),"adult noticed its own decoded gap");
    }
    private Map<String,Object> report(MinecraftServer s,String status){
        var r=new LinkedHashMap<String,Object>();r.put("status",status);r.put("ticks",ticks);r.put("minds",snapshots(s));
        var teacher=body(s,TEACHER);var inventory=new TreeMap<String,Integer>();for(int i=0;i<teacher.getInventory().getContainerSize();i++){var item=teacher.getInventory().getItem(i);if(!item.isEmpty())inventory.merge(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).toString(),item.getCount(),Integer::sum);}var held=teacher.getMainHandItem();if(!held.isEmpty())inventory.merge(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem()).toString(),held.getCount(),Integer::sum);
        check(inventory.size()==6&&inventory.values().stream().allMatch(n->n==1),"all six actual examples conserved across display swaps");r.put("conservedTeacherExamples",inventory);
        r.put("fixture","NEW isolated peaceful platform; seven original bodies. Separate adult teacher/learner starts with a declared missing HOE word; a public demonstration triggers the learner own comprehension-gap repair request, then real teaching must confirm its answer. Controlled main NoAI teacher/baby opportunities call the production TutorLife adapter every ten ticks with its 200-tick budget; only real possessed log, cobble and four tools are shown. Child/control start with actual FIRST_WORDS from the founding plains dialect, no roots or formation. Main child is kept physically stationary by declared NoAI, not by ongoing setters. Walled unexposed child gets no lesson. Separate AI-enabled adult/baby pair runs the ordinary controller and learns bread autonomously. No post-setup body/lexicon/grammar/inventory setters. Two-observation rule checkpoint stops before mastery; resumed native run must generalize a held-out stone-sword form. Fixed authored material/tool categories, controlled attention, no natural cultural-emergence or scaling claim.");return r;
    }
    private Map<String,Object> snapshots(MinecraftServer s){var map=new LinkedHashMap<String,Object>();for(var id:IDS){check(body(s,id).isAlive(),"all originals survive");map.put(id.toString(),session(s,id).mind.snapshot());}return map;}
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static void fail(MinecraftServer s,Throwable t){t.printStackTrace();try{Files.writeString(s.getWorldPath(LevelResource.ROOT).resolve("teaching-failure.txt"),t.toString());}catch(Exception ignored){}s.halt(false);}
}
