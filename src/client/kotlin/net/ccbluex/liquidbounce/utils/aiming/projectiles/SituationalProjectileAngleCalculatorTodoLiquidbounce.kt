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
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoRendererTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryTypeTodoLiquidbounce
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3

/**
 * Uses the best available implementation of [ProjectileAngleCalculator]
 */
object SituationalProjectileAngleCalculatorTodoLiquidbounce: ProjectileAngleCalculatorTodoLiquidbounce {
    override fun calculateAngleFor(
        projectileInfo: TrajectoryInfoTodoLiquidbounce,
        sourcePos: Vec3,
        targetPosFunction: PositionExtrapolationTodoLiquidbounce,
        targetShape: EntityDimensions
    ): RotationTodoLiquidbounce? {
        val basePos = targetPosFunction.getPositionInTicks(0.0)

        val actualImplementation = when {
            // Our flagship implementation is unstable at low distances...
            basePos.distanceToSqr(sourcePos) < 5.0 * 5.0 -> PolynomialProjectileAngleCalculatorTodoLiquidbounce
            else -> CydhranianProjectileAngleCalculatorTodoLiquidbounce
        }

        return actualImplementation.calculateAngleFor(projectileInfo, sourcePos, targetPosFunction, targetShape)
    }

    object VerifyHitResult : ProjectileAngleCalculatorTodoLiquidbounce {
        private fun resolveTrajectoryType(projectileInfo: TrajectoryInfoTodoLiquidbounce): TrajectoryTypeTodoLiquidbounce {
            return when {
                projectileInfo == TrajectoryInfoTodoLiquidbounce.POTION -> TrajectoryTypeTodoLiquidbounce.Potion
                projectileInfo == TrajectoryInfoTodoLiquidbounce.EXP_BOTTLE -> TrajectoryTypeTodoLiquidbounce.ExpBottle
                projectileInfo == TrajectoryInfoTodoLiquidbounce.FISHING_ROD -> TrajectoryTypeTodoLiquidbounce.FishingBobber
                projectileInfo == TrajectoryInfoTodoLiquidbounce.TRIDENT -> TrajectoryTypeTodoLiquidbounce.Trident
                projectileInfo == TrajectoryInfoTodoLiquidbounce.FIREWORK_ROCKET -> TrajectoryTypeTodoLiquidbounce.FireworkRocket
                projectileInfo == TrajectoryInfoTodoLiquidbounce.GENERIC -> TrajectoryTypeTodoLiquidbounce.Snowball
                projectileInfo.hitboxRadius == TrajectoryInfoTodoLiquidbounce.BOW_FULL_PULL.hitboxRadius
                    && projectileInfo.gravity == TrajectoryInfoTodoLiquidbounce.BOW_FULL_PULL.gravity
                    && projectileInfo.drag == TrajectoryInfoTodoLiquidbounce.BOW_FULL_PULL.drag
                    && projectileInfo.dragInWater == TrajectoryInfoTodoLiquidbounce.BOW_FULL_PULL.dragInWater
                    && projectileInfo.copiesPlayerVelocity == TrajectoryInfoTodoLiquidbounce.BOW_FULL_PULL.copiesPlayerVelocity -> {
                    TrajectoryTypeTodoLiquidbounce.Arrow
                }

                projectileInfo.gravity == 0.0 && projectileInfo.hitboxRadius >= 1.0 -> {
                    if (projectileInfo.copiesPlayerVelocity) {
                        TrajectoryTypeTodoLiquidbounce.Fireball
                    } else {
                        TrajectoryTypeTodoLiquidbounce.WindCharge
                    }
                }

                else -> TrajectoryTypeTodoLiquidbounce.Arrow
            }
        }

        override fun calculateAngleFor(
            projectileInfo: TrajectoryInfoTodoLiquidbounce,
            sourcePos: Vec3,
            targetPosFunction: PositionExtrapolationTodoLiquidbounce,
            targetShape: EntityDimensions
        ): RotationTodoLiquidbounce? {
            val rotation = SituationalProjectileAngleCalculatorTodoLiquidbounce
                .calculateAngleFor(projectileInfo, sourcePos, targetPosFunction, targetShape) ?: return null

            val renderer = TrajectoryInfoRendererTodoLiquidbounce.getHypotheticalTrajectory(
                simulationOwner = player,
                trajectoryInfo = projectileInfo,
                rotation = rotation,
                trajectoryType = resolveTrajectoryType(projectileInfo),
            )

            val result = renderer.runSimulation(300)
            val hit = result.hitResult ?: return null

            val baseTargetPos = targetPosFunction.getPositionInTicks(0.0)
            val targetBox = targetShape.makeBoundingBox(baseTargetPos)

            return if (hit is EntityHitResult && hit.entity.box.intersects(targetBox)) {
                rotation
            } else {
                null
            }
        }

    }
}
