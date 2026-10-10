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

package net.ccbluex.liquidbounce.render.engine.type

import net.ccbluex.liquidbounce.annotations.ValueClassCandidateTodoLiquidbounce

@ValueClassCandidateTodoLiquidbounce
@JvmRecord
data class BoundingBox2sTodoLiquidbounce(val min: UV2fTodoLiquidbounce, val max: UV2fTodoLiquidbounce) {
    constructor(rect: BoundingBox2fTodoLiquidbounce) : this(
        rect.xMin,
        rect.yMin,
        rect.xMax,
        rect.yMax,
    )

    constructor(xMin: Float, yMin: Float, xMax: Float, yMax: Float) : this(
        UV2fTodoLiquidbounce(xMin, yMin),
        UV2fTodoLiquidbounce(xMax, yMax)
    )
}
