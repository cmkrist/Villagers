package local.villagers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Short public work reservations prevent two residents cutting the same observed trunk. */
final class TreeClaims {
    record Lease(UUID worker,long until) {}
    static final class Board {
        private final Map<Long,Lease> claims=new HashMap<>();
        private long column(BlockPos tree) { return BlockPos.asLong(tree.getX(),0,tree.getZ()); }
        boolean available(BlockPos tree,UUID worker,long now) {
            claims.values().removeIf(lease->lease.until()<=now);
            var lease=claims.get(column(tree));return lease==null||lease.worker().equals(worker);
        }
        boolean claim(BlockPos tree,UUID worker,long now) {
            if(!available(tree,worker,now)||claims.size()>=256&&!claims.containsKey(column(tree))) return false;
            claims.put(column(tree),new Lease(worker,now+400));return true;
        }
    }
    private static final Map<ServerLevel,Board> BOARDS=new WeakHashMap<>();
    static boolean available(ServerLevel level,BlockPos tree,UUID worker,long now) {
        return BOARDS.computeIfAbsent(level,k->new Board()).available(tree,worker,now);
    }
    static boolean claim(ServerLevel level,BlockPos tree,UUID worker,long now) {
        return BOARDS.computeIfAbsent(level,k->new Board()).claim(tree,worker,now);
    }
    private TreeClaims() {}
}
