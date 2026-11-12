package local.villagers.language;

import java.util.*;
import local.villagers.work.WorkOrders.Task;

/** Concrete recipe, count, price and deadline frames. A recipient reads only parsed words, never intended terms. */
public final class WorkDialogue {
    private WorkDialogue() {}
    public enum Act { REQUEST, QUOTE, ACCEPT, ACKNOWLEDGE, CANCEL_BUYER, CANCEL_WORKER }
    public record Frame(Act act,Task task,int price){public Frame{Objects.requireNonNull(act);Objects.requireNonNull(task);if(price<1||price>2)throw new IllegalArgumentException("Invalid spoken price");}}
    public static Concept output(Task task){return task==Task.BREAD?Concept.BREAD:Concept.HOE;}
    public static Concept material(Task task){return task==Task.BREAD?Concept.WHEAT:Concept.WOOD;}
    public static List<Clause> words(Frame frame){
        boolean buyer=frame.act()==Act.REQUEST||frame.act()==Act.ACCEPT||frame.act()==Act.CANCEL_BUYER;
        boolean cancelled=frame.act()==Act.CANCEL_BUYER||frame.act()==Act.CANCEL_WORKER;
        var result=new ArrayList<Clause>();var output=output(frame.task());
        result.add(new Clause(buyer?Concept.YOU:Concept.I,buyer?Concept.MAKE:Concept.PROMISE,output,cancelled,false));
        result.add(new Clause(output,Concept.FROM,material(frame.task())));
        result.add(new Clause(output,Concept.IS,Concept.ONE));
        result.add(new Clause(buyer?Concept.I:Concept.YOU,Concept.PAY,Concept.EMERALD,cancelled,false));
        result.add(new Clause(Concept.EMERALD,Concept.IS,frame.price()==1?Concept.ONE:Concept.TWO));
        result.add(new Clause(Concept.WORK,Concept.IS,Concept.SOON));
        if(frame.act()==Act.ACCEPT||frame.act()==Act.ACKNOWLEDGE)result.add(new Clause(Concept.I,Concept.SAY,Concept.YES));
        return List.copyOf(result);
    }
    public static Frame decode(Sentence.Interpretation heard){
        if(!heard.complete()||heard.clauses().size()<6||heard.clauses().size()>7)return null;
        for(var act:Act.values())for(var task:Task.values())for(int price=1;price<=2;price++){
            var candidate=new Frame(act,task,price);if(heard.clauses().equals(words(candidate)))return candidate;
        }
        return null;
    }
    public static String english(Sentence.Interpretation heard){
        var frame=decode(heard);if(frame==null)return null;
        String item=frame.task()==Task.BREAD?"one bread from wheat":"one wooden hoe";
        String coins=frame.price()==1?"one emerald":"two emeralds";
        return switch(frame.act()){
            case REQUEST -> "Will you make "+item+" soon for "+coins+"?";
            case QUOTE -> "I can make "+item+" soon for "+coins+".";
            case ACCEPT -> "I accept: make "+item+" soon, and I will pay "+coins+".";
            case ACKNOWLEDGE -> "Agreed: I will make "+item+" soon for "+coins+".";
            case CANCEL_BUYER -> "I am cancelling my order for "+item+" for "+coins+".";
            case CANCEL_WORKER -> "I cannot continue making "+item+" for "+coins+".";
        };
    }
}
