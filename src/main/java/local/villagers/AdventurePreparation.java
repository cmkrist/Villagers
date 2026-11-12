package local.villagers;

import com.mojang.brigadier.CommandDispatcher;
import java.util.Comparator;
import java.nio.file.*;
import java.util.UUID;
import com.google.gson.*;
import local.villagers.genetics.Tendency;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.LevelResource;

/** Persistent mission for an existing body; never supplies ingredients or grants terrain authority. */
final class AdventurePreparation {
    static final String NOMINATED = "villagers:adventurer";
    static boolean nominated(Mob mob) { return mob instanceof Villager && mob.entityTags().contains(NOMINATED); }
    static boolean pickaxe(Villager v) {
        return v.getMainHandItem().is(ItemTags.PICKAXES) || v.getOffhandItem().is(ItemTags.PICKAXES)||v.getInventory().hasAnyMatching(s -> s.is(ItemTags.PICKAXES));
    }
    static boolean stonePickaxe(Villager v) {
        return upgraded(v.getMainHandItem())||upgraded(v.getOffhandItem())||v.getInventory().hasAnyMatching(AdventurePreparation::upgraded);
    }
    static int toolUses(net.minecraft.world.item.ItemStack item){return item.isEmpty()?0:item.isDamageableItem()?Math.max(0,item.getMaxDamage()-item.getDamageValue()):Integer.MAX_VALUE;}
    static boolean reservePickaxe(Villager v){
        return upgraded(v.getMainHandItem())&&toolUses(v.getMainHandItem())>8
            ||upgraded(v.getOffhandItem())&&toolUses(v.getOffhandItem())>8
            ||v.getInventory().hasAnyMatching(a->upgraded(a)&&toolUses(a)>8);
    }
    static boolean ironPickaxe(Villager v){return ironOrBetter(v.getMainHandItem())||ironOrBetter(v.getOffhandItem())||v.getInventory().hasAnyMatching(AdventurePreparation::ironOrBetter);}
    static boolean reserveIronPickaxe(Villager v){
        return ironOrBetter(v.getMainHandItem())&&toolUses(v.getMainHandItem())>8
            ||ironOrBetter(v.getOffhandItem())&&toolUses(v.getOffhandItem())>8
            ||v.getInventory().hasAnyMatching(a->ironOrBetter(a)&&toolUses(a)>8);
    }
    static boolean diamondPickaxe(Villager v){
        return diamondOrBetter(v.getMainHandItem())||diamondOrBetter(v.getOffhandItem())||v.getInventory().hasAnyMatching(AdventurePreparation::diamondOrBetter);
    }
    private static boolean diamondOrBetter(net.minecraft.world.item.ItemStack a){
        return a.is(net.minecraft.world.item.Items.DIAMOND_PICKAXE)||a.is(net.minecraft.world.item.Items.NETHERITE_PICKAXE);
    }
    private static boolean ironOrBetter(net.minecraft.world.item.ItemStack a){
        return a.is(net.minecraft.world.item.Items.IRON_PICKAXE)||a.is(net.minecraft.world.item.Items.DIAMOND_PICKAXE)||a.is(net.minecraft.world.item.Items.NETHERITE_PICKAXE);
    }
    private static boolean upgraded(net.minecraft.world.item.ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.STONE_PICKAXE)||stack.is(net.minecraft.world.item.Items.IRON_PICKAXE)
            ||stack.is(net.minecraft.world.item.Items.DIAMOND_PICKAXE)||stack.is(net.minecraft.world.item.Items.NETHERITE_PICKAXE);
    }
    static Villager nominate(ServerLevel level) {
        boolean nativeMap = NativeRegionsScenario.nativeRegions(level);
        if (!WarCampaign.ready(level) && !nativeMap) return null;
        Path record = level.getServer().getWorldPath(LevelResource.ROOT).resolve("villagers/adventure-objective.json");
        if (nativeMap && Files.exists(record)) {
            try {
                var data = JsonParser.parseString(Files.readString(record)).getAsJsonObject();
                var entity = level.getEntity(UUID.fromString(data.get("villager").getAsString()));
                // Unloaded/dead is not permission to silently replace the recorded hero or their history.
                if (entity instanceof Villager v && v.isAlive() && Villagers.isManaged(v)) {
                    if (!nominated(v)) { v.addTag(NOMINATED); Villagers.session(v,level).mind.remember("Resumed my recorded adventure objective"); }
                    return v;
                }
                return null;
            } catch (Exception error) { Villagers.LOG.error("Native adventure objective cannot be read; preserving it",error); return null; }
        }
        var villagers = java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(), false)
            .filter(e -> e instanceof Villager v && v.isAlive() && !v.isBaby() && Villagers.isManaged(v))
            .map(e -> (Villager)e).toList();
        for (var v : villagers) if (nominated(v)) return v;
        Villager chosen = villagers.stream().filter(v -> {
            var s = Villagers.existingSession(v.getUUID());
            return s != null && v.getHealth() >= .9*v.getMaxHealth() && s.mind.foodLevel() >= 14 && s.mind.fatigue() < .6
                && (!nativeMap || AdventureActions.hasHoe(v) && FoodSupply.deficit(v.getInventory(),true)==0);
        }).max(Comparator.comparingDouble(v -> Villagers.existingSession(v.getUUID()).mind.genome().tendency(Tendency.BOLDNESS)
            + (v.getMainHandItem().is(ItemTags.SWORDS) ? .5 : 0))).orElse(null);
        if (chosen != null) {
            if (nativeMap) {
                try {
                    Files.createDirectories(record.getParent());
                    var data = new JsonObject(); data.addProperty("schema",1); data.addProperty("objective","Defeat the Ender Dragon");
                    data.addProperty("villager",chosen.getUUID().toString()); data.addProperty("nominatedGameTime",level.getGameTime());
                    data.addProperty("state","preparing"); data.addProperty("suppliedItems",0);
                    var temporary = record.resolveSibling("adventure-objective.tmp");
                    Files.writeString(temporary,new GsonBuilder().setPrettyPrinting().create().toJson(data));
                    Files.move(temporary,record,StandardCopyOption.ATOMIC_MOVE);
                } catch (Exception error) { Villagers.LOG.error("Native adventure nomination was not committed",error); return null; }
            }
            chosen.addTag(NOMINATED);
            var s = Villagers.existingSession(chosen.getUUID());
            s.mind.remember("Nominated to prepare for defeating the Ender Dragon");
            Villagers.persist(s);
            if (!nativeMap) WarCampaign.nominated(level, chosen);
        }
        return chosen;
    }
    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("adventure").then(Commands.literal("nominate").executes(c -> {
                var v = nominate(c.getSource().getLevel());
                if (v == null) { c.getSource().sendFailure(Component.literal("No ready loaded adult villager; native adventurers need an owned hoe and food reserve. Existing objectives are preserved.")); return 0; }
                c.getSource().sendSuccess(() -> Component.literal("Adventure assigned to " + v.getUUID() + "; using existing resources."), false);
                return 1;
            }))));
    }
    static String work(ServerLevel level, Villager v, Session s, long now) {
        if (!nominated(v)) return null;
        if (v.isBaby() || v.isSleeping() || v.isTrading() || v.isOnFire() || v.isInWater() || level.isDarkOutside()
            || s.mind.foodLevel() < 14 || v.getHealth() < .9*v.getMaxHealth() || s.mind.fatigue() >= .6) {
            s.adventureStatus = "recovering before adventure preparation"; return null;
        }
        if(stonePickaxe(v)){s.woodBootstrap.goal=null;var iron=AdventureIron.work(level,v,s,now);s.adventureStatus=s.iron.activity;return iron;}
        if (pickaxe(v) && WoodCrafting.stoneCount(v.getInventory())<3) {
            var mining=AdventureMining.work(level,v,s,now);
            if(mining==null&&s.mining.target==null){
                var wood=WoodBootstrap.prepareUpgradeWood(level,v,s,now);
                if(wood!=null){s.adventureStatus=s.woodBootstrap.status;return wood;}
                var quarry=AdventureQuarry.work(level,v,s,now);
                if(quarry!=null){s.adventureStatus=s.quarry.activity;return quarry;}
            }
            s.adventureStatus=s.mining.status;return mining;
        }
        var work = WoodBootstrap.work(level,v,s,pickaxe(v)?WoodBootstrap.Goal.STONE_PICKAXE:WoodBootstrap.Goal.PICKAXE,now);
        s.adventureStatus = s.woodBootstrap.status;
        return work;
    }
}
