package local.villagers.relations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Fair among currently perceived candidates even when their distance ordering changes between attempts. */
public final class PartnerRotation {
    private final LinkedHashSet<UUID> tried=new LinkedHashSet<>();
    public int remembered() {return tried.size();}
    public UUID next(List<UUID> candidates) {
        if(candidates.size()>FoodRequest.MAX_PEERS)throw new IllegalArgumentException("Unbounded partner discovery");
        if(candidates.isEmpty())return null;
        Set<UUID> available=Set.copyOf(candidates);
        UUID selected=null;
        for(var id:candidates)if(!tried.contains(id)) {selected=id;break;}
        if(selected==null)for(var id:tried)if(available.contains(id)) {selected=id;break;}
        if(selected==null)throw new IllegalStateException("Candidate identity missing");
        tried.remove(selected);tried.add(selected);
        if(tried.size()>FoodRequest.MAX_PEERS)tried.remove(tried.iterator().next());
        return selected;
    }
}
