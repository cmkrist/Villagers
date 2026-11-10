package local.villagers;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.LevelResource;

/** Read-only observation after loading a copy of the actual crashed village. No setup or supply. */
public final class LifecycleRecoveryServerProbe implements ModInitializer {
    private int ticks;
    private final Set<UUID> original=new HashSet<>();
    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server->{try {
            ticks++;
            if(ticks==1){server.overworld().setChunkForced(0,0,true);server.overworld().getChunk(0,0);server.tickRateManager().setTickRate(80);}
            var actors=server.overworld().getEntitiesOfClass(Villager.class,new net.minecraft.world.phys.AABB(0,79,0,16,86,16),Villagers::isManaged);
            if(ticks==60){if(actors.size()<4)throw new AssertionError("saved original cohort missing");for(var v:actors)original.add(v.getUUID());}
            if(ticks>=60)for(var id:original){if(!(server.overworld().getEntity(id)instanceof Villager v)||!v.isAlive()||v.getHealth()!=v.getMaxHealth())throw new AssertionError("saved resident lost or hurt during recovery");}
            if(ticks==1200){var data=new LinkedHashMap<String,Object>();data.put("status","passed");data.put("gameTime",server.overworld().getGameTime());data.put("observedTicks",ticks);data.put("sameResidentIDs",original);data.put("scope","Stopped crashed fixture copied once, no resident/resource/terrain setup, ordinary final controller for1200ticks");Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("lifecycle-recovery-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(data));server.halt(false);}
        }catch(Throwable e){e.printStackTrace();server.halt(false);}});
    }
}
