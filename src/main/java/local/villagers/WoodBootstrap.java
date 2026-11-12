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
    enum Goal { TABLE, HOE, BOAT, PICKAXE, STONE_PICKAXE, REPLACEMENT_PICKAXE, IRON_PICKAXE, REPLACEMENT_IRON_PICKAXE, DIAMOND_PICKAXE }
    static final class State {
        BlockPos tree, working;
        Block treeBlock;
        int taken;
        long nextWork, nextPath, lastWork, started, nextSearch, recipes, logs;
        ItemStack tool = ItemStack.EMPTY;
        String status = "no preparation job";
        String lastPath = "none";
        String craftFailure = "none";
        long pathQueries;
        Goal goal;
        BlockPos survey;long surveyUntil,nextSurvey;
        Map<String,Object> surveyReceipt=Map.of();
        final ForestryStep.State step = new ForestryStep.State();
    }
    static Map<String,Object> status(Session s) {
        var w = s.woodBootstrap;
        var row = new LinkedHashMap<String,Object>(Map.of("activity", w.status, "goal", w.goal == null ? "NONE" : w.goal.name(), "logs", w.logs, "recipes", w.recipes,
            "pathQueries",w.pathQueries,"lastPath",w.lastPath,"stepsPlaced",w.step.placed,"lastStep",w.step.receipt,
            "survey",Map.of("target",w.survey==null?"":w.survey.toShortString(),"lastRoute",w.surveyReceipt,"canopy",ForestryCanopy.status(s)),"gates",ForestryGate.status(s)));
        row.put("craftFailure",w.craftFailure);return row;
    }
    static boolean complete(ServerLevel level, Villager v, Session s, Goal goal) {
        return switch (goal) {
            case TABLE -> BreadCrafting.table(level, v, s) != null;
            case HOE -> AdventureActions.hasHoe(v);
            case BOAT -> BoatPreparation.boatSlot(v)>=0;
            case PICKAXE -> AdventurePreparation.pickaxe(v);
            case STONE_PICKAXE -> AdventurePreparation.stonePickaxe(v);
            case REPLACEMENT_PICKAXE -> AdventurePreparation.reservePickaxe(v);
            case IRON_PICKAXE -> AdventurePreparation.ironPickaxe(v);
            case REPLACEMENT_IRON_PICKAXE -> AdventurePreparation.reserveIronPickaxe(v);
            case DIAMOND_PICKAXE -> AdventurePreparation.diamondPickaxe(v);
        };
    }
    static boolean safe(ServerLevel level, Villager v, Session s) {
        return Villagers.isManaged(v) && v.level() == level && !v.isPassenger() && !v.isTrading() && !v.isSleeping()
            && !v.isOnFire() && !v.isInWater() && v.getAirSupply() >= 200
            && FoodSupply.mayWork(v.getAge(), v.getHealth(), s.mind.fatigue(), !VillageSurvival.scheduledRest(level, v));
    }
    static String prepareBeddingWood(ServerLevel level,Villager v,Session s,long now){
        if(!safe(level,v,s)||!VillageBedding.hasBeddingFiber(v.getInventory()))return null;
        s.woodBootstrap.goal=Goal.TABLE;
        return gather(level,v,s,now);
    }
    static String prepareUpgradeWood(ServerLevel level,Villager v,Session s,long now){
        if(!AdventurePreparation.nominated(v)||!safe(level,v,s)||v.getInventory().countItem(Items.STICK)>=2)return null;
        int units=0;
        for(int i=0;i<v.getInventory().getContainerSize();i++){
            var stack=v.getInventory().getItem(i);if(WoodCrafting.raw(stack))units+=4*stack.getCount();else if(WoodCrafting.plank(stack))units+=stack.getCount();
        }
        if(units>=2)return null;
        s.woodBootstrap.goal=Goal.STONE_PICKAXE;return gather(level,v,s,now);
    }
    static String work(ServerLevel level, Villager v, Session s, Goal goal, long now) {
        var w = s.woodBootstrap;
        if(goal==Goal.BOAT&&!AdventureActions.PERMISSIONS.allows(Capability.CRAFT_BOAT))return null;
        if (!safe(level, v, s) || goal!=Goal.TABLE&&goal!=Goal.HOE&&goal!=Goal.BOAT&&!AdventurePreparation.nominated(v)) {
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
            case BOAT -> WoodCrafting.boat(v.getInventory());
            case PICKAXE -> WoodCrafting.pickaxe(v.getInventory());
            case STONE_PICKAXE,REPLACEMENT_PICKAXE -> WoodCrafting.stonePickaxe(v.getInventory());
            case IRON_PICKAXE,REPLACEMENT_IRON_PICKAXE -> WoodCrafting.ironPickaxe(v.getInventory());
            case DIAMOND_PICKAXE -> WoodCrafting.diamondPickaxe(v.getInventory());
        };
        if (plan == null) return gather(level, v, s, now);
        boolean needsTable = toolRecipe(plan.step());
        if (needsTable && !BreadCrafting.canWorkAt(level, v, s, table)) {
            w.started = 0; BreadCrafting.approach(level, v, s, table, now);
            return w.status = "taking owned wood to a workbench";
        }
        v.getNavigation().stop();
        if (craft(level, v, s, plan, table)) {
            w.nextWork = now + 30; return w.status = "crafting " + goal.name().toLowerCase() + " supplies";
        }
        w.nextWork = now + 100; return w.status = "preparation recipe blocked: " + w.craftFailure;
    }
    static boolean compactMiningWood(ServerLevel level,Villager v,Session s){
        var goal=s.woodBootstrap.goal;
        if(!AdventurePreparation.nominated(v)||!AdventurePreparation.stonePickaxe(v)
            ||goal==Goal.BOAT||goal==Goal.HOE||goal==Goal.TABLE||!safe(level,v,s))return false;
        var plan=WoodCrafting.compactPlanks(v.getInventory());if(plan==null)return false;
        var recipe=level.recipeAccess().getRecipeFor(RecipeType.CRAFTING,plan.input(),level);
        if(recipe.isEmpty())return false;
        var output=recipe.get().value().assemble(plan.input());
        // Preflight the actual native output too, so a pack recipe with custom
        // components cannot recurse into the same inventory-space operation.
        var capacity=new SimpleContainer(v.getInventory().getContainerSize());
        for(int slot=0;slot<capacity.getContainerSize();slot++)capacity.setItem(slot,plan.remaining().getItem(slot).copy());
        return capacity.addItem(output.copy()).isEmpty()&&craft(level,v,s,plan,null);
    }
    private static boolean craft(ServerLevel level, Villager v, Session s, WoodCrafting.Plan plan, BlockPos table) {
        Capability permission = switch (plan.step()) {
            case HOE -> Capability.CRAFT_WOODEN_HOE;
            case PICKAXE -> Capability.CRAFT_WOODEN_PICKAXE;
            case STONE_PICKAXE -> Capability.CRAFT_STONE_PICKAXE;
            case IRON_PICKAXE -> Capability.CRAFT_IRON_PICKAXE;
            case DIAMOND_PICKAXE -> Capability.CRAFT_DIAMOND_PICKAXE;
            case BOAT -> Capability.CRAFT_BOAT;
            default -> Capability.BUILD_CRAFTING_TABLE;
        };
        if (!AdventureActions.PERMISSIONS.allows(permission) || !safe(level, v, s)) return craftFailed(s,"permission or work safety");
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, plan.input(), level);
        if (recipe.isEmpty()) return craftFailed(s,"no native recipe for " + plan.step());
        if(recipe.get().value().getRemainingItems(plan.input()).stream().anyMatch(a -> !a.isEmpty())) return craftFailed(s,"native recipe has remaining items for " + plan.step());
        if (toolRecipe(plan.step())&&!BreadCrafting.canWorkAt(level, v, s, table)) return craftFailed(s,"workbench contact for " + plan.step());
        var output=recipe.get().value().assemble(plan.input());var capacity=new SimpleContainer(v.getInventory().getContainerSize());
        for(int i=0;i<capacity.getContainerSize();i++)capacity.setItem(i,plan.remaining().getItem(i).copy());
        if(!capacity.addItem(output.copy()).isEmpty()){
            NativeWorkInventory.makeRoom(level,v,s);return craftFailed(s,"bag capacity for " + plan.step() + " output " + output);
        }
        if (!WoodCrafting.commit(v.getInventory(), plan, output)) return craftFailed(s,"stale ingredients or unexpected native output for " + plan.step() + ": expected " + plan.count() + " " + plan.output() + ", got " + output);
        s.woodBootstrap.craftFailure="none";
        s.woodBootstrap.recipes++;
        if (plan.step() == WoodCrafting.Step.HOE) { s.hoesCrafted++; AdventureActions.prepareHoeForWork(level, v); }
        if(s.woodBootstrap.goal==Goal.BOAT)s.boatRecipes++;
        else if (s.woodBootstrap.goal!=Goal.TABLE&&s.woodBootstrap.goal!=Goal.HOE) s.adventureCrafts++;
        s.mind.remember("Prepared " + plan.count() + " " + plan.output() + " from possessed materials using a native recipe");
        Villagers.persist(s); return true;
    }
    private static boolean craftFailed(Session s,String reason){s.woodBootstrap.craftFailure=reason;return false;}
    private static boolean toolRecipe(WoodCrafting.Step step){return step==WoodCrafting.Step.HOE||step==WoodCrafting.Step.BOAT||step==WoodCrafting.Step.PICKAXE||step==WoodCrafting.Step.STONE_PICKAXE||step==WoodCrafting.Step.IRON_PICKAXE||step==WoodCrafting.Step.DIAMOND_PICKAXE;}
    /** An actual remembered public destination can provide another view of the
     * village. It supplies no tree locations and extends no work permissions. */
    private static net.minecraft.world.level.pathfinder.Path surveyPath(ServerLevel level,Villager v,Session s,BlockPos bell){
        var path=NativePathLimits.search(v,1,()->WorkNavigation.pathTo(level,v,s,bell,2));
        if(WorkNavigation.staysHome(level,v,s,path)&&BoatTravel.dryPath(level,path))return path;
        try{
            v.getNavigation().setMaxVisitedNodesMultiplier(32);
            path=NativePathLimits.search(v,1,()->v.getNavigation().createPath(bell,2,128));
        }finally{v.getNavigation().resetMaxVisitedNodesMultiplier();}
        return WorkNavigation.staysHome(level,v,s,path)&&BoatTravel.dryPath(level,path)?path:null;
    }
    private static String surveyVillage(ServerLevel level,Villager v,Session s,long now){
        var w=s.woodBootstrap;if(!NativeRegionsScenario.nativeRegions(level)||now<w.nextSurvey)return null;
        var meeting=v.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.MEETING_POINT);
        if(meeting.isEmpty()||!meeting.get().dimension().equals(level.dimension()))return null;
        var bell=meeting.get().pos();w.nextSurvey=now+2400;
        if(!level.isLoaded(bell)||!level.getBlockState(bell).is(net.minecraft.world.level.block.Blocks.BELL)
            ||!VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(bell))
            ||v.position().distanceToSqr(Vec3.atCenterOf(bell))<=9)return null;
        // The caller already admitted its bounded tree-survey batch.
        var path=surveyPath(level,v,s,bell);
        if(!WorkNavigation.staysHome(level,v,s,path)||!BoatTravel.dryPath(level,path))return null;
        if(!v.getNavigation().moveTo(path,.6))return null;
        w.survey=bell;w.surveyUntil=now+1200;w.nextPath=now+80;
        w.surveyReceipt=Map.of("gameTime",now,"actor",v.position().toString(),"rememberedBell",bell.toShortString(),"nodes",path.getNodeCount(),"end",path.getEndNode().asBlockPos().toShortString());
        return "walking to the remembered village centre to look for wood";
    }
    private static String gather(ServerLevel level, Villager v, Session s, long now) {
        var w = s.woodBootstrap;
        if(w.goal!=Goal.TABLE&&w.goal!=Goal.HOE&&w.goal!=Goal.BOAT&&AdventurePreparation.nominated(v)&&v.getInventory().countItem(Items.STICK)<2){
            var supplies=AdventureSupplies.work(level,v,s,now);if(supplies!=null)return w.status=supplies;
        }
        if (!AdventureActions.PERMISSIONS.allows(Capability.FORESTRY)) return null;
        var stepping=ForestryStep.work(level,v,s,now);
        if(stepping!=null)return w.status=stepping;
        var litter=LooseWood.collect(level,v,s,now);
        if(litter!=null)return w.status=litter;
        var canopy=ForestryCanopy.work(level,v,s,now);if(canopy!=null)return w.status=canopy;
        var gate=ForestryGate.work(level,v,s,now);if(gate!=null)return w.status=gate;
        if(w.survey!=null){
            if(now>=w.surveyUntil){w.survey=null;w.nextSearch=0;}
            else if(v.position().distanceToSqr(Vec3.atCenterOf(w.survey))<=9){
                w.survey=null;w.nextSearch=0;return w.status="observing preparation wood from the remembered village centre";
            }else{
                var current=v.getNavigation().getPath();
                if(current!=null&&!current.isDone()&&current.getTarget().equals(w.survey)&&BoatTravel.dryPath(level,current))
                    return w.status="walking to the remembered village centre to look for wood";
                if(now>=w.nextPath&&WorkNavigation.admit(level,now,v)){
                    w.nextPath=now+80;var target=w.survey;
                    var path=surveyPath(level,v,s,target);
                    if(WorkNavigation.staysHome(level,v,s,path)&&BoatTravel.dryPath(level,path))v.getNavigation().moveTo(path,.6);
                    else{w.survey=null;w.nextSearch=now+200;return null;}
                }
                return w.status="walking to the remembered village centre to look for wood";
            }
        }
        if (w.tree != null && (w.taken >= 4 || !level.isLoaded(w.tree.above(w.taken))
                || !level.getBlockState(w.tree.above(w.taken)).is(w.treeBlock)
                || !TreeClaims.claim(level,w.tree,v.getUUID(),now))) { w.tree = null; w.started = 0; }
        if (w.tree == null) {
            if (now < w.nextSearch) return null;
            if (!WorkNavigation.admit(level, now, v)) return w.status = "looking for preparation wood";
            var origin = v.blockPosition(); var trees = new ArrayList<BlockPos>();
            for (BlockPos p : BlockPos.betweenClosed(origin.offset(-28,-3,-28), origin.offset(28,6,28)))
                if (TreeClaims.available(level,p,v.getUUID(),now) && BoatPreparation.naturalTrunk(level, p)
                        && woodInWorkArea(level,s,p)) trees.add(p.immutable());
            trees.sort(Comparator.comparingDouble(p -> p.distSqr(origin)));
            if (trees.isEmpty()) {
                w.nextSearch=now+200;var survey=surveyVillage(level,v,s,now);if(survey!=null)return w.status=survey;
                w.status="needs accessible natural wood";return null;
            }
            for(var tree:trees.stream().limit(4).toList()){
                if(BlockContact.point(level,v,v.getEyePosition(),tree,3)!=null){w.tree=tree;break;}
                var path=approachPath(level,v,s,tree);
                if(path!=null&&(!NativeRegionsScenario.nativeRegions(level)||BoatTravel.dryPath(level,path))){w.tree=tree;v.getNavigation().moveTo(path,.6);w.nextPath=now+40;break;}
            }
            if(w.tree==null){
                // A closed gate can make every stand beneath the crown look
                // unreachable. Use its existing entrance before cutting a
                // corridor around the outside of an intact animal pen.
                for(var tree:trees.stream().limit(4).toList())
                    if(ForestryGate.plan(level,v,s,tree,now))return w.status="approaching the real village gate beside preparation wood";
                for(var tree:trees.stream().limit(4).toList())
                    if(ForestryStep.plan(level,v,s,tree,now))return w.status="carrying dirt to a safe forestry step";
                for(var tree:trees.stream().limit(4).toList())
                    if(ForestryCanopy.plan(level,v,s,tree,now))return w.status="planning real headroom beneath a low natural canopy";
                var survey=surveyVillage(level,v,s,now);if(survey!=null)return w.status=survey;
                w.nextSearch=now+200;w.status="needs a dry route to natural preparation wood";return null;
            }
            if(!TreeClaims.claim(level,w.tree,v.getUUID(),now)){w.tree=null;return null;}
            w.treeBlock = level.getBlockState(w.tree).getBlock(); w.taken = 0;
        }
        var p = w.tree.above(w.taken); var block = level.getBlockState(p);
        if (ConstructionSites.isProtected(level,p) || !woodInWorkArea(level,s,p)) {
            w.tree = null; w.started = 0; w.nextSearch = now + 200; return null;
        }
        if (BlockContact.point(level,v,v.getEyePosition(),p,3)==null) {
            w.started = 0;
            if (now >= w.nextPath && WorkNavigation.admit(level,now,v)) {
                w.nextPath = now + 40;
                var path = approachPath(level,v,s,p);
                w.pathQueries++;
                w.lastPath = path == null ? "none to " + p : "reach="+path.canReach()+" nodes="+path.getNodeCount()+" target="+p+" end="+path.getNodePos(path.getNodeCount()-1);
                if (WorkNavigation.staysHome(level,v,s,path)&&(!NativeRegionsScenario.nativeRegions(level)||BoatTravel.dryPath(level,path))) w.lastPath += " moved="+v.getNavigation().moveTo(path,.6);
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
        if(drops.isEmpty())return w.status="native preparation log has no harvestable drops";
        if (drops.stream().anyMatch(a -> !next.addItem(a.copy()).isEmpty())) {
            var room=NativeWorkInventory.makeRoom(level,v,s);if(room!=null)return w.status=room;
            w.nextWork = now + 100; return w.status = "preparation wood will not fit";
        }
        if (!level.getBlockState(p).equals(block) || !ItemStack.matches(w.tool,v.getMainHandItem()) || !level.destroyBlock(p,false,v)) return null;
        for (int i=0;i<next.getContainerSize();i++) v.getInventory().setItem(i,next.getItem(i));
        var tool = v.getMainHandItem().get(DataComponents.TOOL);
        if (tool != null && block.getDestroySpeed(level,p) > 0) v.getMainHandItem().hurtAndBreak(tool.damagePerBlock(),v,EquipmentSlot.MAINHAND);
        w.taken++; w.logs++; w.started = 0; w.nextWork = now + 10;
        if(w.goal==Goal.BOAT)s.boatLogsGathered++;
        s.mind.remember("Gathered actual natural wood for " + w.goal.name().toLowerCase() + " at " + p.toShortString());
        return w.status = "gathered preparation wood";
    }
    private static boolean woodInWorkArea(ServerLevel level,Session s,BlockPos p){
        return s.woodBootstrap.goal==Goal.BOAT?BoatPreparation.woodInReachOfHome(level,s,p)
            :VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(p));
    }
    /** Solid logs are interaction targets. Retry a few actual stands when the
     * native search budget misses a detour through an opened village gate. */
    static net.minecraft.world.level.pathfinder.Path approachPath(ServerLevel level,Villager v,Session s,BlockPos target) {
        var stands = new ArrayList<BlockPos>();
        for (BlockPos stand : BlockPos.betweenClosed(target.offset(-2,-4,-2),target.offset(2,1,2)))
            if (!stand.equals(v.blockPosition()) && ForestryWork.standFailure(level,v,s,target,stand)==null
                    && BlockContact.point(level,v,ForestryWork.standEye(v,stand),target,2.5)!=null) stands.add(stand.immutable());
        stands.sort(Comparator.comparingDouble(p -> p.distSqr(v.blockPosition())));
        record Retry(BlockPos stand,double remaining){}
        var retries=new ArrayList<Retry>();
        for (BlockPos stand : stands.stream().limit(8).toList()) {
            var path = NativePathLimits.search(v,1,()->v.getNavigation().createPath(stand,0,64));
            if (WorkNavigation.staysHome(level,v,s,path)) return path;
            retries.add(new Retry(stand,path==null?Double.MAX_VALUE:path.getEndNode().asBlockPos().distSqr(stand)));
        }
        retries.sort(Comparator.comparingDouble(Retry::remaining));
        try{
            v.getNavigation().setMaxVisitedNodesMultiplier(32);
            for(var retry:retries.stream().limit(3).toList()){
                var path=NativePathLimits.search(v,1,()->v.getNavigation().createPath(retry.stand(),0,128));
                if(WorkNavigation.staysHome(level,v,s,path))return path;
            }
        }finally{v.getNavigation().resetMaxVisitedNodesMultiplier();}
        return null;
    }
    private WoodBootstrap() {}
}
