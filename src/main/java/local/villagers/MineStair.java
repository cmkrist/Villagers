package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

/** A paid three-high descending passage. Each step retains its real floor and the route back. */
final class MineStair {
    private static final String TAG="villagers:mine_stair:";
    private static final String FLOOR="villagers:mine_floor:";
    private static final String ENTRY="villagers:mine_entry:";
    private static final String EXHAUSTED="villagers:mine_exhausted";
    private static final String FRONTIER="villagers:mine_frontier:";
    private static final String LATERAL="villagers:mine_lateral:";
    private static final String RETIRED="villagers:mine_exhausted_entry:";
    static final int DEPTH=24;
    static final int FRONTIER_RETRY_TICKS=10;
    static final class State {
        BlockPos root,entry,face,frontier;Direction direction;int depth,part;boolean loaded,level,reselect;
        String faceBlock="";
        final Set<BlockPos> floors=new HashSet<>();
        final Set<BlockPos> entries=new LinkedHashSet<>();long revisitAt;
        final Set<BlockPos> exhaustedEntries=new HashSet<>();
        final Map<BlockPos,Set<String>> rejected=new LinkedHashMap<>();
        final Map<BlockPos,Long> frontierCooldown=new HashMap<>();
        int backtracks;long frontierUntil;
        Map<String,Object> route=Map.of();
        String frontierActivity="not checking junctions";int frontierCandidates,frontierQueries;
        int turns,failedScans,foundations,withoutDepth;boolean exhausted;
        Map<String,Object> foundationReceipt=Map.of();
        final MineFoundation.State foundation=new MineFoundation.State();
        final MineWaterSeal.State waterSeals=new MineWaterSeal.State();
        long pathAt,scanAt;String activity="no prospecting passage";
    }
    static Map<String,Object> status(Session s){
        var w=s.mineStair;var result=new LinkedHashMap<String,Object>(Map.of("activity",w.activity,"root",w.root==null?"":w.root.toShortString(),"depth",w.depth,"part",w.part,"face",w.face==null?"":w.face.toShortString(),"faceBlock",w.faceBlock,"turns",w.turns,"retainedFloors",w.floors.size(),"mode",w.level?"level":"descending","reselect",w.reselect));
        result.put("retainedEntries",w.entries.size());result.put("exhaustedEntries",w.exhaustedEntries.size());result.put("stepsWithoutDepth",w.withoutDepth);result.put("waterSeals",w.waterSeals.status());result.put("foundations",w.foundations);result.put("lastFoundation",w.foundationReceipt);
        result.put("exhausted",w.exhausted);result.put("failedScans",w.failedScans);result.put("entry",w.entry==null?"":w.entry.toShortString());
        result.put("frontier",w.frontier==null?"":w.frontier.toShortString());result.put("backtracks",w.backtracks);result.put("lastRoute",w.route);
        result.put("frontierSearch",Map.of("activity",w.frontierActivity,"candidates",w.frontierCandidates,"queries",w.frontierQueries));return result;
    }
    static List<BlockPos> step(BlockPos root,Direction direction,int depth){
        return step(root,direction,depth,false);
    }
    static List<BlockPos> step(BlockPos root,Direction direction,int depth,boolean level){
        if(depth<1||depth>DEPTH||direction.getAxis().isVertical())throw new IllegalArgumentException("Invalid mine step");
        var feet=root.relative(direction,depth).below(level?0:depth);return List.of(feet.above(2),feet.above(),feet);
    }
    private static void load(Villager v,State w){
        if(w.loaded)return;w.loaded=true;w.exhausted=v.entityTags().contains(EXHAUSTED);
        for(var tag:v.entityTags())if(tag.startsWith(ENTRY))try{w.entries.add(BlockPos.of(Long.parseLong(tag.substring(ENTRY.length()))));}catch(NumberFormatException ignored){}
        for(var tag:v.entityTags())if(tag.startsWith(FLOOR))try{w.floors.add(BlockPos.of(Long.parseLong(tag.substring(FLOOR.length()))));}catch(NumberFormatException ignored){}
        for(var tag:v.entityTags())if(tag.startsWith(FRONTIER))try{w.frontier=BlockPos.of(Long.parseLong(tag.substring(FRONTIER.length())));}catch(NumberFormatException ignored){}
        for(var tag:v.entityTags())if(tag.startsWith(LATERAL))try{w.withoutDepth=Math.max(0,Integer.parseInt(tag.substring(LATERAL.length())));}catch(NumberFormatException ignored){}
        for(var tag:v.entityTags())if(tag.startsWith(RETIRED))try{w.exhaustedEntries.add(BlockPos.of(Long.parseLong(tag.substring(RETIRED.length()))));}catch(NumberFormatException ignored){}
        for(var tag:v.entityTags())restorePassage(w,tag);
        if(w.entry!=null)w.entries.add(w.entry);
    }
    static String passageTag(State w){
        if(w.root==null)return null;
        if(w.entry==null||w.direction==null&&(!w.reselect||w.depth!=0||w.part!=0))throw new IllegalStateException("Invalid pending mining passage");
        return TAG+w.root.asLong()+":"+w.entry.asLong()+":"+(w.direction==null?"NONE":w.direction.name())+":"+w.depth+":"+w.part+":"+(w.level?"level":"down")+":"+(w.reselect?"select":"active");
    }
    static boolean restorePassage(State w,String tag){
        if(!tag.startsWith(TAG))return false;
        try{
            var a=tag.substring(TAG.length()).split(":");if(a.length<5||a.length>7)return false;
            if(a.length>=6&&!a[5].equals("level")&&!a[5].equals("down"))return false;
            if(a.length==7&&!a[6].equals("select")&&!a[6].equals("active"))return false;
            boolean select=a.length==7&&a[6].equals("select");
            var direction=a[2].equals("NONE")?null:Direction.valueOf(a[2]);int depth=Integer.parseInt(a[3]),part=Integer.parseInt(a[4]);
            if(depth<0||depth>DEPTH||part<0||part>3||direction==null&&(!select||depth!=0||part!=0)
                ||direction!=null&&direction.getAxis().isVertical())return false;
            var root=BlockPos.of(Long.parseLong(a[0]));var entry=BlockPos.of(Long.parseLong(a[1]));
            w.root=root;w.entry=entry;w.direction=direction;w.depth=depth;w.part=part;w.level=a.length>=6&&a[5].equals("level");w.reselect=select;
            w.floors.add(root.below());
            for(int i=1;i<=depth;i++)w.floors.add(root.relative(direction,i).below(w.level?1:i+1));
            return true;
        }catch(IllegalArgumentException invalid){return false;}
    }
    static boolean exhaustedNear(Villager v,BlockPos root){
        for(var tag:v.entityTags())if(tag.startsWith(RETIRED))try{
            if(BlockPos.of(Long.parseLong(tag.substring(RETIRED.length()))).distSqr(root)<16)return true;
        }catch(NumberFormatException ignored){}
        return false;
    }
    static void rememberEntry(Villager v,Session s,BlockPos entry){
        load(v,s.mineStair);s.mineStair.entries.add(entry.immutable());v.addTag(ENTRY+entry.asLong());
    }
    private static void save(Villager v,State w){
        v.entityTags().stream().filter(t->t.startsWith(TAG)).toList().forEach(v::removeTag);
        var passage=passageTag(w);if(passage!=null)v.addTag(passage);
        if(w.entry!=null)w.entries.add(w.entry);
        for(var entry:w.entries)v.addTag(ENTRY+entry.asLong());
        for(var entry:w.exhaustedEntries)v.addTag(RETIRED+entry.asLong());
        for(var floor:w.floors)v.addTag(FLOOR+floor.asLong());
        if(w.exhausted)v.addTag(EXHAUSTED);else v.removeTag(EXHAUSTED);
        v.entityTags().stream().filter(t->t.startsWith(FRONTIER)).toList().forEach(v::removeTag);
        if(w.frontier!=null)v.addTag(FRONTIER+w.frontier.asLong());
        v.entityTags().stream().filter(t->t.startsWith(LATERAL)).toList().forEach(v::removeTag);
        v.addTag(LATERAL+w.withoutDepth);
    }
    static boolean retainsFloor(State w,Collection<BlockPos> cells){return cells.stream().anyMatch(w.floors::contains);}
    static boolean newWorkFloor(State w,List<BlockPos> cells){
        // Previously walked passage floors remain available to native return
        // paths, but are not fresh excavation fronts. Selecting them again
        // lets an all-air corridor cycle indefinitely through the same turns.
        return !w.floors.contains(cells.getLast().below())&&!retainsFloor(w,cells);
    }
    static boolean clearCell(net.minecraft.world.level.block.state.BlockState state){
        if(!state.getFluidState().isEmpty())return false;
        return state.isAir()
            ||state.getBlock() instanceof net.minecraft.world.level.block.FlowerBlock&&!state.is(net.minecraft.world.level.block.Blocks.WITHER_ROSE)
            ||state.getBlock() instanceof net.minecraft.world.level.block.TallGrassBlock
            ||state.is(net.minecraft.world.level.block.Blocks.TALL_GRASS)||state.is(net.minecraft.world.level.block.Blocks.LARGE_FERN);
    }
    private static boolean observedFace(ServerLevel level,Villager v,State w,BlockPos origin,Direction direction,boolean flat,Vec3 eye){
        var cells=step(origin,direction,1,flat);var foot=cells.getLast();
        if(w.rejected.getOrDefault(origin,Set.of()).contains(direction.name()+":"+flat)||!newWorkFloor(w,cells)
            ||!level.isLoaded(foot.below())||!level.getBlockState(foot.below()).isFaceSturdy(level,foot.below(),Direction.UP)
            ||cells.stream().anyMatch(p->AdventureQuarry.accessSupport(v,p)))return false;
        var first=cells.stream().filter(p->!clearCell(level.getBlockState(p))).findFirst().orElse(null);
        if(first==null||AdventureMining.exposedAt(level,v,first,true,origin.getY())&&BlockContact.point(level,v,eye,first,3)!=null)return true;
        var s=Villagers.existingSession(v.getUUID());
        return s!=null&&MineWaterSeal.preview(level,v,s,origin,direction,flat)!=null;
    }
    /** Inspect only junctions whose support was recorded during this actor's
     * real travel. No ore search, unseen route, or new excavation is implied. */
    private enum FrontierSearch { FOUND, WAIT, NONE }
    static int stepsWithoutDepth(int previous,int nextFloorY,int deepestFloorY){
        return nextFloorY<deepestFloorY?0:Math.min(DEPTH,previous+1);
    }
    static Comparator<BlockPos> frontierOrder(Set<BlockPos> descending,BlockPos actor){
        return Comparator.<BlockPos>comparingInt(p->descending.contains(p)?0:1)
            .thenComparingInt(BlockPos::getY).thenComparingDouble(p->p.distSqr(actor));
    }
    private static FrontierSearch chooseFrontier(ServerLevel level,Villager v,Session s,long now){
        return chooseFrontier(level,v,s,now,false);
    }
    private static FrontierSearch chooseFrontier(ServerLevel level,Villager v,Session s,long now,boolean deeperOnly){
        var w=s.mineStair;w.frontierCooldown.entrySet().removeIf(e->e.getValue()<=now);
        int deepest=w.floors.stream().mapToInt(BlockPos::getY).min().orElse(v.getBlockY()-1);
        var candidates=new ArrayList<BlockPos>();var descending=new HashSet<BlockPos>();
        for(var floor:w.floors){
            var root=floor.above();
            if(root.distSqr(v.blockPosition())<=4||root.distSqr(v.blockPosition())>32*32||w.frontierCooldown.containsKey(root)
                ||!level.isLoaded(root.above())||!level.getBlockState(root).isAir()||!level.getBlockState(root.above()).isAir()
                ||!level.getBlockState(floor).isFaceSturdy(level,floor,Direction.UP))continue;
            boolean fresh=false;var eye=ForestryWork.standEye(v,root);
            for(var d:Direction.Plane.HORIZONTAL)for(boolean flat:List.of(false,true))
                if(observedFace(level,v,w,root,d,flat,eye)){fresh=true;if(!flat)descending.add(root);}
            if(fresh&&(!deeperOnly||descending.contains(root)&&floor.getY()<=deepest))candidates.add(root);
        }
        // Revisit a known descent before extending nearby level corridors.
        // Only recorded, locally inspected junctions enter this ordering;
        // their full native return route is still checked below.
        candidates.sort(frontierOrder(descending,v.blockPosition()));
        w.frontierCandidates=candidates.size();
        if(candidates.isEmpty()){w.frontierActivity="no remaining eligible observed junctions";return FrontierSearch.NONE;}
        if(!WorkNavigation.admit(level,now,v)){w.frontierActivity="waiting for fair junction-path admission";return FrontierSearch.WAIT;}
        for(var target:candidates.stream().limit(6).toList()){
            w.frontierQueries++;var path=route(level,v,s,target);
            if(!WorkNavigation.staysHome(level,v,s,path)||!BoatTravel.dryPath(level,path)){
                w.frontierCooldown.put(target,now+6000);continue;
            }
            if(!v.getNavigation().moveTo(path,.6)){w.frontierCooldown.put(target,now+6000);continue;}
            w.frontierActivity="following a complete native path to a recorded junction";
            w.frontier=target;w.frontierUntil=now+1200;w.pathAt=now+40;w.exhausted=false;w.backtracks++;save(v,w);return FrontierSearch.FOUND;
        }
        w.frontierActivity=candidates.size()>6?"checking remaining junction routes in another bounded batch":"no safe complete junction route";
        return candidates.size()>6?FrontierSearch.WAIT:FrontierSearch.NONE;
    }
    /** Revisit actual recorded work before paying for another quarry. */
    static String revisitRecorded(ServerLevel level,Villager v,Session s,long now){
        var w=s.mineStair;load(v,w);
        if(w.root!=null||s.quarry.root!=null||w.entries.isEmpty()||!WoodBootstrap.safe(level,v,s))return null;
        if(w.entry==null||w.exhaustedEntries.contains(w.entry)||AdventureQuarry.failedEntry(v,w.entry,now))
            w.entry=w.entries.stream().filter(p->!w.exhaustedEntries.contains(p)&&!AdventureQuarry.failedEntry(v,p,now))
                .min(Comparator.comparingDouble(p->p.distSqr(v.blockPosition()))).orElse(null);
        if(w.entry==null)return null;
        if(w.frontier!=null)return followFrontier(level,v,s,now);
        if(!v.onGround())return null;
        if(now<w.revisitAt){w.entry=null;return null;}
        var result=chooseFrontier(level,v,s,now);
        if(result==FrontierSearch.FOUND)return "returning along a known route to unfinished recorded mine work";
        if(result==FrontierSearch.WAIT){w.revisitAt=now+FRONTIER_RETRY_TICKS;return "waiting to inspect recorded mine junctions before another quarry";}
        w.entry=null;w.revisitAt=now+400;return null;
    }
    private static String followFrontier(ServerLevel level,Villager v,Session s,long now){
        var w=s.mineStair;if(w.frontier==null)return null;
        if(w.frontierUntil==0)w.frontierUntil=now+1200;
        if(v.onGround()&&Math.abs(v.getY()-w.frontier.getY())<.1
            &&v.position().distanceToSqr(Vec3.atBottomCenterOf(w.frontier))<=1){
            w.frontierCooldown.put(w.frontier,now+1200);w.frontier=null;w.root=v.blockPosition();
            w.depth=w.part=w.failedScans=0;w.reselect=true;w.scanAt=0;save(v,w);
            return "reached a recorded mining junction; observing its unexplored faces";
        }
        if(now>=w.frontierUntil){w.frontierCooldown.put(w.frontier,now+2400);w.frontier=null;save(v,w);return null;}
        if(now>=w.pathAt&&WorkNavigation.admit(level,now,v)){
            w.pathAt=now+40;var target=w.frontier;
            var path=NativePathLimits.search(v,1,()->v.getNavigation().createPath(target,0,128));
            if(WorkNavigation.staysHome(level,v,s,path)&&BoatTravel.dryPath(level,path))v.getNavigation().moveTo(path,.6);
        }
        return "backtracking along actual mine floors to an unexplored junction";
    }
    static void resume(Villager v,Session s){load(v,s.mineStair);}
    static boolean exitFloor(Villager v,BlockPos p){
        var s=Villagers.existingSession(v.getUUID());if(s==null)return false;
        load(v,s.mineStair);return s.mineStair.floors.contains(p);
    }
    private static String turn(Villager v,State w,String reason){
        if(!v.onGround())return w.activity=reason;
        w.rejected.computeIfAbsent(v.blockPosition(),p->new HashSet<>()).add(w.direction.name()+":"+w.level);
        while(w.rejected.size()>128)w.rejected.remove(w.rejected.keySet().iterator().next());
        w.root=v.blockPosition();w.depth=w.part=0;w.reselect=true;w.turns++;save(v,w);
        return w.activity=reason+"; observing another direction from the supported floor";
    }
    static String returnToEntry(ServerLevel level,Villager v,Session s,long now){
        return returnToEntry(level,v,s,now,false);
    }
    static boolean atEntry(Vec3 position,BlockPos entry){
        return position.y>=entry.getY()-.1&&position.distanceToSqr(Vec3.atBottomCenterOf(entry))<=4;
    }
    /** Travel through the remembered entrance before requesting a deep working
     * floor from the far side of a village. Each leg remains an actual path. */
    static BlockPos approachTarget(Vec3 position,BlockPos entry,BlockPos floor){
        return entry!=null&&position.y>=entry.getY()-.1&&!atEntry(position,entry)?entry:floor;
    }
    /** Intermediate feet positions on the connected supports actually walked
     * by this worker. Native pathfinding still validates every proposed leg. */
    static BlockPos recordedWaypoint(Set<BlockPos> floors,Vec3 position,BlockPos target){
        if(floors.isEmpty()||target==null)return null;
        var start=floors.stream().min(Comparator.comparingDouble(p->position.distanceToSqr(Vec3.atBottomCenterOf(p.above())))).orElseThrow();
        if(position.distanceToSqr(Vec3.atBottomCenterOf(start.above()))>4)return null;
        var goal=floors.stream().min(Comparator.comparingDouble(p->p.above().distSqr(target))).orElseThrow();
        if(start.equals(goal))return null;
        var previous=new HashMap<BlockPos,BlockPos>();var queue=new ArrayDeque<BlockPos>();
        previous.put(start,start);queue.add(start);
        while(!queue.isEmpty()&&!previous.containsKey(goal)){
            var floor=queue.remove();
            for(var direction:Direction.Plane.HORIZONTAL)for(int dy=-1;dy<=1;dy++){
                var next=floor.relative(direction).offset(0,dy,0);
                if(floors.contains(next)&&!previous.containsKey(next)){previous.put(next,floor);queue.add(next);}
            }
        }
        if(!previous.containsKey(goal))return null;
        var reverse=new ArrayList<BlockPos>();
        for(var at=goal;!at.equals(start);at=previous.get(at))reverse.add(at);
        Collections.reverse(reverse);return reverse.get(Math.min(23,reverse.size()-1)).above();
    }
    private static net.minecraft.world.level.pathfinder.Path route(ServerLevel level,Villager v,Session s,BlockPos target){
        var path=NativePathLimits.search(v,1,()->v.getNavigation().createPath(target,0,128));
        if(!WorkNavigation.staysHome(level,v,s,path)||!BoatTravel.dryPath(level,path))try{
            v.getNavigation().setMaxVisitedNodesMultiplier(32);
            path=NativePathLimits.search(v,1,()->v.getNavigation().createPath(target,0,128));
        }finally{v.getNavigation().resetMaxVisitedNodesMultiplier();}
        if(!WorkNavigation.staysHome(level,v,s,path)||!BoatTravel.dryPath(level,path)){
            var waypoint=recordedWaypoint(s.mineStair.floors,v.position(),target);
            if(waypoint!=null&&!waypoint.equals(target)){
                path=NativePathLimits.search(v,1,()->v.getNavigation().createPath(waypoint,0,64));
                if(!WorkNavigation.staysHome(level,v,s,path)||!BoatTravel.dryPath(level,path))try{
                    v.getNavigation().setMaxVisitedNodesMultiplier(32);
                    path=NativePathLimits.search(v,1,()->v.getNavigation().createPath(waypoint,0,64));
                }finally{v.getNavigation().resetMaxVisitedNodesMultiplier();}
            }
        }
        boolean safe=WorkNavigation.staysHome(level,v,s,path)&&BoatTravel.dryPath(level,path);
        s.mineStair.route=Map.of("gameTime",level.getGameTime(),"actor",v.position().toString(),"target",target.toShortString(),
            "safe",safe,"nodes",path==null?0:path.getNodeCount(),"end",path==null?"":path.getEndNode().asBlockPos().toShortString());
        return safe?path:null;
    }
    static String returnToEntry(ServerLevel level,Villager v,Session s,long now,boolean requireArrival){
        var w=s.mineStair;load(v,w);
        var entry=w.entry==null?AdventureQuarry.rememberedEntry(v,s,now):w.entry;
        // Quarry stone may be sufficient before a prospecting shaft exists.
        // Return along that remembered entrance too; surface needs must still
        // yield to the ordinary food/rest controller rather than revisit a mine.
        if(entry==null||!requireArrival&&v.getY()>=entry.getY()-.1||v.isSleeping()||v.isPassenger()||v.isInWater())return null;
        if(requireArrival?atEntry(v.position(),entry):v.position().distanceToSqr(Vec3.atBottomCenterOf(entry))<=4)return null;
        var current=v.getNavigation().getPath();
        if(current!=null&&!current.isDone()&&current.getTarget().equals(entry))return "following the retained mine exit";
        if(now>=w.pathAt&&WorkNavigation.admit(level,now,v)){
            w.pathAt=now+40;
            var path=route(level,v,s,entry);
            if(path!=null&&path.getNodeCount()<=128&&WorkNavigation.staysHome(level,v,s,path)&&BoatTravel.dryPath(level,path)){
                v.getNavigation().moveTo(path,.6);return "carrying mined supplies back along the actual stairs";
            }
            return null;
        }
        return "waiting for a safe route up the mine stairs";
    }
    static String retreatForNeeds(ServerLevel level,Villager v,Session s,long now){
        if(VillageSurvival.scheduledRest(level,v)||s.mind.fatigue()>=.55||s.mind.foodLevel()<14||v.getHealth()<v.getMaxHealth()*.9)
            return returnToEntry(level,v,s,now);
        return null;
    }
    static String leaveExhausted(ServerLevel level,Villager v,Session s,long now){
        var w=s.mineStair;load(v,w);if(!w.exhausted)return null;
        var exit=returnToEntry(level,v,s,now,true);
        if(w.entry!=null&&!atEntry(v.position(),w.entry))return exit==null?"waiting for a safe exit from the exhausted mine":exit;
        if(!v.onGround())return "finishing the actual ascent out of the exhausted mine";
        if(w.entry!=null)w.exhaustedEntries.add(w.entry);
        AdventureQuarry.retireForProspecting(v,s,now);
        w.root=w.entry=w.face=w.frontier=null;w.depth=w.part=w.failedScans=w.withoutDepth=0;w.reselect=w.exhausted=false;w.scanAt=0;save(v,w);
        return "left an exhausted mine; seeking another observed dry prospect";
    }
    static String work(ServerLevel level,Villager v,Session s,long now){
        var w=s.mineStair;load(v,w);
        if(!NativeRegionsScenario.nativeRegions(level)||!Villagers.config().nativeAdventureQuarry()
            ||!WoodBootstrap.safe(level,v,s)||!AdventurePreparation.stonePickaxe(v)
            ||!AdventureActions.PERMISSIONS.allows(Capability.MINE_ADVENTURE_PASSAGE))return null;
        var sealingWork=MineWaterSeal.resume(level,v,s,now);if(sealingWork!=null)return w.activity=sealingWork;
        var foundationWork=MineFoundation.resume(level,v,s,now);if(foundationWork!=null)return w.activity=foundationWork;
        var backtrack=followFrontier(level,v,s,now);if(backtrack!=null)return w.activity=backtrack;
        if(w.withoutDepth>=DEPTH){
            if(!v.onGround())return w.activity="finishing the native step before reviewing the prospecting limit";
            boolean deeperHere=false;
            for(var direction:Direction.Plane.HORIZONTAL)
                if(observedFace(level,v,w,v.blockPosition(),direction,false,v.getEyePosition()))deeperHere=true;
            if(deeperHere&&w.root!=null&&w.level){
                w.root=v.blockPosition();w.depth=w.part=0;w.reselect=true;w.scanAt=0;
            }
            if(!deeperHere){
                var deeper=chooseFrontier(level,v,s,now,true);
                if(deeper==FrontierSearch.FOUND)return w.activity="returning to an observed deeper descent after bounded lateral prospecting";
                if(deeper==FrontierSearch.WAIT)return w.activity="waiting to check a deeper recorded prospecting route";
                w.exhausted=true;save(v,w);
                return w.activity="retained the dry exit after bounded prospecting found no deeper descent";
            }
        }
        if(w.root!=null){
            var current=w.direction==null?w.root:w.root.relative(w.direction,w.depth).below(w.level?0:w.depth);
            var next=w.direction!=null&&w.depth<DEPTH?step(w.root,w.direction,w.depth+1,w.level).getLast():current;
            if(v.position().distanceToSqr(Vec3.atBottomCenterOf(current))>1
                &&(w.part<3||v.position().distanceToSqr(Vec3.atBottomCenterOf(next))>1)){
                if(now>=w.pathAt&&WorkNavigation.admit(level,now,v)){
                    w.pathAt=now+40;var target=approachTarget(v.position(),w.entry,current);var path=route(level,v,s,target);
                    if(WorkNavigation.staysHome(level,v,s,path)&&BoatTravel.dryPath(level,path))v.getNavigation().moveTo(path,.6);
                }
                return w.activity="returning to the recorded supported prospecting floor";
            }
            if(w.reselect){
                if(!v.onGround())return w.activity="waiting to stand on the recorded floor before choosing another face";
                if(now<w.scanAt)return w.activity="rechecking the observed blocked prospecting faces after a short pause";
                w.root=null;w.reselect=false;
            }
        }
        if(w.root==null){
            if(!v.onGround())return null;var origin=v.blockPosition();
            if(w.floors.size()>=512)return w.activity="retained the bounded mine network and its exit floors";
            var directions=new ArrayList<Direction>();if(s.quarry.direction!=null)directions.add(s.quarry.direction);for(var d:Direction.Plane.HORIZONTAL)if(!directions.contains(d))directions.add(d);
            search:for(boolean flat:List.of(false,true))for(var d:directions){
                if(!observedFace(level,v,w,origin,d,flat,v.getEyePosition()))continue;
                w.root=origin;if(w.entry==null)w.entry=s.quarry.root==null?origin:s.quarry.root;
                w.floors.add(origin.below());w.direction=d;w.level=flat;w.depth=w.part=w.failedScans=0;save(v,w);break search;
            }
            if(w.root==null){
                var seal=MineWaterSeal.work(level,v,s,origin,directions,now);if(seal!=null)return w.activity=seal;
                var foundation=MineFoundation.work(level,v,s,origin,directions,now);
                if(foundation!=null)return w.activity=foundation;
                if(w.entry!=null||!w.entries.isEmpty()){
                    if(w.entry==null)w.entry=w.entries.stream().min(Comparator.comparingDouble(p->p.distSqr(origin))).orElseThrow();
                    w.root=origin;w.reselect=true;w.scanAt=now+100;w.failedScans++;
                    if(w.failedScans>=3){
                        var frontier=chooseFrontier(level,v,s,now);
                        if(frontier==FrontierSearch.FOUND)return w.activity="returning to an observed junction before abandoning the mine";
                        if(frontier==FrontierSearch.WAIT){
                            // Keep the fair queue reservation alive. The normal
                            // 100tick perception pause exceeds its 80tick lease.
                            w.scanAt=now+FRONTIER_RETRY_TICKS;save(v,w);return w.activity="waiting to check remaining recorded mining junctions";
                        }
                    }
                    w.exhausted=w.failedScans>=3;save(v,w);
                }
                return null;
            }
        }
        if(w.depth>=DEPTH){w.root=v.blockPosition();w.floors.add(w.root.below());w.depth=w.part=0;save(v,w);return w.activity="continued prospecting from the observed passage floor";}
        var cells=step(w.root,w.direction,w.depth+1,w.level);var feet=cells.get(2);
        if(!level.isLoaded(feet.below())||!FloorCentering.safeSupport(level.getBlockState(feet.below()))
            ||!level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),Direction.UP))return turn(v,w,"retained the mine floor above an unsafe opening");
        if(!newWorkFloor(w,cells))return turn(v,w,"left an explored passage available for return rather than excavating it again");
        while(w.part<3&&level.isLoaded(cells.get(w.part))&&clearCell(level.getBlockState(cells.get(w.part))))w.part++;
        if(w.part<3){
            var p=cells.get(w.part);w.face=p;w.faceBlock=level.getBlockState(p).toString();
            if(AdventureQuarry.accessSupport(v,p))return turn(v,w,"left a retained quarry support intact");
            if(!AdventureMining.exposed(level,v,p,true)){
                var origin=w.root.relative(w.direction,w.depth).below(w.level?0:w.depth);
                var seal=MineWaterSeal.work(level,v,s,origin,List.of(w.direction),now);if(seal!=null)return w.activity=seal;
                return turn(v,w,"left a hazardous or structural mining face intact");
            }
            var activity=AdventureMining.mineAt(level,v,s,p,now,true);save(v,w);return w.activity=activity==null?"examining the observed mining face":activity;
        }
        if(v.onGround()&&Math.abs(v.getY()-feet.getY())<.1&&v.position().distanceToSqr(Vec3.atBottomCenterOf(feet))<=1){
            int deepest=w.floors.stream().mapToInt(BlockPos::getY).min().orElse(feet.getY());
            w.withoutDepth=stepsWithoutDepth(w.withoutDepth,feet.getY()-1,deepest);
            w.depth++;w.part=0;w.floors.add(feet.below());save(v,w);return w.activity="observing newly exposed rock from the next supported step";
        }
        if(now>=w.pathAt&&WorkNavigation.admit(level,now,v)){
            w.pathAt=now+40;var path=NativePathLimits.search(v,1,()->WorkNavigation.pathTo(level,v,s,feet,0));
            if(path!=null&&BoatTravel.dryPath(level,path))v.getNavigation().moveTo(path,.6);
        }
        return w.activity="walking down one retained mine step";
    }
    private MineStair(){}
}
