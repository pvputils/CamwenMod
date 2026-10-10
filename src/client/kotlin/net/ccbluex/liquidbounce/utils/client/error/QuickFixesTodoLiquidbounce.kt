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
package net.ccbluex.liquidbounce.utils.client.error

import net.ccbluex.liquidbounce.features.addon.AddonApiTodoLiquidbounce
import java.util.concurrent.CopyOnWriteArrayList

/**
 * [ErrorHandler.fatal] shows the first registered quick fix whose [QuickFix.testError] matches, unless the
 * caller passes one.
 */
@AddonApiTodoLiquidbounce
object QuickFixesTodoLiquidbounce {

    // Errors reach the handler from any thread
    private val registry = CopyOnWriteArrayList<QuickFixTodoLiquidbounce>()

    val CLASS_NOT_FOUND = register(QuickFixTodoLiquidbounce(
        description = "Some class not found",
        testError = { it is ClassNotFoundException },
        whatYouNeed = InstructionsTodoLiquidbounce(false) { _ ->
            arrayOf(
                "Make sure you have all the libraries required by minecraft installed"
            )
        },
        whatToDo = InstructionsTodoLiquidbounce(false) {
            val message = it.message
            if (message == null) {
                null
            } else {
                when {
                    message.contains("viaversion") -> arrayOf("Try to install ViaFabric")
                    message.contains("modmenu") -> arrayOf("Try to install ModMenu")
                    else -> null
                }
            }
        }
    ))

    val BROWSER_IS_NOT_RESPONDING = QuickFixTodoLiquidbounce(
        description = "The browser is not responding",
        whatToDo = InstructionsTodoLiquidbounce(true) {
            arrayOf(
                "Disable System-wide proxy",
                "Disable Web Security/AV software",
                "Disable Smart App Control",
                "Restart LiquidBounce and try again."
            )
        }
    )

    val BROWSER_FAILED_TO_LOAD_UI = QuickFixTodoLiquidbounce(
        description = "The browser failed to load the UI.",
        whatToDo = InstructionsTodoLiquidbounce(true) {
            arrayOf(
                "Disable System-wide proxy",
                "Disable Web Security/AV software",
                "Restart LiquidBounce and try again."
            )
        }
    )

    val entries: List<QuickFixTodoLiquidbounce> get() = registry

    fun register(quickFix: QuickFixTodoLiquidbounce): QuickFixTodoLiquidbounce {
        registry += quickFix
        return quickFix
    }

    fun unregister(quickFix: QuickFixTodoLiquidbounce): Boolean = registry.remove(quickFix)

}
