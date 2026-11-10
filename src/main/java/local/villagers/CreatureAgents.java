package local.villagers;

import java.time.Duration;
import java.util.*;
import local.villagers.display.ActivityLabel;
import local.villagers.learning.LearningMode;
import local.villagers.mind.*;
import local.villagers.persistence.MindPersistence;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

/** Shared persistent minds in additional native bodies. No terrain edits or invented food. */
public final class CreatureAgents {
    public static final String MANAGED="villagers:creature_managed";
    public static final String RELEASED="villagers:creature_released";
    private static final Map<UUID,State> STATES=new HashMap<>();
    private static MindPersistence persistence;
    private static final Random RANDOM=new Random(9471);
    static final class State {
        final AgentMind mind;
        long lastTick,nextSave,started;
        long nextBedSearch,nextFamilyAttempt;
        long nextFamilySignal;
        local.villagers.family.Courtship.Agreement familyAgreement;
        String familyStatus = "not considering a family";
        long familyAttempts;
        String familyBlock = "";
        Map<String,Object> lastBirth=Map.of();
        Action action;
        Observation before;
        BlockPos destination;
        String activity="settling in";
        final IllagerFood.Work foodWork = new IllagerFood.Work();
        final NativeCampWork.State campWork=new NativeCampWork.State();
        State(AgentMind mind,long now) { this.mind=mind;lastTick=now; }
    }
    private CreatureAgents() {}
    public static boolean supported(Mob mob) {
        return mob instanceof AbstractIllager || mob instanceof Enderman || mob instanceof WanderingTrader;
    }
    public static boolean managed(Mob mob) { return supported(mob) && mob.entityTags().contains(MANAGED); }
    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            STATES.clear();
            persistence=new MindPersistence(AgentStore.worldScoped(server.getWorldPath(LevelResource.ROOT)
                .resolve("creature-minds")),Villagers.LOG,Duration.ofSeconds(1));
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity,level)->{
            if (entity instanceof Mob mob && entity.getRemovalReason() == Entity.RemovalReason.DISCARDED)
                IllagerFamilies.releaseBed(level, mob);
            State state=STATES.remove(entity.getUUID());
            if(state!=null && persistence!=null)persistence.queue(state.mind);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{
            if(!(entity instanceof Mob mob) || !managed(mob) || persistence==null)return;
            State state=state(mob,true);
            state.mind.die(entity.level().getGameTime(),source.getMsgId());
            IllagerFamilies.releaseBed((ServerLevel)mob.level(), mob);
            state.mind.remember("Died from "+source.getMsgId()); persistence.queue(state.mind);
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(persistence==null || !WarScenario.active(server.overworld()) || server.getTickCount()%40!=0)return;
            // Upgrade previously created scenario bodies without spawning replacements or granting supplies.
            for(var entity:server.overworld().getAllEntities())if(entity instanceof Mob mob && supported(mob)
                && mob.entityTags().contains(WarScenario.ACTOR) && !managed(mob) && !mob.entityTags().contains(RELEASED))
                adopt(server.overworld(),mob);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{
            if(persistence!=null) {
                STATES.values().forEach(s->persistence.queue(s.mind));
                if(!persistence.close(Duration.ofSeconds(15)))Villagers.LOG.error("Creature mind saves did not finish");
            }
            persistence=null;STATES.clear();
        });
    }
    public static void adopt(ServerLevel level,Mob mob) {
        if(!supported(mob) || persistence==null)throw new IllegalArgumentException("Unsupported creature or server not ready");
        mob.addTag(MANAGED);mob.removeTag(RELEASED);mob.setPersistenceRequired();
        State state=state(mob,true);
        if(state.mind.home()==null)state.mind.home(new Home(level.dimension().identifier().toString(),
            mob.blockPosition().getX(),mob.blockPosition().getY(),mob.blockPosition().getZ()));
        state.mind.remember("Adopted as "+mob.typeHolder().getRegisteredName());persistence.queue(state.mind);
    }
    public static void release(Mob mob) {
        if (mob.level() instanceof ServerLevel level) IllagerFamilies.release(level, mob);
        State state=STATES.remove(mob.getUUID());
        if(state!=null) {
            persistence.queue(state.mind);
            String name=mob.hasCustomName()?mob.getCustomName().getString():null;
            if(ActivityLabel.isLabel(state.mind.name(),name)){mob.setCustomName(null);mob.setCustomNameVisible(false);}
        }
        mob.removeTag(MANAGED);mob.addTag(RELEASED);mob.getNavigation().stop();mob.setTarget(null);
    }
    private static State state(Mob mob,boolean blocking) {
        State state=STATES.get(mob.getUUID());
        if(state!=null || persistence==null)return state;
        AgentMind mind=blocking?persistence.acquire(mob.getUUID()):persistence.tryAcquire(mob.getUUID());
        if(mind==null)return null;
        state=new State(mind,mob.level().getGameTime());STATES.put(mob.getUUID(),state);return state;
    }
    static AgentMind mind(Mob mob) { State s=state(mob,false);return s==null?null:s.mind; }
    static State loadedState(Mob mob) { return state(mob, false); }
    static void persist(State state) { if (persistence != null) persistence.queue(state.mind); }
    static void registerChild(ServerLevel level, Mob baby, AgentMind mind) {
        baby.addTag(MANAGED); baby.setPersistenceRequired();
        State state = new State(mind, level.getGameTime()); STATES.put(baby.getUUID(), state); persist(state);
    }
    public static Map<String,Object> status(Mob mob) {
        State s=state(mob,true);
        var report = new LinkedHashMap<String,Object>(Map.of("id",mob.getUUID().toString(),"species",mob.typeHolder().getRegisteredName(),
            "name",s.mind.name(),"activity",s.activity,"food",s.mind.foodLevel(),"fatigue",s.mind.fatigue(),
            "learningUpdates",s.mind.learningUpdates(),"memories",s.mind.memories(),"foodWork",s.foodWork.status(),"family",IllagerFamilies.status(mob,s)));
        report.put("health", mob.getHealth());
        report.put("campWork",s.campWork.status());
        report.put("mainhand", mob.getMainHandItem().toString()); report.put("offhand", mob.getOffhandItem().toString());
        if (mob instanceof Pillager pillager) report.put("inventory", HarvestMaterials.snapshot(pillager.getInventory()).stream().map(ItemStack::toString).toList());
        else if (mob instanceof AbstractVillager trader) report.put("inventory", HarvestMaterials.snapshot(trader.getInventory()).stream().map(ItemStack::toString).toList());
        return report;
    }
    private static boolean eats(Mob mob) { return !(mob instanceof Enderman); }
    static ItemStack ration(Mob mob) {
        if(mob instanceof AbstractVillager trader) {
            for(int i=0;i<trader.getInventory().getContainerSize();i++) {
                ItemStack stack=trader.getInventory().getItem(i);if(stack.has(DataComponents.FOOD))return stack;
            }
        } else if(mob instanceof Pillager pillager) {
            for(int i=0;i<pillager.getInventory().getContainerSize();i++) {
                ItemStack stack=pillager.getInventory().getItem(i);if(stack.has(DataComponents.FOOD))return stack;
            }
        }
        return mob.getOffhandItem().has(DataComponents.FOOD)?mob.getOffhandItem():ItemStack.EMPTY;
    }
    private static Observation observe(Mob mob,State s,boolean danger) {
        boolean found=eats(mob) && (!ration(mob).isEmpty() || !mob.level().getEntitiesOfClass(ItemEntity.class,
            mob.getBoundingBox().inflate(10),item->item.getItem().has(DataComponents.FOOD) && mob.hasLineOfSight(item)).isEmpty());
        return new Observation(eats(mob)?s.mind.hunger():0,s.mind.fatigue(),s.mind.loneliness(),
            mob.getHealth()/mob.getMaxHealth(),danger,false,found,s.mind.home()!=null);
    }
    public static void tick(ServerLevel level,Mob mob) {
        if(!managed(mob) || persistence==null || !mob.isAlive() || (mob.tickCount+mob.getId())%10!=0)return;
        State s=state(mob,false);if(s==null)return;
        long now=level.getGameTime(),elapsed=Math.clamp(now-s.lastTick,0,200);s.lastTick=now;
        s.mind.advanceDrives(elapsed,eats(mob)?Villagers.config().baseExhaustionPerDay():0);
        if(now>=s.nextSave){persistence.queue(s.mind);s.nextSave=now+600;}
        if(mob instanceof WanderingTrader trader && trader.isTrading()) {
            mob.getNavigation().stop();s.activity="trading";return;
        }
        if(eats(mob) && (s.mind.foodLevel()<=16||MealPolicy.needsRecoveryFood(s.mind.foodLevel(),mob.getHealth(),mob.getMaxHealth())) && !ration(mob).isEmpty()) {
            ItemStack real=ration(mob);var food=real.get(DataComponents.FOOD);
            interrupt(s);
            real.shrink(1);s.mind.eat(food.nutrition(),food.saturation());s.mind.remember("Ate owned food");
        }
        if(eats(mob) && now%80<10) {
            if(s.mind.foodLevel()==0)mob.hurtServer(level,mob.damageSources().starve(),1);
            else if(s.mind.foodLevel()>=18 && mob.getHealth()<mob.getMaxHealth()) {
                mob.heal(1);s.mind.exert(6);
            }
        }
        if(!mob.isAlive())return;
        IllagerFamilies.care(level, mob, s);
        String combat=WarCombat.tick(level,mob,s.mind);
        if(combat!=null){interrupt(s);label(mob,s,combat);return;}
        LivingEntity attacker=mob.getLastHurtByMob();
        if(attacker!=null && (!attacker.isAlive() || mob.tickCount-mob.getLastHurtByMobTimestamp()>200))attacker=null;
        if(attacker!=null && mob instanceof Enderman) {
            mob.setTarget(attacker);WarCombat.engage(level,mob,attacker);label(mob,s,"defending itself");return;
        }
        LivingEntity danger=attacker;
        if(danger==null && mob instanceof WanderingTrader)danger=level.getEntitiesOfClass(AbstractIllager.class,
            mob.getBoundingBox().inflate(16),e->e.isAlive() && mob.hasLineOfSight(e)).stream()
            .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        if(danger!=null || mob.isOnFire()) {
            interrupt(s);
            Vec3 away=danger==null?mob.position().add(8,0,0):mob.position().subtract(danger.position()).normalize().scale(10).add(mob.position());
            if(NativeIllagers.member(mob))NativeIllagers.navigateLocal(level,mob,away.x,away.z);
            else WarCombat.navigate(level,mob,away.x,away.z);
            label(mob,s,"escaping danger");return;
        }
        boolean campReserve=mob instanceof Pillager p&&NativeIllagers.member(mob)&&IllagerFood.bread(p)>=6&&s.mind.foodLevel()>=14;
        if(campReserve){String work=NativeCampWork.tick(level,mob,s,now);if(work!=null){interrupt(s);label(mob,s,work);return;}}
        String foodWork = IllagerFood.tick(level, mob, s.mind, s.foodWork);
        if (foodWork != null) { interrupt(s); label(mob,s,foodWork); return; }
        String campWork=campReserve?null:NativeCampWork.tick(level,mob,s,now);
        if(campWork!=null){interrupt(s);label(mob,s,campWork);return;}
        String foodRequest = IllagerFood.seekMeal(level, mob, s.mind);
        if (foodRequest != null) { interrupt(s); label(mob,s,foodRequest); return; }
        String family = IllagerFamilies.seek(level, mob, s);
        if (family != null) { interrupt(s); label(mob,s,family); return; }
        Observation current=observe(mob,s,false);
        if(s.action!=null && now-s.started>=80) {
            if(Villagers.LEARNING_MODE!=LearningMode.FROZEN)s.mind.learn(s.before,s.action,current);
            interrupt(s);
        }
        if(s.action==null) {
            s.before=current;s.action=s.mind.choose(current,RANDOM);s.started=now;
            s.destination=null;
        }
        switch(s.action) {
            case REST -> {mob.getNavigation().stop();s.mind.rest();label(mob,s,"resting");}
            case FORAGE -> {
                // Owned meals above are authored care, not credited to a forage action.
                var food=level.getEntitiesOfClass(ItemEntity.class,mob.getBoundingBox().inflate(10),
                    item->!item.getItem().isEmpty() && item.getItem().has(DataComponents.FOOD) && mob.hasLineOfSight(item))
                    .stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
                if(food!=null && mob.distanceToSqr(food)<4) {
                    var properties=food.getItem().get(DataComponents.FOOD);food.getItem().shrink(1);
                    s.mind.eat(properties.nutrition(),properties.saturation());if(food.getItem().isEmpty())food.discard();
                    s.mind.remember("Ate food found on the ground");
                } else if(food!=null){
                    if(NativeIllagers.member(mob))NativeIllagers.approach(level,mob,food,1);
                    else mob.getNavigation().moveTo(food,1);
                }
                label(mob,s,"foraging");
            }
            case SOCIALIZE -> {
                var peer=level.getEntitiesOfClass(Mob.class,mob.getBoundingBox().inflate(10),
                    other->other!=mob && managed(other) && WarCombat.faction(other)==WarCombat.faction(mob)
                        && other.isAlive() && mob.hasLineOfSight(other)).stream()
                    .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
                if(peer!=null && mob.distanceToSqr(peer)<9) {
                    mob.getLookControl().setLookAt(peer,30,30);s.mind.socialize();
                    if(now-s.started<20)s.mind.remember("Met "+peer.getUUID());
                    IllagerFamilies.consider(level, mob, s, peer);
                } else if(peer!=null){
                    if(NativeIllagers.member(mob))NativeIllagers.approach(level,mob,peer,.8);
                    else mob.getNavigation().moveTo(peer,.8);
                }
                label(mob,s,"seeking company");
            }
            case EXPLORE -> {
                if(s.destination==null) {
                    double angle=RANDOM.nextDouble()*Math.PI*2;
                    Home home=s.mind.home();
                    int step = local.villagers.war.WarMap.ROAM_STEP, range = local.villagers.war.WarMap.HOME_ROAM_RADIUS;
                    int x=(int)(mob.getX()+Math.cos(angle)*step),z=(int)(mob.getZ()+Math.sin(angle)*step);
                    if(home!=null && mob.distanceToSqr(home.x(),home.y(),home.z())>range*range){x=home.x();z=home.z();}
                    s.destination=new BlockPos(x,mob.getBlockY(),z);
                }
                if(NativeIllagers.member(mob))NativeIllagers.navigateLocal(level,mob,s.destination.getX(),s.destination.getZ());
                else WarCombat.navigate(level,mob,s.destination.getX(),s.destination.getZ());
                label(mob,s,"exploring");
            }
            case FLEE -> label(mob,s,"watching for danger");
        }
    }
    private static void interrupt(State s) {s.action=null;s.before=null;s.destination=null;}
    private static void label(Mob mob,State s,String activity) {
        s.activity=activity;
        String current=mob.hasCustomName()?mob.getCustomName().getString():null;
        if(ActivityLabel.ownedBy(s.mind.name(),current)) {
            mob.setCustomName(Component.literal(ActivityLabel.text(s.mind.name(),activity)));mob.setCustomNameVisible(true);
        }
    }
}
