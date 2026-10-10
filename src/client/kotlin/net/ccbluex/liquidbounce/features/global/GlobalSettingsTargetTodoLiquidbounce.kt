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

package net.ccbluex.liquidbounce.features.global

import net.ccbluex.fastutil.enumSetAllOf
import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.config.types.group.ValueGroupTodoLiquidbounce
import net.ccbluex.liquidbounce.utils.combat.TargetsTodoLiquidbounce
import java.util.EnumSet

object GlobalSettingsTargetTodoLiquidbounce : ValueGroupTodoLiquidbounce(
    name = "Targets",
    aliases = listOf("Enemies")
) {

    val combatChoices = multiEnumChoice("Combat",
        default = enumSetOf(
            TargetsTodoLiquidbounce.PLAYERS,
            // codex start
            // Targets.HOSTILE,
            // codex end
            // codex start
            // Targets.ANGERABLE,
            // codex end
            // codex start
            // Targets.WATER_CREATURE,
            // codex end
            TargetsTodoLiquidbounce.INVISIBLE,
        ),
        choices = enumSetAllOf<TargetsTodoLiquidbounce>().apply { remove(TargetsTodoLiquidbounce.SELF) },
    )

    val visualChoices = multiEnumChoice("Visual",
        default = enumSetOf(
            TargetsTodoLiquidbounce.PLAYERS,
            // codex start
            // Targets.HOSTILE,
            // codex end
            // codex start
            // Targets.ANGERABLE,
            // codex end
            // codex start
            // Targets.WATER_CREATURE,
            // codex end
            TargetsTodoLiquidbounce.INVISIBLE,
        ),
        choices = enumSetAllOf(),
    )

    inline val combat: EnumSet<TargetsTodoLiquidbounce> get() = combatChoices.get() as EnumSet

    inline val visual: EnumSet<TargetsTodoLiquidbounce> get() = visualChoices.get() as EnumSet
}
