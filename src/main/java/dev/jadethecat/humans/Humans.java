package dev.jadethecat.humans;

import dev.jadethecat.humans.entity.HumanEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.impl.registry.sync.FabricRegistry;
import net.fabricmc.fabric.mixin.object.builder.DefaultAttributeRegistryMixin;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class Humans implements ModInitializer {
	public static final EntityType<HumanEntity> HUMAN = Registry.register(
		Registry.ENTITY_TYPE,
		new Identifier("humans", "human"),
		FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, HumanEntity::new).dimensions(EntityDimensions.fixed(0.6f, 1.8f)).trackRangeBlocks(50).trackedUpdateRate(1).build()
	);
	public static final Item HUMAN_SPAWN_EGG = new SpawnEggItem(HUMAN, 0xFFFFFF, 0x000000, new Item.Settings().group(ItemGroup.MISC));

	public static final Identifier LEGACY_HURT_SOUND_ID = new Identifier("humans", "entity.human.hurt");
	public static SoundEvent LEGACY_HURT_SOUND_EVENT = new SoundEvent(LEGACY_HURT_SOUND_ID);

	@Override
	public void onInitialize() {
		FabricDefaultAttributeRegistry.register(HUMAN, HumanEntity.createMobAttributes());
		Registry.register(Registry.ITEM, new Identifier("humans", "human_spawn_egg"), HUMAN_SPAWN_EGG);
		Registry.register(Registry.SOUND_EVENT, LEGACY_HURT_SOUND_ID, LEGACY_HURT_SOUND_EVENT);
		FabricDefaultAttributeRegistry.register(HUMAN, HumanEntity.createHumanAttributes());;
	}
}
