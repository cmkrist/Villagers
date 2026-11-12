package local.villagers;

import java.util.UUID;
import local.villagers.family.Courtship;
import local.villagers.mind.AgentMind;
import local.villagers.relations.Partnerships;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PartnershipFamilyConsentTest {
    @Test void parenthoodRequiresBothPersistentPartnershipAndSeparateCurrentChildConsent(){
        var a=new Session(new AgentMind(new UUID(92,1)),0);var b=new Session(new AgentMind(new UUID(92,2)),0);
        a.courtship=new Courtship.Agreement(b.mind.id(),100,1300);b.courtship=new Courtship.Agreement(a.mind.id(),100,1300);
        assertFalse(VillageFamilies.consented(a,b,100),"An old transient invitation cannot manufacture a saved partnership");
        a.mind.partnerships(a.mind.partnerships().form(b.mind.id(),100));
        assertFalse(VillageFamilies.consented(a,b,100),"A half-save cannot authorize a family action");
        b.mind.partnerships(b.mind.partnerships().form(a.mind.id(),100));
        assertTrue(VillageFamilies.consented(a,b,100));assertFalse(VillageFamilies.consented(a,b,1300));
        a.courtship=null;assertFalse(VillageFamilies.consented(a,b,200),"Partnership alone is not child consent");
        a.courtship=new Courtship.Agreement(b.mind.id(),100,1300);
        a.mind.partnerships(a.mind.partnerships().end(b.mind.id(),200,Partnerships.End.WITHDREW));
        assertFalse(VillageFamilies.consented(a,b,201),"Withdrawal revokes permission even if the partner hasn't understood it");
    }
    @Test void aFutureDatedCommitmentCannotAuthorizePresentFamilyActions(){
        var a=new Session(new AgentMind(new UUID(92,1)),0);var b=new Session(new AgentMind(new UUID(92,2)),0);
        a.mind.partnerships(a.mind.partnerships().form(b.mind.id(),200));b.mind.partnerships(b.mind.partnerships().form(a.mind.id(),200));
        a.courtship=new Courtship.Agreement(b.mind.id(),100,1300);b.courtship=new Courtship.Agreement(a.mind.id(),100,1300);
        assertFalse(VillageFamilies.consented(a,b,100));
    }
}
