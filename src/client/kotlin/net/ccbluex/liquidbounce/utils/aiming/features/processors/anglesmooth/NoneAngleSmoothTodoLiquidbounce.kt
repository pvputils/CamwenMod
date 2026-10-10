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
package net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth

import net.ccbluex.liquidbounce.config.types.group.ModeValueGroupTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.RotationTargetTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.data.RotationTodoLiquidbounce

/**
 * This is used by [net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl.AiAngleSmooth]
 * to define an angle smooth mode that does not affect the current rotation.
 *
 * It essentially does nothing.
 */
class NoneAngleSmoothTodoLiquidbounce(parent: ModeValueGroupTodoLiquidbounce<*>) : AngleSmoothTodoLiquidbounce("None", parent) {

    override fun calculateTicks(
        currentRotation: RotationTodoLiquidbounce,
        targetRotation: RotationTodoLiquidbounce
    ): Int = 0

    override fun process(
        rotationTarget: RotationTargetTodoLiquidbounce,
        currentRotation: RotationTodoLiquidbounce,
        targetRotation: RotationTodoLiquidbounce
    ): RotationTodoLiquidbounce = currentRotation

}
