package local.villagers;

import java.util.*;
import local.villagers.navigation.FoodPassagePlanner;
import local.villagers.mind.AgentMind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Urgent access to existing public hay stores. Natural passage work is finite, native and never structural. */
final class FoodAccess {
    static final int RANGE=48,MAX_ASCENT=48,MAX_DIGS=64,MAX_VISITED=64000;
    static final class State {
        BlockPos store,anchor,table,origin,working;
        String dimension;
        FoodPassagePlanner.Plan plan;
        int step;
        int walkEnd=-1;
        long until,nextSearch,nextPath,localPathAt,started,lastWork,digs,arrivals,settlingWaits;
        int jobDigs;
        boolean edible;
        BlockState block;
        net.minecraft.world.item.ItemStack tool=net.minecraft.world.item.ItemStack.EMPTY;
        String status="no food access trip";
        String lastFailure="none";
        Map<String,Object> lastPlan=Map.of();
        final ArrayDeque<Map<String,Object>> recentWork=new ArrayDeque<>();
    }
    static Map<String,Object> status(Session s){
        var a=s.foodAccess;
        return Map.of("activity",a.status,"digs",a.digs,"arrivals",a.arrivals,"lastPlan",a.lastPlan,"lastFailure",a.lastFailure,
            "settlingWaits",a.settlingWaits,"recentWork",List.copyOf(a.recentWork),"route",Map.of("active",a.plan!=null,
                "step",a.step,"walkEnd",a.walkEnd,"until",a.until,"target",a.store==null?"":a.store.toShortString(),
                "current",a.plan==null||a.step>=a.plan.steps().size()?"":a.plan.steps().get(a.step).feet().toShortString()));
    }
    record Option(BlockPos store,BlockPos table,FoodPassagePlanner.Plan route,boolean edible){
        Option(BlockPos store,BlockPos table,FoodPassagePlanner.Plan route){this(store,table,route,false);}
    }
    private static boolean edible(ServerLevel level,BlockPos p){
        if(!level.isLoaded(p)||!level.getWorldBorder().isWithinBounds(p)||ConstructionSites.isProtected(level,p)||ConstructionSites.isProtected(level,p.below()))return false;
        var b=level.getBlockState(p);
        return (b.is(Blocks.BEETROOTS)||b.is(Blocks.CARROTS)||b.is(Blocks.POTATOES))&&b.getBlock() instanceof CropBlock crop
            &&crop.isMaxAge(b)&&level.getBlockState(p.below()).is(Blocks.FARMLAND);
    }
    static Option preview(ServerLevel level,Villager v){
        return preview(level,v,MAX_DIGS,MAX_VISITED);
    }
    static Option preview(ServerLevel level,Villager v,int maxDigs,int maxVisited){
        boolean ingredients=HayForaging.hasHay(v.getInventory())||BreadCrafting.wheatCount(v.getInventory())>=3;
        var session=Villagers.existingSession(v.getUUID());
        var known=session==null?null:BreadCrafting.rememberedMealTable(level,v);
        if(ingredients&&known!=null){
            var route=plan(level,v,known,maxDigs,maxVisited);
            if(route!=null)return new Option(known,known,route);
        }
        var origin=v.blockPosition();var candidates=new ArrayList<BlockPos>();var observedTables=new ArrayList<BlockPos>();var produce=new ArrayList<BlockPos>();
        for(int x=-RANGE;x<=RANGE;x++)for(int z=-RANGE;z<=RANGE;z++){
            if(x*x+z*z>RANGE*RANGE||!level.isLoaded(origin.offset(x,0,z)))continue;
            for(int y=Math.max(level.getMinY(),origin.getY()-48);y<=origin.getY()+MAX_ASCENT;y++){
                var p=new BlockPos(origin.getX()+x,y,origin.getZ()+z);if(store(level,p))candidates.add(p);
                if(level.getBlockState(p).is(Blocks.CRAFTING_TABLE)&&!ConstructionSites.isProtected(level,p))observedTables.add(p);
                if(edible(level,p))produce.add(p);
            }
        }
        candidates.sort(Comparator.comparingDouble(p->p.distSqr(origin)));
        produce.sort(Comparator.comparingDouble(p->p.distSqr(origin)));
        if(!produce.isEmpty()){
            var route=plan(level,v,produce.stream().limit(64).toList(),maxDigs,maxVisited);
            if(route!=null){
                var stand=route.steps().isEmpty()?origin:route.steps().getLast().feet();
                for(var p:produce)if(reachesHay(level,v,stand,p))return new Option(p,null,route,true);
            }
        }
        if(ingredients){
            observedTables.sort(Comparator.comparingDouble(p->p.distSqr(origin)));
            for(var table:observedTables.stream().filter(p->!p.equals(known)).limit(3).toList()){
                var route=plan(level,v,table,maxDigs,maxVisited);if(route!=null)return new Option(table,table,route);
            }
            // More hay cannot turn already-owned wheat into a meal when every
            // station route failed. Leave room for actual help/other recovery.
            return null;
        }
        // Search the whole observed pile together. The nearest bales can have inaccessible stands
        // while another exposed bale in the same pile is reachable from the descending passage.
        var tables=new LinkedHashMap<BlockPos,BlockPos>();
        for(var target:candidates.stream().limit(64).toList()){
            var table=observedTables.stream().filter(p->Math.abs(p.getX()-target.getX())<=24&&Math.abs(p.getZ()-target.getZ())<=24&&Math.abs(p.getY()-target.getY())<=16)
                .min(Comparator.comparingDouble(p->p.distSqr(target))).orElse(null);
            if(table!=null)tables.put(target,table);
        }
        if(tables.isEmpty())return null;
        var route=plan(level,v,List.copyOf(tables.keySet()),maxDigs,maxVisited);
        if(route==null)return null;
        var stand=route.steps().isEmpty()?v.blockPosition():route.steps().getLast().feet();
        for(var target:tables.keySet())if(reachesHay(level,v,stand,target))return new Option(target,tables.get(target),route);
        return null;
    }
    private static boolean reachesHay(ServerLevel level,Villager v,BlockPos stand,BlockPos hay){
        if(stand.below().equals(hay)||ForestryWork.standEye(v,stand).distanceToSqr(Vec3.atCenterOf(hay))>6.25)return false;
        var hit=ForestryWork.clip(level,v,ForestryWork.standEye(v,stand),hay);
        return hit!=null&&(hit.getType()==HitResult.Type.MISS||hit.getBlockPos().equals(hay));
    }
    static boolean store(ServerLevel level,BlockPos p){
        return level.isLoaded(p)&&level.isLoaded(p.above())&&level.getBlockState(p).is(Blocks.HAY_BLOCK)
            &&level.getBlockState(p.above()).isAir()&&!ConstructionSites.isProtected(level,p);
    }
    static boolean within(ServerLevel level,AgentMind mind,Vec3 p){
        var s=Villagers.existingSession(mind.id());if(s==null)return false;var a=s.foodAccess;
        return a.anchor!=null&&a.origin!=null&&level.dimension().identifier().toString().equals(a.dimension)&&level.getGameTime()<a.until
            &&p.distanceToSqr(Vec3.atCenterOf(a.origin))<=64*64
            &&(p.distanceToSqr(Vec3.atCenterOf(a.anchor))<=16*16
                ||a.plan!=null&&a.plan.steps().stream().anyMatch(n->p.distanceToSqr(Vec3.atBottomCenterOf(n.feet()))<=4*4));
    }
    static boolean active(Villager v){var s=Villagers.existingSession(v.getUUID());return s!=null&&s.foodAccess.anchor!=null&&v.level().getGameTime()<s.foodAccess.until;}
    static boolean preparingGrain(Session s,long now){
        var a=s.foodAccess;
        return a.anchor!=null&&a.plan!=null&&!a.edible&&a.store!=null&&a.store.equals(a.table)&&now<a.until;
    }
    private static void clear(State a){a.store=null;a.anchor=null;a.plan=null;a.started=0;a.walkEnd=-1;}
    /** Reuse this actor's admitted planning batch for ordinary, already-clear walking. */
    private static boolean walkClearStretch(ServerLevel level,Villager v,Session s,long now){
        var a=s.foodAccess;
        if(a.plan==null||a.step>=a.plan.steps().size()
            ||a.plan.steps().get(a.step).dig().stream().anyMatch(p->!level.getBlockState(p).getCollisionShape(level,p).isEmpty()))return false;
        int end=FoodPassagePlanner.clearWalkEnd(a.plan.steps(),a.step,p->!level.getBlockState(p).getCollisionShape(level,p).isEmpty());
        for(int candidate:new LinkedHashSet<>(List.of(end,(a.step+end)/2,a.step))){
            var target=a.plan.steps().get(candidate).feet();
            var path=NativePathLimits.search(v,2,()->WorkNavigation.pathTo(level,v,s,target,0));
            if(WorkNavigation.staysHome(level,v,s,path)&&BoatTravel.dryPath(level,path)&&v.getNavigation().moveTo(path,.6)){
                a.walkEnd=candidate;a.nextPath=now+40;a.localPathAt=now+40;return true;
            }
        }
        return false;
    }
    private static boolean natural(BlockState b){return b.is(Blocks.SAND)||b.is(Blocks.RED_SAND)||b.is(Blocks.SANDSTONE)
        ||b.is(Blocks.RED_SANDSTONE)||b.is(Blocks.DIRT)||b.is(Blocks.GRASS_BLOCK)||b.is(Blocks.STONE);}
    private static List<BlockPos> fallingColumn(ServerLevel level,BlockPos base){
        var blocks=new ArrayList<BlockPos>();var p=base.above();
        while(level.isLoaded(p)&&level.getBlockState(p).getBlock() instanceof FallingBlock){
            if(blocks.size()>=4)return null;blocks.add(p);p=p.above();
        }
        return level.isLoaded(p)?blocks:null;
    }
    static boolean besideColumn(Vec3 feet,AABB body,BlockPos base,int height){
        if(height<1||height>4)return false;var stand=BlockPos.containing(feet);
        return (stand.getX()!=base.getX()||stand.getZ()!=base.getZ())
            &&!body.intersects(new AABB(base).expandTowards(0,height+2,0));
    }
    private static boolean sideColumnSafe(ServerLevel level,Villager v,Vec3 feet,BlockPos base,List<BlockPos> column){
        if(column==null||column.isEmpty()||!level.isLoaded(base.below())
            ||!level.getBlockState(base.below()).isFaceSturdy(level,base.below(),Direction.UP))return false;
        var fall=new AABB(base).expandTowards(0,column.size()+2,0);
        var body=v.getBoundingBox().move(feet.subtract(v.position()));
        if(!besideColumn(feet,body,base,column.size())||!level.getEntities(v,fall.inflate(.5,0,.5)).isEmpty())return false;
        return column.stream().allMatch(p->excavatable(level,v,p,true));
    }
    static boolean excavatable(ServerLevel level,Villager v,BlockPos p){
        return excavatable(level,v,p,false);
    }
    private static boolean excavatable(ServerLevel level,Villager v,BlockPos p,boolean plannedAbove){
        if(!level.isLoaded(p.above(2))||!level.isLoaded(p.below())||!natural(level.getBlockState(p))
                ||!level.getWorldBorder().isWithinBounds(p)||ConstructionSites.isProtected(level,p)
                ||v.getBoundingBox().intersects(new AABB(p))||!level.getEntities(v,new AABB(p).expandTowards(0,2,0)).isEmpty())return false;
        for(Direction d:Direction.values()){
            var q=p.relative(d);if(!level.isLoaded(q)||!level.getFluidState(q).isEmpty())return false;
        }
        if(!plannedAbove&&level.getBlockState(p.above()).getBlock() instanceof FallingBlock
            &&!sideColumnSafe(level,v,v.position(),p,fallingColumn(level,p)))return false;
        for(var q:BlockPos.betweenClosed(p.offset(-1,-1,-1),p.offset(1,2,1))){
            var b=level.getBlockState(q);
            if(b.getBlock() instanceof BedBlock||b.getBlock() instanceof DoorBlock||b.is(Blocks.CRAFTING_TABLE)
                ||b.getBlock() instanceof CropBlock||b.is(Blocks.FARMLAND)||b.is(net.minecraft.tags.BlockTags.PLANKS)||level.getBlockEntity(q)!=null)return false;
        }
        // Native village pieces include the cliff under their paths and houses.
        // A bounded paid food passage may clear plain soil/sandstone there;
        // the physical furnishing/support checks above still apply. Camps and
        // other structures retain their separate, explicit work capabilities.
        return !level.structureManager().getStructureWithPieceAt(p,holder->true).isValid()
            ||level.structureManager().getStructureWithPieceAt(p,net.minecraft.tags.StructureTags.VILLAGE).isValid();
    }
    static FoodPassagePlanner.Plan plan(ServerLevel level,Villager v,BlockPos target){
        return plan(level,v,target,MAX_DIGS,MAX_VISITED);
    }
    static FoodPassagePlanner.Plan plan(ServerLevel level,Villager v,BlockPos target,int maxDigs,int maxVisited){
        return plan(level,v,List.of(target),maxDigs,maxVisited);
    }
    private static FoodPassagePlanner.Plan plan(ServerLevel level,Villager v,List<BlockPos> targets,int maxDigs,int maxVisited){
        if(maxVisited<=0||maxDigs<0)return null;
        var cache=new HashMap<BlockPos,Integer>();var start=v.blockPosition();
        int bottom=Math.min(start.getY(),targets.stream().mapToInt(BlockPos::getY).min().orElse(start.getY()))-2;
        var closest=new BlockPos[]{start};var nearest=new int[]{Integer.MAX_VALUE};
        FoodPassagePlanner.Grid grid=new FoodPassagePlanner.Grid(){
            public void visited(BlockPos p){
                int d=targets.stream().mapToInt(target->Math.max(Math.max(Math.abs(p.getX()-target.getX()),Math.abs(p.getZ()-target.getZ())),Math.abs(p.getY()-target.getY()))).min().orElse(Integer.MAX_VALUE);
                if(d<nearest[0]){nearest[0]=d;closest[0]=p;}
            }
            public int clearance(BlockPos p){return cache.computeIfAbsent(p.immutable(),q->{
                if(!level.isLoaded(q)||!level.getFluidState(q).isEmpty())return -1;
                var b=level.getBlockState(q);
                var shape=b.getCollisionShape(level,q);
                if(shape.isEmpty()||shape.max(Direction.Axis.Y)<=.5||b.is(net.minecraft.tags.BlockTags.WOODEN_DOORS))return 0;
                return excavatable(level,v,q,true)?Math.max(1,(int)Math.ceil(b.getDestroySpeed(level,q)*(b.requiresCorrectToolForDrops()?100:30))):-1;
            });}
            public List<BlockPos> excavation(BlockPos from,List<BlockPos> blocks,Set<BlockPos> cleared){
                var result=new LinkedHashSet<BlockPos>();var eye=ForestryWork.standEye(v,from);
                for(var p:blocks){
                    var falling=new ArrayList<BlockPos>();var above=p.above();
                    while(level.isLoaded(above)&&!cleared.contains(above)&&level.getBlockState(above).getBlock() instanceof FallingBlock&&!result.contains(above)){
                        if(falling.size()>=4||clearance(above)<=0)return null;
                        falling.add(above);above=above.above();
                    }
                    if(sideColumnSafe(level,v,Vec3.atBottomCenterOf(from),p,falling)){
                        // The real falling ticker refills the same lower cell.
                        // Keep all original blocks in the paid footprint; the
                        // executor waits for settling and clears each refill.
                        result.add(p);result.addAll(falling);continue;
                    }
                    if(falling.stream().anyMatch(q->eye.distanceToSqr(Vec3.atCenterOf(q))>6.25))return null;
                    Collections.reverse(falling);result.addAll(falling);result.add(p);
                }
                return List.copyOf(result);
            }
            public boolean support(BlockPos p){
                if(!level.isLoaded(p)||!level.getFluidState(p).isEmpty()||!FloorCentering.safeSupport(level.getBlockState(p)))return false;
                var shape=level.getBlockState(p).getCollisionShape(level,p);
                return !shape.isEmpty()&&shape.max(Direction.Axis.Y)>.0;
            }
            public boolean allowed(BlockPos p){return p.distSqr(start)<=64*64&&p.getY()>=bottom&&p.getY()<=start.getY()+MAX_ASCENT
                &&level.getWorldBorder().isWithinBounds(p)&&level.isLoaded(p.above());}
        };
        var goals=new HashSet<BlockPos>();
        for(var target:targets)for(var p:BlockPos.betweenClosed(target.offset(-2,-1,-2),target.offset(2,1,2)))
            if(reachesHay(level,v,p,target)&&grid.clearance(p)==0&&grid.clearance(p.above())==0&&grid.support(p.below()))goals.add(p.immutable());
        int firstBudget=Math.max(1,maxVisited/2);
        var result=FoodPassagePlanner.find(grid,start,goals,maxDigs,firstBudget);
        boolean directedRetry=result==null&&maxVisited>firstBudget;
        if(directedRetry)result=FoodPassagePlanner.find(grid,start,goals,maxDigs,maxVisited-firstBudget,32);
        var edge=new ArrayList<Map<String,Object>>();
        if(result==null)for(Direction d:Direction.Plane.HORIZONTAL){
            var p=closest[0].relative(d).below();
            edge.add(Map.of("feet",p.toShortString(),"feetBlock",level.getBlockState(p).toString(),"feetClearance",grid.clearance(p),
                "headClearance",grid.clearance(p.above()),"support",grid.support(p.below()),"structure",level.structureManager().getStructureWithPieceAt(p,h->true).isValid()));
        }
        var s=Villagers.existingSession(v.getUUID());
        if(s!=null)s.foodAccess.lastPlan=Map.of("targets",targets.stream().map(BlockPos::toShortString).toList(),"goals",goals.stream().map(BlockPos::toShortString).toList(),
            "cachedCells",cache.size(),"diggable",cache.values().stream().filter(n->n>0).count(),"clear",cache.values().stream().filter(n->n==0).count(),"found",result!=null,
            "closest",closest[0].toShortString(),"distance",nearest[0],"edge",edge,"directedRetry",directedRetry);
        return result;
    }
    static String work(ServerLevel level,Villager v,Session s,LivingEntity threat,boolean localFood,long now){
        var a=s.foodAccess;
        boolean healingMeal=s.mind.foodLevel()<18&&v.getHealth()<v.getMaxHealth();
        boolean urgent=s.mind.foodLevel()<=6||healingMeal;
        if(!NativeRegionsScenario.nativeRegions(level)||!Villagers.config().hayForaging()
                ||!AdventureActions.PERMISSIONS.allows(local.villagers.capability.CapabilityPermissions.Capability.DIG_FOOD_PASSAGE)
                ||threat!=null||v.isOnFire()||v.isTrading()||v.isSleeping()&&!urgent||v.isPassenger()
                ||v.isInWater()||v.getAirSupply()<200||VillageSurvival.scheduledRest(level,v)&&!urgent||v.isBaby()){
            clear(a);return null;
        }
        if(a.anchor!=null&&now>=a.until){clear(a);a.nextSearch=now+200;}
        boolean ingredients=HayForaging.hasHay(v.getInventory())||BreadCrafting.wheatCount(v.getInventory())>=3;
        if(a.anchor==null){
            // Observation includes grain with a remembered table as "food" even
            // when ordinary navigation cannot reach that table from this floor.
            // Empty-handed residents on a cliff need time to reach a store and
            // its workbench. Start while fed enough to finish the actual work,
            // rather than wait until six more hunger points have been spent.
            // Nearby perceived food may still require the surveyed passage.
            // Once hungry, do not let that perception veto the actual route.
            if(localFood&&!ingredients&&!urgent||s.mind.foodLevel()>16&&!healingMeal||FoodInventory.anySlot(v.getInventory(),AdventureActions.canHarvest(v))>=0
                    ||now<a.nextSearch||!WorkNavigation.admit(level,now,v))return null;
            a.nextSearch=now+400;var origin=v.blockPosition();var option=preview(level,v);
            if(option!=null&&v.isSleeping()){
                // Native waking moves the body off the bed and changes its collision box/eye height.
                // The sleep-pose route authorizes waking, but cannot be executed as a standing route.
                v.stopSleeping();origin=v.blockPosition();option=preview(level,v);
            }
            if(option!=null){
                var target=option.store();var table=option.table();var route=option.route();
                a.store=target;a.anchor=target;a.table=table;a.edible=option.edible();a.origin=origin.immutable();a.dimension=level.dimension().identifier().toString();a.plan=route;a.step=0;a.walkEnd=-1;a.until=now+3000;a.jobDigs=0;
                a.lastPlan=Map.of("gameTime",now,"store",target.toShortString(),"table",table==null?"":table.toShortString(),"edible",a.edible,"steps",route.steps().size(),"digs",route.digCount(),"visited",route.visited());
                s.mind.remember("Seeking "+(a.edible?"actual ripe village crops":"existing village hay or a workbench")+" at "+target.toShortString()+" with a bounded natural passage plan");
                // Preview already owns this body's fair planning admission.
                // Start the clear native leg now rather than replacing a
                // 26-block meal walk with one small query every40ticks.
                if(walkClearStretch(level,v,s,now))return a.status="walking the clear food passage";
            }
            if(a.store==null){a.status="no safe food passage found";return null;}
        }
        if(ingredients&&!a.edible){
            BreadCrafting.rememberTable(level,v,s,a.table);
            // Owning grain does not mean the workbench is physically accessible.
            // Continue the surveyed passage out of the blocked area before yielding.
            if(BreadCrafting.canWorkAt(level,v,s,a.table)){
                // Keep this surveyed meal mission alive through the actual
                // recipe. Clearing it first revoked work authority at an
                // earned station outside the ordinary home radius, causing
                // the hungry owner to leave without using its own wheat.
                v.getNavigation().stop();
                if(BreadCrafting.wheatCount(v.getInventory())<3&&HayForaging.hasHay(v.getInventory()))HayForaging.unpack(level,v,s);
                if(BreadCrafting.tryCraft(level,v,s,a.table)){
                    clear(a);a.nextSearch=now;return "crafted real bread at the reached meal workbench";
                }
                return a.status="preparing owned grain at the reached meal workbench";
            }
            if(!a.store.equals(a.table)){
                // The hay ledge and the village workbench can be separated by
                // another cliff. An owner needs a route to that actual table,
                // rather than another zero-length trip to the emptied pile.
                var route=plan(level,v,a.table);
                if(route==null||route.digCount()+a.jobDigs>MAX_DIGS){clear(a);a.nextSearch=now+100;return null;}
                a.store=a.anchor=a.table;a.plan=route;a.step=0;a.walkEnd=-1;a.started=0;a.until=now+3000;
                a.status="opening a bounded route from owned grain to the known workbench";
            }
        }
        if(s.mind.foodLevel()>=14&&FoodSupply.deficit(v.getInventory(),AdventureActions.canHarvest(v))==0){
            clear(a);return null;
        }
        if(a.edible&&!edible(level,a.store)){
            a.lastFailure="food target no longer ripe or available at "+a.store.toShortString()+": "+level.getBlockState(a.store);
            clear(a);a.nextSearch=now+20;return null;
        }
        boolean workbench=!a.edible&&a.store.equals(a.table);
        if(workbench&&!level.getBlockState(a.store).is(Blocks.CRAFTING_TABLE)){clear(a);a.nextSearch=now+100;return null;}
        if(!a.edible&&!workbench&&!store(level,a.store)){
            BlockPos next=null;double best=Double.MAX_VALUE;
            for(var p:BlockPos.betweenClosed(a.anchor.offset(-6,-3,-6),a.anchor.offset(6,3,6)))if(store(level,p)){
                double d=p.distSqr(v.blockPosition());if(d<best){next=p.immutable();best=d;}
            }
            if(next==null){clear(a);a.nextSearch=now+100;return null;}
            a.store=next;a.plan=plan(level,v,next);a.step=0;a.walkEnd=-1;
            if(a.plan==null||a.plan.digCount()+a.jobDigs>MAX_DIGS){clear(a);return null;}
        }
        if(v.getEyePosition().distanceToSqr(Vec3.atCenterOf(a.store))<=9&&BoatRoutes.visible(level,v,Vec3.atCenterOf(a.store))){
            if(a.edible){
                a.status="harvesting reachable ripe produce with native replanting costs";
                if(AdventureActions.tryHarvest(level,v,a.store)){a.arrivals++;clear(a);a.nextSearch=now+20;return "harvested actual edible crops after reaching the field";}
                return a.status;
            }
            if(ingredients&&workbench){
                // Center distance alone cannot finish a station mission;
                // the actual recipe and interaction checks above must pass.
                a.status="approaching actual workbench interaction reach";
            }
            if(!workbench){
                s.hayTarget=a.store;a.status="collecting existing village hay";
                if(HayForaging.gather(level,v,s,a.store,now)){a.arrivals++;BreadCrafting.rememberTable(level,v,s,a.table);a.status="gathered real hay after reaching the store";}
                return a.status;
            }
        }
        if(a.step>=a.plan.steps().size()){a.lastFailure="plan ended outside hay reach at "+v.position();clear(a);a.nextSearch=now+20;return null;}
        if(a.walkEnd>=a.step&&v.position().distanceToSqr(Vec3.atBottomCenterOf(a.plan.steps().get(a.walkEnd).feet()))<.65*.65){
            a.step=a.walkEnd+1;a.walkEnd=-1;a.started=0;return "walked the clear food passage";
        }
        var step=a.plan.steps().get(a.step);
        for(var p:step.dig())if(!level.getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class,
            new AABB(p).expandTowards(0,5,0)).isEmpty()){
            v.getNavigation().stop();a.started=0;a.settlingWaits++;return a.status="waiting for native sand to settle before continuing";
        }
        if(v.position().distanceToSqr(Vec3.atBottomCenterOf(step.feet()))<.65*.65){a.step++;a.started=0;return "walking the food passage";}
        for(var p:step.dig())if(!level.getBlockState(p).getCollisionShape(level,p).isEmpty()){
            var contact=BlockContact.point(level,v,v.getEyePosition(),p,3);
            if(!excavatable(level,v,p)||contact==null){
                a.lastFailure="dig rejected at "+p.toShortString()+" from "+v.position()+": safe="+excavatable(level,v,p)+", reach="+v.getEyePosition().distanceToSqr(Vec3.atCenterOf(p))+", visible="+BoatRoutes.visible(level,v,Vec3.atCenterOf(p));
                clear(a);a.nextSearch=now+100;return null;
            }
            v.getNavigation().stop();var block=level.getBlockState(p);
            if(!p.equals(a.working)||a.started==0||now-a.lastWork>20||!block.equals(a.block)
                    ||!net.minecraft.world.item.ItemStack.matches(a.tool,v.getMainHandItem())){a.working=p;a.started=now;a.block=block;a.tool=v.getMainHandItem().copy();}
            a.lastWork=now;v.swingForAttack(InteractionHand.MAIN_HAND);
            boolean harvest=!block.requiresCorrectToolForDrops()||v.getMainHandItem().isCorrectToolForDrops(block);
            int duration=(int)Math.ceil(block.getDestroySpeed(level,p)*(harvest?30:100)/Math.max(1,v.getMainHandItem().getDestroySpeed(block)));
            if(now-a.started<Math.max(1,duration))return a.status="digging a natural step to reach food";
            if(a.jobDigs>=MAX_DIGS){clear(a);a.nextSearch=now+400;return "completed the bounded food excavation allowance";}
            var drops=harvest?Block.getDrops(block,level,p,null,v,v.getMainHandItem()):List.<net.minecraft.world.item.ItemStack>of();
            var dropNames=drops.stream().map(Object::toString).toList();
            if(level.getBlockState(p).equals(block)&&excavatable(level,v,p)
                &&BlockContact.point(level,v,v.getEyePosition(),p,3)!=null&&level.destroyBlock(p,false,v)){
                for(var drop:drops){var rest=v.getInventory().addItem(drop);if(!rest.isEmpty())Block.popResource(level,p,rest);}
                var tool=v.getMainHandItem().get(DataComponents.TOOL);if(tool!=null)v.getMainHandItem().hurtAndBreak(tool.damagePerBlock(),v,EquipmentSlot.MAINHAND);
                Villagers.exert(v,.005f);a.digs++;a.jobDigs++;a.started=0;
                a.recentWork.addLast(Map.of("gameTime",now,"position",p.toShortString(),"block",block.toString(),
                    "drops",dropNames,"contact",contact.toString(),"jobDigs",a.jobDigs));
                while(a.recentWork.size()>8)a.recentWork.removeFirst();
            }
            return a.status="cleared a paid natural food step";
        }
        if(a.walkEnd>a.step&&!v.getNavigation().isDone()&&BoatTravel.dryPath(level,v.getNavigation().getPath()))
            return a.status="following the clear native meal route";
        if(now>=a.nextPath&&WorkNavigation.admit(level,now,v)){
            a.nextPath=now+40;
            if(walkClearStretch(level,v,s,now))return a.status="walking the clear food passage";
            a.lastFailure="native clear walking stretch rejected from "+v.position()+" toward "+step.feet().toShortString();
            clear(a);a.nextSearch=now+100;return null;
        }
        // Heavy passage searches share the global budget. An urgent body can
        // still take one short, already-cleared native step every two seconds
        // instead of starving while repeatedly waiting for a fresh reservation.
        if(urgent&&now>=a.localPathAt&&step.feet().distSqr(v.blockPosition())<=16){
            a.localPathAt=now+40;
            var local=NativePathLimits.search(v,1,()->v.getNavigation().createPath(step.feet(),0,16));
            if(local!=null&&local.getNodeCount()<=16&&WorkNavigation.staysHome(level,v,s,local)
                &&BoatTravel.dryPath(level,local)&&v.getNavigation().moveTo(local,.6)){
                a.walkEnd=a.step;a.nextPath=now+40;return a.status="following the cleared next step toward a real meal";
            }
        }
        return a.status="walking toward village food supplies";
    }
    private FoodAccess(){}
}
