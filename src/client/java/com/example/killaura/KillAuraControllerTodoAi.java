/* Aiming algorithms adapted from LiquidBounce, copyright CCBlueX 2015-2026.
 * GPL-3.0-or-later. See assets/untitled/KillAuraLicenseTodoAi.txt.
 * CamwenMod adaptation is aim-only: no attack, swing, input simulation, or packet sending. */
package com.example.killaura;

import com.example.UntitledClient;
import com.example.aimassist.AimGeometryTodoAi;
import com.example.aimassist.AimGeometryTodoAi.Rotation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.concurrent.ThreadLocalRandom;
import static com.example.killaura.KillAuraGeometryTodoAi.*;

/** Visible look correction only while a miss becomes a player hit in a scoped margin probe. */
public final class KillAuraControllerTodoAi {
    private static ClientLevel level;
    private static Player target, pointOwner;
    private static Rotation start, end, lastApplied, previousGoal, previous, managed;
    private static KillAuraConfigTodoAi.MovementCorrection plannedMode;
    private static Vec3 delayedPoint, lazyPoint, gaussianOffset = Vec3.ZERO, gaussianGoal = Vec3.ZERO;
    private static int delayTicks;
    private static double lazyThreshold;
    private static boolean paused;
    private KillAuraControllerTodoAi() {}
    public static KillAuraConfigTodoAi config() {
        if (UntitledClient.config.killAura == null) UntitledClient.config.killAura = new KillAuraConfigTodoAi();
        UntitledClient.config.killAura.repair();
        return UntitledClient.config.killAura;
    }
    public static Player target() { return target; }
    public static boolean paused() { return paused; }
    public static boolean ownsLook(Minecraft mc) {
        return enabledContext(mc) && target != null && marginTarget(mc) == target;
    }
    /** Used only by movement/network hooks; ordinary picking keeps the camera rotation. */
    public static Rotation managedRotation(Minecraft mc) {
        return ownsLook(mc) && plannedMode == config().rotations.movementCorrection &&
                plannedMode != KillAuraConfigTodoAi.MovementCorrection.CHANGE_LOOK ? managed : null;
    }
    public static Rotation movementRotation(Minecraft mc) {
        return config().rotations.movementCorrection == KillAuraConfigTodoAi.MovementCorrection.OFF ? null : managedRotation(mc);
    }
    private static boolean enabledContext(Minecraft mc) {
        return config().enabled && mc.player != null && mc.level != null && mc.player.isAlive() &&
                !mc.player.isSpectator() && mc.gui.screen() == null && mc.getCameraEntity() == mc.player;
    }
    private static Rotation current(Minecraft mc) { return new Rotation(mc.player.getYRot(), mc.player.getXRot()); }
    private static void clearPlan() { start = end = lastApplied = previousGoal = previous = managed = null; }
    private static void resetPoints() {
        pointOwner = null; delayedPoint = lazyPoint = null; delayTicks = 0;
        gaussianOffset = gaussianGoal = Vec3.ZERO;
    }
    private static boolean allowed(Minecraft mc, Player candidate) {
        if (candidate == mc.player || candidate.isRemoved() || candidate.isSpectator() || !candidate.isAlive() ||
                !mc.level.getWorldBorder().isWithinBounds(candidate.blockPosition())) return false;
        if (UntitledClient.config.isAimAssistDisabledOnTeammates) {
            var team = UntitledClient.config.nameplateUuids.get(candidate.getUUID());
            if (team != null && team.isFriendly) return false;
        }
        return true;
    }
    private static Player marginTarget(Minecraft mc) {
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.MISS) return null;
        if (KillAuraMarginPickTodoAi.pick(mc, 0).getType() != HitResult.Type.MISS) return null;
        if (KillAuraMarginPickTodoAi.pick(mc, config().margin) instanceof EntityHitResult hit &&
                hit.getEntity() instanceof Player player && allowed(mc, player)) return player;
        return null;
    }
    private static boolean visible(Minecraft mc, Vec3 eyes, Vec3 point) {
        HitResult block = mc.level.clip(new ClipContext(eyes, point, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, mc.player));
        return block.getType() == HitResult.Type.MISS || block.getLocation().distanceToSqr(eyes) + 1e-7 >= point.distanceToSqr(eyes);
    }
    private static boolean inRange(Minecraft mc, Vec3 eyes, Vec3 point, double range) {
        return eyes.distanceToSqr(point) <= range * range && visible(mc, eyes, point);
    }
    private static Vec3 processPoint(Vec3 point, AABB box) {
        var cfg = config().aimPoint;
        if (cfg.delay) {
            if (delayedPoint == null || delayTicks-- <= 0) {
                delayedPoint = point;
                delayTicks = (int) Math.round(random(cfg.delayMin, cfg.delayMax, 2, 5));
            }
            point = clamp(delayedPoint, box);
        }
        if (cfg.lazy) {
            if (lazyPoint == null || point.distanceToSqr(lazyPoint) >= lazyThreshold * lazyThreshold) {
                lazyPoint = point;
                lazyThreshold = random(cfg.lazyMin, cfg.lazyMax, 0.1, 0.4);
            }
            point = clamp(lazyPoint, box);
        }
        if (cfg.gaussian) {
            var random = ThreadLocalRandom.current();
            double tolerance = finite(cfg.gaussianTolerance, 0.05, 0.01, 0.1);
            if (gaussianOffset.distanceToSqr(gaussianGoal) <= tolerance * tolerance) {
                if (random.nextDouble(100) < finite(cfg.gaussianChance, 100, 0, 100)) {
                    double yaw = finite(cfg.gaussianYaw, 0, 0, 1), pitch = finite(cfg.gaussianPitch, 0, 0, 1);
                    gaussianGoal = new Vec3(random.nextGaussian(0.00942273861037109, 0.23319837528201348) * yaw,
                            random.nextGaussian(-0.30075078007595923, 0.3492437109081718) * pitch,
                            random.nextGaussian(0.013282929419023442, 0.24453708645460387) * yaw);
                }
            } else gaussianOffset = gaussianOffset.lerp(gaussianGoal,
                    random(cfg.gaussianSpeedMin, cfg.gaussianSpeedMax, 0.1, 1));
            point = clamp(point.add(gaussianOffset), box);
        }
        return point;
    }
    public static void tick(Minecraft mc) {
        if (level != mc.level) { level = mc.level; target = null; resetPoints(); clearPlan(); }
        if (!enabledContext(mc)) { target = null; paused = false; clearPlan(); resetPoints(); return; }
        var cfg = config();
        if (plannedMode != cfg.rotations.movementCorrection) {
            clearPlan(); plannedMode = cfg.rotations.movementCorrection;
        }
        double maximum = mc.player.entityInteractionRange();
        Vec3 eyes = mc.player.getEyePosition();
        Player selected = marginTarget(mc);
        if (target != selected) { clearPlan(); resetPoints(); }
        target = selected;
        paused = selected == null;
        if (selected == null) { clearPlan(); return; }
        Vec3 selectedPoint = choosePoint(eyes, selected.getBoundingBox(), cfg.aimPoint,
                p -> inRange(mc, eyes, p, maximum));
        if (selectedPoint == null) { target = null; paused = true; clearPlan(); return; }
        if (pointOwner != selected) { resetPoints(); pointOwner = selected; }
        Vec3 point = processPoint(selectedPoint, selected.getBoundingBox());
        if (!inRange(mc, eyes, point, maximum)) point = selectedPoint;
        Rotation actual = managed != null ? managed : current(mc), goal = AimGeometryTodoAi.lookAt(eyes, point);
        if (cfg.rotations.lazyRotation && AimGeometryTodoAi.hits(eyes, selected.getBoundingBox(), actual, maximum)) goal = actual;
        start = actual;
        end = smooth(actual, goal, previousGoal, previous, cfg.rotations);
        previousGoal = goal; previous = actual; lastApplied = current(mc);
        if (plannedMode != KillAuraConfigTodoAi.MovementCorrection.CHANGE_LOOK) managed = normalize(mc, actual, end);
    }
    public static void render(Minecraft mc, float partial) {
        if (!ownsLook(mc)) { paused = true; target = null; clearPlan(); return; }
        paused = false;
        if (plannedMode != config().rotations.movementCorrection) { clearPlan(); return; }
        if (plannedMode != KillAuraConfigTodoAi.MovementCorrection.CHANGE_LOOK) return;
        if (start == null || end == null) return;
        Rotation actual = current(mc);
        if (lastApplied != null) {
            double yaw = AimGeometryTodoAi.wrap(actual.yaw() - lastApplied.yaw()), pitch = actual.pitch() - lastApplied.pitch();
            start = new Rotation(start.yaw() + yaw, start.pitch() + pitch);
            end = new Rotation(end.yaw() + yaw, end.pitch() + pitch);
        }
        Rotation proposed = start.toward(end, Math.clamp(partial, 0, 1));
        Rotation applied = normalize(mc, actual, proposed);
        mc.player.setYRot((float)applied.yaw());
        mc.player.setXRot((float)applied.pitch());
        lastApplied = current(mc);
    }
    private static Rotation normalize(Minecraft mc, Rotation actual, Rotation proposed) {
        double f = mc.options.sensitivity().get() * 0.6 + 0.2, step = f * f * f * 8 * 0.15;
        return new Rotation(actual.yaw() + Math.round(AimGeometryTodoAi.wrap(proposed.yaw() - actual.yaw()) / step) * step,
                Math.clamp(actual.pitch() + Math.round((proposed.pitch() - actual.pitch()) / step) * step, -90, 90));
    }

}
