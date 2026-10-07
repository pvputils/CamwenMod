package com.example.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.example.UntitledClient.config;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixinTodoAi {
    @Inject(method = "isEntityVisible", at = @At("HEAD"), cancellable = true)
    private void hideRiddenHorse(
            Entity entity,
            Frustum frustum,
            double cameraX,
            double cameraY,
            double cameraZ,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (config.isRiddenHorseRenderingDisabled
                && entity instanceof AbstractHorse horse
                && minecraft.player != null
                && minecraft.player.getVehicle() == horse) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
