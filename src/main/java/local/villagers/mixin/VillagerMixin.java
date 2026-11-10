package local.villagers.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.villagers.Villagers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Only replace the decision writer. Keep vanilla movement, trading and physiology.
 * WrapOperation (bundled with Fabric Loader) chains with other mods wrapping the same call, unlike a Redirect.
 */
@Mixin(Villager.class)
public abstract class VillagerMixin {
    @WrapOperation(method = "customServerAiStep", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/ai/Brain;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void villagers$think(Brain<Villager> brain, ServerLevel level, LivingEntity body, Operation<Void> original) {
        if (body instanceof Villager villager && Villagers.isManaged(villager)) Villagers.tick(level, villager);
        else original.call(brain, level, body);
    }
}
