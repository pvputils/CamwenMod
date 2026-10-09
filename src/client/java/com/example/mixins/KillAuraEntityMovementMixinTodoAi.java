/* Rotation application adapted from LiquidBounce, copyright CCBlueX 2015-2026.
 * GPL-3.0-or-later; see assets/untitled/KillAuraLicenseTodoAi.txt. */
package com.example.mixins;
import com.example.killaura.*;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(Entity.class)
public abstract class KillAuraEntityMovementMixinTodoAi {
    @ModifyExpressionValue(method="moveRelative",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;getYRot()F"))
    private float movementYaw(float original) {
        var mc=Minecraft.getInstance();
        if((Object)this != mc.player)return original;
        return KillAuraRotationModesTodoAi.movementYaw(original,KillAuraControllerTodoAi.managedRotation(mc),
                KillAuraControllerTodoAi.config().rotations.movementCorrection);
    }
}
