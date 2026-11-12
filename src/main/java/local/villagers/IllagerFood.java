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
        BlockPos workingHay;boolean shortHayRoute;
        int harvested, crafted, shared;
        int repeatHarvests;
        boolean restoredHayMemory;
        Map<String,Object> hayReturn=Map.of();
        net.minecraft.world.level.pathfinder.Path hayRoute,workRoute;BlockPos hayLandmark,workDestination;
        final local.villagers.navigation.RouteProgress workProgress=new local.villagers.navigation.RouteProgress(120);
        final local.villagers.navigation.RouteProgress hayProgress=new local.villagers.navigation.RouteProgress(120);
        final LinkedHashSet<BlockPos> harvestedCells = new LinkedHashSet<>();
        final Map<BlockPos, Long> blocked = new HashMap<>();
        final Set<BlockPos> doors = new HashSet<>();
        Map<String, Object> status() {
            return Map.of("harvested", harvested, "crafted", crafted, "shared", shared,
                "repeatHarvests", repeatHarvests, "target", target == null ? "" : target.toShortString(),
                "blocked",blocked.entrySet().stream().map(e->Map.of("position",e.getKey().toShortString(),"until",e.getValue())).toList(),"hayReturn",hayReturn);
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
            var target=new BlockPos(home.x(),home.y(),home.z());
            var state=level.getBlockState(target);
            // A useful partial route can repeatedly stop on a roof before the
            // native search has explored the stairs. Prefer a complete safe
            // interaction stand for the actual home before that fallback.
            if(state.getBlock() instanceof BedBlock){
                var foot=target.relative(state.getValue(BedBlock.FACING).getOpposite());
                var path=CampBeds.approachPath(level,mob,target,foot);
                if(path!=null&&mob.getNavigation().moveTo(path,.7))return true;
            }else if(mob instanceof Pillager p&&approachNearby(level,p,target))return true;
            return NativeIllagers.returnHome(level,mob,target);
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
        NativeIllagers.rememberHay(level,mob,p);
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
        return visibleFrom(level,mob,mob.getEyePosition(),pos);
    }
    private static boolean visibleFrom(ServerLevel level,Mob mob,Vec3 eye,BlockPos pos){
        var hit = level.clip(new ClipContext(eye, Vec3.atCenterOf(pos),
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
    private static void hurryFoodSupplies(Pillager mob,long now){
        var actor=CreatureAgents.loadedState(mob);
        if(actor!=null&&NativeIllagers.member(mob))NativeCampMeals.hurryForCriticalSupplies(mob,actor,now);
    }
    private static boolean approach(ServerLevel level, Pillager mob, Work work, BlockPos target, long now) {
        openRouteDoors(level, mob, work);
        var active=mob.getNavigation().getPath();
        if(active!=null&&active==work.workRoute&&!active.isDone()&&target.equals(work.workDestination)){
            if(!work.workProgress.stalledAdvance(target.toShortString(),now,active.getNextNodeIndex())){
                hurryFoodSupplies(mob,now);return true;
            }
            NativeBodyRoutes.failedStep(mob,active.getNextNodePos(),now);mob.getNavigation().stop();work.workRoute=null;work.pathAt=now;
        }
        if (now < work.pathAt) return true;
        work.pathAt = now + 40;
        if (!NativeIllagers.member(mob)&&mob.distanceToSqr(Vec3.atCenterOf(target)) > 16*16)
            return WarCombat.navigate(level, mob, target.getX(), target.getZ());
        var path=approachPath(level,mob,target);
        if(path!=null&&mob.getNavigation().moveTo(path,.8)){
            work.workRoute=mob.getNavigation().getPath();work.workDestination=target;work.workProgress.reset();hurryFoodSupplies(mob,now);return true;
        }
        work.workRoute=null;mob.getNavigation().stop();return false;
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
        var path=approachPath(level,mob,target);
        if(path!=null&&mob.getNavigation().moveTo(path,.8))return true;
        // A failed new job must not leave the previous route driving the body.
        mob.getNavigation().stop();return false;
    }
    static net.minecraft.world.level.pathfinder.Path approachPath(ServerLevel level,Pillager mob,BlockPos target){
        var stands = new ArrayList<BlockPos>();
        for (BlockPos pos : BlockPos.betweenClosed(target.offset(-2, -1, -2), target.offset(2, 1, 2))) {
            if (local(mob, pos) && level.isLoaded(pos) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty() && level.getFluidState(pos).isEmpty()
                && (level.getBlockState(pos.below()).is(Blocks.FARMLAND)
                    || level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP))
                && Vec3.atBottomCenterOf(pos).add(0, mob.getEyeHeight(), 0).distanceToSqr(Vec3.atCenterOf(target)) <= 9
                &&visibleFrom(level,mob,Vec3.atBottomCenterOf(pos).add(0,mob.getEyeHeight(),0),target))
                stands.add(pos.immutable());
        }
        stands.sort(Comparator.comparingDouble(p -> mob.position().distanceToSqr(Vec3.atBottomCenterOf(p))));
        if(NativeIllagers.member(mob)){
            var route=CampBeds.pathToStands(level,mob,stands);
            return route;
        }
        for (var stand : stands.stream().limit(6).toList()) {
            var next = NativeIllagers.member(mob)?mob.getNavigation().createPath(stand,0,64):mob.getNavigation().createPath(stand, 0);
            if(NativeIllagers.member(mob)&&!NativeIllagers.safePath(level,mob,next))next=null;
            if (next != null && next.canReach()) return next;
        }
        return null;
    }
    /** Return near the actor's own remembered harvesting place, not to an
     * interaction stand for a block that it already removed. Actual hay is
     * still found by the ordinary local visible scan and harvested at contact. */
    static net.minecraft.world.level.pathfinder.Path hayLandmarkPath(ServerLevel level,Pillager mob,BlockPos landmark){
        if(!NativeIllagers.member(mob)||!local(mob,landmark)||!level.isLoaded(landmark))return null;
        var stands=new ArrayList<BlockPos>();double distance=mob.position().distanceToSqr(Vec3.atCenterOf(landmark));
        for(var p:BlockPos.betweenClosed(landmark.offset(-6,-3,-6),landmark.offset(6,2,6))){
            if(!local(mob,p)||!NativeIllagers.insideCamp(level,mob,p)||!level.isLoaded(p.above())||!level.isLoaded(p.below())
                ||!level.getBlockState(p).isAir()||!level.getBlockState(p.above()).isAir()
                ||!level.getFluidState(p.below()).isEmpty())continue;
            double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,p);
            var feet=new Vec3(p.getX()+.5,floor,p.getZ()+.5);
            double remaining=feet.distanceToSqr(Vec3.atCenterOf(landmark));
            if(remaining>64||remaining>=distance-4)continue;
            var body=mob.getBoundingBox().move(feet.subtract(mob.position()));
            if(level.noCollision(mob,body)&&!level.noBlockCollision(mob,body.move(0,-.03,0)))stands.add(p.immutable());
        }
        return CampBeds.pathToStands(level,mob,stands);
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
        if(NativeIllagers.member(mob)&&!work.restoredHayMemory){
            work.restoredHayMemory=true;
            // Older saves already contain the actor's own paid harvesting
            // memories. Migrate those landmarks, never operator coordinates.
            String prefix="Gathered native camp hay at ";
            for(var memory:mind.memories())if(memory.note()!=null&&memory.note().startsWith(prefix))try{
                var a=memory.note().substring(prefix.length()).split(", ");
                if(a.length==3)NativeIllagers.rememberHay(level,mob,new BlockPos(Integer.parseInt(a[0]),Integer.parseInt(a[1]),Integer.parseInt(a[2])));
            }catch(NumberFormatException ignored){}
        }
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
            boolean meetingFamily = state != null && (state.familyAgreement != null && state.familyAgreement.active(now)
                ||state.campMeals.patrolling(now));
            var home = mind.home();
            if (!meetingFamily && home != null && home.dimension().equals(level.dimension().identifier().toString())
                && Math.hypot(mob.getX() - home.x(), mob.getZ() - home.z()) > 12) {
                if(goHome(level,mob,home))return "bringing food home";
            }
            return null;
        }
        if (mind.fatigue() > .65 || mob.getHealth() < mob.getMaxHealth() * .5 || level.isDarkOutside()&&!NativeIllagers.member(mob)) return null;
        if(BreadCrafting.wheatCount(mob.getInventory())<3&&HayForaging.hasHay(mob.getInventory())&&unpackHay(level,mob,mind))return "unpacked real camp hay";
        boolean crafting = BreadCrafting.wheatCount(mob.getInventory()) >= 3;
        // Finish this actor's selected, bounded store inspection before an old
        // hay recollection starts a different route. No container contents are
        // consulted here; NativeCampWork must still reach and open the store.
        var state=CreatureAgents.loadedState(mob);
        if(!crafting&&work.target==null&&state!=null&&state.campWork.target!=null
                &&now<=state.campWork.targetUntil)return null;
        work.blocked.entrySet().removeIf(entry -> now >= entry.getValue());
        var continuing=mob.getNavigation().getPath();
        if(work.target!=null&&work.target.equals(work.workDestination)&&continuing!=null&&continuing==work.workRoute&&!continuing.isDone()
            &&!work.workProgress.stalledAdvance(work.target.toShortString(),now,continuing.getNextNodeIndex()))
            work.targetUntil=Math.max(work.targetUntil,now+120);
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
                    // A geometrically close table can require a much longer
                    // roof detour than another known station. Choose an actual
                    // complete native route while this planning batch is open.
                    net.minecraft.world.level.pathfinder.Path best=null;BlockPos chosen=null;
                    for(var table:NativeIllagers.knownTables(level,mob).stream().filter(p->local(mob,p)&&!work.blocked.containsKey(p))
                        .sorted(Comparator.comparingDouble(p->p.distSqr(mob.blockPosition()))).limit(4).toList()){
                        var path=approachPath(level,mob,table);
                        if(path!=null&&(best==null||path.getNodeCount()<best.getNodeCount())){best=path;chosen=table;}
                    }
                    work.target=chosen;
                    if(best!=null&&mob.getNavigation().moveTo(best,.8)){
                        work.pathAt=now+40;work.workRoute=mob.getNavigation().getPath();work.workDestination=chosen;work.workProgress.reset();hurryFoodSupplies(mob,now);
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
            if(!crafting&&NativeIllagers.member(mob)){
                var active=mob.getNavigation().getPath();
                // Native navigation can abandon an obstructed short path before
                // our route timeout. Reconstructing that path is not progress:
                // measure the same landmark across replacements, using the body.
                if(work.hayLandmark!=null&&work.shortHayRoute&&work.hayProgress.stalledCloser(work.hayLandmark.toShortString(),now,mob.position().distanceTo(Vec3.atCenterOf(work.hayLandmark)))){
                    if(active==work.hayRoute){
                        if(active!=null&&!active.isDone())NativeBodyRoutes.failedStep(mob,active.getNextNodePos(),now);
                        mob.getNavigation().stop();
                    }
                    work.blocked.put(work.hayLandmark,now+400);work.hayRoute=null;work.hayLandmark=null;
                }
                if(work.hayRoute!=null&&active==work.hayRoute&&!active.isDone()){
                    if(work.shortHayRoute||!work.hayProgress.stalledAdvance(work.hayLandmark.toShortString(),now,active.getNextNodeIndex()))
                        return "following the native return to my observed hay area";
                    NativeBodyRoutes.failedStep(mob,active.getNextNodePos(),now);mob.getNavigation().stop();
                    work.blocked.put(work.hayLandmark,now+400);work.hayRoute=null;work.hayLandmark=null;
                }
                var stores=new ArrayList<>(NativeIllagers.knownHay(level,mob));
                stores.sort(Comparator.comparingDouble(p->p.distSqr(mob.blockPosition())));
                for(var store:stores){
                    if(work.blocked.containsKey(store))continue;
                    if(mob.distanceToSqr(Vec3.atCenterOf(store))<=8*8&&visible(level,mob,store)){
                        NativeIllagers.forgetHay(mob,store);continue;
                    }
                    // No active return path means this is only a planning
                    // cooldown. Let real local storage work use the turn.
                    if(now<work.pathAt)return null;
                    work.pathAt=now+40;
                    var route=hayLandmarkPath(level,mob,store);
                    boolean moved=route!=null&&mob.getNavigation().moveTo(route,.8);
                    work.hayReturn=Map.of("gameTime",now,"landmark",store.toShortString(),"from",mob.position().toString(),
                        "moved",moved,"nodes",route==null?0:route.getNodeCount(),"end",route==null?"":route.getEndNode().asBlockPos().toShortString());
                    if(moved){
                        work.hayRoute=mob.getNavigation().getPath();work.hayLandmark=store;
                        work.shortHayRoute=route.getNodeCount()<=8;
                        if(work.shortHayRoute)work.hayProgress.stalledCloser(store.toShortString(),now,mob.position().distanceTo(Vec3.atCenterOf(store)));
                        else work.hayProgress.reset();
                        hurryFoodSupplies(mob,now);
                        return "returning near a hay store I actually harvested to observe its remaining supplies";
                    }
                    work.hayRoute=null;work.blocked.put(store,now+400);
                }
            }
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
                    if (mob.distanceToSqr(Vec3.atCenterOf(field)) <= 9&&visible(level,mob,field)) {
                        if(!level.getBlockState(field).is(Blocks.WHEAT)){
                            work.blocked.put(field,now+1200);return null;
                        }
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
            if (crafting) {
                int made=1;var actorState=CreatureAgents.loadedState(mob);
                // Finish a small native shift-craft at the real bench before
                // urgent deliveries take the provisioner away from it.
                boolean urgent=NativeIllagers.member(mob)&&actorState!=null
                    &&(actorState.campMeals.heard.entrySet().stream().anyMatch(e->now<e.getValue()&&actorState.campMeals.injured.contains(e.getKey()))
                        ||NativeCampCalls.heardInjury(actorState.campMeals.calls,now));
                while(urgent&&made<3&&craft(level,mob,work.target))made++;
                work.crafted+=made;
                for(int i=0;i<made;i++)mind.remember("Crafted real bread from three wheat");
                if(made>1)mind.exert(.05f*(made-1));
            }
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
