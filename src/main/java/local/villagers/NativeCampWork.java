package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;

/** Native camp storage and paid bedding. No structural harvesting, stolen beds or supplied ingredients. */
final class NativeCampWork {
    private static final String BUILT="villagers:camp_beds_built:";
    static final class State {
        BlockPos target,site,survey;Direction facing;
        long scanAt,pathAt,workAt,targetUntil,surveyUntil;
        int transfers,recipes,beds,inspections;
        boolean loaded;
        String activity="no camp work";
        String route="none";
        final Map<BlockPos,Long> inspected=new HashMap<>();
        final Map<BlockPos,Integer> visited=new HashMap<>();
        final CampBeds.State reusedBeds=new CampBeds.State();
        Map<String,Object> receipt=Map.of();
        Map<String,Object> status(){return Map.of("activity",activity,"transfers",transfers,"recipes",recipes,"beds",beds,"lastReceipt",receipt,
            "target",target==null?"":target.toShortString(),"route",route,"inspections",inspections,
            "reusedBeds",Map.of("collected",reusedBeds.moved,"lastReceipt",reusedBeds.receipt));}
    }
    private static boolean permitted(Capability c){return AdventureActions.PERMISSIONS.allows(c);}
    static boolean local(ServerLevel level,Mob mob,BlockPos p){
        var home=WarCombat.home(mob);int radius=NativeIllagers.radius(mob);
        return home!=null&&level.isLoaded(p)&&level.getWorldBorder().isWithinBounds(p)&&!ConstructionSites.isProtected(level,p)
            &&Math.abs(p.getX()-home.x())<=radius&&Math.abs(p.getZ()-home.z())<=radius;
    }
    static boolean visible(ServerLevel level,Mob mob,BlockPos p){
        var hit=level.clip(new net.minecraft.world.level.ClipContext(mob.getEyePosition(),Vec3.atCenterOf(p),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,mob));
        return hit.getType()==HitResult.Type.MISS||hit.getBlockPos().equals(p);
    }
    private static Container storage(ServerLevel level,Pillager mob,BlockPos p){
        return local(level,mob,p)&&NativeIllagers.insideCamp(level,mob,p)?NativeContainers.accessible(level,p):null;
    }
    private static int edible(Container bag){int count=0;for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).has(DataComponents.FOOD))count+=bag.getItem(i).getCount();return count;}
    private static int raw(Container bag){int count=0;for(int i=0;i<bag.getContainerSize();i++)if(WoodCrafting.raw(bag.getItem(i)))count+=bag.getItem(i).getCount();return count;}
    private static int planks(Container bag){int count=0;for(int i=0;i<bag.getContainerSize();i++)if(WoodCrafting.plank(bag.getItem(i)))count+=bag.getItem(i).getCount();return count;}
    private static int wanted(Pillager mob,ItemStack item,boolean housing){
        var bag=mob.getInventory();
        if(item.is(Items.BREAD))return Math.max(0,8-IllagerFood.bread(mob));
        if(item.has(DataComponents.FOOD)){
            if(housing&&IllagerFood.bread(mob)>0)return 0;
            for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).has(DataComponents.FOOD)&&!bag.getItem(i).is(item.getItem()))return 0;
            return Math.max(0,8-edible(bag));
        }
        if(item.is(Items.WHEAT))return Math.max(0,9-bag.countItem(Items.WHEAT));
        if(!housing)return 0;
        if(item.is(Items.STRING))return Math.max(0,12-bag.countItem(Items.STRING)-4*bag.countItem(Items.WOOL.white()));
        if(item.is(Items.WOOL.white()))return Math.max(0,3-bag.countItem(Items.WOOL.white()));
        if(WoodCrafting.raw(item))return planks(bag)>=3?0:Math.max(0,1-raw(bag));
        if(WoodCrafting.plank(item))return Math.max(0,3-planks(bag));
        return 0;
    }
    private static boolean take(ServerLevel level,Pillager mob,State w,BlockPos p,boolean housing,long now){
        if(!permitted(Capability.OPEN_CAMP_STORAGE)||mob.getEyePosition().distanceToSqr(Vec3.atCenterOf(p))>9||!visible(level,mob,p))return false;
        var source=storage(level,mob,p);if(source==null)return false;
        w.inspections++;
        var contents=new ArrayList<String>();for(int i=0;i<source.getContainerSize();i++)if(!source.getItem(i).isEmpty())contents.add(source.getItem(i).toString());
        w.receipt=Map.of("gameTime",now,"inspected",p.toShortString(),"contents",contents);
        if(housing&&IllagerFood.bread(mob)>0){
            var bag=mob.getInventory();boolean full=true;for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).isEmpty())full=false;
            if(full)for(int i=0;i<bag.getContainerSize();i++){
                var surplus=bag.getItem(i);if(!surplus.has(DataComponents.FOOD)||surplus.is(Items.BREAD))continue;
                for(int j=0;j<source.getContainerSize();j++)if(source.getItem(j).isEmpty()&&source.canPlaceItem(j,surplus)&&surplus.getCount()<=source.getMaxStackSize(surplus)){
                    int count=surplus.getCount();String item=surplus.getItem().toString();source.setItem(j,surplus.copy());bag.setItem(i,ItemStack.EMPTY);source.setChanged();
                    w.transfers++;w.receipt=Map.of("gameTime",now,"container",p.toShortString(),"returned",item,"sourceBefore",0,"sourceAfter",count,"moved",count);
                    CreatureAgents.mind(mob).remember("Returned real surplus camp food to make room for bedding materials");return true;
                }
            }
        }
        // The first physical access invokes Minecraft's saved native loot table once.
        // No remote inventory scan or loot-table/seed replacement is performed.
        for(int i=0;i<source.getContainerSize();i++){
            var item=source.getItem(i);int count=Math.min(item.getCount(),wanted(mob,item,housing));if(count==0)continue;
            while(count>0&&!mob.getInventory().canAddItem(item.copyWithCount(count)))count--;
            if(count==0)continue;
            var before=item.getCount();String itemId=item.getItem().toString();var real=source.removeItem(i,count);if(real.isEmpty())continue;
            var remaining=mob.getInventory().addItem(real);
            if(!remaining.isEmpty()){
                if(source.getItem(i).isEmpty())source.setItem(i,remaining);
                else source.getItem(i).grow(remaining.getCount());
            }
            int moved=count-remaining.getCount();if(moved<=0)continue;
            source.setChanged();w.transfers++;
            w.receipt=Map.of("gameTime",now,"container",p.toShortString(),"item",itemId,"sourceBefore",before,"sourceAfter",source.getItem(i).getCount(),"moved",moved);
            mob.level().gameEvent(mob,GameEvent.CONTAINER_OPEN,p);mob.swingForAttack(net.minecraft.world.InteractionHand.MAIN_HAND);
            CreatureAgents.mind(mob).remember("Took "+moved+" owned camp supplies from "+p.toShortString());return true;
        }
        return false;
    }
    static String tick(ServerLevel level,Mob body,CreatureAgents.State agent,long now){
        if(!(body instanceof Pillager mob)||!NativeIllagers.member(mob)||IllagerFamilies.juvenile(mob)||!mob.isAlive()
                ||mob.isOnFire()||mob.isInWater()||mob.getHealth()<mob.getMaxHealth()*.9||agent.mind.fatigue()>.65||level.isDarkOutside())return null;
        var w=agent.campWork;var bag=mob.getInventory();
        IllagerFood.openRouteDoors(level,mob,agent.foodWork);
        if(!w.loaded){
            for(var tag:mob.entityTags())if(tag.startsWith(BUILT))try{w.beds=Math.clamp(Integer.parseInt(tag.substring(BUILT.length())),0,2);}catch(NumberFormatException ignored){}
            w.loaded=true;
        }
        boolean housing=Villagers.config().breeding()&&w.beds<2&&IllagerFamilies.needsHousing(level,mob);
        boolean food=edible(bag)<6&&IllagerFood.bread(mob)<6;
        if(now<w.workAt)return w.target!=null?w.activity:null;
        if(housing&&agent.mind.foodLevel()>=14){
            if(bag.hasAnyMatching(CampBeds::bedItem))return build(level,mob,agent,w,now);
            String reused=CampBeds.work(level,mob,agent,w.reusedBeds,now);
            if(reused!=null)return w.activity=reused;
            var plan=BedCrafting.next(bag);
            if(plan!=null&&permitted(Capability.CRAFT_BED)){
                if(plan.table()){
                    var table=findTable(level,mob);
                    if(table==null)return null;
                    if(mob.getEyePosition().distanceToSqr(Vec3.atCenterOf(table))>9||!visible(level,mob,table)){
                        if(now>=w.pathAt){w.pathAt=now+40;IllagerFood.approachNearby(level,mob,table);}
                        return w.activity="taking bed materials to a workbench";
                    }
                }
                var recipe=level.recipeAccess().getRecipeFor(RecipeType.CRAFTING,plan.input(),level);
                if(recipe.isPresent()&&recipe.get().value().getRemainingItems(plan.input()).stream().allMatch(ItemStack::isEmpty)
                        &&BedCrafting.commit(bag,plan,recipe.get().value().assemble(plan.input()))){
                    mob.getNavigation().stop();w.recipes++;w.workAt=now+30;agent.mind.exert(.005f);
                    agent.mind.remember("Crafted "+plan.output()+" from actual camp materials");return w.activity="crafting paid bedding";
                }
            }
        }
        if(!food&&!housing){w.target=null;return null;}
        if(w.target!=null&&(now>w.targetUntil||storage(level,mob,w.target)==null)){w.inspected.put(w.target,now+1200);w.target=null;}
        if(w.target==null&&now>=w.scanAt){
            w.scanAt=now+100;w.inspected.entrySet().removeIf(e->e.getValue()<=now);
            var origin=mob.blockPosition();double nearest=Double.MAX_VALUE;
            for(var p:BlockPos.betweenClosed(origin.offset(-12,-3,-12),origin.offset(12,3,12))){
                if(w.inspected.containsKey(p)||!local(level,mob,p)||!visible(level,mob,p)||storage(level,mob,p)==null)continue;
                double d=p.distSqr(origin);if(d<nearest){nearest=d;w.target=p.immutable();}
            }
            w.targetUntil=now+400;
        }
        if(w.target==null)return survey(level,mob,w,now);
        var p=w.target;
        if(mob.getEyePosition().distanceToSqr(Vec3.atCenterOf(p))<=9&&visible(level,mob,p)){
            mob.getNavigation().stop();
            if(take(level,mob,w,p,housing,now)){w.workAt=now+30;return w.activity="collecting real camp supplies";}
            w.inspected.put(p,now+4000);w.target=null;return null;
        }
        if(now>=w.pathAt){
            w.pathAt=now+40;
            if(!IllagerFood.approachNearby(level,mob,p)){w.route="no safe complete storage path";w.inspected.put(p,now+1200);w.target=null;return null;}
            w.route="native storage path started";
        }
        return w.activity="walking to camp storage";
    }
    private static BlockPos cell(BlockPos p){return new BlockPos(Math.floorDiv(p.getX(),8),Math.floorDiv(p.getY(),4),Math.floorDiv(p.getZ(),8));}
    private static String survey(ServerLevel level,Pillager mob,State w,long now){
        var origin=mob.blockPosition();
        if(w.survey!=null&&(origin.distSqr(w.survey)<2||now>=w.surveyUntil)){
            w.visited.merge(cell(w.survey),1,Integer::sum);w.survey=null;
        }
        if(w.survey==null){
            w.visited.merge(cell(origin),1,Integer::sum);var candidates=new ArrayList<BlockPos>();
            for(int dx:new int[]{-12,-6,0,6,12})for(int dz:new int[]{-12,-6,0,6,12})for(int dy:new int[]{-2,0,2,4}){
                if(dx*dx+dz*dz<36)continue;var p=origin.offset(dx,dy,dz);
                if(local(level,mob,p)&&NativeIllagers.insideCamp(level,mob,p)&&level.isLoaded(p.above())&&level.getBlockState(p).isAir()
                    &&level.getBlockState(p.above()).isAir()&&level.getFluidState(p).isEmpty()&&level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))candidates.add(p);
            }
            candidates.sort(Comparator.<BlockPos>comparingInt(p->w.visited.getOrDefault(cell(p),0)).thenComparingDouble(p->-p.distSqr(origin)));
            for(var p:candidates.stream().limit(6).toList()){
                var path=mob.getNavigation().createPath(p,0,64);
                if(NativeIllagers.safePath(level,mob,path)&&mob.getNavigation().moveTo(path,.7)){
                    w.survey=p;w.surveyUntil=now+400;w.pathAt=now+40;break;
                }
                w.visited.merge(cell(p),1,Integer::sum);
            }
            if(w.survey==null){w.activity="no safe camp supply survey route";return null;}
        }else if(now>=w.pathAt){
            w.pathAt=now+40;var path=mob.getNavigation().createPath(w.survey,0,64);
            if(!NativeIllagers.safePath(level,mob,path)||!mob.getNavigation().moveTo(path,.7)){w.visited.merge(cell(w.survey),1,Integer::sum);w.survey=null;return null;}
        }
        return w.activity="looking for real camp supplies";
    }
    private static BlockPos findTable(ServerLevel level,Pillager mob){
        BlockPos found=null;
        for(var p:NativeIllagers.knownTables(level,mob))if(local(level,mob,p)&&(found==null||p.distSqr(mob.blockPosition())<found.distSqr(mob.blockPosition())))found=p;
        for(var p:BlockPos.betweenClosed(mob.blockPosition().offset(-16,-3,-16),mob.blockPosition().offset(16,3,16)))
            if(local(level,mob,p)&&level.getBlockState(p).is(Blocks.CRAFTING_TABLE)){
                NativeIllagers.rememberTable(level,mob,p);
                if(found==null||p.distSqr(mob.blockPosition())<found.distSqr(mob.blockPosition()))found=p.immutable();
            }
        return found;
    }
    private static boolean site(ServerLevel level,Pillager mob,BlockPos foot,Direction facing){
        var head=foot.relative(facing);
        for(var p:List.of(foot,head)){
            if(!local(level,mob,p)||!NativeIllagers.insideCamp(level,mob,p)||!local(level,mob,p.below())||!level.isLoaded(p.above(2))||level.canSeeSky(p.above())
                    ||!level.getBlockState(p).isAir()||!level.getBlockState(p.above()).isAir()||!level.getBlockState(p.above(2)).isAir()
                    ||!level.getFluidState(p).isEmpty()||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP)
                    ||!level.getEntities(null,new AABB(p).expandTowards(0,2,0)).isEmpty())return false;
        }
        // Leave a supported aisle beside both bed halves.
        for(var side:List.of(facing.getClockWise(),facing.getCounterClockWise())){
            boolean clear=true;
            for(var p:List.of(foot.relative(side),head.relative(side)))if(!local(level,mob,p)||!level.getBlockState(p).isAir()
                    ||!level.getBlockState(p.above()).isAir()||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))clear=false;
            if(clear)return true;
        }
        return false;
    }
    private static String build(ServerLevel level,Pillager mob,CreatureAgents.State agent,State w,long now){
        if(!permitted(Capability.PLACE_CAMP_BED))return null;
        if(w.site!=null&&!site(level,mob,w.site,w.facing))w.site=null;
        if(w.site==null&&now>=w.scanAt){
            w.scanAt=now+100;var candidates=new ArrayList<BlockPos>();var origin=mob.blockPosition();
            for(var p:BlockPos.betweenClosed(origin.offset(-8,-1,-8),origin.offset(8,1,8)))candidates.add(p.immutable());
            candidates.sort(Comparator.comparingDouble(p->p.distSqr(origin)));
            outer:for(var p:candidates)for(var direction:Direction.Plane.HORIZONTAL)if(site(level,mob,p,direction)){
                var path=mob.getNavigation().createPath(p,1,64);
                if(NativeIllagers.safePath(level,mob,path)){w.site=p;w.facing=direction;break outer;}
            }
        }
        if(w.site==null){w.activity="needs sheltered floor space for an owned bed";return null;}
        var p=w.site;var head=p.relative(w.facing);
        if(mob.getEyePosition().distanceToSqr(Vec3.atCenterOf(p))>9||mob.getEyePosition().distanceToSqr(Vec3.atCenterOf(head))>9||!visible(level,mob,p)){
            if(now>=w.pathAt){w.pathAt=now+40;approachBed(level,mob,p,w.facing);}
            return w.activity="carrying an owned bed to shelter";
        }
        var bag=mob.getInventory();int slot=-1;
        for(int i=0;i<bag.getContainerSize();i++)if(CampBeds.bedItem(bag.getItem(i))){slot=i;break;}
        if(slot<0||!site(level,mob,p,w.facing))return null;
        var bed=((BlockItem)bag.getItem(slot).getItem()).getBlock();var footState=bed.defaultBlockState().setValue(BedBlock.FACING,w.facing).setValue(BedBlock.PART,BedPart.FOOT);
        var headState=footState.setValue(BedBlock.PART,BedPart.HEAD);
        if(!level.setBlock(p,footState,Block.UPDATE_CLIENTS))return null;
        bed.setPlacedBy(level,p,footState,mob,bag.getItem(slot));
        if(!level.getBlockState(p).equals(footState)||!level.getBlockState(head).equals(headState)){
            if(level.getBlockState(p).equals(footState))level.removeBlock(p,false);
            if(level.getBlockState(head).equals(headState))level.removeBlock(head,false);return null;
        }
        int before=bag.getItem(slot).getCount();bag.removeItem(slot,1);w.beds++;w.site=null;w.workAt=now+40;
        mob.entityTags().stream().filter(t->t.startsWith(BUILT)).toList().forEach(mob::removeTag);mob.addTag(BUILT+w.beds);
        w.receipt=Map.of("gameTime",now,"foot",p.toShortString(),"head",head.toShortString(),"bedItemsBefore",before,"bedItemsAfter",before-1);
        level.updateNeighborsAt(p,bed);level.gameEvent(mob,GameEvent.BLOCK_PLACE,p);agent.mind.exert(.005f);
        agent.mind.remember("Placed an actual crafted camp bed at "+head.toShortString());return w.activity="placed paid sheltered bedding";
    }
    private static boolean approachBed(ServerLevel level,Pillager mob,BlockPos foot,Direction facing){
        var head=foot.relative(facing);var stands=new ArrayList<BlockPos>();
        for(var side:List.of(facing.getClockWise(),facing.getCounterClockWise()))for(var end:List.of(foot,head)){
            var p=end.relative(side);var eye=Vec3.atBottomCenterOf(p).add(0,mob.getEyeHeight(),0);
            if(local(level,mob,p)&&level.getBlockState(p).isAir()&&level.getBlockState(p.above()).isAir()
                &&level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP)
                &&eye.distanceToSqr(Vec3.atCenterOf(foot))<=9&&eye.distanceToSqr(Vec3.atCenterOf(head))<=9)stands.add(p);
        }
        stands.sort(Comparator.comparingDouble(p->mob.position().distanceToSqr(Vec3.atBottomCenterOf(p))));
        for(var p:stands){var path=mob.getNavigation().createPath(p,0,64);if(NativeIllagers.safePath(level,mob,path))return mob.getNavigation().moveTo(path,.7);}
        return false;
    }
    private NativeCampWork(){}
}
