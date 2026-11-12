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
    private static final String FOOD_STORE="villagers:camp_food_store:";
    static final class State {
        BlockPos target,site,survey;Direction facing;
        long scanAt,pathAt,workAt,targetUntil,surveyUntil;
        int transfers,recipes,beds,inspections,fallenBeds;
        int routeFailures;
        net.minecraft.world.level.pathfinder.Path activeStoreRoute;BlockPos activeStore;
        final local.villagers.navigation.RouteProgress storeProgress=new local.villagers.navigation.RouteProgress(120);
        java.util.UUID fallenTarget;BlockPos fallenStand;long fallenUntil;
        boolean loaded;
        String activity="no camp work";
        String route="none";
        final Map<BlockPos,Long> inspected=new HashMap<>();
        final Map<BlockPos,Integer> visited=new HashMap<>();
        final CampBeds.State reusedBeds=new CampBeds.State();
        final NativeCampAccess.State access=new NativeCampAccess.State();
        Map<String,Object> receipt=Map.of();
        Map<String,Object> storageRoute=Map.of();
        Map<String,Object> bedApproach=Map.of();
        long nextFallenAudit;
        Map<String,Object> fallenAudit=Map.of(),fallenRoute=Map.of();
        Map<String,Object> status(){var result=new LinkedHashMap<String,Object>(Map.of("activity",activity,"transfers",transfers,"recipes",recipes,"beds",beds,"lastReceipt",receipt,
            "target",target==null?"":target.toShortString(),"route",route,"inspections",inspections,
            "reusedBeds",Map.of("collected",reusedBeds.moved,"lastReceipt",reusedBeds.receipt,"activity",reusedBeds.status,"routeRejections",reusedBeds.rejections),
            "access",NativeCampAccess.status(access)));
            result.put("storageRoute",storageRoute);result.put("bedSite",site==null?"":site.toShortString());result.put("bedApproach",bedApproach);result.put("fallenBeds",fallenBeds);
            result.put("fallenAudit",fallenAudit);result.put("fallenRoute",fallenRoute);return result;}
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
        if(NativeCampFarms.preparing((ServerLevel)mob.level(),mob)){
            int farm=NativeCampFarms.wanted(bag,NativeCampFarms.ownsHoe(mob),item);if(farm>0)return farm;
        }
        if(!housing)return 0;
        if(item.is(Items.STRING))return Math.max(0,12-bag.countItem(Items.STRING)-4*bag.countItem(Items.WOOL.white()));
        for(var wool:Items.WOOL.asList())if(item.is(wool))return Math.max(0,3-bag.countItem(wool));
        if(WoodCrafting.raw(item))return planks(bag)>=3?0:Math.max(0,1-raw(bag));
        if(WoodCrafting.plank(item))return Math.max(0,3-planks(bag));
        return 0;
    }
    /** One physical container interaction may collect matching food stacks.
     * Stage both inventories so overflow or a stale source cannot create food. */
    static int takeFoodBatch(Container source,Container bag,ItemStack item,int maximum){
        if(!FoodInventory.supported(item)||maximum<=0)return 0;
        var beforeSource=HarvestMaterials.snapshot(source);var beforeBag=HarvestMaterials.snapshot(bag);
        var nextSource=new net.minecraft.world.SimpleContainer(source.getContainerSize());
        var nextBag=new net.minecraft.world.SimpleContainer(bag.getContainerSize());
        for(int i=0;i<beforeSource.size();i++)nextSource.setItem(i,beforeSource.get(i).copy());
        for(int i=0;i<beforeBag.size();i++)nextBag.setItem(i,beforeBag.get(i).copy());
        int moved=0;
        for(int i=0;i<nextSource.getContainerSize()&&moved<maximum;i++){
            var stack=nextSource.getItem(i);if(!ItemStack.isSameItemSameComponents(stack,item))continue;
            int count=Math.min(stack.getCount(),maximum-moved);
            var left=nextBag.addItem(stack.copyWithCount(count));int actual=count-left.getCount();
            if(actual>0){stack.shrink(actual);moved+=actual;}
        }
        if(moved==0||!HarvestMaterials.unchanged(source,beforeSource)||!HarvestMaterials.unchanged(bag,beforeBag))return 0;
        for(int i=0;i<nextSource.getContainerSize();i++)source.setItem(i,nextSource.getItem(i));
        for(int i=0;i<nextBag.getContainerSize();i++)bag.setItem(i,nextBag.getItem(i));
        source.setChanged();return moved;
    }
    private static boolean take(ServerLevel level,Pillager mob,State w,BlockPos p,boolean housing,long now){
        if(!permitted(Capability.OPEN_CAMP_STORAGE)||BlockContact.interactionPoint(level,mob,mob.getEyePosition(),p,3)==null)return false;
        var source=storage(level,mob,p);if(source==null)return false;
        w.inspections++;
        var contents=new ArrayList<String>();for(int i=0;i<source.getContainerSize();i++)if(!source.getItem(i).isEmpty())contents.add(source.getItem(i).toString());
        boolean foodHere=false;for(int i=0;i<source.getContainerSize();i++)if(FoodInventory.supported(source.getItem(i)))foodHere=true;
        String foodTag=FOOD_STORE+p.asLong();
        if(foodHere&&mob.entityTags().stream().filter(t->t.startsWith(FOOD_STORE)).count()<16)mob.addTag(foodTag);
        if(!foodHere)mob.removeTag(foodTag);
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
            if(FoodInventory.supported(item)){
                var kind=item.getItem();int before=source.countItem(kind);
                int moved=takeFoodBatch(source,mob.getInventory(),item,wanted(mob,item,housing));if(moved==0)continue;
                w.transfers++;w.receipt=Map.of("gameTime",now,"container",p.toShortString(),"item",kind.toString(),"sourceBefore",before,"sourceAfter",source.countItem(kind),"moved",moved);
                level.gameEvent(mob,GameEvent.CONTAINER_OPEN,p);mob.swingForAttack(net.minecraft.world.InteractionHand.MAIN_HAND);
                CreatureAgents.mind(mob).remember("Took "+moved+" actual food items from inspected camp storage "+p.toShortString());return true;
            }
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
                ||mob.isOnFire()||mob.isInWater()||mob.getHealth()<mob.getMaxHealth()*.9||agent.mind.fatigue()>.65)return null;
        var w=agent.campWork;var bag=mob.getInventory();
        IllagerFood.openRouteDoors(level,mob,agent.foodWork);
        if(!w.loaded){
            for(var tag:mob.entityTags())if(tag.startsWith(BUILT))try{w.beds=Math.clamp(Integer.parseInt(tag.substring(BUILT.length())),0,2);}catch(NumberFormatException ignored){}
            w.loaded=true;
        }
        boolean housing=Villagers.config().breeding()&&w.beds<2&&(IllagerFamilies.needsHousing(level,mob)||bag.hasAnyMatching(CampBeds::bedItem));
        // Other meals sustain the body, but cannot pay the native family bread
        // cost. Prospective parents still provision actual bread.
        boolean food=IllagerFood.bread(mob)<6&&(edible(bag)<6||Villagers.config().breeding());
        boolean farming=NativeCampFarms.preparing(level,mob)&&(!NativeCampFarms.ownsHoe(mob)||bag.countItem(Items.WHEAT_SEEDS)<2);
        var home=agent.mind.home();
        if(IllagerFood.bread(mob)>=6&&home!=null&&home.dimension().equals(level.dimension().identifier().toString())
                &&mob.distanceToSqr(home.x(),home.y(),home.z())>48*48&&!agent.campMeals.patrolling(now)
                &&NativeIllagers.returnHome(level,mob,new BlockPos(home.x(),home.y(),home.z())))return w.activity="bringing supplies back to known housing";
        if(now<w.workAt)return w.target!=null?w.activity:null;
        if(housing&&agent.mind.foodLevel()>=14){
            if(bag.hasAnyMatching(CampBeds::bedItem)){
                var building=build(level,mob,agent,w,now);
                if(building!=null)return building;
                // A carried bed with no suitable floor must not prevent a
                // provisioner from collecting the real food it still needs.
                housing=false;
            }else{
            String fallen=collectFallenBed(level,mob,w,now);if(fallen!=null)return w.activity=fallen;
            String reused=CampBeds.work(level,mob,agent,w.reusedBeds,now);
            if(reused!=null)return w.activity=reused;
            String passage=NativeCampAccess.work(level,mob,agent,w.reusedBeds,w.access,now);
            if(passage!=null)return w.activity=passage;
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
        }
        if(!food&&!housing&&!farming){w.target=null;return null;}
        var active=mob.getNavigation().getPath();
        boolean continuingStore=w.target!=null&&w.target.equals(w.activeStore)&&active!=null&&active==w.activeStoreRoute&&!active.isDone();
        if(continuingStore){
            if(!w.storeProgress.stalledAdvance(w.target.toShortString(),now,active.getNextNodeIndex()))w.targetUntil=Math.max(w.targetUntil,now+120);
            else{NativeBodyRoutes.failedStep(mob,active.getNextNodePos(),now);mob.getNavigation().stop();w.activeStoreRoute=null;w.pathAt=now;continuingStore=false;}
        }
        if(w.target!=null&&(now>w.targetUntil||storage(level,mob,w.target)==null)){w.inspected.put(w.target,now+200);w.target=null;w.routeFailures=0;}
        if(w.target==null&&now>=w.scanAt){
            w.scanAt=now+100;w.inspected.entrySet().removeIf(e->e.getValue()<=now);
            var origin=mob.blockPosition();double nearest=Double.MAX_VALUE;
            if(food)for(var tag:mob.entityTags())if(tag.startsWith(FOOD_STORE))try{
                var p=BlockPos.of(Long.parseLong(tag.substring(FOOD_STORE.length())));
                if(w.inspected.containsKey(p)||storage(level,mob,p)==null)continue;
                double d=p.distSqr(origin);if(d<nearest){nearest=d;w.target=p;}
            }catch(NumberFormatException ignored){}
            if(w.target==null)for(var p:BlockPos.betweenClosed(origin.offset(-12,-3,-12),origin.offset(12,3,12))){
                if(w.inspected.containsKey(p)||!local(level,mob,p)||storage(level,mob,p)==null
                    ||!visible(level,mob,p))continue;
                double d=p.distSqr(origin);if(d<nearest){nearest=d;w.target=p.immutable();}
            }
            w.targetUntil=now+1200;w.routeFailures=0;
        }
        if(w.target==null)return survey(level,mob,w,now);
        var p=w.target;
        if(BlockContact.interactionPoint(level,mob,mob.getEyePosition(),p,3)!=null){
            mob.getNavigation().stop();
            if(take(level,mob,w,p,housing,now)){w.workAt=now+30;return w.activity="collecting real camp supplies";}
            w.inspected.put(p,now+4000);w.target=null;return null;
        }
        if(!mob.onGround())return w.activity="finishing a native step before the camp-store route";
        if(continuingStore){
            if(food)NativeCampMeals.hurryForCriticalSupplies(mob,agent,now);
            return w.activity="following the native route to the selected camp store";
        }
        if(now>=w.pathAt){
            w.pathAt=now+40;
            var path=IllagerFood.approachPath(level,mob,p);
            boolean moved=path!=null&&mob.getNavigation().moveTo(path,.8);
            w.storageRoute=Map.of("gameTime",now,"from",mob.position().toString(),"store",p.toShortString(),
                "nodes",path==null?0:path.getNodeCount(),"moved",moved,"failures",w.routeFailures);
            if(!moved){
                w.route="retrying bounded native storage stands";
                if(++w.routeFailures<3)return w.activity="rechecking another safe route to the remembered store";
                mob.getNavigation().stop();w.inspected.put(p,now+200);w.target=null;w.routeFailures=0;return null;
            }
            w.routeFailures=0;w.activeStoreRoute=mob.getNavigation().getPath();w.activeStore=p;w.storeProgress.reset();
            if(food)NativeCampMeals.hurryForCriticalSupplies(mob,agent,now);
            w.route="native storage path started";
        }
        return w.activity="walking to camp storage";
    }
    static String recoverBed(ServerLevel level,Mob body,CreatureAgents.State actor,long now){
        if(!(body instanceof Pillager mob)||!NativeIllagers.member(mob)||IllagerFamilies.juvenile(mob)||!Villagers.config().breeding()
            ||!actor.campWork.loaded||actor.campWork.beds>=2
            ||actor.mind.foodLevel()<14||actor.mind.fatigue()>.65||mob.getHealth()<mob.getMaxHealth()*.9||mob.isInWater()||mob.isOnFire()
            ||mob.getInventory().hasAnyMatching(CampBeds::bedItem))return null;
        return collectFallenBed(level,mob,actor.campWork,now);
    }
    private static String collectFallenBed(ServerLevel level,Pillager mob,State w,long now){
        if(now>=w.nextFallenAudit){
            w.nextFallenAudit=now+40;
            var observed=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,mob.getBoundingBox().inflate(12),
                item->item.isAlive()&&CampBeds.bedItem(item.getItem()));
            // Retain the last actual sighting so expiry cannot erase why pickup failed.
            if(!observed.isEmpty())w.fallenAudit=Map.of("gameTime",now,"actor",mob.position().toString(),
                "items",observed.stream().limit(4).map(item->{
                    var point=item.position().add(0,.1,0);
                    var hit=level.clip(new net.minecraft.world.level.ClipContext(mob.getEyePosition(),point,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,mob));
                    var row=new LinkedHashMap<String,Object>();row.put("id",item.getUUID().toString());row.put("position",item.position().toString());
                    row.put("distance",mob.distanceTo(item));row.put("pickupDelay",item.hasPickUpDelay());row.put("owned",item.getOwner()!=null);
                    row.put("named",item.getItem().has(DataComponents.CUSTOM_NAME));row.put("local",local(level,mob,item.blockPosition()));
                    row.put("inCamp",NativeIllagers.insideCamp(level,mob,item.blockPosition()));row.put("fits",mob.getInventory().canAddItem(item.getItem().copyWithCount(1)));
                    row.put("hit",hit.getType().name());row.put("hitBlock",hit.getBlockPos().toShortString());return row;
                }).toList());
        }
        var current=mob.getNavigation().getPath();
        if(w.fallenTarget!=null&&now<w.fallenUntil&&current!=null&&!current.isDone()&&w.fallenStand!=null&&current.getTarget().equals(w.fallenStand))
            return "following the native route to a real fallen bed";
        var items=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,mob.getBoundingBox().inflate(12),
            item->item.isAlive()&&!item.hasPickUpDelay()&&item.getOwner()==null&&CampBeds.bedItem(item.getItem())
                &&!item.getItem().has(DataComponents.CUSTOM_NAME)&&local(level,mob,item.blockPosition())&&NativeIllagers.insideCamp(level,mob,item.blockPosition()));
        items.sort(Comparator.comparingDouble(mob::distanceToSqr));
        for(var item:items.stream().limit(3).toList()){
            if(!mob.getInventory().canAddItem(item.getItem().copyWithCount(1)))continue;
            var point=item.position().add(0,.1,0);var hit=level.clip(new net.minecraft.world.level.ClipContext(mob.getEyePosition(),point,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,mob));
            if(hit.getType()!=HitResult.Type.MISS)continue;
            if(mob.distanceToSqr(item)<=2.25){
                int before=item.getItem().getCount();var taken=item.getItem().copyWithCount(1);
                if(!mob.getInventory().addItem(taken).isEmpty())continue;
                item.getItem().shrink(1);if(item.getItem().isEmpty())item.discard();w.transfers++;w.fallenBeds++;w.fallenTarget=null;
                w.receipt=Map.of("gameTime",now,"itemEntity",item.getUUID().toString(),"collectedBed",taken.toString(),"before",before,"after",before-1);
                return "collected a real fallen camp bed";
            }
            if(now>=w.pathAt){
                w.pathAt=now+40;var stands=new ArrayList<BlockPos>();
                for(var p:BlockPos.betweenClosed(item.blockPosition().offset(-1,-1,-1),item.blockPosition().offset(1,1,1))){
                    if(!local(level,mob,p)||!level.isLoaded(p.above())||!level.isLoaded(p.below())||!level.getFluidState(p).isEmpty())continue;
                    double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,p);
                    var feet=new Vec3(p.getX()+.5,floor,p.getZ()+.5);var body=mob.getBoundingBox().move(feet.subtract(mob.position()));
                    if(feet.distanceToSqr(item.position())>2.25||!level.noCollision(mob,body)||level.noCollision(mob,body.move(0,-.03,0)))continue;
                    var view=level.clip(new net.minecraft.world.level.ClipContext(feet.add(0,mob.getEyeHeight(),0),point,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,mob));
                    if(view.getType()==HitResult.Type.MISS)stands.add(p.immutable());
                }
                var path=CampBeds.pathToStands(level,mob,stands);
                w.fallenRoute=Map.of("gameTime",now,"item",item.getUUID().toString(),"stands",stands.stream().map(BlockPos::toShortString).toList(),
                    "nodes",path==null?0:path.getNodeCount(),"reaches",path!=null&&path.canReach(),
                    "end",path==null||path.getEndNode()==null?"":path.getEndNode().asBlockPos().toShortString());
                if(NativeIllagers.safePath(level,mob,path)){
                    if(mob.getNavigation().isDone())mob.getNavigation().stop();mob.getNavigation().moveTo(path,.7);
                    w.fallenTarget=item.getUUID();w.fallenStand=path.getTarget();w.fallenUntil=now+800;
                    return "walking to a real fallen camp bed";
                }
            }
        }
        return null;
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
                if(local(level,mob,p)&&NativeIllagers.insideCamp(level,mob,p)&&level.isLoaded(p.above())&&level.getBlockState(p).getCollisionShape(level,p).isEmpty()
                    &&level.getBlockState(p.above()).getCollisionShape(level,p.above()).isEmpty()&&level.getFluidState(p).isEmpty()&&level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))candidates.add(p);
            }
            candidates.sort(Comparator.<BlockPos>comparingInt(p->w.visited.getOrDefault(cell(p),0)).thenComparingDouble(p->-p.distSqr(origin)));
            for(var p:candidates.stream().limit(6).toList()){
                var path=CampBeds.pathToStands(level,mob,List.of(p));
                if(NativeIllagers.safePath(level,mob,path)&&mob.getNavigation().moveTo(path,.7)){
                    w.survey=p;w.surveyUntil=now+400;w.pathAt=now+40;break;
                }
                w.visited.merge(cell(p),1,Integer::sum);
            }
            if(w.survey==null){mob.getNavigation().stop();w.activity="no safe camp supply survey route";return null;}
        }else if(now>=w.pathAt){
            w.pathAt=now+40;var path=CampBeds.pathToStands(level,mob,List.of(w.survey));
            if(!NativeIllagers.safePath(level,mob,path)||!mob.getNavigation().moveTo(path,.7)){mob.getNavigation().stop();w.visited.merge(cell(w.survey),1,Integer::sum);w.survey=null;return null;}
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
                // Reserve a site only with a complete route to an actual aisle
                // stand; arrival in the future bed footprint would invalidate it.
                var path=bedPath(level,mob,p,direction);
                if(NativeIllagers.safePath(level,mob,path)){w.site=p;w.facing=direction;break outer;}
            }
        }
        if(w.site==null){w.activity="needs sheltered floor space for an owned bed";return null;}
        var p=w.site;var head=p.relative(w.facing);
        if(mob.getEyePosition().distanceToSqr(Vec3.atCenterOf(p))>9||mob.getEyePosition().distanceToSqr(Vec3.atCenterOf(head))>9||!visible(level,mob,p)){
            if(now>=w.pathAt){
                w.pathAt=now+40;var path=bedPath(level,mob,p,w.facing);
                boolean moved=path!=null&&mob.getNavigation().moveTo(path,.7);
                w.bedApproach=Map.of("gameTime",now,"actor",mob.position().toString(),"site",p.toShortString(),
                    "accepted",moved,"nodes",path==null?0:path.getNodeCount(),"end",path==null?"":path.getEndNode().asBlockPos().toShortString());
            }
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
    private static net.minecraft.world.level.pathfinder.Path bedPath(ServerLevel level,Pillager mob,BlockPos foot,Direction facing){
        var head=foot.relative(facing);var stands=new ArrayList<BlockPos>();
        for(var side:List.of(facing.getClockWise(),facing.getCounterClockWise()))for(var end:List.of(foot,head)){
            var p=end.relative(side);var eye=Vec3.atBottomCenterOf(p).add(0,mob.getEyeHeight(),0);
            if(local(level,mob,p)&&level.getBlockState(p).isAir()&&level.getBlockState(p.above()).isAir()
                &&level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP)
                &&eye.distanceToSqr(Vec3.atCenterOf(foot))<=9&&eye.distanceToSqr(Vec3.atCenterOf(head))<=9)stands.add(p);
        }
        return CampBeds.pathToStands(level,mob,stands);
    }
    private NativeCampWork(){}
}
