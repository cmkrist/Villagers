package local.villagers;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Ordered species allow deterministic planning and preserve the native boat type. */
final class BoatMaterials {
    record Type(Item item, EntityType<? extends AbstractBoat> entity) {}
    static final Map<Item, Type> TYPES;
    static {
        var types = new LinkedHashMap<Item, Type>();
        types.put(Items.OAK_PLANKS, new Type(Items.OAK_BOAT, EntityTypes.OAK_BOAT));
        types.put(Items.SPRUCE_PLANKS, new Type(Items.SPRUCE_BOAT, EntityTypes.SPRUCE_BOAT));
        types.put(Items.BIRCH_PLANKS, new Type(Items.BIRCH_BOAT, EntityTypes.BIRCH_BOAT));
        types.put(Items.JUNGLE_PLANKS, new Type(Items.JUNGLE_BOAT, EntityTypes.JUNGLE_BOAT));
        types.put(Items.ACACIA_PLANKS, new Type(Items.ACACIA_BOAT, EntityTypes.ACACIA_BOAT));
        types.put(Items.DARK_OAK_PLANKS, new Type(Items.DARK_OAK_BOAT, EntityTypes.DARK_OAK_BOAT));
        types.put(Items.MANGROVE_PLANKS, new Type(Items.MANGROVE_BOAT, EntityTypes.MANGROVE_BOAT));
        types.put(Items.CHERRY_PLANKS, new Type(Items.CHERRY_BOAT, EntityTypes.CHERRY_BOAT));
        types.put(Items.PALE_OAK_PLANKS, new Type(Items.PALE_OAK_BOAT, EntityTypes.PALE_OAK_BOAT));
        types.put(Items.POPLAR_PLANKS, new Type(Items.POPLAR_BOAT, EntityTypes.POPLAR_BOAT));
        TYPES = java.util.Collections.unmodifiableMap(types);
    }
    static Type type(ItemStack stack) {
        return TYPES.values().stream().filter(t -> stack.is(t.item())).findFirst().orElse(null);
    }
    private BoatMaterials() {}
}
