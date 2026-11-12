package local.villagers;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Narrow crafting capability: real wheat, a reachable existing table, and the server's current bread recipe. */
final class BreadCrafting {
    private BreadCrafting() {}
    private static final String WORKBENCH_MEMORY = "villagers:workbench:";
    /** Remember a physically placed workbench; temporary occlusion is not permission to spend another table. */
    static void rememberTable(ServerLevel level,Villager v,Session s,BlockPos p) {
        v.entityTags().stream().filter(t -> t.startsWith(WORKBENCH_MEMORY)).toList().forEach(v::removeTag);
        v.addTag(WORKBENCH_MEMORY+p.getX()+","+p.getY()+","+p.getZ()+","+level.dimension().identifier());
        s.craftingTable=p.immutable();s.nextCraftScan=level.getGameTime()+100;
        s.blockedCraftTable=null;s.blockedCraftUntil=0;
    }
    static BlockPos rememberedTable(ServerLevel level,Villager v,Session s) {
        var p=rememberedMealTable(level,v);
        return p!=null&&candidateHomeRange(level,s,p)?p:null;
    }
    /** Recall an earned station for a bounded meal mission after a save/reload.
     * This does not admit ordinary work outside home: FoodAccess must still
     * survey and execute a physically safe route to this specific station. */
    static BlockPos rememberedMealTable(ServerLevel level,Villager v) {
        for(String tag:v.entityTags())if(tag.startsWith(WORKBENCH_MEMORY)) {
            try {
                var parts=tag.substring(WORKBENCH_MEMORY.length()).split(",",4);
                if(parts.length!=4||!parts[3].equals(level.dimension().identifier().toString()))return null;
                var p=new BlockPos(Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),Integer.parseInt(parts[2]));
                if(level.isLoaded(p)&&level.getBlockState(p).is(Blocks.CRAFTING_TABLE)
                        &&p.distSqr(v.blockPosition())<=64*64)return p;
            } catch(RuntimeException invalid){return null;}
        }
        return null;
    }
    record Reservation(List<ItemStack> original, SimpleContainer remaining, CraftingInput input) {}

    static int wheatCount(Container inventory) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
            if (inventory.getItem(slot).is(Items.WHEAT)) count += inventory.getItem(slot).getCount();
        return count;
    }

    /** Work on detached stacks until the entire recipe and capacity check succeeds. */
    static Reservation reserve(Container inventory) {
        if (wheatCount(inventory) < 3) return null;
        List<ItemStack> original = new ArrayList<>();
        SimpleContainer remaining = new SimpleContainer(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            original.add(inventory.getItem(slot).copy());
            remaining.setItem(slot, inventory.getItem(slot).copy());
        }
        List<ItemStack> ingredients = new ArrayList<>();
        for (int slot = 0; slot < remaining.getContainerSize() && ingredients.size() < 3; slot++) {
            while (remaining.getItem(slot).is(Items.WHEAT) && ingredients.size() < 3)
                ingredients.add(remaining.removeItem(slot, 1));
        }
        return new Reservation(List.copyOf(original), remaining, CraftingInput.of(3, 1, ingredients));
    }

    /** False leaves every live slot untouched, including when the output does not fit. */
    static boolean commit(Container inventory, Reservation reservation, ItemStack output) {
        if (reservation == null || output.isEmpty() || !output.is(Items.BREAD)) return false;
        if (inventory.getContainerSize() != reservation.original().size()) return false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
            if (!ItemStack.matches(inventory.getItem(slot), reservation.original().get(slot))) return false;
        SimpleContainer planned = new SimpleContainer(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
            planned.setItem(slot, reservation.remaining().getItem(slot).copy());
        if (!planned.addItem(output.copy()).isEmpty()) return false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
            inventory.setItem(slot, planned.getItem(slot).copy());
        return true;
    }

    static BlockPos table(ServerLevel level, Villager villager, Session session) {
        long now = level.getGameTime();
        if(session.craftingTable==null)session.craftingTable=rememberedTable(level,villager,session);
        if (session.craftingTable != null && (!level.isLoaded(session.craftingTable)
                || !level.getBlockState(session.craftingTable).is(Blocks.CRAFTING_TABLE)
                || !candidateHomeRange(level, session, session.craftingTable)
                || session.craftingTable.equals(session.blockedCraftTable) && now < session.blockedCraftUntil)) session.craftingTable = null;
        if (now < session.nextCraftScan) return session.craftingTable;
        session.nextCraftScan = now + 100;
        BlockPos origin = villager.blockPosition(), best = session.craftingTable;
        double nearest = best==null?Double.MAX_VALUE:villager.position().distanceToSqr(Vec3.atCenterOf(best));
        int radius=NativeRegionsScenario.nativeRegions(level)?32:16;
        for (BlockPos candidate : BlockPos.betweenClosed(origin.offset(-radius, -3, -radius), origin.offset(radius, 3, radius))) {
            if (!level.isLoaded(candidate) || !level.getBlockState(candidate).is(Blocks.CRAFTING_TABLE)
                    || !candidateHomeRange(level, session, candidate)
                    || candidate.equals(session.blockedCraftTable) && now < session.blockedCraftUntil) continue;
            // Nearby public workbenches may be behind a doorway. Discover the station here;
            // approach() and canWorkAt() still require physical reach and unobstructed interaction.
            double distance = villager.position().distanceToSqr(Vec3.atCenterOf(candidate));
            if (distance < nearest) { best = candidate.immutable(); nearest = distance; }
        }
        session.craftingTable = best;
        return best;
    }

    private static boolean candidateHomeRange(ServerLevel level, Session s, BlockPos table) {
        var home = s.mind.home();
        return NativeVillageWorkArea.contains(level,s.mind,Vec3.atCenterOf(table))||FoodAccess.within(level,s.mind,Vec3.atCenterOf(table))||home == null || !home.dimension().equals(level.dimension().identifier().toString())
                || local.villagers.survival.SurvivalPolicy.withinHomeInteractionRange(
                Vec3.atCenterOf(new BlockPos(home.x(), home.y(), home.z())).distanceTo(Vec3.atCenterOf(table)), 3);
    }

    private static boolean visible(ServerLevel level, Villager v, Vec3 eye, BlockPos table) {
        var hit = level.clip(new ClipContext(eye, Vec3.atCenterOf(table), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, v));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(table);
    }

    /** Up to75 nearby stand candidates and3 actual paths per admitted planning batch. */
    static boolean approach(ServerLevel level, Villager v, Session s, BlockPos table, long now) {
        var current=v.getNavigation().getPath();
        if(now<s.craftApproachUntil&&s.craftingStand!=null&&current!=null&&!current.isDone()
            &&current.getTarget().equals(s.craftingStand)&&WorkNavigation.staysHome(level,v,s,current))return true;
        // Waiting for the shared planning queue is not a failed physical
        // trip. Start/sample its stall timer only on admitted attempts.
        if (now < s.nextThink || !WorkNavigation.admit(level, now, v)) return false;
        if (s.craftingProgress.stalled(table.toShortString(), now, v.getX(), v.getY(), v.getZ())) {
            block(s, table, now); v.getNavigation().stop(); return false;
        }
        s.nextThink = now + 40;
        List<BlockPos> stands = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(table.offset(-2, -1, -2), table.offset(2, 1, 2))) {
            if (!level.isLoaded(pos) || !level.isLoaded(pos.above()) || !level.isLoaded(pos.below())
                    || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()
                    || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) continue;
            Vec3 feet = new Vec3(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
            Vec3 eye = feet.add(0, v.getEyePosition().y - v.getY(), 0);
            if (VillageSurvival.withinHomeRange(level, s.mind, feet) && eye.distanceToSqr(Vec3.atCenterOf(table)) <= 9
                    && visible(level, v, eye, table)) stands.add(pos.immutable());
        }
        stands.sort(Comparator.comparingDouble(p -> v.position().distanceToSqr(new Vec3(p.getX() + .5, p.getY(), p.getZ() + .5))));
        var retries=new ArrayList<BlockPos>();
        for (BlockPos stand : stands.stream().limit(3).toList()) {
            var path = mealPath(level,v,s,stand,64,2);
            if (WorkNavigation.staysHome(level, v, s, path)&&(!NativeRegionsScenario.nativeRegions(level)||BoatTravel.dryPath(level,path))) {
                boolean moved=v.getNavigation().moveTo(path,.65);
                if(moved){s.craftingStand=stand;s.craftApproachUntil=now+600;}return moved;
            }
            if(NativeRegionsScenario.nativeRegions(level))retries.add(stand);
        }
        // Hillside villages can require a long stair detour to a nearby table.
        // Retry the same observed stands with a bounded native search; do not
        // let a short incomplete route make the owner abandon usable grain.
        if(!retries.isEmpty())try{
            v.getNavigation().setMaxVisitedNodesMultiplier(32);
            for(var stand:retries){
                var path=mealPath(level,v,s,stand,128,1);
                if(WorkNavigation.staysHome(level,v,s,path)&&BoatTravel.dryPath(level,path)&&v.getNavigation().moveTo(path,.65)){
                    s.craftingStand=stand;s.craftApproachUntil=now+600;return true;
                }
            }
        }finally{
            v.getNavigation().resetMaxVisitedNodesMultiplier();
        }
        block(s, table, now);return false;
    }

    private static net.minecraft.world.level.pathfinder.Path mealPath(ServerLevel level,Villager v,Session s,BlockPos stand,int range,int fall){
        if(!NativeRegionsScenario.nativeRegions(level))return WorkNavigation.pathTo(level,v,s,stand,0);
        // A wet shortest route followed by rejection never discovers a dry
        // detour. Exclude the same wet cells during the native search, while
        // retaining any existing route predicate and all final safety checks.
        return NativePathLimits.search(v,fall,()->NativePathLimits.alsoAvoiding(v,
            p->!level.isLoaded(p)||!level.getFluidState(p).isEmpty()||!level.getFluidState(p.below()).isEmpty(),
            ()->v.getNavigation().createPath(stand,0,range)));
    }

    private static void block(Session s, BlockPos table, long now) {
        s.blockedCraftTable = table; s.blockedCraftUntil = now + 200;
        s.craftingTable = null; s.nextCraftScan = now; s.craftingProgress.reset();
        s.craftingStatus = "crafting table unreachable";
        s.craftingStand=null;s.craftApproachUntil=0;
    }

    static boolean canWorkAt(ServerLevel level, Villager villager, Session session, BlockPos table) {
        if (!Villagers.isManaged(villager)
                || !villager.isAlive() || villager.isSleeping() || villager.isTrading() || villager.level() != level
                || table == null || !level.isLoaded(table) || !level.getBlockState(table).is(Blocks.CRAFTING_TABLE)
                || !VillageSurvival.withinHomeRange(level, session.mind, villager.position())
                || villager.getEyePosition().distanceToSqr(Vec3.atCenterOf(table)) > 9) return false;
        var sight = level.clip(new ClipContext(villager.getEyePosition(), Vec3.atCenterOf(table),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, villager));
        return sight.getType() == HitResult.Type.MISS || sight.getBlockPos().equals(table);
    }

    static boolean tryCraft(ServerLevel level, Villager villager, Session session, BlockPos table) {
        if (!AdventureActions.PERMISSIONS.allows(Capability.CRAFT_BREAD)
                || !canWorkAt(level, villager, session, table)) return false;
        Reservation reserved = reserve(villager.getInventory());
        if (reserved == null) return false;
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, reserved.input(), level);
        if (recipe.isEmpty()) { session.craftingStatus = "bread recipe unavailable"; return false; }
        var rule = recipe.get().value();
        ItemStack output = rule.assemble(reserved.input());
        // This capability only supports the wheat-to-bread recipe, without byproducts.
        if (rule.getRemainingItems(reserved.input()).stream().anyMatch(stack -> !stack.isEmpty())) {
            session.craftingStatus = "unsupported bread byproducts"; return false;
        }
        if (!level.getBlockState(table).is(Blocks.CRAFTING_TABLE) || !commit(villager.getInventory(), reserved, output)) {
            session.craftingStatus = "bread output blocked"; return false;
        }
        session.breadCrafted += output.getCount();
        WorkLife.produced(level,villager,session,local.villagers.work.WorkOrders.Task.BREAD,table);
        session.craftingStatus = "crafted bread";
        session.mind.remember("Crafted " + output.getCount() + " bread from 3 wheat at " + table.toShortString());
        Villagers.persist(session);
        return true;
    }
}
