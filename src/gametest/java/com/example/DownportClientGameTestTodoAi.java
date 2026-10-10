package com.example;

import com.example.aimassist.AimAssistScreenTodoAi;
import com.example.aimassist.TargetingMarginPickTodoAi;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.lwjgl.glfw.GLFW;

/** Exercises version-sensitive hooks in a real 1.21.4 client and flat world. */
public final class DownportClientGameTestTodoAi implements FabricClientGameTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            context.waitFor(mc -> UntitledClient.cameraRenderState != null);
            context.runOnClient(mc -> {
                check(UntitledClient.cameraRenderState.projectionMatrix.isFinite(), "finite world projection");
                check(TargetingMarginPickTodoAi.pick(mc, null) != null, "GameRenderer picking invoker");
                UntitledClient.config.isInventoryKeyHoldEnabled = true;
            });
            context.getInput().holdKey(GLFW.GLFW_KEY_E);
            context.waitForScreen(InventoryScreen.class);
            context.getInput().releaseKey(GLFW.GLFW_KEY_E);
            context.waitForScreen(null);
            context.runOnClient(mc -> {
                UntitledClient.config.isInventoryKeyHoldEnabled = false;
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.setScreen(new AimAssistScreenTodoAi(null));
            });
            context.waitForScreen(AimAssistScreenTodoAi.class);
            context.waitTicks(3);
            context.setScreen(() -> null);
            context.waitTicks(3);
            context.runOnClient(mc -> {
                UntitledClient.headRunCameraOffset = UntitledClient.HEAD_RUN_OFFSET_TYPE.LEFT;
            });
            context.waitTicks(3);
            context.runOnClient(mc -> {
                float expectedYaw = mc.player.getYRot() + UntitledClient.headRunCameraOffset.delta;
                check(Math.abs(mc.gameRenderer.getMainCamera().getYRot() - expectedYaw) < 0.01f,
                        "third-person camera orbit yaw");
                UntitledClient.headRunCameraOffset = UntitledClient.HEAD_RUN_OFFSET_TYPE.NONE;
                check(TargetingMarginPickTodoAi.pick(mc, 0.0f) != null, "margin probe restores state");
                Utils.onXrayChange();
            });
            context.waitTicks(3);
            context.runOnClient(mc -> Utils.onXrayChange());
            context.waitTicks(3);
            System.out.println("PASS: 1.21.4 world rendering, picking, inventory hold, config screen, third-person and Sodium rebuild");
        }
    }
}
