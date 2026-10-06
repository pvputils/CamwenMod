package com.example.aimassist;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import static com.example.aimassist.AimGeometryTodoAi.*;

/** Screen-space distance to the hitbox, including camera and target translation. */
public final class AimCrosshairMotionTodoAi {
    public record Observation(Vec3 eyes, AABB box, Rotation crosshair) {
        public Proximity proximity() { return sample(eyes, box, crosshair); }
    }
    public record Proximity(double yaw, double pitch, boolean inside) { //codex (old code snippet) public record Proximity(double yaw, double pitch) {
        public double distance() { return inside ? 0 : Math.hypot(yaw, pitch); } //codex (old code snippet) public double distance() { return Math.hypot(yaw, pitch); }
    }
    public static Proximity sample(Vec3 eyes, AABB box, Rotation crosshair) {
        double rayLength = eyes.distanceTo(box.getCenter()) + box.getXsize() + box.getYsize() + box.getZsize() + 1;
        // codex start
        if (hits(eyes, box, crosshair, rayLength)) { //codex (old code snippet) if (hits(eyes, box, crosshair, rayLength)) return new Proximity(0, 0);
            Rotation center = lookAt(eyes, box.getCenter());
            return new Proximity(wrap(center.yaw() - crosshair.yaw()), center.pitch() - crosshair.pitch(), true);
        }
        //codex end
        Rotation edge = lookAt(eyes, nearestPoint(eyes, box, crosshair, rayLength, 0));
        return new Proximity(wrap(edge.yaw() - crosshair.yaw()), edge.pitch() - crosshair.pitch(), false); //codex (old code snippet) return new Proximity(wrap(edge.yaw() - crosshair.yaw()), edge.pitch() - crosshair.pitch());
    }
    public static boolean allowsAssist(Proximity previous, Proximity current) {
        if (previous == null) return true;
        // codex start
        if (previous.inside()) {
            return current.inside() && Math.hypot(wrap(current.yaw() - previous.yaw()),
                    current.pitch() - previous.pitch()) <= 1e-7;
        }
        if (current.inside()) return true;
        //codex end
        // Comparing after-assist history with before-assist input excludes the assist's own turn.
        return current.distance() <= previous.distance() + 1e-7 &&
                previous.yaw() * current.yaw() + previous.pitch() * current.pitch() >= -1e-7;
    }
}
