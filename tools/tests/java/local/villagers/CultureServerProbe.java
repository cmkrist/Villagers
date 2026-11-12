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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;

/** Supported first use and visible contact-chain diffusion in a NEW controlled native world. */
public final class CultureServerProbe implements ModInitializer {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final UUID SOURCE=new UUID(1301,1),BRIDGE=new UUID(1301,2),TARGET=new UUID(1301,3),CONTROL=new UUID(1301,4);
    private static final List<UUID> IDS=List.of(SOURCE,BRIDGE,TARGET,CONTROL);
    private static final Clause FACT=new Clause(Concept.I,Concept.IS,Concept.HURT),NOVEL=new Clause(Concept.I,Concept.IS,Concept.WALK);
    private int ticks,innovations,relays;private final Set<UUID> reopened=new HashSet<>();private final List<Map<String,Object>> trace=new ArrayList<>();
    public void onInitialize(){
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            var root=level.getServer().getWorldPath(LevelResource.ROOT);var file=root.resolve("culture-result.json");if(!(entity instanceof Villager v)||!Files.exists(file)||!IDS.contains(v.getUUID()))return;
            try{var expected=JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonObject("minds").getAsJsonObject(v.getUUID().toString());var actual=JsonParser.parseString(Files.readString(root.resolve("villagers").resolve(v.getUUID().toString()).resolve("mind.json"))).getAsJsonObject();
                for(var field:List.of("lexicon","voice","lessons","beliefs","partnerships","workOrders"))check(expected.get(field).equals(actual.get(field)),field+" exact before restart decisions");check(v.isAlive(),"original alive");reopened.add(v.getUUID());
            }catch(Throwable t){fail(level.getServer(),t);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{try{
            ticks++;var level=server.overworld();var root=server.getWorldPath(LevelResource.ROOT);
            if(ticks==1){level.setChunkForced(0,0,true);level.getChunk(0,0);server.tickRateManager().setTickRate(80);}
            if(ticks==40){
                if(Files.exists(root.resolve("culture-result.json"))){
                    check(reopened.size()==4,"all original minds reopen");var target=session(server,TARGET);var control=session(server,CONTROL);
                    var novel=Sentence.speak(List.of(NOVEL),target.mind.lexicon());check(novel.sounds().size()==2&&novel.correct(Sentence.interpret(novel.sounds(),target.mind.lexicon())),"unheard combination survives restart");
                    check(!Sentence.interpret(novel.sounds(),control.mind.lexicon()).complete(),"isolated comparison remains unresolved");
                    if(Villagers.LEARNING_MODE==LearningMode.FROZEN){var before=target.mind.snapshot();Speech.clauses(level,body(server,BRIDGE),session(server,BRIDGE),List.of(FACT),true,body(server,TARGET),Set.of(Concept.HURT));check(before.lexicon().equals(target.mind.lexicon())&&before.beliefs().equals(target.mind.beliefs())&&before.voice().equals(target.mind.voice()),"frozen exposure changes no learned culture");}
                    var phase=Villagers.LEARNING_MODE==LearningMode.FROZEN?"frozen":"restart";Files.writeString(root.resolve("culture-"+phase+"-result.json"),JSON.toJson(Map.of("status",phase+"-passed","originalsVerifiedAtLoad",4,"heldOutConstructionRetained",true)));server.halt(false);return;
                }
                setup(server);
            }
            if(ticks<40)return;var source=body(server,SOURCE);var bridge=body(server,BRIDGE);var target=body(server,TARGET);var s=session(server,SOURCE);var b=session(server,BRIDGE);var t=session(server,TARGET);
            if(ticks%200==40){
                check(s.dialogueBudget.take(level.getGameTime()),"controlled source attention budget");check(Speech.innovateObservedState(level,source,s,bridge),"actual public injury supports authored compact first use");innovations++;
                if(Sentence.speak(List.of(FACT),b.mind.lexicon()).sounds().size()==2&&b.dialogueBudget.take(level.getGameTime())){check(Speech.clauses(level,bridge,b,List.of(FACT),true,target,Set.of(Concept.HURT))!=null,"relay speaks its own learned production");relays++;}
                trace.add(Map.of("tick",ticks,"innovations",innovations,"relays",relays,"bridgeGrammar",b.mind.lexicon().grammar(),"targetGrammar",t.mind.lexicon().grammar()));Files.writeString(root.resolve("culture-progress.json"),JSON.toJson(trace));
            }
            var novel=Sentence.speak(List.of(NOVEL),t.mind.lexicon());
            if(innovations>=3&&relays>=3&&novel.sounds().size()==2&&novel.correct(Sentence.interpret(novel.sounds(),t.mind.lexicon()))){
                check(!source.hasLineOfSight(target)&&source.hasLineOfSight(bridge)&&bridge.hasLineOfSight(target),"declared visibility contact chain remains intact");
                var c=session(server,CONTROL);check(!Sentence.interpret(novel.sounds(),c.mind.lexicon()).complete(),"known words without contact do not supply compact syntax");
                check(c.mind.lexicon().grammar().constructions().isEmpty(),"unexposed culture never gained the new construction");
                var r=new LinkedHashMap<String,Object>();r.put("status","passed");r.put("ticks",ticks);r.put("controlledInnovationOpportunities",innovations);r.put("actualRelayUtterances",relays);r.put("sourceInitiallyHadNoConstructions",true);r.put("targetCouldNotHearSourceFact",true);r.put("heldOutProduction",novel);r.put("isolatedComparisonUnresolved",true);r.put("trace",trace);
                var minds=new LinkedHashMap<String,Object>();for(var id:IDS){check(body(server,id).isAlive(),"all originals survive");minds.put(id.toString(),session(server,id).mind.snapshot());}r.put("minds",minds);
                r.put("fixture","NEW peaceful platform, four original stationary NoAI villagers. Source adult starts with full plains SVO and ZERO constructions. Three sparse learner/control lexicons use the actual child inheritance routine plus explicitly equal I/IS/HURT/WALK lexical priors to isolate syntax; no productive construction or parent memory is seeded. Source and bridge have one setup native injury each. A stone visibility barrier prevents target from observing source, while bridge can see both. Separate wall isolates the comparison. Controlled 200-tick opportunities deliberately exercise the already-authored IS-omission option through Speech.innovateObservedState. Bridge speaks only after its own grammar selects the learned form, then target learns from its actual relay utterances. Held-out WALK construction is generated/interpreted for evaluation, never taught as an event. This is supported first use and controlled diffusion, not spontaneous discovery of new syntax, actual births, natural maturation or survival. Only public-state Speech/learning paths run; no post-setup actor setters or new resources.");
                Files.writeString(root.resolve("culture-result.json"),JSON.toJson(r));server.halt(false);return;
            }
            if(ticks>3000)throw new AssertionError("Cultural transmission incomplete "+JSON.toJson(trace));
        }catch(Throwable t){fail(server,t);}});
    }
    private static Villager body(MinecraftServer s,UUID id){return (Villager)s.overworld().getEntity(id);}
    private static Session session(MinecraftServer s,UUID id){return Villagers.session(body(s,id),s.overworld());}
    private static Lexicon novice(Lexicon parent){var random=new Random(1301){@Override public double nextDouble(){return .99;}};var c=Lexicon.child(parent,parent,random);for(var concept:List.of(Concept.I,Concept.IS,Concept.HURT,Concept.WALK))c=c.reinforce(concept,parent.wordFor(concept),1);return c;}
    private static Villager spawn(MinecraftServer s,UUID id,Lexicon language,int x,int z){var m=new AgentMind(id);m.establishOrigin("minecraft:plains");m.lexicon(language);Villagers.registerNewborn(m,s.overworld().getGameTime());var v=new Villager(EntityTypes.VILLAGER,s.overworld());v.setUUID(id);v.snapTo(x+.5,81,z+.5,0,0);v.setNoAi(true);s.overworld().addFreshEntity(v);Villagers.adopt(s.overworld(),v);return v;}
    private void setup(MinecraftServer s){
        for(int x=1;x<=14;x++)for(int z=1;z<=14;z++)for(int y=80;y<=84;y++)s.overworld().setBlock(new BlockPos(x,y,z),y==80||y<=83&&(x==1||x==10||x==14||z==1||z==14||x==6&&z==6)?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        var full=BiomeDialect.foundingLanguage("minecraft:plains");var stageOne=novice(full);var stageTwo=novice(stageOne);
        var source=spawn(s,SOURCE,full,4,4);var bridge=spawn(s,BRIDGE,stageOne,4,8);var target=spawn(s,TARGET,stageTwo,8,8);spawn(s,CONTROL,stageTwo,12,4);
        check(session(s,SOURCE).mind.lexicon().grammar().constructions().isEmpty(),"no source construction seed");
        check(source.hurtServer(s.overworld(),source.damageSources().generic(),1)&&bridge.hurtServer(s.overworld(),bridge.damageSources().generic(),1),"two actual native injuries supply visible facts");
        check(!source.hasLineOfSight(target)&&source.hasLineOfSight(bridge)&&bridge.hasLineOfSight(target),"physical contact topology established");
        var compact=Sentence.speak(List.of(NOVEL),full,true);check(!Sentence.interpret(compact.sounds(),session(s,TARGET).mind.lexicon()).complete(),"initial known words do not provide omitted syntax");
    }
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    private static void fail(MinecraftServer s,Throwable t){t.printStackTrace();try{Files.writeString(s.getWorldPath(LevelResource.ROOT).resolve("culture-failure.txt"),t.toString());}catch(Exception ignored){}s.halt(false);}
}
