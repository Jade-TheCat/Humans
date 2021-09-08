package dev.jadethecat.humans.client;

import com.google.common.collect.ImmutableList;

import dev.jadethecat.humans.entity.HumanEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.util.math.MatrixStack;

public class HumanEntityModel extends EntityModel<HumanEntity> {
    private final ModelPart base;

    public HumanEntityModel(ModelPart modelPart) {
        this.base = modelPart.getChild(EntityModelPartNames.CUBE);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        modelPartData.addChild(EntityModelPartNames.CUBE, ModelPartBuilder.create()
            .uv(0, 0)
            .cuboid(-6f, 12f, -6f, 12f, 12f, 12f), ModelTransform.pivot(0f, 0f, 0f));
        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(HumanEntity entity, float limbAngle, float limbDistance, float animationProgress,
            float headYaw, float headPitch) {
        
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green,
            float blue, float alpha) {
        ImmutableList.of(this.base).forEach((modelRenderer) -> {
            modelRenderer.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        });
        
    }
}
