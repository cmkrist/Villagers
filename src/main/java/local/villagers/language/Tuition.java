package local.villagers.language;

import java.util.*;
import java.util.random.RandomGenerator;

/** Deliberate lessons operate on heard words, the learner's dictionary and independently perceived objects. */
public final class Tuition {
    private Tuition() {}
    public record Seen(UUID target,Set<Concept> features){public Seen{Objects.requireNonNull(target);features=Set.copyOf(features);if(features.isEmpty())throw new IllegalArgumentException("No observable feature");}}
    public record Answer(UUID pointedAt,Concept meaning,List<Clause> words){}
    public static List<Clause> demonstrate(Concept topic){return List.of(new Clause(Concept.I,Concept.SEE,topic));}
    public static List<Clause> question(Concept topic){return List.of(new Clause(Concept.I,Concept.WANT,topic));}
    /** This receives all publicly apparent properties, not the teacher's selected meaning. */
    public static Lexicon observe(Lexicon learner,List<String> sounds,Set<Concept> features,RandomGenerator random){
        var context=EnumSet.noneOf(Concept.class);context.addAll(features);context.add(Concept.I);context.add(Concept.SEE);
        return LanguageLearning.hear(learner,sounds,context,null,random);
    }
    /** The child chooses an actual visible object using its own decoded request; unknown requests earn no response. */
    public static Answer answer(List<String> question,Lexicon own,List<Seen> visible){
        if(visible.size()>8)throw new IllegalArgumentException("Unbounded lesson choice");
        var parsed=Sentence.interpret(question,own);if(!parsed.complete()||parsed.clauses().size()!=1)return null;
        var c=parsed.clauses().getFirst();if(c.subject()!=Concept.I||c.verb()!=Concept.WANT||c.past()||c.negated()||c.objectReference()!=null)return null;
        String word=own.wordFor(c.object());if(word==null||own.meaning(word)!=c.object())return null;
        var target=visible.stream().filter(v->v.features().contains(c.object())).findFirst().orElse(null);if(target==null)return null;
        return new Answer(target.target(),c.object(),List.of(new Clause(Concept.I,Concept.SEE,c.object())));
    }
    /** Teacher evaluates its own request, its own heard reply and the publicly pointed object. No student dictionary. */
    public static boolean confirms(Concept requested,Sentence.Interpretation reply,Seen pointed){
        return pointed!=null&&pointed.features().contains(requested)&&reply.complete()
                &&reply.clauses().equals(List.of(new Clause(Concept.I,Concept.SEE,requested)));
    }
}
