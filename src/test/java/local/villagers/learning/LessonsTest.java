package local.villagers.learning;

import java.util.*;
import local.villagers.language.Concept;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LessonsTest {
    @Test void historyRequiresObservedDemonstrationsAndDoesNotConfuseChildren(){
        var a=UUID.randomUUID();var b=UUID.randomUUID();var l=Lessons.empty();assertEquals(l,l.confirm(a,Concept.HOE,1));
        l=l.demonstrate(a,Concept.HOE,2);assertFalse(l.confirmed(a,Concept.HOE));l=l.confirm(a,Concept.HOE,3);
        assertTrue(l.confirmed(a,Concept.HOE));assertFalse(l.confirmed(b,Concept.HOE));assertEquals(l,l.confirm(a,Concept.HOE,4));
        assertThrows(IllegalArgumentException.class,()->new Lessons.Entry(a,Concept.HOE,1,2,5));
    }
    @Test void historyIsBoundedAcrossManyChildren(){var l=Lessons.empty();for(int i=0;i<100;i++)l=l.demonstrate(new UUID(7,i),Concept.WOOD,i);assertEquals(Lessons.MAX_ENTRIES,l.entries().size());assertNull(l.of(new UUID(7,0),Concept.WOOD));}
}
