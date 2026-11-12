package local.villagers;

import local.villagers.navigation.WorkJourney;
import local.villagers.mind.Place;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Return to actually observed farm patches, with the native remembered composter as fallback. */
final class FarmWorksite {
    private FarmWorksite() {}
    static void interrupt(Session s, long now) { s.farmJourney.interrupt(now); s.rememberedFarmTarget=null; }

    private static Place place(ServerLevel level,BlockPos p) {
        return new Place(level.dimension().identifier().toString(),p.getX(),p.getY(),p.getZ());
    }
    private static boolean crop(ServerLevel level,BlockPos p) {
        return level.isLoaded(p) && level.isLoaded(p.below())
                && level.getBlockState(p).getBlock() instanceof CropBlock
                && level.getBlockState(p.below()).is(Blocks.FARMLAND)
                && AdventureActions.cropKind(level.getBlockState(p)) != local.villagers.capability.CropPolicy.CropKind.OTHER;
    }
    private static boolean visible(ServerLevel level,Villager v,BlockPos p) {
        if(v.getEyePosition().distanceToSqr(Vec3.atCenterOf(p))>64)return false;
        var hit=level.clip(new ClipContext(v.getEyePosition(),Vec3.atCenterOf(p),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,v));
        return hit.getType()==HitResult.Type.MISS || hit.getBlockPos().equals(p);
    }
    /** Called only for one candidate from the existing cached local crop scan. At most one sight ray. */
    static void observe(ServerLevel level,Villager v,BlockPos p) {
        var s=Villagers.existingSession(v.getUUID());
        if(s==null || v.level()!=level || !v.isAlive() || !Villagers.isManaged(v) || !crop(level,p) || !visible(level,v,p)
                || !VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(p))
                || ConstructionSites.isProtected(level,p) || ConstructionSites.isProtected(level,p.below()))return;
        String kind=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(p).getBlock()).toString();
        if(s.mind.observeFarm(place(level,p),kind,level.getGameTime(),VillagerController.witnesses(level,v)))Villagers.persist(s);
    }
    private static boolean permitted(ServerLevel level,Villager v,Session s,BlockPos p) {
        return level.isLoaded(p) && level.isLoaded(p.below())
                && VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(p))
                && !ConstructionSites.isProtected(level,p) && !ConstructionSites.isProtected(level,p.below())
                && v.position().distanceToSqr(Vec3.atCenterOf(p))<=1024;
    }
    private static BlockPos recalled(ServerLevel level,Villager v,Session s,long now) {
        var home=s.mind.home();
        if(home==null || !home.dimension().equals(level.dimension().identifier().toString()))return null;
        boolean changed=false;
        // Forget only with an actual nearby clear view; do not inspect unseen crop readiness remotely.
        if(now>=s.nextFarmRecallScan) {
          s.nextFarmRecallScan=now+100;
          for(var e:s.mind.observedFarms()) {
            var p=e.place();if(!p.dimension().equals(level.dimension().identifier().toString()))continue;
            var b=new BlockPos(p.x(),p.y(),p.z());
            if(level.isLoaded(b) && visible(level,v,b) && !crop(level,b))changed |= s.mind.forgetFarm(p);
          }
        }
        if(changed)Villagers.persist(s);
        // A different nearer patch must not restart an active trip's total deadline/stall budget.
        if(s.farmJourney.active() && s.rememberedFarmTarget!=null && permitted(level,v,s,s.rememberedFarmTarget)
                && s.mind.observedFarms().stream().anyMatch(e -> e.place().equals(place(level,s.rememberedFarmTarget))))
            return s.rememberedFarmTarget;
        return s.mind.observedFarms().stream().map(e -> e.place())
                .filter(p -> p.dimension().equals(level.dimension().identifier().toString()))
                .map(p -> new BlockPos(p.x(),p.y(),p.z()))
                .filter(p -> permitted(level,v,s,p))
                .min(Comparator.comparingDouble(p -> v.position().distanceToSqr(Vec3.atCenterOf(p)))).orElse(null);
    }

    static String travel(ServerLevel level, Villager v, Session s, long now) {
        var memory = v.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        BlockPos site = recalled(level,v,s,now);
        boolean fromObservation=site!=null;
        if(site==null)site=memory.filter(p -> p.dimension().equals(level.dimension())).map(p -> p.pos()).orElse(null);
        boolean stock = v.getInventory().hasAnyMatching(item -> item.is(Items.WHEAT_SEEDS) || item.is(Items.CARROT)
                || item.is(Items.POTATO) || item.is(Items.BEETROOT_SEEDS));
        if (!AdventureActions.canHarvest(v) || !fromObservation && !stock || site == null || !level.isLoaded(site)
                || !fromObservation && !level.getBlockState(site).is(Blocks.COMPOSTER)
                || !VillageSurvival.withinHomeRange(level, s.mind, Vec3.atCenterOf(site))
                || v.position().distanceToSqr(Vec3.atCenterOf(site)) > 1024) {
            interrupt(s, now); return null;
        }
        boolean wasTravelling = s.farmJourney.active();
        s.rememberedFarmTarget=fromObservation?site:null;
        WorkJourney.Step step = s.farmJourney.advance(site.toShortString(), now,
                v.position().distanceToSqr(Vec3.atCenterOf(site)) <= 4, v.getX(), v.getY(), v.getZ());
        if (step == WorkJourney.Step.WAIT) return null;
        s.action = null; s.before = null; s.succeeded = false;
        if (step == WorkJourney.Step.ARRIVED || step == WorkJourney.Step.FAILED) {
            v.getNavigation().stop(); s.nextCropScan = now; s.nextCraftScan = now;
            s.foodStatus = step == WorkJourney.Step.ARRIVED ? "checking remembered farm" : "remembered farm route stalled";
            if(fromObservation && step==WorkJourney.Step.ARRIVED && wasTravelling)s.farmRecallArrivals++;
            s.farmRecallStatus=s.foodStatus;
            if (step == WorkJourney.Step.ARRIVED && wasTravelling) s.mind.remember("Arrived at remembered farm " + site.toShortString());
            if (step == WorkJourney.Step.FAILED) s.mind.remember("Stopped farm trip without arriving at " + site.toShortString());
            return s.foodStatus;
        }
        if (step == WorkJourney.Step.START) {
            if(fromObservation)s.farmRecallTrips++;
            s.mind.remember("Planned trip to remembered farm " + site.toShortString());
            v.getNavigation().stop(); s.action = null; s.before = null; s.succeeded = false;
            s.nextFarmPath = now;
        }
        if (now >= s.nextFarmPath && WorkNavigation.admit(level, now, v)) {
            s.nextFarmPath = now + 40;
            var target=site;
            // Remembered work is not bounded by combat follow range. The
            // native short search can stop one cell short of a distant farm
            // despite an ordinary permitted route to the same observed crop.
            var path = NativeRegionsScenario.nativeRegions(level)
                ? NativePathLimits.search(v,2,()->WorkNavigation.pathTo(level,v,s,target,1))
                : v.getNavigation().createPath(site, 1);
            boolean allowed=WorkNavigation.staysHome(level,v,s,path);
            s.lastFarmPath=java.util.Map.of("gameTime",now,"target",site.toShortString(),"fromObservation",fromObservation,
                    "nodes",path==null?0:path.getNodeCount(),"canReach",path!=null && path.canReach(),"permitted",allowed);
            if (!allowed || !v.getNavigation().moveTo(path,.65)) {
                if(fromObservation)s.farmRecallRejected++;
                s.farmJourney.reject(now); v.getNavigation().stop();
                s.mind.remember("No permitted route to remembered farm " + site.toShortString());
                s.farmRecallStatus=s.foodStatus = "remembered farm unreachable"; return s.foodStatus;
            }
            s.farmJourney.navigating(site.toShortString(),now,v.getX(),v.getY(),v.getZ());
        }
        s.foodStatus = "returning to remembered farm";
        s.farmRecallStatus=fromObservation?s.foodStatus:"using assigned composter";
        return s.foodStatus;
    }
}
