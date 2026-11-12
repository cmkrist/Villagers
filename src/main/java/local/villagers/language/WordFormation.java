package local.villagers.language;

import java.util.*;
import java.util.function.Function;

/** Bounded productive compounds. Roots and conventions are learned; semantic material/tool classes are authored. */
public record WordFormation(Pattern preferred,List<Pattern> patterns) {
    public static final int MIN_EVIDENCE=3,MAX_PATTERNS=4;
    public enum Order { MATERIAL_HEAD, HEAD_MATERIAL }
    public record Roots(Concept material,Concept head) {}
    public record Pattern(Order order,String linker,int evidence){
        public Pattern{Objects.requireNonNull(order);if(linker==null||linker.length()>2||!linker.chars().allMatch(c->c>='a'&&c<='z')||evidence<1||evidence>1000)throw new IllegalArgumentException("Invalid word-formation convention");}
        public boolean same(Pattern other){return order==other.order&&linker.equals(other.linker);}
        public String form(String material,String head){if(!Words.valid(material)||!Words.valid(head))return null;return order==Order.MATERIAL_HEAD?stem(material)+linker+stem(head):stem(head)+linker+stem(material);}
    }
    public WordFormation{
        patterns=List.copyOf(Objects.requireNonNull(patterns));if(patterns.size()>MAX_PATTERNS)throw new IllegalArgumentException("Too many formation conventions");
        for(int i=0;i<patterns.size();i++)for(int j=0;j<i;j++)if(patterns.get(i).same(patterns.get(j)))throw new IllegalArgumentException("Duplicate formation convention");
        if(preferred!=null&&patterns.stream().noneMatch(p->p.same(preferred)&&p.evidence()>=MIN_EVIDENCE))throw new IllegalArgumentException("Unknown productive convention");
    }
    public static WordFormation empty(){return new WordFormation(null,List.of());}
    public static WordFormation founding(Order order,String linker){var p=new Pattern(order,linker,6);return new WordFormation(p,List.of(p));}
    public static String stem(String word){return word.substring(0,Math.min(4,word.length()));}
    public static Roots roots(Concept concept){return switch(concept){
        case WOODEN_HOE -> new Roots(Concept.WOOD,Concept.HOE);case STONE_HOE -> new Roots(Concept.STONE,Concept.HOE);
        case WOODEN_SWORD -> new Roots(Concept.WOOD,Concept.SWORD);case STONE_SWORD -> new Roots(Concept.STONE,Concept.SWORD);default -> null;
    };}
    public static List<Concept> derived(){return List.of(Concept.WOODEN_HOE,Concept.STONE_HOE,Concept.WOODEN_SWORD,Concept.STONE_SWORD);}
    public static boolean root(Concept concept){return concept==Concept.WOOD||concept==Concept.STONE||concept==Concept.HOE||concept==Concept.SWORD;}
    public String form(Concept concept,Function<Concept,String> knownRoot){
        var r=roots(concept);if(r==null||preferred==null)return null;
        return preferred.form(knownRoot.apply(r.material()),knownRoot.apply(r.head()));
    }
    public Set<Concept> meanings(String word,Function<Concept,List<String>> knownRoots){
        var result=EnumSet.noneOf(Concept.class);if(!Words.valid(word)||word.length()<4||word.length()>10)return Set.of();
        for(var p:patterns)if(p.evidence()>=MIN_EVIDENCE)for(var c:derived()){
            var r=roots(c);for(var a:knownRoots.apply(r.material()))for(var b:knownRoots.apply(r.head()))if(word.equals(p.form(a,b)))result.add(c);
        }
        return Set.copyOf(result);
    }
    private WordFormation observe(Pattern heard){
        var next=new ArrayList<Pattern>();boolean found=false,decay=patterns.stream().anyMatch(p->p.same(heard)&&p.evidence()==1000);
        for(var p:patterns){int n=decay?Math.max(preferred!=null&&p.same(preferred)?MIN_EVIDENCE:1,p.evidence()/2):p.evidence();if(p.same(heard)){n++;found=true;}next.add(new Pattern(p.order(),p.linker(),n));}
        if(!found){if(next.size()==MAX_PATTERNS){var removable=next.stream().filter(p->preferred==null||!p.same(preferred)).min(Comparator.comparingInt(Pattern::evidence)).orElseThrow();next.remove(removable);}next.add(heard);}
        var speaking=preferred;
        if(speaking==null)speaking=next.stream().filter(p->p.evidence()>=MIN_EVIDENCE).findFirst().orElse(null);
        // A stored production convention is independent of how many foreign conventions were heard.
        return new WordFormation(speaking,next);
    }
    /** Only actual public material/tool features and already understood root sounds can teach a convention. */
    public WordFormation hear(List<String> sounds,Function<Concept,List<String>> knownRoots,Function<String,Concept> directMeaning,Set<Concept> observed){
        var possible=new ArrayList<Pattern>();
        for(var word:new LinkedHashSet<>(sounds)){
            if(!Words.valid(word))continue;var known=directMeaning.apply(word);if(known!=null&&roots(known)==null)continue;
            var matches=new LinkedHashMap<String,Pattern>();
            for(var c:observed){var r=roots(c);if(r==null)continue;
                for(var material:knownRoots.apply(r.material()))for(var head:knownRoots.apply(r.head()))for(var order:Order.values()){
                    String first=stem(order==Order.MATERIAL_HEAD?material:head),last=stem(order==Order.MATERIAL_HEAD?head:material);
                    int middle=word.length()-first.length()-last.length();if(middle<0||middle>2||!word.startsWith(first)||!word.endsWith(last))continue;
                    var pattern=new Pattern(order,word.substring(first.length(),word.length()-last.length()),1);matches.put(order+":"+pattern.linker(),pattern);
                }
            }
            if(matches.size()==1)possible.add(matches.values().iterator().next());
        }
        // One utterance counts as one observation, not one vote per repeated word.
        if(possible.isEmpty())return this;var first=possible.getFirst();if(possible.stream().anyMatch(p->!p.same(first)))return this;
        return observe(first);
    }
}
