package com.example.killaura;

import java.util.ArrayList;
import java.util.List;

/** Native CamwenMod settings. Combat always uses the held item's vanilla reach. */
public final class KillAuraConfigTodoAi {
    public boolean enabled = false;
    public String raycast = "All";
    public Target target = new Target();
    public Targets targets = new Targets();
    public Rotations rotations = new Rotations();
    public Visuals visuals = new Visuals();

    public static final class Target {
        public double fov = 180;
        public int hurtTime = 10;
        public boolean ignoreShield = true;
        public boolean excludeTeammates = false;
        public List<String> priorities = new ArrayList<>(List.of("Type", "Health"));
    }
    public static final class Targets {
        public boolean players = true, hostile = true, angerable = true, waterCreatures = true;
        public boolean passive = false, invisible = true, dead = false;
    }
    public static final class Rotations {
        public String timing = "Normal", smoothing = "Linear", movementCorrection = "Silent";
        public boolean lazyRotation = false;
        public double resetThreshold = 2;
        public int ticksUntilReset = 5;
        public Linear linear = new Linear();
        public Sigmoid sigmoid = new Sigmoid();
        public Interpolation interpolation = new Interpolation();
        public Acceleration acceleration = new Acceleration();
        public ShortStop shortStop = new ShortStop();
        public Fail fail = new Fail();
    }
    public static class Linear {
        public double horizontalMin = 180, horizontalMax = 180, verticalMin = 180, verticalMax = 180;
    }
    public static final class Sigmoid extends Linear {
        public double steepness = 10, midpoint = 0.3;
    }
    public static final class Interpolation {
        public int horizontalMin = 80, horizontalMax = 85, verticalMin = 20, verticalMax = 25;
        public int directionMin = 95, directionMax = 100;
        public double midpoint = 0.35;
    }
    public static final class Acceleration {
        public double yawMin = 20, yawMax = 25, pitchMin = 20, pitchMax = 25;
        public boolean dynamic = false;
        public double distanceCoefficient = -1.393;
        public double crosshairYawMin = 17, crosshairYawMax = 20, crosshairPitchMin = 17, crosshairPitchMax = 20;
        public boolean accelerationError = true, constantError = true, sigmoidDeceleration = false;
        public double yawAccelerationError = 0.1, pitchAccelerationError = 0.1;
        public double yawConstantError = 0.1, pitchConstantError = 0.1;
        public double steepness = 10, midpoint = 0.3;
    }
    public static final class ShortStop {
        public boolean enabled = false;
        public int rate = 3, durationMin = 1, durationMax = 2;
    }
    public static final class Fail {
        public boolean enabled = false;
        public int rate = 3, durationMin = 1, durationMax = 4;
        public double factor = 0.04, horizontalMin = 5, horizontalMax = 10, verticalMin = 0, verticalMax = 2;
    }
    /** Uses Minecraft's existing HUD drawing rather than LiquidBounce's rendering framework. */
    public static final class Visuals {
        public boolean targetMarker = true, rangeIndicator = false;
        public int color = 0xFF50FF50;
    }

    public void repair() {
        if (target == null) target = new Target();
        if (targets == null) targets = new Targets();
        if (rotations == null) rotations = new Rotations();
        if (visuals == null) visuals = new Visuals();
        if (target.priorities == null || target.priorities.isEmpty()) target.priorities = new ArrayList<>(List.of("Type", "Health"));
        if (target.priorities.stream().anyMatch(priority -> priority == null || !List.of("Type", "Health", "Distance", "Direction", "HurtTime", "Age").contains(priority))) {
            target.priorities = new ArrayList<>(target.priorities.stream().filter(priority -> priority != null && List.of("Type", "Health", "Distance", "Direction", "HurtTime", "Age").contains(priority)).toList());
        }
        if (target.priorities.isEmpty()) target.priorities = new ArrayList<>(List.of("Type"));
        if (rotations.linear == null) rotations.linear = new Linear();
        if (rotations.sigmoid == null) rotations.sigmoid = new Sigmoid();
        if (rotations.interpolation == null) rotations.interpolation = new Interpolation();
        if (rotations.acceleration == null) rotations.acceleration = new Acceleration();
        if (rotations.shortStop == null) rotations.shortStop = new ShortStop();
        if (rotations.fail == null) rotations.fail = new Fail();
        if (!List.of("All", "OnlyEnemy", "None").contains(raycast == null ? "" : raycast)) raycast = "All";
        if (!List.of("Normal", "Snap", "OnTick").contains(rotations.timing == null ? "" : rotations.timing)) rotations.timing = "Normal";
        if (!List.of("Linear", "Sigmoid", "Interpolation", "Acceleration").contains(rotations.smoothing == null ? "" : rotations.smoothing)) rotations.smoothing = "Linear";
        if (!List.of("Off", "Strict", "Silent", "ChangeLook").contains(rotations.movementCorrection == null ? "" : rotations.movementCorrection)) rotations.movementCorrection = "Silent";
        target.fov = finite(target.fov, 0, 180, 180);
        target.hurtTime = Math.clamp(target.hurtTime, 0, 10);
        rotations.resetThreshold = finite(rotations.resetThreshold, 1, 180, 2);
        rotations.ticksUntilReset = Math.clamp(rotations.ticksUntilReset, 1, 30);
    }
    static double finite(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.clamp(value, min, max) : fallback;
    }
}
