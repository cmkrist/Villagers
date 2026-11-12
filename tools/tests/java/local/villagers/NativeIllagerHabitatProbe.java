package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.level.block.*;

/** Frozen actual native illagers, structure membership and existing settlement resources in a COPY. */
public final class NativeIllagerHabitatProbe implements ModInitializer {
    private static Map<String,Object> dryCropRules(){
        try{
            var soil=BlockPos.ZERO;var crop=soil.above();
            var dry=Blocks.FARMLAND.defaultBlockState();
            java.util.function.Function<Boolean,net.minecraft.world.level.BlockGetter> fixture=wet->new net.minecraft.world.level.BlockGetter(){
                public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos p){return null;}
                public net.minecraft.world.level.block.state.BlockState getBlockState(BlockPos p){
                    return p.equals(soil)?wet?dry.setValue(FarmlandBlock.MOISTURE,7):dry:p.equals(crop)?Blocks.WHEAT.defaultBlockState():Blocks.AIR.defaultBlockState();
                }
                public net.minecraft.world.level.material.FluidState getFluidState(BlockPos p){return getBlockState(p).getFluidState();}
                public int getHeight(){return 384;}public int getMinY(){return -64;}
            };
            var speed=CropBlock.class.getDeclaredMethod("getGrowthSpeed",Block.class,net.minecraft.world.level.BlockGetter.class,BlockPos.class);speed.setAccessible(true);
            var maintain=FarmlandBlock.class.getDeclaredMethod("shouldMaintainFarmland",net.minecraft.world.level.BlockGetter.class,BlockPos.class);maintain.setAccessible(true);
            return Map.of("defaultMoisture",dry.getValue(FarmlandBlock.MOISTURE),"wheatMaintainsFarmland",Blocks.WHEAT.defaultBlockState().is(net.minecraft.tags.BlockTags.MAINTAINS_FARMLAND),
                "drySoilMaintainedUnderWheat",maintain.invoke(null,fixture.apply(false),soil),"dryGrowthSpeed",speed.invoke(null,Blocks.WHEAT,fixture.apply(false),crop),
                "hydratedGrowthSpeed",speed.invoke(null,Blocks.WHEAT,fixture.apply(true),crop),
                "scope","Separate in-memory native rule fixture only. No world crop, soil moisture, time, body or inventory changed; not evidence of completed farming.");
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Native dry-crop rules unavailable",error);}
    }
    /** Operator-only frozen survey; coordinates are never given to a creature mind. */
    private static Map<String,Object> farmSurvey(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.monster.illager.Pillager mob){
        var home=WarCombat.home(mob);int radius=NativeIllagers.radius(mob);var soils=new ArrayList<BlockPos>();
        int visible=0,bare=0,wet=0;var visibleGround=new ArrayList<String>();
        for(int x=home.x()-radius;x<=home.x()+radius;x++)for(int z=home.z()-radius;z<=home.z()+radius;z++){
            if(!level.isLoaded(new BlockPos(x,mob.getBlockY(),z)))continue;
            int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1;
            // Plants have no motion collision, so the heightmap normally points to soil.
            var p=new BlockPos(x,y,z);var b=level.getBlockState(p);
            if(!(b.is(Blocks.DIRT)||b.is(Blocks.GRASS_BLOCK)||b.is(Blocks.FARMLAND))||!NativeCampWork.local(level,mob,p)
                ||ConstructionSites.isProtected(level,p.above())||!level.canSeeSky(p.above())
                ||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),net.minecraft.core.Direction.UP))continue;
            var cover=level.getBlockState(p.above());
            if(!(cover.isAir()||cover.is(Blocks.SHORT_GRASS)||cover.is(Blocks.TALL_GRASS)))continue;
            bare++;
            if(mob.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p))<=64&&NativeCampWork.visible(level,mob,p))visibleGround.add(p.toShortString());
            boolean hydrated=false;
            for(var water:BlockPos.betweenClosed(p.offset(-4,0,-4),p.offset(4,1,4)))
                if(level.isLoaded(water)&&level.getFluidState(water).is(net.minecraft.tags.FluidTags.WATER)){hydrated=true;break;}
            if(!hydrated)continue;wet++;soils.add(p);
            if(mob.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p))<=64&&NativeCampWork.visible(level,mob,p))visible++;
        }
        soils.sort(Comparator.comparingDouble(p->p.distSqr(mob.blockPosition())));
        var plots=new ArrayList<Map<String,Object>>();
        for(var p:soils.stream().limit(12).toList()){
            var path=IllagerFood.approachPath(level,mob,p);
            plots.add(Map.of("soil",p.toShortString(),"distance",Math.sqrt(p.distSqr(mob.blockPosition())),
                "surface",level.getBlockState(p.above()).toString(),"visibleWithinEight",mob.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p))<=64&&NativeCampWork.visible(level,mob,p),
                "safeRoute",NativeIllagers.safePath(level,mob,path),"nodes",path==null?0:path.getNodeCount()));
        }
        var bag=mob.getInventory();var plan=WoodCrafting.next(bag);
        var tools=new ArrayList<String>();for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).is(net.minecraft.tags.ItemTags.HOES))tools.add(bag.getItem(i).toString());
        if(mob.getMainHandItem().is(net.minecraft.tags.ItemTags.HOES))tools.add(mob.getMainHandItem().toString());
        return Map.of("bareNaturalSurfaceCount",bare,"hydratedGroundCount",wet,"visibleHydratedWithinEight",visible,
            "nearestGround",plots,"ownedSeeds",bag.countItem(net.minecraft.world.item.Items.WHEAT_SEEDS),"ownedHoes",tools,
            "ownedWoodRecipeStep",plan==null?"none":plan.step().toString(),
            "visibleGroundIncludingDry",visibleGround,"nativeDryCropRules",dryCropRules(),
            "scope","Frozen geometry and actual owned stock only; no chest loot opened, crops planted, resources supplied, or operator coordinates taught.");
    }
    private int ticks;private long time;private boolean done;
    private boolean started;private final boolean rehearse=Boolean.getBoolean("villagers.boats.rehearse");
    private final Set<UUID> founders=new HashSet<>();
    private final Set<UUID> initialActors=new HashSet<>();
    private final Map<UUID,Long> initialAges=new HashMap<>();
    private final int window=Integer.getInteger("villagers.illagers.windowTicks",8000);
    private boolean healthStop(net.minecraft.world.entity.LivingEntity body){
        int minimum=Integer.getInteger("villagers.illagers.minHealth",4);
        if(!local.villagers.lab.ObservationHealthGuard.critical(body.getHealth(),body.getAirSupply(),body.isEyeInFluid(net.minecraft.tags.FluidTags.WATER),minimum))return false;
        if(minimum==1&&Boolean.getBoolean("villagers.illagers.lastHealthRecovery")&&body instanceof net.minecraft.world.entity.npc.villager.Villager v
            &&!body.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)){
            var actor=Villagers.existingSession(v.getUUID());
            if(actor!=null&&local.villagers.lab.ObservationHealthGuard.lastHealthRecovery(v.getHealth(),actor.mind.foodLevel(),actor.mind.snapshot().food().timer()))return false;
        }
        return true;
    }

    /** Frozen cache/inspection check: navigation intent changes here never advance the world. */
    private static Map<String,Object> nativePathCache(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.Mob mob){
        var navigation=mob.getNavigation();var previous=navigation.getPath();
        var seed=navigation.createPath(mob.blockPosition(),0,16);
        if(seed==null||!navigation.moveTo(seed,.8))return Map.of("seed",false);
        int next=seed.getNextNodeIndex();
        var cached=navigation.createPath(mob.blockPosition(),0,16);
        if(cached!=seed)throw new AssertionError("Unscoped native search should reuse its unfinished same-target route");
        var fresh=NativePathLimits.avoiding(mob,p->false,()->navigation.createPath(mob.blockPosition(),0,16));
        if(fresh==null||fresh==seed)throw new AssertionError("Scoped search must actually bypass the cached route");
        var inspected=CampBeds.pathToStands(level,mob,List.of(mob.blockPosition()));
        boolean preserved=navigation.getPath()==seed&&seed.getNextNodeIndex()==next&&!seed.isDone();
        if(!preserved)throw new AssertionError("Planning an inspection must not cancel or advance active navigation");
        var activeTarget=navigation.getTargetPos();net.minecraft.world.level.pathfinder.Path alternative=null;
        var fallLimited=NativePathLimits.search(mob,1,()->navigation.createPath(mob.blockPosition(),0,16));
        if(fallLimited==null||fallLimited==seed)throw new AssertionError("Fall-only scope must not reuse the unconstrained cached path");
        if(navigation.getPath()!=seed||!Objects.equals(navigation.getTargetPos(),activeTarget))throw new AssertionError("Fall-only inspection must preserve active path and destination");
        for(var target:List.of(mob.blockPosition().north(),mob.blockPosition().south(),mob.blockPosition().east(),mob.blockPosition().west())){
            alternative=NativePathLimits.avoiding(mob,p->false,()->navigation.createPath(target,0,16));
            if(alternative!=null&&!Objects.equals(alternative.getTarget(),activeTarget))break;
        }
        if(alternative==null||Objects.equals(alternative.getTarget(),activeTarget))throw new AssertionError("A different native query destination is required");
        if(!Objects.equals(navigation.getTargetPos(),activeTarget)||navigation.getPath()!=seed)throw new AssertionError("Inspection must preserve active destination metadata");
        if(!navigation.moveTo(alternative,.8)||!Objects.equals(navigation.getTargetPos(),alternative.getTarget()))throw new AssertionError("Committing a scoped route must update its actual destination");
        navigation.stop();if(previous!=null)navigation.moveTo(previous,.8);
        return Map.of("seed",true,"ordinaryCacheReused",true,"scopedSearchFresh",true,"activePathPreserved",preserved,
            "activeDestinationPreserved",true,"chosenDestinationCommitted",true,
            "commonQueryReturnedPath",inspected!=null,"commonQueryWasFresh",inspected!=null&&inspected!=seed,
            "fallLimitedSearchFresh",true,"fallLimitedDestinationPreserved",true);
    }
    private static void forceCampAreas(net.minecraft.server.level.ServerLevel level){
        for(var area:NativeIllagers.areas()){
            int radius=area.radius()+16;
            for(int x=(area.x()-radius)>>4;x<=(area.x()+radius)>>4;x++)for(int z=(area.z()-radius)>>4;z<=(area.z()+radius)>>4;z++){
                level.setChunkForced(x,z,true);level.getChunk(x,z);
            }
        }
    }
    private static Map<String,Object> captiveAccess(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.npc.villager.Villager v){
        var result=new LinkedHashMap<String,Object>();result.put("id",v.getUUID().toString());result.put("position",v.position().toString());result.put("health",v.getHealth());
        var s=Villagers.sessionIfLoaded(v,level);if(s==null){result.put("mind","not loaded");return result;}
        result.put("food",s.mind.foodLevel());result.put("home",String.valueOf(s.mind.home()));result.put("inventory",v.getInventory().toString());
        result.put("stores",NativeFoodStorage.status(s));var stores=new ArrayList<Map<String,Object>>();var origin=v.blockPosition();
        var candidates=new ArrayList<BlockPos>();
        for(var p:BlockPos.betweenClosed(origin.offset(-12,-6,-12),origin.offset(12,6,12)))
            if(level.isLoaded(p)&&NativeContainers.accessible(level,p)!=null&&NativeFoodStorage.homeStructure(level,v,s,p))candidates.add(p.immutable());
        candidates.sort(Comparator.comparingDouble(p->p.distSqr(origin)));
        var sampled=new LinkedHashSet<BlockPos>(candidates.stream().filter(p->Math.abs(p.getY()-origin.getY())<=2).limit(6).toList());
        sampled.addAll(candidates.stream().limit(6).toList());
        for(var p:sampled){
            var stands=new ArrayList<Map<String,Object>>();var orderedStands=new ArrayList<BlockPos>();
            for(var q:BlockPos.betweenClosed(p.offset(-2,-1,-2),p.offset(2,1,2))){
                if(!level.isLoaded(q.above())||!level.getBlockState(q).isAir()||!level.getBlockState(q.above()).isAir()
                    ||!level.getBlockState(q.below()).isFaceSturdy(level,q.below(),net.minecraft.core.Direction.UP))continue;
                var eye=net.minecraft.world.phys.Vec3.atBottomCenterOf(q).add(0,v.getEyeHeight(),0);
                var hit=level.clip(new net.minecraft.world.level.ClipContext(eye,net.minecraft.world.phys.Vec3.atCenterOf(p),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,v));
                if(eye.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p))>9||hit.getType()!=net.minecraft.world.phys.HitResult.Type.MISS&&!hit.getBlockPos().equals(p))continue;
                orderedStands.add(q.immutable());
            }
            orderedStands.sort(Comparator.comparingDouble(q->q.distSqr(v.blockPosition())));
            for(var q:orderedStands.stream().limit(32).toList()){
                var path=v.getNavigation().createPath(q,0,64);net.minecraft.world.level.pathfinder.Path longer;
                try{v.getNavigation().setMaxVisitedNodesMultiplier(32);longer=v.getNavigation().createPath(q,0,128);}finally{v.getNavigation().resetMaxVisitedNodesMultiplier();}
                stands.add(Map.of("position",q.toShortString(),"nativeReachable",path!=null&&path.canReach(),"longReachable",longer!=null&&longer.canReach(),
                    "end",path==null?"":path.getEndNode().asBlockPos().toShortString(),"longEnd",longer==null?"":longer.getEndNode().asBlockPos().toShortString()));
            }
            stores.add(Map.of("position",p.toShortString(),"block",level.getBlockState(p).toString(),"distance",v.getEyePosition().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(p)),
                "visible",NativeFoodStorage.visible(level,v,p),"stands",stands));
        }
        var barriers=new ArrayList<Map<String,Object>>();
        try{
            var check=NativeCellEscape.class.getDeclaredMethod("fence",net.minecraft.server.level.ServerLevel.class,
                net.minecraft.world.entity.npc.villager.Villager.class,Session.class,BlockPos.class,boolean.class);check.setAccessible(true);
            var floorCheck=NativeCellEscape.class.getDeclaredMethod("exitFloor",net.minecraft.server.level.ServerLevel.class,BlockPos.class);floorCheck.setAccessible(true);
            for(var at:BlockPos.betweenClosed(origin.offset(-3,0,-3),origin.offset(3,0,3))){
                var middle=level.getBlockState(at.above());if(!(level.getBlockState(at).getBlock() instanceof net.minecraft.world.level.block.FenceBlock))continue;
                var path=v.getNavigation().createPath(at,1,32);var row=new LinkedHashMap<String,Object>();
                row.put("position",at.toShortString());row.put("eligible",check.invoke(null,level,v,s,at,false));
                row.put("middleWood",middle.is(net.minecraft.tags.BlockTags.WOODEN_FENCES));row.put("topSame",level.getBlockState(at.above(2)).is(middle.getBlock()));
                row.put("homeScope",NativeFoodStorage.homeStructure(level,v,s,at));row.put("protected",ConstructionSites.isProtected(level,at));
                row.put("sides",Map.of("east",floorCheck.invoke(null,level,at.east()),"west",floorCheck.invoke(null,level,at.west()),
                    "north",floorCheck.invoke(null,level,at.north()),"south",floorCheck.invoke(null,level,at.south())));
                row.put("nativeReachable",path!=null&&path.canReach());row.put("homePath",WorkNavigation.staysHome(level,v,s,path));
                row.put("contact",NativeFoodStorage.visible(level,v,at));barriers.add(row);
            }
        }catch(Exception error){throw new IllegalStateException("Cannot inspect native cage eligibility",error);}
        result.put("cageBarriers",barriers);
        result.put("nearbyStores",stores);
        var gates=new ArrayList<Map<String,Object>>();
        for(var p:BlockPos.betweenClosed(origin.offset(-8,-2,-8),origin.offset(8,1,8))){
            var block=level.getBlockState(p);
            if(!block.is(Blocks.IRON_DOOR)||block.getValue(DoorBlock.HALF)!=net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER
                ||!NativeFoodStorage.homeStructure(level,v,s,p))continue;
            var normal=v.getNavigation().createPath(p,1,32);
            var limited=NativePathLimits.search(v,v.getHealth()<=5?1:2,()->v.getNavigation().createPath(p,1,32));
            var square=NativePathLimits.search(v,v.getHealth()<=5?1:2,()->NativeCampMeals.gateApproach(level,v,s,p));
            gates.add(Map.of("door",p.toShortString(),"normal",gateSummary(v,normal),"limited",gateSummary(v,limited),"squareApproach",gateSummary(v,square)));
        }
        result.put("gateRoutes",gates);result.put("lowerGapRoutes",lowerGapRoutes(level,v));return result;
    }
    /** Read-only operator survey of actual lower openings and the captive's own native paths. */
    private static List<Map<String,Object>> lowerGapRoutes(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.npc.villager.Villager v){
        var rows=new ArrayList<Map<String,Object>>();var origin=v.blockPosition();
        for(var gap:BlockPos.betweenClosed(origin.offset(-3,0,-3),origin.offset(3,0,3))){
            var upper=level.getBlockState(gap.above());
            if(!level.getBlockState(gap).isAir()||!upper.is(net.minecraft.tags.BlockTags.WOODEN_FENCES)
                ||!level.getBlockState(gap.above(2)).is(upper.getBlock()))continue;
            for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL){
                var at=gap.relative(direction);if(!level.getBlockState(at.below()).is(net.minecraft.tags.BlockTags.PLANKS)
                    ||!level.getBlockState(at).isAir()||!level.getBlockState(at.above()).isAir())continue;
                double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,at);
                var feet=new net.minecraft.world.phys.Vec3(at.getX()+.5,floor,at.getZ()+.5);
                var box=v.getBoundingBox().move(feet.subtract(v.position()));
                var path=NativePathLimits.search(v,1,()->v.getNavigation().createPath(at,0,16));
                var row=new LinkedHashMap<String,Object>();row.put("gap",gap.toShortString());row.put("stand",at.toShortString());row.put("floor",floor);
                row.put("position",v.position().toString());row.put("targetBodyBlockClear",level.noBlockCollision(v,box));
                row.put("sweptBlockClear",level.noBlockCollision(v,v.getBoundingBox().minmax(box)));
                row.put("nearBodies",level.getEntities(v,box,e->e.isAlive()).stream().map(e->Map.of("id",e.getUUID().toString(),"position",e.position().toString())).toList());
                row.put("path",gateSummary(v,path));rows.add(row);
            }
        }
        return rows;
    }
    private static Map<String,Object> gateSummary(net.minecraft.world.entity.npc.villager.Villager v,net.minecraft.world.level.pathfinder.Path path){
        if(path==null)return Map.of("present",false);
        int y=v.getBlockY(),drop=0;var last=new ArrayList<String>();
        for(int i=0;i<path.getNodeCount();i++){var p=path.getNodePos(i);drop=Math.max(drop,y-p.getY());y=p.getY();if(i>=path.getNodeCount()-6)last.add(p.toShortString());}
        return Map.of("present",true,"reaches",path.canReach(),"nodes",path.getNodeCount(),"maxDrop",drop,"end",path.getEndNode().asBlockPos().toShortString(),"lastNodes",last);
    }
    private static List<Map<String,Object>> mealRoutes(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.monster.illager.Pillager donor){
        var result=new ArrayList<Map<String,Object>>();
        for(var v:level.getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class,donor.getBoundingBox().inflate(Math.min(128,NativeIllagers.radius(donor))),
                v->Villagers.isManaged(v)&&NativeIllagers.insideCamp(level,donor,v.blockPosition()))) {
            var stands=NativeCampMeals.givingStands(level,donor,v);var paths=new ArrayList<Map<String,Object>>();
            stands.sort(Comparator.comparingDouble(p->p.distSqr(donor.blockPosition())));
            for(var p:stands.stream().limit(32).toList()){
                var path=donor.getNavigation().createPath(p,0,64);net.minecraft.world.level.pathfinder.Path longer;
                try{donor.getNavigation().setMaxVisitedNodesMultiplier(32);longer=donor.getNavigation().createPath(p,0,128);}finally{donor.getNavigation().resetMaxVisitedNodesMultiplier();}
                paths.add(Map.of("position",p.toShortString(),"floor",level.getBlockState(p.below()).toString(),"nativeReachable",path!=null&&path.canReach(),
                    "longReachable",longer!=null&&longer.canReach(),"safe",NativeIllagers.safePath(level,donor,longer),
                    "end",path==null?"":path.getEndNode().asBlockPos().toShortString(),"longEnd",longer==null?"":longer.getEndNode().asBlockPos().toShortString()));
            }
            net.minecraft.world.level.pathfinder.Path actual=null;var attempts=new ArrayList<Map<String,Object>>();
            for(int attempt=0;attempt<Math.min(8,Math.max(1,(stands.size()+2)/3));attempt++){
                actual=CampBeds.pathToStands(level,donor,stands);
                attempts.add(Map.of("present",actual!=null,"nodes",actual==null?0:actual.getNodeCount(),"end",actual==null?"":actual.getEndNode().asBlockPos().toShortString()));
                if(actual!=null)break;
            }
            var row=new LinkedHashMap<String,Object>();
            row.put("planningAttempts",attempts);row.put("recipient",v.getUUID().toString());row.put("position",v.position().toString());
            row.put("visible",donor.hasLineOfSight(v));row.put("stands",stands.size());row.put("paths",paths);
            row.put("bodyPath",Map.of("present",actual!=null,"reaches",actual!=null&&actual.canReach(),"nodes",actual==null?0:actual.getNodeCount(),"end",actual==null?"":actual.getEndNode().asBlockPos().toShortString()));
            var gateStands=NativeCampMeals.meetingGateStands(level,donor,v.blockPosition());
            var gatePath=CampBeds.pathToStands(level,donor,gateStands);
            row.put("gateMeeting",Map.of("stands",gateStands.size(),"present",gatePath!=null,"nodes",gatePath==null?0:gatePath.getNodeCount(),"end",gatePath==null?"":gatePath.getEndNode().asBlockPos().toShortString(),"scope","One native gate-search batch; bodyPath records the recipient approach separately."));
            if(donor.getUUID().toString().startsWith("cb79")&&v.getUUID().toString().startsWith("573a")){
                var gateOptions=new ArrayList<Map<String,Object>>();
                for(var stand:gateStands){
                    var route=CampBeds.pathToStands(level,donor,List.of(stand));
                    gateOptions.add(Map.of("stand",stand.toShortString(),"floor",net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,stand),
                        "nodes",route==null?0:route.getNodeCount(),"end",route==null?"":route.getEndNode().asBlockPos().toShortString(),
                        "steps",route==null?List.of():java.util.stream.IntStream.range(0,route.getNodeCount()).mapToObj(i->route.getNodePos(i).toShortString()).toList()));
                }
                row.put("gateOptions",gateOptions);
                var scoped=new ArrayList<Map<String,Object>>();
                for(var stand:stands){
                    var raw=NativePathLimits.search(donor,2,()->donor.getNavigation().createPath(stand,0,256));
                    if(!NativeIllagers.safePath(level,donor,raw))continue;
                    var steps=new ArrayList<Map<String,Object>>();
                    var neighbors=level.getEntities(donor,donor.getBoundingBox().inflate(8),e->e instanceof net.minecraft.world.entity.LivingEntity&&e.isAlive());
                    for(int i=0;i<raw.getNodeCount();i++){
                        var at=raw.getNodePos(i);double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,at);
                        var feet=new net.minecraft.world.phys.Vec3(at.getX()+.5,floor,at.getZ()+.5);
                        var body=donor.getBoundingBox().move(feet.subtract(donor.position()));
                        steps.add(Map.of("node",at.toShortString(),"floor",floor,"blockedBy",neighbors.stream().filter(e->e.getBoundingBox().inflate(.05).intersects(body)).map(e->Map.of("id",e.getUUID().toString(),"position",e.position().toString(),"type",e.typeHolder().getRegisteredName())).toList()));
                    }
                    scoped.add(Map.of("stand",stand.toShortString(),"nodes",steps));
                    if(scoped.size()>=3)break;
                }
                row.put("limitedContactPaths",scoped);

            }

            if(v.getX()<-670||donor.getUUID().toString().startsWith("cb79")&&v.getUUID().toString().startsWith("764e")||donor.getUUID().toString().startsWith("cb79")&&v.getUUID().toString().startsWith("8889")){
                row.put("blockedHandoffs",blockedHandoffs(level,donor,v));
                var detours=new ArrayList<Map<String,Object>>();
                for(var stand:stands.stream().limit(3).toList()){
                    var detour=NativeBodyRoutes.search(level,donor,()->NativePathLimits.search(donor,2,()->{
                        try{donor.getNavigation().setMaxVisitedNodesMultiplier(128);return donor.getNavigation().createPath(stand,0,256);}
                        finally{donor.getNavigation().resetMaxVisitedNodesMultiplier();}
                    }));
                    detours.add(Map.of("stand",stand.toShortString(),"reaches",detour!=null&&detour.canReach(),"safe",NativeIllagers.safePath(level,donor,detour),
                        "nodes",detour==null?0:detour.getNodeCount(),"end",detour==null?"":detour.getEndNode().asBlockPos().toShortString()));
                }
                row.put("largerDetours",detours);
            }
            result.add(row);
        }
        return result;
    }
    /** Frozen operator geometry only. No opening, target or coordinates taught to any actor. */
    private static List<Map<String,Object>> blockedHandoffs(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.monster.illager.Pillager donor,net.minecraft.world.entity.npc.villager.Villager receiver){
        var rows=new ArrayList<Map<String,Object>>();var seen=new HashSet<String>();
        for(var at:BlockPos.betweenClosed(receiver.blockPosition().offset(-3,-2,-3),receiver.blockPosition().offset(3,1,3))){
            if(!level.isLoaded(at.above())||!level.getBlockState(at).isAir()||!level.getBlockState(at.above()).isAir())continue;
            double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,at);
            var feet=new net.minecraft.world.phys.Vec3(at.getX()+.5,floor,at.getZ()+.5);var body=donor.getBoundingBox().move(feet.subtract(donor.position()));
            if(!level.noCollision(donor,body)||level.noCollision(donor,body.move(0,-.03,0)))continue;
            var eye=feet.add(0,donor.getEyeHeight(),0);
            for(var end:CampFoodHatch.contactPoints(receiver.getBoundingBox())){
                if(eye.distanceToSqr(end)>9)continue;
                var ray=level.clip(new net.minecraft.world.level.ClipContext(eye,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,donor));
                if(ray.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK)continue;var wall=ray.getBlockPos();
                var after=level.clip(new net.minecraft.world.level.ClipContext(eye,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,donor){
                    @Override public net.minecraft.world.phys.shapes.VoxelShape getBlockShape(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.BlockGetter world,BlockPos p){
                        return p.equals(wall)?net.minecraft.world.phys.shapes.Shapes.empty():super.getBlockShape(state,world,p);
                    }
                });
                if(after.getType()!=net.minecraft.world.phys.HitResult.Type.MISS||!seen.add(at.toShortString()+":"+wall.toShortString()))continue;
                var stand=at.immutable();var path=NativeBodyRoutes.search(level,donor,()->NativePathLimits.search(donor,2,()->{
                    try{donor.getNavigation().setMaxVisitedNodesMultiplier(32);return donor.getNavigation().createPath(stand,0,128);}
                    finally{donor.getNavigation().resetMaxVisitedNodesMultiplier();}
                }));
                rows.add(Map.of("stand",stand.toShortString(),"floor",floor,"wall",wall.toShortString(),"block",level.getBlockState(wall).toString(),
                    "above",level.getBlockState(wall.above()).toString(),"below",level.getBlockState(wall.below()).toString(),"bodyClear",true,
                    "nativeReachable",NativeIllagers.safePath(level,donor,path),"reach",eye.distanceTo(end)));
                if(rows.size()>=24)return rows;break;
            }
        }
        return rows;
    }
    /** Operator-only geometry: never teaches these controls to an actor. */
    private static List<Map<String,Object>> gateSurvey(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.monster.illager.Pillager donor){
        var home=NativeIllagers.home(donor);if(home==null)return List.of();var levers=new ArrayList<BlockPos>();
        int radius=NativeIllagers.radius(donor);
        for(var p:BlockPos.betweenClosed(new BlockPos(home.x()-radius,donor.getBlockY()-4,home.z()-radius),
            new BlockPos(home.x()+radius,donor.getBlockY()+12,home.z()+radius)))
            if(level.isLoaded(p)&&(level.getBlockState(p).getBlock() instanceof LeverBlock||level.getBlockState(p).getBlock() instanceof FenceGateBlock)&&NativeIllagers.insideCamp(level,donor,p))levers.add(p.immutable());
        levers.sort(Comparator.comparingDouble(p->p.distSqr(donor.blockPosition())));var rows=new ArrayList<Map<String,Object>>();
        for(var lever:levers.stream().limit(24).toList()){
            var doors=new ArrayList<String>();
            for(var p:BlockPos.betweenClosed(lever.offset(-2,-2,-2),lever.offset(2,1,2)))
                if(level.getBlockState(p).is(Blocks.IRON_DOOR)&&level.getBlockState(p).getValue(DoorBlock.HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER)
                    doors.add(p.toShortString()+" "+level.getBlockState(p));
            if(level.getBlockState(lever).getBlock() instanceof FenceGateBlock)doors.add("public fence gate");
            if(doors.isEmpty())continue;var stands=new ArrayList<BlockPos>();
            for(var p:BlockPos.betweenClosed(lever.offset(-3,-2,-3),lever.offset(3,1,3))){
                if(!level.isLoaded(p.above())||!level.getFluidState(p).isEmpty()||!level.getFluidState(p.below()).isEmpty())continue;
                double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,p);var feet=new net.minecraft.world.phys.Vec3(p.getX()+.5,floor,p.getZ()+.5);
                var body=donor.getBoundingBox().move(feet.subtract(donor.position()));
                if(!level.noCollision(donor,body)||level.noCollision(donor,body.move(0,-.03,0)))continue;
                if(BlockContact.interactionPoint(level,donor,feet.add(0,donor.getEyeHeight(),0),lever,3)!=null)stands.add(p.immutable());
            }
            var paths=new ArrayList<Map<String,Object>>();stands.sort(Comparator.comparingDouble(p->p.distSqr(donor.blockPosition())));
            for(var p:stands.stream().limit(5).toList()){
                net.minecraft.world.level.pathfinder.Path path;
                try{donor.getNavigation().setMaxVisitedNodesMultiplier(32);path=NativePathLimits.search(donor,2,()->donor.getNavigation().createPath(p,0,128));}
                finally{donor.getNavigation().resetMaxVisitedNodesMultiplier();}
                var last=new ArrayList<String>();if(path!=null)for(int n=Math.max(0,path.getNodeCount()-5);n<path.getNodeCount();n++)last.add(path.getNodePos(n).toShortString());
                paths.add(Map.of("stand",p.toShortString(),"reaches",path!=null&&path.canReach(),"safe",NativeIllagers.safePath(level,donor,path),"nodes",path==null?0:path.getNodeCount(),"lastNodes",last));
                if(NativeIllagers.safePath(level,donor,path))break;
            }
            rows.add(Map.of("lever",lever.toShortString(),"state",level.getBlockState(lever).toString(),"doors",doors,"stands",stands.size(),"paths",paths));
        }
        return rows;
    }
    private static Map<?,?> entrapment(net.minecraft.world.entity.Mob mob){
        try{return (Map<?,?>)Class.forName("local.villagers.NativeEntrapment").getMethod("status",net.minecraft.world.entity.Mob.class).invoke(null,mob);}
        catch(ClassNotFoundException|NoSuchMethodException absent){return Map.of("incidents",0);}
        catch(ReflectiveOperationException error){throw new IllegalStateException("Native entrapment observation",error);}
    }
    private static Map<String,Object> navigation(net.minecraft.world.entity.Mob mob){
        var path=mob.getNavigation().getPath();var control=mob.getMoveControl();
        return Map.of("position",mob.position().toString(),"onGround",mob.onGround(),"velocity",mob.getDeltaMovement().toString(),
            "wanted",List.of(control.getWantedX(),control.getWantedY(),control.getWantedZ()),
            "path",path==null?Map.of():Map.of("nodes",path.getNodeCount(),"next",path.isDone()?"":path.getNextNodePos().toShortString(),
                "nextIndex",path.getNextNodeIndex(),"end",path.getEndNode().asBlockPos().toShortString(),"done",path.isDone()));
    }
    @Override public void onInitialize(){
        // Apply the pathfinder mixins before opening any copied world. A
        // linkage error must fail startup, never masquerade as absent bodies.
        try{Class.forName("net.minecraft.world.level.pathfinder.WalkNodeEvaluator");}
        catch(ClassNotFoundException error){throw new IllegalStateException("Native pathfinder unavailable",error);}
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            if(!Files.exists(FabricLoader.getInstance().getGameDir().resolve("food-diagnostic-copy.json")))throw new IllegalStateException("marked COPY required");
            time=server.overworld().getGameTime();server.tickRateManager().setFrozen(true);
            ProbeResidents.load(server.overworld());
            if(rehearse)server.tickRateManager().setTickRate(Integer.getInteger("villagers.farm.tps",20));
            for(var center:List.of(new BlockPos(-656,70,-656),new BlockPos(680,70,24),new BlockPos(-825,70,40)))
                for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){
                    int x=(center.getX()>>4)+dx,z=(center.getZ()>>4)+dz;server.overworld().setChunkForced(x,z,true);server.overworld().getChunk(x,z);
                }
            forceCampAreas(server.overworld());
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(done)return;
            if(NativeCareStartup.failed(server)){done=true;server.halt(false);return;}
            if(NativeCareStartup.active(server)&&!NativeCareStartup.ready(server))return;
            if(++ticks<40)return;
            var output=FabricLoader.getInstance().getGameDir().resolve("native-illager-habitats.json");
            try{
                var level=server.overworld();
                if(ticks==40){
                    var materials=new net.minecraft.world.SimpleContainer(5);
                    materials.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DARK_OAK_LOG));
                    materials.setItem(1,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STRING,12));
                    var steps=new ArrayList<String>();
                    for(int i=0;i<5;i++){
                        var plan=BedCrafting.next(materials);if(plan==null)throw new AssertionError("complete bed recipe plan");
                        var recipe=level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,plan.input(),level);
                        if(recipe.isEmpty()||!BedCrafting.commit(materials,plan,recipe.get().value().assemble(plan.input())))throw new AssertionError("native bed recipe output and conserved ingredients");
                        steps.add(plan.output().toString());
                    }
                    if(materials.countItem(net.minecraft.world.item.Items.BED.white())!=1||materials.countItem(net.minecraft.world.item.Items.DARK_OAK_PLANKS)!=1)throw new AssertionError("one bed plus one spare plank only");
                    Files.writeString(output.resolveSibling("native-bed-recipe-audit.json"),new Gson().toJson(Map.of("steps",steps,"result",materials.toString(),"scope","Separate in-memory recipe fixture only. No body or world supplies changed; not evidence of camp production.")));
                }
                if(!rehearse&&ticks==40&&Boolean.getBoolean("villagers.illagers.enrollFrozen")){
                    var before=new HashMap<UUID,String>();
                    for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.monster.illager.Pillager guard)
                        before.put(guard.getUUID(),guard.getHealth()+":"+guard.getInventory()+":"+guard.getMainHandItem()+":"+guard.getOffhandItem());
                    int enrolled=NativeIllagers.enroll(level);if(NativeIllagers.founders().isEmpty())throw new AssertionError("existing resource-ready native pair required");
                    forceCampAreas(level);
                    for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.monster.illager.Pillager guard&&before.containsKey(guard.getUUID()))
                        if(!Objects.equals(before.get(guard.getUUID()),guard.getHealth()+":"+guard.getInventory()+":"+guard.getMainHandItem()+":"+guard.getOffhandItem()))throw new AssertionError("enrollment must preserve actual bodies and supplies");
                    Files.writeString(output.resolveSibling("frozen-camp-enrollment.json"),new Gson().toJson(Map.of("enrolledExisting",enrolled,"founders",NativeIllagers.founders(),"spawnedBodies",0,"suppliedItems",0)));
                }
                if(!rehearse&&ticks==40&&!System.getProperty("villagers.illagers.recruitWorker","").isEmpty()){
                    var before=new HashMap<UUID,String>();
                    for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.monster.illager.Pillager p)
                        before.put(p.getUUID(),p.position()+":"+p.getHealth()+":"+p.isNoAi()+":"+p.isInvulnerable()+":"+p.getInventory()+":"+p.getMainHandItem()+":"+p.getOffhandItem());
                    var foundersBefore=NativeIllagers.founders();
                    var receipt=NativeIllagers.recruitWorker(level,UUID.fromString(System.getProperty("villagers.illagers.recruitWorker")));
                    if(((Number)receipt.get("enrolledExisting")).intValue()!=1)throw new AssertionError("one existing worker with real resource access required: "+receipt);
                    if(!foundersBefore.equals(NativeIllagers.founders()))throw new AssertionError("founder and death history unchanged");
                    for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.monster.illager.Pillager p&&before.containsKey(p.getUUID()))
                        if(!before.get(p.getUUID()).equals(p.position()+":"+p.getHealth()+":"+p.isNoAi()+":"+p.isInvulnerable()+":"+p.getInventory()+":"+p.getMainHandItem()+":"+p.getOffhandItem()))
                            throw new AssertionError("existing worker adoption must preserve bodies and stock");
                    Files.writeString(output.resolveSibling("frozen-worker-enrollment.json"),new GsonBuilder().setPrettyPrinting().create().toJson(receipt));
                }
                if(rehearse){
                    if(!started){
                        if(!Villagers.ready())return;
                        var before=new HashMap<UUID,String>();for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.monster.illager.Pillager p)
                            before.put(p.getUUID(),p.getInventory().toString()+p.getMainHandItem()+p.getOffhandItem());
                        boolean resume=!NativeIllagers.founders().isEmpty();
                        int enrolled=NativeIllagers.enroll(level);if(resume?enrolled!=0:enrolled<2)throw new AssertionError("existing founders retained, or one actual resource-ready founding pair required");
                        forceCampAreas(level);
                        // Preserve the recorded history, including prior deaths.
                        // This window checks the founders that actually survived
                        // into this save; it never resurrects a recorded founder.
                        for(var id:NativeIllagers.founders())if(level.getEntity(id) instanceof AbstractIllager body&&body.isAlive())founders.add(id);
                        Files.writeString(output.resolveSibling("native-founder-history.json"),new Gson().toJson(Map.of(
                            "recorded",NativeIllagers.founders(),"aliveAtWindowStart",founders,
                            "absentAtWindowStart",NativeIllagers.founders().stream().filter(id->!founders.contains(id)).toList(),
                            "scope","Historical absent founders remain absent. No replacement or resurrection.")));
                        int adoptedVillagers=0;
                        var residentStartup=new ArrayList<Map<String,Object>>();
                        for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.npc.villager.Villager v&&v.isAlive()){
                            float health=v.getHealth();String inventory=v.getInventory().toString(),held=v.getMainHandItem().toString()+v.getOffhandItem();
                            if(NativeRegionsScenario.enrollResident(level,v))adoptedVillagers++;
                            // Restore the real persisted metabolism while still
                            // frozen. A 1-HP fed resident must not be judged as
                            // having an absent mind while its async load waits.
                            if(Villagers.isManaged(v)){
                                var resident=Villagers.session(v,level);
                                residentStartup.add(Map.of("id",v.getUUID().toString(),"health",v.getHealth(),
                                    "food",resident.mind.foodLevel(),"metabolism",resident.mind.snapshot().food()));
                            }
                            if(v.getHealth()!=health||!inventory.equals(v.getInventory().toString())||!held.equals(v.getMainHandItem().toString()+v.getOffhandItem()))throw new AssertionError("Native resident enrollment preserves actual body health and supplies");
                        }
                    for(var e:level.getAllEntities())if(e instanceof AbstractIllager mob&&CreatureAgents.managed(mob)&&NativeIllagers.member(mob)){
                            initialActors.add(mob.getUUID());var p=(net.minecraft.world.entity.monster.illager.Pillager)mob;
                            CreatureAgents.status(mob);initialAges.put(mob.getUUID(),CreatureAgents.loadedState(mob).mind.ageTicks());
                            if(!Objects.equals(before.get(mob.getUUID()),p.getInventory().toString()+p.getMainHandItem()+p.getOffhandItem()))throw new AssertionError("no enrollment supplies");
                        }
                        int again=NativeIllagers.enroll(level);if(again!=0)throw new AssertionError("founders cannot be replenished");
                        for(int x=-44;x<=-36;x++)for(int z=-44;z<=-33;z++){level.setChunkForced(x,z,true);level.getChunk(x,z);}
                        var beds=new ArrayList<Map<String,Object>>();
                        var pois=level.getPoiManager().findAll(t->t.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME),p->true,new BlockPos(-640,73,-606),128,
                            net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY).collect(java.util.stream.Collectors.toSet());
                        var freePois=level.getPoiManager().findAll(t->t.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME),p->true,new BlockPos(-640,73,-606),128,
                            net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.HAS_SPACE).collect(java.util.stream.Collectors.toSet());
                        for(var p:BlockPos.betweenClosed(new BlockPos(-708,38,-688),new BlockPos(-573,107,-524))){
                            if(!level.isLoaded(p))continue;var b=level.getBlockState(p);
                            if(b.getBlock() instanceof BedBlock&&b.getValue(BedBlock.PART)==net.minecraft.world.level.block.state.properties.BedPart.HEAD)
                                beds.add(Map.of("position",p.toShortString(),"above",level.getBlockState(p.above()).toString(),"above2",level.getBlockState(p.above(2)).toString(),
                                    "sky",level.canSeeSky(p.above()),"homePoi",pois.contains(p),"free",freePois.contains(p),"protected",ConstructionSites.isProtected(level,p),
                                    "reusableByFounders",founders.stream().filter(id->level.getEntity(id) instanceof net.minecraft.world.entity.monster.illager.Pillager mob&&CampBeds.reusable(level,mob,p)).map(UUID::toString).toList()));
                        }
                        Files.writeString(output.resolveSibling("native-illager-beds.json"),new GsonBuilder().setPrettyPrinting().create().toJson(beds));
                        Files.writeString(output.resolveSibling("native-illager-initial.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("gameTime",time,"founders",founders,"initialActors",initialActors,"existingPillagers",before.size(),"enrolled",enrolled,"resumed",resume,"adoptedExistingVillagers",adoptedVillagers,"scope","Existing bodies retained; inventory and equipment unchanged; record persisted before adoption. Resume keeps saved founders and native supplies. Native resident enrollment matches the client; no birth counted for adoption.")));
                        Files.writeString(output.resolveSibling("native-resident-startup.json"),new GsonBuilder().setPrettyPrinting().create().toJson(residentStartup));
                        started=true;
                        server.tickRateManager().setTickRate(Integer.getInteger("villagers.farm.tps",20));
                        if(NativeCareStartup.active(server))NativeCareStartup.resume(server);
                        else server.tickRateManager().setFrozen(false);
                        return;
                    }
                    long elapsed=level.getGameTime()-time;
                    for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.LivingEntity body
                        &&(body instanceof net.minecraft.world.entity.npc.villager.Villager||body instanceof AbstractIllager)
                        &&healthStop(body)){
                        var stop=Map.of("status","failed","stage",body.getAirSupply()<=0?"air_stop":"health_stop",
                            "elapsed",elapsed,"gameTime",level.getGameTime(),"actor",body.getUUID().toString(),
                            "health",body.getHealth(),"air",body.getAirSupply(),"position",body.position().toString());
                        server.tickRateManager().setFrozen(true);
                        Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(stop));
                        Files.writeString(output.resolveSibling("native-emergency-stop.json"),new GsonBuilder().setPrettyPrinting().create().toJson(stop));
                        done=true;server.halt(false);return;
                    }
                    if(elapsed%40!=0)return;
                        var adventurers=new ArrayList<Map<String,Object>>();
                    for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.npc.villager.Villager v&&AdventurePreparation.nominated(v)){
                        var s=Villagers.existingSession(v.getUUID());if(s==null)continue;
                        var adventurer=new LinkedHashMap<String,Object>(Map.of("id",v.getUUID().toString(),"position",v.position().toString(),"health",v.getHealth(),"food",s.mind.foodLevel(),
                            "ageTicks",s.mind.ageTicks(),"mainhand",v.getMainHandItem().toString(),"inventory",HarvestMaterials.snapshot(v.getInventory()).stream().map(Object::toString).toList(),
                            "iron",AdventureIron.status(s),"armor",AdventureArmor.status(v,s),"navigation",navigation(v)));
                        adventurer.put("quarry",AdventureQuarry.status(s));
                        adventurer.put("bedding",VillageBedding.status(s));
                        adventurer.put("foodWork",Map.of("activity",s.foodStatus,"ownFoodGoal",s.selfFoodGoal,
                            "familyFoodGoal",s.familyFoodGoal,"wheatTarget",s.stockWheatTarget,
                            "provisionStarted",s.familyProvisionStarted,"nextProvision",s.nextFamilyProvision,
                            "farmJourney",s.farmJourney.active(),"nextFarmPath",s.nextFarmPath,
                            "crop",s.cropTarget==null?"":s.cropTarget.toShortString()));
                        adventurer.put("family",Map.of("status",s.familyStatus,"foodGoal",s.familyFoodGoal,
                            "courtship",s.courtship==null?"":s.courtship.toString()));
                        adventurers.add(adventurer);
                    }
                    Files.writeString(output.resolveSibling("native-adventurer-progress.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("gameTime",level.getGameTime(),"elapsed",elapsed,"actors",adventurers)));
                    if(elapsed%400==0)Files.writeString(output.resolveSibling("native-adventurer-timeline.jsonl"),new Gson().toJson(Map.of("gameTime",level.getGameTime(),"elapsed",elapsed,"actors",adventurers))+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);

                    var actors=new ArrayList<Map<String,Object>>();boolean birth=false,healthStop=false,residencyStop=false;
                    var criticalVillagers=new ArrayList<String>();
                    var rescues=new ArrayList<Map<?,?>>();
                    for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.Mob mob){
                        var rescue=entrapment(mob);if(rescue.get("incidents") instanceof Number count&&count.intValue()>0)rescues.add(rescue);
                    }
                    if(!rescues.isEmpty())Files.writeString(output.resolveSibling("native-entrapment-progress.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("gameTime",level.getGameTime(),"rescues",rescues)));

                    var villagerStores=new ArrayList<Map<String,Object>>();
                    for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.npc.villager.Villager v&&Villagers.isManaged(v)&&v.isAlive()&&healthStop(v)){
                        criticalVillagers.add(v.getUUID().toString());healthStop=true;
                    }
                    for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.npc.villager.Villager v&&Villagers.isManaged(v)&&v.isAlive()){
                        var s=Villagers.existingSession(v.getUUID());if(s!=null)villagerStores.add(Map.of("id",v.getUUID().toString(),"name",s.mind.name(),"health",v.getHealth(),"food",s.mind.foodLevel(),
                            "position",v.blockPosition().toShortString(),"stores",NativeFoodStorage.status(s),"campMeals",Map.of("requests",s.campMealRequests,"lastReceived",s.lastFoodReceived),"body",Map.of("baby",v.isBaby(),"sleeping",v.isSleeping(),"trading",v.isTrading(),"water",v.isInWater(),"fire",v.isOnFire(),"passenger",v.isPassenger(),
                                "home",String.valueOf(s.mind.home()),"ownedMeal",FoodInventory.anySlot(v.getInventory(),AdventureActions.canHarvest(v)),
                                "activity",v.getName().getString(),"gateWait",Map.of("until",s.campMealWaitUntil,
                                    "stand",s.campMealStand==null?"":s.campMealStand.toShortString(),"door",s.campMealDoor==null?"":s.campMealDoor.toShortString())),"survival",VillageSurvival.status(v.getUUID()),"navigation",navigation(v)));
                    }
                    for(var e:level.getAllEntities())if(e instanceof AbstractIllager mob&&CreatureAgents.managed(mob)&&NativeIllagers.member(mob)){
                        var row=new LinkedHashMap<>(CreatureAgents.status(mob));row.put("position",mob.blockPosition().toShortString());actors.add(row);
                        long observed=CreatureAgents.loadedState(mob).mind.ageTicks()-initialAges.getOrDefault(mob.getUUID(),CreatureAgents.loadedState(mob).mind.ageTicks());
                        row.put("observedTicks",observed);
                        var nav=mob.getNavigation().getPath();
                        var nextNodes=new ArrayList<String>();
                        if(nav!=null)for(int n=nav.getNextNodeIndex();n<Math.min(nav.getNodeCount(),nav.getNextNodeIndex()+3);n++)nextNodes.add(nav.getNodePos(n).toShortString());
                        row.put("nativeMovement",Map.of("feet",mob.position().toString(),"onGround",mob.onGround(),
                            "velocity",mob.getDeltaMovement().toString(),"pathDone",nav==null||nav.isDone(),
                            "pathTarget",nav==null?"":nav.getTarget().toShortString(),"nextNode",nav==null?-1:nav.getNextNodeIndex(),
                            "nodes",nav==null?0:nav.getNodeCount(),"nextNodes",nextNodes,
                            "wanted",List.of(mob.getMoveControl().getWantedX(),mob.getMoveControl().getWantedY(),mob.getMoveControl().getWantedZ()),
                            "rotation",mob.getYRot()));
                        if(founders.contains(mob.getUUID())&&elapsed-observed>80)residencyStop=true;
                        if(!initialActors.contains(mob.getUUID()))birth=true;if(mob.getHealth()<=Integer.getInteger("villagers.illagers.minHealth",4))healthStop=true;
                    }
                    for(var id:founders)if(!(level.getEntity(id) instanceof AbstractIllager mob)||!mob.isAlive())throw new AssertionError("founder remains alive: "+id);
                    var data=Map.of("status",healthStop||residencyStop?"failed":"passed","stage",birth?"native_illager_birth":healthStop?"health_stop":residencyStop?"residency_stop":elapsed>=window||Files.exists(output.resolveSibling("stop-observation"))?"bounded_observation":"observing",
                        "elapsed",elapsed,"gameTime",level.getGameTime(),"founders",founders,"actors",actors,"targetTps",server.tickRateManager().tickrate(),"criticalVillagers",criticalVillagers,"villagerFoodStores",villagerStores,
                        "scope","Existing native illager founders; normal resource work and paid breeding only. No body, food, health, inventory, crop or resource setters; copied world only.");
                    Files.writeString(output.resolveSibling("native-illager-progress.json"),new GsonBuilder().setPrettyPrinting().create().toJson(data));
                    Files.writeString(output.resolveSibling("native-illager-timeline.jsonl"),new Gson().toJson(data)+"\n",
                        java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);
                    if(!birth&&!healthStop&&!residencyStop&&elapsed<window&&!Files.exists(output.resolveSibling("stop-observation")))return;
                    server.tickRateManager().setFrozen(true);
                    Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(data));done=true;server.halt(false);return;
                }
                if(level.getGameTime()!=time)throw new AssertionError("frozen audit clock");
                var foodChecks=new ArrayList<Map<String,Object>>();
                for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.npc.villager.Villager v&&Villagers.isManaged(v)&&v.getX()<-570&&v.getX()>-710&&v.getZ()<-524){
                    var s=Villagers.session(v,level);var stores=new ArrayList<Map<String,Object>>();var origin=v.blockPosition();
                    for(var p:BlockPos.betweenClosed(origin.offset(-16,-5,-16),origin.offset(16,5,16))){
                        if(!level.isLoaded(p)||!(level.getBlockEntity(p) instanceof net.minecraft.world.level.block.entity.BaseContainerBlockEntity))continue;
                        stores.add(Map.of("position",p.toShortString(),"accessible",NativeContainers.accessible(level,p)!=null,"homeScope",NativeFoodStorage.homeStructure(level,v,s,p),
                            "visible",NativeFoodStorage.visible(level,v,p),"eyeDistance2",v.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p))));
                    }
                    foodChecks.add(Map.of("id",v.getUUID().toString(),"home",String.valueOf(s.mind.home()),"position",v.blockPosition().toShortString(),"stores",stores));
                }
                Files.writeString(output.resolveSibling("native-villager-storage-audit.json"),new GsonBuilder().setPrettyPrinting().create().toJson(foodChecks));
                var rows=new ArrayList<Map<String,Object>>();var camps=new LinkedHashMap<String,Map<String,Object>>();
                for(var e:level.getAllEntities())if(e instanceof AbstractIllager mob){
                    var start=level.structureManager().getStructureWithPieceAt(mob.blockPosition(),h->true);
                    String key=start.isValid()?String.valueOf(level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(start.getStructure())):"none";
                    var row=new LinkedHashMap<String,Object>();row.put("id",mob.getUUID().toString());row.put("type",mob.typeHolder().getRegisteredName());
                    row.put("position",mob.blockPosition().toShortString());row.put("health",mob.getHealth());row.put("tags",mob.entityTags());row.put("structure",key);rows.add(row);
                    if(NativeIllagers.member(mob)){
                        if(mob instanceof net.minecraft.world.entity.monster.illager.Pillager p)row.put("bedChecks",List.of(
                            CampBeds.inspect(level,p,new BlockPos(-664,65,-632)),CampBeds.inspect(level,p,new BlockPos(-664,65,-630)),
                            CampBeds.inspect(level,p,new BlockPos(-664,64,-632)),CampBeds.inspect(level,p,new BlockPos(-664,64,-630)),
                            CampBeds.inspect(level,p,new BlockPos(-652,74,-632))));
                        var doorPaths=new ArrayList<Map<String,Object>>();
                        mob.getNavigation().setCanOpenDoors(true);mob.getNavigation().getNodeEvaluator().setCanPassDoors(true);
                        for(var destination:List.of(new BlockPos(-658,64,-627),new BlockPos(-659,64,-627),new BlockPos(-660,64,-627),new BlockPos(-661,64,-631))){
                            net.minecraft.world.level.pathfinder.Path route;
                            try{mob.getNavigation().setMaxVisitedNodesMultiplier(32);route=mob.getNavigation().createPath(destination,0,128);}finally{mob.getNavigation().resetMaxVisitedNodesMultiplier();}
                            var context=new net.minecraft.world.level.pathfinder.PathfindingContext(level,mob);
                            var evaluator=(net.minecraft.world.level.pathfinder.WalkNodeEvaluator)mob.getNavigation().getNodeEvaluator();
                            var endNodes=new ArrayList<String>();if(route!=null)for(int n=Math.max(0,route.getNodeCount()-6);n<route.getNodeCount();n++)endNodes.add(route.getNodePos(n).toShortString());
                            doorPaths.add(Map.of("destination",destination.toShortString(),"reachable",route!=null&&route.canReach(),"lastNodes",endNodes,
                                "block",level.getBlockState(destination).toString(),"above",level.getBlockState(destination.above()).toString(),"below",level.getBlockState(destination.below()).toString(),
                                "rawType",evaluator.getPathType(context,destination.getX(),destination.getY(),destination.getZ()).name(),
                                "canOpenDoors",evaluator.canOpenDoors(),"canPassDoors",evaluator.canPassDoors()));
                        }
                        row.put("doorPaths",doorPaths);
                        var threshold=new ArrayList<Map<String,Object>>();
                        for(var at:BlockPos.betweenClosed(new BlockPos(-669,62,-631),new BlockPos(-655,68,-623))){
                            var block=level.getBlockState(at);double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,at);
                            var feet=new net.minecraft.world.phys.Vec3(at.getX()+.5,floor,at.getZ()+.5);var body=mob.getBoundingBox().move(feet.subtract(mob.position()));
                            threshold.add(Map.of("position",at.toShortString(),"block",block.toString(),"floor",floor,
                                "clear",level.noBlockCollision(mob,body),"supported",!level.noBlockCollision(mob,body.move(0,-.03,0))));
                        }
                        row.put("doorThresholdCells",threshold);

                        if(mob instanceof net.minecraft.world.entity.monster.illager.Pillager donor){
                            if(CreatureAgents.managed(donor)&&NativeIllagers.member(donor))row.put("nativePathCache",nativePathCache(level,donor));
                            row.put("nativeBody",Map.of("inWall",donor.isInWall(),"clear",level.noBlockCollision(donor,donor.getBoundingBox()),
                                "onGround",donor.onGround(),"feet",donor.position().toString(),"bounds",donor.getBoundingBox().toString(),
                                "pathStartType",((net.minecraft.world.level.pathfinder.WalkNodeEvaluator)donor.getNavigation().getNodeEvaluator())
                                    .getPathType(new net.minecraft.world.level.pathfinder.PathfindingContext(level,donor),donor.getBlockX(),donor.getBlockY(),donor.getBlockZ()).name()));
                            var storageChecks=new ArrayList<Map<String,Object>>();var here=donor.blockPosition();
                            for(var at:BlockPos.betweenClosed(here.offset(-12,-3,-12),here.offset(12,3,12))){
                                if(!level.isLoaded(at)||NativeContainers.accessible(level,at)==null)continue;
                                var route=IllagerFood.approachPath(level,donor,at);
                                storageChecks.add(Map.of("position",at.toShortString(),"visible",NativeCampWork.visible(level,donor,at),
                                    "centerDistance",donor.getEyePosition().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(at)),
                                    "contact",BlockContact.interactionPoint(level,donor,donor.getEyePosition(),at,3)!=null,
                                    "path",Map.of("present",route!=null,"reaches",route!=null&&route.canReach(),"end",route==null?"":route.getEndNode().asBlockPos().toShortString())));
                            }
                            row.put("nearbyStorage",storageChecks);
                            var callPaths=new ArrayList<Map<String,Object>>();
                            for(var person:level.getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class,donor.getBoundingBox().inflate(72),Villagers::isManaged)){
                                if(!NativeIllagers.insideCamp(level,donor,person.blockPosition()))continue;
                                var callPath=NativeCampMeals.patrolPath(level,donor,person.blockPosition());
                                var nodes=new ArrayList<Map<String,Object>>();
                                if(callPath!=null)for(int i=0;i<Math.min(10,callPath.getNodeCount());i++){
                                    var at=callPath.getNodePos(i);nodes.add(Map.of("position",at.toShortString(),"block",level.getBlockState(at).toString(),
                                        "above",level.getBlockState(at.above()).toString(),"below",level.getBlockState(at.below()).toString()));
                                }
                                callPaths.add(Map.of("caller",person.getUUID().toString(),"position",person.position().toString(),
                                    "reachable",callPath!=null&&callPath.canReach(),"nodes",nodes));
                            }
                            row.put("callPaths",callPaths);
                            var bodyState=new NativeBodyRoutes.State();
                            var avoided=NativeBodyRoutes.search(level,donor,bodyState,level.getGameTime(),()->NativeCampMeals.patrolPath(level,donor,donor.blockPosition().offset(40,0,2)));
                            row.put("bodyRouteTrial",Map.of("status",bodyState.status(),"reachable",avoided!=null&&avoided.canReach(),"nodes",avoided==null?List.of():java.util.stream.IntStream.range(0,Math.min(20,avoided.getNodeCount())).mapToObj(i->avoided.getNodePos(i).toShortString()).toList()));
                            row.put("nearbyBodies",level.getEntities(donor,donor.getBoundingBox().inflate(2.5)).stream().map(other->Map.of(
                                "id",other.getUUID().toString(),"type",other.typeHolder().getRegisteredName(),"position",other.position().toString(),
                                "bounds",other.getBoundingBox().toString())).toList());
                            row.put("mealRoutes",mealRoutes(level,donor));
                            row.put("gateSurvey",gateSurvey(level,donor));
                            var tables=new ArrayList<Map<String,Object>>();
                            for(var table:NativeIllagers.knownTables(level,donor)){
                                var route=IllagerFood.approachPath(level,donor,table);
                                tables.add(Map.of("position",table.toShortString(),"nodes",route==null?0:route.getNodeCount(),
                                    "safe",NativeIllagers.safePath(level,donor,route),"end",route==null?"":route.getEndNode().asBlockPos().toShortString()));
                            }
                            row.put("tableRoutes",tables);
                            var hayRoutes=new ArrayList<Map<String,Object>>();
                            for(var hay:NativeIllagers.knownHay(level,donor)){
                                var route=IllagerFood.approachPath(level,donor,hay);
                                net.minecraft.world.level.pathfinder.Path area=null;int queries=0;
                                while(queries<6){queries++;area=IllagerFood.hayLandmarkPath(level,donor,hay);if(area!=null)break;}
                                hayRoutes.add(Map.of("position",hay.toShortString(),"block",level.getBlockState(hay).toString(),
                                    "visible",NativeCampWork.visible(level,donor,hay),"safe",NativeIllagers.safePath(level,donor,route),
                                    "end",route==null?"":route.getEndNode().asBlockPos().toShortString(),
                                    "areaQueries",queries,"areaSafe",NativeIllagers.safePath(level,donor,area),
                                    "areaNodes",area==null?0:area.getNodeCount(),"areaEnd",area==null?"":area.getEndNode().asBlockPos().toShortString()));
                            }
                            row.put("rememberedHayRoutes",hayRoutes);
                            var observedHay=new ArrayList<Map<String,Object>>();
                            for(var at:BlockPos.betweenClosed(here.offset(-16,-3,-16),here.offset(16,3,16))){
                                if(!level.isLoaded(at)||!level.getBlockState(at).is(Blocks.HAY_BLOCK)||!level.getBlockState(at.above()).isAir()
                                    ||!NativeCampWork.visible(level,donor,at))continue;
                                var route=IllagerFood.approachPath(level,donor,at);
                                observedHay.add(Map.of("position",at.toShortString(),"safe",NativeIllagers.safePath(level,donor,route),
                                    "end",route==null?"":route.getEndNode().asBlockPos().toShortString()));
                            }
                            row.put("locallyVisibleHayRoutes",observedHay);
                            row.put("farmSurvey",farmSurvey(level,donor));
                        }
                        var checks=new ArrayList<Map<String,Object>>();
                        for(var p:List.of(new BlockPos(-638,63,-670),new BlockPos(-640,63,-666),new BlockPos(-661,67,-644),new BlockPos(-676,65,-609))){
                            if(!level.isLoaded(p))continue;var b=level.getBlockState(p);
                            var any=level.structureManager().getStructureWithPieceAt(p,h->true);
                            checks.add(Map.of("position",p.toShortString(),"insideCamp",NativeIllagers.insideCamp(level,mob,p),
                                "anyStructure",any.isValid()?String.valueOf(level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(any.getStructure())):"none",
                                "above",level.getBlockState(p.above()).toString(),"openable",b.getBlock() instanceof ChestBlock chest&&ChestBlock.getContainer(chest,b,level,p,false)!=null,
                                "safeNativePath",NativeIllagers.safePath(level,mob,mob.getNavigation().createPath(p,1,64))));
                        }
                        row.put("storageChecks",checks);
                    }
                    if(!start.isValid())continue;
                    var box=start.getBoundingBox();String camp=key+":"+box.toString();
                    if(camps.containsKey(camp))continue;
                    var resources=new ArrayList<Map<String,Object>>();var origin=box.getCenter();
                    for(var p:BlockPos.betweenClosed(origin.offset(-32,-24,-32),origin.offset(32,24,32))){
                        if(!level.isLoaded(p))continue;var b=level.getBlockState(p);
                        if(!(b.getBlock() instanceof BedBlock)&&!(b.getBlock() instanceof CropBlock)&&!b.is(Blocks.HAY_BLOCK)
                            &&!b.is(Blocks.CRAFTING_TABLE)&&!b.is(Blocks.CHEST)&&!b.is(net.minecraft.tags.BlockTags.WOOL))continue;
                        if(resources.size()<240)resources.add(Map.of("position",p.toShortString(),"state",b.toString()));
                    }
                    camps.put(camp,Map.of("structure",key,"center",origin.toShortString(),"bounds",box.toString(),"resources",resources));
                }
                var captives=new ArrayList<Map<String,Object>>();
                for(var entity:level.getAllEntities())if(entity instanceof net.minecraft.world.entity.npc.villager.Villager v&&Villagers.isManaged(v)
                    &&NativeIllagers.areas().stream().anyMatch(a->Math.abs(v.getX()-a.x())<=a.radius()&&Math.abs(v.getZ()-a.z())<=a.radius()))captives.add(captiveAccess(level,v));
                Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("status","passed","gameTime",time,"illagers",rows,"camps",camps.values(),"captiveFoodAccess",captives,"scope",Boolean.getBoolean("villagers.illagers.enrollFrozen")?"Frozen existing bodies/native structures; existing native pair enrolled. No actor movement, food, health, item, block or clock changes. Source unchanged.":"Frozen existing bodies/native structures. No adoption, actor movement, food, health, item, block or clock changes. Primary unchanged.")));
                done=true;server.halt(false);
            }catch(Throwable error){error.printStackTrace();done=true;try{Files.writeString(output,new Gson().toJson(Map.of("status","failed","error",error.toString())));}catch(Exception ignored){}server.halt(false);}
        });
    }
}
