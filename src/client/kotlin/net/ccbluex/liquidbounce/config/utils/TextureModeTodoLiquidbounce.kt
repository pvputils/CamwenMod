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

package net.ccbluex.liquidbounce.config.utils

import net.ccbluex.liquidbounce.config.types.group.ModeTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroupTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.list.TaggedTodoLiquidbounce
import net.ccbluex.liquidbounce.config.types.toTextureProperty
import net.minecraft.client.renderer.texture.DynamicTexture

sealed class TextureModeTodoLiquidbounce(name: String) : ModeTodoLiquidbounce(name) {

    abstract val texture: DynamicTexture?

    class Custom(override val parent: ModeValueGroupTodoLiquidbounce<*>) : TextureModeTodoLiquidbounce("Custom") {
        override val texture by file("File", supportedExtensions = setOf("png")).toTextureProperty(this)
    }

    class Builtin<T : Builtin.Preset>(
        override val parent: ModeValueGroupTodoLiquidbounce<*>,
        default: T,
        choices: Set<T>,
    ) : TextureModeTodoLiquidbounce("Builtin") {

        private val mode = enumChoice("Preset", default, choices)

        override val texture get() = mode.get().texture

        interface Preset : TaggedTodoLiquidbounce {
            val texture: DynamicTexture?
        }
    }

}
