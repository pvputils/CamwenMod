package com.example.gametestmixins;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftAttackInvokerTodoAi {
    @Invoker("startAttack") boolean killAuraStartAttackTodoAi();
}
