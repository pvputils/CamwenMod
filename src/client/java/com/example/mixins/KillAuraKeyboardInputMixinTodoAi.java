/* Rotation application adapted from LiquidBounce, copyright CCBlueX 2015-2026.
 * GPL-3.0-or-later; see assets/untitled/KillAuraLicenseTodoAi.txt. */
package com.example.mixins;
import com.example.killaura.*;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(KeyboardInput.class)
public abstract class KillAuraKeyboardInputMixinTodoAi {
    @ModifyExpressionValue(method="tick",at=@At(value="NEW",target="(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;"))
    private Input transform(Input original) {
        var mc=Minecraft.getInstance();
        return mc.player == null ? original : KillAuraRotationModesTodoAi.transform(original,mc.player.getYRot(),
                KillAuraControllerTodoAi.managedRotation(mc),KillAuraControllerTodoAi.config().rotations.movementCorrection);
    }
}
