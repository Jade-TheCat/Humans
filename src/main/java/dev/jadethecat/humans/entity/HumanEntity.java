package dev.jadethecat.humans.entity;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;

import org.jetbrains.annotations.Nullable;

import dev.jadethecat.humans.Humans;
import dev.jadethecat.humans.HumansConfig;
import dev.jadethecat.humans.client.HumansClient;
import dev.jadethecat.humans.components.HumansComponents;
import dev.jadethecat.humans.entity.ai.FollowBestFriendGoal;
import dev.jadethecat.humans.entity.ai.HumanWanderGoal;
import dev.jadethecat.humans.entity.ai.SentryGoal;
import dev.jadethecat.humans.mixin.SkullBlockEntityAccessor;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.FollowTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class HumanEntity extends PathAwareEntity implements Angerable {
    private static final TrackedData<NbtCompound> SKIN_PROFILE;
	private static final UniformIntProvider ANGER_TIME_RANGE;
    private static final TrackedData<NbtCompound> AFFINITY;
    private static final TrackedData<Optional<UUID>> BEST_FRIEND;
    private static final TrackedData<String> STATE;
    private static final TrackedData<Optional<BlockPos>> HOME_POS;
    /**
     * Human Flags.
     * 0b00000001 - Legacy Sound.
     * 0b00000010 - Legacy Animation.
     * 0b00000100 - Slim Skin.
     * 0b00001000 - Skin Lock.
     */
    private static final TrackedData<Byte> HUMAN_FLAGS;
    public static final byte LEGACY_SOUND_MASK =         (byte)0b00000001;
    public static final byte LEGACY_ANIMATION_MASK =     (byte)0b00000010;
    public static final byte SLIM_SKIN_MASK =            (byte)0b00000100;
    public static final byte SKIN_LOCK_MASK =            (byte)0b00001000;

    static {
        // SKIN_PROFILE is the GameProfile which contains the Human's skin.
        SKIN_PROFILE = DataTracker.registerData(HumanEntity.class, TrackedDataHandlerRegistry.TAG_COMPOUND);
        // AFFINITY tracks the Human's affinity with players.
        AFFINITY = DataTracker.registerData(HumanEntity.class, TrackedDataHandlerRegistry.TAG_COMPOUND);
        // ANGER_TIME_RANGE is used for the anger mechanics.
		ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
        // BEST_FRIEND tracks which player the Human follows and listens to.
        BEST_FRIEND = DataTracker.registerData(HumanEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
        // STATE tracks what the Human is doing.
        STATE = DataTracker.registerData(HumanEntity.class, TrackedDataHandlerRegistry.STRING);
        // HOME_POS tracks where a Human will be a Sentry.
        HOME_POS = DataTracker.registerData(HumanEntity.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS);
        // HUMAN_FLAGS contains flags for rendering of a Human, including a lock to prevent changes to skin.
        HUMAN_FLAGS = DataTracker.registerData(HumanEntity.class, TrackedDataHandlerRegistry.BYTE);
    }

    // MAX_AFFINITY is the maximum affinity a player can have with a Human.
    public static final int MAX_AFFINITY = 1000;
    // HIGH_AFFINITY is the affinity a player needs too become a Human's best friend.
    public static final int HIGH_AFFINITY = MathHelper.floor(MAX_AFFINITY*0.75);
    // the following are constants to help with setting a Human's state.
    public static final String FOLLOWING_STATE = "humans:following";
    public static final String SENTRY_STATE = "humans:sentry";
    public static final String WAITING_STATE = "humans:waiting";
    public static final String NONE_STATE = "humans:none";
    public static Set<String> humanStates = new HashSet<>();
    // Anger-related variables, used by the Angerable functions.
    private int angerTime;
    private UUID angeryAt;
    private UUID previouslyAngryAt;
    // Cape-related variables for rendering.
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

    /**
     * <h3>Initialize a human.</h3>
     * Runs on spawn, decides the Human's characteristics, specifically: 
     * If the Human has a name from the config, if the human has a name and skin of a Mojangsta from the config, or if they have a slim skin, legacy animations, and/or legacy sounds. All of the chances for these is in {@link dev.jadethecat.humans.HumansConfig}.
     */
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        HumansConfig config = AutoConfig.getConfigHolder(HumansConfig.class).getConfig();
        if (config.exclusivelySpawnNamed || this.getRandom().nextFloat() < config.chanceToSpawnNamed) {
            int i = this.getRandom().nextInt(config.spawnableNames.size());
            this.setCustomName(new LiteralText(config.spawnableNames.get(i).name));
            if (config.spawnableNames.get(i).slimSkin)
                this.dataTracker.set(HUMAN_FLAGS, SLIM_SKIN_MASK);
        } else if (this.getRandom().nextFloat() < config.chanceToSpawnMojangsta) {
            int i = this.getRandom().nextInt(config.spawnableMojangstas.size());
            this.setCustomName(new LiteralText(config.spawnableMojangstas.get(i).name).formatted(Formatting.GOLD));
            if (config.spawnableMojangstas.get(i).slimSkin)
                this.dataTracker.set(HUMAN_FLAGS, SLIM_SKIN_MASK);
        } else {
            byte flags = (byte)0;
            if (this.getRandom().nextFloat() < config.chanceToSpawnSlim) flags |= SLIM_SKIN_MASK;
            if (this.getRandom().nextFloat() < config.chanceToSpawnWithLegacyAnimation) flags |= LEGACY_ANIMATION_MASK;
            if (this.getRandom().nextFloat() < config.chanceToSpawnWithLegacySounds) flags |= LEGACY_SOUND_MASK;
            this.dataTracker.set(HUMAN_FLAGS, flags);
        }
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    /**
     * <h3>Initialize the DataTracker</h3>
     * The DataTracker syncs data between server and client. Most of the Human's variables are used for rendering, and need to be on the client too.
     */
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SKIN_PROFILE, new NbtCompound());
        this.dataTracker.startTracking(AFFINITY, new NbtCompound());
        this.dataTracker.startTracking(BEST_FRIEND, Optional.empty());
        this.dataTracker.startTracking(STATE, NONE_STATE);
        this.dataTracker.startTracking(HOME_POS, Optional.empty());
        this.dataTracker.startTracking(HUMAN_FLAGS, (byte)0);
    }
    /**
     * <h3>Initialize Human AI goals.</h3>
     * First block initializes the goalSelector, the second initializes the targetSelector.
     * Goals are used because they are simpler, and Humans don't need too complex of memory.
     */
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(1, new MeleeAttackGoal(this, 0.35D, false));
        this.goalSelector.add(1, new FollowBestFriendGoal(this, 10, 0.5D));
		this.goalSelector.add(2, new HumanWanderGoal(this, 0.35D));
        this.goalSelector.add(2, new SentryGoal(this, 0.5D));
		this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		this.goalSelector.add(3, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this, new Class[0]));
        this.targetSelector.add(2, new FollowTargetGoal<PlayerEntity>(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt));
		this.targetSelector.add(2, new FollowTargetGoal<MobEntity>(this, MobEntity.class, 5, false, false, (entity) -> {
			return entity instanceof Monster && !Humans.HUMAN_IGNORED_MOBS.contains(entity.getType()) && !entity.isSwimming();
		}));
    }

    /**
     * Puts states in the {@link dev.jadethecat.humans.entity.HumanEntity#humanStates} Set.
     * 
     * @see dev.jadethecat.humans.Humans#onInitialize()
     */
    public static void initStates() {
        humanStates.add(FOLLOWING_STATE);
        humanStates.add(WAITING_STATE);
        humanStates.add(SENTRY_STATE);
    }

    /**
     * Gives Humans their {@link net.minecraft.entity.attribute.EntityAttributes}.
     * @return The EntityAttributes.
     */
    public static DefaultAttributeContainer.Builder createHumanAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 50);
    }

    /**
     * Ranks an item by the affinity to give a player for a human.
     * @param item The item to rank
     * @return int Amount of Affinity a player should get for giving a Human the item
     */
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
    
    /**
     * <h3>Interaction handler</h3>
     * Handles when a player interacts with a Human.
     * Opens the Flute GUI for managing a human if holding a flute, does various things if holding a Heart-Shaped Locket.
     */
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        Item item = stack.getItem();
        String name = this.hasCustomName() ? this.getCustomName().asString() : TranslationStorage.getInstance().get("entity.human.this_human");
        if (this.world.isClient) {
            if (item == Humans.HEART_LOCKET && this.getAffinity(player.getUuid()) >= HIGH_AFFINITY) {
                if (this.getBestFriend().isPresent() && this.getBestFriend().get() != player.getUuid()) {
                    return ActionResult.FAIL;
                } else {
                        return ActionResult.SUCCESS;
                }
            } else if (item == Humans.HEART_LOCKET) {
                return ActionResult.FAIL;
            } else if (item == Items.POPPY) {
                return ActionResult.SUCCESS;
            } else if (Humans.HUMAN_LIKED_ITEMS.contains(item)) {
                return ActionResult.CONSUME;
            } else if (item == Humans.FLUTE 
                        && this.getBestFriend().isPresent() 
                        && this.getBestFriend().get() == player.getUuid()) {
                            HumansClient.openFluteScreen(this);
                return ActionResult.SUCCESS;
            }
            return ActionResult.FAIL;
        } else {
            if (item == Humans.HEART_LOCKET && this.getAffinity(player.getUuid()) >= HIGH_AFFINITY) {
                if (this.getBestFriend().isPresent() && this.getBestFriend().get() != player.getUuid()) {
                    player.sendMessage(new TranslatableText("entity.human.best_friend_exists", name).formatted(Formatting.RED), true);
                    return ActionResult.FAIL;
                } else if (this.getBestFriend().isPresent()) {
                    if (player.isSneaking()) {
                        this.setBestFriend(null);
                        player.sendMessage(
                            new TranslatableText("entity.human.best_friend_removed", name)
                            .formatted(Formatting.RED), true);
                        return ActionResult.SUCCESS;
                    }
                    player.sendMessage(
                        new TranslatableText("entity.human.best_friend_already", name)
                        .formatted(Formatting.GOLD), true);
                    return ActionResult.SUCCESS;
                } else {
                    this.setBestFriend(player.getUuid());
                    player.sendMessage(
                        new TranslatableText("entity.human.best_friend_set", name)
                        .formatted(Formatting.GREEN), true);
                    return ActionResult.SUCCESS;
                }
            } else if (item == Humans.HEART_LOCKET) {
                player.sendMessage(
                    new TranslatableText("entity.human.low_affinity", 
                        name,
                        this.getAffinity(player.getUuid()),
                        HIGH_AFFINITY)
                    .formatted(Formatting.RED), false);
                return ActionResult.FAIL;
            } else if (item == Items.POPPY) {
                this.setHomePos(this.getBlockPos());
                return ActionResult.SUCCESS;
            } else if (Humans.HUMAN_LIKED_ITEMS.contains(item)) {
                tryEquip(stack);
                this.addAffinity(player.getUuid(), rankLikedItem(item));
                stack.setCount(stack.getCount() - 1);
                player.setStackInHand(hand, stack);
                return ActionResult.CONSUME;
            } else if (item == Humans.FLUTE 
                        && this.getBestFriend().isPresent() 
                        && this.getBestFriend().get() == player.getUuid()) {
                return ActionResult.SUCCESS;
            }
            return ActionResult.FAIL;
        }
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

    public Optional<UUID> getBestFriend() {
        return this.dataTracker.get(BEST_FRIEND);
    }

    public void setBestFriend(@Nullable UUID bestFriendUuid) {
        if (bestFriendUuid == null) {
            if (this.getBestFriend().isPresent()) {
                UUID bestFriendToRemove = this.getBestFriend().get();
                PlayerEntity bestFriend = this.world.getPlayerByUuid(bestFriendToRemove);
                if (bestFriend != null) HumansComponents.PARTY.get(bestFriend).remove(this.getUuid());
                this.dataTracker.set(STATE, SENTRY_STATE);
            }
            this.dataTracker.set(BEST_FRIEND, Optional.empty());
        } else {
            this.dataTracker.set(BEST_FRIEND, Optional.of(bestFriendUuid));
            this.dataTracker.set(STATE, FOLLOWING_STATE);
            PlayerEntity newBestFriend = this.world.getPlayerByUuid(bestFriendUuid);
            HumansComponents.PARTY.get(newBestFriend).add(this.getUuid());
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
        if (affinity > MAX_AFFINITY) affinity = MAX_AFFINITY;
        if (uuid != null && compound.contains(uuid.toString())) {
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
        } else if (uuid != null) {
            NbtCompound affinitySetup = new NbtCompound();
            affinitySetup.putInt("Value", affinity);
            affinitySetup.putInt("Cooldown", 48000);
            compound.put(uuid.toString(), affinitySetup);
        }
        this.dataTracker.set(AFFINITY, compound);
        if (affinity < 0) {
            setAngryAt(uuid);
        }
    }

    public void addAffinity (UUID uuid) {
        this.addAffinity(uuid, 1);
    }

    public void addAffinity(UUID uuid, int amount) {
        NbtCompound compound = this.dataTracker.get(AFFINITY);
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

    public boolean usesLegacySound() {
        return ((this.dataTracker.get(HUMAN_FLAGS) >> 0) & 1) == 1;
    }

    public boolean usesLegacyAnim() {
        return ((this.dataTracker.get(HUMAN_FLAGS) >> 1) & 1) == 1;
    }

    public boolean usesSlimSkin() {
        return ((this.dataTracker.get(HUMAN_FLAGS) >> 2) & 1) == 1;
    }

    public boolean getSkinLock() {
        return ((this.dataTracker.get(HUMAN_FLAGS) >> 3) & 1) == 1;
    }

    public void setFlags(byte flags) {
        this.dataTracker.set(HUMAN_FLAGS, flags);
    }

    public byte getFlags() {
        return this.dataTracker.get(HUMAN_FLAGS);
    }
    
    public void setFlag(byte mask, boolean value) {
        byte flags = this.getFlags();
        if (value)
            flags |= mask;
        else
            flags &= ~(mask);
        this.setFlags(flags);
    }

    public boolean getFlag(byte mask) {
        int shiftBy = 0;
        for (int i = 0; i < 8; i++) {
            if ((mask & 1) == 1) {
                shiftBy = i;
                break;
            }
        }
        byte flags = this.getFlags();
        return ((flags >> shiftBy) & 1) == 1;
    }

    public String getState() {
        return this.dataTracker.get(STATE);
    }

    public void setState(String newState) {
        this.dataTracker.set(STATE, newState);
    }

    public Optional<BlockPos> getHomePos() {
        return this.dataTracker.get(HOME_POS);
    }

    public void setHomePos(BlockPos newHome) {
        if (newHome == null) {
            this.dataTracker.set(HOME_POS, Optional.empty());
        } else {
            this.dataTracker.set(HOME_POS, Optional.of(newHome));
        }
    }

    @Override
    public void setCustomName(@Nullable Text name) {
        super.setCustomName(name);
        GameProfile gprofile = getSkinProfile();
        if (name != null && !this.getSkinLock() && (gprofile == null || gprofile.getName() != name.asString())) {
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
        if (nbt.contains("UseLegacySound") || nbt.contains("UseLegacyAnimation") || 
            nbt.contains("UseSlimSkin") || nbt.contains("SkinLock")) nbt = convertToFlagByte(nbt);
		super.writeCustomDataToNbt(nbt);
        NbtCompound compound = new NbtCompound();
        GameProfile prof = this.getSkinProfile();
        if (prof != null)
            NbtHelper.writeGameProfile(compound, prof);
		nbt.put("SkinProfile", compound);
        nbt.put("Affinity", this.dataTracker.get(AFFINITY));
        if (this.dataTracker.get(BEST_FRIEND).isPresent()) {
            nbt.putUuid("BestFriend", this.dataTracker.get(BEST_FRIEND).get());
        }
        nbt.putString("State", this.dataTracker.get(STATE));
        if (this.dataTracker.get(HOME_POS).isPresent()) {
            BlockPos home = this.dataTracker.get(HOME_POS).get();
            nbt.putIntArray("HomePos", new int[]{home.getX(), home.getY(), home.getZ()});
        }
        nbt.putByte("HumanFlags", this.dataTracker.get(HUMAN_FLAGS));
	}

    @Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("UseLegacySound") || nbt.contains("UseLegacyAnimation") || 
            nbt.contains("UseSlimSkin") || nbt.contains("SkinLock")) nbt = convertToFlagByte(nbt);
        
		super.readCustomDataFromNbt(nbt);
        if (nbt.contains("SkinProfile"))
            this.setSkinProfile(NbtHelper.toGameProfile(nbt.getCompound("SkinProfile")));
        if (nbt.contains("Affinity"))
            this.dataTracker.set(AFFINITY, nbt.getCompound("Affinity"));
        if (nbt.contains("BestFriend"))
            this.dataTracker.set(BEST_FRIEND, Optional.of(nbt.getUuid("BestFriend")));
        if (nbt.contains("State"))
            this.dataTracker.set(STATE, nbt.getString("State"));
        if (nbt.contains("HomePos")) {
            int[] home = nbt.getIntArray("HomePos");
            this.dataTracker.set(HOME_POS, Optional.of(new BlockPos(home[0], home[1], home[2])));
        }
	}

    /**
     * Previous versions of Humans used a different data layout from current versions. This converts it for us, since Fabric DataFixer API is not released.
     * @param compound The compound in the old format
     * @return The compound, updated to the new format.
     */
    private NbtCompound convertToFlagByte(NbtCompound compound) {
        byte flagByte = 0;
        if (compound.contains("UseLegacySound") && compound.getBoolean("UseLegacySound")) {
            flagByte |= 1;
            compound.remove("UseLegacySound");
        }
        if (compound.contains("UseLegacyAnimation") && compound.getBoolean("UseLegacyAnimation")) {
            flagByte |= 2;
            compound.remove("UseLegacyAnimation");
        }
        if (compound.contains("UseSlimSkin") && compound.getBoolean("UseSlimSkin")) {
            flagByte |= 4;
            compound.remove("UseSlimSkin");
        }
        if (compound.contains("SkinLock") && compound.getBoolean("SkinLock")) {
            flagByte |= 8;
            compound.remove("SkinLock");
        }
        compound.putByte("HumanFlags", flagByte);
        return compound;
    }

    /**
     * Used for Cape render handling.
     */
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

    /**
     * Used for Cape render handling when riding.
     */
    @Override
    public void tickRiding() {
        super.tickRiding();
        this.prevStrideDistance = this.strideDistance;
        this.strideDistance = 0.0F;
    }

    /**
     * Tick the Human. does a few things:
     * 1. Updates the cape angles for this Human.
     * 2. Ticks Affinity for those players who have it.
     * 3. Randomly gives food or health to the Human's best friend if they are low.
     */
    @Override
    public void tick() {
        super.tick();
        updateCapeAngles();
        tickAffinity();
        if (this.getBestFriend().isPresent()) {
            PlayerEntity bestFriend = this.world.getPlayerByUuid(this.getBestFriend().get());
            if (bestFriend != null && this.getRandom().nextInt(1000) < 2) {
                if (bestFriend.getHealth() < 4.0f) {
                    bestFriend.addStatusEffect(new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 1), this);
                    String name = this.hasCustomName() ? this.getCustomName().asString() : new TranslatableText("entity.humans.human").asString();
                    bestFriend.sendMessage(new TranslatableText("entity.human.gave_health", name), false);
                } else if (bestFriend.getHungerManager().getFoodLevel() < 4) {
                    bestFriend.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 10, 1), this);
                }
            }
        }
    }

    /**
     * Ticks affinity, which goes down by 5 every 2 in-game days.
     */
    private void tickAffinity() {
        NbtCompound compound = this.dataTracker.get(AFFINITY);
        if (compound != null) {
            for (String k : compound.getKeys()) {
                NbtCompound affinitySetup = compound.getCompound(k);
                if (affinitySetup != null && affinitySetup.contains("Cooldown")) {
                    int cooldown = affinitySetup.getInt("Cooldown") - 5;
                    if (cooldown <= 0) {
                        affinitySetup.putInt("Cooldown", 48000);
                        if (!this.getBestFriend().isPresent() || UUID.fromString(k) != this.getBestFriend().get()) {
                            affinitySetup.putInt("Value", affinitySetup.getInt("Value"));
                        }
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

    /**
     * Allows a Human to pick up items. May be removed eventually since they can be given items with right-click.
     */
    @Override
    protected void loot(ItemEntity item) {
        ItemStack stack = item.getStack();
        if (item.getThrower() != null && Humans.HUMAN_LIKED_ITEMS.contains(stack.getItem())) {
            this.addAffinity(item.getThrower(), item.getStack().getCount() * rankLikedItem(item.getStack().getItem()));
        }
        super.loot(item);
    }

    /**
     * Allows a Human to equip an item, or eat to refill their health.
     */
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

    /**
     * Set affinity to -1 when a Player hits them with low affinity.
     */
    @Override
    protected void applyDamage(DamageSource source, float amount) {
        if (!this.isInvulnerableTo(source) 
                && !source.isSourceCreativePlayer() 
                && source.getAttacker() instanceof PlayerEntity
                && this.getAffinity(source.getAttacker().getUuid()) < 200) {
            this.setAffinity(source.getAttacker().getUuid(), -1);
        }
        super.applyDamage(source, amount);
    }

    /**
     * Makes sure the hurt sound is the old "oof" sound if the Human is set to do so.
     */
    @Override
    @Nullable
	protected SoundEvent getHurtSound(DamageSource source) {
		return this.usesLegacySound() ? Humans.LEGACY_HURT_SOUND_EVENT : SoundEvents.ENTITY_PLAYER_HURT;
	}

    /**
     * Makes sure the death sound is the old "oof" sound if the Human is set to do so.
     */
    @Override
	@Nullable
	protected SoundEvent getDeathSound() {
		return this.usesLegacySound() ? Humans.LEGACY_HURT_SOUND_EVENT : SoundEvents.ENTITY_PLAYER_DEATH;
	}

    @Override
    public boolean canMoveVoluntarily() {
        return this.dataTracker.get(STATE) != WAITING_STATE && super.canMoveVoluntarily();
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
        if (this.angeryAt != this.previouslyAngryAt && player != null && !player.isCreative() && this.getAffinity(this.angeryAt) != -1) {
            this.setAffinity(this.angeryAt, -1);
        }
    }

    @Override
    public void chooseRandomAngerTime() {
        setAngerTime(ANGER_TIME_RANGE.get(this.random));
    }

    public boolean tryTeleportToBestFriend(PlayerEntity requester) {
        Humans.LOGGER.info("Try Teleport to Best Friend for " + this.getUuidAsString());
        if (this.getBestFriend().isPresent() && this.getBestFriend().get().equals(requester.getUuid())) {
            BlockPos blockPos = requester.getBlockPos();

            for(int i = 0; i < 10; ++i) {
                int j = this.getRandom().nextInt(7) - 3;
                int k = this.getRandom().nextInt(3) - 1;
                int l = this.getRandom().nextInt(7) - 3;;
                boolean bl = this.tryTeleportTo(blockPos.getX() + j, blockPos.getY() + k, blockPos.getZ() + l, requester);
                if (bl) {
                    return true;
                }
            }
        }
        Humans.LOGGER.info("Could not teleport to Best Friend.");
        return false;
    }

    private boolean tryTeleportTo(int x, int y, int z, PlayerEntity target) {
        if (Math.abs((double)x - target.getX()) < 2.0D && Math.abs((double)z - target.getZ()) < 2.0D) {
			return false;
		} else if (!this.canTeleportTo(new BlockPos(x, y, z))) {
			return false;
		} else {
			this.refreshPositionAndAngles((double)x + 0.5D, (double)y, (double)z + 0.5D, this.getYaw(), this.getPitch());
			this.navigation.stop();
			return true;
		}
    }
    private boolean canTeleportTo(BlockPos pos) {
        PathNodeType pathNodeType = LandPathNodeMaker.getLandNodeType(this.world, pos.mutableCopy());
		if (pathNodeType != PathNodeType.WALKABLE) {
			return false;
		} else {
            BlockPos blockPos = pos.subtract(this.getBlockPos());
            return this.world.isSpaceEmpty(this, this.getBoundingBox().offset(blockPos));
		}
    }
}
