package local.villagers;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.phys.*;

/** A trapped native body may walk out; never moves it directly or suppresses damage. */
public final class NativeEntrapment {
    private static final class Escape {
        Vec3 target;long until;int incidents,completed;float health;
        final Set<BlockPos> tried=new HashSet<>();Map<String,Object> receipt=Map.of();
    }
    private static final Map<Mob,Escape> ESCAPES=new WeakHashMap<>();
    private static boolean clear(ServerLevel level,Mob mob,Vec3 feet){
        var p=BlockPos.containing(feet);var body=mob.getBoundingBox().move(feet.subtract(mob.position()));
        return level.isLoaded(p.below())&&level.isLoaded(p.above(2))&&level.getWorldBorder().isWithinBounds(p)
            &&level.getFluidState(p).isEmpty()&&level.getFluidState(p.below()).isEmpty()
            &&level.noCollision(mob,body)&&!level.noBlockCollision(mob,body.move(0,-.03,0));
    }
    public static boolean prepare(ServerLevel level,Mob mob){
        if(!(mob instanceof Pillager)||mob.isNoAi()||!mob.isAlive()||!NativeRegionsScenario.nativeRegions(level)
            ||!(Villagers.config().nativeCampMeals()||Villagers.config().nativeCampPassages()))return false;
        var w=ESCAPES.get(mob);boolean trapped=mob.isInWall();
        if(!trapped){
            if(w==null||w.target==null)return false;
            if(mob.position().distanceToSqr(w.target)<.4*.4||level.noBlockCollision(mob,mob.getBoundingBox())&&level.getGameTime()>=w.until){
                w.completed++;w.receipt=Map.of("action","walked out using native movement","gameTime",level.getGameTime(),
                    "position",mob.position().toString(),"healthBefore",w.health,"healthAfter",mob.getHealth());
                w.target=null;w.tried.clear();return false;
            }
            return true;
        }
        if(w==null){w=new Escape();ESCAPES.put(mob,w);}
        long now=level.getGameTime();
        if(w.target!=null&&now<w.until&&clear(level,mob,w.target))return true;
        if(w.target==null){w.incidents++;w.health=mob.getHealth();}
        var options=new ArrayList<Vec3>();var origin=mob.blockPosition();
        for(var p:BlockPos.betweenClosed(origin.offset(-2,-1,-2),origin.offset(2,1,2))){
            if(w.tried.contains(p)||!level.isLoaded(p.below()))continue;
            double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,p);
            var feet=new Vec3(p.getX()+.5,floor,p.getZ()+.5);double horizontal=feet.subtract(mob.position()).horizontalDistanceSqr();
            if(horizontal<.35*.35||horizontal>4||floor<mob.getY()-1||floor>mob.getY()+.5||!clear(level,mob,feet))continue;
            options.add(feet);
        }
        options.sort(Comparator.comparingDouble(p->p.distanceToSqr(mob.position())));
        if(options.isEmpty()){w.target=null;w.tried.clear();return false;}
        w.target=options.getFirst();w.tried.add(BlockPos.containing(w.target));w.until=now+40;
        w.receipt=Map.of("action","attempting a native walk from an entrapping block","gameTime",now,
            "from",mob.position().toString(),"target",w.target.toString(),"healthBefore",mob.getHealth());
        return true;
    }
    public static void move(Mob mob){
        var w=ESCAPES.get(mob);if(w==null||w.target==null)return;
        mob.getNavigation().stop();mob.getMoveControl().setWantedPosition(w.target.x,w.target.y,w.target.z,.8);
    }
    public static Map<String,Object> status(Mob mob){
        var w=ESCAPES.get(mob);return w==null?Map.of("incidents",0):Map.of("id",mob.getUUID().toString(),
            "incidents",w.incidents,"completed",w.completed,"active",w.target!=null,"lastReceipt",w.receipt);
    }
    private NativeEntrapment(){}
}
