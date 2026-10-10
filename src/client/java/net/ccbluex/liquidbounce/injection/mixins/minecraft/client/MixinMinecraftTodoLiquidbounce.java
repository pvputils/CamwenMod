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

import com.mojang.blaze3d.platform.Window;
import net.ccbluex.liquidbounce.LiquidBounceTodoLiquidbounce;
import net.ccbluex.liquidbounce.event.CoroutineTickerTodoLiquidbounce;
import net.ccbluex.liquidbounce.event.EventManagerTodoLiquidbounce;
import net.ccbluex.liquidbounce.event.TickLoopTaskExecutorTodoLiquidbounce;
import net.ccbluex.liquidbounce.event.events.*;
import net.ccbluex.liquidbounce.features.misc.SelfDestructTodoLiquidbounce;
import net.ccbluex.liquidbounce.render.ClientTesselatorTodoLiquidbounce;
import net.ccbluex.liquidbounce.render.buffers.StaticGpuBufferPoolTodoLiquidbounce;
import net.ccbluex.liquidbounce.render.mesh.MeshDrawTodoLiquidbounce;
import net.ccbluex.liquidbounce.render.utils.RenderingDebugTodoLiquidbounce;
import net.ccbluex.liquidbounce.utils.client.vfp.VfpCompatibilityTodoLiquidbounce;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.User;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import static net.ccbluex.liquidbounce.utils.client.ProtocolUtilTodoLiquidbounceKt.getUsesViaFabricPlus;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftTodoLiquidbounce {

    @Shadow
    @Nullable
    public LocalPlayer player;
    @Shadow
    @Nullable
    public HitResult hitResult;
    @Shadow
    @Final
    public Options options;
    @Shadow
    @Nullable
    private IntegratedServer singleplayerServer;
    @Shadow
    private int rightClickDelay;
    @Shadow
    @Nullable
    public MultiPlayerGameMode gameMode;

    @Shadow
    @Nullable
    public abstract ClientPacketListener getConnection();

    @Shadow
    public abstract @Nullable ServerData getCurrentServer();

    @Shadow
    public abstract Window getWindow();

    @Shadow
    public abstract int getFps();

    @Shadow
    public abstract User getUser();

    @Shadow
    protected abstract void continueAttack(boolean breaking);

    @Shadow
    @Nullable
    public ClientLevel level;

    @Shadow
    @Final
    public Gui gui;

    /**
     * Entry point
     */
    @Inject(method = "<init>(Lnet/minecraft/client/main/GameConfig;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;resizeGui()V"))
    private void startClient(CallbackInfo callback) {
        EventManagerTodoLiquidbounce.INSTANCE.callEvent(ClientStartEventTodoLiquidbounce.INSTANCE);
    }

    /**
     * Exit point
     */
    @Inject(method = "close", at = @At("HEAD"))
    private void stopClient(CallbackInfo callback) {
        MeshDrawTodoLiquidbounce.DefaultUploader.close();
        EventManagerTodoLiquidbounce.INSTANCE.callEvent(ClientShutdownEventTodoLiquidbounce.INSTANCE);
    }

    // codex start
    // @Inject(method = "<init>(Lnet/minecraft/client/main/GameConfig;)V", at = @At(value = "FIELD",
    //     target = "Lnet/minecraft/client/Minecraft;profileKeyPairManager:Lnet/minecraft/client/multiplayer/ProfileKeyPairManager;",
    //     ordinal = 0, shift = At.Shift.AFTER, opcode = Opcodes.PUTFIELD))
    // private void onSessionInit(CallbackInfo callback) {
    //     EventManager.INSTANCE.callEvent(new SessionEvent(getUser()));
    // }
    // codex end

    /**
     * Modify window title to our client title.
     * Example: LiquidBounce v1.0.0 | 1.16.3
     *
     * @param callback our window title
     *                 <p>
     *                 todo: modify constant Minecraft instead
     */
    @Inject(method = "createTitle", at = @At(
            value = "INVOKE",
            target = "Ljava/lang/StringBuilder;append(Ljava/lang/String;)Ljava/lang/StringBuilder;",
            ordinal = 1),
            cancellable = true)
    private void getClientTitle(CallbackInfoReturnable<String> callback) {
        if (SelfDestructTodoLiquidbounce.INSTANCE.isDestructed()) {
            return;
        }

        LiquidBounceTodoLiquidbounce.INSTANCE.getLogger().debug("Modifying window title");

        StringBuilder titleBuilder = new StringBuilder(LiquidBounceTodoLiquidbounce.CLIENT_NAME);
        titleBuilder.append(" v");
        titleBuilder.append(LiquidBounceTodoLiquidbounce.INSTANCE.getClientVersion());
        titleBuilder.append(" ");

        if (LiquidBounceTodoLiquidbounce.IN_DEVELOPMENT) {
            titleBuilder.append("(dev) ");
        }

        titleBuilder.append(LiquidBounceTodoLiquidbounce.INSTANCE.getClientCommit());

        titleBuilder.append(" | ");

        // ViaFabricPlus compatibility
        if (getUsesViaFabricPlus()) {
            var protocolVersion = VfpCompatibilityTodoLiquidbounce.INSTANCE.unsafeGetProtocolVersion();

            if (protocolVersion != null) {
                titleBuilder.append(protocolVersion.name());
            } else {
                titleBuilder.append(SharedConstants.getCurrentVersion().name());
            }
        } else {
            titleBuilder.append(SharedConstants.getCurrentVersion().name());
        }

        // codex start
        // EventManager.INSTANCE.callEvent(new WindowTitleEvent(titleBuilder));
        // codex end

        ClientPacketListener clientPlayNetworkHandler = this.getConnection();
        if (clientPlayNetworkHandler != null && clientPlayNetworkHandler.getConnection().isConnected()) {
            titleBuilder.append(" - ");
            ServerData serverInfo = this.getCurrentServer();
            if (this.singleplayerServer != null && !this.singleplayerServer.isPublished()) {
                titleBuilder.append(I18n.get("title.singleplayer"));
            } else if (serverInfo != null && serverInfo.isRealm()) {
                titleBuilder.append(I18n.get("title.multiplayer.realms"));
            } else if (this.singleplayerServer == null && (serverInfo == null || !serverInfo.isLan())) {
                titleBuilder.append(I18n.get("title.multiplayer.other"));
            } else {
                titleBuilder.append(I18n.get("title.multiplayer.lan"));
            }
        }

        callback.setReturnValue(titleBuilder.toString());
    }

    /**
     * Hook game tick event at HEAD
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void hookTickEvent(CallbackInfo callbackInfo) {
        CoroutineTickerTodoLiquidbounce.INSTANCE.beginMinecraftTick();
        TickLoopTaskExecutorTodoLiquidbounce.INSTANCE.onTickLoopStart();
        CoroutineTickerTodoLiquidbounce.INSTANCE.tick();
        EventManagerTodoLiquidbounce.INSTANCE.callEvent(GameTickEventTodoLiquidbounce.INSTANCE);
    }

    @Inject(method = "tick", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V",
        shift = At.Shift.AFTER
    ))
    private void hookTickLoopCompletedAfterTickEndPacket(CallbackInfo callbackInfo) {
        TickLoopTaskExecutorTodoLiquidbounce.INSTANCE.onTickLoopCompleted();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void fallbackCompleteTickLoop(CallbackInfo callbackInfo) {
        if (TickLoopTaskExecutorTodoLiquidbounce.INSTANCE.isInTickLoop()) {
            TickLoopTaskExecutorTodoLiquidbounce.INSTANCE.onTickLoopCompleted();
        }

        CoroutineTickerTodoLiquidbounce.INSTANCE.endMinecraftTick();
    }

    // codex start
    // /**
    //  * Hook game render task queue event
    //  */
    // @Inject(method = "runTick", at = @At("HEAD"))
    // private void hookRenderTaskQueue(CallbackInfo callbackInfo) {
    //     EventManager.INSTANCE.callEvent(GameRenderTaskQueueEvent.INSTANCE);
    // }
    // codex end

    // codex start
    // @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;runAllTasks()V", shift = At.Shift.BEFORE))
    // private void hookPacketProcess(CallbackInfo callbackInfo) {
    //     EventManager.INSTANCE.callEvent(TickPacketProcessEvent.INSTANCE);
    // }
    // codex end

    // codex start
    // /**
    //  * Hook input handling
    //  */
    // @Inject(method = "handleKeybinds", at = @At("RETURN"))
    // private void hookHandleInputEvent(CallbackInfo callbackInfo) {
    //     EventManager.INSTANCE.callEvent(InputHandleEvent.INSTANCE);
    // }
    // codex end

    // codex start
    // /**
    //  * Hook item use cooldown
    //  */
    // @Inject(method = "startUseItem", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;rightClickDelay:I", shift = At.Shift.AFTER, opcode = Opcodes.PUTFIELD))
    // private void hookItemUseCooldown(CallbackInfo callbackInfo) {
    //     UseCooldownEvent useCooldownEvent = new UseCooldownEvent(rightClickDelay);
    //     EventManager.INSTANCE.callEvent(useCooldownEvent);
    //     rightClickDelay = useCooldownEvent.getCooldown();
    // }
    // codex end

    // codex start
    // @Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
    // private void hookItemPick(CallbackInfo ci) {
    //     if (ModuleMiddleClickAction.Pearl.INSTANCE.cancelPick()) {
    //         ci.cancel();
    //     }
    // }
    //
    // @ModifyExpressionValue(method = "startAttack",
    //         at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;missTime:I", ordinal = 0, opcode = Opcodes.GETFIELD))
    // private int injectNoMissCooldown(int original) {
    //     if (ModuleNoMissCooldown.INSTANCE.getRunning() && ModuleNoMissCooldown.INSTANCE.getRemoveAttackCooldown()) {
    //         return 0;
    //     }
    //
    //     if (ModuleAutoClicker.AttackButton.INSTANCE.getRunning()) {
    //         var clickAmount = ModuleAutoClicker.AttackButton.INSTANCE.getClicker().getClickAmount();
    //         if (clickAmount != null && clickAmount > 0) {
    //             return 0;
    //         }
    //     }
    //
    //     return original;
    // }
    //
    // @ModifyReceiver(
    //     method = "startAttack",
    //     at = @At(
    //         value = "INVOKE",
    //         target = "Lnet/minecraft/world/item/component/AttackRange;isInRange(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;)Z"
    //     )
    // )
    // private AttackRange injectReachAttackRange(AttackRange instance, LivingEntity entity, Vec3 pos) {
    //     if (ModuleReach.INSTANCE.getRunning()) {
    //         return ModuleReach.INSTANCE.getEntity().adjustAttackRange(instance);
    //     }
    //
    //     return instance;
    // }
    //
    // @WrapWithCondition(method = "startAttack", at = @At(value = "FIELD",
    //     target = "Lnet/minecraft/client/Minecraft;missTime:I", ordinal = 1, opcode = Opcodes.PUTFIELD))
    // private boolean disableAttackCooldown(Minecraft instance, int value) {
    //     return !(ModuleNoMissCooldown.INSTANCE.getRunning() && ModuleNoMissCooldown.INSTANCE.getRemoveAttackCooldown());
    // }
    //
    // @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    // private void injectCombatPause(CallbackInfoReturnable<Boolean> cir) {
    //     if (player == null || hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
    //         if (ModuleNoMissCooldown.INSTANCE.getRunning() && ModuleNoMissCooldown.INSTANCE.getCancelAttackOnMiss()) {
    //             // Prevent swinging
    //             cir.setReturnValue(true);
    //         }
    //         return;
    //     }
    //
    //     if (CombatManager.INSTANCE.getShouldPauseCombat()) {
    //         cir.setReturnValue(false);
    //     }
    // }
    //
    // codex end
    @Inject(method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V", at = @At("HEAD"))
    private void hookWorldChangeEvent(ClientLevel world, boolean bl, CallbackInfo ci) {
        EventManagerTodoLiquidbounce.INSTANCE.callEvent(new WorldChangeEventTodoLiquidbounce(world));
    }

    // codex start
    // // codex start
    // // @Inject(method = "renderFrame", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;fps:I",
    // //     ordinal = 0, shift = At.Shift.AFTER, opcode = Opcodes.PUTSTATIC))
    // // private void hookFpsChange(CallbackInfo ci) {
    // //     EventManager.INSTANCE.callEvent(new FpsChangeEvent(this.getFps()));
    // // }
    // // codex end
    // @Inject(method = "onResourceLoadFinished", at = @At("HEAD"))
    // private void onFinishedLoading(CallbackInfo ci) {
    //     EventManager.INSTANCE.callEvent(ResourceReloadEvent.INSTANCE);
    // }
    // codex end

    // codex start
    // @ModifyExpressionValue(method = "continueAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    // private boolean injectMultiActionsBreakingWhileUsing(boolean original) {
    //     return original && !ModuleMultiActions.mayBreakWhileUsing();
    // }
    // codex end

    // codex start
    // @ModifyExpressionValue(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;isDestroying()Z"))
    // private boolean injectMultiActionsPlacingWhileBreaking(boolean original) {
    //     return original && !ModuleMultiActions.mayPlaceWhileBreaking();
    // }
    // codex end

    // codex start
    // /**
    //  * Alternative input handler of [handleInputEvents] while being inside a client-side screen.
    //  */
    // @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;screen()Lnet/minecraft/client/gui/screens/Screen;", ordinal = 1, shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILSOFT)
    // private void passthroughInputHandler(CallbackInfo ci, @Local(name = "profiler") ProfilerFiller profiler) {
    //     if (this.gui.overlay() == null && this.player != null && this.level
    //         != null && ScreenManager.isClientScreen(this.gui.screen())) {
    //         profiler.popPush("Keybindings");
    //
    //         if (ModuleAutoBreak.INSTANCE.getEnabled()) {
    //             this.continueAttack(this.options.keyAttack.isDown());
    //         }
    //     }
    // }
    //
    // codex end
    // codex start
    // @ModifyExpressionValue(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z", ordinal = 0))
    // private boolean injectMultiActionsAttackingWhileUsingAndEnforcedBlockingState(boolean isUsingItem) {
    //     if (isUsingItem) {
    //         if (!this.options.keyUse.isDown() && !(KillAuraAutoBlock.INSTANCE.getRunning() && KillAuraAutoBlock.INSTANCE.getEnforcedBlockingHand() != null)) {
    //             this.gameMode.releaseUsingItem(this.player);
    //         }
    //
    //         if (!ModuleMultiActions.mayAttackWhileUsing()) {
    //             this.options.keyAttack.clickCount = 0;
    //         }
    //
    //         this.options.keyPickItem.clickCount = 0;
    //         this.options.keyUse.clickCount = 0;
    //     }
    //
    //     return false;
    // }
    // codex end

    // codex start
    // @WrapWithCondition(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;missTime:I", ordinal = 0, opcode = Opcodes.PUTFIELD))
    // private boolean injectFixAttackCooldownOnVirtualBrowserScreen(Minecraft instance, int value) {
    //     // Do not reset attack cooldown when we are in the vr/browser screen, as this poses an
    //     // unintended modification to the attack cooldown, which is not intended.
    //     return !ScreenManager.isClientScreen(this.gui.screen());
    // }
    //
    // codex end
    @Inject(method = "clearDownloadedResourcePacks", at = @At("HEAD"))
    private void handleDisconnection(CallbackInfo ci) {
        EventManagerTodoLiquidbounce.INSTANCE.callEvent(DisconnectEventTodoLiquidbounce.INSTANCE);
    // codex start
    // }
    //
    // codex end
    // codex start
    // @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"), cancellable = true)
    // private void hookBlockInteract(CallbackInfo ci) {
    //     final BlockHitResult blockHitResult = (BlockHitResult) this.hitResult;
    //     if (blockHitResult == null) return; // it should never be null
    //
    //     if (ModuleNoBlockInteract.INSTANCE.getRunning() &&
    //             ModuleNoBlockInteract.INSTANCE.shouldSneak(blockHitResult)) {
    //
    //         ModuleNoBlockInteract.INSTANCE.startSneaking();
    //         ci.cancel();
    //     }
    // }
    // codex end
    }

    @Inject(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;submit()V", shift = At.Shift.BEFORE))
    private void endDynamicGpuBufferFrame(boolean advanceGameTime, CallbackInfo ci) {
        MeshDrawTodoLiquidbounce.DefaultUploader.endFrame();
    }

    @Inject(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;endFrame()V", shift = At.Shift.AFTER))
    private void onFlipFrame(boolean advanceGameTime, CallbackInfo ci) {
        RenderingDebugTodoLiquidbounce.flipFrame();
        ClientTesselatorTodoLiquidbounce.Shared.clear();
        StaticGpuBufferPoolTodoLiquidbounce.cleanup();
    }

    // codex start
    // @WrapOperation(method = "pick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;raycastHitResult(FLnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/HitResult;"))
    // private HitResult updateTargetedEntityInvoke(LocalPlayer instance, float a, Entity cameraEntity, Operation<HitResult> original) {
    //     HitResult result;
    //     if (cameraEntity == instance && ModuleFreeCam.shouldCameraInteractActive()) {
    //         final Vec3 position = cameraEntity.position();
    //         final AABB boundingBox = cameraEntity.getBoundingBox();
    //         final Vec3 lastPosition = new Vec3(cameraEntity.xo, cameraEntity.yo, cameraEntity.zo);
    //         final float yRot = cameraEntity.getYRot();
    //         final float xRot = cameraEntity.getXRot();
    //         final float yRot0 = cameraEntity.yRotO;
    //         final float xRot0 = cameraEntity.xRotO;
    //
    //         final Vec3 cameraPosition = ModuleFreeCam.PositionState.pos.subtract(0.0, cameraEntity.getEyeHeight(), 0.0);
    //         ((MixinEntityAccessor) cameraEntity).position(cameraPosition);
    //         cameraEntity.setBoundingBox(boundingBox.move(cameraPosition.subtract(position)));
    //         cameraEntity.xo = ModuleFreeCam.PositionState.lastPos.x;
    //         cameraEntity.yo = ModuleFreeCam.PositionState.lastPos.y - cameraEntity.getEyeHeight();
    //         cameraEntity.zo = ModuleFreeCam.PositionState.lastPos.z;
    //         ((MixinEntityAccessor) cameraEntity).yRot(ModuleFreeCam.PositionState.rot.yRot());
    //         ((MixinEntityAccessor) cameraEntity).xRot(ModuleFreeCam.PositionState.rot.xRot());
    //         cameraEntity.yRotO = ModuleFreeCam.PositionState.lastRot.yRot();
    //         cameraEntity.xRotO = ModuleFreeCam.PositionState.lastRot.xRot();
    //
    //         try {
    //             result = original.call(instance, a, cameraEntity);
    //         } finally {
    //             ((MixinEntityAccessor) cameraEntity).position(position);
    //             cameraEntity.setBoundingBox(boundingBox);
    //             cameraEntity.xo = lastPosition.x;
    //             cameraEntity.yo = lastPosition.y;
    //             cameraEntity.zo = lastPosition.z;
    //             ((MixinEntityAccessor) cameraEntity).yRot(yRot);
    //             ((MixinEntityAccessor) cameraEntity).xRot(xRot);
    //             cameraEntity.yRotO = yRot0;
    //             cameraEntity.xRotO = xRot0;
    //         }
    //     } else {
    //         result = original.call(instance, a, cameraEntity);
    //     }
    //
    //     return result;
    // }
    // codex end
}
