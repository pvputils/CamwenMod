package com.example.aimassist;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.function.Supplier;

/** Scoped client-side reach range used only by aim-assist probes. */
public final class AimAssistReachTodoAi {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("pvputils", "aim_assist_reach");
    public static double range(double reach) { return Double.isFinite(reach) ? Math.max(0, reach) : 0; }
    public static <T> T withRange(Player player, double reach, Supplier<T> action) {
        double desired = range(reach);
        if (desired == 0) return action.get();
        AttributeInstance block = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        AttributeInstance entity = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (block == null || entity == null) return action.get();
        AttributeModifier oldBlock = block.getModifier(ID), oldEntity = entity.getModifier(ID);
        boolean permanentBlock = oldBlock != null && block.getPermanentModifiers().contains(oldBlock);
        boolean permanentEntity = oldEntity != null && entity.getPermanentModifiers().contains(oldEntity);
        try {
            block.removeModifier(ID);
            entity.removeModifier(ID);
            block.addTransientModifier(new AttributeModifier(ID, Math.max(0, desired - block.getValue()), AttributeModifier.Operation.ADD_VALUE));
            entity.addTransientModifier(new AttributeModifier(ID, Math.max(0, desired - entity.getValue()), AttributeModifier.Operation.ADD_VALUE));
            return action.get();
        } finally {
            restore(block, oldBlock, permanentBlock);
            restore(entity, oldEntity, permanentEntity);
        }
    }
    private static void restore(AttributeInstance attribute, AttributeModifier previous, boolean permanent) {
        attribute.removeModifier(ID);
        if (previous != null) {
            if (permanent) attribute.addPermanentModifier(previous);
            else attribute.addTransientModifier(previous);
        }
    }
    private AimAssistReachTodoAi() {}
}
