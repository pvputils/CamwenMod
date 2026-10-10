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
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.config.OptionalInclusionTodoLiquidbounce
import net.ccbluex.liquidbounce.features.addon.AddonApiTodoLiquidbounce
import java.util.TreeMap

@AddonApiTodoLiquidbounce
object ModuleCategoriesTodoLiquidbounce {

    private val registry = TreeMap<String, ModuleCategoryTodoLiquidbounce>(String.CASE_INSENSITIVE_ORDER)

    @JvmField
    val COMBAT = register(ModuleCategoryTodoLiquidbounce("Combat"))

    @JvmField
    val PLAYER = register(ModuleCategoryTodoLiquidbounce("Player"))

    @JvmField
    val MOVEMENT = register(ModuleCategoryTodoLiquidbounce("Movement"))

    @JvmField
    val RENDER = register(ModuleCategoryTodoLiquidbounce("Render", inclusionGroup = OptionalInclusionTodoLiquidbounce.RENDER))

    @JvmField
    val WORLD = register(ModuleCategoryTodoLiquidbounce("World"))

    @JvmField
    val MISC = register(ModuleCategoryTodoLiquidbounce("Misc"))

    @JvmField
    val EXPLOIT = register(ModuleCategoryTodoLiquidbounce("Exploit"))

    @JvmField
    val FUN = register(ModuleCategoryTodoLiquidbounce("Fun", inclusionGroup = OptionalInclusionTodoLiquidbounce.FUN))

    @JvmStatic
    val entries: Collection<ModuleCategoryTodoLiquidbounce> get() = registry.sequencedValues()

    @JvmStatic
    fun register(category: ModuleCategoryTodoLiquidbounce): ModuleCategoryTodoLiquidbounce {
        if (registry.putIfAbsent(category.tag, category) != null) {
            error("A module category with the name '${category.tag}' is already registered!")
        }

        return category
    }

    /**
     * Modules filed under [category] must be removed first.
     */
    @JvmStatic
    fun unregister(category: ModuleCategoryTodoLiquidbounce): Boolean = registry.remove(category.tag, category)

    @JvmStatic
    fun byName(name: String): ModuleCategoryTodoLiquidbounce? {
        return registry[name]
    }

}
