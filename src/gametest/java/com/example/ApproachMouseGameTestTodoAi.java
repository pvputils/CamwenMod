package com.example;

import java.util.Arrays;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Ensures the mouse redirect attaches and ordinary mouse turning still works. */
public final class ApproachMouseGameTestTodoAi implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            context.waitFor(mc -> mc.player != null && mc.level != null && mc.gui.screen() == null);
            context.waitTicks(3);
            context.runOnClient(mc -> {
                var hook = Arrays.stream(mc.mouseHandler.getClass().getDeclaredMethods())
                        .filter(method -> method.getName().contains("captureMouseTurnTodoAi"))
                        .findFirst().orElseThrow(() -> new AssertionError("Directional mouse redirect did not attach"));
                try {
                    hook.setAccessible(true);
                    float before = mc.player.getYRot();
                    hook.invoke(mc.mouseHandler, mc.player, 20.0, 0.0);
                    float after = mc.player.getYRot();
                    if (before == after) throw new AssertionError("Mouse redirect blocked ordinary turning");
                    var field = com.example.aimassist.AimAssistControllerTodoAi.class.getDeclaredField("MOUSE_MOTION");
                    field.setAccessible(true);
                    var tracker = (com.example.aimassist.AimMouseMotionTodoAi) field.get(null);
                    var motion = tracker.consume();
                    if (motion == null || motion.before().yaw() != before || motion.after().yaw() != after || tracker.consume() != null)
                        throw new AssertionError("Mouse rotation was not captured for exactly one assist frame");
                } catch (ReflectiveOperationException error) {
                    throw new AssertionError("Mouse capture integration", error);
                }
            });
            System.out.println("PASS: 26.2 mouse redirect attaches and preserves mouse turning");
        }
    }
}
