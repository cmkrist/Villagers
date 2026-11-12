package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.villagers.language.*;
import local.villagers.learning.LearningMode;
import local.villagers.mind.*;
import local.villagers.relations.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;

/** Native partnership behavior plus refusal/language controls, in a new world with declared prior friendships. */
public final class PartnershipServerProbe implements ModInitializer {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final UUID SEEKER=new UUID(801,1),HOST=new UUID(801,2),REFUSER_A=new UUID(802,1),REFUSER_B=new UUID(802,2),FOREIGN_A=new UUID(803,1),FOREIGN_B=new UUID(803,2),RECOVER_A=new UUID(804,1),RECOVER_B=new UUID(804,2);
    private static final List<UUID> IDS=List.of(SEEKER,HOST,REFUSER_A,REFUSER_B,FOREIGN_A,FOREIGN_B,RECOVER_A,RECOVER_B);
    private final Set<UUID> reopened=new HashSet<>();
    private final List<Map<String,Object>> trace=new ArrayList<>();
    private Villager seeker,host;
    private int ticks;
    private boolean refused,unknown,recovered,familyPhase;
    public void onInitialize(){
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            var root=level.getServer().getWorldPath(LevelResource.ROOT);Path report=root.resolve("partnership-result.json");
            if(!(entity instanceof Villager v)||!Files.exists(report)||!IDS.contains(v.getUUID()))return;
            try{
                Path baseline=root.resolve("partnership-restart-baseline.json");
                if(!Files.exists(baseline)&&Files.exists(root.resolve("partnership-family-baseline.json")))baseline=root.resolve("partnership-family-baseline.json");
                var expected=JsonParser.parseString(Files.readString(Files.exists(baseline)?baseline:report)).getAsJsonObject().getAsJsonObject("minds").getAsJsonObject(v.getUUID().toString());
                var actual=JsonParser.parseString(Files.readString(root.resolve("villagers").resolve(v.getUUID().toString()).resolve("mind.json"))).getAsJsonObject();
                for(var field:List.of("partnerships","lexicon","beliefs","opinions","episodes"))check(expected.get(field).equals(actual.get(field)),field+" exact before restart decisions");
                check(v.isAlive()&&Villagers.isManaged(v),"original body survived");reopened.add(v.getUUID());
            }catch(Throwable t){fail(level.getServer(),t);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            try{
                ticks++;
                if(ticks==1){for(int x=0;x<=1;x++){server.overworld().setChunkForced(x,0,true);server.overworld().getChunk(x,0);}server.tickRateManager().setTickRate(80);}
                var root=server.getWorldPath(LevelResource.ROOT);
                if(ticks==40){
                    if(Files.exists(root.resolve("partnership-result.json"))){
                        check(reopened.size()==8,"all original bodies reopen with exact memories");
                        seeker=(Villager)server.overworld().getEntity(SEEKER);host=(Villager)server.overworld().getEntity(HOST);
                        var a=Villagers.session(seeker,server.overworld());var b=Villagers.session(host,server.overworld());
                        if(Villagers.LEARNING_MODE==LearningMode.FROZEN){
                            var before=a.mind.snapshot();var other=b.mind.snapshot();
                            check(PartnershipLife.mutual(a,b),"frozen actors can use their saved commitment");
                            Speech.converse(server.overworld(),seeker,a,host);
                            check(!PartnershipLife.withdraw(server.overworld(),seeker,a,host),"frozen checkpoint refuses new relationship learning");
                            check(before.partnerships().equals(a.mind.partnerships())&&other.partnerships().equals(b.mind.partnerships()),"frozen commitment histories unchanged");
                            check(before.lexicon().equals(a.mind.lexicon())&&before.beliefs().equals(a.mind.beliefs())&&before.opinions().equals(a.mind.opinions()),"frozen dialogue changes no language or social knowledge");
                            Files.writeString(root.resolve("partnership-frozen-result.json"),"{\"status\":\"frozen-passed\",\"originalsVerifiedAtLoad\":8,\"savedCommitmentUsable\":true,\"learnedStateUnchanged\":true}");server.halt(false);return;
                        }
                        if(Villagers.config().breeding()){
                            check(PartnershipLife.mutual(a,b),"child invitation starts from an existing partnership");
                            familyPhase=true;return;
                        }
                        if(Files.exists(root.resolve("partnership-restart-result.json"))){
                            check(a.mind.partnerships().active()==null&&b.mind.partnerships().active()==null,"separation persists after restart");
                            Files.writeString(root.resolve("partnership-separated-result.json"),"{\"status\":\"separated-passed\",\"originalsVerifiedAtLoad\":8}");server.halt(false);return;
                        }
                        check(PartnershipLife.mutual(a,b),"saved partnership is still mutual");
                        check(a.courtship==null&&b.courtship==null,"expired session child consent is not resurrected by a persistent partnership");
                        // One declared separation opportunity exercises the production dialogue; no ledger is written by the fixture.
                        check(PartnershipLife.withdraw(server.overworld(),seeker,a,host),"actor can withdraw without permission");
                        check(!PartnershipLife.mutual(a,b)&&a.mind.partnerships().active()==null&&b.mind.partnerships().active()==null,"understood withdrawal ends both accounts");
                        check(a.mind.partnerships().history().getLast().end()==Partnerships.End.WITHDREW,"own consent ends immediately");
                        check(b.mind.partnerships().history().getLast().end()==Partnerships.End.HEARD_WITHDRAWAL,"peer learns withdrawal through words");
                        Files.writeString(root.resolve("partnership-restart-baseline.json"),JSON.toJson(Map.of("minds",snapshots(server))));
                        Files.writeString(root.resolve("partnership-restart-result.json"),"{\"status\":\"restart-passed\",\"originalsVerifiedAtLoad\":8,\"withdrawalUnderstood\":true}");server.halt(false);return;
                    }
                    setup(server);
                }
                if(seeker==null)return;
                var s=Villagers.existingSession(SEEKER);var h=Villagers.existingSession(HOST);
                if(familyPhase){
                    if(VillageFamilies.consented(s,h,server.overworld().getGameTime())
                            &&s.familyStatus.equals("needs a spare sheltered bed")){
                        check(s.courtship!=null&&h.courtship!=null,"parenthood has its own current agreement");
                        check(seeker.getInventory().countItem(Items.BREAD)==6&&host.getInventory().countItem(Items.BREAD)==6,"unavailable nursery charges no parent food");
                        check(server.overworld().getEntitiesOfClass(Villager.class,seeker.getBoundingBox().inflate(40),Villager::isAlive).size()==8,"no child or replacement body invented");
                        Files.writeString(root.resolve("partnership-family-baseline.json"),JSON.toJson(Map.of("minds",snapshots(server))));
                        Files.writeString(root.resolve("partnership-family-result.json"),JSON.toJson(Map.of("status","family-passed","ticks",ticks,"separateChildConsent",true,"nurseryAbsentBlockedBirth",true,"parentFoodConserved",true,"originalsVerifiedAtLoad",8)));server.halt(false);return;
                    }
                    if(ticks%100==0)Files.writeString(root.resolve("partnership-family-progress.json"),JSON.toJson(Map.of("tick",ticks,"ownStatus",s.familyStatus,"peerStatus",h.familyStatus,"nextSignal",s.nextFamilySignal,"gameTime",server.overworld().getGameTime(),"ownReady",VillageFamilies.readyToPrepare(server.overworld(),seeker,s,server.overworld().getGameTime()),"peerReady",VillageFamilies.readyToPrepare(server.overworld(),host,h,server.overworld().getGameTime()))));
                    if(ticks>1800)throw new AssertionError("Production child invitation did not complete: "+s.familyStatus+" / "+h.familyStatus);
                    return;
                }
                if(ticks%100==0){trace.add(Map.of("tick",ticks,"seekerPartnerships",s.mind.partnerships(),"hostPartnerships",h.mind.partnerships(),
                        "proposals",s.partnershipProposals,"agreements",s.partnershipAgreements,"status",s.partnershipStatus,"activity",VillagerController.currentActivity(s),
                        "loneliness",s.mind.loneliness(),"position",seeker.position().toString()));Files.writeString(root.resolve("partnership-trace.json"),JSON.toJson(trace));}
                if(PartnershipLife.mutual(s,h)&&s.mind.partnerships().active().sharedVisits()>0){
                    check(s.partnershipAgreements==1&&h.partnershipAgreements==1,"mutual promises rather than repeated passive speech formed partnership");
                    check(s.mind.memories().stream().anyMatch(m->m.note().startsWith("Visited ")),"native controller completed actual willing company");
                    check(refused&&unknown&&recovered,"refusal, foreign-language and half-save recovery controls pass");
                    for(var id:List.of(REFUSER_A,REFUSER_B,FOREIGN_A,FOREIGN_B))check(Villagers.existingSession(id).mind.partnerships().history().isEmpty(),"control has no invented agreement");
                    check(s.courtship==null&&h.courtship==null,"partner promises alone are not parenthood consent");
                    check(s.mind.lineage()==null&&h.mind.lineage()==null,"no new family history invented");
                    var records=snapshots(server);check(records.size()==8,"all eight originals survive");
                    for(var id:IDS)check(((Villager)server.overworld().getEntity(id)).getInventory().countItem(Items.BREAD)==6,"partner promises neither charge nor create food");
                    var r=new LinkedHashMap<String,Object>();r.put("status","passed");r.put("ticks",ticks);r.put("nativeCompany",true);r.put("refusalBlocked",refused);r.put("foreignLanguageBlocked",unknown);r.put("halfSaveReconfirmedThroughSpeech",recovered);
                    r.put("seekerPartnerships",s.mind.partnerships());r.put("hostPartnerships",h.mind.partnerships());r.put("minds",records);r.put("trace",trace);
                    r.put("fixture","NEW peaceful walled platform, two loaded fixture chunks, eight original adults. Initial prior friendships are explicitly seeded as three historic food-favor opinions, not claimed to emerge here. Seeker begins lonely .95 and full; host begins .1 lonely and full. Each has six actual bread. Ordinary production controller drives seeker; host and six control actors are NoAI stationary responders. Three controlled proposal opportunities run through production PartnershipLife: a willing proposer is independently refused by a neutral respondent; another pair lacks a shared biome language; a third starts with an explicitly seeded one-sided prior receipt and reconciles it through fresh reciprocal spoken promises. No post-setup body/health/inventory/relationship/grammar setters or cooldown resets. Autonomous actual company leads to mutual partnership, then a later actual visit records shared history. Exhaustion0/breedingFalse are controlled conditions; no birth, resource invention, existing society advancement, natural emergence, visual UX or scaling claim. Restart opportunities separately test frozen use, separate actual child consent with a missing nursery, explicit spoken separation and persistence.");
                    Files.writeString(root.resolve("partnership-result.json"),JSON.toJson(r));System.out.println("PARTNERSHIP PASS at "+ticks);server.halt(false);
                }
                if(ticks>5200)throw new AssertionError("Partnership native acceptance incomplete: "+s.partnershipStatus+" "+s.mind.partnerships());
            }catch(Throwable t){fail(server,t);}
        });
    }
    private Map<String,Object> snapshots(MinecraftServer server){var result=new LinkedHashMap<String,Object>();for(var id:IDS){var body=server.overworld().getEntity(id);check(body!=null&&body.isAlive(),"original body alive");result.put(id.toString(),Villagers.session((Villager)body,server.overworld()).mind.snapshot());}return result;}
    private AgentMind mind(UUID id,String biome,double loneliness){var m=new AgentMind(id);m.establishOrigin(biome);m.lexicon(BiomeDialect.foundingLanguage(biome));
        var json=JSON.toJsonTree(m.snapshot()).getAsJsonObject();json.addProperty("loneliness",loneliness);json.getAsJsonObject("food").addProperty("foodLevel",20);json.getAsJsonObject("food").addProperty("saturation",20);
        return AgentMind.fromSnapshot(JSON.fromJson(json,AgentMind.Snapshot.class));}
    private void friends(AgentMind m,UUID peer){for(int i=0;i<3;i++)m.witness(peer,Opinion.Event.SHARED_FOOD,0);}
    private Villager body(MinecraftServer server,AgentMind m,double x,double z,boolean stationary){Villagers.registerNewborn(m,server.overworld().getGameTime());var v=new Villager(EntityTypes.VILLAGER,server.overworld());v.setUUID(m.id());v.snapTo(x,81,z,0,0);v.setNoAi(stationary);v.getInventory().addItem(new ItemStack(Items.BREAD,6));server.overworld().addFreshEntity(v);Villagers.adopt(server.overworld(),v);return v;}
    private void setup(MinecraftServer server){
        for(int x=1;x<=31;x++)for(int z=1;z<=14;z++)for(int y=80;y<=84;y++)server.overworld().setBlock(new BlockPos(x,y,z),
                y==80||y<=83&&(x==1||x==14||x==16||x==24||x==26||x==31||z==1||z==14||x>=16&&z==7)?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        var s=mind(SEEKER,"minecraft:plains",.95);var h=mind(HOST,"minecraft:plains",.1);friends(s,HOST);friends(h,SEEKER);
        host=body(server,h,7.5,5.5,true);seeker=body(server,s,5.5,5.5,false);
        var ra=mind(REFUSER_A,"minecraft:plains",.1);friends(ra,REFUSER_B);
        var a=body(server,ra,18.5,4.5,true);var b=body(server,mind(REFUSER_B,"minecraft:plains",.1),20.5,4.5,true);
        var fa=mind(FOREIGN_A,"minecraft:plains",.1);friends(fa,FOREIGN_B);var fb=mind(FOREIGN_B,"minecraft:forest",.1);friends(fb,FOREIGN_A);
        var c=body(server,fa,18.5,10.5,true);var d=body(server,fb,20.5,10.5,true);
        refused=PartnershipLife.propose(server.overworld(),a,Villagers.existingSession(REFUSER_A),b)==PartnershipLife.Outcome.REFUSED;
        unknown=PartnershipLife.propose(server.overworld(),c,Villagers.existingSession(FOREIGN_A),d)==PartnershipLife.Outcome.UNKNOWN;
        check(refused&&unknown,"independent refusal and comprehension controls run through speech");
        var ma=mind(RECOVER_A,"minecraft:plains",.1);var mb=mind(RECOVER_B,"minecraft:plains",.1);friends(ma,RECOVER_B);friends(mb,RECOVER_A);
        // Declared initial fault condition: only one prior receipt survived. No post-setup saved mind is edited.
        ma.partnerships(ma.partnerships().form(RECOVER_B,0));
        var e=body(server,ma,27.5,5.5,true);var f=body(server,mb,29.5,5.5,true);
        var left=Villagers.existingSession(RECOVER_A);var right=Villagers.existingSession(RECOVER_B);
        check(!PartnershipLife.mutual(left,right),"initial half-save grants no consent");
        recovered=PartnershipLife.propose(server.overworld(),e,left,f)==PartnershipLife.Outcome.AGREED;
        check(recovered&&PartnershipLife.mutual(left,right)&&left.mind.partnerships().history().getFirst().end()==Partnerships.End.RECONFIRMED,"fresh heard reciprocal promises repair the half-save and retain prior account");

    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void fail(MinecraftServer server,Throwable t){t.printStackTrace();try{Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("partnership-failure.txt"),t.toString());}catch(Exception ignored){}server.halt(false);}
}
