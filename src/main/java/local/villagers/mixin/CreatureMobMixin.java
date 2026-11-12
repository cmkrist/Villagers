package local.villagers.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.villagers.CreatureAgents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replace only opted-in goal writers; navigation, physics, item use and native trading remain. */
@Mixin(Mob.class)
public abstract class CreatureMobMixin {
    @Shadow @Final protected GoalSelector goalSelector;
    @Shadow @Final protected GoalSelector targetSelector;
    private boolean villagers$controlled;
    @Inject(method="getMaxFallDistance",at=@At("RETURN"),cancellable=true)
    private void villagers$workRouteFallLimit(CallbackInfoReturnable<Integer> callback) {
        callback.setReturnValue(local.villagers.NativePathLimits.maxFall(this,callback.getReturnValue()));
    }
    @Inject(method="serverAiStep",at=@At("HEAD"))
    private void villagers$creatureThink(CallbackInfo callback) {
        Mob mob=(Mob)(Object)this;
        boolean managed=CreatureAgents.managed(mob);
        boolean escaping=!managed&&mob.level() instanceof ServerLevel level&&local.villagers.NativeEntrapment.prepare(level,mob);
        boolean controlled=managed||escaping;
        if(controlled && !villagers$controlled) {
            goalSelector.getAvailableGoals().forEach(WrappedGoal::stop);
            targetSelector.getAvailableGoals().forEach(WrappedGoal::stop);
            mob.getNavigation().stop();mob.setTarget(null);
        }
        villagers$controlled=controlled;
        // Replacing decisions must retain the body's native drowning reflex.
        // This existing goal requests ordinary jumps; physics and air still run.
        if(controlled)for(var wrapped:goalSelector.getAvailableGoals())
            if(wrapped.getGoal() instanceof FloatGoal floating&&floating.canUse()){
                floating.tick();CreatureAgents.nativeFloatTick(mob);
            }
        if(managed && mob.level() instanceof ServerLevel level)CreatureAgents.tick(level,mob);
        else if(escaping)local.villagers.NativeEntrapment.move(mob);
    }
    @WrapOperation(method="serverAiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/ai/goal/GoalSelector;tick()V"))
    private void villagers$goals(GoalSelector selector,Operation<Void> original) {
        if(!villagers$controlled)original.call(selector);
    }
    @WrapOperation(method="serverAiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/ai/goal/GoalSelector;tickRunningGoals(Z)V"))
    private void villagers$runningGoals(GoalSelector selector,boolean tickAll,Operation<Void> original) {
        if(!villagers$controlled)original.call(selector,tickAll);
    }
}
