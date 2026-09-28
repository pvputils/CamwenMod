package com.example.mixins;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface AuraLivingAccessTodoAi {
    @Accessor("attackStrengthTicker")
    int auraAttackTicks();
    @Accessor("attackStrengthTicker")
    void auraSetAttackTicks(int ticks);
    @Accessor("autoSpinAttackDmg")
    float auraAutoSpinDamage();
}
