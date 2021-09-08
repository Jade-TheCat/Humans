package dev.jadethecat.humans.client;

import dev.jadethecat.humans.entity.HumanEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.util.Identifier;

public class HumanEntityRenderer extends MobEntityRenderer<HumanEntity, PlayerEntityModel<HumanEntity>> {

    public HumanEntityRenderer(EntityRendererFactory.Context context, boolean slim) {
        super(context, new PlayerEntityModel<>(slim ? context.getPart(EntityModelLayers.PLAYER_SLIM) : context.getPart(EntityModelLayers.PLAYER), slim), 0.5f);
        this.addFeature(new ArmorFeatureRenderer<>(this, new BipedEntityModel<>(
            context.getPart(slim ? EntityModelLayers.PLAYER_SLIM_INNER_ARMOR : EntityModelLayers.PLAYER_INNER_ARMOR)), 
            new BipedEntityModel<>(context.getPart(slim ? EntityModelLayers.PLAYER_SLIM_OUTER_ARMOR : EntityModelLayers.PLAYER_OUTER_ARMOR))));
    }

    @Override
    public Identifier getTexture(HumanEntity entity) {
       return new Identifier("humans", "textures/entity/human/steve.png");
    }
    
}
