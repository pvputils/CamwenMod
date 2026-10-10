/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.event.EventManagerTodoLiquidbounce;
import net.ccbluex.liquidbounce.event.events.MovementInputEventTodoLiquidbounce;
import net.ccbluex.liquidbounce.event.events.SprintEventTodoLiquidbounce;
import net.ccbluex.liquidbounce.utils.aiming.RotationManagerTodoLiquidbounce;
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrectionTodoLiquidbounce;
import net.ccbluex.liquidbounce.utils.movement.DirectionalInputTodoLiquidbounce;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import static net.minecraft.util.Mth.DEG_TO_RAD;

@Mixin(KeyboardInput.class)
public abstract class MixinKeyboardInputTodoLiquidbounce extends MixinClientInputTodoLiquidbounce {

    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;"))
    private Input modifyInput(Input original) {
        this.initial = original;

        var event = new MovementInputEventTodoLiquidbounce(new DirectionalInputTodoLiquidbounce(original), original.jump(), original.shift());
        EventManagerTodoLiquidbounce.INSTANCE.callEvent(event);
        var untransformedDirectionalInput = event.getDirectionalInput();
        var directionalInput = transformDirection(untransformedDirectionalInput);

        var sprintEvent = new SprintEventTodoLiquidbounce(directionalInput, original.sprint(), SprintEventTodoLiquidbounce.Source.INPUT);
        EventManagerTodoLiquidbounce.INSTANCE.callEvent(sprintEvent);

        // Store the untransformed input for later use
        this.untransformed = new Input(
                untransformedDirectionalInput.getForwards(),
                untransformedDirectionalInput.getBackwards(),
                untransformedDirectionalInput.getLeft(),
                untransformedDirectionalInput.getRight(),
                event.getJump(),
                event.getSneak(),
                sprintEvent.getSprint()
        );

        return new Input(
                directionalInput.getForwards(),
                directionalInput.getBackwards(),
                directionalInput.getLeft(),
                directionalInput.getRight(),
                event.getJump(),
                event.getSneak(),
                sprintEvent.getSprint()
        );
    }

    @Unique
    private DirectionalInputTodoLiquidbounce transformDirection(DirectionalInputTodoLiquidbounce input) {
        var player = Minecraft.getInstance().player;
        var rotation = RotationManagerTodoLiquidbounce.INSTANCE.getCurrentRotation();
        var configurable = RotationManagerTodoLiquidbounce.INSTANCE.getActiveRotationTarget();

        float z = KeyboardInput.calculateImpulse(input.getForwards(), input.getBackwards());
        float x = KeyboardInput.calculateImpulse(input.getLeft(), input.getRight());

        if (configurable == null || configurable.getMovementCorrection() != MovementCorrectionTodoLiquidbounce.SILENT
                || rotation == null || player == null) {
            return input;
        }

        float deltaYaw = player.getYRot() - rotation.yRot();

        float newX = x * Mth.cos(deltaYaw * DEG_TO_RAD) - z *
                Mth.sin(deltaYaw * DEG_TO_RAD);
        float newZ = z * Mth.cos(deltaYaw * DEG_TO_RAD) + x *
                Mth.sin(deltaYaw * DEG_TO_RAD);

        var movementSideways = Math.round(newX);
        var movementForward = Math.round(newZ);

        return new DirectionalInputTodoLiquidbounce(movementForward, movementSideways);
    }

}
