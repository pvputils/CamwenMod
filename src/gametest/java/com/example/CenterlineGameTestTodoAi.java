package com.example;

import com.example.aimassist.*;
import com.example.aimassist.AimGeometryTodoAi.Rotation;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.phys.AABB;
import java.util.Map;
import java.util.UUID;

/** Actual upward-only targeting with different configured horizontal band widths. */
public final class CenterlineGameTestTodoAi implements FabricClientGameTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void settled(Minecraft mc, RemotePlayer target, float yaw, float pitch, double width) {
        var cfg = AimAssistControllerTodoAi.config().targetting;
        cfg.enabled = false;
        cfg.centerlineWidth = width;
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
        AimAssistControllerTodoAi.render(mc, 1f);
        try {
            var field = AimAssistControllerTodoAi.class.getDeclaredField("CROSSHAIR_GATES");
            field.setAccessible(true);
            var gates = (Map<UUID, AimCrosshairMotionTodoAi.Gate>) field.get(null);
            var proximity = AimCrosshairMotionTodoAi.sample(mc.player.getEyePosition(1f), target.getBoundingBox(), new Rotation(yaw, pitch));
            gates.get(target.getUUID()).update(proximity, proximity, System.nanoTime() + 150_000_000L);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        cfg.enabled = true;
        AimAssistControllerTodoAi.attackAttempt();
        AimAssistControllerTodoAi.tick(mc);
        AimAssistControllerTodoAi.render(mc, 1f);
    }
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            context.waitFor(mc -> mc.player != null && mc.level != null && mc.screen == null);
            context.waitTicks(3);
            context.runOnClient(mc -> {
                var cfg = AimAssistControllerTodoAi.config();
                cfg.aura.enabled = false;
                cfg.targetting.enabled = false;
                cfg.targetting.range = 4.2;
                cfg.targetting.requires.attackWindow = 200;
                cfg.targetting.requires.notBreaking = false;
                UntitledClient.config.isCheatsEnabled = true;
                Utils.computeCheatConfig().isTargetingMarginReverted = false;
                Utils.computeCheatConfig().staticTargetingMarginBypass = 0;
                var eyes = mc.player.getEyePosition();
                var target = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "CenterBandRegression"));
                target.setId(1_000_002);
                target.setPos(eyes.x, eyes.y, eyes.z + 2);
                target.setBoundingBox(new AABB(eyes.x - 0.5, eyes.y - 0.5, eyes.z + 2,
                        eyes.x + 0.5, eyes.y + 0.5, eyes.z + 3));
                mc.level.addEntity(target);
                AimAssistControllerTodoAi.tick(mc);
                settled(mc, target, 12, 3, 40);
                check(mc.player.getYRot() < 12 && mc.player.getYRot() > 0, "right edge interpolates toward wider band");
                check(mc.player.getXRot() < 3 && mc.player.getXRot() >= 0, "head moves upward toward neutral");
                check(AimGeometryTodoAi.hits(mc.player.getEyePosition(), target.getBoundingBox(),
                        new Rotation(mc.player.getYRot(), mc.player.getXRot()), 4.2), "combined correction preserves actual hit");
                settled(mc, target, -12, -3, 40);
                check(mc.player.getYRot() > -12 && mc.player.getYRot() < 0, "left edge pulls to opposite side of band");
                check(mc.player.getXRot() == -3, "looking upward never receives downward correction");
                settled(mc, target, 2, 3, 40);
                check(mc.player.getYRot() == 2 && mc.player.getXRot() < 3, "inside band keeps yaw while lifting head");
                settled(mc, target, 12, -3, 100);
                check(mc.player.getYRot() == 12 && mc.player.getXRot() == -3, "full width and upward pitch remain untouched");
                settled(mc, target, 12, 3, 0);
                check(mc.player.getYRot() < 12, "zero width restores centerline pull");
                // codex start
                target.setBoundingBox(new AABB(eyes.x + 0.6, eyes.y - 0.5, eyes.z + 2,
                        eyes.x + 1.4, eyes.y + 0.5, eyes.z + 3));
                cfg.aura.range = 4.2;
                cfg.aura.targetingMargin = 1;
                cfg.aura.requires.attackWindow = 200;
                cfg.aura.requires.notBreaking = false;
                cfg.aura.horizontal = cfg.aura.vertical = false;
                cfg.aura.enabled = true;
                settled(mc, target, 0, 3, 40);
                check(mc.player.getYRot() < 0 && mc.player.getXRot() < 3,
                        "active aura miss activates targeting centerline and upward assistance");
                cfg.aura.enabled = false;
                settled(mc, target, 0, 3, 40);
                check(mc.player.getYRot() == 0 && mc.player.getXRot() == 3,
                        "miss without active aura remains unassisted");
                cfg.aura.enabled = true;
                cfg.targetting.range = 1;
                settled(mc, target, 0, 3, 40);
                check(mc.player.getYRot() == 0 && mc.player.getXRot() == 3,
                        "aura fallback respects targeting range");
                cfg.targetting.range = 4.2;
                settled(mc, target, 0, -3, 40);
                check(mc.player.getYRot() < 0 && mc.player.getXRot() <= -3,
                        "aura fallback can center while looking up without downward correction");
                //codex end
                // codex start
                cfg.aura.maxCorrectionFov = 1;
                settled(mc, target, 0, 3, 40);
                check(AimAssistControllerTodoAi.auraEligibleTarget(mc, cfg.aura) == target,
                        "FOV safeguard does not replace reach and margin eligibility");
                check(mc.player.getYRot() == 0 && mc.player.getXRot() == 3,
                        "large aura goal is rejected rather than clamped");
                cfg.aura.maxCorrectionFov = 180;
                cfg.targetting.maxCorrectionFov = 1;
                settled(mc, target, 0, 3, 40);
                check(mc.player.getYRot() == 0 && mc.player.getXRot() == 3,
                        "targeting fallback cannot bypass its correction safeguard");
                cfg.targetting.maxCorrectionFov = 180;
                settled(mc, target, 0, 3, 40);
                check(mc.player.getYRot() < 0 && mc.player.getXRot() < 3,
                        "allowed FOV still permits aura-supported targeting");
                cfg.targetting.maxCorrectionFov = 0;
                cfg.aura.maxCorrectionFov = 30;
                cfg.aura.horizontal = true;
                cfg.aura.interpolation.horizontalMin = cfg.aura.interpolation.horizontalMax = 2000;
                cfg.aura.interpolation.directionMin = cfg.aura.interpolation.directionMax = 0;
                settled(mc, target, 0, 3, 40);
                check(mc.player.getYRot() == 0 && mc.player.getXRot() == 3,
                        "render safeguard rejects oversized aura interpolation even with eligible goal");
                //codex end
            });
            System.out.println("PASS: upward-only head pitch, configurable center band, interpolation and hit preservation");
        }
    }
}
