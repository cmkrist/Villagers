package local.villagers.family;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CourtshipTest {
    @Test void seekingVisibleCompanyUsesPublicAppearanceAndOwnRegardWithoutGrantingAgreement() {
        assertTrue(Courtship.mayApproach(24 * 24, true, true, true, 0));
        assertFalse(Courtship.mayApproach(24 * 24 + .01, true, true, true, 1));
        assertFalse(Courtship.mayApproach(9, false, true, true, 1));
        assertFalse(Courtship.mayApproach(9, true, false, true, 1));
        assertFalse(Courtship.mayApproach(9, true, true, false, 1));
        assertFalse(Courtship.mayApproach(9, true, true, true, -.01));
        assertFalse(Courtship.mayApproach(Double.NaN, true, true, true, 1));
        assertFalse(Courtship.mutual(new UUID(10, 1), null, new UUID(10, 2), null, 100));
    }
    @Test void aBirthRequiresBothSidesOfTheSameLiveAgreement() {
        UUID a = new UUID(8, 1), b = new UUID(8, 2);
        var first = new Courtship.Agreement(b, 100, 1300);
        var second = new Courtship.Agreement(a, 100, 1300);
        assertTrue(Courtship.mutual(a, first, b, second, 100));
        assertTrue(Courtship.mutual(a, first, b, second, 1299));
        assertFalse(Courtship.mutual(a, first, b, second, 1300));
        assertFalse(Courtship.mutual(a, first, b, second, 99));
        assertFalse(Courtship.mutual(a, first, b, null, 200));
        assertFalse(Courtship.mutual(a, null, b, second, 200));
    }

    @Test void aDifferentPartnerOrDifferentConversationCannotAuthorizeABirth() {
        UUID a = new UUID(8, 1), b = new UUID(8, 2), c = new UUID(8, 3);
        var first = new Courtship.Agreement(b, 100, 1300);
        assertFalse(Courtship.mutual(a, first, b, new Courtship.Agreement(c, 100, 1300), 200));
        assertFalse(Courtship.mutual(a, first, b, new Courtship.Agreement(a, 101, 1300), 200));
        assertFalse(Courtship.mutual(a, first, b, new Courtship.Agreement(a, 100, 1299), 200));
        assertFalse(Courtship.mutual(a, new Courtship.Agreement(a, 100, 1300), a,
                new Courtship.Agreement(a, 100, 1300), 200));
    }
    private static final UUID A = new UUID(1, 1), B = new UUID(2, 2), C = new UUID(3, 3);
    @Test void consentNeedsLocalUnderstoodReadinessAndIsNotImpliedByAnOffer() {
        var offer = Courtship.Offer.at(A, 100);
        assertTrue(Courtship.mayAccept(offer, B, null, 101, true, true, true, .1, false, true));
        assertFalse(Courtship.mayAccept(offer, B, null, 101, false, true, true, .1, false, true));
        assertFalse(Courtship.mayAccept(offer, B, null, 101, true, false, true, .1, false, true));
        assertFalse(Courtship.mayAccept(offer, B, null, 101, true, true, false, .1, false, true));
        assertFalse(Courtship.mayAccept(offer, B, null, 101, true, true, true, .1, false, false));
        assertFalse(Courtship.mayAccept(offer, B, null, 101, true, true, true, -.1, false, true));
    }
    @Test void anExpiredOfferOrRewoundClockCannotAuthorizeAMeeting() {
        var offer = Courtship.Offer.at(A, 100);
        assertFalse(Courtship.mayAccept(offer, B, null, 99, true, true, true, 1, false, true));
        assertFalse(Courtship.mayAccept(offer, B, null, 1300, true, true, true, 1, false, true));
        var agreed = new Courtship.Agreement(A, 100, 1300);
        assertNull(Courtship.preferred(agreed, Set.of(A), 1300));
        assertNull(Courtship.preferred(agreed, Set.of(A), 99));
    }
    @Test void agreementDoesNotRevealPartnersOutsideLocalSight() {
        var agreed = new Courtship.Agreement(A, 100, 1300);
        assertEquals(A, Courtship.preferred(agreed, Set.of(A, C), 101));
        assertNull(Courtship.preferred(agreed, Set.of(C), 101));
    }
    @Test void aCurrentAgreementCannotBeReplacedByAThirdOffer() {
        var offer = Courtship.Offer.at(C, 100);
        var agreed = new Courtship.Agreement(A, 100, 1300);
        assertFalse(Courtship.mayAccept(offer, B, agreed, 101, true, true, true, 1, false, true));
        assertFalse(Courtship.mayAccept(Courtship.Offer.at(B, 100), B, null, 101, true, true, true, 1, false, true));
    }
    @Test void declaredFamilyIdentityExcludesKinWithoutPrivateMindLookup() {
        var offer = Courtship.Offer.at(A, 100, Set.of(C));
        assertTrue(Courtship.related(offer, B, Set.of(C)));
        assertTrue(Courtship.related(offer, C, Set.of()));
        assertFalse(Courtship.related(offer, B, Set.of()));
        assertFalse(Courtship.mayAccept(offer, B, null, 101, true, true, true, 1, true, true));
        assertThrows(IllegalArgumentException.class, () -> Courtship.Offer.at(A, 100, Set.of(A)));
        assertThrows(IllegalArgumentException.class, () -> new Courtship.Agreement(A, 100, 1400));
    }
}
