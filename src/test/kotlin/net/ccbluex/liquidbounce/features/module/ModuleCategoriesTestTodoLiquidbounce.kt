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

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ModuleCategoriesTestTodoLiquidbounce {

    @Test
    fun `a duplicate tag keeps the category registered first`() {
        val original = ModuleCategoriesTodoLiquidbounce.register(ModuleCategoryTodoLiquidbounce("ModuleCategoriesTestDuplicate"))

        try {
            assertFailsWith<IllegalStateException> {
                ModuleCategoriesTodoLiquidbounce.register(ModuleCategoryTodoLiquidbounce("modulecategoriestestduplicate"))
            }
            assertSame(original, ModuleCategoriesTodoLiquidbounce.byName("ModuleCategoriesTestDuplicate"))
        } finally {
            ModuleCategoriesTodoLiquidbounce.unregister(original)
        }
    }

    @Test
    fun `unregister only removes the identical instance`() {
        val registered = ModuleCategoriesTodoLiquidbounce.register(ModuleCategoryTodoLiquidbounce("ModuleCategoriesTestUnregister"))

        try {
            assertFalse(ModuleCategoriesTodoLiquidbounce.unregister(ModuleCategoryTodoLiquidbounce("ModuleCategoriesTestUnregister")))
            assertSame(registered, ModuleCategoriesTodoLiquidbounce.byName("ModuleCategoriesTestUnregister"))
        } finally {
            assertTrue(ModuleCategoriesTodoLiquidbounce.unregister(registered))
        }

        assertNull(ModuleCategoriesTodoLiquidbounce.byName("ModuleCategoriesTestUnregister"))
    }

}
