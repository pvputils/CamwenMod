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

package net.ccbluex.liquidbounce.config.types.list

import com.google.gson.Gson
import com.google.gson.JsonElement
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.gson.stategies.ExcludeTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.ValueTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.ValueTypeTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.list.TaggedTodoLiquidbounce.Companion.makeLookupTable
import net.ccbluex.liquidbounce.features.addon.AddonApiTodoLiquidbounce
import java.util.SortedMap

class ChoiceListValueTodoLiquidbounce<T : TaggedTodoLiquidbounce>(
    name: String,
    aliases: List<String> = emptyList(),
    defaultValue: T,
    @ExcludeTodoLiquidbounce val choices: Set<T>
) : ValueTodoLiquidbounce<T>(name, aliases, defaultValue, ValueTypeTodoLiquidbounce.CHOOSE) {

    init {
        require(defaultValue in choices) { "default value must be in [${choices}]" }
    }

    @ExcludeTodoLiquidbounce //codex (@ProtocolExclude)
    private val choiceByName = choices.makeLookupTable()

    override fun deserializeFrom(gson: Gson, element: JsonElement) {
        val name = element.asString

        setByString(name)
    }

    override fun setByString(string: String) {
        val newValue = choiceByName[string] ?: throw IllegalArgumentException(
            "ChoiceListValue `${this.name}` has no option named $string" +
                " (available options are ${this.choices.joinToString { it.tag }})"
        )

        set(newValue)
    }

    @AddonApiTodoLiquidbounce
    fun getChoicesStrings(): Array<String> {
        return choices.mapToArray { it.tag }
    }

}

interface TaggedTodoLiquidbounce {
    val tag: String

    val tagAliases: List<String> get() = emptyList()

    companion object {
        @JvmStatic
        fun <T : TaggedTodoLiquidbounce> Iterable<T>.makeLookupTable(): SortedMap<String, T> {
            val map = Object2ObjectRBTreeMap<String, T>(String.CASE_INSENSITIVE_ORDER)
            for (item in this) {
                if (map.put(item.tag, item) != null) {
                    throw IllegalArgumentException("Duplicate tag: ${item.tag}")
                }
                for (alias in item.tagAliases) {
                    if (map.put(alias, item) != null) {
                        throw IllegalArgumentException("Duplicate alias: $alias")
                    }
                }
            }
            return map
        }

        @JvmName("of")
        @JvmStatic
        fun String.asTagged(): TaggedTodoLiquidbounce = object : TaggedTodoLiquidbounce, Comparable<TaggedTodoLiquidbounce> {
            override val tag get() = this@asTagged

            override fun equals(other: Any?): Boolean =
                when (other) {
                    is TaggedTodoLiquidbounce -> other.tag == this.tag
                    is CharSequence -> this.tag == other
                    is Enum<*> -> this.tag == other.name
                    else -> false
                }

            override fun hashCode(): Int = this.tag.hashCode()

            override fun toString(): String = this.tag

            override fun compareTo(other: TaggedTodoLiquidbounce): Int = this.tag.compareTo(other.tag)
        }
    }
}
