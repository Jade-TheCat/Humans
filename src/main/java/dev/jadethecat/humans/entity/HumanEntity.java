package dev.jadethecat.humans.entity;

import java.util.Optional;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;

import org.jetbrains.annotations.Nullable;

import dev.jadethecat.humans.Humans;
import dev.jadethecat.humans.entity.ai.FollowHighAffinityGoal;
import dev.jadethecat.humans.mixin.SkullBlockEntityAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class HumanEntity extends PathAwareEntity implements Angerable {
    private static final TrackedData<NbtCompound> SKIN_PROFILE;
    private static final TrackedData<Boolean> LEGACY_SOUND;
	private static final UniformIntProvider ANGER_TIME_RANGE;
    private static final TrackedData<NbtCompound> AFFINITY;
    public static final int MAX_AFFINITY = 1000;
    public static final int HIGH_AFFINITY = MathHelper.floor(MAX_AFFINITY*0.75);
    private int angerTime;
    private UUID angeryAt;
    private UUID previouslyAngryAt;
    protected PlayerEntity leader;
	public double prevCapeX;
	public double prevCapeY;
	public double prevCapeZ;
	public double capeX;
	public double capeY;
	public double capeZ;
	public float prevStrideDistance;
	public float strideDistance;

    public HumanEntity(EntityType<? extends HumanEntity> entityType, World world) {
        super(entityType, world);
		((MobNavigation)this.getNavigation()).setCanPathThroughDoors(true);
		this.getNavigation().setCanSwim(true);
		this.setCanPickUpLoot(true);
    }
    
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(1, new MeleeAttackGoal(this, 0.35D, false));
        this.goalSelector.add(1, new FollowHighAffinityGoal(this, 10, 0.5D));
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
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 50);
    }

    public static int rankLikedItem(Item item) {
        if (item.getFoodComponent() != null) {
            if (item == Items.ENCHANTED_GOLDEN_APPLE) {
                return 1000;
            } else if (item == Items.GOLDEN_APPLE) {
                return 125;
            } else if (item == Items.GOLDEN_CARROT) {
                return 10;
            }
            return 5;
        } else if (item instanceof ArmorItem) {
            ArmorItem ai = (ArmorItem)item;
            if (ai.getMaterial() == ArmorMaterials.LEATHER) {
                return 5;
            } else if (ai.getMaterial() == ArmorMaterials.CHAIN) {
                return 6;
            } else if (ai.getMaterial() == ArmorMaterials.IRON) {
                return 10;
            } else if (ai.getMaterial() == ArmorMaterials.GOLD) {
                return 20;
            } else if (ai.getMaterial() == ArmorMaterials.DIAMOND) {
                return 50;
            }
        } else if (item == Items.DIAMOND) {
            return 25;
        }
        return 0;
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
        this.dataTracker.startTracking(AFFINITY, new NbtCompound());
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

    public int getAffinity(UUID uuid) {
        NbtCompound compound = this.dataTracker.get(AFFINITY);
        if (compound.contains(uuid.toString())) {
            NbtCompound affinityPair = compound.getCompound(uuid.toString());
            if (affinityPair != null && affinityPair.contains("Value")) {
                return affinityPair.getInt("Value");
            } else if(affinityPair == null) {
                Pair<Integer,NbtCompound> pair = repairAffinity(compound, uuid);
                this.dataTracker.set(AFFINITY, pair.getSecond());
                return pair.getFirst();
            }
        }
        return 0;
    }

    public void setAffinity(UUID uuid, int affinity) {
        NbtCompound compound = this.dataTracker.get(AFFINITY);
        if (affinity < MAX_AFFINITY) affinity = MAX_AFFINITY;
        if (compound.contains(uuid.toString())) {
            NbtCompound affinitySetup = compound.getCompound(uuid.toString());
            if (affinitySetup != null) {
                affinitySetup.putInt("Value", affinity);
                affinitySetup.putInt("Cooldown", 48000);
                compound.put(uuid.toString(), affinitySetup);
            } else {
                compound = repairAffinity(compound, uuid).getSecond();
                this.dataTracker.set(AFFINITY, compound);
                setAffinity(uuid, affinity);
            }
        } else {
            NbtCompound affinitySetup = new NbtCompound();
            affinitySetup.putInt("Value", affinity);
            affinitySetup.putInt("Cooldown", 48000);
            compound.put(uuid.toString(), affinitySetup);
        }
        this.dataTracker.set(AFFINITY, compound);
        if (affinity < 0) {
            setAngryAt(uuid);
            this.produceParticles(ParticleTypes.ANGRY_VILLAGER);
        }
    }

    public void addAffinity (UUID uuid) {
        this.addAffinity(uuid, 1);
    }

    public void addAffinity(UUID uuid, int amount) {
        NbtCompound compound = this.dataTracker.get(AFFINITY);
        if (amount < 0) this.produceParticles(ParticleTypes.ANGRY_VILLAGER);
        if (amount > 0) this.produceParticles(ParticleTypes.HAPPY_VILLAGER);
        if (compound.contains(uuid.toString())) {
            NbtCompound affinitySet = compound.getCompound(uuid.toString());
            if (affinitySet != null) {
                int newAffinity = affinitySet.getInt("Value") + amount;
                if (newAffinity < -1) newAffinity = -1;
                setAffinity(uuid, newAffinity);
            } else {
                Pair<Integer,NbtCompound> pair = repairAffinity(compound, uuid);
                this.dataTracker.set(AFFINITY, pair.getSecond());
                int newAffinity = pair.getFirst() + amount;
                if (newAffinity < -1) newAffinity = -1;
                setAffinity(uuid, newAffinity);
            }
        } else {
            if (amount < -1) amount = -1;
            setAffinity(uuid, amount);
        }
    }

    private Pair<Integer,NbtCompound> repairAffinity(NbtCompound affinityCompound, UUID uuid) {
        int oldAffinity = affinityCompound.getInt(uuid.toString());
        affinityCompound.remove(uuid.toString());
        NbtCompound affinityPair = new NbtCompound();
        affinityPair.putInt("Value", oldAffinity);
        affinityPair.putInt("Cooldown", 48000);
        affinityCompound.put(uuid.toString(), affinityPair);
        return Pair.of(oldAffinity, affinityCompound);
    }

    public boolean hasHighAffinity() {
        NbtCompound compound = this.dataTracker.get(AFFINITY);
        for (String k : compound.getKeys()) {
            NbtCompound affinitySetup = compound.getCompound(k);
            if (affinitySetup.contains("Value") && affinitySetup.getInt("Value") >= HumanEntity.HIGH_AFFINITY) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setCustomName(@Nullable Text name) {
        if (name.asString().contains("&L")) {
            this.dataTracker.set(LEGACY_SOUND, true);
            name = new LiteralText(name.asString().replace("&L", ""));
        } else {
            this.dataTracker.set(LEGACY_SOUND, false);
        }
        boolean keepSkin = name.asString().contains("&K");
        if (keepSkin) name = new LiteralText(name.asString().replace("&K", ""));
        super.setCustomName(name);
        GameProfile gprofile = getSkinProfile();
        if (name != null && !keepSkin && (gprofile == null || gprofile.getName() != name.asString())) {
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
        nbt.put("Affinity", this.dataTracker.get(AFFINITY));
	}

    @Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
        if (nbt.contains("SkinProfile"))
            this.setSkinProfile(NbtHelper.toGameProfile(nbt.getCompound("SkinProfile")));
		if (nbt.contains("UseLegacySound"))
			this.dataTracker.set(LEGACY_SOUND, nbt.getBoolean("UseLegacySound"));
        if (nbt.contains("Affinity"))
            this.dataTracker.set(AFFINITY, nbt.getCompound("Affinity"));
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
        tickAffinity();
    }

    private void tickAffinity() {
        NbtCompound compound = this.dataTracker.get(AFFINITY);
        if (compound != null) {
            for (String k : compound.getKeys()) {
                NbtCompound affinitySetup = compound.getCompound(k);
                if (affinitySetup != null && affinitySetup.contains("Cooldown")) {
                    int cooldown = affinitySetup.getInt("Cooldown") - 1;
                    if (cooldown <= 0) {
                        affinitySetup.putInt("Cooldown", 48000);
                        affinitySetup.putInt("Value", affinitySetup.getInt("Value"));
                        compound.put(k, affinitySetup);
                    } else {
                        affinitySetup.putInt("Cooldown", cooldown);
                        compound.put(k, affinitySetup);
                    }
                } else {
                    Pair<Integer,NbtCompound> pair = repairAffinity(compound, UUID.fromString(k));
                    compound = pair.getSecond();
                }
            }
            this.dataTracker.set(AFFINITY, compound);
        }
    }

    @Override
    protected void loot(ItemEntity item) {
        ItemStack stack = item.getStack();
        if (item.getThrower() != null && Humans.HUMAN_LIKED_ITEMS.contains(stack.getItem())) {
            this.addAffinity(item.getThrower(), item.getStack().getCount() * rankLikedItem(item.getStack().getItem()));
        }
        super.loot(item);
    }

    @Override
    public boolean tryEquip(ItemStack equipment) {
        if (this.canPickupItem(equipment) && Humans.HUMAN_FOOD.contains(equipment.getItem())) {
            if (equipment.getItem().getFoodComponent() != null) {
                this.heal(equipment.getItem().getFoodComponent().getHunger());
                for (Pair<StatusEffectInstance, Float> effect : equipment.getItem().getFoodComponent().getStatusEffects()) {
                    if (this.canHaveStatusEffect(effect.getFirst()))
                        this.addStatusEffect(effect.getFirst());
                }
                this.playSound(SoundEvents.ENTITY_PLAYER_BURP, 1.0f, 1.0f);
                this.emitGameEvent(GameEvent.EAT);
                this.onEquipStack(equipment);
                return true;
            }
        }
        return super.tryEquip(equipment);
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        if (!this.isInvulnerableTo(source) 
                && !source.isSourceCreativePlayer() 
                && source.getAttacker() instanceof PlayerEntity) {
            this.setAffinity(uuid, -1);
        }
        super.applyDamage(source, amount);
    }

    private void updateCapeAngles() {
		this.prevCapeX = this.capeX;
		this.prevCapeY = this.capeY;
		this.prevCapeZ = this.capeZ;
		double d = this.getX() - this.capeX;
		double e = this.getY() - this.capeY;
		double f = this.getZ() - this.capeZ;
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

    protected void produceParticles(ParticleEffect parameters) {
		for(int i = 0; i < 5; ++i) {
			double d = this.random.nextGaussian() * 0.02D;
			double e = this.random.nextGaussian() * 0.02D;
			double f = this.random.nextGaussian() * 0.02D;
			this.world.addParticle(parameters, this.getParticleX(1.0D), this.getRandomBodyY() + 1.0D, this.getParticleZ(1.0D), d, e, f);
		}

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
        this.previouslyAngryAt = this.angeryAt;
        this.setAffinity(this.previouslyAngryAt, 0);
        this.angeryAt = uuid;
        PlayerEntity player = this.world.getPlayerByUuid(uuid);
        if (player != null && !player.isCreative()) {
            this.setAffinity(uuid, -1);
        }
    }

    @Override
    public void chooseRandomAngerTime() {
        setAngerTime(ANGER_TIME_RANGE.get(this.random));
    }

    static {
        SKIN_PROFILE = DataTracker.registerData(HumanEntity.class, TrackedDataHandlerRegistry.TAG_COMPOUND);
        LEGACY_SOUND = DataTracker.registerData(HumanEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        AFFINITY = DataTracker.registerData(HumanEntity.class, TrackedDataHandlerRegistry.TAG_COMPOUND);
		ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        Item item = stack.getItem();
        if (this.world.isClient) {
            return ActionResult.SUCCESS;
        } else {
            return ActionResult.SUCCESS;
        }
    }
}
