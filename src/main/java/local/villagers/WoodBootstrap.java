package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/** Finite, material-backed preparation for an observed food need or a nominated adventure. */
final class WoodBootstrap {
    enum Goal { TABLE, HOE, PICKAXE, STONE_PICKAXE }
    static final class State {
        BlockPos tree, working;
        Block treeBlock;
        int taken;
        long nextWork, nextPath, lastWork, started, nextSearch, recipes, logs;
        ItemStack tool = ItemStack.EMPTY;
        String status = "no preparation job";
        String lastPath = "none";
        long pathQueries;
        Goal goal;
    }
    static Map<String,Object> status(Session s) {
        var w = s.woodBootstrap;
        return Map.of("activity", w.status, "goal", w.goal == null ? "NONE" : w.goal.name(), "logs", w.logs, "recipes", w.recipes,
            "pathQueries",w.pathQueries,"lastPath",w.lastPath);
    }
    static boolean complete(ServerLevel level, Villager v, Session s, Goal goal) {
        return switch (goal) {
            case TABLE -> BreadCrafting.table(level, v, s) != null;
            case HOE -> AdventureActions.hasHoe(v);
            case PICKAXE -> AdventurePreparation.pickaxe(v);
            case STONE_PICKAXE -> AdventurePreparation.stonePickaxe(v);
        };
    }
    static boolean safe(ServerLevel level, Villager v, Session s) {
        return Villagers.isManaged(v) && v.level() == level && !v.isPassenger() && !v.isTrading() && !v.isSleeping()
            && !v.isOnFire() && !v.isInWater() && v.getAirSupply() >= 200
            && FoodSupply.mayWork(v.getAge(), v.getHealth(), s.mind.fatigue(), !VillageSurvival.scheduledRest(level, v));
    }
    static String work(ServerLevel level, Villager v, Session s, Goal goal, long now) {
        var w = s.woodBootstrap;
        if (!safe(level, v, s) || (goal == Goal.PICKAXE || goal == Goal.STONE_PICKAXE) && !AdventurePreparation.nominated(v)) {
            w.started = 0; return null;
        }
        if (complete(level, v, s, goal)) { w.goal = null; w.started = 0; w.status = "prepared " + goal.name().toLowerCase(); return null; }
        w.goal = goal;
        if (now < w.nextWork) return w.status;
        BlockPos table = BreadCrafting.table(level, v, s);
        if(table==null&&BreadCrafting.rememberedTable(level,v,s)!=null) {
            w.nextWork=Math.max(now+20,s.blockedCraftUntil);
            return w.status="waiting for access to the remembered workbench";
        }
        if (table == null && v.getInventory().hasAnyMatching(a -> a.is(Items.CRAFTING_TABLE))) {
            if (BoatPreparation.placeTable(level, v, s)) return w.status = "placed an owned workbench";
            w.nextWork = now + 100; return w.status = "needs a clear outdoor workbench site";
        }
        var plan = table == null ? WoodCrafting.table(v.getInventory()) : switch (goal) {
            case TABLE -> null;
            case HOE -> WoodCrafting.next(v.getInventory());
            case PICKAXE -> WoodCrafting.pickaxe(v.getInventory());
            case STONE_PICKAXE -> WoodCrafting.stonePickaxe(v.getInventory());
        };
        if (plan == null) return gather(level, v, s, now);
        boolean needsTable = plan.step() == WoodCrafting.Step.HOE || plan.step() == WoodCrafting.Step.PICKAXE || plan.step() == WoodCrafting.Step.STONE_PICKAXE;
        if (needsTable && !BreadCrafting.canWorkAt(level, v, s, table)) {
            w.started = 0; BreadCrafting.approach(level, v, s, table, now);
            return w.status = "taking owned wood to a workbench";
        }
        v.getNavigation().stop();
        if (craft(level, v, s, plan, table)) {
            w.nextWork = now + 30; return w.status = "crafting " + goal.name().toLowerCase() + " supplies";
        }
        w.nextWork = now + 100; return w.status = "preparation recipe or bag capacity blocked";
    }
    private static boolean craft(ServerLevel level, Villager v, Session s, WoodCrafting.Plan plan, BlockPos table) {
        Capability permission = switch (plan.step()) {
            case HOE -> Capability.CRAFT_WOODEN_HOE;
            case PICKAXE -> Capability.CRAFT_WOODEN_PICKAXE;
            case STONE_PICKAXE -> Capability.CRAFT_STONE_PICKAXE;
            default -> Capability.BUILD_CRAFTING_TABLE;
        };
        if (!AdventureActions.PERMISSIONS.allows(permission) || !safe(level, v, s)) return false;
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, plan.input(), level);
        if (recipe.isEmpty() || recipe.get().value().getRemainingItems(plan.input()).stream().anyMatch(a -> !a.isEmpty())) return false;
        if ((plan.step() == WoodCrafting.Step.HOE || plan.step() == WoodCrafting.Step.PICKAXE || plan.step() == WoodCrafting.Step.STONE_PICKAXE)
                && !BreadCrafting.canWorkAt(level, v, s, table)) return false;
        if (!WoodCrafting.commit(v.getInventory(), plan, recipe.get().value().assemble(plan.input()))) return false;
        s.woodBootstrap.recipes++;
        if (plan.step() == WoodCrafting.Step.HOE) { s.hoesCrafted++; AdventureActions.prepareHoeForWork(level, v); }
        if (s.woodBootstrap.goal == Goal.PICKAXE || s.woodBootstrap.goal == Goal.STONE_PICKAXE) s.adventureCrafts++;
        s.mind.remember("Prepared " + plan.count() + " " + plan.output() + " from possessed wood using a native recipe");
        Villagers.persist(s); return true;
    }
    private static String gather(ServerLevel level, Villager v, Session s, long now) {
        var w = s.woodBootstrap;
        if (!AdventureActions.PERMISSIONS.allows(Capability.FORESTRY)) return null;
        var litter=LooseWood.collect(level,v,s,now);
        if(litter!=null)return w.status=litter;
        if (w.tree != null && (w.taken >= 4 || !level.isLoaded(w.tree.above(w.taken))
                || !level.getBlockState(w.tree.above(w.taken)).is(w.treeBlock))) { w.tree = null; w.started = 0; }
        if (w.tree == null) {
            if (now < w.nextSearch) return null;
            if (!WorkNavigation.admit(level, now, v)) return w.status = "looking for preparation wood";
            var origin = v.blockPosition(); var trees = new ArrayList<BlockPos>();
            for (BlockPos p : BlockPos.betweenClosed(origin.offset(-28,-3,-28), origin.offset(28,3,28)))
                if (BoatPreparation.naturalTrunk(level, p) && VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(p))) trees.add(p.immutable());
            trees.sort(Comparator.comparingDouble(p -> p.distSqr(origin)));
            if (trees.isEmpty()) { w.nextSearch = now + 200; w.status = "needs accessible natural wood"; return null; }
            w.tree = trees.getFirst(); w.treeBlock = level.getBlockState(w.tree).getBlock(); w.taken = 0;
        }
        var p = w.tree.above(w.taken); var block = level.getBlockState(p);
        if (ConstructionSites.isProtected(level,p) || !VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(p))) {
            w.tree = null; w.started = 0; w.nextSearch = now + 200; return null;
        }
        if (v.getEyePosition().distanceToSqr(Vec3.atCenterOf(p)) > 9 || !BoatRoutes.visible(level,v,Vec3.atCenterOf(p))) {
            w.started = 0;
            if (now >= w.nextPath && WorkNavigation.admit(level,now,v)) {
                w.nextPath = now + 40;
                var path = approachPath(level,v,s,p);
                w.pathQueries++;
                w.lastPath = path == null ? "none to " + p : "reach="+path.canReach()+" nodes="+path.getNodeCount()+" target="+p+" end="+path.getNodePos(path.getNodeCount()-1);
                if (WorkNavigation.staysHome(level,v,s,path)) w.lastPath += " moved="+v.getNavigation().moveTo(path,.6);
                else { w.tree = null; w.nextSearch = now + 200; return null; }
            }
            return w.status = "walking to a natural tree for " + w.goal.name().toLowerCase();
        }
        v.getNavigation().stop(); v.swingForAttack(InteractionHand.MAIN_HAND);
        if (!p.equals(w.working) || now - w.lastWork > 20 || !ItemStack.matches(w.tool,v.getMainHandItem())) {
            w.started = now; w.working = p; w.tool = v.getMainHandItem().copy();
        }
        w.lastWork = now;
        int duration = (int)Math.ceil(block.getDestroySpeed(level,p) * 30 / Math.max(1,v.getMainHandItem().getDestroySpeed(block)));
        if (now - w.started < Math.max(10,duration)) return w.status = "working natural wood for " + w.goal.name().toLowerCase();
        var drops = Block.getDrops(block,level,p,null,v,v.getMainHandItem());
        var next = new SimpleContainer(v.getInventory().getContainerSize());
        for (int i=0;i<next.getContainerSize();i++) next.setItem(i,v.getInventory().getItem(i).copy());
        if (drops.isEmpty() || drops.stream().anyMatch(a -> !next.addItem(a.copy()).isEmpty())) {
            w.nextWork = now + 100; return w.status = "preparation wood will not fit";
        }
        if (!level.getBlockState(p).equals(block) || !ItemStack.matches(w.tool,v.getMainHandItem()) || !level.destroyBlock(p,false,v)) return null;
        for (int i=0;i<next.getContainerSize();i++) v.getInventory().setItem(i,next.getItem(i));
        var tool = v.getMainHandItem().get(DataComponents.TOOL);
        if (tool != null && block.getDestroySpeed(level,p) > 0) v.getMainHandItem().hurtAndBreak(tool.damagePerBlock(),v,EquipmentSlot.MAINHAND);
        w.taken++; w.logs++; w.started = 0; w.nextWork = now + 10;
        s.mind.remember("Gathered actual natural wood for " + w.goal.name().toLowerCase() + " at " + p.toShortString());
        return w.status = "gathered preparation wood";
    }
    /** Solid logs are interaction targets, never native navigation destinations. At most three stand paths. */
    static net.minecraft.world.level.pathfinder.Path approachPath(ServerLevel level,Villager v,Session s,BlockPos target) {
        var stands = new ArrayList<BlockPos>();
        for (BlockPos stand : BlockPos.betweenClosed(target.offset(-2,-3,-2),target.offset(2,1,2)))
            if (!stand.equals(v.blockPosition()) && ForestryWork.standFailure(level,v,s,target,stand)==null
                    && ForestryWork.standEye(v,stand).distanceToSqr(Vec3.atCenterOf(target))<=6.25) stands.add(stand.immutable());
        stands.sort(Comparator.comparingDouble(p -> p.distSqr(v.blockPosition())));
        for (BlockPos stand : stands.stream().limit(3).toList()) {
            var path = WorkNavigation.pathTo(level,v,s,stand,0);
            if (WorkNavigation.staysHome(level,v,s,path)) return path;
        }
        return null;
    }
    private WoodBootstrap() {}
}
