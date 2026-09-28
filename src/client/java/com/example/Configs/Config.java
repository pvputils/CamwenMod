package com.example.Configs;

import java.util.HashMap;
import java.util.UUID;
import net.minecraft.network.chat.TextColor;

import static com.example.Utils.serializeJsonBlocking;

public class Config {
    public enum NameplateTeam {
        FRIENDLY(TextColor.GREEN),
        ALLY(TextColor.AQUA);
        //        ENEMY,
//        FOCUS,
        public final TextColor color;

        NameplateTeam(TextColor color) {
            this.color = color;
        }
    }

    public HashMap<UUID, NameplateTeam> nameplateUuids = new HashMap<>();
    public boolean isToggleSneakGuiEnabled = false;
//    public boolean isSneakEnabled = false;
//    public boolean isSprintEnabled = false;
//    public boolean isFullbrightEnabled = false;
//    public String currentPotionEnchantmentGlintType = "";
    //    public boolean isSharpnessParticleReverted = false;
//    public boolean isCritParticleReverted = false;
//    public boolean isWeakAttackSoundDisabled = false;
    public boolean isDamageTakenValueNotificationEnabled = false;
//    public boolean isDepthStriderReverted = true; // TODO ?
//    public boolean isNameplateIronLeatherSwapped = true;
//    public boolean isMovementTogglePvpDisabling = false;
    public boolean isMovementToggleMirrorPressDisabling = false;
    public boolean isBackwardSprintResetSuppressionEnabled = false;
    public boolean isViewBobbingCameraShakeDisabled = false;
    // codex start
    public boolean isInventoryKeyHoldEnabled = false;
    // codex end
    // codex start
    public boolean isTeammateTargetCrosshairMarkerEnabled = false;
    // codex end
    // codex start
    public boolean isUnclampedPlayerWaypointsDisabled = false;
    // codex end
    // codex start
    public boolean isProjectileTrajectoryPreviewEnabled = false;
    // codex end
//    public boolean isGuiCheatsPvpDisabling = false;
    // TODO -> should have an option for it making a noise if a player shows up nearby
    public enum PlayerWaypointCategory {
        NONE,
        ALL,
        ENEMIES
    }
    public PlayerWaypointCategory playerWaypointCategory = PlayerWaypointCategory.ALL;
//    public boolean isPlayerLoginMessagingEnabled = false;
    public boolean isCheatsEnabled = true;

    public boolean isDebugModeEnabled = false;
    public boolean isReachDebugModeEnabled = false;
    public boolean isParkourCheatEnabled = false;

    // codex start
    /** Percentage chance (0-100) that an attack targeting a friendly teammate is suppressed. */
    public float teammateSwingSuppressionChance = 0.0F;
    // codex end

    public void saveConfig() {
        serializeJsonBlocking("config", this);
    }
}
