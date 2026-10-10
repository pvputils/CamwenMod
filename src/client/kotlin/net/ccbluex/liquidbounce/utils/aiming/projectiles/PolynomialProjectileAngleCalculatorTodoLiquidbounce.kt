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

package net.ccbluex.liquidbounce.utils.aiming.projectiles

import net.ccbluex.liquidbounce.utils.aiming.data.RotationTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.math.yaw
import net.ccbluex.liquidbounce.utils.math.toDegrees
import net.ccbluex.liquidbounce.utils.entity.PositionExtrapolationTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoTodoLiquidbounce
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.phys.Vec3
import kotlin.math.atan
import kotlin.math.sqrt

/**
 * Solves this problem by approximating the trajectory as a second degree polynomial. This approximation is good for
 * ~20 ticks.
 *
 * Currently only used as backup
 */
object PolynomialProjectileAngleCalculatorTodoLiquidbounce: ProjectileAngleCalculatorTodoLiquidbounce {
    override fun calculateAngleFor(
        projectileInfo: TrajectoryInfoTodoLiquidbounce,
        sourcePos: Vec3,
        targetPosFunction: PositionExtrapolationTodoLiquidbounce,
        targetShape: EntityDimensions
    ): RotationTodoLiquidbounce? {
        val basePos = targetPosFunction.getPositionInTicks(0.0)
        val estimatedTicksUntilImpact = basePos.distanceTo(sourcePos) / projectileInfo.initialVelocity

        val diff: Vec3 = targetPosFunction.getPositionInTicks(estimatedTicksUntilImpact).subtract(sourcePos)

        val horizontalDistance = diff.horizontalDistance()
        val pearlInfo = TrajectoryInfoTodoLiquidbounce.GENERIC

        val velocity = pearlInfo.initialVelocity
        val gravity = pearlInfo.gravity

        val velocity2 = velocity * velocity
        val velocity4 = velocity2 * velocity2
        val y = diff.y

        val sqrt = velocity4 - gravity * (gravity * horizontalDistance * horizontalDistance + 2 * y * velocity2)

        if (sqrt < 0) {
            return null
        }

        val pitchRad = atan((velocity2 - sqrt(sqrt)) / (gravity * horizontalDistance))
        return RotationTodoLiquidbounce(
            diff.yaw,
            Mth.wrapDegrees(-pitchRad.toDegrees().toFloat())
        )
    }
}
