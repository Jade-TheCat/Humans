package dev.jadethecat.humans.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import dev.jadethecat.humans.entity.HumanEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.ZombieEntity;

@Mixin(ZombieEntity.class)
public abstract class ZombieEntityMixin {
    @Inject(method="initCustomGoals()V", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void initCustomGoals(CallbackInfo info) {
        ((MobEntityAccessor)(Object)this).getTargetSelector().add(3, new ActiveTargetGoal<>((ZombieEntity)(Object)this, HumanEntity.class, true));
    }
}
