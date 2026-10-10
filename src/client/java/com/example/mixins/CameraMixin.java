package com.example.mixins;

import com.example.UntitledClient;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.example.Constants.MINECRAFT_CLIENT_INSTANCE;
import static com.example.DelayedConstantsTodo.JUMP_VANILLA;
import static com.example.UntitledClient.headRunCameraOffset;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private @Nullable Entity entity;

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    // codex start TODO -> ?
    @Shadow
    public abstract float getYRot(); //codex (old code snippet) public abstract float yRot();

    @Shadow
    public abstract float getXRot(); //codex (old code snippet) public abstract float xRot();

    @Shadow
    private Vec3 position;
    // codex end

    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Inject(method = "setup", at = @At(value = "RETURN")) //codex (old code snippet) @Inject(method = "alignWithEntity", at = @At(value = "RETURN"))
    void onAlignWithEntity(CallbackInfo ci) {
        if (!(this.entity instanceof LocalPlayer player)) {
            return;
        }
        if (headRunCameraOffset == UntitledClient.HEAD_RUN_OFFSET_TYPE.NONE) { // TODO ?
            return;
        }
        float yawOffset = getHeadRunYawOffset();
        switch (MINECRAFT_CLIENT_INSTANCE.options.getCameraType()) {
            case FIRST_PERSON -> {
                this.setRotation(
                        player.getYRot() + yawOffset,
                        player.getXRot());
            }
            case THIRD_PERSON_BACK, THIRD_PERSON_FRONT -> {
            }
        }
    }

    // codex start
    @Inject(method = "setup", at = @At("TAIL")) //codex (old code snippet) @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void offsetThirdPersonOrbit(net.minecraft.world.level.BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) { //codex (old code snippet) private void offsetThirdPersonOrbit(float partialTick, CallbackInfo ci) {
        if (!(this.entity instanceof LocalPlayer)
                || headRunCameraOffset == UntitledClient.HEAD_RUN_OFFSET_TYPE.NONE
                || MINECRAFT_CLIENT_INSTANCE.options.getCameraType().isFirstPerson()) {
            return;
        }

        float yawOffset = getHeadRunYawOffset();
        // Vanilla has completed either orbit here, including the mirrored front-camera transform.
        this.setRotation(this.getYRot() + yawOffset, this.getXRot()); //codex (old code snippet) this.setRotation(this.yRot() + yawOffset, this.xRot());
        Vec3 eyePosition = ((LocalPlayer) this.entity).getEyePosition(partialTick);
        Vec3 rotatedOrbit = this.position.subtract(eyePosition)
                .yRot((float) Math.toRadians(-yawOffset));
        this.setPosition(eyePosition.add(rotatedOrbit));
    }

    @Unique
    private static float getHeadRunYawOffset() {
        float jumpModifier = JUMP_VANILLA.isDown()
                ? headRunCameraOffset == UntitledClient.HEAD_RUN_OFFSET_TYPE.LEFT ? 12.0F : -12.0F
                : 0.0F;
        return headRunCameraOffset.delta + jumpModifier;
    }
    // codex end
}
