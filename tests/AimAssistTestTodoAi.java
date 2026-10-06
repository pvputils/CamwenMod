package com.example.aimassist;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import static com.example.aimassist.AimGeometryTodoAi.*;

public final class AimAssistTestTodoAi {
    private static void check(boolean pass, String description) {
        if (!pass) throw new AssertionError(description);
    }
    public static void main(String[] args) throws Exception {
        Vec3 eyes = Vec3.ZERO;
        AABB edgeBox = new AABB(0.5, -0.5, 3, 1.5, 0.5, 4);
        Rotation outside = new Rotation(0, 0);
        Rotation nearest = lookAt(eyes, nearestPoint(eyes, edgeBox, outside, 8, 0.15));
        check(error(outside, nearest) < 7.5, "nearest screen-space edge, not world-space closest point");
        check(hits(eyes, edgeBox.deflate(0.001), nearest, 8), "aura enters hitbox interior");
        Rotation inside = new Rotation(-14, 0);
        check(error(inside, lookAt(eyes, nearestPoint(eyes, edgeBox, inside, 8, 0.15))) < 1e-6, "aura preserves existing hit");
        AABB close = new AABB(-0.25, -1.62, 0.75, 0.25, 0.3, 1.25);
        check(error(outside, lookAt(eyes, nearestPoint(eyes, close, outside, 4.2, 0.15))) < 1e-6, "close hitbox stays inside FOV");
        for (int sign : new int[]{-1, 1}) {
            AABB box = sign < 0 ? new AABB(0.5, -1.5, 1, 1.5, -0.5, 2) : new AABB(0.5, 0.2, 1, 1.5, 1, 2);
            Rotation current = lookAt(eyes, new Vec3(1.49, sign < 0 ? -1.49 : 0.99, 1.99)); //codex (old code snippet) Rotation current = lookAt(eyes, box.getCenter());
            Rotation goal = neutralPitch(eyes, box, current, 4, 0.15); //codex (old code snippet) Rotation goal = centered(eyes, box, current, 4, 0.15);
            check(goal != null && hits(eyes, box, goal, 4), "neutral pitch preserves an above/below hit"); //codex (old code snippet) check(goal != null && hits(eyes, box, goal, 4), "centered goal preserves hit above/below");
            check(goal.yaw() == current.yaw(), "off-center target never changes yaw"); //codex (old code snippet) Vec3 atDepth = eyes.add(goal.direction().scale(box.getCenter().z / goal.direction().z));
            check(Math.abs(goal.pitch()) < Math.abs(current.pitch()), "pitch moves toward zero"); //codex (old code snippet) check(Math.abs(atDepth.x - box.getCenter().x) < 1e-6, "horizontal center");
            check(Math.signum(goal.pitch()) == Math.signum(current.pitch()), "pitch never crosses neutral"); //codex (old code snippet) double boundary = sign < 0 ? box.maxY : box.minY; check(Math.abs(atDepth.y - boundary) < 0.02, "neutral height evaluated at center depth");
        }
        AABB conflict = new AABB(0.5, 0.2, 1, 1.5, 1, 2);
        Rotation corner = lookAt(eyes, new Vec3(1.49, 0.201, 1.99));
        Rotation goal = neutralPitch(eyes, conflict, corner, 4, 0.15); //codex (old code snippet) Rotation goal = centered(eyes, conflict, corner, 4, 0.15);
        check(goal.yaw() == corner.yaw() && Math.abs(goal.pitch()) <= Math.abs(corner.pitch()), "neutral pitch wins over hitbox centering"); //codex (old code snippet) check(Math.abs(goal.pitch()) > Math.abs(corner.pitch()), "centering wins over neutral angle");
        check(neutralPitch(eyes, conflict, new Rotation(90, 0), 4, 0.15) == null, "targetting never acquires an invalid hit"); //codex (old code snippet) Rotation constrained = constrain(new Rotation(goal.yaw(), corner.pitch()), goal, r -> hits(eyes, conflict, r, 4));
        AABB levelBox = new AABB(0.5, -1, 1, 1.5, 1, 2); //codex (old code snippet) check(constrained != null && constrained.yaw() == goal.yaw() && hits(eyes, conflict, constrained, 4), "simultaneous turn clamps pitch instead of losing yaw progress");
        Rotation offCenter = lookAt(eyes, new Vec3(1.49, 0.8, 1.99)); //codex (old code snippet) check(centered(eyes, conflict, new Rotation(90, 0), 4, 0.15) == null, "targetting never acquires an invalid hit");
        // codex start
        Rotation levelGoal = neutralPitch(eyes, levelBox, offCenter, 4, 0.15);
        check(levelGoal.yaw() == offCenter.yaw() && levelGoal.pitch() == 0, "neutral pitch reaches zero without centering yaw");
        check(neutralPitch(eyes, levelBox, levelGoal, 4, 0.15).equals(levelGoal), "already neutral stays unchanged");
        check(neutralPitch(eyes, levelBox, offCenter, 0.1, 0.15) == null, "out-of-range target stays inactive");
        var neutralSettings = new AimAssistConfigTodoAi.Interpolation();
        Rotation neutralSmoothed = smooth(offCenter, levelGoal, neutralSettings, null);
        check(neutralSmoothed.yaw() == offCenter.yaw(), "neutral-only smoothing leaves yaw untouched");
        //codex end
        check(!AimAssistControllerTodoAi.clickActive(false, 100, 100 + 200_000_000, 200), "window ends at 200ms");
        check(AimAssistControllerTodoAi.clickActive(false, 100, 100 + 199_000_000, 200), "every attempt activates 199ms later");
        check(!AimAssistControllerTodoAi.clickActive(false, 100, 100, 0), "zero disables post-attempt window");
        check(AimAssistControllerTodoAi.clickActive(true, 0, 100, 0), "held click remains mandatory alternative");
        var cfg = new AimAssistConfigTodoAi();
        cfg.targetting.interpolation.verticalMin = cfg.targetting.interpolation.verticalMax = 25;
        Rotation smoothed = smooth(new Rotation(8, 10), new Rotation(0, 0), cfg.targetting.interpolation, null);
        check(smoothed.yaw() > 0 && smoothed.yaw() < 8 && smoothed.pitch() > 0 && smoothed.pitch() < 10, "both axes interpolate together");
        check(cfg.aura.interpolation != cfg.targetting.interpolation, "independent settings");
        try {
            AimAssistScreenTodoAi.apply(cfg.aura.requires, cfg.aura.requires.getClass().getField("attackWindow"), "201");
            throw new AssertionError("window must reject >200ms");
        } catch (IllegalArgumentException expected) {}
        System.out.println("Aim assist geometry, interpolation, requirements and config checks passed.");
    }
}
