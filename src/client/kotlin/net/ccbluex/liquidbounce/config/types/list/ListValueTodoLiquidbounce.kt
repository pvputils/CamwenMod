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
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import net.ccbluex.fastutil.enumMapOf
import net.ccbluex.liquidbounce.config.gson.stategies.ExcludeTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.ValueTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.ValueTypeTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.input.HumanInputDeserializerTodoLiquidbounce
import java.util.SequencedSet

open class ListValueTodoLiquidbounce<T : MutableCollection<E>, E>(
    name: String,
    /**
     * Enabled values. A mutable and unordered [Set].
     */
    value: T,

    /**
     * Not the type of [value] but the type of list.
     */
    valueType: ValueTypeTodoLiquidbounce = ValueTypeTodoLiquidbounce.LIST,

    /**
     * Used to determine the type of the inner value.
     */
    @ExcludeTodoLiquidbounce val innerValueType: ValueTypeTodoLiquidbounce = ValueTypeTodoLiquidbounce.INVALID,

    /**
     * Used to deserialize the [value] from JSON.
     * TODO: Might replace [innerType] with a [Class] variable
     *   from the inner value type in the future.
     */
    @ExcludeTodoLiquidbounce val innerType: Class<E>, //codex (@ProtocolExclude)

    ) : ValueTodoLiquidbounce<T>(
    name,
    defaultValue = value,
    valueType = valueType,
) {

    @Suppress("UNCHECKED_CAST")
    final override fun setByString(string: String) {
        val deserializer = this.innerValueType.deserializer

        requireNotNull(deserializer) { "Cannot deserialize values of type ${this.innerValueType} yet." }

        set(HumanInputDeserializerTodoLiquidbounce.parseArray(string, deserializer) as T)
    }

    final override fun deserializeFrom(gson: Gson, element: JsonElement) {
        val currValue = this.inner
        if (element is JsonArray) {
            val newItems = Array(element.size()) {
                gson.fromJson(element[it], this.innerType)
            }
            currValue.clear()
            currValue.addAll(newItems)
        } else {
            val newItem = gson.fromJson(element, this.innerType)
            currValue.clear()
            currValue.add(newItem)
        }

        set(currValue) { /** Trigger listener callbacks */ }
    }

}

/**
 * This allows users to input any kind of [E] value,
 * so it might not deserialize correctly if the input cannot be
 * converted to the [innerType].
 *
 * TODO: Implement support for input validation in the UI.
 */
open class MutableListValueTodoLiquidbounce<T : MutableCollection<E>, E>(
    name: String,
    value: T,
    innerValueType: ValueTypeTodoLiquidbounce = ValueTypeTodoLiquidbounce.INVALID,
    innerType: Class<E>,
) : ListValueTodoLiquidbounce<T, E>(
    name,
    value,
    ValueTypeTodoLiquidbounce.MUTABLE_LIST,
    innerValueType,
    innerType
)

open class ItemListValueTodoLiquidbounce<T : MutableSet<E>, E>(
    name: String,
    value: T,
    @ExcludeTodoLiquidbounce var items: Set<NamedItem<E>>,
    innerValueType: ValueTypeTodoLiquidbounce = ValueTypeTodoLiquidbounce.INVALID,
    innerType: Class<E>,
) : ListValueTodoLiquidbounce<T, E>(
    name,
    value,
    ValueTypeTodoLiquidbounce.NAMED_ITEM_LIST,
    innerValueType,
    innerType
) {

    init {
        require(items.isNotEmpty()) {
            "ItemListValue must have at least one item defined."
        }
    }

    data class NamedItem<T>(
        val name: String,
        val value: T,
        val icon: String? = null
    )

}

/**
 *
 */
class RegistryListValueTodoLiquidbounce<T : SequencedSet<E>, E>(
    name: String,
    value: T,
    innerValueType: ValueTypeTodoLiquidbounce = ValueTypeTodoLiquidbounce.INVALID,
    innerType: Class<E>,
) : ListValueTodoLiquidbounce<T, E>(
    name,
    value,
    ValueTypeTodoLiquidbounce.REGISTRY_LIST,
    innerValueType,
    innerType
) {

    /**
     * This is used to determine the registry endpoint for the API.
     */
    @ExcludeTodoLiquidbounce
    val registry: String =
        VALUE_TYPE_TO_REGISTRY_NAME[innerValueType] ?: error("Unsupported registry type: $innerValueType")

}

private val VALUE_TYPE_TO_REGISTRY_NAME = enumMapOf(
    ValueTypeTodoLiquidbounce.BLOCK, "block",
    ValueTypeTodoLiquidbounce.ITEM, "item",
    ValueTypeTodoLiquidbounce.SOUND_EVENT, "sound_event",
    ValueTypeTodoLiquidbounce.MOB_EFFECT, "mob_effect",
    ValueTypeTodoLiquidbounce.C2S_PACKET, "c2s_packet",
    ValueTypeTodoLiquidbounce.S2C_PACKET, "s2c_packet",
    ValueTypeTodoLiquidbounce.ENTITY_TYPE, "entity_type",
    ValueTypeTodoLiquidbounce.ENCHANTMENT, "enchantment",
    ValueTypeTodoLiquidbounce.MENU, "menu",
    ValueTypeTodoLiquidbounce.CLIENT_MODULE, "client_module",
)


class RegistryMutableListValueTodoLiquidbounce<T : MutableList<E>, E>(
    name: String,
    value: T,
    innerValueType: ValueTypeTodoLiquidbounce = ValueTypeTodoLiquidbounce.INVALID,
    innerType: Class<E>,
) : ListValueTodoLiquidbounce<T, E>(
    name,
    value,
    ValueTypeTodoLiquidbounce.REGISTRY_MUTABLE_LIST,
    innerValueType,
    innerType
) {

    /**
     * This is used to determine the registry endpoint for the API.
     */
    @ExcludeTodoLiquidbounce
    val registry: String =
        VALUE_TYPE_TO_REGISTRY_NAME[innerValueType] ?: error("Unsupported registry type: $innerValueType")

}
