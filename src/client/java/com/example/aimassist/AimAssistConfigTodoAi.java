package com.example.aimassist;


/** Targeting assist and its live settings; saved with CamwenMod's config. */ //codex (old code snippet) /** Only the two aim assists and their live settings; saved with CamwenMod's config. */
public final class AimAssistConfigTodoAi {
    public Assist targetting = new Assist();
    public Targets targets = new Targets();
    public TargetLock targetLock = new TargetLock();

    public static final class Assist {
        public boolean enabled;
        public Requirements requires = new Requirements();
        public Interpolation interpolation = new Interpolation();
        public double range = 4.2;
        // codex start
        public double centerlineWidth = 40;
        public double maxCorrectionFov = 30;
        //codex end
    }
    public static final class Requirements {
        public int attackWindow = 200;
        public boolean notBreaking;
    }
    public static final class Interpolation {
        public int horizontalMin = 80, horizontalMax = 85;
        public int verticalMin = 20, verticalMax = 25;
        public int directionMin = 95, directionMax = 100;
        public double midpoint = 0.35;
    }
    public static final class Targets {
        public boolean players = true, hostile = true, angerable = true, waterCreature = true;
        public boolean passive, armorStand, invisible = true, dead;
    }
    public static final class TargetLock {
        public boolean enabled;
        public String mode = "Temporary";
        public int maximumTime = 30;
        public double maximumRange = 20;
        public boolean allowWhenUnlocked = true;
        public String usernames = "Notch";
        public boolean whitelist = true;
    }
}
