/*
 * Standalone adaptation of LiquidBounce, Copyright (c) 2015-2026 CCBlueX.
 * Modified for CamwenMod: explicit entity requests and Minecraft 26.2.
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Distributed without warranty; see LICENSE-LiquidBounceTodoAi.txt.
 * Upstream: CCBlueX/LiquidBounce debd001505f39fc0c41698496b895e5dd9d2dfd5
 */
package com.example.combat;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

/**
 * Adapted from RotationFinding.raytraceBox and EntityRaytracing.isLookingAtEntity.
 * Keeps the preferred-ray fast path, samples the box if needed, and ranks visible
 * rotations before wall-range rotations. Minecraft integration supplies occlusion.
 */
public final class AuraAimTodoAi {
    private AuraAimTodoAi() {}

    public record Rotation(float yaw, float pitch) {
        public Vec3 direction() { return Vec3.directionFromRotation(pitch, yaw); }
    }

    public record Aim(Rotation rotation, Vec3 hit, boolean visible) {}

    public static Aim find(Vec3 eyes, AABB box, Rotation previous, double range,
                           double wallsRange, double sensitivity, Predicate<Vec3> visible,
                           Predicate<Vec3> unobstructedByEntity) {
        Aim preferred = trace(eyes, box, previous, range, wallsRange, visible, unobstructedByEntity);
        if (preferred != null && preferred.visible()) return preferred;
        Aim best = preferred;
        double bestDifference = preferred == null ? Double.POSITIVE_INFINITY : 0;
        // Upstream searches candidate points when the preferred point cannot be hit.
        // Use the real box, not CamwenMod's expanded targeting margin.
        for (int x = 1; x <= 9; x += 2) {
            for (int y = 1; y <= 9; y += 2) {
                for (int z = 1; z <= 9; z += 2) {
                    Vec3 point = new Vec3(Mth.lerp(x / 10.0, box.minX, box.maxX),
                            Mth.lerp(y / 10.0, box.minY, box.maxY),
                            Mth.lerp(z / 10.0, box.minZ, box.maxZ));
                    Rotation rotation = quantize(lookingAt(eyes, point), previous, sensitivity);
                    Aim candidate = trace(eyes, box, rotation, range, wallsRange, visible, unobstructedByEntity);
                    if (candidate == null) continue;
                    double difference = difference(previous, rotation);
                    if (best == null || candidate.visible() && !best.visible() ||
                            candidate.visible() == best.visible() && difference < bestDifference) {
                        best = candidate;
                        bestDifference = difference;
                    }
                }
            }
        }
        return best;
    }

    public static Aim trace(Vec3 eyes, AABB box, Rotation rotation, double range,
                            double wallsRange, Predicate<Vec3> visible,
                            Predicate<Vec3> unobstructedByEntity) {
        Vec3 end = eyes.add(rotation.direction().scale(range));
        Vec3 hit = box.contains(eyes) ? eyes : box.clip(eyes, end).orElse(null);
        if (hit == null || !unobstructedByEntity.test(hit)) return null;
        boolean canSee = visible.test(hit);
        double allowed = canSee ? range : wallsRange;
        if (!canSee && wallsRange == 0 || eyes.distanceToSqr(hit) > allowed * allowed) return null;
        return new Aim(rotation, hit, canSee);
    }

    public static Rotation lookingAt(Vec3 eyes, Vec3 point) {
        Vec3 delta = point.subtract(eyes);
        return new Rotation((float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90,
                (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
    }

    private static Rotation quantize(Rotation wanted, Rotation previous, double sensitivity) {
        double factor = sensitivity * 0.6 + 0.2;
        double step = factor * factor * factor * 8 * 0.15;
        float yaw = previous.yaw + (float) (Math.round(Mth.wrapDegrees(wanted.yaw - previous.yaw) / step) * step);
        float pitch = previous.pitch + (float) (Math.round((wanted.pitch - previous.pitch) / step) * step);
        return new Rotation(yaw, Mth.clamp(pitch, -90, 90));
    }

    private static double difference(Rotation a, Rotation b) {
        return Math.hypot(Mth.wrapDegrees(a.yaw - b.yaw), a.pitch - b.pitch);
    }
}
