package local.villagers;

import java.util.*;
import local.villagers.mind.AgentMind;
import local.villagers.war.WarMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;

/** Authored faction objectives, with local perception and native paths/weapons. */
public final class WarCombat {
    private static final String MARCHED="villagers:war_marched";
    private static final String RETREATING="villagers:war_retreating";
    private static final Map<UUID,State> STATES=new HashMap<>();
    private static final class State {long attackAt,pathAt,chargeAt=-1;boolean retreating;}
    private WarCombat() {}
    static void clear(){STATES.clear();}
    static void forget(UUID id){STATES.remove(id);}
    public static WarMap.Faction faction(Mob mob) {
        if(!mob.entityTags().contains(WarScenario.ACTOR))return WarMap.Faction.NEUTRAL;
        return mob instanceof Villager?WarMap.Faction.VILLAGERS:mob instanceof AbstractIllager?WarMap.Faction.ILLAGERS:WarMap.Faction.NEUTRAL;
    }
    public static boolean enemies(Mob a,Mob b) {
        return a!=b && faction(a)!=WarMap.Faction.NEUTRAL && faction(b)!=WarMap.Faction.NEUTRAL && faction(a)!=faction(b);
    }
    static WarMap.Settlement home(Mob mob) {
        for(int i=0;i<WarMap.SETTLEMENTS.size();i++)if(mob.entityTags().contains(WarScenario.HOME+i))return WarMap.SETTLEMENTS.get(i);
        return NativeIllagers.home(mob);
    }
    public static String tick(ServerLevel level,Mob mob,AgentMind mind) {
        if (IllagerFamilies.juvenile(mob)) return null;
        if(!WarCampaign.ready(level) || faction(mob)==WarMap.Faction.NEUTRAL || !mob.entityTags().contains(WarScenario.ACTOR))return null;
        if(mob instanceof Villager villager && villager.isTrading())return null;
        if(mob instanceof Villager villager && villager.isBaby())return null;
        State state=STATES.computeIfAbsent(mob.getUUID(),id->new State());
        WarMap.Settlement home=home(mob);if(home==null)return null;
        long now=level.getGameTime();
        // Give founders time to settle. This uses saved world time and never resets on reload.
        if(now<200)return null;
        boolean soldier=mob.entityTags().contains(WarScenario.SOLDIER) && !AdventurePreparation.nominated(mob);
        state.retreating=mob.entityTags().contains(RETREATING);
        if(state.retreating && mob.getHealth()/mob.getMaxHealth()>=0.7 && mind.hunger()<0.65 && mind.fatigue()<0.5)state.retreating=false;
        if(mob.getHealth()/mob.getMaxHealth()<0.3 || mind.hunger()>0.85 || mind.fatigue()>0.85)state.retreating=true;
        if(state.retreating)mob.addTag(RETREATING);else mob.removeTag(RETREATING);
        if(state.retreating) {
            mob.setTarget(null);stopCharging(mob,state);
            if(Math.hypot(mob.getX()-home.x(),mob.getZ()-home.z())<8) {
                // Keep the retreat latch until recovery, but hand control back to local care.
                // Returning a war activity here starves farming, forage and family routines.
                mob.getNavigation().stop();
                return null;
            }
            else navigate(level,mob,home.x(),home.z());
            return "retreating to "+home.name();
        }
        // The director already knows each garrison. Rebuild the eight-adult founding
        // population before launching further raids; juveniles are never replacements.
        boolean rebuilding = soldier && WarCampaign.adultResidents(level,home)<8;
        if (rebuilding && Math.hypot(mob.getX()-home.x(),mob.getZ()-home.z())>32) {
            mob.setTarget(null);stopCharging(mob,state);
            navigate(level,mob,home.x(),home.z());return "regrouping at "+home.name();
        }
        Mob enemy=level.getEntitiesOfClass(Mob.class,mob.getBoundingBox().inflate(28),
            other->other.isAlive() && enemies(mob,other) && mob.hasLineOfSight(other)).stream()
            .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        if(enemy!=null) {
            if(mob instanceof Villager villager && villager.isSleeping())villager.stopSleeping();
            engage(level,mob,enemy);return "fighting "+enemy.getName().getString().split(" · ")[0];
        }
        mob.setTarget(null);stopCharging(mob,state);
        if(!soldier || rebuilding)return null;
        if(!mob.entityTags().contains(MARCHED) && Math.hypot(mob.getX()-home.x(),mob.getZ()-home.z())>32) {
            mob.addTag(MARCHED);mind.remember("Marched from "+home.name());WarCampaign.marched(level,mob);
        }
        // Strategic settlement locations are authored knowledge. Individuals only acquire visible targets locally.
        var target=WarMap.SETTLEMENTS.stream().filter(t->t.faction()!=faction(mob) && WarCampaign.occupied(level,t))
            .min(Comparator.comparingDouble(t->Math.pow(mob.getX()-t.x(),2)+Math.pow(mob.getZ()-t.z(),2))).orElse(null);
        if(target==null) {
            navigate(level,mob,home.x(),home.z());mind.rest();return "returning after battle";
        }
        if(mob instanceof Villager villager && villager.isSleeping())villager.stopSleeping();
        navigate(level,mob,target.x(),target.z());return "marching toward "+target.name();
    }
    private static void stopCharging(Mob mob,State state) {
        if(mob instanceof Pillager pillager && state.chargeAt>=0){pillager.stopUsingItem();pillager.setChargingCrossbow(false);state.chargeAt=-1;}
    }
    public static void engage(ServerLevel level,Mob mob,LivingEntity target) {
        State state=STATES.computeIfAbsent(mob.getUUID(),id->new State());long now=level.getGameTime();
        mob.setTarget(target);mob.setAggressive(true);mob.getLookControl().setLookAt(target,30,30);
        boolean visible=mob.hasLineOfSight(target);
        if(mob instanceof Pillager pillager && mob.getMainHandItem().is(Items.CROSSBOW)) {
            if(mob.distanceToSqr(target)>14*14 || !visible){navigate(level,mob,target.getX(),target.getZ());return;}
            mob.getNavigation().stop();
            if(now<state.attackAt)return;
            var crossbow=mob.getMainHandItem();
            if(CrossbowItem.isCharged(crossbow)) {
                pillager.performRangedAttack(target,1);state.attackAt=now+40;state.chargeAt=-1;pillager.setChargingCrossbow(false);
            } else if(state.chargeAt<0) {
                pillager.startUsingItem(InteractionHand.MAIN_HAND);pillager.setChargingCrossbow(true);state.chargeAt=now;
            } else if(now-state.chargeAt>=CrossbowItem.getChargeDuration(crossbow,mob)) {
                pillager.releaseUsingItem();pillager.setChargingCrossbow(false);state.chargeAt=-1;
            }
            return;
        }
        if(!visible || !mob.isWithinMeleeAttackRange(target)){navigate(level,mob,target.getX(),target.getZ());return;}
        mob.getNavigation().stop();
        if(now<state.attackAt || !mob.canAttack(target))return;
        if(!(mob instanceof Enderman) && mob.getMainHandItem().isEmpty())return;
        double speed=mob.getAttributes().hasAttribute(Attributes.ATTACK_SPEED)?mob.getAttributeValue(Attributes.ATTACK_SPEED):1;
        state.attackAt=now+local.villagers.capability.DefensePolicy.cooldownTicks(speed);
        mob.swingForAttack(InteractionHand.MAIN_HAND);
        if(mob.doHurtTarget(level,target)) {
            mob.getMainHandItem().postHurtEnemy(target,mob);
            if(mob instanceof Villager villager)Villagers.exert(villager,0.1f);
        }
    }
    /** Advance through short complete native paths. Long routes never load chunks or teleport actors. */
    public static boolean navigate(ServerLevel level,Mob mob,double x,double z) {
        State state=STATES.computeIfAbsent(mob.getUUID(),id->new State());long now=level.getGameTime();
        if(now<state.pathAt)return !mob.getNavigation().isDone();state.pathAt=now+20;
        double dx=x-mob.getX(),dz=z-mob.getZ(),distance=Math.hypot(dx,dz);
        if(distance<2){mob.getNavigation().stop();return true;}
        double angle=Math.atan2(dz,dx),step=Math.min(10,distance);
        for(double offset:new double[]{0,0.5,-0.5,1,-1}) {
            int px=(int)Math.floor(mob.getX()+Math.cos(angle+offset)*step);
            int pz=(int)Math.floor(mob.getZ()+Math.sin(angle+offset)*step);
            if(!WarMap.contains(px,pz) && WarScenario.active(level))continue;
            if(!level.hasChunk(px>>4,pz>>4))continue;
            int py=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,px,pz);
            var path=mob.getNavigation().createPath(new BlockPos(px,py,pz),1);
            if(path!=null && path.canReach())return mob.getNavigation().moveTo(path,1);
        }
        return false;
    }
}
