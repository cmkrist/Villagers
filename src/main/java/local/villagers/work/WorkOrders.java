package local.villagers.work;

import java.util.*;
import local.villagers.mind.Place;

/** Personal records of explicit production obligations, never stock, money, capability or private peer knowledge. */
public record WorkOrders(List<Order> history) {
    public static final int MAX_HISTORY=16;
    public static final long LIFETIME=2400;
    public enum Task { WOODEN_HOE, BREAD }
    public enum State { AGREED, WORKING, READY, PAUSED, COMPLETED, CANCELLED, EXPIRED }
    public enum Reason { NONE, UNSAFE, MATERIALS, STATION, ROUTE, PEER_UNAVAILABLE, TERMS, STOCK, PAYMENT, CAPACITY, DEADLINE, WITHDREW, HEARD_WITHDRAWAL }
    public record Terms(UUID buyer,UUID worker,Task task,int quantity,int price,long agreedAt,long dueAt){
        public Terms {
            Objects.requireNonNull(buyer);Objects.requireNonNull(worker);Objects.requireNonNull(task);
            if(buyer.equals(worker)||quantity!=1||price<1||price>2||agreedAt<0||dueAt<agreedAt||dueAt-agreedAt!=LIFETIME)throw new IllegalArgumentException("Invalid work terms");
        }
        public UUID peer(UUID actor){if(buyer.equals(actor))return worker;if(worker.equals(actor))return buyer;throw new IllegalArgumentException("Not a work participant");}
        public boolean sameContext(Terms other){return buyer.equals(other.buyer)&&worker.equals(other.worker)&&agreedAt==other.agreedAt;}
    }
    /** Only the worker's actual successful recipe callback creates this personal production evidence. */
    public record Production(long tick,Place station,int count){public Production{Objects.requireNonNull(station);if(tick<0||count!=1)throw new IllegalArgumentException("Invalid production evidence");}}
    /** Own inventory observations only. The counterpart's private bag is not copied into an agent's knowledge. */
    public record Settlement(long tick,int goodsBefore,int goodsAfter,int coinsBefore,int coinsAfter){
        public Settlement{if(tick<0||goodsBefore<0||goodsAfter<0||coinsBefore<0||coinsAfter<0)throw new IllegalArgumentException("Invalid settlement");}
        public boolean proves(Terms terms,UUID actor){
            int sign=terms.buyer().equals(actor)?1:terms.worker().equals(actor)?-1:0;
            return sign!=0&&goodsAfter-goodsBefore==sign*terms.quantity()&&coinsAfter-coinsBefore==-sign*terms.price();
        }
    }
    public record Order(Terms terms,State state,Reason reason,Production production,Settlement settlement,long updated,long interruptions){
        public Order{
            Objects.requireNonNull(terms);Objects.requireNonNull(state);Objects.requireNonNull(reason);
            if(updated<terms.agreedAt()||interruptions<0||production!=null&&(production.tick()<terms.agreedAt()||production.tick()>=terms.dueAt()||production.tick()>updated)
                    ||(state==State.COMPLETED)!=(settlement!=null)||settlement!=null&&(settlement.tick()<terms.agreedAt()||settlement.tick()>=terms.dueAt()||settlement.tick()>updated)
                    ||state==State.READY&&production==null)throw new IllegalArgumentException("Invalid work outcome");
        }
        public boolean open(){return state!=State.COMPLETED&&state!=State.CANCELLED&&state!=State.EXPIRED;}
        public boolean current(long now){return open()&&now>=terms.agreedAt()&&now<terms.dueAt();}
    }
    public WorkOrders {
        history=List.copyOf(Objects.requireNonNull(history));
        if(history.size()>MAX_HISTORY||history.stream().filter(Order::open).count()>1)throw new IllegalArgumentException("Too many active work obligations");
        var keys=new HashSet<String>();for(var e:history)if(!keys.add(e.terms().buyer()+":"+e.terms().worker()+":"+e.terms().agreedAt()))throw new IllegalArgumentException("Duplicate work context");
    }
    public static WorkOrders empty(){return new WorkOrders(List.of());}
    public Order open(){return history.stream().filter(Order::open).findFirst().orElse(null);}
    public Order find(Terms terms){return history.stream().filter(e->e.terms().equals(terms)).findFirst().orElse(null);}
    public WorkOrders accept(Terms terms){
        if(open()!=null)throw new IllegalStateException("Already committed to work");
        var next=new ArrayList<>(history);if(next.size()==MAX_HISTORY)next.removeFirst();
        next.add(new Order(terms,State.AGREED,Reason.NONE,null,null,terms.agreedAt(),0));return new WorkOrders(next);
    }
    private WorkOrders replace(Order order){return new WorkOrders(history.stream().map(e->e.terms().equals(order.terms())?order:e).toList());}
    public WorkOrders state(Terms terms,State state,Reason reason,long now){
        if(state==State.COMPLETED||state==State.READY||state==State.AGREED)throw new IllegalArgumentException("Use evidence-aware transition");
        var old=find(terms);if(old==null||!old.current(now)||old.state()==state&&old.reason()==reason)return this;
        long pauses=old.interruptions();if(state==State.PAUSED&&old.state()!=State.PAUSED&&pauses<Long.MAX_VALUE)pauses++;
        return replace(new Order(terms,state,reason,old.production(),null,now,pauses));
    }
    public WorkOrders expire(long now){
        var next=history.stream().map(e->e.open()&&now>=e.terms().dueAt()?new Order(e.terms(),State.EXPIRED,
                e.reason()==Reason.NONE?Reason.DEADLINE:e.reason(),e.production(),null,now,e.interruptions()):e).toList();
        return new WorkOrders(next);
    }
    public WorkOrders produced(Terms terms,Production actual,long now){
        var old=find(terms);if(old==null||!old.current(now)||old.production()!=null||actual.tick()!=now)return this;
        return replace(new Order(terms,State.READY,Reason.NONE,actual,null,now,old.interruptions()));
    }
    public WorkOrders resume(Terms terms,long now){
        var old=find(terms);if(old==null||!old.current(now)||old.state()!=State.PAUSED)return this;
        return replace(new Order(terms,old.production()==null?State.AGREED:State.READY,Reason.NONE,old.production(),null,now,old.interruptions()));
    }
    public WorkOrders complete(Terms terms,UUID actor,Settlement actual,long now){
        var old=find(terms);if(old==null||!old.current(now)||actual.tick()!=now||!actual.proves(terms,actor)
                ||terms.worker().equals(actor)&&old.production()==null)throw new IllegalStateException("Unproved work completion");
        return replace(new Order(terms,State.COMPLETED,Reason.NONE,old.production(),actual,now,old.interruptions()));
    }
    /** Mechanical consent gate only: neither party learns the other's private reasoning or bag through this check. */
    public static boolean mutual(Order a,Order b,long now){return a!=null&&b!=null&&a.current(now)&&b.current(now)&&a.terms().equals(b.terms());}
}
