package com.example.mixins;

import com.example.aimassist.AimAssistControllerTodoAi;
import com.example.aimassist.AimGeometryTodoAi.Rotation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixinTodoAi {
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void captureMouseTurnTodoAi(LocalPlayer player, double yaw, double pitch) {
        Rotation before = new Rotation(player.getYRot(), player.getXRot());
        player.turn(yaw, pitch);
        AimAssistControllerTodoAi.recordMouseTurnTodoAi(before, new Rotation(player.getYRot(), player.getXRot()));
    }
}
