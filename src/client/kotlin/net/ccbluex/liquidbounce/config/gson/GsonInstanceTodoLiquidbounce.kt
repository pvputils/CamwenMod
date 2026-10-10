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

package net.ccbluex.liquidbounce.config.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.config.gson.adapter.CodecBasedAdapterTodoLiquidbounce
import net.ccbluex.liquidbounce.config.gson.adapter.ColorAdapterTodoLiquidbounce
import net.ccbluex.liquidbounce.config.gson.adapter.IdentifierWithRegistryAdapterTodoLiquidbounce
import net.ccbluex.liquidbounce.config.gson.adapter.InputBindAdapterTodoLiquidbounce
import net.ccbluex.liquidbounce.config.gson.adapter.InstantAdapter
import net.ccbluex.liquidbounce.config.gson.adapter.IntRangeAdapterTodoLiquidbounce
import net.ccbluex.liquidbounce.config.gson.adapter.LocalDateAdapter
import net.ccbluex.liquidbounce.config.gson.adapter.LocalDateTimeAdapter
import net.ccbluex.liquidbounce.config.gson.adapter.OffsetDateTimeAdapter
import net.ccbluex.liquidbounce.config.gson.adapter.OptionalAdapterTodoLiquidbounce
import net.ccbluex.liquidbounce.config.gson.adapter.RangeAdapterTodoLiquidbounce
import net.ccbluex.liquidbounce.config.gson.adapter.SimpleStringTypeAdapterTodoLiquidbounce
import net.ccbluex.liquidbounce.config.gson.adapter.Vec2fAdapterTodoLiquidbounce
import net.ccbluex.liquidbounce.config.gson.adapter.Vec3dAdapterTodoLiquidbounce
import net.ccbluex.liquidbounce.config.gson.adapter.Vec3iAdapterTodoLiquidbounce
import net.ccbluex.liquidbounce.config.gson.adapter.Vector2fcAdapterTodoLiquidbounce
import net.ccbluex.liquidbounce.config.gson.serializer.ModeValueGroupSerializerTodoLiquidbounce
import net.ccbluex.liquidbounce.config.gson.serializer.SupplierSerializerTodoLiquidbounce
import net.ccbluex.liquidbounce.config.gson.serializer.TaggedSerializerTodoLiquidbounce
import net.ccbluex.liquidbounce.config.gson.serializer.ValueGroupSerializerTodoLiquidbounce
import net.ccbluex.liquidbounce.config.gson.serializer.minecraft.ItemStackSerializerTodoLiquidbounce
import net.ccbluex.liquidbounce.config.gson.serializer.minecraft.StatusEffectInstanceSerializerTodoLiquidbounce
import net.ccbluex.liquidbounce.config.gson.serializer.minecraft.StringRepresentableSerializerTodoLiquidbounce
import net.ccbluex.liquidbounce.config.gson.stategies.ExcludeStrategyTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroupTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.group.ValueGroupTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.list.TaggedTodoLiquidbounce
import net.ccbluex.liquidbounce.render.engine.type.Color4bTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.input.InputBindTodoLiquidbounce
import net.minecraft.core.Vec3i
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.util.StringRepresentable
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.EntityType
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.joml.Vector2fc
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.function.Supplier

/**
 * A GSON instance which is used for local files.
 */
val fileGson: Gson = GsonBuilder()
    .addSerializationExclusionStrategy(ExcludeStrategyTodoLiquidbounce)
    .registerCommonTypeAdapters()
    .registerTypeAdapter(ModeValueGroupTodoLiquidbounce::class.java, ModeValueGroupSerializerTodoLiquidbounce.FILE_SERIALIZER)
    .registerTypeHierarchyAdapter(ValueGroupTodoLiquidbounce::class.java, ValueGroupSerializerTodoLiquidbounce.FILE_SERIALIZER)
    .create()

/**
 * A GSON instance which is used for JSON that is distributed to other players.
 */
val publicGson: Gson = GsonBuilder()
    .setPrettyPrinting()
    .addSerializationExclusionStrategy(ExcludeStrategyTodoLiquidbounce)
    .registerCommonTypeAdapters()
    .registerTypeAdapter(ModeValueGroupTodoLiquidbounce::class.java, ModeValueGroupSerializerTodoLiquidbounce.PUBLIC_CONFIG_SERIALIZER)
    .registerTypeHierarchyAdapter(ValueGroupTodoLiquidbounce::class.java, ValueGroupSerializerTodoLiquidbounce.PUBLIC_SERIALIZER)
    .create()

/**
 * This GSON instance is used for interop communication.
 */

// codex start
// internal val interopGson: Gson = GsonBuilder()
//     .addSerializationExclusionStrategy(ProtocolExcludeStrategy)
//     .registerCommonTypeAdapters()
//     .registerTypeAdapter(ModeValueGroup::class.java, ModeValueGroupSerializer.INTEROP_SERIALIZER)
//     .registerTypeHierarchyAdapter(ValueGroup::class.java, ValueGroupSerializer.INTEROP_SERIALIZER)
//     .create()
//
// /**
//  * This GSON instance is used for serializing objects as accessible JSON which means it is READ-ONLY (!)
//  * and often comes with an easier syntax to use in other programming languages like JavaScript.
//  */
// // codex start
// // internal val accessibleInteropGson: Gson = GsonBuilder()
// //     .addSerializationExclusionStrategy(ProtocolExcludeStrategy)
// //     .registerCommonTypeAdapters()
// //     .registerTypeAdapter(ModeValueGroup::class.java, ModeValueGroupSerializer.INTEROP_SERIALIZER)
// //     .registerTypeHierarchyAdapter(ValueGroup::class.java, ValueGroupSerializer.INTEROP_SERIALIZER)
// //     // codex start
// //     // .registerTypeHierarchyAdapter(Theme::class.javaObjectType, ReadOnlyThemeSerializer)
// //     // .registerTypeHierarchyAdapter(HudComponent::class.javaObjectType, ReadOnlyComponentSerializer)
// //     // .registerTypeHierarchyAdapter(Alignment::class.javaObjectType, AlignmentAdapter)
// //     // codex end
// //     .create()
// //
// // /**
// //  * Register common type adapters
// //  * These adapters include anything from Kotlin classes to Minecraft and LiquidBounce types
// //  * They are safe to use on any GSON instance. (clientGson, autoConfigGson, ...)
// //  * It does not include any configurable serializers, which means you need to add them yourself if needed!
// //  *
// //  * @see GsonBuilder.registerTypeHierarchyAdapter
// //  * @see GsonBuilder.registerTypeAdapter
// //  */
// // codex end
// codex end

private fun GsonBuilder.registerCommonTypeAdapters() =
    registerTypeAdapter(LocalDate::class.java, LocalDateAdapter)
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter)
        .registerTypeAdapter(OffsetDateTime::class.java, OffsetDateTimeAdapter)
        .registerTypeAdapter(Instant::class.java, InstantAdapter)
        .registerTypeAdapter(Regex::class.java, SimpleStringTypeAdapterTodoLiquidbounce.KT_REGEX)
        .registerTypeHierarchyAdapter(ClosedRange::class.javaObjectType, RangeAdapterTodoLiquidbounce)
        .registerTypeHierarchyAdapter(IntRange::class.javaObjectType, IntRangeAdapterTodoLiquidbounce)
        .registerTypeHierarchyAdapter(File::class.java, SimpleStringTypeAdapterTodoLiquidbounce.FILE)
        .registerTypeHierarchyAdapter(EntityType::class.java, IdentifierWithRegistryAdapterTodoLiquidbounce.ENTITY_TYPE)
        .registerTypeHierarchyAdapter(Item::class.javaObjectType, IdentifierWithRegistryAdapterTodoLiquidbounce.ITEM)
        .registerTypeAdapter(DataComponentPatch::class.java, CodecBasedAdapterTodoLiquidbounce.DATA_COMPONENT_PATCH)
        .registerTypeHierarchyAdapter(SoundEvent::class.javaObjectType, IdentifierWithRegistryAdapterTodoLiquidbounce.SOUND_EVENT)
        .registerTypeHierarchyAdapter(MobEffect::class.javaObjectType, IdentifierWithRegistryAdapterTodoLiquidbounce.STATUS_EFFECT)
        .registerTypeHierarchyAdapter(MenuType::class.java, IdentifierWithRegistryAdapterTodoLiquidbounce.SCREEN_HANDLER)
        .registerTypeHierarchyAdapter(Color4bTodoLiquidbounce::class.javaObjectType, ColorAdapterTodoLiquidbounce)
        .registerTypeHierarchyAdapter(Vec3::class.javaObjectType, Vec3dAdapterTodoLiquidbounce)
        .registerTypeHierarchyAdapter(Vec3i::class.javaObjectType, Vec3iAdapterTodoLiquidbounce)
        .registerTypeHierarchyAdapter(Vec2::class.javaObjectType, Vec2fAdapterTodoLiquidbounce)
        .registerTypeHierarchyAdapter(Vector2fc::class.java, Vector2fcAdapterTodoLiquidbounce)
        .registerTypeHierarchyAdapter(Block::class.javaObjectType, IdentifierWithRegistryAdapterTodoLiquidbounce.BLOCK)
        .registerTypeHierarchyAdapter(InputConstants.Key::class.javaObjectType, SimpleStringTypeAdapterTodoLiquidbounce.INPUT_KEY)
        .registerTypeHierarchyAdapter(InputBindTodoLiquidbounce::class.javaObjectType, InputBindAdapterTodoLiquidbounce)
        .registerTypeHierarchyAdapter(TaggedTodoLiquidbounce::class.javaObjectType, TaggedSerializerTodoLiquidbounce)
        // codex start
        // .registerTypeHierarchyAdapter(MinecraftAccount::class.javaObjectType, MinecraftAccountAdapter)
        // codex end
        .registerTypeHierarchyAdapter(Component::class.javaObjectType, CodecBasedAdapterTodoLiquidbounce.SANITIZED_COMPONENT)
        // codex start
        // .registerTypeHierarchyAdapter(Screen::class.javaObjectType, ScreenSerializer)
        // .registerTypeHierarchyAdapter(User::class.javaObjectType, SessionSerializer)
        // .registerTypeAdapter(ServerData::class.javaObjectType, ServerInfoSerializer)
        // codex end
        .registerTypeHierarchyAdapter(StringRepresentable::class.java, StringRepresentableSerializerTodoLiquidbounce)
        .registerTypeAdapter(ItemStack::class.javaObjectType, ItemStackSerializerTodoLiquidbounce)
        .registerTypeAdapter(Identifier::class.javaObjectType, SimpleStringTypeAdapterTodoLiquidbounce.IDENTIFIER)
        .registerTypeAdapter(MobEffectInstance::class.javaObjectType, StatusEffectInstanceSerializerTodoLiquidbounce)
        .registerTypeHierarchyAdapter(Supplier::class.javaObjectType, SupplierSerializerTodoLiquidbounce)
        .registerTypeAdapterFactory(OptionalAdapterTodoLiquidbounce)
