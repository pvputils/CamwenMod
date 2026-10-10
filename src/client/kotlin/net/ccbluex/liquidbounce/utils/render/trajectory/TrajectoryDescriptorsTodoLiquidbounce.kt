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

package net.ccbluex.liquidbounce.utils.render.trajectory

import net.minecraft.core.component.DataComponentGetter
import net.minecraft.world.item.ItemStack

@JvmRecord
data class TrajectoryDescriptorTodoLiquidbounce(
    val trajectoryInfo: TrajectoryInfoTodoLiquidbounce,
    val trajectoryType: TrajectoryTypeTodoLiquidbounce,
) {
    fun toShotDescriptor(
        yawOffsetDegrees: Float = 0f,
        icon: ItemStack = ItemStack.EMPTY,
        colorSource: DataComponentGetter = icon,
    ): TrajectoryShotDescriptorTodoLiquidbounce {
        return TrajectoryShotDescriptorTodoLiquidbounce(
            trajectoryInfo = trajectoryInfo,
            trajectoryType = trajectoryType,
            yawOffsetDegrees = yawOffsetDegrees,
            icon = icon,
            colorSource = colorSource,
        )
    }

    companion object {
        @JvmField
        val BOW_ARROW = TrajectoryDescriptorTodoLiquidbounce(TrajectoryInfoTodoLiquidbounce.BOW_FULL_PULL, TrajectoryTypeTodoLiquidbounce.Arrow)

        @JvmField
        val CROSSBOW_ARROW = TrajectoryDescriptorTodoLiquidbounce(TrajectoryInfoTodoLiquidbounce.CROSSBOW_ARROW, TrajectoryTypeTodoLiquidbounce.Arrow)

        @JvmField
        val ENTITY_ARROW = TrajectoryDescriptorTodoLiquidbounce(TrajectoryInfoTodoLiquidbounce(0.05, 0.3), TrajectoryTypeTodoLiquidbounce.Arrow)

        @JvmField
        val POTION = TrajectoryDescriptorTodoLiquidbounce(TrajectoryInfoTodoLiquidbounce.POTION, TrajectoryTypeTodoLiquidbounce.Potion)

        @JvmField
        val ENDER_PEARL = TrajectoryDescriptorTodoLiquidbounce(TrajectoryInfoTodoLiquidbounce.GENERIC, TrajectoryTypeTodoLiquidbounce.EnderPearl)

        @JvmField
        val FISHING_BOBBER = TrajectoryDescriptorTodoLiquidbounce(TrajectoryInfoTodoLiquidbounce.FISHING_ROD, TrajectoryTypeTodoLiquidbounce.FishingBobber)

        @JvmField
        val TRIDENT = TrajectoryDescriptorTodoLiquidbounce(TrajectoryInfoTodoLiquidbounce.TRIDENT, TrajectoryTypeTodoLiquidbounce.Trident)

        @JvmField
        val SNOWBALL = TrajectoryDescriptorTodoLiquidbounce(TrajectoryInfoTodoLiquidbounce.GENERIC, TrajectoryTypeTodoLiquidbounce.Snowball)

        @JvmField
        val EGG = TrajectoryDescriptorTodoLiquidbounce(TrajectoryInfoTodoLiquidbounce.GENERIC, TrajectoryTypeTodoLiquidbounce.Egg)

        @JvmField
        val EXP_BOTTLE = TrajectoryDescriptorTodoLiquidbounce(TrajectoryInfoTodoLiquidbounce.EXP_BOTTLE, TrajectoryTypeTodoLiquidbounce.ExpBottle)

        @JvmField
        val FIREWORK_ROCKET = TrajectoryDescriptorTodoLiquidbounce(TrajectoryInfoTodoLiquidbounce.FIREWORK_ROCKET, TrajectoryTypeTodoLiquidbounce.FireworkRocket)

        @JvmField
        val FIREBALL = TrajectoryDescriptorTodoLiquidbounce(TrajectoryInfoTodoLiquidbounce.FIREBALL, TrajectoryTypeTodoLiquidbounce.Fireball)

        @JvmField
        val WIND_CHARGE = TrajectoryDescriptorTodoLiquidbounce(TrajectoryInfoTodoLiquidbounce.WIND_CHARGE, TrajectoryTypeTodoLiquidbounce.WindCharge)
    }
}

@JvmRecord
data class TrajectoryShotDescriptorTodoLiquidbounce(
    val trajectoryInfo: TrajectoryInfoTodoLiquidbounce,
    val trajectoryType: TrajectoryTypeTodoLiquidbounce,
    val yawOffsetDegrees: Float = 0f,
    val icon: ItemStack = ItemStack.EMPTY,
    val colorSource: DataComponentGetter = icon,
)
