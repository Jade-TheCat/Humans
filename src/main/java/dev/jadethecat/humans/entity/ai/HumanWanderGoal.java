package dev.jadethecat.humans.entity.ai;

import dev.jadethecat.humans.entity.HumanEntity;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.mob.PathAwareEntity;

public class HumanWanderGoal extends WanderAroundFarGoal {

    public HumanWanderGoal(PathAwareEntity mob, double speed) {
        super(mob, speed, 0.0F);
    }
    @Override
    public boolean canStart() {
        if (this.mob instanceof HumanEntity) {
            return super.canStart() && ((HumanEntity)this.mob).getState() == HumanEntity.NONE_STATE || ((HumanEntity)this.mob).getState() == HumanEntity.FOLLOWING_STATE;
        } else {
            return super.canStart();
        }
    }

    @Override
    public boolean shouldContinue() {
        if (this.mob instanceof HumanEntity) {
            return super.shouldContinue() && 
                ((HumanEntity)this.mob).getState() == HumanEntity.NONE_STATE || 
                ((HumanEntity)this.mob).getState() == HumanEntity.FOLLOWING_STATE;
        } else {
            return super.shouldContinue();
        }
    }
}
