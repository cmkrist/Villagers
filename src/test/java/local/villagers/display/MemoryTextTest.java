package local.villagers.display;

import java.util.List;
import java.util.UUID;
import local.villagers.mind.Episode;
import local.villagers.mind.Place;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MemoryTextTest {
    private static final UUID ALEX = new UUID(1, 1), SAM = new UUID(2, 2);

    private static Episode gift(List<UUID> knownBy) {
        return new Episode(0, Episode.Kind.RECEIVED_ITEM, 48_000, ALEX, null, "minecraft:iron_hoe", knownBy);
    }

    @Test void survivalPlayersOnlySeeSharedMemories() {
        Episode episode = gift(List.of(ALEX));
        assertEquals("Day 3: You gave me iron hoe", MemoryText.line(episode, ALEX, false, id -> "Alex"));
        assertEquals(MemoryText.UNKNOWN, MemoryText.line(episode, SAM, false, id -> "Alex"));
        assertEquals(MemoryText.UNKNOWN, MemoryText.line(episode, null, false, id -> "Alex"));
    }

    @Test void omniscientViewersSeeEverythingWithOtherPlayersNamed() {
        Episode episode = gift(List.of(ALEX));
        assertEquals("Day 3: Alex gave me iron hoe", MemoryText.line(episode, SAM, true, id -> "Alex"));
        assertEquals("Day 3: A player gave me iron hoe", MemoryText.line(episode, SAM, true, id -> null), "offline players stay anonymous");
    }

    @Test void describesEveryKind() {
        Episode bed = new Episode(1, Episode.Kind.CLAIMED_BED, 0, null, new Place("minecraft:overworld", 4, 65, -2), null, List.of());
        assertEquals("Day 1: I claimed a bed at 4 65 -2", MemoryText.line(bed, SAM, true, id -> null));
        Episode hurt = new Episode(2, Episode.Kind.HURT_BY_PLAYER, 0, SAM, null, null, List.of(SAM));
        assertEquals("Day 1: You hurt me", MemoryText.line(hurt, SAM, false, id -> null));
        Episode saved = new Episode(3, Episode.Kind.PROTECTED_BY_PLAYER, 0, ALEX, null, null, List.of(ALEX, SAM));
        assertEquals("Day 1: Alex saved me from a monster", MemoryText.line(saved, SAM, false, id -> "Alex"));
    }
}
