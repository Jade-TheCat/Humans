package dev.jadethecat.humans.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import dev.jadethecat.humans.entity.HumanEntity;
import dev.jadethecat.humans.client.HumanEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.entity.Entity;
import net.minecraft.resource.ResourceManager;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    private static final Map<String, EntityRendererFactory<HumanEntity>> HUMAN_RENDERER_FACTORIES = 
        ImmutableMap.of("default", (context) -> {
            return new HumanEntityRenderer(context, false);
        }, "slim", (context) -> {
            return new HumanEntityRenderer(context, true);
        });

    private static Map<String, EntityRenderer<? extends HumanEntity>> reloadHumanRenderers(EntityRendererFactory.Context ctx) {
        Builder<String, EntityRenderer<? extends HumanEntity>> builder = ImmutableMap.builder();
        HUMAN_RENDERER_FACTORIES.forEach((string, entityRendererFactory) -> {
            try {
                builder.put(string, entityRendererFactory.create(ctx));
            } catch (Exception ex) {
                throw new IllegalArgumentException("Failed to create model for " + string, ex);
            }
        });
        return builder.build();
    }

    private  Map<String, EntityRenderer<? extends HumanEntity>> humanRenderers = ImmutableMap.of();


    @Inject(at = @At("TAIL"), method = "getRenderer(Lnet/minecraft/entity/Entity;)Lnet/minecraft/client/render/entity/EntityRenderer;", cancellable = true)
    private void getRenderer(Entity entity, CallbackInfoReturnable<EntityRenderer<?>> info) {
        if (entity instanceof HumanEntity) {
            String string = ((HumanEntity)entity).getModel();
			EntityRenderer<? extends HumanEntity> entityRenderer = (EntityRenderer<? extends HumanEntity>)this.humanRenderers.get(string);
			info.setReturnValue(entityRenderer != null ? entityRenderer : (EntityRenderer<? extends HumanEntity>)this.humanRenderers.get("default"));
        }
    }

    @Inject(at = @At("TAIL"), method = "reload(Lnet/minecraft/resource/ResourceManager;)V", locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void reload(ResourceManager man, CallbackInfo info, EntityRendererFactory.Context ctx) {
        this.humanRenderers = reloadHumanRenderers(ctx);
    }
}
