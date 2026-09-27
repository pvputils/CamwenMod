package com.example.mixins;

import com.example.Configs.Config;
import com.example.overlayTodoAi.ExternalConfigWindow;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.UUID;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import static com.example.Constants.*;
import static com.example.DelayedConstantsTodo.*;
import static com.example.UntitledClient.*;
import static com.example.Utils.*;

@Mixin(value = KeyboardHandler.class)
public class KeyboardMixin {
    @Unique
    private static boolean isMovementToggleMirrorSequencePressed = false;
//    // codex start
//    @Unique
//    private static boolean shouldCloseInventoryOnKeyRelease = false;
//    // codex end

    // TODO -> there has to be a better place for handling this rather than checking all keyMappings
    // codex start
    @Inject(at = @At(value = "HEAD"), method = "keyPress", cancellable = true)
    private void suppressInventoryKeyRepeats(
            long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (config.isInventoryKeyHoldEnabled
                && action == GLFW.GLFW_REPEAT
                && MINECRAFT_CLIENT_INSTANCE.options.keyInventory.matches(event)) {
            ci.cancel();
        }
    }
    // codex end

    @Inject(at = @At(value = "RETURN"), method = "keyPress")
    private void onKeyPress(
            long handle, int action, KeyEvent event, CallbackInfo ci) {
        // codex start
        if (MINECRAFT_CLIENT_INSTANCE.options.keyInventory.matches(event)) {
//            if (action == GLFW.GLFW_PRESS && MINECRAFT_CLIENT_INSTANCE.player instanceof LocalPlayer) {
//                if (config.isInventoryKeyHoldEnabled) {
//                    // Vanilla has opened the player inventory by this RETURN injection point.
//                    shouldCloseInventoryOnKeyRelease = true;
//                }
//            } else if (action == GLFW.GLFW_RELEASE && shouldCloseInventoryOnKeyRelease) {
//                MINECRAFT_CLIENT_INSTANCE.setScreenAndShow(null);
//                shouldCloseInventoryOnKeyRelease = false;
//            }
            if (config.isInventoryKeyHoldEnabled && action == GLFW.GLFW_RELEASE && MINECRAFT_CLIENT_INSTANCE.gui.screen() instanceof InventoryScreen) {
                MINECRAFT_CLIENT_INSTANCE.setScreenAndShow(null);
            }
        }
        // codex end
        if (config.isMovementToggleMirrorPressDisabling) {
            if (!(getIsKeyBindingPressed(SNEAK_VANILLA) == toggleMovementState.shift()
                    && getIsKeyBindingPressed(SPRINT_VANILLA) == toggleMovementState.sprint()
                    && getIsKeyBindingPressed(JUMP_VANILLA) == toggleMovementState.jump()
                    && getIsKeyBindingPressed(FORWARD_VANILLA) == toggleMovementState.forward()
                    && getIsKeyBindingPressed(LEFT_VANILLA) == toggleMovementState.left()
                    && getIsKeyBindingPressed(RIGHT_VANILLA) == toggleMovementState.right()
                    && getIsKeyBindingPressed(BACKWARD_VANILLA) == toggleMovementState.backward())) {
                isMovementToggleMirrorSequencePressed = false;
            } else if (!isMovementToggleMirrorSequencePressed) {
                toggleMovementState = new Input(false, false, false, false, false, false, false);
            }
        }
        // TODO -> I should probably just use if if possible
//        while (MOVEMENT_TOGGLE.consumeClick()) {
//            if (isJumpEnabled
//                    || isForwardEnabled
//                    || isLeftEnabled
//                    || isRightEnabled
//                    || isBackwardEnabled)
//                doMovementToggleDisable();
//            else
//                doMovementToggleEnable();
//        }
        while (MOVEMENT_ENABLE.consumeClick()) {
            if (MINECRAFT_CLIENT_INSTANCE.player instanceof LocalPlayer) {
                toggleMovementState = new Input(
                        getIsKeyBindingPressed(FORWARD_VANILLA),
                        getIsKeyBindingPressed(BACKWARD_VANILLA),
                        getIsKeyBindingPressed(LEFT_VANILLA),
                        getIsKeyBindingPressed(RIGHT_VANILLA),
                        getIsKeyBindingPressed(JUMP_VANILLA),
                        getIsKeyBindingPressed(SNEAK_VANILLA),
                        getIsKeyBindingPressed(SPRINT_VANILLA));
                isMovementToggleMirrorSequencePressed = true;
            }
        }
//        while (MOVEMENT_DISABLE.consumeClick())
//            doMovementToggleDisable();


//        if (getIsKeyBindingPressed(SNEAK_TOGGLE)) {
//            if (!isSneakToggleButtonPressed)
//                config.isSneakEnabled = !config.isSneakEnabled;
//            while (SNEAK_TOGGLE.consumeClick()) {
//            }
//        } else
//            isSneakToggleButtonPressed = false;
//        if (getIsKeyBindingPressed(SNEAK_ENABLE)) { // TODO these could benefit from the handling above too, but they aren't toggled so w/e
//            config.isSneakEnabled = true;
//            while (SNEAK_ENABLE.consumeClick()) {
//            }
//        }
//        if (getIsKeyBindingPressed(SNEAK_DISABLE)) {
//            config.isSneakEnabled = false;
//            while (SNEAK_DISABLE.consumeClick()) {
//            }
//        }

//        while (SPRINT_TOGGLE.consumeClick())
//            config.isSprintEnabled = !config.isSprintEnabled;
//        while (SPRINT_ENABLE.consumeClick())
//            config.isSprintEnabled = true;
//        while (SPRINT_DISABLE.consumeClick())
//            config.isSprintEnabled = false;
//
//        if (getIsKeyBindingPressed(FULLBRIGHT_TOGGLE)) {
//            if (!isFullbrightToggleButtonPressed)
//                config.isFullbrightEnabled = !config.isFullbrightEnabled;
//            while (FULLBRIGHT_TOGGLE.consumeClick()) {
//            }
//        } else
//            isFullbrightToggleButtonPressed = false;
//        if (getIsKeyBindingPressed(FULLBRIGHT_ENABLE)) { // TODO: see -> sneak handling meme
//            config.isFullbrightEnabled = true;
//            while (FULLBRIGHT_ENABLE.consumeClick()) {
//            }
//        }
//        if (getIsKeyBindingPressed(FULLBRIGHT_DISABLE)) {
//            config.isFullbrightEnabled = false;
//            while (FULLBRIGHT_DISABLE.consumeClick()) {
//            }
//        }

        while (FRIENDLY_TOGGLE.consumeClick()) {
            onAbstractNameplateToggle(Config.NameplateTeam.FRIENDLY);
        }
        while (ALLY_TOGGLE.consumeClick()) {
            onAbstractNameplateToggle(Config.NameplateTeam.ALLY);
        }

        while (KEYBIND_CONFIG.consumeClick()) {
            // codex start
//            MINECRAFT_CLIENT_INSTANCE.setScreenAndShow(buildConfig());
//            ExternalConfigWindow.show();
            ExternalConfigWindow.toggle();
            // codex end
        }

//        while (PLAYER_WAYPOINTS_TOGGLE.consumeClick()) {
//            config.isPlayerWaypointsEnabled = !config.isPlayerWaypointsEnabled;
//        }
        while (PLAYER_WAYPOINTS_CYCLE.consumeClick()) {
            Config.PlayerWaypointCategory[] values = Config.PlayerWaypointCategory.values();
            config.playerWaypointCategory = values[(config.playerWaypointCategory.ordinal() + 1) % values.length];
        }
        while (PLAYER_WAYPOINTS_DISABLE.consumeClick()) {
            config.playerWaypointCategory = Config.PlayerWaypointCategory.NONE;
        }

//        while (BLOCK_XRAY_TOGGLE.consumeClick()) {
//            onXrayChange(Objects.equals(currentXrayType, "block") ? "" : "block");
//        }
        while (PLAYER_XRAY_TOGGLE.consumeClick()) {
            onXrayChange();
        }

        while (HEAD_RUN_CAMERA_OFFSET_ENABLE.consumeClick()) {
            if (!(MINECRAFT_CLIENT_INSTANCE.player instanceof LocalPlayer player)) {
                continue;
            }
            if (!player.input.keyPresses.forward()) {
                continue;
            }
            boolean left = player.input.keyPresses.left();
            if (left == player.input.keyPresses.right()) {
                continue;
            }
            headRunCameraOffset = left ? HEAD_RUN_OFFSET_TYPE.LEFT : HEAD_RUN_OFFSET_TYPE.RIGHT;
        }

        if (headRunCameraOffset == HEAD_RUN_OFFSET_TYPE.LEFT && !LEFT_VANILLA.isDown()) {
            headRunCameraOffset = HEAD_RUN_OFFSET_TYPE.NONE;
        }
        if (headRunCameraOffset == HEAD_RUN_OFFSET_TYPE.RIGHT && !RIGHT_VANILLA.isDown()) {
            headRunCameraOffset = HEAD_RUN_OFFSET_TYPE.NONE;
        }

//        while (DECREMENT_CHEATS.consumeClick()) {
//            if (rageCheatLevel.ordinal() == 0) {
//                continue;
//            }
//            rageCheatLevel = RAGE_CHEAT_LEVEL.values()[rageCheatLevel.ordinal() - 1];
////            computeCheatConfig().targetingMarginBypass = rageCheatLevel.TargetingMarginBypass;
//        }
//        while (INCREMENT_CHEATS.consumeClick()) {
//            if (rageCheatLevel.ordinal() == RAGE_CHEAT_LEVEL.values().length - 1) {
//                continue;
//            }
//            rageCheatLevel = RAGE_CHEAT_LEVEL.values()[rageCheatLevel.ordinal() + 1];
////            computeCheatConfig().targetingMarginBypass = rageCheatLevel.TargetingMarginBypass; // TODO -> method-ize
//        }
    }

    @Unique
    private Player ComputePlayerRaytrace() {
        final double REACH = 50.f;
        float tickDelta = MINECRAFT_CLIENT_INSTANCE.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        LocalPlayer player = Objects.requireNonNull(MINECRAFT_CLIENT_INSTANCE.player);
        Vec3 cameraPos = player.getEyePosition(tickDelta);
        Vec3 rotationVec = player.getViewVector(tickDelta);
        Vec3 endPos = cameraPos.add(rotationVec.scale(REACH));

        AABB searchBox = player.getBoundingBox()
                .expandTowards(rotationVec.scale(REACH))
                .inflate(.3D);

        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
                player,
                cameraPos,
                endPos,
                searchBox,
                entity -> !entity.isSpectator()
                        && entity.isPickable()
                        && entity instanceof Player,
                REACH * REACH
        );

        if (hitResult != null && hitResult.getEntity() instanceof Player targetPlayer) {
            return targetPlayer;
        }

        return null;
    }

    @Unique
    void onAbstractNameplateToggle(Config.NameplateTeam team) {
        if (!(ComputePlayerRaytrace() instanceof Player playerEntity)) {
            return;
        }

        UUID playerUuid = playerEntity.getUUID();
        if (config.nameplateUuids.get(playerUuid) == team) {
            config.nameplateUuids.remove(playerUuid);
        } else {
            config.nameplateUuids.put(playerUuid, team);
        }
        config.saveConfig();
    }
}
