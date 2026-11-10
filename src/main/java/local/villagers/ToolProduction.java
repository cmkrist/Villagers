package local.villagers;

import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.crafting.RecipeType;

/** Replace a missing farm tool using possessed wood and a reachable existing table. No supplied outputs. */
final class ToolProduction {
    private ToolProduction() {}
    private static boolean forCustomer(Villager v,Session s,long now) {
        if(s.toolDemand.requester(now)==null)return false;
        return local.villagers.economy.ToolDemand.mayProduce(true,
                MarketGoods.usableHoe(v.getMainHandItem()) || v.getInventory().hasAnyMatching(MarketGoods::usableHoe),
                MarketGoods.offer(v.getInventory(),v.getMainHandItem(),true,MarketGoods.Good.HOE)>=0,
                s.mind.foodLevel(),FoodSupply.deficit(v.getInventory(),AdventureActions.canHarvest(v)))
                && local.villagers.economy.TradePolicy.sellerSafe(s.mind.foodLevel(),v.getHealth(),v.getMaxHealth(),s.mind.fatigue());
    }
    /** A real understood tool request can create input demand; no speculative material hoarding. */
    static boolean needsWood(Villager v,Session s,long now) {
        return forCustomer(v,s,now) && WoodCrafting.next(v.getInventory())==null;
    }
    /** An admitted customer job may use its combined body/food reserve before topping up stored food. */
    static String serveCustomer(ServerLevel level,Villager v,Session s,long now) {
        return forCustomer(v,s,now)?work(level,v,s,now):null;
    }
    static boolean canPrepare(Villager v,Session s) {
        return local.villagers.economy.ToolDemand.mayProduce(true,
                MarketGoods.usableHoe(v.getMainHandItem()) || v.getInventory().hasAnyMatching(MarketGoods::usableHoe),
                MarketGoods.offer(v.getInventory(),v.getMainHandItem(),true,MarketGoods.Good.HOE)>=0,
                s.mind.foodLevel(),FoodSupply.deficit(v.getInventory(),AdventureActions.canHarvest(v)))
                && (WoodCrafting.next(v.getInventory())!=null || v.getInventory().countItem(net.minecraft.world.item.Items.EMERALD)>0
                        || s.forestryJob!=null);
    }
    static void interrupt(Session s) { s.toolProductionStarted = 0; s.toolWorkStarted = 0; s.toolTable = null; }
    private static boolean eligible(ServerLevel level, Villager v, Session s) {
        boolean ownTool=!AdventureActions.hasHoe(v) && v.getInventory().hasAnyMatching(PlantingMaterials::supported);
        return AdventureActions.PERMISSIONS.allows(Capability.CRAFT_WOODEN_HOE) && (ownTool || forCustomer(v,s,level.getGameTime()))
                && FoodSupply.mayWork(v.getAge(), v.getHealth(), s.mind.fatigue(),
                !level.isDarkOutside() && !v.isSleeping() && !v.isTrading() && !v.isInWater()
                        && !v.isOnFire() && v.getAirSupply() >= 200);
    }
    static String work(ServerLevel level, Villager v, Session s, long now) {
        if (!eligible(level, v, s)) {
            interrupt(s); return null;
        }
        if (now < s.nextToolWork) return null;
        if (WoodCrafting.next(v.getInventory()) == null) {
            interrupt(s); s.nextToolWork = now + 100; s.toolStatus = "missing wood for a hoe"; return null;
        }
        BlockPos table = BreadCrafting.table(level, v, s);
        if (table == null) {
            interrupt(s); s.nextToolWork = now + 100; s.toolStatus = "hoe needs an existing crafting table"; return null;
        }
        if (s.toolProductionStarted != 0 && (now < s.lastToolWork || now - s.lastToolWork > 10
                || !table.equals(s.toolTable))) interrupt(s);
        if (s.toolProductionStarted == 0) {
            s.toolProductionStarted = now; s.toolTable = table; s.craftingProgress.reset();
        }
        if (now - s.toolProductionStarted >= 400) {
            interrupt(s); v.getNavigation().stop(); s.nextToolWork = now + 400;
            s.toolStatus = "hoe production route timed out"; return null;
        }
        s.lastToolWork = now;
        s.action = null; s.before = null; s.succeeded = false;
        if (!BreadCrafting.canWorkAt(level, v, s, table)) {
            s.toolWorkStarted = 0;
            BreadCrafting.approach(level, v, s, table, now);
            s.toolStatus = "approaching table to make a hoe"; return s.toolStatus;
        }
        v.getNavigation().stop();
        if (s.toolWorkStarted == 0) s.toolWorkStarted = now;
        if (now - s.toolWorkStarted >= 20) {
            boolean made = craft(level, v, s, table);
            s.toolWorkStarted = 0;
            if (!made) { interrupt(s); s.nextToolWork = now + 200; return null; }
            if (!eligible(level,v,s)) { interrupt(s); s.nextToolWork = now + 100; }
        }
        return s.toolStatus = !eligible(level,v,s) ? "made a real wooden hoe" : "working wood for a hoe";
    }
    static boolean craft(ServerLevel level, Villager v, Session s, BlockPos table) {
        if (!eligible(level, v, s) || !BreadCrafting.canWorkAt(level, v, s, table)) return false;
        var plan = WoodCrafting.next(v.getInventory());
        if (plan == null) return false;
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, plan.input(), level);
        if (recipe.isEmpty()) { s.toolStatus = "wood recipe unavailable"; return false; }
        var rule = recipe.get().value();
        var output = rule.assemble(plan.input());
        boolean customer=AdventureActions.canHarvest(v);
        if (rule.getRemainingItems(plan.input()).stream().anyMatch(stack -> !stack.isEmpty())
                || !eligible(level, v, s) || !BreadCrafting.canWorkAt(level, v, s, table)
                || !WoodCrafting.commit(v.getInventory(), plan, output)) {
            s.toolStatus = "wood recipe or output blocked"; return false;
        }
        s.woodRecipesCrafted++;
        if(customer && FoodSupply.deficit(v.getInventory(),AdventureActions.canHarvest(v))>0)s.toolReserveWork++;
        if (plan.step() == WoodCrafting.Step.HOE) {
            s.hoesCrafted++;
            if(!customer)AdventureActions.prepareHoeForWork(level, v);
        }
        v.swingForAttack(InteractionHand.MAIN_HAND);
        s.mind.remember("Crafted " + output.getCount() + " " + BuiltInRegistries.ITEM.getKey(output.getItem())
                + " from owned wood using native recipe at " + table.toShortString());
        Villagers.persist(s);
        return true;
    }
}
