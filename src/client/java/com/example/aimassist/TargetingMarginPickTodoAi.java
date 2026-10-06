package com.example.aimassist;

import com.example.Utils;
import com.example.mixins.ClientPlayerEntityInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;

/** The debug-message pick, with an optional temporary CheatConfig bypass value. */
public final class TargetingMarginPickTodoAi {
    private TargetingMarginPickTodoAi() {}

    public static HitResult pick(Minecraft mc, Float bypass) {
        // codex start
        return pick(mc, bypass, 0);
    }
    public static HitResult pick(Minecraft mc, Float bypass, double range) {
        return AimAssistReachTodoAi.withRange(mc.player, range, () -> pickWithCurrentReach(mc, bypass, range));
    }
    private static HitResult pickWithCurrentReach(Minecraft mc, Float bypass, double range) {
        //codex end
        var cheats = Utils.computeCheatConfig();
        float previous = cheats.staticTargetingMarginBypass;
        try {
            if (bypass != null) cheats.staticTargetingMarginBypass = bypass;
            return ((ClientPlayerEntityInvoker) mc.gameRenderer).invokePick(mc.getCameraEntity(), //codex (old code snippet) return ClientPlayerEntityInvoker.aimAssistPickTodoAi(mc.getCameraEntity(),
                range > 0 ? Math.min(range, mc.player.blockInteractionRange()) : mc.player.blockInteractionRange(), //codex (old code snippet) mc.player.blockInteractionRange(), mc.player.entityInteractionRange(),
                range > 0 ? Math.min(range, mc.player.entityInteractionRange()) : mc.player.entityInteractionRange(),
                mc.getDeltaTracker().getGameTimeDeltaTicks());
        } finally {
            cheats.staticTargetingMarginBypass = previous;
        }
    }
}
