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

/** Native controller integration with declared prior acquaintances and a stationary public witness. */
public final class QuestionServerProbe implements ModInitializer {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private int ticks;
    private Villager seeker,witness,provider,control;
    private UUID firstQuestionPeer;
    private final Set<UUID> reopened=new HashSet<>();
    private final List<Map<String,Object>> trace=new ArrayList<>();
    public void onInitialize(){
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            Path path=level.getServer().getWorldPath(LevelResource.ROOT).resolve("question-result.json");
            if(!(entity instanceof Villager v)||!Files.exists(path))return;
            try{
                var expected=JsonParser.parseString(Files.readString(path)).getAsJsonObject().getAsJsonObject("minds").getAsJsonObject(v.getUUID().toString());
                if(expected==null)return;
                var actual=JsonParser.parseString(Files.readString(level.getServer().getWorldPath(LevelResource.ROOT).resolve("villagers").resolve(v.getUUID().toString()).resolve("mind.json"))).getAsJsonObject();
                check(expected.get("lexicon").equals(actual.get("lexicon")),"language exact at restart");
                check(expected.get("beliefs").equals(actual.get("beliefs")),"sources and beliefs exact at restart");
                check(v.isAlive()&&Villagers.isManaged(v),"original body alive");reopened.add(v.getUUID());
            }catch(Throwable t){t.printStackTrace();level.getServer().halt(false);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            try{
                ticks++;
                if(ticks==1){server.overworld().setChunkForced(0,0,true);server.overworld().getChunk(0,0);server.tickRateManager().setTickRate(80);}
                if(ticks==40){
                    if(Files.exists(server.getWorldPath(LevelResource.ROOT).resolve("question-result.json"))){
                        check(reopened.size()==4,"all four originals reopen");
                        Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("question-restart-result.json"),"{\"status\":\"restart-passed\",\"originalsVerifiedAtLoad\":4}");server.halt(false);return;
                    }
                    setup(server);
                }
                if(seeker==null)return;
                var s=Villagers.existingSession(seeker.getUUID());var w=Villagers.existingSession(witness.getUUID());var p=Villagers.existingSession(provider.getUUID());
                if(firstQuestionPeer==null&&s.foodQuestion!=null)firstQuestionPeer=s.foodQuestion.peer();
                if(ticks%20==0)trace.add(Map.of("tick",ticks,"seekerPosition",seeker.position().toString(),"providerPosition",provider.position().toString(),
                        "food",s.mind.foodLevel(),"questions",s.foodQuestions,"sources",s.sourceQuestions,"status",s.foodHelpStatus,"conversation",s.foodQuestion==null?"none":s.foodQuestion,
                        "received",s.foodSharedReceived,"providerDelivery",p.foodDeliveries));
                if(s.foodLeadVerifications>0){
                    check(witness.getUUID().equals(firstQuestionPeer),"initial question chose nearer empty witness");
                    check(s.foodQuestions==1&&s.sourceQuestions==1&&s.qualifiedFoodLeads==1,"separate bounded question and source turns");
                    check(s.foodLeadInvestigations>0,"decoded answer redirected actual food-help target");
                    check(server.overworld().getGameTime()-s.foodQuestion.started()>=Dialogue.Budget.INTERVAL,"source follow-up occurred in a later attention turn");
                    var unacquainted=Villagers.existingSession(control.getUUID());
                    check(unacquainted.mind.lexicon().referents().nameFor(Referents.Target.person(provider.getUUID()))==null,"control never received hidden provider identity");
                    check(unacquainted.mind.beliefs().foodLead(provider.getUUID(),server.overworld().getGameTime())==0,"unacquainted control could not resolve the named lead");
                    check(provider.getUUID().equals(s.foodQuestion.claim().subject()),"provider named by the listener's own parse");
                    check(provider.getUUID().toString().equals(s.lastFoodReceived.get("from")),"physical meal came from reported provider");
                    check(((Number)s.lastFoodReceived.get("giverBefore")).intValue()-((Number)s.lastFoodReceived.get("giverAfter")).intValue()==1,"donor paid one food item");
                    check(((Number)s.lastFoodReceived.get("receiverAfter")).intValue()-((Number)s.lastFoodReceived.get("receiverBefore")).intValue()==1,"receiver gained the same item");
                    check(witness.getInventory().isEmpty(),"witness did not receive or supply hidden food");
                    var qualified=s.mind.beliefs().entries().stream().filter(e->e.source().equals(witness.getUUID())&&provider.getUUID().equals(e.claim().subject())
                            &&Beliefs.food(e.claim())&&e.provenance().basis()==Beliefs.Basis.REPORTED_DIRECT).findFirst();
                    check(qualified.isPresent()&&qualified.get().evidence()==Beliefs.Evidence.HEARD,"reported sight stays attributed testimony");
                    check(s.mind.beliefs().entries().stream().anyMatch(e->e.evidence()==Beliefs.Evidence.OBSERVED&&provider.getUUID().equals(e.claim().subject())&&e.claim().verb()==Concept.GIVE&&e.claim().past()),"actual handoff supplies separate observed evidence");
                    var report=new LinkedHashMap<String,Object>();report.put("status","passed");report.put("ticks",ticks);report.put("firstQuestionPeer",firstQuestionPeer);
                    report.put("witness",witness.getUUID());report.put("provider",provider.getUUID());report.put("trace",trace);report.put("receipt",s.lastFoodReceived);
                    report.put("questions",s.foodQuestions);report.put("sourceQuestions",s.sourceQuestions);report.put("qualifiedLeads",s.qualifiedFoodLeads);report.put("investigations",s.foodLeadInvestigations);report.put("verifications",s.foodLeadVerifications);
                    var minds=new LinkedHashMap<String,Object>();for(var body:List.of(seeker,witness,provider,control)){
                        check(body.isAlive(),"all originals survive");minds.put(body.getUUID().toString(),Villagers.existingSession(body.getUUID()).mind.snapshot());
                    }report.put("minds",minds);
                    report.put("fixture","NEW peaceful stone platform. Declared prior acquaintance: seeker/witness/provider initially share aliases fern=provider and oak=witness; acquisition was tested separately, not claimed here. Seeker starts food8, full health and empty bag. Witness starts full with an empty bag and NoAI to keep its location fixed; control is also stationary. Farther generous provider starts full, with two actual bread in its bag and one additional public held bread. Ordinary production controller, language, food-help, navigation and delivery run for seeker/provider; fixture calls no action adapters. Exhaustion0 is a controlled condition. No post-setup teleport, health, inventory, grammar, belief or reference setters. No existing society opened; no natural emergence or survival-scale claim.");
                    Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("question-result.json"),JSON.toJson(report));System.out.println("QUESTION PASS at "+ticks);server.halt(false);
                }
                if(ticks>1600){Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("question-trace.json"),JSON.toJson(trace));throw new AssertionError("Question investigation incomplete");}
            }catch(Throwable t){t.printStackTrace();try{Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("question-failure.txt"),t.toString());}catch(Exception ignored){}server.halt(false);}
        });
    }
    private Villager body(MinecraftServer server,AgentMind mind,double x,double z,int food,boolean stationary,Referents prior){
        mind.establishOrigin("minecraft:plains");mind.lexicon(BiomeDialect.foundingLanguage(mind.originBiome()).withReferents(prior));
        var json=JSON.toJsonTree(mind.snapshot()).getAsJsonObject();json.add("food",JSON.toJsonTree(new Metabolism.State(food,food==20?20:0,0,0)));
        mind=AgentMind.fromSnapshot(JSON.fromJson(json,AgentMind.Snapshot.class));
        Villagers.registerNewborn(mind,server.overworld().getGameTime());var v=new Villager(EntityTypes.VILLAGER,server.overworld());
        v.setUUID(mind.id());v.snapTo(x,81,z,0,0);v.setNoAi(stationary);server.overworld().addFreshEntity(v);Villagers.adopt(server.overworld(),v);return v;
    }
    private void setup(MinecraftServer server){
        for(int x=1;x<=14;x++)for(int z=1;z<=14;z++)for(int y=80;y<=84;y++)server.overworld().setBlock(new BlockPos(x,y,z),
                y==80||y<=83&&(x==1||x==14||z==1||z==14)?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        AgentMind generous=null;for(int i=3;i<10000;i++){var m=new AgentMind(new UUID(711,i));if(m.has(Trait.GENEROUS)&&!m.has(Trait.GREEDY)){generous=m;break;}}
        check(generous!=null,"generous founder found");
        var wm=new AgentMind(new UUID(712,1));var sm=new AgentMind(new UUID(713,1));
        var prior=Referents.empty().establish("fern",Referents.Target.person(generous.id()),server.overworld().getGameTime()).establish("oak",Referents.Target.person(wm.id()),server.overworld().getGameTime());
        witness=body(server,wm,4.5,5.5,20,true,prior);provider=body(server,generous,9.5,5.5,20,false,prior);
        provider.getInventory().addItem(new ItemStack(Items.BREAD,2));provider.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BREAD,1));
        control=body(server,new AgentMind(new UUID(714,1)),12.5,12.5,20,true,Referents.empty());
        seeker=body(server,sm,3.5,5.5,8,false,prior);
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
