package dev.jadethecat.humans.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;

@Mixin(MobEntity.class)
public interface MobEntityAccessor {
    @Accessor("targetSelector")
    abstract GoalSelector getTargetSelector();
}
