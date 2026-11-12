package local.villagers;

import java.util.*;
import local.villagers.family.BreedingPolicy;
import local.villagers.family.Courtship;
import local.villagers.genetics.Tendency;
import local.villagers.mind.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.monster.illager.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BedPart;

/** Paid ordinary families in scenario towns. Illagers lack vanilla baby bodies, so juveniles use native scale. */
final class IllagerFamilies {
    private static final String BED = "villagers:illager_bed:";
    private static final String YOUNG = "villagers:illager_juvenile";
    private static final Random RANDOM = new Random();
    static boolean juvenile(Mob mob) { return mob.entityTags().contains(YOUNG); }
    static void markJuvenile(Mob mob) { mob.addTag(YOUNG); mob.getAttribute(Attributes.SCALE).setBaseValue(.5); }
    static Map<String, Object> status(Mob mob, CreatureAgents.State s) {
        var report=new LinkedHashMap<String,Object>(Map.of("activity", s.familyStatus, "attempts", s.familyAttempts, "blocked", s.familyBlock,
            "nursery", bed(mob) == null ? "" : bed(mob).toShortString(), "juvenile", juvenile(mob),
            "ageTicks", s.mind.ageTicks(), "generation", s.mind.generation(),"lastBirth",s.lastBirth));
        if(mob.level() instanceof ServerLevel level){
            report.put("breedingEnabled",Villagers.config().breeding());report.put("nextBedSearch",s.nextBedSearch);
            report.put("availableNurseries",level.getPoiManager().findAll(t->t.is(PoiTypes.HOME),p->nursery(level,mob,p),mob.blockPosition(),64,
                net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.HAS_SPACE).map(BlockPos::toShortString).toList());
        }
        return report;
    }
    private static BlockPos bed(Mob mob) {
        for (String tag : mob.entityTags()) if (tag.startsWith(BED)) {
            try { return BlockPos.of(Long.parseLong(tag.substring(BED.length()))); }
            catch (NumberFormatException invalid) { return null; }
        }
        return null;
    }
    static boolean ownsBed(Mob mob,BlockPos pos){return pos.equals(bed(mob));}
    static void removedOwnBed(Mob mob,BlockPos pos,CreatureAgents.State state){
        if(ownsBed(mob,pos)){mob.removeTag(BED+pos.asLong());state.mind.home(null);}
    }
    static boolean needsHousing(ServerLevel level,Mob mob){
        return bed(mob)==null||level.getPoiManager().findAll(t->t.is(PoiTypes.HOME),p->nursery(level,mob,p),mob.blockPosition(),48,
            net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.HAS_SPACE).findAny().isEmpty();
    }
    private static boolean homeBed(ServerLevel level, Mob mob, BlockPos pos) {
        var town = WarCombat.home(mob);
        int radius=NativeIllagers.member(mob)?NativeIllagers.radius(mob):34;
        if (town == null || !level.isLoaded(pos) || Math.abs(pos.getX() - town.x()) > radius
            || Math.abs(pos.getZ() - town.z()) > radius || ConstructionSites.isProtected(level, pos)||!TraderRecruitment.allowedBed(level,pos)) return false;
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BedBlock) || state.getValue(BedBlock.PART) != BedPart.HEAD
            || !level.getBlockState(pos.above()).isAir()) return false;
        if(NativeIllagers.member(mob))return NativeIllagers.insideCamp(level,mob,pos)&&!level.canSeeSky(pos.above());
        for (int dy = 2; dy <= 6; dy++) if (!level.getBlockState(pos.above(dy)).getCollisionShape(level, pos.above(dy)).isEmpty()) return true;
        return false;
    }
    private static boolean nursery(ServerLevel level,Mob mob,BlockPos pos){
        return homeBed(level,mob,pos)&&(!NativeIllagers.member(mob)||level.getBlockState(pos.above(2)).isAir());
    }
    static void releaseBed(ServerLevel level, Mob mob) {
        BlockPos claimed = bed(mob);
        if (claimed != null && level.isLoaded(claimed) && level.getBlockState(claimed).getBlock() instanceof BedBlock)
            level.getPoiManager().release(claimed);
        mob.entityTags().stream().filter(t -> t.startsWith(BED)).toList().forEach(mob::removeTag);
    }
    static void release(ServerLevel level, Mob mob) {
        releaseBed(level, mob);
        if (juvenile(mob)) { mob.removeTag(YOUNG); mob.getAttribute(Attributes.SCALE).setBaseValue(1); }
    }
    static void care(ServerLevel level, Mob mob, CreatureAgents.State state) {
        if (!(mob instanceof AbstractIllager) || WarCombat.home(mob) == null) return;
        if (juvenile(mob) && state.mind.ageTicks() >= 24000) {
            mob.removeTag(YOUNG); mob.getAttribute(Attributes.SCALE).setBaseValue(1);
            state.mind.remember("Grew to adulthood"); CreatureAgents.persist(state);
        }
        if (!Villagers.config().breeding() || level.getGameTime() < state.nextBedSearch) return;
        state.nextBedSearch = level.getGameTime() + 1200;
        var claimed = bed(mob);
        if (claimed != null && homeBed(level, mob, claimed)
            &&(!NativeIllagers.member(mob)||mob.blockPosition().distSqr(claimed)>48*48
                ||CampBeds.approachPath(level,mob,claimed,claimed.relative(level.getBlockState(claimed).getValue(BedBlock.FACING).getOpposite()))!=null)) {
            if(NativeIllagers.member(mob))state.mind.home(new Home(level.dimension().identifier().toString(),claimed.getX(),claimed.getY(),claimed.getZ()));
            return;
        }
        releaseBed(level, mob);
        level.getPoiManager().take(t -> t.is(PoiTypes.HOME), (t, p) -> homeBed(level, mob, p)
                &&(!NativeIllagers.member(mob)||CampBeds.approachPath(level,mob,p,p.relative(level.getBlockState(p).getValue(BedBlock.FACING).getOpposite()))!=null), mob.blockPosition(), 64)
            .ifPresent(p -> {
                mob.addTag(BED + p.asLong());
                if(NativeIllagers.member(mob))state.mind.home(new Home(level.dimension().identifier().toString(),p.getX(),p.getY(),p.getZ()));
            });
    }
    private static List<ItemStack> stock(Mob mob) {
        var out = new ArrayList<ItemStack>();
        if (mob instanceof Pillager pillager)
            for (int i = 0; i < pillager.getInventory().getContainerSize(); i++) out.add(pillager.getInventory().getItem(i));
        out.add(mob.getOffhandItem()); return out;
    }
    private static boolean provisioned(Mob mob) {
        return stock(mob).stream().filter(s -> s.is(Items.BREAD)).mapToInt(ItemStack::getCount).sum() >= 6;
    }
    private static void charge(Mob mob) {
        int remaining = 3;
        for (var item : stock(mob)) if (item.is(Items.BREAD)) {
            int paid = Math.min(remaining, item.getCount()); item.shrink(paid); remaining -= paid;
            if (remaining == 0) return;
        }
        throw new IllegalStateException("Family payment changed during birth");
    }
    private static boolean willing(Mob mob, CreatureAgents.State s, Mob partner, long now) {
        return !juvenile(mob) && mob.isAlive() && !mob.isOnFire() && !mob.isInWater() && bed(mob) != null
            && BreedingPolicy.willing(mob.getHealth()/mob.getMaxHealth(), s.mind.foodLevel(), s.mind.fatigue(),
                s.mind.mood().low(), s.mind.opinionOf(partner.getUUID()).regard(), now, s.mind.nextBreedingAt()) && provisioned(mob);
    }
    private static boolean consent(CreatureAgents.State first, CreatureAgents.State second, Mob a, Mob b, long now) {
        return first.familyAgreement != null && second.familyAgreement != null
            && first.familyAgreement.active(now) && second.familyAgreement.active(now)
            && first.familyAgreement.partner().equals(b.getUUID()) && second.familyAgreement.partner().equals(a.getUUID());
    }
    static String seek(ServerLevel level, Mob mob, CreatureAgents.State state) {
        if (!Villagers.config().breeding() || !(mob instanceof AbstractIllager) || WarCombat.home(mob) == null
            || !(WarScenario.active(level)||NativeIllagers.member(mob)) || level.isDarkOutside()) return null;
        long now = level.getGameTime();
        state.mind.updateMood(now, mob.getHealth()/mob.getMaxHealth());
        if (!willing(mob, state, mob, now)) {
            state.familyAgreement = null; state.familyStatus = "not ready for a child"; return null;
        }
        var neighbours = level.getEntitiesOfClass(AbstractIllager.class, mob.getBoundingBox().inflate(Courtship.COMPANY_RADIUS),
            other -> other != mob && other.isAlive() && CreatureAgents.managed(other)
                && Objects.equals(WarCombat.home(mob), WarCombat.home(other)) && mob.hasLineOfSight(other));
        if (now >= state.nextFamilySignal && (state.familyAgreement == null || !state.familyAgreement.active(now))) {
            state.nextFamilySignal = now + 200;
            Courtship.Offer offer = Courtship.Offer.at(mob.getUUID(), now,
                state.mind.lineage() == null ? Set.of() : state.mind.lineage().parents());
            for (var listener : neighbours.stream().limit(Courtship.MAX_HEARERS).toList()) {
                var receiver = CreatureAgents.loadedState(listener); if (receiver == null) continue;
                receiver.mind.updateMood(now, listener.getHealth()/listener.getMaxHealth());
                if (!willing(listener, receiver, mob, now) || state.mind.opinionOf(listener.getUUID()).regard() < 0
                    || receiver.familyAgreement != null && receiver.familyAgreement.active(now)
                    || BreedingPolicy.closeKin(mob.getUUID(), state.mind.lineage(), listener.getUUID(), receiver.mind.lineage())) continue;
                state.familyAgreement = new Courtship.Agreement(listener.getUUID(), now, offer.expiresAt());
                receiver.familyAgreement = new Courtship.Agreement(mob.getUUID(), now, offer.expiresAt());
                state.mind.remember("Agreed to consider a family with " + listener.getUUID());
                receiver.mind.remember("Accepted family invitation from " + mob.getUUID());
                break;
            }
        }
        if (state.familyAgreement == null || !state.familyAgreement.active(now)) {
            state.familyStatus = "seeking a willing family partner"; return null;
        }
        var partner = neighbours.stream().filter(other -> other.getUUID().equals(state.familyAgreement.partner())).findFirst().orElse(null);
        if (partner == null) { state.familyStatus = "partner out of sight"; return null; }
        var other = CreatureAgents.loadedState(partner);
        if (other == null || !consent(state, other, mob, partner, now) || !willing(partner, other, mob, now)) return null;
        if (mob.distanceToSqr(partner) > 9) {
            var path = mob.getNavigation().createPath(partner, 1);
            if (path == null || !path.canReach()||NativeIllagers.member(mob)&&!NativeIllagers.safePath(level,mob,path)) { state.familyStatus = "partner unreachable"; return null; }
            mob.getNavigation().moveTo(path, .8); state.familyStatus = "visiting family partner";
        } else {
            mob.getNavigation().stop(); state.mind.socialize();
            consider(level, mob, state, partner); state.familyStatus = "considering a child";
        }
        return state.familyStatus;
    }
    static void consider(ServerLevel level, Mob a, CreatureAgents.State first, Mob b) {
        if (!Villagers.config().breeding() || !(a instanceof AbstractIllager) || !(b instanceof AbstractIllager)
            || !CreatureAgents.managed(b) || !(WarScenario.active(level)||NativeIllagers.member(a)) || !Objects.equals(WarCombat.home(a), WarCombat.home(b))
            || WarCombat.home(a) == null || level.isDarkOutside()) return;
        var second = CreatureAgents.loadedState(b);
        long now = level.getGameTime();
        if (second == null || !consent(first, second, a, b, now)
            || now < first.nextFamilyAttempt || now < second.nextFamilyAttempt || a.distanceToSqr(b) > 9 || !a.hasLineOfSight(b)) return;
        first.nextFamilyAttempt = second.nextFamilyAttempt = now + 200;
        first.familyAttempts++; second.familyAttempts++;
        first.familyBlock = second.familyBlock = "";
        first.mind.updateMood(now, a.getHealth()/a.getMaxHealth()); second.mind.updateMood(now, b.getHealth()/b.getMaxHealth());
        if (!willing(a, first, b, now) || !willing(b, second, a, now)
            || BreedingPolicy.closeKin(a.getUUID(), first.mind.lineage(), b.getUUID(), second.mind.lineage())) {
            first.familyBlock = second.familyBlock = "parent readiness or kinship"; return;
        }
        if (!level.getEntitiesOfClass(Mob.class, a.getBoundingBox().inflate(16),
            enemy -> enemy.isAlive() && WarCombat.enemies(a, enemy) && (a.hasLineOfSight(enemy) || b.hasLineOfSight(enemy))).isEmpty()) {
            first.familyBlock = second.familyBlock = "visible enemy"; return;
        }
        if (level.getEntitiesOfClass(AbstractIllager.class, a.getBoundingBox().inflate(64), CreatureAgents::managed).size()
            >= BreedingPolicy.LOCAL_POPULATION_CAP) return;
        var beds = level.getPoiManager().findAll(t -> t.is(PoiTypes.HOME), p -> nursery(level, a, p), a.blockPosition(),
            48, net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.HAS_SPACE).limit(8).toList();
        if (beds.isEmpty()) { first.familyBlock = second.familyBlock = "no spare sheltered nursery"; return; }
        if (RANDOM.nextDouble() >= BreedingPolicy.chance(first.mind.genome().tendency(Tendency.FERTILITY),
            second.mind.genome().tendency(Tendency.FERTILITY), first.mind.opinionOf(b.getUUID()).regard(),
            second.mind.opinionOf(a.getUUID()).regard(), beds.size())) {
            first.familyBlock = second.familyBlock = "waiting for conception"; return;
        }
        first.familyBlock = second.familyBlock = "no complete nursery path";
        for (var pos : beds) {
            // Complete native access is required, not merely an unused bed across a wall.
            a.getNavigation().setCanOpenDoors(true);
            var path = NativeIllagers.member(a)?a.getNavigation().createPath(pos,1,64):a.getNavigation().createPath(pos, 1);
            if (path == null || !path.canReach()||NativeIllagers.member(a)&&!NativeIllagers.safePath(level,a,path)) continue;
            if (birth(level, a, first, b, second, pos, now)) return;
        }
    }
    private static boolean birth(ServerLevel level, Mob a, CreatureAgents.State first, Mob b,
                                 CreatureAgents.State second, BlockPos pos, long now) {
        var reserved = level.getPoiManager().take(t -> t.is(PoiTypes.HOME), (t, p) -> p.equals(pos) && nursery(level, a, p), pos, 1);
        if (reserved.isEmpty()) return false;
        Mob baby = RANDOM.nextBoolean() && a instanceof Vindicator && b instanceof Vindicator
            ? new Vindicator(EntityTypes.VINDICATOR, level) : new Pillager(EntityTypes.PILLAGER, level);
        markJuvenile(baby);
        if(NativeIllagers.member(a))NativeIllagers.inherit(a,baby);
        else {baby.addTag(WarScenario.ACTOR);baby.addTag(WarScenario.HOME + local.villagers.war.WarMap.SETTLEMENTS.indexOf(WarCombat.home(a)));}
        baby.addTag(BED + pos.asLong());
        boolean space = false;
        for (var side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            baby.snapTo(a.getX() + side.getStepX()*1.3, a.getY(), a.getZ() + side.getStepZ()*1.3, 0, 0);
            if (level.noCollision(baby) && level.getFluidState(baby.blockPosition()).isEmpty()
                && level.getBlockState(baby.blockPosition().below()).isFaceSturdy(level, baby.blockPosition().below(), net.minecraft.core.Direction.UP)) { space = true; break; }
        }
        if (!space) {
            first.familyBlock = second.familyBlock = "no safe newborn space";
            releaseBed(level, baby); return false;
        }
        if (!willing(a, first, b, now) || !willing(b, second, a, now) || !level.addFreshEntity(baby)) {
            releaseBed(level, baby); return false;
        }
        if (!willing(a, first, b, now) || !willing(b, second, a, now)) {
            releaseBed(level, baby); baby.discard(); return false;
        }
        int beforeA=stock(a).stream().filter(s->s.is(Items.BREAD)).mapToInt(ItemStack::getCount).sum();
        int beforeB=stock(b).stream().filter(s->s.is(Items.BREAD)).mapToInt(ItemStack::getCount).sum();
        charge(a); charge(b);
        var receipt=Map.<String,Object>of("gameTime",now,"child",baby.getUUID().toString(),"parentA",a.getUUID().toString(),"parentB",b.getUUID().toString(),
            "breadBeforeA",beforeA,"breadAfterA",beforeA-3,"breadBeforeB",beforeB,"breadAfterB",beforeB-3,"nursery",pos.toShortString());
        first.lastBirth=second.lastBirth=receipt;
        AgentMind child = AgentMind.born(baby.getUUID(), first.mind, second.mind, RANDOM);
        child.home(new Home(level.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ()));
        child.recordEpisode(Episode.Kind.BORN, now, a.getUUID(), null, "Parents " + a.getUUID() + " and " + b.getUUID(), List.of());
        CreatureAgents.registerChild(level, baby, child);
        first.familyBlock = second.familyBlock = "";
        first.mind.recordEpisode(Episode.Kind.HAD_CHILD, now, child.id(), null, child.name(), List.of());
        second.mind.recordEpisode(Episode.Kind.HAD_CHILD, now, child.id(), null, child.name(), List.of());
        first.familyAgreement = second.familyAgreement = null;
        first.mind.bred(now); second.mind.bred(now);
        first.mind.remember("Had child " + child.id() + " with " + b.getUUID());
        second.mind.remember("Had child " + child.id() + " with " + a.getUUID());
        CreatureAgents.persist(first); CreatureAgents.persist(second);
        Villagers.LOG.info("Ordinary illager birth {} parents={} and {} nursery={}", child.id(), a.getUUID(), b.getUUID(), pos);
        return true;
    }
}
