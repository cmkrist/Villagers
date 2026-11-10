package local.villagers.relations;

import local.villagers.language.Concept;
import local.villagers.language.Lexicon;
import local.villagers.language.Utterance;

/** A hungry actor's local request; no peer inventory or private needs are consulted. */
public final class FoodRequest {
    public static final long COOLDOWN = 600;
    public static final int SEARCH_RADIUS = 16, MAX_PEERS = 16;
    private FoodRequest() {}

    public static boolean due(long now, long nextRequest, double ownHunger, boolean glutton) {
        return now >= 0 && now <= Long.MAX_VALUE - COOLDOWN && now >= nextRequest
                && Double.isFinite(ownHunger) && ownHunger <= 1 && ownHunger >= (glutton ? .35 : .5);
    }

    public static boolean inReach(double distanceSquared, double hearingRadius, boolean visible) {
        return visible && Double.isFinite(distanceSquared) && distanceSquared >= 0 && distanceSquared <= 9
                && Double.isFinite(hearingRadius) && hearingRadius > 0
                && distanceSquared <= hearingRadius * hearingRadius;
    }
    public static boolean dueForCare(long now,long nextRequest,int food,float health,float maxHealth,boolean glutton){
        return due(now,nextRequest,(20-food)/20.0,glutton)
            || local.villagers.mind.MealPolicy.needsRecoveryFood(food,health,maxHealth)&&due(now,nextRequest,1,glutton);
    }
    /** A voice can carry farther than a hand; hearing never authorizes a remote transfer. */
    public static boolean audible(double distanceSquared,double hearingRadius,boolean visible){
        return visible&&Double.isFinite(distanceSquared)&&distanceSquared>=0&&Double.isFinite(hearingRadius)
            &&hearingRadius>0&&distanceSquared<=Math.min(SEARCH_RADIUS,hearingRadius)*Math.min(SEARCH_RADIUS,hearingRadius);
    }

    /** Finding a person does not reveal their inventory or willingness. */
    public static boolean mayApproach(double distanceSquared, boolean visible, boolean withinHome) {
        return visible && withinHome && Double.isFinite(distanceSquared) && distanceSquared >= 0
                && distanceSquared <= SEARCH_RADIUS * SEARCH_RADIUS;
    }

    /** Require the complete actual phrase and the listener's decoded meanings, not hidden concept labels. */
    public static boolean understands(Utterance words, Lexicon listener, Concept... expected) {
        if (words == null || listener == null || expected.length == 0 || words.words().size() != expected.length) return false;
        for (int i = 0; i < expected.length; i++) {
            var spoken = words.words().get(i);
            if (spoken.concept() != expected[i] || listener.meaning(spoken.word()) != expected[i]) return false;
        }
        return true;
    }
}
