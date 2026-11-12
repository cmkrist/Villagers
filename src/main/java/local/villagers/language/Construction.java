package local.villagers.language;

import java.util.*;
import java.util.function.Function;

/** A learned surface pattern, induced from grounded events rather than a fixed menu of whole sentences. */
public record Construction(List<Grammar.Role> slots, Concept implicitVerb, int evidence) {
    public static final int MIN_EVIDENCE = 3, MAX_PATTERNS = 8;
    public Construction {
        slots = List.copyOf(Objects.requireNonNull(slots));
        if (slots.size() < 2 || slots.size() > 5 || !slots.contains(Grammar.Role.SUBJECT) || !slots.contains(Grammar.Role.OBJECT)
                || (slots.contains(Grammar.Role.VERB) ? implicitVerb != null : implicitVerb != Concept.IS)
                || evidence < 1 || evidence > 1000) throw new IllegalArgumentException("Invalid learned construction");
    }
    public boolean samePattern(Construction other) { return slots.equals(other.slots) && implicitVerb == other.implicitVerb; }
    public static Construction infer(List<String> sounds, Function<String,Concept> decode, Clause event) {
        if (event == null || event.past() || event.negated() || sounds.size() < 2 || sounds.size() > 5 || sounds.size() == 3) return null;
        var roles = new ArrayList<Grammar.Role>();
        for (String sound : sounds) {
            Concept c = decode.apply(sound); Grammar.Role role = null;
            if (c == null) return null;
            for (Grammar.Role candidate : Grammar.Role.values()) if (event.at(candidate) == c) {
                if (role != null) return null; role = candidate;
            }
            if (role == null) return null;
            roles.add(role);
        }
        try { return new Construction(roles, roles.contains(Grammar.Role.VERB) ? null : event.verb(), 1); }
        catch (IllegalArgumentException ambiguous) { return null; }
    }
    public Clause decode(List<Concept> core, boolean negated, boolean past) {
        if (evidence < MIN_EVIDENCE || core.size() != slots.size()) return null;
        var values = new EnumMap<Grammar.Role,Concept>(Grammar.Role.class);
        if (implicitVerb != null) values.put(Grammar.Role.VERB, implicitVerb);
        for (int i=0;i<core.size();i++) {
            Concept old = values.putIfAbsent(slots.get(i),core.get(i));
            if (old != null && old != core.get(i)) return null;
        }
        try { return new Clause(values.get(Grammar.Role.SUBJECT),values.get(Grammar.Role.VERB),values.get(Grammar.Role.OBJECT),negated,past); }
        catch (IllegalArgumentException | NullPointerException invalid) { return null; }
    }
}
