package com.example.killaura;

import com.google.gson.Gson;
import java.io.*;
import java.util.List;
import static com.example.killaura.KillAuraRotationTodoAi.*;

/** Deterministic math, actual bundled inference, corrupt-model rejection, and settings tests. */
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
        var interpolated = smoother.step(new Rotation(0, 0), new Rotation(90, 0), config.rotations, false, 0, null);
        close(interpolated.yaw(), 0.7625 * 0.8 * 90, "upstream Bezier interpolation");
        config.rotations.smoothing = "AI";
        smoother.reset();
        close(smoother.step(new Rotation(0, 0), new Rotation(90, 0), config.rotations, false, 0, null).yaw(), interpolated.yaw(), "missing AI uses interpolation");
        config.rotations.ai.correction = "None";
        close(smoother.step(new Rotation(0, 0), new Rotation(90, 0), config.rotations, false, 0, new float[]{2, 3}).yaw(), 3, "real model output is scaled");
        close(quantize(new Rotation(0, 0), new Rotation(0.22, 0.22), 0.5).yaw(), 0.15, "mouse sensitivity quantization");
        for (String name : List.of("19kc8kp", "21kc11kp")) {
            try (InputStream input = KillAuraModelTodoAi.class.getResourceAsStream("/assets/camwenmod/models/" + name + "TodoAi.params")) {
                var model = KillAuraModelTodoAi.load(input);
                float[] prediction = model.predict(new float[]{30, -10, 2, 1, 0.2f, 4});
                check(prediction.length == 2 && Float.isFinite(prediction[0]) && Float.isFinite(prediction[1]), "real " + name + " model predicts");
                check(Math.abs(prediction[0]) + Math.abs(prediction[1]) > 0.001, "model output is not a placeholder");
                // Reference values independently computed with NumPy matrix multiplication.
                double[] reference = name.equals("19kc8kp") ? new double[]{4.3705291748, 1.2414865494} : new double[]{6.2316946983, 0.6565242410};
                check(Math.abs(prediction[0] - reference[0]) < 1E-5 && Math.abs(prediction[1] - reference[1]) < 1E-5, "inference matches independent numerical reference");
                System.out.println(name + " actual prediction: " + prediction[0] + ", " + prediction[1]);
            }
        }
        try { KillAuraModelTodoAi.load(new ByteArrayInputStream(new byte[]{1, 2, 3})); throw new AssertionError("invalid model accepted"); }
        catch (IOException expected) {}
        var field = KillAuraConfigTodoAi.Rotations.class.getField("smoothing");
        try { KillAuraScreenTodoAi.apply(config.rotations, field, "PretendAI"); throw new AssertionError("invalid mode accepted"); }
        catch (IllegalArgumentException expected) {}
        try { KillAuraScreenTodoAi.apply(config.target, KillAuraConfigTodoAi.Target.class.getField("fov"), "NaN"); throw new AssertionError("NaN accepted"); }
        catch (IllegalArgumentException expected) {}
        KillAuraScreenTodoAi.apply(config.target, KillAuraConfigTodoAi.Target.class.getField("priorities"), "Distance,Health");
        check(config.target.priorities.equals(List.of("Distance", "Health")), "priority order preserved");
        config.enabled = true;
        var gson = new Gson(); var roundTrip = gson.fromJson(gson.toJson(config), KillAuraConfigTodoAi.class);
        check(roundTrip.enabled && roundTrip.rotations.smoothing.equals("AI") && roundTrip.target.priorities.equals(config.target.priorities), "native settings roundtrip");
        System.out.println("PASS: KillAura native settings, rotation math and both bundled AI models");
    }
}
