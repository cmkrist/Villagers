package local.villagers.language;

import java.util.*;

/** Explicit relationship speech acts. Only a complete independent parse can recognize a commitment. */
public final class RelationshipDialogue {
    private RelationshipDialogue() {}
    public enum Act { PROPOSE_PARTNER, PROMISE_PARTNER, WITHDRAW_PARTNER, PROPOSE_CHILD, PROMISE_CHILD, REFUSE, OTHER }
    public static List<Clause> words(Act act){return switch(act){
        case PROPOSE_PARTNER, PROPOSE_CHILD -> List.of(new Clause(Concept.I,Concept.WANT,act==Act.PROPOSE_PARTNER?Concept.PARTNER:Concept.CHILD),new Clause(Concept.I,Concept.ASK,Concept.YOU));
        case PROMISE_PARTNER, PROMISE_CHILD -> List.of(new Clause(Concept.I,Concept.PROMISE,act==Act.PROMISE_PARTNER?Concept.PARTNER:Concept.CHILD),new Clause(Concept.I,Concept.SAY,Concept.YES));
        case WITHDRAW_PARTNER -> List.of(new Clause(Concept.I,Concept.PROMISE,Concept.PARTNER,true,false));
        case REFUSE -> List.of(new Clause(Concept.I,Concept.SAY,Concept.NO));
        case OTHER -> throw new IllegalArgumentException("No authored relationship act");
    };}
    public static Act act(Sentence.Interpretation heard){
        if(heard.complete())for(var a:Act.values())if(a!=Act.OTHER&&heard.clauses().equals(words(a)))return a;
        return Act.OTHER;
    }
    public static String english(Sentence.Interpretation heard){return switch(act(heard)){
        case PROPOSE_PARTNER -> "Will you be my partner?";case PROMISE_PARTNER -> "I agree to be your partner.";
        case WITHDRAW_PARTNER -> "I am ending our partnership.";case PROPOSE_CHILD -> "Would you like to have a child with me?";
        case PROMISE_CHILD -> "I agree to try for a child with you.";case REFUSE, OTHER -> null;
    };}
}
