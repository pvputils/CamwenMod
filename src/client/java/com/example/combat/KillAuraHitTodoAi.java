package com.example.combat;

import com.example.Configs.Config;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import static com.example.UntitledClient.config;
import static com.example.Utils.getAbstractPvpUtilsKeybind;
import static com.example.Utils.getIsKeyBindingPressed;

public final class KillAuraHitTodoAi {
    private static final KeyMapping HIT_HOLD = getAbstractPvpUtilsKeybind("Kill aura hit (Hold)");

    private KillAuraHitTodoAi() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(KillAuraHitTodoAi::hitConfiguredTarget);
    }

    private static void hitConfiguredTarget(Minecraft client) {
        if (!config.isCheatsEnabled || !getIsKeyBindingPressed(HIT_HOLD) ||
                !(client.player instanceof LocalPlayer player) || client.level == null || client.gameMode == null ||
                player.getAttackStrengthScale(0.5F) < 1.0F) {
            return;
        }

        String targetName = config.killAuraTargetName.trim();
        if (targetName.isEmpty()) {
            return;
        }

        double reachSquared = Math.pow(player.entityInteractionRange(), 2);
        client.level.players().stream()
                .filter(target -> target.getName().getString().equalsIgnoreCase(targetName))
                .filter(target -> isValidTarget(player, target, reachSquared))
                .filter(target -> !isTeammate(target))
                .findFirst()
                .ifPresent(target -> {
                    client.gameMode.attack(player, target);
                    player.swing(InteractionHand.MAIN_HAND);
                });
    }

    private static boolean isValidTarget(LocalPlayer player, Player target, double reachSquared) {
        return target != player && target.isAlive() && !target.isSpectator() &&
                player.hasLineOfSight(target) &&
                target.getBoundingBox().distanceToSqr(player.getEyePosition()) <= reachSquared;
    }

    private static boolean isTeammate(Player target) {
        Config.NameplateTeam team = config.nameplateUuids.get(target.getUUID());
        return team == Config.NameplateTeam.FRIENDLY || team == Config.NameplateTeam.ALLY;
    }
}
