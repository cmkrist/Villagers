package local.villagers;

import java.util.Comparator;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.gamerules.GameRules;

/** Approach visible equipment; vanilla mob pickup performs the actual inventory transfer. */
final class LooseEquipment {
    static String seek(ServerLevel level, Villager v, Session s, long now) {
        if (now < s.nextEquipmentScan || v.isBaby() || v.isSleeping() || v.isPassenger()
                || !v.canPickUpLoot() || !level.getGameRules().get(GameRules.MOB_GRIEFING)
                || !AdventureActions.PERMISSIONS.allows(Capability.EQUIP_ITEM)) return null;
        var item = level.getEntitiesOfClass(ItemEntity.class, v.getBoundingBox().inflate(8),
            i -> i.isAlive() && !i.hasPickUpDelay() && AdventureActions.acceptsDroppedEquipment(i.getItem())
                && v.wantsToPickUp(level, i.getItem()) && v.hasLineOfSight(i)
                && VillageSurvival.withinHomeRange(level, s.mind, i.position()))
            .stream().min(Comparator.comparingDouble(v::distanceToSqr)).orElse(null);
        // A player's thrower reference is provenance, not an exclusive ownership claim.
        if (item == null) { s.nextEquipmentScan = now + 40; return null; }
        if (now >= s.nextEquipmentPath && WorkNavigation.admit(level, now, v)) {
            s.nextEquipmentPath = now + 40;
            var path = WorkNavigation.pathTo(level, v, s, item.blockPosition(), 0);
            if (path == null || !v.getNavigation().moveTo(path, .6)) {
                s.nextEquipmentScan = now + 100;
                return null;
            }
        }
        return "collecting dropped tools or weapons";
    }

    private LooseEquipment() {}
}
