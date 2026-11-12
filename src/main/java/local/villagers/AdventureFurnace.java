package local.villagers;

import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.phys.*;

/** Owned furnace, real input/fuel/output slots, and the ordinary native cooking ticker. */
final class AdventureFurnace {
    private static final String TAG="villagers:owned_furnace:";
    private static final String FUEL="villagers:stored_furnace_fuel:";
    static void load(ServerLevel level,Villager v,AdventureIron.State w){
        if(w.loaded)return;w.loaded=true;
        for(var tag:v.entityTags())if(tag.startsWith(TAG))try{
            var a=tag.substring(TAG.length()).split(":");if(a.length!=3)continue;
            w.furnace=BlockPos.of(Long.parseLong(a[0]));w.loadedOre=Math.max(0,Long.parseLong(a[1]));w.takenIron=Math.max(0,Long.parseLong(a[2]));
        }catch(NumberFormatException ignored){}
        w.fuelStored=w.furnace!=null&&v.entityTags().contains(FUEL+w.furnace.asLong());
    }
    private static void save(Villager v,AdventureIron.State w){
        v.entityTags().stream().filter(t->t.startsWith(TAG)).toList().forEach(v::removeTag);
        if(w.furnace!=null)v.addTag(TAG+w.furnace.asLong()+":"+w.loadedOre+":"+w.takenIron);
        v.entityTags().stream().filter(t->t.startsWith(FUEL)).toList().forEach(v::removeTag);
        if(w.fuelStored&&w.furnace!=null)v.addTag(FUEL+w.furnace.asLong());
    }
    static String work(ServerLevel level,Villager v,Session s,long now){
        var w=s.iron;load(level,v,w);var bag=v.getInventory();
        if(!AdventureActions.PERMISSIONS.allows(Capability.OPERATE_FURNACE)||!WoodBootstrap.safe(level,v,s))return null;
        if(w.furnace!=null&&level.isLoaded(w.furnace)&&!level.getBlockState(w.furnace).is(Blocks.FURNACE)){w.furnace=null;w.loadedOre=w.takenIron=0;w.fuelStored=false;save(v,w);}
        if(w.furnace==null){
            if(!bag.hasAnyMatching(a->a.is(Items.FURNACE))){
                var plan=IronCrafting.furnace(bag);if(plan==null)return null;
                if(bag.hasAnyMatching(a->a.is(Items.CRAFTING_TABLE))&&BoatPreparation.placeTable(level,v,s))return "placed an owned workbench for furnace crafting";
                var table=BreadCrafting.table(level,v,s);if(table==null)return "needs a real workbench for the furnace";
                if(!BreadCrafting.canWorkAt(level,v,s,table)){BreadCrafting.approach(level,v,s,table,now);return "taking eight owned stone to a workbench";}
                if(!AdventureActions.PERMISSIONS.allows(Capability.CRAFT_FURNACE)||now<w.nextWork)return "preparing furnace ingredients";
                var recipe=level.recipeAccess().getRecipeFor(RecipeType.CRAFTING,plan.input(),level);
                if(recipe.isPresent()&&recipe.get().value().getRemainingItems(plan.input()).stream().allMatch(ItemStack::isEmpty)
                    &&IronCrafting.commit(bag,plan,recipe.get().value().assemble(plan.input()))){w.crafted++;w.nextWork=now+30;return "crafted a furnace from eight owned stone";}
                return "native furnace recipe or inventory capacity blocked";
            }
            var origin=v.blockPosition();
            for(var p:BlockPos.betweenClosed(origin.offset(-2,0,-2),origin.offset(2,0,2))){
                if(!site(level,v,s,p))continue;
                var hit=level.clip(new net.minecraft.world.level.ClipContext(v.getEyePosition(),Vec3.atBottomCenterOf(p).add(0,-.001,0),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,v));
                if(hit.getType()!=HitResult.Type.BLOCK||!hit.getBlockPos().equals(p.below())||hit.getDirection()!=Direction.UP||v.getEyePosition().distanceToSqr(hit.getLocation())>9)continue;
                for(int slot=0;slot<bag.getContainerSize();slot++)if(bag.getItem(slot).is(Items.FURNACE)){
                    if(!level.setBlock(p,Blocks.FURNACE.defaultBlockState(),Block.UPDATE_ALL))return null;
                    bag.removeItem(slot,1);w.furnace=p.immutable();w.site=null;save(v,w);
                    level.gameEvent(v,net.minecraft.world.level.gameevent.GameEvent.BLOCK_PLACE,p);
                    w.receipt=Map.of("gameTime",now,"action","placed owned furnace","position",p.toShortString());return "placed the actual crafted furnace";
                }
            }
            return approachSite(level,v,s,now);
        }
        if(!(level.getBlockEntity(w.furnace) instanceof FurnaceBlockEntity furnace))return null;
        if(BlockContact.interactionPoint(level,v,v.getEyePosition(),w.furnace,3)==null){
            BreadCrafting.approach(level,v,s,w.furnace,now);return "walking to the owned furnace";
        }
        v.getNavigation().stop();
        boolean actualFuel=furnace.getItem(1).is(Items.COAL)||furnace.getItem(1).is(Items.CHARCOAL)||level.getBlockState(w.furnace).getValue(FurnaceBlock.LIT);
        if(w.fuelStored!=actualFuel){w.fuelStored=actualFuel;save(v,w);}
        var output=furnace.getItem(2);
        if(output.is(Items.IRON_INGOT)&&bag.canAddItem(output.copy())){
            int count=output.getCount();bag.addItem(furnace.removeItem(2,count));w.takenIron+=count;furnace.setChanged();save(v,w);
            w.receipt=Map.of("gameTime",now,"action","collected native furnace output","item","minecraft:iron_ingot","count",count);return "collected iron smelted by the native furnace";
        }
        if((furnace.getItem(0).isEmpty()||furnace.getItem(0).is(Items.RAW_IRON))&&bag.countItem(Items.RAW_IRON)>0){
            int room=Math.max(0,furnace.getMaxStackSize(Items.RAW_IRON.getDefaultInstance())-furnace.getItem(0).getCount());
            for(int slot=0;slot<bag.getContainerSize()&&room>0;slot++)if(bag.getItem(slot).is(Items.RAW_IRON)){
                int n=Math.min(room,bag.getItem(slot).getCount());var taken=bag.removeItem(slot,n);
                if(furnace.getItem(0).isEmpty())furnace.setItem(0,taken);else furnace.getItem(0).grow(n);
                w.loadedOre+=n;room-=n;furnace.setChanged();save(v,w);
                w.receipt=Map.of("gameTime",now,"action","loaded owned raw iron","count",n,"furnace",w.furnace.toShortString());
            }
        }
        // Native furnaces accept paid reserve fuel while the current item is
        // burning. Keeping that coal in an eight-slot bag can block its ore.
        var storedFuel=furnace.getItem(1);
        if((storedFuel.isEmpty()||storedFuel.is(Items.COAL))&&storedFuel.getCount()<furnace.getMaxStackSize(Items.COAL.getDefaultInstance()))
            for(int slot=0;slot<bag.getContainerSize();slot++)if(bag.getItem(slot).is(Items.COAL)){
            var paid=bag.removeItem(slot,1);
            if(storedFuel.isEmpty())furnace.setItem(1,paid);else storedFuel.grow(paid.getCount());
            furnace.setChanged();
            w.fuelStored=true;save(v,w);
            w.receipt=Map.of("gameTime",now,"action","loaded one owned coal","furnace",w.furnace.toShortString());break;
        }
        // A stalled paid ore batch needs another actual fuel item. Yield to
        // the existing supply/mining work instead of waiting forever here.
        if(!furnace.getItem(0).isEmpty()&&furnace.getItem(1).isEmpty()
            &&!level.getBlockState(w.furnace).getValue(FurnaceBlock.LIT)&&bag.countItem(Items.COAL)==0)return null;
        return furnace.getItem(0).isEmpty()?"stored owned fuel for the next real ore load":"waiting for ordinary furnace cooking with owned ore and fuel";
    }
    private static boolean site(ServerLevel level,Villager v,Session s,BlockPos p){
        return StationProduction.safeSite(level,p)&&!ConstructionSites.isProtected(level,p)&&!ConstructionSites.isProtected(level,p.below())
            &&VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(p))&&level.getEntities(null,new AABB(p)).isEmpty();
    }
    /** A crafted item is not a completed station. Find observed clear ground,
     * walk to a supported placement stand, then let the actual UP-face hit
     * and inventory payment above decide whether placement succeeds. */
    private static String approachSite(ServerLevel level,Villager v,Session s,long now){
        var w=s.iron;
        if(w.site!=null&&!site(level,v,s,w.site))w.site=null;
        if(w.site==null){
            if(now<w.nextSiteSearch)return "looking for supported clear ground for the owned furnace";
            w.nextSiteSearch=now+100;var origin=v.blockPosition();var sites=new ArrayList<BlockPos>();
            for(var p:BlockPos.betweenClosed(origin.offset(-8,-2,-8),origin.offset(8,2,8)))
                if(site(level,v,s,p)&&BoatRoutes.visible(level,v,Vec3.atCenterOf(p)))sites.add(p.immutable());
            sites.sort(Comparator.comparingDouble(p->p.distSqr(origin)));
            if(!sites.isEmpty())w.site=sites.getFirst();
        }
        if(w.site==null){
            // A sleeper can wake inside a house with no visible placement
            // ground. The actual remembered workbench is a known destination
            // through its doorway, not permission to see through the wall.
            var table=BreadCrafting.table(level,v,s);
            if(table!=null&&!BreadCrafting.canWorkAt(level,v,s,table)&&BreadCrafting.approach(level,v,s,table,now))
                return "carrying the owned furnace back to the known workbench yard";
            return "needs observed supported ground for the furnace";
        }
        if(now<w.nextSitePath||!WorkNavigation.admit(level,now,v))return "waiting to approach a clear furnace site";
        w.nextSitePath=now+40;var stands=new ArrayList<BlockPos>();var destination=w.site;
        for(var p:BlockPos.betweenClosed(destination.offset(-2,-1,-2),destination.offset(2,1,2))){
            if(!level.isLoaded(p.above())||!level.getBlockState(p).isAir()||!level.getBlockState(p.above()).isAir()
                ||!level.getFluidState(p).isEmpty()||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))continue;
            var feet=Vec3.atBottomCenterOf(p);var body=v.getBoundingBox().move(feet.subtract(v.position()));
            if(body.intersects(new AABB(destination))||!level.noCollision(v,body))continue;
            var eye=ForestryWork.standEye(v,p);
            var hit=level.clip(new net.minecraft.world.level.ClipContext(eye,Vec3.atBottomCenterOf(destination).add(0,-.001,0),
                net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,v));
            if(hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(destination.below())&&hit.getDirection()==Direction.UP&&eye.distanceToSqr(hit.getLocation())<=9)stands.add(p.immutable());
        }
        stands.sort(Comparator.comparingDouble(p->p.distSqr(v.blockPosition())));
        for(var p:stands.stream().limit(3).toList()){
            var path=NativePathLimits.search(v,1,()->WorkNavigation.pathTo(level,v,s,p,0));
            if(path!=null&&BoatTravel.dryPath(level,path)&&v.getNavigation().moveTo(path,.6)){
                w.approach=Map.of("gameTime",now,"site",destination.toShortString(),"stand",p.toShortString(),"nodes",path.getNodeCount());
                return "carrying the crafted furnace to supported clear ground";
            }
        }
        w.site=null;return "no complete dry route to the observed furnace site";
    }
    private AdventureFurnace(){}
}
