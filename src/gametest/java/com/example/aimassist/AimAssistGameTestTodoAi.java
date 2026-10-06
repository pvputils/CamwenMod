package com.example.aimassist;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import java.lang.reflect.Method;
import java.util.List;

/** Boots the actual client, joins single player, follows the attack path and verifies live aiming. */
public final class AimAssistGameTestTodoAi implements FabricClientGameTest {
    private static void check(boolean pass, String message) { if (!pass) throw new AssertionError(message); }
    @Override public void runTest(ClientGameTestContext context) {
        context.waitFor(mc -> mc.gui.screen() instanceof TitleScreen, 1200);
        context.runOnClient(mc -> {
            mc.setScreenAndShow(new AimAssistScreenTodoAi(mc.gui.screen()));
            List<String> labels = mc.gui.screen().children().stream().filter(Button.class::isInstance)
                .map(Button.class::cast).map(b -> b.getMessage().getString()).toList();
            check(labels.equals(List.of("aura", "targetting", "Done")), "two separate module buttons");
        });
        context.takeScreenshot("AimAssistMenuTodoAi");
        context.runOnClient(mc -> {
            var button = mc.gui.screen().children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(b -> b.getMessage().getString().equals("targetting")).findFirst().orElseThrow();
            button.onPress(new InputWithModifiers() { public int input() { return 257; } public int modifiers() { return 0; } });
            check(mc.gui.screen().children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .anyMatch(b -> b.getMessage().getString().equals("Interpolation...")), "targetting interpolation menu");
        });
        context.takeScreenshot("TargettingConfigTodoAi");
        context.runOnClient(mc -> mc.setScreenAndShow(null));
        try (var world = context.worldBuilder().create()) {
            context.waitFor(mc -> mc.player != null && mc.player.onGround(), 200);
            context.runOnClient(mc -> {
                AimAssistControllerTodoAi.config().aura.enabled = true;
                mc.hitResult = BlockHitResult.miss(mc.player.getEyePosition(), Direction.UP, BlockPos.containing(mc.player.getEyePosition()));
                attack(mc);
                var pos = mc.player.blockPosition().below();
                mc.hitResult = new BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), Direction.UP, pos, false);
                attack(mc);
                AimAssistControllerTodoAi.config().aura.enabled = false;
            });
            world.getServer().runCommand("execute at @p run summon minecraft:armor_stand ~ ~ ~1");
            context.waitFor(mc -> { for (var entity : mc.level.entitiesForRendering()) if (entity instanceof ArmorStand) return true; return false; }, 100);
            context.runOnClient(mc -> {
                ArmorStand stand = null;
                for (var entity : mc.level.entitiesForRendering()) if (entity instanceof ArmorStand s) { stand = s; break; }
                var cfg = AimAssistControllerTodoAi.config();
                cfg.targets.armorStand = true;
                cfg.aura.enabled = true;
                mc.player.setYRot(40); mc.player.setXRot(0);
                mc.hitResult = BlockHitResult.miss(mc.player.getEyePosition(), Direction.UP, BlockPos.containing(mc.player.getEyePosition()));
                attack(mc);
                AimAssistControllerTodoAi.tick(mc);
                AimAssistControllerTodoAi.render(mc, 1);
                check(mc.player.getYRot() < 40, "aura assists after an unsuccessful attack attempt");
                cfg.aura.enabled = false;
                cfg.targetting.enabled = true;
                mc.player.setYRot(8); mc.player.setXRot(10);
                mc.hitResult = new EntityHitResult(stand);
                AimAssistControllerTodoAi.attackAttempt();
                AimAssistControllerTodoAi.tick(mc);
                AimAssistControllerTodoAi.render(mc, 1);
                check(Math.abs(mc.player.getYRot()) < 8 && mc.player.getXRot() < 10, "simultaneous live yaw/pitch correction");
                mc.hitResult = new EntityHitResult(stand);
                attack(mc);
                cfg.targetting.enabled = false;
                cfg.targets.armorStand = false;
                com.example.UntitledClient.config.saveConfig();
            });
            context.waitTicks(10);
            context.takeScreenshot("SinglePlayerAimAssistTodoAi");
        }
    }
    private static void attack(Minecraft mc) {
        try {
            Method attack = Minecraft.class.getDeclaredMethod("startAttack");
            attack.setAccessible(true); attack.invoke(mc);
        } catch (ReflectiveOperationException e) { throw new AssertionError("attack path crashed", e); }
    }
}
