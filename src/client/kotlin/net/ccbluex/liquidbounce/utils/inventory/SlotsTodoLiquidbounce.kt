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
package net.ccbluex.liquidbounce.utils.inventory

import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.features.addon.AddonApiTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.util.function.Predicate

fun <T : HotbarItemSlotTodoLiquidbounce> Iterable<T>.findClosestSlot(item: Item): T? =
    findClosestSlot { it.item === item }

fun <T : HotbarItemSlotTodoLiquidbounce> Iterable<T>.findClosestSlot(itemTag: TagKey<Item>): T? =
    findClosestSlot { it.`is`(itemTag) }

fun <T : HotbarItemSlotTodoLiquidbounce> Iterable<T>.findClosestSlot(vararg items: Item): T? =
    findClosestSlot { it.item in items }

fun <T : HotbarItemSlotTodoLiquidbounce> Iterable<T>.findClosestSlot(items: Collection<Item>): T? =
    findClosestSlot { it.item in items }

inline fun <T : HotbarItemSlotTodoLiquidbounce> Iterable<T>.findClosestSlot(predicate: (ItemStack) -> Boolean): T? {
    var candidate: T? = null
    for (slot in this) {
        if (!predicate(slot.itemStack)) continue
        candidate = if (candidate == null) {
            slot
        } else {
            minOf(candidate, slot, HotbarItemSlotTodoLiquidbounce.PREFER_NEARBY)
        }
    }
    return candidate
}

@AddonApiTodoLiquidbounce
class SlotsTodoLiquidbounce<T : ItemSlotTodoLiquidbounce>(private val slots: List<T>) : List<T> by slots {
    val stacks: Array<ItemStack>
        get() = slots.mapToArray { it.itemStack }

    val items: Array<Item>
        get() = slots.mapToArray { it.itemStack.item }

    fun findSlot(item: Item): T? = findSlot { it.item === item }

    // Java takes the Predicate overload; both would match a lambda otherwise.
    @JvmSynthetic
    inline fun findSlot(predicate: (ItemStack) -> Boolean): T? {
        return if (mc.player == null) null else find { predicate(it.itemStack) }
    }

    fun findSlot(predicate: Predicate<ItemStack>): T? = findSlot(predicate::test)

    operator fun plus(other: SlotsTodoLiquidbounce<*>): SlotsTodoLiquidbounce<ItemSlotTodoLiquidbounce> {
        return SlotsTodoLiquidbounce(this.slots + other.slots)
    }

    operator fun plus(other: ItemSlotTodoLiquidbounce): SlotsTodoLiquidbounce<ItemSlotTodoLiquidbounce> {
        return SlotsTodoLiquidbounce(this.slots + other)
    }

    companion object {
        /**
         * Hotbar 0~8
         */
        @JvmField
        val Hotbar = SlotsTodoLiquidbounce(HotbarItemSlotTodoLiquidbounce.mainHandSlots)

        /**
         * Inventory 0~26
         */
        @JvmField
        val Inventory = SlotsTodoLiquidbounce(InventoryItemSlotTodoLiquidbounce.ALL)

        /**
         * Hotbar + Inventory
         */
        @JvmField
        val HotbarAndInventory = Hotbar + Inventory

        /**
         * Armor slots 0~3
         *
         * Boots/Leggings/Chestplate/Helmet
         */
        @JvmField
        val Armor = SlotsTodoLiquidbounce(ArmorItemSlotTodoLiquidbounce.entries)

        /**
         * Offhand + Hotbar
         */
        @JvmField
        val OffhandWithHotbar = SlotsTodoLiquidbounce(HotbarItemSlotTodoLiquidbounce.entries)

        /**
         * Hotbar + OffHand + Inventory + Armor
         */
        @JvmField
        val All = Hotbar + HotbarItemSlotTodoLiquidbounce.OFFHAND + Inventory + Armor
    }
}
