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
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;

/** Native villager/pillager sample playback, independently acquired words and reciprocal fallible echo practice. */
public final class VoiceServerProbe implements ModInitializer {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final UUID V=new UUID(1201,1),P=new UUID(1201,2);
    private int ticks,examples,echoedAt=-1;private final Set<UUID> reopened=new HashSet<>();private String pillagerOrigin;
    public void onInitialize(){
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            var root=level.getServer().getWorldPath(LevelResource.ROOT);var file=root.resolve("voice-result.json");
            if(!(entity instanceof Mob body)||!Files.exists(file)||!List.of(V,P).contains(body.getUUID()))return;
            try{var baseline=root.resolve("voice-restart-baseline.json");var expected=JsonParser.parseString(Files.readString(Files.exists(baseline)?baseline:file)).getAsJsonObject().getAsJsonObject("minds").getAsJsonObject(body.getUUID().toString());
                var path=root.resolve(body instanceof Villager?"villagers":"creature-minds").resolve(body.getUUID().toString()).resolve("mind.json");
                var saved=JsonParser.parseString(Files.readString(path)).getAsJsonObject();for(var field:List.of("voice","lexicon","lessons","beliefs","workOrders","partnerships"))check(expected.get(field).equals(saved.get(field)),field+" survives exact original-body load");
                check(body.isAlive(),"original alive");reopened.add(body.getUUID());
            }catch(Throwable t){fail(level.getServer(),t);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{try{
            ticks++;var level=server.overworld();var root=server.getWorldPath(LevelResource.ROOT);
            if(ticks==1){level.setChunkForced(0,0,true);level.getChunk(0,0);server.tickRateManager().setTickRate(80);}
            if(ticks==40){
                if(Files.exists(root.resolve("voice-result.json"))){
                    check(reopened.size()==2,"both original minds verified");var villager=(Villager)level.getEntity(V);var pillager=(Pillager)level.getEntity(P);
                    Villagers.session(villager,level);CreatureAgents.status(pillager);var vm=VocalSpeech.mind(villager);var pm=VocalSpeech.mind(pillager);var oldV=vm.snapshot();var oldP=pm.snapshot();
                    check(vm.voice().model(Voice.Kind.PILLAGER).practice()==(Villagers.LEARNING_MODE==LearningMode.FROZEN?2:1)&&pm.voice().model(Voice.Kind.VILLAGER).practice()==1,"incomplete motor practice persisted");
                    if(Villagers.LEARNING_MODE==LearningMode.FROZEN){
                        check(!VocalSpeech.imitate(level,villager,Voice.Kind.PILLAGER)&&!VocalSpeech.imitate(level,pillager,Voice.Kind.VILLAGER),"frozen new imitation practice disabled");
                        var words=Sentence.speak(Tuition.demonstrate(Concept.BREAD),pm.lexicon());VocalSpeech.say(level,pillager,pm,words.sounds(),Set.of(Concept.BREAD),null,null);
                        check(vm.voice().equals(oldV.voice())&&vm.lexicon().equals(oldV.lexicon())&&pm.voice().equals(oldP.voice())&&pm.lexicon().equals(oldP.lexicon()),"frozen known language and sound memory unchanged");
                    }else{
                        check(VocalSpeech.imitate(level,villager,Voice.Kind.PILLAGER),"saved foreign sound exemplar can be practiced again");check(vm.voice().model(Voice.Kind.PILLAGER).practice()==2,"practice resumes incrementally");
                    }
                    if(Villagers.LEARNING_MODE!=LearningMode.FROZEN)Files.writeString(root.resolve("voice-restart-baseline.json"),JSON.toJson(Map.of("minds",Map.of(V.toString(),vm.snapshot(),P.toString(),pm.snapshot()))));
                    var phase=Villagers.LEARNING_MODE==LearningMode.FROZEN?"frozen":"restart";
                    Files.writeString(root.resolve("voice-"+phase+"-result.json"),JSON.toJson(Map.of("status",phase+"-passed","originalsVerifiedAtLoad",2)));server.halt(false);return;
                }
                setup(server);
            }
            if(ticks<40)return;var villager=(Villager)level.getEntity(V);var pillager=(Pillager)level.getEntity(P);var vs=Villagers.existingSession(V);var pm=VocalSpeech.mind(pillager);
            if(ticks%200==40&&examples<4){
                check(vs.dialogueBudget.take(level.getGameTime()),"villager demonstration attention budget");
                Speech.clauses(level,villager,vs,Tuition.demonstrate(Concept.BREAD),true,null,Set.of(Concept.BREAD));
                var words=Sentence.speak(Tuition.demonstrate(Concept.BREAD),pm.lexicon());VocalSpeech.say(level,pillager,pm,words.sounds(),Set.of(Concept.BREAD),null,null);examples++;
            }
            if(examples==4&&echoedAt<0){
                check(vs.mind.lexicon().meaning(pm.lexicon().wordFor(Concept.BREAD))==Concept.BREAD,"villager learned pillager bread through public repeated examples");
                check(pm.lexicon().meaning(vs.mind.lexicon().wordFor(Concept.BREAD))==Concept.BREAD,"pillager learned villager bread through public repeated examples");
                var beforeV=vs.mind.lexicon();var beforeP=pm.lexicon();var expectedV=vs.mind.voice().imitate(Voice.Kind.VILLAGER,Voice.Kind.PILLAGER,level.getGameTime()).sounds();
                check(VocalSpeech.imitate(level,villager,Voice.Kind.PILLAGER),"villager attempts observed pillager sounds");check(VocalSpeech.status(villager).get("lastWords").equals(expectedV),"actual villager emitted articulation errors enter the wire");
                var expectedP=pm.voice().imitate(Voice.Kind.PILLAGER,Voice.Kind.VILLAGER,level.getGameTime()).sounds();
                check(VocalSpeech.imitate(level,pillager,Voice.Kind.VILLAGER),"pillager attempts observed villager sounds");check(VocalSpeech.status(pillager).get("lastWords").equals(expectedP),"actual pillager emitted articulation errors enter the wire");
                check(beforeV.equals(vs.mind.lexicon())&&beforeP.equals(pm.lexicon()),"ungrounded parroting grants no semantics");echoedAt=ticks;
            }
            if(echoedAt>=0&&ticks>=echoedAt+80){
                check((int)VocalSpeech.status(villager).get("played")>=3&&(int)VocalSpeech.status(pillager).get("played")>=3,"native default sound playback executed for both species");
                check(VocalSpeech.status(villager).get("lastSample").equals("minecraft:entity.villager.ambient")&&VocalSpeech.status(pillager).get("lastSample").equals("minecraft:entity.pillager.ambient"),"early attempts retain each actual body voice");
                check(villager.getMainHandItem().is(Items.BREAD)&&villager.getMainHandItem().getCount()==1&&pillager.getMainHandItem().is(Items.BREAD)&&pillager.getMainHandItem().getCount()==1,"visible food unchanged by talking or imitation");
                var r=new LinkedHashMap<String,Object>();r.put("status","passed");r.put("ticks",ticks);r.put("examples",examples);r.put("pillagerOrigin",pillagerOrigin);r.put("vocalPlayback",Map.of("villager",VocalSpeech.status(villager),"pillager",VocalSpeech.status(pillager)));r.put("crossSpeciesBreadAcquired",true);r.put("reciprocalImitationWithoutSemanticGrant",true);r.put("minds",Map.of(V.toString(),vs.mind.snapshot(),P.toString(),pm.snapshot()));
                r.put("fixture","NEW normal-difficulty isolated platform; two original stationary NoAI adults, one villager and one pillager, each actually holding one bread. Separate species founding languages with two explicitly seeded foreign I/SEE lexical priors and native SVO; acquisition of those core words is not claimed. Four controlled public demonstrations learn the foreign bread label. One reciprocal sound-only imitation per speaker uses actual previously heard words and authored motor substitutions; no intention or vocabulary is copied. Both native default sample playback methods must execute. No semantic/resource grant from parroting. No existing population opened; no client auditory assessment, waveform recognition or natural cross-species diplomacy claimed.");
                Files.writeString(root.resolve("voice-result.json"),JSON.toJson(r));server.halt(false);return;
            }
            if(ticks>2000)throw new AssertionError("voice fixture timeout");
        }catch(Throwable t){fail(server,t);}});
    }
    private static AgentMind mind(UUID id,String origin,Lexicon language){var m=new AgentMind(id);m.establishOrigin(origin);m.lexicon(language);return m;}
    private void setup(MinecraftServer s){
        for(int x=1;x<=9;x++)for(int z=1;z<=9;z++)for(int y=80;y<=84;y++)s.overworld().setBlock(new BlockPos(x,y,z),y==80||y<=83&&(x==1||x==9||z==1||z==9)?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        var vl=VocalSpeech.founding(Voice.Kind.VILLAGER,"minecraft:plains");Lexicon pl=null;
        for(var biome:List.of("minecraft:plains","minecraft:beach","minecraft:river","minecraft:ocean","minecraft:meadow")){
            var candidate=VocalSpeech.founding(Voice.Kind.PILLAGER,biome);
            if(List.of(Concept.I,Concept.SEE,Concept.BREAD).stream().allMatch(c->vl.meaning(candidate.wordFor(c))==null&&candidate.meaning(vl.wordFor(c))==null)){pl=candidate;pillagerOrigin=biome;break;}
        }check(pl!=null,"declared distinct actual founding vocabulary exists");
        var pPrior=pl.reinforce(Concept.I,vl.wordFor(Concept.I),.4).reinforce(Concept.SEE,vl.wordFor(Concept.SEE),.4);
        var vPrior=vl.reinforce(Concept.I,pl.wordFor(Concept.I),.4).reinforce(Concept.SEE,pl.wordFor(Concept.SEE),.4);
        var villager=new Villager(EntityTypes.VILLAGER,s.overworld());villager.setUUID(V);villager.snapTo(4.5,81,4.5,0,0);villager.setNoAi(true);villager.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BREAD));Villagers.registerNewborn(mind(V,"minecraft:plains",vPrior),s.overworld().getGameTime());s.overworld().addFreshEntity(villager);Villagers.adopt(s.overworld(),villager);
        var pillager=new Pillager(EntityTypes.PILLAGER,s.overworld());pillager.setUUID(P);pillager.snapTo(5.5,81,4.5,0,0);pillager.setNoAi(true);pillager.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BREAD));CreatureAgents.registerChild(s.overworld(),pillager,mind(P,pillagerOrigin,pPrior));s.overworld().addFreshEntity(pillager);
    }
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static void fail(MinecraftServer s,Throwable t){t.printStackTrace();try{Files.writeString(s.getWorldPath(LevelResource.ROOT).resolve("voice-failure.txt"),t.toString());}catch(Exception ignored){}s.halt(false);}
}
