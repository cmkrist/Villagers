package local.villagers;

import java.util.*;
import local.villagers.mind.AgentMind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Native pillager bags supply a narrow wheat/bread economy; no generic terrain edits. */
final class IllagerFood {
    static final class Work {
        BlockPos target;
        long scanAt, pathAt, targetUntil, workAt, shareAt;
        long hayStarted,hayLast;
        BlockPos workingHay;
        int harvested, crafted, shared;
        int repeatHarvests;
        final LinkedHashSet<BlockPos> harvestedCells = new LinkedHashSet<>();
        final Map<BlockPos, Long> blocked = new HashMap<>();
        final Set<BlockPos> doors = new HashSet<>();
        Map<String, Object> status() {
            return Map.of("harvested", harvested, "crafted", crafted, "shared", shared,
                "repeatHarvests", repeatHarvests, "target", target == null ? "" : target.toShortString());
        }
    }
    private IllagerFood() {}
    static int bread(Pillager mob) {
        return mob.getInventory().countItem(Items.BREAD)
            + (mob.getOffhandItem().is(Items.BREAD) ? mob.getOffhandItem().getCount() : 0);
    }
    static String seekMeal(ServerLevel level, Mob mob, AgentMind mind) {
        if (!(mob instanceof AbstractIllager) || !member(level, mob) || mind.foodLevel() > 16
            || !CreatureAgents.ration(mob).isEmpty()) return null;
        var supplier = level.getEntitiesOfClass(Pillager.class, mob.getBoundingBox().inflate(24),
            other -> other != mob && member(level, other) && other.getOffhandItem().is(Items.BREAD)
                && Objects.equals(WarCombat.home(other), WarCombat.home(mob)) && mob.hasLineOfSight(other))
            .stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        if (supplier == null) {
            var home = mind.home();
            if (home != null && home.dimension().equals(level.dimension().identifier().toString())
                && Math.hypot(mob.getX() - home.x(), mob.getZ() - home.z()) > 12) {
                if(goHome(level,mob,home))return "returning home for food";
            }
            return null;
        }
        if (mob.distanceToSqr(supplier) <= 4) return null;
        if(NativeIllagers.member(mob))NativeIllagers.approach(level,mob,supplier,.8);
        else mob.getNavigation().moveTo(supplier, .8);
        return "seeking offered food";
    }
    private static boolean member(ServerLevel level, Mob mob) {
        return mob.level() == level && mob.isAlive() && CreatureAgents.managed(mob)
            && (WarScenario.active(level)||NativeIllagers.member(mob)) && WarCombat.home(mob) != null;
    }
    private static boolean local(Mob mob, BlockPos pos) {
        var home = WarCombat.home(mob);
        int range=NativeIllagers.member(mob)?NativeIllagers.radius(mob):34;
        return home != null && Math.abs(pos.getX() - home.x()) <= range && Math.abs(pos.getZ() - home.z()) <= range
            &&(!NativeIllagers.member(mob)||mob.level().getWorldBorder().isWithinBounds(pos));
    }
    private static boolean goHome(ServerLevel level,Mob mob,local.villagers.mind.Home home){
        if(NativeIllagers.member(mob)){
            var path=mob.getNavigation().createPath(new BlockPos(home.x(),home.y(),home.z()),1,64);
            if(NativeIllagers.safePath(level,mob,path))return mob.getNavigation().moveTo(path,.8);
            return mob instanceof Pillager p&&approachNearby(level,p,new BlockPos(home.x(),home.y(),home.z()));
        }
        return WarCombat.navigate(level,mob,home.x(),home.z());
    }
    private static boolean hay(ServerLevel level,Pillager mob,BlockPos p){
        return NativeIllagers.member(mob)&&Villagers.config().hayForaging()&&local(mob,p)&&level.isLoaded(p)
            &&level.getBlockState(p).is(Blocks.HAY_BLOCK)&&level.getBlockState(p.above()).isAir()
            &&!ConstructionSites.isProtected(level,p)&&!p.equals(mob.blockPosition().below())
            &&!mob.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(p));
    }
    private static boolean gatherHay(ServerLevel level,Pillager mob,AgentMind mind,Work work,BlockPos p,long now){
        if(!member(level,mob)||!hay(level,mob,p)||!AdventureActions.PERMISSIONS.allows(local.villagers.capability.CapabilityPermissions.Capability.GATHER_HAY)
                ||mob.getEyePosition().distanceToSqr(Vec3.atCenterOf(p))>9||!visible(level,mob,p)
                ||!level.getEntities(mob,new net.minecraft.world.phys.AABB(p).expandTowards(0,2,0)).isEmpty())return false;
        var state=level.getBlockState(p);
        if(!p.equals(work.workingHay)||now-work.hayLast>20){work.workingHay=p;work.hayStarted=now;}
        work.hayLast=now;mob.getNavigation().stop();mob.swingForAttack(InteractionHand.MAIN_HAND);
        if(now-work.hayStarted<Math.max(1,(int)Math.ceil(state.getDestroySpeed(level,p)*30/Math.max(1,mob.getMainHandItem().getDestroySpeed(state)))))return false;
        var next=new net.minecraft.world.SimpleContainer(mob.getInventory().getContainerSize());
        for(int i=0;i<next.getContainerSize();i++)next.setItem(i,mob.getInventory().getItem(i).copy());
        var drops=Block.getDrops(state,level,p,null,mob,mob.getMainHandItem());
        if(drops.isEmpty()||drops.stream().anyMatch(s->!next.addItem(s.copy()).isEmpty())||!hay(level,mob,p)||!level.getBlockState(p).equals(state))return false;
        if(!level.destroyBlock(p,false,mob))return false;
        for(int i=0;i<next.getContainerSize();i++)mob.getInventory().setItem(i,next.getItem(i));
        mind.remember("Gathered native camp hay at "+p.toShortString());mind.exert(.005f);work.workingHay=null;return true;
    }
    private static boolean unpackHay(ServerLevel level,Pillager mob,AgentMind mind){
        if(!NativeIllagers.member(mob)||!AdventureActions.PERMISSIONS.allows(local.villagers.capability.CapabilityPermissions.Capability.UNPACK_HAY))return false;
        var plan=HayForaging.reserve(mob.getInventory());if(plan==null)return false;
        var recipe=level.recipeAccess().getRecipeFor(RecipeType.CRAFTING,plan.input(),level);
        if(recipe.isEmpty()||recipe.get().value().getRemainingItems(plan.input()).stream().anyMatch(s->!s.isEmpty())
                ||!HayForaging.commit(mob.getInventory(),plan,recipe.get().value().assemble(plan.input())))return false;
        mind.remember("Unpacked owned camp hay into nine wheat");return true;
    }
    private static boolean visible(ServerLevel level, Mob mob, BlockPos pos) {
        var hit = level.clip(new ClipContext(mob.getEyePosition(), Vec3.atCenterOf(pos),
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mob));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }
    private static boolean ripe(ServerLevel level, Mob mob, BlockPos pos) {
        if (!local(mob, pos) || !level.isLoaded(pos) || ConstructionSites.isProtected(level, pos)
            || ConstructionSites.isProtected(level, pos.below())) return false;
        var state = level.getBlockState(pos);
        return state.is(Blocks.WHEAT) && ((CropBlock) state.getBlock()).isMaxAge(state)
            && level.getBlockState(pos.below()).is(Blocks.FARMLAND);
    }
    /** Wheat can be harvested by hand in native Minecraft. The seed cost still applies. */
    static boolean harvest(ServerLevel level, Pillager mob, BlockPos pos) {
        if (!member(level, mob) || !ripe(level, mob, pos)
            || mob.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > 9 || !visible(level, mob, pos)) return false;
        var original = level.getBlockState(pos);
        var soil = level.getBlockState(pos.below());
        var tool = mob.getMainHandItem().copy();
        var drops = Block.getDrops(original, level, pos, null, mob, mob.getMainHandItem());
        var plan = HarvestMaterials.reserve(mob.getInventory(), drops, Items.WHEAT_SEEDS);
        if (plan == null || !HarvestMaterials.unchanged(mob.getInventory(), plan)
            || !ItemStack.matches(tool, mob.getMainHandItem()) || !ripe(level, mob, pos)) return false;
        var replanted = ((CropBlock) original.getBlock()).getStateForAge(0);
        if (!level.setBlock(pos, replanted, Block.UPDATE_ALL)) return false;
        if (!member(level, mob) || !level.getBlockState(pos).equals(replanted)
            || !level.getBlockState(pos.below()).equals(soil) || !visible(level, mob, pos)
            || ConstructionSites.isProtected(level, pos) || ConstructionSites.isProtected(level, pos.below())
            || !ItemStack.matches(tool, mob.getMainHandItem()) || !HarvestMaterials.commit(mob.getInventory(), plan)) {
            if (level.getBlockState(pos).equals(replanted)) level.setBlock(pos, original, Block.UPDATE_ALL);
            return false;
        }
        for (var stack : plan.overflow) Block.popResource(level, pos, stack.copy());
        // Keep one planting stack; leave real surplus at the field instead of filling
        // every bag slot with seeds and blocking wheat/bread production.
        int plantingReserve = 64;
        for (int i = 0; i < mob.getInventory().getContainerSize(); i++) {
            var seeds = mob.getInventory().getItem(i);
            if (!seeds.is(Items.WHEAT_SEEDS)) continue;
            int keep = Math.min(plantingReserve, seeds.getCount()); plantingReserve -= keep;
            int excess = seeds.getCount() - keep;
            if (excess > 0) Block.popResource(level, pos, seeds.split(excess));
        }
        mob.swingForAttack(InteractionHand.MAIN_HAND);
        level.levelEvent(2001, pos, Block.getId(original));
        return true;
    }
    static boolean craft(ServerLevel level, Pillager mob, BlockPos pos) {
        if (!member(level, mob) || !local(mob, pos) || !level.isLoaded(pos)
            || !level.getBlockState(pos).is(Blocks.CRAFTING_TABLE)
            || mob.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > 9 || !visible(level, mob, pos)) return false;
        var reserved = BreadCrafting.reserve(mob.getInventory());
        if (reserved == null) return false;
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, reserved.input(), level);
        if (recipe.isEmpty()) return false;
        var rule = recipe.get().value();
        if (rule.getRemainingItems(reserved.input()).stream().anyMatch(s -> !s.isEmpty())) return false;
        return BreadCrafting.commit(mob.getInventory(), reserved, rule.assemble(reserved.input()));
    }
    private static boolean approach(ServerLevel level, Pillager mob, Work work, BlockPos target, long now) {
        openRouteDoors(level, mob, work);
        if (now < work.pathAt) return true;
        work.pathAt = now + 40;
        if (!NativeIllagers.member(mob)&&mob.distanceToSqr(Vec3.atCenterOf(target)) > 16*16)
            return WarCombat.navigate(level, mob, target.getX(), target.getZ());
        return approachNearby(level, mob, target);
    }
    static void openRouteDoors(ServerLevel level, Pillager mob, Work work) {
        mob.getNavigation().setCanOpenDoors(true);
        // Operate only a wooden door on the immediate native route; never break it.
        var path = mob.getNavigation().getPath();
        if (path != null && !path.isDone()) {
            for (int i = path.getNextNodeIndex(); i < Math.min(path.getNodeCount(), path.getNextNodeIndex() + 3); i++) {
                var p = path.getNodePos(i);
                var state = level.getBlockState(p);
                if (p.distSqr(mob.blockPosition()) <= 9 && state.getBlock() instanceof DoorBlock door
                    && DoorBlock.isWoodenDoor(state) && !ConstructionSites.isProtected(level, p)
                    && !state.getValue(DoorBlock.OPEN)) {
                    door.setOpen(mob, level, state, p, true); work.doors.add(p.immutable());
                }
            }
        }
    }
    static boolean approachNearby(ServerLevel level, Pillager mob, BlockPos target) {
        var stands = new ArrayList<BlockPos>();
        for (BlockPos pos : BlockPos.betweenClosed(target.offset(-2, -1, -2), target.offset(2, 1, 2))) {
            if (local(mob, pos) && level.isLoaded(pos) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty() && level.getFluidState(pos).isEmpty()
                && (level.getBlockState(pos.below()).is(Blocks.FARMLAND)
                    || level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP))
                && Vec3.atBottomCenterOf(pos).add(0, mob.getEyeHeight(), 0).distanceToSqr(Vec3.atCenterOf(target)) <= 9)
                stands.add(pos.immutable());
        }
        stands.sort(Comparator.comparingDouble(p -> mob.position().distanceToSqr(Vec3.atBottomCenterOf(p))));
        for (var stand : stands.stream().limit(6).toList()) {
            var next = NativeIllagers.member(mob)?mob.getNavigation().createPath(stand,0,64):mob.getNavigation().createPath(stand, 0);
            if(NativeIllagers.member(mob)&&!NativeIllagers.safePath(level,mob,next))next=null;
            if (next != null && next.canReach()) return mob.getNavigation().moveTo(next, .8);
        }
        return false;
    }
    private static void closeDoors(ServerLevel level, Mob mob, Work work) {
        work.doors.removeIf(pos -> {
            if (!level.isLoaded(pos)) return true;
            if (pos.distSqr(mob.blockPosition()) < 9) return false;
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof DoorBlock door && state.getValue(DoorBlock.OPEN)
                && !ConstructionSites.isProtected(level, pos)
                && level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(pos).inflate(1)).isEmpty())
                door.setOpen(mob, level, state, pos, false);
            return true;
        });
    }
    static String tick(ServerLevel level, Mob actor, AgentMind mind, Work work) {
        if (!(actor instanceof Pillager mob) || IllagerFamilies.juvenile(mob) || !member(level, mob)) return null;
        long now = level.getGameTime();
        closeDoors(level, mob, work);
        openRouteDoors(level, mob, work);
        if (mob.getOffhandItem().isEmpty() && mob.getInventory().countItem(Items.BREAD) > 1) {
            for (int i = 0; i < mob.getInventory().getContainerSize(); i++)
                if (mob.getInventory().getItem(i).is(Items.BREAD)) {
                    mob.setItemSlot(EquipmentSlot.OFFHAND, mob.getInventory().removeItem(i, 1)); break;
                }
        }
        // A visible empty offhand is an invitation to offer a meal, not access to another mind/bag.
        if (now >= work.shareAt && bread(mob) > 3) {
            var neighbour = level.getEntitiesOfClass(AbstractIllager.class, mob.getBoundingBox().inflate(3),
                other -> other != mob && member(level, other) && other.getOffhandItem().isEmpty()
                    && Objects.equals(WarCombat.home(other), WarCombat.home(mob)) && mob.hasLineOfSight(other))
                .stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
            if (neighbour != null) {
                ItemStack food = mob.getOffhandItem().is(Items.BREAD) ? mob.getOffhandItem() : ItemStack.EMPTY;
                for (int i = 0; food.isEmpty() && i < mob.getInventory().getContainerSize(); i++)
                    if (mob.getInventory().getItem(i).is(Items.BREAD)) food = mob.getInventory().getItem(i);
                if (!food.isEmpty()) {
                    neighbour.setItemSlot(EquipmentSlot.OFFHAND, food.split(1));
                    work.shared++; work.shareAt = now + 200;
                    mind.remember("Shared real bread with " + neighbour.getUUID());
                }
            }
        }
        if (bread(mob) >= (NativeIllagers.member(mob)?6:8)) {
            work.target = null;
            var state = CreatureAgents.loadedState(mob);
            boolean meetingFamily = state != null && state.familyAgreement != null && state.familyAgreement.active(now);
            var home = mind.home();
            if (!meetingFamily && home != null && home.dimension().equals(level.dimension().identifier().toString())
                && Math.hypot(mob.getX() - home.x(), mob.getZ() - home.z()) > 12) {
                if(goHome(level,mob,home))return "bringing food home";
            }
            return null;
        }
        if (mind.fatigue() > .65 || mob.getHealth() < mob.getMaxHealth() * .5 || level.isDarkOutside()) return null;
        if(BreadCrafting.wheatCount(mob.getInventory())<3&&HayForaging.hasHay(mob.getInventory())&&unpackHay(level,mob,mind))return "unpacked real camp hay";
        boolean crafting = BreadCrafting.wheatCount(mob.getInventory()) >= 3;
        work.blocked.entrySet().removeIf(entry -> now >= entry.getValue());
        if (work.target != null && (now >= work.targetUntil || !local(mob, work.target)
            || (crafting ? !level.getBlockState(work.target).is(Blocks.CRAFTING_TABLE) : !ripe(level, mob, work.target)&&!hay(level,mob,work.target)))) {
            if (now >= work.targetUntil) work.blocked.put(work.target, now + 400);
            work.target = null;
        }
        if (work.target == null && now >= work.scanAt) {
            work.scanAt = now + 100;
            if (crafting) {
                if(NativeIllagers.member(mob)){
                    for(var pos:NativeIllagers.knownTables(level,mob))if(local(mob,pos)&&!work.blocked.containsKey(pos)
                        &&(work.target==null||mob.distanceToSqr(Vec3.atCenterOf(pos))<mob.distanceToSqr(Vec3.atCenterOf(work.target))))work.target=pos;
                    for(var pos:BlockPos.betweenClosed(mob.blockPosition().offset(-16,-3,-16),mob.blockPosition().offset(16,3,16)))
                        if(local(mob,pos)&&level.isLoaded(pos)&&level.getBlockState(pos).is(Blocks.CRAFTING_TABLE)&&!ConstructionSites.isProtected(level,pos)&&!work.blocked.containsKey(pos)
                            ){
                            NativeIllagers.rememberTable(level,mob,pos);
                            if(work.target==null||mob.distanceToSqr(Vec3.atCenterOf(pos))<mob.distanceToSqr(Vec3.atCenterOf(work.target)))work.target=pos.immutable();
                        }
                }else{
                // These are authored public town stations, already known like settlement destinations.
                var home = WarCombat.home(mob);
                for (int dx : new int[]{-16, 20}) for (int dz : new int[]{-16, 20}) {
                    var pos = new BlockPos(home.x() + dx, local.villagers.war.WarMap.height(home.x(), home.z()) + 1, home.z() + dz);
                    if (level.isLoaded(pos) && level.getBlockState(pos).is(Blocks.CRAFTING_TABLE) && !work.blocked.containsKey(pos)
                        && (work.target == null || mob.distanceToSqr(Vec3.atCenterOf(pos)) < mob.distanceToSqr(Vec3.atCenterOf(work.target))))
                        work.target = pos;
                }
                }
            } else {
                var origin = mob.blockPosition();
                int radius=NativeIllagers.member(mob)?16:12;
                for (var pos : BlockPos.betweenClosed(origin.offset(-radius, -3, -radius), origin.offset(radius, 3, radius)))
                    if ((ripe(level, mob, pos)||hay(level,mob,pos)) && !work.blocked.containsKey(pos) && visible(level, mob, pos)
                        && (work.target == null || mob.distanceToSqr(Vec3.atCenterOf(pos)) < mob.distanceToSqr(Vec3.atCenterOf(work.target))))
                        work.target = pos.immutable();
            }
            work.targetUntil = now + 600;
            if (!crafting && work.target != null&&level.getBlockState(work.target).is(Blocks.WHEAT)) {
                var p = work.target;
                mind.observeFarm(new local.villagers.mind.Place(level.dimension().identifier().toString(),
                    p.getX(), p.getY(), p.getZ()), "minecraft:wheat", now, List.of());
            }
        }
        if (work.target == null) {
            // A provisioner returns to a field it actually saw instead of wandering away
            // during scan cooldowns. Farm episodes persist through ordinary mind saves.
            if (!crafting) {
                var field = mind.observedFarms().stream().map(e -> e.place())
                    .filter(p -> p.dimension().equals(level.dimension().identifier().toString()))
                    .map(p -> new BlockPos(p.x(), p.y(), p.z()))
                    .filter(p -> local(mob, p) && level.isLoaded(p) && !work.blocked.containsKey(p)
                        && !ConstructionSites.isProtected(level, p) && !ConstructionSites.isProtected(level, p.below()))
                    .min(Comparator.comparingDouble(p -> mob.distanceToSqr(Vec3.atCenterOf(p)))).orElse(null);
                if (field != null) {
                    if (mob.distanceToSqr(Vec3.atCenterOf(field)) <= 9) {
                        mob.getNavigation().stop(); if (now % 80 < 10) mind.rest();
                        return "waiting for wheat";
                    }
                    if (approach(level, mob, work, field, now)) return "returning to wheat field";
                    work.blocked.put(field, now + 400);
                }
            }
            return null;
        }
        boolean nativeHay=hay(level,mob,work.target);
        if (now >= work.workAt && (crafting ? craft(level, mob, work.target) : nativeHay?gatherHay(level,mob,mind,work,work.target,now):harvest(level, mob, work.target))) {
            if (crafting) { work.crafted++; mind.remember("Crafted real bread from three wheat"); }
            else if(!nativeHay){
                work.harvested++;
                if (!work.harvestedCells.add(work.target)) work.repeatHarvests++;
                if (work.harvestedCells.size() > 128) work.harvestedCells.remove(work.harvestedCells.iterator().next());
                mind.remember("Harvested wheat and paid a real seed at " + work.target.toShortString());
            }
            mind.exert(.05f); work.workAt = now + 40; work.target = null;
        } else if (!approach(level, mob, work, work.target, now)) {
            work.blocked.put(work.target, now + 400); work.target = null;
        }
        return crafting ? "crafting bread" : nativeHay?"gathering existing camp hay":"harvesting wheat";
    }
}
