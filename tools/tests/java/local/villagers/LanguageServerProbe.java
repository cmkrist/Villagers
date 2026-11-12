package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.villagers.language.*;
import local.villagers.mind.AgentMind;
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

/** Controlled communication fixture in a new world. Never opens a development society or native save. */
public final class LanguageServerProbe implements ModInitializer {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final UUID REQUESTER = new UUID(301, 1), TEACHER = new UUID(301, 2), LEARNER = new UUID(301, 3);
    private int ticks;
    private Villager requester, donor, teacher, learner;
    private final Set<UUID> reopened = new HashSet<>();
    private final List<Map<String,Object>> observations = new ArrayList<>();
    private boolean wrongRejected, shared;
    private long started;
    public void onInitialize() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (!(entity instanceof Villager v)) return;
            Path report = level.getServer().getWorldPath(LevelResource.ROOT).resolve("language-result.json");
            if (!Files.exists(report)) return;
            try {
                var expected = JsonParser.parseString(Files.readString(report)).getAsJsonObject().getAsJsonObject("minds").getAsJsonObject(v.getUUID().toString());
                if (expected == null) return;
                var path = level.getServer().getWorldPath(LevelResource.ROOT).resolve("villagers").resolve(v.getUUID().toString()).resolve("mind.json");
                var saved = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                check(expected.get("lexicon").equals(saved.get("lexicon")), "saved vocabulary and grammar exact before restart decisions");
                check(v.isAlive() && Villagers.isManaged(v), "original managed body reopens");
                reopened.add(v.getUUID());
            } catch (Throwable failure) { failure.printStackTrace(); level.getServer().halt(false); }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                ticks++;
                if (ticks == 1) { server.overworld().setChunkForced(0,0,true); server.overworld().getChunk(0,0); server.tickRateManager().setTickRate(80); }
                if (ticks == 40) {
                    if (Files.exists(server.getWorldPath(LevelResource.ROOT).resolve("language-result.json"))) {
                        check(reopened.size() == 4, "four original language minds verified at load");
                        Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("language-restart-result.json"), "{\"status\":\"restart-passed\",\"originalsVerifiedAtLoad\":4}");
                        server.halt(false); return;
                    }
                    setup(server);
                }
                if (learner == null) return;
                var session = Villagers.existingSession(LEARNER);
                if (ticks % 100 == 0) {
                    // Controlled encounter schedule; ordinary cooldown, spatial hearing and learning remain active.
                    Speech.converse(server.overworld(), teacher, Villagers.existingSession(TEACHER), learner);
                    observations.add(Map.of("tick", ticks, "grammar", session.mind.lexicon().grammar(), "preferred", session.mind.lexicon().grammar().preferred(), "teacherGrammar", Villagers.existingSession(TEACHER).mind.lexicon().grammar()));
                }
                boolean learnerChanged = session.mind.lexicon().grammar().preferred() == Grammar.Order.OVS;
                boolean teacherChanged = Villagers.existingSession(TEACHER).mind.lexicon().grammar().preferred() == Grammar.Order.SVO;
                if (learnerChanged || teacherChanged) {
                    check(wrongRejected && shared, "interpretation controlled real handoff");
                    for (Villager v : List.of(donor, requester, teacher, learner)) check(v.isAlive(), "all fixture bodies alive");
                    var report = new LinkedHashMap<String,Object>();
                    report.put("status", "passed"); report.put("wrongInterpretationPreventedHandoff", wrongRejected);
                    report.put("correctInterpretationTransferredRealBread", shared); report.put("grammarChangedByActualSpeech", true);
                    report.put("learnerAdoptedOVS", learnerChanged); report.put("teacherAdoptedSVO", teacherChanged);
                    report.put("observations", observations); report.put("ticks", ticks);
                    report.put("elapsedNanos", System.nanoTime() - started);
                    var minds = new LinkedHashMap<String,Object>();
                    for (Villager v : List.of(donor, requester, teacher, learner)) minds.put(v.getUUID().toString(), Villagers.existingSession(v.getUUID()).mind.snapshot());
                    report.put("minds", minds);
                    report.put("fixture", "NEW peaceful walled platform, four provisioned adults at80TPS; requester starts4food, generous donor4bread; learner starts with teacher vocabulary but SVO grammar, teacher OVS. Controlled food-request calls reset request cooldown once between cases; donor lexical error repaired with explicit visible-food lessons. Teacher/learner encounter scheduled every100ticks, ordinary speech cooldown5seconds and hearing apply. No post-setup body, health, terrain, food or physics setters. Not a natural-language emergence, survival, scale or client-rendering claim.");
                    Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("language-result.json"), JSON.toJson(report));
                    System.out.println("LANGUAGE SERVER PASS at " + ticks + " ticks"); server.halt(false);
                }
                if (ticks > 3000) throw new AssertionError("No observable grammar change within bounded conversations");
            } catch (Throwable failure) {
                failure.printStackTrace();
                try { Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("language-failure.txt"), failure.toString()); } catch (Exception ignored) {}
                server.halt(false);
            }
        });
    }
    private Villager body(MinecraftServer server, AgentMind mind, double x, double z) {
        var level = server.overworld();
        Villagers.registerNewborn(mind, level.getGameTime());
        var v = new Villager(EntityTypes.VILLAGER, level); v.setUUID(mind.id()); v.snapTo(x,81,z,0,0);
        level.addFreshEntity(v); Villagers.adopt(level,v); return v;
    }
    private AgentMind mind(UUID id, String biome) {
        var mind = new AgentMind(id); mind.establishOrigin(biome); mind.lexicon(BiomeDialect.foundingLanguage(biome)); return mind;
    }
    private void setup(MinecraftServer server) {
        started = System.nanoTime(); var level = server.overworld();
        for (int x=1;x<=14;x++) for (int z=1;z<=14;z++) for (int y=80;y<=84;y++)
            level.setBlock(new BlockPos(x,y,z), y==80 || y<=83 && (x==1||x==14||z==1||z==14) ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(),3);
        var hungry = mind(REQUESTER, "minecraft:plains");
        var snapshot = JSON.toJsonTree(hungry.snapshot()).getAsJsonObject();
        snapshot.getAsJsonObject("food").addProperty("foodLevel",4); snapshot.getAsJsonObject("food").addProperty("saturation",0);
        hungry = AgentMind.fromSnapshot(JSON.fromJson(snapshot, AgentMind.Snapshot.class));
        requester = body(server,hungry,3.5,4.5);
        AgentMind generous = null;
        for (int i=10;i<10000;i++) {
            var candidate = mind(new UUID(301,i), "minecraft:plains");
            if (candidate.has(local.villagers.genetics.Trait.GENEROUS) && !candidate.has(local.villagers.genetics.Trait.GREEDY)) { generous=candidate; break; }
        }
        check(generous != null, "generous fixture donor found");
        donor = body(server,generous,4.5,4.5); donor.getInventory().addItem(new ItemStack(Items.BREAD,4));
        var donorSession = Villagers.existingSession(donor.getUUID());
        var original = donorSession.mind.lexicon();
        var mistaken = original.entries().stream().map(e -> e.concept()==Concept.FOOD ? new Lexicon.Entry(Concept.BED,e.word(),e.strength())
                : e.concept()==Concept.BED ? new Lexicon.Entry(Concept.FOOD,e.word(),e.strength()) : e).toList();
        donorSession.mind.lexicon(new Lexicon(mistaken,List.of(),false,original.grammar()));
        var request = Villagers.existingSession(REQUESTER);
        Speech.requestFood(level,requester,request,donor);
        wrongRejected = requester.getInventory().countItem(Items.BREAD)==0 && donor.getInventory().countItem(Items.BREAD)==4;
        check(donorSession.mind.memories().stream().anyMatch(m -> m.note().contains("want a bed")), "actual donor interpreted the wrong object");
        var clear = new Random(1) { @Override public double nextDouble() { return .5; } };
        for(int i=0;i<5;i++) donorSession.mind.lexicon(donorSession.mind.lexicon().observe(List.of(original.wordFor(Concept.FOOD)),Set.of(Concept.FOOD),clear));
        request.nextFoodRequest=level.getGameTime();
        Speech.requestFood(level,requester,request,donor);
        shared = requester.getInventory().countItem(Items.BREAD)==1 && donor.getInventory().countItem(Items.BREAD)==3;
        check(shared, "decoded request transfers one conserved real bread");
        var teacherMind = mind(TEACHER,"minecraft:snowy_slopes");
        teacher = body(server,teacherMind,8.5,8.5);
        var learnerMind = mind(LEARNER,"minecraft:plains");
        learnerMind.lexicon(teacherMind.lexicon().withGrammar(Grammar.founding(Grammar.Order.SVO)));
        learner = body(server,learnerMind,9.5,8.5);
    }
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
