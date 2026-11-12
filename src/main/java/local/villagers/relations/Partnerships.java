package local.villagers.relations;

import java.util.*;

/** One person's explicit commitments. Friendship/opinion and parenthood remain separate records. */
public record Partnerships(List<Bond> history) {
    public static final int MAX_HISTORY=8;
    public static final long SHARED_INTERVAL=400, RECONSIDER_INTERVAL=2400;
    public enum End { NONE, WITHDREW, HEARD_WITHDRAWAL, RECONFIRMED }
    public record Bond(UUID peer,long agreedAt,long endedAt,End end,long sharedVisits,long lastSharedAt) {
        public Bond {
            Objects.requireNonNull(peer);Objects.requireNonNull(end);
            if(agreedAt<0||sharedVisits<0||(end==End.NONE?endedAt!=-1:endedAt<agreedAt)
                    ||(lastSharedAt!=-1&&lastSharedAt<agreedAt)||(sharedVisits==0)!=(lastSharedAt==-1)
                    ||endedAt>=0&&lastSharedAt>endedAt)throw new IllegalArgumentException("Invalid partnership history");
        }
        public boolean active(){return end==End.NONE;}
    }
    public Partnerships {
        history=List.copyOf(Objects.requireNonNull(history));
        if(history.size()>MAX_HISTORY||history.stream().filter(Bond::active).count()>1)throw new IllegalArgumentException("Invalid concurrent commitments");
        var keys=new HashSet<String>();
        for(var b:history)if(!keys.add(b.peer()+":"+b.agreedAt()))throw new IllegalArgumentException("Duplicate commitment");
    }
    public static Partnerships empty(){return new Partnerships(List.of());}
    public Bond active(){return history.stream().filter(Bond::active).findFirst().orElse(null);}
    public boolean committedTo(UUID peer){return active()!=null&&active().peer().equals(peer);}
    public boolean mayBegin(long now){return active()==null&&now>=0&&history.stream().noneMatch(b->b.endedAt()>now||now-b.endedAt()<RECONSIDER_INTERVAL);}
    public Partnerships form(UUID peer,long now){
        if(!mayBegin(now))throw new IllegalStateException("Existing commitment or separation cooldown");
        var next=new ArrayList<>(history);
        if(next.size()==MAX_HISTORY)next.removeFirst();
        next.add(new Bond(peer,now,-1,End.NONE,0,-1));return new Partnerships(next);
    }
    /** A fresh mutually interpreted promise can reconcile a half-save without copying the missing receipt. */
    public Partnerships agree(UUID peer,long now){
        var current=active();if(current==null)return form(peer,now);
        if(!current.peer().equals(peer)||now<current.agreedAt())throw new IllegalStateException("Cannot replace a different or future commitment");
        if(now==current.agreedAt())return this;
        var next=new ArrayList<>(end(peer,now,End.RECONFIRMED).history());
        if(next.size()==MAX_HISTORY)next.removeFirst();
        next.add(new Bond(peer,now,-1,End.NONE,0,-1));return new Partnerships(next);
    }
    /** Either participant can withdraw immediately. Comprehension governs the other's knowledge, never this consent. */
    public Partnerships end(UUID peer,long now,End reason){
        if(reason==End.NONE)throw new IllegalArgumentException("Missing withdrawal reason");
        return new Partnerships(history.stream().map(b->b.active()&&b.peer().equals(peer)
                ?new Bond(b.peer(),b.agreedAt(),Math.max(now,Math.max(b.agreedAt(),b.lastSharedAt())),reason,b.sharedVisits(),b.lastSharedAt()):b).toList());
    }
    /** Caller must supply an actual completed willing contact, not travel or an uttered promise. */
    public Partnerships shared(UUID peer,long now){
        return new Partnerships(history.stream().map(b->b.active()&&b.peer().equals(peer)&&now>=b.agreedAt()
                &&(b.lastSharedAt()<0||now-b.lastSharedAt()>=SHARED_INTERVAL)
                ?new Bond(b.peer(),b.agreedAt(),-1,End.NONE,b.sharedVisits()==Long.MAX_VALUE?Long.MAX_VALUE:b.sharedVisits()+1,now):b).toList());
    }
    /** Both independently saved receipts must agree. A half-save cannot grant mutual consent. */
    public static boolean mutual(UUID a,Partnerships first,UUID b,Partnerships second){
        var x=first.active();var y=second.active();
        return !a.equals(b)&&x!=null&&y!=null&&x.peer().equals(b)&&y.peer().equals(a)&&x.agreedAt()==y.agreedAt();
    }
    /** Own needs, own history and actual visible adulthood only; never the proposed partner's private needs. */
    public static boolean willing(boolean adult,boolean free,boolean threatened,double health,int food,double fatigue,boolean lowMood,
                                  Opinion own,double sociability,boolean loner){
        return adult&&free&&!threatened&&Double.isFinite(health)&&health>=.9&&food>=12&&food<=20
                &&Double.isFinite(fatigue)&&fatigue>=0&&fatigue<.6&&!lowMood&&Double.isFinite(sociability)&&sociability>=0&&sociability<=1
                &&own.witnessed()>=3&&own.regard()>=.20+(loner?.10:0)+.05*(1-sociability)&&own.trust()>=.1&&own.fear()<.25;
    }
    public static boolean wantsToEnd(Opinion own){return own.regard()<-.25||own.fear()>.5;}
}
