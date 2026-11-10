package local.villagers.mixin;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Registers missing native equipment attributes, without adding attack AI to ordinary villagers. */
@Mixin(Villager.class)
public abstract class VillagerAttributesMixin {
    @Inject(method = "createAttributes", at = @At("RETURN"))
    private static void villagers$equipmentAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> callback) {
        callback.getReturnValue().add(Attributes.ATTACK_DAMAGE, 1.0).add(Attributes.ATTACK_SPEED, 4.0);
    }
}
