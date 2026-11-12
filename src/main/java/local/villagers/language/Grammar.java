package local.villagers.language;

import java.util.*;
import java.util.function.Function;

/** Bounded cultural word-order evidence. Observations describe visible events, never speaker metadata. */
public record Grammar(List<Integer> evidence, Order speakingOrder, List<Construction> constructions, ReceptiveGrammar receptive) {
    public Grammar(List<Integer> evidence,Order speakingOrder,List<Construction> constructions){this(evidence,speakingOrder,constructions,ReceptiveGrammar.empty());}
    public Grammar(List<Integer> evidence) { this(evidence, null, List.of()); }
    public Grammar(List<Integer> evidence, Order speakingOrder) { this(evidence, speakingOrder, List.of()); }
    public enum Role { SUBJECT, VERB, OBJECT }
    public enum Order {
        SVO(Role.SUBJECT, Role.VERB, Role.OBJECT), SOV(Role.SUBJECT, Role.OBJECT, Role.VERB),
        VSO(Role.VERB, Role.SUBJECT, Role.OBJECT), VOS(Role.VERB, Role.OBJECT, Role.SUBJECT),
        OVS(Role.OBJECT, Role.VERB, Role.SUBJECT), OSV(Role.OBJECT, Role.SUBJECT, Role.VERB);
        private final List<Role> roles;
        Order(Role... roles) { this.roles = List.of(roles); }
        public List<Role> roles() { return roles; }
    }
    public Grammar {
        evidence = List.copyOf(Objects.requireNonNull(evidence));
        if (evidence.size() != Order.values().length || evidence.stream().anyMatch(n -> n < 0 || n > 1000))
            throw new IllegalArgumentException("Invalid grammar evidence");
        if (speakingOrder == null) {
            speakingOrder = Order.SVO;
            for (Order order : Order.values())
                if (evidence.get(order.ordinal()) > evidence.get(speakingOrder.ordinal())) speakingOrder = order;
        }
        for (int count : evidence) if (count > evidence.get(speakingOrder.ordinal()))
            throw new IllegalArgumentException("Speaking grammar must have maximal evidence");
        receptive = receptive==null?ReceptiveGrammar.empty():receptive;
        constructions = constructions == null ? List.of() : List.copyOf(constructions);
        if (constructions.size() > Construction.MAX_PATTERNS) throw new IllegalArgumentException("Too many constructions");
        for (int i=0;i<constructions.size();i++) for (int j=0;j<i;j++)
            if (constructions.get(i).samePattern(constructions.get(j))) throw new IllegalArgumentException("Duplicate construction");
    }
    public static Grammar empty() { return new Grammar(Collections.nCopies(6, 0)); }
    public static Grammar founding(Order order) { return empty().reinforce(order, 6); }
    public boolean initialized() { return evidence.stream().anyMatch(n -> n > 0); }
    public Order preferred() {
        return speakingOrder;
    }
    public double confidence() {
        int total = evidence.stream().mapToInt(Integer::intValue).sum();
        return total == 0 ? 0 : evidence.get(preferred().ordinal()) / (double) total;
    }
    public Grammar reinforce(Order order, int amount) {
        if (amount < 1 || amount > 6) throw new IllegalArgumentException("Invalid grammar lesson");
        var next = new ArrayList<>(evidence);
        if (next.get(order.ordinal()) + amount > 1000) next.replaceAll(n -> n / 2);
        next.set(order.ordinal(), next.get(order.ordinal()) + amount);
        // Equal evidence retains the speaker's established convention, rather than favoring English order.
        Order preferred = speakingOrder;
        if (next.get(order.ordinal()) > next.get(preferred.ordinal())) preferred = order;
        return new Grammar(next, preferred, constructions,receptive);
    }
    public Grammar learn(Construction observation) {
        var next = new ArrayList<Construction>(); boolean found = false;
        boolean decay = constructions.stream().anyMatch(c -> c.samePattern(observation) && c.evidence()==1000);
        for (Construction recorded : constructions) {
            Construction old = decay ? new Construction(recorded.slots(),recorded.implicitVerb(),Math.max(1,recorded.evidence()/2)) : recorded;
            if (old.samePattern(observation)) {
                next.add(new Construction(old.slots(),old.implicitVerb(),old.evidence()+1)); found = true;
            } else next.add(old);
        }
        if (!found) {
            if (next.size() == Construction.MAX_PATTERNS)
                next.remove(next.stream().min(Comparator.comparingInt(Construction::evidence)).orElseThrow());
            next.add(observation);
        }
        return new Grammar(evidence,speakingOrder,next,receptive);
    }
    public List<Role> production(Clause clause) {
        return constructions.stream().filter(c -> c.evidence() >= Construction.MIN_EVIDENCE && c.evidence() > evidence.get(speakingOrder.ordinal())
                        && (c.implicitVerb() == null || c.implicitVerb() == clause.verb()))
                .max(Comparator.comparingInt(Construction::evidence)).map(Construction::slots).orElse(preferred().roles());
    }
    /** Hearing builds comprehension, not a new native speaking order. The first grounded order can seed an uninitialized learner. */
    public Grammar observe(List<String> sounds, Function<String, Concept> decode, Clause observed) {
        var pattern=ReceptiveGrammar.infer(sounds,decode,observed);if(pattern==null)return this;
        var next=receptive.observe(sounds,decode,observed);var production=this;
        if(!initialized()&&pattern.slots().size()==3)for(var order:Order.values())if(order.roles().equals(pattern.slots()))production=reinforce(order,1);
        return new Grammar(production.evidence,production.speakingOrder,production.constructions,next);
    }
    /** Only actually producing a grounded form reinforces one's own production. */
    public Grammar rehearse(List<String> sounds,Function<String,Concept> decode,Clause observed){
        var pattern=ReceptiveGrammar.infer(sounds,decode,observed);if(pattern==null)return this;
        if(pattern.slots().size()==3)for(var order:Order.values())if(order.roles().equals(pattern.slots()))return reinforce(order,1);
        return learn(pattern);
    }
    /** Same-language variants can spread without making receptive foreign grammar replace one's native order. */
    public Grammar accommodate(List<String> sounds,Function<String,Concept> decode,Clause observed,Function<Concept,String> preferredWord){
        if(sounds.size()==3||preferredWord==null)return this;
        var pattern=ReceptiveGrammar.infer(sounds,decode,observed);if(pattern==null)return this;
        for(var sound:sounds){var meaning=decode.apply(sound);if(meaning==null||!sound.equals(preferredWord.apply(meaning)))return this;}
        var unique=pattern.slots().stream().distinct().toList();
        var nativeRoles=preferred().roles().stream().filter(unique::contains).toList();
        return unique.equals(nativeRoles)?learn(pattern):this;
    }
    public List<ReceptiveGrammar.Reading> readings(List<String> sounds,Function<String,Concept> decode,Function<Concept,String> preferredWord){
        var result=new ArrayList<>(receptive.readings(sounds,decode));int scope=0;
        if(preferredWord!=null)for(var cue:ReceptiveGrammar.cues(sounds,decode))if(cue.word().equals(preferredWord.apply(cue.meaning())))scope+=cue.weight();
        if(initialized())result.add(new ReceptiveGrammar.Reading(preferred().roles(),null,scope,confidence()));
        for(var c:constructions)if(c.evidence()>=Construction.MIN_EVIDENCE)result.add(new ReceptiveGrammar.Reading(c.slots(),c.implicitVerb(),scope,1));
        return List.copyOf(result);
    }
}
