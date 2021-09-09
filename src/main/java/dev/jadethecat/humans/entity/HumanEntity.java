package dev.jadethecat.humans.entity;

import java.util.Optional;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import org.jetbrains.annotations.Nullable;

import dev.jadethecat.humans.Humans;
import dev.jadethecat.humans.mixin.SkullBlockEntityAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.FollowTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.World;

public class HumanEntity extends PathAwareEntity implements Angerable {
    private static final TrackedData<NbtCompound> SKIN_PROFILE;
    private static final TrackedData<Boolean> LEGACY_SOUND;
	private static final UniformIntProvider ANGER_TIME_RANGE;
    private int angerTime;
    private UUID angeryAt;
	public double prevCapeX;
	public double prevCapeY;
	public double prevCapeZ;
	public double capeX;
	public double capeY;
	public double capeZ;
	public float prevStrideDistance;
	public float strideDistance;

    public HumanEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
		((MobNavigation)this.getNavigation()).setCanPathThroughDoors(true);
		this.getNavigation().setCanSwim(true);
		this.setCanPickUpLoot(true);
    }
    
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(1, new MeleeAttackGoal(this, 0.35D, false));
		this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.35D, 0.0F));
		this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		this.goalSelector.add(3, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this, new Class[0]));
        this.targetSelector.add(2, new FollowTargetGoal<PlayerEntity>(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt));
		this.targetSelector.add(2, new FollowTargetGoal<MobEntity>(this, MobEntity.class, 5, false, false, (entity) -> {
			return entity instanceof Monster && !(entity instanceof EndermanEntity);
		}));
    }

    public static DefaultAttributeContainer.Builder createHumanAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1);
    }

    public String getModel() {
        if (this.hasCustomName()) {
            UUID uuid = PlayerEntity.getOfflinePlayerUuid(this.getCustomName().asString());
            return DefaultSkinHelper.getModel(uuid);
        }
        return "default";
    }

    @Override
    @Nullable
	protected SoundEvent getHurtSound(DamageSource source) {
		return this.dataTracker.get(LEGACY_SOUND) ? Humans.LEGACY_HURT_SOUND_EVENT : SoundEvents.ENTITY_PLAYER_HURT;
	}

    @Override
	@Nullable
	protected SoundEvent getDeathSound() {
		return this.dataTracker.get(LEGACY_SOUND) ? Humans.LEGACY_HURT_SOUND_EVENT : SoundEvents.ENTITY_PLAYER_DEATH;
	}

    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SKIN_PROFILE, new NbtCompound());
        this.dataTracker.startTracking(LEGACY_SOUND, false);
    }

    @Nullable
    public GameProfile getSkinProfile() {
        NbtCompound comp = this.dataTracker.get(SKIN_PROFILE);
        if (comp.isEmpty()) {
            return null;
        }
        return NbtHelper.toGameProfile(comp);
    }

    public void setSkinProfile(@Nullable GameProfile profile) {
        if (profile != null) {
            NbtCompound nbtCompound = new NbtCompound();
            NbtHelper.writeGameProfile(nbtCompound, profile);
            this.dataTracker.set(SKIN_PROFILE, nbtCompound);
        } else {
            this.dataTracker.set(SKIN_PROFILE, new NbtCompound());
        }
    }

    @Override
    public void setCustomName(@Nullable Text name) {
        if (name.asString().startsWith("{legacy}")) {
            this.dataTracker.set(LEGACY_SOUND, true);
            name = new LiteralText(name.asString().replace("{legacy}", ""));
        } else {
            this.dataTracker.set(LEGACY_SOUND, false);
        }
        super.setCustomName(name);
        GameProfile gprofile = getSkinProfile();
        if (name != null && (gprofile == null || gprofile.getName() != name.asString())) {
            Optional<GameProfile> profile = SkullBlockEntityAccessor.getUserCache().findByName(name.asString());
            if (profile.isPresent()) {
                GameProfile profile2;
                if (this.world.isClient) {
                    profile2 = MinecraftClient.getInstance().getSessionService().fillProfileProperties(profile.get(), false);
                } else {
                    profile2 = this.getServer().getSessionService().fillProfileProperties(profile.get(), false);
                }
                this.setSkinProfile(profile2);
            }
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
        NbtCompound compound = new NbtCompound();
        GameProfile prof = this.getSkinProfile();
        if (prof != null)
            NbtHelper.writeGameProfile(compound, prof);
		nbt.put("SkinProfile", compound);
		nbt.putBoolean("UseLegacySound", (boolean)this.dataTracker.get(LEGACY_SOUND));
	}

    @Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
        this.setSkinProfile(NbtHelper.toGameProfile(nbt.getCompound("SkinProfile")));
		if (nbt.contains("UseLegacySound")) {
			this.dataTracker.set(LEGACY_SOUND, nbt.getBoolean("UseLegacySound"));
		}
	}

    @Override
    public void tickMovement() {
		this.prevStrideDistance = this.strideDistance;
        super.tickMovement();
        float g;
		if (this.onGround && !this.isDead() && !this.isSwimming()) {
			g = Math.min(0.1F, (float)this.getVelocity().horizontalLength());
		} else {
			g = 0.0F;
		}

        this.strideDistance += (g - this.strideDistance) * 0.4F;
    }

    @Override
    public void tick() {
        super.tick();
        updateCapeAngles();
    }

    private void updateCapeAngles() {
		this.prevCapeX = this.capeX;
		this.prevCapeY = this.capeY;
		this.prevCapeZ = this.capeZ;
		double d = this.getX() - this.capeX;
		double e = this.getY() - this.capeY;
		double f = this.getZ() - this.capeZ;
		double g = 10.0D;
		if (d > 10.0D) {
			this.capeX = this.getX();
			this.prevCapeX = this.capeX;
		}

		if (f > 10.0D) {
			this.capeZ = this.getZ();
			this.prevCapeZ = this.capeZ;
		}

		if (e > 10.0D) {
			this.capeY = this.getY();
			this.prevCapeY = this.capeY;
		}

		if (d < -10.0D) {
			this.capeX = this.getX();
			this.prevCapeX = this.capeX;
		}

		if (f < -10.0D) {
			this.capeZ = this.getZ();
			this.prevCapeZ = this.capeZ;
		}

		if (e < -10.0D) {
			this.capeY = this.getY();
			this.prevCapeY = this.capeY;
		}

		this.capeX += d * 0.25D;
		this.capeZ += f * 0.25D;
		this.capeY += e * 0.25D;
	}

    @Override
    public void tickRiding() {
        super.tickRiding();
        this.prevStrideDistance = this.strideDistance;
        this.strideDistance = 0.0F;
    }

    @Override
    public int getAngerTime() {
        return this.angerTime;
    }

    @Override
    public void setAngerTime(int ticks) {
        this.angerTime = ticks;
    }

    @Override
    public UUID getAngryAt() {
        return this.angeryAt;
    }

    @Override
    public void setAngryAt(UUID uuid) {
        this.angeryAt = uuid;
    }

    @Override
    public void chooseRandomAngerTime() {
        setAngerTime(ANGER_TIME_RANGE.get(this.random));
    }

    static {
        SKIN_PROFILE = DataTracker.registerData(HumanEntity.class, TrackedDataHandlerRegistry.TAG_COMPOUND);
        LEGACY_SOUND = DataTracker.registerData(HumanEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
		ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
    }
}
