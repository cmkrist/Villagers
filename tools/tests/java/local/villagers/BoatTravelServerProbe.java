package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.LevelResource;

/** Declared two-bank fixture: empty bag + real natural tree + distant loose carrots; no supplied boat/logs. */
public final class BoatTravelServerProbe implements ModInitializer {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private int ticks;
    private boolean built, finished;
    private Villager actor;
    private UUID id;
    private final boolean crops = Boolean.getBoolean("villagers.boats.crops");
    private static void check(boolean pass, String why) { if (!pass) throw new AssertionError(why); }
    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (finished) return;
            var level = server.overworld(); var root = server.getWorldPath(LevelResource.ROOT);
            var result = root.resolve("boat-travel-result.json"); var before = root.resolve("boat-travel-before-restart.json");
            try {
                ticks++;
                if (!Villagers.ready()) return;
                if (!built) {
                    for (int x = -18; x <= -13; x++) for (int z = -18; z <= -15; z++) { level.setChunkForced(x, z, true); level.getChunk(x, z); }
                    if (Files.exists(before)) {
                        var prior = JsonParser.parseString(Files.readString(before)).getAsJsonObject(); id = UUID.fromString(prior.get("actor").getAsString());
                        actor = (Villager) level.getEntity(id); if (actor == null) return;
                        check(actor.isPassenger(), "native passenger restored at sea");
                        check(actor.entityTags().stream().anyMatch(t -> t.startsWith("villagers:sea:")), "trip persisted in native body");
                        check(actor.getInventory().countItem(Items.OAK_PLANKS) == 3, "native spare planks persisted");
                    } else {
                        // Raised, sealed channel at y85. All fixture terrain/body/item setup happens here once.
                        for (int x=-269;x<=-219;x++) for(int z=-267;z<=-253;z++) for(int y=84;y<=93;y++)
                            level.setBlock(new BlockPos(x,y,z), y==84?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(), 2);
                        for(int x=-269;x<=-219;x++) for(int z=-266;z<=-254;z++) {
                            boolean bank=x<=-261||x>=-228;
                            level.setBlock(new BlockPos(x,85,z),(bank?Blocks.GRASS_BLOCK:Blocks.WATER).defaultBlockState(),2);
                        }
                        for(int x=-270;x<=-218;x++) for(int z : new int[]{-267,-253}) for(int y=85;y<=88;y++)
                            level.setBlock(new BlockPos(x,y,z),Blocks.BARRIER.defaultBlockState(),2);
                        for(int z=-267;z<=-253;z++) for(int x : new int[]{-270,-218}) for(int y=85;y<=88;y++)
                            level.setBlock(new BlockPos(x,y,z),Blocks.BARRIER.defaultBlockState(),2);
                        var tree=new BlockPos(-266,86,-260);
                        for(int y=0;y<4;y++)level.setBlock(tree.above(y),Blocks.OAK_LOG.defaultBlockState(),2);
                        for(BlockPos p:BlockPos.betweenClosed(tree.offset(-2,4,-2),tree.offset(2,4,2)))level.setBlock(p,Blocks.OAK_LEAVES.defaultBlockState(),2);
                        check(BoatPreparation.naturalTrunk(level,tree),"declared natural trunk accepted");
                        actor=EntityTypes.VILLAGER.create(level,EntitySpawnReason.COMMAND);
                        actor.snapTo(-264.5,86,-257.5,0,0); level.addFreshEntity(actor); Villagers.adopt(level,actor); id=actor.getUUID();
                        check(actor.getInventory().isEmpty(),"starts with empty bag");
                        if (crops) {
                            for (int x=-226;x<=-222;x++) for (int z=-260;z<=-256;z++) {
                                level.setBlock(new BlockPos(x,85,z),Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.MOISTURE,7),2);
                                level.setBlock(new BlockPos(x,86,z),((CropBlock)Blocks.CARROTS).getStateForAge(7),2);
                            }
                        } else level.addFreshEntity(new ItemEntity(level,-224.5,86,-257.5,new ItemStack(Items.CARROT,16)));
                        // An unrelated boat is not owned or piloted by this actor.
                        var unowned=EntityTypes.OAK_BOAT.create(level,EntitySpawnReason.COMMAND);unowned.setInitialPos(-250,85.6,-262);level.addFreshEntity(unowned);
                        check(BoatTravel.pilot(unowned)==null,"unowned boat never controlled");
                    }
                    built=true;
                }
                var s=Villagers.existingSession(id);if(s==null)return;
                if(ticks%40==0)Files.writeString(root.resolve("boat-travel-progress.json"),JSON.toJson(Map.of("ticks",ticks,"position",List.of(actor.getX(),actor.getY(),actor.getZ()),"boating",BoatTravel.status(s),"inventory",actor.getInventory().toString(),"boat",actor.getVehicle()==null?"none":List.of(actor.getVehicle().getX(),actor.getVehicle().getY(),actor.getVehicle().getZ(),actor.getVehicle().getYRot()))));
                check(ticks<2400,"boat observation exhausted: "+BoatTravel.status(s));
                check(actor.isAlive()&&actor.getHealth()==actor.getMaxHealth(),"healthy actual villager");
                boolean restart=Files.exists(before);
                if(!restart) {
                    if(!(actor.getVehicle() instanceof AbstractBoat boat)||boat.getX()<-247||s.boatPaddleTicks<=40)return;
                    check(BoatTravel.pilot(boat)==actor,"managed first passenger steers native boat");
                    check(s.boatPaddleTicks>40,"native paddle-driven movement observed");
                    check(s.boatLogsGathered==(crops?4:3)&&s.boatRecipes==(crops?8:5),"exact gathered logs and native intermediate/final recipes");
                    check(!crops||AdventureActions.hasHoe(actor),"crop expedition carries a real hoe");
                    check(actor.getInventory().countItem(Items.OAK_PLANKS)==3&&BoatPreparation.boatSlot(actor)<0,"exact costs, consumed boat item, spare planks");
                } else if(s.boatLandings<2||actor.isPassenger()||actor.getX()>-260||s.boatTrip!=null)return;
                check(!restart||actor.getInventory().countItem(Items.CARROT)>0,"actual distant food carried home");
                if(restart&&crops)check(actor.getMainHandItem().is(Items.WOODEN_HOE)&&actor.getMainHandItem().getDamageValue()>0,"native harvesting wore the actual crafted hoe");
                long boats=0;for(var e:level.getAllEntities())if(e instanceof AbstractBoat b&&BoatTravel.owned(b,actor))boats++;
                check(boats==1,"one native owned boat, no duplication on return/reopen");
                Files.writeString(result,JSON.toJson(Map.of("status","passed","stage",restart?"returned_with_food":"saved_at_sea","actor",id.toString(),"boating",BoatTravel.status(s),"position",List.of(actor.getX(),actor.getY(),actor.getZ()),"carrots",actor.getInventory().countItem(Items.CARROT),"ownedBoats",boats,"crops",crops,"held",actor.getMainHandItem().toString(),"fixture","Raised sealed water channel, two banks, one natural tree, distant loose carrots OR mature planted carrots, empty villager bag, one unowned boat. Initial setup only. Normal controller/native recipes/native boat movement and dismount. No post-setup actor/velocity/inventory setters.")));
                finished=true;server.halt(false);
            }catch(Throwable error){error.printStackTrace();finished=true;try{Files.writeString(result,JSON.toJson(Map.of("status","failed","failure",error.toString())));}catch(Exception ignored){}server.halt(false);}
        });
    }
}
