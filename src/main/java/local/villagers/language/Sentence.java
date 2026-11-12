package local.villagers.language;

import java.util.*;
import java.util.function.Function;

/** Wire contains sounds and clause punctuation only. A decoder never receives intended clauses or word order. */
public record Sentence(List<Clause> intended, List<String> sounds) {
    public static final int MAX_CLAUSES = 8, MAX_SOUNDS = 63;
    public Sentence { intended = List.copyOf(intended); sounds = checked(sounds); }
    private static List<String> checked(List<String> sounds) {
        if (sounds.isEmpty() || sounds.size() > MAX_SOUNDS || sounds.stream().anyMatch(s -> !s.equals(";") && !Words.valid(s)))
            throw new IllegalArgumentException("Invalid sentence sounds");
        return List.copyOf(sounds);
    }
    public static Sentence speak(List<Clause> clauses, Lexicon lexicon) {
        return speak(clauses,lexicon,false);
    }
    /** An optional local copula omission is a surface innovation; listeners still must learn its construction. */
    public static Sentence speak(List<Clause> clauses, Lexicon lexicon, boolean innovate) {
        if (clauses.isEmpty() || clauses.size() > MAX_CLAUSES) throw new IllegalArgumentException("Invalid sentence clauses");
        var sounds = new ArrayList<String>();
        for (Clause clause : clauses) {
            if (!sounds.isEmpty()) sounds.add(";");
            if (clause.past()) { String w = lexicon.wordFor(Concept.PAST); if (w == null) return null; sounds.add(w); }
            if (clause.negated()) { String w = lexicon.wordFor(Concept.NOT); if (w == null) return null; sounds.add(w); }
            var roles = lexicon.grammar().production(clause);
            if (innovate && clause.verb() == Concept.IS && !clause.past() && !clause.negated())
                roles = roles.stream().filter(r -> r != Grammar.Role.VERB).toList();
            for (Grammar.Role role : roles) {
                Referents.Reference reference=clause.referenceAt(role);
                if(reference!=null&&!reference.equals(lexicon.referents().resolve(reference.word())))return null;
                String word = reference==null?lexicon.wordFor(clause.at(role)):reference.word();
                if (word == null) return null;
                sounds.add(word);
            }
        }
        return new Sentence(clauses, sounds);
    }
    public record Interpretation(List<Clause> clauses, double confidence, int unknownWords) {
        public Interpretation { clauses = List.copyOf(clauses); }
        public boolean complete() { return unknownWords == 0 && !clauses.isEmpty(); }
        public String english() { String question=PracticalQuestions.english(this);if(question!=null)return question;String relationship=RelationshipDialogue.english(this);if(relationship!=null)return relationship;String work=WorkDialogue.english(this);if(work!=null)return work;return clauses.stream().map(Clause::english).collect(java.util.stream.Collectors.joining(" ")); }
    }
    public static Interpretation interpret(List<String> sounds, Lexicon listener) {
        return interpret(sounds, listener::meaning, listener.grammar(),listener.referents(),listener::wordFor);
    }
    public static Interpretation interpret(List<String> sounds, Function<String, Concept> decode, Grammar grammar) {
        return interpret(sounds,decode,grammar,Referents.empty());
    }
    public static Interpretation interpret(List<String> sounds, Function<String, Concept> decode, Grammar grammar,Referents referents) {
        return interpret(sounds,decode,grammar,referents,null);
    }
    private static Interpretation interpret(List<String> sounds,Function<String,Concept> decode,Grammar grammar,Referents referents,Function<Concept,String> preferredWord){
        checked(sounds);
        var clauses = new ArrayList<Clause>();
        int unknown = 0;double confidence=1;
        var part = new ArrayList<String>();
        for (int i = 0; i <= sounds.size(); i++) {
            if (i < sounds.size() && !sounds.get(i).equals(";")) { part.add(sounds.get(i)); continue; }
            var core = new ArrayList<Concept>();var references=new ArrayList<Referents.Reference>(); boolean past = false, negated = false, invalid = false;
            for (String sound : part) {
                Concept c = decode.apply(sound);
                Referents.Reference reference=referents.resolve(sound);
                if(c!=null&&referents.hasWord(sound)){unknown++;invalid=true;continue;}
                if(reference!=null){core.add(reference.target().concept());references.add(reference);continue;}
                if (c == null) { unknown++; invalid = true; }
                else if (c == Concept.PAST) { if (past || !core.isEmpty()) invalid = true; past = true; }
                else if (c == Concept.NOT) { if (negated || !core.isEmpty()) invalid = true; negated = true; }
                else {core.add(c);references.add(null);}
            }
            if(!invalid){
                var candidates=new LinkedHashMap<Clause,Double>();int bestScope=-1;
                for(var reading:grammar.readings(part,decode,preferredWord)){
                    var pattern=new Construction(reading.slots(),reading.implicitVerb(),Construction.MIN_EVIDENCE);
                    var decoded=pattern.decode(core,negated,past);if(decoded==null)continue;
                    var named=withReferences(decoded,reading.slots(),references);if(named==null)continue;
                    if(reading.scope()>bestScope){bestScope=reading.scope();candidates.clear();}
                    if(reading.scope()==bestScope)candidates.merge(named,reading.confidence(),Math::max);
                }
                if(candidates.size()==1){var chosen=candidates.entrySet().iterator().next();clauses.add(chosen.getKey());confidence=Math.min(confidence,chosen.getValue());}
                else invalid=true;
            }
            if (invalid) unknown++;
            part.clear();
        }
        return new Interpretation(clauses, unknown == 0 ? confidence : 0, unknown);
    }
    private static Clause withReferences(Clause clause,List<Grammar.Role> roles,List<Referents.Reference> references){
        var byRole=new EnumMap<Grammar.Role,Referents.Reference>(Grammar.Role.class);
        for(int i=0;i<roles.size();i++){
            if(byRole.containsKey(roles.get(i))&&!Objects.equals(byRole.get(roles.get(i)),references.get(i)))return null;
            byRole.put(roles.get(i),references.get(i));
        }
        try{return new Clause(clause.subject(),clause.verb(),clause.object(),clause.negated(),clause.past(),
                byRole.get(Grammar.Role.SUBJECT),byRole.get(Grammar.Role.OBJECT));}
        catch(IllegalArgumentException invalid){return null;}
    }
    /** For tests/diagnostics only. Never use correctness to select an NPC's response. */
    public boolean correct(Interpretation heard) { return heard.complete() && intended.equals(heard.clauses()); }
}
