package com.example;

import com.example.aimassist.AimCrosshairMotionTodoAi;
import com.example.aimassist.AimAssistControllerTodoAi;
import net.minecraft.client.player.RemotePlayer;
import com.mojang.authlib.GameProfile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.client.Minecraft;
import java.util.Map;
import java.util.UUID;
import org.lwjgl.glfw.GLFW;
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
            // codex start
            context.getInput().holdMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
            //codex end
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
                    var target = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "ExitRegression"));
                    target.setPos(eyes.x, eyes.y, eyes.z + 2);
                    target.setBoundingBox(new AABB(eyes.x - 0.5, eyes.y - 0.5, eyes.z + 2,
                            eyes.x + 0.5, eyes.y + 0.5, eyes.z + 3));
                    target.setId(1_000_000);
                    mc.level.addEntity(target);
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
                    var gatesField = AimAssistControllerTodoAi.class.getDeclaredField("CROSSHAIR_GATES");
                    gatesField.setAccessible(true);
                    var gates = (Map<UUID, AimCrosshairMotionTodoAi.Gate>) gatesField.get(null);
                    var gate = new AimCrosshairMotionTodoAi.Gate();
                    gates.put(target.getUUID(), gate);
                    mc.hitResult = new EntityHitResult(target);
                    var apply = AimAssistControllerTodoAi.class.getDeclaredMethod("apply",
                            Minecraft.class, state.getClass(), cfg.getClass(), float.class); //codex (old code snippet) Minecraft.class, state.getClass(), cfg.getClass(), boolean.class, float.class);
                    apply.setAccessible(true);
                    player.setXRot(3);
                    long inputTime = System.nanoTime();
                    gate.update(AimCrosshairMotionTodoAi.sample(eyes, target.getBoundingBox(), new Rotation(0, 2)),
                            AimCrosshairMotionTodoAi.sample(eyes, target.getBoundingBox(), new Rotation(0, 3)), inputTime);
                    AimAssistControllerTodoAi.attackAttempt();
                    apply.invoke(null, mc, state, cfg, 1f); //codex (old code snippet) apply.invoke(null, mc, state, cfg, true, 1f);
                    if (player.getXRot() != 3)
                        throw new AssertionError("Targeting dragged the view while moving inside toward exit");
                    history.put(target.getUUID(), new AimCrosshairMotionTodoAi.Observation(
                            eyes, target.getBoundingBox(), new Rotation(0, 3)));
                    var stationary = AimCrosshairMotionTodoAi.sample(eyes, target.getBoundingBox(), new Rotation(0, 3));
                    gate.update(stationary, stationary, inputTime + 8_000_000L);
                    AimAssistControllerTodoAi.attackAttempt();
                    apply.invoke(null, mc, state, cfg, 1f); //codex (old code snippet) apply.invoke(null, mc, state, cfg, true, 1f);
                    if (player.getXRot() != 3)
                        throw new AssertionError("Targeting pulled back between mouse updates");
                    gate.update(stationary, stationary, inputTime + 150_000_000L);
                    AimAssistControllerTodoAi.attackAttempt();
                    apply.invoke(null, mc, state, cfg, 1f); //codex (old code snippet) apply.invoke(null, mc, state, cfg, true, 1f);
                    if (!(player.getXRot() < 3))
                        throw new AssertionError("Stationary targeting must still approach neutral pitch");
                    // codex start
                    cfg.enabled = false;
                    history.clear();
                    gates.clear();
                    player.setXRot(2);
                    AimAssistControllerTodoAi.render(mc, 1f);
                    player.setXRot(3);
                    AimAssistControllerTodoAi.render(mc, 1f);
                    if (gates.get(target.getUUID()).allowed())
                        throw new AssertionError("Render failed to track exit while targeting was disabled");
                    cfg.enabled = true;
                    player.setXRot(15);
                    AimAssistControllerTodoAi.attackAttempt();
                    AimAssistControllerTodoAi.tick(mc);
                    for (int frame = 0; frame < 6; frame++) {
                        AimAssistControllerTodoAi.render(mc, 1f);
                        if (player.getXRot() != 15)
                            throw new AssertionError("Renderer assisted a missed hitbox after exit");
                    }
                    //codex end
                } catch (ReflectiveOperationException error) {
                    throw new AssertionError("Inside-to-outside targeting integration", error);
                }
                //codex end
            });
            // codex start
            context.getInput().releaseMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
            //codex end
            System.out.println("PASS: targeting exit, polling gaps and full renderer miss rejection"); //codex (old code snippet) System.out.println("PASS: targeting/aura exit, polling gaps, full renderer and reversal controls");
        }
    }
}
