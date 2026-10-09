/* Rotation application adapted from LiquidBounce, copyright CCBlueX 2015-2026.
 * GPL-3.0-or-later; see assets/untitled/KillAuraLicenseTodoAi.txt. */
package com.example.mixins;
import com.example.killaura.KillAuraControllerTodoAi;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(LivingEntity.class)
public abstract class KillAuraLivingMovementMixinTodoAi {
    @ModifyExpressionValue(method="jumpFromGround",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float jumpYaw(float original) {
        var mc=Minecraft.getInstance();
        var rotation=(Object)this == mc.player ? KillAuraControllerTodoAi.movementRotation(mc) : null;
        return rotation == null ? original : (float)rotation.yaw();
    }
    @ModifyExpressionValue(method="updateFallFlyingMovement",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;getXRot()F"))
    private float glidingPitch(float original) {
        var mc=Minecraft.getInstance();
        var rotation=(Object)this == mc.player ? KillAuraControllerTodoAi.movementRotation(mc) : null;
        return rotation == null ? original : (float)rotation.pitch();
    }
    @ModifyExpressionValue(method="updateFallFlyingMovement",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 glidingDirection(Vec3 original) {
        var mc=Minecraft.getInstance();
        var rotation=(Object)this == mc.player ? KillAuraControllerTodoAi.movementRotation(mc) : null;
        return rotation == null ? original : rotation.direction();
    }
}
