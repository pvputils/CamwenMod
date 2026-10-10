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

package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot

import net.ccbluex.liquidbounce.event.events.GameTickEventTodoLiquidbounce
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModuleTodoLiquidbounce
import net.ccbluex.liquidbounce.features.module.ModuleCategoriesTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.RotationManagerTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroupTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.aiming.projectiles.SituationalProjectileAngleCalculatorTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.combat.TargetSelectorTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.entity.handItems
import net.ccbluex.liquidbounce.utils.kotlin.PriorityTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.render.trajectory.HeldItemTrajectoryResolverTodoLiquidbounce

object ModuleProjectileAimbotTodoLiquidbounce : ClientModuleTodoLiquidbounce("ProjectileAimbot", ModuleCategoriesTodoLiquidbounce.COMBAT) {

    private val targetSelector = TargetSelectorTodoLiquidbounce()
    private val rotations = RotationsValueGroupTodoLiquidbounce(this)

    init {
        tree(targetSelector)
        tree(rotations)
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEventTodoLiquidbounce> {
        val target = targetSelector.targets().firstOrNull() ?: return@handler

        val rotation = player.handItems.firstNotNullOfOrNull {
            val trajectoryDescriptor = HeldItemTrajectoryResolverTodoLiquidbounce.resolveHeldItemPrimaryShot(
                player,
                it,
                true
            ) ?: return@firstNotNullOfOrNull null

            SituationalProjectileAngleCalculatorTodoLiquidbounce.calculateAngleForEntity(
                trajectoryDescriptor.trajectoryInfo,
                target
            )
        } ?: return@handler

        RotationManagerTodoLiquidbounce.setRotationTarget(
            rotation,
            considerInventory = false,
            rotations,
            PriorityTodoLiquidbounce.IMPORTANT_FOR_USAGE_1,
            ModuleProjectileAimbotTodoLiquidbounce
        )
    }



}
