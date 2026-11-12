package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.gameevent.GameEvent;

/** Actual village sheep and native shearing loot, never supplied fleece or forced regrowth. */
final class SheepWool {
    static final class State {
        UUID sheep;long nextScan,nextPath,nextWork;int crafted,sheared,collected;
        Map<String,Object> receipt=Map.of();
    }
    static Map<String,Object> status(Session s){var w=s.sheepWool;return Map.of("craftedShears",w.crafted,"sheared",w.sheared,"collected",w.collected,"target",w.sheep==null?"":w.sheep.toString(),"lastReceipt",w.receipt);}
    static boolean usableShears(ItemStack item){return item.is(Items.SHEARS)&&AdventurePreparation.toolUses(item)>0;}
    static boolean equippedOrPacked(Villager v){return usableShears(v.getMainHandItem())||usableShears(v.getOffhandItem())||v.getInventory().hasAnyMatching(SheepWool::usableShears);}
    private static boolean useful(ServerLevel level,Villager v,Session s,Sheep sheep){
        return sheep.isAlive()&&sheep.readyForShearing()&&!sheep.hasCustomName()&&!sheep.isLeashed()&&!sheep.isInWater()&&!sheep.isOnFire()
            &&v.distanceToSqr(sheep)<=24*24&&v.hasLineOfSight(sheep)&&AdventureSupplies.inVillage(level,v,s,sheep.blockPosition())
            &&VillageBedding.wanted(v.getInventory(),new ItemStack(Items.WOOL.pick(sheep.getColor())))>0;
    }
    static String work(ServerLevel level,Villager v,Session s,long now){
        if(!WoodBootstrap.safe(level,v,s)||!NativeRegionsScenario.nativeRegions(level))return null;
        var w=s.sheepWool;if(now<w.nextWork)return null;
        var loose=level.getEntitiesOfClass(ItemEntity.class,v.getBoundingBox().inflate(12),i->i.isAlive()&&!i.hasPickUpDelay()&&i.getOwner()==null
            &&VillageBedding.wanted(v.getInventory(),i.getItem())>0
            &&Items.WOOL.asList().contains(i.getItem().getItem())&&v.hasLineOfSight(i)&&AdventureSupplies.inVillage(level,v,s,i.blockPosition()))
            .stream().min(Comparator.comparingDouble(v::distanceToSqr)).orElse(null);
        if(loose!=null){
            if(v.distanceToSqr(loose)<=4){
                var material=loose.getItem().getItem();int before=loose.getItem().getCount(),owned=v.getInventory().countItem(material);
                int count=Math.min(before,VillageBedding.wanted(v.getInventory(),loose.getItem()));
                var remainder=v.getInventory().addItem(loose.getItem().copyWithCount(count));int moved=count-remainder.getCount();
                if(moved<=0)return NativeWorkInventory.makeRoom(level,v,s);
                v.take(loose,moved);loose.getItem().shrink(moved);if(loose.getItem().isEmpty())loose.discard();
                w.collected+=moved;w.nextWork=now+10;
                w.receipt=Map.of("gameTime",now,"action","collected actual loose wool","item",BuiltInRegistries.ITEM.getKey(material).toString(),"worldBefore",before,"worldAfter",before-moved,"bagBefore",owned,"bagAfter",v.getInventory().countItem(material));
                return "collected native wool for a real nursery bed";
            }
            if(now>=w.nextPath&&WorkNavigation.admit(level,now,v)){
                w.nextPath=now+40;var path=NativePathLimits.search(v,1,()->WorkNavigation.pathTo(level,v,s,loose.blockPosition(),1));
                if(WorkNavigation.staysHome(level,v,s,path)&&BoatTravel.dryPath(level,path)&&v.getNavigation().moveTo(path,.6))return "walking to actual loose village wool";
            }
        }
        if(!equippedOrPacked(v)&&v.getInventory().countItem(Items.IRON_INGOT)<2)return null;
        Sheep sheep=w.sheep!=null&&level.getEntity(w.sheep) instanceof Sheep a&&useful(level,v,s,a)?a:null;
        if(sheep==null){
            if(now<w.nextScan)return null;w.nextScan=now+100;
            sheep=level.getEntitiesOfClass(Sheep.class,v.getBoundingBox().inflate(24),a->useful(level,v,s,a)).stream().min(Comparator.comparingDouble(v::distanceToSqr)).orElse(null);
            w.sheep=sheep==null?null:sheep.getUUID();
        }
        if(sheep==null)return null;
        if(!equippedOrPacked(v)){
            if(!AdventureActions.PERMISSIONS.allows(Capability.CRAFT_SHEARS))return null;
            var plan=BedCrafting.shears(v.getInventory());if(plan==null)return null;
            var recipe=level.recipeAccess().getRecipeFor(RecipeType.CRAFTING,plan.input(),level);
            if(recipe.isEmpty()||recipe.get().value().getRemainingItems(plan.input()).stream().anyMatch(a->!a.isEmpty())
                ||!BedCrafting.commit(v.getInventory(),plan,recipe.get().value().assemble(plan.input())))return null;
            w.crafted++;w.nextWork=now+30;w.receipt=Map.of("gameTime",now,"action","crafted native shears","ironSpent",2);
            s.mind.remember("Crafted real shears from two owned iron ingots for village bedding");Villagers.persist(s);return "crafted shears from two real owned ingots";
        }
        if(!AdventureActions.PERMISSIONS.allows(Capability.SHEAR_NATIVE_SHEEP))return null;
        if(v.distanceToSqr(sheep)>9){
            if(now>=w.nextPath&&WorkNavigation.admit(level,now,v)){
                w.nextPath=now+40;var target=sheep;
                var path=NativePathLimits.search(v,1,()->WorkNavigation.pathTo(level,v,s,target.blockPosition(),1));
                if(WorkNavigation.staysHome(level,v,s,path)&&BoatTravel.dryPath(level,path)&&v.getNavigation().moveTo(path,.6))return "walking to an observed sheep for native wool";
            }
            return null;
        }
        if(!useful(level,v,s,sheep)||!AdventureActions.prepareShears(level,v))return null;
        v.getNavigation().stop();var tool=v.getMainHandItem();int damage=tool.getDamageValue();float health=sheep.getHealth();
        v.swingForAttack(InteractionHand.MAIN_HAND);sheep.shear(level,SoundSource.NEUTRAL,tool);level.gameEvent(v,GameEvent.SHEAR,sheep.blockPosition());
        tool.hurtAndBreak(1,v,EquipmentSlot.MAINHAND);w.sheared++;w.nextWork=now+40;w.sheep=null;
        w.receipt=Map.of("gameTime",now,"action","native sheep shearing","sheep",sheep.getUUID().toString(),"sheepHealthBefore",health,"sheepHealthAfter",sheep.getHealth(),
            "toolDamageBefore",damage,"toolDamageAfter",v.getMainHandItem().getDamageValue(),"nativeSheared",sheep.isSheared(),"distance",v.distanceTo(sheep));
        s.mind.remember("Sheared a real native village sheep for bedding; it can regrow its fleece by grazing");return "sheared an actual village sheep with owned shears";
    }
    private SheepWool(){}
}
