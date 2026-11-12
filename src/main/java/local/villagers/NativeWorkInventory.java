package local.villagers;

import java.util.Map;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

/** Make real inventory space for work; placed tables and dropped stock remain in the world. */
final class NativeWorkInventory {
    static int offhandBackup(Container bag,ItemStack offhand){
        if(!offhand.isEmpty())return -1;
        for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).is(Items.WOODEN_PICKAXE)&&AdventurePreparation.toolUses(bag.getItem(i))>0)return i;
        return -1;
    }
    static int offhandStoneBackup(Container bag,ItemStack mainhand,ItemStack offhand){
        if(!offhand.is(Items.WOODEN_PICKAXE)||!usableUpgrade(mainhand))return -1;
        for(int slot=0;slot<bag.getContainerSize();slot++){
            var item=bag.getItem(slot);
            if(item.is(Items.STONE_PICKAXE)&&AdventurePreparation.toolUses(item)>8)return slot;
        }
        return -1;
    }
    static boolean decorativeRock(ItemStack item){
        return item.is(Items.ANDESITE)||item.is(Items.DIORITE)||item.is(Items.GRANITE)||item.is(Items.TUFF);
    }
    static int quarryByproduct(Container bag){
        for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).is(Items.CLAY_BALL)||decorativeRock(bag.getItem(i)))return i;
        int dirt=bag.countItem(Items.DIRT);
        for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).is(Items.DIRT)&&dirt-bag.getItem(i).getCount()>=64)return i;
        return -1;
    }
    static int secondaryCrop(Container bag){
        if(bag.countItem(Items.WHEAT_SEEDS)<2)return -1;
        for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).is(Items.BEETROOT_SEEDS))return i;
        for(int i=0;i<bag.getContainerSize();i++){
            var item=bag.getItem(i);
            if(item.getCount()==1&&(item.is(Items.CARROT)||item.is(Items.POTATO)))return i;
        }
        return -1;
    }
    static int supersededPickaxe(Container bag,ItemStack held){
        boolean replacement=usableUpgrade(held);
        for(int i=0;i<bag.getContainerSize()&&!replacement;i++)replacement=usableUpgrade(bag.getItem(i));
        if(!replacement)return -1;
        for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).is(Items.WOODEN_PICKAXE))return i;
        for(int i=0;i<bag.getContainerSize();i++){
            var item=bag.getItem(i);
            if(item.is(Items.STONE_PICKAXE)&&AdventurePreparation.toolUses(item)<=8)return i;
        }
        return -1;
    }
    private static boolean usableUpgrade(ItemStack item){
        return (item.is(Items.STONE_PICKAXE)||item.is(Items.IRON_PICKAXE)||item.is(Items.DIAMOND_PICKAXE)||item.is(Items.NETHERITE_PICKAXE))
            &&(!item.isDamageableItem()||item.getMaxDamage()-item.getDamageValue()>8);
    }
    static String makeRoom(ServerLevel level,Villager v,Session s){
        if(!NativeRegionsScenario.nativeRegions(level)||!WoodBootstrap.safe(level,v,s))return null;
        var bag=v.getInventory();
        if(AdventureActions.PERMISSIONS.allows(Capability.BUILD_CRAFTING_TABLE)
            &&bag.hasAnyMatching(item->item.is(Items.CRAFTING_TABLE))&&BoatPreparation.placeTable(level,v,s)){
            s.inventoryRoomActions++;
            s.inventoryRoomReceipt=Map.of("gameTime",level.getGameTime(),"action","placed carried table","position",s.craftingTable.toShortString());
            return "put down an owned workbench to make working space";
        }
        int backup=offhandBackup(bag,v.getOffhandItem());
        if(backup>=0&&AdventureActions.PERMISSIONS.allows(Capability.EQUIP_ITEM)){
            var actual=bag.getItem(backup);var item=actual.getItem();int before=bag.countItem(item);
            v.setItemSlot(EquipmentSlot.OFFHAND,actual);bag.setItem(backup,ItemStack.EMPTY);v.setGuaranteedDrop(EquipmentSlot.OFFHAND);
            s.inventoryRoomActions++;s.inventoryRoomReceipt=Map.of("gameTime",level.getGameTime(),"action","carried backup pickaxe in empty offhand",
                "item",BuiltInRegistries.ITEM.getKey(item).toString(),"bagBefore",before,"bagAfter",bag.countItem(item),"offhandCount",v.getOffhandItem().getCount());
            s.mind.remember("Kept my real backup wooden pickaxe in my empty offhand to free a working slot");
            return "carrying the backup pickaxe in the offhand to make working space";
        }
        int stoneBackup=offhandStoneBackup(bag,v.getMainHandItem(),v.getOffhandItem());
        if(stoneBackup>=0&&AdventureActions.PERMISSIONS.allows(Capability.EQUIP_ITEM)){
            var actual=bag.getItem(stoneBackup);var previous=v.getOffhandItem();
            bag.setItem(stoneBackup,previous);v.setItemSlot(EquipmentSlot.OFFHAND,actual);v.setGuaranteedDrop(EquipmentSlot.OFFHAND);
            s.inventoryRoomActions++;s.inventoryRoomReceipt=Map.of("gameTime",level.getGameTime(),
                "action","carried owned stone backup in offhand","offhand",actual.toString(),"displacedToBag",previous.toString());
            s.mind.remember("Moved my real stone backup into my offhand; the older wooden pick remains in my bag");
            return "carrying the stone backup in the offhand to free a mining slot";
        }
        if(WoodBootstrap.compactMiningWood(level,v,s)){
            s.inventoryRoomActions++;s.inventoryRoomReceipt=Map.of("gameTime",level.getGameTime(),
                "action","crafted actual spare planks into existing stick stock","planksSpent",2,"sticksProduced",4);
            return "crafted spare planks into sticks to free a mining slot";
        }
        if(!AdventureActions.PERMISSIONS.allows(Capability.DROP_SURPLUS_MATERIALS))return null;
        int slot=secondaryCrop(bag);boolean tool=false,waste=false;
        if(slot<0){slot=supersededPickaxe(bag,v.getMainHandItem());tool=slot>=0;}
        if(slot<0){slot=quarryByproduct(bag);waste=slot>=0;}
        if(slot<0)return null;
        var stack=bag.getItem(slot).copy();var dropped=v.spawnAtLocation(level,stack.copy());if(dropped==null)return null;
        // Let the pending recipe use its freed slot before ordinary pickup can
        // immediately reclaim the stock the actor just put down.
        dropped.setPickUpDelay(waste?1200:200);
        int before=bag.countItem(stack.getItem());bag.removeItem(slot,stack.getCount());s.inventoryRoomActions++;
        s.inventoryRoomReceipt=Map.of("gameTime",level.getGameTime(),"action",waste?"put down quarry byproduct":tool?"put down superseded pickaxe":"put down secondary crop stock",
            "item",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),"itemEntity",dropped.getUUID().toString(),
            "bagBefore",before,"bagAfter",bag.countItem(stack.getItem()),"worldCount",dropped.getItem().getCount());
        s.mind.remember(waste?"Put actual quarry byproducts on the ground to make room for useful working materials":tool?"Put down my superseded pickaxe to make working space while retaining a usable upgraded pickaxe"
            :"Put down secondary crop stock to make room for work; retained my primary wheat planting reserve");
        return waste?"put down actual quarry byproducts to make working space":tool?"put down the superseded pickaxe to make working space":"put down secondary crop stock to make working space";
    }
    private NativeWorkInventory(){}
}
