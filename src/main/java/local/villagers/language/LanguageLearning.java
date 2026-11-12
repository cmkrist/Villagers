package local.villagers.language;

import java.util.*;
import java.util.random.RandomGenerator;

/** Acoustic learning boundary: only sounds and independently observable candidate meanings enter. */
public final class LanguageLearning {
    private LanguageLearning() {}
    public static Lexicon hear(Lexicon listener, List<String> sounds, Set<Concept> visible, Clause event, RandomGenerator random) {
        var words = sounds.stream().filter(s -> !s.equals(";")).toList();
        Lexicon next = listener.observe(words, visible, random);
        next=next.withFormation(next.formation().hear(words,next::rootWords,next::atomicMeaning,visible));
        Clause observed=event==null?visibleEvent(sounds,next::meaning,visible):event;
        return next.withGrammar(next.grammar().observe(sounds,next::meaning,observed).accommodate(sounds,next::meaning,observed,next::wordFor));
    }
    /** A listener may resolve one visible property through its own known words. Private wants are excluded. */
    private static Clause visibleEvent(List<String> sounds,java.util.function.Function<String,Concept> decode,Set<Concept> visible){
        Clause found=null;
        for(var feature:visible){
            if(feature==Concept.WANT||feature==Concept.TRADE||feature==Concept.I||feature==Concept.YOU||feature==Concept.PAST||feature==Concept.NOT)continue;
            Clause candidate;try{candidate=new Clause(Concept.I,feature==Concept.HELLO?Concept.SAY:feature==Concept.HURT||feature==Concept.WALK?Concept.IS:Concept.SEE,feature);}catch(IllegalArgumentException invalidRole){continue;}
            if(ReceptiveGrammar.infer(sounds,decode,candidate)!=null){if(found!=null)return null;found=candidate;}
        }
        return found;
    }
    public static Glossary hear(Glossary listener, List<String> sounds, Set<Concept> visible, Clause event) {
        Glossary next = listener;
        Set<Concept> unassigned = EnumSet.noneOf(Concept.class);
        unassigned.addAll(visible);
        for (String sound : sounds) {
            Concept known = listener.translate(sound);
            if (known != null) unassigned.remove(known);
        }
        for (String word : sounds) if (!word.equals(";") && !listener.referents().hasWord(word)) {
            Concept familiar = listener.translate(word);
            if (familiar != null && !visible.contains(familiar)) continue;
            Set<Concept> candidates = familiar != null ? Set.of(familiar) : unassigned.isEmpty() ? visible : unassigned;
            next = next.hear(word, candidates);
        }
        next=next.withFormation(next.formation().hear(sounds,next::rootWords,next::atomicTranslate,visible));
        Clause observed=event==null?visibleEvent(sounds,next::translate,visible):event;
        return next.withGrammar(next.grammar().observe(sounds, next::translate, observed));
    }
}
