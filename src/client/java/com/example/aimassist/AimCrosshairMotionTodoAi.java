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
    // codex start
    /** Holds an outward gesture across render frames between input updates. */
    public static final class Gate {
        private static final long STILL_NANOS = 150_000_000L;
        private boolean exiting, allowed = true;
        private long lastOutward;
        public boolean update(Proximity previous, Proximity current, long now) {
            if (!allowsAssist(previous, current)) {
                exiting = true;
                lastOutward = now;
                return allowed = false;
            }
            boolean steady = previous != null && previous.inside() == current.inside() &&
                    Math.hypot(wrap(current.yaw() - previous.yaw()), current.pitch() - previous.pitch()) <= 1e-7;
            if (exiting && steady && now - lastOutward < STILL_NANOS) return allowed = false;
            exiting = false;
            return allowed = true;
        }
        public boolean allowed() { return allowed; }
    }
    //codex end
    public static boolean allowsAssist(Proximity previous, Proximity current) {
        if (previous == null) return true;
        // codex start
        if (previous.inside()) {
            // codex start
            double previousOffset = Math.hypot(previous.yaw(), previous.pitch());
            double currentOffset = Math.hypot(current.yaw(), current.pitch());
            return current.inside() && currentOffset <= previousOffset + 1e-7 && //codex (old code snippet) return current.inside() && Math.hypot(wrap(current.yaw() - previous.yaw()),
                    previous.yaw() * current.yaw() + previous.pitch() * current.pitch() >= -1e-7; //codex (old code snippet) current.pitch() - previous.pitch()) <= 1e-7;
            //codex end
        }
        if (current.inside()) return true;
        //codex end
        // Comparing after-assist history with before-assist input excludes the assist's own turn.
        return current.distance() <= previous.distance() + 1e-7 &&
                previous.yaw() * current.yaw() + previous.pitch() * current.pitch() >= -1e-7;
    }
}
