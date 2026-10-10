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
package net.ccbluex.liquidbounce.utils.aiming.data

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class RotationTestTodoLiquidbounce {

    @Test
    fun testDirectionAngleUsesViewDirections() {
        assertEquals(
            20f,
            RotationTodoLiquidbounce(0f, 80f).directionAngleTo(RotationTodoLiquidbounce(180f, 80f)),
            1e-2f
        )
        assertEquals(
            0f,
            RotationTodoLiquidbounce(0f, 90f).directionAngleTo(RotationTodoLiquidbounce(180f, 90f)),
            1e-2f
        )
    }

    @Test
    fun testRotationDeltaLengthPreservesControlAxes() {
        assertEquals(
            180f,
            RotationTodoLiquidbounce(0f, 90f).rotationDeltaLengthTo(RotationTodoLiquidbounce(180f, 90f)),
            1e-3f
        )
        assertEquals(
            2f,
            RotationTodoLiquidbounce(179f, 0f).rotationDeltaLengthTo(RotationTodoLiquidbounce(-179f, 0f)),
            1e-3f
        )
    }

    @Test
    fun testDirectionAndRotationClosenessAreDistinct() {
        val first = RotationTodoLiquidbounce(0f, 90f)
        val second = RotationTodoLiquidbounce(180f, 90f)

        assertTrue(first.isDirectionCloseTo(second))
        assertFalse(first.isRotationDeltaCloseTo(second))
    }

}
