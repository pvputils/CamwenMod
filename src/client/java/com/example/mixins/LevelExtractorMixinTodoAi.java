package com.example.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.example.UntitledClient.config;

/** 1.21.4 renders entities directly rather than through LevelExtractor. */
@Mixin(LevelRenderer.class)
public abstract class LevelExtractorMixinTodoAi {
    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    private void hideRiddenHorse(Entity entity, double cameraX, double cameraY, double cameraZ,
            float partialTick, PoseStack poseStack, MultiBufferSource buffers, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (config.isRiddenHorseRenderingDisabled && entity instanceof AbstractHorse horse
                && minecraft.player != null && minecraft.player.getVehicle() == horse) {
            ci.cancel();
        }
    }
}