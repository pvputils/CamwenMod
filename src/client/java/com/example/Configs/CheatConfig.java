package com.example.Configs;

import static com.example.UntitledClient.config;

public class CheatConfig {
    //    public boolean isEthylene = false;
    // codex start
    public boolean isEthylene = false;
    // codex end
//    TODO; // slower than walking should use static
    // TODO -> vs sprinting/jumping/speed etc.
    public boolean isTargetingMarginReverted = false;
    //    public record MovementPair() {}
//    public HashMap<> advanced?dynamic? reach
    public float staticTargetingMarginBypass = .0f;
//    public float movingTargetMarginBypass = 0.f;
//    public float doubleWalkingTargetMarginBypass = 0.f;
//    TODO;
//    public float sprintVsWalkingTargetMarginBypass = 0.f;
//    public float speedVsWalkingTargetMarginBypass = 0.f;
//    public float walkJumpVsWalkingTargetMarginBypass = 0.f;
//    public float targetingMarginWidthBypass = 0.f;
//    public double attackVelocityBypass = 0.6;
//    public boolean isAutoCobweb = false; // TODO -> struct?
//    public double cobwebRangeBypassDelta = .0f;

    public float computeTargetingMarginBypass() {
        if (!config.isCheatsEnabled) {
            return 0.f;
        }

        float base = isTargetingMarginReverted
                ? .1f
                : 0.f;
        float one = staticTargetingMarginBypass;
//        float two = isMoving
//                ? movingTargetMarginBypass
//                : 0.f;
//        float three = isMoving && isTargetMovingPlayer
//                ? doubleWalkingTargetMarginBypass
//                : 0.f;
        return base + one;
    }
}
