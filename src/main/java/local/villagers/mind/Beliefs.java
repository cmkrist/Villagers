package local.villagers.mind;

import java.util.*;
import local.villagers.language.*;

/** Personal, fallible accounts. Testimony is not observation; repetition never makes it independent evidence. */
public record Beliefs(List<Entry> entries) {
    public static final int MAX_ENTRIES = 64;
    public static final long LIFETIME = 2400, NEED_LIFETIME = 600;
    public enum Evidence { HEARD, OBSERVED, CONFIRMED, CONTRADICTED, SUPERSEDED }
    public record Claim(UUID subject, Concept verb, Concept object, UUID objectPerson, boolean negated, boolean past,
                        Place subjectPlace,Place objectPlace) {
        public Claim(UUID subject,Concept verb,Concept object,UUID objectPerson,boolean negated,boolean past){this(subject,verb,object,objectPerson,negated,past,null,null);}
        public Claim {
            Objects.requireNonNull(verb); Objects.requireNonNull(object);
            if((subject==null)==(subjectPlace==null))throw new IllegalArgumentException("A claim needs one identified subject");
            new Clause(Concept.I, verb, object, negated, past);
            if ((object == Concept.I || object == Concept.YOU || object==Concept.PERSON) != (objectPerson != null)) throw new IllegalArgumentException("Missing spoken person context");
            if((object==Concept.PLACE)!=(objectPlace!=null))throw new IllegalArgumentException("Missing spoken place context");
            // Preserve schema-eleven's canonical resolved-person representation; the UUID supplies identity.
            if(objectPerson!=null)object=Concept.YOU;
        }
        public static Claim resolve(Clause clause, UUID speaker, UUID listener) {
            UUID subject = clause.subject() == Concept.I ? speaker : clause.subject() == Concept.YOU ? listener : null;
            Place subjectPlace=null,objectPlace=null;
            if(clause.subjectReference()!=null){subject=clause.subjectReference().target().person();subjectPlace=clause.subjectReference().target().place();}
            if(subject==null&&subjectPlace==null)return null;
            UUID object = clause.object() == Concept.I ? speaker : clause.object() == Concept.YOU ? listener : null;
            if(clause.objectReference()!=null){object=clause.objectReference().target().person();objectPlace=clause.objectReference().target().place();}
            if(clause.object()==Concept.PERSON&&object==null||clause.object()==Concept.PLACE&&objectPlace==null)return null;
            return new Claim(subject, clause.verb(), object==null?clause.object():Concept.PERSON, object,
                    clause.negated(), clause.past(),subjectPlace,objectPlace);
        }

        public boolean sameTopic(Claim other) {
            return Objects.equals(subject,other.subject) && Objects.equals(subjectPlace,other.subjectPlace) && verb == other.verb && object == other.object
                    && Objects.equals(objectPerson, other.objectPerson) && Objects.equals(objectPlace,other.objectPlace) && past == other.past;
        }
    }
    /** Reported sight is still testimony. The root is learned from spoken attribution, never copied from another mind. */
    public enum Basis { DIRECT, UNQUALIFIED, REPORTED_DIRECT, REPORTED_HEARSAY, UNKNOWN, OLD, CIRCULAR }
    public record Provenance(Basis basis, UUID root, long basisTick) {
        public Provenance {
            Objects.requireNonNull(basis);
            if(basisTick<0 || root==null && basis!=Basis.UNKNOWN)throw new IllegalArgumentException("Invalid provenance");
        }
        public double weight() { return switch(basis) {
            case DIRECT -> 1; case UNQUALIFIED -> .5; case REPORTED_DIRECT -> .9;
            case REPORTED_HEARSAY -> .6; case UNKNOWN -> .25; case OLD, CIRCULAR -> 0;
        }; }
    }
    public record Entry(Claim claim, UUID source, Evidence evidence, double confidence, long tick, long expires, Provenance provenance) {
        public Entry(Claim claim,UUID source,Evidence evidence,double confidence,long tick,long expires){this(claim,source,evidence,confidence,tick,expires,null);}
        public Entry {
            Objects.requireNonNull(claim); Objects.requireNonNull(source); Objects.requireNonNull(evidence);
            // Schema twelve did not record origins. Preserve the known immediate speaker without inventing a witness.
            if(provenance==null)provenance=new Provenance(evidence==Evidence.OBSERVED?Basis.DIRECT:Basis.UNQUALIFIED,source,tick);
            if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1 || tick < 0 || expires < tick
                    || expires - tick > LIFETIME || provenance.basisTick()>tick
                    || evidence==Evidence.OBSERVED && (provenance.basis()!=Basis.DIRECT || !source.equals(provenance.root())))
                throw new IllegalArgumentException("Invalid belief evidence");
        }
        public boolean current(long now) { return now >= tick && now < expires && evidence != Evidence.CONTRADICTED && evidence != Evidence.SUPERSEDED; }
        public Entry evidence(Evidence next){return new Entry(claim,source,next,confidence,tick,expires,provenance);}
        public double strength(long now) {
            if(!current(now)||expires==tick)return 0;
            return confidence*provenance.weight()*(expires-now)/(double)(expires-provenance.basisTick());
        }
    }
    public Beliefs {
        entries = List.copyOf(Objects.requireNonNull(entries));
        if (entries.size() > MAX_ENTRIES) throw new IllegalArgumentException("Too many beliefs");
        var keys = new HashSet<String>();
        for (Entry e : entries) if (!keys.add(e.claim().toString() + e.source())) throw new IllegalArgumentException("Duplicate belief source");
    }
    public static Beliefs empty() { return new Beliefs(List.of()); }
    private Beliefs put(Entry entry, long now) {
        var next = new ArrayList<>(entries.stream().filter(e -> e.expires() > now
                && !(e.claim().equals(entry.claim()) && e.source().equals(entry.source()))).toList());
        if (next.size() == MAX_ENTRIES) next.removeFirst();
        next.add(entry); return new Beliefs(next);
    }
    private static long until(long now, long lifetime) {
        if (now < 0 || now > Long.MAX_VALUE - LIFETIME) throw new IllegalArgumentException("Invalid belief clock");
        return now + lifetime;
    }
    public Beliefs hear(Sentence.Interpretation heard, UUID speaker, UUID listener, double trust, long now) {
        if (!Double.isFinite(trust) || trust < -1 || trust > 1) throw new IllegalArgumentException("Invalid source trust");
        if (!heard.complete()) return this;
        Beliefs result = this;
        for (Clause clause : heard.clauses()) {
            Claim claim = Claim.resolve(clause, speaker, listener);
            if (claim == null) continue;
            double confidence = Math.min(.8, heard.confidence() * (.4 + .4 * Math.max(0, trust)));
            var direct = result.entries.stream().filter(e -> e.claim().sameTopic(claim)
                    && e.evidence() == Evidence.OBSERVED && e.current(now)).findFirst();
            Evidence evidence = direct.isPresent() ? direct.get().claim().negated() == claim.negated()
                    ? Evidence.CONFIRMED : Evidence.CONTRADICTED : Evidence.HEARD;
            result = new Beliefs(result.entries.stream().map(e -> e.source().equals(speaker) && e.claim().sameTopic(claim)
                    && e.claim().negated()!=claim.negated() && e.evidence()!=Evidence.OBSERVED
                    ? e.evidence(Evidence.SUPERSEDED) : e).toList());
            var previous=result.entries.stream().filter(e->e.source().equals(speaker)&&e.claim().equals(claim)
                    &&e.evidence()!=Evidence.OBSERVED).findFirst().orElse(null);
            // An identical repetition is not a new sighting and cannot refresh the information's age.
            if(previous!=null && previous.expires()<=now)continue;
            result = result.put(new Entry(claim, speaker, evidence, confidence, previous==null?now:previous.tick(),
                    previous==null?until(now, lifetime(claim)):previous.expires(),previous==null?null:previous.provenance()), now);
        }
        return result;
    }
    /** Caller must have actually observed this event. Conflicting testimony remains attributed and marked. */
    public Beliefs observe(Claim observed, UUID observer, long now) {
        var updated = entries.stream().map(e -> e.claim().sameTopic(observed) && e.evidence() != Evidence.OBSERVED
                ? e.evidence(e.claim().negated() == observed.negated() ? Evidence.CONFIRMED : Evidence.CONTRADICTED) : e).toList();
        return new Beliefs(updated.stream().filter(e -> !(e.evidence() == Evidence.OBSERVED && e.claim().sameTopic(observed))).toList()).put(new Entry(observed, observer, Evidence.OBSERVED, 1, now, until(now,lifetime(observed))), now);
    }
    private static long lifetime(Claim claim){return claim.verb()==Concept.WANT||claim.verb()==Concept.HAVE||claim.verb()==Concept.PROMISE?NEED_LIFETIME:LIFETIME;}
    public static boolean food(Claim c){return !c.past()&&c.verb()==Concept.HAVE&&c.object()==Concept.FOOD;}
    public double foodLead(UUID person, long now) {
        double positive=0,negative=0;
        for(var e:entries)if(food(e.claim())&&Objects.equals(e.claim().subject(),person)){
            if(e.claim().negated())negative=Math.max(negative,e.strength(now));else positive=Math.max(positive,e.strength(now));
        }
        return Math.max(0,positive-negative);
    }
    /** Called only with the asker's decoded source reply for its own pending decoded claim. */
    public Beliefs qualify(Claim claim,UUID speaker,UUID listener,local.villagers.language.PracticalQuestions.Source heard,long now){
        var previous=entries.stream().filter(e->e.source().equals(speaker)&&e.claim().equals(claim)&&e.current(now)
                &&e.evidence()!=Evidence.OBSERVED).findFirst().orElse(null);
        if(previous==null)return this;
        UUID root=heard.root();
        Basis basis=root==null?Basis.UNKNOWN:root.equals(listener)?Basis.CIRCULAR:!heard.recent()?Basis.OLD
                :heard.direct()?Basis.REPORTED_DIRECT:Basis.REPORTED_HEARSAY;
        long basisTick=Math.max(0,now-local.villagers.language.PracticalQuestions.RECENT_WINDOW);
        // Repetition or another route from the same stated root cannot launder the age of this account.
        for(var e:entries)if(e.claim().equals(claim)&&Objects.equals(e.provenance().root(),root))basisTick=Math.min(basisTick,e.provenance().basisTick());
        long expires=Math.min(previous.expires(),until(basisTick,lifetime(claim)));
        expires=Math.max(previous.tick(),expires);
        return put(new Entry(claim,speaker,previous.evidence(),previous.confidence(),previous.tick(),expires,
                new Provenance(basis,root,Math.min(previous.tick(),basisTick))),now);
    }
    public boolean promisedCompany(UUID person, long now) {
        return entries.stream().anyMatch(e -> e.current(now) && e.source().equals(person) && Objects.equals(e.claim().subject(),person)
                && e.claim().verb() == Concept.PROMISE && e.claim().object() == Concept.FRIEND && !e.claim().negated() && !e.claim().past());
    }
    /** Retraction requires an actually understood denial of the current conversational interpretation. */
    public Beliefs retract(Sentence.Interpretation rejected, UUID speaker, UUID listener) {
        var claims = rejected.clauses().stream().map(c -> Claim.resolve(c,speaker,listener)).filter(Objects::nonNull).toList();
        return new Beliefs(entries.stream().map(e -> e.source().equals(speaker) && claims.contains(e.claim())
                && e.evidence() != Evidence.OBSERVED ? e.evidence(Evidence.CONTRADICTED) : e).toList());
    }
    public Beliefs fulfilledCompany(UUID person, UUID observer, long now) {
        var next = entries.stream().filter(e -> !(Objects.equals(e.claim().subject(),person) && e.claim().verb() == Concept.PROMISE
                && e.claim().object() == Concept.FRIEND)).toList();
        return new Beliefs(next).observe(new Claim(person, Concept.MEET, Concept.FRIEND, null, false, true), observer, now);
    }
}
