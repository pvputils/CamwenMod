package com.example.killaura;

import com.example.aimassist.TargetingMarginPickTodoAi;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;

/** A scoped player-only pick radius, independent of global cheat/margin settings. */
public final class KillAuraMarginPickTodoAi {
    private static final ThreadLocal<Float> CURRENT = new ThreadLocal<>();
    private KillAuraMarginPickTodoAi() {}
    public static Float current() { return CURRENT.get(); }
    public static HitResult pick(Minecraft mc, double margin) {
        Float previous = CURRENT.get();
        try {
            CURRENT.set((float) KillAuraGeometryTodoAi.finite(margin, 0.3, 0, 1));
            return TargetingMarginPickTodoAi.pick(mc, null);
        } finally {
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }
}
