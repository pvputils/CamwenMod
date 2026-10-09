/* Aiming algorithms adapted from LiquidBounce, copyright CCBlueX 2015-2026.
 * GPL-3.0-or-later. See assets/untitled/KillAuraLicenseTodoAi.txt.
 * CamwenMod adaptation is aim-only: no attack, swing, input simulation, or packet sending. */
package com.example.killaura;

import com.example.aimassist.AimGeometryTodoAi;
import com.example.aimassist.AimGeometryTodoAi.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/** Small mathematical ports of point projection and angle smoothing. */
public final class KillAuraGeometryTodoAi {
    private KillAuraGeometryTodoAi() {}
    public static double finite(double value, double fallback, double min, double max) {
        return Double.isFinite(value) ? Math.clamp(value, min, max) : fallback;
    }
    public static double random(double min, double max, double fallback, double limit) {
        double low = finite(Math.min(min, max), fallback, 0, limit);
        double high = finite(Math.max(min, max), fallback, 0, limit);
        return high <= low ? low : ThreadLocalRandom.current().nextDouble(low, high);
    }
    public static Vec3 clamp(Vec3 point, AABB box) {
        return new Vec3(Math.clamp(point.x, box.minX, box.maxX), Math.clamp(point.y, box.minY, box.maxY),
                Math.clamp(point.z, box.minZ, box.maxZ));
    }
    public static List<Vec3> projectedPoints(Vec3 eyes, AABB box) {
        if (box.contains(eyes)) return List.of(clamp(eyes, box));
        Vec3 normal = box.getCenter().subtract(eyes).normalize();
        Vec3 reference = Math.abs(normal.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = normal.cross(reference).normalize(), v = normal.cross(u).normalize();
        double planeDistance = Double.POSITIVE_INFINITY;
        List<Vec3> vertices = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Vec3 vertex = new Vec3((i & 1) == 0 ? box.minX : box.maxX,
                    (i & 2) == 0 ? box.minY : box.maxY, (i & 4) == 0 ? box.minZ : box.maxZ);
            vertices.add(vertex);
            planeDistance = Math.min(planeDistance, vertex.subtract(eyes).dot(normal));
        }
        planeDistance = Math.max(1e-5, planeDistance * 0.9);
        Vec3 origin = eyes.add(normal.scale(planeDistance));
        double minU = 0, maxU = 0, minV = 0, maxV = 0;
        for (Vec3 vertex : vertices) {
            Vec3 ray = vertex.subtract(eyes);
            Vec3 projection = eyes.add(ray.scale(planeDistance / Math.max(1e-8, ray.dot(normal)))).subtract(origin);
            minU = Math.min(minU, projection.dot(u)); maxU = Math.max(maxU, projection.dot(u));
            minV = Math.min(minV, projection.dot(v)); maxV = Math.max(maxV, projection.dot(v));
        }
        List<Vec3> points = new ArrayList<>();
        points.add(clamp(eyes, box));
        for (int i = 0; i < 16; i++) for (int j = 0; j < 8; j++) {
            Vec3 projected = origin.add(u.scale(minU + (maxU - minU) * i / 15))
                    .add(v.scale(minV + (maxV - minV) * j / 7));
            box.clip(eyes, eyes.add(projected.subtract(eyes).scale(101))).ifPresent(points::add);
        }
        return points;
    }
    public static boolean exempt(Vec3 point, Vec3 best, AABB box, KillAuraConfigTodoAi.AimPoint cfg) {
        double third = box.getYsize() / 3;
        if (cfg.exemptHead && point.y > box.maxY - third ||
                cfg.exemptBody && point.y >= box.minY + third && point.y <= box.maxY - third ||
                cfg.exemptFeet && point.y < box.minY + third) return true;
        double horizontal = finite(cfg.exemptHorizontal, 0.1, 0, 1);
        double vertical = finite(cfg.exemptVertical, 0.2, 0, 1);
        return cfg.exemptBestHitVector && Math.hypot(point.x - best.x, point.z - best.z) < horizontal &&
                Math.abs(point.y - best.y) < vertical;
    }
    public static Vec3 choosePoint(Vec3 eyes, AABB box, KillAuraConfigTodoAi.AimPoint cfg, Predicate<Vec3> valid) {
        List<Vec3> points = new ArrayList<>(projectedPoints(eyes, box));
        points.sort(Comparator.comparingDouble(eyes::distanceToSqr));
        Vec3 best = points.getFirst();
        Vec3 filtered = points.stream().filter(p -> !exempt(p, best, box, cfg)).filter(valid).findFirst().orElse(null);
        return filtered != null ? filtered : points.stream().filter(valid).findFirst().orElse(null);
    }
    public static Rotation linear(Rotation current, Rotation goal, double horizontal, double vertical) {
        double yaw = AimGeometryTodoAi.wrap(goal.yaw() - current.yaw()), pitch = goal.pitch() - current.pitch();
        double length = Math.hypot(yaw, pitch);
        if (length < 1e-8) return current;
        double yawLimit = Math.abs(yaw / length) * horizontal, pitchLimit = Math.abs(pitch / length) * vertical;
        return new Rotation(current.yaw() + Math.clamp(yaw, -yawLimit, yawLimit),
                Math.clamp(current.pitch() + Math.clamp(pitch, -pitchLimit, pitchLimit), -90, 90));
    }
    private static double interpolation(double difference, double speed, double change, double midpoint) {
        double t = Math.clamp(difference / 180, 0, 1), u = 1 - t;
        return t > midpoint ? (0.05 * (1-u) * (1-u) + 2 * (1-u) * u + u*u) * speed :
                (1 / (1 + Math.exp(-0.5 * (t - 0.3)))) * Math.clamp(speed + change, 0, 1);
    }
    public static Rotation smooth(Rotation current, Rotation goal, Rotation previousGoal, Rotation previous,
                                  KillAuraConfigTodoAi.Rotations cfg) {
        double horizontal = random(cfg.horizontalMin, cfg.horizontalMax, 180, 180);
        double vertical = random(cfg.verticalMin, cfg.verticalMax, 180, 180);
        if (cfg.smoothing == KillAuraConfigTodoAi.Smoothing.SIGMOID) {
            double delta = Math.min(180, AimGeometryTodoAi.error(current, goal));
            double factor = 1 / (1 + Math.exp(-finite(cfg.steepness, 10, 0, 20) *
                    (delta / 120 - finite(cfg.sigmoidMidpoint, 0.3, 0, 1))));
            horizontal *= factor; vertical *= factor;
        } else if (cfg.smoothing == KillAuraConfigTodoAi.Smoothing.INTERPOLATION) {
            double change = previousGoal == null ? 0 : Math.clamp(AimGeometryTodoAi.error(previousGoal, goal) / 180, 0, 1) *
                    random(cfg.directionPercentMin, cfg.directionPercentMax, 95, 100) / 100;
            double midpoint = finite(cfg.interpolationMidpoint, 0.35, 0, 1);
            double yaw = Math.abs(AimGeometryTodoAi.wrap(goal.yaw() - current.yaw()));
            double pitch = Math.abs(goal.pitch() - current.pitch());
            horizontal = interpolation(yaw, random(cfg.horizontalPercentMin, cfg.horizontalPercentMax, 80, 100) / 100,
                    change, midpoint) * yaw;
            vertical = interpolation(pitch, random(cfg.verticalPercentMin, cfg.verticalPercentMax, 20, 100) / 100,
                    change, midpoint) * pitch;
        } else if (cfg.smoothing == KillAuraConfigTodoAi.Smoothing.ACCELERATION) {
            double yawSpeed = previous == null ? 0 : AimGeometryTodoAi.wrap(current.yaw() - previous.yaw());
            double pitchSpeed = previous == null ? 0 : current.pitch() - previous.pitch();
            double yaw = AimGeometryTodoAi.wrap(goal.yaw() - current.yaw()), pitch = goal.pitch() - current.pitch();
            double yawAcceleration = random(cfg.yawAccelerationMin, cfg.yawAccelerationMax, 20, 180);
            double pitchAcceleration = random(cfg.pitchAccelerationMin, cfg.pitchAccelerationMax, 20, 180);
            double nextYaw = Math.clamp(yaw, yawSpeed - yawAcceleration, yawSpeed + yawAcceleration);
            double nextPitch = Math.clamp(pitch, pitchSpeed - pitchAcceleration, pitchSpeed + pitchAcceleration);
            nextYaw = Math.copySign(Math.min(Math.abs(nextYaw), Math.abs(yaw)), yaw);
            nextPitch = Math.copySign(Math.min(Math.abs(nextPitch), Math.abs(pitch)), pitch);
            return new Rotation(current.yaw() + nextYaw, Math.clamp(current.pitch() + nextPitch, -90, 90));
        }
        return linear(current, goal, horizontal, vertical);
    }
}
