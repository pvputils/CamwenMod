package com.example.aimassist;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import static com.example.aimassist.AimGeometryTodoAi.*;

/** Pulls to a configurable horizontal center band, with upward-only neutral pitch. */
public final class AimCenterlineTodoAi {
    public static Rotation goal(Vec3 eyes, AABB box, Rotation current, double range, double step, double widthPercent) {
        if (!hits(eyes, box, current, range) || box.contains(eyes)) return null;
        double centerYaw = lookAt(eyes, box.getCenter()).yaw();
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (double x : new double[]{box.minX, box.maxX}) for (double z : new double[]{box.minZ, box.maxZ}) {
            double offset = wrap(lookAt(eyes, new Vec3(x, box.getCenter().y, z)).yaw() - centerYaw);
            min = Math.min(min, offset);
            max = Math.max(max, offset);
        }
        double width = Double.isFinite(widthPercent) ? Math.clamp(widthPercent, 0, 100) / 100 : 0.4;
        double middle = (min + max) / 2, half = (max - min) * width / 2;
        double offset = wrap(current.yaw() - centerYaw);
        double delta = Math.clamp(offset, middle - half, middle + half) - offset;
        Rotation horizontal = new Rotation(current.yaw() + delta, current.pitch());
        if (!hits(eyes, box, horizontal, range)) {
            double low = 0, high = 1;
            for (int i = 0; i < 40; i++) {
                double fraction = (low + high) / 2;
                if (hits(eyes, box, current.toward(horizontal, fraction), range)) low = fraction;
                else high = fraction;
            }
            horizontal = current.toward(horizontal, low);
        }
        return neutralPitch(eyes, box, horizontal, range, step);
    }
    /** Interpolation cannot overshoot the band or move the head downward. */
    public static Rotation clamp(Rotation current, Rotation proposed, Rotation goal) {
        double delta = wrap(goal.yaw() - current.yaw());
        double yaw = Math.clamp(wrap(proposed.yaw() - current.yaw()), Math.min(0, delta), Math.max(0, delta));
        double pitch = Math.clamp(proposed.pitch(), Math.min(current.pitch(), goal.pitch()), current.pitch());
        return new Rotation(current.yaw() + yaw, pitch);
    }
    private AimCenterlineTodoAi() {}
}
