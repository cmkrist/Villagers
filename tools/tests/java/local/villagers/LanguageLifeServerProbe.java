package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.villagers.language.*;
import local.villagers.mind.*;
import local.villagers.relations.Opinion;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;

/** New, declared communication fixture. Never opens a production population world. */
public final class LanguageLifeServerProbe implements ModInitializer {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final UUID REQUESTER=new UUID(401,1), VISITOR=new UUID(401,2), HOST=new UUID(401,3);
    private Villager requester,donor,visitor,host;
    private int ticks,innovationAttempts,damageEvents;
    private boolean initiallyBlocked,socialVisit,constructionPhase;
    private final Set<UUID> reopened=new HashSet<>();
    private final List<Map<String,Object>> trace=new ArrayList<>();
    public void onInitialize() {
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            Path report=level.getServer().getWorldPath(LevelResource.ROOT).resolve("language-life-result.json");
            if(!(entity instanceof Villager v)||!Files.exists(report))return;
            try{
                Path baseline=level.getServer().getWorldPath(LevelResource.ROOT).resolve("language-life-restart-baseline.json");
                var expected=JsonParser.parseString(Files.readString(Files.exists(baseline)?baseline:report)).getAsJsonObject().getAsJsonObject("minds").getAsJsonObject(v.getUUID().toString());
                if(expected==null)return;
                var saved=JsonParser.parseString(Files.readString(level.getServer().getWorldPath(LevelResource.ROOT).resolve("villagers")
                        .resolve(v.getUUID().toString()).resolve("mind.json"))).getAsJsonObject();
                check(saved.get("beliefs").equals(expected.get("beliefs")),"beliefs and provenance survive restart exactly");
                check(saved.get("lexicon").equals(expected.get("lexicon")),"learned constructions survive restart exactly");
                check(v.isAlive()&&Villagers.isManaged(v),"original body survives restart");reopened.add(v.getUUID());
            }catch(Throwable t){t.printStackTrace();level.getServer().halt(false);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            try{
                ticks++;
                if(ticks==1){server.overworld().setChunkForced(0,0,true);server.overworld().getChunk(0,0);server.tickRateManager().setTickRate(80);}
                if(ticks==40){
                    if(Files.exists(server.getWorldPath(LevelResource.ROOT).resolve("language-life-result.json"))){
                        check(reopened.size()==4,"four original minds verified before restart decisions");
                        if(Villagers.LEARNING_MODE==local.villagers.learning.LearningMode.FROZEN) {
                            var bodies=new ArrayList<Villager>();
                            for(var id:reopened)if(server.overworld().getEntity(id) instanceof Villager body)bodies.add(body);
                            var before=new HashMap<UUID,AgentMind.Snapshot>();for(var body:bodies)before.put(body.getUUID(),Villagers.session(body,server.overworld()).mind.snapshot());
                            boolean understood=false;
                            outer:for(var a:bodies)for(var b:bodies)if(a!=b&&a.distanceToSqr(b)<=100&&a.hasLineOfSight(b)&&b.hasLineOfSight(a)) {
                                var sa=Villagers.session(a,server.overworld());var sb=Villagers.session(b,server.overworld());
                                if(Speech.inviteCompany(server.overworld(),a,sa,b,sb,true)) {
                                    Speech.fulfilledCompany(sa,sb,server.overworld().getGameTime());understood=true;break outer;
                                }
                            }
                            check(understood,"frozen controls still understand existing social agreements");
                            for(var body:bodies) {
                                var after=Villagers.session(body,server.overworld()).mind.snapshot();var old=before.get(body.getUUID());
                                check(old.lexicon().equals(after.lexicon())&&old.beliefs().equals(after.beliefs())&&old.opinions().equals(after.opinions()),"frozen agreement changes no learned language, beliefs or opinions");
                            }
                            Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("language-life-frozen-result.json"),"{\"status\":\"frozen-passed\",\"originalsVerifiedAtLoad\":4,\"understoodAgreement\":true,\"learnedStateUnchanged\":true}");
                            server.halt(false);return;
                        }
                        var checkpoint=new LinkedHashMap<String,Object>();
                        for(var id:reopened) checkpoint.put(id.toString(),Villagers.session((Villager)server.overworld().getEntity(id),server.overworld()).mind.snapshot());
                        Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("language-life-restart-baseline.json"),JSON.toJson(Map.of("minds",checkpoint)));
                        Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("language-life-restart-result.json"),"{\"status\":\"restart-passed\",\"originalsVerifiedAtLoad\":4}");
                        server.halt(false);return;
                    }
                    setup(server);
                }
                if(requester==null)return;
                Session r=Villagers.existingSession(REQUESTER),d=Villagers.existingSession(donor.getUUID()),v=Villagers.existingSession(VISITOR),h=Villagers.existingSession(HOST);
                if(ticks%20==0&&r.foodSharedReceived==0)Speech.requestFood(server.overworld(),requester,r,donor);
                socialVisit |= v.mind.memories().stream().anyMatch(m->m.note().startsWith("Visited "))
                        && v.mind.beliefs().entries().stream().anyMatch(e->e.evidence()==Beliefs.Evidence.OBSERVED&&e.claim().verb()==Concept.MEET);
                if(r.foodSharedReceived>0&&socialVisit&&!constructionPhase){
                    check(d.foodSharedGiven==1&&donor.getInventory().countItem(Items.BREAD)==0,"one conserved actual gift after repair");
                    constructionPhase=true;
                }
                if(constructionPhase&&server.overworld().getGameTime()>=v.nextSpeech&&visitor.distanceToSqr(host)<=144
                        &&visitor.hasLineOfSight(host)&&host.hasLineOfSight(visitor)) {
                    // Bounded declared stimulus, through native damage. No health/pose/position setters.
                    if(visitor.getHealth()>=visitor.getMaxHealth()&&damageEvents<12
                            &&visitor.hurtServer(server.overworld(),visitor.damageSources().generic(),1))damageEvents++;
                    if(Speech.innovateObservedState(server.overworld(),visitor,v,host))innovationAttempts++;
                }
                if(ticks%100==0)trace.add(Map.of("tick",ticks,"requests",d.clarificationRequests,"denials",r.clarificationDenials,
                        "repairs",d.groundedRepairs,"given",d.foodSharedGiven,"socialVisit",socialVisit,
                        "visitorPatterns",v.mind.lexicon().grammar().constructions(),"hostPatterns",h.mind.lexicon().grammar().constructions()));
                if(ticks%100==0)Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("language-life-progress.json"),JSON.toJson(Map.of(
                        "tick",ticks,"social",socialVisit,"given",d.foodSharedGiven,"repairs",d.groundedRepairs,"innovations",innovationAttempts,
                        "hostPatterns",h.mind.lexicon().grammar().constructions(),"speechDueIn",v.nextSpeech-server.overworld().getGameTime(),
                        "visitorVelocity",visitor.getDeltaMovement().toString())));
                boolean pattern=h.mind.lexicon().grammar().production(new Clause(Concept.YOU,Concept.IS,Concept.HAPPY)).size()==2;
                if(initiallyBlocked&&r.foodSharedReceived==1&&d.groundedRepairs>=3&&r.clarificationDenials>=1&&socialVisit&&pattern){
                    var generated=Sentence.speak(List.of(new Clause(Concept.YOU,Concept.IS,Concept.HAPPY)),h.mind.lexicon());
                    check(generated.sounds().size()==2,"learned pattern generates a novel combination");
                    check(generated.correct(Sentence.interpret(generated.sounds(),h.mind.lexicon())),"generalized construction remains interpretable");
                    var report=new LinkedHashMap<String,Object>();report.put("status","passed");report.put("ticks",ticks);
                    report.put("initialMisinterpretationBlockedGift",initiallyBlocked);report.put("automaticGroundedRepairs",d.groundedRepairs);
                    report.put("spokenDenials",r.clarificationDenials);report.put("realFoodReceipt",r.lastFoodReceived);
                    report.put("autonomousUnderstoodSocialVisit",socialVisit);report.put("socialAgreements",v.socialAgreements);
                    report.put("innovationOpportunities",innovationAttempts);report.put("nativeOnePointDamageEvents",damageEvents);report.put("generalizedConstruction",generated);
                    report.put("trace",trace);
                    var minds=new LinkedHashMap<String,Object>();for(Villager body:List.of(requester,donor,visitor,host)){
                        check(body.isAlive(),"all four fixture bodies survive");minds.put(body.getUUID().toString(),Villagers.existingSession(body.getUUID()).mind.snapshot());
                    }report.put("minds",minds);
                    report.put("fixture","NEW peaceful walled platform, four adults in two separate chambers, declared requester4food, generous full donor20food/20saturation and1inventorybread+1publichandbread, lonely visitor with3prior food-favour opinions of host; visitor/host each6bread. No post-setup position/food/health setters, seed repair, grammar assignment or cooldown resets. Controlled request opportunities every20ticks respect ordinary request/repair cooldowns. After actual feeding+native social visit, continuous controlled compression opportunities respect speech cooldown and require actual native walking or visible injury. At most12declared native1damage stimuli, only when the visitor is at full health, provide repeated public injury evidence; healing and food consumption remain native. No body movement is forced. Tests learning and generalization of a bounded construction, not spontaneous invention of semantic categories or natural survival.");
                    Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("language-life-result.json"),JSON.toJson(report));
                    System.out.println("LANGUAGE LIFE PASS at "+ticks);server.halt(false);
                }
                if(ticks>5200)throw new AssertionError("Bounded dialogue acceptance incomplete: social="+socialVisit+", repairs="+d.groundedRepairs+", gifts="+r.foodSharedReceived+", patterns="+h.mind.lexicon().grammar().constructions()+", innovations="+innovationAttempts);
            }catch(Throwable t){t.printStackTrace();try{Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("language-life-failure.txt"),t.toString());}catch(Exception ignored){}server.halt(false);}
        });
    }
    private AgentMind mind(UUID id){var m=new AgentMind(id);m.establishOrigin("minecraft:plains");m.lexicon(BiomeDialect.foundingLanguage(m.originBiome()));return m;}
    private Villager body(MinecraftServer server,AgentMind mind,double x,double z){
        Villagers.registerNewborn(mind,server.overworld().getGameTime());var v=new Villager(EntityTypes.VILLAGER,server.overworld());
        v.setUUID(mind.id());v.snapTo(x,81,z,0,0);server.overworld().addFreshEntity(v);Villagers.adopt(server.overworld(),v);return v;
    }
    private AgentMind state(AgentMind mind,int food,double lonely){
        var json=JSON.toJsonTree(mind.snapshot()).getAsJsonObject();json.addProperty("loneliness",lonely);
        json.getAsJsonObject("food").addProperty("foodLevel",food);json.getAsJsonObject("food").addProperty("saturation",Math.min(food,5));
        return AgentMind.fromSnapshot(JSON.fromJson(json,AgentMind.Snapshot.class));
    }
    private void setup(MinecraftServer server){
        for(int x=1;x<=14;x++)for(int z=1;z<=14;z++)for(int y=80;y<=84;y++)
            server.overworld().setBlock(new BlockPos(x,y,z),y==80||y<=83&&(x==1||x==7||x==14||z==1||z==14)?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        requester=body(server,state(mind(REQUESTER),4,.1),3.5,4.5);
        AgentMind generous=null;
        for(int i=10;i<10000;i++){var candidate=mind(new UUID(401,i));if(candidate.has(local.villagers.genetics.Trait.GENEROUS)&&!candidate.has(local.villagers.genetics.Trait.GREEDY)){generous=candidate;break;}}
        check(generous!=null,"fixture donor found");generous.eat(4,20);
        var language=generous.lexicon();var wrong=new ArrayList<>(language.entries().stream().filter(e->e.concept()!=Concept.FOOD).toList());
        wrong.add(new Lexicon.Entry(Concept.BED,language.wordFor(Concept.FOOD),.6));
        generous.lexicon(new Lexicon(wrong,List.of(),false,language.grammar()));
        donor=body(server,generous,4.5,4.5);donor.getInventory().addItem(new ItemStack(Items.BREAD,1));donor.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.BREAD,1));
        var visitorMind=state(mind(VISITOR),16,.85);for(int i=0;i<3;i++)visitorMind.witness(HOST,Opinion.Event.SHARED_FOOD,0);
        visitor=body(server,visitorMind,10.5,8.5);host=body(server,state(mind(HOST),16,.1),12.5,8.5);
        visitor.getInventory().addItem(new ItemStack(Items.BREAD,6));host.getInventory().addItem(new ItemStack(Items.BREAD,6));
        Speech.requestFood(server.overworld(),requester,Villagers.existingSession(REQUESTER),donor);
        var d=Villagers.existingSession(donor.getUUID());
        initiallyBlocked=Villagers.existingSession(REQUESTER).foodSharedReceived==0&&d.clarificationRequests==1&&d.groundedRepairs==1;
        check(initiallyBlocked,"first misunderstood request starts a real bounded repair and transfers no food");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
