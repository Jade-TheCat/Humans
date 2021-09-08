package dev.jadethecat.humans.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.world.World;

public class HumanEntity extends PathAwareEntity {

    public HumanEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }
    
}
