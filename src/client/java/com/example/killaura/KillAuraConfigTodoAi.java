/* Aiming algorithms adapted from LiquidBounce, copyright CCBlueX 2015-2026.
 * GPL-3.0-or-later. See assets/untitled/KillAuraLicenseTodoAi.txt.
 * CamwenMod adaptation is aim-only: no attack, swing, input simulation, or packet sending. */
package com.example.killaura;

/** Native settings saved by CamwenMod, independent of LiquidBounce's runtime. */
public final class KillAuraConfigTodoAi {
    public boolean enabled;
    public Range range = new Range();
    public Target target = new Target();
    public AimPoint aimPoint = new AimPoint();
    public Rotations rotations = new Rotations();
    public enum Priority { HEALTH, DISTANCE, DIRECTION, HURT_TIME, AGE }
    public enum Smoothing { LINEAR, SIGMOID, INTERPOLATION, ACCELERATION }
    public static final class Range {
        public double rangeIncrease = 1;
        public double throughWallsRange = 3;
        public double scanRangeMin = 2, scanRangeMax = 3;
    }
    public static final class Target {
        public Priority priority = Priority.HEALTH;
        public double fov = 180;
        public int hurtTime = 10;
        public boolean invisible = true, sleeping, dead, ignoreShield = true;
    }
    public static final class AimPoint {
        public boolean exemptHead, exemptBody, exemptFeet, exemptBestHitVector;
        public double exemptHorizontal = 0.1, exemptVertical = 0.2;
        public boolean delay, lazy, gaussian;
        public int delayMin = 2, delayMax = 4;
        public double lazyMin = 0.1, lazyMax = 0.2;
        public double gaussianYaw, gaussianPitch;
        public double gaussianSpeedMin = 0.1, gaussianSpeedMax = 0.2;
        public double gaussianTolerance = 0.05, gaussianChance = 100;
    }
    public static final class Rotations {
        public Smoothing smoothing = Smoothing.LINEAR;
        public double horizontalMin = 180, horizontalMax = 180;
        public double verticalMin = 180, verticalMax = 180;
        public double steepness = 10, sigmoidMidpoint = 0.3;
        public double interpolationMidpoint = 0.35;
        public int horizontalPercentMin = 80, horizontalPercentMax = 85;
        public int verticalPercentMin = 20, verticalPercentMax = 25;
        public int directionPercentMin = 95, directionPercentMax = 100;
        public double yawAccelerationMin = 20, yawAccelerationMax = 25;
        public double pitchAccelerationMin = 20, pitchAccelerationMax = 25;
        public boolean lazyRotation, aimThroughWalls;
    }
    public void repair() {
        if (range == null) range = new Range();
        if (target == null) target = new Target();
        if (aimPoint == null) aimPoint = new AimPoint();
        if (rotations == null) rotations = new Rotations();
        if (target.priority == null) target.priority = Priority.HEALTH;
        if (rotations.smoothing == null) rotations.smoothing = Smoothing.LINEAR;
    }
}
