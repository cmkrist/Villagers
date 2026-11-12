package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.server.level.ServerLevel;

/** Paid native armor recipes after the iron pickaxe; equipped items stay in entity NBT. */
final class AdventureArmor {
    enum Piece {
        CHEST(Items.IRON_CHESTPLATE,EquipmentSlot.CHEST,"I I","III","III"),
        LEGS(Items.IRON_LEGGINGS,EquipmentSlot.LEGS,"III","I I","I I"),
        HEAD(Items.IRON_HELMET,EquipmentSlot.HEAD,"III","I I"),
        FEET(Items.IRON_BOOTS,EquipmentSlot.FEET,"I I","I I");
        final Item item;final EquipmentSlot slot;final List<String> rows;final int iron;
        Piece(Item item,EquipmentSlot slot,String...rows){
            this.item=item;this.slot=slot;this.rows=List.of(rows);
            this.iron=(int)this.rows.stream().flatMapToInt(String::chars).filter(c->c=='I').count();
        }
    }
    record Plan(Piece piece,List<ItemStack> before,SimpleContainer remaining,CraftingInput input){}
    static final class State {
        long nextWork;int crafted,equipped;Map<String,Object> receipt=Map.of();
    }
    static Map<String,Object> status(Villager v,Session s){
        var next=next(v);var w=s.armor;
        return Map.of("nextPiece",next==null?"":next.name(),"ironTarget",next==null?0:next.iron,
            "crafted",w.crafted,"equipped",w.equipped,"lastReceipt",w.receipt,"ready",next==null);
    }
    private static boolean protectedSlot(ItemStack item,Piece piece){
        if(item.isEmpty())return false;
        return switch(piece){
            case CHEST -> item.is(Items.IRON_CHESTPLATE)||item.is(Items.DIAMOND_CHESTPLATE)||item.is(Items.NETHERITE_CHESTPLATE);
            case LEGS -> item.is(Items.IRON_LEGGINGS)||item.is(Items.DIAMOND_LEGGINGS)||item.is(Items.NETHERITE_LEGGINGS);
            case HEAD -> item.is(Items.IRON_HELMET)||item.is(Items.DIAMOND_HELMET)||item.is(Items.NETHERITE_HELMET);
            case FEET -> item.is(Items.IRON_BOOTS)||item.is(Items.DIAMOND_BOOTS)||item.is(Items.NETHERITE_BOOTS);
        }&&AdventurePreparation.toolUses(item)>0;
    }
    static Piece next(Villager v){
        for(var piece:Piece.values())if(!protectedSlot(v.getItemBySlot(piece.slot),piece))return piece;
        return null;
    }
    static int ironTarget(Villager v){var piece=next(v);return piece==null?0:piece.iron;}
    static Plan plan(Container bag,Piece piece){
        if(piece==null||bag.countItem(Items.IRON_INGOT)<piece.iron)return null;
        var before=HarvestMaterials.snapshot(bag);var remaining=new SimpleContainer(bag.getContainerSize());
        for(int i=0;i<bag.getContainerSize();i++)remaining.setItem(i,bag.getItem(i).copy());
        var cells=new ArrayList<ItemStack>();
        for(var row:piece.rows)for(int c=0;c<row.length();c++){
            if(row.charAt(c)==' '){cells.add(ItemStack.EMPTY);continue;}
            for(int slot=0;slot<remaining.getContainerSize();slot++)if(remaining.getItem(slot).is(Items.IRON_INGOT)){
                cells.add(remaining.removeItem(slot,1));break;
            }
        }
        return new Plan(piece,before,remaining,CraftingInput.of(3,piece.rows.size(),cells));
    }
    static boolean commit(Container bag,Plan plan,ItemStack output){
        if(plan==null||!output.is(plan.piece.item)||output.getCount()!=1||!HarvestMaterials.unchanged(bag,plan.before))return false;
        var next=new SimpleContainer(bag.getContainerSize());
        for(int i=0;i<next.getContainerSize();i++)next.setItem(i,plan.remaining.getItem(i).copy());
        if(!next.addItem(output.copy()).isEmpty())return false;
        for(int i=0;i<next.getContainerSize();i++)bag.setItem(i,next.getItem(i));return true;
    }
    static String work(ServerLevel level,Villager v,Session s,long now){
        if(!AdventurePreparation.nominated(v)||!WoodBootstrap.safe(level,v,s))return null;
        var piece=next(v);if(piece==null)return null;var w=s.armor;if(now<w.nextWork)return "preparing the next owned armor action";
        var bag=v.getInventory();
        if(AdventureActions.PERMISSIONS.allows(Capability.EQUIP_ITEM))for(int slot=0;slot<bag.getContainerSize();slot++){
            var actual=bag.getItem(slot);if(!actual.is(piece.item)||actual.getCount()!=1||AdventurePreparation.toolUses(actual)==0)continue;
            var previous=v.getItemBySlot(piece.slot);bag.setItem(slot,previous);v.setItemSlot(piece.slot,actual);v.setGuaranteedDrop(piece.slot);
            w.equipped++;w.nextWork=now+20;
            w.receipt=Map.of("gameTime",now,"action","equipped owned armor","item",BuiltInRegistries.ITEM.getKey(piece.item).toString(),
                "slot",piece.slot.getName(),"displaced",previous.toString());
            return "equipped a real owned iron armor piece";
        }
        var plan=plan(bag,piece);if(plan==null||!AdventureActions.PERMISSIONS.allows(Capability.CRAFT_IRON_ARMOR))return null;
        if(bag.hasAnyMatching(a->a.is(Items.CRAFTING_TABLE))&&BoatPreparation.placeTable(level,v,s))return "placed an owned workbench for armor crafting";
        var table=BreadCrafting.table(level,v,s);if(table==null)return "needs a real workbench for iron armor";
        if(!BreadCrafting.canWorkAt(level,v,s,table)){
            BreadCrafting.approach(level,v,s,table,now);return "carrying owned iron to a workbench for armor";
        }
        var recipe=level.recipeAccess().getRecipeFor(RecipeType.CRAFTING,plan.input,level);
        if(recipe.isPresent()&&recipe.get().value().getRemainingItems(plan.input).stream().allMatch(ItemStack::isEmpty)
            &&commit(bag,plan,recipe.get().value().assemble(plan.input))){
            w.crafted++;w.nextWork=now+30;s.adventureCrafts++;
            w.receipt=Map.of("gameTime",now,"action","crafted native iron armor","item",BuiltInRegistries.ITEM.getKey(piece.item).toString(),
                "ironSpent",piece.iron,"table",table.toShortString());
            s.mind.remember("Paid "+piece.iron+" owned iron ingots for real "+BuiltInRegistries.ITEM.getKey(piece.item));Villagers.persist(s);
            return "crafted iron armor using the actual native recipe";
        }
        var room=NativeWorkInventory.makeRoom(level,v,s);return room==null?"native armor recipe or inventory capacity blocked":room;
    }
    private AdventureArmor(){}
}
