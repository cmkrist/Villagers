package local.villagers;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import local.villagers.navigation.WorkBudget;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Shared work-path admission: at most two bounded planning batches per dimension/second. */
final class WorkNavigation {
    private static final Map<ServerLevel, WorkBudget> BUDGETS = new IdentityHashMap<>();
    private WorkNavigation() {}
    static void clear() { BUDGETS.clear(); }
    static void forget(UUID actor) { BUDGETS.values().forEach(budget -> budget.forget(actor)); }
    static boolean admit(ServerLevel level, long now, Villager actor) {
        return BUDGETS.computeIfAbsent(level, ignored -> new WorkBudget()).admit(actor.getUUID(), now);
    }
    // Read-only operator queries never enqueue a future reservation.
    static boolean admit(ServerLevel level, long now) {
        return BUDGETS.computeIfAbsent(level, ignored -> new WorkBudget()).inspect(now);
    }
    /** Plan to an already observed work destination independently of combat follow range. */
    static Path pathTo(ServerLevel level,Villager v,Session s,BlockPos target,int reach) {
        if(!level.isLoaded(target)||!level.getWorldBorder().isWithinBounds(target)
                ||!VillageSurvival.withinHomeRange(level,s.mind,Vec3.atBottomCenterOf(target)))return null;
        var path=v.getNavigation().createPath(target,reach,64);
        return staysHome(level,v,s,path)?path:null;
    }
    static boolean staysHome(ServerLevel level, Villager v, Session s, Path path) {
        if (path == null || !path.canReach() || path.getNodeCount() < 1 || path.getNodeCount() > 128) return false;
        for (int i = 0; i < path.getNodeCount(); i++) {
            var pos = path.getNodePos(i);
            if (!level.isLoaded(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || !VillageSurvival.withinHomeRange(level, s.mind, new Vec3(pos.getX() + .5, pos.getY(), pos.getZ() + .5))) return false;
        }
        return true;
    }
}
