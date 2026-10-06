package com.example.aimassist;

import com.example.Utils;
import com.example.mixins.ClientPlayerEntityInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;

/** The debug-message pick, with an optional temporary CheatConfig bypass value. */
public final class TargetingMarginPickTodoAi {
    private TargetingMarginPickTodoAi() {}

    public static HitResult pick(Minecraft mc, Float bypass) {
        var cheats = Utils.computeCheatConfig();
        float previous = cheats.staticTargetingMarginBypass;
        try {
            if (bypass != null) cheats.staticTargetingMarginBypass = bypass;
            return ((ClientPlayerEntityInvoker) mc.gameRenderer).invokePick(mc.getCameraEntity(), //codex (old code snippet) return ClientPlayerEntityInvoker.aimAssistPickTodoAi(mc.getCameraEntity(),
                mc.player.blockInteractionRange(), mc.player.entityInteractionRange(),
                mc.getDeltaTracker().getGameTimeDeltaTicks());
        } finally {
            cheats.staticTargetingMarginBypass = previous;
        }
    }
}
