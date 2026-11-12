package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Focused observation of an existing body and real station paths; no supplies or body setters. */
public final class ResidentMealProbe implements ModInitializer {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private final UUID id=UUID.fromString(System.getProperty("villagers.food.focus"));
    private final boolean rehearse=Boolean.getBoolean("villagers.boats.rehearse");
    private final boolean cycle=Boolean.getBoolean("villagers.food.focusCycle");
    private final int window=Integer.getInteger("villagers.food.focusTicks",8000);
    private int ticks,elapsed;private long time;private boolean started,finished;
    private Map<String,Object> initial;
    private Path out(String name){return FabricLoader.getInstance().getGameDir().resolve(name);}
    private Map<String,Object> route(net.minecraft.world.level.pathfinder.Path p){
        return p==null?Map.of("found",false):Map.of("found",true,"reaches",p.canReach(),"nodes",p.getNodeCount(),"end",p.getEndNode().asBlockPos().toShortString());
    }
    private Map<String,Object> futurePlacementContact(ServerLevel level,Villager v,BlockPos target,Vec3 eye,Set<BlockPos> removed){
        var witness=new LinkedHashMap<String,Object>();
        search:for(var side:Direction.values()){
            var anchor=target.relative(side.getOpposite());
            if(removed.contains(anchor)||!level.isLoaded(anchor)||!level.getBlockState(anchor).isFaceSturdy(level,anchor,side))continue;
            for(double h:new double[]{.5,.9,.1}){
                var point=Vec3.atCenterOf(anchor).add(side.getStepX()*.499,side.getStepY()*.499,side.getStepZ()*.499);
                if(side.getAxis().isHorizontal())point=point.add(0,h-.5,0);
                var hit=level.clip(new ClipContext(eye,point,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,v){
                    @Override public net.minecraft.world.phys.shapes.VoxelShape getBlockShape(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.BlockGetter world,BlockPos at){
                        return removed.contains(at)?net.minecraft.world.phys.shapes.Shapes.empty():super.getBlockShape(state,world,at);
                    }
                });
                if(hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(anchor)&&hit.getDirection()==side&&eye.distanceToSqr(hit.getLocation())<=9){
                    witness.put("anchor",anchor.toShortString());witness.put("face",side.name());witness.put("reach",eye.distanceTo(hit.getLocation()));
                    witness.put("contact",hit.getLocation().toString());break search;
                }
            }
        }
        return witness;
    }
    /** Frozen construction geometry only. No excavation, placement, movement or actor knowledge writes. */
    private List<Map<String,Object>> wetFloorSurvey(ServerLevel level,Villager v){
        var result=new ArrayList<Map<String,Object>>();var origin=v.blockPosition();
        for(var direction:Direction.Plane.HORIZONTAL){
            var removed=new HashSet<>(MineStair.step(origin,direction,1,false));var target=MineStair.step(origin,direction,1,false).getLast().below();
            var stands=new ArrayList<Map<String,Object>>();
            for(double offset:new double[]{0,.3,.6}){
                var feet=Vec3.atBottomCenterOf(origin).add(direction.getStepX()*offset,0,direction.getStepZ()*offset);
                var eye=feet.add(0,v.getEyeHeight(),0);var body=v.getBoundingBox().move(feet.subtract(v.position()));
                var swept=v.getBoundingBox().minmax(body);boolean clear=true,supported=false;
                var low=BlockPos.containing(swept.minX-1e-6,swept.minY-.04,swept.minZ-1e-6);
                var high=BlockPos.containing(swept.maxX+1e-6,swept.maxY+1e-6,swept.maxZ+1e-6);
                for(var p:BlockPos.betweenClosed(low,high)){
                    if(removed.contains(p))continue;
                    for(var shape:level.getBlockState(p).getCollisionShape(level,p).toAabbs()){
                        var box=shape.move(p.getX(),p.getY(),p.getZ());
                        if(box.intersects(swept))clear=false;
                        if(box.intersects(body.move(0,-.03,0)))supported=true;
                    }
                }
                var witness=futurePlacementContact(level,v,target,eye,removed);
                var dropping=futurePlacementContact(level,v,target.above(),eye,removed);
                stands.add(Map.of("offset",offset,"feet",feet.toString(),"bodyClearAfterExcavation",clear,"retainedSupport",supported,
                    "border",level.noBorderCollision(v,body),"placementContactAfterExcavation",witness,"fallingBlockContactAfterExcavation",dropping));
            }
            var column=new ArrayList<Map<String,Object>>();
            for(int depth=0;depth<16;depth++){
                var at=target.below(depth);column.add(Map.of("at",at.toShortString(),"state",level.getBlockState(at).toString()));
                if(level.getBlockState(at).isFaceSturdy(level,at,Direction.UP))break;
            }
            var anchors=new ArrayList<Map<String,Object>>();
            for(var face:Direction.values()){var at=target.relative(face);anchors.add(Map.of("at",at.toShortString(),"state",level.getBlockState(at).toString()));}
            result.add(Map.of("direction",direction.name(),"target",target.toShortString(),"state",level.getBlockState(target).toString(),
                "hypotheticallyRemoved",removed.stream().map(BlockPos::toShortString).sorted().toList(),"stands",stands,"column",column,"anchors",anchors));
        }
        return result;
    }
    private boolean plainMasonry(net.minecraft.world.level.block.state.BlockState b){
        return b.is(net.minecraft.world.level.block.Blocks.COBBLESTONE)||b.is(net.minecraft.world.level.block.Blocks.MOSSY_COBBLESTONE)
            ||b.is(net.minecraft.world.level.block.Blocks.STONE_BRICKS)||b.is(net.minecraft.world.level.block.Blocks.MOSSY_STONE_BRICKS)
            ||b.is(net.minecraft.world.level.block.Blocks.COBBLESTONE_STAIRS)||b.is(net.minecraft.world.level.block.Blocks.MOSSY_COBBLESTONE_STAIRS);
    }
    private Map<String,Object> fromExit(ServerLevel level,net.minecraft.world.entity.monster.illager.Pillager donor,BlockPos exit,BlockPos voice){
        var evaluator=new net.minecraft.world.level.pathfinder.WalkNodeEvaluator(){
            @Override public net.minecraft.world.level.pathfinder.Node getStart(){
                var node=getNode(exit);node.type=getPathTypeOfMob(currentContext,exit.getX(),exit.getY(),exit.getZ(),mob);
                node.costMalus=mob.getPathfindingMalus(node.type);return node;
            }
        };
        evaluator.setCanOpenDoors(true);evaluator.setCanPassDoors(true);evaluator.setCanFloat(donor.getNavigation().canFloat());
        var home=WarCombat.home(donor);int spread=NativeIllagers.radius(donor)+2;var center=new BlockPos(home.x(),exit.getY(),home.z());
        var region=new net.minecraft.world.level.PathNavigationRegion(level,center.offset(-spread,-8,-spread),center.offset(spread,16,spread));
        var finder=new net.minecraft.world.level.pathfinder.PathFinder(evaluator,8192);
        var result=NativePathLimits.search(donor,2,()->finder.findPath(region,donor,Set.of(voice),128f,(int)NativeCampCalls.listeningRadius()-1,1f));
        return Map.of("route",route(result),"safe",NativeIllagers.safePath(level,donor,result),"scope","Hypothetical native start at the existing far-side stand; no body or block change.");
    }
    private List<Map<String,Object>> entranceCandidates(ServerLevel level,net.minecraft.world.entity.monster.illager.Pillager donor,BlockPos voice){
        var rows=new ArrayList<Map<String,Object>>();var origin=donor.blockPosition();int considered=0;
        for(var at:BlockPos.betweenClosed(origin.offset(-16,-2,-16),origin.offset(16,2,16))){
            if(!NativeCampWork.local(level,donor,at)||!NativeIllagers.insideCamp(level,donor,at)||!plainMasonry(level.getBlockState(at))
                ||BlockContact.point(level,donor,donor.getEyePosition(),at.above(),16)==null)continue;
            for(var direction:Direction.Plane.HORIZONTAL)for(int width=1;width<=3;width++){
                if((donor.getX()-at.getX()-.5)*direction.getStepX()+(donor.getZ()-at.getZ()-.5)*direction.getStepZ()>=0)continue;
                var exit=at.relative(direction,width);if(exit.distSqr(voice)>=at.distSqr(voice)-4)continue;
                boolean safe=true;var cells=new ArrayList<BlockPos>();
                for(int depth=0;depth<width;depth++){
                    var base=at.relative(direction,depth);
                    if(!level.getBlockState(base.below()).isFaceSturdy(level,base.below(),Direction.UP))safe=false;
                    for(int height=0;height<3;height++){
                        var p=base.above(height);cells.add(p.immutable());
                        if(!NativeCampWork.local(level,donor,p)||ConstructionSites.isProtected(level,p)||level.getBlockEntity(p)!=null
                            ||!(plainMasonry(level.getBlockState(p))||level.getBlockState(p).isAir()
                                ||level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.DIRT)
                                ||level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)))safe=false;
                        for(var face:Direction.values()){
                            var q=p.relative(face);var b=level.getBlockState(q).getBlock();
                            if(!level.isLoaded(q)||!level.getFluidState(q).isEmpty()||b instanceof net.minecraft.world.level.block.FallingBlock
                                ||b instanceof net.minecraft.world.level.block.BedBlock||b instanceof net.minecraft.world.level.block.DoorBlock
                                ||b instanceof net.minecraft.world.level.block.FenceBlock||level.getBlockState(q).is(net.minecraft.world.level.block.Blocks.IRON_BARS))safe=false;
                        }
                    }
                }
                double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,exit);
                var body=donor.getBoundingBox().move(exit.getX()+.5-donor.getX(),floor-donor.getY(),exit.getZ()+.5-donor.getZ());
                if(Math.abs(floor-at.getY())>1||!level.noCollision(donor,body)||level.noBlockCollision(donor,body.move(0,-.03,0))||!safe)continue;
                var outside=at.relative(direction.getOpposite());double outFloor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,outside);
                var shape=level.getBlockState(outside).getCollisionShape(level,outside);if(!shape.isEmpty())outFloor=Math.max(outFloor,outside.getY()+shape.max(Direction.Axis.Y));
                var stand=BlockPos.containing(outside.getX()+.5,Math.ceil(outFloor),outside.getZ()+.5);
                var path=CampBeds.pathToStands(level,donor,List.of(stand));considered++;
                rows.add(Map.of("outer",at.toShortString(),"inward",direction.toString(),"width",width,"exitFloor",floor,"outsideStand",stand.toShortString(),
                    "route",route(path),"safe",NativeIllagers.safePath(level,donor,path),"fromExit",fromExit(level,donor,BlockPos.containing(exit.getX()+.5,Math.ceil(floor),exit.getZ()+.5),voice),
                    "cells",cells.stream().map(p->Map.of("at",p.toShortString(),"state",level.getBlockState(p).toString())).toList()));
                if(considered>=24)return rows;
            }
        }
        return rows;
    }
    private Map<String,Object> nativeStart(ServerLevel level,net.minecraft.world.entity.Mob mob,BlockPos voice){
        var origin=mob.blockPosition();var actual=mob.getNavigation().getNodeEvaluator();
        var eval=new net.minecraft.world.level.pathfinder.WalkNodeEvaluator();
        eval.setCanOpenDoors(actual.canOpenDoors());eval.setCanPassDoors(actual.canPassDoors());
        eval.setCanFloat(actual.canFloat());eval.setCanWalkOverFences(actual.canWalkOverFences());
        eval.setCanOpenDoors(true); // Inspect the ordinary hand-openable door graph as well.
        var camp=WarCombat.home(mob);int spread=NativeIllagers.radius(mob)+2;
        var center=camp==null?origin:new BlockPos(camp.x(),origin.getY(),camp.z());
        var region=new net.minecraft.world.level.PathNavigationRegion(level,center.offset(-spread,-8,-spread),center.offset(spread,8,spread));
        eval.prepare(region,mob);
        try{
            var start=eval.getStart();var edges=new net.minecraft.world.level.pathfinder.Node[32];
            int count=NativePathLimits.search(mob,2,()->eval.getNeighbors(edges,start));
            var queue=new ArrayDeque<net.minecraft.world.level.pathfinder.Node>();queue.add(start);
            var seen=new HashSet<BlockPos>();var near=new ArrayList<BlockPos>();int inspected=0;
            while(!queue.isEmpty()&&inspected<12000){
                var current=queue.removeFirst();var at=current.asBlockPos();if(!seen.add(at))continue;inspected++;
                near.add(at);var next=new net.minecraft.world.level.pathfinder.Node[32];
                int n=NativePathLimits.search(mob,2,()->eval.getNeighbors(next,current));
                for(int i=0;i<n;i++){
                    var to=next[i].asBlockPos();if(seen.contains(to)||!level.isLoaded(to)||!NativeCampWork.local(level,mob,to)
                        ||!level.getFluidState(to).isEmpty()||current.y-to.getY()>2||to.distSqr(origin)>96*96)continue;
                    double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,to);
                    var shape=level.getBlockState(to).getCollisionShape(level,to);
                    if(!shape.isEmpty())floor=Math.max(floor,to.getY()+shape.max(Direction.Axis.Y));
                    var body=mob.getBoundingBox().move(to.getX()+.5-mob.getX(),floor-mob.getY(),to.getZ()+.5-mob.getZ());
                    if(!level.getBlockState(to).is(net.minecraft.tags.BlockTags.WOODEN_DOORS)&&!level.noBlockCollision(mob,body))continue;
                    queue.addLast(next[i]);
                }
            }
            near.sort(Comparator.comparingDouble(p->p.distSqr(voice)));
            var nativeQueue=new ArrayDeque<net.minecraft.world.level.pathfinder.Node>();nativeQueue.add(start);
            var nativeSeen=new HashSet<BlockPos>();var parent=new HashMap<BlockPos,BlockPos>();BlockPos closest=start.asBlockPos();int nativeVisited=0;
            while(!nativeQueue.isEmpty()&&nativeVisited<24000){
                var current=nativeQueue.removeFirst();var at=current.asBlockPos();if(!nativeSeen.add(at))continue;nativeVisited++;
                if(at.distSqr(voice)<closest.distSqr(voice))closest=at;
                if(at.distSqr(voice)<NativeCampCalls.listeningRadius()*NativeCampCalls.listeningRadius())break;
                var next=new net.minecraft.world.level.pathfinder.Node[32];int n=NativePathLimits.search(mob,2,()->eval.getNeighbors(next,current));
                for(int i=0;i<n;i++){
                    var to=next[i].asBlockPos();if(nativeSeen.contains(to)||parent.containsKey(to)||!level.isLoaded(to)
                        ||!NativeCampWork.local(level,mob,to)||current.y-to.getY()>2)continue;
                    parent.put(to,at);nativeQueue.addLast(next[i]);
                }
            }
            var trace=new ArrayList<Map<String,Object>>();var point=closest;int limit=0;
            while(point!=null&&limit++<1024){
                double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,point);var b=level.getBlockState(point);
                var shape=b.getCollisionShape(level,point);if(!shape.isEmpty())floor=Math.max(floor,point.getY()+shape.max(Direction.Axis.Y));
                var body=mob.getBoundingBox().move(point.getX()+.5-mob.getX(),floor-mob.getY(),point.getZ()+.5-mob.getZ());
                trace.add(Map.of("at",point.toShortString(),"state",b.toString(),"fluid",level.getFluidState(point).toString(),"bodyFits",level.noBlockCollision(mob,body)));
                point=parent.get(point);
            }
            var controls=new LinkedHashSet<BlockPos>();
            for(var p:near)for(var q:BlockPos.betweenClosed(p.offset(-1,0,-1),p.offset(1,2,1))){
                var b=level.getBlockState(q).getBlock();if(b instanceof net.minecraft.world.level.block.DoorBlock
                    ||b instanceof net.minecraft.world.level.block.FenceGateBlock||b instanceof net.minecraft.world.level.block.LeverBlock
                    ||b instanceof net.minecraft.world.level.block.ButtonBlock)controls.add(q.immutable());
            }
            var controlRows=controls.stream().sorted(Comparator.comparingDouble(p->p.distSqr(voice))).limit(40)
                .map(p->Map.of("at",p.toShortString(),"state",level.getBlockState(p).toString(),"distanceToVoice",Math.sqrt(p.distSqr(voice)))).toList();
            var allControls=new ArrayList<Map<String,Object>>();
            for(var p:BlockPos.betweenClosed(center.offset(-spread,-12,-spread),center.offset(spread,24,spread))){
                var b=region.getBlockState(p).getBlock();if(!(b instanceof net.minecraft.world.level.block.DoorBlock)
                    &&!(b instanceof net.minecraft.world.level.block.LeverBlock)&&!(b instanceof net.minecraft.world.level.block.FenceGateBlock)
                    &&!(b instanceof net.minecraft.world.level.block.ButtonBlock))continue;
                boolean nearReachable=false;for(var q:BlockPos.betweenClosed(p.offset(-2,-2,-2),p.offset(2,1,2)))if(seen.contains(q)){nearReachable=true;break;}
                allControls.add(Map.of("at",p.toShortString(),"state",region.getBlockState(p).toString(),"nearReachable",nearReachable));
            }
            var rows=new ArrayList<String>();int minX=center.getX()-72,minZ=center.getZ()-84;
            for(int z=minZ;z<=center.getZ()+84;z++){
                var line=new StringBuilder();for(int x=minX;x<=center.getX()+72;x++){
                    var p=new BlockPos(x,64,z);var state=region.getBlockState(p);var b=state.getBlock();
                    char glyph=state.isAir()?' ':state.getCollisionShape(region,p).isEmpty()?'t':'#';
                    if(state.is(net.minecraft.world.level.block.Blocks.IRON_DOOR))glyph='I';
                    else if(b instanceof net.minecraft.world.level.block.DoorBlock)glyph='D';
                    else if(b instanceof net.minecraft.world.level.block.FenceGateBlock)glyph='G';
                    else if(b instanceof net.minecraft.world.level.block.LeverBlock)glyph='L';
                    else if(b instanceof net.minecraft.world.level.block.FenceBlock)glyph='F';
                    else if(b instanceof net.minecraft.world.level.block.BedBlock)glyph='B';
                    else if(!region.getFluidState(p).isEmpty())glyph='W';
                    else if(seen.contains(p))glyph='o';
                    if(p.equals(voice))glyph='V';if(p.equals(origin))glyph='P';line.append(glyph);
                }rows.add(z+":"+line);
            }
            var nearest=near.stream().limit(16).map(p->{var sides=new ArrayList<Map<String,Object>>();
                for(var direction:Direction.Plane.HORIZONTAL){var q=p.relative(direction);sides.add(Map.of("at",q.toShortString(),"block",level.getBlockState(q).toString(),
                    "head",level.getBlockState(q.above()).toString(),"below",level.getBlockState(q.below()).toString()));}
                return Map.of("at",p.toShortString(),"distanceToVoice",Math.sqrt(p.distSqr(voice)),"sides",sides);}).toList();
            var context=new net.minecraft.world.level.pathfinder.PathfindingContext(region,mob);var cells=new ArrayList<Map<String,Object>>();
            for(var p:BlockPos.betweenClosed(origin.offset(-2,-2,-2),origin.offset(2,2,2))){
                var kind=eval.getPathTypeOfMob(context,p.getX(),p.getY(),p.getZ(),mob);
                double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,p);
                var feet=new Vec3(p.getX()+.5,floor,p.getZ()+.5);var body=mob.getBoundingBox().move(feet.subtract(mob.position()));
                cells.add(Map.of("at",p.toShortString(),"block",level.getBlockState(p).toString(),"pathType",kind.toString(),
                    "malus",mob.getPathfindingMalus(kind),"floor",floor,"bodyFits",level.noBlockCollision(mob,body)));
            }
            return Map.of("start",Map.of("at",start.asBlockPos().toShortString(),"type",start.type.toString(),"malus",start.costMalus),
                "canOpenDoors",actual.canOpenDoors(),"canPassDoors",actual.canPassDoors(),"canFloat",actual.canFloat(),
                "edges",java.util.stream.IntStream.range(0,count).mapToObj(i->Map.of("at",edges[i].asBlockPos().toShortString(),"type",edges[i].type.toString(),"malus",edges[i].costMalus)).toList(),"cells",cells,
                "dryConnectivity",Map.of("inspected",inspected,"remainingQueue",queue.size(),"nearest",nearest,"reachableControls",controlRows,
                    "allControls",allControls,"operatorFloorMap",Map.of("y",64,"minX",minX,"minZ",minZ,"rows",rows),
                    "nativeConnectivity",Map.of("inspected",nativeVisited,"remainingQueue",nativeQueue.size(),"closest",closest.toShortString(),
                        "distanceToVoice",Math.sqrt(closest.distSqr(voice)),"trace",trace)));
        }finally{eval.done();}
    }
    private Map<String,Object> voiceRoutes(ServerLevel level,net.minecraft.world.entity.monster.illager.Pillager donor,BlockPos voice){
        var stands=new ArrayList<BlockPos>();double radius=NativeCampCalls.listeningRadius();int r=(int)Math.ceil(radius);
        for(var p:BlockPos.betweenClosed(voice.offset(-r,-3,-r),voice.offset(r,3,r))){
            if(!level.isLoaded(p.above())||!NativeCampWork.local(level,donor,p)||!NativeIllagers.insideCamp(level,donor,p)
                ||!level.getFluidState(p).isEmpty()||!level.getFluidState(p.below()).isEmpty())continue;
            double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,p);
            var feet=new Vec3(p.getX()+.5,floor,p.getZ()+.5);var body=donor.getBoundingBox().move(feet.subtract(donor.position()));
            if(feet.distanceToSqr(Vec3.atCenterOf(voice))<=radius*radius&&level.noCollision(donor,body)&&!level.noBlockCollision(donor,body.move(0,-.03,0)))stands.add(p.immutable());
        }
        stands.sort(Comparator.comparingDouble(p->p.distSqr(donor.blockPosition())));
        var paths=new ArrayList<Map<String,Object>>();
        try{
            donor.getNavigation().setMaxVisitedNodesMultiplier(32);
            for(var at:stands.stream().limit(8).toList()){
                var path=NativePathLimits.search(donor,2,()->donor.getNavigation().createPath(at,0,256));
                var three=NativePathLimits.search(donor,3,()->donor.getNavigation().createPath(at,0,256));
                boolean wasOpen=donor.getNavigation().getNodeEvaluator().canOpenDoors();
                net.minecraft.world.level.pathfinder.Path doorRoute;
                try{
                    donor.getNavigation().setCanOpenDoors(true);
                    doorRoute=NativePathLimits.search(donor,2,()->donor.getNavigation().createPath(at,0,256));
                }finally{donor.getNavigation().setCanOpenDoors(wasOpen);}
                double y=donor.getY(),drop=0;boolean dry=true;
                if(three!=null)for(int i=0;i<three.getNodeCount();i++){
                    var node=three.getNodePos(i);double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,node);
                    drop=Math.max(drop,y-floor);y=floor;dry&=level.getFluidState(node).isEmpty();
                }
                paths.add(Map.of("stand",at.toShortString(),"nativeWithoutBodyAvoidance",route(path),"safe",NativeIllagers.safePath(level,donor,path),
                    "threeBlockNativeSearch",route(three),"threeBlockSearchMaxFloorDrop",drop,"threeBlockSearchDry",dry,
                    "woodDoorPlanningRoute",route(doorRoute),"woodDoorPlanningSafe",NativeIllagers.safePath(level,donor,doorRoute)));
            }
        }finally{donor.getNavigation().resetMaxVisitedNodesMultiplier();}
        return Map.of("position",donor.position().toString(),"entranceCandidates",entranceCandidates(level,donor,voice),"nativeStart",nativeStart(level,donor,voice),"standCount",stands.size(),"paths",paths,"neighbors",level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
            donor.getBoundingBox().inflate(8),b->b!=donor&&b.isAlive()).stream().map(b->Map.of("id",b.getUUID().toString(),
                "type",b.getType().toString(),"position",b.position().toString(),"noAI",b.isNoAi(),"managed",CreatureAgents.managed(b))).toList());
    }
    private Map<String,Object> row(ServerLevel level,Villager v,boolean inspect){
        var s=Villagers.session(v,level);var r=new LinkedHashMap<String,Object>();
        r.put("id",id.toString());r.put("name",s.mind.name());r.put("position",List.of(v.getX(),v.getY(),v.getZ()));
        r.put("health",v.getHealth());r.put("food",s.mind.foodLevel());r.put("inventory",v.getInventory().toString());
        r.put("activity",v.getName().getString());r.put("home",String.valueOf(s.mind.home()));
        if(!rehearse&&s.mind.home()!=null){
            var home=s.mind.home();var nativeVillage=level.structureManager().getStructureWithPieceAt(new BlockPos(home.x(),home.y(),home.z()),net.minecraft.tags.StructureTags.VILLAGE);
            r.put("nativeVillageBounds",nativeVillage.isValid()?nativeVillage.getBoundingBox().toString():"none");
        }
        r.put("running",VillageSurvival.running(id));r.put("restScheduled",VillageSurvival.scheduledRest(level,v));
        r.put("survival",VillageSurvival.status(id));
        r.put("threats",level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,v.getBoundingBox().inflate(12),
            m->VillagerController.threatens(m,v)&&v.hasLineOfSight(m)).stream().map(m->Map.of("id",m.getUUID().toString(),"position",m.position().toString(),"type",m.getType().toString(),"distance",v.distanceTo(m))).toList());
        r.put("crafting",s.craftingStatus);r.put("breadCrafted",s.breadCrafted);r.put("foodAccess",FoodAccess.status(s));
        r.put("preparation",WoodBootstrap.status(s));r.put("fatigue",s.mind.fatigue());
        r.put("foodStorage",NativeFoodStorage.status(s));
        var navigation=v.getNavigation();var active=navigation.getPath();
        r.put("navigation",Map.of("done",navigation.isDone(),"target",String.valueOf(navigation.getTargetPos()),
            "path",route(active),"nextIndex",active==null?-1:active.getNextNodeIndex(),
            "next",active==null||active.isDone()?"":active.getNextNodePos().toShortString()));
        r.put("foodHelp",s.foodHelpStatus);r.put("foodRequests",s.foodHelpRequests);
        if(inspect&&AdventurePreparation.nominated(v)){
            MineStair.resume(v,s);r.put("mineState",MineStair.status(s));
            var faces=new ArrayList<Map<String,Object>>();var origin=v.blockPosition();
            for(var direction:Direction.Plane.HORIZONTAL)for(boolean flat:List.of(false,true)){
                var cells=MineStair.step(origin,direction,1,flat);var details=new ArrayList<Map<String,Object>>();
                for(var at:cells){var cover=NativeTerrainWork.structuralCover(level,at);var row=new LinkedHashMap<String,Object>();
                    row.put("at",at.toShortString());row.put("state",level.getBlockState(at).toString());row.put("exitFloor",MineStair.exitFloor(v,at));
                    row.put("cover",cover==null?"":cover.toShortString());row.put("coverState",cover==null?"":level.getBlockState(cover).toString());
                    row.put("exposed",AdventureMining.exposedAt(level,v,at,true,origin.getY()));
                    row.put("handContact",BlockContact.point(level,v,v.getEyePosition(),at,3)!=null);details.add(row);
                }
                faces.add(Map.of("direction",direction.name(),"flat",flat,"freshFloor",MineStair.newWorkFloor(s.mineStair,cells),
                    "floor",level.getBlockState(cells.getLast().below()).toString(),"cells",details));
            }
            r.put("miningFaceSurvey",faces);
            r.put("wetFloorPlacementSurvey",wetFloorSurvey(level,v));
            var retired=v.entityTags().stream().map(tag->AdventureQuarry.failure(tag,level.getGameTime()))
                .filter(Objects::nonNull).sorted(Comparator.comparingLong(AdventureQuarry.FailedSite::observedAt).reversed())
                .limit(3).map(site->{
                    var footprint=new ArrayList<Map<String,Object>>();
                    for(var direction:Direction.Plane.HORIZONTAL)for(var at:AdventureQuarry.cells(site.root(),direction))
                        if(level.isLoaded(at))footprint.add(Map.of("direction",direction.name(),"at",at.toShortString(),"block",level.getBlockState(at).toString()));
                    var features=new ArrayList<Map<String,Object>>();
                    for(var at:BlockPos.betweenClosed(site.root().offset(-6,-7,-6),site.root().offset(6,2,6))){
                        if(!level.isLoaded(at))continue;var block=level.getBlockState(at);
                        if(block.getBlock() instanceof net.minecraft.world.level.block.CropBlock
                            ||block.is(net.minecraft.world.level.block.Blocks.FARMLAND)
                            ||block.getBlock() instanceof net.minecraft.world.level.block.BedBlock
                            ||block.is(net.minecraft.tags.BlockTags.PLANKS)||level.getBlockEntity(at)!=null)
                            features.add(Map.of("at",at.toShortString(),"block",block.toString()));
                    }
                    return Map.of("root",site.root().toShortString(),"observedAt",site.observedAt(),
                        "footprint",footprint,"nearbyProtectedFeatures",features);
                }).toList();
            r.put("retiredQuarrySurvey",Map.of("sites",retired,"scope","Read-only frozen native blocks around this actor's own retired quarry tags; no route or knowledge supplied."));
            try{
                var fill=Class.forName("local.villagers.MineFill");
                var survey=fill.getDeclaredMethod("survey",ServerLevel.class,Villager.class,Session.class);survey.setAccessible(true);
                r.put("mineFillSurvey",survey.invoke(null,level,v,s));
            }catch(ReflectiveOperationException unavailable){r.put("mineFillSurvey",Map.of("available",false));}
            try{
                var foundation=Class.forName("local.villagers.MineGravityFoundation");
                var survey=foundation.getDeclaredMethod("survey",ServerLevel.class,Villager.class,Session.class);survey.setAccessible(true);
                r.put("gravityFoundationSurvey",survey.invoke(null,level,v,s));
            }catch(ReflectiveOperationException unavailable){r.put("gravityFoundationSurvey",Map.of("available",false));}
            var activeQuarries=new ArrayList<Map<String,Object>>();
            for(var tag:v.entityTags())if(tag.startsWith("villagers:quarry:"))try{
                var parts=tag.substring("villagers:quarry:".length()).split(":");
                if(parts.length!=3)continue;
                var root=BlockPos.of(Long.parseLong(parts[0]));var direction=Direction.valueOf(parts[1]);
                if(direction.getAxis().isVertical())continue;
                var cells=new ArrayList<Map<String,Object>>();
                for(var at:AdventureQuarry.cells(root,direction))if(level.isLoaded(at))
                    cells.add(Map.of("at",at.toShortString(),"block",level.getBlockState(at).toString()));
                var supports=new ArrayList<Map<String,Object>>();
                for(var at:AdventureQuarry.accessSupports(root,direction))if(level.isLoaded(at))
                    supports.add(Map.of("at",at.toShortString(),"block",level.getBlockState(at).toString(),
                        "taggedAccessSupport",AdventureQuarry.accessSupport(v,at),"retainedMineFloor",MineStair.exitFloor(v,at)));
                activeQuarries.add(Map.of("root",root.toShortString(),"direction",direction.name(),"step",Integer.parseInt(parts[2]),
                    "cells",cells,"supports",supports));
            }catch(IllegalArgumentException invalid){/* Malformed tags do not authorize an inspection. */}
            r.put("activeQuarrySurvey",Map.of("sites",activeQuarries,"scope","Read-only frozen native blocks from the actor's own persisted quarry work tags."));
        }
        if(inspect){
            var threat=level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,v.getBoundingBox().inflate(12),
                m->VillagerController.threatens(m,v)&&v.hasLineOfSight(m)).stream().min(Comparator.comparingDouble(v::distanceToSqr)).orElse(null);
            if(threat!=null){
                var exits=new ArrayList<BlockPos>();var origin=v.blockPosition();
                for(var p:BlockPos.betweenClosed(origin.offset(-12,-3,-12),origin.offset(12,5,12))){
                    if(!level.isLoaded(p.above())||!level.getFluidState(p).isEmpty()||!level.getFluidState(p.below()).isEmpty()
                        ||p.distSqr(origin)<9||Vec3.atBottomCenterOf(p).distanceToSqr(threat.position())<36)continue;
                    var feet=Vec3.atBottomCenterOf(p);var body=v.getBoundingBox().move(feet.subtract(v.position()));
                    if(level.noBlockCollision(v,body)&&!level.noBlockCollision(v,body.move(0,-.03,0)))exits.add(p.immutable());
                }
                exits.sort(Comparator.comparingDouble(p->p.distSqr(origin)));
                var checks=new ArrayList<Map<String,Object>>();
                for(var target:exits.stream().limit(32).toList()){
                    var path=NativePathLimits.search(v,2,()->v.getNavigation().createPath(target,0,64));
                    var nodes=new ArrayList<String>();boolean crosses=false;
                    if(path!=null)for(int n=0;n<path.getNodeCount();n++){
                        var at=path.getNodePos(n);nodes.add(at.toShortString());
                        if(n>=3&&Vec3.atCenterOf(at).distanceToSqr(threat.position())<6.25)crosses=true;
                    }
                    checks.add(Map.of("target",target.toShortString(),"path",route(path),"dry",BoatTravel.dryPath(level,path),"crossesThreat",crosses,"nodes",nodes));
                }
                r.put("escapeSurvey",Map.of("candidates",exits.size(),"checks",checks,"scope","Frozen operator-only actual native routes; no movement or actor knowledge writes."));
            }
            var words=local.villagers.language.Utterance.of(s.mind.lexicon(),List.of(local.villagers.language.Concept.WANT,
                local.villagers.language.Concept.FOOD,local.villagers.language.Concept.HUNGRY),true);
            var listeners=new ArrayList<Map<String,Object>>();
            for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.monster.illager.Pillager donor&&CreatureAgents.managed(donor)){
                CreatureAgents.status(donor); // Load its saved vocabulary before interpreting a diagnostic result.
                var ds=CreatureAgents.loadedState(donor);
                net.minecraft.world.level.pathfinder.Path listen=null;int queries=0;
                while(queries<12&&listen==null){queries++;listen=NativeCampCalls.listeningPath(level,donor,v.blockPosition());}
                listeners.add(Map.of("id",donor.getUUID().toString(),"distance",v.distanceTo(donor),
                    "sameCamp",NativeCampMeals.sameCamp(level,donor,v,s),"homeCoversDonor",NativeFoodStorage.homeStructure(level,v,s,donor.blockPosition()),
                    "insideDonorCamp",NativeIllagers.insideCamp(level,donor,v.blockPosition()),"listeningRoute",route(listen),"listeningQueries",queries,
                    "voiceRouteDiagnosis",voiceRoutes(level,donor,v.blockPosition()),
                    "decodesRequest",ds!=null&&local.villagers.relations.FoodRequest.understands(words,ds.mind.lexicon(),
                        local.villagers.language.Concept.WANT,local.villagers.language.Concept.FOOD,local.villagers.language.Concept.HUNGRY)));
            }
            r.put("campCommunication",Map.of("homeCoversSelf",NativeFoodStorage.homeStructure(level,v,s,v.blockPosition()),
                "needsCall",NativeCampCalls.needsCall(s.mind.foodLevel(),v.getHealth(),v.getMaxHealth()),"canFormUrgentWords",words!=null,
                "requestsThisLoad",s.campMealRequests,"listeners",listeners));
            var farms=new ArrayList<Map<String,Object>>();
            for(var episode:s.mind.observedFarms()){
                var known=episode.place();if(!known.dimension().equals(level.dimension().identifier().toString()))continue;
                var p=new BlockPos(known.x(),known.y(),known.z());if(!level.isLoaded(p))continue;
                var ordinary=v.getNavigation().createPath(p,1);
                var bounded=NativePathLimits.search(v,2,()->WorkNavigation.pathTo(level,v,s,p,1));
                farms.add(Map.of("position",p.toShortString(),"block",level.getBlockState(p).toString(),
                    "ordinaryRoute",route(ordinary),"workRoute",route(bounded)));
            }
            r.put("rememberedFarmRoutes",farms);
            var foodPassage=FoodAccess.preview(level,v);var passageRows=new ArrayList<Map<String,Object>>();
            if(foodPassage!=null)for(var step:foodPassage.route().steps().stream().limit(12).toList()){
                var p=step.feet();double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,p);
                passageRows.add(Map.of("feet",p.toShortString(),"floor",floor,"state",level.getBlockState(p).toString(),
                    "below",level.getBlockState(p.below()).toString(),"dig",step.dig().stream().map(BlockPos::toShortString).toList(),
                    "nativeRoute",route(v.getNavigation().createPath(p,0))));
            }
            r.put("foodPassagePreview",Map.of("found",foodPassage!=null,"firstSteps",passageRows,
                "scope","Read-only operator preview, no route issued or coordinates taught."));
            // Operator-only physical survey. Do not inspect storage contents,
            // teach coordinates, or move any resident to test a route.
            var foodSites=new ArrayList<BlockPos>();var origin=v.blockPosition();
            var counts=new TreeMap<String,Integer>();
            for(var p:BlockPos.betweenClosed(origin.offset(-48,-12,-48),origin.offset(48,12,48))){
                if(!level.isLoaded(p))continue;var b=level.getBlockState(p);var block=b.getBlock();
                if(!(block instanceof net.minecraft.world.level.block.CropBlock||b.is(net.minecraft.world.level.block.Blocks.HAY_BLOCK)
                    ||b.is(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE)||b.is(net.minecraft.world.level.block.Blocks.BELL)
                    ||b.is(net.minecraft.tags.BlockTags.LOGS)&&BoatPreparation.naturalTrunk(level,p)||NativeContainers.accessible(level,p)!=null))continue;
                counts.merge(block.toString(),1,Integer::sum);foodSites.add(p.immutable());
            }
            foodSites.sort(Comparator.comparingDouble(p->p.distSqr(origin)));var sites=new ArrayList<Map<String,Object>>();
            var shown=new HashMap<net.minecraft.world.level.block.Block,Integer>();
            for(var p:foodSites){
                if(shown.merge(level.getBlockState(p).getBlock(),1,Integer::sum)>8)continue;
                var b=level.getBlockState(p);var hit=level.clip(new ClipContext(v.getEyePosition(),Vec3.atCenterOf(p),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,v));
                sites.add(Map.of("at",p.toShortString(),"state",b.toString(),"distance",Math.sqrt(p.distSqr(origin)),
                    "visible",hit.getType()==HitResult.Type.MISS||hit.getBlockPos().equals(p),
                    "homeRange",VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(p)),
                    "route",route(v.getNavigation().createPath(p,1))));
            }
            r.put("localFoodSurvey",Map.of("counts",counts,"nearest",sites,"scope","Frozen operator-only block and native route survey; no contents read, resource grants, body movement, or coordinates taught."));
            var storeRoutes=new ArrayList<Map<String,Object>>();
            var physicalStores=new ArrayList<BlockPos>();
            for(var p:BlockPos.betweenClosed(origin.offset(-64,-6,-64),origin.offset(64,6,64)))
                if(level.isLoaded(p)&&NativeContainers.accessible(level,p)!=null&&NativeFoodStorage.homeStructure(level,v,s,p))physicalStores.add(p.immutable());
            physicalStores.sort(Comparator.comparingDouble(p->p.distSqr(origin)));
            for(var p:physicalStores.stream().limit(4).toList()){
                var passage=FoodAccess.plan(level,v,p);var approaches=new ArrayList<Map<String,Object>>();
                var stands=new ArrayList<BlockPos>();
                for(var q:BlockPos.betweenClosed(p.offset(-3,-2,-3),p.offset(3,2,3))){
                    double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,q);
                    var feet=new Vec3(q.getX()+.5,floor,q.getZ()+.5);var body=v.getBoundingBox().move(feet.subtract(v.position()));
                    if(level.noBlockCollision(v,body)&&!level.noBlockCollision(v,body.move(0,-.03,0))&&level.getFluidState(q).isEmpty()
                        &&BlockContact.interactionPoint(level,v,feet.add(0,v.getEyeHeight(),0),p,3)!=null)stands.add(q.immutable());
                }
                stands.sort(Comparator.comparingDouble(q->q.distSqr(v.blockPosition())));
                for(var q:stands.stream().limit(16).toList()){
                    net.minecraft.world.level.pathfinder.Path path;
                    try{v.getNavigation().setMaxVisitedNodesMultiplier(32);path=NativePathLimits.search(v,2,()->v.getNavigation().createPath(q,0,128));}
                    finally{v.getNavigation().resetMaxVisitedNodesMultiplier();}
                    var outside=new ArrayList<String>();var wet=new ArrayList<String>();
                    if(path!=null)for(int i=0;i<path.getNodeCount();i++){
                        var n=path.getNodePos(i);if(!NativeFoodStorage.homeStructure(level,v,s,n)&&outside.size()<4)outside.add(n.toShortString());
                        if(!level.getFluidState(n).isEmpty()&&wet.size()<4)wet.add(n.toShortString());
                    }
                    var routeGuards=new java.util.LinkedHashMap<String,Object>();
                    try{
                        var boundsMethod=NativeFoodStorage.class.getDeclaredMethod("homeBounds",ServerLevel.class,Villager.class,Session.class);
                        boundsMethod.setAccessible(true);var bounds=boundsMethod.invoke(null,level,v,s);
                        var guardMethod=NativeFoodStorage.class.getDeclaredMethod("safeRoute",ServerLevel.class,Villager.class,
                            net.minecraft.world.level.levelgen.structure.BoundingBox.class,net.minecraft.world.level.pathfinder.Path.class,boolean.class);
                        guardMethod.setAccessible(true);routeGuards.put("territoryPresent",bounds!=null);
                        if(bounds!=null){
                            routeGuards.put("ordinary",guardMethod.invoke(null,level,v,bounds,path,false));
                            routeGuards.put("critical",guardMethod.invoke(null,level,v,bounds,path,true));
                        }
                        var nodeFooting=new java.util.ArrayList<Map<String,Object>>();
                        if(path!=null)for(int ni=0;ni<Math.min(12,path.getNodeCount());ni++){
                            var node=path.getNodePos(ni);var state=level.getBlockState(node);
                            double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,node);
                            var shape=state.getCollisionShape(level,node);
                            if(!shape.isEmpty())floor=Math.max(floor,node.getY()+shape.max(net.minecraft.core.Direction.Axis.Y));
                            var feet=new net.minecraft.world.phys.Vec3(node.getX()+.5,floor,node.getZ()+.5);
                            var body=v.getBoundingBox().move(feet.subtract(v.position()));
                            nodeFooting.add(Map.of("at",node.toShortString(),"floor",floor,"state",state.toString(),
                                "below",level.getBlockState(node.below()).toString(),"bodyClear",level.noBlockCollision(v,body),
                                "supported",!level.noBlockCollision(v,body.move(0,-.03,0)),
                                "woodenDoorExempt",state.is(net.minecraft.tags.BlockTags.WOODEN_DOORS)));
                        }
                        routeGuards.put("firstNodeFootings",nodeFooting);
                    }catch(ReflectiveOperationException unavailable){routeGuards.put("unavailable",unavailable.toString());}
                    approaches.add(Map.of("stand",q.toShortString(),"state",level.getBlockState(q).toString(),
                        "floor",net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,q),"route",route(path),
                        "outsideHome",outside,"wetNodes",wet,"nativeStoreRouteGuards",routeGuards));
                }
                storeRoutes.add(Map.of("store",p.toShortString(),"validStands",stands.size(),"nativeApproaches",approaches,
                    "passage",passage==null?Map.of("found",false):Map.of("found",true,"digs",passage.digCount(),"steps",passage.steps().size())));
            }
            r.put("storeRouteSurvey",storeRoutes);
            var nativeWorkers=new ArrayList<Map<String,Object>>();
            for(var candidate:level.getEntitiesOfClass(net.minecraft.world.entity.monster.illager.Pillager.class,
                v.getBoundingBox().inflate(16),p->p.isAlive()&&!CreatureAgents.managed(p)&&!p.entityTags().contains(CreatureAgents.RELEASED)
                    &&NativeFoodStorage.homeStructure(level,v,s,p.blockPosition())).stream().sorted(Comparator.comparingDouble(v::distanceToSqr)).limit(6).toList()){
                double score=NativeIllagers.resources(level,candidate);
                nativeWorkers.add(Map.of("id",candidate.getUUID().toString(),"position",candidate.position().toString(),
                    "health",candidate.getHealth(),"grounded",candidate.onGround(),"resourceReady",Double.isFinite(score)&&score<Double.MAX_VALUE,
                    "score",score==Double.MAX_VALUE?"unavailable":Double.toString(score)));
            }
            r.put("existingWorkerSurvey",Map.of("candidates",nativeWorkers,"scope","Frozen existing bodies and physical resources only; no adoption, supplied items or body changes."));
        }
        var table=s.craftingTable==null?BreadCrafting.rememberedTable(level,v,s):s.craftingTable;
        // Operator-only discovery must not put a station into the actor's cache
        // before its controller decides to seek one, especially in a healthy run.
        if(inspect&&table==null)table=BreadCrafting.rememberedMealTable(level,v);
        if(inspect&&table==null){
            double nearest=Double.MAX_VALUE;var origin=v.blockPosition();
            for(var p:BlockPos.betweenClosed(origin.offset(-32,-3,-32),origin.offset(32,3,32)))
                if(level.isLoaded(p)&&level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE)&&p.distSqr(origin)<nearest){nearest=p.distSqr(origin);table=p.immutable();}
        }
        r.put("table",String.valueOf(table));r.put("blockedTable",String.valueOf(s.blockedCraftTable));
        r.put("canWorkAt",table!=null&&BreadCrafting.canWorkAt(level,v,s,table));
        if(inspect&&table!=null){
            r.put("overhead",java.util.stream.IntStream.rangeClosed(1,4).mapToObj(d->{var p=v.blockPosition().above(d);return Map.of("position",p.toShortString(),"block",level.getBlockState(p).toString(),"diggable",FoodAccess.excavatable(level,v,p));}).toList());
            var stands=new ArrayList<Map<String,Object>>();
            for(var p:BlockPos.betweenClosed(table.offset(-2,-1,-2),table.offset(2,1,2))){
                if(!level.isLoaded(p.above())||!level.getBlockState(p).isAir()||!level.getBlockState(p.above()).isAir())continue;
                var feet=Vec3.atBottomCenterOf(p);var eye=feet.add(0,v.getEyeHeight(),0);
                var hit=level.clip(new ClipContext(eye,Vec3.atCenterOf(table),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,v));
                var stand=new LinkedHashMap<String,Object>();stand.put("position",p.toShortString());
                stand.put("support",level.getBlockState(p.below()).toString());stand.put("solidFloor",level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP));
                stand.put("withinHome",VillageSurvival.withinHomeRange(level,s.mind,feet));stand.put("reach",eye.distanceTo(Vec3.atCenterOf(table)));
                stand.put("sight",hit.getType().toString());stand.put("sightBlock",hit.getBlockPos().toShortString());
                var stationRoute=v.getNavigation().createPath(p,0,64);
                stand.put("normalRoute",route(stationRoute));
                stand.put("normalDryPath",BoatTravel.dryPath(level,stationRoute));
                stand.put("normalHomePath",WorkNavigation.staysHome(level,v,s,stationRoute));
                if(stationRoute!=null)for(int node=0;node<stationRoute.getNodeCount();node++){
                    var at=stationRoute.getNodePos(node);
                    if(!level.getFluidState(at).isEmpty()||!level.getFluidState(at.below()).isEmpty()){
                        stand.put("normalFirstFluid",Map.of("node",at.toShortString(),"block",level.getBlockState(at).toString(),
                            "below",level.getBlockState(at.below()).toString(),"floor",net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,at)));
                        break;
                    }
                }
                try{
                    v.getNavigation().setMaxVisitedNodesMultiplier(32);
                    var expanded=v.getNavigation().createPath(p.immutable(),0,128);
                    stand.put("route",route(expanded));
                    stand.put("expandedDryPath",BoatTravel.dryPath(level,expanded));
                    stand.put("expandedHomePath",WorkNavigation.staysHome(level,v,s,expanded));
                    if(level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP)
                        &&eye.distanceToSqr(Vec3.atCenterOf(table))<=9
                        &&(hit.getType()==HitResult.Type.MISS||hit.getBlockPos().equals(table))){
                        var dry=NativePathLimits.search(v,1,()->NativePathLimits.alsoAvoiding(v,
                            at->!level.isLoaded(at)||!level.getFluidState(at).isEmpty()||!level.getFluidState(at.below()).isEmpty(),
                            ()->v.getNavigation().createPath(p.immutable(),0,128)));
                        stand.put("dryConstrainedRoute",route(dry));
                        stand.put("dryConstrainedHome",WorkNavigation.staysHome(level,v,s,dry));
                        stand.put("dryConstrainedValid",BoatTravel.dryPath(level,dry));
                    }
                    if(expanded!=null)for(int node=0;node<expanded.getNodeCount();node++){
                        var at=expanded.getNodePos(node);
                        if(!level.getFluidState(at).isEmpty()||!level.getFluidState(at.below()).isEmpty()){
                            stand.put("expandedFirstFluid",Map.of("node",at.toShortString(),"block",level.getBlockState(at).toString(),
                                "below",level.getBlockState(at.below()).toString(),"floor",net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,at)));
                            break;
                        }
                    }
                }
                finally{v.getNavigation().resetMaxVisitedNodesMultiplier();}
                stands.add(stand);
            }
            r.put("stands",stands);
            var passage=FoodAccess.plan(level,v,table);
            r.put("stationPassage",passage==null?Map.of("found",false):Map.of("found",true,"digs",passage.digCount(),"steps",passage.steps().stream().map(step->Map.of("feet",step.feet().toShortString(),"dig",step.dig().stream().map(BlockPos::toShortString).toList())).toList()));
            r.put("passageDiagnosis",FoodAccess.status(s));
            if(passage!=null)r.put("nextDigContacts",passage.steps().stream().flatMap(step->step.dig().stream()).distinct().limit(8)
                .map(p->Map.of("position",p.toShortString(),"block",level.getBlockState(p).toString(),"centerVisible",BoatRoutes.visible(level,v,Vec3.atCenterOf(p)),
                    "occluder",level.clip(new ClipContext(v.getEyePosition(),Vec3.atCenterOf(p),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,v)).getBlockPos().toShortString(),
                    "faceContact",String.valueOf(BlockContact.point(level,v,v.getEyePosition(),p,3)))).toList());
            if(passage==null){
                var extended=FoodAccess.plan(level,v,table,FoodAccess.MAX_DIGS,FoodAccess.MAX_VISITED*4);
                r.put("extendedStationPassage",extended==null?Map.of("found",false):Map.of("found",true,"digs",extended.digCount(),"steps",extended.steps().size()));
                r.put("extendedPassageDiagnosis",FoodAccess.status(s));
            }
        }
        if(inspect){
            // Empty-handed residents without a remembered table need this
            // diagnosis too. A missing station must not hide all meal options.
            var alternative=FoodAccess.preview(level,v);
            r.put("alternativeFood",alternative==null?Map.of("found",false):Map.of("found",true,"store",alternative.store().toShortString(),"table",String.valueOf(alternative.table()),"edible",alternative.edible(),"digs",alternative.route().digCount(),"steps",alternative.route().steps().size()));
            var exits=new ArrayList<BlockPos>();var origin=v.blockPosition();
            for(var p:BlockPos.betweenClosed(origin.offset(-6,-3,-6),origin.offset(6,1,6))){
                if(p.distSqr(origin)<16||p.distSqr(origin)>49||!level.isLoaded(p.above())
                    ||!level.getFluidState(p).isEmpty()||!level.getFluidState(p.below()).isEmpty()
                    ||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))continue;
                var body=v.getBoundingBox().move(Vec3.atBottomCenterOf(p).subtract(v.position()));
                if(level.noBlockCollision(v,body))exits.add(p.immutable());
            }
            exits.sort(Comparator.comparingDouble(p->p.distSqr(origin)));
            var routes=new ArrayList<Map<String,Object>>();
            try{
                v.getNavigation().setMaxVisitedNodesMultiplier(32);
                for(var p:exits.stream().limit(32).toList())for(int fall:List.of(1,2)){
                    var path=NativePathLimits.search(v,fall,()->NativePathLimits.alsoAvoiding(v,
                        at->!level.isLoaded(at)||!level.getFluidState(at).isEmpty()||!level.getFluidState(at.below()).isEmpty(),
                        ()->v.getNavigation().createPath(p,0,128)));
                    routes.add(Map.of("stand",p.toShortString(),"maxFall",fall,"route",route(path),
                        "dry",BoatTravel.dryPath(level,path),"home",WorkNavigation.staysHome(level,v,s,path),
                        "nodes",path==null?List.of():java.util.stream.IntStream.range(0,path.getNodeCount()).mapToObj(i->{
                            var at=path.getNodePos(i);return Map.of("at",at.toShortString(),
                                "fluid",level.getFluidState(at).toString(),"belowFluid",level.getFluidState(at.below()).toString(),
                                "below",level.getBlockState(at.below()).toString());
                        }).toList()));
                }
            }finally{v.getNavigation().resetMaxVisitedNodesMultiplier();}
            r.put("localDryExitSurvey",Map.of("eligibleStands",exits.size(),"routes",routes,
                "scope","Frozen native searches only; no path issued or coordinates taught."));
            var geometry=new ArrayList<Map<String,Object>>();
            for(var p:BlockPos.betweenClosed(origin.offset(-2,-2,-2),origin.offset(2,2,2))){
                if(!level.isLoaded(p))continue;var state=level.getBlockState(p);
                if(state.isAir())continue;
                geometry.add(Map.of("at",p.toShortString(),"block",state.toString(),
                    "collision",state.getCollisionShape(level,p).toAabbs().stream().map(box->box.move(p).toString()).toList()));
            }
            r.put("localBodyGeometry",Map.of("body",v.getBoundingBox().toString(),"cells",geometry));
            var doors=new ArrayList<BlockPos>();
            for(var p:BlockPos.betweenClosed(origin.offset(-8,-2,-8),origin.offset(8,1,8))){
                var block=level.getBlockState(p);
                if(block.is(net.minecraft.world.level.block.Blocks.IRON_DOOR)
                    &&block.getValue(net.minecraft.world.level.block.DoorBlock.HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER
                    &&NativeFoodStorage.homeStructure(level,v,s,p))doors.add(p.immutable());
            }
            doors.sort(Comparator.comparingDouble(p->p.distSqr(origin)));
            r.put("gateApproachSurvey",doors.stream().limit(4).map(p->{
                var path=NativeCampMeals.gateApproach(level,v,s,p);
                var facing=level.getBlockState(p).getValue(net.minecraft.world.level.block.DoorBlock.FACING);
                var edges=new ArrayList<Map<String,Object>>();
                for(var stand:List.of(p.relative(facing),p.relative(facing.getOpposite()))){
                    var start=v.blockPosition();
                    var blocked=NativePathLimits.search(v,1,()->NativeBodyRoutes.blockedEdge(v,
                        new net.minecraft.world.level.pathfinder.Node(start.getX(),start.getY(),start.getZ()),
                        new net.minecraft.world.level.pathfinder.Node(stand.getX(),stand.getY(),stand.getZ())));
                    edges.add(Map.of("stand",stand.toShortString(),"blockedDirectEdge",blocked,
                        "floor",net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,stand),
                        "startFloor",net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,start)));
                }
                return Map.of("door",p.toShortString(),"route",route(path),
                    "candidateEdges",edges,"nodes",path==null?List.of():java.util.stream.IntStream.range(0,path.getNodeCount()).mapToObj(i->path.getNodePos(i).toShortString()).toList());
            }).toList());
        }
        return r;
    }
    public void onInitialize(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            if(!Files.exists(out("food-diagnostic-copy.json")))throw new IllegalStateException("copy required");
            time=server.overworld().getGameTime();server.tickRateManager().setFrozen(true);
            server.tickRateManager().setTickRate(Integer.getInteger("villagers.farm.tps",20));ProbeResidents.load(server.overworld());
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(finished)return;
            try{
                ticks++;var level=server.overworld();
                if(!(level.getEntity(id) instanceof Villager v)||!Villagers.ready()||ticks<40){if(ticks>400)throw new AssertionError("saved focus body unavailable");return;}
                if(!started){if(level.getGameTime()!=time)throw new AssertionError("simulation before snapshot");initial=row(level,v,true);started=true;
                    Files.writeString(out("resident-meal-initial.json"),JSON.toJson(initial));
                    if(rehearse){server.tickRateManager().setFrozen(false);return;}}
                var current=row(level,v,false);var s=Villagers.existingSession(id);boolean guard=v.getHealth()<=(cycle?8:4);
                var residents=new ArrayList<Map<String,Object>>();
                for(var e:level.getAllEntities())if(e instanceof Villager other&&Villagers.isManaged(other)){
                    if(other.getHealth()<=(cycle?8:4))guard=true;var state=Villagers.existingSession(other.getUUID());
                    residents.add(Map.of("id",other.getUUID().toString(),"health",other.getHealth(),"food",state==null?-1:state.mind.foodLevel()));
                }
                boolean fed=s.mind.foodLevel()>=12&&s.breadCrafted>0;
                if(rehearse){elapsed++;if(elapsed%40==0){Files.writeString(out("resident-meal-progress.json"),JSON.toJson(current));
                    Files.writeString(out("resident-meal-trace.jsonl"),new Gson().toJson(Map.of("elapsed",elapsed,"actor",current))+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
                    if(!guard&&(cycle||!fed)&&elapsed<window&&!Files.exists(out("stop-observation")))return;}
                Files.writeString(out("resident-meal.json"),JSON.toJson(Map.of("status",!rehearse||!guard&&(cycle?elapsed>=window:fed)?"passed":"failed","stage",!rehearse?"frozen_station_routes":guard?"health_stop":cycle?"food_cycle_window_complete":fed?"native_bread_consumed":"bounded_observation","elapsed",elapsed,"initialGameTime",time,"gameTime",level.getGameTime(),"initial",initial,"actor",current,"residentHealth",residents)));
                finished=true;server.tickRateManager().setFrozen(false);server.halt(false);
            }catch(Throwable error){error.printStackTrace();finished=true;try{Files.writeString(out("resident-meal.json"),JSON.toJson(Map.of("status","failed","failure",error.toString())));}catch(Exception ignored){}server.tickRateManager().setFrozen(false);server.halt(false);}
        });
    }
}
