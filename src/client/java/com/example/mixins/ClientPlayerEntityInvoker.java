package com.example.mixins;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class) //codex (old code snippet) @Mixin(LocalPlayer.class)
public interface ClientPlayerEntityInvoker {
    //codex (old code snippet) // codex start @Invoker("pick") static HitResult aimAssistPickTodoAi(Entity camera, double blockRange, double entityRange, float partialTick) { throw new UnsupportedOperationException("Mixin invoker not transformed"); } //codex end
    @Invoker("pick")
    HitResult invokePick(
            Entity camera,
            double blockInteractionRange,
            double entityInteractionRange,
            float tickDelta);
}
