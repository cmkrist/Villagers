package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.villagers.genetics.Trait;
import local.villagers.language.*;
import local.villagers.mind.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;

/** Acoustic bilingual grammar learned from public facts, with native production and an unexposed comparison. */
public final class ReceptionServerProbe implements ModInitializer {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final List<Concept> PRIOR=List.of(Concept.I,Concept.SEE,Concept.BREAD,Concept.IS,Concept.HURT,Concept.WANT,Concept.FOOD);
    private Villager foreign,injured,nativeSpeaker,learner,control;
    private int ticks,fullLessons,compactLessons,nativeLessons;
    private String foreignBiome;
    private boolean initiallyWrong;
    private final Set<UUID> reopened=new HashSet<>();
    private final List<Map<String,Object>> trace=new ArrayList<>();
    public void onInitialize(){
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            var root=level.getServer().getWorldPath(LevelResource.ROOT);var path=root.resolve("reception-result.json");
            if(!(entity instanceof Villager v)||!Files.exists(path))return;
            try{
                var expected=JsonParser.parseString(Files.readString(path)).getAsJsonObject().getAsJsonObject("minds").getAsJsonObject(v.getUUID().toString());if(expected==null)return;
                var actual=JsonParser.parseString(Files.readString(root.resolve("villagers").resolve(v.getUUID().toString()).resolve("mind.json"))).getAsJsonObject();
                for(var field:List.of("lexicon","beliefs","workOrders","partnerships"))check(expected.get(field).equals(actual.get(field)),field+" exact before restart decisions");
                check(v.isAlive()&&Villagers.isManaged(v),"original body alive");reopened.add(v.getUUID());
            }catch(Throwable t){fail(level.getServer(),t);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            try{
                ticks++;var root=server.getWorldPath(LevelResource.ROOT);
                if(ticks==1){server.overworld().setChunkForced(0,0,true);server.overworld().getChunk(0,0);server.tickRateManager().setTickRate(80);}
                if(ticks==40){
                    if(Files.exists(root.resolve("reception-result.json"))){
                        check(reopened.size()==5,"all five original minds reopen");var r=JsonParser.parseString(Files.readString(root.resolve("reception-result.json"))).getAsJsonObject();
                        learner=(Villager)server.overworld().getEntity(UUID.fromString(r.get("learner").getAsString()));foreign=(Villager)server.overworld().getEntity(UUID.fromString(r.get("foreign").getAsString()));
                        var l=session(learner);var f=session(foreign);var before=l.mind.lexicon();
                        var request=Sentence.speak(List.of(new Clause(Concept.I,Concept.WANT,Concept.FOOD)),f.mind.lexicon());
                        check(Dialogue.response(Sentence.interpret(request.sounds(),before))==Dialogue.Response.FOOD_REQUEST,"learned receptive grammar still supports a practical request");
                        check(before.grammar().preferred()==Grammar.Order.SVO,"native speaking order survives");
                        if(Villagers.LEARNING_MODE==local.villagers.learning.LearningMode.FROZEN){
                            Speech.clauses(server.overworld(),foreign,f,List.of(new Clause(Concept.I,Concept.SEE,Concept.BREAD)),true,learner,Set.of(Concept.BREAD));
                            check(before.equals(l.mind.lexicon()),"frozen hearing uses the known grammar without learning");
                            Files.writeString(root.resolve("reception-frozen-result.json"),"{\"status\":\"frozen-passed\",\"originalsVerifiedAtLoad\":5,\"knownReceptionUsable\":true,\"learnedLanguageUnchanged\":true}");
                        }else Files.writeString(root.resolve("reception-restart-result.json"),"{\"status\":\"restart-passed\",\"originalsVerifiedAtLoad\":5,\"knownReceptionUsable\":true}");
                        server.halt(false);return;
                    }
                    setup(server);
                }
                if(learner==null)return;var l=session(learner);var f=session(foreign);var i=session(injured);var n=session(nativeSpeaker);var c=session(control);long now=server.overworld().getGameTime();
                if(ticks%20==0){
                    if(f.dialogueBudget.take(now)){Speech.clauses(server.overworld(),foreign,f,List.of(new Clause(Concept.I,Concept.SEE,Concept.BREAD)),true,learner,Set.of(Concept.BREAD));fullLessons++;}
                    if(i.dialogueBudget.take(now)){Speech.clauses(server.overworld(),injured,i,List.of(new Clause(Concept.I,Concept.IS,Concept.HURT)),true,learner,Set.of(Concept.HURT));compactLessons++;}
                    if(n.dialogueBudget.take(now)){Speech.clauses(server.overworld(),nativeSpeaker,n,List.of(new Clause(Concept.I,Concept.SEE,Concept.BREAD)),true,learner,Set.of(Concept.BREAD));nativeLessons++;}
                }
                var full=Sentence.speak(List.of(new Clause(Concept.I,Concept.SEE,Concept.BREAD)),f.mind.lexicon());
                var compact=Sentence.speak(List.of(new Clause(Concept.I,Concept.IS,Concept.HURT)),i.mind.lexicon());
                var request=Sentence.speak(List.of(new Clause(Concept.I,Concept.WANT,Concept.FOOD)),f.mind.lexicon());
                var nativeExample=Sentence.speak(List.of(new Clause(Concept.I,Concept.SEE,Concept.BREAD)),n.mind.lexicon());
                boolean receptive=full.correct(Sentence.interpret(full.sounds(),l.mind.lexicon()))&&compact.correct(Sentence.interpret(compact.sounds(),l.mind.lexicon()))
                        &&nativeExample.correct(Sentence.interpret(nativeExample.sounds(),l.mind.lexicon()));
                if(ticks%100==0){trace.add(Map.of("tick",ticks,"fullLessons",fullLessons,"compactLessons",compactLessons,"preferred",l.mind.lexicon().grammar().preferred(),"profiles",l.mind.lexicon().grammar().receptive(),"correctReception",receptive));Files.writeString(root.resolve("reception-progress.json"),JSON.toJson(trace));}
                if(receptive&&fullLessons>=3&&compactLessons>=3&&nativeLessons>=3){
                    check(initiallyWrong,"word familiarity alone did not supply foreign grammar");
                    check(l.mind.lexicon().grammar().preferred()==Grammar.Order.SVO&&l.mind.lexicon().grammar().evidence().equals(Grammar.founding(Grammar.Order.SVO).evidence()),"foreign hearing did not replace native production evidence");
                    var own=Sentence.speak(List.of(new Clause(Concept.I,Concept.IS,Concept.HURT)),l.mind.lexicon());
                    check(own.sounds().size()==3&&own.correct(Sentence.interpret(own.sounds(),l.mind.lexicon())),"foreign omission was understood without adopting it");
                    check(c.mind.lexicon().grammar().receptive().dialects().isEmpty(),"wall control received no acoustic event evidence");
                    check(Dialogue.response(Sentence.interpret(request.sounds(),c.mind.lexicon()))!=Dialogue.Response.FOOD_REQUEST,"unexposed control still cannot resolve the role reversal");
                    check(Speech.requestFood(server.overworld(),foreign,f,learner),"actual foreign request sent through the production dialogue");
                    check(l.foodSharedGiven==1&&f.foodSharedReceived==1,"newly understood request changed a real permitted decision");
                    check(learner.getInventory().countItem(Items.BREAD)==2&&foreign.getInventory().countItem(Items.BREAD)==1,"one real bread conserved at contact");
                    var r=new LinkedHashMap<String,Object>();r.put("status","passed");r.put("ticks",ticks);r.put("learner",learner.getUUID());r.put("foreign",foreign.getUUID());r.put("foreignBiome",foreignBiome);r.put("initiallyWrong",initiallyWrong);r.put("fullLessons",fullLessons);r.put("compactLessons",compactLessons);r.put("nativeLessons",nativeLessons);r.put("receptiveProfiles",l.mind.lexicon().grammar().receptive());r.put("nativeOrder",l.mind.lexicon().grammar().preferred());r.put("unexposedControlBlocked",true);r.put("actualFoodReceipt",f.lastFoodReceived);r.put("fullForeignExample",full);r.put("compactForeignExample",compact);r.put("retainedNativeExample",own);r.put("trace",trace);r.put("fixture",fixture());
                    var minds=new LinkedHashMap<String,Object>();for(var body:List.of(foreign,injured,nativeSpeaker,learner,control)){check(body.isAlive(),"all originals survive");minds.put(body.getUUID().toString(),session(body).mind.snapshot());}r.put("minds",minds);
                    Files.writeString(root.resolve("reception-result.json"),JSON.toJson(r));System.out.println("RECEPTION PASS at "+ticks);server.halt(false);
                }
                if(ticks>4000)throw new AssertionError("Receptive grammar incomplete: "+l.mind.lexicon().grammar());
            }catch(Throwable t){fail(server,t);}
        });
    }
    private Session session(Villager v){return Villagers.session(v,(net.minecraft.server.level.ServerLevel)v.level());}
    private Lexicon prior(Lexicon nativeLanguage,Lexicon other){var entries=new ArrayList<>(nativeLanguage.entries());for(var concept:PRIOR)entries.add(new Lexicon.Entry(concept,other.wordFor(concept),.4));return new Lexicon(entries,List.of(),false,nativeLanguage.grammar());}
    private AgentMind mind(UUID id,String biome,Lexicon language,int food){var m=new AgentMind(id);m.establishOrigin(biome);m.lexicon(language);var json=JSON.toJsonTree(m.snapshot()).getAsJsonObject();json.getAsJsonObject("food").addProperty("foodLevel",food);json.getAsJsonObject("food").addProperty("saturation",food);return AgentMind.fromSnapshot(JSON.fromJson(json,AgentMind.Snapshot.class));}
    private Villager body(MinecraftServer server,AgentMind mind,double x,double z){Villagers.registerNewborn(mind,server.overworld().getGameTime());var v=new Villager(EntityTypes.VILLAGER,server.overworld());v.setUUID(mind.id());v.snapTo(x,81,z,0,0);v.setNoAi(true);server.overworld().addFreshEntity(v);Villagers.adopt(server.overworld(),v);return v;}
    private void setup(MinecraftServer server){
        for(int x=1;x<=14;x++)for(int z=1;z<=14;z++)for(int y=80;y<=84;y++)server.overworld().setBlock(new BlockPos(x,y,z),y==80||y<=83&&(x==1||x==10||x==14||z==1||z==14)?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        var nativeLanguage=BiomeDialect.foundingLanguage("minecraft:plains");Lexicon other=null;
        for(var biome:List.of("minecraft:snowy_slopes","minecraft:frozen_peaks","minecraft:jagged_peaks","minecraft:snowy_plains","minecraft:snowy_taiga","minecraft:frozen_river","minecraft:deep_frozen_ocean","minecraft:frozen_ocean","minecraft:stony_peaks","minecraft:snowy_beach")){
            var candidate=BiomeDialect.foundingLanguage(biome);if(BiomeDialect.order(biome)==Grammar.Order.OVS&&PRIOR.stream().allMatch(c->nativeLanguage.meaning(candidate.wordFor(c))==null)){foreignBiome=biome;other=candidate;break;}
        }check(other!=null,"a declared founding dialect with distinct known lexical cues exists");
        foreign=body(server,mind(new UUID(1001,1),foreignBiome,other,8),4.5,5.5);foreign.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BREAD,1));
        var compressed=other.withGrammar(other.grammar().learn(new Construction(List.of(Grammar.Role.OBJECT,Grammar.Role.SUBJECT),Concept.IS,7)));
        injured=body(server,mind(new UUID(1001,2),foreignBiome,compressed,20),4.5,7.5);check(injured.hurtServer(server.overworld(),injured.damageSources().generic(),1),"initial native injury supplies a public state");
        AgentMind generous=null;for(int id=1;id<10000;id++){var m=mind(new UUID(1002,id),"minecraft:plains",prior(nativeLanguage,other),20);if(m.has(Trait.GENEROUS)&&!m.has(Trait.GREEDY)){generous=m;break;}}check(generous!=null,"generous listener fixture found");
        learner=body(server,generous,5.5,5.5);learner.getInventory().addItem(new ItemStack(Items.BREAD,3));
        nativeSpeaker=body(server,mind(new UUID(1003,1),"minecraft:plains",nativeLanguage,20),6.5,5.5);nativeSpeaker.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BREAD,1));
        control=body(server,mind(new UUID(1004,1),"minecraft:plains",prior(nativeLanguage,other),20),12.5,5.5);
        var raw=Sentence.speak(List.of(new Clause(Concept.I,Concept.WANT,Concept.FOOD)),other);
        initiallyWrong=Dialogue.response(Sentence.interpret(raw.sounds(),session(learner).mind.lexicon()))!=Dialogue.Response.FOOD_REQUEST;
        check(initiallyWrong,"prior vocabulary and own SVO do not infer the speaker's OVS intention");
    }
    private static String fixture(){return "NEW peaceful platform with a separate walled comparison chamber. Five original stationary NoAI adults. The listener/control start with native plains SVO and declared prior acquaintance with seven foreign word meanings, but no foreign receptive grammar; those lexical acquisitions are not claimed here. A real existing snowy/frozen/peaks founding vocabulary with distinct cues is selected and reported. A healthy foreign speaker visibly holds bread; a separate same-biome speaker starts with one native injury and a seeded compact OS copula convention; a native peer also visibly holds bread. Controlled public utterance opportunities obey each speaker's 200-tick dialogue budget. The listener hears full OVS, compact OS and native SVO from actual sounds plus public speaker-specific facts. The wall control has the same lexical priors but no visibility/exposure. No post-setup position, health, vocabulary, grammar or inventory setters; one actual interpreted food request causes a conserved production-code handoff. No existing society opened; no natural bilingual emergence, new semantic invention, visual UX or scaling claim.";}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void fail(MinecraftServer server,Throwable t){t.printStackTrace();try{Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("reception-failure.txt"),t.toString());}catch(Exception ignored){}server.halt(false);}
}
