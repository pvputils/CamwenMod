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

import net.ccbluex.liquidbounce.config.types.group.ValueGroupTodoLiquidbounce
import net.ccbluex.liquidbounce.event.EventListenerTodoLiquidbounce
import net.ccbluex.liquidbounce.features.addon.AddonApiTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.data.RotationTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrectionTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.features.processors.FailRotationProcessorTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.features.processors.ShortStopRotationProcessorTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl.AccelerationAngleSmoothTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl.AiAngleSmoothTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl.InterpolationAngleSmoothTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl.LinearAngleSmoothTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl.SigmoidAngleSmoothTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.client.RestrictedSingleUseActionTodoLiquidbounce
import net.minecraft.world.entity.Entity

/**
 * Configurable to configure the dynamic rotation engine
 */
@AddonApiTodoLiquidbounce
open class RotationsValueGroupTodoLiquidbounce(
    owner: EventListenerTodoLiquidbounce,
    movementCorrection: MovementCorrectionTodoLiquidbounce = MovementCorrectionTodoLiquidbounce.SILENT,
    combatSpecific: Boolean = false
) : ValueGroupTodoLiquidbounce("Rotations") {

    private val angleSmooth = modes(owner, "AngleSmooth", 0) {
        val linearAngleSmooth = LinearAngleSmoothTodoLiquidbounce(it)
        val interpolationAngleSmooth = if (combatSpecific) InterpolationAngleSmoothTodoLiquidbounce(it) else null

        listOfNotNull(
            linearAngleSmooth,
            SigmoidAngleSmoothTodoLiquidbounce(it),
            interpolationAngleSmooth,
            AccelerationAngleSmoothTodoLiquidbounce(it),
            if (combatSpecific) AiAngleSmoothTodoLiquidbounce(it, interpolationAngleSmooth ?: linearAngleSmooth) else null
        ).toTypedArray()
    }

    private val shortStop = if (combatSpecific) tree(ShortStopRotationProcessorTodoLiquidbounce(owner)) else null
    private val fail = if (combatSpecific) tree(FailRotationProcessorTodoLiquidbounce(owner)) else null

    private val movementCorrection by enumChoice("MovementCorrection", movementCorrection)
    private val resetThreshold by float("ResetThreshold", 2f, 1f..180f)
    private val ticksUntilReset by int("TicksUntilReset", 5, 1..30, "ticks")

    @AddonApiTodoLiquidbounce
    fun toRotationTarget(
        rotation: RotationTodoLiquidbounce,
        entity: Entity? = null,
        considerInventory: Boolean = false,
        whenReached: RestrictedSingleUseActionTodoLiquidbounce? = null
    ) = RotationTargetTodoLiquidbounce(
        rotation,
        entity,
        listOfNotNull(
            angleSmooth.activeMode,
            fail?.takeIf { it.running },
            shortStop?.takeIf { it.running }
        ),
        ticksUntilReset,
        resetThreshold,
        considerInventory,
        movementCorrection,
        whenReached
    )

    /**
     * How long it takes to rotate to a rotation in ticks
     *
     * Calculates the difference from the server rotation to the target rotation and divides it by the
     * minimum turn speed (to make sure we are always there in time)
     *
     * @param rotation The rotation to rotate to
     * @return The amount of ticks it takes to rotate to the rotation
     */
    fun calculateTicks(rotation: RotationTodoLiquidbounce) = angleSmooth.activeMode
        .calculateTicks(RotationManagerTodoLiquidbounce.actualServerRotation, rotation)

}
