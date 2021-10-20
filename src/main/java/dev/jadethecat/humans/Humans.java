package dev.jadethecat.humans;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.jadethecat.humans.entity.HumanEntity;
import dev.jadethecat.humans.item.FluteItem;
import dev.jadethecat.humans.network.HumansServerPlay;
import dev.jadethecat.humans.world.Spawning;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.tag.TagFactory;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.sound.SoundEvent;
import net.minecraft.tag.Tag;
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
	public static final Tag<Item> HUMAN_LIKED_ITEMS = TagFactory.ITEM.create(new Identifier("humans", "human_liked_items"));
	public static final Tag<Item> HUMAN_FOOD = TagFactory.ITEM.create(new Identifier("humans", "human_food"));
	public static final Tag<EntityType<?>> HUMAN_IGNORED_MOBS = TagFactory.ENTITY_TYPE.create(new Identifier("humans", "human_ignored_mobs"));
	public static final Tag<Block> HUMAN_SPAWNABLE = TagFactory.BLOCK.create(new Identifier("humans", "human_spawnable"));

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
		HumansServerPlay.initReceiviers();
		HumanEntity.initStates();
		Spawning.addHumanToBiomes();
	}
}
