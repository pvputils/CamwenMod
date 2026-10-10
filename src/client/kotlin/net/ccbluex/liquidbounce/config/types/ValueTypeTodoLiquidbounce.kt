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

package net.ccbluex.liquidbounce.config.types

import net.ccbluex.liquidbounce.config.utils.AutoCompletionProviderTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.input.HumanInputDeserializerTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.input.HumanInputDeserializerTodoLiquidbounce.registryItemDeserializer
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries

enum class ValueTypeTodoLiquidbounce(
    val deserializer: HumanInputDeserializerTodoLiquidbounce.StringDeserializer<*>? = null,
    val completer: AutoCompletionProviderTodoLiquidbounce = AutoCompletionProviderTodoLiquidbounce.Default
) {

    // Primitive Types
    BOOLEAN(HumanInputDeserializerTodoLiquidbounce.booleanDeserializer, AutoCompletionProviderTodoLiquidbounce.booleanCompleter),
    FLOAT(HumanInputDeserializerTodoLiquidbounce.floatDeserializer, AutoCompletionProviderTodoLiquidbounce.rangedCompleter),
    FLOAT_RANGE(HumanInputDeserializerTodoLiquidbounce.floatRangeDeserializer, AutoCompletionProviderTodoLiquidbounce.rangedCompleter),
    INT(HumanInputDeserializerTodoLiquidbounce.intDeserializer, AutoCompletionProviderTodoLiquidbounce.rangedCompleter),
    INT_RANGE(HumanInputDeserializerTodoLiquidbounce.intRangeDeserializer, AutoCompletionProviderTodoLiquidbounce.rangedCompleter),
    TEXT(HumanInputDeserializerTodoLiquidbounce.textDeserializer),
    COLOR(HumanInputDeserializerTodoLiquidbounce.colorDeserializer),

    // Registry Types
    BLOCK(registryItemDeserializer(BuiltInRegistries.BLOCK)),
    ITEM(registryItemDeserializer(BuiltInRegistries.ITEM)),
    ENCHANTMENT(registryItemDeserializer(Registries.ENCHANTMENT)),
    SOUND_EVENT(registryItemDeserializer(BuiltInRegistries.SOUND_EVENT)),
    MOB_EFFECT(registryItemDeserializer(BuiltInRegistries.MOB_EFFECT)),
    MENU(registryItemDeserializer(BuiltInRegistries.MENU)),
    ENTITY_TYPE(registryItemDeserializer(BuiltInRegistries.ENTITY_TYPE)),
    C2S_PACKET,
    S2C_PACKET,
    CLIENT_MODULE(HumanInputDeserializerTodoLiquidbounce.clientModuleDeserializer),

    KEY(HumanInputDeserializerTodoLiquidbounce.keyDeserializer),
    FILE(HumanInputDeserializerTodoLiquidbounce.fileDeserializer),
    BIND,
    VECTOR3_I,
    VECTOR3_D,
    VECTOR2_F,

    // Lists
    LIST,
    // todo: rename to CHOICE_LIST
    CHOOSE(completer = AutoCompletionProviderTodoLiquidbounce.choiceListCompleter),
    // todo: rename to MULTI_CHOICE_LIST
    MULTI_CHOOSE(HumanInputDeserializerTodoLiquidbounce.textArrayDeserializer, AutoCompletionProviderTodoLiquidbounce.multiChoiceCompleter),
    MUTABLE_LIST,
    NAMED_ITEM_LIST,
    REGISTRY_LIST,
    REGISTRY_MUTABLE_LIST,
    CURVE,

    // Groups
    // todo: rename to VALUE_GROUP
    CONFIGURABLE,
    // todo: rename to TOGGLEABLE_GROUP
    TOGGLEABLE,
    // todo: rename to MODE_GROUP
    CHOICE(completer = AutoCompletionProviderTodoLiquidbounce.modeGroupCompleter),

    // Client Types
    FRIEND,
    PROXY,
    ACCOUNT,
    SUBSCRIBED_ITEM,

    // Invalid type
    INVALID
}
