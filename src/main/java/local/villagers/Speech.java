package local.villagers;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import local.villagers.language.Concept;
import local.villagers.language.Clause;
import local.villagers.language.Referents;
import local.villagers.language.Sentence;
import local.villagers.language.LanguageLearning;
import local.villagers.language.Glossary;
import local.villagers.language.GlossaryBook;
import local.villagers.language.GlossaryStore;
import local.villagers.language.Lexicon;
import local.villagers.language.Phrases;
import local.villagers.language.Telling;
import local.villagers.language.Utterance;
import local.villagers.learning.LearningMode;
import local.villagers.mind.Episode;
import local.villagers.relations.Gossip;
import local.villagers.relations.FoodRequest;
import local.villagers.relations.Opinion;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Villager language in the world: giving villagers a language, speaking to nearby players (each sees the words
 * they understand translated), conversations between villagers (word spread, opinions, gossip, food requests),
 * telling memories, and players teaching words.
 */
final class Speech {
    private static final RandomGenerator RANDOM = new Random(4111);
    private static final int MAX_VILLAGER_HEARERS = 8;
    /** How much one lesson from a fully receptive villager strengthens a taught word. */
    private static final double TAUGHT_STRENGTH = 0.35;
    private static final long TEACH_COOLDOWN_TICKS = 40;
    private static GlossaryBook glossaries;

    private Speech() {}

    static void start(Path villagersRoot) {
        glossaries = new GlossaryBook(new GlossaryStore(villagersRoot), problem -> Villagers.LOG.warn("Glossary: {}", problem));
    }

    static void stop() {
        if (glossaries != null && !glossaries.close(10_000)) Villagers.LOG.error("Glossary saves still pending at shutdown");
        glossaries = null;
    }

    static void join(UUID player) { if (glossaries != null) glossaries.join(player); }
    static void leave(UUID player) { if (glossaries != null) glossaries.leave(player); }
    static void flush() { if (glossaries != null) glossaries.flush(); }

    static Glossary glossary(UUID player) { return glossaries == null ? Glossary.empty(player) : glossaries.get(player); }

    /** Assign origin once; migration never replaces a saved faction or vocabulary. */
    static void ensureLanguage(ServerLevel level, Villager v, Session s) {
        boolean changed = s.mind.originBiome() == null;
        if (changed) s.mind.establishOrigin(originBiome(level, v));
        // Even an empty developing child's lexicon must not be replaced with an adult vocabulary.
        if (s.mind.lexicon().isEmpty() && !s.mind.lexicon().developing()) {
            s.mind.lexicon(local.villagers.language.BiomeDialect.foundingLanguage(s.mind.originBiome()));
            changed = true;
        }
        var established = s.mind.lexicon().establishGrammar(s.mind.originBiome()).establishFormation(s.mind.originBiome());
        if (!established.equals(s.mind.lexicon())) { s.mind.lexicon(established); changed = true; }
        var names=s.mind.lexicon().referents();
        var self=Referents.Target.person(v.getUUID());
        if(names.nameFor(self)==null){
            String word=s.mind.name().split("-",2)[0].toLowerCase(java.util.Locale.ROOT);
            if(!local.villagers.language.Words.valid(word)||s.mind.lexicon().meaning(word)!=null||names.hasWord(word)) {
                do {word=local.villagers.language.Words.invent(RANDOM);} while(s.mind.lexicon().meaning(word)!=null||names.hasWord(word));
            }
            s.mind.lexicon(s.mind.lexicon().withReferents(names.establish(word,self,level.getGameTime())));changed=true;
        }
        if (changed) Villagers.persist(s);
    }

    static String originBiome(ServerLevel level, Villager v) {
        return level.getBiome(v.blockPosition()).unwrapKey().orElseThrow().identifier().toString();
    }

    /**
     * Say something grounded in what the villager is visibly doing. Unprompted remarks respect the speech cooldown;
     * {@code prompted} replies (thanks, answers) are always said.
     */
    static void say(ServerLevel level, Villager v, Session s, Phrases.Situation situation, Concept extra, boolean prompted) {
        List<Concept> concepts = new java.util.ArrayList<>(Phrases.concepts(situation, RANDOM));
        if (extra != null) concepts.add(extra);
        speak(level, v, s, concepts, true, prompted, null);
    }

    /** The caller's finite family budgets cover both offers and replies; courtesy speech cannot starve an invitation. */
    static Utterance familySignal(ServerLevel level, Villager speaker, Session session, boolean reply) {
        return clauses(level,speaker,session,local.villagers.language.RelationshipDialogue.words(reply
                ?local.villagers.language.RelationshipDialogue.Act.PROMISE_CHILD:local.villagers.language.RelationshipDialogue.Act.PROPOSE_CHILD),false,null,Set.of());
    }
    /** Local caller bounds proposals; explicit quotes/replies use the existing language and hearing UI. */
    static Utterance tradeSignal(ServerLevel level,Villager speaker,Session session,Villager listener,List<Concept> concepts) {
        return speak(level,speaker,session,concepts,true,true,listener);
    }

    /** Speak to every player in hearing range; returns the utterance, or null if nothing was said. */
    private static Utterance speak(ServerLevel level, Villager v, Session s, List<Concept> concepts, boolean grounded,
                                   boolean prompted, Villager addressee) {
        return speak(level, v, s, concepts, grounded, prompted, addressee, visibleContext(v));
    }

    private static Utterance speak(ServerLevel level, Villager v, Session s, List<Concept> concepts, boolean grounded,
                                   boolean prompted, Villager addressee, Set<Concept> visibleContext) {
        return speak(level, v, s, concepts, grounded, prompted, addressee, visibleContext, null);
    }

    private static Utterance speak(ServerLevel level, Villager v, Session s, List<Concept> concepts, boolean grounded,
                                   boolean prompted, Villager addressee, Set<Concept> visibleContext, BlockPos referent) {
        long now = level.getGameTime();
        if (!prompted && now < s.nextSpeech) return null;
        // Gifts and meals may prompt speech before the first ordinary think has initialized this speaker.
        ensureLanguage(level, v, s);
        if (Villagers.LEARNING_MODE != LearningMode.FROZEN && !s.mind.lexicon().developing()) {
            for (Clause clause : Clause.fromConcepts(concepts))
                for (Concept concept : clause.concepts()) s.mind.lexicon(s.mind.lexicon().nameMissing(concept, RANDOM));
        }
        Utterance utterance = Utterance.compose(s.mind.lexicon(), concepts, grounded);
        if (utterance == null && s.mind.lexicon().developing())
            utterance = Utterance.of(s.mind.lexicon(), concepts, grounded);
        if (utterance == null) return null;
        Clause observed = observedClause(visibleContext);
        if (grounded && utterance.sentence() != null && observed != null && observed.verb() == Concept.IS
                && Villagers.LEARNING_MODE != LearningMode.FROZEN && RANDOM.nextDouble() < .02) {
            var innovation = Utterance.clauses(s.mind.lexicon(),utterance.sentence().intended(),true,true);
            if (innovation != null) utterance = innovation;
        }
        return emit(level,v,s,utterance,grounded,addressee,visibleContext,referent);
    }

    static Utterance clauses(ServerLevel level,Villager v,Session s,List<Clause> clauses,boolean grounded,
                             Villager addressee,Set<Concept> context) {
        ensureLanguage(level,v,s);
        if (Villagers.LEARNING_MODE != LearningMode.FROZEN && !s.mind.lexicon().developing())
            for (Clause clause : clauses) for (Concept c : clause.concepts())
                s.mind.lexicon(s.mind.lexicon().nameMissing(c,RANDOM));
        Clause observed = observedClause(context);
        boolean innovate = observed != null && observed.verb() == Concept.IS && RANDOM.nextDouble() < .02;
        Utterance words = Utterance.clauses(s.mind.lexicon(),clauses,grounded,innovate);
        if (words == null) return null;
        if (grounded && observed != null && Villagers.LEARNING_MODE != LearningMode.FROZEN)
            s.mind.lexicon(s.mind.lexicon().withGrammar(s.mind.lexicon().grammar().rehearse(words.sounds(),s.mind.lexicon()::meaning,observed)));
        return emit(level,v,s,words,grounded,addressee,context,null);
    }

    /** Pointing supplies all actual visible item features; the chosen noun is never a learner input. */
    static Utterance lesson(ServerLevel level,Villager speaker,Session s,Villager listener,List<Clause> clauses,
                            Villager pointed,Set<Concept> features,boolean demonstration){
        ensureLanguage(level,speaker,s);
        if(Villagers.LEARNING_MODE!=LearningMode.FROZEN&&!s.mind.lexicon().developing())
            for(var clause:clauses)for(var concept:clause.concepts())s.mind.lexicon(s.mind.lexicon().nameMissing(concept,RANDOM));
        var words=Utterance.clauses(s.mind.lexicon(),clauses,demonstration,false);if(words==null)return null;
        speaker.getLookControl().setLookAt(pointed,30,30);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,pointed.getX(),pointed.getY()+1,pointed.getZ(),2,.08,.08,.08,0);
        return emit(level,speaker,s,words,demonstration,listener,features,pointed.blockPosition().above());
    }

    private static Utterance emit(ServerLevel level,Villager v,Session s,Utterance utterance,boolean grounded,
                                  Villager addressee,Set<Concept> visibleContext,BlockPos referent) {
        if(v.isSilent())return null;
        long now = level.getGameTime();
        VocalSpeech.say(level,v,s.mind,utterance.sounds(),grounded?visibleContext:Set.of(),referent,null);
        s.nextSpeech = now + 20L * Villagers.config().speechCooldownSeconds();
        String speaker = s.mind.name() + (addressee == null ? "" : " → " + nameOf(addressee));
        for (ServerPlayer player : hearers(level, v)) {
            // Hearing alone is not evidence of a meaning or access to a private need.
            Set<Concept> context = EnumSet.noneOf(Concept.class);
            Clause event = null;
            if (grounded && player.hasLineOfSight(v) && (referent == null || canSee(level, player, referent))) {
                context.addAll(visibleContext);
                event = observedClause(visibleContext);
                if (event != null) context.addAll(event.concepts());
                context.addAll(ambient(player));
            }
            final Utterance heard = utterance;
            final Clause observed = event;
            local.villagers.mind.Place seen=referent!=null&&level.isLoaded(referent)&&player.hasLineOfSight(v)&&canSee(level,player,referent)
                    ?new local.villagers.mind.Place(level.dimension().identifier().toString(),referent.getX(),referent.getY(),referent.getZ()):null;
            if (glossaries != null) glossaries.update(player.getUUID(),g -> {
                var known=g.withReferents(g.referents().hearIntroduction(heard.sounds(),g::translate,g.grammar(),
                        player.hasLineOfSight(v)?v.getUUID():null,seen,now));
                return LanguageLearning.hear(known,heard.sounds(),context,observed);
            });
            player.sendSystemMessage(render(speaker, utterance, glossary(player.getUUID())));
        }
        if (grounded) teachNearbyVillagers(level, v, utterance, visibleContext, referent);
        if (Villagers.LEARNING_MODE != LearningMode.FROZEN && utterance.sentence() != null) {
            double radius = Math.min(16,Villagers.config().hearingRadius());
            for (Villager listener : level.getEntitiesOfClass(Villager.class,v.getBoundingBox().inflate(radius),
                    other -> other != v && other.isAlive() && !other.isSleeping() && Villagers.isManaged(other)
                            && v.distanceToSqr(other) <= radius*radius && other.hasLineOfSight(v)).stream()
                    .sorted(Comparator.comparingDouble(v::distanceToSqr)).limit(MAX_VILLAGER_HEARERS).toList()) {
                Session session = Villagers.existingSession(listener.getUUID());
                if (session != null) {
                    var lexicon=session.mind.lexicon();
                    local.villagers.mind.Place seen=referent!=null&&level.isLoaded(referent)&&canSee(level,listener,referent)
                            ?new local.villagers.mind.Place(level.dimension().identifier().toString(),referent.getX(),referent.getY(),referent.getZ()):null;
                    session.mind.lexicon(lexicon.withReferents(lexicon.referents().hearIntroduction(utterance.sounds(),lexicon::meaning,
                            lexicon.grammar(),v.getUUID(),seen,now)));
                    if(grounded)TutorLife.misunderstood(session,v.getUUID(),utterance.sounds(),now);
                    session.mind.beliefs(session.mind.beliefs().hear(
                            Sentence.interpret(utterance.sounds(),session.mind.lexicon()),v.getUUID(),listener.getUUID(),
                            session.mind.opinionOf(v.getUUID()).trust(),now));
                }
            }
        }
        return utterance;
    }

    /** Context is externally observable, not a speaker's private needs, intended words, or inventory. */
    private static Set<Concept> visibleContext(Villager speaker) {
        Set<Concept> context = EnumSet.noneOf(Concept.class);
        Concept held = Concept.forItem(BuiltInRegistries.ITEM.getKey(speaker.getMainHandItem().getItem()).toString());
        if (held != null) context.add(held);
        if (speaker.isSleeping()) context.add(Concept.BED);
        if (speaker.isOnFire()) context.add(Concept.DANGER);
        if (speaker.getHealth() < speaker.getMaxHealth()) context.add(Concept.HURT);
        return context;
    }

    private static void teachNearbyVillagers(ServerLevel level, Villager speaker, Utterance words, Set<Concept> context, BlockPos referent) {
        if (Villagers.LEARNING_MODE == LearningMode.FROZEN || context.isEmpty()) return;
        double radius = Math.min(16, Villagers.config().hearingRadius());
        var listeners = level.getEntitiesOfClass(Villager.class, speaker.getBoundingBox().inflate(radius),
                other -> other != speaker && other.isAlive() && !other.isSleeping() && Villagers.isManaged(other)
                        && speaker.distanceToSqr(other) <= radius * radius && other.hasLineOfSight(speaker)
                        && (referent == null || canSee(level, other, referent)))
                .stream().sorted(Comparator.comparingDouble(speaker::distanceToSqr)).limit(MAX_VILLAGER_HEARERS).toList();
        for (Villager listener : listeners) {
            Session session = Villagers.existingSession(listener.getUUID());
            Clause event = observedClause(context);
            var visible = EnumSet.copyOf(context);
            if (event != null) visible.addAll(event.concepts());
            if (session != null) session.mind.lexicon(LanguageLearning.hear(session.mind.lexicon(), words.sounds(), visible, event, RANDOM));
        }
    }

    /** The caller supplies only public gestures/objects, never an intended sentence. */
    static Clause observedClause(Set<Concept> context) {
        if (context.size() != 1) return null;
        Concept object = context.iterator().next();
        if (object == Concept.WANT || object == Concept.TRADE) return null;
        return new Clause(Concept.I, object == Concept.HELLO ? Concept.SAY
                : object == Concept.HURT || object == Concept.WALK ? Concept.IS : Concept.SEE, object);
    }

    private static List<ServerPlayer> hearers(ServerLevel level, Villager v) {
        double radius = Villagers.config().hearingRadius();
        return level.getPlayers(p -> p.isAlive() && p.distanceToSqr(v) <= radius * radius);
    }

    /** What else a listener can see that might be what a word means: the item in their hand. */
    private static Set<Concept> ambient(ServerPlayer player) {
        Set<Concept> context = EnumSet.noneOf(Concept.class);
        Concept held = Concept.forItem(BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString());
        if (held != null) context.add(held);
        return context;
    }

    /** "Fern-ab12: kalu food" with understood words plain and unknown words in grey italics. */
    private static Component render(String speaker, Utterance utterance, Glossary glossary) {
        MutableComponent line = Component.literal(speaker + ": ").withStyle(ChatFormatting.GOLD);
        if (utterance.sentence() != null) {
            var interpreted = Sentence.interpret(utterance.sounds(), glossary::translate, glossary.grammar(),glossary.referents());
            if (interpreted.complete()) return line.append(Component.literal(interpreted.english()).withStyle(ChatFormatting.WHITE));
            for (String sound : utterance.sounds()) {
                if (sound.equals(";")) { line.append("; "); continue; }
                Concept decoded = glossary.translate(sound);
                line.append(Component.literal((decoded == null ? sound : decoded.gloss()) + " ")
                        .withStyle(decoded == null ? ChatFormatting.GRAY : ChatFormatting.WHITE));
            }
            return line;
        }
        boolean first = true;
        for (Utterance.Segment segment : utterance.render(glossary)) {
            if (!first) line.append(" ");
            first = false;
            line.append(segment.translation() == null
                    ? Component.literal(segment.word()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
                    : Component.literal(segment.translation().gloss()).withStyle(ChatFormatting.WHITE));
        }
        return line;
    }

    static void showForeign(ServerLevel level,net.minecraft.world.entity.Mob speaker,String name,List<String> sounds,Set<Concept> visible){
        var utterance=new Utterance(List.of(),false,new Sentence(List.of(),sounds));double radius=Math.min(16,Villagers.config().hearingRadius());
        for(var player:level.getPlayers(p->p.isAlive()&&p.distanceToSqr(speaker)<=radius*radius)){
            Set<Concept> observed=player.hasLineOfSight(speaker)?visible:Set.of();
            if(glossaries!=null&&Villagers.LEARNING_MODE!=LearningMode.FROZEN)glossaries.update(player.getUUID(),g->LanguageLearning.hear(g,sounds,observed,null));
            player.sendSystemMessage(render(name,utterance,glossary(player.getUUID())));
        }
    }

    private static String nameOf(Villager villager) {
        Session other = Villagers.existingSession(villager.getUUID());
        return other == null ? "villager" : other.mind.name();
    }

    /**
     * After a successful chat: the speaker passes on a word, may gossip, may tell a memory, and a hungry listener
     * may ask for food. Generic gossip never identifies private player/event records.
     * Frozen research controls keep their checkpoint, so nothing is learned in frozen mode.
     */
    static void converse(ServerLevel level, Villager speaker, Session s, Villager listener) {
        Session l = Villagers.existingSession(listener.getUUID());
        if (l == null || Villagers.LEARNING_MODE == LearningMode.FROZEN) return;
        long now = level.getGameTime();
        s.mind.witness(listener.getUUID(), Opinion.Event.CHATTED, now);
        l.mind.witness(speaker.getUUID(), Opinion.Event.CHATTED, now);
        PartnershipLife.contact(level,speaker,s,listener,l);

        if (RANDOM.nextDouble() < Gossip.chance(s.mind.traits())) gossip(level, speaker, s, listener, l, now);
        // A visible held object can be named; otherwise offer a greeting. No random private meaning is sent.
        Demonstration lesson = demonstration(level, speaker, listener);
        if (!(RANDOM.nextDouble()<.02 && innovateObservedState(level,speaker,s,listener)))
            speak(level, speaker, s, List.of(lesson.concept()), true, false, listener, Set.of(lesson.concept()), lesson.target());
        // Give the other dialect equal opportunity to be heard, within its own speech budget.
        Demonstration reply = demonstration(level, listener, speaker);
        speak(level, listener, l, List.of(reply.concept()), true, false, speaker, Set.of(reply.concept()), reply.target());

        askForFood(level, speaker, s, listener, l, now);
        if(lesson.target()!=null&&RANDOM.nextBoolean()) nameNearbyPlace(level,speaker,s,listener,lesson.target());
        else introduceSelf(level,speaker,s,listener);
        introduceSelf(level,listener,l,speaker);
        // A voluntary self-report can create a fallible lead, never access to the speaker's stock.
        if (s.dialogueBudget.take(now)) {
            if(namedFoodReport(level,speaker,s,listener)||namedPlaceReport(level,speaker,s,listener)) { }
            else if (FoodInventory.anySlot(speaker.getInventory(),AdventureActions.canHarvest(speaker)) >= 0)
                clauses(level,speaker,s,List.of(new Clause(Concept.I,Concept.HAVE,Concept.FOOD)),false,listener,Set.of());
            else if (s.mind.opinionOf(listener.getUUID()).regard() > .1)
                clauses(level,speaker,s,List.of(new Clause(Concept.I,Concept.LIKE,Concept.YOU)),false,listener,Set.of());
        }
    }

    static boolean introduceSelf(ServerLevel level,Villager speaker,Session session,Villager listener){
        if(Villagers.LEARNING_MODE==LearningMode.FROZEN||!audible(speaker,listener)||!session.namingBudget.take(level.getGameTime()))return false;
        ensureLanguage(level,speaker,session);
        var name=session.mind.lexicon().referents().nameFor(Referents.Target.person(speaker.getUUID()));
        if(name==null)return false;
        var words=clauses(level,speaker,session,List.of(new Clause(Concept.I,Concept.NAMED,Concept.PERSON,false,false,null,name)),false,listener,Set.of());
        if(words!=null)session.introductions++;
        return words!=null;
    }

    /** Naming a place requires both bodies to see the actual nearby supported block; coordinates are never spoken. */
    static boolean nameNearbyPlace(ServerLevel level,Villager speaker,Session session,Villager listener,BlockPos target){
        if(Villagers.LEARNING_MODE==LearningMode.FROZEN||!audible(speaker,listener)||speaker.distanceToSqr(Vec3.atCenterOf(target))>25
                ||!level.isLoaded(target)||!canSee(level,speaker,target)||!canSee(level,listener,target))return false;
        var block=level.getBlockState(target).getBlock();
        if(!(block instanceof BedBlock)&&!(block instanceof net.minecraft.world.level.block.CropBlock))return false;
        if(!session.namingBudget.take(level.getGameTime()))return false;
        ensureLanguage(level,speaker,session);
        var place=new local.villagers.mind.Place(level.dimension().identifier().toString(),target.getX(),target.getY(),target.getZ());
        var known=session.mind.lexicon().referents();var label=known.nameFor(Referents.Target.place(place));
        if(label==null){
            String word;
            do{word=local.villagers.language.Words.invent(RANDOM);}while(known.hasWord(word)||session.mind.lexicon().meaning(word)!=null);
            known=known.establish(word,Referents.Target.place(place),level.getGameTime());
            session.mind.lexicon(session.mind.lexicon().withReferents(known));label=known.nameFor(Referents.Target.place(place));
        }
        speaker.getLookControl().setLookAt(target.getX()+.5,target.getY()+.5,target.getZ()+.5);
        var sentence=Utterance.clauses(session.mind.lexicon(),List.of(new Clause(Concept.HERE,Concept.NAMED,Concept.PLACE,false,false,null,label)),false,false);
        if(sentence==null)return false;
        // Make the pointing event public in the world, rather than passing an invisible intended coordinate as a lesson.
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,target.getX()+.5,target.getY()+.8,target.getZ()+.5,2,.08,.08,.08,0);
        emit(level,speaker,session,sentence,false,listener,Set.of(),target);session.placeNamings++;return true;
    }

    /** A third party's public hand can be reported by its learned name; their private bag is never inspected. */
    static boolean namedFoodReport(ServerLevel level,Villager speaker,Session session,Villager listener){
        if(!audible(speaker,listener))return false;
        for(Villager other:level.getEntitiesOfClass(Villager.class,speaker.getBoundingBox().inflate(8),
                v->v!=speaker&&v!=listener&&v.isAlive()&&speaker.distanceToSqr(v)<=64&&speaker.hasLineOfSight(v)).stream().limit(8).toList()){
            if(!other.getMainHandItem().has(net.minecraft.core.component.DataComponents.FOOD))continue;
            var name=session.mind.lexicon().referents().nameFor(Referents.Target.person(other.getUUID()));
            if(name==null)continue;
            var event=Clause.named(name,Concept.HAVE,Concept.FOOD);
            if(Villagers.LEARNING_MODE!=LearningMode.FROZEN)session.mind.beliefs(session.mind.beliefs().observe(local.villagers.mind.Beliefs.Claim.resolve(event,speaker.getUUID(),listener.getUUID()),
                    speaker.getUUID(),level.getGameTime()));
            if(clauses(level,speaker,session,List.of(event),false,listener,Set.of())!=null){session.namedReports++;return true;}
        }
        for(var entry:session.mind.lexicon().referents().entries()){
            var place=entry.target().place();if(place==null||!place.dimension().equals(level.dimension().identifier().toString()))continue;
            BlockPos pos=new BlockPos(place.x(),place.y(),place.z());
            if(speaker.distanceToSqr(Vec3.atCenterOf(pos))>64||!level.isLoaded(pos)||!canSee(level,speaker,pos))continue;
            var state=level.getBlockState(pos);
            if(!(state.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop)||!crop.isMaxAge(state))continue;
            var name=session.mind.lexicon().referents().resolve(entry.word());if(name==null)continue;
            var event=Clause.named(name,Concept.HAVE,Concept.FOOD);
            if(Villagers.LEARNING_MODE!=LearningMode.FROZEN)session.mind.beliefs(session.mind.beliefs().observe(local.villagers.mind.Beliefs.Claim.resolve(event,speaker.getUUID(),listener.getUUID()),speaker.getUUID(),level.getGameTime()));
            if(clauses(level,speaker,session,List.of(event),false,listener,Set.of())!=null){session.namedReports++;return true;}
        }
        return false;
    }

    static boolean namedPlaceReport(ServerLevel level,Villager speaker,Session session,Villager listener){
        if(!audible(speaker,listener))return false;
        for(var entry:session.mind.lexicon().referents().entries()){
            var place=entry.target().place();if(place==null||!place.dimension().equals(level.dimension().identifier().toString()))continue;
            BlockPos pos=new BlockPos(place.x(),place.y(),place.z());
            if(speaker.distanceToSqr(Vec3.atCenterOf(pos))>64||!level.isLoaded(pos)||!canSee(level,speaker,pos)
                    ||!(level.getBlockState(pos).getBlock() instanceof BedBlock))continue;
            var name=session.mind.lexicon().referents().resolve(entry.word());if(name==null)continue;
            var event=Clause.named(name,Concept.IS,Concept.BED);
            if(Villagers.LEARNING_MODE!=LearningMode.FROZEN)session.mind.beliefs(session.mind.beliefs().observe(
                    local.villagers.mind.Beliefs.Claim.resolve(event,speaker.getUUID(),listener.getUUID()),speaker.getUUID(),level.getGameTime()));
            if(clauses(level,speaker,session,List.of(event),false,listener,Set.of())!=null){session.namedReports++;return true;}
        }
        return false;
    }

    /** A proposal and independently chosen reply must both be decoded before movement becomes a shared plan. */
    static boolean inviteCompany(ServerLevel level,Villager visitor,Session from,Villager host,Session to,boolean willing) {
        if (!from.dialogueBudget.take(level.getGameTime())
                || !to.dialogueBudget.take(level.getGameTime()) || !audible(visitor,host)) return false;
        long now = level.getGameTime(); from.socialInvitations++;
        Utterance invitation = clauses(level,visitor,from,List.of(new Clause(Concept.I,Concept.WANT,Concept.FRIEND)),false,host,Set.of());
        var heard = interpret(invitation,to);
        if (Villagers.LEARNING_MODE != LearningMode.FROZEN)
            to.mind.beliefs(to.mind.beliefs().hear(heard,visitor.getUUID(),host.getUUID(),to.mind.opinionOf(visitor.getUUID()).trust(),now));
        if (local.villagers.language.Dialogue.response(heard) != local.villagers.language.Dialogue.Response.INVITATION) {
            clarification(level,visitor,from,host,to,invitation,heard); return false;
        }
        Utterance reply = clauses(level,host,to,List.of(new Clause(Concept.I,willing?Concept.PROMISE:Concept.SAY,
                willing?Concept.FRIEND:Concept.NO)),false,visitor,Set.of());
        if (!willing || local.villagers.language.Dialogue.response(interpret(reply,from)) != local.villagers.language.Dialogue.Response.PROMISE) return false;
        if (Villagers.LEARNING_MODE != LearningMode.FROZEN)
            from.mind.beliefs(from.mind.beliefs().hear(interpret(reply,from),host.getUUID(),visitor.getUUID(),from.mind.opinionOf(host.getUUID()).trust(),now));
        from.socialAgreementPeer=host.getUUID();to.socialAgreementPeer=visitor.getUUID();
        from.socialAgreementUntil=to.socialAgreementUntil=now+200;
        from.socialAgreements++;to.socialAgreements++;
        from.mind.remember("Understood a promise of company from " + to.mind.name());
        to.mind.remember("Agreed to spend time with " + from.mind.name());
        return true;
    }

    static void fulfilledCompany(Session from,Session to,long now) {
        if (Villagers.LEARNING_MODE == LearningMode.FROZEN) {from.socialAgreementPeer=to.socialAgreementPeer=null;return;}
        if (from.mind.beliefs().promisedCompany(to.mind.id(),now)) {
            from.mind.witness(to.mind.id(),Opinion.Event.KEPT_PROMISE,now);
            from.mind.beliefs(from.mind.beliefs().fulfilledCompany(to.mind.id(),from.mind.id(),now));
        }
        // Both participants witnessed the contact; no private affection is inferred from it.
        to.mind.beliefs(to.mind.beliefs().observe(new local.villagers.mind.Beliefs.Claim(from.mind.id(),Concept.MEET,
                Concept.FRIEND,null,false,true),to.mind.id(),now));
        from.socialAgreementPeer=to.socialAgreementPeer=null;
    }

    /** Propose a shorter construction for a publicly visible body state. Ordinary speech timing applies. */
    static boolean innovateObservedState(ServerLevel level,Villager v,Session s,Villager listener) {
        if (Villagers.LEARNING_MODE==LearningMode.FROZEN || level.getGameTime()<s.nextSpeech
                || !audible(v,listener)) return false;
        Concept state = v.getHealth()<v.getMaxHealth() ? Concept.HURT
                : v.onGround() && v.getDeltaMovement().horizontalDistanceSqr()>.0001 ? Concept.WALK : null;
        if (state==null) return false;
        ensureLanguage(level,v,s);
        Clause event = new Clause(Concept.I,Concept.IS,state);
        var words=Utterance.clauses(s.mind.lexicon(),List.of(event),true,true);
        if (words==null) return false;
        s.mind.lexicon(s.mind.lexicon().withGrammar(s.mind.lexicon().grammar().rehearse(words.sounds(),s.mind.lexicon()::meaning,event)));
        emit(level,v,s,words,true,listener,Set.of(state),null);
        return true;
    }

    private static boolean audible(Villager a,Villager b) {
        return a != b && a.isAlive() && b.isAlive() && !a.isSleeping() && !b.isSleeping()
                && !a.isTrading() && !b.isTrading() && a.level()==b.level()
                && FoodRequest.audible(a.distanceToSqr(b),Villagers.config().hearingRadius(),a.hasLineOfSight(b)&&b.hasLineOfSight(a));
    }
    private static Sentence.Interpretation interpret(Utterance words,Session listener) {
        return words == null || words.sentence()==null ? new Sentence.Interpretation(List.of(),0,1)
                : Sentence.interpret(words.sounds(),listener.mind.lexicon());
    }

    /** Confirmation and correction are words. Only a jointly visible real food item can ground a food-name repair. */
    private static void clarification(ServerLevel level,Villager speaker,Session from,Villager listener,Session to,
                                      Utterance original,Sentence.Interpretation misunderstood) {
        if (original==null || original.sentence()==null || !audible(speaker,listener)) return;
        to.clarificationRequests++;
        Utterance echo=clauses(level,listener,to,local.villagers.language.Dialogue.confirmation(misunderstood),false,speaker,Set.of());
        boolean confirmed=local.villagers.language.Dialogue.confirms(interpret(echo,from),original.sentence().intended());
        Utterance answer=clauses(level,speaker,from,List.of(new Clause(Concept.I,Concept.SAY,confirmed?Concept.YES:Concept.NO)),false,listener,Set.of());
        if (Villagers.LEARNING_MODE != LearningMode.FROZEN
                && local.villagers.language.Dialogue.response(interpret(answer,to))==local.villagers.language.Dialogue.Response.REFUSAL)
            to.mind.beliefs(to.mind.beliefs().retract(misunderstood,speaker.getUUID(),listener.getUUID()));
        if (!confirmed) { from.clarificationDenials++; from.mind.remember("Denied an incorrect interpretation by " + to.mind.name()); }
        // Deictic repair must have physical evidence; an intended concept is not a lesson.
        var target = original.sentence().intended().size()==1 ? original.sentence().intended().getFirst().object() : null;
        if (target != Concept.FOOD || !visibleFood(speaker,listener)) return;
        Utterance demonstration=speak(level,speaker,from,List.of(Concept.FOOD),true,true,listener,Set.of(Concept.FOOD));
        if (demonstration!=null) { to.groundedRepairs++; to.mind.remember("Observed a food-word clarification from " + from.mind.name()); }
    }
    private static boolean visibleFood(Villager a,Villager b) {
        // Public hands only. Looking into another villager's bag would bypass communication.
        return (a.getMainHandItem().has(net.minecraft.core.component.DataComponents.FOOD)
                || b.getMainHandItem().has(net.minecraft.core.component.DataComponents.FOOD)) && a.hasLineOfSight(b)&&b.hasLineOfSight(a);
    }

    /** Joint attention to a visible object, or a greeting; bounded to a few adjacent blocks. */
    private record Demonstration(Concept concept, BlockPos target) {}

    private static Demonstration demonstration(ServerLevel level, Villager speaker, Villager listener) {
        if (RANDOM.nextBoolean()) return new Demonstration(Concept.HELLO, null);
        Concept held = Concept.forItem(BuiltInRegistries.ITEM.getKey(speaker.getMainHandItem().getItem()).toString());
        if (held != null) return new Demonstration(held, null);
        var origin = speaker.blockPosition();
        for (var target : BlockPos.betweenClosed(origin.offset(-2, -1, -2), origin.offset(2, 1, 2))) {
            if (!level.isLoaded(target)) continue;
            var block = level.getBlockState(target).getBlock();
            Concept object = block instanceof BedBlock ? Concept.BED
                    : block == Blocks.WHEAT ? Concept.WHEAT
                    : block == Blocks.CARROTS ? Concept.CARROT
                    : block == Blocks.POTATOES ? Concept.POTATO
                    : block == Blocks.BEETROOTS ? Concept.BEETROOT : null;
            if (object == null || !canSee(level, speaker, target) || !canSee(level, listener, target)) continue;
            speaker.getLookControl().setLookAt(target.getX() + .5, target.getY() + .5, target.getZ() + .5);
            return new Demonstration(object, target.immutable());
        }
        return new Demonstration(Concept.HELLO, null);
    }

    static boolean canSee(ServerLevel level, net.minecraft.world.entity.LivingEntity viewer, BlockPos target) {
        var hit = level.clip(new ClipContext(viewer.getEyePosition(),
                Vec3.atCenterOf(target), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, viewer));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(target);
    }

    /** A real meal is observable evidence of FOOD; the ordinary speech budget still applies. */
    static void ate(ServerLevel level, Villager speaker, Session session) {
        speak(level, speaker, session, List.of(Concept.FOOD), true, false, null, Set.of(Concept.FOOD));
    }

    private static void gossip(ServerLevel level, Villager speaker, Session s, Villager listener, Session l, long now) {
        // Prefer a memory about a player (reputation), else an opinion of another villager.
        Episode memory = s.mind.episodes().stream()
                .filter(e -> e.kind() == Episode.Kind.HURT_BY_PLAYER || e.kind() == Episode.Kind.PROTECTED_BY_PLAYER
                        || e.kind() == Episode.Kind.RECEIVED_ITEM)
                .reduce((first, second) -> second).orElse(null);
        if (memory != null && memory.subject() != null && RANDOM.nextBoolean()) {
            speak(level, speaker, s, Telling.concepts(memory), false, false, listener);
            // Generic clauses do not identify a player/event. Never transfer the hidden subject's exact record.
            return;
        }
        Opinion opinion = Gossip.pick(s.mind.opinions().known(), listener.getUUID(), RANDOM);
        if (opinion == null) return;
        speak(level, speaker, s, List.of(Concept.FRIEND, opinion.regard() > 0 ? Concept.HAPPY : Concept.AFRAID),
                false, false, listener);
        // An unnamed FRIEND is not enough to identify which opinion the speaker meant.
    }

    /** Authored hunger request uses the same phrase, budget, consent and handoff as incidental conversation. */
    static boolean requestFood(ServerLevel level,Villager requester,Session request,Villager neighbour) {
        if (Villagers.LEARNING_MODE==LearningMode.FROZEN) return false;
        var donor=Villagers.existingSession(neighbour.getUUID());
        if(donor==null&&request.mind.foodLevel()==0&&requester.getHealth()<=requester.getMaxHealth()*.25f)
            donor=Villagers.session(neighbour,level);
        if (donor==null) return false;
        long before=request.nextFoodRequest;
        askForFood(level,neighbour,donor,requester,request,level.getGameTime());
        return request.nextFoodRequest!=before;
    }

    /** The caller checks actual camp listeners; words do not reveal any donor inventory. */
    static Utterance campFoodSignal(ServerLevel level,Villager speaker,Session session,boolean urgent){
        var concepts=new ArrayList<Concept>(List.of(Concept.WANT,Concept.FOOD));
        if(urgent)concepts.add(Concept.HUNGRY);
        if(speaker.getHealth()<speaker.getMaxHealth())concepts.add(Concept.HURT);
        return speak(level,speaker,session,concepts,
            true,true,null,visibleContext(speaker));
    }

    static boolean willingToShare(Session donor,UUID peer) {
        boolean warm=donor.mind.has(local.villagers.genetics.Trait.GENEROUS)
                || donor.mind.mood().band()==local.villagers.mood.Mood.Band.HAPPY;
        return !donor.mind.has(local.villagers.genetics.Trait.GREEDY) && !donor.mind.mood().low()
                && (warm || donor.mind.opinionOf(peer).regard()>=.05) && donor.mind.hunger()<.4;
    }

    /** Complete a previously understood request. The recipient decides whether it still wants this real item. */
    static boolean deliverPendingFood(ServerLevel level,Villager donor,Session from,Villager recipient,long now) {
        if (Villagers.LEARNING_MODE==LearningMode.FROZEN || !recipient.getUUID().equals(from.foodDemand.giftRequester(now))
                || !donor.isAlive() || !recipient.isAlive() || !Villagers.isManaged(recipient)
                || donor.level()!=level || recipient.level()!=level || donor.isSleeping() || recipient.isSleeping()
                || donor.isTrading() || recipient.isTrading() || !willingToShare(from,recipient.getUUID())
                || !local.villagers.economy.FoodOffer.mayDeferOwnMeal(from.mind.foodLevel(),donor.getHealth(),donor.getMaxHealth())
                || !FoodRequest.inReach(donor.distanceToSqr(recipient),Villagers.config().hearingRadius(),
                        donor.hasLineOfSight(recipient) && recipient.hasLineOfSight(donor))) return false;
        var to=Villagers.existingSession(recipient.getUUID());if(to==null)return false;
        if (!local.villagers.economy.FoodOffer.wantsMeal(to.mind.foodLevel(),recipient.getHealth(),recipient.getMaxHealth(),to.mind.has(local.villagers.genetics.Trait.GLUTTON),
                FoodInventory.anySlot(recipient.getInventory(),AdventureActions.canHarvest(recipient))>=0)) {
            from.foodDemand.withdrawn(recipient.getUUID());
            return false;
        }
        if (!VillagerController.giveFood(donor,recipient))return false;
        from.foodDemand.fulfilled(recipient.getUUID());from.foodDeliveries++;
        to.mind.witness(donor.getUUID(),Opinion.Event.SHARED_FOOD,now);
        speak(level,donor,from,List.of(Concept.YES,Concept.FOOD),true,true,recipient);
        return true;
    }

    /** A hungry listener asks locally; only an understood request can elicit sharing or an explicit refusal. */
    private static void askForFood(ServerLevel level, Villager speaker, Session s, Villager listener, Session l, long now) {
        if (speaker == listener || speaker.level() != level || listener.level() != level
                || !speaker.isAlive() || !listener.isAlive() || speaker.isTrading() || listener.isTrading()
                || speaker.isSleeping() || listener.isSleeping()
                || !FoodRequest.audible(speaker.distanceToSqr(listener), Villagers.config().hearingRadius(),
                        speaker.hasLineOfSight(listener) && listener.hasLineOfSight(speaker))
                || !FoodRequest.dueForCare(now,l.nextFoodRequest,l.mind.foodLevel(),listener.getHealth(),listener.getMaxHealth(),l.mind.has(local.villagers.genetics.Trait.GLUTTON))) return;
        // A healthy asker's pending source question owns its attention until answered or expired.
        if(l.foodQuestion!=null&&l.foodQuestion.current(now)&&l.foodQuestion.stage()==local.villagers.language.PracticalQuestions.Stage.ANSWERED
                &&l.mind.foodLevel()>4&&!local.villagers.mind.MealPolicy.needsRecoveryFood(l.mind.foodLevel(),listener.getHealth(),listener.getMaxHealth()))return;
        l.nextFoodRequest = now + FoodRequest.COOLDOWN;
        // A situated request supplies candidate meanings, not a per-word translation.
        Utterance request = speak(level, listener, l, List.of(Concept.WANT, Concept.FOOD), true, true, speaker,
                visibleContext(listener));
        if (request != null && request.sentence() != null) {
            var interpretation = Sentence.interpret(request.sounds(), s.mind.lexicon());
            s.mind.remember("Heard " + l.mind.name() + ": " + (interpretation.complete()
                    ? interpretation.english() + " (confidence " + Math.round(100 * interpretation.confidence()) + "%)"
                    : "unresolved words or grammar"));
        }
        if (!FoodRequest.understands(request, s.mind.lexicon(), Concept.WANT, Concept.FOOD)) {
            if (s.dialogueBudget.take(now) && l.dialogueBudget.take(now))
                clarification(level,listener,l,speaker,s,request,interpret(request,s));
            if (!FoodRequest.understands(request, s.mind.lexicon(), Concept.WANT, Concept.FOOD)) return;
        }
        s.mind.lexicon(s.mind.lexicon().confirm(request.sounds()));
        // Unhappy villagers keep their food; happy and generous ones share even with acquaintances.
        boolean willing = willingToShare(s,listener.getUUID());
        boolean emergencyRoot=!s.mind.has(local.villagers.genetics.Trait.GREEDY)
            &&FoodInventory.emergencyRootGiftSlot(speaker.getInventory(),AdventureActions.canHarvest(speaker),s.mind.foodLevel(),
                speaker.getHealth(),speaker.getMaxHealth(),listener.getHealth(),listener.getMaxHealth())>=0;
        if (willing||emergencyRoot) {
            if (willing&&VillagerController.giveFood(speaker, listener)
                    ||emergencyRoot&&VillagerController.giveFood(speaker,listener,true)) {
                s.foodDemand.fulfilled(listener.getUUID());
                l.mind.witness(speaker.getUUID(), Opinion.Event.SHARED_FOOD, now);
                level.sendParticles(ParticleTypes.HEART, listener.getX(), listener.getY() + 1.8, listener.getZ(), 2, .2, .2, .2, 0);
                speak(level, speaker, s, List.of(Concept.YES, Concept.FOOD), true, true, listener);
            } else {
                // A failed transfer alone proves nothing about stock. Only the owner can report its own absence.
                if(!FoodInventory.containsFood(speaker.getInventory())
                        &&!speaker.getMainHandItem().has(net.minecraft.core.component.DataComponents.FOOD)){
                    var absent=clauses(level,speaker,s,List.of(new Clause(Concept.I,Concept.HAVE,Concept.FOOD,true,false)),false,listener,Set.of());
                    var decoded=interpret(absent,l);
                    if(decoded.complete()&&decoded.clauses().equals(List.of(new Clause(Concept.I,Concept.HAVE,Concept.FOOD,true,false)))
                            &&speaker.getUUID().equals(FoodQuestions.lead(l,now)))FoodQuestions.unavailable(l);
                }
                if(s.foodDemand.hear(listener.getUUID(), now)){
                    // The heard request wakes existing bounded food production; it grants no food or work permission.
                    s.nextFamilyProvision = now;
                    s.mind.remember("Heard unmet food request from " + listener.getUUID() + "; preparing or delivering a real meal");
                }
            }
            // No food or no receiving capacity is not a deliberate social refusal.
        } else {
            Utterance refusal = speak(level, speaker, s, List.of(Concept.NO), true, true, listener);
            if (!FoodRequest.understands(refusal, l.mind.lexicon(), Concept.NO)) return;
            l.mind.witness(speaker.getUUID(), Opinion.Event.REFUSED, now);
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, listener.getX(), listener.getY() + 1.8, listener.getZ(), 1, .2, .2, .2, 0);
        }
    }

    /**
     * A player talks to a villager. It greets them, and if it trusts them it tells its newest memory they don't
     * share. Generic clauses communicate their expressed meaning without unlocking hidden episode details.
     */
    static void talk(ServerLevel level, Villager v, Session s, ServerPlayer player) {
        say(level, v, s, Phrases.Situation.GREETING, null, true);
        Episode untold = s.mind.untoldTo(player.getUUID());
        if (untold == null) return;
        if (s.mind.playerImpression(player.getUUID()).trust() < 0.2) {
            player.sendSystemMessage(Component.literal(s.mind.name() + " doesn't trust you enough to share more.")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        Utterance told = speak(level, v, s, Telling.concepts(untold), false, true, null);
        if (told == null) return;
        // The player hears their interpretation above. Exact episode disclosure requires spoken referents,
        // which generic memory clauses do not yet express; never recover them from private episode metadata.
    }

    enum TeachResult { LEARNED, REINFORCED, COOLDOWN, INVALID }

    /** A player teaches a villager a word for a concept. Receptive villagers learn faster; they repeat it back. */
    static TeachResult teach(ServerLevel level, Villager v, Session s, ServerPlayer player, Concept concept, String word) {
        if (concept == null || !local.villagers.language.Words.valid(word)) return TeachResult.INVALID;
        long now = level.getGameTime();
        if (now < s.nextLesson) return TeachResult.COOLDOWN;
        s.nextLesson = now + TEACH_COOLDOWN_TICKS;
        double strength = TAUGHT_STRENGTH * s.mind.receptivenessTo(player.getUUID());
        s.mind.lexicon(s.mind.lexicon().reinforce(concept, word, strength));
        boolean adopted = word.equals(s.mind.lexicon().wordFor(concept));
        // A lexical lesson repeats one sound, including verbs; it is not a malformed sentence about that verb.
        Utterance repeated=Utterance.of(s.mind.lexicon(),List.of(concept),true);
        if(repeated!=null)emit(level,v,s,repeated,true,null,visibleContext(v),null);
        Villagers.persist(s);
        return adopted ? TeachResult.LEARNED : TeachResult.REINFORCED;
    }
}
