/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package net.ccbluex.liquidbounce.config.types.group

import net.ccbluex.liquidbounce.config.types.ValueTodoLiquidbounce
import net.ccbluex.liquidbounce.test.MinecraftBootstrapTodoLiquidbounce
import kotlin.test.Test
import kotlin.test.assertEquals

class ValueGroupTraversalTestTodoLiquidbounce {

    companion object {
        init {
            MinecraftBootstrapTodoLiquidbounce.ensureInitialized()
        }
    }

    @Test
    fun `collectors include toggleable groups and every mode`() {
        val root = ValueGroupTodoLiquidbounce("Root")
        val direct = root.boolean("Direct", false)
        val toggle = root.tree(TestToggleable("Toggle")).apply {
            boolean("Nested", false)
        }
        val choice = root.modes(null, "Choice", { 0 }) { parent ->
            arrayOf(
                TestMode(parent, "First").apply { boolean("FirstValue", false) },
                TestMode(parent, "Second").apply { boolean("SecondValue", false) },
            )
        }
        root.walkKeyPath()

        assertEquals(
            listOf("Direct", "Toggle", "Enabled", "Nested", "Choice", "FirstValue", "SecondValue"),
            root.collectValuesRecursively().map(ValueTodoLiquidbounce<*>::name).toList()
        )
        assertEquals(
            listOf("Root", "Toggle", "Choice", "First", "Second"),
            root.collectValueGroupsRecursively().map(ValueGroupTodoLiquidbounce::name).toList()
        )
        assertEquals(listOf("First", "Second"), choice.modes.map(ModeTodoLiquidbounce::name))
    }

    @Test
    fun `prefix collectors prune unrelated branches and ignore case`() {
        val root = ValueGroupTodoLiquidbounce("Root")
        root.tree(ValueGroupTodoLiquidbounce("Visible")).boolean("Setting", false)
        root.tree(ValueGroupTodoLiquidbounce("Hidden")).boolean("Other", false)
        root.walkKeyPath()

        assertEquals(
            listOf("Setting"),
            root.collectValuesRecursively("LIQUIDBOUNCE.ROOT.VISIBLE").map(ValueTodoLiquidbounce<*>::name).toList()
        )
        assertEquals(
            listOf("Root", "Visible"),
            root.collectValueGroupsRecursively("liquidbounce.root.visible").map(ValueGroupTodoLiquidbounce::name).toList()
        )
    }

    private class TestToggleable(name: String) : ToggleableValueGroupTodoLiquidbounce(null, name, enabled = false)

    private class TestMode(
        override val parent: ModeValueGroupTodoLiquidbounce<*>,
        name: String,
    ) : ModeTodoLiquidbounce(name)
}
