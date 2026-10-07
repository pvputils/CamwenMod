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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.common.ChunkUpdateFlagTodoAi;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.*;
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Optional;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListenerTodoAi extends ClientCommonPacketListenerImpl {

    protected MixinClientPacketListenerTodoAi(Minecraft client, Connection connection, CommonListenerCookie connectionState) {
        super(client, connection, connectionState);
    }

    @Inject(method = "handleLevelChunkWithLight", at = @At("RETURN"))
    private void injectChunkLoadEvent(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ChunkLoadEvent(packet.getX(), packet.getZ()));
    }

    @Inject(method = "handleForgetLevelChunk", at = @At("RETURN"))
    private void injectUnloadEvent(ClientboundForgetLevelChunkPacket packet, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ChunkUnloadEvent(packet.pos()));
    }

    @WrapMethod(method = "handleChunkBlocksUpdate")
    private void onChunkDeltaUpdateStart(ClientboundSectionBlocksUpdatePacket packet, Operation<Void> original) {
        ChunkUpdateFlagTodoAi.withChunkDeltaUpdating(() -> {
            original.call(packet);
            EventManager.INSTANCE.callEvent(new ChunkDeltaUpdateEvent(packet));
        });
    }













    @Inject(method = "handleSetHealth", at = @At("HEAD"))
    private void injectHealthUpdate(ClientboundSetHealthPacket packet, CallbackInfo ci) {
        LocalPlayer player = this.minecraft.player;

        if (player == null) {
            return;
        }

        EventManager.INSTANCE.callEvent(new HealthUpdateEvent(packet.getHealth(), packet.getFood(), packet.getSaturation(), player.getHealth()));

        if (packet.getHealth() == 0) {
            EventManager.INSTANCE.callEvent(DeathEvent.INSTANCE);
        }
    }

    @Unique
    private final ThreadLocal<Rotation> rotationThreadLocal = ThreadLocal.withInitial(() -> null);

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;setValuesFromPositionPacket(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;Lnet/minecraft/world/entity/Entity;Z)Z"))
    private void injectPlayerPositionLook(
        ClientboundPlayerPositionPacket packet, CallbackInfo ci, @Local(name = "player") Player player) {
        rotationThreadLocal.set(new Rotation(player.getYRot(), player.getXRot(), true));
    }





}
