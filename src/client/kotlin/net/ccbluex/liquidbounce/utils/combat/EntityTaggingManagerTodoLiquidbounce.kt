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

package net.ccbluex.liquidbounce.utils.combat

import net.ccbluex.liquidbounce.event.EventListenerTodoLiquidbounce
import net.ccbluex.liquidbounce.event.EventManagerTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.GameTickEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.TagEntityEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.engine.type.Color4bTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConventionTodoLiquidbounce.FIRST_PRIORITY
import net.minecraft.world.entity.Entity
import java.util.concurrent.ConcurrentHashMap

object EntityTaggingManagerTodoLiquidbounce: EventListenerTodoLiquidbounce {
    private val cache = ConcurrentHashMap<Entity, EntityTagTodoLiquidbounce>()

    @Suppress("unused")
    val tickHandler = handler<GameTickEventTodoLiquidbounce>(priority = FIRST_PRIORITY) {
        cache.clear()
    }

    fun getTag(suspect: Entity): EntityTagTodoLiquidbounce {
        return this.cache.computeIfAbsent(suspect) {
            val targetingInfo = TagEntityEventTodoLiquidbounce(it, EntityTargetingInfoTodoLiquidbounce.DEFAULT)

            EventManagerTodoLiquidbounce.callEvent(targetingInfo)

            return@computeIfAbsent EntityTagTodoLiquidbounce(targetingInfo.targetingInfo, targetingInfo.color.value)
        }
    }

}

class EntityTagTodoLiquidbounce(
    val targetingInfo: EntityTargetingInfoTodoLiquidbounce,
    val color: Color4bTodoLiquidbounce?
)
