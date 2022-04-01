package dev.jadethecat.humans;

import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.jadethecat.humans.entity.HumanEntity;
import dev.jadethecat.humans.events.HumansServerPlay;
import dev.jadethecat.humans.item.FluteItem;
import dev.jadethecat.humans.world.Spawning;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.server.command.CommandManager;
import net.minecraft.sound.SoundEvent;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class Humans implements ModInitializer {
	// Human Entity & Spawn Egg
	public static final EntityType<HumanEntity> HUMAN = Registry.register(
		Registry.ENTITY_TYPE,
		new Identifier("humans", "human"),
		FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, HumanEntity::new).dimensions(EntityDimensions.fixed(0.6f, 1.8f)).trackRangeBlocks(50).trackedUpdateRate(1).build()
	);
	public static final Item HUMAN_SPAWN_EGG = new SpawnEggItem(HUMAN, 0x463AA5, 0x00AFAF, new Item.Settings().group(ItemGroup.MISC));

	// Sound Events
	public static final Identifier LEGACY_HURT_SOUND_ID = new Identifier("humans", "entity.human.hurt");
	public static SoundEvent LEGACY_HURT_SOUND_EVENT = new SoundEvent(LEGACY_HURT_SOUND_ID);

	// Tags
	public static final TagKey<Item> HUMAN_LIKED_ITEMS = TagKey.of(Registry.ITEM_KEY, new Identifier("humans", "human_liked_items"));
	public static final TagKey<Item> HUMAN_FOOD = TagKey.of(Registry.ITEM_KEY, new Identifier("humans", "human_food"));
	public static final TagKey<EntityType<?>> HUMAN_IGNORED_MOBS = TagKey.of(Registry.ENTITY_TYPE_KEY, new Identifier("humans", "human_ignored_mobs"));
	public static final TagKey<Block> HUMAN_SPAWNABLE = TagKey.of(Registry.BLOCK_KEY, new Identifier("humans", "human_spawnable"));

	// Items
	public static final Item FLUTE = new FluteItem();
	public static final Item HEART_LOCKET = new Item(new FabricItemSettings().group(ItemGroup.MISC));

	public static final Logger LOGGER = LogManager.getLogger("Humans");

	@Override
	public void onInitialize() {
		FabricDefaultAttributeRegistry.register(HUMAN, HumanEntity.createMobAttributes());
		Registry.register(Registry.ITEM, new Identifier("humans", "human_spawn_egg"), HUMAN_SPAWN_EGG);
		Registry.register(Registry.ITEM, new Identifier("humans", "flute"), FLUTE);
		Registry.register(Registry.ITEM, new Identifier("humans", "heart_locket"), HEART_LOCKET);
		Registry.register(Registry.SOUND_EVENT, LEGACY_HURT_SOUND_ID, LEGACY_HURT_SOUND_EVENT);
		FabricDefaultAttributeRegistry.register(HUMAN, HumanEntity.createHumanAttributes());
		AutoConfig.register(HumansConfig.class, Toml4jConfigSerializer::new);
		HumansServerPlay.initServerPlayEvents();
		HumanEntity.initStates();
		Spawning.addHumanToBiomes();
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(CommandManager.literal("humans")
				.then(CommandManager.literal("clearbestfriend")
					.then(CommandManager.argument("target", EntityArgumentType.entity())
					.requires(source -> source.hasPermissionLevel(4))
					.executes(context -> {
						Entity e = EntityArgumentType.getEntity(context, "target");
						if (e instanceof HumanEntity) {
							HumanEntity h = (HumanEntity)e;
							h.setBestFriend(null);
							context.getSource().sendFeedback(new TranslatableText("command.humans.cleared_best_friend", h.getName()), true);
							return 1;
						}
						Text message = new TranslatableText("command.humans.not_human");
						context.getSource().sendError(message);
						throw new SimpleCommandExceptionType(message).create();
					}))
				)
			);
        });
	}
}
