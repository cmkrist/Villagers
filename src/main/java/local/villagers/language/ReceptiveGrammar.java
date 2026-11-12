package local.villagers.language;

import java.util.*;
import java.util.function.Function;

/** Acoustic dialect profiles. No biome, speaker ID or intended meaning can select a reading. */
public record ReceptiveGrammar(List<Dialect> dialects) {
    public static final int MAX_DIALECTS=8,MAX_CUES=24;
    public record Cue(String word,Concept meaning){
        public Cue {if(!Words.valid(word))throw new IllegalArgumentException("Invalid grammatical cue");Objects.requireNonNull(meaning);if(Clause.marker(meaning))throw new IllegalArgumentException("Marker is not a dialect cue");}
        public int weight(){return switch(meaning){case I,YOU,HERE,IS,SEE,SAY,GIVE,WANT,HAVE,NAMED,ASK,PROMISE,MAKE,PAY,FROM,KNOW,LIKE,MEET,TRADE -> 3;default -> 1;};}
    }
    public record Dialect(List<Cue> cues,List<Construction> patterns){
        public Dialect {
            cues=List.copyOf(cues);patterns=List.copyOf(patterns);
            if(cues.isEmpty()||cues.size()>MAX_CUES||new HashSet<>(cues).size()!=cues.size()||patterns.isEmpty()||patterns.size()>Construction.MAX_PATTERNS)throw new IllegalArgumentException("Invalid receptive dialect");
            for(int i=0;i<patterns.size();i++)for(int j=0;j<i;j++)if(patterns.get(i).samePattern(patterns.get(j)))throw new IllegalArgumentException("Duplicate receptive pattern");
        }
        public int score(List<Cue> heard){return heard.stream().filter(cues::contains).mapToInt(Cue::weight).sum();}
        public int evidence(){return patterns.stream().mapToInt(Construction::evidence).sum();}
        Dialect observe(List<Cue> heard,Construction pattern){
            var words=new ArrayList<>(cues);for(var cue:heard)if(!words.contains(cue))words.add(cue);
            while(words.size()>MAX_CUES){int weight=words.stream().mapToInt(Cue::weight).min().orElseThrow();words.remove(words.stream().filter(c->c.weight()==weight).findFirst().orElseThrow());}
            var next=new ArrayList<Construction>();boolean found=false;
            boolean decay=patterns.stream().anyMatch(p->p.samePattern(pattern)&&p.evidence()==1000);
            for(var p:patterns){int count=decay?Math.max(1,p.evidence()/2):p.evidence();if(p.samePattern(pattern)){count++;found=true;}next.add(new Construction(p.slots(),p.implicitVerb(),count));}
            if(!found){if(next.size()==Construction.MAX_PATTERNS)next.remove(next.stream().min(Comparator.comparingInt(Construction::evidence)).orElseThrow());next.add(pattern);}
            return new Dialect(words,next);
        }
    }
    public record Reading(List<Grammar.Role> slots,Concept implicitVerb,int scope,double confidence) {}
    public ReceptiveGrammar {dialects=List.copyOf(Objects.requireNonNull(dialects));if(dialects.size()>MAX_DIALECTS)throw new IllegalArgumentException("Too many receptive dialects");}
    public static ReceptiveGrammar empty(){return new ReceptiveGrammar(List.of());}
    public static List<Cue> cues(List<String> sounds,Function<String,Concept> decode){
        var cues=new LinkedHashSet<Cue>();for(var sound:sounds){var c=decode.apply(sound);if(c!=null&&!Clause.marker(c))cues.add(new Cue(sound,c));}return List.copyOf(cues);
    }
    /** Unique public event-role alignment only; surface patterns are not supplied by a speaker. */
    public static Construction infer(List<String> sounds,Function<String,Concept> decode,Clause observed){
        if(observed==null||observed.past()||observed.negated())return null;
        if(sounds.size()!=3)return Construction.infer(sounds,decode,observed);
        Grammar.Order match=null;
        for(var order:Grammar.Order.values()){
            boolean same=true;for(int i=0;i<3;i++)if(decode.apply(sounds.get(i))!=observed.at(order.roles().get(i)))same=false;
            if(same){if(match!=null)return null;match=order;}
        }
        return match==null?null:new Construction(match.roles(),null,1);
    }
    public ReceptiveGrammar observe(List<String> sounds,Function<String,Concept> decode,Clause observed){
        var pattern=infer(sounds,decode,observed);if(pattern==null)return this;
        var heard=cues(sounds,decode);int best=0,index=-1;boolean tied=false;
        for(int i=0;i<dialects.size();i++){int score=dialects.get(i).score(heard);if(score>best){best=score;index=i;tied=false;}else if(score==best&&score>0)tied=true;}
        // Shared ambiguous cues cannot silently merge two languages into one.
        if(tied)return this;
        var next=new ArrayList<>(dialects);
        if(best>=3)next.set(index,next.get(index).observe(heard,pattern));
        else {if(next.size()==MAX_DIALECTS)next.remove(next.stream().min(Comparator.comparingInt(Dialect::evidence)).orElseThrow());next.add(new Dialect(heard,List.of(pattern)));}
        return new ReceptiveGrammar(next);
    }
    public List<Reading> readings(List<String> sounds,Function<String,Concept> decode){
        var heard=cues(sounds,decode);var result=new ArrayList<Reading>();
        for(var dialect:dialects){int scope=dialect.score(heard);if(scope<3)continue;
            for(var pattern:dialect.patterns())if(pattern.evidence()>=Construction.MIN_EVIDENCE){
                int total=dialect.patterns().stream().filter(p->p.slots().size()==pattern.slots().size()).mapToInt(Construction::evidence).sum();
                result.add(new Reading(pattern.slots(),pattern.implicitVerb(),scope,pattern.evidence()/(double)total));
            }
        }
        return List.copyOf(result);
    }
}
