package com.example.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.example.aimassist.AimAssistControllerTodoAi;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.CameraType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.example.Constants.MINECRAFT_CLIENT_INSTANCE;
import static com.example.UntitledClient.config;
import static com.example.UntitledClient.headRunCameraOffset;
import static com.example.UntitledClient.HEAD_RUN_OFFSET_TYPE;

import net.minecraft.client.renderer.GameRenderer;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    // codex start
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void aimAssistRenderTodoAi(DeltaTracker delta, CallbackInfo ci) {
        AimAssistControllerTodoAi.render(MINECRAFT_CLIENT_INSTANCE, delta.getGameTimeDeltaPartialTick(false));
    }
    //codex end
    @Unique
    private boolean isRenderingHandBobbing;

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void beginHandBobbing(CallbackInfo ci) {
        if (headRunCameraOffset != HEAD_RUN_OFFSET_TYPE.NONE) {
            ci.cancel();
            return;
        }
        isRenderingHandBobbing = true;
    }

    @Inject(method = "renderItemInHand", at = @At("TAIL"))
    private void endHandBobbing(CallbackInfo ci) {
        isRenderingHandBobbing = false;
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void onBobView(
            PoseStack poseStack, float partialTick, CallbackInfo ci) { //codex (old code snippet) CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (config.isViewBobbingCameraShakeDisabled &&
                !isRenderingHandBobbing &&
                MINECRAFT_CLIENT_INSTANCE.options.getCameraType() == CameraType.FIRST_PERSON) {
            ci.cancel();
        }
    }

//    @Inject(
//            at = @At(value = "HEAD"),
//            method = "nightVisionScale",
//            cancellable = true)
//    private static void onGetNightVisionStrength(
//            LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> cir) {
//        if (config.isFullbrightEnabled) {
//            cir.setReturnValue(1.0f);
//            cir.cancel();
//        }
//    }
}
