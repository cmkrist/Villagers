package local.villagers.mind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Map;
import local.villagers.genetics.Genome;
import local.villagers.genetics.Tendency;
import local.villagers.genetics.Trait;
import local.villagers.language.Lexicon;
import local.villagers.mood.Experience;
import local.villagers.mood.Mood;
import local.villagers.relations.Opinion;
import local.villagers.relations.Opinions;
import java.util.UUID;
import java.util.random.RandomGenerator;

/** Small contextual action-value learner. World interaction and outcome attribution belong to the controller. */
public final class AgentMind {
    public static final int SCHEMA_VERSION = 9;
    public static final int MAX_FARMS = 8;
    /** Exhaustion per day of loaded life on top of activity; walking costs players nothing, so a body needs a baseline. */
    public static final double DEFAULT_BASE_EXHAUSTION_PER_DAY = 80;
    public static final int MAX_PLAYER_IMPRESSIONS = 32;
    public static final int MAX_EPISODES = 64;
    private static final int CONTEXTS = 32;
    private static final int MEMORY_LIMIT = 48;
    private static final String[] NAMES = {"Alder", "Birch", "Clover", "Dara", "Ember", "Fern", "Hazel", "Iris",
            "Juniper", "Linden", "Moss", "Nell", "Oak", "Pip", "Rowan", "Sage", "Tansy", "Willow"};
    private final UUID id;
    private final String name;
    private final Personality personality;
    private final Genome genome;
    private final Set<Trait> traits;
    private final double[][] values = new double[CONTEXTS][Action.values().length];
    private final int[][] visits = new int[CONTEXTS][Action.values().length];
    private final ArrayDeque<Memory> memories = new ArrayDeque<>();
    private final LinkedHashMap<UUID, PlayerImpression> playerImpressions = new LinkedHashMap<>();
    private final ArrayDeque<Episode> episodes = new ArrayDeque<>();
    private long nextEpisodeId;
    // Matches the old starting hunger of 0.2: 16 of 20 food points, the player's starting saturation.
    private Metabolism food = new Metabolism(16, Metabolism.START_SATURATION);
    private long ageTicks;
    private Lineage lineage;
    private long nextBreedingAt;
    private Death death;
    private Lexicon lexicon = Lexicon.empty();
    private String originBiome;
    private Opinions opinions = Opinions.empty();
    private Experience experience = Experience.none();
    private Mood mood = Mood.neutral();
    private double fatigue = 0.1;
    private double loneliness = 0.2;
    private Home home;
    private long learningUpdates;

    public record Personality(double boldness, double sociability, double curiosity) {
        public Personality {
            boldness = Observation.normalized(boldness);
            sociability = Observation.normalized(sociability);
            curiosity = Observation.normalized(curiosity);
        }
    }
    public record Memory(long index, Action action, int context, double reward, String note) {}
    public record Snapshot(int schemaVersion, UUID id, String name, Personality personality,
                        double hunger, double fatigue, double loneliness, Home home,
                        long learningUpdates, double[][] values, int[][] visits, List<Memory> memories,
                        List<PlayerImpression> impressions, List<Episode> episodes, long nextEpisodeId,
                        Genome genome, Metabolism.State food, long ageTicks, Lineage lineage, Death death,
                        Lexicon lexicon, Opinions opinions, Experience experience, long nextBreedingAt, String originBiome) {
        /** Source compatibility for schema-one callers; schema-one JSON also omits impressions and episodes. */
        public Snapshot(int schemaVersion, UUID id, String name, Personality personality,
                        double hunger, double fatigue, double loneliness, Home home,
                        long learningUpdates, double[][] values, int[][] visits, List<Memory> memories) {
            this(schemaVersion, id, name, personality, hunger, fatigue, loneliness, home,
                    learningUpdates, values, visits, memories, List.of(), List.of(), 0, null, null, 0, null, null, null, null, null, 0, null);
        }
        public Snapshot {
            if (originBiome != null) local.villagers.language.BiomeDialect.validate(originBiome);
            values = copy(values);
            visits = copy(visits);
            memories = memories == null ? null : List.copyOf(memories);
            impressions = impressions == null ? List.of() : List.copyOf(impressions);
            episodes = episodes == null ? List.of() : List.copyOf(episodes); // Absent before schema 3.
            // genome is absent before schema 4; loading derives a founder genome from the saved personality.
            // food, age, lineage and death are absent before schema 5; food is then derived from hunger.
            // lexicon and opinions are absent before schema 6. Uninitialized adults get their origin
            // biome's dialect when next observed. Sparse developing lexicons retain their learning state.
            lexicon = lexicon == null ? Lexicon.empty() : lexicon;
            opinions = opinions == null ? Opinions.empty() : opinions;
            experience = experience == null ? Experience.none() : experience;
        }
        @Override public double[][] values() { return copy(values); }
        @Override public int[][] visits() { return copy(visits); }
        private static double[][] copy(double[][] table) {
            return table == null ? null : Arrays.stream(table).map(row -> row == null ? null : row.clone()).toArray(double[][]::new);
        }
        private static int[][] copy(int[][] table) {
            return table == null ? null : Arrays.stream(table).map(row -> row == null ? null : row.clone()).toArray(int[][]::new);
        }
    }

    /** A first-generation villager: personality seeded from its UUID, and a founder genome that matches it. */
    public AgentMind(UUID id) {
        this.id = Objects.requireNonNull(id);
        Random seeded = seeded(id);
        name = seededName(id, seeded);
        personality = new Personality(seeded.nextDouble(), seeded.nextDouble(), seeded.nextDouble());
        genome = Genome.founder(id, pinned(personality));
        traits = genome.traits();
    }

    private AgentMind(UUID id, Genome inherited) {
        this.id = Objects.requireNonNull(id);
        name = seededName(id, seeded(id));
        genome = Objects.requireNonNull(inherited);
        traits = genome.traits();
        personality = new Personality(genome.tendency(Tendency.BOLDNESS), genome.tendency(Tendency.SOCIABILITY),
                genome.tendency(Tendency.CURIOSITY));
    }

    /** A newborn whose personality is expressed from an inherited genome, see {@link Genome#inherit}. */
    public static AgentMind born(UUID id, Genome genome) { return new AgentMind(id, genome); }

    /**
     * A god child: a newborn carrying a mutated copy of {@code ancestor}'s genome (see {@link Genome#godChild}),
     * sent when a village's last villager starves. It starts with a full food bar, like a respawned player.
     */
    public static AgentMind godChild(UUID id, AgentMind ancestor, RandomGenerator random) {
        AgentMind child = new AgentMind(id, Genome.godChild(ancestor.genome(), random));
        child.lineage = new Lineage(ancestor.id(), ancestor.name(), Lineage.nextGeneration(ancestor.generation(), 0), true);
        child.food = new Metabolism(Metabolism.MAX_FOOD, Metabolism.START_SATURATION);
        return child;
    }

    /** Ordinary child: both parents contribute genes and words, while learned action values start fresh. */
    public static AgentMind born(UUID id, AgentMind first, AgentMind second, RandomGenerator random) {
        if (first.id().equals(second.id())) throw new IllegalArgumentException("Distinct parents required");
        AgentMind child = new AgentMind(id, Genome.inherit(first.genome(), second.genome(), random));
        child.lineage = new Lineage(first.id(), first.name(), Lineage.nextGeneration(first.generation(), second.generation()),
                false, second.id(), second.name());
        child.lexicon = Lexicon.child(first.lexicon(), second.lexicon(), random);
        child.food = new Metabolism(Metabolism.MAX_FOOD, Metabolism.START_SATURATION);
        return child;
    }

    public long nextBreedingAt() { return nextBreedingAt; }
    public void bred(long now) {
        if (now < 0) throw new IllegalArgumentException("Negative birth tick");
        nextBreedingAt = now > Long.MAX_VALUE - local.villagers.family.BreedingPolicy.COOLDOWN ? Long.MAX_VALUE
                : now + local.villagers.family.BreedingPolicy.COOLDOWN;
    }

    private static Random seeded(UUID id) {
        return new Random(id.getMostSignificantBits() ^ Long.rotateLeft(id.getLeastSignificantBits(), 17));
    }

    private static String seededName(UUID id, Random seeded) {
        return NAMES[seeded.nextInt(NAMES.length)] + "-" + id.toString().substring(0, 4);
    }

    private static Map<Tendency, Double> pinned(Personality personality) {
        return Map.of(Tendency.BOLDNESS, personality.boldness(), Tendency.SOCIABILITY, personality.sociability(),
                Tendency.CURIOSITY, personality.curiosity());
    }

    private AgentMind(Snapshot state) {
        id = Objects.requireNonNull(state.id());
        if (state.name() == null || state.name().isBlank() || state.name().length() > 64)
            throw new IllegalArgumentException("Invalid persisted name");
        name = state.name();
        Personality traits = Objects.requireNonNull(state.personality());
        personality = new Personality(traits.boldness(), traits.sociability(), traits.curiosity());
        genome = state.genome() != null ? state.genome() : Genome.founder(id, pinned(personality));
        this.traits = genome.traits();
        food = state.food() != null ? new Metabolism(state.food()) : Metabolism.fromLegacyHunger(state.hunger());
        if (state.ageTicks() < 0) throw new IllegalArgumentException("Invalid age");
        ageTicks = state.ageTicks();
        lineage = state.lineage();
        if (state.nextBreedingAt() < 0) throw new IllegalArgumentException("Invalid breeding cooldown");
        nextBreedingAt = state.nextBreedingAt();
        death = state.death();
        lexicon = state.lexicon();
        originBiome = state.originBiome();
        opinions = state.opinions();
        experience = state.experience();
        fatigue = Observation.normalized(state.fatigue());
        loneliness = Observation.normalized(state.loneliness());
        home = state.home() == null ? null : new Home(state.home().dimension(), state.home().x(), state.home().y(), state.home().z());
        if (state.learningUpdates() < 0) throw new IllegalArgumentException("Invalid learning counter");
        learningUpdates = state.learningUpdates();
        double[][] savedValues = state.values();
        int[][] savedVisits = state.visits();
        if (savedValues == null || savedVisits == null || savedValues.length != CONTEXTS || savedVisits.length != CONTEXTS)
            throw new IllegalArgumentException("Invalid context table");
        for (int c = 0; c < CONTEXTS; c++) {
            if (savedValues[c] == null || savedVisits[c] == null
                    || savedValues[c].length != Action.values().length || savedVisits[c].length != Action.values().length)
                throw new IllegalArgumentException("Invalid action table");
            for (int a = 0; a < Action.values().length; a++) {
                double value = savedValues[c][a];
                if (!Double.isFinite(value) || value < -1 || value > 1 || savedVisits[c][a] < 0)
                    throw new IllegalArgumentException("Invalid learned value");
                values[c][a] = value;
                visits[c][a] = savedVisits[c][a];
            }
        }
        if (state.memories() != null) {
            if (state.memories().size() > MEMORY_LIMIT) throw new IllegalArgumentException("Too many memories");
            for (Memory memory : state.memories()) {
                if (memory == null || memory.context() < -1 || memory.context() >= CONTEXTS
                        || (memory.action() == null && memory.context() != -1)
                        || memory.index() < 0 || memory.index() > learningUpdates
                        || !Double.isFinite(memory.reward()) || Math.abs(memory.reward()) > 1
                        || memory.note() == null || memory.note().length() > 256)
                    throw new IllegalArgumentException("Invalid memory");
                memories.addLast(memory);
            }
        }
        if (state.impressions().size() > MAX_PLAYER_IMPRESSIONS)
            throw new IllegalArgumentException("Too many player impressions");
        for (PlayerImpression impression : state.impressions()) {
            if (playerImpressions.put(impression.playerId(), impression) != null)
                throw new IllegalArgumentException("Duplicate player impression");
        }
        if (state.episodes().size() > MAX_EPISODES) throw new IllegalArgumentException("Too many episodes");
        if (state.nextEpisodeId() < 0) throw new IllegalArgumentException("Invalid episode counter");
        Set<Long> ids = new HashSet<>();
        long next = state.nextEpisodeId();
        for (Episode episode : state.episodes()) {
            if (episode == null || !ids.add(episode.id())) throw new IllegalArgumentException("Invalid episode");
            if (state.schemaVersion() < 8 && episode.kind() == Episode.Kind.OBSERVED_FARM)
                throw new IllegalArgumentException("Farm memories require schema 8");
            next = Math.max(next, episode.id() + 1);
            episodes.addLast(episode);
        }
        nextEpisodeId = next;
        if (observedFarms().size() > MAX_FARMS) throw new IllegalArgumentException("Too many remembered farms");
    }

    public static AgentMind fromSnapshot(Snapshot state) {
        Objects.requireNonNull(state);
        if (state.schemaVersion() < 1 || state.schemaVersion() > SCHEMA_VERSION)
            throw new IllegalArgumentException("Unsupported mind schema " + state.schemaVersion());
        return new AgentMind(state);
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public Personality personality() { return personality; }
    public Genome genome() { return genome; }
    /** Expressed traits, fixed for life. */
    public Set<Trait> traits() { return traits; }
    public boolean has(Trait trait) { return traits.contains(trait); }

    /** This villager's words, including bounded evidence for meanings still being learned. */
    public Lexicon lexicon() { return lexicon; }
    /** Cultural faction remains tied to origin; travel and foreign-word learning never change membership. */
    public String originBiome() { return originBiome; }
    public String faction() { return originBiome; }
    public void establishOrigin(String biome) {
        local.villagers.language.BiomeDialect.validate(biome);
        if (originBiome == null) originBiome = biome;
    }
    public void lexicon(Lexicon lexicon) { this.lexicon = Objects.requireNonNull(lexicon); }

    /**
     * How strongly a word taught by this player sticks, in [0.05, 1]. Awe and trust make a villager receptive;
     * fear makes it wary. Unfamiliar players are revered, so villagers listen to them from the start.
     */
    public double receptivenessTo(UUID player) {
        PlayerImpression view = playerImpression(player);
        return Math.clamp(0.1 + 0.4 * view.awe() + 0.4 * Math.max(0, view.trust()) - 0.5 * view.fear(), 0.05, 1);
    }

    /** Make an episode known to a player, e.g. because the villager told it and the player understood. */
    public boolean reveal(long episodeId, UUID player) {
        Objects.requireNonNull(player);
        List<Episode> updated = new ArrayList<>();
        boolean changed = false;
        for (Episode episode : episodes) {
            Episode next = episode.id() == episodeId ? episode.withKnower(player) : episode;
            changed |= next != episode;
            updated.add(next);
        }
        if (changed) { episodes.clear(); episodes.addAll(updated); }
        return changed;
    }

    /** The newest episode this player doesn't know yet, or null. */
    public Episode untoldTo(UUID player) {
        Episode newest = null;
        for (Episode episode : episodes) if (!episode.isKnownBy(player)) newest = episode;
        return newest;
    }

    /**
     * Hearing a neighbour's view of a player. Only views with some evidence behind them are passed on, and the
     * weight follows trust in the teller, so a village gradually shares a reputation.
     */
    public void hearAboutPlayer(UUID teller, PlayerImpression told) {
        if (told.evidenceCount() == 0) return;
        double weight = Opinion.HEARSAY_WEIGHT * Math.max(0, opinionOf(teller).trust() + 0.3);
        PlayerImpression current = playerImpression(told.playerId());
        PlayerImpression changed = current.hearsay(told, weight);
        if (changed.equals(current)) return;
        playerImpressions.remove(told.playerId());
        if (playerImpressions.size() == MAX_PLAYER_IMPRESSIONS)
            playerImpressions.remove(playerImpressions.keySet().iterator().next());
        playerImpressions.put(told.playerId(), changed);
    }

    /** Opinions of other villagers. */
    public Opinions opinions() { return opinions; }
    public Opinion opinionOf(UUID villager) { return opinions.of(villager); }
    public void witness(UUID villager, Opinion.Event event, long tick) {
        if (villager.equals(id)) return;
        opinions = opinions.witness(villager, event, tick);
        switch (event) {
            case PROTECTED -> experience(Experience.Event.RESCUED);
            case CHATTED -> experience(Experience.Event.CHATTED);
            case REFUSED -> experience(Experience.Event.REFUSED);
            case SHARED_FOOD -> { }
        }
    }

    /** Inherited tendencies as shifted by experience: what actually drives choices. */
    public Personality expressedPersonality() {
        return new Personality(personality.boldness() + experience.boldness(),
                personality.sociability() + experience.sociability(), personality.curiosity() + experience.curiosity());
    }
    public Experience experience() { return experience; }
    public void experience(Experience.Event event) { experience = experience.after(event); }

    /** Recompute mood from current needs, health and recent memories; call once per think. */
    public Mood updateMood(long now, double health) {
        mood = Mood.compute(hunger(), fatigue, loneliness, health, List.copyOf(episodes), now, traits);
        return mood;
    }
    public Mood mood() { return mood; }
    /** Being told {@code teller}'s view of {@code about}; weighted by trust in the teller. */
    public void hearOpinion(UUID teller, Opinion told, long tick) {
        if (told.other().equals(id) || teller.equals(id)) return;
        opinions = opinions.hear(told.other(), told.liking(), told.trust(), opinionOf(teller).trust() + 0.3, tick);
    }
    /** The empty part of the food bar: 0 when full, 1 when starving. */
    public double hunger() { return food.hunger(); }
    public int foodLevel() { return food.foodLevel(); }
    public float saturation() { return food.saturation(); }
    public boolean canEat() { return food.canEat(); }
    public boolean canSprint() { return food.canSprint(); }
    /** Simulated ticks lived while loaded. */
    public long ageTicks() { return ageTicks; }
    /** Null for a founder. */
    public Lineage lineage() { return lineage; }
    public int generation() { return lineage == null ? 0 : lineage.generation(); }
    /** Null while alive. */
    public Death death() { return death; }
    public void die(long tick, String cause) { if (death == null) death = new Death(tick, cause); }
    public double fatigue() { return fatigue; }
    public double loneliness() { return loneliness; }
    public Home home() { return home; }
    public void home(Home home) { this.home = home; }
    public long learningUpdates() { return learningUpdates; }
    public List<Memory> memories() { return List.copyOf(memories); }
    /** Memorable events, oldest first. */
    public List<Episode> episodes() { return List.copyOf(episodes); }

    /** Bounded actual observations, not knowledge inherited from a parent or another mind. */
    public List<Episode> observedFarms() {
        return episodes.stream().filter(e -> e.kind() == Episode.Kind.OBSERVED_FARM).toList();
    }

    public boolean observeFarm(Place place, String crop, long now, Collection<UUID> witnesses) {
        Objects.requireNonNull(place);
        new Episode(0,Episode.Kind.OBSERVED_FARM,now,null,place,crop,List.of());
        for (Episode e : observedFarms()) {
            Place p=e.place();
            if (p.dimension().equals(place.dimension()) && Math.abs((long)p.y()-place.y()) <= 2
                    && Math.pow((double)p.x()-place.x(),2)+Math.pow((double)p.z()-place.z(),2) <= 16) return false;
        }
        recordEpisode(Episode.Kind.OBSERVED_FARM,now,null,place,crop,witnesses);
        return true;
    }

    /** Only after a nearby unobstructed view confirms the remembered crop is gone. */
    public boolean forgetFarm(Place place) {
        return episodes.removeIf(e -> e.kind() == Episode.Kind.OBSERVED_FARM && e.place().equals(place));
    }

    /**
     * Record a memorable event. {@code witnesses} are players who shared the experience, nearest first;
     * at most {@link Episode#MAX_KNOWERS} are kept. The oldest episode is forgotten when the list is full.
     */
    public Episode recordEpisode(Episode.Kind kind, long tick, UUID subject, Place place, String detail,
                                 Collection<UUID> witnesses) {
        String bounded = detail == null ? null : detail.substring(0, Math.min(Episode.MAX_DETAIL, detail.length()));
        Episode episode = new Episode(nextEpisodeId, kind, tick, subject, place, bounded,
                Episode.boundedWitnesses(Objects.requireNonNull(witnesses)));
        if (kind == Episode.Kind.OBSERVED_FARM && observedFarms().size() == MAX_FARMS)
            episodes.remove(observedFarms().getFirst());
        if (nextEpisodeId < Long.MAX_VALUE) nextEpisodeId++;
        if (episodes.size() == MAX_EPISODES) episodes.removeFirst();
        episodes.addLast(episode);
        return episode;
    }

    /** Reading an unfamiliar player is pure, so frozen policies cannot accumulate lookup-only evidence. */
    public PlayerImpression playerImpression(UUID playerId) {
        Objects.requireNonNull(playerId);
        PlayerImpression remembered = playerImpressions.get(playerId);
        return remembered == null ? PlayerImpression.prior(playerId) : remembered;
    }

    public List<PlayerImpression> impressions() { return List.copyOf(playerImpressions.values()); }

    /** Evidence must describe an actual witnessed event; the controller gates this for frozen learners. */
    public boolean observePlayer(UUID playerId, PlayerImpression.Evidence evidence, long gameTick) {
        return observePlayer(playerId, evidence, gameTick, List.of());
    }

    /**
     * As above. Harm and protection are also kept as episodes, shared by the player involved and by
     * {@code otherWitnesses} (other players who saw it, nearest first).
     */
    public boolean observePlayer(UUID playerId, PlayerImpression.Evidence evidence, long gameTick,
                                 Collection<UUID> otherWitnesses) {
        PlayerImpression changed = playerImpression(playerId).observe(evidence, gameTick);
        if (changed == null) return false;
        playerImpressions.remove(playerId);
        if (playerImpressions.size() == MAX_PLAYER_IMPRESSIONS)
            playerImpressions.remove(playerImpressions.keySet().iterator().next());
        playerImpressions.put(playerId, changed);
        remember("Player " + playerId + ": " + evidence);
        Episode.Kind kind = switch (evidence) {
            case PLAYER_HURT -> Episode.Kind.HURT_BY_PLAYER;
            case WITNESSED_PROTECTION -> Episode.Kind.PROTECTED_BY_PLAYER;
            case EXPOSURE, USEFUL_GIFT -> null; // Routine, or recorded as RECEIVED_ITEM by the gift handler.
        };
        if (evidence == PlayerImpression.Evidence.PLAYER_HURT) experience(Experience.Event.HURT);
        if (evidence == PlayerImpression.Evidence.WITNESSED_PROTECTION) experience(Experience.Event.RESCUED);
        if (kind != null) {
            List<UUID> witnesses = new ArrayList<>();
            witnesses.add(playerId);
            witnesses.addAll(otherWitnesses);
            recordEpisode(kind, gameTick, playerId, null, null, witnesses);
        }
        return true;
    }

    /** Only call for simulated loaded time; unloaded agents do not accumulate hidden starvation. */
    public void advanceDrives(long elapsedTicks) { advanceDrives(elapsedTicks, DEFAULT_BASE_EXHAUSTION_PER_DAY); }

    /** As above, burning {@code baseExhaustionPerDay} (scaled by {@link #hungerRate()}) as food exhaustion. */
    public void advanceDrives(long elapsedTicks, double baseExhaustionPerDay) {
        if (elapsedTicks < 0) throw new IllegalArgumentException("Time cannot run backwards");
        double ticks = Math.min(elapsedTicks, 24_000L);
        if (ageTicks < Long.MAX_VALUE - elapsedTicks) ageTicks += elapsedTicks;
        food.exert((float) (Math.max(0, baseExhaustionPerDay) * hungerRate() * ticks / 24_000.0));
        fatigue = Math.min(1, fatigue + fatigueRate() * ticks / 32_000.0);
        loneliness = Math.min(1, loneliness + lonelinessRate() * ticks / 28_000.0);
    }
    /** Appetite scales hunger between 0.85x and 1.15x; gluttons get hungry 30% faster, frugal villagers 20% slower. */
    public double hungerRate() {
        double rate = 0.85 + 0.3 * genome.tendency(Tendency.APPETITE);
        return rate * (has(Trait.GLUTTON) ? 1.3 : has(Trait.FRUGAL) ? 0.8 : 1);
    }
    public double fatigueRate() { return has(Trait.DILIGENT) ? 0.85 : has(Trait.LAZY) ? 1.2 : 1; }
    public double lonelinessRate() { return has(Trait.GOSSIP) ? 1.3 : has(Trait.LONER) ? 0.5 : 1; }

    /** Eat food with the item's own nutrition and saturation, as a player would. */
    public void eat(int nutrition, float saturation) { food.eat(nutrition, saturation); }
    /** Exhaustion from real activity: running, swimming, attacking or being hurt, at the player's costs. */
    public void exert(float exhaustion) { food.exert(exhaustion); }
    /** The player's regeneration and starvation timers; see {@link Metabolism#tick}. */
    public Metabolism.Vitals metabolize(int ticks, float health, float maxHealth, boolean naturalRegeneration) {
        return food.tick(ticks, health, maxHealth, naturalRegeneration);
    }
    public void rest() { fatigue = Math.max(0, fatigue - 0.12); }
    public void socialize() { loneliness = Math.max(0, loneliness - 0.2); }

    public boolean allowed(Action action, Observation observation) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(observation);
        if (observation.danger()) return action == Action.FLEE;
        return switch (action) {
            case FLEE -> false;
            // Like a player, a villager with a full food bar cannot eat.
            case FORAGE -> observation.foodAvailable() && observation.hunger() > 0;
            case REST -> true; // Resting in place remains possible without an assigned bed.
            case SOCIALIZE -> !observation.night() && observation.health() > 0.3 && observation.hunger() < 0.9;
            // A hungry agent without known food must still be able to search for it.
            // Cowards stay in when hurt; the brave also wander at night while healthy, at their own risk.
            case EXPLORE -> has(Trait.BRAVE)
                    ? observation.health() > 0.3 && (!observation.night() || observation.health() > 0.6)
                    : !observation.night() && observation.health() > (has(Trait.COWARD) ? 0.6 : 0.3);
        };
    }

    public Action choose(Observation observation, RandomGenerator random) {
        Objects.requireNonNull(random);
        if (observation.danger()) return Action.FLEE;
        List<Action> candidates = Arrays.stream(Action.values()).filter(a -> allowed(a, observation)).toList();
        if (random.nextDouble() < 0.04) return candidates.get(random.nextInt(candidates.size()));
        int context = context(observation);
        Action best = Action.REST;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Action action : candidates) {
            double score = utility(action, observation) + 4 * values[context][action.ordinal()];
            if (score > bestScore) {
                best = action;
                bestScore = score;
            }
        }
        return best;
    }

    private double utility(Action action, Observation o) {
        Personality personality = expressedPersonality();
        // Low spirits make a villager sulk at home rather than seek company; good spirits do the opposite.
        double sulk = switch (mood.band()) { case MISERABLE -> 0.35; case UNHAPPY -> 0.15; default -> 0; };
        double cheer = mood.band() == Mood.Band.HAPPY ? 0.1 : 0;
        return switch (action) {
            case FORAGE -> 0.15 + 1.1 * o.hunger() + (has(Trait.GLUTTON) ? 0.15 : 0);
            case REST -> 0.1 + 0.9 * o.fatigue() + (o.night() ? 0.4 : 0) + (o.homeAvailable() ? 0.05 : 0)
                    + (has(Trait.LAZY) ? 0.25 : has(Trait.DILIGENT) ? -0.1 : 0) + sulk;
            case SOCIALIZE -> 0.1 + 0.65 * o.loneliness() + 0.2 * personality.sociability()
                    + (has(Trait.GOSSIP) ? 0.25 : has(Trait.LONER) ? -0.25 : 0) - sulk + cheer;
            case EXPLORE -> 0.05 + 0.2 * personality.curiosity() + 0.1 * (personality.boldness() - 0.5)
                    - 0.3 * o.hunger() - 0.2 * o.fatigue() + (o.foodAvailable() ? 0 : 2 * o.hunger())
                    + (has(Trait.BRAVE) ? 0.15 : has(Trait.COWARD) ? -0.15 : 0);
            case FLEE -> 10;
        };
    }

    /** Submit once per actual completed or failed attempt, never once per tick of a pending action. */
    public void learn(Observation before, Action action, Observation after) {
        Objects.requireNonNull(after);
        if (!allowed(action, before) || action == Action.FLEE || before.danger()) return;
        // Each action receives only its relevant need delta. Ambient relief of another need cannot reward it.
        double relief = switch (action) {
            case FORAGE -> before.hunger() - after.hunger();
            case REST -> before.fatigue() - after.fatigue();
            case SOCIALIZE -> before.loneliness() - after.loneliness();
            case EXPLORE, FLEE -> 0;
        };
        double reward = 2 * relief + 2 * Math.min(0, after.health() - before.health());
        if (action != Action.EXPLORE && relief <= 0) reward -= 0.15;
        reward = Math.clamp(reward, -1, 1);
        int context = context(before);
        int slot = action.ordinal();
        values[context][slot] = Math.clamp(values[context][slot] + 0.25 * (reward - values[context][slot]), -1, 1);
        if (visits[context][slot] < Integer.MAX_VALUE) visits[context][slot]++;
        if (learningUpdates < Long.MAX_VALUE) learningUpdates++;
        if (memories.size() == MEMORY_LIMIT) memories.removeFirst();
        memories.addLast(new Memory(learningUpdates, action, context, reward, "Observed outcome of " + action));
    }

    public void remember(String note) {
        Objects.requireNonNull(note);
        String bounded = note.substring(0, Math.min(256, note.length()));
        if (memories.size() == MEMORY_LIMIT) memories.removeFirst();
        memories.addLast(new Memory(learningUpdates, null, -1, 0, bounded));
    }

    private static int context(Observation o) {
        return (o.hunger() >= 0.5 ? 1 : 0) | (o.fatigue() >= 0.5 ? 2 : 0)
                | (o.loneliness() >= 0.5 ? 4 : 0) | (o.night() ? 8 : 0) | (o.health() <= 0.3 ? 16 : 0);
    }

    public Snapshot snapshot() {
        return new Snapshot(SCHEMA_VERSION, id, name, personality, hunger(), fatigue, loneliness, home,
                learningUpdates, values, visits, memories(), impressions(), episodes(), nextEpisodeId, genome,
                food.state(), ageTicks, lineage, death, lexicon, opinions, experience, nextBreedingAt, originBiome);
    }
}
