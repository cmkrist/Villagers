package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.LevelResource;

/** Declared empty-bag native fixture: observe wood→table/hoe→real crops→native nomination→pickaxe. */
public final class SurvivalBootstrapServerProbe implements ModInitializer {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private Villager actor;
    private int ticks;
    private boolean built,finished,verifiedAtLoad;
    private UUID id;
    private Villager swimmer;
    private UUID swimmerId;
    private final boolean stone=Boolean.getBoolean("villagers.adventure.stone");
    private final boolean immature=Boolean.getBoolean("villagers.bootstrap.immature");
    private final boolean hand=Boolean.getBoolean("villagers.bootstrap.hand");
    private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    private static Map<String,Object> items(Villager v){
        return Map.of("logs",v.getInventory().countItem(Items.OAK_LOG),"planks",v.getInventory().countItem(Items.OAK_PLANKS),
            "sticks",v.getInventory().countItem(Items.STICK),"pickaxes",count(v,Items.WOODEN_PICKAXE),
            "carrots",v.getInventory().countItem(Items.CARROT),"held",v.getMainHandItem().toString(),"hoeDamage",damage(v,Items.WOODEN_HOE),
            "stonePickaxes",count(v,Items.STONE_PICKAXE),"woodenPickaxeDamage",damage(v,Items.WOODEN_PICKAXE));
    }
    private static int count(Villager v,Item item){return v.getInventory().countItem(item)+(v.getMainHandItem().is(item)?v.getMainHandItem().getCount():0);}
    private static int damage(Villager v,Item item){if(v.getMainHandItem().is(item))return v.getMainHandItem().getDamageValue();for(int i=0;i<v.getInventory().getContainerSize();i++)if(v.getInventory().getItem(i).is(item))return v.getInventory().getItem(i).getDamageValue();return -1;}
    @Override public void onInitialize(){
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            if(!(entity instanceof Villager v))return;
            var before=level.getServer().getWorldPath(LevelResource.ROOT).resolve("survival-bootstrap-before-restart.json");
            if(!Files.exists(before))return;
            try{
                var data=JsonParser.parseString(Files.readString(before)).getAsJsonObject();
                if(!v.getUUID().toString().equals(data.get("actor").getAsString()))return;
                check(JSON.toJsonTree(items(v)).equals(data.get("items")),"owned kit and produce persisted exactly before AI");
                check(AdventurePreparation.nominated(v),"actual body retained nomination");
                check(v.entityTags().stream().anyMatch(t->t.startsWith("villagers:workbench:")),"paid workbench memory persisted in actual body");
                verifiedAtLoad=true;actor=v;id=v.getUUID();
            }catch(Exception error){throw new RuntimeException(error);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(finished)return;
            var level=server.overworld();var root=server.getWorldPath(LevelResource.ROOT);var result=root.resolve("survival-bootstrap-result.json");
            boolean restart=Files.exists(root.resolve("survival-bootstrap-before-restart.json"));
            try{
                ticks++;check(ticks<2400,"bootstrap observation exhausted");
                if(!Villagers.ready())return;
                check(NativeRegionsScenario.nativeRegions(level),"actual native-region generator");
                if(!built){
                    for(int x=11;x<=14;x++)for(int z=11;z<=14;z++){level.setChunkForced(x,z,true);level.getChunk(x,z);}
                    if(!restart){
                        // One-time terrain/body fixture declaration. No supplied inventory/tool/food or later setters.
                        for(int x=193;x<=216;x++)for(int z=190;z<=212;z++)for(int y=99;y<=108;y++)
                            level.setBlock(new BlockPos(x,y,z),(y==99?Blocks.STONE:y==100?Blocks.GRASS_BLOCK:Blocks.AIR).defaultBlockState(),2);
                        var tree=new BlockPos(198,101,199);
                        if(!hand){
                            for(int y=0;y<4;y++)level.setBlock(tree.above(y),Blocks.OAK_LOG.defaultBlockState(),2);
                            for(var p:BlockPos.betweenClosed(tree.offset(-2,4,-2),tree.offset(2,4,2)))
                                level.setBlock(p,Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE,1),2);
                        }
                        for(int x=206;x<=211;x++)for(int z=196;z<=202;z++){
                            level.setBlock(new BlockPos(x,100,z),Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7),2);
                            level.setBlock(new BlockPos(x,101,z),((CropBlock)Blocks.CARROTS).getStateForAge(immature?0:7),2);
                        }
                        if(stone)for(int x=194;x<=196;x++)level.setBlock(new BlockPos(x,101,205),Blocks.STONE.defaultBlockState(),2);
                        actor=EntityTypes.VILLAGER.create(level,EntitySpawnReason.COMMAND);actor.snapTo(200.5,101,200.5,0,0);level.addFreshEntity(actor);
                        Villagers.adopt(level,actor);id=actor.getUUID();check(actor.getInventory().isEmpty(),"empty initial inventory");
                        if(!hand)check(BoatPreparation.naturalTrunk(level,tree),"verified natural source trunk");
                        // A separate enclosed pool checks the native Swim behavior lost when replacing a brain.
                        for(int x=182;x<=188;x++)for(int z=205;z<=211;z++)for(int y=98;y<=108;y++) {
                            var block=y==98?Blocks.STONE:(x==182||x==188||z==205||z==211)&&y<=105?Blocks.GLASS:y<=101?Blocks.WATER:Blocks.AIR;
                            level.setBlock(new BlockPos(x,y,z),block.defaultBlockState(),2);
                        }
                        swimmer=EntityTypes.VILLAGER.create(level,EntitySpawnReason.COMMAND);swimmer.snapTo(185.5,99,208.5,0,0);
                        level.addFreshEntity(swimmer);Villagers.adopt(level,swimmer);swimmerId=swimmer.getUUID();
                        if(stone){
                            var state=Villagers.existingSession(id);var rock=new BlockPos(195,101,205);var side=rock.south();
                            check(AdventureMining.surface(level,actor,state,rock),"exposed natural stone admitted");
                            check(!AdventureMining.surface(level,actor,state,new BlockPos(195,99,205)),"support floor rejected");
                            level.setBlock(side,Blocks.WATER.defaultBlockState(),2);check(!AdventureMining.surface(level,actor,state,rock),"fluid-adjacent quarry rejected");
                            level.setBlock(side,Blocks.BED.red().defaultBlockState(),2);check(!AdventureMining.surface(level,actor,state,rock),"bed-adjacent quarry rejected");
                            level.setBlock(side,Blocks.AIR.defaultBlockState(),2);
                        }
                    } else {
                        var data=JsonParser.parseString(Files.readString(root.resolve("survival-bootstrap-before-restart.json"))).getAsJsonObject();
                        swimmerId=UUID.fromString(data.get("swimmer").getAsString());
                    }
                    built=true;
                }
                if(actor==null)return;
                if(swimmer==null)swimmer=(Villager)level.getEntity(swimmerId);
                if(swimmer==null)return;
                check(actor.isAlive()&&actor.getHealth()==actor.getMaxHealth(),"same healthy living body");
                if(ticks>=160)check(swimmer!=null&&swimmer.isAlive()&&swimmer.getHealth()==swimmer.getMaxHealth()
                    &&swimmer.getY()>100.5&&swimmer.getAirSupply()>=200,"ordinary jump-control swimming reaches breathing air without setters");
                var s=Villagers.existingSession(id);if(s==null)return;
                if(hand&&AdventureActions.activityStatus(id).get("cropsHarvested")>0){
                    check(!AdventureActions.hasHoe(actor)&&actor.getMainHandItem().isEmpty(),"actual bare hand, no supplied tool");
                    check(s.woodBootstrap.logs==0&&s.woodBootstrap.recipes==0,"no wood or tool manufactured");
                    check(AdventureActions.activityStatus(id).get("replantsFromHarvest")>0,"real native yield paid planting cost");
                    check(actor.getInventory().countItem(Items.CARROT)>0,"actual surplus produce");
                    check(Boolean.FALSE.equals(AdventureActions.lastHarvest(id).get("usedHoe")),"truthful native hand receipt");
                    check(Boolean.FALSE.equals(AdventureActions.lastHarvest(id).get("hoeBroke")),"no imaginary broken tool");
                    Files.writeString(result,JSON.toJson(Map.of("status","passed","stage","native_hand_harvest","actor",id.toString(),"items",items(actor),"receipt",AdventureActions.lastHarvest(id),"ticks",ticks,
                        "fixture","Declared mature crops and empty-bag villager; no tree, wood, tools or produce supplied. Native loot and paid replant; no post-setup setters.")));
                    finished=true;server.halt(false);return;
                }
                if(immature&&AdventureActions.hasHoe(actor)){
                    check(count(actor,Items.WOODEN_HOE)==1&&damage(actor,Items.WOODEN_HOE)==0,"real new hoe, no harvesting yet");
                    check(actor.getInventory().countItem(Items.CARROT)==0,"no invented produce");
                    check(s.woodBootstrap.logs==2&&s.woodBootstrap.recipes==5,"exact two real logs and five native preparation recipes");
                    for(int x=206;x<=211;x++)for(int z=196;z<=202;z++){
                        var state=level.getBlockState(new BlockPos(x,101,z));
                        check(state.getBlock() instanceof CropBlock c&&!c.isMaxAge(state),"all original plants still unripe");
                    }
                    Files.writeString(result,JSON.toJson(Map.of("status","passed","stage","prepared_before_ripeness","actor",id.toString(),"items",items(actor),"wood",WoodBootstrap.status(s),"ticks",ticks,
                        "fixture","Empty bag, declared natural tree and age-zero crops. Native tool preparation, no post-setup actor/items/crop/clock setters. No harvest or growth success claimed.")));
                    finished=true;server.halt(false);return;
                }
                if(!restart&&AdventureActions.hasHoe(actor)&&FoodSupply.deficit(actor.getInventory(),true)==0){
                    var snapshot=items(actor);check(AdventurePreparation.nominate(level)==actor,"native map accepts a genuinely provisioned candidate");
                    check(AdventurePreparation.nominate(level)==actor&&snapshot.equals(items(actor)),"repeat nomination preserves identity/items");
                }
                if(ticks%40==0)Files.writeString(root.resolve("survival-bootstrap-progress.json"),JSON.toJson(Map.of("ticks",ticks,"position",List.of(actor.getX(),actor.getY(),actor.getZ()),
                    "items",items(actor),"wood",WoodBootstrap.status(s),"adventure",s.adventureStatus,"food",s.mind.foodLevel(),"harvest",AdventureActions.activityStatus(id))));
                if(!(stone?AdventurePreparation.stonePickaxe(actor):AdventurePreparation.pickaxe(actor)))return;
                check(!restart||verifiedAtLoad,"same body loaded with exact persisted kit");
                check(actor.getInventory().countItem(Items.OAK_PLANKS)==(stone?3:1)&&actor.getInventory().countItem(Items.STICK)==(stone?2:0)
                    &&actor.getInventory().countItem(Items.OAK_LOG)==0,"exact conserved kit spares");
                check(count(actor,Items.WOODEN_HOE)==1&&damage(actor,Items.WOODEN_HOE)>0,"real crafted hoe and native wear");
                check(actor.getInventory().countItem(Items.CARROT)>0,"actual harvested produce");
                check(AdventurePreparation.nominate(level)==actor,"persistent objective resolves same living hero");
                check(actor.entityTags().stream().anyMatch(t->t.startsWith("villagers:workbench:")),"actor remembers its paid workbench");
                if(!restart)check(s.woodBootstrap.logs==(stone?4:3)&&s.woodBootstrap.recipes==(stone?10:7),"exact actual logs and native kit recipes");
                if(stone){check(count(actor,Items.STONE_PICKAXE)==1&&damage(actor,Items.WOODEN_PICKAXE)==4,"real upgrade and exact mining/log tool wear");if(!restart)check(s.mining.blocks==3,"three actual stone blocks mined");}
                var evidence=new LinkedHashMap<String,Object>(Map.of("status","passed","restart",restart,"actor",id.toString(),"items",items(actor),"wood",WoodBootstrap.status(s),
                    "harvest",AdventureActions.activityStatus(id),"mining",AdventureMining.status(s),"stoneUpgrade",stone,"gameTime",level.getGameTime(),"fixture","Empty inventory, one natural trunk, mature planted crops and (upgrade mode) three exposed stone blocks on a declared dry platform. No post-setup body/inventory/clock/health setters. Actual food preparation, harvesting/replanting, native-map nomination, pickaxe preparation/mining/upgrade and exact kit persistence."));
                evidence.put("swimmer",swimmer.getUUID().toString());evidence.put("swimming",Map.of("height",swimmer.getY(),"air",swimmer.getAirSupply(),"health",swimmer.getHealth(),"fixture","Separate managed villager initially submerged in an enclosed pool; ordinary native jump control only."));
                Files.writeString(result,JSON.toJson(evidence));
                finished=true;server.halt(false);
            }catch(Throwable error){error.printStackTrace();finished=true;try{Files.writeString(result,JSON.toJson(Map.of("status","failed","failure",error.toString())));}catch(Exception ignored){}server.halt(false);}
        });
    }
}
