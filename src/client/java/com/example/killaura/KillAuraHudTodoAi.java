package com.example.killaura;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Native target marker and held-item range readout; no custom fonts or GPU pipeline. */
public final class KillAuraHudTodoAi {
    private KillAuraHudTodoAi() {}
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance(); var config = KillAuraControllerTodoAi.config();
        if (!config.enabled || mc.player == null || mc.gui.screen() != null || mc.player.isDeadOrDying() || mc.player.isSpectator()) return;
        int x = mc.getWindow().getGuiScaledWidth() / 2, y = mc.getWindow().getGuiScaledHeight() / 2;
        var target = KillAuraControllerTodoAi.target();
        if (config.visuals.targetMarker && target != null) {
            graphics.fill(x - 8, y - 8, x - 6, y - 4, config.visuals.color);
            graphics.fill(x + 6, y - 8, x + 8, y - 4, config.visuals.color);
            graphics.fill(x - 8, y + 4, x - 6, y + 8, config.visuals.color);
            graphics.fill(x + 6, y + 4, x + 8, y + 8, config.visuals.color);
            graphics.centeredText(mc.font, target.getDisplayName(), x, y + 14, config.visuals.color);
        }
        if (config.visuals.rangeIndicator) {
            var range = KillAuraControllerTodoAi.attackRange(mc);
            graphics.centeredText(mc.font, Component.literal(String.format(java.util.Locale.ROOT, "KillAura reach: %.2f–%.2f", range.effectiveMinRange(mc.player), range.effectiveMaxRange(mc.player))), x, y + 28, config.visuals.color);
        }
    }
}
