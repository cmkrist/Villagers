package local.villagers.language;

import java.util.*;

/** Meaning owned by a speaker, or independently reconstructed by a listener. No private world references. */
public record Clause(Concept subject, Concept verb, Concept object, boolean negated, boolean past,
                     Referents.Reference subjectReference,Referents.Reference objectReference) {
    private static final Set<Concept> VERBS = Set.of(Concept.WANT, Concept.IS, Concept.SEE, Concept.SAY, Concept.GIVE, Concept.TRADE, Concept.HAVE, Concept.LIKE, Concept.ASK, Concept.PROMISE, Concept.MEET,Concept.NAMED,Concept.FROM,Concept.KNOW,Concept.MAKE,Concept.PAY);
    public Clause {
        Objects.requireNonNull(subject); Objects.requireNonNull(object);
        if (!VERBS.contains(verb) || marker(subject) || marker(object) || VERBS.contains(subject) || (VERBS.contains(object) && object != Concept.TRADE))
            throw new IllegalArgumentException("Invalid clause roles");
        if(subjectReference!=null&&subjectReference.target().concept()!=subject || objectReference!=null&&objectReference.target().concept()!=object)
            throw new IllegalArgumentException("Reference kind does not match role");
    }
    public Clause(Concept subject,Concept verb,Concept object,boolean negated,boolean past){this(subject,verb,object,negated,past,null,null);}
    public Clause(Concept subject, Concept verb, Concept object) { this(subject, verb, object, false, false); }
    public static boolean marker(Concept c) { return c == Concept.PAST || c == Concept.NOT; }
    public Concept at(Grammar.Role role) {
        return switch (role) { case SUBJECT -> subject; case VERB -> verb; case OBJECT -> object; };
    }
    public Referents.Reference referenceAt(Grammar.Role role){return role==Grammar.Role.SUBJECT?subjectReference:role==Grammar.Role.OBJECT?objectReference:null;}
    public static Clause named(Referents.Reference subject,Concept verb,Concept object){return new Clause(subject.target().concept(),verb,object,false,false,subject,null);}
    public List<Concept> concepts() {
        var result = new ArrayList<>(List.of(subject, verb, object));
        if (negated) result.add(Concept.NOT);
        if (past) result.add(Concept.PAST);
        return List.copyOf(result);
    }
    /** Adapter for existing care/trade callers. A missing object never silently completes a request. */
    public static List<Clause> fromConcepts(List<Concept> concepts) {
        if (concepts.isEmpty() || concepts.size() > 8) throw new IllegalArgumentException("Invalid phrase length");
        var result = new ArrayList<Clause>();
        for (int i = 0; i < concepts.size(); i++) {
            Concept c = concepts.get(i);
            if (c == Concept.WANT) {
                if (++i == concepts.size()) return List.of();
                result.add(new Clause(Concept.I, c, concepts.get(i)));
            } else {
                Concept verb = switch (c) {
                    case HUNGRY, TIRED, LONELY, HAPPY, AFRAID, HURT, WALK -> Concept.IS;
                    case HELLO, YES, NO, THANKS -> Concept.SAY;
                    default -> Concept.SEE;
                };
                result.add(new Clause(Concept.I, verb, c));
            }
        }
        return List.copyOf(result);
    }
    /** English is a diagnostic/translation surface assembled from the listener's own interpretation. */
    public String english() {
        String who = subjectReference!=null?subjectReference.word():subject == Concept.I ? "I" : subject == Concept.YOU ? "You" : subject==Concept.HERE?"Here":"The " + subject.gloss();
        String action = switch (verb) {
            case IS -> past ? (subject == Concept.YOU ? "were" : "was") : subject == Concept.I ? "am" : subject == Concept.YOU ? "are" : "is";
            case WANT -> past ? "wanted" : subject == Concept.I || subject == Concept.YOU ? "want" : "wants";
            case SEE -> past ? "saw" : subject == Concept.I || subject == Concept.YOU ? "see" : "sees";
            case SAY -> past ? "said" : subject == Concept.I || subject == Concept.YOU ? "say" : "says";
            case GIVE -> past ? "gave" : subject == Concept.I || subject == Concept.YOU ? "give" : "gives";
            case HAVE -> past ? "had" : subject == Concept.I || subject == Concept.YOU ? "have" : "has";
            case LIKE -> past ? "liked" : subject == Concept.I || subject == Concept.YOU ? "like" : "likes";
            case ASK -> past ? "asked" : subject == Concept.I || subject == Concept.YOU ? "ask" : "asks";
            case PROMISE -> past ? "promised" : subject == Concept.I || subject == Concept.YOU ? "promise" : "promises";
            case MEET -> past ? "met" : subject == Concept.I || subject == Concept.YOU ? "meet" : "meets";
            case MAKE -> past?"made":subject==Concept.I||subject==Concept.YOU?"make":"makes";
            case PAY -> past?"paid":subject==Concept.I||subject==Concept.YOU?"pay":"pays";
            case FROM -> past ? "was told by" : "comes from";
            case KNOW -> past ? "knew" : subject == Concept.I || subject == Concept.YOU ? "know" : "knows";
            case NAMED -> past?"was called":subject==Concept.I?"am called":subject==Concept.YOU?"are called":"is called";
            case TRADE -> past ? "traded" : subject == Concept.I || subject == Concept.YOU ? "trade" : "trades";
            default -> throw new IllegalStateException();
        };
        if (negated) action = verb == Concept.IS ? action + " not" : (past ? "did not " : subject == Concept.I || subject == Concept.YOU ? "do not " : "does not ") + verb.gloss();
        String what = object == Concept.I ? "me" : object == Concept.YOU ? "you" : object.gloss();
        if (verb == Concept.IS && object == Concept.WALK) what = "walking";
        if (Set.of(Concept.BED, Concept.FRIEND, Concept.MONSTER, Concept.CARROT, Concept.POTATO, Concept.BEETROOT,
                Concept.HOE, Concept.SWORD,Concept.WOODEN_HOE,Concept.STONE_HOE,Concept.WOODEN_SWORD,Concept.STONE_SWORD).contains(object)) what = "a " + what;
        if (object == Concept.EMERALD) what = "an emerald";
        if(objectReference!=null)what=objectReference.word();
        return who + " " + action + " " + what + ".";
    }
}
