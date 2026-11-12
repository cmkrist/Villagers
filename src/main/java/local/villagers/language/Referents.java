package local.villagers.language;

import java.util.*;
import java.util.function.Function;
import local.villagers.mind.Place;

/** Learned names, not globally resolvable identifiers. Targets enter only through local observation adapters. */
public record Referents(List<Entry> entries) {
    public static final int MAX_ENTRIES=64, MIN_OBSERVATIONS=3;
    public static final long OBSERVATION_INTERVAL=40;
    public enum Kind { PERSON, PLACE }
    public record Target(Kind kind,UUID person,Place place) {
        public Target {
            Objects.requireNonNull(kind);
            if(kind==Kind.PERSON ? person==null||place!=null : place==null||person!=null)
                throw new IllegalArgumentException("Invalid reference target");
        }
        public static Target person(UUID id){return new Target(Kind.PERSON,id,null);}
        public static Target place(Place place){return new Target(Kind.PLACE,null,place);}
        public Concept concept(){return kind==Kind.PERSON?Concept.PERSON:Concept.PLACE;}
    }
    public record Reference(String word,Target target) {
        public Reference {if(!Words.valid(word))throw new IllegalArgumentException("Invalid name sound");Objects.requireNonNull(target);}
    }
    public record Entry(String word,Target target,int observations,long lastSeen) {
        public Entry {
            new Reference(word,target);
            if(observations<1||observations>1000||lastSeen<0)throw new IllegalArgumentException("Invalid name evidence");
        }
    }
    public Referents {
        entries=List.copyOf(Objects.requireNonNull(entries));
        if(entries.size()>MAX_ENTRIES)throw new IllegalArgumentException("Too many reference bindings");
        var keys=new HashSet<String>();
        for(Entry e:entries)if(!keys.add(e.word()+e.target()))throw new IllegalArgumentException("Duplicate reference binding");
    }
    public static Referents empty(){return new Referents(List.of());}
    public boolean hasWord(String word){return entries.stream().anyMatch(e->e.word().equals(word));}
    public Reference resolve(String word) {
        var candidates=entries.stream().filter(e->e.word().equals(word)).toList();
        if(candidates.size()!=1||candidates.getFirst().observations()<MIN_OBSERVATIONS)return null;
        return new Reference(word,candidates.getFirst().target());
    }
    public Reference nameFor(Target target) {
        return entries.stream().filter(e->e.target().equals(target)&&resolve(e.word())!=null)
                .max(Comparator.comparingInt(Entry::observations).thenComparingLong(Entry::lastSeen))
                .map(e->new Reference(e.word(),target)).orElse(null);
    }
    public Referents observe(String word,Target seen,long now) {return record(word,seen,now,false);}
    /** Creating one's own announced label is different from learning somebody else's identity. */
    public Referents establish(String word,Target personallyObserved,long now){return record(word,personallyObserved,now,true);}
    private Referents record(String word,Target target,long now,boolean establish) {
        new Reference(word,target);if(now<0)throw new IllegalArgumentException("Invalid reference clock");
        Entry old=entries.stream().filter(e->e.word().equals(word)&&e.target().equals(target)).findFirst().orElse(null);
        if(old!=null&&!establish&&(now<old.lastSeen()||now-old.lastSeen()<OBSERVATION_INTERVAL))return this;
        if(old!=null&&establish&&old.observations()>=MIN_OBSERVATIONS)return this;
        var next=new ArrayList<>(entries.stream().filter(e->e!=old).toList());
        if(next.size()==MAX_ENTRIES)next.remove(next.stream().min(Comparator.comparingInt(Entry::observations).thenComparingLong(Entry::lastSeen)).orElseThrow());
        next.add(new Entry(word,target,establish?Math.max(MIN_OBSERVATIONS,old==null?0:old.observations()):Math.min(1000,old==null?1:old.observations()+1),now));
        return new Referents(next);
    }
    /** A known naming construction and a visible speaker/pointed place are required. No intended clause enters. */
    public Referents hearIntroduction(List<String> sounds,Function<String,Concept> decode,Grammar grammar,
                                     UUID visibleSpeaker,Place jointlyVisiblePlace,long now) {
        if(sounds.size()!=3||!grammar.initialized())return this;
        var candidates=new LinkedHashMap<Reference,Integer>();int best=-1;
        for(var reading:grammar.readings(sounds,decode,null)){
            if(reading.implicitVerb()!=null||reading.slots().size()!=3)continue;
            var roles=reading.slots();
            String subject=sounds.get(roles.indexOf(Grammar.Role.SUBJECT));
            String verb=sounds.get(roles.indexOf(Grammar.Role.VERB));
            String name=sounds.get(roles.indexOf(Grammar.Role.OBJECT));
            if(decode.apply(verb)!=Concept.NAMED||decode.apply(name)!=null||!Words.valid(name))continue;
            Target target=decode.apply(subject)==Concept.I&&visibleSpeaker!=null?Target.person(visibleSpeaker)
                    :decode.apply(subject)==Concept.HERE&&jointlyVisiblePlace!=null?Target.place(jointlyVisiblePlace):null;
            if(target==null)continue;
            if(reading.scope()>best){best=reading.scope();candidates.clear();}
            if(reading.scope()==best)candidates.put(new Reference(name,target),best);
        }
        if(candidates.size()==1){var ref=candidates.keySet().iterator().next();return observe(ref.word(),ref.target(),now);}
        return this;
    }
}
