package local.villagers;

import com.google.gson.*;
import com.mojang.brigadier.CommandDispatcher;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

/** Explicit map configuration for the native-world adventure, never an actor/resource operation. */
final class NativeExploration {
    static final int SIZE=8192;
    static void expand(ServerLevel level) throws IOException {
        if(level.dimension()!=Level.OVERWORLD||!NativeRegionsScenario.nativeRegions(level)
                ||!level.getServer().getWorldData().getLevelName().equals("NativeFourRegions"))
            throw new IllegalStateException("Exploration requires the separate NativeFourRegions world");
        var root=level.getServer().getWorldPath(LevelResource.ROOT);
        var objective=root.resolve("villagers/adventure-objective.json");
        if(!Files.isRegularFile(objective))throw new IllegalStateException("A recorded native adventurer is required");
        var mission=JsonParser.parseString(Files.readString(objective)).getAsJsonObject();
        var id=UUID.fromString(mission.get("villager").getAsString());
        if(!(level.getEntity(id) instanceof Villager v)||!v.isAlive()||!Villagers.isManaged(v)
                ||!AdventurePreparation.nominated(v)||!AdventurePreparation.pickaxe(v))
            throw new IllegalStateException("The recorded adventurer must be loaded, alive and own a pickaxe");
        var record=root.resolve("native-exploration.json");
        var border=level.getWorldBorder();
        if(Files.exists(record)){
            var previous=JsonParser.parseString(Files.readString(record)).getAsJsonObject();
            double configured=previous.get("border").getAsDouble();
            if(!Double.isFinite(configured)||configured<SIZE||configured>59_999_968)throw new IllegalStateException("Unrecognized exploration record; preserving it");
            if(border.getSize()<configured)border.setSize(configured);
            return;
        }
        var data=new JsonObject();data.addProperty("schema",1);data.addProperty("previousBorder",border.getSize());
        data.addProperty("border",Math.max(SIZE,border.getSize()));data.addProperty("gameTime",level.getGameTime());
        data.addProperty("adventurer",id.toString());data.addProperty("fourNativeRegionsRetained",true);
        data.addProperty("suppliedItems",0);data.addProperty("authoredStructures",0);data.addProperty("actorMoves",0);
        var temporary=record.resolveSibling("native-exploration.json.tmp");
        Files.writeString(temporary,new GsonBuilder().setPrettyPrinting().create().toJson(data));
        Files.move(temporary,record,StandardCopyOption.ATOMIC_MOVE);
        if(border.getSize()<SIZE)border.setSize(SIZE);
    }
    static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("native-exploration").executes(c->{
                try{expand(c.getSource().getLevel());
                    c.getSource().sendSuccess(()->Component.literal("Native exploration border opened to "+SIZE+" blocks; existing terrain, four regions and residents preserved."),false);return 1;
                }catch(IOException|IllegalStateException|IllegalArgumentException error){
                    c.getSource().sendFailure(Component.literal("Exploration unchanged: "+error.getMessage()));return 0;
                }
            })));
    }
    private NativeExploration(){}
}
