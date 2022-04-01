package dev.jadethecat.humans.entity.ai;

import dev.jadethecat.humans.entity.HumanEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;

public class FollowBestFriendGoal extends Goal {
    protected final int reciprocalChance;
	private final EntityNavigation navigation;
	private int updateCountdownTicks;
    private double speed;
    private TargetPredicate targetPredicate;
    private HumanEntity human;
    private PlayerEntity target;
    private int timeWithoutVisibility;
    private int maxTimeWithoutVisibility = 60;
    private World world;

    public FollowBestFriendGoal(HumanEntity human, int reciprocalChance, double speed) {
        this.reciprocalChance = reciprocalChance;
        this.navigation = human.getNavigation();
        this.speed = speed;
        this.human = human;
        this.targetPredicate = TargetPredicate.createAttackable().setBaseMaxDistance(this.getFollowRange()).setPredicate(null);
        this.world = human.world;
    }

    @Override
    public boolean canStart() {
        if (!this.human.getBestFriend().isPresent() || this.human.getState() != HumanEntity.FOLLOWING_STATE) {
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
        this.human.getLookControl().lookAt(this.target, 10.0F, (float)this.human.getMaxLookPitchChange());
        if (--this.updateCountdownTicks <= 0) {
            this.updateCountdownTicks = 10;
            if (!this.human.isLeashed() && !this.human.hasVehicle()) {
                if (this.human.squaredDistanceTo(this.target) >= 144.0D) {
					this.tryTeleport();
				} else {
					this.navigation.startMovingTo(this.target, this.speed);
				}
            }
        }
    }

    @Override
    public boolean shouldContinue() {
        if (this.target == null 
                || this.target.isCreative() 
                || !this.human.canTarget(this.target) 
                || this.human.getState() != HumanEntity.FOLLOWING_STATE) {
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
            return livingEntity instanceof PlayerEntity && ((HumanEntity)this.human).getBestFriend().get() == livingEntity.getUuid();
        }), targetPredicate, this.human, this.human.getX(), this.human.getEyeY(), this.human.getZ());
    }

    private void tryTeleport() {
		BlockPos blockPos = this.target.getBlockPos();

		for(int i = 0; i < 10; ++i) {
			int j = this.getRandomInt(-3, 3);
			int k = this.getRandomInt(-1, 1);
			int l = this.getRandomInt(-3, 3);
			boolean bl = this.tryTeleportTo(blockPos.getX() + j, blockPos.getY() + k, blockPos.getZ() + l);
			if (bl) {
				return;
			}
		}

	}

	private boolean tryTeleportTo(int x, int y, int z) {
		if (Math.abs((double)x - this.target.getX()) < 2.0D && Math.abs((double)z - this.target.getZ()) < 2.0D) {
			return false;
		} else if (!this.canTeleportTo(new BlockPos(x, y, z))) {
			return false;
		} else {
			this.human.refreshPositionAndAngles((double)x + 0.5D, (double)y, (double)z + 0.5D, this.human.getYaw(), this.human.getPitch());
			this.navigation.stop();
			return true;
		}
	}

	private boolean canTeleportTo(BlockPos pos) {
		PathNodeType pathNodeType = LandPathNodeMaker.getLandNodeType(this.world, pos.mutableCopy());
		if (pathNodeType != PathNodeType.WALKABLE) {
			return false;
		} else {
            BlockPos blockPos = pos.subtract(this.human.getBlockPos());
            return this.world.isSpaceEmpty(this.human, this.human.getBoundingBox().offset(blockPos));
		}
	}
	private int getRandomInt(int min, int max) {
		return this.human.getRandom().nextInt(max - min + 1) + min;
	}
}
