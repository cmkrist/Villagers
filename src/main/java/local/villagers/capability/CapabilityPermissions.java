package local.villagers.capability;

import java.util.Set;

/** Immutable permissions: specific food operations are separate exceptions to terrain editing. */
public final class CapabilityPermissions {
    public enum Capability { OPEN_DOOR, CLOSE_DOOR, CONSUME_FOOD, EQUIP_ITEM, DEFENSIVE_MELEE, HARVEST_REPLANT, CRAFT_BREAD, GATHER_HAY, UNPACK_HAY, CULTIVATE_PLANT, CRAFT_WOODEN_HOE, CRAFT_WOODEN_PICKAXE, CRAFT_STONE_PICKAXE, MINE_SURFACE_STONE, DIG_FOOD_PASSAGE, BUILD_CRAFTING_TABLE, FORESTRY, SOIL_TRANSFER, CRAFT_BOAT, PILOT_BOAT, OPEN_CAMP_STORAGE, CRAFT_BED, PLACE_CAMP_BED, RELOCATE_CAMP_BED, OPEN_NATIVE_FOOD_STORAGE, BREAK_BLOCK, PLACE_BLOCK }
    private static final CapabilityPermissions ADVENTURE = new CapabilityPermissions(Set.of(
        Capability.OPEN_DOOR, Capability.CLOSE_DOOR, Capability.CONSUME_FOOD,
        Capability.EQUIP_ITEM, Capability.DEFENSIVE_MELEE, Capability.HARVEST_REPLANT, Capability.CRAFT_BREAD,
        Capability.GATHER_HAY, Capability.UNPACK_HAY, Capability.CULTIVATE_PLANT, Capability.CRAFT_WOODEN_HOE, Capability.CRAFT_WOODEN_PICKAXE, Capability.CRAFT_STONE_PICKAXE, Capability.MINE_SURFACE_STONE, Capability.DIG_FOOD_PASSAGE, Capability.BUILD_CRAFTING_TABLE, Capability.FORESTRY, Capability.SOIL_TRANSFER, Capability.CRAFT_BOAT, Capability.PILOT_BOAT,
        Capability.OPEN_CAMP_STORAGE,Capability.CRAFT_BED,Capability.PLACE_CAMP_BED,Capability.RELOCATE_CAMP_BED,Capability.OPEN_NATIVE_FOOD_STORAGE));
    private final Set<Capability> allowed;

    private CapabilityPermissions(Set<Capability> allowed) { this.allowed = Set.copyOf(allowed); }
    public static CapabilityPermissions adventure() { return ADVENTURE; }
    public boolean allows(Capability capability) { return allowed.contains(capability); }
    public Set<Capability> allowed() { return allowed; }
}
