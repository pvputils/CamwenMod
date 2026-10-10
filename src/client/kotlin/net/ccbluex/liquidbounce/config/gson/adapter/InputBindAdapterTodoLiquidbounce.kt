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
package net.ccbluex.liquidbounce.config.gson.adapter

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.config.gson.util.array
import net.ccbluex.liquidbounce.config.gson.util.string
import net.ccbluex.liquidbounce.config.types.list.TaggedTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.input.InputBindTodoLiquidbounce
import java.lang.reflect.Type

object InputBindAdapterTodoLiquidbounce : JsonSerializer<InputBindTodoLiquidbounce>, JsonDeserializer<InputBindTodoLiquidbounce> {

    override fun serialize(src: InputBindTodoLiquidbounce, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonObject().apply {
            add("boundKey", context.serialize(src.boundKey, InputConstants.Key::class.java))
            add("action", context.serialize(src.action, TaggedTodoLiquidbounce::class.java))
            if (src.modifiers.isNotEmpty()) {
                add("modifiers", context.serialize(src.modifiers))
            }
        }
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): InputBindTodoLiquidbounce {
        if (json.isJsonPrimitive) {
            val primitive = json.asJsonPrimitive

            // We do not want to throw an error but simply unbind the key instead.
            if (!primitive.isNumber) {
                return InputBindTodoLiquidbounce(InputConstants.UNKNOWN, InputBindTodoLiquidbounce.BindAction.TOGGLE, emptySet())
            }

            // Bind Action goes missing as we cannot access the action that is located
            // one element above - Sorry!
            return InputBindTodoLiquidbounce(
                InputConstants.Type.KEYSYM.getOrCreate(primitive.asInt),
                InputBindTodoLiquidbounce.BindAction.TOGGLE,
                emptySet(),
            )
        }

        val jsonObject = json.asJsonObject
        val boundKey = context.deserialize<InputConstants.Key>(
            jsonObject.get("boundKey"),
            InputConstants.Key::class.java
        )
        val action = InputBindTodoLiquidbounce.BindAction.of(jsonObject.string("action")) ?: InputBindTodoLiquidbounce.BindAction.TOGGLE
        val modifierSet = jsonObject.array("modifiers")?.mapNotNullTo(enumSetOf<InputBindTodoLiquidbounce.Modifier>()) { element ->
            InputBindTodoLiquidbounce.Modifier.of(element.asString)
        }.orEmpty()

        return InputBindTodoLiquidbounce(boundKey, action, modifierSet)
    }

}
