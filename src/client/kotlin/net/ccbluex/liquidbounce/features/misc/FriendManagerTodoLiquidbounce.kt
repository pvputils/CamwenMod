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
package net.ccbluex.liquidbounce.features.misc

import net.ccbluex.liquidbounce.config.ConfigSystemTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.ConfigTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.ValueTypeTodoLiquidbounce
import net.ccbluex.liquidbounce.event.EventListenerTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.AttackEntityEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.events.TagEntityEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import java.util.TreeSet
import net.ccbluex.liquidbounce.features.addon.AddonApiTodoLiquidbounce

object FriendManagerTodoLiquidbounce : ConfigTodoLiquidbounce("Friends"), EventListenerTodoLiquidbounce {

    val friends by list(name, TreeSet<Friend>(), valueType = ValueTypeTodoLiquidbounce.FRIEND)

    private val cancelAttack by boolean("CancelAttack", false)

    @Suppress("unused")
    private val tagEntityEvent = handler<TagEntityEventTodoLiquidbounce> {
        if (isFriend(it.entity)) {
            it.assumeFriend()
        }
    }

    @Suppress("unused")
    private val onAttack = handler<AttackEntityEventTodoLiquidbounce> {
        if (cancelAttack && isFriend(it.entity)) {
            it.cancelEvent()
        }
    }

    init {
        ConfigSystemTodoLiquidbounce.root(this)
    }

    class Friend(val name: String, var alias: String?) : Comparable<Friend> {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Friend

            return name == other.name
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

        override fun compareTo(other: Friend): Int = this.name.compareTo(other.name)

        fun getDefaultName(id: Int): String = "Friend $id"

    }

    fun isFriend(name: String): Boolean = friends.contains(Friend(name, null))
    fun isFriend(entity: Entity): Boolean = entity is Player && isFriend(entity.gameProfile.name)

    @AddonApiTodoLiquidbounce
    fun add(friend: Friend): Boolean = friends.add(friend).also { added ->
        if (added) {
            // codex start
            // EventManager.callEvent(FriendChangeEvent(friend.name, true))
            // codex end
        }
    }

    @AddonApiTodoLiquidbounce
    fun remove(name: String): Boolean = friends.remove(Friend(name, null)).also { removed ->
        if (removed) {
            // codex start
            // EventManager.callEvent(FriendChangeEvent(name, false))
            // codex end
        }
    }

    @AddonApiTodoLiquidbounce
    fun clear() {
        val names = friends.map(Friend::name)
        friends.clear()
        // codex start
        // names.forEach { EventManager.callEvent(FriendChangeEvent(it, false)) }
        // codex end
    }

}
