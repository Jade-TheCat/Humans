package dev.jadethecat.humans.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.util.UserCache;

@Mixin(SkullBlockEntity.class)
public interface SkullBlockEntityAccessor {
    @Accessor("userCache")
    public static UserCache getUserCache() {
        throw new AssertionError();
    }
}
