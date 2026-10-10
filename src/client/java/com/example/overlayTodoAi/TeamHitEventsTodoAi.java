package com.example.overlayTodoAi;

import com.example.Configs.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.world.entity.player.Player;

import static com.example.UntitledClient.config;

/** Only client-loaded players can supply team hit events. */
public final class TeamHitEventsTodoAi {
    private TeamHitEventsTodoAi() {}

    public static void record(Minecraft client, ClientboundDamageEventPacket packet) {
        TeamHitDeltaCounterTodoAi.updateSession(client.level, client.player, config.isTeamHitDeltaCounterEnabled);
        if (!config.isTeamHitDeltaCounterEnabled || client.level == null || client.player == null) return;
        if (!(client.level.getEntity(packet.entityId()) instanceof Player victim)
                || !(client.level.getEntity(packet.sourceCauseId()) instanceof Player attacker)) return;
        boolean victimTeam = isTeammate(client, victim);
        boolean attackerTeam = isTeammate(client, attacker);
        TeamHitDeltaCounterTodoAi.recordDamage(victimTeam, attackerTeam,
                victimTeam && attackerTeam, victim == attacker);
    }

    private static boolean isTeammate(Minecraft client, Player player) {
        if (player == client.player) return true;
        Config.NameplateTeam team = config.nameplateUuids.get(player.getUUID());
        return team == Config.NameplateTeam.ALLY || team == Config.NameplateTeam.FRIENDLY;
    }
}
