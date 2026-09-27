package com.example.mixins;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.example.Constants.MINECRAFT_CLIENT_INSTANCE;
import static com.example.UntitledClient.config;
import static com.example.Utils.computeCheatConfig;

import net.minecraft.world.entity.Entity;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    public abstract double getX();

    @Shadow
    public abstract double getY();

    @Shadow
    public abstract double getZ();

    @Unique
    double lastX = 0;
    @Unique
    double lastY = 0;
    @Unique
    double lastZ = 0;
    @Unique
    boolean isLocallyMoving = false;
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        double x = getX();
        double y = getY();
        double z = getZ();
        isLocallyMoving = lastX != x || lastY != y || lastZ != z;
        lastX = x;
        lastY = y;
        lastZ = z;
    }
    @Inject(method = "getPickRadius", at = @At("HEAD"), cancellable = true)
    private void onGetTargetingMargin(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof Player && config.isCheatsEnabled) {
            boolean isMoving = MINECRAFT_CLIENT_INSTANCE.player.input.getMoveVector().lengthSquared() > 0.f;
            // TODO ?
//            boolean isTargetMoving = player.getDeltaMovement().horizontalDistanceSqr() > 0.0001f;
//            boolean foo = player == Constants.MINECRAFT_CLIENT_INSTANCE.player;
//            if (foo)
//                MINECRAFT_CLIENT_INSTANCE.player.sendSystemMessage(Component.literal(String.valueOf(foo)));
//            player.getX()
//            double bar = player.getDeltaMovement().horizontalDistanceSqr();
//            if (bar != 0) {
//                MINECRAFT_CLIENT_INSTANCE.player.sendSystemMessage(Component.literal(String.valueOf(bar)));
//                MINECRAFT_CLIENT_INSTANCE.player.sendSystemMessage(Component.literal(String.valueOf(MINECRAFT_CLIENT_INSTANCE.player.getDeltaMovement().horizontalDistanceSqr())));
//            }
//            if (isLocallyMoving) {
//                MINECRAFT_CLIENT_INSTANCE.player.sendSystemMessage(Component.literal("gey"));
//            }
            cir.setReturnValue(computeCheatConfig().computeTargetingMarginBypass());
        }
    }

//    @Inject(method = "onDamaged", at = @At("HEAD"))
//    void onOnDamaged(DamageSource damageSource, CallbackInfo ci) {
//        // doesn't run, not sure why it doesn't when the packet works totally fine but w/e
    // these methods are probably both overridden. it is: livingEntity overrides it
//    }

//    @Inject(method = "clientDamage", at = @At("HEAD"))
//    private void onClientDamage(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
//        //  -> this literally never runs?
//    }
}
