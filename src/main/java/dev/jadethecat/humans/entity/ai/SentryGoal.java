package dev.jadethecat.humans.entity.ai;

import org.jetbrains.annotations.Nullable;

import dev.jadethecat.humans.HumansConfig;
import dev.jadethecat.humans.entity.HumanEntity;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.entity.ai.NoWaterTargeting;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class SentryGoal extends WanderAroundGoal {

    public SentryGoal(PathAwareEntity entity, double speed) {
        super(entity, speed, 10, false);
    }

	public boolean canStart() {
        boolean humanAndSentryCheck = this.mob instanceof HumanEntity ? ((HumanEntity)this.mob).getState().equals(HumanEntity.SENTRY_STATE) && ((HumanEntity)this.mob).getHomePos().isPresent() : false;
		return humanAndSentryCheck && super.canStart();
	}

	@Nullable
	protected Vec3d getWanderTarget() {
        if (this.mob instanceof HumanEntity) {
            HumansConfig config = AutoConfig.getConfigHolder(HumansConfig.class).getConfig();
            BlockPos home = ((HumanEntity)this.mob).getHomePos().get();
            return NoWaterTargeting.find(this.mob, config.humanSentryWanderRange, 7, 0, Vec3d.ofBottomCenter(home), 1.5707963705062866D);
        } else {
            return null;
        }
	}
}
