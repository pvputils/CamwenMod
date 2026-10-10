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
import net.ccbluex.liquidbounce.render.engine.type.Color4bTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.combat.EntityTargetClassificationTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.combat.EntityTargetingInfoTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.kotlin.PriorityTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.kotlin.PriorityFieldTodoLiquidbounce
import net.minecraft.world.entity.Entity

@AddonApiTodoLiquidbounce
@TagTodoLiquidbounce("attack")
class AttackEntityEventTodoLiquidbounce(
    val entity: Entity
) : CancellableEventTodoLiquidbounce()
// codex start
//
// @Tag("entityMargin")
// class EntityMarginEvent(val entity: Entity, var margin: Float) : Event()
// // codex start
// //
// // @Tag("entityHealthUpdate")
// // class EntityHealthUpdateEvent(val entity: LivingEntity, val old: Float, val new: Float, val max: Float) : Event()
// // codex end
// codex end

@AddonApiTodoLiquidbounce
@TagTodoLiquidbounce("tagEntityEvent")
class TagEntityEventTodoLiquidbounce(val entity: Entity, var targetingInfo: EntityTargetingInfoTodoLiquidbounce) : EventTodoLiquidbounce() {
    val color: PriorityFieldTodoLiquidbounce<Color4bTodoLiquidbounce?> = PriorityFieldTodoLiquidbounce(null, PriorityTodoLiquidbounce.NOT_IMPORTANT)

    /**
     * Don't start combat this target
     */
    fun dontTarget() {
        if (this.targetingInfo.classification == EntityTargetClassificationTodoLiquidbounce.TARGET) {
            this.targetingInfo = this.targetingInfo.copy(classification = EntityTargetClassificationTodoLiquidbounce.INTERESTING)
        }
    }

    /**
     * Fully ignore that target
     */
    fun ignore() {
        this.targetingInfo = targetingInfo.copy(classification = EntityTargetClassificationTodoLiquidbounce.IGNORED)
    }

    fun assumeFriend() {
        this.targetingInfo = targetingInfo.copy(isFriend = true)
    }

    fun color(col: Color4bTodoLiquidbounce, priority: PriorityTodoLiquidbounce) {
        this.color.trySet(col, priority)
    }
}
