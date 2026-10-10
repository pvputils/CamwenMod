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
package net.ccbluex.liquidbounce.utils.aiming.point

import net.ccbluex.liquidbounce.config.types.group.ValueGroupTodoLiquidbounce
import net.ccbluex.liquidbounce.event.EventListenerTodoLiquidbounce
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebugTodoLiquidbounce
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebugTodoLiquidbounce.debugGeometry
import net.ccbluex.liquidbounce.render.engine.type.Color4bTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.point.exempts.ExemptBestHitVectorTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.point.exempts.ExemptBoxPartTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.point.exempts.ExemptContextTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.point.features.PointProcessorDelayTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.point.features.PointProcessorGaussianTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.point.features.PointProcessorLazyTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.utils.projectPointsOnBox
import net.ccbluex.liquidbounce.utils.entity.PositionExtrapolationTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.entity.getBoundingBoxAt
import net.ccbluex.liquidbounce.utils.math.getNearestPoint
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

class PointTrackerTodoLiquidbounce(val parent: EventListenerTodoLiquidbounce) : ValueGroupTodoLiquidbounce("AimPoint"), EventListenerTodoLiquidbounce {

    override fun parent() = parent

    private val predicateBoxParts by multiEnumChoice<ExemptBoxPartTodoLiquidbounce>("ExemptBoxParts")
    private val predicateBestHitVector = tree(ExemptBestHitVectorTodoLiquidbounce(this))

    /**
     * This introduces a layer of randomness to the point tracker. A gaussian distribution is being used to
     * calculate the offset.
     */
    private val gaussian = tree(PointProcessorGaussianTodoLiquidbounce(this))

    /**
     * This will allow the point to stay at a certain position when the minimum threshold is not reached.
     */
    private val lazy = tree(PointProcessorLazyTodoLiquidbounce(this))

    /**
     * This will allow the point to be delayed until the ticks expire.
     */
    private val delay = tree(PointProcessorDelayTodoLiquidbounce(this))

    private val processors = arrayOf(delay, lazy, gaussian)

    /**
     * The point tracker is being used to track a certain point of an entity.
     *
     * @param entity The entity we want to track.
     */
    fun findPoint(eyes: Vec3, entity: Entity, ticks: Int = 0): PointInsideBoxTodoLiquidbounce {
        // Predict target position
        val targetPos = PositionExtrapolationTodoLiquidbounce.getBestForEntity(entity)
            .getPositionInTicks(ticks.toDouble())

        // Project points onto box
        val box = entity.getBoundingBoxAt(targetPos)
            // Support [ModuleHitbox]
            .inflate(entity.pickRadius.toDouble())
        val points = box.getPoints(eyes)

        val bestHitVector = points.minByOrNull { it.distanceToSqr(eyes) }
            ?: box.getPseudoClosest(eyes)
        val worstHitVector = points.maxByOrNull { it.distanceToSqr(eyes) }
            ?: box.getPseudoFurthest(eyes)

        // Filter exempts
        val predicateContext = ExemptContextTodoLiquidbounce(box, bestHitVector, worstHitVector)
        val predicates = predicateBoxParts + predicateBestHitVector
        val pointsWithExempts = points.filter { point ->
            predicates.none { predicate -> predicate.predicate(predicateContext, point) }
        }

        parent.debugGeometry("Points") {
            ModuleDebugTodoLiquidbounce.DebugCollection(points.map { point ->
                val percentage = calculateDistancePercentage(point, eyes, bestHitVector, worstHitVector)
                val color = if (point !in pointsWithExempts) {
                    Color4bTodoLiquidbounce.MAGENTA
                } else {
                    Color4bTodoLiquidbounce.GREEN.interpolateTo(Color4bTodoLiquidbounce.RED, percentage)
                }.fade(1.0f - percentage.toFloat())
                ModuleDebugTodoLiquidbounce.DebuggedPoint(point, color, 0.05)
            })
        }

        val pos = pointsWithExempts.minByOrNull { it.distanceToSqr(eyes) } ?: bestHitVector
        var point = PointInsideBoxTodoLiquidbounce(pos, box)
        for (processor in processors) {
            if (processor.enabled) {
                point = processor.process(point)
            }
        }
        return point
    }

    private fun AABB.getPoints(eyes: Vec3) = buildList {
        projectPointsOnBox(eyes, this@getPoints) { point ->
            add(point)
        }
    }

    private fun AABB.getPseudoClosest(eyes: Vec3) = getNearestPoint(eyes)

    private fun AABB.getPseudoFurthest(eyes: Vec3) = Vec3(
        farthestAxis(eyes.x, minX, maxX),
        farthestAxis(eyes.y, minY, maxY),
        farthestAxis(eyes.z, minZ, maxZ),
    )

    private fun farthestAxis(value: Double, min: Double, max: Double): Double {
        val distToMin = abs(value - min)
        val distToMax = abs(value - max)
        return if (distToMin > distToMax) min else max
    }

    // For debug visuals
    private fun calculateDistancePercentage(point: Vec3, eyes: Vec3, bestHitVector: Vec3,
                                            worstHitVector: Vec3
    ): Double {
        val pointDistance = point.distanceTo(eyes)
        val bestDistance = bestHitVector.distanceTo(eyes)
        val worstDistance = worstHitVector.distanceTo(eyes)
        return ((pointDistance - bestDistance) / (worstDistance - bestDistance)).coerceIn(0.0, 1.0)
    }

}
