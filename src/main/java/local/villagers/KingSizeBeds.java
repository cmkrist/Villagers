package local.villagers;

import java.util.stream.Collectors;
import local.villagers.block.KingSizeBedBlock;
import local.villagers.mixin.PoiTypesAccessor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;

final class KingSizeBeds {
    static final Identifier ID = Identifier.fromNamespaceAndPath("villagers", "king_size_bed");
    static final KingSizeBedBlock BLOCK = new KingSizeBedBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BED.red())
            .setId(ResourceKey.create(Registries.BLOCK, ID)));
    static void initialize() {
        Registry.register(BuiltInRegistries.BLOCK, ID, BLOCK);
        Item item = Registry.register(BuiltInRegistries.ITEM, ID, new BlockItem(BLOCK,
                new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ID)).useBlockDescriptionPrefix().stacksTo(1)));
        PoiTypesAccessor.villagers$registerBeds(BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(PoiTypes.HOME),
                BLOCK.getStateDefinition().getPossibleStates().stream().filter(s -> s.getValue(KingSizeBedBlock.PART) == BedPart.HEAD).collect(Collectors.toSet()));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(item));
    }
    private KingSizeBeds() {}
}
