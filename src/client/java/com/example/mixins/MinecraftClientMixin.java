package com.example.mixins;

import com.example.Configs.Config;
import com.example.overlayTodoAi.PlayerWaypointOverlay;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.example.Constants.*;
import static com.example.UntitledClient.*;
import static com.example.Utils.computeCheatConfig;
import static com.example.Utils.onPvpDamage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {
    @Shadow
    @Nullable
    public LocalPlayer player;

    @Inject(at = @At(value = "HEAD"), method = "startAttack", cancellable = true)
    private void onDoAttackHead(CallbackInfoReturnable<Boolean> cir) {
        int previousAttackCooldown = MINECRAFT_CLIENT_INSTANCE.missTime; // TODO ?
        MINECRAFT_CLIENT_INSTANCE.missTime = 0;
        if (player == null) {
            return;
        }
        if (config.isDebugModeEnabled) {
            if (previousAttackCooldown != 0) {
                PlayerWaypointOverlay.appendDebugMessage("miss penalty: " + previousAttackCooldown + " -> " + MINECRAFT_CLIENT_INSTANCE.missTime); // codex (old code) player.sendSystemMessage(Component.literal("miss penalty: " + previousAttackCooldown + " -> " + MINECRAFT_CLIENT_INSTANCE.missTime));
            }
            if (((ClientPlayerEntityInvoker) this.player).invokePick(
                    MINECRAFT_CLIENT_INSTANCE.getCameraEntity(),
                    player.blockInteractionRange(),
                    player.entityInteractionRange(),
                    MINECRAFT_CLIENT_INSTANCE.getDeltaTracker().getGameTimeDeltaTicks()) instanceof EntityHitResult firstHit &&
                    firstHit.getType() != HitResult.Type.MISS) {
                float marginBypass = firstHit.getEntity().getPickRadius();
//                boolean targetingMarginReverted = computeCheatConfig().isTargetingMarginReverted;
                float staticMarginBypass = computeCheatConfig().staticTargetingMarginBypass;
//                float movingMarginBypass = computeCheatConfig().movingTargetMarginBypass;
//                float doubleMovingMarginBypass = computeCheatConfig().doubleWalkingTargetMarginBypass;
//                computeCheatConfig().isTargetingMarginReverted = false;
                computeCheatConfig().staticTargetingMarginBypass = 0.f;
//                computeCheatConfig().movingTargetMarginBypass = 0.f;
//                computeCheatConfig().doubleWalkingTargetMarginBypass = 0.f;
                boolean flag = false;
                if (!(((ClientPlayerEntityInvoker) this.player).invokePick(
                        MINECRAFT_CLIENT_INSTANCE.getCameraEntity(),
                        player.blockInteractionRange(),
                        player.entityInteractionRange(),
                        MINECRAFT_CLIENT_INSTANCE.getDeltaTracker().getGameTimeDeltaTicks()) instanceof EntityHitResult secondHit) ||
                        secondHit.getType() == HitResult.Type.MISS) {
                    Vec3 point = firstHit.getLocation();
                    AABB box = firstHit.getEntity().getBoundingBox();
                    double dx = Math.max(box.minX - point.x, Math.max(0.0, point.x - box.maxX));
                    double dy = Math.max(box.minY - point.y, Math.max(0.0, point.y - box.maxY));
                    double dz = Math.max(box.minZ - point.z, Math.max(0.0, point.z - box.maxZ));
                    double foo = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    PlayerWaypointOverlay.appendDebugMessage("targeting margin hit (" + marginBypass + ", " + foo + "?)"); // codex (old code) Objects.requireNonNull(MINECRAFT_CLIENT_INSTANCE.player).sendSystemMessage(Component.literal("debug mode: targeting margin hit (" + marginBypass + ")"));
                    flag = true;
                }
//                computeCheatConfig().isTargetingMarginReverted = targetingMarginReverted;
                computeCheatConfig().staticTargetingMarginBypass = staticMarginBypass;
//                computeCheatConfig().movingTargetMarginBypass = movingMarginBypass;
//                computeCheatConfig().doubleWalkingTargetMarginBypass = doubleMovingMarginBypass;
                TODO;
                if (!flag && config.isReachDebugModeEnabled) {
                    cir.cancel();
                    return;
                }
            }
        }
        if (MINECRAFT_CLIENT_INSTANCE.hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof LivingEntity) {
            sprintResetBackwardsKeyState = SprintResetState.VALID;
        }
        if (MINECRAFT_CLIENT_INSTANCE.hitResult instanceof EntityHitResult entityHitResult &&
                entityHitResult.getEntity() instanceof LivingEntity target &&
                target instanceof Player playerTarget) {
            onPvpDamage();
            // codex start
            float suppressionChance = Math.clamp(config.teammateSwingSuppressionChance, 0.0F, 100.0F);
            if (config.nameplateUuids.get(playerTarget.getUUID()) instanceof Config.NameplateTeam team &&
                    team == Config.NameplateTeam.FRIENDLY &&
                    Math.random() * 100.0F < suppressionChance) {
                cir.setReturnValue(false);
                return;
            }
            // codex end
        }
        // TODO -> I could keep a counter for the random boolean passes that get bypass by the hurtTime being 0
//        if (config.isAttackSuppressionEnabled &&
//                new Random().nextBoolean() && // TODO ?
//                (!(MINECRAFT_CLIENT_INSTANCE.hitResult instanceof EntityHitResult firstHit) ||
//                        !(firstHit.getEntity() instanceof Player enemy) ||
//                        enemy.hurtTime > 0)) {
//            cir.setReturnValue(false);
//            Objects.requireNonNull(MINECRAFT_CLIENT_INSTANCE.player).swing(InteractionHand.MAIN_HAND, false);
//            // TODO -> this isn't running side effects?
////            if (MINECRAFT_CLIENT_INSTANCE.hitResult instanceof EntityHitResult firstHit &&
////                    firstHit.getEntity() instanceof LivingEntity entity) {
////                MINECRAFT_CLIENT_INSTANCE.player.magicCrit(entity);
////            }
//            return;
//        }
    }

//    @Inject(at = @At(value = "RETURN"), method = "startAttack")
//    private void onDoAttackReturn(CallbackInfoReturnable<Boolean> cir) {
//        if (config.isCheatsEnabled && // TODO -> method-ize
//                computeCheatConfig().isSneakyReachEnabled &&
//                hitResult != null &&
//                hitResult.getType() == HitResult.Type.MISS &&
//                this.getCameraEntity() instanceof Entity camera &&
//                player != null) {
////            TODO; // give reach to compensate for the angle and re-check, then attack
//            float tickDelta = this.getDeltaTracker().getGameTimeDeltaPartialTick(false);
//            var foo = ((ClientPlayerEntityInvoker) this.player).invokePick(
//                    camera,
//                    4.f,
//                    4.f, // TODO ?
//                    tickDelta);
//            float pitch = camera.getXRot();
//            camera.setXRot(0);
//            // TODO -> config this?
//            float targetingMarginBypass = computeCheatConfig().targetingMarginBypass;
//            var bar = ((ClientPlayerEntityInvoker) this.player).invokePick(
//                    camera,
//                    player.blockInteractionRange() - targetingMarginBypass,
//                    player.entityInteractionRange() - targetingMarginBypass,
//                    tickDelta);
//            camera.setXRot(pitch); // TODO -> debug by not setting this back
//            if (foo.getType() == HitResult.Type.ENTITY &&
//                    bar.getType() == HitResult.Type.ENTITY &&
//                    ((EntityHitResult) foo).getEntity() == ((EntityHitResult) bar).getEntity() &&
//                    gameMode != null &&
//                    ((EntityHitResult) foo).getEntity() instanceof Player) {
//                gameMode.attack(player, ((EntityHitResult) foo).getEntity());
//                // TODO -> debugMode this
////                player.sendMessage(Text.literal("cheating"), false);
//            }
//        }
//    }
}
