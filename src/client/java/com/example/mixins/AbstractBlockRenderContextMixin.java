package com.example.mixins;

import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.example.UntitledClient.isPlayerXrayEnabled;

@Mixin(AbstractBlockRenderContext.class)
public class AbstractBlockRenderContextMixin {
    @Inject(method = "isFaceCulled", at = @At("HEAD"), cancellable = true)
    void onIsFaceCulled(Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (isPlayerXrayEnabled) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
    //codex (old code snippet) @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true) private void shouldDrawSide( Direction facing, CallbackInfoReturnable<Boolean> cir) { if (isPlayerXrayEnabled) { cir.setReturnValue(false); cir.cancel(); } }
}
