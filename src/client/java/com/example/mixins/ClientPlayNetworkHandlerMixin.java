package com.example.mixins;

import com.example.overlayTodoAi.HitDeltaCounterTodoAi;
import com.example.overlayTodoAi.TeamHitEventsTodoAi;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.example.Constants.MINECRAFT_CLIENT_INSTANCE;
import static com.example.UntitledClient.config;
import static com.example.Utils.onPvpDamage;
import static net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "handleSetHealth", at = @At("HEAD"))
    void onOnHealthUpdate(ClientboundSetHealthPacket packet, CallbackInfo ci) {
        // TODO -> get amount of damage taken from teammate? might be impossible
        if (!(MINECRAFT_CLIENT_INSTANCE.player instanceof LocalPlayer player)) {
            return;
        }
        float previous = player.getHealth();
        float health = packet.getHealth();
        if (previous == health) {
            return;
        }

        if (config.isDamageTakenValueNotificationEnabled && previous > health) {
            player.sendSystemMessage(Component.literal(String.valueOf(previous - health)));
        }
    }

    @Inject(method = "handleDamageEvent", at = @At("RETURN"))
    void onOnEntityDamage(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        if (!(MINECRAFT_CLIENT_INSTANCE.level instanceof ClientLevel clientWorld)) {
            return;
        }

        // codex start
        LocalPlayer localPlayer = MINECRAFT_CLIENT_INSTANCE.player;
        HitDeltaCounterTodoAi.updateSession(clientWorld, localPlayer, config.isHitDeltaCounterEnabled);
        if (localPlayer != null && clientWorld.getEntity(packet.entityId()) instanceof Player
                && clientWorld.getEntity(packet.sourceCauseId()) instanceof Player) {
            HitDeltaCounterTodoAi.recordDamage(packet.entityId(), packet.sourceCauseId(), localPlayer.getId());
        }
        //codex end
        // codex start
        TeamHitEventsTodoAi.record(MINECRAFT_CLIENT_INSTANCE, packet);
        //codex end
        Entity entity = clientWorld.getEntity(packet.entityId());
        if (clientWorld.getEntity(packet.sourceCauseId()) instanceof Player attacker) {
            if (MINECRAFT_CLIENT_INSTANCE.player instanceof LocalPlayer player &&
                    player == entity) {
                // TODO -> player == attacker?
                onPvpDamage();
            }
        }
    }

    @Inject(method = "handleAddEntity", at = @At("RETURN"))
    void onOnEntitySpawn(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        // TODO -> waypoint this?
        if (packet.getType() == LIGHTNING_BOLT &&
                MINECRAFT_CLIENT_INSTANCE.player instanceof LocalPlayer player) {
            player.sendSystemMessage(Component.literal(packet.getX() + ", " + packet.getY() + ", " + packet.getZ()));
        }
    }
}
