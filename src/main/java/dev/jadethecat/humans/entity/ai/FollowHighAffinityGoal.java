package dev.jadethecat.humans.entity.ai;

import dev.jadethecat.humans.entity.HumanEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.util.math.Box;

public class FollowHighAffinityGoal extends Goal {
    protected final int reciprocalChance;
	private final EntityNavigation navigation;
	private int updateCountdownTicks;
    private double speed;
    private TargetPredicate targetPredicate;
    private HumanEntity human;
    private PlayerEntity target;
    private int timeWithoutVisibility;
    private int maxTimeWithoutVisibility = 60;

    public FollowHighAffinityGoal(HumanEntity human, int reciprocalChance, double speed) {
        this.reciprocalChance = reciprocalChance;
        this.navigation = human.getNavigation();
        this.speed = speed;
        this.human = human;
        this.targetPredicate = TargetPredicate.createAttackable().setBaseMaxDistance(this.getFollowRange()).setPredicate(null);
    }

    @Override
    public boolean canStart() {
        if (!this.human.hasHighAffinity()) {
			return false;
		} else {
			this.findClosestTarget();
			return this.target != null && !(this.human.squaredDistanceTo(this.target) < (double)(25.0D));
		}
    }

    @Override
    public void stop() {
        this.target = null;
		this.navigation.stop();
    }

    @Override
	public void start() {
		this.updateCountdownTicks = 0;
		this.timeWithoutVisibility = 0;
    }

    public void tick() {
        this.human.getLookControl().lookAt(this.target, 10.0F, (float)this.human.getLookPitchSpeed());
        if (--this.updateCountdownTicks <= 0) {
            this.updateCountdownTicks = 10;
            if (!this.human.isLeashed() && !this.human.hasVehicle()) {
                this.navigation.startMovingTo(this.target, this.speed);
            }
        }
    }

    @Override
    public boolean shouldContinue() {
        if (this.target == null) {
            return false;
        } else if (this.target.isCreative() || !this.human.canTarget(this.target)) {
            return false;
        } else {
            AbstractTeam abstractTeam = this.human.getScoreboardTeam();
			AbstractTeam abstractTeam2 = this.target.getScoreboardTeam();
			if (abstractTeam != null && abstractTeam2 == abstractTeam) {
				return false;
			} else {
                double d = this.getFollowRange();
				if (this.human.squaredDistanceTo(this.target) > d * d) {
					return false;
				} else {
                    if (this.human.getVisibilityCache().canSee(this.target)) {
                        this.timeWithoutVisibility = 0;
                    } else if (++this.timeWithoutVisibility > this.maxTimeWithoutVisibility) {
                        return false;
                    }
					return !(this.human.squaredDistanceTo(this.target) <= (double)(25.0d));
				}
            }
        }
    }
    
	protected Box getSearchBox(double distance) {
		return this.human.getBoundingBox().expand(distance, 4.0D, distance);
	}
    
	protected double getFollowRange() {
		return this.human.getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE);
	}
    
    protected void findClosestTarget() {
        this.target = this.human.world.getClosestEntity(this.human.world.getEntitiesByClass(PlayerEntity.class, this.getSearchBox(this.getFollowRange()), (livingEntity) -> {
            return livingEntity instanceof PlayerEntity && ((HumanEntity)this.human).getAffinity(livingEntity.getUuid()) > HumanEntity.HIGH_AFFINITY;
        }), targetPredicate, this.human, this.human.getX(), this.human.getEyeY(), this.human.getZ());
    }
}
