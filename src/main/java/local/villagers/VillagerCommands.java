package local.villagers;

import com.mojang.brigadier.CommandDispatcher;
import local.villagers.language.Concept;
import net.minecraft.commands.SharedSuggestionProvider;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Comparator;
import java.util.Locale;
import local.villagers.mind.PlayerImpression;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

/** Operator commands available in every install: opt-in, opt-out and inspection. */
final class VillagerCommands {
    private static final int NEAREST_RADIUS = 24;
    private static final int NEARBY_RADIUS = 48;

    private VillagerCommands() {}

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("adopt").executes(c -> adoptNearest(c.getSource())))
                .then(Commands.literal("adoptnearby").executes(c -> adoptNearby(c.getSource())))
                .then(Commands.literal("help").executes(c -> help(c.getSource())))
                .then(Commands.literal("release").executes(c -> releaseNearest(c.getSource())))
                .then(Commands.literal("status").executes(c -> status(c.getSource())))
                .then(Commands.literal("impression").executes(c -> impression(c.getSource())))
                .then(Commands.literal("memories").executes(c -> memories(c.getSource())))
                .then(Commands.literal("language").executes(c -> language(c.getSource())))
                .then(Commands.literal("save").executes(c -> save(c.getSource()))));
    }

    /** Player commands, available without operator permission. */
    static void registerPlayerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("teach")
                .then(Commands.argument("concept", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                java.util.Arrays.stream(Concept.values()).map(Concept::gloss), builder))
                        .then(Commands.argument("word", StringArgumentType.word())
                                .executes(c -> teach(c.getSource(), StringArgumentType.getString(c, "concept"),
                                        StringArgumentType.getString(c, "word"))))));
        dispatcher.register(Commands.literal("glossary").executes(c -> glossary(c.getSource())));
    }

    /** Teach the nearest adopted villager (within 6 blocks, in sight) a word for a concept. */
    private static int teach(CommandSourceStack source, String conceptText, String word) {
        if (!ready(source)) return 0;
        if (!(source.getEntity() instanceof ServerPlayer player)) { tell(source, "Only players can teach words."); return 0; }
        Concept concept = Concept.parse(conceptText);
        if (concept == null) { tell(source, "Unknown concept \"" + conceptText + "\". Try tab completion."); return 0; }
        if (!local.villagers.language.Words.valid(word)) {
            tell(source, "Words are 2 to 12 lowercase letters a-z.");
            return 0;
        }
        ServerLevel level = player.level();
        Villager v = level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(6),
                        candidate -> candidate.isAlive() && Villagers.isManaged(candidate) && player.hasLineOfSight(candidate))
                .stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        if (v == null) { tell(source, "Stand within 6 blocks of an adopted villager you can see."); return 0; }
        Session s = Villagers.session(v, level);
        return switch (Speech.teach(level, v, s, player, concept, word)) {
            case LEARNED -> { tell(source, s.mind.name() + " now says \"" + word + "\" for " + concept.gloss() + "."); yield 1; }
            case REINFORCED -> { tell(source, s.mind.name() + " is learning \"" + word + "\" for " + concept.gloss()
                    + ". Teach it again, or teach its neighbours, until it sticks."); yield 1; }
            case COOLDOWN -> { tell(source, s.mind.name() + " needs a moment before the next word."); yield 0; }
            case INVALID -> 0;
        };
    }

    private static int glossary(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) { tell(source, "Only players have a glossary."); return 0; }
        var glossary = Speech.glossary(player.getUUID());
        var understood = glossary.understood();
        int unsure = glossary.words().size() - understood.size();
        tell(source, "You understand " + understood.size() + " villager words" + (unsure > 0 ? "; " + unsure + " more are still unclear." : "."));
        understood.forEach((word, meaning) -> tell(source, "  " + word + " = " + meaning.gloss()));
        return understood.size();
    }

    /** Operator view of a villager's language and how well its neighbours share it. */
    private static int language(CommandSourceStack source) {
        if (!ready(source)) return 0;
        Villager v = nearest(source);
        if (v == null || !Villagers.isManaged(v)) { tell(source, "No managed villager nearby."); return 0; }
        Session s = Villagers.session(v, source.getLevel());
        var lexicon = s.mind.lexicon();
        StringBuilder words = new StringBuilder();
        for (Concept concept : Concept.values()) {
            String word = lexicon.wordFor(concept);
            if (word != null) words.append(concept.gloss()).append('=').append(word).append(' ');
        }
        tell(source, s.mind.name() + " faction=" + s.mind.faction() + " originBiome=" + s.mind.originBiome()
                + " vocabulary=" + lexicon.knownConcepts() + "/" + Concept.values().length
                + " learningWords=" + lexicon.evidence().size() + (lexicon.developing() ? " (developing)" : ""));
        tell(source, "Says: " + words.toString().trim());
        var neighbours = source.getLevel().getEntitiesOfClass(Villager.class, v.getBoundingBox().inflate(128),
                other -> other != v && Villagers.isManaged(other)).stream()
                .map(other -> Villagers.existingSession(other.getUUID())).filter(java.util.Objects::nonNull).toList();
        double mean = neighbours.stream().mapToDouble(other -> local.villagers.language.Lexicon.comprehension(lexicon, other.mind.lexicon()))
                .average().orElse(Double.NaN);
        tell(source, neighbours.isEmpty() ? "No loaded adopted villagers within 128 blocks to compare."
                : String.format(Locale.ROOT, "Neighbours understand %.0f%% of spoken words, on average (%d villagers within 128 blocks).", 100 * mean, neighbours.size()));
        return 1;
    }

    static void tell(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal("[Villagers] " + message), false);
    }

    private static Villager nearest(CommandSourceStack source) {
        var origin = source.getPosition();
        return source.getLevel().getEntitiesOfClass(Villager.class, new AABB(origin, origin).inflate(NEAREST_RADIUS))
                .stream().min(Comparator.comparingDouble(v -> v.position().distanceToSqr(origin))).orElse(null);
    }

    private static boolean ready(CommandSourceStack source) {
        if (Villagers.ready()) return true;
        tell(source, "Villager storage is not available yet.");
        return false;
    }

    private static int help(CommandSourceStack source) {
        tell(source, "/villagers adopt or adoptnearby opts in residents; sneak-right-click to give tools, food or seeds; "
                + "sneak-right-click with an empty hand to hear memories; status, impression and memories inspect; release restores vanilla AI.");
        return 1;
    }

    private static int adoptNearest(CommandSourceStack source) {
        if (!ready(source)) return 0;
        Villager v = nearest(source);
        if (v == null) { tell(source, "No villager within " + NEAREST_RADIUS + " blocks."); return 0; }
        Villagers.adopt(source.getLevel(), v);
        tell(source, "Adopted " + Villagers.session(v, source.getLevel()).mind.name());
        return 1;
    }

    private static int adoptNearby(CommandSourceStack source) {
        if (!ready(source)) return 0;
        var origin = source.getPosition();
        var residents = source.getLevel().getEntitiesOfClass(Villager.class, new AABB(origin, origin).inflate(NEARBY_RADIUS),
                v -> v.isAlive() && !Villagers.isManaged(v));
        residents.forEach(v -> Villagers.adopt(source.getLevel(), v));
        tell(source, "Adopted " + residents.size() + " nearby residents.");
        return residents.size();
    }

    private static int releaseNearest(CommandSourceStack source) {
        if (!ready(source)) return 0;
        Villager v = nearest(source);
        if (v == null || !Villagers.isManaged(v)) { tell(source, "No managed villager within " + NEAREST_RADIUS + " blocks."); return 0; }
        VillagerController.release(source.getLevel(), v);
        tell(source, "Restored vanilla AI; saved learned identity.");
        return 1;
    }

    private static int status(CommandSourceStack source) {
        if (!ready(source)) return 0;
        Villager v = nearest(source);
        if (v == null || !Villagers.isManaged(v)) { tell(source, "No managed villager nearby."); return 0; }
        Session s = Villagers.session(v, source.getLevel());
        tell(source, s.mind.name() + " id=" + v.getUUID() + " action=" + s.action + " updates=" + s.mind.learningUpdates()
                + " home=" + s.mind.home());
        tell(source, String.format(Locale.ROOT, "food=%d/20 saturation=%.1f health=%.1f/%.1f age=%.2f days generation=%d%s",
                s.mind.foodLevel(), s.mind.saturation(), v.getHealth(), v.getMaxHealth(), s.mind.ageTicks() / 24_000.0,
                s.mind.generation(), s.mind.lineage() == null ? "" : s.mind.lineage().godChild() ? " (god child of " + s.mind.lineage().ancestorName() + ")"
                        : " (child of " + s.mind.lineage().ancestorName() + " and " + s.mind.lineage().secondParentName() + ")"));
        tell(source, "crafting=" + (BreadCrafting.wheatCount(v.getInventory()) >= 3 && s.craftingTable == null
                ? "needs crafting table" : s.craftingStatus) + " breadCraftedThisLoad=" + s.breadCrafted);
        tell(source, "family=" + s.familyStatus + " nextBirthTick=" + s.mind.nextBreedingAt()
                + " agreedPartner=" + (s.courtship == null ? "none" : s.courtship.partner()));
        var p = s.mind.expressedPersonality(); // Inherited tendencies as shifted by experience.
        tell(source, String.format(Locale.ROOT, "traits=%s boldness=%.2f sociability=%.2f curiosity=%.2f appetite=%.2f fertility=%.2f",
                s.mind.traits().isEmpty() ? "none" : s.mind.traits().toString().toLowerCase(Locale.ROOT),
                p.boldness(), p.sociability(), p.curiosity(),
                s.mind.genome().tendency(local.villagers.genetics.Tendency.APPETITE),
                s.mind.genome().tendency(local.villagers.genetics.Tendency.FERTILITY)));
        var mood = s.mind.mood();
        tell(source, String.format(Locale.ROOT, "mood=%s (%.2f)%s", mood.band().name().toLowerCase(Locale.ROOT), mood.value(),
                mood.reasons().isEmpty() ? "" : " because: " + String.join(", ", mood.reasons())));
        return 1;
    }

    private static int save(CommandSourceStack source) {
        if (!ready(source)) return 0;
        Villagers.persistAll();
        tell(source, "Queued identity and memory saves.");
        return 1;
    }

    /** Same visibility as talking to the villager: a survival operator only sees shared memories. */
    private static int memories(CommandSourceStack source) {
        if (!ready(source)) return 0;
        ServerPlayer viewer = source.getEntity() instanceof ServerPlayer caller ? caller : null;
        var origin = source.getPosition();
        Villager v = source.getLevel().getEntitiesOfClass(Villager.class, new AABB(origin, origin).inflate(NEAREST_RADIUS),
                        candidate -> candidate.isAlive() && Villagers.isManaged(candidate)).stream()
                .min(Comparator.comparingDouble(candidate -> candidate.position().distanceToSqr(origin))).orElse(null);
        if (v == null) { tell(source, "No managed villager within " + NEAREST_RADIUS + " blocks."); return 0; }
        Session s = Villagers.session(v, source.getLevel());
        boolean omniscient = viewer == null || VillagerController.omniscient(viewer);
        var lines = VillagerController.recall(source.getLevel(), s, viewer == null ? null : viewer.getUUID(), omniscient);
        tell(source, s.mind.name() + (lines.isEmpty() ? " has no memories yet." : " remembers (" + lines.size() + " most recent):"));
        lines.forEach(line -> tell(source, "  " + line));
        return lines.size();
    }

    private static int impression(CommandSourceStack source) {
        if (!ready(source)) return 0;
        ServerPlayer player = source.getEntity() instanceof ServerPlayer caller ? caller : null;
        boolean fallback = player == null;
        // Dev convenience: a console in a single-player lab speaks for the only player.
        if (player == null && DevMode.enabled() && source.getServer().getPlayerList().getPlayers().size() == 1)
            player = source.getServer().getPlayerList().getPlayers().getFirst();
        if (player == null) { tell(source, "Run as a player; a console has no player impression."); return 0; }
        ServerPlayer observer = player;
        ServerLevel level = observer.level();
        Villager v = level.getEntitiesOfClass(Villager.class, observer.getBoundingBox().inflate(NEAREST_RADIUS),
                        candidate -> candidate.isAlive() && Villagers.isManaged(candidate)).stream()
                .min(Comparator.comparingDouble(observer::distanceToSqr)).orElse(null);
        if (v == null) { tell(source, "No managed villager within " + NEAREST_RADIUS + " blocks of the resolved player."); return 0; }
        Session s = Villagers.session(v, level);
        PlayerImpression p = s.mind.playerImpression(observer.getUUID());
        tell(source, String.format(Locale.ROOT,
                "%s (%s) views %s%s: awe=%.2f trust=%.2f fear=%.2f confidence=%.2f evidence=%d; disposition=%s, active=%s. Prior remains revisable.",
                s.mind.name(), v.getUUID(), observer.getUUID(), fallback ? " (unique dev observer)" : "",
                p.awe(), p.trust(), p.fear(), p.confidence(), p.evidenceCount(),
                p.reaction(VillagerController.observe(level, v, s, false)), SocialReactions.status(v.getUUID()).get("reaction")));
        return 1;
    }
}
