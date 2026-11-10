package local.villagers.navigation;

import java.util.List;
import java.util.function.Predicate;

/** A finite candidate snapshot survives admission boundaries; rejected paths do not discard later candidates. */
public final class WorkSearch<T> {
    public enum Step { FOUND, SEARCHING, EXHAUSTED }
    private List<T> candidates=List.of();
    private int cursor;
    private boolean active;
    public boolean active() { return active; }
    public void begin(List<T> values) {
        if(values.size()>343)throw new IllegalArgumentException("Unbounded work search");
        candidates=List.copyOf(values);cursor=0;active=true;
    }
    public void clear() { candidates=List.of();cursor=0;active=false; }
    /** Eligibility is rechecked before each native query. Call only inside an admitted planning batch. */
    public Step advance(Predicate<T> eligible,Predicate<T> startPath) {
        if(!active)return Step.EXHAUSTED;
        int queries=0;
        while(cursor<candidates.size() && queries<3) {
            T next=candidates.get(cursor++);
            if(!eligible.test(next))continue;
            queries++;
            if(startPath.test(next)) {clear();return Step.FOUND;}
        }
        if(cursor==candidates.size()) {clear();return Step.EXHAUSTED;}
        return Step.SEARCHING;
    }
}
