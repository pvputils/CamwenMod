package com.example.mixins;

import com.example.UntitledClient;
import com.example.Configs.Config;
import com.example.aimassist.AimAssistMarginScopeTodoAi;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Supplies a temporary pick radius only during aim-assist probes. */
@Mixin(ProjectileUtil.class)
public abstract class AimAssistPickRadiusMixinTodoAi {
    @WrapOperation(
        method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getPickRadius()F"))
    private static float aimAssistRadius(Entity entity, Operation<Float> original) {
        Float margin = AimAssistMarginScopeTodoAi.current();
        var config = UntitledClient.config;
        if (margin == null || !(entity instanceof Player player) || !config.isCheatsEnabled
                || (config.teammateSwingSuppressionChance > 0f
                    && (config.nameplateUuids.get(player.getUUID()) instanceof Config.NameplateTeam team && team.isFriendly))) { //codex (old code snippet) && config.nameplateUuids.get(player.getUUID()) instanceof Config.NameplateTeam)) {
            return original.call(entity);
        }
        return margin;
    }
}