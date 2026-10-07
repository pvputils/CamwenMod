package com.example.killaura;

import com.example.UntitledClient;
import com.example.gametestmixins.MinecraftAttackInvokerTodoAi;
import com.example.aimassist.AimAssistConfigTodoAi;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.*;
import java.util.function.Consumer;

/** Checks actual vanilla input, server damage, native persistence, reach, LOS and restoration. */
public final class KillAuraGameTestTodoAi implements FabricClientGameTest {
    public static final java.util.concurrent.atomic.AtomicInteger attacks = new java.util.concurrent.atomic.AtomicInteger();
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    @Override public void runTest(ClientGameTestContext context) {
        context.waitFor(mc -> mc.gui.screen() instanceof TitleScreen, 1200);
        KillAuraConfigTodoAi original = UntitledClient.config.killAura;
        AimAssistConfigTodoAi originalAssist = UntitledClient.config.aimAssist;
        try {
            context.runOnClient(mc -> {
                UntitledClient.config.killAura = new KillAuraConfigTodoAi();
                UntitledClient.config.aimAssist = new AimAssistConfigTodoAi();
                var screen = new KillAuraScreenTodoAi(mc.gui.screen());
                mc.gui.setScreen(screen);
                check(screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).anyMatch(b -> b.getMessage().getString().startsWith("Enabled:")), "native enabled control");
                var cfg = KillAuraControllerTodoAi.config(); cfg.enabled = true; cfg.rotations.smoothing = "AI";
                UntitledClient.config.saveConfig();
                var restored = (com.example.Configs.Config) com.example.Utils.getDeserializedJsonBlocking("config", com.example.Configs.Config.class);
                check(restored.killAura.enabled && restored.killAura.rotations.smoothing.equals("AI"), "KillAura persists in CamwenMod config");
                cfg.enabled = false; cfg.rotations.smoothing = "Linear";
            });
            context.takeScreenshot("KillAuraNativeSettingsTodoAi");
            context.runOnClient(mc -> mc.gui.screen().onClose());
            try (var world = context.worldBuilder().create()) {
                world.getServer().runCommand("gamemode survival @a");
                world.getServer().runCommand("time set midnight");
                world.getServer().runCommand("give @a minecraft:iron_sword");
                world.getServer().runCommand("execute at @p run summon minecraft:zombie ~ ~ ~2 {NoAI:1b,PersistenceRequired:1b}");
                context.waitFor(mc -> mc.player != null && mc.player.onGround() && mc.level.entitiesForRendering().iterator().hasNext(), 200);
                context.waitFor(mc -> zombie(mc) != null, 200);
                context.runOnClient(mc -> {
                    var cfg = KillAuraControllerTodoAi.config(); cfg.enabled = true;
                    cfg.rotations.timing = "Snap";
                    attacks.set(0);
                });
                world.getServer().runCommand("execute at @p run tp @e[type=minecraft:zombie,limit=1] ~ ~ ~6");
                context.waitTicks(10);
                context.getInput().pressKey(options -> options.keyAttack);
                context.waitTicks(5);
                check(attacks.get() == 0, "no attack outside vanilla range");
                world.getServer().runCommand("execute at @p run tp @e[type=minecraft:zombie,limit=1] ~ ~ ~2");
                world.getServer().runCommand("execute at @p run fill ~-2 ~ ~1 ~2 ~3 ~1 minecraft:stone");
                context.waitTicks(10);
                context.getInput().pressKey(options -> options.keyAttack);
                context.waitTicks(5);
                check(attacks.get() == 0, "no attack through solid wall");
                world.getServer().runCommand("execute at @p run fill ~-2 ~ ~1 ~2 ~3 ~1 minecraft:air");
                context.waitFor(mc -> KillAuraControllerTodoAi.target() != null, 200);
                context.waitTicks(40);
                check(attacks.get() == 0, "no automatic attacks");
                context.getInput().holdKey(options -> options.keyAttack);
                context.waitTicks(40);
                check(attacks.get() == 1, "held button attacks once");
                context.getInput().releaseKey(options -> options.keyAttack);
                context.waitTicks(20);
                check(attacks.get() == 1, "release does not repeat attack");
                world.getServer().runCommand("execute at @p run tp @e[type=minecraft:zombie,limit=1] ~ ~ ~2");
                context.waitTicks(30);
                context.getInput().pressKey(options -> options.keyAttack);
                context.waitTicks(5);
                check(attacks.get() == 2, "second press attacks once");
                world.getServer().runCommand("execute at @p run tp @e[type=minecraft:zombie,limit=1] ~ ~ ~2");
                context.waitTicks(20);
                context.getInput().pressKey(options -> options.keyAttack);
                context.getInput().pressKey(options -> options.keyAttack);
                context.waitTicks(5);
                check(attacks.get() == 4, "fast presses retained");
                context.runOnClient(mc -> { KillAuraControllerTodoAi.config().enabled = false; KillAuraControllerTodoAi.reset(); });
                world.getServer().runCommand("kill @e[type=minecraft:zombie]");
                float ground = damage(context, world.getServer()::runCommand, false, false);
                float auraGround = damage(context, world.getServer()::runCommand, true, false);
                float falling = damage(context, world.getServer()::runCommand, false, true);
                float auraFalling = damage(context, world.getServer()::runCommand, true, true);
                check(Math.abs(ground - auraGround) < 0.01f, "grounded damage matches vanilla");
                check(Math.abs(falling - auraFalling) < 0.01f && falling > ground, "falling crit matches vanilla");
                context.waitFor(mc -> mc.player.onGround(), 200);
                world.getServer().runCommand("execute at @p run tp @e[type=minecraft:cow,limit=1] ~ ~ ~2");
                context.waitTicks(10);
                context.runOnClient(this::verifyModesAndRestoration);
                System.out.println("PASS: KillAura native UI, config persistence, real presses, reach, walls and vanilla damage (ground=" + ground + ", crit=" + falling + ")");
            }
        } finally {
            context.runOnClient(mc -> {
                UntitledClient.config.killAura = original;
                UntitledClient.config.aimAssist = originalAssist;
                KillAuraControllerTodoAi.reset();
                UntitledClient.config.saveConfig();
            });
        }
    }
    private static Zombie zombie(Minecraft mc) {
        if (mc.level == null) return null;
        for (var entity : mc.level.entitiesForRendering()) if (entity instanceof Zombie zombie && zombie.isAlive()) return zombie;
        return null;
    }
    private static LivingEntity cow(Minecraft mc) {
        for (var entity : mc.level.entitiesForRendering()) if (entity instanceof LivingEntity living && entity.getType().toString().contains("cow") && living.isAlive()) return living;
        return null;
    }
    private void verifyModesAndRestoration(Minecraft mc) {
        var cfg = KillAuraControllerTodoAi.config();
        cfg.enabled = true; cfg.targets.passive = true; cfg.rotations.timing = "Normal";
        float yaw = mc.player.getYRot(), pitch = mc.player.getXRot();
        HitResult original = mc.hitResult;
        try {
            for (String mode : java.util.List.of("Linear", "Sigmoid", "Interpolation", "Acceleration", "AI")) {
                KillAuraControllerTodoAi.reset(); cfg.rotations.smoothing = mode;
                mc.player.setYRot(45); mc.player.setXRot(0);
                KillAuraControllerTodoAi.tick(mc);
                check(KillAuraControllerTodoAi.target() != null && KillAuraControllerTodoAi.rotation() != null, mode + " produces a live rotation");
                check(Double.isFinite(KillAuraControllerTodoAi.rotation().yaw()) && Double.isFinite(KillAuraControllerTodoAi.rotation().pitch()), mode + " stays finite");
                check(mc.player.getYRot() == 45 && mc.player.getXRot() == 0, "silent aiming preserves camera");
            }
            cfg.rotations.smoothing = "Linear"; cfg.rotations.timing = "Snap";
            KillAuraControllerTodoAi.tick(mc);
            mc.hitResult = new EntityHitResult(KillAuraControllerTodoAi.target());
            HitResult previous = mc.hitResult;
            check(!KillAuraControllerTodoAi.attack(mc, () -> false), "cancelled vanilla attack preserved");
            check(mc.hitResult == previous && mc.player.getYRot() == 45 && mc.player.getXRot() == 0, "cancelled attack restores state");
            try {
                KillAuraControllerTodoAi.attack(mc, () -> { throw new IllegalStateException("test cancellation"); });
                throw new AssertionError("expected attack failure");
            } catch (IllegalStateException expected) { check(expected.getMessage().equals("test cancellation"), "exception preserved"); }
            check(mc.hitResult == previous && mc.player.getYRot() == 45 && mc.player.getXRot() == 0, "failed attack restores state");
            cfg.enabled = false; KillAuraControllerTodoAi.tick(mc);
            check(KillAuraControllerTodoAi.rotation() == null && KillAuraControllerTodoAi.target() == null, "disabled clears aiming state");
        } finally {
            mc.hitResult = original; mc.player.setYRot(yaw); mc.player.setXRot(pitch);
            cfg.enabled = false; KillAuraControllerTodoAi.reset();
        }
    }
    private float damage(ClientGameTestContext context, Consumer<String> command, boolean aura, boolean falling) {
        context.runOnClient(mc -> { KillAuraControllerTodoAi.config().enabled = false; KillAuraControllerTodoAi.reset(); });
        context.waitFor(mc -> mc.player.onGround(), 200);
        command.accept("kill @e[type=minecraft:cow]"); context.waitTicks(10);
        command.accept("execute at @p run summon minecraft:cow ~ ~ ~2 {NoAI:1b,PersistenceRequired:1b}");
        context.waitFor(mc -> cow(mc) != null, 200);
        LivingEntity cow = context.computeOnClient(KillAuraGameTestTodoAi::cow);
        float health = context.computeOnClient(mc -> cow.getHealth());
        context.runOnClient(mc -> {
            var cfg = KillAuraControllerTodoAi.config(); cfg.targets.passive = true; cfg.enabled = aura;
        });
        context.waitTicks(40);
        if (falling) {
            context.runOnClient(mc -> mc.player.jumpFromGround());
            context.waitFor(mc -> !mc.player.onGround() && mc.player.getDeltaMovement().y < -0.1, 100);
        }
        context.runOnClient(mc -> {
            check(!mc.player.isSprinting() && mc.player.getAttackStrengthScale(0) > 0.9f, "matched vanilla player state");
            HitResult previous = mc.hitResult;
            float yaw = mc.player.getYRot(), pitch = mc.player.getXRot();
            if (aura) {
                check(KillAuraControllerTodoAi.target() == cow, "aura acquired cow");
                int before = attacks.get();
                ((MinecraftAttackInvokerTodoAi) mc).killAuraStartAttackTodoAi();
                check(attacks.get() == before + 1, "vanilla attack executed once");
                check(mc.hitResult == previous && mc.player.getYRot() == yaw && mc.player.getXRot() == pitch, "crosshair and camera restored");
            } else mc.gameMode.attack(mc.player, cow);
        });
        context.waitFor(mc -> cow.getHealth() < health, 100);
        return context.computeOnClient(mc -> health - cow.getHealth());
    }
}
