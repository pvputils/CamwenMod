package com.example.mixins;

import com.example.UntitledClient;
import com.example.CameraSnapshotTodoAi;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At("HEAD")) //codex (old code snippet) @Inject(method = "render", at = @At("HEAD"))
    private void onRender(
            GraphicsResourceAllocator resourceAllocator,
            DeltaTracker deltaTracker,
            boolean renderOutline,
            Camera camera, //codex (old code snippet) CameraRenderState cameraState,
            GameRenderer gameRenderer, //codex (old code snippet) Matrix4fc modelViewMatrix,
            Matrix4f modelViewMatrix, //codex (old code snippet) GpuBufferSlice terrainFog,
            Matrix4f projectionMatrix, //codex (old code snippet) Vector4f fogColor, boolean shouldRenderSky,
            CallbackInfo ci
    ) {
//        UntitledClient.projectionMatrix = new Matrix4f(projectionMatrix);
        UntitledClient.cameraRenderState = new CameraSnapshotTodoAi(camera, projectionMatrix); //codex (old code snippet) UntitledClient.cameraRenderState = cameraState;
    }
}
