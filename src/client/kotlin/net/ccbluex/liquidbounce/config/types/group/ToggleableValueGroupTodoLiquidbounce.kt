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

package net.ccbluex.liquidbounce.config.types.group

import net.ccbluex.liquidbounce.config.gson.stategies.ExcludeTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.ValueTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.ValueTypeTodoLiquidbounce
import net.ccbluex.liquidbounce.event.EventListenerTodoLiquidbounce
import net.ccbluex.liquidbounce.event.removeEventListenerScope
import net.ccbluex.liquidbounce.features.addon.AddonApiTodoLiquidbounce
import net.ccbluex.liquidbounce.features.misc.ToggleableTodoLiquidbounce
import net.ccbluex.liquidbounce.features.module.MinecraftShortcutsTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.client.logger

/**
 * A [ToggleableValueGroup] has a state that can be toggled on and off. It also allows you
 * to register event handlers that are only active when the state is on,
 * it also features [onEnabled] and [onDisabled] which are called when the state is toggled.
 */
@AddonApiTodoLiquidbounce
abstract class ToggleableValueGroupTodoLiquidbounce @JvmOverloads constructor(
    @ExcludeTodoLiquidbounce val parent: EventListenerTodoLiquidbounce? = null, //codex (@ProtocolExclude)
    name: String,
    enabled: Boolean,
    aliases: List<String> = emptyList(),
) : ValueGroupTodoLiquidbounce(name, valueType = ValueTypeTodoLiquidbounce.TOGGLEABLE, aliases = aliases), EventListenerTodoLiquidbounce, ToggleableTodoLiquidbounce,
    MinecraftShortcutsTodoLiquidbounce {

    @AddonApiTodoLiquidbounce
    @get:JvmName("getEnabledValue")
    val enabledValue: ValueTodoLiquidbounce<Boolean> = boolean("Enabled", enabled)
        .also(::onEnabledValueRegistration)
        .onChange(::onToggled)

    @AddonApiTodoLiquidbounce
    override var enabled by enabledValue

    open fun onEnabledValueRegistration(value: ValueTodoLiquidbounce<Boolean>): ValueTodoLiquidbounce<Boolean> {
        return value
    }

    override fun onToggled(state: Boolean): Boolean {
        if (!inGame) {
            return state
        }

        return onToggled(state, false)
    }

    fun onToggled(state: Boolean, isParentUpdate: Boolean): Boolean {
        // We cannot use [parent.running] because we are interested in the state of the parent,
        // not if it is running. We do not care if we are the root.
        if (!isParentUpdate && parent is ToggleableTodoLiquidbounce && !parent.enabled) {
            return state
        }

        if (!state) {
            runCatching {
                // Remove and cancel coroutine scope
                removeEventListenerScope()
            }.onFailure {
                logger.error("Failed to cancel sequences or remove scope for $this", it)
            }
        }

        val state = super.onToggled(state)
        this@ToggleableValueGroupTodoLiquidbounce.updateChildState(state)
        return state
    }

    /**
     * Because we pass the parent to the Listenable, we can simply
     * call the super.handleEvents() and it will return false if the upper-listenable is disabled.
     */
    override val running: Boolean
        get() = super.running && enabled

    // Declared here so Java subclasses override a plain method, not the interface default.
    override fun onEnabled() = Unit

    override fun onDisabled() = Unit

    final override fun parent() = parent

    protected fun <T : ModeTodoLiquidbounce> choices(name: String, active: T, choices: Array<T>) =
        modes(this, name, active, choices)

    /**
     * The first of [modes] starts active.
     */
    @Suppress("UNCHECKED_CAST")
    protected fun <T : ModeTodoLiquidbounce> choices(name: String, vararg modes: T) =
        modes(this, name, modes[0], modes as Array<T>)

    protected fun <T : ModeTodoLiquidbounce> choices(
        name: String,
        activeIndex: Int = 0,
        choicesCallback: (ModeValueGroupTodoLiquidbounce<T>) -> Array<T>
    ) = modes(this, name, activeIndex, choicesCallback)

}

/**
 * Updates the state of all child [ValueGroup]s.
 *
 * All implementations of [Toggleable] with super class [ValueGroup]
 * should call this function in [Toggleable.onToggled].
 */
private fun ValueGroupTodoLiquidbounce.updateChildState(state: Boolean) {
    for (value in inner) {
        when (value) {
            is ToggleableValueGroupTodoLiquidbounce -> if (state && value.enabled) {
                value.onToggled(state = true, isParentUpdate = true)
            } else if (!state && value.enabled) {
                value.onToggled(state = false, isParentUpdate = true)
            }
            is ModeValueGroupTodoLiquidbounce<*> -> value.updateChildState(state)
            is ValueGroupTodoLiquidbounce -> value.updateChildState(state)
            is ToggleableTodoLiquidbounce -> if (state && value.enabled) {
                value.onToggled(true)
            } else if (!state && value.enabled) {
                value.onToggled(false)
            }
        }
    }
}
