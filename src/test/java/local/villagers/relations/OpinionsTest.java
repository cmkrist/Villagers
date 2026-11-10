package local.villagers.relations;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import local.villagers.genetics.Trait;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpinionsTest {
    private static final UUID A = new UUID(1, 1), B = new UUID(2, 2), C = new UUID(3, 3);

    @Test void witnessedKindnessBuildsRegardAndRefusalsErodeIt() {
        Opinions opinions = Opinions.empty().witness(A, Opinion.Event.SHARED_FOOD, 10).witness(A, Opinion.Event.PROTECTED, 20);
        assertTrue(opinions.of(A).regard() > 0.3);
        assertEquals(2, opinions.of(A).witnessed());
        Opinions refused = Opinions.empty();
        for (int i = 0; i < 5; i++) refused = refused.witness(B, Opinion.Event.REFUSED, i);
        assertTrue(refused.of(B).regard() < -0.3);
    }

    @Test void hearsayMovesLessThanWitnessingAndDistrustedTellersAreIgnored() {
        Opinion heard = Opinion.neutral(C).hear(1, 1, 1, 5);
        assertEquals(Opinion.HEARSAY_WEIGHT, heard.liking(), 1e-9);
        assertEquals(1, heard.hearsay());
        assertEquals(0, heard.witnessed());
        assertEquals(Opinion.neutral(C), Opinion.neutral(C).hear(1, 1, -0.5, 5), "a distrusted teller changes nothing");
    }

    @Test void lookupsArePureAndTheLeastEngagedAcquaintanceIsForgotten() {
        Opinions opinions = Opinions.empty();
        opinions.of(A);
        assertTrue(opinions.known().isEmpty());
        opinions = opinions.witness(A, Opinion.Event.CHATTED, 0).witness(A, Opinion.Event.CHATTED, 1);
        for (int i = 0; i < Opinions.MAX_KNOWN + 5; i++) opinions = opinions.witness(new UUID(9, i), Opinion.Event.CHATTED, 100 + i);
        assertEquals(Opinions.MAX_KNOWN, opinions.known().size());
        assertEquals(2, opinions.of(A).witnessed(), "a closer friend outlasts passing acquaintances");
    }

    @Test void favouritesAreTheBestRegardedPresentVillagers() {
        Opinions opinions = Opinions.empty().witness(A, Opinion.Event.CHATTED, 0)
                .witness(B, Opinion.Event.SHARED_FOOD, 0).witness(C, Opinion.Event.REFUSED, 0);
        assertEquals(B, opinions.favourite(Set.of(A, B, C)).orElseThrow());
        assertEquals(A, opinions.favourite(Set.of(A, C)).orElseThrow());
        assertTrue(opinions.favourite(Set.of(C)).isEmpty(), "nobody disliked is a favourite");
    }

    @Test void gossipsTalkMostAndPassOnStrongFeelings() {
        assertTrue(Gossip.chance(Set.of(Trait.GOSSIP)) > Gossip.chance(Set.of()));
        assertTrue(Gossip.chance(Set.of(Trait.LONER)) < Gossip.chance(Set.of()));
        Opinions opinions = Opinions.empty().witness(A, Opinion.Event.CHATTED, 0).witness(B, Opinion.Event.PROTECTED, 0);
        Random random = new Random(1);
        int aboutB = 0;
        for (int i = 0; i < 1000; i++) {
            Opinion told = Gossip.pick(opinions.known(), C, random);
            if (told != null && told.other().equals(B)) aboutB++;
        }
        assertEquals(1000, aboutB, "a mild chat isn't worth gossiping about; a rescue is");
        assertNull(Gossip.pick(List.of(opinions.of(B)), B, random), "villagers don't gossip to someone about themselves");
    }
}
