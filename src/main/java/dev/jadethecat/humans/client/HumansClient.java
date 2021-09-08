package dev.jadethecat.humans.client;

import dev.jadethecat.humans.Humans;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class HumansClient implements ClientModInitializer {

    public static final EntityModelLayer MODEL_HUMAN_LAYER = new EntityModelLayer(new Identifier("humans", "human"), "main");

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(Humans.HUMAN, (context) -> {
            return new HumanEntityRenderer(context, false);
        });
        EntityModelLayerRegistry.registerModelLayer(MODEL_HUMAN_LAYER, HumanEntityModel::getTexturedModelData);
    }
    
}
