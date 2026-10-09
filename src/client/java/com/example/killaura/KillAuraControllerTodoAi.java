/* Aiming algorithms adapted from LiquidBounce, copyright CCBlueX 2015-2026.
 * GPL-3.0-or-later. See assets/untitled/KillAuraLicenseTodoAi.txt.
 * CamwenMod adaptation is aim-only: no attack, swing, input simulation, or packet sending. */
package com.example.killaura;

import com.example.UntitledClient;
import com.example.aimassist.AimGeometryTodoAi;
import com.example.aimassist.AimGeometryTodoAi.Rotation;
import com.example.aimassist.TargetingMarginPickTodoAi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import static com.example.killaura.KillAuraGeometryTodoAi.*;

/** Visible look assistance only. Range settings never alter Minecraft's interaction attributes. */
public final class KillAuraControllerTodoAi {
    private static ClientLevel level;
    private static Player target, pointOwner;
    private static Rotation start, end, lastApplied, previousGoal, previous;
    private static Vec3 delayedPoint, lazyPoint, gaussianOffset = Vec3.ZERO, gaussianGoal = Vec3.ZERO;
    private static int delayTicks;
    private static double lazyThreshold, scanAddition = 2.5, scanMin = Double.NaN, scanMax = Double.NaN;
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
        return config().enabled && mc.player != null && mc.level != null && mc.player.isAlive() &&
                !mc.player.isSpectator() && mc.gui.screen() == null && mc.getCameraEntity() == mc.player;
    }
    private static Rotation current(Minecraft mc) { return new Rotation(mc.player.getYRot(), mc.player.getXRot()); }
    private static void clearPlan() { start = end = lastApplied = previousGoal = previous = null; }
    private static void resetPoints() {
        pointOwner = null; delayedPoint = lazyPoint = null; delayTicks = 0;
        gaussianOffset = gaussianGoal = Vec3.ZERO;
    }
    private static boolean allowed(Minecraft mc, Player candidate) {
        var cfg = config().target;
        if (candidate == mc.player || candidate.isRemoved() || candidate.isSpectator() ||
                !candidate.isAlive() && !cfg.dead || candidate.isInvisible() && !cfg.invisible ||
                candidate.isSleeping() && !cfg.sleeping || candidate.hurtTime > Math.clamp(cfg.hurtTime, 0, 10) ||
                !mc.level.getWorldBorder().isWithinBounds(candidate.blockPosition())) return false;
        if (UntitledClient.config.isAimAssistDisabledOnTeammates) {
            var team = UntitledClient.config.nameplateUuids.get(candidate.getUUID());
            if (team != null && team.isFriendly) return false;
        }
        if (!cfg.ignoreShield && candidate.isBlocking()) return false;
        return AimGeometryTodoAi.error(current(mc), AimGeometryTodoAi.lookAt(mc.player.getEyePosition(),
                candidate.getBoundingBox().getCenter())) <= finite(cfg.fov, 180, 0, 180);
    }
    private static double interactionRange(Minecraft mc) {
        return mc.player.entityInteractionRange() + finite(config().range.rangeIncrease, 1, 0, 5);
    }
    private static boolean visible(Minecraft mc, Vec3 eyes, Vec3 point) {
        HitResult block = mc.level.clip(new ClipContext(eyes, point, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, mc.player));
        return block.getType() == HitResult.Type.MISS || block.getLocation().distanceToSqr(eyes) + 1e-7 >= point.distanceToSqr(eyes);
    }
    private static boolean inRange(Minecraft mc, Vec3 eyes, Vec3 point, double range) {
        double distance = eyes.distanceToSqr(point);
        return distance <= range * range && (visible(mc, eyes, point) || distance <=
                Math.pow(finite(config().range.throughWallsRange, 3, 0, 8), 2));
    }
    private static boolean normalCrosshairOn(Minecraft mc, Player candidate) {
        return TargetingMarginPickTodoAi.pick(mc, 0f) instanceof EntityHitResult hit && hit.getEntity() == candidate;
    }
    private static Comparator<Player> priority(Minecraft mc) {
        return switch (config().target.priority) {
            case HEALTH -> Comparator.comparingDouble(p -> p.getHealth() + p.getAbsorptionAmount());
            case DISTANCE -> Comparator.comparingDouble(p -> p.getBoundingBox().distanceToSqr(mc.player.position()));
            case DIRECTION -> Comparator.comparingDouble(p -> AimGeometryTodoAi.error(current(mc),
                    AimGeometryTodoAi.lookAt(mc.player.getEyePosition(), p.getBoundingBox().getCenter())));
            case HURT_TIME -> Comparator.comparingInt(p -> p.hurtTime);
            case AGE -> Comparator.comparingInt(p -> -p.tickCount);
        };
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
        if (!ownsLook(mc)) { target = null; paused = false; clearPlan(); resetPoints(); return; }
        var cfg = config();
        if (cfg.range.scanRangeMin != scanMin || cfg.range.scanRangeMax != scanMax) {
            scanMin = cfg.range.scanRangeMin; scanMax = cfg.range.scanRangeMax;
            scanAddition = random(scanMin, scanMax, 2, 7);
        }
        double normal = interactionRange(mc);
        List<Player> candidates = new ArrayList<>();
        for (var entity : mc.level.entitiesForRendering()) if (entity instanceof Player candidate && allowed(mc, candidate))
            candidates.add(candidate);
        double nearest = candidates.stream().mapToDouble(p -> p.getBoundingBox().distanceToSqr(mc.player.position()))
                .min().orElse(Double.POSITIVE_INFINITY);
        double maximum = nearest > normal * normal ? Math.max(normal, finite(cfg.range.throughWallsRange, 3, 0, 8)) + scanAddition : normal;
        candidates.removeIf(p -> p.getBoundingBox().distanceToSqr(mc.player.position()) > maximum * maximum);
        candidates.sort(priority(mc));
        candidates.sort(Comparator.comparingInt(p -> p.getBoundingBox().distanceToSqr(mc.player.position()) <= normal * normal ? 0 : 1));
        Vec3 eyes = mc.player.getEyePosition();
        Player selected = null; Vec3 selectedPoint = null;
        for (Player candidate : candidates) {
            if (normalCrosshairOn(mc, candidate)) { selected = candidate; break; }
            Vec3 point = choosePoint(eyes, candidate.getBoundingBox(), cfg.aimPoint,
                    p -> inRange(mc, eyes, p, maximum) || cfg.rotations.aimThroughWalls && eyes.distanceToSqr(p) <= maximum * maximum);
            if (point != null) { selected = candidate; selectedPoint = point; break; }
        }
        if (target != selected) { clearPlan(); resetPoints(); }
        target = selected; paused = selected != null && normalCrosshairOn(mc, selected);
        if (selected == null || paused) { clearPlan(); return; }
        if (pointOwner != selected) { resetPoints(); pointOwner = selected; }
        Vec3 point = processPoint(selectedPoint, selected.getBoundingBox());
        if (!inRange(mc, eyes, point, maximum) && !cfg.rotations.aimThroughWalls) point = selectedPoint;
        Rotation actual = current(mc), goal = AimGeometryTodoAi.lookAt(eyes, point);
        if (cfg.rotations.lazyRotation && AimGeometryTodoAi.hits(eyes, selected.getBoundingBox(), actual, maximum)) goal = actual;
        start = actual;
        end = smooth(actual, goal, previousGoal, previous, cfg.rotations);
        previousGoal = goal; previous = actual; lastApplied = actual;
    }
    public static void render(Minecraft mc, float partial) {
        if (!ownsLook(mc) || target == null || !allowed(mc, target)) { clearPlan(); return; }
        if (normalCrosshairOn(mc, target)) { paused = true; clearPlan(); return; }
        paused = false;
        if (start == null || end == null) return;
        Rotation actual = current(mc);
        if (lastApplied != null) {
            double yaw = AimGeometryTodoAi.wrap(actual.yaw() - lastApplied.yaw()), pitch = actual.pitch() - lastApplied.pitch();
            start = new Rotation(start.yaw() + yaw, start.pitch() + pitch);
            end = new Rotation(end.yaw() + yaw, end.pitch() + pitch);
        }
        Rotation proposed = start.toward(end, Math.clamp(partial, 0, 1));
        double f = mc.options.sensitivity().get() * 0.6 + 0.2, step = f * f * f * 8 * 0.15;
        mc.player.setYRot((float) (actual.yaw() + Math.round(AimGeometryTodoAi.wrap(proposed.yaw() - actual.yaw()) / step) * step));
        mc.player.setXRot((float) Math.clamp(actual.pitch() + Math.round((proposed.pitch() - actual.pitch()) / step) * step, -90, 90));
        lastApplied = current(mc);
    }
}
