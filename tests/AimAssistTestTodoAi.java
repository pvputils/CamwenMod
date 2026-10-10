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
        check(hits(eyes, edgeBox.deflate(0.001), nearest, 8), "nearest point enters hitbox interior"); //codex (old code snippet) check(hits(eyes, edgeBox.deflate(0.001), nearest, 8), "aura enters hitbox interior");
        Rotation inside = new Rotation(-14, 0);
        check(error(inside, lookAt(eyes, nearestPoint(eyes, edgeBox, inside, 8, 0.15))) < 1e-6, "nearest point preserves existing hit"); //codex (old code snippet) check(error(inside, lookAt(eyes, nearestPoint(eyes, edgeBox, inside, 8, 0.15))) < 1e-6, "aura preserves existing hit");
        AABB close = new AABB(-0.25, -1.62, 0.75, 0.25, 0.3, 1.25);
        check(error(outside, lookAt(eyes, nearestPoint(eyes, close, outside, 4.2, 0.15))) < 1e-6, "close hitbox stays inside FOV");
        for (int sign : new int[]{-1, 1}) {
            AABB box = sign < 0 ? new AABB(0.5, -1.5, 1, 1.5, -0.5, 2) : new AABB(0.5, 0.2, 1, 1.5, 1, 2);
            Rotation current = lookAt(eyes, new Vec3(1.49, sign < 0 ? -1.49 : 0.99, 1.99)); //codex (old code snippet) Rotation current = lookAt(eyes, box.getCenter());
            Rotation goal = neutralPitch(eyes, box, current, 4, 0.15); //codex (old code snippet) Rotation goal = centered(eyes, box, current, 4, 0.15);
            check(goal != null && hits(eyes, box, goal, 4), "neutral pitch preserves an above/below hit"); //codex (old code snippet) check(goal != null && hits(eyes, box, goal, 4), "centered goal preserves hit above/below");
            check(goal.yaw() == current.yaw(), "off-center target never changes yaw"); //codex (old code snippet) Vec3 atDepth = eyes.add(goal.direction().scale(box.getCenter().z / goal.direction().z));
            check(sign < 0 ? goal.pitch() < current.pitch() : goal.pitch() == current.pitch(), "head moves upward only; upward-looking aim is preserved"); //codex (old code snippet) check(Math.abs(atDepth.x - box.getCenter().x) < 1e-6, "horizontal center");
            check(Math.signum(goal.pitch()) == Math.signum(current.pitch()), "pitch never crosses neutral"); //codex (old code snippet) double boundary = sign < 0 ? box.maxY : box.minY; check(Math.abs(atDepth.y - boundary) < 0.02, "neutral height evaluated at center depth");
        }
        AABB conflict = new AABB(0.5, 0.2, 1, 1.5, 1, 2);
        Rotation corner = lookAt(eyes, new Vec3(1.49, 0.201, 1.99));
        Rotation goal = neutralPitch(eyes, conflict, corner, 4, 0.15); //codex (old code snippet) Rotation goal = centered(eyes, conflict, corner, 4, 0.15);
        check(goal.yaw() == corner.yaw() && Math.abs(goal.pitch()) <= Math.abs(corner.pitch()), "neutral pitch wins over hitbox centering"); //codex (old code snippet) check(Math.abs(goal.pitch()) > Math.abs(corner.pitch()), "centering wins over neutral angle");
        check(neutralPitch(eyes, conflict, new Rotation(90, 0), 4, 0.15) == null, "targetting never acquires an invalid hit"); //codex (old code snippet) Rotation constrained = constrain(new Rotation(goal.yaw(), corner.pitch()), goal, r -> hits(eyes, conflict, r, 4));
        AABB levelBox = new AABB(0.5, -1, 1, 1.5, 1, 2); //codex (old code snippet) check(constrained != null && constrained.yaw() == goal.yaw() && hits(eyes, conflict, constrained, 4), "simultaneous turn clamps pitch instead of losing yaw progress");
        Rotation offCenter = lookAt(eyes, new Vec3(1.49, -0.8, 1.99)); //codex (old code snippet) check(centered(eyes, conflict, new Rotation(90, 0), 4, 0.15) == null, "targetting never acquires an invalid hit");
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
        try {
            AimAssistScreenTodoAi.apply(cfg.targetting.requires, cfg.targetting.requires.getClass().getField("attackWindow"), "201"); //codex (old code snippet) AimAssistScreenTodoAi.apply(cfg.aura.requires, cfg.aura.requires.getClass().getField("attackWindow"), "201");
            throw new AssertionError("window must reject >200ms");
        } catch (IllegalArgumentException expected) {}
        // codex start
        var scaling = new AimAssistConfigTodoAi.Interpolation();
        for (String fieldName : new String[]{"horizontalMin", "horizontalMax", "verticalMin", "verticalMax", "directionMin", "directionMax"}) {
            try {
                AimAssistScreenTodoAi.apply(scaling, scaling.getClass().getField(fieldName), "250");
                throw new AssertionError("targeting editor must retain its limit: " + fieldName);
            } catch (IllegalArgumentException expected) {}
        }
        check(java.util.Arrays.stream(AimAssistConfigTodoAi.class.getFields()).noneMatch(f -> f.getName().equals("aura")),
                "removed module has no persisted config or settings entry");
        //codex end
        // codex start
        AABB motionBox = new AABB(-0.5, -0.5, 3, 0.5, 0.5, 4);
        Rotation still = new Rotation(0, 0);
        var insideProximity = AimCrosshairMotionTodoAi.sample(eyes, motionBox, still);
        var outsideProximity = AimCrosshairMotionTodoAi.sample(new Vec3(1, 0, 0), motionBox, still);
        var fartherProximity = AimCrosshairMotionTodoAi.sample(new Vec3(2, 0, 0), motionBox, still);
        check(!AimCrosshairMotionTodoAi.allowsAssist(insideProximity, outsideProximity), "walking out with static yaw blocks assistance");
        check(!AimCrosshairMotionTodoAi.allowsAssist(outsideProximity, fartherProximity), "walking farther away with static yaw blocks assistance");
        check(AimCrosshairMotionTodoAi.allowsAssist(fartherProximity, outsideProximity), "walking toward hitbox with static yaw allows assistance");
        check(AimCrosshairMotionTodoAi.allowsAssist(outsideProximity, insideProximity), "walking into hitbox allows assistance");
        check(AimCrosshairMotionTodoAi.allowsAssist(outsideProximity, outsideProximity), "stationary outside allows assistance");
        check(AimCrosshairMotionTodoAi.allowsAssist(insideProximity, insideProximity), "stationary inside allows neutral pitch assistance");
        var movingBox = motionBox.move(1, 0, 0);
        check(!AimCrosshairMotionTodoAi.allowsAssist(insideProximity, AimCrosshairMotionTodoAi.sample(eyes, movingBox, still)), "target walking away blocks assistance");
        var approach = AimCrosshairMotionTodoAi.sample(eyes, motionBox, new Rotation(-20, 0));
        var closer = AimCrosshairMotionTodoAi.sample(eyes, motionBox, new Rotation(-15, 0));
        check(AimCrosshairMotionTodoAi.allowsAssist(approach, closer), "crosshair turn toward edge assists");
        check(!AimCrosshairMotionTodoAi.allowsAssist(closer, approach), "crosshair turn away does not assist");
        check(!AimCrosshairMotionTodoAi.allowsAssist(approach, AimCrosshairMotionTodoAi.sample(eyes, motionBox, new Rotation(15, 0))), "crossing past hitbox does not pull back");
        check(AimCrosshairMotionTodoAi.allowsAssist(null, outsideProximity), "new target initializes history");
        // codex start
        for (Rotation leaving : new Rotation[]{new Rotation(1, 0), new Rotation(-1, 0), new Rotation(0, 1), new Rotation(0, -1)}) {
            var nearEdge = AimCrosshairMotionTodoAi.sample(eyes, motionBox, leaving);
            check(nearEdge.inside(), "exit starts with crosshair still inside");
            check(!AimCrosshairMotionTodoAi.allowsAssist(insideProximity, nearEdge), "moving inside toward edge never assists");
            Rotation outsideTurn = new Rotation(leaving.yaw() * 15, leaving.pitch() * 15);
            var afterExit = AimCrosshairMotionTodoAi.sample(eyes, motionBox, outsideTurn);
            check(!afterExit.inside() && !AimCrosshairMotionTodoAi.allowsAssist(nearEdge, afterExit), "crossing the edge never assists");
            check(!AimCrosshairMotionTodoAi.allowsAssist(afterExit,
                    AimCrosshairMotionTodoAi.sample(eyes, motionBox, new Rotation(leaving.yaw() * 20, leaving.pitch() * 20))), "continuing away never assists");
            check(AimCrosshairMotionTodoAi.allowsAssist(afterExit, afterExit), "stopping after exit restores stationary assistance");
        }
        var shiftedInside = AimCrosshairMotionTodoAi.sample(new Vec3(0.1, 0, 0), motionBox, still);
        check(shiftedInside.inside() && !AimCrosshairMotionTodoAi.allowsAssist(insideProximity, shiftedInside), "walking toward edge while still inside stays unassisted");
        var offCenterInside = AimCrosshairMotionTodoAi.sample(eyes, motionBox, new Rotation(2, 2));
        check(AimCrosshairMotionTodoAi.allowsAssist(offCenterInside, offCenterInside), "off-center stationary crosshair still assists");
        //codex end
        check(insideProximity.distance() == 0, "crosshair inside hitbox has zero distance");
        //codex end
        // codex start
        var gesture = new AimCrosshairMotionTodoAi.Gate();
        long time = 1_000_000_000L;
        check(gesture.update(insideProximity, insideProximity, time), "initial stationary held-click assist remains available");
        var movingInside = AimCrosshairMotionTodoAi.sample(eyes, motionBox, new Rotation(1, 0));
        check(!gesture.update(insideProximity, movingInside, time += 8_000_000), "outward gesture begins while inside the hitbox"); //codex (old code snippet) check(!gesture.update(insideProximity, movingInside, time += 8_000_000), "outward gesture begins before aura is eligible");
        for (int frame = 0; frame < 8; frame++)
            check(!gesture.update(movingInside, movingInside, time += 8_000_000), "render frames between mouse updates do not re-enable assist");
        check(!gesture.update(movingInside, outsideProximity, time += 8_000_000), "crossing the hitbox edge preserves outward block"); //codex (old code snippet) check(!gesture.update(movingInside, outsideProximity, time += 8_000_000), "targeting-to-aura transition preserves outward block");
        check(!gesture.update(outsideProximity, outsideProximity, time += 8_000_000), "first still outside frame does not pull back");
        check(!gesture.update(outsideProximity, fartherProximity, time += 8_000_000), "continued outward input refreshes suppression");
        check(!gesture.update(fartherProximity, fartherProximity, time + 149_000_000), "short mouse polling gap remains blocked");
        check(gesture.update(fartherProximity, fartherProximity, time + 150_000_000), "settled stationary crosshair resumes assistance");
        check(!gesture.update(outsideProximity, fartherProximity, time += 200_000_000), "next outward gesture blocks again");
        check(gesture.update(fartherProximity, outsideProximity, time + 1_000_000), "deliberate reversal toward target assists immediately");
        //codex end
        // codex start
        var reachConfig = new AimAssistConfigTodoAi();
        check(reachConfig.targetting.range == 4.2, "targeting range default preserved"); //codex (old code snippet) check(reachConfig.aura.range == 4.2 && reachConfig.targetting.range == 4.2, "existing range defaults preserved");
        AimAssistScreenTodoAi.apply(reachConfig.targetting, reachConfig.targetting.getClass().getField("range"), "2.5"); //codex (old code snippet) AimAssistScreenTodoAi.apply(reachConfig.aura, reachConfig.aura.getClass().getField("range"), "2.5");
        check(reachConfig.targetting.range == 2.5, "targeting reach editor saves value"); //codex (old code snippet) check(reachConfig.aura.range == 2.5 && reachConfig.targetting.range == 4.2, "independent per-module reach editor values");
        for (String invalid : new String[]{"-1", "NaN", "Infinity"}) {
            try {
                AimAssistScreenTodoAi.apply(reachConfig.targetting, reachConfig.targetting.getClass().getField("range"), invalid);
                throw new AssertionError("invalid reach accepted: " + invalid);
            } catch (IllegalArgumentException expected) {}
        }
        check(AimAssistReachTodoAi.range(Double.NaN) == 0 && AimAssistReachTodoAi.range(-1) == 0, "invalid persisted reach cannot contaminate attributes");
        //codex end
        // codex start
        AABB bandBox = new AABB(-0.5, -1, 3, 0.5, 1, 4);
        var bandInside = new Rotation(2, 3);
        var bandInsideGoal = AimCenterlineTodoAi.goal(eyes, bandBox, bandInside, 8, 0.15, 40);
        check(bandInsideGoal.yaw() == bandInside.yaw() && bandInsideGoal.pitch() == 0, "inside band preserves yaw and lifts head");
        Rotation leftGoal = AimCenterlineTodoAi.goal(eyes, bandBox, new Rotation(-8, 3), 8, 0.15, 40);
        Rotation rightGoal = AimCenterlineTodoAi.goal(eyes, bandBox, new Rotation(8, 3), 8, 0.15, 40);
        check(leftGoal.yaw() > -8 && leftGoal.yaw() < 0 && rightGoal.yaw() < 8 && rightGoal.yaw() > 0, "opposite sides converge on different band edges");
        check(hits(eyes, bandBox, leftGoal, 8) && hits(eyes, bandBox, rightGoal, 8), "combined horizontal and upward goals preserve hits");
        check(AimCenterlineTodoAi.goal(eyes, bandBox, new Rotation(8, -3), 8, 0.15, 40).pitch() == -3, "already looking up never moves down");
        check(Math.abs(AimCenterlineTodoAi.goal(eyes, bandBox, new Rotation(8, 0), 8, 0.15, 0).yaw()) < 1e-9, "zero width restores exact centerline");
        check(AimCenterlineTodoAi.goal(eyes, bandBox, new Rotation(8, 0), 8, 0.15, 100).yaw() == 8, "full width disables horizontal pull");
        check(Math.abs(AimCenterlineTodoAi.goal(eyes, bandBox, new Rotation(8, 0), 8, 0.15, 20).yaw()) < Math.abs(rightGoal.yaw()), "narrower setting pulls farther toward center");
        check(AimCenterlineTodoAi.goal(eyes, bandBox, new Rotation(30, 0), 8, 0.15, 40) == null, "targeting never acquires a missed hitbox");
        Rotation clampGoal = new Rotation(3, 0);
        check(AimCenterlineTodoAi.clamp(new Rotation(8, 3), new Rotation(-10, -10), clampGoal).equals(clampGoal), "interpolation cannot overshoot band or neutral");
        check(AimCenterlineTodoAi.clamp(new Rotation(8, -3), new Rotation(9, 10), new Rotation(3, -3)).pitch() == -3, "stale interpolation cannot move head down");
        check(reachConfig.targetting.centerlineWidth == 40, "configurable centerline default");
        AimAssistScreenTodoAi.apply(reachConfig.targetting, reachConfig.targetting.getClass().getField("centerlineWidth"), "65");
        check(reachConfig.targetting.centerlineWidth == 65, "centerline width editor saves setting");
        try {
            AimAssistScreenTodoAi.apply(reachConfig.targetting, reachConfig.targetting.getClass().getField("centerlineWidth"), "101");
            throw new AssertionError("centerline width must reject greater than 100");
        } catch (IllegalArgumentException expected) {}
        //codex end
        // codex start
        check(AimCorrectionFovTodoAi.allows(new Rotation(0, 0), new Rotation(5, 0), 5), "FOV boundary is allowed");
        check(!AimCorrectionFovTodoAi.allows(new Rotation(0, 0), new Rotation(5.1, 0), 5), "corrections above FOV are rejected");
        check(AimCorrectionFovTodoAi.allows(new Rotation(179, 0), new Rotation(-179, 0), 3), "FOV wraps yaw correctly");
        check(!AimCorrectionFovTodoAi.allows(new Rotation(0, 0), new Rotation(0, 31), 30), "vertical twitch is rejected");
        check(!AimCorrectionFovTodoAi.allows(new Rotation(0, 0), new Rotation(25, 25), 30), "combined angular correction is checked");
        check(AimCorrectionFovTodoAi.allows(new Rotation(0, 89), new Rotation(90, 89), 3), "FOV uses actual view angle near vertical");
        check(!AimCorrectionFovTodoAi.allows(new Rotation(0, 0), new Rotation(Double.NaN, 0), 30), "invalid rotations fail closed");
        check(!AimCorrectionFovTodoAi.allows(new Rotation(0, 0), new Rotation(1, 0), Double.NaN), "invalid FOV fails closed");
        check(AimCorrectionFovTodoAi.allows(new Rotation(0, 0), new Rotation(180, 0), 180), "180 degrees permits all finite turns");
        AimAssistScreenTodoAi.apply(reachConfig.targetting, reachConfig.targetting.getClass().getField("maxCorrectionFov"), "180"); //codex (old code snippet) AimAssistScreenTodoAi.apply(reachConfig.aura, reachConfig.aura.getClass().getField("maxCorrectionFov"), "180");
        check(reachConfig.targetting.maxCorrectionFov == 180, "targeting FOV editor saves value"); //codex (old code snippet) check(reachConfig.aura.maxCorrectionFov == 180 && reachConfig.targetting.maxCorrectionFov == 30, "independent module FOV settings");
        //codex end
        System.out.println("Aim assist geometry, interpolation, requirements and config checks passed.");
    }
}
