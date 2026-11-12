package local.villagers.mixin;

import local.villagers.BoatTravel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(AbstractBoat.class)
public abstract class AbstractBoatMixin {
    @Shadow private void controlBoat() { throw new AssertionError(); }
    @Inject(method="canAddPassenger",at=@At("HEAD"),cancellable=true)
    private void villagers$intentionalBoarding(net.minecraft.world.entity.Entity passenger,CallbackInfoReturnable<Boolean> cir){
        var boat=(AbstractBoat)(Object)this;
        if(!boat.level().isClientSide()&&!BoatTravel.mayBoard(boat,passenger))cir.setReturnValue(false);
    }
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;floatBoat()V", shift = At.Shift.AFTER))
    private void villagers$paddle(CallbackInfo ci) {
        var boat = (AbstractBoat) (Object) this;
        if (!boat.level().isClientSide() && BoatTravel.steer(boat)) controlBoat();
    }
    @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
    private void villagers$pilot(CallbackInfoReturnable<LivingEntity> cir) {
        var boat = (AbstractBoat) (Object) this;
        // Client entity tags are not synchronized. Expose the visible passenger for native paddle rendering;
        // server steering still requires ownership, managed AI and a valid mission.
        if (boat.level().isClientSide() && boat.getFirstPassenger() instanceof net.minecraft.world.entity.npc.villager.Villager v) {
            cir.setReturnValue(v); return;
        }
        var pilot = BoatTravel.pilot(boat);
        if (pilot != null) cir.setReturnValue(pilot);
    }
}
