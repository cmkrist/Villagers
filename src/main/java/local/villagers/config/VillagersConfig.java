package local.villagers.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Settings from {@code config/villagers.json}. A missing file is created with defaults. An unreadable or invalid
 * file is never overwritten: defaults are used and the problem is reported. Out-of-range values are clamped, and
 * settings missing from an older file take their defaults. Every setting is read by a feature; see {@link Setting}.
 */
public record VillagersConfig(int memoryWitnessRadius, int baseExhaustionPerDay, boolean godChildren,
                              int hearingRadius, int speechCooldownSeconds,
                              int tradeProposalsPerVillagerPerDay, int tradeProposalsPerVillagePerDay,
                              boolean breeding, int outsideVillageChunkRadius, int maxResidentChunks, boolean hayForaging, int traderRecruitmentChancePercent, boolean nativeCellEscape, boolean nativeAdventureQuarry, boolean nativeCampPassages, boolean nativeCampMeals, boolean nativeCampFarming) {
    public static final int DEFAULT_WITNESS_RADIUS = 16;
    public static final int MIN_WITNESS_RADIUS = 1;
    public static final int MAX_WITNESS_RADIUS = 64;
    /** Food exhaustion a villager burns per loaded day besides activity; 4.0 exhaustion is one food point. */
    public static final int DEFAULT_BASE_EXHAUSTION = 80;
    public static final int MIN_BASE_EXHAUSTION = 0;
    public static final int MAX_BASE_EXHAUSTION = 400;
    public static final boolean DEFAULT_GOD_CHILDREN = true;

    /** Every setting: its JSON key, default, allowed range (for numbers), and accessor. */
    public enum Setting {
        /** Blocks within which a player, with line of sight, shares a villager's experience. */
        MEMORY_WITNESS_RADIUS("memoryWitnessRadius", DEFAULT_WITNESS_RADIUS, MIN_WITNESS_RADIUS, MAX_WITNESS_RADIUS, VillagersConfig::memoryWitnessRadius),
        BASE_EXHAUSTION_PER_DAY("baseExhaustionPerDay", DEFAULT_BASE_EXHAUSTION, MIN_BASE_EXHAUSTION, MAX_BASE_EXHAUSTION, VillagersConfig::baseExhaustionPerDay),
        TRADER_RECRUITMENT_CHANCE("traderRecruitmentChancePercent", 20, 0, 100, VillagersConfig::traderRecruitmentChancePercent),
        GOD_CHILDREN("godChildren", DEFAULT_GOD_CHILDREN, VillagersConfig::godChildren),
        /** Blocks within which players hear villager speech. */
        HEARING_RADIUS("hearingRadius", 12, 2, 48, VillagersConfig::hearingRadius),
        /** Minimum time between a villager's unprompted remarks. Replies and thanks are not limited. */
        SPEECH_COOLDOWN_SECONDS("speechCooldownSeconds", 45, 5, 600, VillagersConfig::speechCooldownSeconds),
        /** How many times a day one villager may approach a player to propose a trade. */
        TRADE_PROPOSALS_PER_VILLAGER_PER_DAY("tradeProposalsPerVillagerPerDay", 1, 0, 24, VillagersConfig::tradeProposalsPerVillagerPerDay),
        /** How many trade proposals a whole village may make to one player per day. */
        TRADE_PROPOSALS_PER_VILLAGE_PER_DAY("tradeProposalsPerVillagePerDay", 3, 0, 100, VillagersConfig::tradeProposalsPerVillagePerDay),
        /** Whether adopted villagers may have children. */
        BREEDING("breeding", true, VillagersConfig::breeding),
        /** Explicit opt-in: consume visible exposed existing hay as a finite grain supply. */
        HAY_FORAGING("hayForaging", false, VillagersConfig::hayForaging),
        /** Experimental: dismantle native barracks cell doors when hungry. Captors may attack outside. */
        NATIVE_CELL_ESCAPE("nativeCellEscape", false, VillagersConfig::nativeCellEscape),
        NATIVE_ADVENTURE_QUARRY("nativeAdventureQuarry", false, VillagersConfig::nativeAdventureQuarry),
        NATIVE_CAMP_PASSAGES("nativeCampPassages", false, VillagersConfig::nativeCampPassages),
        NATIVE_CAMP_MEALS("nativeCampMeals", false, VillagersConfig::nativeCampMeals),
        NATIVE_CAMP_FARMING("nativeCampFarming", false, VillagersConfig::nativeCampFarming),
        /** Chunk radius kept loaded around each adopted villager outside its village (0 = its own chunk only). */
        OUTSIDE_VILLAGE_CHUNK_RADIUS("outsideVillageChunkRadius", 1, 0, 4, VillagersConfig::outsideVillageChunkRadius),
        /** Hard cap on chunks this mod keeps loaded; 0 disables chunk residency. */
        MAX_RESIDENT_CHUNKS("maxResidentChunks", 256, 0, 4096, VillagersConfig::maxResidentChunks);

        public final String key;
        final Object fallback;
        final int min, max;
        final Function<VillagersConfig, Object> read;

        Setting(String key, int fallback, int min, int max, Function<VillagersConfig, Object> read) {
            this.key = key; this.fallback = fallback; this.min = min; this.max = max; this.read = read;
        }

        Setting(String key, boolean fallback, Function<VillagersConfig, Object> read) {
            this.key = key; this.fallback = fallback; this.min = 0; this.max = 0; this.read = read;
        }

        boolean isBoolean() { return fallback instanceof Boolean; }

        int clamp(int value) { return Math.clamp(value, min, max); }
    }

    public static VillagersConfig defaults() { return from(new EnumMap<>(Setting.class)); }

    public VillagersConfig {
        traderRecruitmentChancePercent = Setting.TRADER_RECRUITMENT_CHANCE.clamp(traderRecruitmentChancePercent);
        memoryWitnessRadius = Setting.MEMORY_WITNESS_RADIUS.clamp(memoryWitnessRadius);
        baseExhaustionPerDay = Setting.BASE_EXHAUSTION_PER_DAY.clamp(baseExhaustionPerDay);
        hearingRadius = Setting.HEARING_RADIUS.clamp(hearingRadius);
        speechCooldownSeconds = Setting.SPEECH_COOLDOWN_SECONDS.clamp(speechCooldownSeconds);
        tradeProposalsPerVillagerPerDay = Setting.TRADE_PROPOSALS_PER_VILLAGER_PER_DAY.clamp(tradeProposalsPerVillagerPerDay);
        tradeProposalsPerVillagePerDay = Setting.TRADE_PROPOSALS_PER_VILLAGE_PER_DAY.clamp(tradeProposalsPerVillagePerDay);
        outsideVillageChunkRadius = Setting.OUTSIDE_VILLAGE_CHUNK_RADIUS.clamp(outsideVillageChunkRadius);
        maxResidentChunks = Setting.MAX_RESIDENT_CHUNKS.clamp(maxResidentChunks);
    }

    /** Build from parsed values; missing settings take their defaults. */
    private static VillagersConfig from(Map<Setting, Object> values) {
        Function<Setting, Integer> i = s -> (Integer) values.getOrDefault(s, s.fallback);
        Function<Setting, Boolean> b = s -> (Boolean) values.getOrDefault(s, s.fallback);
        return new VillagersConfig(i.apply(Setting.MEMORY_WITNESS_RADIUS), i.apply(Setting.BASE_EXHAUSTION_PER_DAY),
                b.apply(Setting.GOD_CHILDREN), i.apply(Setting.HEARING_RADIUS), i.apply(Setting.SPEECH_COOLDOWN_SECONDS),
                i.apply(Setting.TRADE_PROPOSALS_PER_VILLAGER_PER_DAY), i.apply(Setting.TRADE_PROPOSALS_PER_VILLAGE_PER_DAY),
                b.apply(Setting.BREEDING), i.apply(Setting.OUTSIDE_VILLAGE_CHUNK_RADIUS), i.apply(Setting.MAX_RESIDENT_CHUNKS),
                b.apply(Setting.HAY_FORAGING), i.apply(Setting.TRADER_RECRUITMENT_CHANCE), b.apply(Setting.NATIVE_CELL_ESCAPE), b.apply(Setting.NATIVE_ADVENTURE_QUARRY), b.apply(Setting.NATIVE_CAMP_PASSAGES), b.apply(Setting.NATIVE_CAMP_MEALS), b.apply(Setting.NATIVE_CAMP_FARMING));
    }

    /** The loaded settings plus human-readable problems to log, if any. */
    public record Loaded(VillagersConfig config, List<String> problems) {
        public Loaded { problems = List.copyOf(problems); }
    }

    public static Loaded load(Path file) {
        List<String> problems = new ArrayList<>();
        if (!Files.exists(file)) {
            try {
                Files.createDirectories(file.toAbsolutePath().getParent());
                Files.writeString(file, toJson(defaults()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                problems.add("Could not write default config " + file + ": " + e.getMessage());
            }
            return new Loaded(defaults(), problems);
        }
        JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new IllegalStateException("top level is not an object");
            root = parsed.getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            problems.add("Invalid config " + file + " (" + e.getMessage() + "); using defaults and leaving the file untouched");
            return new Loaded(defaults(), problems);
        }
        Map<Setting, Object> values = new EnumMap<>(Setting.class);
        for (Setting setting : Setting.values()) {
            JsonElement value = root.get(setting.key);
            if (value == null) continue;
            if (setting.isBoolean()) {
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) values.put(setting, value.getAsBoolean());
                else problems.add(setting.key + " must be true or false; using " + setting.fallback);
                continue;
            }
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber() || value.getAsDouble() != Math.rint(value.getAsDouble())) {
                problems.add(setting.key + " must be a whole number; using " + setting.fallback);
                continue;
            }
            double number = value.getAsDouble();
            int clamped = (int) Math.clamp(number, setting.min, setting.max);
            if (number != clamped)
                problems.add(setting.key + " must be between " + setting.min + " and " + setting.max + "; using " + clamped);
            values.put(setting, clamped);
        }
        return new Loaded(from(values), problems);
    }

    static String toJson(VillagersConfig config) {
        JsonObject root = new JsonObject();
        for (Setting setting : Setting.values()) {
            Object value = setting.read.apply(config);
            if (value instanceof Boolean flag) root.addProperty(setting.key, flag);
            else root.addProperty(setting.key, (Integer) value);
        }
        return new GsonBuilder().setPrettyPrinting().create().toJson(root) + System.lineSeparator();
    }
}
