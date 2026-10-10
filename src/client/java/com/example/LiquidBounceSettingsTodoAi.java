package com.example;

import net.ccbluex.liquidbounce.integration.screen.KillAuraConfigScreenTodoLiquidbounce;
import net.minecraft.client.Minecraft;

public final class LiquidBounceSettingsTodoAi {
    private LiquidBounceSettingsTodoAi() {}
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.setScreenAndShow(new KillAuraConfigScreenTodoLiquidbounce(mc.gui.screen(), net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAuraTodoLiquidbounce.INSTANCE, 0)));
    }
}
