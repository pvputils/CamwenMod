package com.example;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * Records the server-confirmed health loss from a player hit so players can compare equivalent PvP situations.
 */
public final class DamageHealthEstimatorTodoAi {
    private static PendingHit pendingHit;

    private DamageHealthEstimatorTodoAi() {
    }

    public static void capture(Player attacker, float healthBefore, int armorPoints) {
        pendingHit = new PendingHit(
                BuiltInRegistries.ITEM.getKey(attacker.getMainHandItem().getItem()).toString(),
                healthBefore,
                armorPoints);
    }

    public static Optional<String> recordHealth(float healthAfter) {
        if (pendingHit == null || healthAfter >= pendingHit.healthBefore()) {
            return Optional.empty();
        }

        float damage = pendingHit.healthBefore() - healthAfter;
        String message = String.format(
                "Damage calibration: %s vs %d armor points = %.1f health damage (about %d equal hits per 20 health)",
                pendingHit.weaponId(),
                pendingHit.armorPoints(),
                damage,
                hitsToDefeat(damage));
        pendingHit = null;
        return Optional.of(message);
    }

    static int hitsToDefeat(float damagePerHit) {
        return damagePerHit <= 0.0F ? 0 : (int) Math.ceil(20.0F / damagePerHit);
    }

    private record PendingHit(String weaponId, float healthBefore, int armorPoints) {
    }
}
