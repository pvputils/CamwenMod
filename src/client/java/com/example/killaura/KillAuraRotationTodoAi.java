/* Rotation smoothing adapted from LiquidBounce, copyright CCBlueX 2015-2026.
 * GPL-3.0-or-later. See KillAuraLicenseTodoAi.txt. */
package com.example.killaura;

import java.util.concurrent.ThreadLocalRandom;

/** Stateful rotation math with no event bus or module framework. */
public final class KillAuraRotationTodoAi {
    public record Rotation(double yaw, double pitch) {
        public Rotation add(double yaw, double pitch) { return new Rotation(this.yaw + yaw, Math.clamp(this.pitch + pitch, -90, 90)); }
    }
    private Rotation previous, previousGoal;
    private int stopTicks, failTicks;
    private double failYaw, failPitch;
    public void reset() { previous = previousGoal = null; stopTicks = failTicks = 0; }
    public static double wrap(double angle) { return angle - Math.floor((angle + 180) / 360) * 360; }
    public static double distance(Rotation a, Rotation b) { return Math.hypot(wrap(b.yaw - a.yaw), b.pitch - a.pitch); }
    private static double random(double min, double max, double low, double high) {
        min = KillAuraConfigTodoAi.finite(min, low, high, low);
        max = KillAuraConfigTodoAi.finite(max, low, high, min);
        double first = Math.min(min, max), last = Math.max(min, max);
        return first == last ? first : ThreadLocalRandom.current().nextDouble(first, last);
    }
    private static int duration(int min, int max, int limit) {
        return ThreadLocalRandom.current().nextInt(Math.clamp(Math.min(min, max), 0, limit), Math.clamp(Math.max(min, max), 0, limit) + 1);
    }
    private static boolean chance(int rate) { return ThreadLocalRandom.current().nextDouble(100) < Math.clamp(rate, 0, 100); }
    public static Rotation linear(Rotation current, Rotation goal, double horizontal, double vertical) {
        double yaw = wrap(goal.yaw - current.yaw), pitch = goal.pitch - current.pitch;
        double length = Math.hypot(yaw, pitch);
        if (length < 1E-9) return current;
        double yawLimit = Math.abs(yaw / length) * horizontal, pitchLimit = Math.abs(pitch / length) * vertical;
        return current.add(Math.clamp(yaw, -yawLimit, yawLimit), Math.clamp(pitch, -pitchLimit, pitchLimit));
    }
    private static Rotation linear(Rotation current, Rotation goal, KillAuraConfigTodoAi.Linear config) {
        return linear(current, goal, random(config.horizontalMin, config.horizontalMax, 0, 180), random(config.verticalMin, config.verticalMax, 0, 180));
    }
    private Rotation interpolation(Rotation current, Rotation goal, KillAuraConfigTodoAi.Interpolation config) {
        double yaw = Math.abs(wrap(goal.yaw - current.yaw)), pitch = Math.abs(goal.pitch - current.pitch);
        double change = previousGoal == null ? 0 : Math.clamp(distance(previousGoal, goal) / 180, 0, 1) * random(config.directionMin, config.directionMax, 0, 100) / 100;
        return linear(current, goal, yaw * factor(yaw, random(config.horizontalMin, config.horizontalMax, 1, 100) / 100, change, config.midpoint),
            pitch * factor(pitch, random(config.verticalMin, config.verticalMax, 1, 100) / 100, change, config.midpoint));
    }
    private static double factor(double angle, double speed, double change, double midpoint) {
        double t = Math.clamp(angle / 180, 0, 1), u = 1 - t;
        return t > KillAuraConfigTodoAi.finite(midpoint, 0, 1, 0.35)
            ? (t * t * 0.05 + 2 * t * u + u * u) * speed
            : 1 / (1 + Math.exp(-0.5 * (t - 0.3))) * Math.clamp(speed + change, 0, 1);
    }
    public Rotation step(Rotation current, Rotation goal, KillAuraConfigTodoAi.Rotations config, boolean crosshair,
                         double distance) {
        Rotation next = switch (config.smoothing) {
            case "Sigmoid" -> {
                double factor = 1 / (1 + Math.exp(-KillAuraConfigTodoAi.finite(config.sigmoid.steepness, 0, 20, 10) *
                    (Math.min(180, distance(current, goal)) / 120 - KillAuraConfigTodoAi.finite(config.sigmoid.midpoint, 0, 1, 0.3))));
                yield linear(current, goal, factor * random(config.sigmoid.horizontalMin, config.sigmoid.horizontalMax, 0, 180),
                    factor * random(config.sigmoid.verticalMin, config.sigmoid.verticalMax, 0, 180));
            }
            case "Interpolation" -> interpolation(current, goal, config.interpolation);
            case "Acceleration" -> acceleration(current, goal, config.acceleration, crosshair, distance);
            default -> linear(current, goal, config.linear);
        };
        var fail = config.fail;
        if (fail.enabled) {
            if (chance(fail.rate)) {
                failTicks = duration(fail.durationMin, fail.durationMax, 20);
                failYaw = random(fail.horizontalMin, fail.horizontalMax, 1, 90) * (ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
                failPitch = random(fail.verticalMin, fail.verticalMax, 0, 90) * (ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
            }
            if (failTicks-- > 0 && previous != null) next = next.add(failYaw + wrap(previous.yaw - current.yaw) * KillAuraConfigTodoAi.finite(fail.factor, 0.01, 0.99, 0.04),
                failPitch + (previous.pitch - current.pitch) * KillAuraConfigTodoAi.finite(fail.factor, 0.01, 0.99, 0.04));
        } else failTicks = 0;
        var stop = config.shortStop;
        if (stop.enabled) {
            if (chance(stop.rate)) stopTicks = duration(stop.durationMin, stop.durationMax, 5);
            if (stopTicks-- > 0) next = linear(current, next, random(0, 0.1, 0, 0.1), random(0, 0.1, 0, 0.1));
        } else stopTicks = 0;
        previous = current; previousGoal = goal;
        return next;
    }
    private Rotation acceleration(Rotation current, Rotation goal, KillAuraConfigTodoAi.Acceleration config, boolean crosshair, double distance) {
        Rotation last = previous == null ? current : previous;
        double yaw = wrap(goal.yaw - current.yaw), pitch = goal.pitch - current.pitch;
        double previousYaw = wrap(current.yaw - last.yaw), previousPitch = current.pitch - last.pitch;
        boolean dynamic = config.dynamic && crosshair;
        double distanceFactor = KillAuraConfigTodoAi.finite(config.distanceCoefficient, -2, 2, -1.393) * distance;
        double factor = config.sigmoidDeceleration ? 1 / (1 + Math.exp(-KillAuraConfigTodoAi.finite(config.steepness, 0, 20, 10) *
            (Math.hypot(yaw, pitch) / 120 - KillAuraConfigTodoAi.finite(config.midpoint, 0, 1, 0.3)))) : 1;
        double yawAccel = random(dynamic ? config.crosshairYawMin : config.yawMin, dynamic ? config.crosshairYawMax : config.yawMax, 1, 180);
        double pitchAccel = random(dynamic ? config.crosshairPitchMin : config.pitchMin, dynamic ? config.crosshairPitchMax : config.pitchMax, 1, 180);
        double ay = Math.clamp(wrap(yaw - previousYaw), -yawAccel + distanceFactor, yawAccel + distanceFactor) * factor;
        double ap = Math.clamp(wrap(pitch - previousPitch), -pitchAccel + distanceFactor, pitchAccel + distanceFactor) * factor;
        return current.add(previousYaw + ay + error(ay, config.accelerationError, config.yawAccelerationError, config.constantError, config.yawConstantError),
            previousPitch + ap + error(ap, config.accelerationError, config.pitchAccelerationError, config.constantError, config.pitchConstantError));
    }
    private static double error(double acceleration, boolean accelerationEnabled, double accelerationError, boolean constantEnabled, double constantError) {
        double ae = KillAuraConfigTodoAi.finite(accelerationError, 0, 1, 0.1), ce = KillAuraConfigTodoAi.finite(constantError, 0, 1, 0.1);
        return (accelerationEnabled ? random(-ae, ae, -1, 1) * acceleration : 0) + (constantEnabled ? random(-ce, ce, -1, 1) : 0);
    }
    public static Rotation quantize(Rotation previous, Rotation next, double sensitivity) {
        double step = Math.pow(Math.clamp(sensitivity, 0, 1) * 0.6 + 0.2, 3) * 1.2;
        return previous.add(Math.rint(wrap(next.yaw - previous.yaw) / step) * step, Math.rint((next.pitch - previous.pitch) / step) * step);
    }
    /** Conservative arrival estimate using minimum configured speeds, without altering live state. */
    public static int estimateTicks(Rotation current, Rotation goal, KillAuraConfigTodoAi.Rotations config) {
        String mode = config.smoothing;
        Rotation previous = current;
        for (int ticks = 0; ticks < 80; ticks++) {
            Rotation next;
            if (mode.equals("Interpolation")) {
                var interpolation = config.interpolation;
                double yaw = Math.abs(wrap(goal.yaw - current.yaw)), pitch = Math.abs(goal.pitch - current.pitch);
                next = linear(current, goal, yaw * factor(yaw, Math.clamp(Math.min(interpolation.horizontalMin, interpolation.horizontalMax), 1, 100) / 100.0, 0, interpolation.midpoint),
                    pitch * factor(pitch, Math.clamp(Math.min(interpolation.verticalMin, interpolation.verticalMax), 1, 100) / 100.0, 0, interpolation.midpoint));
            } else if (mode.equals("Acceleration")) {
                double yaw = wrap(goal.yaw - current.yaw), pitch = goal.pitch - current.pitch;
                double vy = wrap(current.yaw - previous.yaw), vp = current.pitch - previous.pitch;
                double ay = KillAuraConfigTodoAi.finite(Math.min(config.acceleration.yawMin, config.acceleration.yawMax), 1, 180, 20);
                double ap = KillAuraConfigTodoAi.finite(Math.min(config.acceleration.pitchMin, config.acceleration.pitchMax), 1, 180, 20);
                next = current.add(vy + Math.clamp(wrap(yaw - vy), -ay, ay), vp + Math.clamp(wrap(pitch - vp), -ap, ap));
            } else {
                var speed = mode.equals("Sigmoid") ? config.sigmoid : config.linear;
                double factor = mode.equals("Sigmoid") ? 1 / (1 + Math.exp(-config.sigmoid.steepness * (Math.min(180, distance(current, goal)) / 120 - config.sigmoid.midpoint))) : 1;
                next = linear(current, goal, factor * KillAuraConfigTodoAi.finite(Math.min(speed.horizontalMin, speed.horizontalMax), 0, 180, 180),
                    factor * KillAuraConfigTodoAi.finite(Math.min(speed.verticalMin, speed.verticalMax), 0, 180, 180));
            }
            if (distance(next, goal) <= 2) return ticks;
            if (next.equals(current)) return 80;
            previous = current; current = next;
        }
        return 80;
    }
}
