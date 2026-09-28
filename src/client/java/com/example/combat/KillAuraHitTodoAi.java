/*
 * Standalone adaptation of LiquidBounce, Copyright (c) 2015-2026 CCBlueX.
 * Modified for CamwenMod: explicit entity requests and Minecraft 26.2.
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Distributed without warranty; see LICENSE-LiquidBounceTodoAi.txt.
 * Upstream: CCBlueX/LiquidBounce debd001505f39fc0c41698496b895e5dd9d2dfd5
 */
package com.example.combat;

import com.example.UntitledClient;
import com.example.combat.clicking.AttackClockTodoAi;
import com.example.mixins.AuraGameModeAccessTodoAi;
import com.example.mixins.AuraLivingAccessTodoAi;
import com.example.mixins.AuraPlayerAccessTodoAi;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static com.example.UntitledClient.config;
import static com.example.UntitledClient.killAuraTargetName;
import static com.example.Utils.getAbstractPvpUtilsKeybind;
import static com.example.Utils.getIsKeyBindingPressed;
import static com.example.combat.HitQueueTodoAi.Result.*;
import static com.example.combat.HitQueueTodoAi.Stage.*;

/**
 * Standalone scheduled-hit port of ModuleKillAura.attackTarget, KillAuraClicker.prepareForAttack,
 * Clicker and CombatExtensions.attackEntity. Uses LiquidBounce's ON_TICK rotation path.
 * All public mutating methods run on the Minecraft client thread.
 */
public final class KillAuraHitTodoAi {
    public enum Criticals { IGNORE, ALWAYS }

    public record Options(double range, double wallsRange, int timeoutTicks,
                          boolean humanTiming, int minimumCps, int maximumCps,
                          Criticals criticals, boolean keepSprint, boolean unblock, int unblockTicks, boolean reblock) {
        public Options {
            if (!Double.isFinite(range) || range <= 0 || range > 6 ||
                    !Double.isFinite(wallsRange) || wallsRange < 0 || wallsRange > range ||
                    timeoutTicks < 1 || timeoutTicks > 1200 ||
                    minimumCps < 1 || maximumCps < minimumCps || maximumCps > 30 ||
                    criticals == null || unblockTicks < 0 || unblockTicks > 20) {
                throw new IllegalArgumentException("Invalid scheduled hit options");
            }
        }

        public static Options defaults() {
            return new Options(3, 0, 100, true, 11, 14, Criticals.IGNORE, true, true, 1, true);
        }
    }

    private static final KeyMapping HIT_HOLD = getAbstractPvpUtilsKeybind("Kill aura hit (Hold)");
    private static final HitQueueTodoAi<Player, Options> QUEUE = new HitQueueTodoAi<>(64);
    private static final AttackClockTodoAi CLOCK = new AttackClockTodoAi();
    private static long tick;
    private static ClientLevel world;
    private static LocalPlayer owner;
    private static boolean initialized;
    private static HitQueueTodoAi.Request<Player, Options> heldRequest;
    private static HitQueueTodoAi.Request<Player, Options> preparedRequest;
    private static InteractionHand blockedHand;
    private static ItemStack blockedStack;
    private static long unblockUntil;

    private KillAuraHitTodoAi() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ClientTickEvents.END_CLIENT_TICK.register(KillAuraHitTodoAi::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            forgetPreparation();
            QUEUE.clear(DISCONNECTED);
            heldRequest = null;
            world = null;
            owner = null;
        });
    }

    /** Enqueues exactly one attack on this entity object. SENT means dispatched, not server-confirmed damage. */
    public static HitQueueTodoAi.Request<Player, Options> scheduleHit(Player target) {
        return scheduleHit(target, Options.defaults());
    }

    public static HitQueueTodoAi.Request<Player, Options> scheduleHit(Player target, Options options) {
        Minecraft client = Minecraft.getInstance();
        requireClientThread(client);
        synchronizeWorld(client);
        if (client.level == null || client.player == null || target == null ||
                target == client.player || target.level() != client.level ||
                client.level.getEntity(target.getId()) != target || target.isRemoved() ||
                !target.isAlive() || target.isSpectator()) {
            throw new IllegalArgumentException("Target must be a live remote player in the current client world");
        }
        return QUEUE.schedule(target, options, tick, options.timeoutTicks());
    }

    public static void cancelAll() {
        Minecraft client = Minecraft.getInstance();
        requireClientThread(client);
        restoreBlocking(client);
        QUEUE.clear(CANCELLED);
        heldRequest = null;
    }

    private static void requireClientThread(Minecraft client) {
        if (!client.isSameThread()) throw new IllegalStateException("Use Minecraft.execute to schedule/cancel hits");
    }

    private static void synchronizeWorld(Minecraft client) {
        if (client.level != world || client.player != owner) {
            forgetPreparation();
            heldRequest = null;
            world = client.level;
            owner = client.player;
            QUEUE.clear(DISCONNECTED);
        }
    }

    private static void tick(Minecraft client) {
        tick++;
        synchronizeWorld(client);
        if (world == null || owner == null || client.gameMode == null) return;
        if (!config.isCheatsEnabled || !owner.isAlive() || owner.isSpectator()) {
            restoreBlocking(client);
            QUEUE.clear(DISABLED);
            return;
        }
        updateHoldBinding(client);
        var request = QUEUE.current(tick);
        if (preparedRequest != null && preparedRequest != request) restoreBlocking(client);
        if (request == null) return;
        Player target = request.target();
        if (target.level() != world || target.isRemoved() || !target.isAlive() ||
                target.isSpectator() || world.getEntity(target.getId()) != target) {
            finish(client, request, INVALID_TARGET);
            return;
        }
        try {
            runPipeline(client, request);
        } catch (RuntimeException error) {
            finish(client, request, FAILED);
            org.slf4j.LoggerFactory.getLogger("KillAuraHit").error("Scheduled hit failed", error);
        }
    }

    private static void runPipeline(Minecraft client, HitQueueTodoAi.Request<Player, Options> request) {
        LocalPlayer player = client.player;
        Options options = request.options();
        Player target = request.target();
        float delay = player.getCurrentItemAttackStrengthDelay();
        int attackTicks = ((AuraLivingAccessTodoAi) player).auraAttackTicks();
        float cooldown = attackTicks / delay;
        boolean crossedCooldown = cooldown >= 1 && (attackTicks - 1) / delay < 1;
        CLOCK.tick(System.nanoTime() / 1_000_000, options.humanTiming(),
                options.minimumCps(), options.maximumCps(), crossedCooldown);

        // ModuleKillAura.canAttackNow and the inventory/item-use gates from prepareForAttack.
        if (client.gui.screen() != null || client.isPaused()) {
            request.stage(WAITING_FOR_ITEM);
            return;
        }
        ItemStack item = player.getMainHandItem();
        if (!item.isItemEnabled(world.enabledFeatures()) || player.cannotAttackWithItem(item, 0)) {
            request.stage(WAITING_FOR_ITEM);
            return;
        }
        request.stage(AIMING);
        var rotation = new AuraAimTodoAi.Rotation(player.getYRot(), player.getXRot());
        double reach = Math.min(options.range(), player.entityInteractionRange());
        var aim = AuraAimTodoAi.find(player.getEyePosition(), target.getBoundingBox(), rotation,
                reach, Math.min(options.wallsRange(), reach), client.options.sensitivity().get(),
                hit -> visible(player, hit), hit -> entityPathClear(player, target, hit));
        if (aim == null) return;

        if (!CLOCK.ready() || cooldown < 1 || client.missTime > 0) {
            request.stage(WAITING_FOR_CLICK);
            return;
        }
        if (options.criticals() == Criticals.ALWAYS && !canCritical(player)) {
            request.stage(WAITING_FOR_CRITICAL);
            return;
        }
        if (player.isUsingItem()) {
            if (!player.isBlocking() || !options.unblock()) {
                request.stage(WAITING_FOR_ITEM);
                return;
            }
            if (preparedRequest == null) {
                preparedRequest = request;
                blockedHand = player.getUsedItemHand();
                blockedStack = player.getItemInHand(blockedHand).copy();
                unblockUntil = tick + options.unblockTicks();
            }
            // Vanilla may restart blocking between ticks while the use key remains held.
            client.gameMode.releaseUsingItem(player);
        }
        if (tick < unblockUntil && preparedRequest == request) {
            request.stage(UNBLOCKING);
            return;
        }
        if (player.isUsingItem()) {
            request.stage(WAITING_FOR_ITEM);
            return;
        }

        // Recheck the quantized ray immediately before dispatch, without substituting another entity.
        if (AuraAimTodoAi.trace(player.getEyePosition(), target.getBoundingBox(), aim.rotation(),
                reach, Math.min(options.wallsRange(), reach),
                hit -> visible(player, hit), hit -> entityPathClear(player, target, hit)) == null) return;
        boolean sent = CLOCK.consume(() -> {
            if (request.isDone() || !target.isAlive() || target.isRemoved() ||
                    player.getAttackStrengthScale(0) < 1 ||
                    player.cannotAttackWithItem(player.getMainHandItem(), 0)) return false;
            request.stage(ATTACKING);
            attackAndRestore(client, target, aim, options);
            return true;
        });
        if (sent) finish(client, request, SENT);
    }

    private static boolean visible(LocalPlayer player, Vec3 hit) {
        HitResult block = world.clip(new ClipContext(player.getEyePosition(), hit,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return block.getType() == HitResult.Type.MISS ||
                player.getEyePosition().distanceToSqr(block.getLocation()) + 1e-7 >=
                        player.getEyePosition().distanceToSqr(hit);
    }

    private static boolean entityPathClear(LocalPlayer player, Player target, Vec3 hit) {
        Vec3 eyes = player.getEyePosition();
        AABB search = new AABB(eyes, hit).inflate(1e-6);
        double targetDistance = eyes.distanceToSqr(hit);
        for (Entity other : world.getEntities(player, search,
                entity -> entity != target && entity.isPickable() && !entity.isSpectator())) {
            AABB box = other.getBoundingBox();
            Vec3 intercept = box.contains(eyes) ? eyes : box.clip(eyes, hit).orElse(null);
            if (intercept != null && eyes.distanceToSqr(intercept) < targetDistance - 1e-7) return false;
        }
        return true;
    }

    private static boolean canCritical(LocalPlayer player) {
        return !player.onGround() && player.fallDistance > 0 && !player.isInWater() &&
                !player.isInLava() && !player.onClimbable() && !player.isPassenger() &&
                !player.getAbilities().flying && !player.isFallFlying() && !player.isNoGravity() &&
                !player.hasEffect(MobEffects.BLINDNESS) && !player.hasEffect(MobEffects.LEVITATION) &&
                !player.hasEffect(MobEffects.SLOW_FALLING) && player.getAttackStrengthScale(0.5F) > 0.9F;
    }

    private static void attackAndRestore(Minecraft client, Player target, AuraAimTodoAi.Aim aim, Options options) {
        LocalPlayer player = client.player;
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        HitResult oldHit = client.hitResult;
        Entity oldCrosshair = client.crosshairPickEntity;
        boolean sprint = player.isSprinting();
        boolean stopSprint = sprint && options.criticals() == Criticals.ALWAYS;
        boolean rotated = false;
        try {
            if (stopSprint) {
                player.setSprinting(false);
                player.connection.send(new ServerboundPlayerCommandPacket(player,
                        ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
            }
            sendRotation(player, aim.rotation().yaw(), aim.rotation().pitch());
            rotated = true;
            player.setYRot(aim.rotation().yaw());
            player.setXRot(aim.rotation().pitch());
            client.hitResult = new EntityHitResult(target, aim.hit());
            client.crosshairPickEntity = target;
            var piercing = player.getMainHandItem().get(DataComponents.PIERCING_WEAPON);
            if (piercing != null) {
                client.gameMode.piercingAttack(piercing);
            } else {
                dispatchAttack(client, player, target, options.keepSprint() && !stopSprint);
            }
            player.swing(InteractionHand.MAIN_HAND);
        } finally {
            player.setYRot(yaw);
            player.setXRot(pitch);
            client.hitResult = oldHit;
            client.crosshairPickEntity = oldCrosshair;
            if (rotated) sendRotation(player, yaw, pitch);
            if (stopSprint) {
                player.setSprinting(sprint);
                player.connection.send(new ServerboundPlayerCommandPacket(player,
                        ServerboundPlayerCommandPacket.Action.START_SPRINTING));
            }
        }
    }

    /** Port of CombatExtensions.attackEntity for the native 26.2 protocol. */
    private static void dispatchAttack(Minecraft client, LocalPlayer player, Player target, boolean keepSprint) {
        ((AuraGameModeAccessTodoAi) client.gameMode).auraSyncCarriedItem();
        player.connection.send(new ServerboundAttackPacket(target.getId()));
        if (keepSprint) {
            float base = player.isAutoSpinAttack()
                    ? ((AuraLivingAccessTodoAi) player).auraAutoSpinDamage()
                    : (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float enchanted = ((AuraPlayerAccessTodoAi) player).auraEnchantedDamage(
                    target, base, player.damageSources().playerAttack(player)) - base;
            float strength = player.getAttackStrengthScale(0.5F);
            base *= 0.2F + strength * strength * 0.8F;
            enchanted *= strength;
            if (base > 0 || enchanted > 0) {
                if (enchanted > 0) player.magicCrit(target);
                if (canCritical(player)) {
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_ATTACK_CRIT, player.getSoundSource(), 1, 1);
                    player.crit(target);
                }
            }
        } else {
            player.attack(target);
        }
        ((AuraLivingAccessTodoAi) player).auraSetAttackTicks(0);
    }

    private static void sendRotation(LocalPlayer player, float yaw, float pitch) {
        player.connection.send(new ServerboundMovePlayerPacket.PosRot(player.getX(), player.getY(), player.getZ(),
                yaw, pitch, player.onGround(), player.horizontalCollision));
    }

    private static void finish(Minecraft client, HitQueueTodoAi.Request<Player, Options> request,
                               HitQueueTodoAi.Result result) {
        restoreBlocking(client);
        QUEUE.finish(request, result);
    }

    private static void restoreBlocking(Minecraft client) {
        var prepared = preparedRequest;
        var hand = blockedHand;
        var stack = blockedStack;
        forgetPreparation();
        if (prepared != null && prepared.options().reblock() && client.player == owner &&
                client.level == world && owner != null && owner.isAlive() && !owner.isSpectator() &&
                client.gameMode != null && hand != null && !owner.isUsingItem() &&
                ItemStack.isSameItemSameComponents(owner.getItemInHand(hand), stack)) {
            client.gameMode.useItem(owner, hand);
        }
    }

    private static void forgetPreparation() {
        preparedRequest = null;
        blockedHand = null;
        blockedStack = null;
        unblockUntil = 0;
    }

    private static void updateHoldBinding(Minecraft client) {
        boolean held = client.gui.screen() == null && getIsKeyBindingPressed(HIT_HOLD);
        if (!held) {
            if (heldRequest != null) heldRequest.cancel();
            heldRequest = null;
            return;
        }
        if (heldRequest != null && !heldRequest.isDone() || QUEUE.current(tick) != null) return;
        String name = killAuraTargetName == null ? "" : killAuraTargetName.trim();
        if (name.isEmpty()) return;
        client.level.players().stream()
                .filter(target -> target != owner && target.isAlive() && !target.isSpectator())
                .filter(target -> target.getGameProfile().name().equalsIgnoreCase(name))
                .findFirst().ifPresent(target -> heldRequest = scheduleHit(target));
    }
}
