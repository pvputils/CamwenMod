package com.example.gametestmixins;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.concurrent.atomic.AtomicInteger;

/** Test-only count of actual vanilla attack calls, independent of the KillAura implementation. */
@Mixin(MultiPlayerGameMode.class)
public class KillAuraAttackCounterTodoAi {
    @Inject(method = "attack", at = @At("HEAD"))
    private void countTodoAi(Player player, Entity entity, CallbackInfo ci) { com.example.killaura.KillAuraGameTestTodoAi.attacks.incrementAndGet(); }
}
