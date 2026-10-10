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
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.PositionExtrapolationTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoTodoLiquidbounce
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3

/**
 * Calculates the shooting angle which hits the supplied target
 */
fun interface ProjectileAngleCalculatorTodoLiquidbounce {
    fun calculateAngleFor(
        projectileInfo: TrajectoryInfoTodoLiquidbounce,
        sourcePos: Vec3,
        targetPosFunction: PositionExtrapolationTodoLiquidbounce,
        targetShape: EntityDimensions,
    ): RotationTodoLiquidbounce?

    fun calculateAngleForStaticTarget(
        projectileInfo: TrajectoryInfoTodoLiquidbounce,
        target: Vec3,
        shape: EntityDimensions
    ): RotationTodoLiquidbounce? {
        return this.calculateAngleFor(
            projectileInfo,
            sourcePos = player.eyePosition,
            targetPosFunction = PositionExtrapolationTodoLiquidbounce.constant(target),
            targetShape = shape
        )
    }

    fun calculateAngleForEntity(projectileInfo: TrajectoryInfoTodoLiquidbounce, entity: LivingEntity): RotationTodoLiquidbounce? {
        return this.calculateAngleFor(
            projectileInfo,
            sourcePos = player.eyePosition,
            targetPosFunction = PositionExtrapolationTodoLiquidbounce.getBestForEntity(entity),
            targetShape = entity.dimensions
        )
    }
}
