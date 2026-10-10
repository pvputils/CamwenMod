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
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.config.types.RangedValueTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.group.ValueGroupTodoLiquidbounce

/**
 * Provides a ranged value to a submodule.
 * This has the advantage that the value can be either registered in the module or in the submodule.
 */
sealed interface RangedValueProviderTodoLiquidbounce {

    /**
     * Offers the provider to register to the configurable.
     *
     * @return The ranged value.
     */
    fun register(offeredValueGroup: ValueGroupTodoLiquidbounce): RangedValueTodoLiquidbounce<*>?

}

/**
 * Just returns the [value]; expects the value to be already registered elsewhere.
 */
class DummyRangedValueProviderTodoLiquidbounce(private val value: RangedValueTodoLiquidbounce<*>) : RangedValueProviderTodoLiquidbounce {

    override fun register(offeredValueGroup: ValueGroupTodoLiquidbounce) = value

}

/**
 * Does nothing; Has no value.
 */
data object NoneRangedValueProviderTodoLiquidbounce : RangedValueProviderTodoLiquidbounce {

    override fun register(offeredValueGroup: ValueGroupTodoLiquidbounce) = null

}

/**
 * [ValueGroup.float] registered to the submodule directly.
 */
// codex start
// class FloatValueProvider(
//     val name: String,
//     val default: Float,
//     val range: ClosedFloatingPointRange<Float>,
//     val suffix: String = ""
// ) : RangedValueProvider {
//
//     override fun register(offeredValueGroup: ValueGroup) : RangedValue<*> {
//         return offeredValueGroup.float(name, default, range, suffix)
//     }
//
// }
// codex end
