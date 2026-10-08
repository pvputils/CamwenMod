package com.example.aimassist;

import com.example.mixins.ClientPlayerEntityInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;

/** The debug-message pick, with a temporary probe-only margin. */
public final class TargetingMarginPickTodoAi {
    private TargetingMarginPickTodoAi() {}

    public static HitResult pick(Minecraft mc, Float bypass) {
        return pick(mc, bypass, 0);
    }
    public static HitResult pick(Minecraft mc, Float bypass, double range) {
        return AimAssistReachTodoAi.withRange(mc.player, range, () -> pickWithCurrentReach(mc, bypass, range));
    }
    private static HitResult pickWithCurrentReach(Minecraft mc, Float bypass, double range) {
        return AimAssistMarginScopeTodoAi.withMargin(bypass, () ->
            ((ClientPlayerEntityInvoker) mc.gameRenderer).invokePick(mc.getCameraEntity(),
                range > 0 ? Math.min(range, mc.player.blockInteractionRange()) : mc.player.blockInteractionRange(),
                range > 0 ? Math.min(range, mc.player.entityInteractionRange()) : mc.player.entityInteractionRange(),
                mc.getDeltaTracker().getGameTimeDeltaTicks()));
    }
}