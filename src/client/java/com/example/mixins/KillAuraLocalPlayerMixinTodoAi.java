/* Rotation application adapted from LiquidBounce, copyright CCBlueX 2015-2026.
 * GPL-3.0-or-later; see assets/untitled/KillAuraLicenseTodoAi.txt. */
package com.example.mixins;
import com.example.killaura.KillAuraControllerTodoAi;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(LocalPlayer.class)
public abstract class KillAuraLocalPlayerMixinTodoAi {
    @ModifyExpressionValue(method={"sendPosition","tick"}, at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private float rotationYaw(float original) {
        var rotation=KillAuraControllerTodoAi.managedRotation(Minecraft.getInstance());
        return rotation == null ? original : (float)rotation.yaw();
    }
    @ModifyExpressionValue(method={"sendPosition","tick"}, at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float rotationPitch(float original) {
        var rotation=KillAuraControllerTodoAi.managedRotation(Minecraft.getInstance());
        return rotation == null ? original : (float)rotation.pitch();
    }
}
