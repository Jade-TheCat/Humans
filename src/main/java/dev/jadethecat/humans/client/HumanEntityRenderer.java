package dev.jadethecat.humans.client;

import java.util.Map;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import dev.jadethecat.humans.entity.HumanEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.feature.StuckArrowsFeatureRenderer;
import net.minecraft.util.Identifier;

public class HumanEntityRenderer extends MobEntityRenderer<HumanEntity, PlayerEntityModel<HumanEntity>> {
    private MinecraftClient mcClient;

    public HumanEntityRenderer(EntityRendererFactory.Context context, boolean slim) {
        super(context, new PlayerEntityModel<>(slim ? context.
        getPart(EntityModelLayers.PLAYER_SLIM) : context.getPart(EntityModelLayers.PLAYER), slim), 0.5f);

        this.mcClient = MinecraftClient.getInstance();
        this.addFeature(new ArmorFeatureRenderer<>(this, new BipedEntityModel<>(
            context.getPart(slim ? EntityModelLayers.PLAYER_SLIM_INNER_ARMOR : EntityModelLayers.PLAYER_INNER_ARMOR)), 
            new BipedEntityModel<>(context.getPart(slim ? EntityModelLayers.PLAYER_SLIM_OUTER_ARMOR : EntityModelLayers.PLAYER_OUTER_ARMOR))));
        this.addFeature(new HeadFeatureRenderer<>(this, context.getModelLoader()));
        this.addFeature(new ElytraFeatureRenderer<>(this, context.getModelLoader()));
        this.addFeature(new HeldItemFeatureRenderer<>(this));
        this.addFeature(new StuckArrowsFeatureRenderer<>(context, this));
        this.addFeature(new HumanCapeFeatureRenderer(this));
    }

    @Override
    public Identifier getTexture(HumanEntity entity) {
        GameProfile prof = entity.getSkinProfile();
        if (prof != null) {
            Map<Type,MinecraftProfileTexture> map = this.mcClient.getSkinProvider().getTextures(prof);
            Identifier id = map.containsKey(Type.SKIN) ? mcClient.getSkinProvider().loadSkin(map.get(Type.SKIN), Type.SKIN) 
                : DefaultSkinHelper.getTexture(PlayerEntity.getUuidFromProfile(entity.getSkinProfile()));
            return id;
        }
        if (entity.usesSlimSkin()) return new Identifier("minecraft", "textures/entity/alex.png");
        if (entity.usesLegacyAnim() || entity.usesLegacySound()) return new Identifier("humans", "textures/entity/human/legasteve.png");
        return new Identifier("minecraft", "textures/entity/steve.png");
    }
    
    public boolean canRenderCapeTexture(HumanEntity entity) {
        GameProfile prof = entity.getSkinProfile();
        if (prof != null) {
            return getTextures(prof).containsKey(Type.CAPE);
        }
        return false;
    }

    public Identifier getCapeTexture(HumanEntity human) {
        GameProfile prof = human.getSkinProfile();
        if (prof != null) {
            MinecraftProfileTexture texture = getTextures(prof).get(Type.CAPE);
            return mcClient.getSkinProvider().loadSkin(texture, Type.CAPE);
        }
        return null;
    }

    private Map<Type,MinecraftProfileTexture> getTextures(GameProfile profile) {
        return mcClient.getSkinProvider().getTextures(profile);
    }
}
