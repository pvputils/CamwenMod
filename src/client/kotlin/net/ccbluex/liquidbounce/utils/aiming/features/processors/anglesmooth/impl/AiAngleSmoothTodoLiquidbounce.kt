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
package net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl

import net.ccbluex.liquidbounce.config.types.group.ModeValueGroupTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.group.ValueGroupTodoLiquidbounce
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngineTodoLiquidbounce
import net.ccbluex.liquidbounce.deeplearn.ModelManagerTodoLiquidbounce.models
import net.ccbluex.liquidbounce.deeplearn.data.CombatSampleTodoLiquidbounce
import net.ccbluex.liquidbounce.deeplearn.models.TwoDimensionalRegressionModelTodoLiquidbounce
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebugTodoLiquidbounce
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.aiming.RotationManagerTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.RotationTargetTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.data.RotationTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.AngleSmoothTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.NoneAngleSmoothTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.client.ChronometerTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.entity.lastPos
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

/**
 * Record using
 * - [net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.DebugCombatRecorder]
 * - [net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.DebugCombatTrainerRecorder]
 * and then train a model - after that you will be able to use it with
 * [net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl.AiAngleSmooth].
 */
class AiAngleSmoothTodoLiquidbounce(
    parent: ModeValueGroupTodoLiquidbounce<*>,
    val fallback: AngleSmoothTodoLiquidbounce
) : AngleSmoothTodoLiquidbounce("AI", parent, listOf("Minarai")) {

    private val choices = modes<TwoDimensionalRegressionModelTodoLiquidbounce>("Model", 0) { local ->
        models.onChanged {
            runCatching {
                val activeModelName = local.activeMode.tag
                local.modes = models.modes
                val nextModelName = local.modes.firstOrNull { model -> model.tag == activeModelName }
                    ?.tag ?: models.activeMode.tag
                local.setByString(nextModelName)
            }.onFailure { error ->
                logger.error("Failed to sync AI model selection after model reload.", error)
            }
        }

        models.modes.toTypedArray()
    }

    private class OutputMultiplier : ValueGroupTodoLiquidbounce("OutputMultiplier") {
        val yawMultiplier by float("Yaw", 1.5f, 0.5f..2f)
        val pitchMultiplier by float("Pitch", 1f, 0.5f..2f)
    }

    private val correctionMode = modes(this, "Correction") {
        arrayOf(
            /**
             * Works best with the model, as it allows for the most natural movement.
             */
            InterpolationAngleSmoothTodoLiquidbounce(it, 2..5, 2..5, 95..100),
            /**
             * Not recommended to use this one, as it completely eliminates any acceleration
             * effects from the model.
             */
            LinearAngleSmoothTodoLiquidbounce(it,
                horizontalTurnSpeed = 5f..5f,
                verticalTurnSpeed = 5f..5f
            ),
            NoneAngleSmoothTodoLiquidbounce(it)
        )
    }

    private val outputMultiplier = tree(OutputMultiplier())

    companion object {
        private const val UNSUPPORTED_NOTIFICATION_TIME = 5000L
        private val notificationChronometer = ChronometerTodoLiquidbounce()
    }

    override fun process(
        rotationTarget: RotationTargetTodoLiquidbounce,
        currentRotation: RotationTodoLiquidbounce,
        targetRotation: RotationTodoLiquidbounce
    ): RotationTodoLiquidbounce {
        if (!DeepLearningEngineTodoLiquidbounce.isInitialized) {
            if (notificationChronometer.hasElapsed(UNSUPPORTED_NOTIFICATION_TIME)) {
                chat(markAsError(translation("liquidbounce.unsupportedDeepLearning")))
                chat(markAsError(translation(
                    "liquidbounce.rotationSystem.angleSmooth.ai.fallback",
                    fallback.name
                )))
                notificationChronometer.reset()
            }

            return fallback.process(rotationTarget, currentRotation, targetRotation)
        }

        val entity = rotationTarget.entity as? LivingEntity
        val prevRotation = RotationManagerTodoLiquidbounce.previousRotation ?: player.lastRotation
        val totalDelta = currentRotation.rotationDeltaTo(targetRotation)
        val velocityDelta = prevRotation.rotationDeltaTo(currentRotation)

        ModuleDebugTodoLiquidbounce.debugParameter(this, "DeltaYaw", totalDelta.deltaYaw)
        ModuleDebugTodoLiquidbounce.debugParameter(this, "DeltaPitch", totalDelta.deltaPitch)

        val input = CombatSampleTodoLiquidbounce(
            currentVector = currentRotation.directionVector,
            previousVector = prevRotation.directionVector,
            targetVector = targetRotation.directionVector,
            velocityDelta = velocityDelta.toVec2f(),

            playerDiff = player.position().subtract(player.lastPos),
            targetDiff = entity?.let { entity.position().subtract(entity.lastPos) } ?: Vec3.ZERO,

            hurtTime = entity?.let {entity.hurtTime } ?: 10,
            distance = entity?.let { player.squaredBoxedDistanceTo(entity).toFloat() } ?: 3f,
            age = 0
        )

        val (output, time) = runCatching {
            measureTimedValue {
                choices.activeMode.predict(input.asInput)
            }
        }.getOrElse {
            return fallback.process(rotationTarget, currentRotation, targetRotation)
        }
        if (output.size < 2 || !output[0].isFinite() || !output[1].isFinite()) {
            return fallback.process(rotationTarget, currentRotation, targetRotation)
        }
        ModuleDebugTodoLiquidbounce.debugParameter(this, "Output [0]", output[0])
        ModuleDebugTodoLiquidbounce.debugParameter(this, "Output [1]", output[1])
        ModuleDebugTodoLiquidbounce.debugParameter(this, "Time", "${time.toString(DurationUnit.MILLISECONDS, 2)} ms")

        val modelOutput = RotationTodoLiquidbounce(
            currentRotation.yaw + output[0] * outputMultiplier.yawMultiplier,
            currentRotation.pitch + output[1] * outputMultiplier.pitchMultiplier
        )

        return correctionMode.activeMode.process(
            rotationTarget,
            modelOutput,
            targetRotation
        )
    }

    override fun calculateTicks(
        currentRotation: RotationTodoLiquidbounce,
        targetRotation: RotationTodoLiquidbounce
    ): Int {
        // TODO: Implement correctly
        return correctionMode.activeMode.calculateTicks(currentRotation, targetRotation)
    }

}
