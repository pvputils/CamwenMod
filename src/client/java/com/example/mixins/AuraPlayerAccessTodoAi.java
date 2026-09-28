package com.example.mixins;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Player.class)
public interface AuraPlayerAccessTodoAi {
    @Invoker("getEnchantedDamage")
    float auraEnchantedDamage(Entity target, float damage, DamageSource source);
}
