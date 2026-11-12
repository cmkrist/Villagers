package local.villagers;

import java.util.function.Supplier;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;

/** Constrain native route search only; never change fall damage or movement physics. */
public final class NativePathLimits {
    private record Limit(Object actor,int fall,Predicate<BlockPos> blocked) {}
    private static final ThreadLocal<Limit> CURRENT=new ThreadLocal<>();
    public static int maxFall(Object actor,int nativeFall) {
        var limit=CURRENT.get();return limit!=null&&limit.actor()==actor?Math.min(nativeFall,limit.fall()):nativeFall;
    }
    static <T> T search(Object actor,int fall,Supplier<T> planner) {
        if(actor==null||fall<0||fall>3)throw new IllegalArgumentException("Invalid native route limit");
        var previous=CURRENT.get();CURRENT.set(new Limit(actor,fall,previous!=null&&previous.actor()==actor?previous.blocked():null));
        try{return planner.get();}
        finally{if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
    }
    public static boolean mustReplan(Object actor){
        // A route cached before either constraint existed cannot prove that
        // constraint. Fall-only searches need the same fresh, observational
        // planning as searches which exclude occupied nodes.
        var limit=CURRENT.get();return limit!=null&&limit.actor()==actor;
    }
    public static boolean blocked(Object actor,int x,int y,int z){
        var limit=CURRENT.get();return limit!=null&&limit.actor()==actor&&limit.blocked()!=null&&limit.blocked().test(new BlockPos(x,y,z));
    }
    static <T> T avoiding(Object actor,Predicate<BlockPos> blocked,Supplier<T> planner){
        var previous=CURRENT.get();
        CURRENT.set(new Limit(actor,previous!=null&&previous.actor()==actor?previous.fall():3,blocked));
        try{return planner.get();}
        finally{if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
    }
    static <T> T alsoAvoiding(Object actor,Predicate<BlockPos> blocked,Supplier<T> planner){
        var previous=CURRENT.get();
        var earlier=previous!=null&&previous.actor()==actor?previous.blocked():null;
        return avoiding(actor,earlier==null?blocked:earlier.or(blocked),planner);
    }
    private NativePathLimits() {}
}
