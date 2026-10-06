package com.example;

import com.example.aimassist.AimCrosshairMotionTodoAi;
import com.example.aimassist.AimGeometryTodoAi.Rotation;
import net.minecraft.world.phys.AABB;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Checks actual client camera translation without mouse rotation. */
public final class ApproachMouseGameTestTodoAi implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            context.waitFor(mc -> mc.player != null && mc.level != null && mc.gui.screen() == null);
            context.waitTicks(3);
            context.runOnClient(mc -> {
                var player = mc.player;
                player.setYRot(0);
                player.setXRot(0);
                var eyes = player.getEyePosition();
                var position = player.position();
                var box = new AABB(eyes.x - 0.5, eyes.y - 0.5, eyes.z + 3,
                        eyes.x + 0.5, eyes.y + 0.5, eyes.z + 4);
                var crosshair = new Rotation(player.getYRot(), player.getXRot());
                var inside = AimCrosshairMotionTodoAi.sample(eyes, box, crosshair);
                player.setPos(position.x + 1, position.y, position.z);
                var outside = AimCrosshairMotionTodoAi.sample(player.getEyePosition(), box, crosshair);
                if (AimCrosshairMotionTodoAi.allowsAssist(inside, outside))
                    throw new AssertionError("Static view still assisted walking out of the hitbox");
                player.setPos(position.x + 2, position.y, position.z);
                var farther = AimCrosshairMotionTodoAi.sample(player.getEyePosition(), box, crosshair);
                if (AimCrosshairMotionTodoAi.allowsAssist(outside, farther))
                    throw new AssertionError("Static view still assisted walking away");
                if (!AimCrosshairMotionTodoAi.allowsAssist(farther, outside) ||
                        !AimCrosshairMotionTodoAi.allowsAssist(outside, outside))
                    throw new AssertionError("Approaching or stationary crosshair incorrectly blocked");
                if (player.getYRot() != 0 || player.getXRot() != 0)
                    throw new AssertionError("Translation test unexpectedly changed view angle");
            });
            System.out.println("PASS: static client view respects walking toward and away from hitbox");
        }
    }
}
