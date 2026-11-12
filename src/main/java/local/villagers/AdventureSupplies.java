package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gameevent.GameEvent;

/** Nearby storage in the actor's own native village. Contents are read only at contact. */
final class AdventureSupplies {
    static final class State {
        BlockPos target;long scanAt,until;int inspected,moved;
        final Map<BlockPos,Long> checked=new HashMap<>();
        String activity="not seeking village supplies";
        Map<String,Object> receipt=Map.of();
    }
    static Map<String,Object> status(Session s){var w=s.adventureSupplies;return Map.of("activity",w.activity,"target",w.target==null?"":w.target.toShortString(),"inspected",w.inspected,"moved",w.moved,"lastReceipt",w.receipt);}
    static boolean store(ServerLevel level,Villager v,Session s,BlockPos p){
        if(!inVillage(level,v,s,p))return false;
        var block=level.getBlockState(p);
        return (block.getBlock() instanceof ChestBlock||block.is(Blocks.BARREL))
            &&NativeContainers.accessible(level,p)!=null;
    }
    static boolean inVillage(ServerLevel level,Villager v,Session s,BlockPos p){
        var home=s.mind.home();
        if(home==null||!home.dimension().equals(level.dimension().identifier().toString())||!level.isLoaded(p)
            ||!level.getWorldBorder().isWithinBounds(p)||ConstructionSites.isProtected(level,p))return false;
        var start=level.structureManager().getStructureWithPieceAt(new BlockPos(home.x(),home.y(),home.z()),StructureTags.VILLAGE);
        return start.isValid()&&start.getBoundingBox().isInside(p);
    }
    static int wanted(Villager v,Session s,ItemStack item){
        var bag=v.getInventory();int metal=bag.countItem(Items.IRON_INGOT)+bag.countItem(Items.RAW_IRON)+(int)Math.max(0,s.iron.loadedOre-s.iron.takenIron);
        if(item.is(Items.IRON_INGOT)||item.is(Items.RAW_IRON))return Math.max(0,AdventureIron.metalTarget(v)-metal);
        if(item.is(Items.COAL))return AdventureIron.needsFuel(v,s)?Math.max(0,1-bag.countItem(Items.COAL)):0;
        if(item.is(Items.STICK))return Math.max(0,2-bag.countItem(Items.STICK));
        if(item.is(Items.IRON_PICKAXE)&&!AdventurePreparation.ironPickaxe(v)&&AdventurePreparation.toolUses(item)>8)return 1;
        return 0;
    }
    static String work(ServerLevel level,Villager v,Session s,long now){
        if(!AdventurePreparation.nominated(v)||!NativeRegionsScenario.nativeRegions(level)||!WoodBootstrap.safe(level,v,s)
            ||!AdventureActions.PERMISSIONS.allows(Capability.OPEN_VILLAGE_SUPPLIES))return null;
        var w=s.adventureSupplies;
        if(w.target!=null&&(now>=w.until||!store(level,v,s,w.target))){w.checked.put(w.target,now+2400);w.target=null;}
        if(w.target==null){
            if(now<w.scanAt)return null;w.scanAt=now+100;w.checked.entrySet().removeIf(a->a.getValue()<=now);
            var origin=v.blockPosition();double best=Double.MAX_VALUE;
            for(var p:BlockPos.betweenClosed(origin.offset(-16,-3,-16),origin.offset(16,3,16))){
                if(w.checked.containsKey(p)||!store(level,v,s,p))continue;
                double distance=p.distSqr(origin);if(distance<best){best=distance;w.target=p.immutable();}
            }
            w.until=now+1200;
        }
        if(w.target==null)return null;
        var p=w.target;
        if(BlockContact.interactionPoint(level,v,v.getEyePosition(),p,3)==null){
            if(BreadCrafting.approach(level,v,s,p,now))return w.activity="walking to inspect a real village supply chest";
            return w.activity="waiting for a complete route to the village supply chest";
        }
        var source=NativeContainers.accessible(level,p);if(source==null){w.target=null;return null;}
        w.inspected++;level.gameEvent(v,GameEvent.CONTAINER_OPEN,p);
        for(int slot=0;slot<source.getContainerSize();slot++){
            var item=source.getItem(slot);int count=Math.min(item.getCount(),wanted(v,s,item));if(count<=0)continue;
            if(!v.getInventory().canAddItem(item.copyWithCount(count))){
                var room=NativeWorkInventory.makeRoom(level,v,s);return w.activity=room==null?"village supplies need real bag space":room;
            }
            var material=item.getItem();int before=item.getCount(),owned=v.getInventory().countItem(material);var key=BuiltInRegistries.ITEM.getKey(material).toString();
            var actual=source.removeItem(slot,count);var rest=v.getInventory().addItem(actual);int moved=count-rest.getCount();
            if(!rest.isEmpty()){
                if(source.getItem(slot).isEmpty())source.setItem(slot,rest);else source.getItem(slot).grow(rest.getCount());
            }
            source.setChanged();w.moved+=moved;
            w.receipt=Map.of("gameTime",now,"container",p.toShortString(),"item",key,"sourceBefore",before,"sourceAfter",source.getItem(slot).getCount(),"bagBefore",owned,"bagAfter",v.getInventory().countItem(material),"moved",moved);
            if(moved>0){s.mind.remember("Collected "+moved+" actual "+key+" from my native village store at "+p.toShortString());return w.activity="collected actual native village supplies";}
        }
        w.checked.put(p,now+24000);w.target=null;return null;
    }
    private AdventureSupplies(){}
}
