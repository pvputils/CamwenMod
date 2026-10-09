/* Rotation application adapted from LiquidBounce, copyright CCBlueX 2015-2026.
 * GPL-3.0-or-later; see assets/untitled/KillAuraLicenseTodoAi.txt. */
package com.example.killaura;
import com.example.aimassist.AimGeometryTodoAi.Rotation;
import net.minecraft.world.entity.player.Input;
import net.minecraft.util.Mth;
import static com.example.killaura.KillAuraConfigTodoAi.MovementCorrection;

/** Native equivalents of upstream resolveMovementYaw and keyboard transformDirection. */
public final class KillAuraRotationModesTodoAi {
    private KillAuraRotationModesTodoAi() {}
    public static float movementYaw(float playerYaw, Rotation managed, MovementCorrection mode) {
        return managed != null && mode != MovementCorrection.OFF ? (float)managed.yaw() : playerYaw;
    }
    public static Input transform(Input original, float playerYaw, Rotation managed, MovementCorrection mode) {
        if (managed == null || mode != MovementCorrection.SILENT) return original;
        float z = original.forward() == original.backward() ? 0 : original.forward() ? 1 : -1;
        float x = original.left() == original.right() ? 0 : original.left() ? 1 : -1;
        float deltaYaw = (playerYaw - (float)managed.yaw()) * Mth.DEG_TO_RAD;
        int sideways = Math.round(x * Mth.cos(deltaYaw) - z * Mth.sin(deltaYaw));
        int forward = Math.round(z * Mth.cos(deltaYaw) + x * Mth.sin(deltaYaw));
        return new Input(forward > 0, forward < 0, sideways > 0, sideways < 0,
                original.jump(), original.shift(), original.sprint());
    }
}
