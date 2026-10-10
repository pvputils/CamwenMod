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

package net.ccbluex.liquidbounce.event.events

import net.ccbluex.liquidbounce.annotations.TagTodoLiquidbounce
import net.ccbluex.liquidbounce.event.CancellableEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.EventTodoLiquidbounce
import net.ccbluex.liquidbounce.features.addon.AddonApiTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.entity.cameraDistance
import net.ccbluex.liquidbounce.utils.movement.DirectionalInputTodoLiquidbounce
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.entity.Entity

@AddonApiTodoLiquidbounce
@TagTodoLiquidbounce("gameTick")
object GameTickEventTodoLiquidbounce : EventTodoLiquidbounce()

/**
 * We can use this event to populate the render task queue with tasks that should be
 * executed in the same frame. This is useful for more responsive task execution
 * and allows to also schedule tasks off-schedule.
 */
// codex start
// @Tag("gameRenderTaskQueue")
// object GameRenderTaskQueueEvent : Event()
// codex end
// codex start
//
// @Tag("tickPacketProcess")
// object TickPacketProcessEvent : Event()
// codex end
// codex start
//
// @Tag("key")
// class KeyEvent(
//     val key: InputConstants.Key,
//     val action: Int,
// ) : Event(), WebSocketEvent
//
// // Input events
// // codex start
// // @Tag("inputHandle")
// // object InputHandleEvent : Event()
// // codex end
// codex end

@AddonApiTodoLiquidbounce
@TagTodoLiquidbounce("movementInput")
class MovementInputEventTodoLiquidbounce(
    var directionalInput: DirectionalInputTodoLiquidbounce,
    var jump: Boolean,
    var sneak: Boolean,
) : EventTodoLiquidbounce()

@TagTodoLiquidbounce("sprint")
class SprintEventTodoLiquidbounce(
    val directionalInput: DirectionalInputTodoLiquidbounce,
    var sprint: Boolean,
    val source: Source,
) : EventTodoLiquidbounce() {
    enum class Source {
        INPUT,
        MOVEMENT_TICK,
        NETWORK,
    }
}

@TagTodoLiquidbounce("mouseRotation")
class MouseRotationEventTodoLiquidbounce(
    var cursorDeltaX: Double,
    var cursorDeltaY: Double,
) : CancellableEventTodoLiquidbounce()
// codex start
//
// @Tag("keybindChange")
// object KeybindChangeEvent : Event(), WebSocketEvent
// codex end
// codex start
//
// @Tag("keybindIsPressed")
// class KeybindIsPressedEvent(
//     val keyBinding: KeyMapping,
//     var isPressed: Boolean,
// ) : Event()
// codex end
// codex start
//
// @Tag("useCooldown")
// class UseCooldownEvent(
//     var cooldown: Int,
// ) : Event()
// // codex start
// //
// // @Tag("cancelBlockBreaking")
// // class CancelBlockBreakingEvent : CancellableEvent()
// // // codex start
// // //
// // // @Tag("allowAutoJump")
// // // class AllowAutoJumpEvent(
// // //     var isAllowed: Boolean,
// // // ) : Event()
// // //
// // // /**
// // //  * All events which are related to the minecraft client
// // //  */
// // // codex end
// // codex end
// // codex start
// //
// // @Tag("session")
// // class SessionEvent(
// //     val session: User,
// // ) : Event(), WebSocketEvent
// // codex end
// codex end

@AddonApiTodoLiquidbounce
@TagTodoLiquidbounce("screen")
class ScreenEventTodoLiquidbounce(
    val screen: Screen?,
) : CancellableEventTodoLiquidbounce()
// codex start
//
// @AddonApi
// @Tag("chatSend")
// class ChatSendEvent(
//     val message: String,
// ) : CancellableEvent(), WebSocketEvent
// codex end
// codex start
//
// @AddonApi
// @Tag("chatReceive")
// class ChatReceiveEvent(
//     val message: String,
//     val textData: Component,
//     val type: ChatType,
//     @ProtocolExclude
//     val applyChatDecoration: UnaryOperator<Component>,
// ) : CancellableEvent(), WebSocketEvent {
//     @AddonApi
//     enum class ChatType(override val tag: String) : Tagged {
//         CHAT_MESSAGE("ChatMessage"),
//         DISGUISED_CHAT_MESSAGE("DisguisedChatMessage"),
//         GAME_MESSAGE("GameMessage"),
//     }
// }
// codex end
// codex start
//
// @Tag("serverConnect")
// class ServerConnectEvent(
//     val connectScreen: ConnectScreen,
//     val address: ServerAddress,
//     val serverInfo: ServerData,
//     val cookieStorage: TransferState?,
// ) : CancellableEvent()
// codex end

@AddonApiTodoLiquidbounce
@TagTodoLiquidbounce("disconnect")
object DisconnectEventTodoLiquidbounce : EventTodoLiquidbounce() //codex (, WebSocketEvent)
// codex start
//
// @Tag("overlayMessage")
// class OverlayMessageEvent(
//     val text: Component,
//     val tinted: Boolean,
// ) : Event(), WebSocketEvent
// codex end

@TagTodoLiquidbounce("perspective")
object PerspectiveEventTodoLiquidbounce : EventTodoLiquidbounce() {
    var perspective: CameraType = CameraType.FIRST_PERSON
    var distance: Float = 0f
    var noClip: Boolean = false

    var lastPerspective: CameraType = CameraType.FIRST_PERSON
    var lastDistance: Float = 0f

    fun update(mc: Minecraft, entity: Entity?) {
        lastDistance = distance
        lastPerspective = perspective

        perspective = mc.options.cameraType
        noClip = false
        distance = entity.cameraDistance
    }
}
// codex start
//
// @Tag("itemLoreQuery")
// class ItemLoreQueryEvent(
//     val itemStack: ItemStack,
//     val lore: ArrayList<Component>,
// ) : Event()
// codex end
