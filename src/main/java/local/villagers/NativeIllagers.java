package local.villagers;

import com.google.gson.*;
import com.mojang.brigadier.CommandDispatcher;
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
    private static final String TABLE="villagers:camp_table:";
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
            var start=level.structureManager().getStructureWithPieceAt(p,h->h.is(key));
            if(start.isValid()&&camp.id.equals(Long.toString(start.getBoundingBox().getCenter().asLong())))return true;
        }
        return false;
    }
    static int radius(Mob mob){for(var tag:mob.entityTags())if(tag.startsWith(TAG)){var c=CAMPS.get(tag.substring(TAG.length()));if(c!=null)return c.radius>0?c.radius:48;}return 34;}
    static boolean safePath(ServerLevel level,Mob mob,net.minecraft.world.level.pathfinder.Path path){
        if(path==null||!path.canReach())return false;
        var home=home(mob);if(home==null)return false;int y=mob.getBlockY(),range=radius(mob);
        for(int i=0;i<path.getNodeCount();i++){
            var p=path.getNodePos(i);
            if(!level.isLoaded(p)||!level.getWorldBorder().isWithinBounds(p)||Math.abs(p.getX()-home.x())>range
                    ||Math.abs(p.getZ()-home.z())>range||p.getY()<y-(mob.getHealth()<=5?1:2)||!level.getFluidState(p).isEmpty())return false;
            y=p.getY();
        }
        return true;
    }
    /** Explore the current floor using native paths; the heightmap may be a barracks roof. */
    static boolean navigateLocal(ServerLevel level,Mob mob,double x,double z){
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
        var path=mob.getNavigation().createPath(other.blockPosition(),1,64);
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
    private static double resources(ServerLevel level,Pillager mob){
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
        mob.getNavigation().setCanOpenDoors(true);
        var path=mob.getNavigation().createPath(p,1,64);if(path==null||!path.canReach())return false;
        int y=mob.getBlockY();
        for(int i=0;i<path.getNodeCount();i++){
            var n=path.getNodePos(i);if(!level.isLoaded(n)||!level.getWorldBorder().isWithinBounds(n)||n.getY()<y-2||!level.getFluidState(n).isEmpty())return false;
            y=n.getY();
        }
        return true;
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
    static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.literal("native-illagers").executes(c->{
            try{int count=enroll(c.getSource().getLevel());c.getSource().sendSuccess(()->Component.literal("Enrolled "+count+" existing native illager founders; no bodies or supplies created."),false);return count;}
            catch(IOException|IllegalStateException error){c.getSource().sendFailure(Component.literal(error.getMessage()));return 0;}
        })));
    }
    private NativeIllagers(){}
}
