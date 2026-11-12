package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;

/** Actual saved island residents in a marked COPY. Never creates bodies, resources or routes. */
public final class NativeBoatAccessProbe implements ModInitializer {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private final boolean rehearse=Boolean.getBoolean("villagers.boats.rehearse");
    private final boolean expanded=Boolean.getBoolean("villagers.audit.expandedBorder");
    private final boolean untilIronIngots=Boolean.getBoolean("villagers.boats.untilIronIngots");
    private final boolean untilIronArmor=Boolean.getBoolean("villagers.boats.untilIronArmor");
    private final boolean untilIronPickaxe=Boolean.getBoolean("villagers.boats.untilIronPickaxe");
    private final boolean untilStonePickaxe=Boolean.getBoolean("villagers.boats.untilStonePickaxe");
    private int ticks, elapsed;
    private final int window=Integer.getInteger("villagers.boats.windowTicks",2400);
    private long initialTime;
    private boolean started, finished;
    private final List<UUID> ids=new ArrayList<>();
    private final List<Map<String,Object>> initial=new ArrayList<>();
    private final Map<UUID,BlockPos> returningHomes=new HashMap<>();
    private Path output(String name){return FabricLoader.getInstance().getGameDir().resolve(name);}
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private Map<String,Object> actor(ServerLevel level,Villager v,boolean inspect){
        var s=Villagers.session(v,level);var row=new LinkedHashMap<String,Object>();
        row.put("id",v.getUUID().toString());row.put("name",s.mind.name());row.put("position",List.of(v.getX(),v.getY(),v.getZ()));
        row.put("production",Map.of("actions",AdventureActions.activityStatus(v.getUUID()),"lastHarvest",AdventureActions.lastHarvest(v.getUUID()),"breadCrafted",s.breadCrafted,"planted",s.plotsCultivated,"lastPlant",s.lastFarmExpansion));
        row.put("health",v.getHealth());row.put("food",s.mind.foodLevel());row.put("inventory",v.getInventory().toString());
        row.put("adventure",Map.of("nominated",AdventurePreparation.nominated(v),"stonePickaxe",AdventurePreparation.stonePickaxe(v),"stone",WoodCrafting.stoneCount(v.getInventory()),"mining",AdventureMining.status(s),"activity",s.adventureStatus,"quarry",AdventureQuarry.status(s)));
        row.put("woodPreparation",WoodBootstrap.status(s));
        row.put("ironPreparation",AdventureIron.status(s));row.put("ironIngots",v.getInventory().countItem(Items.IRON_INGOT));
        row.put("villageSupplies",AdventureSupplies.status(s));row.put("ironPickaxe",AdventurePreparation.ironPickaxe(v));
        row.put("villageBedding",beddingStatus(s));row.put("ironArmor",armorStatus(v,s));
        row.put("equippedArmor",Map.of("head",v.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).toString(),
            "chest",v.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).toString(),
            "legs",v.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).toString(),
            "feet",v.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).toString()));
        row.put("inventoryRoom",Map.of("actions",s.inventoryRoomActions,"lastReceipt",s.inventoryRoomReceipt));
        row.put("body",Map.of("sleeping",v.isSleeping(),"passenger",v.isPassenger(),"activity",v.getName().getString(),"fatigue",s.mind.fatigue(),"courtship",s.courtship==null?"NONE":s.courtship.active(level.getGameTime())?"ACTIVE":"EXPIRED"));
        row.put("held",v.getMainHandItem().toString());row.put("offhand",v.getOffhandItem().toString());row.put("boat",BoatTravel.status(s));row.put("tags",v.entityTags());
        if(s.boatTrip!=null&&s.boatTrip.route!=null){
            var dock=s.boatTrip.route.launch();var nav=v.getNavigation().getPath();
            row.put("boardingApproach",Map.of("launch",dock.land().toShortString(),"home",s.boatTrip.home.land().toShortString(),
                "position",v.position().toString(),"distanceSquared",v.position().distanceToSqr(Vec3.atBottomCenterOf(dock.land())),
                "navigation",nav==null?Map.of():Map.of("end",nav.getEndNode().asBlockPos().toShortString(),"next",nav.isDone()?"":nav.getNextNodePos().toShortString(),"done",nav.isDone())));
        }
        row.put("woodJob",Map.of("tree",String.valueOf(s.boatTree),"taken",s.boatLogsTaken,"table",String.valueOf(s.craftingTable),"blockedTable",String.valueOf(s.blockedCraftTable)));
        if(inspect){
            v.getNavigation().setCanOpenDoors(true);v.getNavigation().getNodeEvaluator().setCanPassDoors(true);v.getNavigation().setCanFloat(true);
            row.put("nurseryAccess",nurseryAccess(level,v,s));
            if(AdventurePreparation.nominated(v))row.put("woodRecipeChecks",woodRecipeChecks(level,v));
            if(AdventurePreparation.nominated(v)){
                row.put("mineFaces",mineFaces(level,v,s));row.put("mineJunctionFaces",junctionFaces(level,v,s));row.put("armorRecipeChecks",armorRecipeChecks(level));
                row.put("ownedBoats",level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.boat.AbstractBoat.class,v.getBoundingBox().inflate(128),
                    boat->boat.isAlive()&&BoatTravel.owned(boat,v)).stream().map(boat->Map.of("id",boat.getUUID().toString(),"position",boat.position().toString(),
                        "distance",v.distanceTo(boat),"visible",v.hasLineOfSight(boat),"occupied",boat.isVehicle())).toList());
                var meeting=v.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.MEETING_POINT);
                if(meeting.isPresent()&&meeting.get().dimension().equals(level.dimension())){
                    var bell=meeting.get().pos();var normal=NativePathLimits.search(v,1,()->WorkNavigation.pathTo(level,v,s,bell,2));
                    net.minecraft.world.level.pathfinder.Path expanded;
                    try{
                        v.getNavigation().setMaxVisitedNodesMultiplier(32);
                        expanded=NativePathLimits.search(v,1,()->v.getNavigation().createPath(bell,2,128));
                    }finally{v.getNavigation().resetMaxVisitedNodesMultiplier();}
                    row.put("publicWoodSurvey",Map.of("bell",bell.toShortString(),"exists",level.getBlockState(bell).is(Blocks.BELL),
                        "authorized",VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(bell)),
                        "normal",routeSummary(level,v,s,normal),"expanded",routeSummary(level,v,s,expanded)));
                }
            }
            if(AdventurePreparation.nominated(v))row.put("quarrySites",quarrySites(level,v,s));
            if(AdventurePreparation.nominated(v)){
                row.put("quarryInteraction",quarryInteraction(level,v,s));
                var naturalWood=new ArrayList<Map<String,Object>>();
                for(var p:List.of(new BlockPos(-503,63,-327),new BlockPos(-476,66,-359),new BlockPos(-477,66,-350),new BlockPos(-476,64,-363))){
                    if(!level.isLoaded(p))continue;
                    naturalWood.add(Map.of("position",p.toShortString(),"block",level.getBlockState(p).toString(),"eligible",BoatPreparation.naturalTrunk(level,p),"route",routeSummary(level,v,s,WoodBootstrap.approachPath(level,v,s,p)),
                        "stands",BoatPreparation.naturalTrunk(level,p)?woodStands(level,v,s,p):List.of()));
                }
                row.put("naturalWood",naturalWood);
            }
            var localBench=new BlockPos(-525,63,-347);
            if(level.isLoaded(localBench)&&level.getBlockState(localBench).is(Blocks.CRAFTING_TABLE))row.put("westernPublicBench",Map.of("position",localBench.toShortString(),"route",routeSummary(level,v,s,WoodBootstrap.approachPath(level,v,s,localBench))));
            var station=BreadCrafting.table(level,v,s);
            if(station!=null){
                var stands=new ArrayList<BlockPos>();var routes=new ArrayList<Map<String,Object>>();
                for(var p:BlockPos.betweenClosed(station.offset(-2,-1,-2),station.offset(2,1,2))){
                    if(!level.isLoaded(p)||!level.getBlockState(p).isAir()||!level.getBlockState(p.above()).isAir()
                        ||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),net.minecraft.core.Direction.UP))continue;
                    var eye=Vec3.atBottomCenterOf(p).add(0,v.getEyeHeight(),0);
                    var sight=level.clip(new net.minecraft.world.level.ClipContext(eye,Vec3.atCenterOf(station),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,v));
                    if(eye.distanceToSqr(Vec3.atCenterOf(station))<=9&&(sight.getType()==net.minecraft.world.phys.HitResult.Type.MISS||sight.getBlockPos().equals(station)))stands.add(p.immutable());
                }
                stands.sort(Comparator.comparingDouble(p->v.position().distanceToSqr(Vec3.atBottomCenterOf(p))));
                for(var p:stands.stream().limit(3).toList()){
                    var ordinary=v.getNavigation().createPath(p,0);
                    var range64=v.getNavigation().createPath(p,0,64);
                    net.minecraft.world.level.pathfinder.Path budget64;
                    try{v.getNavigation().setMaxVisitedNodesMultiplier(2);budget64=v.getNavigation().createPath(p,0,64);}
                    finally{v.getNavigation().resetMaxVisitedNodesMultiplier();}
                    var waterType=net.minecraft.world.level.pathfinder.PathType.WATER;
                    float old=v.getPathfindingMalus(waterType);
                    net.minecraft.world.level.pathfinder.Path dry;
                    try{v.setPathfindingMalus(waterType,-1);dry=v.getNavigation().createPath(p,0);}
                    finally{v.setPathfindingMalus(waterType,old);}
                    var data=new LinkedHashMap<String,Object>();data.put("stand",p.toShortString());
                    data.put("ordinary",routeSummary(level,v,s,ordinary));data.put("waterExcluded",routeSummary(level,v,s,dry));
                    data.put("range64",routeSummary(level,v,s,range64));data.put("range64Budget2",routeSummary(level,v,s,budget64));routes.add(data);
                }
                row.put("station",station.toShortString());row.put("stationRoutes",routes);
                row.put("followRange",v.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE));
            }
            if(s.mind.foodLevel()<=6){
                var food=FoodAccess.preview(level,v);
                row.put("foodPlan",food==null?Map.of("found",false,"search",s.foodAccess.lastPlan):Map.of("found",true,"store",food.store().toShortString(),"table",food.table()==null?"":food.table().toShortString(),"edible",food.edible(),"steps",food.route().steps().size()));
            }
            v.getNavigation().setCanOpenDoors(true);v.getNavigation().getNodeEvaluator().setCanPassDoors(true);v.getNavigation().setCanFloat(true);
            var trees=new ArrayList<String>();var tables=new ArrayList<Map<String,Object>>();var crops=new ArrayList<String>();
            var origin=v.blockPosition();
            for(var p:BlockPos.betweenClosed(origin.offset(-16,-3,-16),origin.offset(16,3,16))){
                if(!level.isLoaded(p))continue;
                if(BoatPreparation.naturalTrunk(level,p))trees.add(p.toShortString());
                if(level.getBlockState(p).is(Blocks.CRAFTING_TABLE))tables.add(Map.of("position",p.toShortString(),"visible",BoatRoutes.visible(level,v,Vec3.atCenterOf(p))));
                var block=level.getBlockState(p);if(block.getBlock() instanceof CropBlock crop&&crop.isMaxAge(block))crops.add(p.toShortString());
            }
            row.put("trees",trees);row.put("tables",tables);row.put("ripeCrops",crops);
            var meeting=v.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.MEETING_POINT);
            if(meeting.isPresent()){
                var bell=meeting.get().pos();var fields=new ArrayList<Map<String,Object>>();
                for(var p:BlockPos.betweenClosed(bell.offset(-16,-3,-16),bell.offset(16,3,16))){
                    if(!level.isLoaded(p)||!(level.getBlockState(p).getBlock() instanceof CropBlock crop))continue;
                    var path=v.getNavigation().createPath(p,1);boolean dry=path!=null&&path.canReach();
                    if(dry)for(int i=0;i<path.getNodeCount();i++)if(!level.getFluidState(path.getNodePos(i)).isEmpty()){dry=false;break;}
                    fields.add(Map.of("position",p.toShortString(),"mature",crop.isMaxAge(level.getBlockState(p)),"visible",BoatRoutes.visible(level,v,Vec3.atCenterOf(p)),"nativeDryRoute",dry,"boundedRoute",dry&&WorkNavigation.staysHome(level,v,s,path)));
                }
                row.put("meetingPoint",bell.toShortString());row.put("fields",fields);
                var villageTrees=new ArrayList<Map<String,Object>>();
                for(var p:BlockPos.betweenClosed(bell.offset(-48,-6,-48),bell.offset(48,6,48))){
                    if(!BoatPreparation.naturalTrunk(level,p)||!NativeVillageWorkArea.contains(level,s.mind,Vec3.atCenterOf(p)))continue;
                    var path=WoodBootstrap.approachPath(level,v,s,p);boolean dry=path!=null&&path.canReach();
                    if(dry)for(int i=0;i<path.getNodeCount();i++)if(!level.getFluidState(path.getNodePos(i)).isEmpty()){dry=false;break;}
                    villageTrees.add(Map.of("root",p.toShortString(),"boundedApproach",path!=null&&path.canReach(),"dryApproach",dry));
                }
                row.put("villageTrees",villageTrees);
                var trunks=new ArrayList<Map<String,Object>>();
                for(var p:BlockPos.betweenClosed(bell.offset(-64,-10,-64),bell.offset(64,12,64))){
                    var b=level.getBlockState(p);
                    if(!b.is(net.minecraft.tags.BlockTags.LOGS)||!(b.getBlock() instanceof RotatedPillarBlock)
                        ||b.getValue(RotatedPillarBlock.AXIS)!=net.minecraft.core.Direction.Axis.Y
                        ||level.getBlockState(p.below()).is(net.minecraft.tags.BlockTags.LOGS))continue;
                    int height=0;while(height<16&&level.getBlockState(p.above(height)).is(b.getBlock()))height++;
                    int leaves=0;for(var q:BlockPos.betweenClosed(p.offset(-2,height-1,-2),p.offset(2,height+1,2)))
                        if(level.getBlockState(q).is(net.minecraft.tags.BlockTags.LEAVES)&&!level.getBlockState(q).getValue(LeavesBlock.PERSISTENT))leaves++;
                    var obstacles=new ArrayList<String>();for(var q:BlockPos.betweenClosed(p.offset(-3,-1,-3),p.offset(3,3,3))){
                        var n=level.getBlockState(q);
                        if(level.getBlockEntity(q)!=null||n.getBlock() instanceof BedBlock||n.getBlock() instanceof DoorBlock
                            ||n.is(net.minecraft.tags.BlockTags.PLANKS)&&Math.abs(q.getX()-p.getX())+Math.abs(q.getZ()-p.getZ())<=1)
                            if(obstacles.size()<3)obstacles.add(q.toShortString()+":"+n);
                    }
                    if(trunks.size()<96)trunks.add(Map.of("base",p.toShortString(),"block",b.toString(),"soil",level.getBlockState(p.below()).toString(),
                        "height",height,"naturalCrownLeaves",leaves,"nearbyFurniture",obstacles,"eligible",BoatPreparation.naturalTrunk(level,p),
                        "inWorkArea",BoatPreparation.woodInReachOfHome(level,s,p)));
                }
                row.put("trunkSurvey",trunks);
                var stones=new ArrayList<Map<String,Object>>();
                for(var p:BlockPos.betweenClosed(bell.offset(-48,-8,-48),bell.offset(48,8,48))){
                    if(!level.isLoaded(p)||!NativeVillageWorkArea.contains(level,s.mind,Vec3.atCenterOf(p)))continue;
                    var state=level.getBlockState(p);
                    if(!(state.is(Blocks.STONE)||state.is(Blocks.DEEPSLATE))||!level.getBlockState(p.above()).isAir()
                        ||level.structureManager().getStructureWithPieceAt(p,h->true).isValid())continue;
                    stones.add(Map.of("position",p.toShortString(),"eligible",AdventureMining.surface(level,v,s,p),"visible",BoatRoutes.visible(level,v,Vec3.atCenterOf(p))));
                    if(stones.size()>=40)break;
                }
                row.put("surfaceStone",stones);
            }
            var docks=new ArrayList<Map<String,Object>>();
            for(var dock:BoatRoutes.launches(level,v).stream().limit(3).toList()){
                var path=v.getNavigation().createPath(dock.land(),0);
                docks.add(Map.of("land",dock.land().toShortString(),"water",dock.water().toShortString(),"reachable",path!=null&&path.canReach()));
            }
            row.put("launches",docks);var route=BoatRoutes.find(level,v,null);
            row.put("observedCrossing",route==null?Map.of("found",false):Map.of("found",true,"launch",route.launch().land().toShortString(),"arrival",route.arrival().land().toShortString(),"points",route.points().size()));
        }
        return row;
    }
    private static Map<String,Object> quarrySites(ServerLevel level,Villager v,Session s){
        try{
            var site=AdventureQuarry.class.getDeclaredMethod("site",ServerLevel.class,Villager.class,Session.class,BlockPos.class,net.minecraft.core.Direction.class);site.setAccessible(true);
            var safe=AdventureQuarry.class.getDeclaredMethod("safePath",ServerLevel.class,Villager.class,Session.class,net.minecraft.world.level.pathfinder.Path.class);safe.setAccessible(true);
            var approachMethod=AdventureQuarry.class.getDeclaredMethod("approach",ServerLevel.class,Villager.class,Session.class,BlockPos.class);approachMethod.setAccessible(true);
            var origin=v.blockPosition();var roots=new ArrayList<BlockPos>();var counts=new TreeMap<String,Integer>();var examples=new ArrayList<Map<String,Object>>();var readyExamples=new ArrayList<Map<String,Object>>();
            for(var root:BlockPos.betweenClosed(origin.offset(-28,-3,-28),origin.offset(28,1,28))){
                if(!level.isLoaded(root)||!level.getBlockState(root).isAir()||!level.getBlockState(root.above()).isAir())continue;
                var b=level.getBlockState(root.below());if(b.is(Blocks.DIRT)||b.is(Blocks.GRASS_BLOCK)||b.is(Blocks.COARSE_DIRT))roots.add(root.immutable());
            }
            roots.sort(Comparator.comparingDouble(p->p.distSqr(origin)));
            for(var root:roots)for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL){
                var p=AdventureQuarry.cells(root,direction).getFirst();if(!level.isLoaded(p))continue;var b=level.getBlockState(p);
                boolean natural=b.is(Blocks.DIRT)||b.is(Blocks.GRASS_BLOCK)||b.is(Blocks.COARSE_DIRT);
                if(!natural)continue;var reasons=new ArrayList<String>();
                if(!VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(p)))reasons.add("outside work range");
                if(ConstructionSites.isProtected(level,p))reasons.add("protected");
                for(var tag:v.entityTags()){var failed=AdventureQuarry.failure(tag,level.getGameTime());if(failed!=null&&!failed.expired(level.getGameTime())&&failed.root().distSqr(root)<16){reasons.add("recent failed site");break;}}
                if(NativeTerrainWork.structuralCover(level,p)!=null)reasons.add("supports native construction");
                if(!level.canSeeSky(p.above()))reasons.add("covered");
                if(!BoatRoutes.visible(level,v,Vec3.atCenterOf(p).add(0,.5,0)))reasons.add("not visible top");
                if(!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),net.minecraft.core.Direction.UP))reasons.add("unsupported floor");
                for(var d:net.minecraft.core.Direction.values()){
                    var n=p.relative(d);if(!level.isLoaded(n)){reasons.add("adjacent fluid or unloaded");continue;}
                    if(!level.getFluidState(n).isEmpty())reasons.add("adjacent fluid or unloaded");
                    if(level.getBlockState(n).getBlock() instanceof FallingBlock)reasons.add("adjacent falling block");
                }
                for(var n:BlockPos.betweenClosed(p.offset(-2,-1,-2),p.offset(2,2,2))){
                    if(!level.isLoaded(n)||ConstructionSites.isProtected(level,n)||!VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(n)))reasons.add("near work boundary");
                    if(!level.isLoaded(n))continue;var neighbor=level.getBlockState(n);
                    if(level.getBlockEntity(n)!=null||neighbor.getBlock() instanceof BedBlock||neighbor.getBlock() instanceof DoorBlock||neighbor.getBlock() instanceof CropBlock||neighbor.is(net.minecraft.tags.BlockTags.PLANKS))reasons.add("near furnishings or crops");
                }
                boolean ready=(Boolean)site.invoke(null,level,v,s,root,direction);boolean approach=false;
                if(ready){counts.merge("ready site",1,Integer::sum);var path=approachMethod.invoke(null,level,v,s,p);approach=(Boolean)safe.invoke(null,level,v,s,path);counts.merge(approach?"safe native approach":"no safe native approach",1,Integer::sum);}
                if(ready&&readyExamples.size()<6)readyExamples.add(Map.of("root",root.toShortString(),"direction",direction.name(),"safeApproach",approach,"reasons",new HashSet<>(reasons)));
                for(var reason:new HashSet<>(reasons))counts.merge(reason,1,Integer::sum);
                if(examples.size()<12)examples.add(Map.of("root",root.toShortString(),"direction",direction.name(),"target",p.toShortString(),"ready",ready,"safeApproach",approach,"reasons",new HashSet<>(reasons)));
            }
            return Map.of("candidateRoots",roots.size(),"counts",counts,"examples",examples,"readyExamples",readyExamples,"surveyRadius",28);
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Readonly quarry diagnostics",error);}
    }
    private Map<String,Object> quarryInteraction(ServerLevel level,Villager v,Session s){
        for(var tag:v.entityTags())if(tag.startsWith("villagers:quarry:")){
            var parts=tag.substring("villagers:quarry:".length()).split(":");
            var cells=AdventureQuarry.cells(BlockPos.of(Long.parseLong(parts[0])),net.minecraft.core.Direction.valueOf(parts[1]));
            int index=Integer.parseInt(parts[2]);while(index<cells.size()&&level.getBlockState(cells.get(index)).isAir())index++;
            if(index>=cells.size())return Map.of("cleared",true);
            var target=cells.get(index);var stands=new ArrayList<Map<String,Object>>();
            for(var p:BlockPos.betweenClosed(target.offset(-2,-3,-2),target.offset(2,1,2))){
                if(!level.getBlockState(p).isAir()||!level.getBlockState(p.above()).isAir()
                    ||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),net.minecraft.core.Direction.UP))continue;
                var failure=ForestryWork.standFailure(level,v,s,target,p);var hit=ForestryWork.clip(level,v,ForestryWork.standEye(v,p),target);
                var body=v.getBoundingBox().move(p.getX()+.5-v.getX(),p.getY()-v.getY(),p.getZ()+.5-v.getZ());
                var row=new LinkedHashMap<String,Object>();row.put("stand",p.toShortString());row.put("failure",String.valueOf(failure));
                row.put("hit",hit==null?"unloaded":hit.getBlockPos().toShortString());
                row.put("overlap",body.intersects(new net.minecraft.world.phys.AABB(target).expandTowards(0,2,0)));
                if(failure==null)row.put("path",routeSummary(level,v,s,WorkNavigation.pathTo(level,v,s,p,0)));
                stands.add(row);
            }
            var hit=ForestryWork.clip(level,v,v.getEyePosition(),target);
            return Map.of("target",target.toShortString(),"block",level.getBlockState(target).toString(),"eye",v.getEyePosition().toString(),
                "reach",v.getEyePosition().distanceTo(Vec3.atCenterOf(target)),"hit",hit==null?"unloaded":hit.getBlockPos().toShortString(),"stands",stands);
        }
        return Map.of();
    }
    /** Operator-only inspection of the worker's recorded junctions; no actor cache writes or routes. */
    private List<Map<String,Object>> junctionFaces(ServerLevel level,Villager v,Session s){
        MineStair.resume(v,s);var rows=new ArrayList<Map<String,Object>>();
        var floors=s.mineStair.floors.stream().sorted(Comparator.<BlockPos>comparingInt(p->p.getY()).thenComparingDouble(p->p.distSqr(v.blockPosition()))).limit(512).toList();
        for(var floor:floors){
            var origin=floor.above();if(!level.isLoaded(origin.above(2)))continue;
            for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)for(boolean flat:List.of(false,true)){
                var cells=MineStair.step(origin,direction,1,flat);var foot=cells.getLast();
                var first=cells.stream().filter(p->!MineStair.clearCell(level.getBlockState(p))).findFirst().orElse(null);
                var row=new LinkedHashMap<String,Object>();row.put("origin",origin.toShortString());row.put("direction",direction.name());row.put("flat",flat);
                var seal=MineWaterSeal.preview(level,v,s,origin,direction,flat);
                row.put("sealPreview",seal==null?Map.of("present",false):Map.of("present",true,"water",seal.water().toShortString(),"face",seal.face().toShortString(),"stance",seal.stand().toString()));
                row.put("newWorkFloor",MineStair.newWorkFloor(s.mineStair,cells));row.put("support",level.getBlockState(foot.below()).toString());
                row.put("originClear",MineStair.clearCell(level.getBlockState(origin))&&MineStair.clearCell(level.getBlockState(origin.above())));
                var foundationHit=MineFoundation.contact(level,v,foot.below(),ForestryWork.standEye(v,origin));
                row.put("foundationContactFromRecordedFloor",foundationHit==null?"":foundationHit.getLocation().toString());
                row.put("sturdy",level.getBlockState(foot.below()).isFaceSturdy(level,foot.below(),net.minecraft.core.Direction.UP));
                row.put("cells",cells.stream().map(p->Map.of("position",p.toShortString(),"block",level.getBlockState(p).toString())).toList());
                row.put("activeEntry",String.valueOf(s.mineStair.entry));row.put("quarryEnabled",Villagers.config().nativeAdventureQuarry());
                if(first!=null){
                    row.put("first",first.toShortString());row.put("firstBlock",level.getBlockState(first).toString());
                    row.put("exposedAtRecordedFloor",AdventureMining.exposedAt(level,v,first,true,origin.getY()));
                    row.put("contactFromRecordedFloor",String.valueOf(BlockContact.point(level,v,ForestryWork.standEye(v,origin),first,3)));
                    row.put("retainedExitFloor",MineStair.exitFloor(v,first));row.put("structuralCover",String.valueOf(NativeTerrainWork.structuralCover(level,first)));
                    var fluids=new ArrayList<String>();for(var side:net.minecraft.core.Direction.values()){
                        var neighbor=first.relative(side);if(!level.getFluidState(neighbor).isEmpty())fluids.add(neighbor.toShortString()+":"+level.getFluidState(neighbor));
                    }row.put("fluidNeighbors",fluids);
                    if(row.get("newWorkFloor").equals(true)&&row.get("sturdy").equals(true)&&!fluids.isEmpty()){
                        var water=MineWaterSeal.waters(level,first);var center=Vec3.atBottomCenterOf(origin);var stances=new ArrayList<Vec3>();stances.add(center);
                        for(var d:net.minecraft.core.Direction.Plane.HORIZONTAL)stances.add(center.add(d.getStepX()*.6,0,d.getStepZ()*.6));
                        var checks=new ArrayList<Map<String,Object>>();
                        for(var target:water){
                            var contacts=new ArrayList<Map<String,Object>>();
                            for(var stand:stances){var hit=MineFoundation.contact(level,v,target,stand.add(0,v.getEyeHeight(),0));
                                contacts.add(Map.of("stance",stand.toString(),"supported",MineWaterSeal.supportedWalk(level,v,center,stand),"contact",hit==null?"":hit.toString()));}
                            checks.add(Map.of("water",target.toShortString(),"protected",ConstructionSites.isProtected(level,target),
                                "structuralCover",String.valueOf(NativeTerrainWork.structuralCover(level,target)),"withinWorkArea",VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(target)),"occupied",!level.getEntities(null,new net.minecraft.world.phys.AABB(target)).isEmpty(),"stances",contacts));
                        }
                        row.put("sealDiagnosis",Map.of("afterSeals",AdventureMining.afterWaterSeals(level,v,first,origin.getY(),water),"dirt",v.getInventory().countItem(Items.DIRT),"checks",checks));
                    }
                }
                rows.add(row);
            }
        }
        return rows;
    }
    private List<Map<String,Object>> mineFaces(ServerLevel level,Villager v,Session s){
        MineStair.resume(v,s);var rows=new ArrayList<Map<String,Object>>();var origin=v.blockPosition();
        for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)for(boolean flat:List.of(false,true)){
            var cells=MineStair.step(origin,direction,1,flat);var first=cells.stream().filter(p->!level.getBlockState(p).isAir()).findFirst().orElse(null);
            var row=new LinkedHashMap<String,Object>();row.put("direction",direction.name());row.put("flat",flat);
            row.put("feet",cells.getLast().toShortString());row.put("newWorkFloor",MineStair.newWorkFloor(s.mineStair,cells));
            row.put("support",level.getBlockState(cells.getLast().below()).toString());
            row.put("foundationCandidate",MineFoundation.candidate(level,v,s,origin,direction,flat));
            var foundationHit=MineFoundation.contact(level,v,cells.getLast().below());
            row.put("foundationContact",foundationHit==null?Map.of():Map.of("anchor",foundationHit.getBlockPos().toShortString(),
                "face",foundationHit.getDirection().name(),"point",foundationHit.getLocation().toString(),
                "reach",v.getEyePosition().distanceTo(foundationHit.getLocation())));
            row.put("sturdy",level.getBlockState(cells.getLast().below()).isFaceSturdy(level,cells.getLast().below(),net.minecraft.core.Direction.UP));
            if(first!=null){row.put("first",first.toShortString());row.put("block",level.getBlockState(first).toString());
                row.put("exposed",AdventureMining.exposed(level,v,first,true));row.put("contact",String.valueOf(BlockContact.point(level,v,v.getEyePosition(),first,3)));
                row.put("structuralCover",String.valueOf(NativeTerrainWork.structuralCover(level,first)));}
            rows.add(row);
        }
        return rows;
    }
    /** Temporary recipe inputs never enter an actor, container block or world item. */
    private Object armorRecipeChecks(ServerLevel level){
        try{
            var type=Class.forName("local.villagers.AdventureArmor");var piece=Class.forName("local.villagers.AdventureArmor$Piece");
            var planner=type.getDeclaredMethod("plan",net.minecraft.world.Container.class,piece);planner.setAccessible(true);
            var count=piece.getDeclaredField("iron");count.setAccessible(true);var item=piece.getDeclaredField("item");item.setAccessible(true);
            var rows=new ArrayList<Map<String,Object>>();
            for(var target:piece.getEnumConstants()){
                int iron=count.getInt(target);var material=new net.minecraft.world.SimpleContainer(new net.minecraft.world.item.ItemStack(Items.IRON_INGOT,iron));
                var plan=planner.invoke(null,material,target);var getter=plan.getClass().getDeclaredMethod("input");getter.setAccessible(true);
                var input=(net.minecraft.world.item.crafting.CraftingInput)getter.invoke(plan);
                var recipe=level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,level);
                boolean matches=recipe.isPresent()&&recipe.get().value().assemble(input).is((net.minecraft.world.item.Item)item.get(target))
                    &&recipe.get().value().assemble(input).getCount()==1&&recipe.get().value().getRemainingItems(input).stream().allMatch(net.minecraft.world.item.ItemStack::isEmpty);
                check(matches,"native armor recipe matches conserved grid: "+target);
                rows.add(Map.of("piece",target.toString(),"iron",iron,"nativeRecipeMatches",matches));
            }
            return rows;
        }catch(ClassNotFoundException|NoSuchMethodException absent){return Map.of("implementedInPinnedJar",false);}
        catch(ReflectiveOperationException error){throw new IllegalStateException("Native armor recipe diagnostics",error);}
    }
    private Map<String,Object> nurseryAccess(ServerLevel level,Villager v,Session s){
        try{
            var sheltered=VillageFamilies.class.getDeclaredMethod("shelteredBed",ServerLevel.class,BlockPos.class);sheltered.setAccessible(true);
            var free=new HashSet<>(level.getPoiManager().findAll(t->t.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME),level::isLoaded,
                v.blockPosition(),64,net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.HAS_SPACE).toList());
            var rows=new ArrayList<Map<String,Object>>();
            for(var p:level.getPoiManager().findAll(t->t.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME),level::isLoaded,
                v.blockPosition(),64,net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY).toList()){
                var row=new LinkedHashMap<String,Object>();row.put("position",p.toShortString());row.put("distance",Math.sqrt(p.distSqr(v.blockPosition())));
                row.put("free",free.contains(p));row.put("sheltered",sheltered.invoke(null,level,p));row.put("allowed",TraderRecruitment.allowedBed(level,p));
                row.put("above",level.getBlockState(p.above()).toString());row.put("above2",level.getBlockState(p.above(2)).toString());
                if(free.contains(p))row.put("route",routeSummary(level,v,s,WorkNavigation.pathTo(level,v,s,p,1)));
                rows.add(row);
            }
            return Map.of("currentNurseries",VillageFamilies.nurseries(level,v).stream().map(BlockPos::toShortString).toList(),"bedsWithin64",rows);
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Readonly nursery diagnostics",error);}
    }
    /** Read-only native matching against this body's actual possessions. */
    private Map<String,Object> woodRecipeChecks(ServerLevel level,Villager v){
        var rows=new ArrayList<Map<String,Object>>();
        for(var kind:List.of("STONE_PICKAXE","TABLE")){
            var plan=kind.equals("TABLE")?WoodCrafting.table(v.getInventory()):WoodCrafting.stonePickaxe(v.getInventory());
            var row=new LinkedHashMap<String,Object>();row.put("goal",kind);row.put("plan",plan==null?"none":plan.step().name());
            if(plan!=null){
                row.put("expected",plan.count()+" "+plan.output());
                var recipe=level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,plan.input(),level);
                row.put("nativeRecipe",recipe.isPresent());
                if(recipe.isPresent()){
                    var result=recipe.get().value().assemble(plan.input());row.put("actual",result.toString());
                    row.put("remainingItems",recipe.get().value().getRemainingItems(plan.input()).toString());
                    var bag=new net.minecraft.world.SimpleContainer(v.getInventory().getContainerSize());
                    for(int slot=0;slot<bag.getContainerSize();slot++)bag.setItem(slot,plan.remaining().getItem(slot).copy());
                    row.put("fits",bag.addItem(result.copy()).isEmpty());
                }
            }
            rows.add(row);
        }
        // A recipe lookup with temporary grid values, never inserted into any
        // entity, inventory, block or loot source. This checks this pack only.
        var diamondInput=net.minecraft.world.item.crafting.CraftingInput.of(3,3,List.of(
            new net.minecraft.world.item.ItemStack(Items.DIAMOND),new net.minecraft.world.item.ItemStack(Items.DIAMOND),new net.minecraft.world.item.ItemStack(Items.DIAMOND),
            net.minecraft.world.item.ItemStack.EMPTY,new net.minecraft.world.item.ItemStack(Items.STICK),net.minecraft.world.item.ItemStack.EMPTY,
            net.minecraft.world.item.ItemStack.EMPTY,new net.minecraft.world.item.ItemStack(Items.STICK),net.minecraft.world.item.ItemStack.EMPTY));
        var diamondRecipe=level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,diamondInput,level);
        var diamondCheck=new LinkedHashMap<String,Object>();diamondCheck.put("temporaryDiagnosticInputsOnly",true);
        diamondCheck.put("nativeRecipe",diamondRecipe.isPresent());
        if(diamondRecipe.isPresent()){
            var output=diamondRecipe.get().value().assemble(diamondInput);
            diamondCheck.put("output",output.toString());
            diamondCheck.put("matches",output.is(Items.DIAMOND_PICKAXE)&&output.getCount()==1
                &&diamondRecipe.get().value().getRemainingItems(diamondInput).stream().allMatch(net.minecraft.world.item.ItemStack::isEmpty));
        }
        return Map.of("inventorySlots",v.getInventory().getContainerSize(),"plans",rows,"diamondRecipeDiagnostic",diamondCheck);
    }
    private Map<String,Object> routeSummary(ServerLevel level,Villager v,Session s,net.minecraft.world.level.pathfinder.Path path){
        if(path==null)return Map.of("exists",false);
        var wet=new ArrayList<String>();for(int i=0;i<path.getNodeCount();i++)if(!level.getFluidState(path.getNodePos(i)).isEmpty())wet.add(path.getNodePos(i).toShortString());
        return Map.of("exists",true,"canReach",path.canReach(),"bounded",WorkNavigation.staysHome(level,v,s,path),"nodes",path.getNodeCount(),"wet",wet,"end",path.getEndNode().asBlockPos().toShortString());
    }
    private List<Map<String,Object>> woodStands(ServerLevel level,Villager v,Session s,BlockPos target){
        var positions=new ArrayList<BlockPos>();
        for(var p:BlockPos.betweenClosed(target.offset(-2,-4,-2),target.offset(2,1,2)))
            if(ForestryWork.standFailure(level,v,s,target,p)==null&&BlockContact.point(level,v,ForestryWork.standEye(v,p),target,2.5)!=null)positions.add(p.immutable());
        positions.sort(Comparator.comparingDouble(p->p.distSqr(v.blockPosition())));var rows=new ArrayList<Map<String,Object>>();
        for(var p:positions.stream().limit(16).toList()){
            var path=NativePathLimits.search(v,1,()->v.getNavigation().createPath(p,0,128));
            net.minecraft.world.level.pathfinder.Path longer;
            try{v.getNavigation().setMaxVisitedNodesMultiplier(32);longer=NativePathLimits.search(v,1,()->v.getNavigation().createPath(p,0,128));}
            finally{v.getNavigation().resetMaxVisitedNodesMultiplier();}
            rows.add(Map.of("position",p.toShortString(),"normal",routeSummary(level,v,s,path),"largerBudget",routeSummary(level,v,s,longer)));
        }
        return rows;
    }
    /** Older explicitly pinned production JARs can still be inspected without
     * pretending they contain the newly implemented bedding behavior. */
    private Object beddingStatus(Session s){
        try{
            var type=Class.forName("local.villagers.VillageBedding");var method=type.getDeclaredMethod("status",Session.class);
            method.setAccessible(true);return method.invoke(null,s);
        }catch(ClassNotFoundException|NoSuchMethodException absent){return Map.of("implementedInPinnedJar",false);}
        catch(ReflectiveOperationException error){throw new IllegalStateException("Pinned bedding diagnostics failed",error);}
    }
    private Map<?,?> armorStatus(Villager v,Session s){
        try{
            var type=Class.forName("local.villagers.AdventureArmor");var method=type.getDeclaredMethod("status",Villager.class,Session.class);
            method.setAccessible(true);return (Map<?,?>)method.invoke(null,v,s);
        }catch(ClassNotFoundException|NoSuchMethodException absent){
            if(untilIronArmor)throw new IllegalStateException("Pinned production JAR has no armor stage",absent);
            return Map.of("implementedInPinnedJar",false);
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Pinned armor diagnostics failed",error);}
    }
    @Override public void onInitialize(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            check(Files.exists(output("food-diagnostic-copy.json")),"verified copy marker required");
            initialTime=server.overworld().getGameTime();server.tickRateManager().setFrozen(true);
            ProbeResidents.load(server.overworld());
            if(rehearse)server.tickRateManager().setTickRate(Integer.getInteger("villagers.farm.tps",20));
            for(int x=rehearse?-40:expanded?-35:-33;x<=(rehearse?-21:-25);x++)for(int z=rehearse?-32:-27;z<=(rehearse?-11:-15);z++){server.overworld().setChunkForced(x,z,true);server.overworld().getChunk(x,z);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(finished)return;
            try{
                ticks++;var level=server.overworld();
                if(!started){
                    check(ticks<400,"copy entity loading budget");check(level.getGameTime()==initialTime,"no simulation before initial snapshot");
                    var bodies=new ArrayList<Villager>();
                    for(var e:level.getAllEntities())if(e instanceof Villager v&&v.isAlive()&&Villagers.isManaged(v)&&v.getX()<0)bodies.add(v);
                    if(bodies.size()<7||ticks<40||!Villagers.ready())return;
                    if(expanded){
                        NativeExploration.expand(level);
                        var marker=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("native-exploration.json");
                        var before=Files.readString(marker);NativeExploration.expand(level);
                        check(before.equals(Files.readString(marker)),"exploration configuration must be idempotent");
                        check(level.getWorldBorder().getSize()==8192,"native exploration border");
                    }
                    if(rehearse&&Villagers.config().nativeCampMeals()){
                        var before=new HashMap<UUID,String>();
                        for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.monster.illager.Pillager guard)
                            before.put(guard.getUUID(),guard.getHealth()+":"+guard.getInventory()+":"+guard.getMainHandItem()+":"+guard.getOffhandItem());
                        int enrolled=NativeIllagers.enroll(level);
                        check(!NativeIllagers.founders().isEmpty(),"camp-meal rehearsal needs an existing resource-ready native founding pair");
                        check(NativeIllagers.enroll(level)==0,"native camp enrollment cannot replenish founders");
                        for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.monster.illager.Pillager guard)
                            check(Objects.equals(before.get(guard.getUUID()),guard.getHealth()+":"+guard.getInventory()+":"+guard.getMainHandItem()+":"+guard.getOffhandItem()),"enrollment preserves actual guard bodies and possessions");
                        Files.writeString(output("native-camp-enrollment.json"),JSON.toJson(Map.of("enrolledExisting",enrolled,
                            "founders",NativeIllagers.founders(),"spawnedBodies",0,"suppliedItems",0,"scope","Existing resource-ready native pair; no birth or production inferred from enrollment")));
                    }
                    for(var v:bodies){
                        ids.add(v.getUUID());initial.add(actor(level,v,true));
                        for(var tag:v.entityTags())if(tag.startsWith("villagers:sea:RETURN:")||tag.startsWith("villagers:sea:ASHORE:")||tag.startsWith("villagers:sea:SAIL:")){
                            var p=tag.substring("villagers:sea:".length()).split(":")[1].split(",");
                            returningHomes.put(v.getUUID(),new BlockPos(Integer.parseInt(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2])));
                        }
                    }
                    started=true;
                    if(rehearse){Files.writeString(output("native-boat-initial.json"),JSON.toJson(initial));server.tickRateManager().setFrozen(false);return;}
                }
                var current=new ArrayList<Map<String,Object>>();boolean returned=false,returnedWithFood=false,stone=false,stonePickaxe=false,ironReady=false,ironPickaxe=false,ironArmor=false;
                var residentHealth=new ArrayList<Map<String,Object>>();
                for(var entity:level.getAllEntities())if(entity instanceof Villager resident&&Villagers.isManaged(resident)&&resident.isAlive()){
                    var mind=Villagers.existingSession(resident.getUUID());
                    residentHealth.add(Map.of("id",resident.getUUID().toString(),"health",resident.getHealth(),"food",mind==null?-1:mind.mind.foodLevel(),
                        "name",resident.getName().getString(),"position",resident.blockPosition().toShortString(),
                        "campMealRequests",mind==null?0:mind.campMealRequests,"lastFoodReceived",mind==null?Map.of():mind.lastFoodReceived));
                    if(rehearse&&Files.exists(output("managed-residents-before.json")))check(resident.getHealth()>=8,"copy resident health stop: "+resident.getUUID());
                }
                var campGuards=new ArrayList<Map<String,Object>>();
                for(var entity:level.getAllEntities())if(entity instanceof net.minecraft.world.entity.monster.illager.AbstractIllager guard
                    &&CreatureAgents.managed(guard)&&NativeIllagers.member(guard)){
                    var row=new LinkedHashMap<String,Object>(CreatureAgents.status(guard));var path=guard.getNavigation().getPath();
                    row.put("position",List.of(guard.getX(),guard.getY(),guard.getZ()));
                    row.put("navigation",Map.of("done",path==null||path.isDone(),"end",path==null||path.getEndNode()==null?"":path.getEndNode().asBlockPos().toShortString()));
                    campGuards.add(row);
                }
                for(var id:ids){
                    var v=(Villager)level.getEntity(id);check(v!=null&&v.isAlive(),"existing island resident remains alive: "+id);
                    current.add(actor(level,v,false));var s=Villagers.existingSession(id);
                    if(AdventurePreparation.nominated(v)){stone=WoodCrafting.stoneCount(v.getInventory())>=3;stonePickaxe=AdventurePreparation.stonePickaxe(v);ironReady=v.getInventory().countItem(Items.IRON_INGOT)>=3;ironPickaxe=AdventurePreparation.ironPickaxe(v);ironArmor=Boolean.TRUE.equals(armorStatus(v,s).get("ready"));}
                    if(rehearse)check(v.getHealth()>=8,"copy health stop: "+s.mind.name());
                    boolean coldReturn=returningHomes.containsKey(id)&&s.boatLandings>=1
                        &&v.position().distanceToSqr(Vec3.atBottomCenterOf(returningHomes.get(id)))<4;
                    if((s.boatLandings>=2||coldReturn)&&s.boatTrip==null&&!v.isPassenger()&&BoatRoutes.dry(level,v.blockPosition())){
                        returned=true;
                        if(FoodInventory.anySlot(v.getInventory(),AdventureActions.canHarvest(v))>=0)returnedWithFood=true;
                    }
                }
                if(rehearse){
                    elapsed++;
                    if(elapsed%40==0){
                        Files.writeString(output("native-boat-progress.json"),JSON.toJson(Map.of("elapsed",elapsed,"actors",current,"residentHealth",residentHealth,"campGuards",campGuards)));
                        for(var actor:current)if(((Map<?,?>)actor.get("adventure")).get("nominated").equals(true))
                            Files.writeString(output("native-adventure-trace.jsonl"),new Gson().toJson(Map.of("elapsed",elapsed,"actor",actor))+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
                    }
                    boolean reachedResource=untilIronArmor?ironArmor:untilIronPickaxe?ironPickaxe:untilIronIngots?ironReady:stonePickaxe||stone&&!untilStonePickaxe;
                    boolean completedReturn=returned&&window<=2400&&!untilStonePickaxe&&!untilIronIngots&&!untilIronPickaxe&&!untilIronArmor;
                    if(!reachedResource&&!completedReturn&&elapsed<window&&!Files.exists(output("stop-observation")))return;
                }
                var report=new LinkedHashMap<String,Object>(Map.of("status","passed","stage",rehearse?(untilIronArmor?(ironArmor?"iron_armor_equipped":"bounded_iron_armor_observation"):untilIronPickaxe?(ironPickaxe?"iron_pickaxe_prepared":"bounded_iron_pickaxe_observation"):untilIronIngots?(ironReady?"iron_ingots_prepared":"bounded_iron_observation"):stonePickaxe?"stone_pickaxe_prepared":stone?"stone_gathered":returned?(returnedWithFood?"returned_with_food":"returned_without_food"):"bounded_observation"):"frozen_routes", "initialGameTime",initialTime,"gameTime",level.getGameTime(),"elapsed",elapsed,"initial",initial,"actors",current,"copyBorderSize",level.getWorldBorder().getSize(),
                    "scope","Actual saved island residents in a copy; no actor/resources/inventory/health setters. Primary unchanged. Bounded observation alone is not food-voyage proof."));
                report.put("residentHealth",residentHealth);report.put("campGuards",campGuards);Files.writeString(output("native-boat-access.json"),JSON.toJson(report));
                finished=true;server.tickRateManager().setFrozen(false);server.halt(false);
            }catch(Throwable error){error.printStackTrace();finished=true;try{Files.writeString(output("native-boat-access.json"),JSON.toJson(Map.of("status","failed","failure",error.toString())));}catch(Exception ignored){}server.tickRateManager().setFrozen(false);server.halt(false);}
        });
    }
}
