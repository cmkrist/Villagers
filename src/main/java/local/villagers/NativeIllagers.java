package local.villagers;

import com.google.gson.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import local.villagers.war.WarMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.commands.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;

/** Bounded existing-body founders in real hostile structures; never replaces missing/dead founders. */
final class NativeIllagers {
    private static final String TAG="villagers:native_illager_camp:";
    private static final Set<String> STRUCTURES=Set.of("minecraft:pillager_outpost","minecraft:woodland_mansion","nova_structures:illager_barracks");
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private record Camp(String id,String structure,int x,int y,int z,int radius,List<String> founders) {
        WarMap.Settlement settlement(){return new WarMap.Settlement("Native "+structure+" "+id,WarMap.region(x,z),WarMap.Faction.ILLAGERS,x,z);}
    }
    private static final Map<String,Camp> CAMPS=new LinkedHashMap<>();
    private static MinecraftServer owner;
    private static Path file(MinecraftServer server){return server.getWorldPath(LevelResource.ROOT).resolve("native-illager-settlements.json");}
    static void initialize(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            owner=server;CAMPS.clear();if(!NativeRegionsScenario.nativeRegions(server.overworld())||!Files.exists(file(server)))return;
            try{
                var root=JsonParser.parseString(Files.readString(file(server))).getAsJsonObject();
                if(root.get("schema").getAsInt()!=1)throw new IOException("unrecognized native illager schema");
                var loaded=new LinkedHashMap<String,Camp>();
                for(var element:root.getAsJsonArray("camps")){
                    var c=JSON.fromJson(element,Camp.class);
                    if(!STRUCTURES.contains(c.structure)||c.founders.size()<2||c.founders.size()>8||!c.id.equals(Long.toString(new BlockPos(c.x,c.y,c.z).asLong())))throw new IOException("invalid camp");
                    for(var id:c.founders)UUID.fromString(id);
                    if(loaded.put(c.id,c)!=null)throw new IOException("duplicate camp");
                }
                CAMPS.putAll(loaded);
            }catch(Exception error){throw new IllegalStateException("Native illager record preserved; cannot load it",error);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server==owner&&server.getTickCount()%40==0)restore(server.overworld());});
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{if(owner==server){CAMPS.clear();owner=null;}});
    }
    static WarMap.Settlement home(Mob mob){
        if(!(mob.level() instanceof ServerLevel level)||level.getServer()!=owner||!NativeRegionsScenario.nativeRegions(level))return null;
        for(var tag:mob.entityTags())if(tag.startsWith(TAG)){var c=CAMPS.get(tag.substring(TAG.length()));if(c!=null)return c.settlement();}
        return null;
    }
    static boolean member(Mob mob){return home(mob)!=null;}
    record Area(int x,int z,int radius){}
    static List<Area> areas(){return CAMPS.values().stream().map(c->new Area(c.x,c.z,c.radius>0?c.radius:48)).toList();}
    private static final String TABLE="villagers:camp_table:";
    private static final String HAY="villagers:camp_hay:";
    static void rememberHay(ServerLevel level,Mob mob,BlockPos p){
        if(member(mob)&&insideCamp(level,mob,p)&&mob.entityTags().stream().filter(t->t.startsWith(HAY)).count()<32)mob.addTag(HAY+p.asLong());
    }
    static void forgetHay(Mob mob,BlockPos p){mob.removeTag(HAY+p.asLong());}
    static List<BlockPos> knownHay(ServerLevel level,Mob mob){
        var result=new ArrayList<BlockPos>();
        for(var tag:mob.entityTags().stream().filter(t->t.startsWith(HAY)).toList())try{
            var p=BlockPos.of(Long.parseLong(tag.substring(HAY.length())));if(level.isLoaded(p)&&insideCamp(level,mob,p))result.add(p);
        }catch(NumberFormatException ignored){mob.removeTag(tag);}
        return result;
    }
    static void rememberTable(ServerLevel level,Mob mob,BlockPos p){
        if(!member(mob)||!insideCamp(level,mob,p)||!level.getBlockState(p).is(Blocks.CRAFTING_TABLE))return;
        if(mob.entityTags().stream().filter(t->t.startsWith(TABLE)).count()<16)mob.addTag(TABLE+p.asLong());
    }
    static List<BlockPos> knownTables(ServerLevel level,Mob mob){
        var result=new ArrayList<BlockPos>();
        for(var tag:mob.entityTags().stream().filter(t->t.startsWith(TABLE)).toList())try{
            var p=BlockPos.of(Long.parseLong(tag.substring(TABLE.length())));
            if(level.isLoaded(p)){
                if(level.getBlockState(p).is(Blocks.CRAFTING_TABLE)&&insideCamp(level,mob,p))result.add(p);
                else mob.removeTag(tag);
            }
        }catch(NumberFormatException ignored){mob.removeTag(tag);}
        return result;
    }
    static Set<UUID> founders(){var out=new HashSet<UUID>();for(var camp:CAMPS.values())for(var id:camp.founders)out.add(UUID.fromString(id));return Set.copyOf(out);}
    static boolean insideCamp(ServerLevel level,Mob mob,BlockPos p){
        if(!level.isLoaded(p))return false;
        for(var tag:mob.entityTags())if(tag.startsWith(TAG)){
            var camp=CAMPS.get(tag.substring(TAG.length()));if(camp==null)continue;
            var key=net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE,net.minecraft.resources.Identifier.parse(camp.structure));
            var type=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(key);if(type.isEmpty())continue;
            var start=level.structureManager().getStructureAt(p,type.get().value());
            if(start.isValid()&&camp.id.equals(Long.toString(start.getBoundingBox().getCenter().asLong())))return true;
        }
        return false;
    }
    static int radius(Mob mob){for(var tag:mob.entityTags())if(tag.startsWith(TAG)){var c=CAMPS.get(tag.substring(TAG.length()));if(c!=null)return c.radius>0?c.radius:48;}return 34;}
    static boolean safePath(ServerLevel level,Mob mob,net.minecraft.world.level.pathfinder.Path path){
        return path!=null&&path.canReach()&&safeNodes(level,mob,path);
    }
    private static boolean safeNodes(ServerLevel level,Mob mob,net.minecraft.world.level.pathfinder.Path path){
        if(path==null||path.getNodeCount()==0)return false;
        var home=home(mob);if(home==null)return false;int y=mob.getBlockY(),range=radius(mob);
        for(int i=0;i<path.getNodeCount();i++){
            var p=path.getNodePos(i);
            if(!level.isLoaded(p)||!level.getWorldBorder().isWithinBounds(p)||Math.abs(p.getX()-home.x())>range
                    ||Math.abs(p.getZ()-home.z())>range||p.getY()<y-(mob.getHealth()<=5?1:2)||!level.getFluidState(p).isEmpty())return false;
            // Native node classification can accept a carpeted counter under
            // a low ceiling even though this body's actual height cannot fit.
            // Validate the real block shapes before committing that route.
            if(NativeRegionsScenario.nativeRegions(level)){
                var state=level.getBlockState(p);
                if(!state.is(net.minecraft.tags.BlockTags.WOODEN_DOORS)){
                    double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,p);
                    var shape=state.getCollisionShape(level,p);
                    if(!shape.isEmpty())floor=Math.max(floor,p.getY()+shape.max(net.minecraft.core.Direction.Axis.Y));
                    var feet=new Vec3(p.getX()+.5,floor,p.getZ()+.5);
                    if(!level.noBlockCollision(mob,mob.getBoundingBox().move(feet.subtract(mob.position()))))return false;
                }
            }
            y=p.getY();
        }
        return true;
    }
    /** A known home may be farther than one search window; accept only real progress toward it. */
    static boolean returnHome(ServerLevel level,Mob mob,BlockPos home){
        return NativeBodyRoutes.search(level,mob,()->returnHomePath(level,mob,home));
    }
    private static boolean returnHomePath(ServerLevel level,Mob mob,BlockPos home){
        var path=NativePathLimits.search(mob,mob.getHealth()<=5?1:2,()->mob.getNavigation().createPath(home,1,64));
        if(!safeNodes(level,mob,path))return false;
        if(!path.canReach()&&(path.getNodeCount()<2||Vec3.atBottomCenterOf(path.getEndNode().asBlockPos()).distanceToSqr(Vec3.atBottomCenterOf(home))
                >=mob.position().distanceToSqr(Vec3.atBottomCenterOf(home))))return false;
        return mob.getNavigation().moveTo(path,.8);
    }
    /** Explore the current floor using native paths; the heightmap may be a barracks roof. */
    static boolean navigateLocal(ServerLevel level,Mob mob,double x,double z){
        return NativeBodyRoutes.search(level,mob,()->navigateLocalPath(level,mob,x,z));
    }
    private static boolean navigateLocalPath(ServerLevel level,Mob mob,double x,double z){
        var target=new BlockPos((int)Math.floor(x),mob.getBlockY(),(int)Math.floor(z));
        var stands=new ArrayList<BlockPos>();
        for(var p:BlockPos.betweenClosed(target.offset(-2,0,-2),target.offset(2,0,2))){
            if(level.isLoaded(p.above())&&level.getBlockState(p).getCollisionShape(level,p).isEmpty()
                    &&level.getBlockState(p.above()).getCollisionShape(level,p.above()).isEmpty()
                    &&level.getFluidState(p).isEmpty()&&level.getBlockState(p.below()).isFaceSturdy(level,p.below(),net.minecraft.core.Direction.UP))stands.add(p.immutable());
        }
        stands.sort(Comparator.comparingDouble(p->p.distSqr(target)));
        for(var p:stands.stream().limit(4).toList()){
            var path=mob.getNavigation().createPath(p,0,64);
            if(safePath(level,mob,path))return mob.getNavigation().moveTo(path,.7);
        }
        mob.getNavigation().stop();return false;
    }
    static boolean approach(ServerLevel level,Mob mob,net.minecraft.world.entity.Entity other,double speed){
        return NativeBodyRoutes.search(level,mob,()->approachPath(level,mob,other,speed));
    }
    private static boolean approachPath(ServerLevel level,Mob mob,net.minecraft.world.entity.Entity other,double speed){
        var path=NativePathLimits.search(mob,mob.getHealth()<=5?1:2,()->mob.getNavigation().createPath(other.blockPosition(),1,64));
        if(safePath(level,mob,path))return mob.getNavigation().moveTo(path,speed);
        mob.getNavigation().stop();return false;
    }
    static void inherit(Mob parent,Mob child){for(var tag:parent.entityTags())if(tag.startsWith(TAG)&&CAMPS.containsKey(tag.substring(TAG.length())))child.addTag(tag);}
    private static void restore(ServerLevel level){
        if(!NativeRegionsScenario.nativeRegions(level))return;
        for(var c:CAMPS.values())for(var id:c.founders){
            if(!(level.getEntity(UUID.fromString(id)) instanceof Pillager mob)||!mob.isAlive()||mob.entityTags().contains(CreatureAgents.RELEASED))continue;
            mob.addTag(TAG+c.id);
            if(!CreatureAgents.managed(mob))CreatureAgents.adopt(level,mob);
        }
    }
    static double resources(ServerLevel level,Pillager mob){
        double hay=Double.MAX_VALUE,table=Double.MAX_VALUE;var origin=mob.blockPosition();
        var hayCells=new ArrayList<BlockPos>();var tables=new ArrayList<BlockPos>();
        for(var p:BlockPos.betweenClosed(origin.offset(-16,-3,-16),origin.offset(16,3,16))){
            if(!level.isLoaded(p)||ConstructionSites.isProtected(level,p))continue;var b=level.getBlockState(p);
            if(b.is(Blocks.CRAFTING_TABLE))tables.add(p.immutable());
            if(!b.is(Blocks.HAY_BLOCK)||!level.getBlockState(p.above()).isAir()||p.equals(mob.blockPosition().below())
                ||!level.getEntities(null,new AABB(p).expandTowards(0,2,0)).isEmpty())continue;
            var hit=level.clip(new net.minecraft.world.level.ClipContext(mob.getEyePosition(),Vec3.atCenterOf(p),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,mob));
            if(hit.getType()==HitResult.Type.MISS||hit.getBlockPos().equals(p))hayCells.add(p.immutable());
        }
        hayCells.sort(Comparator.comparingDouble(p->p.distSqr(origin)));tables.sort(Comparator.comparingDouble(p->p.distSqr(origin)));
        for(var p:hayCells.stream().limit(3).toList())if(resourcePath(level,mob,p)){hay=p.distSqr(origin);break;}
        for(var p:tables.stream().limit(3).toList())if(resourcePath(level,mob,p)){table=p.distSqr(origin);break;}
        return hay==Double.MAX_VALUE||table==Double.MAX_VALUE?Double.MAX_VALUE:hay+table;
    }
    private static boolean resourcePath(ServerLevel level,Pillager mob,BlockPos p){
        return resourcePathCost(level,mob,p)!=Integer.MAX_VALUE;
    }
    private static int resourcePathCost(ServerLevel level,Pillager mob,BlockPos p){
        boolean open=mob.getNavigation().getNodeEvaluator().canOpenDoors();
        try{return resourcePathCostWithDoors(level,mob,p);}
        finally{mob.getNavigation().setCanOpenDoors(open);}
    }
    private static int resourcePathCostWithDoors(ServerLevel level,Pillager mob,BlockPos p){
        mob.getNavigation().setCanOpenDoors(true);
        if(BlockContact.point(level,mob,mob.getEyePosition(),p,3)!=null)return 0;
        var stands=new ArrayList<BlockPos>();
        for(var q:BlockPos.betweenClosed(p.offset(-3,-2,-3),p.offset(3,2,3))){
            if(!level.isLoaded(q.above())||!level.getFluidState(q).isEmpty())continue;
            double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,q);
            var feet=new Vec3(q.getX()+.5,floor,q.getZ()+.5);var body=mob.getBoundingBox().move(feet.subtract(mob.position()));
            if(level.noBlockCollision(mob,body)&&!level.noBlockCollision(mob,body.move(0,-.03,0))
                &&BlockContact.point(level,mob,feet.add(0,mob.getEyeHeight(),0),p,3)!=null)
                stands.add(BlockPos.containing(feet.x,Math.ceil(feet.y),feet.z));
        }
        stands=new ArrayList<>(new LinkedHashSet<>(stands));stands.sort(Comparator.comparingDouble(q->q.distSqr(mob.blockPosition())));
        try{
            mob.getNavigation().setMaxVisitedNodesMultiplier(32);
            for(var stand:stands.stream().limit(4).toList()){
                var path=NativePathLimits.search(mob,2,()->mob.getNavigation().createPath(stand,0,64));
                if(path==null||!path.canReach()||path.getNodeCount()>128)continue;
                int y=mob.getBlockY();boolean safe=true;
                for(int i=0;i<path.getNodeCount();i++){
                    var n=path.getNodePos(i);
                    if(!level.isLoaded(n)||!level.getWorldBorder().isWithinBounds(n)||n.getY()<y-2||!level.getFluidState(n).isEmpty()){safe=false;break;}
                    y=n.getY();
                }
                if(safe)return path.getNodeCount();
            }
        }finally{mob.getNavigation().resetMaxVisitedNodesMultiplier();}
        return Integer.MAX_VALUE;
    }
    private static int recipientPathCost(ServerLevel level,Pillager mob,net.minecraft.world.entity.npc.villager.Villager receiver){
        if(CampFoodHatch.contact(level,mob,receiver,mob.getEyePosition())!=null)return 0;
        var stands=new ArrayList<BlockPos>();
        for(var p:BlockPos.betweenClosed(receiver.blockPosition().offset(-3,-2,-3),receiver.blockPosition().offset(3,1,3))){
            if(!level.isLoaded(p.above())||!level.getFluidState(p).isEmpty())continue;
            double floor=WalkNodeEvaluator.getFloorLevel(level,p);var feet=new Vec3(p.getX()+.5,floor,p.getZ()+.5);
            var body=mob.getBoundingBox().move(feet.subtract(mob.position()));
            if(level.noBlockCollision(mob,body)&&!level.noBlockCollision(mob,body.move(0,-.03,0))
                &&CampFoodHatch.contact(level,mob,receiver,feet.add(0,mob.getEyeHeight(),0))!=null)stands.add(p.immutable());
        }
        stands.sort(Comparator.comparingDouble(p->p.distSqr(mob.blockPosition())));
        boolean open=mob.getNavigation().getNodeEvaluator().canOpenDoors();
        try{
            mob.getNavigation().setCanOpenDoors(true);mob.getNavigation().setMaxVisitedNodesMultiplier(32);
            int best=Integer.MAX_VALUE;
            for(var p:stands.stream().limit(6).toList()){
                var path=NativePathLimits.search(mob,2,()->mob.getNavigation().createPath(p,0,128));
                if(path==null||!path.canReach()||path.getNodeCount()>128)continue;boolean safe=true;int y=mob.getBlockY();
                for(int i=0;i<path.getNodeCount();i++){
                    var at=path.getNodePos(i);
                    if(!level.isLoaded(at)||!level.getWorldBorder().isWithinBounds(at)||at.getY()<y-2||!level.getFluidState(at).isEmpty()){safe=false;break;}
                    y=at.getY();
                }
                if(safe)best=Math.min(best,path.getNodeCount());
            }
            return best;
        }finally{mob.getNavigation().resetMaxVisitedNodesMultiplier();mob.getNavigation().setCanOpenDoors(open);}
    }
    static int enroll(ServerLevel level)throws IOException{
        if(!NativeRegionsScenario.nativeRegions(level)||!level.getServer().getWorldData().getLevelName().equals("NativeFourRegions"))throw new IllegalStateException("Requires separate native world");
        var candidates=new LinkedHashMap<String,List<Pillager>>();var discovered=new LinkedHashMap<String,Camp>();var scores=new HashMap<UUID,Double>();
        for(var e:level.getAllEntities())if(e instanceof Pillager mob&&mob.isAlive()&&!CreatureAgents.managed(mob)&&!mob.entityTags().contains(CreatureAgents.RELEASED)&&level.getWorldBorder().isWithinBounds(mob.blockPosition())){
            var start=level.structureManager().getStructureWithPieceAt(mob.blockPosition(),h->true);if(!start.isValid())continue;
            String key=String.valueOf(level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(start.getStructure()));if(!STRUCTURES.contains(key))continue;
            var center=start.getBoundingBox().getCenter();var id=Long.toString(center.asLong());
            int radius=Math.min(128,Math.max(48,Math.max(start.getBoundingBox().getXSpan(),start.getBoundingBox().getZSpan())/2+8));
            if(CAMPS.containsKey(id)||Math.abs(mob.getX()-center.getX())>radius||Math.abs(mob.getZ()-center.getZ())>radius)continue;
            double score=resources(level,mob);if(score!=Double.MAX_VALUE)scores.put(mob.getUUID(),score);
            discovered.put(id,new Camp(id,key,center.getX(),center.getY(),center.getZ(),radius,List.of()));candidates.computeIfAbsent(id,k->new ArrayList<>()).add(mob);
        }
        int count=0;var next=new LinkedHashMap<>(CAMPS);
        for(var entry:candidates.entrySet()){
            var bodies=entry.getValue();if(bodies.size()<2)continue;
            // Begin with one existing local pair, rather than enrolling more
            // adults than the camp's scarce spare bedding can support.
            List<Pillager> pair=null;double best=Double.MAX_VALUE;
            for(int a=0;a<bodies.size();a++)for(int b=a+1;b<bodies.size();b++){
                var first=bodies.get(a);var second=bodies.get(b);
                if(first.distanceToSqr(second)>16*16||Math.abs(first.getY()-second.getY())>1.25||!first.hasLineOfSight(second))continue;
                // One provisioner can found a household with a visible native
                // neighbour; the partner still earns every meal and material.
                double resource=Math.min(scores.getOrDefault(first.getUUID(),Double.MAX_VALUE),scores.getOrDefault(second.getUUID(),Double.MAX_VALUE));
                if(resource==Double.MAX_VALUE)continue;
                double score=resource+first.distanceToSqr(second);
                if(score<best){best=score;pair=List.of(first,second);}
            }
            if(pair==null)continue;
            var c=discovered.get(entry.getKey());var ids=pair.stream().map(v->v.getUUID().toString()).toList();
            next.put(c.id,new Camp(c.id,c.structure,c.x,c.y,c.z,c.radius,ids));count+=ids.size();
        }
        if(count==0)return 0;
        var root=new JsonObject();root.addProperty("schema",1);root.add("camps",JSON.toJsonTree(next.values()));root.addProperty("suppliedItems",0);root.addProperty("spawnedFounders",0);
        var temporary=file(level.getServer()).resolveSibling("native-illager-settlements.json.tmp");Files.writeString(temporary,JSON.toJson(root));Files.move(temporary,file(level.getServer()),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        CAMPS.clear();CAMPS.putAll(next);restore(level);return count;
    }
    /** One additional existing body; founder and deceased-body history are never rewritten. */
    static Map<String,Object> recruitWorker(ServerLevel level,UUID resident)throws IOException{
        return recruitWorker(level,resident,null);
    }
    static void requireExpectedWorker(UUID actual,UUID expected){
        if(expected!=null&&!expected.equals(actual))throw new IllegalStateException("Eligible worker differs from the reviewed body; no enrollment performed");
    }
    static Map<String,Object> recruitWorker(ServerLevel level,UUID resident,UUID expected)throws IOException{
        if(!NativeRegionsScenario.nativeRegions(level)||!level.getServer().getWorldData().getLevelName().equals("NativeFourRegions"))
            throw new IllegalStateException("Existing native world required");
        if(!(level.getEntity(resident) instanceof net.minecraft.world.entity.npc.villager.Villager reference)||!reference.isAlive())
            throw new IllegalArgumentException("Existing living resident reference required");
        Camp camp=null;
        for(var c:CAMPS.values()){
            var key=net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE,net.minecraft.resources.Identifier.parse(c.structure));
            var type=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(key);if(type.isEmpty())continue;
            var start=level.structureManager().getStructureAt(reference.blockPosition(),type.get().value());
            if(start.isValid()&&c.id.equals(Long.toString(start.getBoundingBox().getCenter().asLong()))){camp=c;break;}
        }
        if(camp==null)throw new IllegalArgumentException("Resident must be in an existing registered camp");
        var ledger=level.getServer().getWorldPath(LevelResource.ROOT).resolve("native-illager-worker-history.jsonl");
        var recorded=new HashSet<UUID>();int count=0;
        if(Files.exists(ledger))for(var line:Files.readAllLines(ledger)){
            if(line.isBlank())continue;var row=JsonParser.parseString(line).getAsJsonObject();
            if(row.get("schema").getAsInt()!=1)throw new IOException("Unrecognized worker history");
            if(!recorded.add(UUID.fromString(row.get("id").getAsString())))throw new IOException("Duplicate worker history");
            if(camp.id.equals(row.get("camp").getAsString()))count++;
        }
        if(count>=2)return Map.of("enrolledExisting",0,"reason","bounded existing worker history is full");
        var session=Villagers.session(reference,level);Pillager chosen=null;double best=Double.MAX_VALUE;
        var evaluated=new ArrayList<Map<String,Object>>();
        for(var mob:level.getEntitiesOfClass(Pillager.class,reference.getBoundingBox().inflate(16),p->p.isAlive()
            &&!CreatureAgents.managed(p)&&!p.entityTags().contains(CreatureAgents.RELEASED)&&!recorded.contains(p.getUUID())
            &&!p.isNoAi()&&!p.isInWater()&&!p.isInLava()&&!p.isOnFire()&&p.onGround()&&p.getHealth()>=p.getMaxHealth()*.9
            &&p.getActiveEffects().isEmpty()&&NativeFoodStorage.homeStructure(level,reference,session,p.blockPosition()))){
            int access=Integer.MAX_VALUE;
            {
                // A real reachable store may provide supplies; its contents are
                // unknown until the worker actually walks there after adoption.
                var origin=mob.blockPosition();
                for(var p:BlockPos.betweenClosed(origin.offset(-12,-3,-12),origin.offset(12,3,12))){
                    if(!level.isLoaded(p)||!NativeFoodStorage.homeStructure(level,reference,session,p)
                        ||NativeContainers.accessible(level,p)==null)continue;
                    access=Math.min(access,resourcePathCost(level,mob,p));
                    if(access==0)break;
                }
            }
            if(access==Integer.MAX_VALUE&&resources(level,mob)!=Double.MAX_VALUE)access=129;
            double distance=mob.distanceToSqr(reference);
            evaluated.add(Map.of("id",mob.getUUID().toString(),"position",mob.position().toString(),"supplyPathNodes",access,"distanceSquared",distance));
            // Prefer a local existing neighbour after verifying real resource
            // access. A cheap path to just one caller did not demonstrate
            // sustained coverage of the whole camp in copied rehearsals.
            if(access!=Integer.MAX_VALUE&&distance<best){chosen=mob;best=distance;}
        }
        if(chosen==null)return Map.of("enrolledExisting",0,"reason","no existing grounded worker with real resource access");
        requireExpectedWorker(chosen.getUUID(),expected);
        var before=workerBody(chosen);var row=new LinkedHashMap<String,Object>();
        row.put("schema",1);row.put("id",chosen.getUUID().toString());row.put("camp",camp.id);row.put("joinedAt",level.getGameTime());
        row.put("reference",resident.toString());row.put("role","additional existing worker");row.put("bodyBefore",before);
        row.put("selection","Closest eligible local neighbour with real resource access; contents remain unknown until contact");row.put("candidates",evaluated);
        row.put("suppliedItems",0);row.put("spawnedAdults",0);
        Files.writeString(ledger,new Gson().toJson(row)+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        chosen.addTag(TAG+camp.id);chosen.addTag("villagers:native_illager_worker");CreatureAgents.adopt(level,chosen);
        if(!before.equals(workerBody(chosen)))throw new IllegalStateException("Worker adoption altered physical body or stock");
        row.put("enrolledExisting",1);row.put("bodyAfter",workerBody(chosen));return Map.copyOf(row);
    }
    static String workerBody(Pillager mob){
        return mob.getUUID()+":"+mob.position()+":"+mob.getHealth()+":"+mob.isNoAi()+":"+mob.isInvulnerable()
            +":"+HarvestMaterials.snapshot(mob.getInventory())+":"+mob.getMainHandItem()+":"+mob.getOffhandItem();
    }
    static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.literal("native-illagers").executes(c->{
            try{int count=enroll(c.getSource().getLevel());c.getSource().sendSuccess(()->Component.literal("Enrolled "+count+" existing native illager founders; no bodies or supplies created."),false);return count;}
            catch(IOException|IllegalStateException error){c.getSource().sendFailure(Component.literal(error.getMessage()));return 0;}
        })));
        dispatcher.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.literal("native-worker")
            .then(Commands.argument("resident",StringArgumentType.word()).executes(c->{
                try{var receipt=recruitWorker(c.getSource().getLevel(),UUID.fromString(StringArgumentType.getString(c,"resident")));
                    c.getSource().sendSuccess(()->Component.literal(new Gson().toJson(receipt)),false);return ((Number)receipt.get("enrolledExisting")).intValue();}
                catch(IOException|IllegalArgumentException|IllegalStateException error){c.getSource().sendFailure(Component.literal(error.getMessage()));return 0;}
            }))));
    }
    private NativeIllagers(){}
}
