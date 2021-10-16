package dev.jadethecat.humans.components;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import net.minecraft.util.Identifier;

public class HumansComponents implements EntityComponentInitializer {

	public static final ComponentKey<PartyComponent> PARTY =
        ComponentRegistry.getOrCreate(new Identifier("humans", "party"), PartyComponent.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(PARTY, player -> new PartyComponent(player), RespawnCopyStrategy.ALWAYS_COPY);
    }
}
