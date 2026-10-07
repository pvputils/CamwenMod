package com.example.killaura;

import com.google.gson.Gson;
import java.util.List;
import static com.example.killaura.KillAuraRotationTodoAi.*;

/** Deterministic math, settings tests. */
public final class KillAuraTestTodoAi {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void close(double actual, double expected, String message) { check(Math.abs(actual - expected) < 1E-6, message + ": " + actual); }
    public static void main(String[] args) throws Exception {
        var config = new KillAuraConfigTodoAi();
        var current = new Rotation(179, 0); var goal = new Rotation(-179, 0);
        close(linear(current, goal, 1, 1).yaw(), 180, "linear follows shortest wrapped path");
        check(linear(current, current, 180, 180).equals(current), "zero delta stays finite");
        close(linear(new Rotation(0, 0), new Rotation(90, 0), 20, 180).yaw(), 20, "linear speed cap");
        config.rotations.linear.horizontalMin = config.rotations.linear.horizontalMax = 10;
        check(estimateTicks(new Rotation(0, 0), new Rotation(90, 0), config.rotations) == 8, "OnTick uses configured slowest speed");
        config.rotations.linear.horizontalMin = config.rotations.linear.horizontalMax = 180;
        var smoother = new KillAuraRotationTodoAi();
        config.rotations.smoothing = "Interpolation";
        config.rotations.interpolation.horizontalMin = config.rotations.interpolation.horizontalMax = 80;
        config.rotations.interpolation.verticalMin = config.rotations.interpolation.verticalMax = 20;
        config.rotations.interpolation.directionMin = config.rotations.interpolation.directionMax = 0;
        var interpolated = smoother.step(new Rotation(0, 0), new Rotation(90, 0), config.rotations, false, 0);
        close(interpolated.yaw(), 0.7625 * 0.8 * 90, "upstream Bezier interpolation");
        close(quantize(new Rotation(0, 0), new Rotation(0.22, 0.22), 0.5).yaw(), 0.15, "mouse sensitivity quantization");
        var field = KillAuraConfigTodoAi.Rotations.class.getField("smoothing");
        try { KillAuraScreenTodoAi.apply(config.rotations, field, "AI"); throw new AssertionError("invalid mode accepted"); }
        catch (IllegalArgumentException expected) {}
        try { KillAuraScreenTodoAi.apply(config.target, KillAuraConfigTodoAi.Target.class.getField("fov"), "NaN"); throw new AssertionError("NaN accepted"); }
        catch (IllegalArgumentException expected) {}
        KillAuraScreenTodoAi.apply(config.target, KillAuraConfigTodoAi.Target.class.getField("priorities"), "Distance,Health");
        check(config.target.priorities.equals(List.of("Distance", "Health")), "priority order preserved");
        config.enabled = true;
        var gson = new Gson(); var roundTrip = gson.fromJson(gson.toJson(config), KillAuraConfigTodoAi.class);
        check(roundTrip.enabled && roundTrip.rotations.smoothing.equals("Interpolation") && roundTrip.target.priorities.equals(config.target.priorities), "native settings roundtrip");
        var legacy = gson.fromJson("{\"rotations\":{\"smoothing\":\"AI\",\"ai\":{\"model\":\"21KC11KP\"}}}", KillAuraConfigTodoAi.class);
        legacy.repair();
        check(legacy.rotations.smoothing.equals("Linear"), "removed AI setting repairs to Linear");
        check(!gson.toJson(legacy).contains("\"ai\""), "removed model settings are not persisted");
        System.out.println("PASS: KillAura native settings and four rotation smoothing modes");
    }
}
