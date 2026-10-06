package com.example.aimassist;

import static com.example.aimassist.AimGeometryTodoAi.*;

/** Mouse-driven rotations are consumed once, separately from assisted rotations. */
public final class AimMouseMotionTodoAi {
    public record Motion(Rotation before, Rotation after) {}
    private Motion pending;

    public void record(Rotation before, Rotation after) {
        pending = error(before, after) > 1e-7 ? new Motion(before, after) : null;
    }

    public Motion consume() {
        Motion result = pending;
        pending = null;
        return result;
    }
}
