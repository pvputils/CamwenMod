package com.example.combat;

import com.example.Configs.Config;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;

import static com.example.UntitledClient.config;
import static com.example.Utils.getAbstractPvpUtilsKeybind;
import static com.example.Utils.getIsKeyBindingPressed;

public final class KillAuraHitTodoAi {
    private static final KeyMapping HIT_HOLD = getAbstractPvpUtilsKeybind("Kill aura hit (Hold)");

    private KillAuraHitTodoAi() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(KillAuraHitTodoAi::hitClosestTarget);
    }

    private static void hitClosestTarget(Minecraft client) {
        if (!config.isCheatsEnabled || !getIsKeyBindingPressed(HIT_HOLD) ||
                !(client.player instanceof LocalPlayer player) || client.level == null || client.gameMode == null ||
                player.getAttackStrengthScale(0.5F) < 1.0F) {
            return;
        }

        double reachSquared = Math.pow(player.entityInteractionRange(), 2);
        client.level.players().stream()
                .filter(target -> target != player && target.isAlive() && !target.isSpectator())
                .filter(target -> !isTeammate(target))
                .filter(target -> player.distanceToSqr(target) <= reachSquared)
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .ifPresent(target -> {
                    client.gameMode.attack(player, target);
                    player.swing(InteractionHand.MAIN_HAND);
                });
    }

    private static boolean isTeammate(Player target) {
        Config.NameplateTeam team = config.nameplateUuids.get(target.getUUID());
        return team == Config.NameplateTeam.FRIENDLY || team == Config.NameplateTeam.ALLY;
    }
}
