package dev.jadethecat.humans.client;

import dev.jadethecat.humans.entity.HumanEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3f;

public class HumanCapeFeatureRenderer extends FeatureRenderer<HumanEntity, PlayerEntityModel<HumanEntity>>{

    HumanEntityRenderer renderer;
    public HumanCapeFeatureRenderer(FeatureRendererContext<HumanEntity, PlayerEntityModel<HumanEntity>> context) {
        super(context);
        this.renderer = (HumanEntityRenderer)context;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, HumanEntity human,
            float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw,
            float headPitch) {
                if (renderer.canRenderCapeTexture(human) && !human.isInvisible() && renderer.getCapeTexture(human) != null) {
                    ItemStack itemStack = human.getEquippedStack(EquipmentSlot.CHEST);
                    if (!itemStack.isOf(Items.ELYTRA)) {
                        matrices.push();
                        matrices.translate(0.0D, 0.0D, 0.125D);
                        double d = MathHelper.lerp((double)tickDelta, human.prevCapeX, human.capeX) - MathHelper.lerp((double)tickDelta, human.prevX, human.getX());
                        double e = MathHelper.lerp((double)tickDelta, human.prevCapeY, human.capeY) - MathHelper.lerp((double)tickDelta, human.prevY, human.getY());
                        double m = MathHelper.lerp((double)tickDelta, human.prevCapeZ, human.capeZ) - MathHelper.lerp((double)tickDelta, human.prevZ, human.getZ());
                        float n = human.prevBodyYaw + (human.bodyYaw - human.prevBodyYaw);
                        double o = (double)MathHelper.sin(n * 0.017453292F);
                        double p = (double)(-MathHelper.cos(n * 0.017453292F));
                        float q = (float)e * 10.0F;
                        q = MathHelper.clamp(q, -6.0F, 32.0F);
                        float r = (float)(d * o + m * p) * 100.0F;
                        r = MathHelper.clamp(r, 0.0F, 150.0F);
                        float s = (float)(d * p - m * o) * 100.0F;
                        s = MathHelper.clamp(s, -20.0F, 20.0F);
                        if (r < 0.0F) {
                            r = 0.0F;
                        }
        
                        float t = MathHelper.lerp(tickDelta, human.prevStrideDistance, human.strideDistance);
                        q += MathHelper.sin(MathHelper.lerp(tickDelta, human.prevHorizontalSpeed, human.horizontalSpeed) * 6.0F) * 32.0F * t;
                        if (human.isInSneakingPose()) {
                            q += 25.0F;
                        }
        
                        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(6.0F + r / 2.0F + q));
                        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(s / 2.0F));
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180.0F - s / 2.0F));
                        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntitySolid(renderer.getCapeTexture(human)));
                        ((PlayerEntityModel<HumanEntity>)this.getContextModel()).renderCape(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
                        matrices.pop();
                    }
                }
    }
    
}
