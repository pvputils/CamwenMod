package com.example;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** Camera values captured together from the 1.21.4 world render pass. */
public final class CameraSnapshotTodoAi {
    public final Vec3 pos;
    public final Quaternionf orientation;
    public final Matrix4f projectionMatrix;
    public CameraSnapshotTodoAi(Camera camera, Matrix4f projection) {
        pos = camera.getPosition();
        orientation = new Quaternionf(camera.rotation());
        projectionMatrix = new Matrix4f(projection);
    }
}
