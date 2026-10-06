package com.example.aimassist;

import static com.example.aimassist.AimGeometryTodoAi.*;

/** Secondary correction guard; does not select targets or replace margin picking. */
public final class AimCorrectionFovTodoAi {
    public static boolean allows(Rotation current, Rotation goal, double maximumDegrees) {
        if (current == null || goal == null || !Double.isFinite(maximumDegrees) || maximumDegrees < 0 || maximumDegrees > 180) return false;
        double dot = current.direction().dot(goal.direction());
        if (!Double.isFinite(dot)) return false;
        return Math.toDegrees(Math.acos(Math.clamp(dot, -1, 1))) <= maximumDegrees + 1e-7;
    }
    private AimCorrectionFovTodoAi() {}
}
