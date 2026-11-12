package local.villagers;

import local.villagers.capability.CapabilityPermissions;
import local.villagers.capability.CapabilityPermissions.Capability;
import local.villagers.capability.DefensePolicy;
import local.villagers.capability.CropPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Server-thread-only interactions. Only ripe, immediately replanted crops may change block age. */
public final class AdventureActions {
    private static final Logger LOG = LoggerFactory.getLogger("villagers-adventure");
    public static final CapabilityPermissions PERMISSIONS = CapabilityPermissions.adventure();
    private static final String MANAGED = "villagers:managed";
    private static final int MAX_DOORS = 4;
    private static final double DOOR_REACH_SQUARED = 6.25;
    private static final Map<UUID, ActorState> ACTORS = new HashMap<>();

    private static final class ActorState {
        long equipCheckAt, weaponReadyAt, attackReadyAt, doorCheckAt, harvestReadyAt;
        long doorsOpened, doorsClosed, swordHits, cropsHarvested;
        long replantsFromHarvest,replantsFromStock;
        Map<String,Object> lastHarvest=Map.of();
        final LinkedHashMap<BlockPos, OpenedDoor> doors = new LinkedHashMap<>();
    }
    private record OpenedDoor(DoorBlock block, Direction facing, double entrySide, long openedAt) {}

    private AdventureActions() {}
    private static boolean managed(Villager villager) { return villager.isAlive() && villager.entityTags().contains(MANAGED); }
    private static ActorState state(Villager villager) { return ACTORS.computeIfAbsent(villager.getUUID(), ignored -> new ActorState()); }

    /** Idempotent. Prefers a real sword; any displaced actual item is safely stored before switching. */
    public static void prepare(ServerLevel level, Villager villager) {
        if (!managed(villager) || villager.level() != level) return;
        villager.getNavigation().setCanOpenDoors(PERMISSIONS.allows(Capability.OPEN_DOOR));
        villager.getNavigation().getNodeEvaluator().setCanPassDoors(true);
        ActorState actor = state(villager);
        long now = level.getGameTime();
        if (now < actor.equipCheckAt || sword(villager.getMainHandItem())) return;
        actor.equipCheckAt = now + 20;
        if (!PERMISSIONS.allows(Capability.EQUIP_ITEM)) return;
        int preferred = inventoryTool(villager, true);
        if (preferred >= 0) switchTool(level, villager, preferred, actor);
        else if (villager.getMainHandItem().isEmpty() && !SoilWork.wantsBareHand(level,villager)
                && !ForestryWork.wantsBareHand(level,villager)) {
            int hoe = inventoryTool(villager, false);
            if (hoe >= 0) switchTool(level, villager, hoe, actor);
        }
    }

    public static boolean acceptsTool(ItemStack stack) {
        return sword(stack) || hoe(stack) || usable(stack) && stack.is(ItemTags.PICKAXES);
    }

    /** Equipment worth keeping from real ground drops, including tools not yet used by work AI. */
    public static boolean acceptsDroppedEquipment(ItemStack stack) {
        return usable(stack) && (acceptsTool(stack) || stack.has(DataComponents.TOOL)
            || stack.has(DataComponents.WEAPON) || stack.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem
            || stack.is(Items.SHEARS) || stack.is(Items.FISHING_ROD) || stack.is(Items.FLINT_AND_STEEL)
            || stack.is(Items.BRUSH) || stack.is(Items.SHIELD));
    }
    /** Equip one actual possessed pickaxe; the displaced held item uses the same conserved swap as a hoe. */
    static boolean preparePickaxeForMining(ServerLevel level,Villager v,BlockState block) {
        if (!managed(v) || v.level()!=level || !PERMISSIONS.allows(Capability.EQUIP_ITEM)) return false;
        double speed = usable(v.getMainHandItem()) && v.getMainHandItem().is(ItemTags.PICKAXES)
            && (!block.requiresCorrectToolForDrops()||v.getMainHandItem().isCorrectToolForDrops(block)) ? v.getMainHandItem().getDestroySpeed(block) : -1;
        int remaining=speed<0?-1:AdventurePreparation.toolUses(v.getMainHandItem());
        int best=-1;
        for(int i=0;i<v.getInventory().getContainerSize();i++) {
            var stack=v.getInventory().getItem(i);
            if(usable(stack)&&stack.is(ItemTags.PICKAXES)&&(!block.requiresCorrectToolForDrops()||stack.isCorrectToolForDrops(block))
                &&preferPickaxe(stack.getDestroySpeed(block),AdventurePreparation.toolUses(stack),speed,remaining)) {
                best=i;speed=stack.getDestroySpeed(block);remaining=AdventurePreparation.toolUses(stack);
            }
        }
        var offhand=v.getOffhandItem();
        if(usable(offhand)&&offhand.is(ItemTags.PICKAXES)&&(!block.requiresCorrectToolForDrops()||offhand.isCorrectToolForDrops(block))
            &&preferPickaxe(offhand.getDestroySpeed(block),AdventurePreparation.toolUses(offhand),speed,remaining)){
            switchOffhand(level,v);return true;
        }
        return best>=0 ? switchTool(level,v,best,state(v)) : speed>=0;
    }
    static boolean preferPickaxe(double speed,int uses,double currentSpeed,int currentUses){
        if(currentSpeed<0)return true;
        if((uses>8)!=(currentUses>8))return uses>8;
        return speed>currentSpeed||speed==currentSpeed&&uses>currentUses;
    }

    /** Store the actual held hoe/item; a real weapon is never put away for soil work. */
    static boolean prepareBareHandForSoil(ServerLevel level,Villager v) {
        if(!managed(v) || v.level()!=level || !PERMISSIONS.allows(Capability.EQUIP_ITEM))return false;
        var held=v.getMainHandItem();if(held.isEmpty())return true;if(sword(held))return false;
        for(int i=0;i<v.getInventory().getContainerSize();i++)if(v.getInventory().getItem(i).isEmpty()) {
            v.getInventory().setItem(i,held);equip(level,v,ItemStack.EMPTY,state(v));return true;
        }
        return false;
    }

    private static boolean usable(ItemStack stack) {
        return stack != null && !stack.isEmpty()
            && (!stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage());
    }
    private static boolean sword(ItemStack stack) { return usable(stack) && stack.is(ItemTags.SWORDS) && stack.has(DataComponents.WEAPON); }
    private static boolean hoe(ItemStack stack) { return usable(stack) && stack.is(ItemTags.HOES); }
    static boolean prepareHoeForWork(ServerLevel level, Villager v) {
        if (hoe(v.getMainHandItem())) return true;
        if(hoe(v.getOffhandItem())&&managed(v)&&v.level()==level&&PERMISSIONS.allows(Capability.EQUIP_ITEM)){switchOffhand(level,v);return true;}
        int slot = inventoryTool(v, false);
        return slot >= 0 && switchTool(level, v, slot, state(v));
    }
    static boolean prepareShears(ServerLevel level,Villager v){
        if(!managed(v)||v.level()!=level||!PERMISSIONS.allows(Capability.EQUIP_ITEM))return false;
        if(v.getMainHandItem().is(Items.SHEARS)&&usable(v.getMainHandItem()))return true;
        if(v.getOffhandItem().is(Items.SHEARS)&&usable(v.getOffhandItem())){switchOffhand(level,v);return true;}
        for(int i=0;i<v.getInventory().getContainerSize();i++)if(v.getInventory().getItem(i).is(Items.SHEARS)&&usable(v.getInventory().getItem(i)))return switchTool(level,v,i,state(v));
        return false;
    }
    private static int inventoryTool(Villager villager, boolean sword) {
        for (int slot = 0; slot < villager.getInventory().getContainerSize(); slot++) {
            ItemStack item = villager.getInventory().getItem(slot);
            if (sword ? sword(item) : hoe(item)) return slot;
        }
        return -1;
    }

    /** A count-one tool frees its own inventory slot for the displaced equipment, even when full. */
    private static boolean switchTool(ServerLevel level, Villager villager, int slot, ActorState actor) {
        ItemStack selected = villager.getInventory().getItem(slot);
        if (!acceptsTool(selected)) return false;
        ItemStack previous = villager.getMainHandItem();
        int storage = selected.getCount() == 1 ? slot : -1;
        if (!previous.isEmpty() && storage < 0) {
            for (int i = 0; i < villager.getInventory().getContainerSize(); i++) {
                if (villager.getInventory().getItem(i).isEmpty()) { storage = i; break; }
            }
            if (storage < 0) return false;
        }
        ItemStack actualItem = villager.getInventory().removeItem(slot, 1);
        if (!previous.isEmpty()) villager.getInventory().setItem(storage, previous);
        equip(level, villager, actualItem, actor);
        return true;
    }

    /** Display one actually possessed example; the displaced hand goes into the selected stack's freed slot. */
    static boolean displayForLesson(ServerLevel level,Villager v,int slot){
        if(!managed(v)||v.level()!=level||!PERMISSIONS.allows(Capability.EQUIP_ITEM)||v.isSleeping()||v.isTrading()
                ||slot<0||slot>=v.getInventory().getContainerSize())return false;
        var selected=v.getInventory().getItem(slot);if(selected.isEmpty())return false;
        var previous=v.getMainHandItem();
        v.getInventory().setItem(slot,previous);
        equip(level,v,selected,state(v));return true;
    }

    /** Real sneak-right-click handoff; success consumes the interaction and exactly one existing item. */
    public static boolean offerTool(ServerLevel level, Villager villager, Player player, InteractionHand hand) {
        if (!managed(villager) || villager.level() != level || player.level() != level || !player.isShiftKeyDown()
                || !player.isAlive() || player.isSpectator() || player.distanceToSqr(villager) > 9
                || !player.hasLineOfSight(villager)
                || !PERMISSIONS.allows(Capability.EQUIP_ITEM)) return false;
        ItemStack offered = player.getItemInHand(hand);
        if (!acceptsTool(offered)) return false;
        boolean directlyEquip = villager.getMainHandItem().isEmpty();
        if (!directlyEquip && !villager.getInventory().canAddItem(offered.copyWithCount(1))) return false;
        ItemStack actualItem = offered.split(1); // Creative donors also transfer a real existing item.
        String itemId = BuiltInRegistries.ITEM.getKey(actualItem.getItem()).toString();
        if (directlyEquip) equip(level, villager, actualItem, state(villager));
        else {
            ItemStack remaining = villager.getInventory().addItem(actualItem);
            if (!remaining.isEmpty()) { offered.grow(remaining.getCount()); return false; }
        }
        LOG.info("Tool handoff intervention: villager={} item={} count=1 destination={}", villager.getUUID(), itemId,
            directlyEquip ? "mainhand" : "inventory");
        return true;
    }

    private static void equip(ServerLevel level, Villager villager, ItemStack actualItem, ActorState actor) {
        villager.setItemSlot(EquipmentSlot.MAINHAND, actualItem);
        villager.setGuaranteedDrop(EquipmentSlot.MAINHAND);
        // LivingEntity applies equipped attribute modifiers on its next equipment update.
        actor.weaponReadyAt = level.getGameTime() + 1;
    }
    private static void switchOffhand(ServerLevel level,Villager v){
        var previous=v.getMainHandItem();var actual=v.getOffhandItem();
        v.setItemSlot(EquipmentSlot.OFFHAND,previous);v.setGuaranteedDrop(EquipmentSlot.OFFHAND);equip(level,v,actual,state(v));
    }

    /** Attempts one stationary defensive strike. true means native damage succeeded; never starts pursuit. */
    public static boolean tryDefend(ServerLevel level, Villager villager, LivingEntity threat) {
        if (!managed(villager) || villager.level() != level || threat == null || threat.level() != level
                || !threat.isAlive() || villager.isSleeping() || villager.isTrading()
                || !PERMISSIONS.allows(Capability.DEFENSIVE_MELEE)) return false;
        boolean hostile = threat instanceof Monster monster && monster.getTarget() == villager;
        boolean recentAttack = villager.getLastHurtByMob() == threat
            && villager.tickCount - villager.getLastHurtByMobTimestamp() < 100;
        if (!DefensePolicy.mayStrike(sword(villager.getMainHandItem()),
                threat instanceof Monster ? DefensePolicy.TargetKind.MONSTER : DefensePolicy.TargetKind.OTHER,
                hostile, recentAttack, villager.hasLineOfSight(threat), villager.isWithinMeleeAttackRange(threat))) return false;
        if (!villager.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)
                || !villager.getAttributes().hasAttribute(Attributes.ATTACK_SPEED) || !villager.canAttack(threat)) return false;
        double attackSpeed = villager.getAttributeValue(Attributes.ATTACK_SPEED);
        if (!Double.isFinite(attackSpeed) || attackSpeed <= 0) return false;
        ActorState actor = state(villager);
        long now = level.getGameTime();
        if (now < actor.weaponReadyAt || now < actor.attackReadyAt) return false;
        actor.attackReadyAt = now + DefensePolicy.cooldownTicks(attackSpeed);
        villager.getLookControl().setLookAt(threat, 30, 30);
        villager.swingForAttack(InteractionHand.MAIN_HAND);
        ItemStack actualWeapon = villager.getMainHandItem();
        boolean hit = villager.doHurtTarget(level, threat);
        if (hit) {
            actor.swordHits++;
            // Mob.doHurtTarget invokes hurtEnemy but omits the player's postHurtEnemy durability step.
            // Use that native step once, retaining enchantment/weapon component behavior and break events.
            actualWeapon.postHurtEnemy(threat, villager);
            Villagers.exert(villager, local.villagers.mind.Metabolism.EXHAUSTION_ATTACK); // A player's attack cost.
        }
        return hit;
    }

    /** Native planted crops can be harvested by hand; planting payment is still mandatory. */
    public static boolean canHarvest(Villager villager) {
        return managed(villager) && PERMISSIONS.allows(Capability.HARVEST_REPLANT);
    }
    static boolean hasHoe(Villager villager) {
        return hoe(villager.getMainHandItem())||hoe(villager.getOffhandItem())||inventoryTool(villager,false)>=0;
    }

    /** Bounded radius-eight/y±three search. Root should cache results instead of scanning every tick. */
    public static BlockPos findHarvestTarget(ServerLevel level, Villager villager) {
        return findHarvestTarget(level, villager, true);
    }

    public static BlockPos findHarvestTarget(ServerLevel level, Villager villager, boolean allowWheat) {
        if (villager.level() != level || !canHarvest(villager)) return null;
        BlockPos origin = villager.blockPosition(), best = null, observed = null;
        double distance = Double.MAX_VALUE, observedDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -3, -8), origin.offset(8, 3, 8))) {
            int dx = pos.getX() - origin.getX(), dz = pos.getZ() - origin.getZ();
            if (!level.isLoaded(pos) || dx * dx + dz * dz > 64) continue;
            BlockState crop = level.getBlockState(pos);
            CropPolicy.CropKind kind = cropKind(crop);
            if (!(crop.getBlock() instanceof CropBlock plant) || kind == CropPolicy.CropKind.OTHER) continue;
            if (ConstructionSites.isProtected(level, pos) || ConstructionSites.isProtected(level, pos.below())) continue;
            double next = villager.position().distanceToSqr(Vec3.atCenterOf(pos));
            if (level.getBlockState(pos.below()).is(Blocks.FARMLAND) && next <= 64 && next < observedDistance) {
                observed = pos.immutable(); observedDistance = next;
            }
            if (!allowWheat && kind == CropPolicy.CropKind.WHEAT) continue;
            if (!CropPolicy.mayInspectHarvest(kind, plant.getAge(crop), plant.getMaxAge(), true,
                    level.getBlockState(pos.below()).is(Blocks.FARMLAND), true)) continue;
            if (next < distance) { best = pos.immutable(); distance = next; }
        }
        if (observed != null) FarmWorksite.observe(level, villager, observed);
        return best;
    }

    /** Server-thread receipt: reserve one real seed from stock/native loot, replant, then release real yield. */
    public static boolean tryHarvest(ServerLevel level, Villager villager, BlockPos pos) {
        if (pos != null && (ConstructionSites.isProtected(level, pos) || ConstructionSites.isProtected(level, pos.below()))) return false;
        if (pos == null || villager.level() != level || !canHarvest(villager) || villager.isTrading() || villager.isSleeping()
                || !level.isLoaded(pos) || villager.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > 9) return false;
        BlockState original = level.getBlockState(pos);
        CropPolicy.CropKind kind = cropKind(original);
        if (!(original.getBlock() instanceof CropBlock crop)) return false;
        if (!CropPolicy.mayInspectHarvest(kind, crop.getAge(original), crop.getMaxAge(), true,
                level.getBlockState(pos.below()).is(Blocks.FARMLAND), true)) return false;
        if(!harvestSight(level,villager,pos))return false;
        ActorState actor = state(villager);
        long now=level.getGameTime();if(now<actor.harvestReadyAt)return false;
        actor.harvestReadyAt=now+40;
        if (!hoe(villager.getMainHandItem())) {
            int tool = inventoryTool(villager, false);
            if (tool >= 0) { if(!switchTool(level, villager, tool, actor))return false; }
            else if(!prepareBareHandForSoil(level,villager))return false;
        }
        ItemStack realHoe = villager.getMainHandItem();
        var hoeBefore=realHoe.copy();var before=HarvestMaterials.snapshot(villager.getInventory());
        var soil=level.getBlockState(pos.below());
        var drops = Block.getDrops(original, level, pos, null, villager, realHoe);
        var plan=HarvestMaterials.reserve(villager.getInventory(),drops,plantingItem(kind));
        if(plan==null) { actor.harvestReadyAt=now+200;return false; }
        if(!CropPolicy.mayHarvest(kind,crop.getAge(original),crop.getMaxAge(),true,
                plan.planting==plantingItem(kind) ? kind.plantingMaterial() : CropPolicy.PlantMaterial.NONE,soil.is(Blocks.FARMLAND),true))return false;
        if(!HarvestMaterials.unchanged(villager.getInventory(),before) || !ItemStack.matches(villager.getMainHandItem(),hoeBefore)
                || !level.getBlockState(pos).equals(original) || !level.getBlockState(pos.below()).equals(soil)
                || !managed(villager) || villager.level()!=level || villager.isTrading() || villager.isSleeping()
                || !harvestSight(level,villager,pos) || ConstructionSites.isProtected(level,pos) || ConstructionSites.isProtected(level,pos.below()))return false;
        var replanted=crop.getStateForAge(0);
        if(!level.setBlock(pos,replanted,Block.UPDATE_ALL))return false;
        if(!managed(villager) || villager.level()!=level || villager.isTrading() || villager.isSleeping()
                || !ItemStack.matches(villager.getMainHandItem(),hoeBefore) || !level.getBlockState(pos).equals(replanted)
                || !level.getBlockState(pos.below()).equals(soil) || !harvestSight(level,villager,pos) || ConstructionSites.isProtected(level,pos)
                || ConstructionSites.isProtected(level,pos.below()) || !HarvestMaterials.commit(villager.getInventory(),plan)) {
            if(level.getBlockState(pos).equals(replanted))level.setBlock(pos,original,Block.UPDATE_ALL);
            return false;
        }
        for(var overflow:plan.overflow)Block.popResource(level,pos,overflow.copy());
        if(hoe(realHoe))realHoe.hurtAndBreak(1, villager, EquipmentSlot.MAINHAND);
        villager.swingForAttack(InteractionHand.MAIN_HAND);
        level.levelEvent(2001, pos, Block.getId(original));
        level.gameEvent(villager, GameEvent.BLOCK_CHANGE, pos);
        actor.cropsHarvested++;
        if(plan.fromDrops)actor.replantsFromHarvest++;else actor.replantsFromStock++;
        var receipt=new LinkedHashMap<String,Object>();receipt.put("gameTime",now);receipt.put("position",pos.toShortString());receipt.put("crop",kind.name());
        receipt.put("plantingSource",plan.fromDrops ? "native harvest" : "owned stock");receipt.put("nativeDrops",drops.stream().map(ItemStack::toString).toList());
        receipt.put("inventoryBefore",plan.before.stream().map(ItemStack::toString).toList());receipt.put("inventoryAfter",HarvestMaterials.snapshot(villager.getInventory()).stream().map(ItemStack::toString).toList());
        receipt.put("overflow",plan.overflow.stream().map(ItemStack::toString).toList());receipt.put("hoeDamageBefore",hoeBefore.getDamageValue());receipt.put("hoeDamageAfter",realHoe.getDamageValue());receipt.put("hoeBroke",hoe(hoeBefore)&&realHoe.isEmpty());
        receipt.put("usedHoe",hoe(hoeBefore));
        receipt.put("cropAfter",level.getBlockState(pos).toString());actor.lastHarvest=java.util.Collections.unmodifiableMap(receipt);
        var session=Villagers.existingSession(villager.getUUID());
        if(session!=null) { session.mind.remember("Harvested real "+kind+" at "+pos.toShortString()+"; replanted from "+(plan.fromDrops ? "native harvest" : "owned stock"));Villagers.persist(session); }
        int sword = inventoryTool(villager, true);
        if (sword >= 0) switchTool(level, villager, sword, actor);
        return true;
    }

    private static boolean harvestSight(ServerLevel level,Villager v,BlockPos pos) {
        if(!level.isLoaded(pos) || v.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos))>9)return false;
        var origin=BlockPos.containing(v.getEyePosition());
        for(int cx=Math.min(origin.getX()>>4,pos.getX()>>4);cx<=Math.max(origin.getX()>>4,pos.getX()>>4);cx++)
            for(int cz=Math.min(origin.getZ()>>4,pos.getZ()>>4);cz<=Math.max(origin.getZ()>>4,pos.getZ()>>4);cz++)
                if(!level.isLoaded(new BlockPos(cx<<4,pos.getY(),cz<<4)))return false;
        var hit=level.clip(new ClipContext(v.getEyePosition(),Vec3.atCenterOf(pos),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,v));
        return hit.getType()==HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    static CropPolicy.CropKind cropKind(BlockState state) {
        if (state.is(Blocks.WHEAT)) return CropPolicy.CropKind.WHEAT;
        if (state.is(Blocks.CARROTS)) return CropPolicy.CropKind.CARROT;
        if (state.is(Blocks.POTATOES)) return CropPolicy.CropKind.POTATO;
        if (state.is(Blocks.BEETROOTS)) return CropPolicy.CropKind.BEETROOT;
        return CropPolicy.CropKind.OTHER;
    }

    private static Item plantingItem(CropPolicy.CropKind kind) {
        return switch (kind) {
            case WHEAT -> Items.WHEAT_SEEDS;
            case CARROT -> Items.CARROT;
            case POTATO -> Items.POTATO;
            case BEETROOT -> Items.BEETROOT_SEEDS;
            case OTHER -> null;
        };
    }

    /** Call each managed tick, including while fleeing. Checks at most four nearby path nodes/remembered doors. */
    public static void tickDoors(ServerLevel level, Villager villager) {
        if (!managed(villager) || villager.level() != level || !PERMISSIONS.allows(Capability.OPEN_DOOR)) return;
        ActorState actor = state(villager);
        long now = level.getGameTime();
        if (now < actor.doorCheckAt) return;
        actor.doorCheckAt = now + 2;
        Path path = villager.getNavigation().getPath();
        if (path != null && !path.isDone() && !villager.isSleeping()) {
            int next = path.getNextNodeIndex();
            for (int index = Math.max(0, next - 1); index < Math.min(path.getNodeCount(), next + 3); index++) {
                BlockPos node = path.getNodePos(index);
                openOnPath(level, villager, actor, node, now);
                openOnPath(level, villager, actor, node.above(), now);
            }
        }
        if (PERMISSIONS.allows(Capability.CLOSE_DOOR)) closePassedDoors(level, villager, actor, path, now);
    }

    private static void openOnPath(ServerLevel level, Villager villager, ActorState actor, BlockPos node, long now) {
        if (!level.isLoaded(node)) return;
        BlockState block = level.getBlockState(node);
        if (!woodenDoor(block)) return;
        BlockPos pos = block.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? node.below() : node;
        block = level.getBlockState(pos);
        if (!woodenDoor(block) || block.getValue(DoorBlock.POWERED)
                || villager.position().distanceToSqr(Vec3.atCenterOf(pos)) > DOOR_REACH_SQUARED) return;
        var hit = level.clip(new ClipContext(villager.getEyePosition(), Vec3.atCenterOf(pos).add(0, .4, 0),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, villager));
        if (hit.getType() != HitResult.Type.MISS && !hit.getBlockPos().equals(pos) && !hit.getBlockPos().equals(pos.above())) return;
        DoorBlock door = (DoorBlock) block.getBlock();
        Direction facing = block.getValue(DoorBlock.FACING);
        if (actor.doors.containsKey(pos)) return;
        boolean wasOpen = block.getValue(DoorBlock.OPEN);
        if (!wasOpen) door.setOpen(villager, level, block, pos, true);
        BlockState openedState = level.getBlockState(pos);
        if (!openedState.is(door) || !openedState.getValue(DoorBlock.OPEN)) return;
        if (!wasOpen) actor.doorsOpened++;
        if (actor.doors.size() == MAX_DOORS && !actor.doors.containsKey(pos)) {
            actor.doors.remove(actor.doors.keySet().iterator().next()); // Never operate remotely on an evicted door.
        }
        actor.doors.put(pos.immutable(), new OpenedDoor(door, facing, side(villager, pos, facing), now));
    }

    private static boolean woodenDoor(BlockState state) {
        return state.is(BlockTags.WOODEN_DOORS) && state.getBlock() instanceof DoorBlock door && door.type().canOpenByHand();
    }

    private static double side(Villager actor, BlockPos pos, Direction facing) {
        return (actor.getX() - pos.getX() - .5) * facing.getStepX()
            + (actor.getZ() - pos.getZ() - .5) * facing.getStepZ();
    }

    private static void closePassedDoors(ServerLevel level, Villager villager, ActorState actor, Path path, long now) {
        Iterator<Map.Entry<BlockPos, OpenedDoor>> iterator = actor.doors.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            BlockPos pos = entry.getKey();
            OpenedDoor opened = entry.getValue();
            if (now - opened.openedAt() > 200 || !level.isLoaded(pos)
                    || villager.position().distanceToSqr(Vec3.atCenterOf(pos)) > 9) { iterator.remove(); continue; }
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() != opened.block() || !woodenDoor(state) || !state.getValue(DoorBlock.OPEN)
                    || state.getValue(DoorBlock.POWERED)) { iterator.remove(); continue; }
            double currentSide = side(villager, pos, opened.facing());
            boolean crossed = opened.entrySide() * currentSide < 0;
            boolean abandoned = now - opened.openedAt() >= 20 && !nearbyPathUses(path, pos);
            if ((!crossed && !abandoned) || Math.abs(currentSide) < 1.25) continue;
            AABB clearance = new AABB(pos).expandTowards(0, 1, 0).inflate(.6, .05, .6);
            if (villager.getBoundingBox().intersects(clearance)) continue;
            // Residents actually in the doorway, or approaching on a nearby path, keep it open.
            boolean residentsPassing = !level.getEntitiesOfClass(LivingEntity.class, clearance,
                other -> other != villager && other.isAlive() && !(other instanceof Monster)).isEmpty();
            if (!residentsPassing) {
                residentsPassing = level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(3),
                    other -> other != villager && other.isAlive() && nearbyPathUses(other.getNavigation().getPath(), pos)).size() > 0;
            }
            if (residentsPassing) continue;
            opened.block().setOpen(villager, level, state, pos, false);
            BlockState closedState = level.getBlockState(pos);
            if (closedState.is(opened.block()) && !closedState.getValue(DoorBlock.OPEN)) actor.doorsClosed++;
            iterator.remove();
        }
    }

    private static boolean nearbyPathUses(Path path, BlockPos door) {
        if (path == null || path.isDone()) return false;
        for (int i = Math.max(0, path.getNextNodeIndex() - 1); i < Math.min(path.getNodeCount(), path.getNextNodeIndex() + 3); i++) {
            BlockPos node = path.getNodePos(i);
            if (node.getX() == door.getX() && node.getZ() == door.getZ() && Math.abs(node.getY() - door.getY()) <= 1) return true;
        }
        return false;
    }

    public static String equipmentStatus(Villager villager) {
        ItemStack held = villager.getMainHandItem();
        return held.isEmpty() ? "empty hand" : BuiltInRegistries.ITEM.getKey(held.getItem()) + " x" + held.getCount()
            + " damage=" + held.getDamageValue() + "/" + held.getMaxDamage() + " usableSword=" + sword(held) + " usableHoe=" + hoe(held);
    }

    /** Successful physical events only; counters are per load and reset by forget/clear. */
    public static Map<String, Long> activityStatus(UUID id) {
        ActorState actor = ACTORS.get(id);
        return Map.of("doorsOpened", actor == null ? 0L : actor.doorsOpened,
            "doorsClosed", actor == null ? 0L : actor.doorsClosed,
            "swordHits", actor == null ? 0L : actor.swordHits,
            "cropsHarvested", actor == null ? 0L : actor.cropsHarvested,
            "replantsFromHarvest",actor==null ? 0L : actor.replantsFromHarvest,
            "replantsFromStock",actor==null ? 0L : actor.replantsFromStock);
    }
    public static Map<String,Object> lastHarvest(UUID id) {
        var actor=ACTORS.get(id);return actor==null ? Map.of() : actor.lastHarvest;
    }

    /** Root must call on unload/release; held equipment remains in vanilla entity save data. */
    public static void forget(UUID id) { ACTORS.remove(id); }
    public static void clear() { ACTORS.clear(); }
}
