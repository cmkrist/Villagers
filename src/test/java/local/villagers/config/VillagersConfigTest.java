package local.villagers.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class VillagersConfigTest {
    @TempDir Path root;

    @Test void missingFileIsCreatedWithDefaults() throws Exception {
        Path file = root.resolve("config/villagers.json");
        var loaded = VillagersConfig.load(file);
        assertEquals(VillagersConfig.defaults(), loaded.config());
        assertTrue(loaded.problems().isEmpty());
        assertTrue(Files.readString(file).contains("\"memoryWitnessRadius\": 16"));
        assertEquals(VillagersConfig.defaults(), VillagersConfig.load(file).config(), "the written file reads back");
    }

    @Test void readsValidSettings() throws Exception {
        Path file = Files.writeString(root.resolve("villagers.json"), "{\"memoryWitnessRadius\": 24}");
        var loaded = VillagersConfig.load(file);
        assertEquals(24, loaded.config().memoryWitnessRadius());
        assertTrue(loaded.problems().isEmpty());
    }

    @Test void readsHungerSettings() throws Exception {
        Path file = Files.writeString(root.resolve("villagers.json"),
                "{\"baseExhaustionPerDay\": 0, \"godChildren\": false}");
        var loaded = VillagersConfig.load(file);
        assertEquals(0, loaded.config().baseExhaustionPerDay());
        assertFalse(loaded.config().godChildren());
        assertTrue(loaded.problems().isEmpty());
        var wrong = VillagersConfig.load(Files.writeString(root.resolve("bad.json"), "{\"godChildren\": \"yes\"}"));
        assertTrue(wrong.config().godChildren());
        assertEquals(1, wrong.problems().size());
    }

    @Test void consumingExistingHayRequiresExplicitBooleanOptIn() throws Exception {
        assertFalse(VillagersConfig.defaults().hayForaging());
        Path file = Files.writeString(root.resolve("hay.json"), "{}");
        assertFalse(VillagersConfig.load(file).config().hayForaging(), "old configs preserve the crop-only terrain policy");
        Files.writeString(file, "{\"hayForaging\": true}");
        assertTrue(VillagersConfig.load(file).config().hayForaging());
        Files.writeString(file, "{\"hayForaging\": \"true\"}");
        var invalid = VillagersConfig.load(file);
        assertFalse(invalid.config().hayForaging());
        assertEquals(1, invalid.problems().size());
    }

    @Test void recruitmentChanceSupportsOldConfigsAndDisabledOrCertainRolls() throws Exception {
        Path file = Files.writeString(root.resolve("traders.json"), "{}");
        assertEquals(20, VillagersConfig.load(file).config().traderRecruitmentChancePercent());
        for (int chance : new int[]{0, 100}) {
            Files.writeString(file, "{\"traderRecruitmentChancePercent\":" + chance + "}");
            assertEquals(chance, VillagersConfig.load(file).config().traderRecruitmentChancePercent());
        }
        Files.writeString(file, "{\"traderRecruitmentChancePercent\":101}");
        assertEquals(100, VillagersConfig.load(file).config().traderRecruitmentChancePercent());
        Files.writeString(file, "{\"traderRecruitmentChancePercent\":\"always\"}");
        assertEquals(20, VillagersConfig.load(file).config().traderRecruitmentChancePercent());
    }

    @Test void invalidFileIsLeftUntouchedAndDefaultsAreUsed() throws Exception {
        String broken = "{ memoryWitnessRadius: oops";
        Path file = Files.writeString(root.resolve("villagers.json"), broken);
        var loaded = VillagersConfig.load(file);
        assertEquals(VillagersConfig.defaults(), loaded.config());
        assertEquals(1, loaded.problems().size());
        assertEquals(broken, Files.readString(file));
    }

    @Test void outOfRangeAndWrongTypesAreReportedAndCorrected() throws Exception {
        Path file = Files.writeString(root.resolve("villagers.json"), "{\"memoryWitnessRadius\": 500}");
        var loaded = VillagersConfig.load(file);
        assertEquals(VillagersConfig.MAX_WITNESS_RADIUS, loaded.config().memoryWitnessRadius());
        assertEquals(1, loaded.problems().size());
        Files.writeString(file, "{\"memoryWitnessRadius\": \"far\"}");
        assertEquals(VillagersConfig.DEFAULT_WITNESS_RADIUS, VillagersConfig.load(file).config().memoryWitnessRadius());
        Files.writeString(file, "{\"memoryWitnessRadius\": 2.5}");
        assertEquals(VillagersConfig.DEFAULT_WITNESS_RADIUS, VillagersConfig.load(file).config().memoryWitnessRadius());
    }
}
