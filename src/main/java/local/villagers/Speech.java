package local.villagers;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import local.villagers.language.Concept;
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
        return speak(level, speaker, session, List.of(reply ? Concept.YES : Concept.WANT, Concept.FRIEND, Concept.BED),
                false, true, null);
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
            for (Concept concept : concepts) s.mind.lexicon(s.mind.lexicon().nameMissing(concept, RANDOM));
        }
        Utterance utterance = Utterance.of(s.mind.lexicon(), concepts, grounded);
        if (utterance == null) return null;
        s.nextSpeech = now + 20L * Villagers.config().speechCooldownSeconds();
        String speaker = s.mind.name() + (addressee == null ? "" : " → " + nameOf(addressee));
        for (ServerPlayer player : hearers(level, v)) {
            if (glossaries != null) glossaries.update(player.getUUID(), g -> utterance.teach(g, ambient(player)));
            player.sendSystemMessage(render(speaker, utterance, glossary(player.getUUID())));
        }
        if (grounded) teachNearbyVillagers(level, v, utterance, visibleContext, referent);
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
            if (session != null) session.mind.lexicon(words.teach(session.mind.lexicon(), context, RANDOM));
        }
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

    private static String nameOf(Villager villager) {
        Session other = Villagers.existingSession(villager.getUUID());
        return other == null ? "villager" : other.mind.name();
    }

    /**
     * After a successful chat: the speaker passes on a word, may gossip, may tell a memory, and a hungry listener
     * may ask for food. Frozen research controls keep their checkpoint, so nothing is learned in frozen mode.
     */
    static void converse(ServerLevel level, Villager speaker, Session s, Villager listener) {
        Session l = Villagers.existingSession(listener.getUUID());
        if (l == null || Villagers.LEARNING_MODE == LearningMode.FROZEN) return;
        long now = level.getGameTime();
        s.mind.witness(listener.getUUID(), Opinion.Event.CHATTED, now);
        l.mind.witness(speaker.getUUID(), Opinion.Event.CHATTED, now);

        if (RANDOM.nextDouble() < Gossip.chance(s.mind.traits())) gossip(level, speaker, s, listener, l, now);
        // A visible held object can be named; otherwise offer a greeting. No random private meaning is sent.
        Demonstration lesson = demonstration(level, speaker, listener);
        speak(level, speaker, s, List.of(lesson.concept()), true, false, listener, Set.of(lesson.concept()), lesson.target());
        // Give the other dialect equal opportunity to be heard, within its own speech budget.
        Demonstration reply = demonstration(level, listener, speaker);
        speak(level, listener, l, List.of(reply.concept()), true, false, speaker, Set.of(reply.concept()), reply.target());

        askForFood(level, speaker, s, listener, l, now);
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

    private static boolean canSee(ServerLevel level, Villager viewer, BlockPos target) {
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
            Utterance told = speak(level, speaker, s, Telling.concepts(memory), false, false, listener);
            if (told != null) {
                if (told.understoodBy(l.mind.lexicon()) >= Telling.UNDERSTANDING)
                    l.mind.hearAboutPlayer(s.mind.id(), s.mind.playerImpression(memory.subject()));
                revealToUnderstandingListeners(level, speaker, s, memory, told);
            }
            return;
        }
        Opinion opinion = Gossip.pick(s.mind.opinions().known(), listener.getUUID(), RANDOM);
        if (opinion == null) return;
        Utterance told = speak(level, speaker, s, List.of(Concept.FRIEND, opinion.regard() > 0 ? Concept.HAPPY : Concept.AFRAID),
                false, false, listener);
        if (told != null && told.understoodBy(l.mind.lexicon()) >= Telling.UNDERSTANDING)
            l.mind.hearOpinion(s.mind.id(), opinion, now);
    }

    /** Players who overhear a told memory and understand enough of it come to share it. */
    private static void revealToUnderstandingListeners(ServerLevel level, Villager v, Session s, Episode memory, Utterance told) {
        for (ServerPlayer player : hearers(level, v)) {
            if (memory.isKnownBy(player.getUUID())) continue;
            if (told.understoodBy(glossary(player.getUUID())) >= Telling.UNDERSTANDING && s.mind.reveal(memory.id(), player.getUUID()))
                player.sendSystemMessage(Component.literal("You understood: " + VillagerController.describe(level, s, memory, player))
                        .withStyle(ChatFormatting.AQUA));
        }
    }

    /** Authored hunger request uses the same phrase, budget, consent and handoff as incidental conversation. */
    static boolean requestFood(ServerLevel level,Villager requester,Session request,Villager neighbour) {
        if (Villagers.LEARNING_MODE==LearningMode.FROZEN) return false;
        var donor=Villagers.existingSession(neighbour.getUUID());
        if (donor==null) return false;
        long before=request.nextFoodRequest;
        askForFood(level,neighbour,donor,requester,request,level.getGameTime());
        return request.nextFoodRequest!=before;
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
        l.nextFoodRequest = now + FoodRequest.COOLDOWN;
        // A situated request supplies candidate meanings, not a per-word translation.
        Utterance request = speak(level, listener, l, List.of(Concept.WANT, Concept.FOOD), true, true, speaker,
                Set.of(Concept.WANT, Concept.FOOD));
        if (!FoodRequest.understands(request, s.mind.lexicon(), Concept.WANT, Concept.FOOD)) return;
        s.mind.lexicon(s.mind.lexicon().confirm(request.sounds()));
        // Unhappy villagers keep their food; happy and generous ones share even with acquaintances.
        boolean willing = willingToShare(s,listener.getUUID());
        if (willing) {
            if (VillagerController.giveFood(speaker, listener)) {
                s.foodDemand.fulfilled(listener.getUUID());
                l.mind.witness(speaker.getUUID(), Opinion.Event.SHARED_FOOD, now);
                level.sendParticles(ParticleTypes.HEART, listener.getX(), listener.getY() + 1.8, listener.getZ(), 2, .2, .2, .2, 0);
                speak(level, speaker, s, List.of(Concept.YES, Concept.FOOD), true, true, listener);
            } else if (s.foodDemand.hear(listener.getUUID(), now)) {
                // The heard request wakes existing bounded food production; it grants no food or work permission.
                s.nextFamilyProvision = now;
                s.mind.remember("Heard unmet food request from " + listener.getUUID() + "; preparing or delivering a real meal");
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
     * share. The player comes to share it only if they understand enough of the words.
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
        if (told.understoodBy(glossary(player.getUUID())) >= Telling.UNDERSTANDING) {
            if (s.mind.reveal(untold.id(), player.getUUID()))
                player.sendSystemMessage(Component.literal("You understood: " + VillagerController.describe(level, s, untold, player))
                        .withStyle(ChatFormatting.AQUA));
        } else {
            player.sendSystemMessage(Component.literal("You don't understand enough of their words yet.").withStyle(ChatFormatting.GRAY));
        }
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
        speak(level, v, s, List.of(concept), true, true, null);
        Villagers.persist(s);
        return adopted ? TeachResult.LEARNED : TeachResult.REINFORCED;
    }
}
