package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.villagers.language.*;
import local.villagers.mind.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.LevelResource;

/** Controlled naming encounters in a new world; a wall supplies an actually unexposed comparison actor. */
public final class ReferenceServerProbe implements ModInitializer {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final BlockPos SITE=new BlockPos(5,81,7);
    private int ticks;
    private Villager subject,narrator,listener,control;
    private boolean initiallyUnknown;
    private final Set<UUID> reopened=new HashSet<>();
    private final List<Map<String,Object>> trace=new ArrayList<>();
    public void onInitialize(){
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            Path path=level.getServer().getWorldPath(LevelResource.ROOT).resolve("reference-result.json");
            if(!(entity instanceof Villager v)||!Files.exists(path))return;
            try{
                var expected=JsonParser.parseString(Files.readString(path)).getAsJsonObject().getAsJsonObject("minds").getAsJsonObject(v.getUUID().toString());
                if(expected==null)return;
                var actual=JsonParser.parseString(Files.readString(level.getServer().getWorldPath(LevelResource.ROOT).resolve("villagers").resolve(v.getUUID().toString()).resolve("mind.json"))).getAsJsonObject();
                check(expected.get("lexicon").equals(actual.get("lexicon")),"learned names exact before restart decisions");
                check(expected.get("beliefs").equals(actual.get("beliefs")),"referential testimony exact before restart decisions");
                check(v.isAlive()&&Villagers.isManaged(v),"original fixture body alive");reopened.add(v.getUUID());
            }catch(Throwable t){t.printStackTrace();level.getServer().halt(false);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            try{
                ticks++;
                if(ticks==1){server.overworld().setChunkForced(0,0,true);server.overworld().getChunk(0,0);server.tickRateManager().setTickRate(80);}
                if(ticks==40){
                    if(Files.exists(server.getWorldPath(LevelResource.ROOT).resolve("reference-result.json"))){
                        check(reopened.size()==4,"four originals reopen with their own reference memories");
                        Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("reference-restart-result.json"),"{\"status\":\"restart-passed\",\"originalsVerifiedAtLoad\":4}");server.halt(false);return;
                    }
                    setup(server);
                }
                if(subject==null)return;
                Session a=Villagers.existingSession(subject.getUUID()),n=Villagers.existingSession(narrator.getUUID()),l=Villagers.existingSession(listener.getUUID()),c=Villagers.existingSession(control.getUUID());
                if(ticks%20==0){
                    Speech.introduceSelf(server.overworld(),subject,a,narrator);
                    Speech.nameNearbyPlace(server.overworld(),narrator,n,listener,SITE);
                }
                var site=new Place(server.overworld().dimension().identifier().toString(),SITE.getX(),SITE.getY(),SITE.getZ());
                var knownPerson=l.mind.lexicon().referents().nameFor(Referents.Target.person(subject.getUUID()));
                var knownPlace=l.mind.lexicon().referents().nameFor(Referents.Target.place(site));
                if(knownPerson!=null&&knownPlace!=null&&n.dialogueBudget.take(server.overworld().getGameTime())){
                    Speech.namedFoodReport(server.overworld(),narrator,n,listener);
                    Speech.namedPlaceReport(server.overworld(),narrator,n,listener);
                }
                boolean personHeard=l.mind.beliefs().entries().stream().anyMatch(e->e.source().equals(narrator.getUUID())
                        &&subject.getUUID().equals(e.claim().subject())&&e.claim().verb()==Concept.HAVE&&e.claim().object()==Concept.FOOD);
                boolean placeHeard=l.mind.beliefs().entries().stream().anyMatch(e->e.source().equals(narrator.getUUID())
                        &&site.equals(e.claim().subjectPlace())&&e.claim().verb()==Concept.IS&&e.claim().object()==Concept.BED);
                if(ticks%100==0)trace.add(Map.of("tick",ticks,"introductions",a.introductions,"placeNamings",n.placeNamings,
                        "listenerNames",l.mind.lexicon().referents(),"personClaim",personHeard,"placeClaim",placeHeard));
                if(initiallyUnknown&&knownPerson!=null&&knownPlace!=null&&personHeard&&placeHeard){
                    check(c.mind.lexicon().referents().nameFor(Referents.Target.person(subject.getUUID()))==null,"wall control did not learn an unseen person");
                    check(c.mind.lexicon().referents().nameFor(Referents.Target.place(site))==null,"wall control did not learn an unseen place");
                    var person=Sentence.speak(List.of(Clause.named(n.mind.lexicon().referents().nameFor(Referents.Target.person(subject.getUUID())),Concept.HAVE,Concept.FOOD)),n.mind.lexicon());
                    var place=Sentence.speak(List.of(Clause.named(n.mind.lexicon().referents().nameFor(Referents.Target.place(site)),Concept.IS,Concept.BED)),n.mind.lexicon());
                    check(person!=null&&place!=null,"narrator speaks only learned names");
                    check(!Sentence.interpret(person.sounds(),c.mind.lexicon()).complete(),"unexposed actor cannot resolve a spoken name by registry lookup");
                    check(!Sentence.interpret(place.sounds(),c.mind.lexicon()).complete(),"unexposed actor cannot recover hidden coordinates");
                    check(person.sounds().stream().allMatch(Words::valid)&&place.sounds().stream().allMatch(Words::valid),"wire contains phonetic words only");
                    var report=new LinkedHashMap<String,Object>();report.put("status","passed");report.put("ticks",ticks);
                    report.put("initiallyUnknown",initiallyUnknown);report.put("introductions",a.introductions);report.put("placeNamings",n.placeNamings);
                    report.put("namedPersonStatement",person);report.put("namedPlaceStatement",place);report.put("unexposedControlStayedUnknown",true);report.put("trace",trace);
                    var minds=new LinkedHashMap<String,Object>();for(var body:List.of(subject,narrator,listener,control)){
                        check(body.isAlive(),"all four original bodies survive");minds.put(body.getUUID().toString(),Villagers.existingSession(body.getUUID()).mind.snapshot());
                    }report.put("minds",minds);
                    report.put("fixture","NEW peaceful walled platform with a separate closed control chamber. Four provisioned adults with distinct public name stems, six actual bread each; subject holds one additional actual bread. One existing fixture bed is named by pointing. Controlled introduction/naming opportunities every20ticks obey200tick naming budgets; reported claims obey dialogue budget. No post-setup inventory, body, health, reference-memory, grammar or world setters. Name bindings arise through actual Speech introductions/shared attention, not copied speaker metadata. No source society was opened; no natural emergence, visual UX or survival scaling claim.");
                    Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("reference-result.json"),JSON.toJson(report));System.out.println("REFERENCE PASS at "+ticks);server.halt(false);
                }
                if(ticks>2400)throw new AssertionError("References incomplete: person="+(knownPerson!=null)+", place="+(knownPlace!=null)+", personClaim="+personHeard+", placeClaim="+placeHeard);
            }catch(Throwable t){t.printStackTrace();try{Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("reference-failure.txt"),t.toString());}catch(Exception ignored){}server.halt(false);}
        });
    }
    private Villager body(MinecraftServer server,AgentMind mind,double x,double z){
        mind.establishOrigin("minecraft:plains");mind.lexicon(BiomeDialect.foundingLanguage(mind.originBiome()));
        Villagers.registerNewborn(mind,server.overworld().getGameTime());var v=new Villager(EntityTypes.VILLAGER,server.overworld());
        v.setUUID(mind.id());v.snapTo(x,81,z,0,0);v.getInventory().addItem(new ItemStack(Items.BREAD,6));
        server.overworld().addFreshEntity(v);Villagers.adopt(server.overworld(),v);return v;
    }
    private void setup(MinecraftServer server){
        for(int x=1;x<=14;x++)for(int z=1;z<=14;z++)for(int y=80;y<=84;y++)server.overworld().setBlock(new BlockPos(x,y,z),
                y==80||y<=83&&(x==1||x==11||x==14||z==1||z==14)?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        server.overworld().setBlock(SITE,Blocks.BED.red().defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH).setValue(BedBlock.PART,BedPart.FOOT),3);
        server.overworld().setBlock(SITE.north(),Blocks.BED.red().defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH).setValue(BedBlock.PART,BedPart.HEAD),3);
        var minds=new ArrayList<AgentMind>();var names=new HashSet<String>();for(int i=1;i<10000&&minds.size()<4;i++){
            var m=new AgentMind(new UUID(601,i));if(names.add(m.name().split("-",2)[0]))minds.add(m);
        }check(minds.size()==4,"distinct fixture public names found");
        subject=body(server,minds.get(0),4.5,5.5);narrator=body(server,minds.get(1),5.5,5.5);listener=body(server,minds.get(2),6.5,5.5);control=body(server,minds.get(3),12.5,5.5);
        subject.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BREAD,1));
        initiallyUnknown=!Speech.namedFoodReport(server.overworld(),narrator,Villagers.existingSession(narrator.getUUID()),listener);
        check(initiallyUnknown,"narrator cannot name a person it has not learned about");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
