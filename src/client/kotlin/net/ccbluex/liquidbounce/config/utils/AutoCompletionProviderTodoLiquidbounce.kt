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

package net.ccbluex.liquidbounce.config.utils

import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.types.RangedValueTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.ValueTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroupTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.list.ChoiceListValueTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValueTodoLiquidbounce

fun interface AutoCompletionProviderTodoLiquidbounce {

    /**
     * Gives an array with all possible completions for the [value].
     */
    fun possible(value: ValueTodoLiquidbounce<*>): Iterable<String>

    companion object Default : AutoCompletionProviderTodoLiquidbounce {
        override fun possible(value: ValueTodoLiquidbounce<*>): Iterable<String> = emptyList()

        @JvmStatic
        fun ofConst(strings: List<String>): AutoCompletionProviderTodoLiquidbounce {
            return AutoCompletionProviderTodoLiquidbounce { strings }
        }

        @JvmField
        val booleanCompleter = ofConst(listOf("true", "false"))

        @JvmField
        val rangedCompleter = AutoCompletionProviderTodoLiquidbounce { value ->
            val range = (value as RangedValueTodoLiquidbounce<*>).range
            listOf(range.start.toString(), range.endInclusive.toString())
        }

        @JvmField
        val modeGroupCompleter = AutoCompletionProviderTodoLiquidbounce { value ->
            (value as ModeValueGroupTodoLiquidbounce<*>).modes.mapToArray { it.tag }.asList()
        }

        @JvmField
        val choiceListCompleter = AutoCompletionProviderTodoLiquidbounce { value ->
            (value as ChoiceListValueTodoLiquidbounce<*>).choices.mapToArray { it.tag }.asList()
        }

        @JvmField
        val multiChoiceCompleter = AutoCompletionProviderTodoLiquidbounce { value ->
            (value as MultiChoiceListValueTodoLiquidbounce<*>).choices.mapToArray { it.tag }.asList()
        }
    }

}
