package local.villagers;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.npc.villager.Villager;

/** A loud native voice invites investigation; it does not convey understood words or transfer food. */
final class NativeCampCalls {
    static final float VOLUME=6;
    static final float INJURED_VOLUME=10;
    record Call(BlockPos position,long expires,float volume){}
    static final class State {
        final LinkedHashMap<UUID,Call> heard=new LinkedHashMap<>();
        NativeBodyRoutes.State routes=new NativeBodyRoutes.State();
        UUID pursuing;
        long pathAt,listenUntil,pursueUntil;int received,failedSearches;Map<String,Object> receipt=Map.of();
        final Map<UUID,Long> retryAt=new HashMap<>();
        net.minecraft.world.level.pathfinder.Path walking;
        UUID walkingSpeaker;
        Map<String,Object> walkingStep=Map.of();
        final local.villagers.navigation.RouteProgress progress=new local.villagers.navigation.RouteProgress(80);
        final NativeCampEntrance.State entrance=new NativeCampEntrance.State();
        void hear(UUID speaker,BlockPos at,long now){
            hear(speaker,at,now,VOLUME);
        }
        void hear(UUID speaker,BlockPos at,long now,float volume){
            if(!heard.containsKey(speaker)&&heard.size()>=8){
                var drop=heard.entrySet().stream().filter(e->!e.getKey().equals(pursuing))
                    .min(Comparator.comparingDouble(e->e.getValue().volume())).orElse(heard.firstEntry());
                // A later ordinary call cannot erase the critical voice a
                // provisioner heard while still fetching its first ration.
                if(drop.getValue().volume()>volume){received++;return;}
                heard.remove(drop.getKey());
            }
            heard.put(speaker,new Call(at.immutable(),now+1200,volume));received++;
        }
        UUID nearest(net.minecraft.world.phys.Vec3 at,long now){
            heard.values().removeIf(c->now>=c.expires());
            return heard.entrySet().stream().min(Comparator.<Map.Entry<UUID,Call>>comparingDouble(e->-e.getValue().volume())
                .thenComparingDouble(e->at.distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(e.getValue().position()))))
                .map(Map.Entry::getKey).orElse(null);
        }
        UUID select(net.minecraft.world.phys.Vec3 at,long now){
            heard.values().removeIf(c->now>=c.expires());retryAt.keySet().retainAll(heard.keySet());
            var current=pursuing==null?null:heard.get(pursuing);
            if(current!=null&&now>=pursueUntil)retryAt.put(pursuing,now+400);
            var best=heard.entrySet().stream().filter(e->now>=retryAt.getOrDefault(e.getKey(),0L))
                .min(Comparator.<Map.Entry<UUID,Call>>comparingDouble(e->-e.getValue().volume())
                    .thenComparingDouble(e->at.distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(e.getValue().position()))))
                .map(Map.Entry::getKey).orElse(null);
            // Nearby callers emit on different ticks. Give an equally urgent,
            // much closer voice one short initial selection window, before
            // committing to the long detour. Later movement cannot make the
            // guard reverse its trip merely because another caller is closer.
            boolean nearerInitialAlarm=current!=null&&best!=null&&!best.equals(pursuing)
                &&now<pursueUntil-1160&&current.volume()>=INJURED_VOLUME
                &&heard.get(best).volume()==current.volume()
                &&at.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(heard.get(best).position()))*4
                    <at.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(current.position()));
            if(current!=null&&now<pursueUntil&&now>=retryAt.getOrDefault(pursuing,0L)
                &&(best==null||heard.get(best).volume()<=current.volume())&&!nearerInitialAlarm)return pursuing;
            if(!Objects.equals(best,pursuing)){pursuing=best;pursueUntil=now+1200;}
            return best;
        }
        void reject(UUID id,long now){retryAt.put(id,now+400);pursuing=null;}
        Map<String,Object> status(){return Map.of("heard",received,"waiting",heard.size(),"target",pursuing==null||!heard.containsKey(pursuing)?"":heard.get(pursuing).position().toShortString(),"lastHeard",receipt,"bodyRoutes",routes.status(),"entrance",entrance.status(),"tripUntil",pursueUntil,"deferred",retryAt.size(),"walkingStep",walkingStep);}
    }
    static double radius(double hearing,double nativeRange){
        return radius(hearing,nativeRange,VOLUME);
    }
    static double radius(double hearing,double nativeRange,float volume){
        return Double.isFinite(hearing)&&hearing>0&&Double.isFinite(nativeRange)&&nativeRange>0
            &&Float.isFinite(volume)&&volume>0?Math.min(hearing*volume,nativeRange):0;
    }
    static boolean needsCall(int food,float health,float maxHealth){
        return food>=0&&(food<=6||local.villagers.mind.MealPolicy.needsRecoveryFood(food,health,maxHealth));
    }
    static float volume(int food,float health,float maxHealth){
        // A caller near death must be distinguishable from a newly injured
        // hungry caller. Listeners still receive only the actual public sound.
        // At half health, starvation leaves only a short physical supply
        // round. Ask with the critical voice now, while the guard can still
        // gather ingredients, craft and travel to a different camp wing.
        if(food<=2&&health>0&&maxHealth>0&&health<=maxHealth*.5f)return 14;
        return health>0&&maxHealth>0&&health<maxHealth?(food==0?12:INJURED_VOLUME):food<=2?8:VOLUME;
    }
    static boolean heardInjury(State state,long now){
        return louderInjury(state,now,0);
    }
    static boolean louderInjury(State state,long now,float than){
        return state.heard.entrySet().stream().anyMatch(e->now<e.getValue().expires()&&e.getValue().volume()>=INJURED_VOLUME
            &&e.getValue().volume()>than&&now>=state.retryAt.getOrDefault(e.getKey(),0L));
    }
    static double listeningRadius(){return Math.max(0,Math.min(local.villagers.relations.FoodRequest.SEARCH_RADIUS,Villagers.config().hearingRadius())-1);}
    static net.minecraft.world.level.pathfinder.Path listeningPath(ServerLevel level,Pillager listener,BlockPos voice){
        double radius=listeningRadius();if(radius<=0)return null;
        var gate=CampBeds.pathToStands(level,listener,NativeCampMeals.meetingGateStands(level,listener,voice));
        int r=(int)Math.ceil(radius);var stands=new ArrayList<BlockPos>();
        for(var p:BlockPos.betweenClosed(voice.offset(-r,-3,-r),voice.offset(r,3,r))){
            if(!level.isLoaded(p.above())||!NativeCampWork.local(level,listener,p)||!NativeIllagers.insideCamp(level,listener,p)
                ||!level.getFluidState(p).isEmpty()||!level.getFluidState(p.below()).isEmpty())continue;
            double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,p);
            var feet=new net.minecraft.world.phys.Vec3(p.getX()+.5,floor,p.getZ()+.5);
            // The public sound identifies a block. Its center is at most
            // sqrt(3)/2 from the speaker, so the one-block margin remains
            // inside the real word-hearing radius without knowing its body.
            if(feet.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(voice))>radius*radius)continue;
            var body=listener.getBoundingBox().move(feet.subtract(listener.position()));
            if(level.noCollision(listener,body)&&!level.noBlockCollision(listener,body.move(0,-.03,0)))stands.add(p.immutable());
        }
        // Hearing the words needs only an actual supported listening stand.
        // A gate may be much farther around the building; compare complete
        // native routes before committing to that detour. Handoffs still need
        // their separate real lever, ray and hand-contact checks.
        var nearby=CampBeds.pathToStands(level,listener,stands);
        return nearby!=null&&(gate==null||nearby.getNodeCount()<gate.getNodeCount())?nearby:gate;
    }
    static void emit(ServerLevel level,Villager speaker,Session s,long now){
        if(!needsCall(s.mind.foodLevel(),speaker.getHealth(),speaker.getMaxHealth())||speaker.isSilent()||now<s.nextCampCall)return;
        if(speaker.isSleeping())speaker.stopSleeping();
        boolean critical=s.mind.foodLevel()<=2||speaker.getHealth()<speaker.getMaxHealth()*.5;
        float volume=volume(s.mind.foodLevel(),speaker.getHealth(),speaker.getMaxHealth());
        var sound=SoundEvents.VILLAGER_NO;double range=radius(Villagers.config().hearingRadius(),sound.getRange(volume),volume);
        // Repeat an urgent public voice often enough for a newly loaded or
        // passing provisioner to hear it before starvation outruns the trip.
        // This still conveys only a native sound at the speaker's location.
        speaker.playSound(sound,volume,1);s.nextCampCall=now+(critical?100:s.mind.foodLevel()<=4?200:1200);s.mind.exert(.05f);
        for(var listener:level.getEntitiesOfClass(Pillager.class,speaker.getBoundingBox().inflate(range),
            p->p.isAlive()&&CreatureAgents.managed(p)&&p.getTarget()!=speaker&&speaker.distanceToSqr(p)<=range*range
                &&NativeCampMeals.sameCamp(level,p,speaker,s))){
            var state=CreatureAgents.loadedState(listener);if(state==null)continue;
            state.campMeals.calls.hear(speaker.getUUID(),speaker.blockPosition(),now,volume);
            state.campMeals.calls.receipt=Map.of("gameTime",now,"speaker",speaker.getUUID().toString(),
                "position",speaker.blockPosition().toShortString(),"sound",sound.location().toString(),"volume",volume,
                "nativeRange",sound.getRange(volume),"heardRange",range,"distance",speaker.distanceTo(listener));
            state.mind.remember("Heard a loud camp call at "+speaker.blockPosition().toShortString());
        }
    }
    static String investigate(ServerLevel level,Pillager listener,NativeCampMeals.State meals,long now){
        var w=meals.calls;w.routes=NativeBodyRoutes.state(listener);
        var opening=NativeCampEntrance.work(level,listener,w.entrance,now);if(opening!=null)return opening;
        var previous=w.pursuing;var id=w.select(listener.position(),now);
        if(id==null){w.pursuing=null;return null;}
        if(!id.equals(previous)){w.listenUntil=0;w.pathAt=0;w.failedSearches=0;}
        var call=w.heard.get(id);
        if(listener.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(call.position()))<=listeningRadius()*listeningRadius()){
            listener.getNavigation().stop();
            if(w.listenUntil==0)w.listenUntil=now+220;
            if(now>=w.listenUntil){w.heard.remove(id);w.listenUntil=0;}
            return "listening near the loud camp call";
        }
        w.listenUntil=0;
        var current=listener.getNavigation().getPath();
        if(current!=null&&!current.isDone()){
            w.walkingStep=Map.of("gameTime",now,"position",listener.position().toString(),
                "onGround",listener.onGround(),"nextIndex",current.getNextNodeIndex(),"nextNode",current.getNextNodePos().toShortString(),
                "wanted",List.of(listener.getMoveControl().getWantedX(),listener.getMoveControl().getWantedY(),listener.getMoveControl().getWantedZ()));
            // A fresh search during an ordinary stair jump may reject its
            // start and discard a long, already-verified detour. Keep the real
            // native walk until arrival, changed speaker, or measured stall.
            boolean stalled=w.progress.stalledAdvance(id.toString(),now,current.getNextNodeIndex());
            if(!stalled&&current==w.walking&&id.equals(w.walkingSpeaker)&&BoatTravel.dryPath(level,current))
                return "following the verified native route toward the heard camp voice";
            if(!listener.onGround()&&!stalled)return "finishing a native step toward the heard camp voice";
            if(stalled){
                if(!current.isDone())NativeBodyRoutes.failedStep(listener,current.getNextNodePos(),now);
                listener.getNavigation().stop();w.walking=null;w.pathAt=now;
            }
        }
        if(now>=w.pathAt){
            w.pathAt=now+40;
            boolean moved=NativeBodyRoutes.search(level,listener,w.routes,now,()->{
                // First get within spoken hearing. A barred room may have no
                // three-block physical stand, although its words are audible
                // from the corridor. Hearing still cannot authorize a gift.
                var path=listeningPath(level,listener,call.position());
                if(path!=null&&NativeCampMeals.followRoute(listener,meals,path,now,call.volume()>VOLUME))return true;
                return NativeIllagers.returnHome(level,listener,call.position())
                    &&NativeCampMeals.followRoute(listener,meals,listener.getNavigation().getPath(),now,call.volume()>VOLUME);
            });
            if(!moved){
                listener.getNavigation().stop();
                if(++w.failedSearches<3)return "checking another safe stand near the heard camp voice";
                if(NativeCampEntrance.begin(level,listener,w.entrance,call.position(),now))return "planning a narrow entrance toward the heard camp voice";
                w.reject(id,now);return null;
            }
            w.failedSearches=0;w.walking=listener.getNavigation().getPath();w.walkingSpeaker=id;w.progress.reset();
        }
        return "investigating a loud native camp call";
    }
    private NativeCampCalls(){}
}
