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

package net.ccbluex.liquidbounce.config.types

import net.ccbluex.liquidbounce.config.ConfigSystemTodoLiquidbounce
import net.ccbluex.liquidbounce.config.ConfigSystemTodoLiquidbounce.configs
import net.ccbluex.liquidbounce.config.ConfigSystemTodoLiquidbounce.rootFolder
import net.ccbluex.liquidbounce.config.types.group.ValueGroupTodoLiquidbounce
import net.ccbluex.liquidbounce.features.addon.AddonApiTodoLiquidbounce
import java.io.File

@AddonApiTodoLiquidbounce
open class ConfigTodoLiquidbounce(name: String, value: MutableCollection<ValueTodoLiquidbounce<*>> = mutableListOf()) : ValueGroupTodoLiquidbounce(name, value) {

    /** Writes the file now; the client does so on exit anyway. */
    fun saveToDisk() = ConfigSystemTodoLiquidbounce.store(this)

    /** Re-reads the file, dropping unsaved changes. */
    fun loadFromDisk() = ConfigSystemTodoLiquidbounce.load(this)

    val jsonFile: File
        get() {
            require(this in configs) { "${this.name} is not registered" }
            return File(rootFolder, "${this.loweredName}.json")
        }

    /**
     * We write to this temp file, we can safely rename [jsonTmpFile] to [jsonFile],
     * to eliminate any chances of data loss.
     */
    val jsonTmpFile: File
        get() {
            require(this in configs) { "${this.name} is not registered" }
            return File(rootFolder, "${this.loweredName}.json.tmp")
        }

}
