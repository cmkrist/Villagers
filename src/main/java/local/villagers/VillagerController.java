package local.villagers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.random.RandomGenerator;
import java.util.stream.StreamSupport;
import local.villagers.display.ActivityLabel;
import local.villagers.display.MemoryText;
import local.villagers.mind.Action;
import local.villagers.mind.Episode;
import local.villagers.mind.Home;
import local.villagers.mind.Metabolism;
import local.villagers.mind.MealPolicy;
import local.villagers.mind.Observation;
import local.villagers.mind.Place;
import local.villagers.mind.PlayerImpression;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;

/** The per-villager decision loop: observe, choose an action, execute it, and report outcomes to the learner. */
final class VillagerController {
    private static final RandomGenerator RANDOM = new Random(937);
    private static final int ACTION_TIMEOUT_TICKS = 160;
    private static long decisions, completed, safetyInterrupts, thinkNanos;

    private VillagerController() {}

    static void resetStatistics() { decisions = completed = safetyInterrupts = thinkNanos = 0; }

    static void tick(ServerLevel level, Villager v) {
        if (!Villagers.ready() || !v.isAlive()) return;
        // Replacing the brain also removes its native Swim behavior. Keep ordinary jump-control buoyancy.
        v.getNavigation().setCanFloat(true);
        if (v.isInWater() && !v.isPassenger()) v.getJumpControl().jump();
        if ((v.tickCount + v.getId()) % 20 == 0) AdventureActions.prepare(level, v);
        AdventureActions.tickDoors(level, v);
        // Think every ten simulation ticks, staggered; wall-time frequency follows the configured world TPS.
        if ((v.tickCount + v.getId()) % 10 != 0) return;
        // The mind is read from disk on the save thread; idle until it arrives rather than stall the server.
        Session s = Villagers.sessionIfLoaded(v, level);
        if (s == null) return;
        long start = System.nanoTime();
        try {
            think(level, v, s);
        } finally {
            thinkNanos += System.nanoTime() - start;
        }
    }

    private static void think(ServerLevel level, Villager v, Session s) {
        long now = level.getGameTime();
        if (s.action != Action.SOCIALIZE) s.socialVisit.interrupt(now);
        int elapsed = (int) Math.min(200, Math.max(0, now - s.lastTick));
        s.mind.advanceDrives(elapsed, Villagers.config().baseExhaustionPerDay());
        s.lastTick = now;
        exertFromMovement(v, s);
        LivingEntity threat = level.getEntitiesOfClass(Monster.class, v.getBoundingBox().inflate(12),
                m -> threatens(m,v)
                    && v.hasLineOfSight(m)).stream().min(Comparator.comparingDouble(v::distanceToSqr)).orElse(null);
        if (v.getLastHurtByMob() != null && v.tickCount - v.getLastHurtByMobTimestamp() < 100
                && v.getLastHurtByMob().isAlive()) threat = v.getLastHurtByMob();
        eatOwnedRation(level, v, s, now, threat==null);
        metabolize(level, v, s, elapsed);
        if (!v.isAlive()) return; // Starved.
        Speech.ensureLanguage(level, v, s);
        s.mind.updateMood(now, v.getHealth() / v.getMaxHealth());
        if (now >= s.nextSave) { Villagers.persist(s); s.nextSave = now + 1200; }
        Home home = s.mind.home();
        if (home != null && home.dimension().equals(dimensionId(level))
                && level.isLoaded(pos(home)) && (!validHome(level, home) || !TraderRecruitment.canClaim(level, v, pos(home)))) {
            if (v.isSleeping()) v.stopSleeping();
            v.releasePoi(MemoryModuleType.HOME);
            v.getBrain().eraseMemory(MemoryModuleType.HOME);
            s.mind.home(null);
        }
        if (s.mind.home() == null && now >= s.nextHomeSearch) { claimHome(level, v, s); s.nextHomeSearch = now + 1200; }
        String storedMeal=NativeFoodStorage.work(level,v,s,threat,now);
        if(storedMeal!=null){s.action=null;s.before=null;label(v,s,storedMeal);return;}
        // Grain can still require a long route and recipe. A critical body may
        // ask visible company for a real meal before repeating that work trip.
        if(MealPolicy.needsRecoveryFood(s.mind.foodLevel(),v.getHealth(),v.getMaxHealth())&&threat==null&&!v.isOnFire()
                &&!v.isTrading()&&!v.isPassenger()&&!v.isInWater()){
            String help=FoodHelp.seek(level,v,s,now);
            if(help!=null){s.action=null;s.before=null;label(v,s,help);return;}
            if(s.mind.foodLevel()>0&&now<s.nextFoodRequest){
                // A fed patient can safely rest between actual food requests
                // instead of repeatedly taking a risky grain/workbench trip.
                s.action=null;s.before=null;label(v,s,RecoveryRest.work(level,v,s,now));return;
            }
        }
        // Shelter must not make an owner starve with usable meal ingredients in their bag.
        // This is only possessed grain at a real station; danger still takes precedence.
        boolean urgentGrain=s.mind.foodLevel()<=6||s.mind.foodLevel()<18&&v.getHealth()<v.getMaxHealth();
        if(urgentGrain&&threat==null&&!v.isOnFire()&&!v.isTrading()&&!v.isPassenger()&&!v.isInWater()
                &&(HayForaging.hasHay(v.getInventory())||BreadCrafting.wheatCount(v.getInventory())>=3)
                &&BreadCrafting.table(level,v,s)!=null){
            if(v.isSleeping())v.stopSleeping();
            LocalTrade.interrupt(s,now);FoodDelivery.interrupt(s,now);PaidDelivery.interrupt(s,now);FoodHelp.interrupt(s,now);
            s.action=null;s.before=null;forage(level,v,s,now);label(v,s,"preparing an urgent meal from owned grain");return;
        }
        Observation current = observe(level, v, s, threat != null);
        if (s.action != null && s.succeeded && s.before != null) finishAttempt(level, v, s);

        String urgentCrop=UrgentCropMeal.work(level,v,s,threat,now);
        if(urgentCrop!=null){s.action=null;s.before=null;label(v,s,urgentCrop);return;}
        String waterExit=WaterEscape.work(level,v,s,threat,now);
        if(waterExit!=null){s.action=null;s.before=null;label(v,s,waterExit);return;}

        if(urgentGrain&&current.foodAvailable()&&threat==null&&!v.isOnFire()&&!v.isTrading()
                &&!v.isPassenger()&&!v.isInWater()){
            if(v.isSleeping())v.stopSleeping();
            s.action=null;s.before=null;forage(level,v,s,now);label(v,s,"seeking an urgent real meal");return;
        }

        String foodAccess=FoodAccess.work(level,v,s,threat,current.foodAvailable(),now);
        if(foodAccess!=null){
            LocalTrade.interrupt(s,now);FoodDelivery.interrupt(s,now);PaidDelivery.interrupt(s,now);FoodHelp.interrupt(s,now);
            FamilyCompany.interrupt(s,now);FarmWorksite.interrupt(s,now);s.action=null;s.before=null;label(v,s,foodAccess);return;
        }
        String boating = BoatTravel.tick(level, v, s, threat, now);
        if (boating != null) {
            LocalTrade.interrupt(s, now); FoodDelivery.interrupt(s, now); PaidDelivery.interrupt(s, now);
            FoodHelp.interrupt(s, now); FamilyCompany.interrupt(s, now); FarmWorksite.interrupt(s, now);
            s.action = null; s.before = null; label(v, s, boating); return;
        }
        if (VillageFamilies.tickVisit(level, v, now, threat != null)) {
            LocalTrade.interrupt(s, now); FoodDelivery.interrupt(s, now); PaidDelivery.interrupt(s, now);
            FoodHelp.interrupt(s, now); FamilyCompany.interrupt(s, now); FarmWorksite.interrupt(s, now);
            WorkNavigation.forget(v.getUUID()); s.socialVisit.interrupt(now); s.forestryInterval.interrupt();
            s.action = null; s.before = null; label(v, s, "courtship at the King Size Bed"); return;
        }
        String war=WarCombat.tick(level,v,s.mind);
        if(war!=null) {
            LocalTrade.interrupt(s,now);FoodDelivery.interrupt(s,now);PaidDelivery.interrupt(s,now);
            FoodHelp.interrupt(s,now);FamilyCompany.interrupt(s,now);FarmWorksite.interrupt(s,now);
            s.socialVisit.interrupt(now);WorkNavigation.forget(v.getUUID());s.forestryInterval.interrupt();
            s.action=null;s.before=null;label(v,s,war);return;
        }

        String survival = VillageSurvival.tick(level, v, s.mind, threat, now);
        if (survival != null) {
            LocalTrade.interrupt(s,now);
            s.socialVisit.interrupt(now);
            FoodDelivery.interrupt(s,now);
            PaidDelivery.interrupt(s,now);
            WorkNavigation.forget(v.getUUID());
            s.forestryInterval.interrupt();
            FoodHelp.interrupt(s,now);
            FamilyCompany.interrupt(s, now);
            FarmWorksite.interrupt(s, now);
            if (s.action != Action.FLEE && (threat != null || v.isOnFire())) {
                safetyInterrupts++;
                Speech.say(level, v, s, local.villagers.language.Phrases.Situation.DANGER, null, false);
            }
            s.action = Action.FLEE;
            s.before = null;
            label(v, s, survival);
            return;
        }
        if (v.isTrading()) { LocalTrade.interrupt(s,now); FoodDelivery.interrupt(s,now); PaidDelivery.interrupt(s,now); WorkNavigation.forget(v.getUUID()); s.forestryInterval.interrupt(); FoodHelp.interrupt(s,now); FamilyCompany.interrupt(s, now); FarmWorksite.interrupt(s, now); v.getNavigation().stop(); s.action = null; s.before = null; return; }
        if (s.action == Action.FLEE) { s.action = null; s.before = null; }
        String preparation = SelfProvisioning.work(level, v, s, now);
        if (preparation != null) {
            LocalTrade.interrupt(s,now); FoodDelivery.interrupt(s,now); PaidDelivery.interrupt(s,now);
            FoodHelp.interrupt(s,now); FamilyCompany.interrupt(s,now); FarmWorksite.interrupt(s,now);
            s.action = null; s.before = null; label(v,s,preparation); return;
        }
        if (s.action == Action.FORAGE && !s.succeeded && !current.foodAvailable()) {
            // A meal that no longer fits is not a failed skill or a reason to idle until timeout.
            v.getNavigation().stop(); s.action = null; s.before = null;
        }

        VillageFamilies.signal(level, v, s, now);
        FoodSupply.update(s, FoodSupply.mayWork(v.getAge(), v.getHealth(), s.mind.fatigue(),
                !level.isDarkOutside() && !v.isSleeping() && !v.isOnFire() && !v.isInWater() && v.getAirSupply() >= 200),
                v.getInventory(), AdventureActions.canHarvest(v));
        String delivery=FoodDelivery.seek(level,v,s,now);
        if(delivery!=null) {PaidDelivery.interrupt(s,now);LocalTrade.interrupt(s,now);FoodHelp.interrupt(s,now);FamilyCompany.interrupt(s,now);FarmWorksite.interrupt(s,now);label(v,s,delivery);return;}
        String social = SocialReactions.tick(level, v, s.mind, current);
        if (social != null) {
            PaidDelivery.interrupt(s,now);
            LocalTrade.interrupt(s,now);
            FoodHelp.interrupt(s,now);
            FamilyCompany.interrupt(s, now);
            FarmWorksite.interrupt(s, now);
            s.action = null; s.before = null; // An interrupted elective action earns no utility update.
            label(v, s, social);
            return;
        }
        String paidDelivery=PaidDelivery.seek(level,v,s,now);
        if(paidDelivery!=null) {
            LocalTrade.interrupt(s,now);FoodHelp.interrupt(s,now);FamilyCompany.interrupt(s,now);FarmWorksite.interrupt(s,now);
            s.familyProvisionStarted=0;s.craftingProgress.reset();label(v,s,paidDelivery);return;
        }
        String foodTrade=LocalTrade.seekFood(level,v,s,now);
        if(foodTrade!=null) {
            FoodHelp.interrupt(s,now);FamilyCompany.interrupt(s,now);FarmWorksite.interrupt(s,now);
            s.familyProvisionStarted=0;s.craftingProgress.reset();label(v,s,foodTrade);return;
        }
        String toolCollection=LocalTrade.seekKnownSupplier(level,v,s,now);
        if(toolCollection!=null) {
            FoodHelp.interrupt(s,now);FamilyCompany.interrupt(s,now);FarmWorksite.interrupt(s,now);
            s.familyProvisionStarted=0;s.craftingProgress.reset();label(v,s,toolCollection);return;
        }
        String productionInput = LocalTrade.seekProductionInput(level,v,s,now);
        if(productionInput!=null) { FoodHelp.interrupt(s,now); FamilyCompany.interrupt(s,now); FarmWorksite.interrupt(s,now); s.familyProvisionStarted=0;s.craftingProgress.reset();label(v,s,productionInput);return; }
        String customerTool = ToolProduction.serveCustomer(level,v,s,now);
        if(customerTool!=null) { LocalTrade.interrupt(s,now); FoodHelp.interrupt(s,now); FamilyCompany.interrupt(s,now); FarmWorksite.interrupt(s,now); label(v,s,customerTool);return; }
        String provision = provisionFood(level, v, s, now);
        if (provision != null) { LocalTrade.interrupt(s,now); FoodHelp.interrupt(s,now); FamilyCompany.interrupt(s, now); label(v, s, provision); return; }
        String foodHelp=FoodHelp.seek(level,v,s,now);
        if (foodHelp!=null) { LocalTrade.interrupt(s,now); FamilyCompany.interrupt(s,now);FarmWorksite.interrupt(s,now);label(v,s,foodHelp);return; }
        String trade=LocalTrade.seek(level,v,s,now);
        if(trade!=null) {FamilyCompany.interrupt(s,now);FarmWorksite.interrupt(s,now);label(v,s,trade);return;}
        String cultivation=FarmExpansion.invest(level,v,s,now);
        if(cultivation!=null) {FamilyCompany.interrupt(s,now);FarmWorksite.interrupt(s,now);label(v,s,cultivation);return;}
        String soil=SoilWork.work(level,v,s,now);
        if(soil!=null) { FamilyCompany.interrupt(s,now);FarmWorksite.interrupt(s,now);label(v,s,soil);return; }
        String forestry=ForestryWork.work(level,v,s,now);
        if (forestry!=null) { FamilyCompany.interrupt(s,now);label(v,s,forestry);return; }
        String station = StationProduction.work(level, v, s, now);
        if (station != null) { FamilyCompany.interrupt(s, now); label(v, s, station); return; }
        String tool = ToolProduction.work(level, v, s, now);
        if (tool != null) { FamilyCompany.interrupt(s, now); label(v, s, tool); return; }
        String adventure = AdventurePreparation.work(level, v, s, now);
        if (adventure != null) { FamilyCompany.interrupt(s, now); s.action = null; s.before = null; label(v, s, adventure); return; }
        boolean seekingCompany = FamilyCompany.seek(level, v, s, now);
        if (seekingCompany) {
            s.before = null; // This authored goal cannot inherit an elective action's learning observation.
            if (s.action != Action.SOCIALIZE) {
                v.getNavigation().stop(); s.action = null; s.succeeded = false;
            }
        }
        if (s.action != null) {
            if (!s.succeeded) execute(level, v, s, now);
            if (attemptFinished(s,now)) finishAttempt(level, v, s);
        }
        if (s.action == null) {
            if (v.isSleeping()) v.stopSleeping();
            current = observe(level, v, s, false);
            v.getNavigation().stop();
            boolean familyMeeting = (seekingCompany && s.familyCompany != null || VillageFamilies.courting(level, v, s, now))
                    && s.mind.allowed(Action.SOCIALIZE, current);
            boolean visiting = !familyMeeting && SocialCompany.initiate(level,v,s,current,now);
            s.action = familyMeeting || visiting ? Action.SOCIALIZE : s.mind.choose(current, RANDOM);
            s.before = familyMeeting || visiting ? null : current;
            s.started = now; s.sleepStarted = 0; s.succeeded = false; s.target = null; s.nextThink = now;
            s.craftingProgress.reset();
            decisions++;
            Speech.say(level, v, s, situation(s.action), null, false);
            execute(level, v, s, now);
        }
        String activity = currentActivity(s);
        if (s.action == Action.SOCIALIZE && s.socialVisit.active()) {
            var companion = Villagers.existingSession(s.socialVisit.target());
            if (companion != null) activity = "visiting " + companion.mind.name();
        }
        if (s.action == Action.FORAGE && BreadCrafting.wheatCount(v.getInventory()) >= 3)
            activity += " / " + s.craftingStatus;
        label(v, s, activity);
    }

    /** Execution can reject its own route and clear the action; that is not a completed attempt. */
    static boolean attemptFinished(Session s,long now) {
        return s.action!=null && (s.succeeded || !s.socialVisit.active() && now-s.started>=ACTION_TIMEOUT_TICKS);
    }

    static String currentActivity(Session s) {
        return s.action==null?"choosing next activity":s.action.name().toLowerCase(Locale.ROOT);
    }

    static boolean threatens(Monster mob,Villager villager) {
        if(!mob.isAlive())return false;
        if(mob instanceof net.minecraft.world.entity.monster.Enderman)return mob.getTarget()==villager;
        // Native creepers target players, not villagers. An idle creeper must
        // not make its entire field permanently unusable for village food.
        if(mob instanceof net.minecraft.world.entity.monster.Creeper creeper)
            return creeper.getTarget()==villager||creeper.isIgnited()||creeper.getSwellDir()>0;
        return true;
    }

    /** Player movement costs: running 0.1 per block over the ground, swimming 0.01 per block. Walking is free. */
    private static void exertFromMovement(Villager v, Session s) {
        Vec3 here = v.position();
        Vec3 last = s.lastPos;
        s.lastPos = here;
        if (last == null || v.isPassenger()) return;
        double moved = here.distanceTo(last);
        if (moved > 16) return; // A teleport, not travel.
        if (v.isInWater()) s.mind.exert((float) (Metabolism.EXHAUSTION_SWIM_PER_BLOCK * moved));
        else if (VillageSurvival.running(v.getUUID()))
            s.mind.exert((float) (Metabolism.EXHAUSTION_SPRINT_PER_BLOCK * here.subtract(last).horizontalDistance()));
    }

    /** The player's food regeneration and starvation. Starvation ignores difficulty and can kill. */
    private static void metabolize(ServerLevel level, Villager v, Session s, int ticks) {
        if (ticks <= 0) return;
        Metabolism.Vitals vitals = s.mind.metabolize(ticks, v.getHealth(), v.getMaxHealth(),
                level.getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION));
        if (vitals.healing() > 0) v.heal(vitals.healing());
        for (int i = 0; i < vitals.starvationHits() && v.isAlive(); i++)
            v.hurtServer(level, level.damageSources().starve(), 1);
    }

    private static void finishAttempt(ServerLevel level, Villager v, Session s) {
        s.socialVisit.interrupt(level.getGameTime());
        if (s.before != null) Villagers.LEARNING_MODE.apply(s.mind, s.before, s.action, observe(level, v, s, false));
        if (s.succeeded && s.action == Action.EXPLORE && Villagers.LEARNING_MODE == local.villagers.learning.LearningMode.ADAPTIVE)
            s.mind.experience(local.villagers.mood.Experience.Event.EXPLORED);
        if (s.succeeded) { completed++; s.mind.remember("Completed " + s.action + " at " + v.blockPosition().toShortString()); }
        s.action = null;
        s.before = null;
    }

    static Observation observe(ServerLevel level, Villager v, Session s, boolean danger) {
        boolean foodAvailable = forageAvailable(level, v, s);
        return new Observation(s.mind.hunger(), s.mind.fatigue(), s.mind.loneliness(), v.getHealth() / v.getMaxHealth(),
                danger, level.isDarkOutside(), foodAvailable, validHome(level, s.mind.home()));
    }

    static void requestHarvest(ServerLevel level, Villager v, Session s, BlockPos crop) {
        v.getNavigation().stop();
        s.action = Action.FORAGE;
        s.before = observe(level, v, s, false);
        s.started = level.getGameTime(); s.nextThink = s.started; s.succeeded = false;
        s.cropTarget = crop; s.nextCropScan = s.started + 100;
        s.mind.remember("Operator requested one field harvest in the recorded tool probe");
    }

    // ---- Adoption, release and gifts ----

    static void adopt(ServerLevel level, Villager v) {
        v.getBrain().stopAll(level, v);
        v.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        v.getNavigation().stop();
        v.addTag(Villagers.MANAGED);
        v.setPersistenceRequired();
        AdventureActions.prepare(level, v);
        Session s = Villagers.session(v, level);
        if (s.mind.home() == null) claimHome(level, v, s);
        Villagers.persist(s);
    }

    /** Restores vanilla AI. The learned identity is saved and kept for a later adoption. */
    static void release(ServerLevel level, Villager v) {
        // Load the session if needed, so the identity is saved and its label can be recognised.
        Session s = Villagers.session(v, level);
        Villagers.removeSession(v.getUUID());
        Villagers.persist(s);
        ItemStack ownedTool = v.getMainHandItem();
        if (!ownedTool.isEmpty() && AdventureActions.acceptsTool(ownedTool)) {
            v.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            ItemStack remainder = v.getInventory().addItem(ownedTool);
            if (!remainder.isEmpty()) v.spawnAtLocation(level, remainder);
        }
        v.removeTag(Villagers.MANAGED);
        Villagers.forgetRuntime(v.getUUID());
        v.stopSleeping();
        v.getNavigation().stop();
        v.refreshBrain(level);
        // Only remove our own activity label; a player's name tag stays.
        Component current = v.getCustomName();
        if (current != null && ActivityLabel.isLabel(s.mind.name(), current.getString())) {
            v.setCustomName(null);
            v.setCustomNameVisible(false);
        }
    }

    static boolean offerGift(ServerLevel level, Villager villager, Player player, InteractionHand hand) {
        if (!Villagers.ready() || !villager.isAlive() || !player.isAlive() || villager.level() != level || player.level() != level
                || !Villagers.isManaged(villager) || !player.isShiftKeyDown() || player.isSpectator()
                || player.distanceToSqr(villager) > 9 || !player.hasLineOfSight(villager)) return false;
        ItemStack held = player.getItemInHand(hand);
        String itemId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString(); // Read before the stack is consumed.
        boolean usefulTool = AdventureActions.acceptsTool(held) && !ownsToolKind(villager, held);
        if (AdventureActions.offerTool(level, villager, player, hand)) {
            recordGift(level, villager, Villagers.session(villager, level), player, itemId);
            if (usefulTool) Villagers.observePlayerEvent(level, villager, player.getUUID(), PlayerImpression.Evidence.USEFUL_GIFT);
            return true;
        }
        if (!food(held) && !held.is(Items.WHEAT) && !held.is(ItemTags.VILLAGER_PLANTABLE_SEEDS)
                && !WoodCrafting.material(held) && !held.is(Items.EMERALD)) return false;
        Session s = Villagers.session(villager, level);
        boolean useful = held.is(Items.EMERALD) ? LocalTrade.usefulMoney(villager,s) : food(held)
                ? s.mind.hunger() >= .2 && foodInInventory(villager) < 0
                : AdventureActions.canHarvest(villager) && !villager.getInventory().hasAnyMatching(stack -> stack.is(held.getItem()));
        ItemStack remainder = villager.getInventory().addItem(held.copyWithCount(1));
        if (!remainder.isEmpty()) return false;
        held.shrink(1);
        recordGift(level, villager, s, player, itemId);
        if (useful) Villagers.observePlayerEvent(level, villager, player.getUUID(), PlayerImpression.Evidence.USEFUL_GIFT);
        return true;
    }

    private static void recordGift(ServerLevel level, Villager villager, Session s, Player giver, String itemId) {
        List<UUID> witnesses = new ArrayList<>();
        witnesses.add(giver.getUUID()); // The giver always shares the memory.
        witnesses.addAll(witnesses(level, villager));
        s.mind.recordEpisode(Episode.Kind.RECEIVED_ITEM, level.getGameTime(), giver.getUUID(), null, itemId, witnesses);
        Speech.say(level, villager, s, local.villagers.language.Phrases.Situation.THANKS,
                local.villagers.language.Concept.forItem(itemId), true);
    }

    private static local.villagers.language.Phrases.Situation situation(Action action) {
        return switch (action) {
            case FORAGE -> local.villagers.language.Phrases.Situation.FORAGE;
            case REST -> local.villagers.language.Phrases.Situation.REST;
            case SOCIALIZE -> local.villagers.language.Phrases.Situation.SOCIALIZE;
            case EXPLORE -> local.villagers.language.Phrases.Situation.EXPLORE;
            case FLEE -> local.villagers.language.Phrases.Situation.DANGER;
        };
    }

    /** One memory in full, for a player who has just come to share it. */
    static String describe(ServerLevel level, Session s, Episode episode, ServerPlayer viewer) {
        var players = level.getServer().getPlayerList();
        return MemoryText.line(episode, viewer.getUUID(), true, id -> {
            ServerPlayer online = players.getPlayer(id);
            return online == null ? null : online.getName().getString();
        });
    }

    /** Hand one spare food item from one villager to another. False if none could be moved. */
    static boolean giveFood(Villager giver, Villager receiver) {
        if (giver == receiver || !(giver.level() instanceof ServerLevel level) || giver.level() != receiver.level()
                || !giver.isAlive() || !receiver.isAlive() || !Villagers.isManaged(giver) || !Villagers.isManaged(receiver)
                || giver.isTrading() || receiver.isTrading() || giver.isSleeping() || receiver.isSleeping()
                || !local.villagers.relations.FoodRequest.inReach(giver.distanceToSqr(receiver), Villagers.config().hearingRadius(),
                        giver.hasLineOfSight(receiver) && receiver.hasLineOfSight(giver))) return false;
        int slot = FoodInventory.sharingSlot(giver.getInventory(),AdventureActions.canHarvest(giver),
                one -> receiver.getInventory().canAddItem(one));
        if (slot < 0) return false;
        ItemStack one = giver.getInventory().getItem(slot).copyWithCount(1);
        var offered=one.getItem();var nutrition=one.get(DataComponents.FOOD).nutrition();
        boolean named=one.has(DataComponents.CUSTOM_NAME);
        int giverBefore=giver.getInventory().countItem(offered),receiverBefore=receiver.getInventory().countItem(offered);
        if (!receiver.getInventory().addItem(one).isEmpty()) return false;
        giver.getInventory().removeItem(slot, 1);
        var from=Villagers.existingSession(giver.getUUID());var to=Villagers.existingSession(receiver.getUUID());
        String item=BuiltInRegistries.ITEM.getKey(one.getItem()).toString();
        if (from!=null) { from.foodSharedGiven++;from.mind.remember("Gave one actual "+item+" to "+receiver.getUUID());Villagers.persist(from); }
        if (to!=null) { to.foodSharedReceived++;to.mind.remember("Received one actual "+item+" from "+giver.getUUID());Villagers.persist(to); }
        var receipt=new LinkedHashMap<String,Object>();
        receipt.put("gameTime",level.getGameTime());receipt.put("from",giver.getUUID().toString());receipt.put("to",receiver.getUUID().toString());
        receipt.put("item",BuiltInRegistries.ITEM.getKey(offered).toString());receipt.put("nutrition",nutrition);receipt.put("named",named);
        receipt.put("giverBefore",giverBefore);receipt.put("giverAfter",giver.getInventory().countItem(offered));
        receipt.put("receiverBefore",receiverBefore);receipt.put("receiverAfter",receiver.getInventory().countItem(offered));
        var immutable=Map.<String,Object>copyOf(receipt);
        if(from!=null)from.lastFoodGiven=immutable;
        if(to!=null)to.lastFoodReceived=immutable;
        return true;
    }

    static final int RECALL_LIMIT = 10;

    /** The most recent memories as this viewer may see them, oldest first; hidden ones read "???". */
    static List<String> recall(ServerLevel level, Session s, UUID viewer, boolean omniscient) {
        var players = level.getServer().getPlayerList();
        List<Episode> episodes = s.mind.episodes();
        return episodes.subList(Math.max(0, episodes.size() - RECALL_LIMIT), episodes.size()).stream()
                .map(episode -> MemoryText.line(episode, viewer, omniscient, id -> {
                    ServerPlayer online = players.getPlayer(id);
                    return online == null ? null : online.getName().getString();
                })).toList();
    }

    /** Creative and spectator players see every memory; survival and adventure players only shared ones. */
    static boolean omniscient(ServerPlayer viewer) { return viewer.isCreative() || viewer.isSpectator(); }

    /** Sneak-right-click with an empty hand: the villager recalls its memories to that player. */
    static void tellMemories(ServerLevel level, Villager villager, ServerPlayer player) {
        Session s = Villagers.session(villager, level);
        Speech.talk(level, villager, s, player);
        List<String> lines = recall(level, s, player.getUUID(), omniscient(player));
        if (lines.isEmpty()) {
            player.sendSystemMessage(Component.literal(s.mind.name() + " has no memories to share yet."));
            return;
        }
        player.sendSystemMessage(Component.literal(s.mind.name() + " remembers:"));
        lines.forEach(line -> player.sendSystemMessage(Component.literal("  " + line)));
    }

    /**
     * Players who share an experience with this villager: within the configured radius, with line of sight,
     * and not spectating. Nearest first.
     */
    static List<UUID> witnesses(ServerLevel level, Villager villager) {
        double radius = Villagers.config().memoryWitnessRadius();
        return level.getPlayers(player -> !player.isSpectator() && player.isAlive()
                        && player.distanceToSqr(villager) <= radius * radius && player.hasLineOfSight(villager))
                .stream().sorted(Comparator.comparingDouble(player -> player.distanceToSqr(villager)))
                .map(ServerPlayer::getUUID).toList();
    }

    private static boolean ownsToolKind(Villager villager, ItemStack offered) {
        var kind = offered.is(ItemTags.HOES) ? ItemTags.HOES : ItemTags.SWORDS;
        return villager.getMainHandItem().is(kind) || villager.getInventory().hasAnyMatching(stack -> stack.is(kind));
    }

    // ---- Food and homes ----

    private static BlockPos cropTarget(ServerLevel level, Villager v, Session s) {
        if (!AdventureActions.canHarvest(v)) { s.cropTarget = null; return null; }
        if (level.getGameTime() >= s.nextCropScan) {
            s.cropTarget = AdventureActions.findHarvestTarget(level, v);
            s.nextCropScan = level.getGameTime() + 100;
        }
        return s.cropTarget;
    }

    private static boolean grainAvailable(ServerLevel level, Villager v, Session s) {
        if (BreadCrafting.wheatCount(v.getInventory()) < 3 && !HayForaging.hasHay(v.getInventory())
                && !Villagers.config().hayForaging()) return false;
        return BreadCrafting.table(level, v, s) != null && (BreadCrafting.wheatCount(v.getInventory()) >= 3
                || HayForaging.hasHay(v.getInventory()) || HayForaging.target(level, v, s) != null);
    }

    private static BlockPos stockCropTarget(ServerLevel level, Villager v, Session s) {
        if (BreadCrafting.wheatCount(v.getInventory()) < s.stockWheatTarget)
            return cropTarget(level, v, s);
        if (!AdventureActions.canHarvest(v)) return null;
        long now = level.getGameTime();
        if (now >= s.nextRootCropScan) {
            s.rootCropTarget = AdventureActions.findHarvestTarget(level, v, false);
            s.nextRootCropScan = now + 100;
        }
        return s.rootCropTarget;
    }

    private static boolean food(ItemStack item) {
        return FoodInventory.supported(item);
    }

    /** Inventory slot of edible food, keeping the last carrot or potato as seed for a farmer. -1 if none. */
    private static int foodInInventory(Villager v) {
        return FoodInventory.anySlot(v.getInventory(), AdventureActions.canHarvest(v));
    }

    private static ItemEntity nearestFood(ServerLevel level, Villager v, Session s) {
        return nearestFood(level, v, s, false);
    }

    private static ItemEntity nearestFood(ServerLevel level, Villager v, Session s, boolean mealOnly) {
        return level.getEntitiesOfClass(ItemEntity.class, v.getBoundingBox().inflate(16),
                        e -> e.isAlive() && food(e.getItem()) && (!mealOnly || mealFits(v, s, e.getItem()))
                                && VillageSurvival.withinHomeRange(level, s.mind, e.position()))
                .stream().min(Comparator.comparingDouble(v::distanceToSqr)).orElse(null);
    }

    private static int mealSlot(Villager v, Session s) {
        return FoodInventory.mealSlot(v.getInventory(), AdventureActions.canHarvest(v),
                s.mind.foodLevel(), v.getHealth(), v.getMaxHealth());
    }

    private static boolean mealFits(Villager v, Session s, ItemStack item) {
        return FoodInventory.fits(item, s.mind.foodLevel(), v.getHealth(), v.getMaxHealth());
    }

    private static boolean cropMealFits(ServerLevel level, Villager v, Session s, BlockPos crop) {
        if (crop == null || !level.isLoaded(crop)) return false;
        var state = level.getBlockState(crop);
        var item = state.is(net.minecraft.world.level.block.Blocks.WHEAT) ? Items.BREAD
                : state.is(net.minecraft.world.level.block.Blocks.CARROTS) ? Items.CARROT
                : state.is(net.minecraft.world.level.block.Blocks.POTATOES) ? Items.POTATO
                : state.is(net.minecraft.world.level.block.Blocks.BEETROOTS) ? Items.BEETROOT : Items.AIR;
        return mealFits(v, s, item.getDefaultInstance());
    }

    /** Actual meal affordances, not merely a stockpile of food the body cannot yet use. */
    private static boolean forageAvailable(ServerLevel level, Villager v, Session s) {
        if (!s.mind.canEat() || level.getGameTime() < s.nextMeal) return false;
        if (mealSlot(v, s) >= 0 || nearestFood(level, v, s, true) != null) return true;
        // Keep an owned future ration; household provisioning has a separate explicit stock goal.
        if (foodInInventory(v) >= 0) return false;
        return cropMealFits(level, v, s, cropTarget(level, v, s))
                || mealFits(v, s, Items.BREAD.getDefaultInstance()) && grainAvailable(level, v, s);
    }

    private static BlockPos pos(Home home) { return new BlockPos(home.x(), home.y(), home.z()); }

    private static String dimensionId(ServerLevel level) { return level.dimension().identifier().toString(); }

    private static boolean validHome(ServerLevel level, Home home) {
        return home != null && home.dimension().equals(dimensionId(level))
                && level.isLoaded(pos(home))
                && level.getBlockState(pos(home)).getBlock() instanceof BedBlock;
    }

    private static void claimHome(ServerLevel level, Villager v, Session s) {
        // Admission-time search only. Remember the result; do not scan thousands of blocks each decision.
        var existing = v.getBrain().getMemory(MemoryModuleType.HOME);
        if (existing.isPresent() && existing.get().dimension().equals(level.dimension())) {
            BlockPos pos = existing.get().pos();
            Home remembered = new Home(dimensionId(level), pos.getX(), pos.getY(), pos.getZ());
            if (validHome(level, remembered) && TraderRecruitment.canClaim(level, v, pos)) { s.mind.home(remembered); return; }
        }
        String dimension = dimensionId(level);
        level.getPoiManager().findClosest(type -> type.is(PoiTypes.HOME),
                pos -> level.isLoaded(pos) && TraderRecruitment.canClaim(level, v, pos) && StreamSupport.stream(Villagers.sessions().spliterator(), false).noneMatch(other -> {
                    Home claimed = other.mind.home();
                    return claimed != null && claimed.dimension().equals(dimension) && pos(claimed).equals(pos);
                }), homeSearchOrigin(v), 64, PoiManager.Occupancy.HAS_SPACE).ifPresent(pos -> setHome(level, v, s, pos));
    }

    private static BlockPos homeSearchOrigin(Villager v) {
        Home anchor = TraderRecruitment.anchor(v);
        return anchor == null ? v.blockPosition() : pos(anchor);
    }

    static void setHome(ServerLevel level, Villager v, Session s, BlockPos pos) {
        if (!TraderRecruitment.canClaim(level, v, pos)) return;
        var previous = v.getBrain().getMemory(MemoryModuleType.HOME);
        boolean same = previous.isPresent() && previous.get().dimension().equals(level.dimension()) && previous.get().pos().equals(pos);
        if (!same) {
            var acquired = level.getPoiManager().take(type -> type.is(PoiTypes.HOME), (type, point) -> point.equals(pos), pos, 1);
            if (acquired.isEmpty()) return;
            v.releasePoi(MemoryModuleType.HOME);
        }
        s.mind.home(new Home(dimensionId(level), pos.getX(), pos.getY(), pos.getZ()));
        v.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(level.dimension(), pos));
        s.mind.recordEpisode(Episode.Kind.CLAIMED_BED, level.getGameTime(), null,
                new Place(dimensionId(level), pos.getX(), pos.getY(), pos.getZ()), null, witnesses(level, v));
        Villagers.persist(s);
    }

    // ---- Action execution ----

    private static void execute(ServerLevel level, Villager v, Session s, long now) {
        switch (s.action) {
            case FORAGE -> forage(level, v, s, now);
            case REST -> rest(level, v, s, now);
            case SOCIALIZE -> socialize(level, v, s, now);
            case EXPLORE -> explore(v, s, level);
            case FLEE -> { /* Only the survival shield executes flee. */ }
        }
    }

    /** Eats one item with its real nutrition and saturation. */
    private static void eat(ServerLevel level, Villager v, Session s, ItemStack eaten) {
        nourish(level, v, s, eaten);
        s.succeeded = true;
    }

    private static void nourish(ServerLevel level, Villager v, Session s, ItemStack eaten) {
        FoodProperties food = eaten.get(DataComponents.FOOD);
        if (food != null) {
            s.mind.eat(food.nutrition(), food.saturation());
            Speech.ate(level, v, s);
        }
        s.nextMeal = level.getGameTime() + MealPolicy.INTERVAL;
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, v.getX(), v.getY() + 1.7, v.getZ(), 3, .2, .2, .2, .01);
    }

    /** Resolve possessed food before starvation/shelter arbitration; never stop escape movement or credit it. */
    private static void eatOwnedRation(ServerLevel level, Villager v, Session s, long now, boolean safeForTrip) {
        FoodDelivery.handOffNearby(level,v,s,now);
        if (now < s.nextMeal || v.isTrading() || !AdventureActions.PERMISSIONS.allows(
                local.villagers.capability.CapabilityPermissions.Capability.CONSUME_FOOD)) return;
        int slot = mealSlot(v, s);
        if (slot < 0) return;
        if (safeForTrip && FoodDelivery.reserveMeal(level,v,s,now,slot)) return;
        FoodProperties meal = v.getInventory().getItem(slot).get(DataComponents.FOOD);
        if (meal == null || !MealPolicy.shouldEat(s.mind.foodLevel(), meal.nutrition(), v.getHealth(), v.getMaxHealth())) return;
        if (s.action != null && s.succeeded && s.before != null) finishAttempt(level, v, s);
        // An authored meal must not make exploring/resting look as though it produced nutrition.
        s.before = null;
        if (v.isSleeping()) v.stopSleeping();
        ItemStack actual = v.getInventory().removeItem(slot, 1);
        if (actual.isEmpty()) return;
        nourish(level, v, s, actual);
        s.mind.remember("Ate owned " + BuiltInRegistries.ITEM.getKey(actual.getItem()) + " for hunger or healing");
    }

    /** Authored food work, including an adult in birth cooldown; never grants elective utility credit. */
    private static String provisionFood(ServerLevel level, Villager v, Session s, long now) {
        boolean familyWork = s.familyFoodGoal && Villagers.config().breeding() && v.getAge() == 0
                && s.mind.foodLevel() >= local.villagers.family.BreedingPolicy.MIN_FOOD
                && s.mind.fatigue() < .5 && !s.mind.mood().low() && v.getHealth() >= v.getMaxHealth() * .9;
        boolean safeDay = !level.isDarkOutside() && !v.isSleeping() && !v.isTrading() && !v.isOnFire()
                && !v.isInWater() && v.getAirSupply() >= 200;
        if (!safeDay || !familyWork && !s.selfFoodGoal) {
            s.stockWheatTarget = 0; s.foodStatus = "not working";
            FarmWorksite.interrupt(s, now); s.familyProvisionStarted = 0; return null;
        }
        boolean farmer = AdventureActions.canHarvest(v);
        var bread = net.minecraft.world.item.Items.BREAD.getDefaultInstance().get(DataComponents.FOOD);
        s.stockWheatTarget = Math.max(familyWork ? VillageFamilies.wheatReserveNeeded(v.getInventory(), farmer) : 0,
                s.selfFoodGoal ? FoodSupply.wheatTarget(v.getInventory(), farmer, bread == null ? 0 : bread.nutrition(), FoodSupply.target(s, now)) : 0);
        if (now < s.nextFamilyProvision) return null;
        if (s.familyProvisionStarted == 0) {
            boolean supplies = s.stockWheatTarget > 0 && grainAvailable(level, v, s)
                    || stockCropTarget(level, v, s) != null || nearestFood(level, v, s) != null;
            if (!supplies) {
                String soil=SoilWork.work(level,v,s,now);
                if(soil!=null)return soil;
                String station = StationProduction.work(level, v, s, now);
                if (station != null) return station;
                String tool = ToolProduction.work(level, v, s, now);
                if (tool != null) return tool;
                String expansion = FarmExpansion.work(level, v, s, now);
                if (expansion != null) return expansion;
                String trip = FarmWorksite.travel(level, v, s, now);
                if (trip == null && !s.farmJourney.active()) s.nextFamilyProvision = now + 100;
                return trip;
            }
            FarmWorksite.interrupt(s, now);
            // Superseded elective attempts receive no learning credit.
            v.getNavigation().stop();
            s.action = null; s.before = null; s.succeeded = false; s.nextThink = now;
            s.familyProvisionStarted = now;
            s.craftingProgress.reset();
        }
        forage(level, v, s, now, true);
        if (s.succeeded || now - s.familyProvisionStarted >= ACTION_TIMEOUT_TICKS) {
            boolean succeeded = s.succeeded;
            s.succeeded = false; s.familyProvisionStarted = 0;
            s.nextFamilyProvision = now + (succeeded ? 40 : 200);
            v.getNavigation().stop();
            s.foodStatus = succeeded ? "stored real food" : "food route blocked";
        }
        return familyWork ? "gathering food for a child" : "stocking food";
    }

    private static void forage(ServerLevel level, Villager v, Session s, long now) { forage(level, v, s, now, false); }

    private static void forage(ServerLevel level, Villager v, Session s, long now, boolean reserveForFamily) {
        if (!reserveForFamily && (!s.mind.canEat() || now < s.nextMeal)) return;
        // Full villagers may stock real food for a family; they still cannot eat on a full bar.
        int slot = !reserveForFamily ? mealSlot(v, s) : -1;
        if (slot >= 0) {
            if (FoodDelivery.reserveMeal(level,v,s,now,slot)) return;
            eat(level, v, s, v.getInventory().removeItem(slot, 1));
            return;
        }
        ItemEntity item = nearestFood(level, v, s, !reserveForFamily);
        if (item != null) {
            if (v.distanceToSqr(item) <= 3) {
                ItemStack eaten = item.getItem().copyWithCount(1);
                if (reserveForFamily) {
                    if (!v.getInventory().canAddItem(eaten)) return;
                    if (!v.getInventory().addItem(eaten).isEmpty()) return;
                }
                item.getItem().shrink(1);
                if (item.getItem().isEmpty()) item.discard();
                if (reserveForFamily) s.succeeded = true;
                else eat(level, v, s, eaten);
            } else if (now >= s.nextThink) {
                v.getNavigation().moveTo(item, .65);
                s.nextThink = now + 40;
            }
            return;
        }
        if (!reserveForFamily && foodInInventory(v) >= 0) return;
        boolean breadMealFits = reserveForFamily ? s.stockWheatTarget > 0 : mealFits(v, s, Items.BREAD.getDefaultInstance());
        if (breadMealFits && BreadCrafting.wheatCount(v.getInventory()) < 3
                && HayForaging.hasHay(v.getInventory())) HayForaging.unpack(level, v, s);
        if (breadMealFits && BreadCrafting.wheatCount(v.getInventory()) >= 3) {
            BlockPos table = BreadCrafting.table(level, v, s);
            s.craftingStatus = table == null ? "needs crafting table" : "going to crafting table";
            if (table != null) {
                if (BreadCrafting.canWorkAt(level,v,s,table)) {
                    if (BreadCrafting.tryCraft(level, v, s, table)) {
                        int bread = mealSlot(v, s);
                        if (!reserveForFamily && bread >= 0 && !FoodDelivery.reserveMeal(level,v,s,now,bread))
                            eat(level, v, s, v.getInventory().removeItem(bread, 1));
                        s.succeeded = true;
                    }
                } else BreadCrafting.approach(level, v, s, table, now);
                return;
            }
        }
        BlockPos crop = reserveForFamily ? stockCropTarget(level, v, s) : cropTarget(level, v, s);
        if (crop == null) {
            // Finite fallback only when existing supplies/crops cannot make the next bread.
            if (!breadMealFits || BreadCrafting.wheatCount(v.getInventory()) >= 3
                    || HayForaging.hasHay(v.getInventory()) || !Villagers.config().hayForaging()
                    || BreadCrafting.table(level, v, s) == null) return;
            BlockPos hay = HayForaging.target(level, v, s);
            if (hay == null) return;
            s.craftingStatus = "gathering existing hay";
            if (v.getEyePosition().distanceToSqr(Vec3.atCenterOf(hay)) <= 9) {
                if (HayForaging.gather(level, v, s, hay, now)) s.succeeded = true;
            } else if (now >= s.nextThink) {
                var path = WoodBootstrap.approachPath(level,v,s,hay);
                if (path != null && path.canReach()) v.getNavigation().moveTo(path, .65);
                else s.craftingStatus = "hay unreachable";
                s.nextThink = now + 40;
            }
            return;
        }
        if (!reserveForFamily && !cropMealFits(level, v, s, crop)) return;
        if (v.getEyePosition().distanceToSqr(Vec3.atCenterOf(crop)) <= 9) {
            if (AdventureActions.tryHarvest(level, v, crop)) {
                s.cropTarget = null;
                s.nextCropScan = now;
                s.rootCropTarget = null; s.nextRootCropScan = now;
                int edible = !reserveForFamily ? mealSlot(v, s) : -1;
                if (edible >= 0) eat(level, v, s, v.getInventory().removeItem(edible, 1));
                s.succeeded = true;
            }
        } else if (now >= s.nextThink) {
            var path = v.getNavigation().createPath(crop, 1);
            if (path != null && path.canReach()) v.getNavigation().moveTo(path, .65);
            s.nextThink = now + 40;
        }
    }

    private static void rest(ServerLevel level, Villager v, Session s, long now) {
        Home home = s.mind.home();
        if (v.isSleeping()) {
            boolean ownBed = validHome(level, home) && TraderRecruitment.allowedBed(level, pos(home))
                    && v.getSleepingPos().filter(pos(home)::equals).isPresent();
            s.restStatus = ownBed ? "sleeping in own bed" : "bed unavailable";
            if (s.restProgress.quiet("bed:" + home, now, ownBed, v.getX(), v.getY(), v.getZ())) {
                s.mind.rest(); s.bedRests++; s.succeeded = true;
            }
            return;
        }
        if (validHome(level, home) && TraderRecruitment.allowedBed(level, pos(home))) {
            BlockPos bed = pos(home);
            if (VillageSurvival.canRestAtHome(level, v, home) && !level.getBlockState(bed).getValue(BedBlock.OCCUPIED)) {
                v.getNavigation().stop();
                if (v.startSleeping(bed)) { s.restProgress.reset(); s.restStatus = "settling in own bed"; return; }
            } else if (!VillageSurvival.canRestAtHome(level, v, home)) {
                var step = s.restJourney.advance(home.toString(), now, false, v.getX(), v.getY(), v.getZ());
                if (step == local.villagers.navigation.WorkJourney.Step.START || step == local.villagers.navigation.WorkJourney.Step.TRAVEL) {
                    s.restProgress.reset(); s.restStatus = "seeking own bed";
                    if (now < s.nextRestPath || !WorkNavigation.admit(level, now, v)) return;
                    s.nextRestPath = now + 40;
                    var path = v.getNavigation().createPath(bed, 1);
                    if (path != null && path.canReach() && path.getNodeCount() <= 128 && v.getNavigation().moveTo(path, .6)) {
                        s.restJourney.navigating(home.toString(),now,v.getX(),v.getY(),v.getZ()); return;
                    }
                    s.restPathRejections++; s.restJourney.reject(now);
                }
            }
        }
        // A valid but unreachable/occupied bed must not prevent the existing ground-rest ability.
        v.getNavigation().stop();
        boolean safe = v.onGround() && !v.isInWater() && !v.isInLava() && !v.isOnFire() && v.getAirSupply() >= 200;
        for (BlockPos p : BlockPos.betweenClosed(v.blockPosition().below(), v.blockPosition())) {
            var state = level.getBlockState(p);
            if (!state.getFluidState().isEmpty() || state.is(net.minecraft.tags.BlockTags.FIRE) || state.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)
                    || state.is(net.minecraft.world.level.block.Blocks.CACTUS) || state.is(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH)
                    || state.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW) || state.is(net.minecraft.world.level.block.Blocks.CAMPFIRE)
                    || state.is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE)) safe = false;
        }
        s.restStatus = safe ? "resting in place; bed unavailable" : "no safe rest footing";
        if (s.restProgress.quiet("ground", now, safe, v.getX(), v.getY(), v.getZ())) {
            s.mind.rest(); s.groundRests++; s.succeeded = true;
        }
    }

    private static void socialize(ServerLevel level, Villager v, Session s, long now) {
        if (s.familyCompany == null && (s.courtship == null || !s.courtship.active(now))) {
            SocialCompany.tick(level,v,s,now);
            return;
        }
        s.socialVisit.interrupt(now);
        // Seek out a favourite if one is near; avoid villagers it dislikes; otherwise the nearest.
        List<Villager> nearby = level.getEntitiesOfClass(Villager.class, v.getBoundingBox().inflate(local.villagers.family.Courtship.COMPANY_RADIUS),
                e -> e != v && e.isAlive() && !e.isSleeping() && s.mind.opinionOf(e.getUUID()).regard() > -0.3
                        && (v.distanceToSqr(e) <= 100 || e.getUUID().equals(s.familyCompany)
                        || s.courtship != null && s.courtship.active(now)
                        && s.courtship.partner().equals(e.getUUID()) && v.distanceToSqr(e) <= 256));
        var ids = nearby.stream().map(Villager::getUUID).collect(java.util.stream.Collectors.toSet());
        var visible = nearby.stream().filter(Villagers::isManaged).filter(v::hasLineOfSight).map(Villager::getUUID).collect(java.util.stream.Collectors.toSet());
        UUID agreedPartner = local.villagers.family.Courtship.preferred(s.courtship, visible, now);
        UUID favourite = agreedPartner != null ? agreedPartner : visible.contains(s.familyCompany) ? s.familyCompany
                : s.mind.opinions().favourite(ids).orElse(null);
        Villager other = nearby.stream().filter(e -> e.getUUID().equals(favourite)).findFirst()
                .orElse(nearby.stream().min(Comparator.comparingDouble(v::distanceToSqr)).orElse(null));
        if (other == null || !VillageSurvival.withinHomeRange(level, s.mind, other.position())) return;
        v.getLookControl().setLookAt(other, 30, 30);
        if (v.distanceToSqr(other) < 9 && now - s.started >= 40) {
            s.mind.socialize();
            s.succeeded = true;
            if (Villagers.isManaged(other)) {
                Speech.converse(level, v, s, other);
                VillageFamilies.consider(level, v, s, other);
            }
        }
        else if (now >= s.nextThink) {
            if (other.getUUID().equals(s.familyCompany) || agreedPartner != null) {
                if (!WorkNavigation.admit(level, now, v)) return;
                s.nextThink = now + 40;
                var path = v.getNavigation().createPath(other.blockPosition(), 1);
                if (!WorkNavigation.staysHome(level, v, s, path)) {
                    FamilyCompany.reject(s, now); v.getNavigation().stop(); return;
                }
                if (!v.getNavigation().moveTo(path, .5)) { FamilyCompany.reject(s, now); return; }
                s.companyJourney.navigating(other.getUUID().toString(),now,v.getX(),v.getY(),v.getZ());
            } else { v.getNavigation().moveTo(other, .5); s.nextThink = now + 40; }
        }
    }

    private static void explore(Villager v, Session s, ServerLevel level) {
        if (s.target == null) {
            Vec3 pos = DefaultRandomPos.getPos(v, 7, 3);
            if (pos != null && VillageSurvival.withinHomeRange(level, s.mind, pos)) {
                s.target = BlockPos.containing(pos);
                v.getNavigation().moveTo(pos.x, pos.y, pos.z, .5);
            }
        } else if (v.blockPosition().distSqr(s.target) < 4) s.succeeded = true;
    }

    /** Shows "name · activity" above the villager, unless a player has given it a name tag. */
    private static void label(Villager v, Session s, String activity) {
        Component current = v.getCustomName();
        String currentText = current == null ? null : current.getString();
        if (!ActivityLabel.ownedBy(s.mind.name(), currentText)) return;
        // Low spirits are visible: "Fern-ab12 · rest (miserable)".
        if (s.mind.mood().low()) activity += " (" + s.mind.mood().band().name().toLowerCase(Locale.ROOT) + ")";
        String text = ActivityLabel.text(s.mind.name(), activity);
        if (!text.equals(currentText)) v.setCustomName(Component.literal(text)); // Avoid re-syncing an unchanged name.
        if (!v.isCustomNameVisible()) v.setCustomNameVisible(true);
    }

    // ---- Statistics ----

    static Map<String, Object> summary() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("learningMode", Villagers.LEARNING_MODE.name());
        result.put("decisions", decisions);
        result.put("completedSkills", completed);
        result.put("safetyInterrupts", safetyInterrupts);
        result.put("totalThinkMillis", thinkNanos / 1e6);
        result.put("activeAgents", Villagers.sessionCount());
        return result;
    }

    static Map<String, Object> statistics(Iterable<Session> sessions) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", Villagers.LEARNING_MODE.name());
        result.put("activeAgents", Villagers.sessionCount());
        result.put("decisions", decisions);
        result.put("completedSkills", completed);
        result.put("safetyInterrupts", safetyInterrupts);
        result.put("agents", StreamSupport.stream(sessions.spliterator(), false).map(s -> Map.of(
                "uuid", s.mind.id(), "learningUpdates", s.mind.learningUpdates(),
                "hunger", s.mind.hunger(), "foodLevel", s.mind.foodLevel(), "saturation", s.mind.saturation(),
                "fatigue", s.mind.fatigue(), "loneliness", s.mind.loneliness(), "ageTicks", s.mind.ageTicks(),
                "generation", s.mind.generation())).toList());
        return result;
    }
}
