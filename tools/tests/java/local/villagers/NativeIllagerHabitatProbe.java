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
    private int ticks;private long time;private boolean done;
    private boolean started;private final boolean rehearse=Boolean.getBoolean("villagers.boats.rehearse");
    private final Set<UUID> founders=new HashSet<>();
    private final Set<UUID> initialActors=new HashSet<>();
    private final int window=Integer.getInteger("villagers.illagers.windowTicks",8000);
    @Override public void onInitialize(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            if(!Files.exists(FabricLoader.getInstance().getGameDir().resolve("food-diagnostic-copy.json")))throw new IllegalStateException("marked COPY required");
            time=server.overworld().getGameTime();server.tickRateManager().setFrozen(true);
            if(rehearse)server.tickRateManager().setTickRate(Integer.getInteger("villagers.farm.tps",20));
            for(var center:List.of(new BlockPos(-656,70,-656),new BlockPos(680,70,24),new BlockPos(-825,70,40)))
                for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){
                    int x=(center.getX()>>4)+dx,z=(center.getZ()>>4)+dz;server.overworld().setChunkForced(x,z,true);server.overworld().getChunk(x,z);
                }
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(done||++ticks<40)return;
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
                if(rehearse){
                    if(!started){
                        if(!Villagers.ready())return;
                        var before=new HashMap<UUID,String>();for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.monster.illager.Pillager p)
                            before.put(p.getUUID(),p.getInventory().toString()+p.getMainHandItem()+p.getOffhandItem());
                        boolean resume=!NativeIllagers.founders().isEmpty();
                        int enrolled=NativeIllagers.enroll(level);if(resume?enrolled!=0:enrolled<2)throw new AssertionError("existing founders retained, or one actual resource-ready founding pair required");
                        founders.addAll(NativeIllagers.founders());
                        for(var e:level.getAllEntities())if(e instanceof AbstractIllager mob&&CreatureAgents.managed(mob)&&NativeIllagers.member(mob)){
                            initialActors.add(mob.getUUID());var p=(net.minecraft.world.entity.monster.illager.Pillager)mob;
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
                        Files.writeString(output.resolveSibling("native-illager-initial.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("gameTime",time,"founders",founders,"initialActors",initialActors,"existingPillagers",before.size(),"enrolled",enrolled,"resumed",resume,"scope","Existing bodies retained; inventory and equipment unchanged; record persisted before adoption. Resume keeps saved founders and native supplies.")));
                        started=true;server.tickRateManager().setFrozen(false);return;
                    }
                    long elapsed=level.getGameTime()-time;if(elapsed%40!=0)return;
                    var actors=new ArrayList<Map<String,Object>>();boolean birth=false,healthStop=false;
                    var criticalVillagers=new ArrayList<String>();
                    var villagerStores=new ArrayList<Map<String,Object>>();
                    for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.npc.villager.Villager v&&Villagers.isManaged(v)&&v.isAlive()&&v.getHealth()<=4){
                        criticalVillagers.add(v.getUUID().toString());healthStop=true;
                    }
                    for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.npc.villager.Villager v&&Villagers.isManaged(v)&&v.isAlive()){
                        var s=Villagers.existingSession(v.getUUID());if(s!=null)villagerStores.add(Map.of("id",v.getUUID().toString(),"name",s.mind.name(),"health",v.getHealth(),"food",s.mind.foodLevel(),
                            "position",v.blockPosition().toShortString(),"stores",NativeFoodStorage.status(s),"body",Map.of("baby",v.isBaby(),"sleeping",v.isSleeping(),"trading",v.isTrading(),"water",v.isInWater(),"fire",v.isOnFire(),"passenger",v.isPassenger(),
                                "home",String.valueOf(s.mind.home()),"ownedMeal",FoodInventory.anySlot(v.getInventory(),AdventureActions.canHarvest(v)))));
                    }
                    for(var e:level.getAllEntities())if(e instanceof AbstractIllager mob&&CreatureAgents.managed(mob)&&NativeIllagers.member(mob)){
                        var row=new LinkedHashMap<>(CreatureAgents.status(mob));row.put("position",mob.blockPosition().toShortString());actors.add(row);
                        if(!initialActors.contains(mob.getUUID()))birth=true;if(mob.getHealth()<4)healthStop=true;
                    }
                    for(var id:founders)if(!(level.getEntity(id) instanceof AbstractIllager mob)||!mob.isAlive())throw new AssertionError("founder remains alive: "+id);
                    var data=Map.of("status","passed","stage",birth?"native_illager_birth":healthStop?"health_stop":elapsed>=window||Files.exists(output.resolveSibling("stop-observation"))?"bounded_observation":"observing",
                        "elapsed",elapsed,"gameTime",level.getGameTime(),"founders",founders,"actors",actors,"targetTps",server.tickRateManager().tickrate(),"criticalVillagers",criticalVillagers,"villagerFoodStores",villagerStores,
                        "scope","Existing native illager founders; normal resource work and paid breeding only. No body, food, health, inventory, crop or resource setters; copied world only.");
                    Files.writeString(output.resolveSibling("native-illager-progress.json"),new GsonBuilder().setPrettyPrinting().create().toJson(data));
                    if(!birth&&!healthStop&&elapsed<window&&!Files.exists(output.resolveSibling("stop-observation")))return;
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
                Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("status","passed","gameTime",time,"illagers",rows,"camps",camps.values(),"scope","Frozen existing bodies/native structures. No adoption, actor movement, food, health, item, block or clock changes. Primary unchanged.")));
                done=true;server.halt(false);
            }catch(Throwable error){error.printStackTrace();done=true;try{Files.writeString(output,new Gson().toJson(Map.of("status","failed","error",error.toString())));}catch(Exception ignored){}server.halt(false);}
        });
    }
}
