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

import com.google.gson.JsonObject
import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.fastutil.enumSetAllOf
import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.fastutil.forEachIsInstance
import net.ccbluex.fastutil.toEnumSet
import net.ccbluex.liquidbounce.config.ConfigSystemTodoLiquidbounce
import net.ccbluex.liquidbounce.config.OptionalInclusionTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.BindValueTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.ConfigTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.CurveValueTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.CurveValueTodoLiquidbounce.Axis
import net.ccbluex.liquidbounce.config.types.FileDialogModeTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.FileValueTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.RangedValueTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.ValueTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.ValueTypeTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.Vec3ValueTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.list.ChoiceListValueTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.list.ItemListValueTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.list.ListValueTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValueTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.list.MutableListValueTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.list.RegistryListValueTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.list.RegistryMutableListValueTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.list.TaggedTodoLiquidbounce
import net.ccbluex.liquidbounce.event.EventListenerTodoLiquidbounce
import net.ccbluex.liquidbounce.features.addon.AddonApiTodoLiquidbounce
import net.ccbluex.liquidbounce.render.engine.type.Color4bTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.input.InputBindTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.math.EasingTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.text.toLowerCamelCase
import net.minecraft.core.Vec3i
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.Vec3
import org.joml.Vector2f
import org.joml.Vector2fc
import java.io.File
import java.util.EnumSet
import java.util.SequencedSet
import java.util.function.ToIntFunction

@Suppress("TooManyFunctions")
@AddonApiTodoLiquidbounce
open class ValueGroupTodoLiquidbounce @JvmOverloads constructor(
    name: String,
    value: MutableCollection<ValueTodoLiquidbounce<*>> = mutableListOf(),
    valueType: ValueTypeTodoLiquidbounce = ValueTypeTodoLiquidbounce.CONFIGURABLE,

    /**
     * Signalizes that the [ValueGroup]'s translation key
     * should not depend on another [ValueGroup].
     * This means the [baseKey] will be directly used.
     *
     * The options should be used in common options, so that
     * descriptions don't have to be written twice.
     */
    independentDescription: Boolean = false,
    /**
     * Used for backwards compatibility when renaming.
     */
    aliases: List<String> = emptyList(),
) : ValueTodoLiquidbounce<MutableCollection<ValueTodoLiquidbounce<*>>>(
    name,
    aliases,
    defaultValue = value,
    valueType,
    independentDescription = independentDescription
) {

    /**
     * Allows dynamic groups to create their children before stored values are applied.
     */
    open fun prepareDeserialize(jsonObject: JsonObject) = Unit

    /**
     * Stores the [ValueGroup] in which
     * the [ValueGroup] is included, can be null.
     */
    var base: ValueGroupTodoLiquidbounce? = null

    /**
     * The base key used when [base] is null,
     * otherwise the [baseKey] from [base]
     * is used when its base is null and so on.
     */
    open val baseKey: String
        get() = "${ConfigSystemTodoLiquidbounce.KEY_PREFIX}.${name.toLowerCamelCase()}"

    open fun walkInit() {
        inner.forEachIsInstance<ValueGroupTodoLiquidbounce> { valueGroup ->
            valueGroup.walkInit()
        }
    }

    /**
     * Walks the path of the [ValueGroup] and its children
     */
    fun walkKeyPath(previousBaseKey: String? = null) {
        this.key = if (previousBaseKey != null) {
            "$previousBaseKey.${name.toLowerCamelCase()}"
        } else {
            constructBaseKey()
        }

        // Update children
        for (currentValue in this.inner) {
            if (currentValue is ValueGroupTodoLiquidbounce) {
                currentValue.walkKeyPath(this.key)
            } else {
                currentValue.key = "${this.key}.${currentValue.name.toLowerCamelCase()}"
            }

            if (currentValue is ModeValueGroupTodoLiquidbounce<*>) {
                val currentKey = currentValue.key

                currentValue.modes.forEach { choice -> choice.walkKeyPath(currentKey) }
            }
        }
    }

    /**
     * Joins the names of all bases and this and the [baseKey] of the lowest
     * base together to create a translation base key.
     */
    private fun constructBaseKey(): String {
        val values = mutableListOf<String>()
        var current: ValueGroupTodoLiquidbounce? = this
        while (current != null) {
            val base1 = current.base
            if (base1 == null) {
                values.add(current.baseKey)
            } else {
                values.add(current.name.toLowerCamelCase())
            }
            current = base1
        }
        values.reverse()
        return values.joinToString(".")
    }

    @get:JvmName("getContainedValues")
    val containedValues: Array<ValueTodoLiquidbounce<*>>
        get() = this.inner.toTypedArray()

    fun collectValuesRecursively(prefix: String = ""): Sequence<ValueTodoLiquidbounce<*>> = sequence {
        val shouldFilterByPrefix = prefix.isNotBlank()

        suspend fun SequenceScope<ValueTodoLiquidbounce<*>>.walk(current: ValueGroupTodoLiquidbounce) {
            if (shouldFilterByPrefix && !shouldWalkKey(current.key, prefix)) {
                return
            }
            for (value in current.inner) {
                when (value) {
                    is ToggleableValueGroupTodoLiquidbounce -> {
                        yield(value)
                        walk(value)
                    }
                    is ModeValueGroupTodoLiquidbounce<*> -> {
                        yield(value)
                        value.modes.forEach { walk(it) }
                    }
                    is ValueGroupTodoLiquidbounce -> walk(value)
                    else -> yield(value)
                }
            }
        }

        walk(this@ValueGroupTodoLiquidbounce)
    }

    fun collectValueGroupsRecursively(prefix: String = ""): Sequence<ValueGroupTodoLiquidbounce> = sequence {
        val shouldFilterByPrefix = prefix.isNotBlank()

        suspend fun SequenceScope<ValueGroupTodoLiquidbounce>.walk(current: ValueGroupTodoLiquidbounce) {
            if (shouldFilterByPrefix && !shouldWalkKey(current.key, prefix)) {
                return
            }
            yield(current)
            for (value in current.inner) {
                when (value) {
                    is ModeValueGroupTodoLiquidbounce<*> -> {
                        walk(value)
                        value.modes.forEach { walk(it) }
                    }
                    is ValueGroupTodoLiquidbounce -> walk(value)
                }
            }
        }

        walk(this@ValueGroupTodoLiquidbounce)
    }

    private fun shouldWalkKey(currentKey: String?, prefix: String): Boolean {
        if (currentKey == null) {
            return false
        }
        return currentKey.startsWith(prefix, ignoreCase = true) || prefix.startsWith(currentKey, ignoreCase = true)
    }

    /**
     * Restore all values to their default values
     */
    override fun restore() {
        inner.forEach(ValueTodoLiquidbounce<*>::restore)
    }

    override fun inclusionGroup(group: OptionalInclusionTodoLiquidbounce) = apply {
        super.inclusionGroup(group)

        for (v in inner) {
            v.inclusionGroup(group)
        }
    }

    // Common value types

    fun <T : ValueGroupTodoLiquidbounce> tree(valueGroup: T): T {
        require(valueGroup !is ConfigTodoLiquidbounce) {
            "ValueGroup '${valueGroup.name}' is a Config and cannot be added to another ValueGroup."
        }

        if (valueGroup.base != null) {
            logger.warn("ValueGroup '${valueGroup.name}' is already added to a parent '${valueGroup.base?.name}'")
        }

        value(valueGroup)
        valueGroup.base = this
        return valueGroup
    }

    fun <T : ValueGroupTodoLiquidbounce> treeAll(vararg valueGroups: T) {
        valueGroups.forEach(this::tree)
    }

    fun <T : ValueGroupTodoLiquidbounce> drop(valueGroup: T): T {
        require(valueGroup.base === this) {
            "ValueGroup '${valueGroup.name}' is not a child of '${this.name}'."
        }

        inner.remove(valueGroup)
        valueGroup.base = null
        return valueGroup
    }

    fun <T : Any> value(
        name: String,
        defaultValue: T,
        valueType: ValueTypeTodoLiquidbounce = ValueTypeTodoLiquidbounce.INVALID,
        aliases: List<String> = emptyList(),
    ) = value(ValueTodoLiquidbounce(name, aliases = aliases, defaultValue = defaultValue, valueType = valueType))

    // Inline bodies are compiled into add-on jars, so the reified builders only forward to these.

    fun <T : MutableCollection<E>, E> list(
        name: String,
        defaultValue: T,
        valueType: ValueTypeTodoLiquidbounce,
        innerType: Class<E>,
    ) = value(ListValueTodoLiquidbounce(name, defaultValue, innerValueType = valueType, innerType = innerType))

    inline fun <T : MutableCollection<E>, reified E> list(
        name: String,
        defaultValue: T,
        valueType: ValueTypeTodoLiquidbounce,
    ) = list(name, defaultValue, valueType, E::class.java)

    fun <T : MutableCollection<E>, E> mutableList(
        name: String,
        defaultValue: T,
        valueType: ValueTypeTodoLiquidbounce,
        innerType: Class<E>,
    ) = value(MutableListValueTodoLiquidbounce(name, defaultValue, valueType, innerType))

    inline fun <T : MutableCollection<E>, reified E> mutableList(
        name: String,
        defaultValue: T,
        valueType: ValueTypeTodoLiquidbounce,
    ) = mutableList(name, defaultValue, valueType, E::class.java)

    fun <T : MutableSet<E>, E> itemList(
        name: String,
        defaultValue: T,
        items: Set<ItemListValueTodoLiquidbounce.NamedItem<E>>,
        valueType: ValueTypeTodoLiquidbounce,
        innerType: Class<E>,
    ) = value(ItemListValueTodoLiquidbounce(name, defaultValue, items, valueType, innerType))

    inline fun <T : MutableSet<E>, reified E> itemList(
        name: String,
        defaultValue: T,
        items: Set<ItemListValueTodoLiquidbounce.NamedItem<E>>,
        valueType: ValueTypeTodoLiquidbounce,
    ) = itemList(name, defaultValue, items, valueType, E::class.java)

    fun <T : SequencedSet<E>, E> registryList(
        name: String,
        defaultValue: T,
        valueType: ValueTypeTodoLiquidbounce,
        innerType: Class<E>,
    ) = value(RegistryListValueTodoLiquidbounce(name, defaultValue, valueType, innerType))

    inline fun <T : SequencedSet<E>, reified E> registryList(
        name: String,
        defaultValue: T,
        valueType: ValueTypeTodoLiquidbounce,
    ) = registryList(name, defaultValue, valueType, E::class.java)

    fun <T : MutableList<E>, E> registryMutableList(
        name: String,
        defaultValue: T,
        valueType: ValueTypeTodoLiquidbounce,
        innerType: Class<E>,
    ) = value(RegistryMutableListValueTodoLiquidbounce(name, defaultValue, valueType, innerType))

    inline fun <T : MutableList<E>, reified E> registryMutableList(
        name: String,
        defaultValue: T,
        valueType: ValueTypeTodoLiquidbounce,
    ) = registryMutableList(name, defaultValue, valueType, E::class.java)

    private fun <T : Any> rangedValue(
        name: String,
        defaultValue: T,
        range: ClosedRange<*>,
        suffix: String,
        valueType: ValueTypeTodoLiquidbounce,
        aliases: List<String> = emptyList(),
    ) = value(
        RangedValueTodoLiquidbounce(
            name,
            aliases = aliases,
            defaultValue = defaultValue,
            range = range,
            suffix = suffix,
            valueType = valueType,
        )
    )

    // Fixed data types

    // `boolean`, `int` and `float` are Java keywords, hence the JVM names.

    @JvmName("bool")
    @JvmOverloads
    fun boolean(
        name: String,
        default: Boolean,
        aliases: List<String> = emptyList(),
    ) = value(name, default, ValueTypeTodoLiquidbounce.BOOLEAN, aliases)

    @JvmName("floating")
    @JvmOverloads
    fun float(
        name: String,
        default: Float,
        range: ClosedFloatingPointRange<Float>,
        suffix: String = "",
        aliases: List<String> = emptyList(),
    ) = rangedValue(name, default, range, suffix, ValueTypeTodoLiquidbounce.FLOAT, aliases)

    @JvmName("floating")
    @JvmOverloads
    fun float(name: String, default: Float, min: Float, max: Float, suffix: String = "") =
        float(name, default, min..max, suffix)

    @JvmOverloads
    fun floatRange(
        name: String,
        default: ClosedFloatingPointRange<Float>,
        range: ClosedFloatingPointRange<Float>,
        suffix: String = "",
        aliases: List<String> = emptyList(),
    ) = rangedValue(name, default, range, suffix, ValueTypeTodoLiquidbounce.FLOAT_RANGE, aliases)

    @JvmName("integer")
    @JvmOverloads
    fun int(
        name: String,
        default: Int,
        range: IntRange,
        suffix: String = "",
        aliases: List<String> = emptyList(),
    ) = rangedValue(name, default, range, suffix, ValueTypeTodoLiquidbounce.INT, aliases)

    @JvmName("integer")
    @JvmOverloads
    fun int(name: String, default: Int, min: Int, max: Int, suffix: String = "") =
        int(name, default, min..max, suffix)

    @JvmOverloads
    fun intRange(
        name: String,
        default: IntRange,
        range: IntRange,
        suffix: String = "",
        aliases: List<String> = emptyList(),
    ) = rangedValue(name, default, range, suffix, ValueTypeTodoLiquidbounce.INT_RANGE, aliases)

    @JvmOverloads
    fun bind(name: String, default: Int = InputConstants.UNKNOWN.value) = bind(
        name,
        InputBindTodoLiquidbounce(InputConstants.Type.KEYSYM, default, InputBindTodoLiquidbounce.BindAction.TOGGLE)
    )

    fun bind(name: String, default: InputBindTodoLiquidbounce) = value(BindValueTodoLiquidbounce(name, defaultValue = default))

    fun key(name: String, default: Int) = key(name, InputConstants.Type.KEYSYM.getOrCreate(default))

    @JvmOverloads
    fun key(name: String, default: InputConstants.Key = InputConstants.UNKNOWN) =
        value(name, default, ValueTypeTodoLiquidbounce.KEY)

    fun text(name: String, default: String) = value(name, default, ValueTypeTodoLiquidbounce.TEXT)

    fun regex(name: String, default: Regex) = value(name, default, ValueTypeTodoLiquidbounce.TEXT)

    fun <C : MutableCollection<String>> textList(name: String, default: C) =
        mutableList<C, String>(name, default, ValueTypeTodoLiquidbounce.TEXT)

    fun <C : MutableCollection<Regex>> regexList(name: String, default: C) =
        mutableList<C, Regex>(name, default, ValueTypeTodoLiquidbounce.TEXT)

    fun easing(name: String, default: EasingTodoLiquidbounce) = enumChoice(name, default)

    fun color(name: String, default: Color4bTodoLiquidbounce) = value(name, default, ValueTypeTodoLiquidbounce.COLOR)

    fun block(name: String, default: Block) = value(name, default, ValueTypeTodoLiquidbounce.BLOCK)

    fun vec2f(name: String, default: Vector2fc) = value(name, default, ValueTypeTodoLiquidbounce.VECTOR2_F)

    @JvmOverloads
    fun vec3i(
        name: String,
        default: Vec3i = Vec3i.ZERO,
        useLocateButton: Boolean = true,
        aliases: List<String> = emptyList(),
    ): ValueTodoLiquidbounce<Vec3i> = value(Vec3ValueTodoLiquidbounce(name, aliases, default, useLocateButton, ValueTypeTodoLiquidbounce.VECTOR3_I))

    @JvmOverloads
    fun vec3d(
        name: String,
        default: Vec3 = Vec3.ZERO,
        useLocateButton: Boolean = true,
        aliases: List<String> = emptyList(),
    ): ValueTodoLiquidbounce<Vec3> = value(Vec3ValueTodoLiquidbounce(name, aliases, default, useLocateButton, ValueTypeTodoLiquidbounce.VECTOR3_D))

    fun <C : SequencedSet<Block>> blocks(name: String, default: C) =
        registryList(name, default, ValueTypeTodoLiquidbounce.BLOCK)

    fun item(name: String, default: Item) = value(name, default, ValueTypeTodoLiquidbounce.ITEM)

    fun <C : SequencedSet<Item>> items(name: String, default: C) =
        registryList(name, default, ValueTypeTodoLiquidbounce.ITEM)

    fun <C : MutableList<Item>> itemList(name: String, default: C) =
        registryMutableList(name, default, ValueTypeTodoLiquidbounce.ITEM)

    fun <C : SequencedSet<SoundEvent>> sounds(name: String, default: C) =
        registryList(name, default, ValueTypeTodoLiquidbounce.SOUND_EVENT)

    fun <C : SequencedSet<MobEffect>> mobEffects(name: String, default: C) =
        registryList(name, default, ValueTypeTodoLiquidbounce.MOB_EFFECT)

    fun <C : SequencedSet<Identifier>> enchantments(name: String, default: C) =
        registryList(name, default, ValueTypeTodoLiquidbounce.ENCHANTMENT)

    fun <C : SequencedSet<Identifier>> c2sPackets(name: String, default: C) =
        registryList(name, default, ValueTypeTodoLiquidbounce.C2S_PACKET)

    fun <C : SequencedSet<Identifier>> s2cPackets(name: String, default: C) =
        registryList(name, default, ValueTypeTodoLiquidbounce.S2C_PACKET)

    fun <C : SequencedSet<EntityType<*>>> entityTypes(name: String, default: C) =
        registryList(name, default, ValueTypeTodoLiquidbounce.ENTITY_TYPE)

    @Suppress("LongParameterList")
    fun curve(
        name: String,
        default: MutableList<Vector2f>,
        xAxis: Axis,
        yAxis: Axis,
        tension: Float = CurveValueTodoLiquidbounce.DEFAULT_TENSION,
    ) = value(CurveValueTodoLiquidbounce(name, default, xAxis, yAxis, tension))

    inline fun curve(name: String, block: CurveValueTodoLiquidbounce.Builder.() -> Unit): CurveValueTodoLiquidbounce {
        val builder = CurveValueTodoLiquidbounce.Builder()
        builder.name = name
        return value(builder.apply(block).build())
    }

    fun file(
        name: String,
        default: File? = null,
        dialogMode: FileDialogModeTodoLiquidbounce = FileDialogModeTodoLiquidbounce.OPEN_FILE,
        supportedExtensions: Set<String>? = null,
    ) = value(FileValueTodoLiquidbounce(name, default, dialogMode, supportedExtensions))

    inline fun <reified T> multiEnumChoice(
        name: String,
        vararg default: T,
        canBeNone: Boolean = true,
    ) where T : Enum<T>, T : TaggedTodoLiquidbounce =
        multiEnumChoice(name, default.toEnumSet(), canBeNone = canBeNone)

    inline fun <reified T> multiEnumChoice(
        name: String,
        default: Iterable<T>,
        canBeNone: Boolean = true,
    ) where T : Enum<T>, T : TaggedTodoLiquidbounce =
        multiEnumChoice(name, default.toEnumSet(), canBeNone = canBeNone)

    inline fun <reified T> multiEnumChoice(
        name: String,
        default: EnumSet<T> = enumSetOf(),
        choices: EnumSet<T> = enumSetAllOf(),
        canBeNone: Boolean = true,
    ) where T : Enum<T>, T : TaggedTodoLiquidbounce =
        multiEnumChoice(name, default, choices, canBeNone, isOrderSensitive = false)

    inline fun <reified T> multiEnumChoice(
        name: String,
        default: SequencedSet<T>,
        choices: EnumSet<T> = enumSetAllOf(),
        canBeNone: Boolean = true,
    ) where T : Enum<T>, T : TaggedTodoLiquidbounce =
        multiEnumChoice(name, default, choices, canBeNone, isOrderSensitive = true)

    fun <T : TaggedTodoLiquidbounce> multiEnumChoice(
        name: String,
        default: MutableSet<T>,
        choices: Set<T>,
        canBeNone: Boolean,
        isOrderSensitive: Boolean,
    ) = value(MultiChoiceListValueTodoLiquidbounce(name, default, choices, canBeNone, isOrderSensitive))

    inline fun <reified T> enumChoice(
        name: String,
        default: T,
        aliases: List<String> = emptyList(),
    ): ChoiceListValueTodoLiquidbounce<T> where T : Enum<T>, T : TaggedTodoLiquidbounce = enumChoice(name, default, enumSetAllOf(), aliases)

    /**
     * For Java, which cannot call the reified overload.
     */
    fun <T> enumChoice(name: String, default: T): ChoiceListValueTodoLiquidbounce<T> where T : Enum<T>, T : TaggedTodoLiquidbounce =
        enumChoice(name, default, EnumSet.allOf(default.declaringJavaClass), emptyList())

    /**
     * For Java, which cannot call the reified overloads.
     */
    @JvmOverloads
    fun <T> multiEnumChoice(
        name: String,
        type: Class<T>,
        default: Collection<T>,
        canBeNone: Boolean = true,
    ): MultiChoiceListValueTodoLiquidbounce<T> where T : Enum<T>, T : TaggedTodoLiquidbounce =
        multiEnumChoice(name, EnumSet.noneOf(type).apply { addAll(default) }, EnumSet.allOf(type), canBeNone, false)

    @JvmOverloads
    fun <T : TaggedTodoLiquidbounce> enumChoice(
        name: String,
        default: T,
        choices: Set<T>,
        aliases: List<String> = emptyList(),
    ): ChoiceListValueTodoLiquidbounce<T> = value(ChoiceListValueTodoLiquidbounce(name, defaultValue = default, choices = choices, aliases = aliases))

    fun interface ModeBuilder {
        fun ValueGroupTodoLiquidbounce.build()
    }

    protected fun <T : ModeTodoLiquidbounce> modes(
        eventListener: EventListenerTodoLiquidbounce?,
        name: String,
        active: T,
        modes: Array<T>,
    ): ModeValueGroupTodoLiquidbounce<T> {
        return modes(eventListener, name, { modes ->
            val idx = modes.indexOf(active)

            check(idx != -1) {
                "The active choice $active is not contained within the choice array" +
                    " (${modes.joinToString { it.name }})"
            }

            idx
        }) { modes }
    }

    fun <T : ModeTodoLiquidbounce> modes(
        eventListener: EventListenerTodoLiquidbounce?,
        name: String,
        activeCallback: ToIntFunction<List<T>>,
        modesCallback: (ModeValueGroupTodoLiquidbounce<T>) -> Array<T>,
    ): ModeValueGroupTodoLiquidbounce<T> {
        return value(ModeValueGroupTodoLiquidbounce(eventListener, name, activeCallback, modesCallback).apply {
            this.base = this@ValueGroupTodoLiquidbounce
        })
    }

    protected fun <T : ModeTodoLiquidbounce> modes(
        eventListener: EventListenerTodoLiquidbounce,
        name: String,
        activeIndex: Int = 0,
        choicesCallback: (ModeValueGroupTodoLiquidbounce<T>) -> Array<T>,
    ) = modes(eventListener, name, { activeIndex }, choicesCallback)

    fun <V : ValueTodoLiquidbounce<*>> value(value: V) = value.apply {
        this@ValueGroupTodoLiquidbounce.inner.add(this)
        this@ValueGroupTodoLiquidbounce.inclusionGroup?.let { this.inclusionGroup(it) }
    }

}
