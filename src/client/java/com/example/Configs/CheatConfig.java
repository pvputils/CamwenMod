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
    public float movingTargetingMarginBypass = .0f;
    // codex start
    public transient Float aimAssistMarginOverrideTodoAi;
    //codex end
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

    public float computeTargetingMarginBypass(
            boolean isMoving, boolean isTargetMovingPlayer) {
        if (!config.isCheatsEnabled) {
            return 0.f;
        }

        // codex start
        if (aimAssistMarginOverrideTodoAi != null) return aimAssistMarginOverrideTodoAi;
        //codex end
        float base = isTargetingMarginReverted
                ? .1f
                : 0.f;
//        float one = 0.f;
//        float two = isMoving
//                ? movingTargetMarginBypass
//                : 0.f;
        float three = isMoving && isTargetMovingPlayer
                ? movingTargetingMarginBypass
                : 0.f;
        return base + three;
    }
}
