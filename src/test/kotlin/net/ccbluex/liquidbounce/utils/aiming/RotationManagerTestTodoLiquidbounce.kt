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
package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrectionTodoLiquidbounce
import kotlin.test.Test
import kotlin.test.assertEquals

class RotationManagerTestTodoLiquidbounce {

    @Test
    fun `movement yaw follows active correction`() {
        val playerYaw = 30f
        val managedYaw = 120f

        assertEquals(playerYaw, resolveMovementYaw(playerYaw, managedYaw, null))
        assertEquals(playerYaw, resolveMovementYaw(playerYaw, managedYaw, MovementCorrectionTodoLiquidbounce.OFF))
        assertEquals(playerYaw, resolveMovementYaw(playerYaw, Float.NaN, MovementCorrectionTodoLiquidbounce.SILENT))
        assertEquals(managedYaw, resolveMovementYaw(playerYaw, managedYaw, MovementCorrectionTodoLiquidbounce.STRICT))
        assertEquals(managedYaw, resolveMovementYaw(playerYaw, managedYaw, MovementCorrectionTodoLiquidbounce.SILENT))
        assertEquals(managedYaw, resolveMovementYaw(playerYaw, managedYaw, MovementCorrectionTodoLiquidbounce.CHANGE_LOOK))
    }
}
