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
package net.ccbluex.liquidbounce.utils.entity

import it.unimi.dsi.fastutil.objects.ReferenceArrayList
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.liquidbounce.event.EventListenerTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.GameTickEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.PerspectiveEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.WorldChangeEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.global.GlobalSettingsTargetTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.combat.TargetsTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConventionTodoLiquidbounce.FIRST_PRIORITY
import net.minecraft.world.entity.LivingEntity

private val entities = ReferenceArrayList<LivingEntity>()

/**
 * A readonly [Collection] containing all [LivingEntity] instances that meet the [shouldBeShown] condition.
 *
 * This collection will be auto updated on [GameTickEvent],
 * and be cleared on [WorldChangeEvent] or at the unsubscription of last [EventListener].
 */
object RenderedEntitiesTodoLiquidbounce : Collection<LivingEntity> by entities, EventListenerTodoLiquidbounce {
    private val registry = ReferenceOpenHashSet<EventListenerTodoLiquidbounce>()

    private val onUpdate = ReferenceArrayList<Pair<EventListenerTodoLiquidbounce, Runnable>>()

    context(listener: EventListenerTodoLiquidbounce)
    fun onUpdated(callback: Runnable) {
        onUpdate += listener to callback
    }

    private fun update() {
        onUpdate.removeIf { (listener, _) -> listener !in registry }

        // Callbacks can change subscriptions. Skip removed entries and defer new ones until the next update.
        for (entry in onUpdate.toTypedArray()) {
            if (entry in onUpdate) {
                entry.second.run()
            }
        }
    }

    override val running: Boolean
        get() = registry.isNotEmpty()

    fun subscribe(subscriber: EventListenerTodoLiquidbounce) {
        registry.add(subscriber)
    }

    fun unsubscribe(subscriber: EventListenerTodoLiquidbounce) {
        registry.remove(subscriber)
        onUpdate.removeIf { (listener, _) -> listener === subscriber }
        if (registry.isEmpty()) {
            entities.clear()
            update()
        }
    }

    private fun refresh() {
        entities.clear()

        // codex start
        // val shouldCheckCombineMobs = ModuleCombineMobs.running
        // codex end

        for (entity in mc.level?.entitiesForRendering() ?: return) {
            if (entity is LivingEntity && entity.shouldBeShown()) {
                // codex start
                // if (shouldCheckCombineMobs && ModuleCombineMobs.trackEntity(entity, true)) {
                //     continue
                // }
                // codex end

                entities += entity
            }
        }

        update()
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEventTodoLiquidbounce>(priority = FIRST_PRIORITY) {
        if (inGame) {
            refresh()
        }
    }

    @Suppress("unused")
    private val perspectiveChangeHandler = handler<PerspectiveEventTodoLiquidbounce> {
        if (GlobalSettingsTargetTodoLiquidbounce.visual.contains(TargetsTodoLiquidbounce.SELF)) {
            refresh()
        }
    }

    @Suppress("unused")
    private val worldHandler = handler<WorldChangeEventTodoLiquidbounce> {
        entities.clear()
        update()
    }

}
