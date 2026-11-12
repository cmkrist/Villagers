package local.villagers;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** The earned furnace/iron stage after the original stone-pickaxe milestone. */
final class AdventureIron {
    static final class State {
        BlockPos furnace,site;boolean loaded,fuelStored;long nextWork,crafted,loadedOre,takenIron,nextSiteSearch,nextSitePath;
        String activity="not preparing iron";
        Map<String,Object> receipt=Map.of(),approach=Map.of();
    }
    static Map<String,Object> status(Session s){
        var w=s.iron;var result=new java.util.LinkedHashMap<String,Object>(Map.of("activity",w.activity,"furnace",w.furnace==null?"":w.furnace.toShortString(),"crafted",w.crafted,"loadedOre",w.loadedOre,"ironTaken",w.takenIron,"lastReceipt",w.receipt,"passage",MineStair.status(s),"plannedSite",w.site==null?"":w.site.toShortString(),"lastApproach",w.approach));
        result.put("fuelStored",w.fuelStored);return result;
    }
    static boolean ore(BlockState block){return diamondOre(block)||block.is(Blocks.COAL_ORE)||block.is(Blocks.DEEPSLATE_COAL_ORE)||block.is(Blocks.IRON_ORE)||block.is(Blocks.DEEPSLATE_IRON_ORE);}
    static boolean diamondOre(BlockState block){return block.is(Blocks.DIAMOND_ORE)||block.is(Blocks.DEEPSLATE_DIAMOND_ORE);}
    static boolean seekingDiamonds(Villager v){return AdventurePreparation.reserveIronPickaxe(v)&&AdventureArmor.next(v)==null&&!AdventurePreparation.diamondPickaxe(v);}
    static int metalTarget(Villager v){
        if(!AdventurePreparation.ironPickaxe(v))return 3;
        if(AdventureArmor.next(v)!=null)return AdventureArmor.ironTarget(v);
        return !AdventurePreparation.diamondPickaxe(v)&&!AdventurePreparation.reserveIronPickaxe(v)?3:0;
    }
    static boolean needsFuel(Villager v,Session s){
        return v.getInventory().countItem(Items.IRON_INGOT)<metalTarget(v)
            &&v.getInventory().countItem(Items.COAL)==0&&!s.iron.fuelStored;
    }
    static boolean needsMining(Villager v,Session s){
        if(seekingDiamonds(v))return v.getInventory().countItem(Items.DIAMOND)<3;
        if(!AdventurePreparation.stonePickaxe(v))return WoodCrafting.stoneCount(v.getInventory())<3;
        return s.iron.furnace==null&&WoodCrafting.stoneCount(v.getInventory())<8
            ||v.getInventory().countItem(Items.RAW_IRON)+v.getInventory().countItem(Items.IRON_INGOT)+s.iron.loadedOre-s.iron.takenIron<metalTarget(v)
            ||needsFuel(v,s);
    }
    static boolean wants(Villager v,Session s,BlockState block){
        if(diamondOre(block))return seekingDiamonds(v)&&v.getInventory().countItem(Items.DIAMOND)<3;
        if(!AdventurePreparation.stonePickaxe(v))return block.is(Blocks.STONE)||block.is(Blocks.DEEPSLATE);
        if(block.is(Blocks.COAL_ORE)||block.is(Blocks.DEEPSLATE_COAL_ORE))return needsFuel(v,s);
        if(block.is(Blocks.IRON_ORE)||block.is(Blocks.DEEPSLATE_IRON_ORE))return v.getInventory().countItem(Items.RAW_IRON)+v.getInventory().countItem(Items.IRON_INGOT)+s.iron.loadedOre-s.iron.takenIron<metalTarget(v);
        return (s.iron.furnace==null&&WoodCrafting.stoneCount(v.getInventory())<8
            ||!AdventurePreparation.reservePickaxe(v)&&needsMining(v,s)&&WoodCrafting.stoneCount(v.getInventory())<3)
            &&(block.is(Blocks.STONE)||block.is(Blocks.DEEPSLATE));
    }
    static boolean readyOreBatch(int target,int ingots,int raw,long pending){
        return pending>0||raw>0&&(long)ingots+raw>=target;
    }
    static String work(ServerLevel level,Villager v,Session s,long now){
        var w=s.iron;AdventureFurnace.load(level,v,w);MineStair.resume(v,s);
        if(AdventurePreparation.ironPickaxe(v)){
            if(AdventureArmor.next(v)==null){
                if(AdventurePreparation.diamondPickaxe(v)){w.activity="earned a diamond pickaxe and real iron armor; preparing the next adventure stage";return null;}
                boolean replace=!AdventurePreparation.reserveIronPickaxe(v);
                if(replace&&v.getInventory().countItem(Items.IRON_INGOT)>=3||!replace&&v.getInventory().countItem(Items.DIAMOND)>=3){
                    var exit=MineStair.returnToEntry(level,v,s,now);if(exit!=null)return w.activity=exit;
                    var tool=WoodBootstrap.work(level,v,s,replace?WoodBootstrap.Goal.REPLACEMENT_IRON_PICKAXE:WoodBootstrap.Goal.DIAMOND_PICKAXE,now);
                    w.activity=s.woodBootstrap.status;return tool;
                }
            }
            var armor=AdventureArmor.work(level,v,s,now);if(armor!=null)return w.activity=armor;
        }
        if(!AdventurePreparation.ironPickaxe(v)&&v.getInventory().countItem(Items.IRON_INGOT)>=3){
            var upgrade=WoodBootstrap.work(level,v,s,WoodBootstrap.Goal.IRON_PICKAXE,now);w.activity=s.woodBootstrap.status;return upgrade;
        }
        if(!AdventurePreparation.reservePickaxe(v)&&needsMining(v,s)&&WoodCrafting.stoneCount(v.getInventory())>=3){
            var exit=MineStair.returnToEntry(level,v,s,now);if(exit!=null)return w.activity=exit;
            var replacement=WoodBootstrap.work(level,v,s,WoodBootstrap.Goal.REPLACEMENT_PICKAXE,now);w.activity=s.woodBootstrap.status;return replacement;
        }
        var abandoned=MineStair.leaveExhausted(level,v,s,now);if(abandoned!=null)return w.activity=abandoned;
        // Carry the next piece's actual material budget before returning;
        // one raw iron must not trigger a whole mine/workbench round trip.
        // Already deposited batches are collected first. A full carried batch
        // can also be deposited without fuel to make a real coal-carrying slot.
        int carriedOre=v.getInventory().countItem(Items.RAW_IRON);
        if(w.furnace!=null&&readyOreBatch(metalTarget(v),v.getInventory().countItem(Items.IRON_INGOT),carriedOre,w.loadedOre-w.takenIron)
            &&(w.fuelStored||v.getInventory().countItem(Items.COAL)>0||!needsFuel(v,s)||carriedOre>0)){
            var exit=MineStair.returnToEntry(level,v,s,now);if(exit!=null)return w.activity=exit;
            var batch=AdventureFurnace.work(level,v,s,now);if(batch!=null)return w.activity=batch;
        }
        if(w.furnace!=null&&v.getInventory().countItem(Items.COAL)>0){
            var exit=MineStair.returnToEntry(level,v,s,now);if(exit!=null)return w.activity=exit;
            var fuel=AdventureFurnace.work(level,v,s,now);if(fuel!=null)return w.activity=fuel;
        }
        if(!needsMining(v,s)||w.furnace==null&&WoodCrafting.stoneCount(v.getInventory())>=8||v.getInventory().hasAnyMatching(a->a.is(Items.FURNACE))){
            var exit=MineStair.returnToEntry(level,v,s,now);if(exit!=null)return w.activity=exit;
            var furnace=AdventureFurnace.work(level,v,s,now);if(furnace!=null)return w.activity=furnace;
        }
        var supplies=AdventureSupplies.work(level,v,s,now);if(supplies!=null)return w.activity=supplies;
        var ore=AdventureMining.work(level,v,s,now);if(ore!=null)return w.activity=ore;
        if(!needsMining(v,s))return w.activity="waiting for real furnace processing";
        var recorded=MineStair.revisitRecorded(level,v,s,now);if(recorded!=null)return w.activity=recorded;
        var entry=AdventureQuarry.prospect(level,v,s,now);if(entry!=null)return w.activity=entry;
        if(s.mineStair.root==null&&s.quarry.root==null)return w.activity="seeking an observed dry mineral prospect";
        var passage=MineStair.work(level,v,s,now);return w.activity=passage==null?"needs a safe observed mining face":passage;
    }
    private AdventureIron(){}
}
