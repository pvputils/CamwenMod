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
package net.ccbluex.liquidbounce.event

import it.unimi.dsi.fastutil.objects.Object2ReferenceRBTreeMap
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import net.ccbluex.liquidbounce.event.events.AttackEntityEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.ClientShutdownEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.ClientStartEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.DisconnectEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.GameRenderEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.GameTickEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.MouseButtonEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.MouseRotationEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.MovementInputEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.OverlayRenderEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.PacketEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.PerspectiveEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.PlayerMoveEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.PlayerSneakMultiplierTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.PlayerUseMultiplierTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.PlayerVelocityStrafeTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.RotationUpdateEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.ScreenEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.SprintEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.TagEntityEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.WorldChangeEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.WorldRenderEventTodoLiquidbounce
import net.ccbluex.liquidbounce.annotations.TagTodoLiquidbounce
import net.ccbluex.liquidbounce.features.addon.AddonApiTodoLiquidbounce
import net.ccbluex.liquidbounce.features.misc.SelfDestructTodoLiquidbounce.isDestructed
import net.ccbluex.liquidbounce.utils.client.error.ErrorHandlerTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.ReportedException

/**
 * Contains all classes of events. Used to create lookup tables ahead of time
 */
@JvmField
internal val ALL_EVENT_CLASSES: Array<Class<out EventTodoLiquidbounce>> = arrayOf(
    GameTickEventTodoLiquidbounce::class.java,
    // codex start
    // GameRenderTaskQueueEvent::class.java,
    // codex end
    // codex start
    // TickPacketProcessEvent::class.java,
    // codex end
    // codex start
    // BlockChangeEvent::class.java,
    // codex end
    // codex start
    // ChunkLoadEvent::class.java,
    // codex end
    // codex start
    // ChunkDeltaUpdateEvent::class.java,
    // codex end
    // codex start
    // ChunkUnloadEvent::class.java,
    // codex end
    DisconnectEventTodoLiquidbounce::class.java,
    GameRenderEventTodoLiquidbounce::class.java,
    // codex start
    // WorldFeatureSubmitEvent::class.java,
    // codex end
    WorldRenderEventTodoLiquidbounce::class.java,
    OverlayRenderEventTodoLiquidbounce::class.java,
    // codex start
    // ScreenRenderEvent::class.java,
    // WindowResizeEvent::class.java,
    // FramebufferResizeEvent::class.java,
    // codex end
    // codex start
    // WindowTitleEvent::class.java,
    // codex end
    MouseButtonEventTodoLiquidbounce::class.java,
    // codex start
    // MouseScrollEvent::class.java,
    // codex end
    // codex start
    // MouseCursorEvent::class.java,
    // codex end
    KeyboardKeyEventTodoLiquidbounce::class.java,
    // codex start
    // KeyboardCharEvent::class.java,
    // codex end
    // codex start
    // InputHandleEvent::class.java,
    // codex end
    MovementInputEventTodoLiquidbounce::class.java,
    SprintEventTodoLiquidbounce::class.java,
    // codex start
    // KeyEvent::class.java,
    // codex end
    MouseRotationEventTodoLiquidbounce::class.java,
    // codex start
    // KeybindChangeEvent::class.java,
    // codex end
    // codex start
    // KeybindIsPressedEvent::class.java,
    // codex end
    AttackEntityEventTodoLiquidbounce::class.java,
    // codex start
    // SessionEvent::class.java,
    // codex end
    ScreenEventTodoLiquidbounce::class.java,
    // codex start
    // ChatSendEvent::class.java,
    // ChatReceiveEvent::class.java,
    // codex end
    // codex start
    // UseCooldownEvent::class.java,
    // codex end
    // codex start
    // BlockShapeEvent::class.java,
    // codex end
    // codex start
    // BlockBreakingProgressEvent::class.java,
    // codex end
    // codex start
    // BlockVelocityMultiplierEvent::class.java,
    // BlockSlipperinessMultiplierEvent::class.java,
    // codex end
    // codex start
    // EntityMarginEvent::class.java,
    // codex end
    // codex start
    // EntityHealthUpdateEvent::class.java,
    // codex end
    // codex start
    // HealthUpdateEvent::class.java,
    // codex end
    // codex start
    // DeathEvent::class.java,
    // codex end
    // codex start
    // PlayerTickEvent::class.java,
    // codex end
    // codex start
    // PlayerPostTickEvent::class.java,
    // codex end
    // codex start
    // PlayerMovementTickEvent::class.java,
    // codex end
    // codex start
    // PlayerNetworkMovementTickEvent::class.java,
    // codex end
    // codex start
    // PlayerPushOutEvent::class.java,
    // codex end
    PlayerMoveEventTodoLiquidbounce::class.java,
    // codex start
    // PlayerJumpEvent::class.java,
    // codex end
    // codex start
    // PlayerAfterJumpEvent::class.java,
    // codex end
    PlayerUseMultiplierTodoLiquidbounce::class.java,
    // codex start
    // PlayerInteractItemEvent::class.java,
    // codex end
    // codex start
    // PlayerInteractedItemEvent::class.java,
    // codex end
    // codex start
    // ClientPlayerInventoryEvent::class.java,
    // codex end
    PlayerVelocityStrafeTodoLiquidbounce::class.java,
    // codex start
    // PlayerStrideEvent::class.java,
    // codex end
    PlayerSafeWalkEventTodoLiquidbounce::class.java,
    // codex start
    // CancelBlockBreakingEvent::class.java,
    // codex end
    // codex start
    // PlayerStepEvent::class.java,
    // codex end
    // codex start
    // PlayerStepSuccessEvent::class.java,
    // codex end
    // codex start
    // FluidPushEvent::class.java,
    // codex end
    // codex start
    // PipelineEvent::class.java,
    // codex end
    PacketEventTodoLiquidbounce::class.java,
    ClientStartEventTodoLiquidbounce::class.java,
    ClientShutdownEventTodoLiquidbounce::class.java,
    // codex start
    // ClientLanguageChangedEvent::class.java,
    // codex end
    // codex start
    // ValueChangedEvent::class.java,
    // codex end
    // codex start
    // ModuleActivationEvent::class.java,
    // codex end
    // codex start
    // ModuleToggleEvent::class.java,
    // codex end
    // codex start
    // FriendChangeEvent::class.java,
    // codex end
    // codex start
    // NotificationEvent::class.java,
    // codex end
    // codex start
    // ClientChatStateChange::class.java,
    // ClientChatMessageEvent::class.java,
    // ClientChatErrorEvent::class.java,
    // ClientChatJwtTokenEvent::class.java,
    // codex end
    WorldChangeEventTodoLiquidbounce::class.java,
    // codex start
    // AccountManagerMessageEvent::class.java,
    // AccountManagerAdditionResultEvent::class.java,
    // AccountManagerRemovalResultEvent::class.java,
    // AccountManagerLoginResultEvent::class.java,
    // VirtualScreenEvent::class.java,
    // codex end
    // codex start
    // FpsChangeEvent::class.java,
    // codex end
    // codex start
    // FpsLimitEvent::class.java,
    // ClientPlayerDataEvent::class.java,
    // ClientPlayerEffectEvent::class.java,
    // codex end
    RotationUpdateEventTodoLiquidbounce::class.java,
    // codex start
    // RefreshArrayListEvent::class.java,
    // codex end
    // codex start
    // BrowserReadyEvent::class.java,
    // ServerConnectEvent::class.java,
    // ServerPingedEvent::class.java,
    // TargetChangeEvent::class.java,
    // BlockCountChangeEvent::class.java,
    // BedStateChangeEvent::class.java,
    // codex end
    // codex start
    // GameModeChangeEvent::class.java,
    // codex end
    // codex start
    // ComponentsUpdateEvent::class.java,
    // codex end
    // codex start
    // ResourceReloadEvent::class.java,
    // codex end
    // codex start
    // ProxyCheckResultEvent::class.java,
    // ScaleFactorChangeEvent::class.java,
    // codex end
    // codex start
    // DrawOutlinesEvent::class.java,
    // codex end
    // codex start
    // OverlayMessageEvent::class.java,
    // codex end
    // codex start
    // ScheduleInventoryActionEvent::class.java,
    // codex end
    // codex start
    // SelectHotbarSlotSilentlyEvent::class.java,
    // codex end
    // codex start
    // SpaceSeperatedNamesChangeEvent::class.java,
    // ClickGuiScaleChangeEvent::class.java,
    // ThemeColorChangeEvent::class.java,
    // BrowserUrlChangeEvent::class.java,
    // codex end
    TagEntityEventTodoLiquidbounce::class.java,
    // codex start
    // MouseScrollInHotbarEvent::class.java,
    // codex end
    // codex start
    // PlayerFluidCollisionCheckEvent::class.java,
    // codex end
    // codex start
    // PlayerContainerInputEvent::class.java,
    // codex end
    PlayerSneakMultiplierTodoLiquidbounce::class.java,
    PerspectiveEventTodoLiquidbounce::class.java,
    // codex start
    // ItemLoreQueryEvent::class.java,
    // codex end
    // codex start
    // EntityEquipmentChangeEvent::class.java,
    // codex end
    // codex start
    // ClickGuiValueChangeEvent::class.java,
    // codex end
    // codex start
    // BlockAttackEvent::class.java,
    // codex end
    // codex start
    // BlinkPacketEvent::class.java,
    // codex end
    // codex start
    // AllowAutoJumpEvent::class.java,
    // codex end
    // codex start
    // WorldEntityRemoveEvent::class.java,
    // codex end
    // codex start
    // TitleEvent.Title::class.java,
    // TitleEvent.Subtitle::class.java,
    // TitleEvent.Fade::class.java,
    // TitleEvent.Clear::class.java,
    // codex end
    // codex start
    // ClosedCaptionsEvent::class.java,
    // UserLoggedInEvent::class.java,
    // UserLoggedOutEvent::class.java,
    // codex end
)

inline fun <reified E : EventTodoLiquidbounce> eventFlow(): SharedFlow<E> =
    EventManagerTodoLiquidbounce.eventFlow(E::class.java)

/**
 * Swapped as one object, so readers never see the tables disagree.
 */
private class EventTablesTodoLiquidbounce(@JvmField val classes: Set<Class<out EventTodoLiquidbounce>>, previous: EventTablesTodoLiquidbounce?) {

    @JvmField
    val registry: Map<Class<out EventTodoLiquidbounce>, EventHookRegistryTodoLiquidbounce<in EventTodoLiquidbounce>> = classes.associateWithTo(
        Reference2ObjectOpenHashMap(classes.size)
    ) { previous?.registry?.get(it) ?: EventHookRegistryTodoLiquidbounce() }

    @JvmField
    val flows: Map<Class<out EventTodoLiquidbounce>, MutableSharedFlow<EventTodoLiquidbounce>> = classes.associateWithTo(
        Reference2ObjectOpenHashMap(classes.size)
    ) { previous?.flows?.get(it) ?: MutableSharedFlow(replay = 0, extraBufferCapacity = 0) }

    @JvmField
    val classToName: Map<Class<out EventTodoLiquidbounce>, String> =
        Reference2ObjectOpenHashMap<Class<out EventTodoLiquidbounce>, String>(classes.size).apply {
            classes.forEach { eventClass ->
                eventClass.getAnnotation(TagTodoLiquidbounce::class.java)?.let { put(eventClass, it.name) }
            }
        }

    @JvmField
    val nameToClass: Map<String, Class<out EventTodoLiquidbounce>> =
        Object2ReferenceRBTreeMap<String, Class<out EventTodoLiquidbounce>>(String.CASE_INSENSITIVE_ORDER).apply {
            classToName.forEach { (eventClass, name) -> put(name, eventClass) }
        }

}

/**
 * A modern and fast event handler using lambda handlers
 */
@AddonApiTodoLiquidbounce
object EventManagerTodoLiquidbounce {

    @Volatile
    private var tables = EventTablesTodoLiquidbounce(ALL_EVENT_CLASSES.toCollection(LinkedHashSet()), previous = null)

    val knownEventClasses: Set<Class<out EventTodoLiquidbounce>>
        get() = tables.classes

    /**
     * Looks up by [Tag] name, ignoring case.
     */
    fun eventClassByName(name: String): Class<out EventTodoLiquidbounce>? = tables.nameToClass[name]

    internal fun eventNameOrNull(eventClass: Class<out EventTodoLiquidbounce>): String? = tables.classToName[eventClass]

    @Synchronized
    fun registerEventClass(eventClass: Class<out EventTodoLiquidbounce>): Boolean {
        val current = tables
        if (eventClass in current.classes) {
            return false
        }

        eventClass.getAnnotation(TagTodoLiquidbounce::class.java)?.let { tag ->
            val owner = current.nameToClass[tag.name]
            require(owner == null) {
                "Event name '${tag.name}' is already taken by ${owner!!.name}, " +
                    "cannot register ${eventClass.name}"
            }
        }

        tables = EventTablesTodoLiquidbounce(LinkedHashSet(current.classes).apply { add(eventClass) }, current)
        return true
    }

    private fun tablesContaining(eventClass: Class<out EventTodoLiquidbounce>): EventTablesTodoLiquidbounce {
        val current = tables
        if (eventClass in current.classes) {
            return current
        }

        registerEventClass(eventClass)
        return tables
    }

    /**
     * Used by handler methods
     */
    fun <T : EventTodoLiquidbounce> registerEventHook(eventClass: Class<out EventTodoLiquidbounce>, eventHook: EventHookTodoLiquidbounce<T>): EventHookTodoLiquidbounce<T> {
        val handlers = tablesContaining(eventClass).registry.getValue(eventClass)

        @Suppress("UNCHECKED_CAST")
        val hook = eventHook as EventHookTodoLiquidbounce<in EventTodoLiquidbounce>

        handlers.addIfAbsent(hook)

        return eventHook
    }

    /**
     * Unregisters a handler.
     */
    fun <T : EventTodoLiquidbounce> unregisterEventHook(eventClass: Class<out EventTodoLiquidbounce>, eventHook: EventHookTodoLiquidbounce<T>) {
        @Suppress("UNCHECKED_CAST")
        tables.registry[eventClass]?.remove(eventHook as EventHookTodoLiquidbounce<in EventTodoLiquidbounce>)
    }

    fun unregisterEventHandler(eventListener: EventListenerTodoLiquidbounce) {
        tables.registry.values.forEach {
            it.remove(eventListener)
        }
    }

    fun unregisterAll() {
        tables.registry.values.forEach {
            it.clear()
        }
    }

    /**
     * Call event to listeners
     *
     * @param event to call
     */
    fun <T : EventTodoLiquidbounce> callEvent(event: T): T {
        if (isDestructed) {
            return event
        }

        val eventType = event.javaClass
        val snapshot = tables
        val target = snapshot.registry[eventType] ?: return event

        event.isCompleted = false
        for (eventHook in target.snapshot) {
            @Suppress("UNCHECKED_CAST")
            eventHook as EventHookTodoLiquidbounce<T>
            if (!eventHook.handlerClass.running) {
                continue
            }

            try {
                eventHook.handler.accept(event)
            } catch (e: ReportedException) {
                ErrorHandlerTodoLiquidbounce.fatal(
                    error = e,
                    needToReport = true,
                    additionalMessage = "Event (${eventType.simpleName}) handler of ${eventHook.handlerClass}"
                )
            } catch (e: Throwable) {
                logger.error(
                    "Exception while executing event handler of {}, event={}",
                    eventHook.handlerClass.javaClass.simpleName,
                    event,
                    e,
                )
            }
        }
        event.isCompleted = true

        @Suppress("UNCHECKED_CAST")
        (snapshot.flows.getValue(eventType) as MutableSharedFlow<T>).tryEmit(event)

        return event
    }

    /**
     * Gets a [SharedFlow] for the given event class.
     * The flow receives the event instances after all [EventHook]s are executed.
     * So the [Event.isCompleted] will be true when the event is emitted.
     */
    fun <T : EventTodoLiquidbounce> eventFlow(eventClass: Class<T>): SharedFlow<T> {
        @Suppress("UNCHECKED_CAST")
        return tablesContaining(eventClass).flows.getValue(eventClass) as SharedFlow<T>
    }
}
