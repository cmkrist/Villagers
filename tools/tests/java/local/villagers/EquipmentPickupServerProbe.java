package local.villagers;

import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import java.nio.file.Files;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;

/** Fresh-world regression: native movement and native pickup, including a live player thrower reference. */
public final class EquipmentPickupServerProbe implements ModInitializer {
    private int ticks, started;
    private Villager actor;
    private ItemEntity pickaxe, sword;
    private final List<String> passed = new ArrayList<>();
    private void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        passed.add(message);
    }
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                ticks++;
                var level = server.overworld();
                if (ticks == 1) { level.setChunkForced(0,0,true); level.getChunk(0,0); }
                if (ticks == 40) {
                    for (int x=1;x<=14;x++) for (int z=1;z<=14;z++) for (int y=80;y<=84;y++)
                        level.setBlock(new BlockPos(x,y,z), (y==80 || y<=83 && (x==1||x==14||z==1||z==14)
                            ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 3);
                    actor = new Villager(EntityTypes.VILLAGER,level); actor.snapTo(4.5,81,7.5,0,0);
                    level.addFreshEntity(actor); Villagers.adopt(level,actor);
                    var unmanaged = new Villager(EntityTypes.VILLAGER,level);
                    for (var item : List.of(Items.IRON_PICKAXE,Items.DIAMOND_SWORD,Items.IRON_AXE,Items.IRON_SHOVEL,
                            Items.IRON_HOE,Items.SHEARS,Items.BOW,Items.CROSSBOW,Items.TRIDENT,Items.SHIELD)) {
                        var stack = new ItemStack(item);
                        check(AdventureActions.acceptsDroppedEquipment(stack), "recognizes " + item);
                        check(actor.wantsToPickUp(level,stack), "managed villager wants " + item);
                        check(!unmanaged.wantsToPickUp(level,stack), "unmanaged villager unchanged for " + item);
                    }
                    check(!AdventureActions.acceptsDroppedEquipment(new ItemStack(Items.DIRT)), "ordinary blocks excluded");
                    check(!AdventureActions.acceptsDroppedEquipment(ItemStack.EMPTY), "empty stack excluded");
                    for(int i=0;i<actor.getInventory().getContainerSize();i++) actor.getInventory().setItem(i,new ItemStack(Items.DIRT,64));
                    check(!actor.wantsToPickUp(level,new ItemStack(Items.IRON_PICKAXE)), "full inventory rejects equipment");
                    actor.getInventory().clearContent();
                    var donor = new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"EquipmentDonor"),ClientInformation.createDefault());
                    var tool = new ItemStack(Items.IRON_PICKAXE); tool.setDamageValue(37);
                    pickaxe = new ItemEntity(level,8.5,81,7.5,tool); pickaxe.setDeltaMovement(0,0,0);
                    pickaxe.setThrower(donor); pickaxe.setPickUpDelay(60); level.addFreshEntity(pickaxe);
                    sword = new ItemEntity(level,9.5,81,7.5,new ItemStack(Items.DIAMOND_SWORD));
                    sword.setDeltaMovement(0,0,0); sword.setThrower(donor); sword.setPickUpDelay(60); level.addFreshEntity(sword);
                    check(pickaxe.getOwner()==donor, "drop retains player thrower reference");
                    started=ticks;
                }
                if(actor==null) return;
                if(ticks-started==30) check(pickaxe.isAlive()&&sword.isAlive()&&actor.getInventory().isEmpty(), "pickup delay respected");
                if(ticks-started>60 && !pickaxe.isAlive() && !sword.isAlive()) {
                    var all = new ArrayList<ItemStack>();
                    all.add(actor.getMainHandItem()); all.add(actor.getOffhandItem());
                    for(int i=0;i<actor.getInventory().getContainerSize();i++)all.add(actor.getInventory().getItem(i));
                    check(all.stream().filter(s->s.is(Items.IRON_PICKAXE)).mapToInt(ItemStack::getCount).sum()==1, "exactly one pickaxe transferred");
                    check(all.stream().anyMatch(s->s.is(Items.IRON_PICKAXE)&&s.getDamageValue()==37), "tool damage preserved");
                    check(all.stream().filter(s->s.is(Items.DIAMOND_SWORD)).mapToInt(ItemStack::getCount).sum()==1, "exactly one sword transferred");
                    check(actor.isAlive() && actor.getX()>5, "native approach reached the drops");
                    Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("equipment-result.json"),
                        new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("status","passed","ticks",ticks,"checks",passed)));
                    server.halt(false);
                }
                if(ticks>640)throw new AssertionError("Timed out waiting for native pickup: " + actor.position());
            } catch(Throwable failure) { failure.printStackTrace(); server.halt(false); }
        });
    }
}
