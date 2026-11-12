package local.villagers.learning;

import java.util.*;
import local.villagers.language.Concept;

/** Teacher's own observed lesson history. It contains no copy or inferred mastery map of the student's private vocabulary. */
public record Lessons(List<Entry> entries) {
    public static final int MAX_ENTRIES=32;
    public record Entry(UUID peer,Concept topic,long demonstrations,long confirmed,long lastTick){
        public Entry{Objects.requireNonNull(peer);Objects.requireNonNull(topic);if(demonstrations<1||confirmed<0||confirmed>demonstrations||lastTick<0)throw new IllegalArgumentException("Invalid lesson history");}
    }
    public Lessons{entries=List.copyOf(entries);if(entries.size()>MAX_ENTRIES)throw new IllegalArgumentException("Too many lesson records");var keys=new HashSet<String>();for(var e:entries)if(!keys.add(e.peer()+":"+e.topic()))throw new IllegalArgumentException("Duplicate lesson history");}
    public static Lessons empty(){return new Lessons(List.of());}
    public Entry of(UUID peer,Concept topic){return entries.stream().filter(e->e.peer().equals(peer)&&e.topic()==topic).findFirst().orElse(null);}
    public boolean confirmed(UUID peer,Concept topic){var e=of(peer,topic);return e!=null&&e.confirmed()>0;}
    public Lessons demonstrate(UUID peer,Concept topic,long now){
        var old=of(peer,topic);var next=new ArrayList<>(entries.stream().filter(e->e!=old).toList());if(next.size()==MAX_ENTRIES)next.removeFirst();
        next.add(new Entry(peer,topic,old==null?1:Math.min(Long.MAX_VALUE-1,old.demonstrations())+1,old==null?0:old.confirmed(),now));return new Lessons(next);
    }
    public Lessons confirm(UUID peer,Concept topic,long now){
        var old=of(peer,topic);if(old==null||old.confirmed()>=old.demonstrations())return this;
        return new Lessons(entries.stream().map(e->e==old?new Entry(peer,topic,e.demonstrations(),e.confirmed()+1,now):e).toList());
    }
}
