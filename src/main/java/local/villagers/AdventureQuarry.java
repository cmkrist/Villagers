package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** One observed natural-soil worksite: four descending steps and three work bays, at most16 soil blocks. */
final class AdventureQuarry {
    private static final String TAG="villagers:quarry:";
    private static final String FAILED="villagers:quarry_failed:";
    private static final String RETREAT="villagers:quarry_retreat";
    private static final long REVIEW_AFTER=24000;
    record FailedSite(BlockPos root,long observedAt){
        boolean expired(long now){return now>=observedAt&&now-observedAt>=REVIEW_AFTER;}
        String tag(){return FAILED+root.asLong()+":"+observedAt;}
    }
    static FailedSite failure(String tag,long now){
        if(!tag.startsWith(FAILED))return null;
        try{
            var parts=tag.substring(FAILED.length()).split(":",-1);if(parts.length<1||parts.length>2)return null;
            long at=parts.length==1?now:Long.parseLong(parts[1]);
            return at<0?null:new FailedSite(BlockPos.of(Long.parseLong(parts[0])),at);
        }catch(NumberFormatException invalid){return null;}
    }
    static final class State {
        BlockPos root,stand,inspection,inspectWaypoint;Direction direction;int index,dug,centerings;
        boolean loaded,inspectedAll,retreat;int repairs,reconsidered;long reviewAt,scanAt,pathAt,started,lastWork,progressAt,lastActive,centerUntil,inspectUntil,arrivedAt;
        final Set<BlockPos> inspected=new HashSet<>();
        ItemStack tool=ItemStack.EMPTY;
        BlockPos surveyStand;
        long surveyUntil,surveyPathAt,surveyNext;
        final LinkedHashMap<BlockPos,Integer> surveyVisits=new LinkedHashMap<>();
        Map<String,Object> surveyReceipt=Map.of();
        String activity="no quarry job",inspectRoute="none";Map<String,Object> receipt=Map.of(),lastStop=Map.of(),lastApproach=Map.of(),repairReceipt=Map.of(),retirementReceipt=Map.of();
    }
    private record Site(BlockPos root,Direction direction,int clearSteps){}
    static Map<String,Object> status(Session s){var q=s.quarry;return Map.of("activity",q.activity,"dug",q.dug,"step",q.index,
        "origin",q.root==null?"":q.root.toShortString(),"lastReceipt",q.receipt,"lastStop",q.lastStop,"lastApproach",q.lastApproach,
        "stand",q.stand==null?"":q.stand.toShortString(),"centerings",q.centerings,
        "inspection",Map.of("target",q.inspection==null?"":q.inspection.toShortString(),"visited",q.inspected.size(),"route",q.inspectRoute,
            "repairs",q.repairs,"lastRepair",q.repairReceipt,"reconsidered",q.reconsidered,"reviewAt",q.reviewAt,"returning",q.retreat,"lastRetirement",q.retirementReceipt,
            "survey",Map.of("target",q.surveyStand==null?"":q.surveyStand.toShortString(),"visited",q.surveyVisits.size(),"lastRoute",q.surveyReceipt)));}
    private static boolean soil(net.minecraft.world.level.block.state.BlockState b){
        return b.is(Blocks.DIRT)||b.is(Blocks.GRASS_BLOCK)||b.is(Blocks.COARSE_DIRT);
    }
    // A quarry starts on observed dry soil. Clay encountered within that same
    // finite staircase is ordinary excavatable overburden, not a new worksite.
    static boolean overburden(net.minecraft.world.level.block.state.BlockState b){
        return soil(b)||b.is(Blocks.CLAY);
    }
    static List<BlockPos> cells(BlockPos root,Direction direction){
        var out=new ArrayList<BlockPos>();var side=direction.getClockWise();
        for(int depth=1;depth<=4;depth++)for(int layer=1;layer<=depth;layer++)out.add(root.relative(direction,depth).below(layer));
        for(var offset:List.of(side,side.getOpposite(),direction))for(int layer=3;layer<=4;layer++)
            out.add(root.relative(direction,4).relative(offset).below(layer));
        return List.copyOf(out);
    }
    static List<BlockPos> accessSupports(BlockPos root,Direction direction){
        var out=new ArrayList<BlockPos>();
        for(int depth=0;depth<=4;depth++)out.add(root.relative(direction,depth).below(depth+1));
        return List.copyOf(out);
    }
    static boolean accessSupport(Villager v,BlockPos target){
        for(var tag:v.entityTags())if(tag.startsWith(TAG))try{
            var parts=tag.substring(TAG.length()).split(":");if(parts.length!=3)continue;
            var direction=Direction.valueOf(parts[1]);if(direction.getAxis().isVertical())continue;
            if(accessSupports(BlockPos.of(Long.parseLong(parts[0])),direction).contains(target))return true;
        }catch(IllegalArgumentException ignored){}
        return false;
    }
    private static BlockHitResult replacementContact(ServerLevel level,Villager v,BlockPos p,boolean afterRemoval){
        var context=new net.minecraft.world.level.ClipContext(v.getEyePosition(),Vec3.atBottomCenterOf(p).add(0,-.001,0),
            net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,v){
            @Override public net.minecraft.world.phys.shapes.VoxelShape getBlockShape(net.minecraft.world.level.block.state.BlockState state,
                    net.minecraft.world.level.BlockGetter world,BlockPos at){
                return afterRemoval&&at.equals(p)?net.minecraft.world.phys.shapes.Shapes.empty():super.getBlockShape(state,world,at);
            }
        };
        var hit=level.clip(context);
        return hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(p.below())&&hit.getDirection()==Direction.UP
            &&v.getEyePosition().distanceToSqr(hit.getLocation())<=9?hit:null;
    }
    /** A visible support can be exchanged only when its normal dirt replacement is already funded and reachable. */
    static boolean canReplaceAccess(ServerLevel level,Villager v,BlockPos p){
        return NativeRegionsScenario.nativeRegions(level)&&Villagers.config().nativeAdventureQuarry()
            &&AdventureActions.PERMISSIONS.allows(Capability.REPAIR_QUARRY_ACCESS)&&v.onGround()
            &&v.getInventory().countItem(net.minecraft.world.item.Items.DIRT)>0&&level.isLoaded(p.below())
            &&FloorCentering.safeSupport(level.getBlockState(p.below()))
            &&level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP)&&replacementContact(level,v,p,true)!=null;
    }
    /** Replace a missing support in this actor's recorded stairs with actual carried dirt. */
    static String repairAccess(ServerLevel level,Villager v,Session s,long now){
        return repairAccess(level,v,s,now,null);
    }
    static String repairAccess(ServerLevel level,Villager v,Session s,long now,BlockPos only){
        var q=s.quarry;load(v,q,now);
        if(!NativeRegionsScenario.nativeRegions(level)||!Villagers.config().nativeAdventureQuarry()||q.root==null
            ||!AdventurePreparation.nominated(v)||!AdventureActions.PERMISSIONS.allows(Capability.REPAIR_QUARRY_ACCESS)
            ||v.isSleeping()||v.isTrading()||v.isPassenger()||v.isInWater()||v.isOnFire()||!v.onGround()
            ||v.getHealth()<v.getMaxHealth()*.5||s.mind.foodLevel()<8)return null;
        var bag=v.getInventory();int slot=-1;
        for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).is(net.minecraft.world.item.Items.DIRT)){slot=i;break;}
        if(slot<0)return null;
        for(var p:accessSupports(q.root,q.direction)){
            if(only!=null&&!only.equals(p))continue;
            if(!local(level,v,s,p)||!level.getBlockState(p).isAir()||!level.getFluidState(p).isEmpty()
                ||!level.isLoaded(p.below())||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP)
                ||!level.getEntities(null,new AABB(p)).isEmpty())continue;
            var hit=replacementContact(level,v,p,false);if(hit==null)continue;
            var before=HarvestMaterials.snapshot(bag);int count=bag.getItem(slot).getCount();
            if(!level.setBlock(p,Blocks.DIRT.defaultBlockState(),Block.UPDATE_ALL))continue;
            if(!HarvestMaterials.unchanged(bag,before)||!level.getBlockState(p).is(Blocks.DIRT)){
                if(level.getBlockState(p).is(Blocks.DIRT))level.setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);return null;
            }
            bag.removeItem(slot,1);q.repairs++;
            q.repairReceipt=Map.of("gameTime",now,"position",p.toShortString(),"dirtBefore",count,"dirtAfter",count-1,
                "contact",hit.getLocation().toString(),"reach",v.getEyePosition().distanceTo(hit.getLocation()));
            v.getNavigation().stop();v.swingForAttack(InteractionHand.MAIN_HAND);
            level.gameEvent(v,net.minecraft.world.level.gameevent.GameEvent.BLOCK_PLACE,p);Villagers.exert(v,.005f);
            s.mind.remember("Repaired my quarry stair with one real carried dirt at "+p.toShortString());
            return q.activity="repaired a quarry access step with carried dirt";
        }
        return null;
    }
    private static boolean local(ServerLevel level,Villager v,Session s,BlockPos p){
        return level.isLoaded(p)&&level.getWorldBorder().isWithinBounds(p)&&!ConstructionSites.isProtected(level,p)
            &&VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(p));
    }
    private static boolean surroundings(ServerLevel level,Villager v,Session s,BlockPos p){return obstruction(level,v,s,p)==null;}
    /** A neighboring sand column may remain beside the stairs if its own support is intact.
     * Never remove the target when it directly supports a falling block overhead. */
    private static boolean stableFallingColumn(ServerLevel level,BlockPos column){
        var support=column.below();
        for(int depth=0;depth<16;depth++,support=support.below()){
            if(!level.isLoaded(support))return false;
            var block=level.getBlockState(support);
            if(block.getBlock() instanceof FallingBlock)continue;
            return block.isFaceSturdy(level,support,Direction.UP);
        }
        return false;
    }
    private static String obstruction(ServerLevel level,Villager v,Session s,BlockPos p){
        if(!local(level,v,s,p))return "outside loaded, protected or home work bounds";
        var cover=NativeTerrainWork.structuralCover(level,p);
        if(cover!=null)return "supports native construction at "+cover.toShortString();
        if(!level.isLoaded(p.below())||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))return "no supported floor below this block";
        for(var d:Direction.values()){
            var n=p.relative(d);if(!level.isLoaded(n))return "unloaded neighboring block";
            if(!level.getFluidState(n).isEmpty())return "adjacent fluid";
            if(level.getBlockState(n).getBlock() instanceof FallingBlock
                &&(d==Direction.UP||!stableFallingColumn(level,n)))return "unsupported or overhead falling block at "+n.toShortString();
        }
        for(var n:BlockPos.betweenClosed(p.offset(-2,-1,-2),p.offset(2,2,2))){
            if(!local(level,v,s,n))return "near protected or outside-work-area blocks";var b=level.getBlockState(n);
            if(level.getBlockEntity(n)!=null||b.getBlock() instanceof BedBlock||b.getBlock() instanceof DoorBlock
                ||b.getBlock() instanceof CropBlock||b.is(net.minecraft.tags.BlockTags.PLANKS))return "near furnishings or crops at "+n.toShortString();
        }
        return null;
    }
    private static void save(Villager v,State q){
        v.entityTags().stream().filter(t->t.startsWith(TAG)).toList().forEach(v::removeTag);
        if(q.root!=null)v.addTag(TAG+q.root.asLong()+":"+q.direction.name()+":"+q.index);
        if(q.retreat&&q.root!=null)v.addTag(RETREAT);else v.removeTag(RETREAT);
    }
    private static void load(Villager v,State q,long now){
        if(q.loaded)return;q.loaded=true;q.retreat=v.entityTags().contains(RETREAT);
        for(var tag:v.entityTags())if(tag.startsWith(TAG))try{
            var parts=tag.substring(TAG.length()).split(":");if(parts.length!=3)continue;
            var direction=Direction.valueOf(parts[1]);int index=Integer.parseInt(parts[2]);
            if(direction.getAxis().isVertical()||index<0||index>16)continue;
            q.root=BlockPos.of(Long.parseLong(parts[0]));q.direction=direction;q.index=index;q.progressAt=now;return;
        }catch(IllegalArgumentException ignored){}
    }
    private static boolean site(ServerLevel level,Villager v,Session s,BlockPos root,Direction direction){
        for(var top:List.of(cells(root,direction).getFirst())){
            if(!local(level,v,s,top)||!soil(level.getBlockState(top))||!surroundings(level,v,s,top)||!level.canSeeSky(top.above())
                ||!BoatRoutes.visible(level,v,Vec3.atCenterOf(top).add(0,.5,0))
                ||!level.getBlockState(top.above()).getCollisionShape(level,top.above()).isEmpty()
                ||!level.getBlockState(top.above(2)).getCollisionShape(level,top.above(2)).isEmpty())return false;
        }
        return true;
    }
    private static void reviewFailures(Villager v,State q,Session s,long now){
        q.reviewAt=0;
        for(var tag:v.entityTags().stream().filter(t->t.startsWith(FAILED)).toList()){
            var old=failure(tag,now);if(old==null)continue;
            if(old.expired(now)){
                v.removeTag(tag);q.reconsidered++;
                s.mind.remember("Reconsidering an old quarry worksite after a full day; current terrain and route must be surveyed again");
            }else{
                // Old saves lacked a date. Start one real review interval once,
                // persist it, and never refresh the failure age on a reload.
                if(!tag.equals(old.tag())){v.removeTag(tag);v.addTag(old.tag());}
                long at=old.observedAt()+REVIEW_AFTER;q.reviewAt=q.reviewAt==0?at:Math.min(q.reviewAt,at);
            }
        }
    }
    private static boolean failedNear(Villager v,BlockPos root,long now){
        if(MineStair.exhaustedNear(v,root))return true;
        for(var tag:v.entityTags()){
            var old=failure(tag,now);if(old!=null&&!old.expired(now)&&old.root().distSqr(root)<16)return true;
        }
        return false;
    }
    static boolean failedEntry(Villager v,BlockPos root,long now){return failedNear(v,root,now);}
    private static void retire(Villager v,State q,long now){
        v.addTag(new FailedSite(q.root,now).tag());
        // Keep the actual entrance and its support protections until the actor
        // has climbed out. Clearing them at a stalled face strands the worker
        // below the surface with no remembered return target.
        q.retreat=true;q.started=q.centerUntil=0;q.scanAt=now+600;save(v,q);
        q.retirementReceipt=Map.of("started",now,"entry",q.root.toShortString(),"entryHeight",q.root.getY(),"startedPosition",v.position().toString());
    }
    private static String leaveRetired(ServerLevel level,Villager v,Session s,long now){
        var q=s.quarry;if(!q.retreat||q.root==null)return null;
        if(!v.onGround()||!MineStair.atEntry(v.position(),q.root)){
            var exit=MineStair.returnToEntry(level,v,s,now,true);
            return exit==null?"waiting for the retained exit from the retired quarry":exit;
        }
        MineStair.rememberEntry(v,s,q.root);
        s.mineStair.floors.addAll(accessSupports(q.root,q.direction));
        for(var floor:s.mineStair.floors)v.addTag("villagers:mine_floor:"+floor.asLong());
        var receipt=new LinkedHashMap<String,Object>(q.retirementReceipt);
        receipt.put("entry",q.root.toShortString());receipt.put("entryHeight",q.root.getY());receipt.put("returnedGameTime",now);
        receipt.put("returnedPosition",v.position().toString());receipt.put("returnedHeight",v.getY());q.retirementReceipt=Map.copyOf(receipt);
        q.root=q.stand=null;q.index=0;q.retreat=false;save(v,q);
        return "left the retired quarry along its actual retained stairs";
    }
    private static boolean safePath(ServerLevel level,Villager v,Session s,net.minecraft.world.level.pathfinder.Path path){
        if(!WorkNavigation.staysHome(level,v,s,path))return false;int y=v.getBlockY();
        for(int i=0;i<path.getNodeCount();i++){
            var p=path.getNodePos(i);
            if(p.getY()<y-1||!level.getFluidState(p).isEmpty()||!level.getFluidState(p.below()).isEmpty())return false;
            y=p.getY();
        }
        return true;
    }
    private static net.minecraft.world.level.pathfinder.Path approach(ServerLevel level,Villager v,Session s,BlockPos target){
        var danger=new AABB(target).expandTowards(0,2,0);var stands=new ArrayList<BlockPos>();
        for(var p:BlockPos.betweenClosed(target.offset(-2,-3,-2),target.offset(2,1,2))){
            if(ForestryWork.standFailure(level,v,s,target,p)!=null)continue;
            // Native navigation finishes within a cell, not exactly at its center. A stand
            // on the three-block reach boundary can finish with the real eye still too far
            // away, then repeatedly return the same completed one-node path.
            // Keep diagonal stands beside a one-block-lower target (about2.55),
            // with room for native arrival tolerance inside actual three-block reach.
            if(ForestryWork.standEye(v,p).distanceToSqr(Vec3.atCenterOf(target))>7.5625)continue;
            var body=v.getBoundingBox().move(p.getX()+.5-v.getX(),p.getY()-v.getY(),p.getZ()+.5-v.getZ());
            if(!body.intersects(danger))stands.add(p.immutable());
        }
        stands.sort(Comparator.comparingDouble(p->p.distSqr(v.blockPosition())));
        for(var p:stands.stream().limit(6).toList()){
            var path=NativePathLimits.search(v,1,()->WorkNavigation.pathTo(level,v,s,p,0));if(safePath(level,v,s,path))return path;
        }
        return null;
    }
    /** Finish a native path's last fraction of a cell through ordinary MoveControl.
     * Navigation's waypoint tolerance can leave a foot on the soil being removed. */
    private static boolean centerApproach(ServerLevel level,Villager v,Session s,BlockPos target,long now){
        var q=s.quarry;if(q.stand==null||!v.onGround()
            ||Math.abs(v.getY()-q.stand.getY())>.01)return false;
        var point=Vec3.atBottomCenterOf(q.stand);
        // Native waypoint tolerance can stop in the adjacent cell, including
        // atop the block being removed. Finish only the short same-floor,
        // fully collision-clear departure to our already validated stand.
        double distance=v.position().distanceToSqr(point);
        if(distance>1.5625||distance<.0025||ForestryWork.standFailure(level,v,s,target,q.stand)!=null)return false;
        var body=v.getBoundingBox().move(point.subtract(v.position()));
        if(body.intersects(new AABB(target).expandTowards(0,2,0))||!level.noCollision(v,v.getBoundingBox().minmax(body)))return false;
        // This controller thinks every ten native ticks. MoveControl consumes each
        // request for one tick, so allow enough requests to walk across the cell.
        if(q.centerUntil==0){q.centerUntil=now+200;q.centerings++;}
        if(now>=q.centerUntil){q.stand=null;q.centerUntil=0;return false;}
        v.getNavigation().stop();v.getMoveControl().setWantedPosition(point.x,point.y,point.z,.35);return true;
    }
    private static boolean inspectionStand(ServerLevel level,Villager v,Session s,BlockPos p){
        return local(level,v,s,p)&&level.isLoaded(p.above())&&level.isLoaded(p.below())
            &&level.getBlockState(p).isAir()&&level.getBlockState(p.above()).isAir()
            &&FloorCentering.safeSupport(level.getBlockState(p.below()))
            &&level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP);
    }
    private static String pathDescription(Villager v,net.minecraft.world.level.pathfinder.Path path){
        if(path==null)return "no native route";
        int y=v.getBlockY(),drop=0;for(int i=0;i<path.getNodeCount();i++){int next=path.getNodePos(i).getY();drop=Math.max(drop,y-next);y=next;}
        return "nodes="+path.getNodeCount()+",reaches="+path.canReach()+",drop="+drop+",end="+path.getEndNode().asBlockPos().toShortString();
    }
    private static net.minecraft.world.level.pathfinder.Path inspectionPath(ServerLevel level,Villager v,Session s,BlockPos target){
        var path=NativePathLimits.search(v,1,()->WorkNavigation.pathTo(level,v,s,target,0));s.quarry.inspectRoute=pathDescription(v,path);
        if(safePath(level,v,s,path))return path;
        // A native shortest route may cut across a deeper drop. Approach through
        // our already-cleared stair cells instead of increasing the allowed fall.
        for(int depth=4;depth>=0;depth--){
            var step=s.quarry.root.relative(s.quarry.direction,depth).below(depth);
            if(step.equals(v.blockPosition())||!inspectionStand(level,v,s,step))continue;
            var staged=NativePathLimits.search(v,1,()->WorkNavigation.pathTo(level,v,s,step,0));
            if(safePath(level,v,s,staged)){
                s.quarry.inspectRoute+="; via stair "+step.toShortString();return staged;
            }
        }
        return null;
    }
    /** Enter the cleared work bay after removing its floor, using the retained block below.
     * This reveals lower side faces through actual movement, rather than through soil. */
    private static String inspect(ServerLevel level,Villager v,Session s,long now){
        var q=s.quarry;
        if(q.inspection!=null&&(!inspectionStand(level,v,s,q.inspection)||now>=q.inspectUntil)){
            q.inspected.add(q.inspection);q.inspection=q.inspectWaypoint=null;q.arrivedAt=0;
        }
        if(q.inspection==null){
            var candidates=new HashSet<BlockPos>();
            for(var cell:cells(q.root,q.direction))for(var p:List.of(cell,cell.below()))
                if(!q.inspected.contains(p)&&inspectionStand(level,v,s,p))candidates.add(p);
            var ordered=new ArrayList<>(candidates);
            ordered.sort(Comparator.comparingInt((BlockPos p)->p.getY()).thenComparingDouble(p->p.distSqr(v.blockPosition())));
            for(var p:ordered){q.inspection=p;q.inspectUntil=now+1200;break;}
        }
        if(q.inspection==null){q.inspectedAll=true;q.activity="finished inspecting the cleared quarry";return null;}
        var p=q.inspection;
        if(v.blockPosition().equals(p)&&v.onGround()){
            if(q.arrivedAt==0)q.arrivedAt=now;
            v.getNavigation().stop();
            if(now-q.arrivedAt<300)return q.activity="inspecting exposed faces from the cleared quarry floor";
            q.inspected.add(p);q.inspection=q.inspectWaypoint=null;q.arrivedAt=0;return q.activity="finished observing this quarry stand";
        }
        var current=v.getNavigation().getPath();
        if(!v.onGround()||current!=null&&!current.isDone()&&q.inspectWaypoint!=null&&current.getTarget().equals(q.inspectWaypoint))
            return q.activity="following the existing safe quarry route";
        if(now>=q.pathAt&&WorkNavigation.admit(level,now,v)){
            q.pathAt=now+40;var path=inspectionPath(level,v,s,p);
            if(!safePath(level,v,s,path)){q.inspected.add(p);q.inspection=null;return null;}
            q.inspectWaypoint=path.getTarget();
            if(v.getNavigation().isDone())v.getNavigation().stop();v.getNavigation().moveTo(path,.6);
        }
        return q.activity="walking into a supported quarry work bay";
    }
    static boolean prepare(ServerLevel level,Villager v,Session s,long now){
        var q=s.quarry;load(v,q,now);
        if(!NativeRegionsScenario.nativeRegions(level)||!Villagers.config().nativeAdventureQuarry()||!AdventurePreparation.nominated(v)||!AdventurePreparation.pickaxe(v)||!AdventureActions.PERMISSIONS.allows(Capability.DIG_ADVENTURE_QUARRY))return false;
        if(q.root!=null){
            if(q.index<16&&!local(level,v,s,q.root)){retire(v,q,now);return false;}
            if(q.index>=16&&q.inspectedAll){retire(v,q,now);return false;}
            return true;
        }
        if(now<q.scanAt)return false;q.scanAt=now+600;
        reviewFailures(v,q,s,now);
        if(v.entityTags().stream().filter(t->t.startsWith(FAILED)).count()>=8){q.activity="waiting to reconsider recent quarry sites";return false;}
        var origin=v.blockPosition();var starts=new ArrayList<BlockPos>();
        // Survey the same local radius as wood preparation. A small first
        // survey can otherwise stay entirely inside a generated village piece
        // even though observed natural ground is reachable just beyond it.
        for(var p:BlockPos.betweenClosed(origin.offset(-28,-3,-28),origin.offset(28,1,28)))
            if(local(level,v,s,p)&&!failedNear(v,p,now)&&soil(level.getBlockState(p.below()))&&level.getBlockState(p).isAir()
                &&level.getBlockState(p.above()).isAir())starts.add(p.immutable());
        var sites=new ArrayList<Site>();
        for(var start:starts)for(var direction:Direction.Plane.HORIZONTAL){
            if(!site(level,v,s,start,direction))continue;
            int prefix=1;for(int step=2;step<=4;step++){if(!site(level,v,s,start.relative(direction,step-1),direction))break;prefix++;}
            sites.add(new Site(start,direction,prefix));
        }
        sites.sort(Comparator.comparingInt(Site::clearSteps).reversed().thenComparingDouble(candidate->candidate.root().distSqr(origin)));
        for(var candidate:sites.stream().limit(6).toList()){
            var start=candidate.root();var direction=candidate.direction();
            var first=cells(start,direction).getFirst();var path=approach(level,v,s,first);
            if(!safePath(level,v,s,path))continue;
            q.root=start;q.direction=direction;q.index=0;q.progressAt=now;q.surveyStand=null;
            q.inspection=q.stand=null;q.inspected.clear();q.inspectedAll=false;save(v,q);
            s.mind.remember("Selected observed natural soil for a bounded four-step quarry at "+start.toShortString());return true;
        }
        q.activity="needs an observed dry natural-soil quarry site";return false;
    }
    private static BlockPos surveyCell(BlockPos p){return new BlockPos(Math.floorDiv(p.getX(),12),Math.floorDiv(p.getY(),4),Math.floorDiv(p.getZ(),12));}
    /** Walk to observed natural ground to reveal a new view, rather than repeating a survey behind village walls. */
    private static String survey(ServerLevel level,Villager v,Session s,long now){
        var q=s.quarry;
        if(now<q.surveyNext)return null;
        if(q.surveyStand!=null&&(now>=q.surveyUntil||v.position().distanceToSqr(Vec3.atBottomCenterOf(q.surveyStand))<2)){
            q.surveyVisits.merge(surveyCell(q.surveyStand),1,Integer::sum);q.surveyStand=null;
            q.scanAt=Math.min(q.scanAt,now);return q.activity="observing natural ground from a new quarry vantage";
        }
        var current=v.getNavigation().getPath();
        if(q.surveyStand!=null&&current!=null&&!current.isDone()&&current.getTarget().equals(q.surveyStand))
            return q.activity="following a safe route to observe quarry ground";
        if(now<q.surveyPathAt||!WorkNavigation.admit(level,now,v))return q.activity="waiting to plan a quarry survey walk";
        q.surveyPathAt=now+80;
        var origin=v.blockPosition();var candidates=new ArrayList<BlockPos>();
        q.surveyVisits.merge(surveyCell(origin),1,Integer::sum);
        while(q.surveyVisits.size()>128)q.surveyVisits.remove(q.surveyVisits.firstEntry().getKey());
        for(int dx=-24;dx<=24;dx+=6)for(int dz=-24;dz<=24;dz+=6)for(int dy=-3;dy<=2;dy++){
            if(dx*dx+dz*dz<36||dx*dx+dz*dz>28*28)continue;
            var p=origin.offset(dx,dy,dz);
            if(!local(level,v,s,p)||!level.isLoaded(p.above())||!level.isLoaded(p.below())
                ||!soil(level.getBlockState(p.below()))||!level.canSeeSky(p)
                ||!level.getBlockState(p).getCollisionShape(level,p).isEmpty()
                ||!level.getBlockState(p.above()).getCollisionShape(level,p.above()).isEmpty()
                ||!level.getFluidState(p).isEmpty()||!level.getFluidState(p.below()).isEmpty()
                ||!BoatRoutes.visible(level,v,Vec3.atBottomCenterOf(p).add(0,.1,0)))continue;
            candidates.add(p);
        }
        candidates.sort(Comparator.<BlockPos>comparingInt(p->q.surveyVisits.getOrDefault(surveyCell(p),0))
            .thenComparing(p->level.structureManager().getStructureWithPieceAt(p,h->true).isValid())
            .thenComparingDouble(p->-p.distSqr(origin)));
        for(var p:candidates.stream().limit(6).toList()){
            var path=NativePathLimits.search(v,1,()->WorkNavigation.pathTo(level,v,s,p,0));
            if(!safePath(level,v,s,path)||!BoatTravel.dryPath(level,path)){
                q.surveyVisits.merge(surveyCell(p),1,Integer::sum);continue;
            }
            q.surveyStand=p;q.surveyUntil=now+600;
            boolean moved=v.getNavigation().moveTo(path,.6);
            q.surveyReceipt=Map.of("gameTime",now,"origin",origin.toShortString(),"target",p.toShortString(),
                "nodes",path.getNodeCount(),"moved",moved,"end",path.getEndNode().asBlockPos().toShortString());
            return q.activity="walking to observe natural quarry ground";
        }
        q.surveyStand=null;q.surveyNext=now+200;return null;
    }
    static String work(ServerLevel level,Villager v,Session s,long now){
        var q=s.quarry;
        if(!Villagers.config().nativeAdventureQuarry()||!WoodBootstrap.safe(level,v,s)||!AdventureIron.needsMining(v,s)||s.mind.foodLevel()<14){q.started=0;return null;}
        load(v,q,now);var exit=leaveRetired(level,v,s,now);if(exit!=null)return q.activity=exit;
        if(!prepare(level,v,s,now))return survey(level,v,s,now);
        if(now-q.lastActive>20)q.progressAt=now;q.lastActive=now;
        var cells=cells(q.root,q.direction);
        while(q.index<cells.size()&&level.isLoaded(cells.get(q.index))&&level.getBlockState(cells.get(q.index)).isAir())q.index++;
        if(q.index>=cells.size()){save(v,q);return inspect(level,v,s,now);}
        var p=cells.get(q.index);if(!local(level,v,s,p))return null;var block=level.getBlockState(p);
        if(!overburden(block)||!surroundings(level,v,s,p)||now-q.progressAt>1200){
            q.lastStop=Map.of("gameTime",now,"target",p.toShortString(),"block",block.toString(),"step",q.index,"actor",v.blockPosition().toShortString(),
                "idleTicks",now-q.progressAt,"reason",!overburden(block)?"target is not natural overburden":now-q.progressAt>1200?"no physical progress":String.valueOf(obstruction(level,v,s,p)));
            if(AdventureMining.naturalRock(block)&&BoatRoutes.visible(level,v,Vec3.atCenterOf(p))){q.index=cells.size();save(v,q);}
            else if(q.index>=10){
                // The side bays are independent. Leave an unsafe bay intact and try the
                // next one without abandoning the already safe staircase or its exits.
                q.index=10+((q.index-10)/2+1)*2;q.started=0;q.progressAt=now;save(v,q);
                return q.activity="left an unsafe quarry work bay undisturbed";
            }
            else retire(v,q,now);
            q.activity="stopped quarry at a non-soil block, boundary or stalled route";return null;
        }
        if(v.getBoundingBox().intersects(new AABB(p).expandTowards(0,2,0))
            ||v.getEyePosition().distanceToSqr(Vec3.atCenterOf(p))>9||!BoatRoutes.visible(level,v,Vec3.atCenterOf(p))){
            q.started=0;
            if(centerApproach(level,v,s,p,now))return q.activity="stepping clear of the quarry block on supported ground";
            if(now>=q.pathAt&&WorkNavigation.admit(level,now,v)){
                q.pathAt=now+40;var path=approach(level,v,s,p);
                boolean safe=safePath(level,v,s,path);
                if(safe){q.stand=path.getEndNode().asBlockPos();q.centerUntil=0;if(v.getNavigation().isDone())v.getNavigation().stop();}
                boolean moved=safe&&v.getNavigation().moveTo(path,.6);
                q.lastApproach=Map.of("gameTime",now,"target",p.toShortString(),"actor",v.position().toString(),"safe",safe,"accepted",moved,
                    "nodes",path==null?0:path.getNodeCount(),"end",path==null?"":path.getEndNode().asBlockPos().toShortString());
            }
            return q.activity="approaching a visible soil face in the quarry";
        }
        // Native movement stops flush against solid faces. Touching a wall is safe;
        // occupying the block or standing on its support is still excluded.
        if(!level.getEntities(null,new AABB(p).expandTowards(0,2,0)).isEmpty())return null;
        q.centerUntil=0;v.getNavigation().stop();v.swingForAttack(InteractionHand.MAIN_HAND);
        if(q.started==0||now-q.lastWork>20||!ItemStack.matches(q.tool,v.getMainHandItem())){q.started=now;q.tool=v.getMainHandItem().copy();}
        q.lastWork=now;
        int duration=(int)Math.ceil(block.getDestroySpeed(level,p)*30/Math.max(1,v.getMainHandItem().getDestroySpeed(block)));
        if(now-q.started<Math.max(1,duration))return q.activity="digging real soil for a quarry stair";
        var before=HarvestMaterials.snapshot(v.getInventory());var next=new SimpleContainer(v.getInventory().getContainerSize());
        for(int i=0;i<next.getContainerSize();i++)next.setItem(i,v.getInventory().getItem(i).copy());
        var drops=Block.getDrops(block,level,p,null,v,v.getMainHandItem());
        if(drops.isEmpty())return q.activity="quarry has no collectible native drops";
        if(drops.stream().anyMatch(drop->!next.addItem(drop.copy()).isEmpty())){
            var room=NativeWorkInventory.makeRoom(level,v,s);
            return q.activity=room==null?"quarry drops do not fit in the actual bag":room;
        }
        if(!block.equals(level.getBlockState(p))||!ItemStack.matches(q.tool,v.getMainHandItem())||!HarvestMaterials.unchanged(v.getInventory(),before)
            ||!surroundings(level,v,s,p)||!level.getEntities(null,new AABB(p).expandTowards(0,2,0)).isEmpty()||!level.destroyBlock(p,false,v))return null;
        for(int i=0;i<next.getContainerSize();i++)v.getInventory().setItem(i,next.getItem(i));
        var tool=v.getMainHandItem().get(DataComponents.TOOL);if(tool!=null)v.getMainHandItem().hurtAndBreak(tool.damagePerBlock(),v,EquipmentSlot.MAINHAND);
        Villagers.exert(v,.005f);q.dug++;q.index++;q.progressAt=now;q.started=0;save(v,q);
        q.receipt=Map.of("gameTime",now,"position",p.toShortString(),"drops",drops.stream().map(ItemStack::toString).toList(),"step",q.index);
        s.mind.remember("Dug actual quarry soil at "+p.toShortString()+" and retained its native drops");return q.activity="cleared a real quarry soil block";
    }
    private AdventureQuarry(){}
    static BlockPos rememberedEntry(Villager v,Session s,long now){
        load(v,s.quarry,now);return s.quarry.root;
    }
    static void retireForProspecting(Villager v,Session s,long now){
        var q=s.quarry;load(v,q,now);
        if(q.root!=null){s.mineStair.floors.addAll(accessSupports(q.root,q.direction));retire(v,q,now);}
        q.inspection=q.inspectWaypoint=null;q.inspected.clear();q.inspectedAll=false;
    }
    static String prospect(ServerLevel level,Villager v,Session s,long now){
        if(!Villagers.config().nativeAdventureQuarry()||s.mineStair.root!=null)return null;
        var q=s.quarry;load(v,q,now);
        if(q.retreat)return leaveRetired(level,v,s,now);
        if(q.root==null||q.index<cells(q.root,q.direction).size()){
            var activity=work(level,v,s,now);if(activity!=null)return activity;
            return q.root!=null&&q.index<cells(q.root,q.direction).size()?"preparing the observed quarry before entering its work bays":null;
        }
        return enterKnownWorkBay(level,v,s,now);
    }
    static String enterKnownWorkBay(ServerLevel level,Villager v,Session s,long now){
        if(!Villagers.config().nativeAdventureQuarry())return null;
        load(v,s.quarry,now);
        if(s.mineStair.root!=null||s.quarry.root==null||s.quarry.inspectedAll)return null;
        s.quarry.index=cells(s.quarry.root,s.quarry.direction).size();
        return inspect(level,v,s,now);
    }
}
