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
import net.ccbluex.liquidbounce.config.types.list.TaggedTodoLiquidbounce
import net.ccbluex.liquidbounce.event.CancellableEventTodoLiquidbounce
import net.ccbluex.liquidbounce.features.addon.AddonApiTodoLiquidbounce
import net.minecraft.network.protocol.Packet
// codex start
//
// @Tag("pipeline")
// class PipelineEvent(val channelPipeline: ChannelPipeline, val local: Boolean) : Event()
// codex end

@AddonApiTodoLiquidbounce
@TagTodoLiquidbounce("packet")
class PacketEventTodoLiquidbounce(val origin: TransferOriginTodoLiquidbounce, val packet: Packet<*>, val original: Boolean = true) : CancellableEventTodoLiquidbounce()
// codex start
//
// @Tag("queuePacket")
// class BlinkPacketEvent(
//     val packet: Packet<*>?,
//     val origin: TransferOrigin
// ) : Event() {
//
//     var action: BlinkManager.Action = BlinkManager.Action.FLUSH
//         set(value) {
//             if (field == value || field.priority >= value.priority) {
//                 return
//             }
//
//             field = value
//         }
//
// }
// codex end

@AddonApiTodoLiquidbounce
enum class TransferOriginTodoLiquidbounce(override val tag: String) : TaggedTodoLiquidbounce {
    INCOMING("Incoming"),
    OUTGOING("Outgoing");
}
