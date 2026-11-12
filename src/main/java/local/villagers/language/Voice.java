package local.villagers.language;

import java.util.*;

/** Heard vocal exemplars and bounded motor practice; these are sounds, never acquired meanings. */
public record Voice(List<Model> models) {
    public static final int MAX_MODELS=2,MAX_ECHO=12;
    public enum Kind { VILLAGER, PILLAGER }
    public record Model(Kind kind,List<String> words,int heard,int practice,long lastHeard){
        public Model{Objects.requireNonNull(kind);words=List.copyOf(words);if(words.isEmpty()||words.size()>MAX_ECHO||words.stream().anyMatch(w->!w.equals(";")&&!Words.valid(w))||heard<1||heard>1000||practice<0||practice>1000||lastHeard<0)throw new IllegalArgumentException("Invalid heard vocal model");}
    }
    public record Attempt(Kind target,List<String> sounds,Voice practiced){}
    public record Pulse(int at,String word,float pitch){}
    public Voice{models=List.copyOf(models);if(models.size()>MAX_MODELS||models.stream().map(Model::kind).distinct().count()!=models.size())throw new IllegalArgumentException("Invalid voice memory");}
    public static Voice empty(){return new Voice(List.of());}
    public Model model(Kind kind){return models.stream().filter(m->m.kind()==kind).findFirst().orElse(null);}
    public Voice hear(Kind kind,List<String> sounds,long now){
        if(sounds.isEmpty()||sounds.size()>MAX_ECHO)return this;
        var old=model(kind);var next=new ArrayList<>(models.stream().filter(m->m.kind()!=kind).toList());
        next.add(new Model(kind,sounds,old==null?1:Math.min(1000,old.heard()+1),old==null?0:old.practice(),now));return new Voice(next);
    }
    /** An early imitation has authored articulation errors. Only the resulting sounds cross the listener boundary. */
    public Attempt imitate(Kind nativeKind,Kind target,long now){
        var old=model(target);if(old==null||nativeKind==target||now<old.lastHeard()||now-old.lastHeard()>2400)return null;
        var sounds=old.words().stream().map(w->old.practice()>=3?w:articulate(nativeKind,w)).toList();
        var next=new Voice(models.stream().map(m->m.kind()==target?new Model(target,m.words(),m.heard(),Math.min(1000,m.practice()+1),m.lastHeard()):m).toList());
        return new Attempt(target,sounds,next);
    }
    public static String articulate(Kind own,String word){return word.equals(";")?word:own==Kind.VILLAGER?word.replace('z','s').replace('g','k'):word.replace('v','f').replace('b','p');}
    /** UUID affects timbre/rhythm, never meanings. Every actual emitted word contributes one native grunt pulse. */
    public static List<Pulse> plan(UUID id,Kind body,List<String> sounds,boolean imitating){
        if(sounds.isEmpty()||sounds.size()>Sentence.MAX_SOUNDS)throw new IllegalArgumentException("Unbounded vocal phrase");
        var pulses=new ArrayList<Pulse>();int at=0;long seed=id.getMostSignificantBits()^Long.rotateLeft(id.getLeastSignificantBits(),17);
        float center=(body==Kind.VILLAGER?1.03f:.77f)+(Math.floorMod(seed,19)-9)*.013f;
        for(var word:sounds){if(word.equals(";")){at+=5;continue;}if(!Words.valid(word))throw new IllegalArgumentException("Invalid vocal word");
            float melody=(Math.floorMod(word.hashCode(),17)-8)*.022f;
            pulses.add(new Pulse(at,word,Math.clamp(center+melody+(imitating?.06f:0),.5f,1.7f)));
            at+=4+Math.floorMod(seed,3)+word.length()/3;
        }return List.copyOf(pulses);
    }
}
