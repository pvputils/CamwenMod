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
package net.ccbluex.liquidbounce.utils.aiming.preference

import net.ccbluex.liquidbounce.utils.aiming.RotationManagerTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.data.RotationTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.math.geometry.RayTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

class LeastDifferencePreferenceTodoLiquidbounce(
    private val baseRotation: RotationTodoLiquidbounce,
    private val basePoint: Vec3? = null
) : RotationPreferenceTodoLiquidbounce {

    override fun getPreferredSpot(eyesPos: Vec3, range: Double): Vec3 {
        if (basePoint != null) {
            return basePoint
        }

        return eyesPos.fma(range, baseRotation.directionVector)
    }

    override fun getPreferredSpotOnBox(box: AABB, eyesPos: Vec3, range: Double): Vec3 {
        if (basePoint != null) {
            return basePoint
        }

        val preferredSpot = getPreferredSpot(eyesPos, range)
        if (box.contains(preferredSpot)) {
            return preferredSpot
        }

        val look = RayTodoLiquidbounce(eyesPos, preferredSpot - eyesPos)
        return look.firstIntersectionWith(box)
            ?.takeIf { it.distanceToSqr(eyesPos) <= range.sq() }
            ?: preferredSpot
    }

    override fun compare(o1: RotationTodoLiquidbounce, o2: RotationTodoLiquidbounce): Int {
        val rotationDifferenceO1 = baseRotation.rotationDeltaLengthTo(o1)
        val rotationDifferenceO2 = baseRotation.rotationDeltaLengthTo(o2)

        return rotationDifferenceO1.compareTo(rotationDifferenceO2)
    }

    companion {

        fun leastDifferenceToCurrentRotation() =
            LeastDifferencePreferenceTodoLiquidbounce(RotationManagerTodoLiquidbounce.currentRotation ?: player.rotation)

        fun leastDifferenceToLastPoint(eyes: Vec3, point: Vec3): LeastDifferencePreferenceTodoLiquidbounce {
            return LeastDifferencePreferenceTodoLiquidbounce(RotationTodoLiquidbounce.lookingAt(point, from = eyes), point)
        }

    }

}
