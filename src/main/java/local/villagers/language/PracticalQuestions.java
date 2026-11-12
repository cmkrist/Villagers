package local.villagers.language;

import java.util.*;
import local.villagers.mind.Beliefs;

/** Finite pragmatic frames assembled from the listener's own parsed clauses. No intention channel. */
public final class PracticalQuestions {
    private PracticalQuestions() {}
    public static final long RECENT_WINDOW=400, LIFETIME=600;
    public enum Kind { WHO_FOOD, WHERE_FOOD, SOURCE, OTHER }
    public static List<Clause> question(Kind kind){return switch(kind){
        case WHO_FOOD -> List.of(new Clause(Concept.I,Concept.ASK,Concept.FOOD),new Clause(Concept.I,Concept.ASK,Concept.PERSON));
        case WHERE_FOOD -> List.of(new Clause(Concept.I,Concept.ASK,Concept.FOOD),new Clause(Concept.I,Concept.ASK,Concept.PLACE));
        case SOURCE -> List.of(new Clause(Concept.I,Concept.ASK,Concept.SOURCE));
        case OTHER -> throw new IllegalArgumentException("Not a question");
    };}
    public static Kind kind(Sentence.Interpretation heard){
        if(!heard.complete())return Kind.OTHER;
        for(var kind:List.of(Kind.WHO_FOOD,Kind.WHERE_FOOD,Kind.SOURCE))if(heard.clauses().equals(question(kind)))return kind;
        return Kind.OTHER;
    }
    public static List<Clause> unknown(Concept topic){return List.of(new Clause(Concept.I,Concept.KNOW,topic,true,false));}
    public static boolean unknown(Sentence.Interpretation heard,Concept topic){return heard.complete()&&heard.clauses().equals(unknown(topic));}
    public record Answer(Beliefs.Entry entry,Clause clause) {}
    public static Clause express(Beliefs.Claim claim,UUID speaker,UUID listener,Referents names){
        Concept subject;Referents.Reference ref=null;
        if(Objects.equals(claim.subject(),speaker))subject=Concept.I;
        else if(Objects.equals(claim.subject(),listener))subject=Concept.YOU;
        else {
            var target=claim.subject()!=null?Referents.Target.person(claim.subject()):Referents.Target.place(claim.subjectPlace());
            ref=names.nameFor(target);if(ref==null)return null;subject=target.concept();
        }
        return new Clause(subject,claim.verb(),claim.object(),claim.negated(),claim.past(),ref,null);
    }
    public static Answer answer(Kind kind,Beliefs book,UUID speaker,UUID listener,Referents names,long now){
        if(kind!=Kind.WHO_FOOD&&kind!=Kind.WHERE_FOOD)return null;
        for(var entry:book.entries().stream().filter(e->Beliefs.food(e.claim())&&!e.claim().negated()&&e.strength(now)>0)
                .filter(e->kind==Kind.WHO_FOOD?e.claim().subject()!=null:e.claim().subjectPlace()!=null)
                .sorted(Comparator.comparingDouble((Beliefs.Entry e)->-e.strength(now))).toList()){
            if(entry.claim().subject()!=null&&book.foodLead(entry.claim().subject(),now)<=0)continue;
            var clause=express(entry.claim(),speaker,listener,names);
            if(clause!=null)return new Answer(entry,clause);
        }
        return null;
    }
    /** Only a single complete, positive and identified answer to the question can become its pending lead. */
    public static Beliefs.Claim decodedAnswer(Sentence.Interpretation heard,Kind kind,UUID speaker,UUID listener){
        if(!heard.complete()||heard.clauses().size()!=1)return null;
        var c=Beliefs.Claim.resolve(heard.clauses().getFirst(),speaker,listener);
        if(c==null||!Beliefs.food(c)||c.negated())return null;
        return kind==Kind.WHO_FOOD&&c.subject()!=null||kind==Kind.WHERE_FOOD&&c.subjectPlace()!=null?c:null;
    }
    /** A root is a claimed origin, not proof of a sighting or a grant of the origin's private memories. */
    public record Source(UUID root,boolean direct,boolean recent) {
        public static Source unknown(){return new Source(null,false,false);}
    }
    public static List<Clause> sourceReply(Beliefs.Entry entry,UUID speaker,UUID listener,Referents names,long now){
        var p=entry.provenance();
        if(!entry.current(now)||p.root()==null||p.basis()==Beliefs.Basis.UNKNOWN||p.basis()==Beliefs.Basis.CIRCULAR)return unknown(Concept.SOURCE);
        boolean direct=entry.evidence()==Beliefs.Evidence.OBSERVED&&p.basis()==Beliefs.Basis.DIRECT&&p.root().equals(speaker);
        Clause source;
        if(direct)source=new Clause(Concept.I,Concept.SEE,Concept.FOOD,false,true);
        else {
            Concept person;Referents.Reference name=null;
            if(p.root().equals(listener))person=Concept.YOU;
            else if(p.root().equals(speaker))person=Concept.I;
            else {person=Concept.PERSON;name=names.nameFor(Referents.Target.person(p.root()));if(name==null)return unknown(Concept.SOURCE);}
            source=new Clause(Concept.SOURCE,Concept.FROM,person,false,false,null,name);
        }
        boolean recent=p.basis()!=Beliefs.Basis.OLD&&now>=p.basisTick()&&now-p.basisTick()<=RECENT_WINDOW;
        return List.of(source,new Clause(Concept.SOURCE,Concept.IS,recent?Concept.RECENT:Concept.OLD));
    }
    public static Source decodedSource(Sentence.Interpretation heard,UUID speaker,UUID listener){
        if(!heard.complete()||heard.clauses().size()!=2)return Source.unknown();
        var first=heard.clauses().getFirst();var age=heard.clauses().get(1);
        if(age.subject()!=Concept.SOURCE||age.verb()!=Concept.IS||age.negated()||age.past()
                ||age.object()!=Concept.RECENT&&age.object()!=Concept.OLD)return Source.unknown();
        if(first.equals(new Clause(Concept.I,Concept.SEE,Concept.FOOD,false,true)))return new Source(speaker,true,age.object()==Concept.RECENT);
        if(first.subject()!=Concept.SOURCE||first.verb()!=Concept.FROM||first.negated()||first.past())return Source.unknown();
        UUID root=first.object()==Concept.I?speaker:first.object()==Concept.YOU?listener:
                first.objectReference()==null?null:first.objectReference().target().person();
        return root==null?Source.unknown():new Source(root,false,age.object()==Concept.RECENT);
    }
    public enum Stage { ANSWERED, INVESTIGATING, UNKNOWN, VERIFIED, DECLINED }
    public record Conversation(UUID peer,Kind kind,Beliefs.Claim claim,Stage stage,long started,long expires){
        public Conversation {
            Objects.requireNonNull(peer);Objects.requireNonNull(kind);Objects.requireNonNull(stage);
            if(kind!=Kind.WHO_FOOD&&kind!=Kind.WHERE_FOOD||started<0||expires<started||expires-started>LIFETIME||stage!=Stage.UNKNOWN&&claim==null)throw new IllegalArgumentException("Invalid practical conversation");
        }
        public boolean current(long now){return now>=started&&now<expires;}
        public Conversation stage(Stage next){return new Conversation(peer,kind,claim,next,started,expires);}
    }
    /** Responder's own statement context. It is never handed to the asker's interpreter. */
    public record Offer(UUID peer,Beliefs.Entry entry,long expires) {
        public Offer { Objects.requireNonNull(peer);Objects.requireNonNull(entry);if(expires<entry.tick())throw new IllegalArgumentException("Invalid answer context"); }
    }
    public static String english(Sentence.Interpretation heard){return switch(kind(heard)){
        case WHO_FOOD -> "Who has food?";case WHERE_FOOD -> "Where is there food?";case SOURCE -> "How do you know?";
        case OTHER -> null;
    };}
}
