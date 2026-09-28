package com.example.mixins;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface AuraGameModeAccessTodoAi {
    @Invoker("ensureHasSentCarriedItem")
    void auraSyncCarriedItem();
}
