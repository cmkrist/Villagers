package local.villagers.language;

import java.util.*;

/** Interpretation-driven turn decisions. No speaker intention or private entity state enters this policy. */
public final class Dialogue {
    private Dialogue() {}
    public enum Response { FOOD_REQUEST, INVITATION, PROMISE, REFUSAL, CLARIFY, OTHER }
    public static Response response(Sentence.Interpretation heard) {
        if (!heard.complete()) return Response.CLARIFY;
        for (Clause c : heard.clauses()) {
            if (c.negated() || c.past() || c.subject() != Concept.I) continue;
            if (c.verb() == Concept.SAY && c.object() == Concept.NO) return Response.REFUSAL;
            if (c.verb() == Concept.WANT && c.object() == Concept.FOOD) return Response.FOOD_REQUEST;
            if (c.verb() == Concept.WANT && c.object() == Concept.FRIEND) return Response.INVITATION;
            if (c.verb() == Concept.PROMISE && c.object() == Concept.FRIEND) return Response.PROMISE;
        }
        return Response.OTHER;
    }
    /** Repeat back the listener's own interpretation so the original speaker can confirm or deny it. */
    public static List<Clause> confirmation(Sentence.Interpretation heard) {
        if (!heard.complete() || heard.clauses().size() != 1) return List.of(new Clause(Concept.I, Concept.ASK, Concept.AGAIN));
        Clause c = heard.clauses().getFirst();
        if (c.subject() != Concept.I) return List.of(new Clause(Concept.I, Concept.ASK, Concept.AGAIN));
        return List.of(new Clause(Concept.YOU, c.verb(), c.object(), c.negated(), c.past(),null,c.objectReference()));
    }
    public static boolean confirms(Sentence.Interpretation echo, List<Clause> ownIntention) {
        if (!echo.complete() || ownIntention.size() != 1 || echo.clauses().size() != 1) return false;
        Clause mine = ownIntention.getFirst(), repeated = echo.clauses().getFirst();
        return repeated.subject() == Concept.YOU && mine.subject() == Concept.I && mine.verb() == repeated.verb()
                && mine.object() == repeated.object() && mine.past() == repeated.past() && mine.negated() == repeated.negated()
                && Objects.equals(mine.objectReference()==null?null:mine.objectReference().target(),
                        repeated.objectReference()==null?null:repeated.objectReference().target());
    }
    /** Each participant has one bounded repair/relationship attention budget; survival signals have separate budgets. */
    public static final class Budget {
        public static final long INTERVAL = 200;
        private long next;
        public boolean ready(long now) { return now>=next && now>=0 && now<=Long.MAX_VALUE-INTERVAL; }
        public boolean take(long now) {
            if (!ready(now)) return false;
            next = now + INTERVAL; return true;
        }
    }
}
