/* Aim geometry and interpolation adapted from LiquidBounce, copyright CCBlueX 2015-2026.
 * Licensed under GPL-3.0-or-later. See AimAssistLicenseTodoAi.txt. */
package com.example.aimassist;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public final class AimGeometryTodoAi {
    private AimGeometryTodoAi() {}
    public record Rotation(double yaw, double pitch) {
        public Vec3 direction() {
            double y = Math.toRadians(yaw), p = Math.toRadians(pitch), c = Math.cos(p);
            return new Vec3(-Math.sin(y) * c, -Math.sin(p), Math.cos(y) * c);
        }
        public Rotation toward(Rotation end, double factor) {
            return new Rotation(yaw + wrap(end.yaw - yaw) * factor, pitch + (end.pitch - pitch) * factor);
        }
    }
    public static double wrap(double angle) {
        angle %= 360;
        return angle >= 180 ? angle - 360 : angle < -180 ? angle + 360 : angle;
    }
    public static Rotation lookAt(Vec3 eyes, Vec3 point) {
        Vec3 d = point.subtract(eyes);
        return new Rotation(Math.toDegrees(Math.atan2(d.z, d.x)) - 90,
            -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
    }
    public static double error(Rotation a, Rotation b) {
        return Math.hypot(wrap(a.yaw - b.yaw), a.pitch - b.pitch);
    }
    public static boolean hits(Vec3 eyes, AABB box, Rotation rotation, double range) {
        return box.contains(eyes) || box.clip(eyes, eyes.add(rotation.direction().scale(range))).isPresent();
    }
    public static Vec3 nearestPoint(Vec3 eyes, AABB box, Rotation current, double range, double mouseStep) {
        double margin = Math.min(Math.max(0.002, eyes.distanceTo(box.getCenter()) *
            Math.tan(Math.toRadians(mouseStep))), Math.min(box.getXsize(), Math.min(box.getYsize(), box.getZsize())) / 4);
        AABB inset = box.deflate(margin);
        Vec3 end = eyes.add(current.direction().scale(range));
        if (box.contains(eyes)) return end;
        Vec3 already = inset.clip(eyes, end).orElse(null);
        if (already != null) return already;
        Vec3[] vertices = new Vec3[8];
        Vec3 best = null;
        double bestError = Double.POSITIVE_INFINITY;
        for (int i = 0; i < 8; i++) {
            vertices[i] = new Vec3((i & 1) == 0 ? inset.minX : inset.maxX,
                (i & 2) == 0 ? inset.minY : inset.maxY, (i & 4) == 0 ? inset.minZ : inset.maxZ);
            double e = error(current, lookAt(eyes, vertices[i]));
            if (e < bestError) { best = vertices[i]; bestError = e; }
        }
        for (int i = 0; i < 8; i++) for (int axis : new int[]{1, 2, 4}) {
            if ((i & axis) != 0) continue;
            Vec3 a = vertices[i], b = vertices[i | axis];
            double low = 0, high = 1;
            for (int n = 0; n < 32; n++) {
                double left = low + (high - low) / 3, right = high - (high - low) / 3;
                if (error(current, lookAt(eyes, a.lerp(b, left))) < error(current, lookAt(eyes, a.lerp(b, right)))) high = right;
                else low = left;
            }
            Vec3 point = a.lerp(b, (low + high) / 2);
            double e = error(current, lookAt(eyes, point));
            if (e < bestError) { best = point; bestError = e; }
        }
        return best;
    }
    public static Rotation neutralPitch(Vec3 eyes, AABB box, Rotation current, double range, double mouseStep) { //codex (old code snippet) public static Rotation centered(Vec3 eyes, AABB box, Rotation current, double range, double mouseStep) {
        if (!hits(eyes, box, current, range) || box.contains(eyes)) return null;
        Rotation goal = constrain(new Rotation(current.yaw, 0), current, r -> hits(eyes, box, r, range)); //codex (old code snippet) Vec3 center = box.getCenter();
        if (goal.pitch == 0) return goal; //codex (old code snippet) double distance = Math.hypot(center.x - eyes.x, center.z - eyes.z);
        double inset = Math.min(Math.max(0, mouseStep), Math.abs(current.pitch - goal.pitch)); //codex (old code snippet) double margin = Math.min(Math.max(0.002, distance * Math.tan(Math.toRadians(mouseStep))), box.getYsize() / 4);
        return new Rotation(current.yaw, goal.pitch + Math.copySign(inset, current.pitch - goal.pitch)); //codex (old code snippet) double height = Math.clamp(eyes.y, box.minY + margin, box.maxY - margin); Rotation goal = lookAt(eyes, new Vec3(center.x, height, center.z)); return hits(eyes, box, goal, range) ? goal : current;
    }
    public static Rotation constrain(Rotation proposed, Rotation goal, Predicate<Rotation> valid) {
        if (valid.test(proposed)) return proposed;
        Rotation seed = new Rotation(proposed.yaw, goal.pitch);
        if (!valid.test(seed)) return null;
        double inside = seed.pitch, outside = proposed.pitch;
        for (int i = 0; i < 32; i++) {
            double p = (inside + outside) / 2;
            if (valid.test(new Rotation(proposed.yaw, p))) inside = p; else outside = p;
        }
        return new Rotation(proposed.yaw, inside);
    }
    public static Rotation smooth(Rotation current, Rotation goal, AimAssistConfigTodoAi.Interpolation settings,
                                  Rotation previousGoal) {
        double change = previousGoal == null ? 0 : Math.clamp(error(previousGoal, goal) / 180, 0, 1) *
            random(settings.directionMin, settings.directionMax) / 100;
        double yawDiff = wrap(goal.yaw - current.yaw), pitchDiff = goal.pitch - current.pitch;
        return new Rotation(current.yaw + yawDiff * factor(Math.abs(yawDiff), random(settings.horizontalMin, settings.horizontalMax) / 100, change, settings.midpoint),
            current.pitch + pitchDiff * factor(Math.abs(pitchDiff), random(settings.verticalMin, settings.verticalMax) / 100, change, settings.midpoint));
    }
    private static double random(int min, int max) {
        int low = Math.clamp(Math.min(min, max), 0, 100), high = Math.clamp(Math.max(min, max), 0, 100);
        return ThreadLocalRandom.current().nextInt(low, high + 1);
    }
    private static double factor(double difference, double speed, double change, double midpoint) {
        double t = Math.clamp(difference / 180, 0, 1);
        if (t > midpoint) {
            double u = 1 - t;
            return ((1 - u) * (1 - u) * 0.05 + 2 * (1 - u) * u + u * u) * speed;
        }
        return 1 / (1 + Math.exp(-0.5 * (t - 0.3))) * Math.clamp(speed + change, 0, 1);
    }
}
