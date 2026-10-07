package com.example;

import com.example.aimassist.*;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import java.util.UUID;
import java.util.Set;
import java.util.Map;
import com.example.aimassist.AimGeometryTodoAi.Rotation;

/** Actual extended-reach picking, planning, rendering and attribute restoration. */
public final class ReachGameTestTodoAi implements FabricClientGameTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            context.waitFor(mc -> mc.player != null && mc.level != null && mc.gui.screen() == null);
            context.waitTicks(3);
            context.runOnClient(mc -> {
                var player = mc.player;
                var block = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
                var entity = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
                var blockModifiers = Set.copyOf(block.getModifiers());
                var entityModifiers = Set.copyOf(entity.getModifiers());
                double blockRange = player.blockInteractionRange(), entityRange = player.entityInteractionRange();
                AimAssistReachTodoAi.withRange(player, Math.max(blockRange, entityRange) + 2, () -> {
                    check(Math.abs(player.blockInteractionRange() - Math.max(blockRange, entityRange) - 2) < 1e-9, "temporary block reach addition");
                    check(Math.abs(player.entityInteractionRange() - Math.max(blockRange, entityRange) - 2) < 1e-9, "temporary player reach addition");
                    try {
                        AimAssistReachTodoAi.withRange(player, Math.max(blockRange, entityRange) + 1, () -> { throw new IllegalStateException("scope regression"); });
                        throw new AssertionError("expected nested failure");
                    } catch (IllegalStateException expected) {}
                    check(Math.abs(player.entityInteractionRange() - Math.max(blockRange, entityRange) - 2) < 1e-9, "nested failed probe restores outer scope");
                    return null;
                });
                check(block.getModifiers().equals(blockModifiers) && entity.getModifiers().equals(entityModifiers), "exact modifiers restored");
                try {
                    AimAssistReachTodoAi.withRange(player, Math.max(blockRange, entityRange) + 3, () -> { throw new IllegalStateException("failed probe"); });
                    throw new AssertionError("expected failed probe");
                } catch (IllegalStateException expected) {}
                check(block.getModifiers().equals(blockModifiers) && entity.getModifiers().equals(entityModifiers), "failed probe restores exact modifiers");
                UntitledClient.config.isCheatsEnabled = true;
                var cheats = Utils.computeCheatConfig();
                cheats.isTargetingMarginReverted = false;
                cheats.targetingMarginBypass = 0; //codex (old code snippet) cheats.staticTargetingMarginBypass = 0;
                var eyes = player.getEyePosition();
                var target = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "ReachRegression"));
                target.setId(1_000_001);
                target.setPos(eyes.x, eyes.y, eyes.z + 4.2);
                target.setBoundingBox(new AABB(eyes.x - 0.4, eyes.y - 0.5, eyes.z + 4.2,
                        eyes.x + 0.4, eyes.y + 0.5, eyes.z + 4.8));
                mc.level.addEntity(target);
                player.setYRot(0);
                player.setXRot(3);
                check(!(TargetingMarginPickTodoAi.pick(mc, 0f) instanceof EntityHitResult), "vanilla reach misses distant target");
                check(TargetingMarginPickTodoAi.pick(mc, 0f, 5) instanceof EntityHitResult hit && hit.getEntity() == target, "reach alone hits unexpanded target");
                var cfg = AimAssistControllerTodoAi.config();
                cfg.aura.enabled = false;
                cfg.targetting.enabled = true;
                cfg.targetting.range = 5;
                cfg.targetting.requires.attackWindow = 200;
                cfg.targetting.requires.notBreaking = false;
                AimAssistControllerTodoAi.tick(mc);
                AimAssistControllerTodoAi.attackAttempt();
                AimAssistControllerTodoAi.tick(mc);
                AimAssistControllerTodoAi.render(mc, 1f);
                check(player.getXRot() < 3 && player.getYRot() == 0, "extended reach actually renders neutral-pitch assistance");
                cfg.targetting.enabled = false;
                cfg.aura.enabled = true;
                cfg.aura.range = 5;
                cfg.aura.targetingMargin = 1;
                cfg.aura.requires.attackWindow = 200;
                cfg.aura.requires.notBreaking = false;
                target.setBoundingBox(new AABB(eyes.x + 0.6, eyes.y - 0.5, eyes.z + 4.2,
                        eyes.x + 1.4, eyes.y + 0.5, eyes.z + 4.8));
                player.setXRot(0);
                check(!(TargetingMarginPickTodoAi.pick(mc, 0f, 5) instanceof EntityHitResult), "reach alone misses off-axis target");
                check(AimAssistControllerTodoAi.auraEligibleTarget(mc, cfg.aura) == target, "reach plus margin selects aura target");
                cfg.aura.range = 3;
                check(AimAssistControllerTodoAi.auraEligibleTarget(mc, cfg.aura) == null, "margin alone cannot bypass insufficient reach");
                cfg.aura.range = 5;
                AimAssistControllerTodoAi.attackAttempt();
                AimAssistControllerTodoAi.tick(mc);
                // Initialize stationary history after repositioning the target.
                AimAssistControllerTodoAi.render(mc, 1f);
                try {
                    var field = AimAssistControllerTodoAi.class.getDeclaredField("CROSSHAIR_GATES");
                    field.setAccessible(true);
                    var gates = (Map<UUID, AimCrosshairMotionTodoAi.Gate>) field.get(null);
                    var proximity = AimCrosshairMotionTodoAi.sample(player.getEyePosition(1f), target.getBoundingBox(),
                            new Rotation(player.getYRot(), player.getXRot()));
                    gates.get(target.getUUID()).update(proximity, proximity, System.nanoTime() + 150_000_000L);
                } catch (ReflectiveOperationException error) {
                    throw new AssertionError("stationary aura fixture", error);
                }
                AimAssistControllerTodoAi.attackAttempt();
                AimAssistControllerTodoAi.tick(mc);
                AimAssistControllerTodoAi.render(mc, 1f);
                check(player.getYRot() != 0, "reach plus margin actually renders aura assistance");
                check(block.getModifiers().equals(blockModifiers) && entity.getModifiers().equals(entityModifiers), "no reach modifiers leak from planning/rendering");
                check(player.blockInteractionRange() == blockRange && player.entityInteractionRange() == entityRange, "normal interaction ranges unchanged afterward");
                check(cheats.targetingMarginBypass == 0, "temporary margin restored"); //codex (old code snippet) check(cheats.staticTargetingMarginBypass == 0, "temporary margin restored");
            });
            System.out.println("PASS: extended reach picking, neutral assistance, aura routing and scoped attribute cleanup");
        }
    }
}
