package com.example;

import com.example.aimassist.AimCrosshairMotionTodoAi;
import com.example.aimassist.AimAssistControllerTodoAi;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.client.Minecraft;
import java.util.Map;
import java.util.UUID;
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
                // codex start
                try {
                    var cfg = AimAssistControllerTodoAi.config().targetting;
                    cfg.enabled = true;
                    cfg.requires.attackWindow = 200;
                    cfg.requires.notBreaking = false;
                    AimAssistControllerTodoAi.config().targets.armorStand = true;
                    player.setPos(position.x, position.y, position.z);
                    var target = new ArmorStand(mc.level, eyes.x, eyes.y, eyes.z + 2);
                    target.setBoundingBox(new AABB(eyes.x - 0.5, eyes.y - 0.5, eyes.z + 2,
                            eyes.x + 0.5, eyes.y + 0.5, eyes.z + 3));
                    var stateField = AimAssistControllerTodoAi.class.getDeclaredField("TARGETTING");
                    stateField.setAccessible(true);
                    var state = stateField.get(null);
                    for (String name : new String[]{"target", "start", "end"}) {
                        var field = state.getClass().getDeclaredField(name);
                        field.setAccessible(true);
                        field.set(state, name.equals("target") ? target :
                                new Rotation(0, name.equals("start") ? 2 : 0));
                    }
                    var historyField = AimAssistControllerTodoAi.class.getDeclaredField("CROSSHAIR_HISTORY");
                    historyField.setAccessible(true);
                    var history = (Map<UUID, AimCrosshairMotionTodoAi.Observation>) historyField.get(null);
                    history.put(target.getUUID(), new AimCrosshairMotionTodoAi.Observation(
                            eyes, target.getBoundingBox(), new Rotation(0, 2)));
                    mc.hitResult = new EntityHitResult(target);
                    var apply = AimAssistControllerTodoAi.class.getDeclaredMethod("apply",
                            Minecraft.class, state.getClass(), cfg.getClass(), boolean.class, float.class);
                    apply.setAccessible(true);
                    player.setXRot(3);
                    AimAssistControllerTodoAi.attackAttempt();
                    apply.invoke(null, mc, state, cfg, true, 1f);
                    if (player.getXRot() != 3)
                        throw new AssertionError("Targeting dragged the view while moving inside toward exit");
                    history.put(target.getUUID(), new AimCrosshairMotionTodoAi.Observation(
                            eyes, target.getBoundingBox(), new Rotation(0, 3)));
                    AimAssistControllerTodoAi.attackAttempt();
                    apply.invoke(null, mc, state, cfg, true, 1f);
                    if (!(player.getXRot() < 3))
                        throw new AssertionError("Stationary targeting must still approach neutral pitch");
                } catch (ReflectiveOperationException error) {
                    throw new AssertionError("Inside-to-outside targeting integration", error);
                }
                //codex end
            });
            System.out.println("PASS: player translation and actual targeting exit/stationary behavior");
        }
    }
}
