package dev.jadethecat.humans.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.jadethecat.humans.entity.HumanEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

/*
    Adapted from Old Walking Animation (https://gitlab.com/TecnaGamer/OldWalkingAnimation/-/blob/main/src/main/java/me/fivespace/oldwalkinganimation/mixin/BipedEntityModelMixin.java) by TecnaGamer to support Humans.
*/

@Mixin(BipedEntityModel.class)
public class BipedEntityModelMixin {

	@Shadow public ModelPart rightArm;
	@Shadow public ModelPart leftArm;

	@Inject(method="setAngles", at=@At(value = "FIELD",
									   target = "Lnet/minecraft/client/model/ModelPart;roll:F",
									   ordinal = 1,
									   shift = At.Shift.AFTER))
	private void setAngles(LivingEntity livingEntity, float f, float g, float h, float i, float j, CallbackInfo ci) {
		if (livingEntity instanceof HumanEntity && ((HumanEntity)livingEntity).usesLegacyAnim()) {
            this.rightArm.pitch = MathHelper.cos(f * 0.6662F + 3.1415927F) * 2.0F * g;
            this.leftArm.pitch = MathHelper.cos(f * 0.6662F) * 2.0F * g;
            this.rightArm.roll = (MathHelper.cos(f * 0.2312F) + 1.0F) * 1.0F * g;
            this.leftArm.roll = (MathHelper.cos(f * 0.2812F) - 1.0F) * 1.0F * g;
		}
	}

}


