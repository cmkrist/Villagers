package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Real seed gathering, one-log hoe preparation and age-zero crops. Dry farmland grows by native ticks. */
final class NativeCampFarms {
    private static final String PLOT="villagers:camp_crop:";
    static final class State {
        BlockPos target,working;long scanAt,pathAt,until,started,lastWork,workAt,yieldUntil;
        int grass,logs,recipes,planted;String activity="no camp farming";
        final Map<BlockPos,Long> blocked=new HashMap<>();Map<String,Object> receipt=Map.of();
        Map<String,Object> status(){return Map.of("activity",activity,"grass",grass,"logs",logs,"recipes",recipes,"planted",planted,
            "target",target==null?"":target.toShortString(),"lastReceipt",receipt);}
    }
    private static boolean permit(Capability c){return AdventureActions.PERMISSIONS.allows(c);}
    private static boolean hoe(ItemStack item){return item.is(ItemTags.HOES)&&(!item.isDamageableItem()||item.getDamageValue()<item.getMaxDamage());}
    static boolean ownsHoe(Pillager mob){return hoe(mob.getMainHandItem())||mob.getInventory().hasAnyMatching(NativeCampFarms::hoe);}
    static PlantingMaterials.Plan planting(Container bag){
        var p=PlantingMaterials.reserve(bag,item->item==Items.WHEAT_SEEDS?1:0);
        return p!=null&&p.before().is(Items.WHEAT_SEEDS)?p:null;
    }
    static Map<String,Object> status(Pillager mob,State w){
        var report=new LinkedHashMap<String,Object>(w.status());var crops=new ArrayList<Map<String,Object>>();
        for(var tag:mob.entityTags())if(tag.startsWith(PLOT))try{
            var p=BlockPos.of(Long.parseLong(tag.substring(PLOT.length())));boolean loaded=mob.level().isLoaded(p);
            var crop=loaded?mob.level().getBlockState(p.above()):Blocks.AIR.defaultBlockState();
            crops.add(Map.of("soil",p.toShortString(),"loaded",loaded,"age",crop.is(Blocks.WHEAT)?crop.getValue(CropBlock.AGE):-1,
                "state",crop.toString(),"farmland",loaded&&mob.level().getBlockState(p).is(Blocks.FARMLAND)));
        }catch(NumberFormatException ignored){}
        report.put("crops",crops);return report;
    }
    static int wanted(Container bag,boolean ownsHoe,ItemStack item){
        if(item.is(Items.WHEAT_SEEDS))return Math.max(0,8-bag.countItem(Items.WHEAT_SEEDS));
        if(ownsHoe)return 0;if(hoe(item))return 1;
        int wood=0;for(int i=0;i<bag.getContainerSize();i++){
            var s=bag.getItem(i);if(WoodCrafting.raw(s))wood+=4*s.getCount();else if(WoodCrafting.plank(s))wood+=s.getCount();
        }
        int need=bag.countItem(Items.STICK)>=2?2:4;
        if(WoodCrafting.raw(item))return wood<need?1:0;
        if(WoodCrafting.plank(item))return Math.max(0,need-wood);
        if(item.is(Items.STICK))return Math.max(0,2-bag.countItem(Items.STICK));return 0;
    }
    private static boolean enabled(Pillager mob){return Villagers.config().nativeCampFarming()&&CreatureAgents.managed(mob)&&NativeIllagers.member(mob)
        &&!IllagerFamilies.juvenile(mob)&&Villagers.LEARNING_MODE!=local.villagers.learning.LearningMode.FROZEN
        &&permit(Capability.CULTIVATE_NATIVE_CAMP);}
    private static boolean local(ServerLevel level,Pillager mob,BlockPos p){return NativeCampWork.local(level,mob,p)
        &&level.isLoaded(p.above(2))&&!ConstructionSites.isProtected(level,p.above())
        &&(NativeIllagers.insideCamp(level,mob,p)||!level.structureManager().getStructureWithPieceAt(p,h->true).isValid());}
    private static boolean ground(ServerLevel level,Pillager mob,BlockPos p){
        if(!local(level,mob,p))return false;var state=level.getBlockState(p);
        return (state.is(Blocks.DIRT)||state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.FARMLAND))&&level.getBlockEntity(p)==null
            &&level.getBlockState(p.above()).isAir()&&level.getBlockState(p.above(2)).isAir()&&level.canSeeSky(p.above())
            &&level.getFluidState(p).isEmpty()&&level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP)
            &&NativeTerrainWork.structuralCover(level,p)==null;
    }
    private static boolean seedGrass(ServerLevel level,Pillager mob,BlockPos p){return local(level,mob,p)&&permit(Capability.GATHER_PLANTING_SEEDS)
        &&!ConstructionSites.isProtected(level,p.below())&&level.getBlockState(p).is(Blocks.SHORT_GRASS)&&level.getBlockState(p.below()).is(net.minecraft.tags.BlockTags.DIRT);}
    private static boolean tree(ServerLevel level,Pillager mob,BlockPos p){return permit(Capability.FORESTRY)&&local(level,mob,p)
        &&BoatPreparation.naturalTrunk(level,p)&&level.getBlockState(p.below()).is(net.minecraft.tags.BlockTags.DIRT)
        &&!mob.getBoundingBox().intersects(new AABB(p));}
    private static boolean occupied(ServerLevel level,Pillager mob,BlockPos p){
        return level.getEntities(mob,new AABB(p).expandTowards(0,1,0)).stream()
            .anyMatch(e->!(e instanceof net.minecraft.world.entity.item.ItemEntity)&&!(e instanceof net.minecraft.world.entity.ExperienceOrb));
    }
    private static int plots(ServerLevel level,Pillager mob){
        int count=0;for(var tag:mob.entityTags().stream().filter(t->t.startsWith(PLOT)).toList())try{
            var soil=BlockPos.of(Long.parseLong(tag.substring(PLOT.length())));
            if(level.isLoaded(soil)&&(!level.getBlockState(soil).is(Blocks.FARMLAND)||!level.getBlockState(soil.above()).is(Blocks.WHEAT)))mob.removeTag(tag);
            else count++;
        }catch(NumberFormatException ignored){mob.removeTag(tag);}
        return count;
    }
    static boolean preparing(ServerLevel level,Pillager mob){return enabled(mob)&&plots(level,mob)<32;}
    private static boolean gather(ServerLevel level,Pillager mob,State w,BlockPos p,boolean wood,long now){
        if(!(wood?tree(level,mob,p):seedGrass(level,mob,p))||BlockContact.interactionPoint(level,mob,mob.getEyePosition(),p,3)==null
            ||occupied(level,mob,p))return false;
        var block=level.getBlockState(p);var tool=mob.getMainHandItem().copy();
        if(!p.equals(w.working)||now-w.lastWork>20||w.started==0){w.working=p;w.started=now;}
        w.lastWork=now;mob.getNavigation().stop();mob.swingForAttack(InteractionHand.MAIN_HAND);
        boolean harvest=!block.requiresCorrectToolForDrops()||tool.isCorrectToolForDrops(block);
        int duration=Math.max(1,(int)Math.ceil(block.getDestroySpeed(level,p)*(harvest?30:100)/Math.max(1,tool.getDestroySpeed(block))));
        if(now-w.started<duration)return false;
        var before=HarvestMaterials.snapshot(mob.getInventory());var next=new SimpleContainer(mob.getInventory().getContainerSize());
        for(int i=0;i<next.getContainerSize();i++)next.setItem(i,mob.getInventory().getItem(i).copy());
        var drops=Block.getDrops(block,level,p,null,mob,mob.getMainHandItem());var loose=new ArrayList<ItemStack>();
        for(var drop:drops){var left=next.addItem(drop.copy());if(!left.isEmpty())loose.add(left);}
        if(!block.equals(level.getBlockState(p))||!ItemStack.matches(tool,mob.getMainHandItem())||!HarvestMaterials.unchanged(mob.getInventory(),before)
            ||!(wood?tree(level,mob,p):seedGrass(level,mob,p))||!level.destroyBlock(p,false,mob))return false;
        for(int i=0;i<next.getContainerSize();i++)mob.getInventory().setItem(i,next.getItem(i));for(var drop:loose)Block.popResource(level,p,drop);
        var properties=mob.getMainHandItem().get(net.minecraft.core.component.DataComponents.TOOL);
        if(properties!=null)mob.getMainHandItem().hurtAndBreak(properties.damagePerBlock(),mob,EquipmentSlot.MAINHAND);
        if(wood)w.logs++;else w.grass++;w.receipt=Map.of("gameTime",now,"action",wood?"gathered one natural trunk block":"gathered actual short grass",
            "position",p.toShortString(),"duration",duration,"drops",drops.stream().map(ItemStack::toString).toList(),
            "seedsAfter",mob.getInventory().countItem(Items.WHEAT_SEEDS));
        CreatureAgents.mind(mob).exert(.005f);return true;
    }
    private static String craft(ServerLevel level,Pillager mob,State w,WoodCrafting.Plan plan,long now){
        if(plan==null||!permit(Capability.CRAFT_WOODEN_HOE))return null;
        if(plan.step()==WoodCrafting.Step.HOE){
            var tables=NativeIllagers.knownTables(level,mob);
            for(var p:BlockPos.betweenClosed(mob.blockPosition().offset(-16,-3,-16),mob.blockPosition().offset(16,3,16)))
                if(local(level,mob,p)&&level.getBlockState(p).is(Blocks.CRAFTING_TABLE)&&NativeCampWork.visible(level,mob,p))NativeIllagers.rememberTable(level,mob,p);
            tables=NativeIllagers.knownTables(level,mob);tables.sort(Comparator.comparingDouble(p->p.distSqr(mob.blockPosition())));
            BlockPos chosen=null;
            for(var table:tables.stream().limit(4).toList()){
                if(BlockContact.interactionPoint(level,mob,mob.getEyePosition(),table,3)!=null){chosen=table;break;}
                if(now>=w.pathAt){var path=IllagerFood.approachPath(level,mob,table);if(path!=null){mob.getNavigation().moveTo(path,.8);w.pathAt=now+40;return "taking owned hoe ingredients to a real table";}}
            }
            if(chosen==null)return now<w.pathAt?"taking owned hoe ingredients to a real table":null;
        }
        if(now<w.workAt)return "preparing owned hoe ingredients";
        var recipe=level.recipeAccess().getRecipeFor(RecipeType.CRAFTING,plan.input(),level);
        if(recipe.isEmpty()||recipe.get().value().getRemainingItems(plan.input()).stream().anyMatch(s->!s.isEmpty()))return null;
        var output=recipe.get().value().assemble(plan.input());
        if(!WoodCrafting.commit(mob.getInventory(),plan,output))return null;
        mob.getNavigation().stop();w.recipes++;w.workAt=now+30;
        w.receipt=Map.of("gameTime",now,"action","crafted owned hoe ingredients","step",plan.step().toString(),"output",output.toString(),
            "before",plan.before().stream().map(ItemStack::toString).toList(),"after",HarvestMaterials.snapshot(mob.getInventory()).stream().map(ItemStack::toString).toList());
        return "crafted actual "+plan.output()+" for farming";
    }
    private static boolean plant(ServerLevel level,Pillager mob,State w,BlockPos soil,long now){
        if(!permit(Capability.CULTIVATE_PLANT)||!permit(Capability.EQUIP_ITEM)||!ground(level,mob,soil)||BlockContact.point(level,mob,mob.getEyePosition(),soil,3)==null
            ||level.getEntities(null,new AABB(soil.above()).expandTowards(0,1,0)).stream().anyMatch(e->!(e instanceof net.minecraft.world.entity.item.ItemEntity)&&!(e instanceof net.minecraft.world.entity.ExperienceOrb)))return false;
        var seed=planting(mob.getInventory());if(seed==null)return false;
        int slot=-1;for(int i=0;i<mob.getInventory().getContainerSize();i++)if(hoe(mob.getInventory().getItem(i))){slot=i;break;}
        if(slot<0&&!hoe(mob.getMainHandItem()))return false;
        var held=mob.getMainHandItem();boolean borrowed=slot>=0;
        if(borrowed){mob.setItemSlot(EquipmentSlot.MAINHAND,mob.getInventory().removeItem(slot,1));}
        try{
            var tool=mob.getMainHandItem();var toolBefore=tool.copy();var before=level.getBlockState(soil);
            var farmland=before.is(Blocks.FARMLAND)?before:Blocks.FARMLAND.defaultBlockState();var crop=Blocks.WHEAT.defaultBlockState();
            int seeds=mob.getInventory().countItem(Items.WHEAT_SEEDS);
            if(!PlantingMaterials.unchanged(mob.getInventory(),seed)||!before.equals(farmland)&&!level.setBlock(soil,farmland,Block.UPDATE_ALL))return false;
            if(!crop.canSurvive(level,soil.above())||!level.setBlock(soil.above(),crop,Block.UPDATE_ALL)
                ||!level.getBlockState(soil).equals(farmland)||!ItemStack.matches(toolBefore,mob.getMainHandItem())||!PlantingMaterials.commit(mob.getInventory(),seed)){
                if(level.getBlockState(soil.above()).equals(crop))level.setBlock(soil.above(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                if(level.getBlockState(soil).equals(farmland))level.setBlock(soil,before,Block.UPDATE_ALL);return false;
            }
            tool.hurtAndBreak(1,mob,EquipmentSlot.MAINHAND);mob.swingForAttack(InteractionHand.MAIN_HAND);
            mob.addTag(PLOT+soil.asLong());CreatureAgents.mind(mob).observeFarm(new local.villagers.mind.Place(level.dimension().identifier().toString(),soil.getX(),soil.getY()+1,soil.getZ()),"minecraft:wheat",now,List.of());
            w.planted++;w.yieldUntil=now+600;w.receipt=Map.of("gameTime",now,"action","planted a real age-zero camp crop","soil",soil.toShortString(),
                "seedBefore",seeds,"seedAfter",mob.getInventory().countItem(Items.WHEAT_SEEDS),"hoeBefore",toolBefore.getDamageValue(),
                "hoeAfter",tool.getDamageValue(),"hoeBroke",tool.isEmpty(),"moisture",farmland.getValue(FarmlandBlock.MOISTURE),"age",crop.getValue(CropBlock.AGE));
            CreatureAgents.mind(mob).remember("Planted camp wheat with a real seed and hoe at "+soil.above().toShortString());return true;
        }finally{
            if(borrowed){mob.getInventory().setItem(slot,mob.getMainHandItem());mob.setItemSlot(EquipmentSlot.MAINHAND,held);}
        }
    }
    private static net.minecraft.world.level.pathfinder.Path besidePlot(ServerLevel level,Pillager mob,BlockPos soil){
        var stands=new ArrayList<BlockPos>();
        for(var p:BlockPos.betweenClosed(soil.offset(-2,0,-2),soil.offset(2,2,2))){
            if(!local(level,mob,p)||!level.getFluidState(p).isEmpty())continue;
            double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,p);
            var feet=new Vec3(p.getX()+.5,floor,p.getZ()+.5);var body=mob.getBoundingBox().move(feet.subtract(mob.position()));
            if(body.intersects(new AABB(soil.above()).expandTowards(0,1,0))||!level.noCollision(mob,body)||level.noBlockCollision(mob,body.move(0,-.03,0))
                ||BlockContact.point(level,mob,feet.add(0,mob.getEyeHeight(),0),soil,3)==null)continue;
            stands.add(p.immutable());
        }
        return CampBeds.pathToStands(level,mob,stands);
    }
    static String tick(ServerLevel level,Pillager mob,CreatureAgents.State actor,long now){
        if(!enabled(mob)||!NativeRegionsScenario.nativeRegions(level)||mob.isOnFire()||mob.isInWater()||mob.isInLava()
            ||mob.getHealth()<mob.getMaxHealth()*.9||actor.mind.foodLevel()<16||actor.mind.fatigue()>.5||!mob.getActiveEffects().isEmpty())return null;
        var w=actor.campFarms;if(now<w.yieldUntil||plots(level,mob)>=32)return null;
        var bag=mob.getInventory();if(IllagerFood.bread(mob)<3&&bag.countItem(Items.WHEAT)>=3)return null;
        boolean seeds=bag.countItem(Items.WHEAT_SEEDS)<2;boolean wood=!seeds&&!ownsHoe(mob)&&WoodCrafting.next(bag)==null;
        if(!seeds&&!wood&&!ownsHoe(mob)){
            var activity=craft(level,mob,w,WoodCrafting.next(bag),now);if(activity!=null)w.activity=activity;return activity;
        }
        w.blocked.entrySet().removeIf(e->e.getValue()<=now);
        if(w.target!=null&&(now>=w.until||!(seeds?seedGrass(level,mob,w.target):wood?tree(level,mob,w.target):ground(level,mob,w.target))
            ||(seeds||wood)&&occupied(level,mob,w.target)))w.target=null;
        if(w.target==null){
            if(now<w.scanAt)return null;w.scanAt=now+100;var origin=mob.blockPosition();int range=wood?16:8;
            var candidates=new ArrayList<BlockPos>();
            for(var p:BlockPos.betweenClosed(origin.offset(-range,-3,-range),origin.offset(range,3,range)))
                if(!w.blocked.containsKey(p)&&(seeds?seedGrass(level,mob,p):wood?tree(level,mob,p):ground(level,mob,p))
                    &&(!(seeds||wood)||!occupied(level,mob,p))&&NativeCampWork.visible(level,mob,p))candidates.add(p.immutable());
            candidates.sort(Comparator.comparingDouble(p->p.distSqr(origin)));w.target=candidates.isEmpty()?null:candidates.getFirst();w.until=now+800;
        }
        if(w.target==null)return null;var p=w.target;
        boolean inPlot=!seeds&&!wood&&mob.getBoundingBox().intersects(new AABB(p.above()).expandTowards(0,1,0));
        if(!inPlot&&(seeds?BlockContact.interactionPoint(level,mob,mob.getEyePosition(),p,3):BlockContact.point(level,mob,mob.getEyePosition(),p,3))!=null){
            mob.getNavigation().stop();boolean done;
            if(seeds||wood)done=gather(level,mob,w,p,wood,now);
            else{
                if(!p.equals(w.working)||now-w.lastWork>20){w.working=p;w.started=now;}w.lastWork=now;
                done=now-w.started>=20&&plant(level,mob,w,p,now);
            }
            if(done){w.target=w.working=null;w.started=0;w.scanAt=now+20;}
            return w.activity=seeds?"gathering native planting seeds":wood?"gathering one natural log for a hoe":"tilling and planting actual camp wheat";
        }
        w.working=null;w.started=0;
        if(now>=w.pathAt){w.pathAt=now+40;var path=seeds||wood?IllagerFood.approachPath(level,mob,p):besidePlot(level,mob,p);
            if(path==null||!mob.getNavigation().moveTo(path,.8)){w.blocked.put(p,now+400);w.target=null;return null;}}
        return w.activity="walking to locally observed camp farming work";
    }
    private NativeCampFarms(){}
}
