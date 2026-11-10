package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.illager.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

/** New isolated production fixture: empty founders, existing fields/tables, ordinary controllers. */
public final class IllagerFoodServerProbe implements ModInitializer {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private boolean started, finished, restart;
    private long start;
    private UUID farmerId, dependentId;
    private JsonObject previous;
    private boolean farmerAte, dependentAte;
    private final boolean endurance = Boolean.getBoolean("villagers.food.endurance");
    private final boolean families = Boolean.getBoolean("villagers.food.families");
    private final boolean lifecycle = Boolean.getBoolean("villagers.food.lifecycle");
    private final Set<UUID> children = new HashSet<>();
    private final Map<UUID,Integer> childFood = new HashMap<>();
    private final Set<UUID> fedChildren = new HashSet<>();
    private int farmerMeals, dependentMeals, lastFarmerFood = 16, lastDependentFood = 16;
    private long farmerAgeStart = -1, dependentAgeStart = -1;
    private final Set<UUID> loaded = new HashSet<>();
    private int minFood = 20;
    private static void check(boolean value, String detail) { if (!value) throw new AssertionError(detail); }
    private static JsonElement stack(Mob mob, ItemStack item) {
        return item.isEmpty() ? new JsonObject() : ItemStack.CODEC.encodeStart(
            mob.level().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), item).getOrThrow();
    }
    private static JsonObject body(Mob mob) {
        var out = new JsonObject();
        out.addProperty("id", mob.getUUID().toString()); out.addProperty("health", mob.getHealth());
        out.addProperty("x", mob.getX()); out.addProperty("y", mob.getY()); out.addProperty("z", mob.getZ());
        out.addProperty("offhand", mob.getOffhandItem().toString());
        out.addProperty("mainhand", mob.getMainHandItem().toString());
        out.add("offhandStack", stack(mob, mob.getOffhandItem())); out.add("mainhandStack", stack(mob, mob.getMainHandItem()));
        if (mob instanceof Pillager pillager) out.add("bag", JSON.toJsonTree(HarvestMaterials.snapshot(pillager.getInventory()).stream().map(ItemStack::toString).toList()));
        if (mob instanceof Pillager pillager) {
            var slots = new JsonArray(); for (var item : HarvestMaterials.snapshot(pillager.getInventory())) slots.add(stack(mob, item));
            out.add("bagStacks", slots);
        }
        return out;
    }
    private static void compare(Mob mob, JsonObject saved) {
        var current = body(mob);
        for (String key : List.of("health", "x", "y", "z", "offhand", "mainhand", "offhandStack", "mainhandStack"))
            check(current.get(key).equals(saved.get(key)), "body persistence " + key);
        if (current.has("bagStacks")) check(totals(current.getAsJsonArray("bagStacks")).equals(totals(saved.getAsJsonArray("bagStacks"))),
            "bag item totals and components persist, allowing native slot compaction");
    }
    private static Map<String, Integer> totals(JsonArray slots) {
        var result = new TreeMap<String, Integer>();
        for (var slot : slots) {
            var item = slot.getAsJsonObject().deepCopy();
            if (!item.has("id")) continue;
            int count = item.remove("count").getAsInt();
            result.merge(item.toString(), count, Integer::sum);
        }
        return result;
    }
    @Override public void onInitialize() {
        // Validate before either body gets its first AI tick on restart.
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (!(entity instanceof Mob mob)) return;
            Path before = level.getServer().getWorldPath(LevelResource.ROOT).resolve("illager-food-before-restart.json");
            if (!Files.exists(before)) {
                if (Files.exists(before.resolveSibling("illager-food-before-continuation.json"))) return;
                // Clear candidate fixture rations at load, before any native AI tick can eat them.
                if (mob.entityTags().contains(WarScenario.HOME + 1) && !mob.entityTags().contains(WarScenario.SOLDIER)
                    && (mob instanceof Pillager || mob instanceof Vindicator)) {
                    mob.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                    if (mob instanceof Pillager pillager) pillager.getInventory().clearContent();
                }
                return;
            }
            try {
                var prior = JsonParser.parseString(Files.readString(before)).getAsJsonObject();
                if (families && prior.has("children")) for (var id : prior.getAsJsonArray("children")) {
                    if (id.getAsString().equals(mob.getUUID().toString())) {
                        check(CreatureAgents.managed(mob), "same saved managed child body");
                        var mindPath = level.getServer().getWorldPath(LevelResource.ROOT).resolve("creature-minds")
                            .resolve(mob.getUUID().toString()).resolve("mind.json");
                        var savedMind = JsonParser.parseString(Files.readString(mindPath)).getAsJsonObject();
                        boolean young = savedMind.get("ageTicks").getAsLong() < 24000;
                        check(IllagerFamilies.juvenile(mob) == young, "saved juvenile marker agrees with age");
                        check(mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE) == (young ? .5 : 1), "saved native juvenile scale");
                        loaded.add(mob.getUUID());
                    }
                }
                for (String role : List.of("farmer", "dependent")) {
                    var saved = prior.getAsJsonObject(role);
                    if (saved.get("id").getAsString().equals(mob.getUUID().toString())) {
                        compare(mob, saved);
                        var mindFile = level.getServer().getWorldPath(LevelResource.ROOT).resolve("creature-minds")
                            .resolve(mob.getUUID().toString()).resolve("mind.json");
                        var mind = JsonParser.parseString(Files.readString(mindFile)).getAsJsonObject();
                        check(mind.get("food").equals(saved.get("nutrition")), "nutrition and exhaustion persist at entity load");
                        loaded.add(mob.getUUID());
                    }
                }
            } catch (Throwable failure) { throw new AssertionError("Restart load verification", failure); }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (finished) return;
            var level = server.overworld();
            Path result = server.getWorldPath(LevelResource.ROOT).resolve("illager-food-result.json");
            try {
                if (!WarCampaign.ready(level)) return;
                if (!started) {
                    started = true; start = level.getGameTime(); server.tickRateManager().setTickRate(80);
                    var before = result.resolveSibling("illager-food-before-restart.json");
                    restart = Files.exists(before);
                    var continuation = result.resolveSibling("illager-food-before-continuation.json");
                    if (restart || Files.exists(continuation)) {
                        if (!restart) before = continuation;
                        previous = JsonParser.parseString(Files.readString(before)).getAsJsonObject();
                        farmerId = UUID.fromString(previous.getAsJsonObject("farmer").get("id").getAsString());
                        dependentId = UUID.fromString(previous.getAsJsonObject("dependent").get("id").getAsString());
                        if (!restart) {
                            minFood = previous.get("minFood").getAsInt();
                            lastFarmerFood = lastDependentFood = -1;
                        }
                    } else {
                        // Boundary checks use an unspawned scratch body and one explicitly protected crop.
                        var scratch = new Pillager(EntityTypes.PILLAGER, level);
                        scratch.addTag(WarScenario.ACTOR); scratch.addTag(WarScenario.HOME + 1); scratch.addTag(CreatureAgents.MANAGED);
                        scratch.getInventory().addItem(new ItemStack(net.minecraft.world.item.Items.WHEAT_SEEDS, 1));
                        var crop = new net.minecraft.core.BlockPos(104, 75, -92);
                        scratch.snapTo(104.5, 75, -91.5, 0, 0);
                        check(IllagerFood.harvest(level, scratch, crop), "hand harvest pays an owned seed");
                        check(level.getBlockState(crop).getValue(net.minecraft.world.level.block.CropBlock.AGE) == 0, "age-zero replant");
                        var bag = HarvestMaterials.snapshot(scratch.getInventory());
                        check(!IllagerFood.harvest(level, scratch, crop), "immature crop rejected");
                        check(HarvestMaterials.unchanged(scratch.getInventory(), bag), "rejected harvest changes no items");
                        var protectedCrop = crop.offset(1, 0, 0);
                        var operator = new net.minecraft.server.level.ServerPlayer(server, level,
                            new com.mojang.authlib.GameProfile(new UUID(0x494c4c4147455246L, 1), "FoodOperator"),
                            net.minecraft.server.level.ClientInformation.createDefault()) {
                                @Override public void sendSystemMessage(net.minecraft.network.chat.Component message) {
                                    System.out.println("FOOD OPERATOR: " + message.getString());
                                }
                            };
                        var source = operator.createCommandSourceStack().withPermission(server.createCommandSourceStack().permissions());
                        server.getCommands().performPrefixedCommand(source, "villagers site protect 105 74 -92 105 75 -92");
                        check(ConstructionSites.isProtected(level, protectedCrop), "native protection registered");
                        check(!IllagerFood.harvest(level, scratch, protectedCrop), "protected crop rejected");
                        check(HarvestMaterials.unchanged(scratch.getInventory(), bag), "protection charges no items");
                        var seedWorker = new Pillager(EntityTypes.PILLAGER, level);
                        seedWorker.addTag(WarScenario.ACTOR); seedWorker.addTag(WarScenario.HOME + 1); seedWorker.addTag(CreatureAgents.MANAGED);
                        seedWorker.snapTo(104.5, 75, -93.5, 0, 0);
                        seedWorker.getInventory().setItem(0, new ItemStack(net.minecraft.world.item.Items.WHEAT_SEEDS, 64));
                        seedWorker.getInventory().setItem(1, new ItemStack(net.minecraft.world.item.Items.WHEAT_SEEDS, 6));
                        check(seedWorker.getInventory().countItem(net.minecraft.world.item.Items.WHEAT_SEEDS) == 70, "surplus seed fixture has two actual stacks");
                        check(IllagerFood.harvest(level, seedWorker, crop.north()), "surplus seed fixture harvest succeeds");
                        check(seedWorker.getInventory().countItem(net.minecraft.world.item.Items.WHEAT_SEEDS) == 64, "one seed stack leaves food production capacity");
                        int droppedSeeds = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                            new net.minecraft.world.phys.AABB(crop.north()).inflate(4), e -> e.getItem().is(net.minecraft.world.item.Items.WHEAT_SEEDS))
                            .stream().mapToInt(e -> e.getItem().getCount()).sum();
                        check(droppedSeeds >= 5, "real excess seeds are placed on the ground rather than destroyed");
                        var founders = new ArrayList<Mob>();
                        for (var entity : level.getAllEntities()) if (entity instanceof Mob mob && mob.entityTags().contains(WarScenario.ACTOR)) founders.add(mob);
                        Pillager farmer = new Pillager(EntityTypes.PILLAGER, level);
                        Mob dependent = families ? new Pillager(EntityTypes.PILLAGER, level) : new Vindicator(EntityTypes.VINDICATOR, level);
                        farmerId = farmer.getUUID(); dependentId = dependent.getUUID();
                        // Explicit fixture: two new empty founders cannot have eaten scenario rations.
                        for (Mob mob : founders) mob.discard();
                        var dropped = new ArrayList<net.minecraft.world.entity.item.ItemEntity>();
                        for (var entity : level.getAllEntities()) if (entity instanceof net.minecraft.world.entity.item.ItemEntity item) dropped.add(item);
                        for (var item : dropped) item.discard();
                        farmer.snapTo(104, 75, -89, 0, 0); dependent.snapTo(107, 75, -89, 0, 0);
                        for (Mob mob : List.of(farmer, dependent)) {
                            mob.addTag(WarScenario.ACTOR); mob.addTag(WarScenario.HOME + 1);
                            CreatureAgents.adopt(level, mob); check(level.addFreshEntity(mob), "empty fixture founder spawned");
                            check(CreatureAgents.mind(mob).foodLevel() == 16, "uneaten initial nutrition");
                        }
                    }
                }
                var farmer = (Pillager)level.getEntity(farmerId);
                var dependent = (Mob)level.getEntity(dependentId);
                if ((farmer == null || dependent == null) && level.getGameTime() - start < 1200) return;
                check(farmer != null && farmer.isAlive() && dependent != null && dependent.isAlive(), "both live bodies present");
                var fm = CreatureAgents.mind(farmer); var dm = CreatureAgents.mind(dependent);
                if (fm == null || dm == null) return;
                if (farmerAgeStart < 0) { farmerAgeStart = fm.ageTicks(); dependentAgeStart = dm.ageTicks(); }
                long loadedTicks = Math.min(fm.ageTicks() - farmerAgeStart, dm.ageTicks() - dependentAgeStart);
                if (families) {
                    if (restart && previous.has("children")) for (var id : previous.getAsJsonArray("children")) {
                        if (level.getEntity(UUID.fromString(id.getAsString())) == null && level.getGameTime() - start < 1200) return;
                        check(level.getEntity(UUID.fromString(id.getAsString())) instanceof AbstractIllager, "original child present after restart");
                    }
                    for (var entity : level.getAllEntities()) if (entity instanceof AbstractIllager child
                        && !child.getUUID().equals(farmerId) && !child.getUUID().equals(dependentId) && CreatureAgents.managed(child)) {
                        var cm = CreatureAgents.mind(child); if (cm == null) continue;
                        check(cm.lineage() != null && cm.lineage().parents().equals(Set.of(farmerId, dependentId)), "ordinary two-parent ancestry");
                        check(!cm.lineage().godChild() && !child.entityTags().contains(WarScenario.SOLDIER), "ordinary civilian child");
                        check(child.isAlive() && child.getHealth() == child.getMaxHealth() && cm.foodLevel() > 0, "child survives without hunger or damage");
                        children.add(child.getUUID());
                        Integer oldFood = childFood.put(child.getUUID(), cm.foodLevel());
                        if (oldFood != null && cm.foodLevel() > oldFood) fedChildren.add(child.getUUID());
                    }
                }
                minFood = Math.min(minFood, Math.min(fm.foodLevel(), dm.foodLevel()));
                if (lastFarmerFood >= 0 && fm.foodLevel() > lastFarmerFood) farmerMeals++;
                if (lastDependentFood >= 0 && dm.foodLevel() > lastDependentFood) dependentMeals++;
                lastFarmerFood = fm.foodLevel(); lastDependentFood = dm.foodLevel();
                check(minFood > 0, "no starvation interval");
                check(farmer.getHealth() == farmer.getMaxHealth() && dependent.getHealth() == dependent.getMaxHealth(), "no fixture damage");
                var activity = CreatureAgents.status(farmer);
                var work = (Map<?, ?>)activity.get("foodWork");
                farmerAte |= fm.memories().stream().anyMatch(m -> m.note().equals("Ate owned food"));
                dependentAte |= dm.memories().stream().anyMatch(m -> m.note().equals("Ate owned food"));
                if (server.getTickCount() % 400 == 0) {
                    Files.writeString(result.resolveSibling("illager-food-progress.json"), JSON.toJson(Map.of(
                        "elapsed", level.getGameTime() - start, "farmer", body(farmer), "dependent", body(dependent),
                        "activity", activity, "farmerFood", fm.foodLevel(), "dependentFood", dm.foodLevel(), "minFood", minFood,
                        "farmerMeals", farmerMeals, "dependentMeals", dependentMeals, "loadedTicks", loadedTicks)));
                }
                boolean pass = restart ? loaded.containsAll(Set.of(farmerId, dependentId)) && level.getGameTime() - start >= 100
                    : ((Number)work.get("harvested")).intValue() >= 12 && ((Number)work.get("crafted")).intValue() >= 4
                        && ((Number)work.get("shared")).intValue() >= 1 && farmerAte && dependentAte;
                if (families && !restart) pass = !children.isEmpty() && farmerAte && dependentAte;
                if (families && restart) pass &= !children.isEmpty() && previous.getAsJsonArray("children").asList().stream()
                    .allMatch(id -> loaded.contains(UUID.fromString(id.getAsString())));
                if (lifecycle) pass &= previous.getAsJsonArray("children").asList().stream().allMatch(id -> {
                    var child = (Mob)level.getEntity(UUID.fromString(id.getAsString()));
                    var mind = CreatureAgents.mind(child);
                    return mind != null && mind.ageTicks() >= 24000 && fedChildren.contains(child.getUUID()) && !IllagerFamilies.juvenile(child)
                        && child.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE) == 1;
                });
                if (endurance && !restart) pass &= level.getGameTime() - start >= 24000
                    && loadedTicks >= 24000
                    && ((Number)work.get("repeatHarvests")).intValue() > 0 && farmerMeals >= 2 && dependentMeals >= 2;
                if (pass) {
                    var out = new JsonObject(); out.addProperty("status", "passed"); out.addProperty("restart", restart);
                    out.addProperty("gameTime", level.getGameTime()); out.addProperty("minFood", minFood);
                    out.addProperty("endurance", endurance); out.addProperty("farmerMeals", farmerMeals); out.addProperty("dependentMeals", dependentMeals);
                    out.addProperty("families", families); out.add("children", JSON.toJsonTree(children));
                    out.addProperty("lifecycle", lifecycle);
                    out.add("fedChildren", JSON.toJsonTree(fedChildren));
                    var fb = body(farmer); fb.add("nutrition", JSON.toJsonTree(fm.snapshot().food()));
                    var db = body(dependent); db.add("nutrition", JSON.toJsonTree(dm.snapshot().food()));
                    out.add("farmer", fb); out.add("dependent", db); out.add("work", JSON.toJsonTree(work));
                    out.addProperty("farmerProducedMeal", farmerAte || restart && previous.get("farmerProducedMeal").getAsBoolean());
                    out.addProperty("dependentProducedMeal", dependentAte || restart && previous.get("dependentProducedMeal").getAsBoolean());
                    out.addProperty("fixture", "Two new empty illager founders in the existing authored town; scenario founders and loose items discarded at setup to exclude starting rations. Existing ripe fields and public house tables. Unspawned scratch body with one test seed checks paid harvest, immature rejection and an operator-protected crop. No supplies, damage, body, time or growth setters after setup; normal exhaustion at 80TPS. Production/restart check, not indefinite sustainability or population growth.");
                    Files.writeString(result, JSON.toJson(out)); finished = true; server.halt(false);
                } else check(level.getGameTime() - start < (endurance ? 144000 : 24000), "food observation exhausted");
            } catch (Throwable failure) {
                failure.printStackTrace(); finished = true;
                try { Files.writeString(result, JSON.toJson(Map.of("status", "failed", "failure", failure.toString()))); } catch (Exception ignored) {}
                server.halt(false);
            }
        });
    }
}
