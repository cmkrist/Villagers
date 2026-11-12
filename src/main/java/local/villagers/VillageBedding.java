package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;

/** Ordinary family preparation using real village supplies, recipes and covered floor space. */
final class VillageBedding {
    static final class State {
        BlockPos store,site;Direction facing;
        long scanAt,pathAt,workAt,storeUntil;
        int transfers,recipes,placed;
        final Map<BlockPos,Long> checked=new HashMap<>();
        String activity="no village bedding work";
        Map<String,Object> receipt=Map.of();
    }
    static Map<String,Object> status(Session s){
        var w=s.villageBedding;
        return Map.of("activity",w.activity,"transfers",w.transfers,"recipes",w.recipes,"placed",w.placed,
            "store",w.store==null?"":w.store.toShortString(),"site",w.site==null?"":w.site.toShortString(),"lastReceipt",w.receipt,"sheepWool",SheepWool.status(s));
    }
    static boolean bed(ItemStack item){return item.getItem() instanceof BlockItem b&&b.getBlock() instanceof BedBlock;}
    static boolean hasBeddingFiber(Container bag){
        if(bag.countItem(Items.STRING)+4*bag.countItem(Items.WOOL.white())>=12)return true;
        for(var wool:Items.WOOL.asList())if(bag.countItem(wool)>=3)return true;
        return false;
    }
    private static int planks(Container bag){
        int n=0;for(int i=0;i<bag.getContainerSize();i++)if(WoodCrafting.plank(bag.getItem(i)))n+=bag.getItem(i).getCount();return n;
    }
    static int wanted(Container bag,ItemStack item){
        if(bag.hasAnyMatching(VillageBedding::bed))return 0;
        if(bed(item))return 1;
        if(item.is(Items.SHEARS))return bag.hasAnyMatching(s->s.is(Items.SHEARS)&&AdventurePreparation.toolUses(s)>0)?0:1;
        if(item.is(Items.IRON_INGOT))return bag.hasAnyMatching(s->s.is(Items.SHEARS)&&AdventurePreparation.toolUses(s)>0)?0:Math.max(0,2-bag.countItem(Items.IRON_INGOT));
        if(item.is(Items.STRING))return Math.max(0,12-bag.countItem(Items.STRING)-4*bag.countItem(Items.WOOL.white()));
        // Keep one colour rather than filling every slot with incomplete beds.
        Item colour=null;int most=0;
        for(var wool:Items.WOOL.asList())if(bag.countItem(wool)>most){most=bag.countItem(wool);colour=wool;}
        for(var wool:Items.WOOL.asList())if(item.is(wool))return colour==null||colour==wool?Math.max(0,3-bag.countItem(wool)):0;
        if(!hasBeddingFiber(bag))return 0;
        if(WoodCrafting.plank(item))return Math.max(0,3-planks(bag));
        if(WoodCrafting.raw(item)){
            for(int i=0;i<bag.getContainerSize();i++)if(WoodCrafting.raw(bag.getItem(i)))return 0;
            return planks(bag)<3?1:0;
        }
        return 0;
    }
    static String work(ServerLevel level,Villager v,Session s,long now){
        if(!NativeRegionsScenario.nativeRegions(level)||!WoodBootstrap.safe(level,v,s)
            ||s.mind.foodLevel()<16||!VillageFamilies.readyToPrepare(level,v,s,now)||!VillageFamilies.hasFamilyFood(v)
            ||!VillageFamilies.nurseries(level,v).isEmpty())return null;
        var w=s.villageBedding;if(now<w.workAt)return null;
        if(v.getInventory().hasAnyMatching(VillageBedding::bed))return place(level,v,s,now);
        if(!hasBeddingFiber(v.getInventory())){
            var wool=SheepWool.work(level,v,s,now);if(wool!=null)return w.activity=wool;
            return collect(level,v,s,now);
        }
        if(!AdventureActions.PERMISSIONS.allows(Capability.CRAFT_BED))return null;
        var plan=BedCrafting.next(v.getInventory());
        if(plan==null)return WoodBootstrap.prepareBeddingWood(level,v,s,now);
        var table=BreadCrafting.table(level,v,s);
        if(plan.table()&&table==null)return WoodBootstrap.work(level,v,s,WoodBootstrap.Goal.TABLE,now);
        if(plan.table()&&!BreadCrafting.canWorkAt(level,v,s,table)){
            BreadCrafting.approach(level,v,s,table,now);return w.activity="carrying owned bedding materials to a workbench";
        }
        var recipe=level.recipeAccess().getRecipeFor(RecipeType.CRAFTING,plan.input(),level);
        if(recipe.isEmpty()||recipe.get().value().getRemainingItems(plan.input()).stream().anyMatch(a->!a.isEmpty()))return null;
        var result=recipe.get().value().assemble(plan.input());
        if(!BedCrafting.commit(v.getInventory(),plan,result)){
            var room=NativeWorkInventory.makeRoom(level,v,s);w.workAt=now+40;return room;
        }
        v.getNavigation().stop();w.recipes++;w.workAt=now+30;
        w.receipt=Map.of("gameTime",now,"action","crafted bedding ingredient","item",BuiltInRegistries.ITEM.getKey(result.getItem()).toString(),"count",result.getCount());
        s.mind.remember("Crafted actual village bedding supplies from my possessed materials");Villagers.persist(s);
        return w.activity="crafting paid village bedding";
    }
    private static String collect(ServerLevel level,Villager v,Session s,long now){
        if(!AdventureActions.PERMISSIONS.allows(Capability.OPEN_VILLAGE_SUPPLIES))return null;
        var w=s.villageBedding;
        if(w.store!=null&&(now>=w.storeUntil||!AdventureSupplies.store(level,v,s,w.store))){w.checked.put(w.store,now+2400);w.store=null;}
        if(w.store==null){
            if(now<w.scanAt)return null;w.scanAt=now+200;w.checked.entrySet().removeIf(e->e.getValue()<=now);
            var origin=v.blockPosition();double best=Double.MAX_VALUE;
            for(var p:BlockPos.betweenClosed(origin.offset(-16,-3,-16),origin.offset(16,3,16))){
                if(w.checked.containsKey(p)||!AdventureSupplies.store(level,v,s,p)||!BoatRoutes.visible(level,v,Vec3.atCenterOf(p)))continue;
                double distance=p.distSqr(origin);if(distance<best){best=distance;w.store=p.immutable();}
            }
            w.storeUntil=now+1200;
        }
        if(w.store==null)return null;
        if(BlockContact.interactionPoint(level,v,v.getEyePosition(),w.store,3)==null){
            if(BreadCrafting.approach(level,v,s,w.store,now))return w.activity="walking to inspect a village bedding store";
            return w.activity="waiting for a complete route to a village bedding store";
        }
        var source=NativeContainers.accessible(level,w.store);if(source==null){w.store=null;return null;}
        level.gameEvent(v,GameEvent.CONTAINER_OPEN,w.store);
        for(int slot=0;slot<source.getContainerSize();slot++){
            var item=source.getItem(slot);int count=Math.min(item.getCount(),wanted(v.getInventory(),item));
            if((item.is(Items.SHEARS)||item.is(Items.IRON_INGOT))&&SheepWool.equippedOrPacked(v))continue;
            int wanted=count;
            while(count>0&&!v.getInventory().canAddItem(item.copyWithCount(count)))count--;
            if(count==0){
                if(wanted>0){
                    // The visible store still has useful bedding. Make actual
                    // working space before marking this store as exhausted.
                    var room=NativeWorkInventory.makeRoom(level,v,s);
                    if(room!=null){w.workAt=now+10;return w.activity=room;}
                }
                continue;
            }
            Item material=item.getItem();int before=item.getCount(),owned=v.getInventory().countItem(material);
            var actual=source.removeItem(slot,count);var overflow=v.getInventory().addItem(actual);
            if(!overflow.isEmpty()){
                if(source.getItem(slot).isEmpty())source.setItem(slot,overflow);else source.getItem(slot).grow(overflow.getCount());
            }
            int moved=count-overflow.getCount();source.setChanged();w.transfers+=moved;w.workAt=now+40;
            w.receipt=Map.of("gameTime",now,"container",w.store.toShortString(),"item",BuiltInRegistries.ITEM.getKey(material).toString(),"sourceBefore",before,
                "sourceAfter",source.getItem(slot).getCount(),"bagBefore",owned,"bagAfter",v.getInventory().countItem(material),"moved",moved);
            return w.activity="collected actual village bedding materials";
        }
        w.checked.put(w.store,now+24000);w.store=null;return null;
    }
    private static boolean site(ServerLevel level,Villager v,Session s,BlockPos foot,Direction facing){
        for(var p:List.of(foot,foot.relative(facing))){
            if(!AdventureSupplies.inVillage(level,v,s,p)||!level.isLoaded(p.above(2))||!level.isLoaded(p.below())
                ||!level.getBlockState(p).isAir()||!level.getBlockState(p.above()).isAir()||!level.getBlockState(p.above(2)).isAir()
                ||!level.getFluidState(p).isEmpty()||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP)
                ||level.canSeeSky(p.above())||!level.getEntities(null,new AABB(p).expandTowards(0,2,0)).isEmpty())return false;
            boolean door=false;
            for(var q:BlockPos.betweenClosed(p.offset(-4,-1,-4),p.offset(4,2,4)))
                if(level.isLoaded(q)&&level.getBlockState(q).is(net.minecraft.tags.BlockTags.WOODEN_DOORS))door=true;
            if(!door)return false;
        }
        return true;
    }
    private static net.minecraft.world.level.pathfinder.Path approach(ServerLevel level,Villager v,Session s,BlockPos foot,Direction facing){
        var stands=new ArrayList<BlockPos>();var head=foot.relative(facing);
        for(var end:List.of(foot,head))for(var side:List.of(facing.getClockWise(),facing.getCounterClockWise())){
            var p=end.relative(side);
            if(!level.isLoaded(p.above())||!level.getBlockState(p).isAir()||!level.getBlockState(p.above()).isAir()
                ||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))continue;
            stands.add(p);
        }
        stands.sort(Comparator.comparingDouble(p->p.distSqr(v.blockPosition())));
        for(var stand:stands){
            var path=NativePathLimits.search(v,1,()->WorkNavigation.pathTo(level,v,s,stand,0));
            if(WorkNavigation.staysHome(level,v,s,path)&&BoatTravel.dryPath(level,path))return path;
        }
        return null;
    }
    private static String place(ServerLevel level,Villager v,Session s,long now){
        if(!AdventureActions.PERMISSIONS.allows(Capability.PLACE_VILLAGE_BED))return null;
        var w=s.villageBedding;
        if(w.site!=null&&!site(level,v,s,w.site,w.facing))w.site=null;
        if(w.site==null){
            if(now<w.scanAt||!WorkNavigation.admit(level,now,v))return null;w.scanAt=now+200;
            var origin=v.blockPosition();var candidates=new ArrayList<BlockPos>();
            for(var p:BlockPos.betweenClosed(origin.offset(-8,-1,-8),origin.offset(8,1,8)))candidates.add(p.immutable());
            candidates.sort(Comparator.comparingDouble(p->p.distSqr(origin)));
            int routes=0;
            outer:for(var p:candidates)for(var d:Direction.Plane.HORIZONTAL)if(site(level,v,s,p,d)){
                if(routes++>=6)break outer;
                var path=approach(level,v,s,p,d);
                if(path!=null){w.site=p;w.facing=d;v.getNavigation().moveTo(path,.6);break outer;}
            }
        }
        if(w.site==null)return null;
        var foot=w.site;var head=foot.relative(w.facing);
        if(BlockContact.interactionPoint(level,v,v.getEyePosition(),foot.below(),3)==null
            ||BlockContact.interactionPoint(level,v,v.getEyePosition(),head.below(),3)==null){
            if(now>=w.pathAt&&WorkNavigation.admit(level,now,v)){
                w.pathAt=now+40;var path=approach(level,v,s,foot,w.facing);if(path!=null)v.getNavigation().moveTo(path,.6);
            }
            return w.activity="carrying a paid bed to covered village floor space";
        }
        var bag=v.getInventory();int slot=-1;
        for(int i=0;i<bag.getContainerSize();i++)if(bed(bag.getItem(i))){slot=i;break;}
        if(slot<0||!site(level,v,s,foot,w.facing))return null;
        var block=((BlockItem)bag.getItem(slot).getItem()).getBlock();
        var lower=block.defaultBlockState().setValue(BedBlock.FACING,w.facing).setValue(BedBlock.PART,BedPart.FOOT);
        var upper=lower.setValue(BedBlock.PART,BedPart.HEAD);
        if(!level.setBlock(foot,lower,Block.UPDATE_CLIENTS))return null;
        block.setPlacedBy(level,foot,lower,v,bag.getItem(slot));
        if(!level.getBlockState(foot).equals(lower)||!level.getBlockState(head).equals(upper)||!VillageFamilies.shelteredBed(level,head)){
            if(level.getBlockState(foot).equals(lower))level.removeBlock(foot,false);
            if(level.getBlockState(head).equals(upper))level.removeBlock(head,false);return null;
        }
        int before=bag.getItem(slot).getCount();bag.removeItem(slot,1);w.placed++;w.site=null;w.workAt=now+40;
        w.receipt=Map.of("gameTime",now,"foot",foot.toShortString(),"head",head.toShortString(),"bedItemsBefore",before,"bedItemsAfter",before-1);
        v.addTag("villagers:village_bed:"+head.asLong());level.updateNeighborsAt(foot,block);level.updateNeighborsAt(head,block);
        level.gameEvent(v,GameEvent.BLOCK_PLACE,foot);s.mind.exert(.005f);Villagers.persist(s);
        s.mind.remember("Placed one actual paid sheltered nursery bed at "+head.toShortString());
        return w.activity="placed real sheltered village bedding";
    }
    private VillageBedding(){}
}
