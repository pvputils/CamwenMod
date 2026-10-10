package com.example.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.example.Constants.*;
import static com.example.DelayedConstantsTodo.*;
import static com.example.UntitledClient.*;
import static com.example.Utils.getIsKeyBindingPressed;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Objects;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin {
    @Shadow
    public abstract boolean isUsingItem();

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        // TODO -> if (false) return; test this for starting sprint w/ s
        Screen currentScreen = MINECRAFT_CLIENT_INSTANCE.screen; //codex (old code snippet) Screen currentScreen = MINECRAFT_CLIENT_INSTANCE.gui.screen();
        // TODO -> wtf?
        boolean isCurrentHandledScreen = currentScreen instanceof AbstractContainerScreen<?>;
        boolean isMovementValid = currentScreen == null || isCurrentHandledScreen;
        SNEAK_VANILLA.setDown((getIsKeyBindingPressed(SNEAK_VANILLA) && isMovementValid) || toggleMovementState.shift());
        if (!getIsKeyBindingPressed(BACKWARD_VANILLA) && sprintResetBackwardsKeyState == SprintResetState.HELD) {
            sprintResetBackwardsKeyState = SprintResetState.INVALID;
        }
        if (!isCurrentHandledScreen) {
            SPRINT_VANILLA.setDown((getIsKeyBindingPressed(SPRINT_VANILLA) && isMovementValid) || toggleMovementState.sprint());
            JUMP_VANILLA.setDown((getIsKeyBindingPressed(JUMP_VANILLA) && isMovementValid) || (toggleMovementState.jump() && !this.isUsingItem())); // TODO -> config this?
            FORWARD_VANILLA.setDown((getIsKeyBindingPressed(FORWARD_VANILLA) && isMovementValid) || toggleMovementState.forward());
            LEFT_VANILLA.setDown((getIsKeyBindingPressed(LEFT_VANILLA) && isMovementValid) || toggleMovementState.left());
            RIGHT_VANILLA.setDown((getIsKeyBindingPressed(RIGHT_VANILLA) && isMovementValid) || toggleMovementState.right());
            BACKWARD_VANILLA.setDown((getIsKeyBindingPressed(BACKWARD_VANILLA) && isMovementValid) || toggleMovementState.backward());

            if (config.isBackwardSprintResetSuppressionEnabled) {
                // s tap
                if (FORWARD_VANILLA.isDown() && BACKWARD_VANILLA.isDown()) {
                    BACKWARD_VANILLA.setDown(false);
                    if (sprintResetBackwardsKeyState != SprintResetState.INVALID) {
                        SPRINT_VANILLA.setDown(false);
                        Objects.requireNonNull(MINECRAFT_CLIENT_INSTANCE.player).setSprinting(false);
                        if (sprintResetBackwardsKeyState == SprintResetState.VALID) {
                            sprintResetBackwardsKeyState = SprintResetState.HELD;
                        }
                    }
                }

                if (!SPRINT_VANILLA.isDown() &&
                        ((LocalPlayer) (Object)this).isSprinting() &&
                        sprintResetBackwardsKeyState != SprintResetState.INVALID) {
                    Objects.requireNonNull(MINECRAFT_CLIENT_INSTANCE.player).setSprinting(false);
                    sprintResetBackwardsKeyState = SprintResetState.INVALID;
                }
            }
        }
//        if (MINECRAFT_CLIENT_INSTANCE.player instanceof LocalPlayer player) {
//            if (config.isFlyBoostEnabled && player.isCreative()) {
//                Abilities abilities = player.getAbilities();
//                if (abilities.flying &&
//                        (SPRINT_VANILLA.isDown() || SPRINT_TOGGLE.isDown() || SPRINT_ENABLE.isDown())) {
//                    abilities.setFlyingSpeed(BASE_FLY_SPEED * FLY_BOOST_MULTIPLIER);
//                    {
//                        if (SNEAK_VANILLA.isDown() || SNEAK_TOGGLE.isDown() || SNEAK_ENABLE.isDown())
//                            player.setOnGroundWithMovement(player.onGround(), player.getKnownMovement().subtract(0, 0.15D * FLY_BOOST_MULTIPLIER, 0));
//                        if (JUMP_VANILLA.isDown())
//                            player.setOnGroundWithMovement(player.onGround(), player.getKnownMovement().add(0, 0.15D * FLY_BOOST_MULTIPLIER, 0));
//                    }
//                } else
//                    abilities.setFlyingSpeed(BASE_FLY_SPEED);
//            } else
//                player.getAbilities().setFlyingSpeed(BASE_FLY_SPEED);


//            if (computeCheatConfig().isAutoCobweb) {
//                onAutoCobwebTick(player);
//            }
//        }
    }

    @Inject(method = "handleConfusionTransitionEffect", at = @At("RETURN")) //codex (old code snippet) @Inject(method = "handlePortalTransitionEffect", at = @At("RETURN"))
    void onTickNausea(CallbackInfo ci) {
//        if (config.isDarknessDisabled) {
//            this.oPortalEffectIntensity = 0.f;
//            this.portalEffectIntensity = 0.f;
//        }
    }

//    @Inject(method = "updateHealth", at = @At("RETURN"))
//    void onUpdateHealth(float health, CallbackInfo ci) {
//    }

    @Unique
    boolean hasCurrentUseActionPlacedCobweb = false; // TODO ?

    @Unique
    void onAutoCobwebTick(LocalPlayer player) {
        if (!getIsKeyBindingPressed(USE_VANILLA)) {
            hasCurrentUseActionPlacedCobweb = false;
            return;
        }
        if (hasCurrentUseActionPlacedCobweb) {
            return;
        }
        var mainHandStack = player.getMainHandItem();
        if (!mainHandStack.is(Items.COBWEB)) {
            return;
        }
        BlockHitResult blockHitResult = (BlockHitResult) MINECRAFT_CLIENT_INSTANCE.hitResult;
        if (blockHitResult == null) {
            return;
        }
        if (blockHitResult.getType() == HitResult.Type.MISS) {
            return;
        }

        hasCurrentUseActionPlacedCobweb = true;
        Objects.requireNonNull(MINECRAFT_CLIENT_INSTANCE.gameMode).useItemOn(
                player, InteractionHand.MAIN_HAND, blockHitResult);
//        KeyBindingMixin keyBindingMixin = (KeyBindingMixin) USE_VANILLA;
//        keyBindingMixin.setTimesPressed(keyBindingMixin.getTimesPressed() + 1);
    }

}
