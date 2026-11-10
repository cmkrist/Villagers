package local.villagers;

import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** At most343 cells covering the full three-block contact radius; three native paths per admitted batch. */
final class TradeApproach {
    private TradeApproach() {}
    private static boolean sight(ServerLevel level,Villager actor,Vec3 from,Vec3 to) {
        var a=BlockPos.containing(from);var b=BlockPos.containing(to);
        for(int x=Math.min(a.getX()>>4,b.getX()>>4);x<=Math.max(a.getX()>>4,b.getX()>>4);x++)
            for(int z=Math.min(a.getZ()>>4,b.getZ()>>4);z<=Math.max(a.getZ()>>4,b.getZ()>>4);z++)
                if(!level.isLoaded(new BlockPos(x<<4,a.getY(),z<<4)))return false;
        return level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,actor)).getType()==HitResult.Type.MISS;
    }
    static boolean stand(ServerLevel level,Villager buyer,Session s,Villager peer,BlockPos p) {
        var feet=Vec3.atBottomCenterOf(p);
        if(!local.villagers.relations.FoodRequest.inReach(feet.distanceToSqr(peer.position()),Villagers.config().hearingRadius(),true))return false;
        if(!level.isLoaded(p) || !level.isLoaded(p.above()) || !level.isLoaded(p.below())
                || !level.getBlockState(p).isAir() || !level.getBlockState(p.above()).isAir()
                || !level.getFluidState(p).isEmpty() || !level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))return false;
        var eye=feet.add(0,buyer.getEyePosition().y-buyer.getY(),0);
        var bounds=buyer.getBoundingBox().move(feet.subtract(buyer.position()));
        return local.villagers.relations.FoodRequest.inReach(feet.distanceToSqr(peer.position()),Villagers.config().hearingRadius(),true)
                && VillageSurvival.withinHomeRange(level,s.mind,feet) && level.noCollision(buyer,bounds)
                && sight(level,buyer,eye,peer.getEyePosition()) && sight(level,peer,peer.getEyePosition(),eye);
    }
    static local.villagers.navigation.WorkSearch.Step move(ServerLevel level,Villager buyer,Session s,Villager peer) {
        return move(level,buyer,s,peer,s.tradeSearch);
    }
    static local.villagers.navigation.WorkSearch.Step move(ServerLevel level,Villager buyer,Session s,Villager peer,
            local.villagers.navigation.WorkSearch<BlockPos> search) {
      if(!search.active()) {
        var stands=new ArrayList<BlockPos>();var target=peer.blockPosition();
        for(var p:BlockPos.betweenClosed(target.offset(-3,-3,-3),target.offset(3,3,3)))if(stand(level,buyer,s,peer,p))stands.add(p.immutable());
        stands.sort(Comparator.comparingDouble(p->buyer.position().distanceToSqr(Vec3.atBottomCenterOf(p))));
        search.begin(stands);
      }
      return search.advance(goal->stand(level,buyer,s,peer,goal),goal->{
            s.tradePathQueries++;
            var path=buyer.getNavigation().createPath(goal,0);
            boolean accepted=WorkNavigation.staysHome(level,buyer,s,path) && buyer.getNavigation().moveTo(path,.5);
            s.lastTradePath=java.util.Map.of("gameTime",level.getGameTime(),"goal",goal.toShortString(),"peer",peer.blockPosition().toShortString(),
                    "nodes",path==null?0:path.getNodeCount(),"canReach",path!=null && path.canReach(),"actor",buyer.position().toString(),
                    "peerPosition",peer.position().toString(),"actorEye",buyer.getEyePosition().toString(),"peerEye",peer.getEyePosition().toString(),"accepted",accepted);
            if(accepted) {
                s.tradePathStarts++;
                return true;
            }
            s.tradePathRejections++;
            return false;
      });
    }
}
